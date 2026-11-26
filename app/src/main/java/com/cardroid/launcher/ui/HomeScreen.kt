package com.cardroid.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.cardroid.launcher.map.CarMap
import com.cardroid.launcher.map.CarMapState
import com.cardroid.launcher.media.NowPlaying
import com.cardroid.launcher.weather.Weather

/** Everything the home screen shows, gathered in one place. */
data class HomeState(
    val nowPlaying: NowPlaying?,
    val hasMediaAccess: Boolean,
    val weather: Weather?,
    val hasLocationPermission: Boolean,
    val speedKmh: Int,
    val tripMeters: Double,
    val bearing: Float?,
    val refreshKey: Int,
)

class HomeActions(
    val onPlayPause: () -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
    val onResetTrip: () -> Unit,
    val onRequestLocation: () -> Unit,
)

@Composable
fun HomeScreen(state: HomeState, actions: HomeActions, mapState: CarMapState) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(CarColors.Background)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(CardShape)
                    .background(CarColors.Surface),
            ) {
                CarMap(
                    state = mapState,
                    hasLocationPermission = state.hasLocationPermission,
                    modifier = Modifier.fillMaxSize(),
                )
                MapOverlay(
                    mapState = mapState,
                    speedKmh = state.speedKmh,
                    hasLocationPermission = state.hasLocationPermission,
                    onRequestLocation = actions.onRequestLocation,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(184.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                WeatherCard(
                    weather = state.weather,
                    hasLocationPermission = state.hasLocationPermission,
                    modifier = Modifier.weight(1f),
                )
                MusicCard(
                    nowPlaying = state.nowPlaying,
                    hasAccess = state.hasMediaAccess,
                    onPlayPause = actions.onPlayPause,
                    onNext = actions.onNext,
                    onPrevious = actions.onPrevious,
                    modifier = Modifier.weight(1.4f),
                )
            }
            Dock(
                refreshKey = state.refreshKey,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
            )
        }

        SidePanel(
            speedKmh = state.speedKmh,
            tripMeters = state.tripMeters,
            bearing = state.bearing,
            onResetTrip = actions.onResetTrip,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
    }
}
