package com.cardroid.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val MAX_SPEED = 200f
private const val START_ANGLE = 135f
private const val SWEEP = 270f

@Composable
fun SidePanel(
    speedKmh: Int,
    tripMeters: Double,
    bearing: Float?,
    onResetTrip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(CardShape)
                .background(CarColors.Surface)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Speedometer(speedKmh = speedKmh, modifier = Modifier.fillMaxHeight().aspectRatio(1f))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InfoCard(
                label = "Viaje",
                value = formatDistance(tripMeters),
                hint = "Toca para reiniciar",
                onClick = onResetTrip,
                modifier = Modifier.weight(1f),
            )
            InfoCard(
                label = "Rumbo",
                value = bearing?.let { compassPoint(it) } ?: "--",
                hint = null,
                onClick = null,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Speedometer(speedKmh: Int, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = CarColors.TextSecondary, fontSize = 13.sp)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val diameter = min(size.width, size.height) - stroke
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = CarColors.SurfaceHigh,
                startAngle = START_ANGLE,
                sweepAngle = SWEEP,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            val fraction = (speedKmh / MAX_SPEED).coerceIn(0f, 1f)
            if (fraction > 0f) {
                drawArc(
                    color = if (speedKmh > 120) CarColors.Warning else CarColors.Teal,
                    startAngle = START_ANGLE,
                    sweepAngle = SWEEP * fraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }

            // Tick marks and labels every 20 km/h.
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = diameter / 2f
            for (i in 0..10) {
                val angle = Math.toRadians((START_ANGLE + SWEEP * i / 10f).toDouble())
                val dx = cos(angle).toFloat()
                val dy = sin(angle).toFloat()
                val outer = radius - stroke
                val inner = outer - 10.dp.toPx()
                drawLine(
                    color = CarColors.TextSecondary,
                    start = Offset(center.x + dx * inner, center.y + dy * inner),
                    end = Offset(center.x + dx * outer, center.y + dy * outer),
                    strokeWidth = 2.dp.toPx(),
                )
                val label = measurer.measure((i * 20).toString(), labelStyle)
                val labelRadius = inner - 16.dp.toPx()
                drawText(
                    textLayoutResult = label,
                    topLeft = Offset(
                        center.x + dx * labelRadius - label.size.width / 2f,
                        center.y + dy * labelRadius - label.size.height / 2f,
                    ),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = speedKmh.toString(),
                color = CarColors.TextPrimary,
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 72.sp,
            )
            Text(text = "km/h", color = CarColors.TextSecondary, fontSize = 20.sp)
        }
    }
}

@Composable
private fun InfoCard(
    label: String,
    value: String,
    hint: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(CardShape)
            .background(CarColors.Surface)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = label, color = CarColors.TextSecondary, fontSize = 18.sp)
        Text(
            text = value,
            color = CarColors.TextPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (hint != null) {
            Text(text = hint, color = CarColors.TextSecondary, fontSize = 12.sp)
        }
    }
}

private fun formatDistance(meters: Double): String =
    if (meters < 1_000) "${meters.toInt()} m"
    else String.format(Locale.getDefault(), "%.1f km", meters / 1_000)

private fun compassPoint(bearing: Float): String {
    val points = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
    val index = (((bearing % 360) + 360) % 360 / 45f + 0.5f).toInt() % 8
    return points[index]
}
