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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.cardroid.launcher.link.PhoneLink
import com.cardroid.launcher.location.LocationTracker
import com.cardroid.launcher.nav.Navigation
import com.cardroid.launcher.nav.RecentPlaces
import com.cardroid.launcher.ui.LinkPhonePanel
import kotlinx.coroutines.launch
import com.cardroid.launcher.map.CarMapState
import com.cardroid.launcher.media.NowPlayingTracker
import com.cardroid.launcher.ui.CardroidTheme
import com.cardroid.launcher.ui.HomeActions
import com.cardroid.launcher.ui.HomeScreen
import com.cardroid.launcher.ui.HomeState
import com.cardroid.launcher.weather.Weather
import com.cardroid.launcher.weather.WeatherClient
import kotlinx.coroutines.delay
import org.maplibre.android.MapLibre

private const val WEATHER_REFRESH_MS = 15 * 60 * 1000L

class MainActivity : ComponentActivity() {

    private lateinit var media: NowPlayingTracker
    private lateinit var location: LocationTracker
    private lateinit var recents: RecentPlaces
    private lateinit var phoneLink: PhoneLink
    private lateinit var navigation: Navigation
    private val mapState = CarMapState()
    private var showSearch by mutableStateOf(false)
    private var showLinkPhone by mutableStateOf(false)

    /** Bumped every time the home screen comes back, to refresh app lists and icons. */
    private var resumeCount by mutableIntStateOf(0)
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
        recents = RecentPlaces(applicationContext)
        phoneLink = PhoneLink(applicationContext)
        navigation = Navigation(lifecycleScope, location, recents, mapState)

        // Destinations shared from the iPhone arrive here while the launcher is alive.
        lifecycleScope.launch {
            phoneLink.listen { message -> navigation.handlePhoneMessage(message) }
        }

        setContent {
            CardroidTheme {
                val nowPlaying by media.nowPlaying.collectAsStateWithLifecycle()
                val hasMediaAccess by media.hasAccess.collectAsStateWithLifecycle()
                val currentLocation by location.location.collectAsStateWithLifecycle()
                val speedKmh by location.speedKmh.collectAsStateWithLifecycle()
                val tripMeters by location.tripMeters.collectAsStateWithLifecycle()
                val trip by navigation.trip.collectAsStateWithLifecycle()
                val notice by navigation.notice.collectAsStateWithLifecycle()
                val recentPlaces by recents.items.collectAsStateWithLifecycle()

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

                // A launcher never closes on Back; Back only closes panels.
                BackHandler {
                    showSearch = false
                    showLinkPhone = false
                }

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
                            trip = trip,
                            notice = notice,
                            recents = recentPlaces,
                            showSearch = showSearch,
                            latitude = currentLocation?.latitude,
                            longitude = currentLocation?.longitude,
                        ),
                        actions = HomeActions(
                            onPlayPause = media::playPause,
                            onNext = media::next,
                            onPrevious = media::previous,
                            onResetTrip = location::resetTrip,
                            onRequestLocation = requestLocation,
                            onOpenSearch = { showSearch = true },
                            onCloseSearch = { showSearch = false },
                            onPickPlace = { place ->
                                showSearch = false
                                navigation.navigateTo(place)
                            },
                            onStartTrip = navigation::start,
                            onCancelTrip = navigation::cancel,
                            onRetryTrip = navigation::retry,
                            onLinkPhone = { showLinkPhone = true },
                        ),
                        mapState = mapState,
                    )

                    if (showLinkPhone) {
                        LinkPhonePanel(
                            shortcutUrl = phoneLink.shortcutUrl,
                            onClose = { showLinkPhone = false },
                        )
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

    private fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}
