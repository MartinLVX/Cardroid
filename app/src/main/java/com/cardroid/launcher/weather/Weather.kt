package com.cardroid.launcher.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.roundToInt

data class Weather(
    val temperature: Int,
    val max: Int,
    val min: Int,
    val code: Int,
    val isDay: Boolean,
) {
    val emoji: String get() = describe(code, isDay).first
    val description: String get() = describe(code, isDay).second
}

/** Open-Meteo: free and needs no API key. */
object WeatherClient {

    suspend fun fetch(latitude: Double, longitude: Double): Weather? = withContext(Dispatchers.IO) {
        try {
            val url = URL(
                String.format(
                    Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.3f&longitude=%.3f" +
                        "&current=temperature_2m,weather_code,is_day" +
                        "&daily=temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=1",
                    latitude,
                    longitude,
                )
            )
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            val body = try {
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }
            val json = JSONObject(body)
            val current = json.getJSONObject("current")
            val daily = json.getJSONObject("daily")
            Weather(
                temperature = current.getDouble("temperature_2m").roundToInt(),
                max = daily.getJSONArray("temperature_2m_max").getDouble(0).roundToInt(),
                min = daily.getJSONArray("temperature_2m_min").getDouble(0).roundToInt(),
                code = current.getInt("weather_code"),
                isDay = current.getInt("is_day") == 1,
            )
        } catch (e: Exception) {
            null
        }
    }
}

/** WMO weather codes used by Open-Meteo. */
private fun describe(code: Int, isDay: Boolean): Pair<String, String> = when (code) {
    0 -> (if (isDay) "☀️" else "🌙") to "Despejado"
    1, 2 -> (if (isDay) "🌤️" else "🌙") to "Parcialmente nublado"
    3 -> "☁️" to "Nublado"
    45, 48 -> "🌫️" to "Niebla"
    51, 53, 55, 56, 57 -> "🌦️" to "Llovizna"
    61, 63, 65, 66, 67, 80, 81, 82 -> "🌧️" to "Lluvia"
    71, 73, 75, 77, 85, 86 -> "🌨️" to "Nieve"
    95, 96, 99 -> "⛈️" to "Tormenta"
    else -> "🌡️" to ""
}
