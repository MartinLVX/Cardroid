package com.cardroid.launcher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object CarColors {
    val Background = Color(0xFF0B0F17)
    val Surface = Color(0xFF161C28)
    val SurfaceHigh = Color(0xFF202838)
    val Accent = Color(0xFF8AB4F8)
    val TextPrimary = Color(0xFFF1F3F7)
    val TextSecondary = Color(0xFF9AA4B5)
}

@Composable
fun CardroidTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = CarColors.Accent,
            background = CarColors.Background,
            surface = CarColors.Surface,
            onBackground = CarColors.TextPrimary,
            onSurface = CarColors.TextPrimary,
        ),
        content = content,
    )
}
