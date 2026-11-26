package com.cardroid.launcher.nav

import com.cardroid.launcher.location.LocationTracker
import com.cardroid.launcher.map.CarMapState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class Trip(
    val place: Place,
    val route: Route? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val fromPhone: Boolean = false,
    /** When the route was calculated, to show the arrival time. */
    val calculatedAt: Long = System.currentTimeMillis(),
    val started: Boolean = false,
)

/** The current destination and route, shared by the map and the cards. */
class Navigation(
    private val scope: CoroutineScope,
    private val location: LocationTracker,
    private val recents: RecentPlaces,
    private val mapState: CarMapState,
) {
    private val _trip = MutableStateFlow<Trip?>(null)
    val trip: StateFlow<Trip?> = _trip.asStateFlow()

    /** Short notice about destinations arriving from the iPhone. */
    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    private var routeJob: Job? = null
    private var noticeJob: Job? = null

    fun navigateTo(place: Place, fromPhone: Boolean = false) {
        recents.add(place)
        routeJob?.cancel()
        mapState.clearRoute()
        _trip.value = Trip(place = place, fromPhone = fromPhone)
        routeJob = scope.launch {
            val fix = location.location.value
                ?: withTimeoutOrNull(20_000) { location.location.filterNotNull().first() }
            if (fix == null) {
                _trip.update { it?.copy(loading = false, error = "Todavía no hay señal GPS") }
                return@launch
            }
            val route = Router.route(fix.latitude, fix.longitude, place)
            if (route == null) {
                _trip.update { it?.copy(loading = false, error = "No se pudo calcular la ruta") }
                return@launch
            }
            _trip.update {
                it?.copy(route = route, loading = false, error = null, calculatedAt = System.currentTimeMillis())
            }
            mapState.showRoute(route, place)
        }
    }

    fun retry() {
        val trip = _trip.value ?: return
        navigateTo(trip.place, trip.fromPhone)
    }

    fun start() {
        _trip.update { it?.copy(started = true) }
        mapState.recenter()
    }

    fun cancel() {
        routeJob?.cancel()
        _trip.value = null
        mapState.clearRoute()
        mapState.recenter()
    }

    /** Text shared from the iPhone: a maps link, coordinates or an address. */
    fun handlePhoneMessage(text: String) {
        scope.launch {
            showNotice("Destino recibido del iPhone…")
            val fix = location.location.value
            val place = DestinationResolver.resolve(text, fix?.latitude, fix?.longitude)
            if (place == null) {
                showNotice("No encontré el destino que enviaste")
            } else {
                showNotice("Ruta a ${place.name}")
                navigateTo(place, fromPhone = true)
            }
        }
    }

    private fun showNotice(text: String) {
        noticeJob?.cancel()
        _notice.value = text
        noticeJob = scope.launch {
            delay(6_000)
            _notice.value = null
        }
    }
}
