package com.homelab.share

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

class ShareActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                val uri = intentParcelable(intent, Intent.EXTRA_STREAM, Uri::class.java)
                if (uri != null) sendImage(uri) else finish()
            }
            else -> finish()
        }
    }

    private fun sendText(text: String) {
        scope.launch {
            runCatching {
                val body = JSONObject()
                    .put("type", "text")
                    .put("data", text)
                    .toString()
                    .toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(getString(R.string.server_url)).post(body).build()
                client.newCall(request).execute().use { it.isSuccessful }
            }.fold(
                onSuccess = { ok -> done(if (ok) "Sent" else "Server error") },
                onFailure = { e -> done("Failed: ${e.message}") }
            )
        }
    }

    private fun sendUrl(url: String) {
        scope.launch {
            runCatching {
                val body = JSONObject()
                    .put("type", "url")
                    .put("data", url)
                    .toString()
                    .toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(getString(R.string.server_url)).post(body).build()
                client.newCall(request).execute().use { it.isSuccessful }
            }.fold(
                onSuccess = { ok -> done(if (ok) "Sent" else "Server error") },
                onFailure = { e -> done("Failed: ${e.message}") }
            )
        }
    }

    private fun sendImage(uri: Uri) {
        scope.launch {
            runCatching {
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("Cannot read image")
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                val body = bytes.toRequestBody(mimeType.toMediaType())
                val request = Request.Builder().url(getString(R.string.server_url)).post(body).build()
                client.newCall(request).execute().use { it.isSuccessful }
            }.fold(
                onSuccess = { ok -> done(if (ok) "Sent" else "Server error") },
                onFailure = { e -> done("Failed: ${e.message}") }
            )
        }
    }

    private suspend fun done(message: String) = withContext(Dispatchers.Main) {
        Toast.makeText(this@ShareActivity, message, Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    @Suppress("DEPRECATION")
    private fun <T> intentParcelable(intent: Intent, key: String, clazz: Class<T>): T? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getParcelableExtra(key, clazz)
        else
            intent.getParcelableExtra(key)
}
