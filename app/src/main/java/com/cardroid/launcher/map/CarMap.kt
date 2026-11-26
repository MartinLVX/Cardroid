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
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/** OpenStreetMap tiles hosted by OpenFreeMap: free, no account or key. */
private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val FOLLOW_ZOOM = 16.0

/** Keeps the map and lets the on-screen buttons move it. */
class CarMapState {
    internal var map: MapLibreMap? = null
    internal var style: Style? = null
    private var locationOn = false

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
                state.map = map
                state.style = style
                if (hasLocationPermission) state.enableLocation(context)
            }
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) state.enableLocation(context)
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}
