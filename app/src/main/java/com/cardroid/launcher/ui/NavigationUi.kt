package com.cardroid.launcher.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardroid.launcher.R
import com.cardroid.launcher.apps.AppLauncher
import com.cardroid.launcher.nav.Geocoder
import com.cardroid.launcher.nav.Place
import com.cardroid.launcher.nav.Trip
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SearchPanel(
    recents: List<Place>,
    nearLat: Double?,
    nearLon: Double?,
    onPick: (Place) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(query) {
        if (query.trim().length < 3) {
            results = emptyList()
            searching = false
            return@LaunchedEffect
        }
        delay(400)
        searching = true
        results = Geocoder.search(query.trim(), nearLat, nearLon)
        searching = false
    }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(
        modifier = modifier
            .clip(CardShape)
            .background(CarColors.Surface)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("¿A dónde vamos?", fontSize = 20.sp) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 22.sp, color = CarColors.TextPrimary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { results.firstOrNull()?.let(onPick) }),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus),
            )
            Spacer(Modifier.width(8.dp))
            CircleButton(R.drawable.ic_close, "Cerrar", onClose)
        }
        Spacer(Modifier.height(8.dp))

        val showingRecents = query.trim().length < 3
        val items = if (showingRecents) recents else results
        when {
            searching && results.isEmpty() -> Box(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = CarColors.Accent) }

            items.isEmpty() -> Text(
                text = if (showingRecents) "Escribe una dirección o un lugar" else "Sin resultados",
                color = CarColors.TextSecondary,
                fontSize = 18.sp,
                modifier = Modifier.padding(16.dp),
            )

            else -> LazyColumn {
                items(items) { place ->
                    PlaceRow(
                        place = place,
                        icon = if (showingRecents) R.drawable.ic_history else R.drawable.ic_place,
                        onClick = { onPick(place) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceRow(place: Place, @DrawableRes icon: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = CarColors.TextSecondary,
            modifier = Modifier.size(30.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = place.name,
                color = CarColors.TextPrimary,
                fontSize = 22.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (place.address.isNotBlank()) {
                Text(
                    text = place.address,
                    color = CarColors.TextSecondary,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun RouteCard(
    trip: Trip,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val route = trip.route
    Column(
        modifier = modifier
            .clip(CardShape)
            .background(CarColors.Surface)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                if (trip.fromPhone && !trip.started) {
                    Text("Enviado desde tu iPhone", color = CarColors.Accent, fontSize = 15.sp)
                }
                Text(
                    text = trip.place.name,
                    color = CarColors.TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (trip.place.address.isNotBlank() && !trip.started) {
                    Text(
                        text = trip.place.address,
                        color = CarColors.TextSecondary,
                        fontSize = 17.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            CircleButton(R.drawable.ic_close, "Cancelar ruta", onCancel)
        }
        Spacer(Modifier.height(12.dp))

        when {
            trip.loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = CarColors.Accent, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text("Calculando ruta…", color = CarColors.TextSecondary, fontSize = 18.sp)
            }

            route == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = trip.error ?: "Sin ruta",
                    color = CarColors.Warning,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f),
                )
                PillButton("Reintentar", primary = true, onClick = onRetry)
            }

            else -> {
                val arrival = Instant.ofEpochMilli(trip.calculatedAt + (route.durationSeconds * 1000).toLong())
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("HH:mm"))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formatDuration(route.durationSeconds),
                        color = CarColors.Teal,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "${formatKm(route.distanceMeters)} · llegada $arrival",
                        color = CarColors.TextSecondary,
                        fontSize = 19.sp,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }
                if (!trip.started) {
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PillButton("Iniciar", primary = true, onClick = onStart)
                        val lat = trip.place.latitude
                        val lon = trip.place.longitude
                        if (AppLauncher.isInstalled(context, "com.waze")) {
                            PillButton("Waze") {
                                AppLauncher.openUri(context, "https://waze.com/ul?ll=$lat,$lon&navigate=yes")
                            }
                        }
                        if (AppLauncher.isInstalled(context, "com.google.android.apps.maps")) {
                            PillButton("Google Maps") {
                                AppLauncher.openUri(context, "google.navigation:q=$lat,$lon")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoticeBanner(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xF0202124))
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(text = text, color = CarColors.TextPrimary, fontSize = 18.sp)
    }
}

@Composable
fun CircleButton(@DrawableRes icon: Int, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(CarColors.SurfaceHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = CarColors.TextPrimary,
            modifier = Modifier.size(26.dp),
        )
    }
}

@Composable
private fun PillButton(label: String, primary: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(if (primary) CarColors.Accent else CarColors.SurfaceHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (primary) Color(0xFF062E6F) else CarColors.TextPrimary,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun formatDuration(seconds: Double): String {
    val minutes = (seconds / 60).toInt().coerceAtLeast(1)
    return if (minutes < 60) "$minutes min" else "${minutes / 60} h ${minutes % 60} min"
}

private fun formatKm(meters: Double): String =
    if (meters < 1_000) "${meters.toInt()} m"
    else String.format(Locale.getDefault(), "%.1f km", meters / 1_000)
