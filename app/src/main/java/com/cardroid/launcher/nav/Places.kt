package com.cardroid.launcher.nav

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale

data class Place(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("name", name)
        .put("address", address)
        .put("lat", latitude)
        .put("lon", longitude)

    companion object {
        fun fromJson(json: JSONObject) = Place(
            name = json.getString("name"),
            address = json.optString("address"),
            latitude = json.getDouble("lat"),
            longitude = json.getDouble("lon"),
        )
    }
}

/** Address search with Photon (OpenStreetMap data, free, no key). */
object Geocoder {

    suspend fun search(query: String, nearLat: Double?, nearLon: Double?): List<Place> =
        withContext(Dispatchers.IO) {
            try {
                val bias = if (nearLat != null && nearLon != null) {
                    String.format(Locale.US, "&lat=%.5f&lon=%.5f", nearLat, nearLon)
                } else ""
                val body = Http.get(
                    "https://photon.komoot.io/api/?limit=8&q=" +
                        URLEncoder.encode(query, "UTF-8") + bias
                )
                val features = JSONObject(body).getJSONArray("features")
                (0 until features.length()).mapNotNull { i -> parseFeature(features.getJSONObject(i)) }
            } catch (e: Exception) {
                emptyList()
            }
        }

    private fun parseFeature(feature: JSONObject): Place? {
        val coordinates = feature.optJSONObject("geometry")?.optJSONArray("coordinates") ?: return null
        val p = feature.optJSONObject("properties") ?: JSONObject()
        val street = listOf(p.optString("street"), p.optString("housenumber"))
            .filter { it.isNotBlank() }
            .joinToString(" ")
        val name = p.optString("name").ifBlank { street }.ifBlank { p.optString("city") }
        if (name.isBlank()) return null
        val address = listOf(
            street.takeIf { it != name },
            p.optString("district"),
            p.optString("city"),
            p.optString("state"),
        ).filter { !it.isNullOrBlank() && it != name }.distinct().joinToString(", ")
        return Place(name, address, coordinates.getDouble(1), coordinates.getDouble(0))
    }
}

/**
 * Turns whatever the iPhone shared (an Apple or Google Maps link, coordinates
 * or a plain address) into a place on the map.
 */
object DestinationResolver {

    private val urlRegex = Regex("https?://\\S+")
    private val coordinatePatterns = listOf(
        Regex("[?&](?:ll|sll|q|daddr|destination|coordinate|center|query)=(-?\\d{1,2}\\.\\d+)(?:,|%2C)\\s*(-?\\d{1,3}\\.\\d+)"),
        Regex("!3d(-?\\d{1,2}\\.\\d+)!4d(-?\\d{1,3}\\.\\d+)"),
        Regex("@(-?\\d{1,2}\\.\\d+),(-?\\d{1,3}\\.\\d+)"),
        Regex("^\\s*(-?\\d{1,2}\\.\\d+)\\s*,\\s*(-?\\d{1,3}\\.\\d+)\\s*$"),
    )

    suspend fun resolve(text: String, nearLat: Double?, nearLon: Double?): Place? =
        withContext(Dispatchers.IO) {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return@withContext null

            var link = urlRegex.find(trimmed)?.value
            if (link != null && findCoordinates(link) == null && queryFromUrl(link) == null) {
                link = (try { Http.expand(link) } catch (e: Exception) { null }) ?: link
            }

            val source = link ?: trimmed
            val name = link?.let { nameFromUrl(it) }
                ?: trimmed.replace(urlRegex, "").lines().firstOrNull { it.isNotBlank() }?.trim()

            findCoordinates(source)?.let { (lat, lon) ->
                return@withContext Place(name ?: "Destino", "", lat, lon)
            }

            val query = link?.let { queryFromUrl(it) }
                ?: trimmed.replace(urlRegex, " ").replace('\n', ' ').trim()
            if (query.isBlank()) return@withContext null
            Geocoder.search(query, nearLat, nearLon).firstOrNull()
        }

    private fun findCoordinates(text: String): Pair<Double, Double>? {
        for (pattern in coordinatePatterns) {
            val match = pattern.find(text) ?: continue
            val lat = match.groupValues[1].toDoubleOrNull() ?: continue
            val lon = match.groupValues[2].toDoubleOrNull() ?: continue
            if (lat in -90.0..90.0 && lon in -180.0..180.0) return lat to lon
        }
        return null
    }

    private fun param(url: String, key: String): String? {
        val match = Regex("[?&]$key=([^&#]+)").find(url) ?: return null
        return try {
            URLDecoder.decode(match.groupValues[1], "UTF-8").trim().takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    private fun nameFromUrl(url: String): String? {
        Regex("/place/([^/@?]+)").find(url)?.let {
            return URLDecoder.decode(it.groupValues[1], "UTF-8").replace('+', ' ')
        }
        return param(url, "name") ?: param(url, "q")?.takeIf { findCoordinates(it) == null }
            ?: param(url, "address")
    }

    private fun queryFromUrl(url: String): String? =
        param(url, "address")
            ?: param(url, "daddr")
            ?: param(url, "destination")
            ?: param(url, "q")?.takeIf { findCoordinates(it) == null }
            ?: Regex("/place/([^/@?]+)").find(url)?.let {
                URLDecoder.decode(it.groupValues[1], "UTF-8").replace('+', ' ')
            }
}

/** Last destinations, newest first. */
class RecentPlaces(context: Context) {
    private val prefs = context.getSharedPreferences("cardroid", Context.MODE_PRIVATE)
    private val _items = MutableStateFlow(load())
    val items: StateFlow<List<Place>> = _items.asStateFlow()

    fun add(place: Place) {
        val updated = (listOf(place) + _items.value.filterNot {
            it.name == place.name && Math.abs(it.latitude - place.latitude) < 0.0005
        }).take(8)
        _items.value = updated
        val array = JSONArray()
        updated.forEach { array.put(it.toJson()) }
        prefs.edit().putString("recent_places", array.toString()).apply()
    }

    private fun load(): List<Place> = try {
        val array = JSONArray(prefs.getString("recent_places", "[]"))
        (0 until array.length()).map { Place.fromJson(array.getJSONObject(it)) }
    } catch (e: Exception) {
        emptyList()
    }
}
