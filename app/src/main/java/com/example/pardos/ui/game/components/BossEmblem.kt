package com.korkoor.pardos.ui.game.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.korkoor.pardos.domain.level.BossMotif
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Color propio de cada jefe (la tarjeta del jefe lo usa en lugar del rojo de siempre). */
fun BossMotif.tone(): Color = when (this) {
    BossMotif.WALL -> Color(0xFFB4413C)
    BossMotif.STORM -> Color(0xFF4A6FA5)
    BossMotif.CLOCK -> Color(0xFFC77D2E)
    BossMotif.GOLD -> Color(0xFFD9A400)
    BossMotif.STONE -> Color(0xFF6B7285)
    BossMotif.CRYSTAL -> Color(0xFF3FA7C7)
    BossMotif.EYE -> Color(0xFF7B4FB3)
    BossMotif.GEARS -> Color(0xFF3F8F7A)
    BossMotif.MAZE -> Color(0xFF8C5A3C)
    BossMotif.FIRE -> Color(0xFFE0603A)
}

/**
 * Emblema animado del jefe: un dibujo propio por motivo (muro, tormenta, reloj, oro, piedra, cristal, ojo, engranajes, laberinto, fuego)
 * que se mueve solo y despacio, con los colores del juego. Todo es dibujo (sin imágenes ni emojis) y una sola animación por emblema.
 */
@Composable
fun BossEmblem(motif: BossMotif, modifier: Modifier = Modifier, size: Dp = 96.dp) {
    val t = rememberInfiniteTransition(label = "boss-${motif.name}")
    val phase by t.animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart), label = "phase")
    val tone = motif.tone()
    Canvas(modifier.size(size)) {
        drawRoundRect(tone.copy(alpha = 0.14f), cornerRadius = CornerRadius(this.size.width * 0.28f), size = this.size)
        drawRoundRect(tone.copy(alpha = 0.55f), cornerRadius = CornerRadius(this.size.width * 0.28f), size = this.size, style = Stroke(this.size.width * 0.03f))
        when (motif) {
            BossMotif.WALL -> wall(tone, phase)
            BossMotif.STORM -> storm(tone, phase)
            BossMotif.CLOCK -> clock(tone, phase)
            BossMotif.GOLD -> gold(tone, phase)
            BossMotif.STONE -> stone(tone, phase)
            BossMotif.CRYSTAL -> crystal(tone, phase)
            BossMotif.EYE -> eye(tone, phase)
            BossMotif.GEARS -> gears(tone, phase)
            BossMotif.MAZE -> maze(tone, phase)
            BossMotif.FIRE -> fire(tone, phase)
        }
    }
}

private fun wave(phase: Float, k: Float = 1f): Float = sin(phase * 2f * PI.toFloat() * k)

private fun DrawScope.wall(tone: Color, p: Float) {
    val s = size.width
    val rows = 4
    val bh = s * 0.17f
    val bw = s * 0.3f
    val top = s * 0.2f
    for (r in 0 until rows) {
        val off = if (r % 2 == 0) 0f else bw / 2
        var x = s * 0.14f - off
        var col = 0
        while (x < s * 0.86f) {
            val wob = if (r == 1 && col == 1) wave(p, 2f) * s * 0.012f else 0f
            val left = x.coerceAtLeast(s * 0.14f)
            val right = (x + bw - s * 0.02f).coerceAtMost(s * 0.86f)
            if (right > left) drawRoundRect(tone.copy(alpha = 0.85f - r * 0.07f), Offset(left, top + r * (bh + s * 0.02f) + wob), Size(right - left, bh), CornerRadius(s * 0.03f))
            x += bw
            col++
        }
    }
}

private fun DrawScope.storm(tone: Color, p: Float) {
    val s = size.width
    val cloud = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(s * 0.18f, s * 0.3f, s * 0.5f, s * 0.58f))
        addOval(androidx.compose.ui.geometry.Rect(s * 0.34f, s * 0.2f, s * 0.7f, s * 0.58f))
        addOval(androidx.compose.ui.geometry.Rect(s * 0.54f, s * 0.32f, s * 0.84f, s * 0.58f))
    }
    drawPath(cloud, Color(0xFF55627A))
    for (i in 0 until 5) {
        val y = (p * 1.4f + i * 0.2f) % 1f
        val x = s * (0.26f + i * 0.12f)
        drawLine(tone.copy(alpha = 0.9f * (1f - y)), Offset(x, s * 0.62f + y * s * 0.28f), Offset(x - s * 0.02f, s * 0.68f + y * s * 0.28f), strokeWidth = s * 0.025f, cap = StrokeCap.Round)
    }
    val flash = (abs(wave(p, 2f)) - 0.7f).coerceAtLeast(0f) * 3f
    if (flash > 0f) {
        val bolt = Path().apply {
            moveTo(s * 0.52f, s * 0.52f); lineTo(s * 0.44f, s * 0.72f); lineTo(s * 0.52f, s * 0.72f); lineTo(s * 0.46f, s * 0.9f); lineTo(s * 0.62f, s * 0.64f); lineTo(s * 0.53f, s * 0.64f); close()
        }
        drawPath(bolt, Color(0xFFFFE27A).copy(alpha = flash.coerceAtMost(1f)))
    }
}

private fun DrawScope.clock(tone: Color, p: Float) {
    val s = size.width
    val c = Offset(s / 2, s / 2)
    drawCircle(Color.White.copy(alpha = 0.9f), s * 0.34f, c)
    drawCircle(tone, s * 0.34f, c, style = Stroke(s * 0.04f))
    for (i in 0 until 12) {
        val a = i * PI.toFloat() / 6
        drawLine(tone.copy(alpha = 0.7f), Offset(c.x + cos(a) * s * 0.27f, c.y + sin(a) * s * 0.27f), Offset(c.x + cos(a) * s * 0.31f, c.y + sin(a) * s * 0.31f), strokeWidth = s * 0.012f)
    }
    val big = p * 2f * PI.toFloat() - PI.toFloat() / 2
    val small = p * 2f * PI.toFloat() / 12f - PI.toFloat() / 2
    drawLine(tone, c, Offset(c.x + cos(big) * s * 0.26f, c.y + sin(big) * s * 0.26f), strokeWidth = s * 0.022f, cap = StrokeCap.Round)
    drawLine(tone.copy(alpha = 0.8f), c, Offset(c.x + cos(small) * s * 0.17f, c.y + sin(small) * s * 0.17f), strokeWidth = s * 0.03f, cap = StrokeCap.Round)
    drawCircle(tone, s * 0.025f, c)
}

private fun DrawScope.gold(tone: Color, p: Float) {
    val s = size.width
    val c = Offset(s / 2, s / 2)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE9A0), tone), center = Offset(c.x - s * 0.08f, c.y - s * 0.1f), radius = s * 0.4f), s * 0.3f, c)
    drawCircle(tone.copy(alpha = 0.8f), s * 0.3f, c, style = Stroke(s * 0.025f))
    drawCircle(tone.copy(alpha = 0.6f), s * 0.2f, c, style = Stroke(s * 0.015f))
    val sweep = (p * 1.6f) % 1f
    withTransform({ rotate(25f, c) }) {
        drawRect(Color.White.copy(alpha = 0.35f * (1f - abs(sweep - 0.5f) * 2f).coerceAtLeast(0f)), Offset(s * (0.1f + 0.8f * sweep) - s * 0.03f, s * 0.15f), Size(s * 0.06f, s * 0.7f))
    }
    for (i in 0 until 3) {
        val a = (p + i / 3f) % 1f
        val sx = s * (0.2f + 0.6f * ((i * 0.37f) % 1f))
        val sy = s * (0.2f + 0.55f * ((i * 0.61f + 0.2f) % 1f))
        val r = s * 0.035f * (1f - abs(a - 0.5f) * 2f).coerceAtLeast(0f)
        drawLine(Color.White, Offset(sx - r, sy), Offset(sx + r, sy), strokeWidth = s * 0.012f)
        drawLine(Color.White, Offset(sx, sy - r), Offset(sx, sy + r), strokeWidth = s * 0.012f)
    }
}

private fun DrawScope.stone(tone: Color, p: Float) {
    val s = size.width
    val bob = wave(p) * s * 0.015f
    val rock = Path().apply {
        moveTo(s * 0.2f, s * 0.7f + bob); lineTo(s * 0.26f, s * 0.42f + bob); lineTo(s * 0.46f, s * 0.26f + bob)
        lineTo(s * 0.68f, s * 0.32f + bob); lineTo(s * 0.8f, s * 0.58f + bob); lineTo(s * 0.74f, s * 0.74f + bob); close()
    }
    drawPath(rock, Brush.verticalGradient(listOf(Color(0xFFC9CFDB), tone), startY = s * 0.25f, endY = s * 0.75f))
    drawPath(rock, tone.copy(alpha = 0.9f), style = Stroke(s * 0.02f))
    drawLine(Color(0xFF3F4555), Offset(s * 0.5f, s * 0.3f + bob), Offset(s * 0.44f, s * 0.5f + bob), strokeWidth = s * 0.015f)
    drawLine(Color(0xFF3F4555), Offset(s * 0.44f, s * 0.5f + bob), Offset(s * 0.52f, s * 0.64f + bob), strokeWidth = s * 0.015f)
    val dust = (p * 2f) % 1f
    drawCircle(Color.White.copy(alpha = 0.5f * (1f - dust)), s * 0.015f + dust * s * 0.02f, Offset(s * 0.28f - dust * s * 0.05f, s * 0.76f - dust * s * 0.04f))
    drawCircle(Color.White.copy(alpha = 0.5f * (1f - dust)), s * 0.015f + dust * s * 0.02f, Offset(s * 0.72f + dust * s * 0.05f, s * 0.78f - dust * s * 0.04f))
}

private fun DrawScope.crystal(tone: Color, p: Float) {
    val s = size.width
    val c = Offset(s / 2, s / 2)
    val pts = listOf(Offset(c.x, s * 0.16f), Offset(s * 0.74f, c.y - s * 0.02f), Offset(c.x, s * 0.86f), Offset(s * 0.26f, c.y - s * 0.02f))
    val body = Path().apply { moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) }; close() }
    drawPath(body, Brush.linearGradient(listOf(Color(0xFFCFF3FF), tone), Offset(s * 0.3f, s * 0.2f), Offset(s * 0.7f, s * 0.85f)))
    drawPath(body, Color.White.copy(alpha = 0.7f), style = Stroke(s * 0.015f))
    drawLine(Color.White.copy(alpha = 0.6f), pts[3], pts[1], strokeWidth = s * 0.012f)
    drawLine(Color.White.copy(alpha = 0.5f), pts[0], Offset(c.x, s * 0.86f), strokeWidth = s * 0.012f)
    val shine = (p * 1.3f) % 1f
    withTransform({ rotate(20f, c) }) {
        drawRect(Color.White.copy(alpha = 0.5f * (1f - abs(shine - 0.5f) * 2f).coerceAtLeast(0f)), Offset(s * (0.2f + 0.6f * shine), s * 0.12f), Size(s * 0.05f, s * 0.76f))
    }
}

private fun DrawScope.eye(tone: Color, p: Float) {
    val s = size.width
    val c = Offset(s / 2, s / 2)
    val blink = (abs(wave(p, 1f)) < 0.04f)
    val open = if (blink) 0.04f else 1f
    val lid = Path().apply {
        moveTo(s * 0.14f, c.y)
        quadraticBezierTo(c.x, c.y - s * 0.32f * open, s * 0.86f, c.y)
        quadraticBezierTo(c.x, c.y + s * 0.32f * open, s * 0.14f, c.y)
        close()
    }
    drawPath(lid, Color.White)
    drawPath(lid, tone, style = Stroke(s * 0.03f))
    if (!blink) {
        val look = wave(p, 1f) * s * 0.1f
        drawCircle(tone, s * 0.13f, Offset(c.x + look, c.y))
        drawCircle(Color(0xFF1F1A2E), s * 0.065f, Offset(c.x + look, c.y))
        drawCircle(Color.White.copy(alpha = 0.9f), s * 0.025f, Offset(c.x + look - s * 0.03f, c.y - s * 0.035f))
    }
}

private fun DrawScope.gear(center: Offset, r: Float, teeth: Int, angle: Float, color: Color) {
    withTransform({ rotate(angle, center) }) {
        val path = Path()
        val inner = r * 0.78f
        for (i in 0 until teeth) {
            val a0 = i * 2f * PI.toFloat() / teeth
            val a1 = a0 + PI.toFloat() / teeth * 0.5f
            val a2 = a0 + PI.toFloat() / teeth
            val a3 = a0 + PI.toFloat() / teeth * 1.5f
            fun pt(a: Float, rr: Float) = Offset(center.x + cos(a) * rr, center.y + sin(a) * rr)
            if (i == 0) path.moveTo(pt(a0, inner).x, pt(a0, inner).y) else path.lineTo(pt(a0, inner).x, pt(a0, inner).y)
            path.lineTo(pt(a0 + 0.02f, r).x, pt(a0 + 0.02f, r).y)
            path.lineTo(pt(a1, r).x, pt(a1, r).y)
            path.lineTo(pt(a2 - 0.02f, inner).x, pt(a2 - 0.02f, inner).y)
            path.lineTo(pt(a3, inner).x, pt(a3, inner).y)
        }
        path.close()
        drawPath(path, color)
        drawCircle(Color.White.copy(alpha = 0.85f), r * 0.32f, center)
    }
}

private fun DrawScope.gears(tone: Color, p: Float) {
    val s = size.width
    val a = p * 360f
    gear(Offset(s * 0.38f, s * 0.46f), s * 0.24f, 9, a, tone)
    gear(Offset(s * 0.68f, s * 0.64f), s * 0.17f, 7, -a * 9f / 7f + 12f, tone.copy(alpha = 0.7f))
}

private fun DrawScope.maze(tone: Color, p: Float) {
    val s = size.width
    val c = Offset(s / 2, s / 2)
    val path = Path().apply {
        moveTo(s * 0.2f, s * 0.2f); lineTo(s * 0.8f, s * 0.2f); lineTo(s * 0.8f, s * 0.8f); lineTo(s * 0.3f, s * 0.8f)
        lineTo(s * 0.3f, s * 0.35f); lineTo(s * 0.65f, s * 0.35f); lineTo(s * 0.65f, s * 0.65f); lineTo(s * 0.46f, s * 0.65f)
        lineTo(s * 0.46f, s * 0.5f)
    }
    drawPath(path, tone.copy(alpha = 0.85f), style = Stroke(s * 0.04f, cap = StrokeCap.Round))
    val m = android.graphics.PathMeasure(path.asAndroidPath(), false)
    val pos = FloatArray(2)
    m.getPosTan(m.length * p, pos, null)
    drawCircle(Color.White, s * 0.04f, Offset(pos[0], pos[1]))
    drawCircle(tone, s * 0.025f, Offset(pos[0], pos[1]))
}

private fun DrawScope.fire(tone: Color, p: Float) {
    val s = size.width
    fun flame(cx: Float, w: Float, h: Float, ph: Float, color: Color) {
        val flick = wave(p + ph, 2f) * s * 0.02f
        val path = Path().apply {
            moveTo(cx - w, s * 0.8f)
            cubicTo(cx - w * 1.1f, s * 0.8f - h * 0.5f, cx - w * 0.2f + flick, s * 0.8f - h * 0.7f, cx + flick, s * 0.8f - h)
            cubicTo(cx + w * 0.2f + flick, s * 0.8f - h * 0.7f, cx + w * 1.1f, s * 0.8f - h * 0.5f, cx + w, s * 0.8f)
            close()
        }
        drawPath(path, color)
    }
    flame(s * 0.5f, s * 0.24f, s * 0.62f, 0f, tone)
    flame(s * 0.34f, s * 0.12f, s * 0.38f, 0.3f, tone.copy(alpha = 0.7f))
    flame(s * 0.66f, s * 0.12f, s * 0.34f, 0.6f, tone.copy(alpha = 0.7f))
    flame(s * 0.5f, s * 0.12f, s * 0.34f, 0.15f, Color(0xFFFFD27A))
    for (i in 0 until 3) {
        val a = (p * 1.5f + i / 3f) % 1f
        drawCircle(Color(0xFFFFD27A).copy(alpha = 1f - a), s * 0.012f, Offset(s * (0.4f + 0.1f * i) + wave(a, 1f) * s * 0.02f, s * 0.5f - a * s * 0.3f))
    }
}
