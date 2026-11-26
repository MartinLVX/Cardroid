package com.cardroid.launcher

import android.Manifest
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cardroid.launcher.apps.AppEntry
import com.cardroid.launcher.apps.AppLauncher
import com.cardroid.launcher.location.LocationTracker
import com.cardroid.launcher.map.CarMapState
import com.cardroid.launcher.media.NowPlayingTracker
import com.cardroid.launcher.ui.AppDrawer
import com.cardroid.launcher.ui.CardroidTheme
import com.cardroid.launcher.ui.HomeActions
import com.cardroid.launcher.ui.HomeScreen
import com.cardroid.launcher.ui.HomeState
import com.cardroid.launcher.weather.Weather
import com.cardroid.launcher.weather.WeatherClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre

private const val WEATHER_REFRESH_MS = 15 * 60 * 1000L

class MainActivity : ComponentActivity() {

    private lateinit var media: NowPlayingTracker
    private lateinit var location: LocationTracker

    /** Bumped every time the home screen comes back, to refresh app lists and icons. */
    private var resumeCount by mutableIntStateOf(0)
    private var showApps by mutableStateOf(false)
    private var hasLocationPermission by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(applicationContext)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()

        media = NowPlayingTracker(applicationContext)
        location = LocationTracker(applicationContext)
        hasLocationPermission = location.hasPermission()

        setContent {
            CardroidTheme {
                val nowPlaying by media.nowPlaying.collectAsStateWithLifecycle()
                val hasMediaAccess by media.hasAccess.collectAsStateWithLifecycle()
                val currentLocation by location.location.collectAsStateWithLifecycle()
                val speedKmh by location.speedKmh.collectAsStateWithLifecycle()
                val tripMeters by location.tripMeters.collectAsStateWithLifecycle()
                val mapState = remember { CarMapState() }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    hasLocationPermission = location.hasPermission()
                    location.start()
                }
                val requestLocation = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        )
                    )
                }
                LaunchedEffect(Unit) {
                    if (!hasLocationPermission) requestLocation()
                }

                // Weather follows the car, refreshed every 15 minutes.
                val hasFix = currentLocation != null
                val weather by produceState<Weather?>(null, hasFix) {
                    while (hasFix) {
                        val fix = location.location.value ?: break
                        WeatherClient.fetch(fix.latitude, fix.longitude)?.let { value = it }
                        delay(WEATHER_REFRESH_MS)
                    }
                }

                val bearing = currentLocation
                    ?.takeIf { it.hasBearing() && speedKmh >= 5 }
                    ?.bearing

                BackHandler { showApps = false }

                Box(Modifier.fillMaxSize()) {
                    HomeScreen(
                        state = HomeState(
                            nowPlaying = nowPlaying,
                            hasMediaAccess = hasMediaAccess,
                            weather = weather,
                            hasLocationPermission = hasLocationPermission,
                            speedKmh = speedKmh,
                            tripMeters = tripMeters,
                            bearing = bearing,
                            refreshKey = resumeCount,
                        ),
                        actions = HomeActions(
                            onPlayPause = media::playPause,
                            onNext = media::next,
                            onPrevious = media::previous,
                            onOpenApps = { showApps = true },
                            onResetTrip = location::resetTrip,
                            onRequestLocation = requestLocation,
                        ),
                        mapState = mapState,
                    )

                    // Drawn on top so the map keeps running underneath.
                    if (showApps) {
                        val iconPx = with(LocalDensity.current) { 72.dp.roundToPx() }
                        val apps by produceState<List<AppEntry>?>(null, resumeCount) {
                            value = withContext(Dispatchers.IO) {
                                AppLauncher.loadApps(applicationContext, iconPx)
                            }
                        }
                        AppDrawer(apps = apps, onClose = { showApps = false })
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeCount++
        hideSystemBars()
        hasLocationPermission = location.hasPermission()
        location.start()
        // Picks up notification access right after the user grants it.
        media.start()
    }

    override fun onPause() {
        super.onPause()
        media.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        location.stop()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        // Pressing Home while on the launcher returns to the main screen.
        showApps = false
    }

    private fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}
