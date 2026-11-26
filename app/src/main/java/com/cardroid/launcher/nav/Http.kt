package com.cardroid.launcher.nav

import java.net.HttpURLConnection
import java.net.URL

internal object Http {
    private const val USER_AGENT = "Cardroid/0.3 (Android car launcher)"

    fun get(url: String, timeoutMs: Int = 10_000): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = timeoutMs
        connection.readTimeout = timeoutMs
        connection.setRequestProperty("User-Agent", USER_AGENT)
        return try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    /** Follows redirects by hand (also across http/https) and returns the final URL. */
    fun expand(url: String): String? {
        var current = url
        repeat(6) {
            val connection = URL(current).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("User-Agent", USER_AGENT)
            try {
                val code = connection.responseCode
                val location = connection.getHeaderField("Location")
                if (code !in 300..399 || location == null) return current
                current = URL(URL(current), location).toString()
            } finally {
                connection.disconnect()
            }
        }
        return current
    }
}
