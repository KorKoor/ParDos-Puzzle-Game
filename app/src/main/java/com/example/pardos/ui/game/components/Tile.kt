package com.korkoor.pardos.ui.game.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.domain.model.TileModel
import kotlin.math.log2

@Composable
fun Tile(
    tile: TileModel,
    base: Int,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    // 1. Efecto Pop-in: Escala de 0f a 1f usando Spring para rebote sutil
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy, // Rebote sutil
            stiffness = Spring.StiffnessLow               // Velocidad suave
        ),
        label = "TileSpawnAnimation"
    )

    val backgroundColor = getAestheticColor(tile.value, base)
    val textColor = Color(0xFF3D405B)

    // Efecto de brillo (Glow)
    val glowModifier = if (tile.value >= base * 8) {
        Modifier.shadow(
            elevation = 10.dp,
            shape = shape,
            clip = false,
            ambientColor = backgroundColor.copy(alpha = 0.5f),
            spotColor = backgroundColor
        )
    } else Modifier

    // Aplicamos la escala al Box principal
    Box(
        modifier = modifier
            .padding(4.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale) // 👈 La magia ocurre aquí
            .then(glowModifier)
            .clip(shape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = tile.value.toString(),
            fontSize = when {
                tile.value < 10 -> 28.sp
                tile.value < 100 -> 24.sp
                tile.value < 1000 -> 18.sp
                else -> 14.sp
            },
            fontWeight = FontWeight.Black,
            color = textColor
        )
    }
}
/**
 * Función de paleta Aesthetic para cualquier múltiplo
 */
private fun getAestheticColor(value: Int, base: Int): Color {
    val palette = listOf(
        Color(0xFFF4F1DE), // Ficha inicial (ej. 7)
        Color(0xFFE9EDC9), // x2 (ej. 14)
        Color(0xFFFEFAE0), // x4 (ej. 28)
        Color(0xFFFAE1DD), // x8
        Color(0xFFFCD5CE), // x16
        Color(0xFFFFE5D9), // x32
        Color(0xFFD8E2DC), // x64
        Color(0xFFECE4DB), // x128
        Color(0xFFFFE1A8), // x256
        Color(0xFFE07A5F)  // META (x512+)
    )

    // Calculamos qué potencia es: Log2(valor / base)
    val power = log2(value.toFloat() / base).toInt().coerceIn(0, palette.lastIndex)
    return palette[power]
}