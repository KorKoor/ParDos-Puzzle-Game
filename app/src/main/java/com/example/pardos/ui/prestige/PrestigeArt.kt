package com.korkoor.pardos.ui.prestige

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.korkoor.pardos.domain.prestige.PrestigeRank
import com.korkoor.pardos.domain.prestige.TrophyTier
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Colores (claro, medio, oscuro) de cada rango: de la piedra gris al arcoíris del Mítico. */
fun PrestigeRank.palette(): Triple<Color, Color, Color> = when (this) {
    PrestigeRank.NOVICE -> Triple(Color(0xFFD9DDE6), Color(0xFF9EA6B6), Color(0xFF5E6678))
    PrestigeRank.APPRENTICE -> Triple(Color(0xFFF1C9A0), Color(0xFFCD8A4B), Color(0xFF8A4F22))
    PrestigeRank.ADEPT -> Triple(Color(0xFFE6EEFA), Color(0xFF9DB8DC), Color(0xFF557AA8))
    PrestigeRank.EXPERT -> Triple(Color(0xFFB8F0D3), Color(0xFF4FC08D), Color(0xFF1F7A55))
    PrestigeRank.MASTER -> Triple(Color(0xFFFFEFA8), Color(0xFFF2B83B), Color(0xFFB27A12))
    PrestigeRank.GRANDMASTER -> Triple(Color(0xFFE2C8FF), Color(0xFF9B6BE0), Color(0xFF5B34A6))
    PrestigeRank.LEGEND -> Triple(Color(0xFFFFC2B8), Color(0xFFE5533D), Color(0xFF8E1F18))
    PrestigeRank.MYTHIC -> Triple(Color(0xFFFFFFFF), Color(0xFF8FD3FF), Color(0xFF6A4CE0))
}

private fun hexagon(cx: Float, cy: Float, r: Float): Path = Path().apply {
    for (i in 0 until 6) {
        val a = (PI / 3 * i - PI / 2).toFloat()
        val x = cx + r * cos(a)
        val y = cy + r * sin(a)
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private fun star(cx: Float, cy: Float, outer: Float, inner: Float, points: Int = 5): Path = Path().apply {
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outer else inner
        val a = (PI / points * i - PI / 2).toFloat()
        val x = cx + r * cos(a)
        val y = cy + r * sin(a)
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** Insignia del rango: un hexágono con relieve, una estrella grande y tantas estrellitas como peldaños ha subido. */
@Composable
fun RankBadge(rank: PrestigeRank, size: Dp, modifier: Modifier = Modifier, animate: Boolean = true) {
    val infinite = rememberInfiniteTransition(label = "rankBadge")
    val shine by infinite.animateFloat(-0.4f, 1.4f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "shine")
    val hue by infinite.animateFloat(0f, 360f, infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "hue")
    val glow by infinite.animateFloat(0.6f, 1f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    val (light0, mid0, dark0) = rank.palette()
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val cx = w / 2
        val cy = w / 2
        val r = w * 0.44f
        // El Mítico cambia de color sin parar
        val mythic = rank == PrestigeRank.MYTHIC && animate
        val light = if (mythic) Color.hsv(hue, 0.35f, 1f) else light0
        val mid = if (mythic) Color.hsv((hue + 40f) % 360f, 0.7f, 1f) else mid0
        val dark = if (mythic) Color.hsv((hue + 120f) % 360f, 0.75f, 0.75f) else dark0

        // halo
        if (rank.ordinal >= PrestigeRank.MASTER.ordinal && animate) {
            drawCircle(Brush.radialGradient(listOf(mid.copy(alpha = 0.55f * glow), Color.Transparent), center = Offset(cx, cy), radius = w * 0.62f), radius = w * 0.62f, center = Offset(cx, cy))
        }
        // sombra apoyada
        drawPath(hexagon(cx, cy + w * 0.03f, r), Color.Black.copy(alpha = 0.22f))
        // borde exterior y cuerpo
        drawPath(hexagon(cx, cy, r), Brush.verticalGradient(listOf(light, mid, dark), startY = cy - r, endY = cy + r))
        drawPath(hexagon(cx, cy, r * 0.86f), Brush.verticalGradient(listOf(dark.copy(alpha = 0.9f), mid.copy(alpha = 0.85f)), startY = cy - r, endY = cy + r))
        drawPath(hexagon(cx, cy, r * 0.86f), light.copy(alpha = 0.55f), style = Stroke(width = w * 0.012f))
        // estrella central
        val s = star(cx, cy + w * 0.01f, r * 0.52f, r * 0.23f)
        drawPath(s, Color.Black.copy(alpha = 0.25f), alpha = 1f)
        drawPath(star(cx, cy - w * 0.005f, r * 0.52f, r * 0.23f), Brush.verticalGradient(listOf(Color.White, light, mid), startY = cy - r * 0.5f, endY = cy + r * 0.5f))
        // peldaños: estrellitas sobre el borde superior (hasta 7)
        val n = rank.ordinal
        if (n > 0) {
            val gap = w * 0.085f
            val startX = cx - gap * (n - 1) / 2f
            for (i in 0 until n) {
                drawPath(star(startX + gap * i, cy - r * 0.97f, w * 0.036f, w * 0.016f), Color.White.copy(alpha = 0.95f))
            }
        }
        // destello que cruza
        if (animate) {
            clipRect(cx - r, cy - r, cx + r, cy + r) {
                val x = cx - r + 2 * r * shine
                drawRect(
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.38f), Color.Transparent), startX = x - w * 0.12f, endX = x + w * 0.12f),
                    topLeft = Offset(cx - r, cy - r), size = Size(2 * r, 2 * r)
                )
            }
        }
    }
}

/** Medalla de un trofeo: bronce, plata, oro o diamante. */
@Composable
fun TrophyMedal(tier: TrophyTier, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val c = Offset(w / 2, w * 0.52f)
        val r = w * 0.36f
        val (light, mid, dark) = when (tier) {
            TrophyTier.BRONZE -> Triple(Color(0xFFF4C79B), Color(0xFFCD8A4B), Color(0xFF8A4F22))
            TrophyTier.SILVER -> Triple(Color(0xFFFFFFFF), Color(0xFFC3CAD8), Color(0xFF7C869B))
            TrophyTier.GOLD -> Triple(Color(0xFFFFF3B0), Color(0xFFF2B83B), Color(0xFFB27A12))
            TrophyTier.DIAMOND -> Triple(Color(0xFFFFFFFF), Color(0xFF8FD3FF), Color(0xFF4A7FE0))
        }
        // cintas
        val ribbonL = Path().apply { moveTo(w * 0.34f, w * 0.05f); lineTo(w * 0.5f, w * 0.05f); lineTo(w * 0.52f, w * 0.4f); lineTo(w * 0.4f, w * 0.34f); close() }
        val ribbonR = Path().apply { moveTo(w * 0.66f, w * 0.05f); lineTo(w * 0.5f, w * 0.05f); lineTo(w * 0.48f, w * 0.4f); lineTo(w * 0.6f, w * 0.34f); close() }
        drawPath(ribbonL, Color(0xFFE5533D)); drawPath(ribbonR, Color(0xFFB4413C))
        if (tier == TrophyTier.DIAMOND) {
            val d = Path().apply {
                moveTo(c.x, c.y - r * 1.05f); lineTo(c.x + r, c.y - r * 0.25f); lineTo(c.x, c.y + r * 1.05f); lineTo(c.x - r, c.y - r * 0.25f); close()
            }
            drawPath(d, Brush.verticalGradient(listOf(light, mid, dark), startY = c.y - r, endY = c.y + r))
            drawPath(d, Color.White.copy(alpha = 0.7f), style = Stroke(w * 0.015f))
            drawLine(Color.White.copy(alpha = 0.6f), Offset(c.x - r, c.y - r * 0.25f), Offset(c.x + r, c.y - r * 0.25f), strokeWidth = w * 0.012f)
        } else {
            drawCircle(Color.Black.copy(alpha = 0.2f), r * 1.02f, Offset(c.x, c.y + w * 0.02f))
            drawCircle(Brush.verticalGradient(listOf(light, mid, dark), startY = c.y - r, endY = c.y + r), r, c)
            drawCircle(light.copy(alpha = 0.8f), r * 0.78f, c, style = Stroke(w * 0.014f))
            drawPath(star(c.x, c.y, r * 0.5f, r * 0.22f), Color.White.copy(alpha = 0.85f))
        }
    }
}

/** Copa del Platino: se llena de abajo arriba con el progreso; al completarla brilla y lanza destellos. */
@Composable
fun PlatinumTrophy(progress: Float, size: Dp, modifier: Modifier = Modifier, earned: Boolean = false) {
    val infinite = rememberInfiniteTransition(label = "platinum")
    val sparkle by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "sp")
    val pulse by infinite.animateFloat(0.7f, 1f, infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pu")
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cup = Path().apply {
            // copa
            moveTo(w * 0.24f, h * 0.10f); lineTo(w * 0.76f, h * 0.10f)
            cubicTo(w * 0.78f, h * 0.44f, w * 0.64f, h * 0.60f, w * 0.52f, h * 0.62f)
            lineTo(w * 0.52f, h * 0.74f)
            lineTo(w * 0.66f, h * 0.76f); lineTo(w * 0.72f, h * 0.90f); lineTo(w * 0.28f, h * 0.90f); lineTo(w * 0.34f, h * 0.76f)
            lineTo(w * 0.48f, h * 0.74f); lineTo(w * 0.48f, h * 0.62f)
            cubicTo(w * 0.36f, h * 0.60f, w * 0.22f, h * 0.44f, w * 0.24f, h * 0.10f)
            close()
        }
        val handles = Path().apply {
            moveTo(w * 0.25f, h * 0.17f); cubicTo(w * 0.04f, h * 0.17f, w * 0.06f, h * 0.45f, w * 0.30f, h * 0.45f)
            moveTo(w * 0.75f, h * 0.17f); cubicTo(w * 0.96f, h * 0.17f, w * 0.94f, h * 0.45f, w * 0.70f, h * 0.45f)
        }
        if (earned) {
            drawCircle(Brush.radialGradient(listOf(Color(0xFFBFD9FF).copy(alpha = 0.7f * pulse), Color.Transparent), center = Offset(w / 2, h * 0.45f), radius = w * 0.62f), radius = w * 0.62f, center = Offset(w / 2, h * 0.45f))
        }
        // silueta apagada
        drawPath(cup, Color(0xFFB8BECF).copy(alpha = 0.35f))
        drawPath(handles, Color(0xFFB8BECF).copy(alpha = 0.45f), style = Stroke(w * 0.045f, cap = StrokeCap.Round))
        // relleno por progreso (de abajo arriba)
        val p = progress.coerceIn(0f, 1f)
        if (p > 0f) {
            clipRect(0f, h * (1f - p), w, h) {
                val brush = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFD7DEF2), Color(0xFF8E9AC4), Color(0xFFE6EBFA)), startY = 0f, endY = h)
                drawPath(cup, brush)
                drawPath(handles, brush, style = Stroke(w * 0.045f, cap = StrokeCap.Round))
            }
        }
        drawPath(cup, Color(0xFF6C7599).copy(alpha = 0.55f), style = Stroke(w * 0.012f))
        if (earned) {
            for (i in 0 until 4) {
                val t = (sparkle + i * 0.25f) % 1f
                val sx = w * (0.18f + 0.64f * ((i * 37) % 10) / 10f)
                val sy = h * (0.08f + 0.5f * ((i * 53) % 10) / 10f)
                drawPath(star(sx, sy, w * 0.05f * (1f - t * 0.4f), w * 0.015f, 4), Color.White.copy(alpha = (1f - t) * 0.95f))
            }
        }
    }
}

/** Pequeña pastilla redonda con el color del rango (para filas del ranking). */
@Composable
fun RankDot(rank: PrestigeRank, size: Dp = 14.dp, modifier: Modifier = Modifier) {
    val (light, mid, dark) = rank.palette()
    Box(modifier.size(size)) {
        Canvas(Modifier.size(size)) {
            drawPath(hexagon(this.size.width / 2, this.size.width / 2, this.size.width * 0.5f), Brush.verticalGradient(listOf(light, mid, dark)))
        }
    }
}
