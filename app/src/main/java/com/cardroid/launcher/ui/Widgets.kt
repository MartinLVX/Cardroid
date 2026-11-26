package com.cardroid.launcher.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardroid.launcher.R
import com.cardroid.launcher.apps.AppLauncher
import com.cardroid.launcher.apps.Shortcuts
import com.cardroid.launcher.media.NowPlaying
import com.cardroid.launcher.weather.Weather

@Composable
fun WeatherCard(weather: Weather?, hasLocationPermission: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxHeight()
            .clip(CardShape)
            .background(CarColors.Surface)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (weather == null) {
            Text(
                text = if (hasLocationPermission) "Cargando clima…" else "El clima necesita tu ubicación",
                color = CarColors.TextSecondary,
                fontSize = 20.sp,
            )
            return@Row
        }
        Text(text = weather.emoji, fontSize = 72.sp)
        Spacer(Modifier.width(20.dp))
        Column {
            Text(
                text = "${weather.temperature}°",
                color = CarColors.TextPrimary,
                fontSize = 64.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 64.sp,
            )
            Text(
                text = "${weather.max}°  ${weather.min}°",
                color = CarColors.TextSecondary,
                fontSize = 22.sp,
            )
            Text(
                text = weather.description,
                color = CarColors.TextSecondary,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun MusicCard(
    nowPlaying: NowPlaying?,
    hasAccess: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val art = remember(nowPlaying?.art) { nowPlaying?.art?.asImageBitmap() }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(CardShape)
            .background(CarColors.Surface)
            .clickable {
                AppLauncher.open(context, nowPlaying?.packageName ?: Shortcuts.SPOTIFY)
            },
    ) {
        if (art != null) {
            Image(
                bitmap = art,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.55f),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xF01E1F22), Color(0x991E1F22), Color(0x331E1F22))
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            val title = when {
                !hasAccess -> "Música"
                nowPlaying == null || nowPlaying.title.isBlank() -> "Nada sonando"
                else -> nowPlaying.title
            }
            val subtitle = when {
                !hasAccess -> "Toca aquí para activar el acceso"
                nowPlaying == null -> "Toca para abrir Spotify"
                else -> nowPlaying.artist
            }
            Column(
                modifier = if (!hasAccess) {
                    Modifier.clickable { AppLauncher.openNotificationAccessSettings(context) }
                } else {
                    Modifier
                },
            ) {
                Text(
                    text = title,
                    color = CarColors.TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    color = if (!hasAccess) CarColors.Accent else CarColors.TextSecondary,
                    fontSize = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .width(132.dp)
                        .height(60.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(CarColors.Accent)
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(
                            if (nowPlaying?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play
                        ),
                        contentDescription = "Reproducir o pausar",
                        tint = Color(0xFF062E6F),
                        modifier = Modifier.size(34.dp),
                    )
                }
                RoundIcon(R.drawable.ic_skip_previous, "Anterior", onPrevious)
                RoundIcon(R.drawable.ic_skip_next, "Siguiente", onNext)
            }
        }
    }
}

@Composable
private fun RoundIcon(@DrawableRes icon: Int, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = CarColors.TextPrimary,
            modifier = Modifier.size(36.dp),
        )
    }
}

/** App icon loaded from the installed app, or null when it is missing. */
@Composable
fun rememberAppIcon(packageName: String, sizeDp: Int, refreshKey: Int) =
    LocalContext.current.let { context ->
        val px = with(LocalDensity.current) { sizeDp.dp.roundToPx() }
        remember(packageName, refreshKey) { AppLauncher.icon(context, packageName, px) }
    }
