package com.korkoor.pardos.ui.design

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// =====================================================================================
//  ICONOS COZY
//  Ilustraciones suaves dibujadas a mano (degradados, contorno redondeado, brillo, caritas
//  en los grandes). Reemplazan a los iconos de Material en toda la app: el componente `Icon`
//  de este paquete reconoce por nombre los iconos que tienen versión cozy y dibuja esa.
// =====================================================================================

enum class CozyKind { STAR, COIN, GEM, FLAME, TROPHY, CHEST, HEART, GIFT, BOLT, TIMER, SHIELD, PIGGY, SPARKLES, PARTY, DICE, CROWN }

/** Reloj compartido: un solo temporizador mueve los destellos de todos los iconos (barato). */
val LocalCozyClock = staticCompositionLocalOf<State<Float>> { object : State<Float> { override val value = 0.25f } }

@Composable
fun CozyClockProvider(content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition(label = "cozyClock")
    val t = transition.animateFloat(0f, 1f, infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), label = "t")
    CompositionLocalProvider(LocalCozyClock provides t) { content() }
}

private fun kindFor(vector: ImageVector): CozyKind? = when (vector.name.substringAfterLast('.')) {
    "Star", "StarRate", "Stars" -> CozyKind.STAR
    "MonetizationOn" -> CozyKind.COIN
    "Diamond" -> CozyKind.GEM
    "LocalFireDepartment", "Whatshot" -> CozyKind.FLAME
    "EmojiEvents" -> CozyKind.TROPHY
    "Inventory2" -> CozyKind.CHEST
    "Favorite" -> CozyKind.HEART
    "CardGiftcard" -> CozyKind.GIFT
    "Bolt", "FlashOn" -> CozyKind.BOLT
    "Timer" -> CozyKind.TIMER
    "Shield" -> CozyKind.SHIELD
    "Savings" -> CozyKind.PIGGY
    "AutoAwesome" -> CozyKind.SPARKLES
    "Celebration" -> CozyKind.PARTY
    "Casino" -> CozyKind.DICE
    "WorkspacePremium" -> CozyKind.CROWN
    else -> null
}

/**
 * Sustituto de `androidx.compose.material3.Icon`. Si el icono tiene versión cozy la dibuja; si no, usa el de Material.
 * Un tinte casi transparente o blanco (iconos apagados o sobre botones de color) se dibuja como silueta plana de ese tinte.
 */
@Composable
fun Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    val kind = kindFor(imageVector)
    if (kind == null) {
        androidx.compose.material3.Icon(imageVector, contentDescription, modifier, tint)
    } else {
        CozyIcon(kind, modifier.then(Modifier.size(24.dp)), tint = tint, description = contentDescription)
    }
}

/** Icono cozy directo. [tint] solo decide si va a todo color o plano; el color lo da la propia ilustración. */
@Composable
fun CozyIcon(
    kind: CozyKind,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    animate: Boolean = true,
    description: String? = null
) {
    val isWhite = tint != Color.Unspecified && tint.red > 0.95f && tint.green > 0.95f && tint.blue > 0.95f
    val flat = tint != Color.Unspecified && (tint.alpha < 0.55f || isWhite)
    val clock = LocalCozyClock.current
    val sem = if (description != null) Modifier.semantics { contentDescription = description } else Modifier

    Canvas(modifier.then(sem).clipToBounds()) {
        val u = size.minDimension / 100f
        val big = size.minDimension > 26.dp.toPx()
        val t = if (animate && !flat) clock.value else 0.25f
        val pal = if (flat) flatPalette(tint) else naturalPalette(kind)
        scale(u, u, pivot = Offset.Zero) { drawCozy(kind, pal, t, big, flat) }
    }
}

// ------------------------------------------------------------------ paletas

private class Pal(val base: Color, val light: Color, val dark: Color, val extra: Color, val extra2: Color)

private fun flatPalette(c: Color) = Pal(c, c, c, c, c)

private fun naturalPalette(k: CozyKind): Pal = when (k) {
    CozyKind.STAR, CozyKind.COIN, CozyKind.TROPHY, CozyKind.SPARKLES ->
        Pal(Color(0xFFFFC83D), Color(0xFFFFE680), Color(0xFFD9961A), Color(0xFFFFF4C2), Color(0xFFB87510))
    CozyKind.CROWN -> Pal(Color(0xFFFFC83D), Color(0xFFFFE680), Color(0xFFD9961A), Color(0xFFFF6B8A), Color(0xFF5BC0EB))
    CozyKind.GEM -> Pal(Color(0xFF4FB4DC), Color(0xFFA6E6FA), Color(0xFF2A7AA8), Color(0xFFE6FAFF), Color(0xFF1F5E85))
    CozyKind.FLAME -> Pal(Color(0xFFFF7A3C), Color(0xFFFFB347), Color(0xFFD93F2B), Color(0xFFFFE08A), Color(0xFFFFF4C2))
    CozyKind.CHEST -> Pal(Color(0xFFB8733A), Color(0xFFDDA566), Color(0xFF7A4720), Color(0xFFFFD36E), Color(0xFFFFF1B8))
    CozyKind.HEART -> Pal(Color(0xFFFF6B8A), Color(0xFFFFB3C6), Color(0xFFD93A63), Color(0xFFFFE0E8), Color(0xFFB02850))
    CozyKind.GIFT -> Pal(Color(0xFFE5576B), Color(0xFFFF9AA8), Color(0xFFB83048), Color(0xFFFFD36E), Color(0xFFFFF1B8))
    CozyKind.BOLT -> Pal(Color(0xFFFFC83D), Color(0xFFFFE680), Color(0xFFE08A12), Color(0xFFFFF4C2), Color(0xFFB87510))
    CozyKind.TIMER -> Pal(Color(0xFF4FB8A8), Color(0xFFA6EBDD), Color(0xFF2A8576), Color(0xFFFFFFFF), Color(0xFFE07A5F))
    CozyKind.SHIELD -> Pal(Color(0xFF6BB08A), Color(0xFFB5E6C9), Color(0xFF3E8060), Color(0xFFFFFFFF), Color(0xFFFFE680))
    CozyKind.PIGGY -> Pal(Color(0xFFFF9EB5), Color(0xFFFFCBD8), Color(0xFFD9667F), Color(0xFFFFE680), Color(0xFF8A3A50))
    CozyKind.PARTY -> Pal(Color(0xFFFFB347), Color(0xFFFFD98A), Color(0xFFD97A1A), Color(0xFFFF6B8A), Color(0xFF5BC0EB))
    CozyKind.DICE -> Pal(Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFF7C74E8), Color(0xFF6C63FF), Color(0xFFE8E6F5))
}

// ------------------------------------------------------------------ utilidades de dibujo

private fun vGrad(a: Color, b: Color, y0: Float, y1: Float) = Brush.verticalGradient(listOf(a, b), startY = y0, endY = y1)

private fun DrawScope.shadow(cx: Float, cy: Float, rx: Float, ry: Float, flat: Boolean) {
    if (!flat) drawOval(Color.Black.copy(alpha = 0.13f), Offset(cx - rx, cy - ry), Size(rx * 2, ry * 2))
}

private fun DrawScope.shine(x: Float, y: Float, w: Float, h: Float, rot: Float, flat: Boolean, alpha: Float = 0.6f) {
    if (flat) return
    rotate(rot, pivot = Offset(x + w / 2, y + h / 2)) {
        drawOval(Color.White.copy(alpha = alpha), Offset(x, y), Size(w, h))
    }
}

private fun DrawScope.sparkle4(c: Offset, r: Float, color: Color, glow: Boolean = true) {
    if (r <= 0.3f) return
    val p = Path().apply {
        moveTo(c.x, c.y - r)
        quadraticTo(c.x, c.y, c.x + r, c.y); quadraticTo(c.x, c.y, c.x, c.y + r)
        quadraticTo(c.x, c.y, c.x - r, c.y); quadraticTo(c.x, c.y, c.x, c.y - r); close()
    }
    if (glow) drawCircle(color.copy(alpha = 0.22f), r * 0.95f, c)
    drawPath(p, color)
}

private fun starPath(cx: Float, cy: Float, outer: Float, inner: Float): Path {
    val path = Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outer else inner
        val a = (-90.0 + i * 36.0) * PI / 180.0
        val x = cx + (r * cos(a)).toFloat()
        val y = cy + (r * sin(a)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun tw(t: Float, phase: Float): Float = 0.5f + 0.5f * sin(((t + phase) * 2 * PI).toFloat())

private fun DrawScope.face(cx: Float, cy: Float, s: Float, ink: Color, blush: Color) {
    drawCircle(ink, 2.6f * s, Offset(cx - 9f * s, cy))
    drawCircle(ink, 2.6f * s, Offset(cx + 9f * s, cy))
    drawCircle(Color.White.copy(alpha = 0.9f), 0.9f * s, Offset(cx - 9.8f * s, cy - 1f * s))
    drawCircle(Color.White.copy(alpha = 0.9f), 0.9f * s, Offset(cx + 8.2f * s, cy - 1f * s))
    drawArc(ink, 20f, 140f, false, Offset(cx - 4.5f * s, cy + 1f * s), Size(9f * s, 7f * s), style = Stroke(1.8f * s, cap = StrokeCap.Round))
    drawCircle(blush.copy(alpha = 0.55f), 3.4f * s, Offset(cx - 15f * s, cy + 5f * s))
    drawCircle(blush.copy(alpha = 0.55f), 3.4f * s, Offset(cx + 15f * s, cy + 5f * s))
}

// ------------------------------------------------------------------ dibujo por tipo

private fun DrawScope.drawCozy(kind: CozyKind, p: Pal, t: Float, big: Boolean, flat: Boolean) {
    when (kind) {
        CozyKind.STAR -> star(p, t, big, flat)
        CozyKind.COIN -> coin(p, t, flat)
        CozyKind.GEM -> gem(p, t, flat)
        CozyKind.FLAME -> flame(p, t, flat)
        CozyKind.TROPHY -> trophy(p, t, flat)
        CozyKind.CHEST -> chest(p, t, flat)
        CozyKind.HEART -> heart(p, t, big, flat)
        CozyKind.GIFT -> gift(p, t, flat)
        CozyKind.BOLT -> bolt(p, t, flat)
        CozyKind.TIMER -> timer(p, t, flat)
        CozyKind.SHIELD -> shield(p, t, flat)
        CozyKind.PIGGY -> piggy(p, t, flat)
        CozyKind.SPARKLES -> sparkles(p, t, flat)
        CozyKind.PARTY -> party(p, t, flat)
        CozyKind.DICE -> dice(p, t, flat)
        CozyKind.CROWN -> crown(p, t, flat)
    }
}

private fun DrawScope.star(p: Pal, t: Float, big: Boolean, flat: Boolean) {
    shadow(50f, 92f, 28f, 4f, flat)
    val path = starPath(50f, 54f, 44f, 22f)
    val fill = vGrad(p.light, p.base, 10f, 96f)
    if (!flat) drawPath(path, p.dark, style = Stroke(width = 11f, join = StrokeJoin.Round, cap = StrokeCap.Round))
    drawPath(path, fill)
    drawPath(path, fill, style = Stroke(width = 6f, join = StrokeJoin.Round, cap = StrokeCap.Round))
    if (!flat) {
        shine(33f, 26f, 14f, 8f, -35f, false)
        if (big) face(50f, 56f, 1f, p.dark.copy(alpha = 0.85f), Color(0xFFFF8FA3))
        sparkle4(Offset(82f, 22f), 7f * tw(t, 0f), p.extra)
    }
}

private fun DrawScope.coin(p: Pal, t: Float, flat: Boolean) {
    shadow(50f, 92f, 30f, 4f, flat)
    val c = Offset(50f, 50f)
    if (!flat) drawCircle(p.dark, 44f, c)
    drawCircle(vGrad(p.light, p.base, 8f, 92f), if (flat) 44f else 40f, c)
    if (!flat) {
        drawCircle(p.dark.copy(alpha = 0.35f), 29f, c, style = Stroke(3f))
        // sello: una chispa en relieve
        sparkle4(Offset(51f, 52f), 14f, p.dark.copy(alpha = 0.5f), glow = false)
        sparkle4(Offset(50f, 50f), 14f, p.extra, glow = false)
        drawArc(Color.White.copy(alpha = 0.6f), 195f, 55f, false, Offset(16f, 16f), Size(68f, 68f), style = Stroke(4f, cap = StrokeCap.Round))
        // destello que cruza la moneda
        val x = 20f + 60f * ((t * 1.6f) % 1f)
        if (t * 1.6f % 1f < 1f && t < 0.62f) drawLine(Color.White.copy(alpha = 0.45f), Offset(x, 22f), Offset(x - 14f, 78f), strokeWidth = 7f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.gem(p: Pal, t: Float, flat: Boolean) {
    shadow(50f, 92f, 26f, 4f, flat)
    val outline = Path().apply {
        moveTo(20f, 38f); lineTo(33f, 17f); lineTo(67f, 17f); lineTo(80f, 38f); lineTo(50f, 88f); close()
    }
    if (!flat) drawPath(outline, p.dark, style = Stroke(width = 11f, join = StrokeJoin.Round))
    val fill = vGrad(p.light, p.base, 15f, 90f)
    drawPath(outline, fill)
    drawPath(outline, fill, style = Stroke(width = 6f, join = StrokeJoin.Round))
    if (!flat) {
        val table = Path().apply { moveTo(34f, 19f); lineTo(66f, 19f); lineTo(58f, 38f); lineTo(42f, 38f); close() }
        drawPath(table, Color.White.copy(alpha = 0.38f))
        val line = Color.White.copy(alpha = 0.5f)
        drawLine(line, Offset(20f, 38f), Offset(80f, 38f), strokeWidth = 2.2f)
        drawLine(line, Offset(34f, 19f), Offset(42f, 38f), strokeWidth = 2.2f)
        drawLine(line, Offset(66f, 19f), Offset(58f, 38f), strokeWidth = 2.2f)
        drawLine(line, Offset(42f, 38f), Offset(50f, 86f), strokeWidth = 2.2f)
        drawLine(line, Offset(58f, 38f), Offset(50f, 86f), strokeWidth = 2.2f)
        val half = Path().apply { moveTo(50f, 38f); lineTo(80f, 38f); lineTo(50f, 88f); close() }
        drawPath(half, p.dark.copy(alpha = 0.22f))
        sparkle4(Offset(30f, 30f), 7f * tw(t, 0.2f), Color.White)
        sparkle4(Offset(78f, 18f), 5f * tw(t, 0.6f), p.extra)
    }
}

private fun DrawScope.flame(p: Pal, t: Float, flat: Boolean) {
    shadow(50f, 94f, 24f, 3.5f, flat)
    val wob = if (flat) 0f else sin((t * 2 * PI).toFloat()) * 3.5f
    rotate(wob, pivot = Offset(50f, 92f)) {
        val body = Path().apply {
            moveTo(50f, 6f)
            cubicTo(58f, 26f, 82f, 36f, 82f, 62f)
            cubicTo(82f, 82f, 67f, 94f, 50f, 94f)
            cubicTo(33f, 94f, 18f, 82f, 18f, 62f)
            cubicTo(18f, 48f, 27f, 40f, 33f, 30f)
            cubicTo(35f, 41f, 41f, 46f, 45f, 44f)
            cubicTo(40f, 30f, 44f, 17f, 50f, 6f)
            close()
        }
        val fill = vGrad(p.light, p.dark, 6f, 94f)
        if (!flat) drawPath(body, p.dark, style = Stroke(width = 8f, join = StrokeJoin.Round))
        drawPath(body, if (flat) SolidBrush(p.base) else fill)
        drawPath(body, if (flat) SolidBrush(p.base) else fill, style = Stroke(width = 4f, join = StrokeJoin.Round))
        if (!flat) {
            scale(0.56f, 0.56f, pivot = Offset(50f, 92f)) {
                drawPath(body, vGrad(p.extra2, p.extra, 6f, 94f))
            }
            shine(30f, 52f, 9f, 16f, 20f, false, 0.45f)
        }
    }
}

private fun SolidBrush(c: Color) = Brush.linearGradient(listOf(c, c))

private fun DrawScope.trophy(p: Pal, t: Float, flat: Boolean) {
    shadow(50f, 94f, 28f, 3.5f, flat)
    val ink = if (flat) p.base else p.dark
    // asas
    drawArc(ink, 90f, 180f, false, Offset(8f, 18f), Size(28f, 28f), style = Stroke(width = 7f, cap = StrokeCap.Round))
    drawArc(ink, -90f, 180f, false, Offset(64f, 18f), Size(28f, 28f), style = Stroke(width = 7f, cap = StrokeCap.Round))
    // pie
    drawRoundRect(if (flat) p.base else p.dark, Offset(43f, 62f), Size(14f, 16f), CornerRadius(3f))
    drawRoundRect(vGrad(p.light, p.base, 78f, 92f), Offset(28f, 78f), Size(44f, 14f), CornerRadius(5f))
    if (!flat) drawRoundRect(p.dark, Offset(28f, 78f), Size(44f, 14f), CornerRadius(5f), style = Stroke(2.6f))
    // copa
    val cup = Path().apply {
        moveTo(24f, 10f); lineTo(76f, 10f); lineTo(72f, 42f)
        cubicTo(70f, 58f, 60f, 66f, 50f, 66f)
        cubicTo(40f, 66f, 30f, 58f, 28f, 42f); close()
    }
    if (!flat) drawPath(cup, p.dark, style = Stroke(width = 8f, join = StrokeJoin.Round))
    val fill = vGrad(p.light, p.base, 10f, 66f)
    drawPath(cup, fill)
    drawPath(cup, fill, style = Stroke(width = 4f, join = StrokeJoin.Round))
    if (!flat) {
        drawPath(starPath(50f, 36f, 12f, 5.5f), p.extra)
        shine(33f, 16f, 8f, 24f, 8f, false, 0.5f)
        sparkle4(Offset(84f, 12f), 6f * tw(t, 0.4f), p.extra)
    }
}

private fun DrawScope.chest(p: Pal, t: Float, flat: Boolean) {
    shadow(50f, 90f, 36f, 4f, flat)
    val bodyFill = vGrad(p.light, p.base, 44f, 88f)
    drawRoundRect(bodyFill, Offset(12f, 44f), Size(76f, 44f), CornerRadius(9f))
    val lid = Path().apply {
        moveTo(12f, 50f); lineTo(12f, 36f); quadraticTo(50f, 6f, 88f, 36f); lineTo(88f, 50f); close()
    }
    drawPath(lid, vGrad(p.light, p.base, 10f, 50f))
    if (!flat) {
        drawRoundRect(p.dark, Offset(12f, 44f), Size(76f, 44f), CornerRadius(9f), style = Stroke(3f))
        drawPath(lid, p.dark, style = Stroke(3f, join = StrokeJoin.Round))
        // bandas doradas
        drawRoundRect(p.extra, Offset(12f, 42f), Size(76f, 9f), CornerRadius(4f))
        drawRoundRect(p.extra, Offset(27f, 20f), Size(10f, 68f), CornerRadius(3f))
        drawRoundRect(p.extra, Offset(63f, 20f), Size(10f, 68f), CornerRadius(3f))
        // cerradura
        drawCircle(p.extra2, 9f, Offset(50f, 52f))
        drawCircle(p.dark, 9f, Offset(50f, 52f), style = Stroke(2.4f))
        drawRoundRect(p.dark, Offset(48.3f, 50f), Size(3.4f, 8f), CornerRadius(1.7f))
        sparkle4(Offset(84f, 18f), 6f * tw(t, 0.1f), p.extra2)
    } else {
        // silueta plana: la tapa se separa del cuerpo con una línea oscura
        drawLine(Color.Black.copy(alpha = 0.30f), Offset(12f, 49f), Offset(88f, 49f), strokeWidth = 4f, cap = StrokeCap.Round)
        drawCircle(Color.Black.copy(alpha = 0.30f), 7f, Offset(50f, 56f))
    }
}

private fun DrawScope.heart(p: Pal, t: Float, big: Boolean, flat: Boolean) {
    shadow(50f, 92f, 26f, 3.5f, flat)
    val beat = if (flat) 1f else 1f + 0.05f * sin((t * 4 * PI).toFloat()).coerceAtLeast(0f)
    scale(beat, beat, pivot = Offset(50f, 56f)) {
        val path = Path().apply {
            moveTo(50f, 88f)
            cubicTo(8f, 58f, 6f, 28f, 30f, 19f)
            cubicTo(42f, 15f, 50f, 25f, 50f, 31f)
            cubicTo(50f, 25f, 58f, 15f, 70f, 19f)
            cubicTo(94f, 28f, 92f, 58f, 50f, 88f); close()
        }
        val fill = vGrad(p.light, p.base, 15f, 88f)
        if (!flat) drawPath(path, p.dark, style = Stroke(width = 10f, join = StrokeJoin.Round))
        drawPath(path, fill)
        drawPath(path, fill, style = Stroke(width = 5f, join = StrokeJoin.Round))
        if (!flat) {
            shine(24f, 28f, 16f, 9f, -35f, false)
            if (big) face(50f, 52f, 1f, p.dark.copy(alpha = 0.85f), Color(0xFFFFFFFF))
        }
    }
}

private fun DrawScope.gift(p: Pal, t: Float, flat: Boolean) {
    shadow(50f, 92f, 34f, 4f, flat)
    drawRoundRect(vGrad(p.light, p.base, 42f, 90f), Offset(16f, 44f), Size(68f, 46f), CornerRadius(8f))
    drawRoundRect(vGrad(p.light, p.base, 30f, 48f), Offset(10f, 32f), Size(80f, 17f), CornerRadius(7f))
    if (!flat) {
        drawRoundRect(p.dark, Offset(16f, 44f), Size(68f, 46f), CornerRadius(8f), style = Stroke(3f))
        drawRoundRect(p.dark, Offset(10f, 32f), Size(80f, 17f), CornerRadius(7f), style = Stroke(3f))
        drawRoundRect(p.extra, Offset(43f, 32f), Size(14f, 58f), CornerRadius(3f))
        // lazo
        for (dx in listOf(-1f, 1f)) {
            val loop = Path().apply {
                moveTo(50f, 30f)
                cubicTo(50f + dx * 30f, 4f, 50f + dx * 34f, 28f, 50f, 30f); close()
            }
            drawPath(loop, p.extra)
            drawPath(loop, p.extra2.copy(alpha = 0.0f))
            drawPath(loop, Color(0xFFD9961A), style = Stroke(2.6f, join = StrokeJoin.Round))
        }
        drawCircle(p.extra, 7f, Offset(50f, 30f))
        drawCircle(Color(0xFFD9961A), 7f, Offset(50f, 30f), style = Stroke(2.2f))
        sparkle4(Offset(82f, 22f), 6f * tw(t, 0.3f), p.extra2)
    }
}

private fun DrawScope.bolt(p: Pal, t: Float, flat: Boolean) {
    shadow(50f, 94f, 22f, 3f, flat)
    val path = Path().apply {
        moveTo(62f, 4f); lineTo(20f, 56f); lineTo(46f, 56f); lineTo(34f, 96f)
        lineTo(82f, 38f); lineTo(55f, 38f); lineTo(70f, 4f); close()
    }
    val fill = vGrad(p.light, p.base, 4f, 96f)
    if (!flat) drawPath(path, p.dark, style = Stroke(width = 10f, join = StrokeJoin.Round))
    drawPath(path, fill)
    drawPath(path, fill, style = Stroke(width = 5f, join = StrokeJoin.Round))
    if (!flat) {
        shine(52f, 10f, 6f, 20f, 18f, false, 0.55f)
        sparkle4(Offset(24f, 22f), 6f * tw(t, 0.5f), p.extra)
    }
}

private fun DrawScope.timer(p: Pal, t: Float, flat: Boolean) {
    shadow(50f, 94f, 26f, 3.5f, flat)
    drawRoundRect(if (flat) p.base else p.dark, Offset(42f, 4f), Size(16f, 12f), CornerRadius(4f))
    drawRoundRect(if (flat) p.base else p.dark, Offset(44f, 12f), Size(12f, 10f), CornerRadius(2f))
    rotate(-40f, pivot = Offset(50f, 58f)) {
        drawRoundRect(if (flat) p.base else p.dark, Offset(46f, 6f), Size(8f, 8f), CornerRadius(3f))
    }
    val c = Offset(50f, 58f)
    drawCircle(vGrad(p.light, p.base, 22f, 94f), 36f, c)
    if (!flat) {
        drawCircle(p.dark, 36f, c, style = Stroke(3f))
        drawCircle(Color.White, 26f, c)
        drawCircle(p.dark.copy(alpha = 0.25f), 26f, c, style = Stroke(2f))
        for (i in 0 until 12) {
            val a = (i * 30.0 - 90.0) * PI / 180.0
            drawLine(p.dark.copy(alpha = 0.5f), Offset(c.x + (21 * cos(a)).toFloat(), c.y + (21 * sin(a)).toFloat()), Offset(c.x + (25 * cos(a)).toFloat(), c.y + (25 * sin(a)).toFloat()), strokeWidth = 1.8f, cap = StrokeCap.Round)
        }
        val ang = ((t * 2 * PI) - PI / 2).toFloat()
        drawLine(p.extra2, c, Offset(c.x + cos(ang) * 19f, c.y + sin(ang) * 19f), strokeWidth = 3.4f, cap = StrokeCap.Round)
        drawLine(p.dark, c, Offset(c.x, c.y - 14f), strokeWidth = 3.4f, cap = StrokeCap.Round)
        drawCircle(p.dark, 3.4f, c)
    } else {
        drawLine(Color.Black.copy(alpha = 0f), c, c)
    }
}

private fun DrawScope.shield(p: Pal, t: Float, flat: Boolean) {
    shadow(50f, 95f, 24f, 3f, flat)
    val path = Path().apply {
        moveTo(50f, 6f); lineTo(86f, 20f); lineTo(86f, 48f)
        cubicTo(86f, 72f, 70f, 86f, 50f, 94f)
        cubicTo(30f, 86f, 14f, 72f, 14f, 48f); lineTo(14f, 20f); close()
    }
    val fill = vGrad(p.light, p.base, 6f, 94f)
    if (!flat) drawPath(path, p.dark, style = Stroke(width = 9f, join = StrokeJoin.Round))
    drawPath(path, fill)
    drawPath(path, fill, style = Stroke(width = 5f, join = StrokeJoin.Round))
    if (!flat) {
        drawPath(starPath(50f, 50f, 20f, 9f), p.extra2)
        shine(24f, 20f, 8f, 26f, 10f, false, 0.45f)
        sparkle4(Offset(80f, 14f), 5f * tw(t, 0.3f), Color.White)
    }
}

private fun DrawScope.piggy(p: Pal, t: Float, flat: Boolean) {
    shadow(50f, 92f, 34f, 4f, flat)
    val ink = if (flat) p.base else p.dark
    // orejas
    for (x in listOf(30f, 56f)) {
        val ear = Path().apply { moveTo(x, 36f); lineTo(x + 6f, 14f); lineTo(x + 20f, 34f); close() }
        drawPath(ear, if (flat) SolidBrush(p.base) else SolidBrush(p.dark))
        drawPath(ear, SolidBrush(p.base), style = Stroke(3f, join = StrokeJoin.Round))
    }
    // patas
    drawRoundRect(ink, Offset(26f, 74f), Size(12f, 16f), CornerRadius(5f))
    drawRoundRect(ink, Offset(60f, 74f), Size(12f, 16f), CornerRadius(5f))
    // cuerpo
    drawOval(vGrad(p.light, p.base, 28f, 86f), Offset(12f, 30f), Size(70f, 56f))
    if (!flat) drawOval(p.dark, Offset(12f, 30f), Size(70f, 56f), style = Stroke(3f))
    // hocico
    drawOval(vGrad(p.light, p.base, 48f, 72f), Offset(70f, 46f), Size(22f, 22f))
    if (!flat) {
        drawOval(p.dark, Offset(70f, 46f), Size(22f, 22f), style = Stroke(2.6f))
        drawCircle(p.dark, 2.2f, Offset(78f, 57f)); drawCircle(p.dark, 2.2f, Offset(85f, 57f))
        drawCircle(p.extra2, 2.8f, Offset(62f, 46f))
        // ranura y moneda
        drawRoundRect(p.dark, Offset(34f, 34f), Size(22f, 5f), CornerRadius(2.5f))
        val y = 6f + 12f * (0.5f + 0.5f * sin((t * 2 * PI).toFloat()))
        drawCircle(p.extra, 8f, Offset(45f, y + 4f))
        drawCircle(Color(0xFFD9961A), 8f, Offset(45f, y + 4f), style = Stroke(2f))
        // cola
        drawArc(p.dark, 90f, 240f, false, Offset(3f, 48f), Size(14f, 14f), style = Stroke(3f, cap = StrokeCap.Round))
    }
}

private fun DrawScope.sparkles(p: Pal, t: Float, flat: Boolean) {
    sparkle4(Offset(44f, 56f), 34f * (0.9f + 0.1f * tw(t, 0f)), if (flat) p.base else p.base)
    if (!flat) sparkle4(Offset(44f, 56f), 14f, p.extra, glow = false)
    sparkle4(Offset(78f, 22f), 15f * (0.7f + 0.3f * tw(t, 0.33f)), if (flat) p.base else p.light)
    sparkle4(Offset(80f, 80f), 11f * (0.7f + 0.3f * tw(t, 0.66f)), if (flat) p.base else p.light)
}

private fun DrawScope.party(p: Pal, t: Float, flat: Boolean) {
    shadow(40f, 94f, 30f, 3.5f, flat)
    val cone = Path().apply { moveTo(10f, 92f); lineTo(46f, 32f); lineTo(70f, 58f); close() }
    if (!flat) drawPath(cone, p.dark, style = Stroke(width = 8f, join = StrokeJoin.Round))
    drawPath(cone, vGrad(p.light, p.base, 32f, 92f))
    drawPath(cone, vGrad(p.light, p.base, 32f, 92f), style = Stroke(width = 4f, join = StrokeJoin.Round))
    if (!flat) {
        drawLine(p.extra, Offset(24f, 70f), Offset(46f, 80f), strokeWidth = 5f, cap = StrokeCap.Round)
        drawLine(p.extra2, Offset(33f, 54f), Offset(55f, 64f), strokeWidth = 5f, cap = StrokeCap.Round)
        val k = t * 2 * PI
        listOf(
            Triple(70f, 22f, p.extra), Triple(86f, 40f, p.extra2), Triple(58f, 10f, p.light), Triple(90f, 62f, p.extra)
        ).forEachIndexed { i, (x, y, c) ->
            val dy = (sin(k + i) * 2.5f).toFloat()
            drawCircle(c, 4.2f, Offset(x, y + dy))
        }
        sparkle4(Offset(78f, 14f), 7f * tw(t, 0.2f), p.light)
    }
}

private fun DrawScope.dice(p: Pal, t: Float, flat: Boolean) {
    shadow(50f, 94f, 30f, 3.5f, flat)
    rotate(10f, pivot = Offset(50f, 50f)) {
        val r = CornerRadius(20f)
        if (flat) {
            drawRoundRect(p.base, Offset(12f, 12f), Size(76f, 76f), r)
        } else {
            drawRoundRect(p.dark, Offset(12f, 12f), Size(76f, 76f), r)
            drawRoundRect(vGrad(Color.White, p.extra2, 14f, 86f), Offset(15f, 15f), Size(70f, 70f), CornerRadius(17f))
            val pip = p.extra
            listOf(
                Offset(33f, 33f), Offset(67f, 33f), Offset(50f, 50f), Offset(33f, 67f), Offset(67f, 67f)
            ).forEach { drawCircle(pip, 6.5f, it); drawCircle(Color.White.copy(alpha = 0.5f), 2f, Offset(it.x - 2f, it.y - 2f)) }
        }
    }
}

private fun DrawScope.crown(p: Pal, t: Float, flat: Boolean) {
    shadow(50f, 92f, 34f, 3.5f, flat)
    val path = Path().apply {
        moveTo(12f, 78f); lineTo(6f, 30f); lineTo(32f, 54f); lineTo(50f, 18f)
        lineTo(68f, 54f); lineTo(94f, 30f); lineTo(88f, 78f); close()
    }
    if (!flat) drawPath(path, p.dark, style = Stroke(width = 9f, join = StrokeJoin.Round))
    val fill = vGrad(p.light, p.base, 18f, 80f)
    drawPath(path, fill)
    drawPath(path, fill, style = Stroke(width = 5f, join = StrokeJoin.Round))
    drawRoundRect(vGrad(p.light, p.base, 76f, 90f), Offset(10f, 76f), Size(80f, 14f), CornerRadius(6f))
    if (!flat) {
        drawRoundRect(p.dark, Offset(10f, 76f), Size(80f, 14f), CornerRadius(6f), style = Stroke(2.6f))
        drawCircle(p.extra, 5.5f, Offset(30f, 83f)); drawCircle(p.extra2, 5.5f, Offset(50f, 83f)); drawCircle(p.extra, 5.5f, Offset(70f, 83f))
        listOf(Offset(6f, 28f), Offset(50f, 15f), Offset(94f, 28f)).forEach {
            drawCircle(Color.White, 5f, it); drawCircle(p.dark, 5f, it, style = Stroke(2f))
        }
        shine(28f, 44f, 8f, 20f, 10f, false, 0.45f)
        sparkle4(Offset(78f, 44f), 6f * tw(t, 0.4f), Color.White)
    }
}

// =====================================================================================
//  TEXTO CON ICONOS DENTRO: "+3 ◆", "120 ●", "8 ★" → ilustraciones cozy en línea
// =====================================================================================

private val glyphKinds = mapOf(
    '◆' to CozyKind.GEM, '●' to CozyKind.COIN, '★' to CozyKind.STAR,
    '❤' to CozyKind.HEART, '✨' to CozyKind.SPARKLES, '⭐' to CozyKind.STAR
)

/** Texto que cambia ◆ ● ★ por sus iconos cozy, a la altura de la letra. */
@Composable
fun CozyText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val iconSize = if (fontSize == TextUnit.Unspecified) 16.sp else (fontSize.value * 1.25f).sp
    val annotated = remember(text) {
        AnnotatedString.Builder().apply {
            text.forEach { ch ->
                if (ch == '\uFE0F') return@forEach
                if (glyphKinds.containsKey(ch)) appendInlineContent("g$ch", "$ch") else append(ch)
            }
        }.toAnnotatedString()
    }
    val inline = remember(text, iconSize) {
        glyphKinds.entries.filter { text.contains(it.key) }.associate { (ch, kind) ->
            "g$ch" to InlineTextContent(Placeholder(iconSize, iconSize, PlaceholderVerticalAlign.TextCenter)) {
                CozyIcon(kind, Modifier.size(iconSize.value.dp))
            }
        }
    }
    Text(
        text = annotated, inlineContent = inline, modifier = modifier, fontSize = fontSize, fontWeight = fontWeight,
        color = color, letterSpacing = letterSpacing, textAlign = textAlign, maxLines = maxLines, overflow = overflow
    )
}
