package com.korkoor.pardos.ui.design

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * Cuerda de luces de colores que cuelga a lo ancho de la pantalla (solo en Noche de brujas). Las bombillas titilan una a una.
 * No intercepta toques; se pone encima del contenido en la parte alta.
 */
@Composable
fun HalloweenGarland(modifier: Modifier = Modifier, height: Dp = 46.dp, bulbs: Int = 12) {
    if (!Season.halloween) return
    val t = rememberThrottledPhase(5000)
    val colors = listOf(Color(0xFFFFB347), Color(0xFFB27BFF), Color(0xFFFF7A59), Color(0xFFFFE08A), Color(0xFF7DE0A6))
    Canvas(modifier.fillMaxWidth().height(height)) {
        val w = size.width
        val sag = size.height * 0.5f
        val p0 = Offset(-8f, 2f)
        val p1 = Offset(w + 8f, 2f)
        val ctrl = Offset(w / 2f, 2f + sag * 2f)
        val rope = Path().apply { moveTo(p0.x, p0.y); quadraticBezierTo(ctrl.x, ctrl.y, p1.x, p1.y) }
        drawPath(rope, Color(0xFF1A0E2E).copy(alpha = 0.8f), style = Stroke(width = 3f, cap = StrokeCap.Round))
        for (i in 0 until bulbs) {
            val u = (i + 0.5f) / bulbs
            val a = (1 - u) * (1 - u); val b = 2 * (1 - u) * u; val c = u * u
            val x = a * p0.x + b * ctrl.x + c * p1.x
            val y = a * p0.y + b * ctrl.y + c * p1.y
            val tw = 0.55f + 0.45f * sin((t.value * 6.2832f + i * 1.7f).toDouble()).toFloat()
            val col = colors[i % colors.size]
            drawCircle(Brush.radialGradient(listOf(col.copy(alpha = 0.5f * tw), Color.Transparent), Offset(x, y + 10f), 26f), 26f, Offset(x, y + 10f))
            drawLine(Color(0xFF1A0E2E), Offset(x, y), Offset(x, y + 5f), strokeWidth = 3f)
            drawCircle(col.copy(alpha = 0.55f + 0.45f * tw), 5.5f, Offset(x, y + 9f))
        }
    }
}

/** Sprite 3D que flota y se balancea: para botones y tarjetas de temporada. */
@Composable
fun FloatingSprite(res: Int, size: Dp, tilt: Float, phase: Int, modifier: Modifier = Modifier) {
    val bob by rememberInfiniteTransition(label = "sprite$res").animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1700 + phase * 260, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob"
    )
    Image(
        painterResource(res), contentDescription = null,
        modifier = modifier.size(size).graphicsLayer { translationY = bob.dp.toPx(); rotationZ = tilt + bob * 1.2f }
    )
}

/** Medallón con un icono 3D de temporada, en lugar del icono plano de siempre (mismo tamaño que `IconTile`). */
@Composable
fun SeasonTile(res: Int, color: Color, size: Dp = 38.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.26f), color.copy(alpha = 0.10f))), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        FloatingSprite(res, size * 0.82f, 0f, res % 5)
    }
}
