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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardroid.launcher.R
import com.cardroid.launcher.apps.AppLauncher
import com.cardroid.launcher.apps.Shortcut
import com.cardroid.launcher.apps.Shortcuts
import com.cardroid.launcher.media.NowPlaying
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TileShape = RoundedCornerShape(28.dp)

@Composable
fun HomeScreen(
    nowPlaying: NowPlaying?,
    hasMediaAccess: Boolean,
    installedVersion: Int,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenApps: () -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(CarColors.Background)
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(0.42f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Clock(modifier = Modifier.fillMaxWidth())
            NowPlayingCard(
                nowPlaying = nowPlaying,
                hasAccess = hasMediaAccess,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                onOpenPlayer = {
                    val pkg = nowPlaying?.packageName ?: "com.spotify.music"
                    if (!AppLauncher.open(context, pkg)) {
                        AppLauncher.openShortcut(context, Shortcuts.home.first { it.label == "Spotify" })
                    }
                },
                onGrantAccess = { AppLauncher.openNotificationAccessSettings(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }

        val shortcuts = Shortcuts.home
        Column(
            modifier = Modifier
                .weight(0.58f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                ShortcutTile(shortcuts[0], installedVersion, Modifier.weight(1f))
                ShortcutTile(shortcuts[1], installedVersion, Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                ShortcutTile(shortcuts[2], installedVersion, Modifier.weight(1f))
                ShortcutTile(shortcuts[3], installedVersion, Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.weight(0.6f),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                IconTile("Todas las apps", R.drawable.ic_apps, onOpenApps, Modifier.weight(1f))
                IconTile("Ajustes", R.drawable.ic_tune, { AppLauncher.openSettings(context) }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Clock(modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1_000)
        }
    }
    val locale = Locale.getDefault()
    val time = remember(now.hour, now.minute) {
        now.format(DateTimeFormatter.ofPattern("HH:mm", locale))
    }
    val date = remember(now.toLocalDate()) {
        now.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", locale))
            .replaceFirstChar { it.titlecase(locale) }
    }
    Column(modifier = modifier) {
        Text(
            text = time,
            color = CarColors.TextPrimary,
            fontSize = 96.sp,
            fontWeight = FontWeight.Light,
            lineHeight = 96.sp,
        )
        Text(text = date, color = CarColors.TextSecondary, fontSize = 24.sp)
    }
}

@Composable
private fun NowPlayingCard(
    nowPlaying: NowPlaying?,
    hasAccess: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenPlayer: () -> Unit,
    onGrantAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(TileShape)
            .background(CarColors.Surface)
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onOpenPlayer),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArt(nowPlaying?.art)
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                val title = when {
                    !hasAccess -> "Música"
                    nowPlaying == null || nowPlaying.title.isBlank() -> "Nada sonando"
                    else -> nowPlaying.title
                }
                val subtitle = when {
                    !hasAccess -> "Toca aquí abajo para ver la canción actual"
                    nowPlaying == null -> "Toca para abrir Spotify"
                    else -> nowPlaying.artist
                }
                Text(
                    text = title,
                    color = CarColors.TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = CarColors.TextSecondary,
                    fontSize = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (!hasAccess) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CarColors.SurfaceHigh)
                    .clickable(onClick = onGrantAccess)
                    .padding(16.dp),
            ) {
                Text(
                    text = "Activar acceso a notificaciones para ver la canción",
                    color = CarColors.Accent,
                    fontSize = 18.sp,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ControlButton(R.drawable.ic_skip_previous, "Anterior", onPrevious, size = 80)
            ControlButton(
                icon = if (nowPlaying?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play,
                description = "Reproducir o pausar",
                onClick = onPlayPause,
                size = 104,
                highlighted = true,
            )
            ControlButton(R.drawable.ic_skip_next, "Siguiente", onNext, size = 80)
        }
    }
}

@Composable
private fun AlbumArt(art: android.graphics.Bitmap?) {
    val bitmap = remember(art) { art?.asImageBitmap() }
    Box(
        modifier = Modifier
            .size(112.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CarColors.SurfaceHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_music),
                contentDescription = null,
                tint = CarColors.TextSecondary,
                modifier = Modifier.size(48.dp),
            )
        }
    }
}

@Composable
private fun ControlButton(
    @DrawableRes icon: Int,
    description: String,
    onClick: () -> Unit,
    size: Int,
    highlighted: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(if (highlighted) CarColors.Accent else CarColors.SurfaceHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = if (highlighted) CarColors.Background else CarColors.TextPrimary,
            modifier = Modifier.size((size * 0.5f).dp),
        )
    }
}

@Composable
private fun ShortcutTile(shortcut: Shortcut, installedVersion: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val iconPx = with(androidx.compose.ui.platform.LocalDensity.current) { 88.dp.roundToPx() }
    val icon: ImageBitmap? = remember(shortcut.packageName, installedVersion) {
        AppLauncher.icon(context, shortcut.packageName, iconPx)
    }
    val installed = remember(shortcut.packageName, installedVersion) {
        AppLauncher.isInstalled(context, shortcut.packageName)
    }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(TileShape)
            .background(CarColors.Surface)
            .clickable { AppLauncher.openShortcut(context, shortcut) }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(88.dp)
                    .alpha(if (installed) 1f else 0.4f),
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_navigation),
                contentDescription = null,
                tint = CarColors.TextSecondary,
                modifier = Modifier.size(72.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = shortcut.label,
            color = if (installed) CarColors.TextPrimary else CarColors.TextSecondary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
        )
        if (!installed) {
            Text(text = "Toca para instalar", color = CarColors.TextSecondary, fontSize = 16.sp)
        }
    }
}

@Composable
private fun IconTile(
    label: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxHeight()
            .clip(TileShape)
            .background(CarColors.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = CarColors.Accent,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(text = label, color = CarColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Medium)
    }
}
