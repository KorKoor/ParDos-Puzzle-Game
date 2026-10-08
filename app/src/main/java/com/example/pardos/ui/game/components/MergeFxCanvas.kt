package com.korkoor.pardos.ui.game.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.korkoor.pardos.domain.shop.MergeFx
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/** Una partícula de un efecto: se calcula una vez por fusión y luego solo se dibuja según el progreso. */
private class Spark(
    val angle: Float,       // dirección en radianes
    val dist: Float,        // alcance, en tamaños de ficha
    val size: Float,        // tamaño relativo
    val spin: Float,        // giro en grados
    val colorIdx: Int,
    val delay: Float        // 0..0.25 de retraso
)

private fun makeSparks(seed: Int, count: Int): List<Spark> {
    val r = Random(seed)
    return List(count) { i ->
        Spark(
            angle = (i.toFloat() / count) * 2f * PI.toFloat() + r.nextFloat() * 0.6f,
            dist = 0.55f + r.nextFloat() * 0.75f,
            size = 0.6f + r.nextFloat() * 0.7f,
            spin = (r.nextFloat() - 0.5f) * 540f,
            colorIdx = r.nextInt(6),
            delay = r.nextFloat() * 0.22f
        )
    }
}

/** Duración total de cada efecto en milisegundos. */
fun fxDurationMs(fx: MergeFx): Int = when (fx) {
    MergeFx.CLASSIC -> 0
    MergeFx.RIPPLE, MergeFx.LIGHTNING -> 520
    MergeFx.FIREWORKS -> 900
    MergeFx.BUBBLES, MergeFx.HEARTS -> 850
    else -> 700
}

private val confettiColors = listOf(
    Color(0xFFE07A5F), Color(0xFFF2CC8F), Color(0xFF81B29A), Color(0xFF5BC0EB), Color(0xFFB388EB), Color(0xFFFF8FAB)
)
private val fireworkColors = listOf(
    Color(0xFFFFD166), Color(0xFFFF6B8A), Color(0xFF6EE7B7), Color(0xFF7DD3FC), Color(0xFFC4B5FD), Color(0xFFFFFFFF)
)
private val petalColors = listOf(Color(0xFFFFB7C5), Color(0xFFFF9EB5), Color(0xFFFFD1DC), Color(0xFFFFC2D1), Color(0xFFFFA6C1), Color(0xFFFFE0E9))

private fun easeOut(p: Float): Float { val q = 1f - p; return 1f - q * q * q }

/**
 * Estallido que acompaña a una fusión. Se dibuja encima de la ficha y puede salirse de sus límites.
 * [trigger] cambia (se incrementa) cada vez que hay una fusión; con 0 no se dibuja nada.
 */
@Composable
fun MergeBurst(fx: MergeFx, trigger: Int, value: Int, tileSize: Dp, modifier: Modifier = Modifier) {
    if (fx == MergeFx.CLASSIC) return
    val progress = remember { Animatable(1f) }
    val sparks = remember(trigger) { makeSparks(trigger * 31 + value, MergeFx.particleCount(value)) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(fxDurationMs(fx), easing = LinearEasing))
        }
    }
    val p = progress.value
    if (p >= 1f) return
    Canvas(modifier.then(Modifier.size(tileSize))) {
        drawMergeFx(fx, p, sparks, size.minDimension, value)
    }
}

/** Para la vista previa en la tienda: repite el efecto sin parar. */
@Composable
fun MergeFxPreview(fx: MergeFx, modifier: Modifier = Modifier, tileSize: Dp = 48.dp) {
    if (fx == MergeFx.CLASSIC) return
    val progress = remember { Animatable(0f) }
    val sparks = remember(fx) { makeSparks(fx.ordinal * 977 + 5, 14) }
    LaunchedEffect(fx) {
        while (true) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(fxDurationMs(fx), easing = LinearEasing))
            kotlinx.coroutines.delay(450)
        }
    }
    Canvas(modifier.then(Modifier.size(tileSize))) {
        drawMergeFx(fx, progress.value.coerceIn(0f, 0.999f), sparks, size.minDimension, 64)
    }
}

private fun DrawScope.drawMergeFx(fx: MergeFx, p: Float, sparks: List<Spark>, s: Float, value: Int) {
    val c = Offset(size.width / 2f, size.height / 2f)
    val fade = (1f - p * p).coerceIn(0f, 1f)
    when (fx) {
        MergeFx.CLASSIC -> Unit
        MergeFx.SPARKS -> sparks.forEach { sp ->
            val t = ((p - sp.delay) / (1f - sp.delay)).coerceIn(0f, 1f)
            if (t <= 0f) return@forEach
            val e = easeOut(t)
            val pos = c + Offset(cos(sp.angle), sin(sp.angle)) * (s * sp.dist * e)
            val r = s * 0.13f * sp.size * (1f - t * 0.7f)
            sparkle(pos, r, if (sp.colorIdx % 2 == 0) Color(0xFFFFD166) else Color.White, (1f - t).coerceIn(0f, 1f))
        }
        MergeFx.BUBBLES -> sparks.forEach { sp ->
            val t = ((p - sp.delay) / (1f - sp.delay)).coerceIn(0f, 1f)
            if (t <= 0f) return@forEach
            val e = easeOut(t)
            val pos = c + Offset(cos(sp.angle) * s * sp.dist * 0.55f * e, -s * (0.25f + sp.dist * 0.9f) * e)
            val r = s * (0.05f + 0.07f * sp.size) * (0.7f + 0.6f * t)
            val a = (1f - t * t).coerceIn(0f, 1f)
            drawCircle(Color(0xFF9AD8F2).copy(alpha = 0.32f * a), r, pos)
            drawCircle(Color(0xFF5BB6E0).copy(alpha = 0.75f * a), r, pos, style = Stroke(s * 0.018f))
            drawCircle(Color.White.copy(alpha = 0.8f * a), r * 0.28f, pos + Offset(-r * 0.35f, -r * 0.35f))
        }
        MergeFx.PETALS -> sparks.forEach { sp ->
            val t = ((p - sp.delay) / (1f - sp.delay)).coerceIn(0f, 1f)
            if (t <= 0f) return@forEach
            val e = easeOut(t)
            val sway = sin(t * 7f + sp.spin) * s * 0.06f
            val pos = c + Offset(cos(sp.angle) * s * sp.dist * e + sway, sin(sp.angle) * s * sp.dist * e + t * t * s * 0.5f)
            val w = s * 0.17f * sp.size
            rotate(sp.spin * t, pos) {
                drawOval(petalColors[sp.colorIdx].copy(alpha = (1f - t * t).coerceIn(0f, 1f)), Offset(pos.x - w / 2, pos.y - w * 0.32f), Size(w, w * 0.64f))
            }
        }
        MergeFx.HEARTS -> sparks.forEach { sp ->
            val t = ((p - sp.delay) / (1f - sp.delay)).coerceIn(0f, 1f)
            if (t <= 0f) return@forEach
            val e = easeOut(t)
            val pos = c + Offset(cos(sp.angle) * s * sp.dist * 0.6f * e, -s * (0.2f + sp.dist * 0.95f) * e)
            val h = s * 0.16f * sp.size * (0.6f + 0.5f * (1f - t))
            heart(pos, h, if (sp.colorIdx % 3 == 0) Color(0xFFFF6B8A) else Color(0xFFFF9EB5), (1f - t * t).coerceIn(0f, 1f))
        }
        MergeFx.CONFETTI -> sparks.forEach { sp ->
            val t = ((p - sp.delay) / (1f - sp.delay)).coerceIn(0f, 1f)
            if (t <= 0f) return@forEach
            val e = easeOut(t)
            val pos = c + Offset(cos(sp.angle) * s * sp.dist * e, sin(sp.angle) * s * sp.dist * e + t * t * s * 0.8f)
            val w = s * 0.11f * sp.size
            rotate(sp.spin * t * 1.5f, pos) {
                drawRect(confettiColors[sp.colorIdx].copy(alpha = (1f - t * t * t).coerceIn(0f, 1f)), Offset(pos.x - w / 2, pos.y - w * 0.3f), Size(w, w * 0.6f))
            }
        }
        MergeFx.STARS -> sparks.forEach { sp ->
            val t = ((p - sp.delay) / (1f - sp.delay)).coerceIn(0f, 1f)
            if (t <= 0f) return@forEach
            val e = easeOut(t)
            val dir = Offset(cos(sp.angle), sin(sp.angle))
            val pos = c + dir * (s * sp.dist * 1.15f * e)
            val r = s * 0.12f * sp.size * (1f - t * 0.5f)
            val a = (1f - t).coerceIn(0f, 1f)
            // estela corta detrás de la estrella
            drawLine(Color(0xFFFFE08A).copy(alpha = 0.5f * a), pos, pos - dir * (s * 0.18f * (1f - t)), strokeWidth = r * 0.45f, cap = StrokeCap.Round)
            star5(pos, r, Color(0xFFFFD166).copy(alpha = a), sp.spin * t)
        }
        MergeFx.RIPPLE -> {
            for (i in 0..2) {
                val t = ((p - i * 0.12f) / (1f - i * 0.12f)).coerceIn(0f, 1f)
                if (t <= 0f) continue
                val e = easeOut(t)
                drawCircle(Color(0xFFB8A6FF).copy(alpha = 0.7f * (1f - t)), s * (0.3f + 0.9f * e), c, style = Stroke(s * 0.05f * (1f - t * 0.6f)))
            }
            drawCircle(Color.White.copy(alpha = 0.55f * (1f - p * 2f).coerceIn(0f, 1f)), s * 0.5f * (0.4f + p), c)
        }
        MergeFx.LIGHTNING -> {
            val flash = (1f - p * 1.6f).coerceIn(0f, 1f)
            drawCircle(Color(0xFFC4B5FD).copy(alpha = 0.45f * flash), s * (0.45f + p * 0.5f), c)
            sparks.take(5).forEachIndexed { i, sp ->
                val len = s * (0.7f + sp.dist * 0.5f)
                val path = Path().apply {
                    moveTo(c.x, c.y)
                    var cur = c
                    val seg = 4
                    for (k in 1..seg) {
                        val f = k / seg.toFloat()
                        val jitter = (if ((k + i) % 2 == 0) 1 else -1) * s * 0.09f
                        val dirX = cos(sp.angle); val dirY = sin(sp.angle)
                        cur = Offset(c.x + dirX * len * f - dirY * jitter, c.y + dirY * len * f + dirX * jitter)
                        lineTo(cur.x, cur.y)
                    }
                }
                drawPath(path, Color(0xFF7C5CFF).copy(alpha = flash), style = Stroke(s * 0.05f, cap = StrokeCap.Round))
                drawPath(path, Color.White.copy(alpha = flash * 0.9f), style = Stroke(s * 0.02f, cap = StrokeCap.Round))
            }
        }
        MergeFx.FIREWORKS -> sparks.forEach { sp ->
            val t = ((p - sp.delay * 0.6f) / (1f - sp.delay * 0.6f)).coerceIn(0f, 1f)
            if (t <= 0f) return@forEach
            val e = easeOut(t)
            val dir = Offset(cos(sp.angle), sin(sp.angle))
            val col = fireworkColors[sp.colorIdx]
            val head = c + dir * (s * sp.dist * 1.25f * e) + Offset(0f, t * t * s * 0.35f)
            val tail = c + dir * (s * sp.dist * 1.25f * max(0f, e - 0.28f)) + Offset(0f, t * t * s * 0.30f)
            val a = (1f - t * t).coerceIn(0f, 1f)
            drawLine(col.copy(alpha = 0.8f * a), tail, head, strokeWidth = s * 0.035f, cap = StrokeCap.Round)
            drawCircle(col.copy(alpha = a), s * 0.04f * (1f - t * 0.5f), head)
        }
    }
    // pequeño destello central en casi todos los efectos
    if (fx != MergeFx.RIPPLE && fx != MergeFx.LIGHTNING && fx != MergeFx.BUBBLES) {
        val a = (1f - p * 3f).coerceIn(0f, 1f)
        if (a > 0f) drawCircle(Color.White.copy(alpha = 0.5f * a), s * 0.32f * (0.6f + p), c)
    }
    @Suppress("UNUSED_VARIABLE") val unused = fade + value
}

private fun DrawScope.sparkle(center: Offset, r: Float, color: Color, alpha: Float) {
    if (alpha <= 0f) return
    val path = Path().apply {
        moveTo(center.x, center.y - r)
        quadraticTo(center.x, center.y, center.x + r, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + r)
        quadraticTo(center.x, center.y, center.x - r, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - r)
        close()
    }
    drawPath(path, color.copy(alpha = alpha), style = Fill)
}

private fun DrawScope.heart(center: Offset, h: Float, color: Color, alpha: Float) {
    if (alpha <= 0f) return
    val path = Path().apply {
        moveTo(center.x, center.y + h * 0.9f)
        cubicTo(center.x - h * 1.5f, center.y - h * 0.2f, center.x - h * 0.7f, center.y - h * 1.2f, center.x, center.y - h * 0.4f)
        cubicTo(center.x + h * 0.7f, center.y - h * 1.2f, center.x + h * 1.5f, center.y - h * 0.2f, center.x, center.y + h * 0.9f)
        close()
    }
    drawPath(path, color.copy(alpha = alpha), style = Fill)
    drawCircle(Color.White.copy(alpha = 0.7f * alpha), h * 0.18f, center + Offset(-h * 0.45f, -h * 0.25f))
}

private fun DrawScope.star5(center: Offset, r: Float, color: Color, rotation: Float) {
    if (color.alpha <= 0f) return
    val path = Path()
    for (i in 0 until 10) {
        val rad = if (i % 2 == 0) r else r * 0.45f
        val ang = (-PI / 2 + i * PI / 5).toFloat()
        val x = center.x + cos(ang) * rad
        val y = center.y + sin(ang) * rad
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    rotate(rotation, center) { drawPath(path, color, style = Fill) }
}
