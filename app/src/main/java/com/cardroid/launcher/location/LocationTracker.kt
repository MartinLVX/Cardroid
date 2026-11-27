package com.cardroid.launcher.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

/** GPS position, speed and distance driven since the trip counter was reset. */
class LocationTracker(private val context: Context) {

    private val locationManager = context.getSystemService(LocationManager::class.java)

    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location.asStateFlow()

    private val _speedKmh = MutableStateFlow(0)
    val speedKmh: StateFlow<Int> = _speedKmh.asStateFlow()

    private val _tripMeters = MutableStateFlow(0.0)
    val tripMeters: StateFlow<Double> = _tripMeters.asStateFlow()

    private var running = false
    private var lastTripPoint: Location? = null

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) = onLocation(location)
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}

        @Deprecated("Required on Android 10 and older")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun start() {
        if (running || !hasPermission()) return
        try {
            val providers = locationManager.allProviders
            for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
                if (provider in providers) {
                    locationManager.requestLocationUpdates(
                        provider, 1_000L, 0f, listener, Looper.getMainLooper()
                    )
                }
            }
            running = true
            val last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (last != null && _location.value == null) _location.value = last
        } catch (e: SecurityException) {
            running = false
        }
    }

    fun stop() {
        if (!running) return
        locationManager.removeUpdates(listener)
        running = false
    }

    fun resetTrip() {
        _tripMeters.value = 0.0
        lastTripPoint = null
    }

    private fun onLocation(location: Location) {
        val current = _location.value
        // Prefer GPS: ignore a network fix while a recent GPS fix exists.
        if (location.provider == LocationManager.NETWORK_PROVIDER &&
            current?.provider == LocationManager.GPS_PROVIDER &&
            location.time - current.time < 10_000
        ) return

        _location.value = location
        _speedKmh.value = if (location.hasSpeed()) (location.speed * 3.6f).roundToInt() else 0

        if (location.provider != LocationManager.GPS_PROVIDER || location.accuracy > 25f) return
        val previous = lastTripPoint
        if (previous == null) {
            lastTripPoint = location
            return
        }
        val meters = previous.distanceTo(location)
        if (meters in 5f..500f) {
            _tripMeters.value += meters
            lastTripPoint = location
        } else if (meters > 500f) {
            lastTripPoint = location
        }
    }
}
