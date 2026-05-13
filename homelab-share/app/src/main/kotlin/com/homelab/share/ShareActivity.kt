package com.homelab.share

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

private const val TAG = "HomelabShare"
private const val CHANNEL_ID = "homelab_share_results"
private const val NOTIFICATION_ID = 1

class ShareActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureNotificationChannel()

        when {
            intent?.action == Intent.ACTION_SEND && intent.type == "text/plain" -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                when {
                    text == null -> finish()
                    text.startsWith("http://") || text.startsWith("https://") -> sendUrl(text)
                    else -> sendText(text)
                }
            }
            intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true -> {
                val uri = intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                if (uri != null) sendImage(uri) else finish()
            }
            else -> {
                Log.w(TAG, "Unhandled intent: action=${intent?.action} type=${intent?.type}")
                finish()
            }
        }
    }

    private fun sendText(text: String) {
        Log.d(TAG, "Sending text (${text.length} chars)")
        scope.launch {
            runCatching {
                val body = JSONObject()
                    .put("type", "text")
                    .put("data", text)
                    .toString()
                    .toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(getString(R.string.server_url)).post(body).build()
                client.newCall(request).execute().use { response ->
                    Log.d(TAG, "Response ${response.code} for text")
                    if (response.isSuccessful) null
                    else response.body?.string()?.trim()?.takeIf { it.isNotBlank() } ?: "HTTP ${response.code}"
                }
            }.fold(
                onSuccess = { errorBody -> done(errorBody == null, if (errorBody == null) "Sent to HomeLab" else "Server error\n$errorBody") },
                onFailure = { e ->
                    Log.e(TAG, "Text request failed", e)
                    done(false, "Failed: ${e.message}")
                }
            )
        }
    }

    private fun sendUrl(url: String) {
        Log.d(TAG, "Sending URL: $url")
        scope.launch {
            runCatching {
                val body = JSONObject()
                    .put("type", "url")
                    .put("data", url)
                    .toString()
                    .toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(getString(R.string.server_url)).post(body).build()
                client.newCall(request).execute().use { response ->
                    Log.d(TAG, "Response ${response.code} for URL")
                    if (response.isSuccessful) null
                    else response.body?.string()?.trim()?.takeIf { it.isNotBlank() } ?: "HTTP ${response.code}"
                }
            }.fold(
                onSuccess = { errorBody -> done(errorBody == null, if (errorBody == null) "Sent to HomeLab" else "Server error\n$errorBody") },
                onFailure = { e ->
                    Log.e(TAG, "URL request failed", e)
                    done(false, "Failed: ${e.message}")
                }
            )
        }
    }

    private fun sendImage(uri: Uri) {
        Log.d(TAG, "Sending image: $uri")
        scope.launch {
            runCatching {
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("Cannot read image")
                Log.d(TAG, "Image size: ${bytes.size} bytes")
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                val fileName = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
                Log.d(TAG, "Image mime=$mimeType name=$fileName")
                val body = bytes.toRequestBody("application/octet-stream".toMediaType())
                val request = Request.Builder()
                    .url(getString(R.string.server_url))
                    .addHeader("X-Mime-Type", mimeType)
                    .apply { if (fileName != null) addHeader("X-File-Name", fileName) }
                    .post(body)
                    .build()
                client.newCall(request).execute().use { response ->
                    Log.d(TAG, "Response ${response.code} for image")
                    if (response.isSuccessful) null
                    else response.body?.string()?.trim()?.takeIf { it.isNotBlank() } ?: "HTTP ${response.code}"
                }
            }.fold(
                onSuccess = { errorBody -> done(errorBody == null, if (errorBody == null) "Sent to HomeLab" else "Server error\n$errorBody") },
                onFailure = { e ->
                    Log.e(TAG, "Image request failed", e)
                    done(false, "Failed: ${e.message}")
                }
            )
        }
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
