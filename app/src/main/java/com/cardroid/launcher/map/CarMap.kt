package com.cardroid.launcher.map

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import com.cardroid.launcher.nav.Place
import com.cardroid.launcher.nav.Route
import java.util.Locale

/** OpenStreetMap tiles hosted by OpenFreeMap: free, no account or key. */
private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val FOLLOW_ZOOM = 16.0
private const val ROUTE_SOURCE = "cardroid-route"
private const val DEST_SOURCE = "cardroid-destination"
private const val EMPTY_GEOJSON = "{\"type\":\"FeatureCollection\",\"features\":[]}"

/** Keeps the map and lets the on-screen buttons move it. */
class CarMapState {
    internal var map: MapLibreMap? = null
    internal var style: Style? = null
    private var locationOn = false
    private var pendingRoute: Pair<Route, Place>? = null

    fun zoomIn() {
        map?.animateCamera(CameraUpdateFactory.zoomIn())
    }

    fun zoomOut() {
        map?.animateCamera(CameraUpdateFactory.zoomOut())
    }

    /** Goes back to following the car after the user moved the map. */
    fun recenter() {
        val map = map ?: return
        if (!locationOn) return
        map.locationComponent.cameraMode = CameraMode.TRACKING_GPS
        map.animateCamera(CameraUpdateFactory.zoomTo(FOLLOW_ZOOM))
    }

    /** Draws the route and shows all of it on screen. */
    fun showRoute(route: Route, destination: Place) {
        val map = map
        val style = style
        if (map == null || style == null) {
            pendingRoute = route to destination
            return
        }
        pendingRoute = null
        style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE)?.setGeoJson(route.geometryJson)
        style.getSourceAs<GeoJsonSource>(DEST_SOURCE)?.setGeoJson(
            String.format(
                Locale.US,
                "{\"type\":\"Point\",\"coordinates\":[%.6f,%.6f]}",
                destination.longitude,
                destination.latitude,
            )
        )
        if (locationOn) map.locationComponent.cameraMode = CameraMode.NONE
        val builder = LatLngBounds.Builder()
        route.points.forEach { (lat, lon) -> builder.include(LatLng(lat, lon)) }
        builder.include(LatLng(destination.latitude, destination.longitude))
        try {
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 120))
        } catch (e: Exception) {
            // A route with a single point has no area to fit.
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(destination.latitude, destination.longitude), 15.0)
            )
        }
    }

    fun clearRoute() {
        pendingRoute = null
        val style = style ?: return
        style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE)?.setGeoJson(EMPTY_GEOJSON)
        style.getSourceAs<GeoJsonSource>(DEST_SOURCE)?.setGeoJson(EMPTY_GEOJSON)
    }

    /** Route and destination layers, added once the map style has loaded. */
    internal fun onStyleLoaded(map: MapLibreMap, style: Style) {
        this.map = map
        this.style = style
        if (style.getSource(ROUTE_SOURCE) == null) {
            style.addSource(GeoJsonSource(ROUTE_SOURCE, EMPTY_GEOJSON))
            style.addSource(GeoJsonSource(DEST_SOURCE, EMPTY_GEOJSON))
            style.addLayer(
                LineLayer("cardroid-route-casing", ROUTE_SOURCE).withProperties(
                    PropertyFactory.lineColor("#0B3D91"),
                    PropertyFactory.lineWidth(11f),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                )
            )
            style.addLayer(
                LineLayer("cardroid-route-line", ROUTE_SOURCE).withProperties(
                    PropertyFactory.lineColor("#1A73E8"),
                    PropertyFactory.lineWidth(7f),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                )
            )
            style.addLayer(
                CircleLayer("cardroid-destination", DEST_SOURCE).withProperties(
                    PropertyFactory.circleColor("#EA4335"),
                    PropertyFactory.circleRadius(10f),
                    PropertyFactory.circleStrokeColor("#FFFFFF"),
                    PropertyFactory.circleStrokeWidth(3f),
                )
            )
        }
        pendingRoute?.let { (route, place) -> showRoute(route, place) }
    }

    @SuppressLint("MissingPermission")
    internal fun enableLocation(context: Context) {
        val map = map ?: return
        val style = style ?: return
        if (locationOn) return
        try {
            val component = map.locationComponent
            component.activateLocationComponent(
                LocationComponentActivationOptions.builder(context, style)
                    .useDefaultLocationEngine(true)
                    .build()
            )
            component.isLocationComponentEnabled = true
            component.renderMode = RenderMode.GPS
            component.cameraMode = CameraMode.TRACKING_GPS
            locationOn = true
            map.animateCamera(CameraUpdateFactory.zoomTo(FOLLOW_ZOOM))
        } catch (e: SecurityException) {
            locationOn = false
        }
    }
}

@Composable
fun CarMap(
    state: CarMapState,
    hasLocationPermission: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val mapView = remember {
        // Texture mode lets the map follow the rounded corners of its card.
        val options = MapLibreMapOptions.createFromAttributes(context).textureMode(true)
        MapView(context, options).apply { onCreate(null) }
    }

    DisposableEffect(lifecycle, mapView) {
        var started = false
        var resumed = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (!started) { mapView.onStart(); started = true }
                Lifecycle.Event.ON_RESUME -> if (!resumed) { mapView.onResume(); resumed = true }
                Lifecycle.Event.ON_PAUSE -> if (resumed) { mapView.onPause(); resumed = false }
                Lifecycle.Event.ON_STOP -> if (started) { mapView.onStop(); started = false }
                else -> Unit
            }
            Unit
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (resumed) mapView.onPause()
            if (started) mapView.onStop()
            mapView.onDestroy()
            state.map = null
            state.style = null
        }
    }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            map.uiSettings.isCompassEnabled = true
            map.uiSettings.isTiltGesturesEnabled = false
            // Mexico until the first GPS fix arrives.
            map.cameraPosition = CameraPosition.Builder()
                .target(LatLng(23.6, -102.5))
                .zoom(4.0)
                .build()
            map.setStyle(Style.Builder().fromUri(STYLE_URL)) { style ->
                state.onStyleLoaded(map, style)
                if (hasLocationPermission) state.enableLocation(context)
            }
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) state.enableLocation(context)
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}
