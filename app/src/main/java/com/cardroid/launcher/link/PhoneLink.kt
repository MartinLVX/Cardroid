package com.cardroid.launcher.link

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import kotlin.coroutines.coroutineContext

/**
 * Receives destinations sent from the iPhone through ntfy.sh. The iPhone
 * Shortcut posts the shared text to a secret topic and the tablet listens to it.
 */
class PhoneLink(context: Context) {

    private val prefs = context.getSharedPreferences("cardroid", Context.MODE_PRIVATE)

    val topic: String = prefs.getString(KEY_TOPIC, null) ?: newTopic().also {
        prefs.edit().putString(KEY_TOPIC, it).apply()
    }

    val shortcutUrl: String get() = "https://ntfy.sh/$topic"

    /** Runs until cancelled, reconnecting whenever the network drops. */
    suspend fun listen(onMessage: suspend (String) -> Unit) {
        while (coroutineContext.isActive) {
            try {
                stream(onMessage)
            } catch (e: Exception) {
                // Network lost or ntfy unreachable: wait and reconnect.
            }
            delay(5_000)
        }
    }

    private suspend fun stream(onMessage: suspend (String) -> Unit) {
        val since = prefs.getString(KEY_LAST_ID, null) ?: "15m"
        val connection = withContext(Dispatchers.IO) {
            (URL("https://ntfy.sh/$topic/json?since=$since").openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                // ntfy sends a keepalive every 45 seconds.
                readTimeout = 120_000
            }
        }
        val handle = coroutineContext.job.invokeOnCompletion { connection.disconnect() }
        try {
            val reader = withContext(Dispatchers.IO) { connection.inputStream.bufferedReader() }
            while (coroutineContext.isActive) {
                val line = withContext(Dispatchers.IO) { reader.readLine() } ?: break
                val event = try { JSONObject(line) } catch (e: Exception) { continue }
                if (event.optString("event") != "message") continue
                prefs.edit().putString(KEY_LAST_ID, event.optString("id")).apply()
                val ageSeconds = System.currentTimeMillis() / 1000 - event.optLong("time")
                if (ageSeconds > MAX_AGE_SECONDS) continue
                val message = event.optString("message").trim()
                if (message.isNotEmpty()) onMessage(message)
            }
        } finally {
            handle.dispose()
            connection.disconnect()
        }
    }

    private fun newTopic(): String {
        val alphabet = "abcdefghijkmnpqrstuvwxyz23456789"
        val random = SecureRandom()
        return "cardroid-" + (1..16).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("")
    }

    private companion object {
        const val KEY_TOPIC = "ntfy_topic"
        const val KEY_LAST_ID = "ntfy_last_id"
        /** Destinations older than this (sent while the tablet was off) are ignored. */
        const val MAX_AGE_SECONDS = 15 * 60
    }
}
