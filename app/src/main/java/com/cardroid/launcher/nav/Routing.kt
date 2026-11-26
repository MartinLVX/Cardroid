package com.cardroid.launcher.nav

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

data class Route(
    val distanceMeters: Double,
    val durationSeconds: Double,
    /** GeoJSON LineString, ready for the map. */
    val geometryJson: String,
    /** (latitude, longitude) pairs along the route. */
    val points: List<Pair<Double, Double>>,
)

/** Driving routes from the public OSRM server (OpenStreetMap data, free, no key). */
object Router {

    suspend fun route(fromLat: Double, fromLon: Double, to: Place): Route? =
        withContext(Dispatchers.IO) {
            try {
                val url = String.format(
                    Locale.US,
                    "https://router.project-osrm.org/route/v1/driving/%.6f,%.6f;%.6f,%.6f" +
                        "?overview=full&geometries=geojson",
                    fromLon, fromLat, to.longitude, to.latitude,
                )
                val json = JSONObject(Http.get(url, timeoutMs = 15_000))
                if (json.optString("code") != "Ok") return@withContext null
                val route = json.getJSONArray("routes").getJSONObject(0)
                val geometry = route.getJSONObject("geometry")
                val coordinates = geometry.getJSONArray("coordinates")
                val points = (0 until coordinates.length()).map { i ->
                    val point = coordinates.getJSONArray(i)
                    point.getDouble(1) to point.getDouble(0)
                }
                Route(
                    distanceMeters = route.getDouble("distance"),
                    durationSeconds = route.getDouble("duration"),
                    geometryJson = geometry.toString(),
                    points = points,
                )
            } catch (e: Exception) {
                null
            }
        }
}
