package com.cardroid.launcher.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardroid.launcher.R
import com.cardroid.launcher.media.LibraryItem
import com.cardroid.launcher.media.LibraryState
import com.cardroid.launcher.media.NowPlaying
import com.cardroid.launcher.media.QueueEntry
import kotlinx.coroutines.delay

class PlayerActions(
    val onClose: () -> Unit,
    val onPlayPause: () -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
    val onSeek: (Long) -> Unit,
    val onQueueItem: (Long) -> Unit,
    val onLibraryItem: (LibraryItem) -> Unit,
    val onLibraryBack: () -> Unit,
    val onGrantAccess: () -> Unit,
)

private enum class PlayerTab { Queue, Library }

/** Full-screen player, so music is controlled here instead of inside Spotify. */
@Composable
fun MusicPlayer(
    nowPlaying: NowPlaying?,
    hasAccess: Boolean,
    library: LibraryState,
    actions: PlayerActions,
) {
    val art = remember(nowPlaying?.art) { nowPlaying?.art?.asImageBitmap() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CarColors.Background)
            // Keeps taps from reaching the map underneath.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
    ) {
        if (art != null) {
            Image(
                bitmap = art,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(60.dp)
                    .alpha(0.45f),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0x66000000), Color(0xE6000000))))
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            // Cover and controls.
            Column(
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight(),
            ) {
                CircleButton(R.drawable.ic_expand_more, "Cerrar reproductor", actions.onClose)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(CarColors.SurfaceHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (art != null) {
                            Image(
                                bitmap = art,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_music),
                                contentDescription = null,
                                tint = CarColors.TextSecondary,
                                modifier = Modifier.size(96.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(28.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = when {
                                !hasAccess -> "Música"
                                nowPlaying == null || nowPlaying.title.isBlank() -> "Nada sonando"
                                else -> nowPlaying.title
                            },
                            color = CarColors.TextPrimary,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 40.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = nowPlaying?.artist.orEmpty(),
                            color = CarColors.TextPrimary.copy(alpha = 0.8f),
                            fontSize = 24.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!nowPlaying?.album.isNullOrBlank()) {
                            Text(
                                text = nowPlaying?.album.orEmpty(),
                                color = CarColors.TextSecondary,
                                fontSize = 20.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (!hasAccess) {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Activar acceso a notificaciones para ver la canción",
                                color = CarColors.Accent,
                                fontSize = 18.sp,
                                modifier = Modifier.clickable(onClick = actions.onGrantAccess),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                ProgressBar(nowPlaying = nowPlaying, onSeek = actions.onSeek)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(36.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BigControl(R.drawable.ic_skip_previous, "Anterior", 84, false, actions.onPrevious)
                    BigControl(
                        icon = if (nowPlaying?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play,
                        description = "Reproducir o pausar",
                        size = 112,
                        highlighted = true,
                        onClick = actions.onPlayPause,
                    )
                    BigControl(R.drawable.ic_skip_next, "Siguiente", 84, false, actions.onNext)
                }
            }

            // Queue and library.
            SidePlaylist(
                nowPlaying = nowPlaying,
                library = library,
                actions = actions,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun ProgressBar(nowPlaying: NowPlaying?, onSeek: (Long) -> Unit) {
    val duration = nowPlaying?.durationMs ?: 0L
    var position by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(nowPlaying) {
        while (true) {
            position = nowPlaying?.currentPositionMs() ?: 0L
            delay(500)
        }
    }
    val fraction = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    Column {
        Slider(
            value = dragging ?: fraction,
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { if (duration > 0) onSeek((it * duration).toLong()) }
                dragging = null
            },
            enabled = duration > 0,
            colors = SliderDefaults.colors(
                thumbColor = CarColors.TextPrimary,
                activeTrackColor = CarColors.TextPrimary,
                inactiveTrackColor = CarColors.TextPrimary.copy(alpha = 0.25f),
            ),
        )
        Row(Modifier.fillMaxWidth()) {
            val shown = dragging?.let { (it * duration).toLong() } ?: position
            Text(formatTime(shown), color = CarColors.TextSecondary, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            Text(
                text = if (duration > 0) formatTime(duration) else "",
                color = CarColors.TextSecondary,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun BigControl(
    @DrawableRes icon: Int,
    description: String,
    size: Int,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(if (highlighted) CarColors.TextPrimary else Color(0x33FFFFFF))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = if (highlighted) Color.Black else CarColors.TextPrimary,
            modifier = Modifier.size((size * 0.5f).dp),
        )
    }
}

@Composable
private fun SidePlaylist(
    nowPlaying: NowPlaying?,
    library: LibraryState,
    actions: PlayerActions,
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableStateOf(PlayerTab.Queue) }
    Column(
        modifier = modifier
            .clip(CardShape)
            .background(Color(0x99121212))
            .padding(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TabChip("A continuación", tab == PlayerTab.Queue) { tab = PlayerTab.Queue }
            TabChip("Biblioteca", tab == PlayerTab.Library) { tab = PlayerTab.Library }
        }
        Spacer(Modifier.height(12.dp))
        when (tab) {
            PlayerTab.Queue -> QueueList(nowPlaying, actions.onQueueItem)
            PlayerTab.Library -> LibraryList(library, actions)
        }
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) CarColors.TextPrimary else Color(0x22FFFFFF))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            text = label,
            color = if (selected) Color.Black else CarColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun QueueList(nowPlaying: NowPlaying?, onQueueItem: (Long) -> Unit) {
    val queue = nowPlaying?.queue.orEmpty()
    if (queue.isEmpty()) {
        EmptyText("La app de música no comparte su cola de reproducción")
        return
    }
    // Show from the current song onwards.
    val currentIndex = queue.indexOfFirst { it.id == nowPlaying?.activeQueueId }.coerceAtLeast(0)
    LazyColumn {
        items(queue.drop(currentIndex), key = { it.id }) { entry ->
            TrackRow(
                title = entry.title,
                subtitle = entry.subtitle,
                art = entry.art,
                highlighted = entry.id == nowPlaying?.activeQueueId,
                trailing = null,
                onClick = { onQueueItem(entry.id) },
            )
        }
    }
}

@Composable
private fun LibraryList(library: LibraryState, actions: PlayerActions) {
    when (library.status) {
        LibraryState.Status.Idle, LibraryState.Status.Connecting -> Loading()
        LibraryState.Status.Unavailable -> EmptyText(
            "Esta app de música no permite explorar su biblioteca desde otras apps. " +
                "Puedes elegir qué escuchar desde Spotify en tu iPhone con Spotify Connect."
        )
        LibraryState.Status.Ready -> Column {
            if (library.path.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = actions.onLibraryBack)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_left),
                        contentDescription = "Atrás",
                        tint = CarColors.TextPrimary,
                        modifier = Modifier.size(32.dp),
                    )
                    Text(
                        text = library.path.last(),
                        color = CarColors.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            when {
                library.loading -> Loading()
                library.items.isEmpty() -> EmptyText("Aquí no hay nada")
                else -> LazyColumn {
                    items(library.items) { item ->
                        TrackRow(
                            title = item.title,
                            subtitle = item.subtitle,
                            art = item.art,
                            highlighted = false,
                            trailing = if (item.browsable) R.drawable.ic_chevron_right else null,
                            onClick = { actions.onLibraryItem(item) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackRow(
    title: String,
    subtitle: String,
    art: android.graphics.Bitmap?,
    highlighted: Boolean,
    @DrawableRes trailing: Int?,
    onClick: () -> Unit,
) {
    val image = remember(art) { art?.asImageBitmap() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (highlighted) Color(0x22FFFFFF) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CarColors.SurfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            if (image != null) {
                Image(bitmap = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_music),
                    contentDescription = null,
                    tint = CarColors.TextSecondary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (highlighted) CarColors.Teal else CarColors.TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = CarColors.TextSecondary,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            Icon(
                painter = painterResource(trailing),
                contentDescription = null,
                tint = CarColors.TextSecondary,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun Loading() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) { CircularProgressIndicator(color = CarColors.TextPrimary) }
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text = text,
        color = CarColors.TextSecondary,
        fontSize = 18.sp,
        modifier = Modifier.padding(12.dp),
    )
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
