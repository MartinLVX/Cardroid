package com.cardroid.launcher.ui

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardroid.launcher.R
import com.cardroid.launcher.apps.AppLauncher
import com.cardroid.launcher.apps.Shortcuts
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun Dock(refreshKey: Int, onLinkPhone: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(
        modifier = modifier.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DockIcon(R.drawable.ic_mic, "Asistente de voz") { AppLauncher.openAssistant(context) }
        Spacer(Modifier.width(8.dp))
        DockIcon(R.drawable.ic_phone_link, "Enlazar iPhone", onLinkPhone)

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (shortcut in Shortcuts.dock) {
                val icon = rememberAppIcon(shortcut.packageName, 60, refreshKey)
                if (icon == null) continue
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { AppLauncher.openShortcut(context, shortcut) },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(bitmap = icon, contentDescription = shortcut.label, modifier = Modifier.size(56.dp))
                }
            }
            DockIcon(R.drawable.ic_tune, "Ajustes") { AppLauncher.openSettings(context) }
        }

        ClockAndBattery()
    }
}

@Composable
private fun DockIcon(@DrawableRes icon: Int, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
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

@Composable
private fun ClockAndBattery() {
    val context = LocalContext.current
    var time by remember { mutableStateOf(LocalTime.now()) }
    var battery by remember { mutableIntStateOf(-1) }
    var charging by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            time = LocalTime.now()
            val status = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (status != null) {
                val level = status.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = status.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                battery = if (level >= 0 && scale > 0) level * 100 / scale else -1
                charging = status.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
            }
            delay(5_000)
        }
    }
    Column(
        modifier = Modifier.fillMaxHeight().padding(end = 8.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = time.format(DateTimeFormatter.ofPattern("HH:mm")),
            color = CarColors.TextPrimary,
            fontSize = 30.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 30.sp,
        )
        if (battery >= 0) {
            Text(
                text = (if (charging) "⚡ " else "") + "$battery%",
                color = CarColors.TextSecondary,
                fontSize = 15.sp,
            )
        }
    }
}
