package com.cardroid.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardroid.launcher.R

/** Instructions to create the iPhone Shortcut that sends destinations here. */
@Composable
fun LinkPhonePanel(shortcutUrl: String, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 900.dp)
                .padding(24.dp)
                .clip(CardShape)
                .background(CarColors.Surface)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .verticalScroll(rememberScrollState())
                .padding(28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Enlazar tu iPhone",
                    color = CarColors.TextPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                CircleButton(R.drawable.ic_close, "Cerrar", onClose)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Crea este atajo una sola vez. Después, desde Mapas, Google Maps o cualquier " +
                    "texto con una dirección, toca Compartir > Enviar a Cardroid, o di " +
                    "\"Oye Siri, Enviar a Cardroid\".",
                color = CarColors.TextSecondary,
                fontSize = 18.sp,
            )
            Spacer(Modifier.height(20.dp))

            Step(1, "En el iPhone abre la app Atajos, toca + y nombra el atajo \"Enviar a Cardroid\".")
            Step(2, "Toca el ícono ⓘ y activa \"Mostrar en hoja para compartir\". Donde dice " +
                "\"Si no hay entrada\", elige \"Solicitar texto\".")
            Step(3, "Agrega la acción \"Texto\" y dentro pon la variable \"Entrada del atajo\".")
            Step(4, "Agrega la acción \"Obtener contenido de URL\" con esta dirección:")
            SelectionContainer {
                Text(
                    text = shortcutUrl,
                    color = CarColors.Accent,
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 44.dp, top = 4.dp, bottom = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CarColors.SurfaceHigh)
                        .padding(16.dp),
                )
            }
            Step(5, "En esa acción toca \"Mostrar más\": Método POST, Cuerpo de la solicitud " +
                "\"Archivo\" y elige la variable \"Texto\".")
            Spacer(Modifier.height(8.dp))
            Text(
                text = "La dirección es secreta y solo la conocen tu iPhone y esta tablet. No la compartas.",
                color = CarColors.TextSecondary,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
private fun Step(number: Int, text: String) {
    Row(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = "$number.",
            color = CarColors.Accent,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(44.dp),
        )
        Text(text = text, color = CarColors.TextPrimary, fontSize = 20.sp)
    }
}
