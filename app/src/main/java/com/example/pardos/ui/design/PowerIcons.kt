package com.korkoor.pardos.ui.design

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Los poderes del juego, cada uno con su icono dibujado a mano. */
enum class PowerGlyph { UNDO, WAND, MERGE, BROOM, LINK }

private val Gold2 = Color(0xFFFFE08A)

/**
 * Iconos de poderes dibujados en un lienzo de 100×100: degradados, brillo, sombra interior y destellos que titilan.
 * No usan Material Icons: son parte de la identidad visual del juego.
 */
@Composable
fun PowerGlyphIcon(
    glyph: PowerGlyph,
    color: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    animate: Boolean = true
) {
    val clock by rememberInfiniteTransition(label = "glyph").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "glyphClock"
    )
    val t = if (animate && enabled) clock else 0.25f
    val base = if (enabled) color else Color(0xFF9A9AA8)
    val light = lerp(base, Color.White, 0.5f)
    val dark = base.darker(0.68f)

    Canvas(modifier) {
        val u = size.minDimension / 100f
        scale(u, u, pivot = Offset.Zero) {
            scale(1.2f, 1.2f, pivot = Offset(50f, 52f)) {
                when (glyph) {
                    PowerGlyph.UNDO -> drawUndo(base, light, dark, t, enabled)
                    PowerGlyph.WAND -> drawWand(base, light, dark, t, enabled)
                    PowerGlyph.MERGE -> drawMerge(base, light, dark, t, enabled)
                    PowerGlyph.BROOM -> drawBroom(base, light, dark, t, enabled)
                    PowerGlyph.LINK -> drawLink(base, light, dark, t, enabled)
                }
            }
        }
    }
}

private fun tw(t: Float, phase: Float): Float = 0.5f + 0.5f * sin(((t + phase) * 2 * PI).toFloat())

/** Destello de 4 puntas con halo. */
private fun DrawScope.sparkle(c: Offset, r: Float, color: Color, alpha: Float = 1f) {
    if (r <= 0.2f) return
    val p = Path().apply {
        moveTo(c.x, c.y - r)
        quadraticTo(c.x, c.y, c.x + r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y + r)
        quadraticTo(c.x, c.y, c.x - r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y - r)
        close()
    }
    drawCircle(color.copy(alpha = 0.22f * alpha), radius = r * 0.95f, center = c)
    drawPath(p, color.copy(alpha = alpha))
    drawCircle(Color.White.copy(alpha = 0.9f * alpha), radius = r * 0.16f, center = c)
}

private fun DrawScope.arrowHead(tip: Offset, dir: Offset, size: Float, brush: Brush) {
    val len = kotlin.math.sqrt(dir.x * dir.x + dir.y * dir.y).coerceAtLeast(0.001f)
    val d = Offset(dir.x / len, dir.y / len)
    val n = Offset(-d.y, d.x)
    val back = Offset(tip.x - d.x * size, tip.y - d.y * size)
    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(back.x + n.x * size * 0.75f, back.y + n.y * size * 0.75f)
        lineTo(back.x - n.x * size * 0.75f, back.y - n.y * size * 0.75f)
        close()
    }
    drawPath(path, brush)
}

// ---------------------------------------------------------------- DESHACER

private fun DrawScope.drawUndo(base: Color, light: Color, dark: Color, t: Float, enabled: Boolean) {
    val c = Offset(50f, 54f)
    val r = 30f
    val brush = Brush.linearGradient(listOf(light, base, dark), Offset(20f, 20f), Offset(80f, 90f))
    // sombra suave
    drawCircle(base.copy(alpha = 0.12f), radius = r + 14f, center = c)
    // anillo en arco (rebobinar)
    val rect = Rect(c.x - r, c.y - r, c.x + r, c.y + r)
    val path = Path().apply { arcTo(rect, 70f, -205f, true) }
    drawPath(path, brush, style = Stroke(width = 13f, cap = StrokeCap.Round))
    // brillo del arco
    val shine = Path().apply { arcTo(Rect(c.x - r + 2f, c.y - r + 2f, c.x + r - 2f, c.y + r - 2f), -70f, -90f, true) }
    drawPath(shine, Color.White.copy(alpha = 0.55f), style = Stroke(width = 3f, cap = StrokeCap.Round))
    // punta de flecha en el extremo (arriba a la izquierda, mirando hacia abajo-izquierda)
    val a = (-135.0 * PI / 180.0).toFloat()
    val end = Offset(c.x + cos(a) * r, c.y + sin(a) * r)
    val dir = Offset(sin(a), -cos(a)) // tangente antihoraria
    arrowHead(Offset(end.x + dir.x * 13f, end.y + dir.y * 13f), dir, 24f, brush)
    // reloj en el centro: la manecilla corta gira despacio hacia atrás
    drawCircle(Color.White.copy(alpha = 0.85f), radius = 15f, center = c)
    drawCircle(dark.copy(alpha = 0.55f), radius = 15f, center = c, style = Stroke(width = 2.2f))
    val ang = (-(t * 2 * PI) - PI / 2).toFloat()
    drawLine(dark, c, Offset(c.x + cos(ang) * 10f, c.y + sin(ang) * 10f), strokeWidth = 3.2f, cap = StrokeCap.Round)
    drawLine(dark, c, Offset(c.x, c.y - 7f), strokeWidth = 3.2f, cap = StrokeCap.Round)
    drawCircle(dark, radius = 2.4f, center = c)
    if (enabled) sparkle(Offset(80f, 24f), 6f * tw(t, 0.0f), Gold2, 0.9f)
}

// ---------------------------------------------------------------- LIMPIAR (varita)

private fun DrawScope.drawWand(base: Color, light: Color, dark: Color, t: Float, enabled: Boolean) {
    // estela de polvo mágico
    for (i in 0 until 5) {
        val f = i / 5f
        drawCircle(
            light.copy(alpha = 0.35f * (1f - f)), radius = 5.5f - f * 3.2f,
            center = Offset(30f - f * 22f, 76f + f * 14f)
        )
    }
    // cuerpo de la varita
    val handle = Brush.linearGradient(listOf(base.darker(0.85f), light), Offset(20f, 90f), Offset(60f, 46f))
    drawLine(dark.copy(alpha = 0.5f), Offset(24f, 82f), Offset(58f, 48f), strokeWidth = 19f, cap = StrokeCap.Round)
    drawLine(handle, Offset(24f, 82f), Offset(58f, 48f), strokeWidth = 16f, cap = StrokeCap.Round)
    // anillo dorado cerca del mango
    drawLine(Gold2, Offset(35f, 71f), Offset(40f, 66f), strokeWidth = 17f, cap = StrokeCap.Butt)
    // punta clara
    drawLine(Color.White.copy(alpha = 0.9f), Offset(56f, 50f), Offset(62f, 44f), strokeWidth = 16f, cap = StrokeCap.Round)
    // brillo largo
    drawLine(Color.White.copy(alpha = 0.65f), Offset(29f, 75f), Offset(52f, 52f), strokeWidth = 3.4f, cap = StrokeCap.Round)
    // estrella grande en la punta
    val pulse = 0.85f + 0.3f * tw(t, 0f)
    sparkle(Offset(70f, 33f), 20f * pulse, Gold2)
    if (enabled) {
        sparkle(Offset(84f, 58f), 7f * tw(t, 0.33f), light)
        sparkle(Offset(46f, 22f), 6f * tw(t, 0.66f), light)
        sparkle(Offset(86f, 18f), 4.5f * tw(t, 0.15f), Gold2)
    }
}

// ---------------------------------------------------------------- FUSIÓN (dos orbes que se unen)

private fun DrawScope.drawMerge(base: Color, light: Color, dark: Color, t: Float, enabled: Boolean) {
    val approach = 4f * tw(t, 0f) // los orbes "respiran" acercándose
    val lc = Offset(36f + approach * 0.5f, 62f)
    val rc = Offset(64f - approach * 0.5f, 62f)
    val r = 21f
    drawCircle(base.copy(alpha = 0.18f), radius = 42f, center = Offset(50f, 58f))
    val warm = lerp(base, Color(0xFFFF6B57), 0.6f)
    for ((c, hue) in listOf(lc to base, rc to warm)) {
        val brush = Brush.radialGradient(
            listOf(lerp(hue, Color.White, 0.65f), hue, hue.darker(0.8f)), center = Offset(c.x - 6f, c.y - 7f), radius = r * 1.6f
        )
        drawCircle(brush, radius = r, center = c)
        drawCircle(dark.copy(alpha = 0.45f), radius = r, center = c, style = Stroke(width = 2f))
        drawCircle(Color.White.copy(alpha = 0.75f), radius = 4.2f, center = Offset(c.x - 8f, c.y - 9f))
    }
    // lente de unión
    val lens = Path().apply {
        val x = 50f
        val h = kotlin.math.sqrt((r * r - ((rc.x - lc.x) / 2f).let { it * it }).coerceAtLeast(1f))
        moveTo(x, 62f - h)
        quadraticTo(x + 11f, 62f, x, 62f + h)
        quadraticTo(x - 11f, 62f, x, 62f - h)
        close()
    }
    drawPath(lens, Color.White.copy(alpha = 0.95f))
    drawPath(lens, Gold2.copy(alpha = 0.55f))
    // chispa de fusión arriba
    val pulse = 0.8f + 0.4f * tw(t, 0.2f)
    sparkle(Offset(50f, 24f), 15f * pulse, Gold2)
    if (enabled) {
        sparkle(Offset(80f, 34f), 5.5f * tw(t, 0.5f), light)
        sparkle(Offset(20f, 36f), 5f * tw(t, 0.8f), light)
    }
}

// ---------------------------------------------------------------- ESCOBA

private fun DrawScope.drawBroom(base: Color, light: Color, dark: Color, t: Float, enabled: Boolean) {
    // polvo que sale volando
    for (i in 0 until 4) {
        val f = ((t + i * 0.25f) % 1f)
        drawCircle(
            base.copy(alpha = 0.5f * (1f - f)), radius = 3.6f - f * 1.6f,
            center = Offset(18f - f * 10f, 86f - f * 18f - i * 3f)
        )
    }
    val sweep = (if (enabled) sin((t * 2 * PI).toFloat()) * 5f else 0f)
    rotate(36f + sweep, pivot = Offset(50f, 50f)) {
        // mango
        drawLine(
            Brush.verticalGradient(listOf(lerp(dark, Color(0xFFB98A5A), 0.6f), dark), 8f, 56f),
            Offset(50f, 8f), Offset(50f, 56f), strokeWidth = 8f, cap = StrokeCap.Round
        )
        // cabeza de cerdas
        val head = Path().apply {
            moveTo(36f, 56f); lineTo(64f, 56f); lineTo(74f, 92f); lineTo(26f, 92f); close()
        }
        drawPath(head, Brush.verticalGradient(listOf(light, base, dark), 56f, 92f))
        // cerdas
        for (i in 0..6) {
            val x0 = 33f + i * 5.6f
            val x1 = 28f + i * 7.3f
            drawLine(dark.copy(alpha = 0.55f), Offset(x0, 66f), Offset(x1, 91f), strokeWidth = 1.7f, cap = StrokeCap.Round)
        }
        // banda
        drawRoundRect(Gold2, Offset(34f, 52f), Size(32f, 10f), CornerRadius(4f))
        drawRoundRect(Color.White.copy(alpha = 0.6f), Offset(37f, 54f), Size(26f, 2.4f), CornerRadius(1.2f))
    }
    if (enabled) sparkle(Offset(78f, 22f), 7f * tw(t, 0.3f), Gold2)
}

// ---------------------------------------------------------------- FUSIÓN MANUAL (dos fichas que se enlazan)

private fun DrawScope.drawLink(base: Color, light: Color, dark: Color, t: Float, enabled: Boolean) {
    val shift = 3f * tw(t, 0f)
    val tileBrush = Brush.verticalGradient(listOf(light, base), 36f, 72f)
    // fichas
    for (left in listOf(true, false)) {
        val x = (if (left) 10f + shift else 60f - shift)
        drawRoundRect(tileBrush, Offset(x, 38f), Size(30f, 36f), CornerRadius(10f))
        drawRoundRect(dark.copy(alpha = 0.55f), Offset(x, 38f), Size(30f, 36f), CornerRadius(10f), style = Stroke(2.4f))
        drawRoundRect(Color.White.copy(alpha = 0.45f), Offset(x + 4f, 43f), Size(22f, 5f), CornerRadius(2.5f))
        // flecha hacia el centro
        val cx = x + 15f
        val dir = if (left) 1f else -1f
        drawLine(Color.White, Offset(cx - 5f * dir, 56f), Offset(cx + 5f * dir, 56f), strokeWidth = 4f, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(cx + 5f * dir, 56f), Offset(cx + 1f * dir, 51f), strokeWidth = 4f, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(cx + 5f * dir, 56f), Offset(cx + 1f * dir, 61f), strokeWidth = 4f, cap = StrokeCap.Round)
    }
    // eslabón: arco por encima
    val link = Path().apply { moveTo(25f, 38f); quadraticTo(50f, 6f, 75f, 38f) }
    drawPath(link, Brush.horizontalGradient(listOf(base, light, base), 20f, 80f), style = Stroke(width = 5.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    // chispa central
    sparkle(Offset(50f, 56f), 11f * (0.8f + 0.4f * tw(t, 0.1f)), Gold2)
    if (enabled) {
        sparkle(Offset(50f, 14f), 5f * tw(t, 0.45f), Gold2)
        sparkle(Offset(86f, 84f), 5.5f * tw(t, 0.7f), light)
    }
}
