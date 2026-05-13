package com.homelab.share

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import android.os.Bundle
import android.security.KeyChain
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.Socket
import java.security.KeyStore
import java.security.Principal
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509KeyManager
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.resume

private const val TAG = "HomelabShare"
private const val CHANNEL_ID = "homelab_share_results"
private const val NOTIFICATION_ID = 1
private const val PREFS_NAME = "homelab_share_prefs"
private const val KEY_CERT_ALIAS = "cert_alias"

class ShareActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureNotificationChannel()

        scope.launch {
            when {
                intent?.action == Intent.ACTION_SEND && intent.type == "text/plain" -> {
                    val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                    when {
                        text == null -> withContext(Dispatchers.Main) { finish() }
                        text.startsWith("http://") || text.startsWith("https://") -> sendUrl(text)
                        else -> sendText(text)
                    }
                }
                intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true -> {
                    val uri = intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    if (uri != null) sendImage(uri) else withContext(Dispatchers.Main) { finish() }
                }
                else -> {
                    Log.w(TAG, "Unhandled intent: action=${intent?.action} type=${intent?.type}")
                    withContext(Dispatchers.Main) { finish() }
                }
            }
        }
    }

    // Executes a request, retrying with a client certificate if the server demands mTLS.
    // On first mTLS failure the system cert picker is shown; the selected alias is stored
    // so subsequent calls skip straight to the mTLS client.
    private suspend fun executeRequest(buildRequest: () -> Request): String? {
        val storedAlias = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CERT_ALIAS, null)
        val client = if (storedAlias != null) buildClientForAlias(storedAlias) else OkHttpClient()

        val firstCode: Int
        val firstError: String?
        client.newCall(buildRequest()).execute().use { response ->
            firstCode = response.code
            Log.d(TAG, "Response $firstCode")
            firstError = if (response.isSuccessful) null
                         else response.body?.string()?.trim()?.takeIf { it.isNotBlank() } ?: "HTTP $firstCode"
        }

        if (firstCode != 404 || storedAlias != null) return firstError

        Log.d(TAG, "Got 404 without cert, mTLS likely required")
        val alias = promptForCertAlias() ?: return "Certificate required but none selected"
        return buildClientForAlias(alias).newCall(buildRequest()).execute().use { response ->
            Log.d(TAG, "Retry response ${response.code}")
            if (response.isSuccessful) null
            else response.body?.string()?.trim()?.takeIf { it.isNotBlank() } ?: "HTTP ${response.code}"
        }
    }

    private suspend fun buildClientForAlias(alias: String): OkHttpClient {
        val privateKey = KeyChain.getPrivateKey(this@ShareActivity, alias)
            ?: return OkHttpClient().also { Log.w(TAG, "Private key unavailable for $alias") }
        val certChain = KeyChain.getCertificateChain(this@ShareActivity, alias)
            ?: return OkHttpClient().also { Log.w(TAG, "Cert chain unavailable for $alias") }

        val keyManager = object : X509KeyManager {
            override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?) = arrayOf(alias)
            override fun chooseClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, socket: Socket?) = alias
            override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?) = null
            override fun chooseServerAlias(keyType: String?, issuers: Array<out Principal>?, socket: Socket?) = null
            override fun getCertificateChain(alias: String?): Array<X509Certificate> = certChain
            override fun getPrivateKey(alias: String?) = privateKey
        }

        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(null as KeyStore?)
        val trustManager = tmf.trustManagers.filterIsInstance<X509TrustManager>().first()

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(arrayOf(keyManager), null, null)

        Log.d(TAG, "mTLS client ready for alias: $alias")
        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .build()
    }

    // Always shows the system picker, then stores (or clears) the result.
    private suspend fun promptForCertAlias(): String? {
        val selected = withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                KeyChain.choosePrivateKeyAlias(
                    this@ShareActivity,
                    { alias -> cont.resume(alias) },
                    null, null,
                    Uri.parse(getString(R.string.server_url)), null
                )
            }
        }
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (selected != null) {
            prefs.edit().putString(KEY_CERT_ALIAS, selected).apply()
            Log.d(TAG, "Cert alias stored: $selected")
        } else {
            prefs.edit().remove(KEY_CERT_ALIAS).apply()
            Log.d(TAG, "Cert picker dismissed, alias cleared")
        }
        return selected
    }

    private suspend fun sendText(text: String) {
        Log.d(TAG, "Sending text (${text.length} chars)")
        runCatching {
            executeRequest {
                JSONObject().put("type", "text").put("data", text).toString()
                    .toRequestBody("application/json".toMediaType())
                    .let { Request.Builder().url(getString(R.string.server_url)).post(it).build() }
            }
        }.fold(
            onSuccess = { errorBody -> done(errorBody == null, if (errorBody == null) "Sent to HomeLab" else "Server error\n$errorBody") },
            onFailure = { e ->
                Log.e(TAG, "Text request failed", e)
                done(false, "Failed: ${e.message}")
            }
        )
    }

    private suspend fun sendUrl(url: String) {
        Log.d(TAG, "Sending URL: $url")
        runCatching {
            executeRequest {
                JSONObject().put("type", "url").put("data", url).toString()
                    .toRequestBody("application/json".toMediaType())
                    .let { Request.Builder().url(getString(R.string.server_url)).post(it).build() }
            }
        }.fold(
            onSuccess = { errorBody -> done(errorBody == null, if (errorBody == null) "Sent to HomeLab" else "Server error\n$errorBody") },
            onFailure = { e ->
                Log.e(TAG, "URL request failed", e)
                done(false, "Failed: ${e.message}")
            }
        )
    }

    private suspend fun sendImage(uri: Uri) {
        Log.d(TAG, "Sending image: $uri")
        runCatching {
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("Cannot read image")
            Log.d(TAG, "Image size: ${bytes.size} bytes")
            val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
            val fileName = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            Log.d(TAG, "Image mime=$mimeType name=$fileName")
            executeRequest {
                bytes.toRequestBody("application/octet-stream".toMediaType())
                    .let {
                        Request.Builder()
                            .url(getString(R.string.server_url))
                            .addHeader("X-Mime-Type", mimeType)
                            .apply { if (fileName != null) addHeader("X-File-Name", fileName) }
                            .post(it)
                            .build()
                    }
            }
        }.fold(
            onSuccess = { errorBody -> done(errorBody == null, if (errorBody == null) "Sent to HomeLab" else "Server error\n$errorBody") },
            onFailure = { e ->
                Log.e(TAG, "Image request failed", e)
                done(false, "Failed: ${e.message}")
            }
        )
    }

    private suspend fun done(success: Boolean, message: String) = withContext(Dispatchers.Main) {
        Log.d(TAG, "Result: $message")
        if (success) {
            Toast.makeText(this@ShareActivity, message, Toast.LENGTH_SHORT).show()
        } else {
            notify(message)
        }
        finish()
    }

    private fun notify(message: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Notification permission not granted — skipping: $message")
            return
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Homelab Share")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Share Results", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
