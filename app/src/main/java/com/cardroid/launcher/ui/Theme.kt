package com.cardroid.launcher.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object CarColors {
    val Background = Color(0xFF000000)
    val Surface = Color(0xFF1E1F22)
    val SurfaceHigh = Color(0xFF2D2F33)
    val Accent = Color(0xFFA8C7FA)
    val Teal = Color(0xFF7FD8CF)
    val Warning = Color(0xFFF28B82)
    val TextPrimary = Color(0xFFF1F3F4)
    val TextSecondary = Color(0xFFA9ADB3)
    val MapButton = Color(0xE6202124)
}

val CardShape = RoundedCornerShape(24.dp)

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
