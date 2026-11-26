package com.cardroid.launcher.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardroid.launcher.R
import com.cardroid.launcher.map.CarMapState

@Composable
fun MapOverlay(
    mapState: CarMapState,
    speedKmh: Int,
    hasLocationPermission: Boolean,
    onRequestLocation: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (!hasLocationPermission) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CarColors.MapButton)
                    .clickable(onClick = onRequestLocation)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Text(
                    text = "Permitir ubicación para ver tu posición",
                    color = CarColors.Accent,
                    fontSize = 18.sp,
                )
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomEnd),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End,
        ) {
            MapButton(R.drawable.ic_add, "Acercar") { mapState.zoomIn() }
            MapButton(R.drawable.ic_remove, "Alejar") { mapState.zoomOut() }
            MapButton(R.drawable.ic_my_location, "Centrar", highlighted = true) { mapState.recenter() }
        }

        // Speed bubble, like a car's own map display.
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(92.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(3.dp, Color(0x22000000), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = speedKmh.toString(),
                    color = Color(0xFF202124),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 34.sp,
                )
                Text(text = "km/h", color = Color(0xFF5F6368), fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun MapButton(
    @DrawableRes icon: Int,
    description: String,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(if (highlighted) Color(0xFF0B57D0) else CarColors.MapButton)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(32.dp),
        )
    }
}
