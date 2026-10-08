package com.korkoor.pardos.ui.menu

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material.icons.rounded.PlayArrow
import com.korkoor.pardos.ui.design.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.domain.model.LevelInfo
import com.korkoor.pardos.ui.design.Gold
import com.korkoor.pardos.ui.design.Navy
import com.korkoor.pardos.ui.design.Sage
import com.korkoor.pardos.ui.design.darker
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// =====================================================================================
//  MUNDOS DEL MAPA: cada capítulo tiene color, nombre y paisaje propios
// =====================================================================================

internal enum class SceneryKind { DUNES, PINES, RIVER, HILLS, CLOUDS, LAVENDER, LANTERNS, MOUNTAINS, CHERRY, GOLD_DUNES, MOON, BAMBOO }

internal data class ChapterTheme(val name: String, val color: Color, val scenery: SceneryKind)

internal val chapterThemes = listOf(
    ChapterTheme("Jardín de Arena", Color(0xFF6B9E86), SceneryKind.DUNES),
    ChapterTheme("Bosque Sereno", Color(0xFF3F9468), SceneryKind.PINES),
    ChapterTheme("Orilla del Río", Color(0xFF4E8FA6), SceneryKind.RIVER),
    ChapterTheme("Colinas de Té", Color(0xFF8DB04A), SceneryKind.HILLS),
    ChapterTheme("Faro en la Bruma", Color(0xFF7A8FB5), SceneryKind.CLOUDS),
    ChapterTheme("Valle de Lavanda", Color(0xFF8E6BD6), SceneryKind.LAVENDER),
    ChapterTheme("Mercado Nocturno", Color(0xFFE0782F), SceneryKind.LANTERNS),
    ChapterTheme("Pico Nevado", Color(0xFF5B8FC4), SceneryKind.MOUNTAINS),
    ChapterTheme("Isla de Cerezos", Color(0xFFE57A9A), SceneryKind.CHERRY),
    ChapterTheme("Desierto Dorado", Color(0xFFE0A93B), SceneryKind.GOLD_DUNES),
    ChapterTheme("Bahía Lunar", Color(0xFF4B5BA8), SceneryKind.MOON),
    ChapterTheme("Templo de Bambú", Color(0xFF3E9E7A), SceneryKind.BAMBOO)
)

internal fun chapterTheme(chapter: Int): ChapterTheme = chapterThemes[chapter % chapterThemes.size]

/**
 * Dibuja piezas de paisaje en el borde de una fila (a la izquierda o la derecha, lejos del camino).
 * Es determinista: la misma fila siempre se ve igual. [u] es la unidad de tamaño (alto de la fila).
 */
internal fun DrawScope.drawScenery(kind: SceneryKind, tone: Color, left: Boolean, seed: Int, scale: Float = 0.62f) {
    val r = Random(seed * 7919 + kind.ordinal * 131)
    val w = size.width
    val u = size.height
    val pieces = 1 + r.nextInt(2)
    repeat(pieces) { k ->
        val cx = if (left) w * (0.07f + r.nextFloat() * 0.15f) else w * (0.80f + r.nextFloat() * 0.13f)
        val cy = u * (0.30f + r.nextFloat() * 0.5f) + k * u * 0.12f
        val s = u * (0.55f + r.nextFloat() * 0.35f) * scale / 0.62f * 0.62f
        when (kind) {
            SceneryKind.DUNES, SceneryKind.GOLD_DUNES -> dune(cx, cy, s, tone, sun = kind == SceneryKind.GOLD_DUNES && r.nextBoolean())
            SceneryKind.PINES -> pine(cx, cy, s, tone)
            SceneryKind.RIVER -> river(cx, cy, s, tone, k)
            SceneryKind.HILLS -> hills(cx, cy, s, tone)
            SceneryKind.CLOUDS -> cloud(cx, cy, s)
            SceneryKind.LAVENDER -> lavender(cx, cy, s, tone, r)
            SceneryKind.LANTERNS -> lantern(cx, cy, s)
            SceneryKind.MOUNTAINS -> mountain(cx, cy, s, tone)
            SceneryKind.CHERRY -> cherry(cx, cy, s, tone, r)
            SceneryKind.MOON -> moon(cx, cy, s, r)
            SceneryKind.BAMBOO -> bamboo(cx, cy, s, tone)
        }
    }
}

private fun DrawScope.dune(cx: Float, cy: Float, s: Float, tone: Color, sun: Boolean) {
    if (sun) drawCircle(Color(0xFFFFE08A).copy(alpha = 0.45f), radius = s * 0.22f, center = Offset(cx + s * 0.1f, cy - s * 0.55f))
    val dw = s * 1.5f
    val dh = s * 0.55f
    val path = Path().apply {
        moveTo(cx - dw / 2, cy + dh / 2)
        quadraticTo(cx - dw * 0.12f, cy - dh * 1.0f, cx + dw * 0.1f, cy - dh * 0.25f)
        quadraticTo(cx + dw * 0.32f, cy + dh * 0.15f, cx + dw / 2, cy + dh / 2)
        close()
    }
    drawPath(path, tone.copy(alpha = 0.30f))
    // sombra de la cara oculta
    val shade = Path().apply {
        moveTo(cx + dw * 0.1f, cy - dh * 0.25f)
        quadraticTo(cx + dw * 0.32f, cy + dh * 0.15f, cx + dw / 2, cy + dh / 2)
        lineTo(cx + dw * 0.05f, cy + dh / 2)
        close()
    }
    drawPath(shade, tone.darker(0.7f).copy(alpha = 0.18f))
}

private fun DrawScope.pine(cx: Float, cy: Float, s: Float, tone: Color) {
    drawRect(Color(0xFF7A5A3A).copy(alpha = 0.45f), Offset(cx - s * 0.05f, cy + s * 0.32f), Size(s * 0.10f, s * 0.26f))
    for (k in 0..2) {
        val top = cy - s * 0.62f + k * s * 0.27f
        val half = s * (0.20f + k * 0.10f)
        val tri = Path().apply {
            moveTo(cx, top); lineTo(cx + half, top + s * 0.40f); lineTo(cx - half, top + s * 0.40f); close()
        }
        drawPath(tri, tone.copy(alpha = 0.40f + k * 0.07f))
    }
}

private fun DrawScope.river(cx: Float, cy: Float, s: Float, tone: Color, k: Int) {
    for (row in 0..2) {
        val path = Path()
        for (t in 0..22) {
            val x = cx - s * 0.7f + t * s * 0.064f
            val y = cy + row * s * 0.22f + sin(t * 0.55f + row + k) * s * 0.05f
            if (t == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, tone.copy(alpha = 0.42f - row * 0.08f), style = Stroke(width = s * 0.06f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    // nenúfar
    drawCircle(Color(0xFF7FC8A0).copy(alpha = 0.55f), radius = s * 0.11f, center = Offset(cx + s * 0.35f, cy + s * 0.44f))
    drawCircle(Color(0xFFFFB6C8).copy(alpha = 0.75f), radius = s * 0.045f, center = Offset(cx + s * 0.35f, cy + s * 0.42f))
}

private fun DrawScope.hills(cx: Float, cy: Float, s: Float, tone: Color) {
    // tres lomas superpuestas, de la más lejana (clara) a la más cercana (oscura)
    fun loma(x: Float, y: Float, w: Float, h: Float, c: Color) {
        val path = Path().apply {
            moveTo(x - w / 2, y)
            cubicTo(x - w * 0.32f, y - h * 1.25f, x + w * 0.32f, y - h * 1.25f, x + w / 2, y)
            close()
        }
        drawPath(path, c)
    }
    loma(cx + s * 0.25f, cy + s * 0.2f, s * 1.4f, s * 0.6f, lerp(tone, Color.White, 0.45f).copy(alpha = 0.28f))
    loma(cx - s * 0.2f, cy + s * 0.32f, s * 1.3f, s * 0.55f, tone.copy(alpha = 0.30f))
    loma(cx + s * 0.1f, cy + s * 0.46f, s * 1.1f, s * 0.42f, tone.darker(0.85f).copy(alpha = 0.30f))
    // arbustos de té
    for (k in 0..3) {
        drawCircle(tone.darker(0.7f).copy(alpha = 0.30f), s * 0.05f, Offset(cx - s * 0.25f + k * s * 0.17f, cy + s * 0.34f + (k % 2) * s * 0.05f))
    }
}

private fun DrawScope.cloud(cx: Float, cy: Float, s: Float) {
    val c = Color.White
    drawCircle(c.copy(alpha = 0.55f), s * 0.20f, Offset(cx, cy))
    drawCircle(c.copy(alpha = 0.55f), s * 0.27f, Offset(cx + s * 0.22f, cy + s * 0.04f))
    drawCircle(c.copy(alpha = 0.55f), s * 0.19f, Offset(cx + s * 0.46f, cy + s * 0.07f))
    drawRoundRect(c.copy(alpha = 0.55f), Offset(cx - s * 0.18f, cy + s * 0.06f), Size(s * 0.84f, s * 0.22f), CornerRadius(s * 0.11f))
}

private fun DrawScope.lavender(cx: Float, cy: Float, s: Float, tone: Color, r: Random) {
    for (k in 0..4) {
        val x = cx + (k - 2) * s * 0.17f
        val sway = (r.nextFloat() - 0.5f) * s * 0.12f
        drawLine(Color(0xFF6FA06E).copy(alpha = 0.45f), Offset(x, cy + s * 0.55f), Offset(x + sway, cy - s * 0.12f), strokeWidth = s * 0.035f, cap = StrokeCap.Round)
        for (b in 0..3) {
            val t = b / 3f
            drawOval(
                tone.copy(alpha = 0.60f), Offset(x + sway * (1 - t) - s * 0.045f, cy - s * 0.12f + t * s * 0.3f),
                Size(s * 0.09f, s * 0.12f)
            )
        }
    }
}

private fun DrawScope.lantern(cx: Float, cy: Float, s: Float) {
    val glow = Color(0xFFFF9A3C)
    drawLine(Navy.copy(alpha = 0.25f), Offset(cx, -s * 0.2f), Offset(cx, cy - s * 0.12f), strokeWidth = s * 0.02f)
    drawCircle(glow.copy(alpha = 0.16f), s * 0.55f, Offset(cx, cy + s * 0.1f))
    drawRoundRect(glow.copy(alpha = 0.78f), Offset(cx - s * 0.17f, cy - s * 0.12f), Size(s * 0.34f, s * 0.44f), CornerRadius(s * 0.14f))
    drawRoundRect(Color(0xFFB8470F).copy(alpha = 0.6f), Offset(cx - s * 0.12f, cy - s * 0.16f), Size(s * 0.24f, s * 0.06f), CornerRadius(s * 0.03f))
    drawRoundRect(Color(0xFFB8470F).copy(alpha = 0.6f), Offset(cx - s * 0.12f, cy + s * 0.30f), Size(s * 0.24f, s * 0.06f), CornerRadius(s * 0.03f))
    drawLine(Color.White.copy(alpha = 0.55f), Offset(cx - s * 0.07f, cy - s * 0.02f), Offset(cx - s * 0.07f, cy + s * 0.22f), strokeWidth = s * 0.03f, cap = StrokeCap.Round)
}

private fun DrawScope.mountain(cx: Float, cy: Float, s: Float, tone: Color) {
    val body = Path().apply {
        moveTo(cx - s * 0.7f, cy + s * 0.45f); lineTo(cx, cy - s * 0.55f); lineTo(cx + s * 0.7f, cy + s * 0.45f); close()
    }
    drawPath(body, tone.copy(alpha = 0.34f))
    val side = Path().apply {
        moveTo(cx, cy - s * 0.55f); lineTo(cx + s * 0.7f, cy + s * 0.45f); lineTo(cx + s * 0.1f, cy + s * 0.45f); close()
    }
    drawPath(side, tone.darker(0.7f).copy(alpha = 0.18f))
    val cap = Path().apply {
        moveTo(cx, cy - s * 0.55f); lineTo(cx + s * 0.19f, cy - s * 0.2f); lineTo(cx + s * 0.06f, cy - s * 0.26f)
        lineTo(cx - s * 0.04f, cy - s * 0.17f); lineTo(cx - s * 0.19f, cy - s * 0.2f); close()
    }
    drawPath(cap, Color.White.copy(alpha = 0.75f))
}

private fun DrawScope.cherry(cx: Float, cy: Float, s: Float, tone: Color, r: Random) {
    drawLine(Color(0xFF7A5A3A).copy(alpha = 0.45f), Offset(cx, cy + s * 0.55f), Offset(cx + s * 0.03f, cy + s * 0.05f), strokeWidth = s * 0.08f, cap = StrokeCap.Round)
    drawLine(Color(0xFF7A5A3A).copy(alpha = 0.40f), Offset(cx + s * 0.02f, cy + s * 0.2f), Offset(cx + s * 0.22f, cy - s * 0.05f), strokeWidth = s * 0.05f, cap = StrokeCap.Round)
    listOf(
        Offset(0f, -0.1f) to 0.26f, Offset(-0.2f, 0.02f) to 0.2f, Offset(0.22f, -0.02f) to 0.22f,
        Offset(0.05f, -0.28f) to 0.2f, Offset(-0.12f, -0.22f) to 0.16f
    ).forEach { (o, rad) ->
        drawCircle(tone.copy(alpha = 0.42f), s * rad, Offset(cx + o.x * s, cy + o.y * s))
    }
    repeat(5) {
        drawCircle(Color.White.copy(alpha = 0.6f), s * 0.025f, Offset(cx + (r.nextFloat() - 0.5f) * s * 0.7f, cy - s * 0.3f + r.nextFloat() * s * 0.5f))
    }
}

private fun DrawScope.moon(cx: Float, cy: Float, s: Float, r: Random) {
    drawCircle(Color(0xFFFFF4C2).copy(alpha = 0.18f), s * 0.5f, Offset(cx, cy))
    drawCircle(Color(0xFFFFF4C2).copy(alpha = 0.6f), s * 0.3f, Offset(cx, cy))
    drawCircle(Color(0xFFE6D58F).copy(alpha = 0.5f), s * 0.06f, Offset(cx - s * 0.08f, cy - s * 0.05f))
    drawCircle(Color(0xFFE6D58F).copy(alpha = 0.5f), s * 0.04f, Offset(cx + s * 0.1f, cy + s * 0.1f))
    repeat(3) {
        val sx = cx + (r.nextFloat() - 0.5f) * s * 1.3f
        val sy = cy + (r.nextFloat() - 0.2f) * s * 0.9f
        val a = s * 0.07f
        drawLine(Color.White.copy(alpha = 0.7f), Offset(sx - a, sy), Offset(sx + a, sy), strokeWidth = s * 0.02f, cap = StrokeCap.Round)
        drawLine(Color.White.copy(alpha = 0.7f), Offset(sx, sy - a), Offset(sx, sy + a), strokeWidth = s * 0.02f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.bamboo(cx: Float, cy: Float, s: Float, tone: Color) {
    for (k in 0..2) {
        val x = cx + (k - 1) * s * 0.2f
        val top = cy - s * 0.55f + k * s * 0.07f
        val stalkW = s * 0.085f
        drawRoundRect(tone.copy(alpha = 0.40f), Offset(x - stalkW / 2, top), Size(stalkW, s * 1.1f), CornerRadius(stalkW / 2))
        var y = top + s * 0.25f
        while (y < top + s * 1.05f) {
            drawLine(tone.darker(0.6f).copy(alpha = 0.35f), Offset(x - stalkW / 2, y), Offset(x + stalkW / 2, y), strokeWidth = s * 0.02f)
            y += s * 0.28f
        }
        drawOval(tone.copy(alpha = 0.45f), Offset(x + stalkW / 2, top + s * 0.1f), Size(s * 0.2f, s * 0.07f))
    }
}

// =====================================================================================
//  NODOS: gemas de gelatina; los retos son fichas; el nivel actual brilla con ondas
// =====================================================================================

@Composable
internal fun MapNode(
    level: LevelInfo, nextUnlocked: Boolean, isLast: Boolean, isCurrent: Boolean,
    justUnlocked: Boolean = false, distanceAhead: Int = 0,
    friends: List<com.korkoor.pardos.domain.model.UserProfile> = emptyList(),
    onFriendsClick: (String) -> Unit = {},
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val avatarId = if (isCurrent) remember { com.korkoor.pardos.data.local.ProfileManager(context).getProfile().avatarId } else 0
    // Al desbloquear un nivel: rebote de entrada y estallido de destellos
    val pop = remember { Animatable(if (justUnlocked) 0f else 1f) }
    val burst = remember { Animatable(if (justUnlocked) 0f else 1f) }
    LaunchedEffect(justUnlocked) {
        if (justUnlocked) {
            pop.snapTo(0f); burst.snapTo(0f)
            kotlinx.coroutines.coroutineScope {
                launch { pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessLow)) }
                launch { burst.animateTo(1f, tween(1400)) }
            }
        }
    }
    val chapter = chapterOf(level.id)
    val theme = chapterTheme(chapter)
    val color = theme.color
    val locked = level.isLocked
    val completed = !locked && level.starsEarned > 0
    val isChallenge = level.id % 5 == 0
    val isEpic = level.id % 25 == 0

    val myX = nodeX(level.id)
    val nextX = nodeX(level.id + 1)
    val segmentDone = nextUnlocked
    val nightChapter = chapterIsNight(chapter)
    val endsAtBanner = level.id % CHAPTER_SIZE == 0

    val clock = rememberInfiniteTransition(label = "node")
    val ripple by clock.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "ripple")
    val bob by clock.animateFloat(0f, 1f, infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    val pulse by clock.animateFloat(
        1f, if (isCurrent) 1.07f else 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse"
    )

    val nodeSize = if (isCurrent) 70.dp else if (isChallenge) 60.dp else 56.dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .drawBehind {
                // 1) Paisaje del capítulo, en el lado contrario al camino
                drawScenery(theme.scenery, color, left = myX > 0.5f, seed = level.id)

                // sombra de suelo bajo el nodo: le da peso
                if (!locked) {
                    val nr = nodeSize.toPx() / 2f
                    drawOval(
                        Color.Black.copy(alpha = 0.10f),
                        Offset(size.width * myX - nr * 0.9f, size.height / 2f + nr * 0.78f), Size(nr * 1.8f, nr * 0.45f)
                    )
                }

                // 2) Camino hacia el nivel siguiente (arriba)
                if (!isLast) {
                    val x0 = size.width * myX
                    val x1 = size.width * nextX
                    val y0 = size.height / 2f
                    val y1 = -size.height / 2f
                    val xEnd = if (endsAtBanner) x0 else x1
                    val path = Path().apply {
                        moveTo(x0, y0)
                        cubicTo(x0, y0 + (y1 - y0) * 0.55f, xEnd, y1 - (y1 - y0) * 0.55f, xEnd, y1)
                    }
                    if (segmentDone) {
                        // camino recorrido: borde claro + relleno de color + brillo
                        drawPath(path, Color.White.copy(alpha = 0.75f), style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round))
                        drawPath(path, color.copy(alpha = 0.9f), style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
                        drawPath(path, Color.White.copy(alpha = 0.35f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                        // una lucecita que viaja por el camino recorrido
                        val measure = PathMeasure().also { it.setPath(path, false) }
                        val frac = (ripple + (level.id * 0.37f) % 1f) % 1f
                        val pos = measure.getPosition(measure.length * frac)
                        drawCircle(Color.White.copy(alpha = 0.35f), 7.dp.toPx(), pos)
                        drawCircle(Color.White, 3.dp.toPx(), pos)
                    } else {
                        drawPath(
                            path, if (nightChapter) Color.White.copy(alpha = 0.30f) else Navy.copy(alpha = 0.16f),
                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 14.dp.toPx())))
                        )
                    }
                }

                // estallido de destellos al desbloquear
                if (burst.value < 1f && justUnlocked) {
                    val c = Offset(size.width * myX, size.height / 2f)
                    val b = burst.value
                    for (i in 0 until 14) {
                        val a = (i / 14f) * 2f * PI.toFloat()
                        val r = nodeSize.toPx() * (0.6f + 1.4f * b)
                        val col = if (i % 3 == 0) Color(0xFFFFE08A) else if (i % 3 == 1) color else Color.White
                        drawCircle(col.copy(alpha = (1f - b)), (4.dp.toPx() * (1f - b * 0.6f)), Offset(c.x + cos(a) * r, c.y + sin(a) * r))
                    }
                    drawCircle(Color(0xFFFFE08A).copy(alpha = 0.45f * (1f - b)), nodeSize.toPx() * (0.5f + b * 1.2f), c)
                }

                // 3) Ondas de luz alrededor del nivel actual
                if (isCurrent) {
                    val c = Offset(size.width * myX, size.height / 2f)
                    val base = nodeSize.toPx() / 2f
                    for (i in 0..1) {
                        val p = (ripple + i * 0.5f) % 1f
                        drawCircle(color.copy(alpha = 0.38f * (1f - p)), radius = base * (1f + p * 1.0f), center = c, style = Stroke(width = 3.dp.toPx()))
                    }
                    drawCircle(color.copy(alpha = 0.14f), radius = base * 1.45f, center = c)
                }
            }
    ) {
        val xDp = maxWidth * myX - nodeSize / 2
        val shape = if (isChallenge && !isCurrent) RoundedCornerShape(nodeSize * 0.3f) else CircleShape

        val fill = when {
            locked -> Brush.verticalGradient(listOf(Color(0xFFEAE7E0), Color(0xFFD5D1C7)))
            isCurrent -> Brush.verticalGradient(listOf(lerp(color, Color.White, 0.25f), color, color.darker(0.78f)))
            completed -> Brush.verticalGradient(listOf(lerp(color, Color.White, 0.3f), color, color.darker(0.82f)))
            else -> Brush.verticalGradient(listOf(Color.White, Color(0xFFF1EEE6)))
        }
        val numberColor = if (locked) Navy.copy(alpha = 0.3f) else if (completed || isCurrent) Color.White else Navy

        Box(
            modifier = Modifier
                .offset(x = xDp, y = (ROW_HEIGHT - nodeSize) / 2)
                .size(nodeSize)
                .scale((if (isCurrent) pulse else 1f) * pop.value)
                .then(if (locked && distanceAhead > 3) Modifier.alpha((1f - (distanceAhead - 3) * 0.12f).coerceIn(0.45f, 1f)) else Modifier)
                .then(
                    when {
                        isCurrent -> Modifier.shadow(18.dp, shape, spotColor = color, ambientColor = color)
                        completed -> Modifier.shadow(9.dp, shape, spotColor = color)
                        !locked -> Modifier.shadow(6.dp, shape)
                        else -> Modifier
                    }
                )
                .clip(shape)
                .background(fill)
                .border(
                    width = if (isEpic) 3.dp else 2.dp,
                    color = when {
                        isEpic && !locked -> Gold
                        isCurrent -> Color.White
                        completed -> Color.White.copy(alpha = 0.55f)
                        else -> Navy.copy(alpha = 0.08f)
                    },
                    shape = shape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (!locked) {
                // brillo de gelatina arriba
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.38f), 0.5f to Color.Transparent)))
            }
            if (isCurrent && avatarId != 0) {
                // tu avatar marca dónde estás
                com.korkoor.pardos.ui.profile.AvatarImage(
                    avatarId,
                    modifier = Modifier.fillMaxSize().padding(5.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f))
                )
            } else if (locked) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = Navy.copy(alpha = 0.28f), modifier = Modifier.size(22.dp))
            } else {
                Text(
                    text = "${level.id}",
                    fontSize = if (level.id >= 1000) 14.sp else if (isCurrent) 22.sp else 18.sp,
                    fontWeight = FontWeight.Black,
                    color = numberColor
                )
            }
        }

        if (isCurrent) {
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * myX + nodeSize / 2 - 22.dp, y = (ROW_HEIGHT + nodeSize) / 2 - 22.dp)
                    .defaultMinSize(minWidth = 24.dp).height(24.dp)
                    .shadow(4.dp, CircleShape).background(color.darker(0.8f), CircleShape).border(2.dp, Color.White, CircleShape)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) { Text("${level.id}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White) }
        }

        // Corona en los niveles épicos
        if (isEpic && !locked) {
            Icon(
                Icons.Rounded.WorkspacePremium, contentDescription = null, tint = Gold,
                modifier = Modifier.offset(x = maxWidth * myX - 11.dp, y = (ROW_HEIGHT - nodeSize) / 2 - 18.dp).size(22.dp)
            )
        }

        // Estrellas bajo el nodo: la del centro un poco más alta
        if (completed) {
            Row(
                modifier = Modifier.offset(x = maxWidth * myX - 22.dp, y = (ROW_HEIGHT + nodeSize) / 2 - 3.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { i ->
                    var shown by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { delay(80L + i * 130L); shown = true }
                    val sc by animateFloatAsState(
                        if (shown) 1f else 0f, spring(dampingRatio = 0.35f, stiffness = 260f), label = "starPop"
                    )
                    Icon(
                        Icons.Rounded.Star, contentDescription = null,
                        tint = if (i < level.starsEarned) Color(0xFFFFC83D) else Navy.copy(alpha = 0.14f),
                        modifier = Modifier.size(if (i == 1) 17.dp else 14.dp).offset(y = if (i == 1) (-2).dp else 1.dp).scale(sc)
                    )
                }
            }
        }

        // Insignia de reto cronometrado
        if (isChallenge && !locked) {
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * myX + nodeSize / 2 - 16.dp, y = (ROW_HEIGHT - nodeSize) / 2 - 5.dp)
                    .size(22.dp)
                    .shadow(3.dp, CircleShape)
                    .background(Color(0xFFE07A5F), CircleShape)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
            }
        }

        // Amigos que van en este nivel: sus avatares asoman junto al nodo
        if (friends.isNotEmpty()) {
            val onRight = myX < 0.5f
            val pin = 30.dp
            val shown = friends.take(3)
            val groupW = pin + (pin * 0.6f) * (shown.size - 1)
            val gx = if (isCurrent) maxWidth * myX + nodeSize / 2 - 6.dp
                else if (onRight) maxWidth * myX + nodeSize / 2 + 6.dp
                else maxWidth * myX - nodeSize / 2 - 6.dp - groupW
            val gy = if (isCurrent) (ROW_HEIGHT - nodeSize) / 2 - 24.dp else (ROW_HEIGHT - pin) / 2
            Box(
                Modifier.offset(x = gx, y = gy).width(groupW + if (friends.size > 3) 22.dp else 0.dp).height(pin)
                    .clickable { onFriendsClick(friends.joinToString(" y ") { it.name } + (if (friends.size == 1) " está aquí" else " están aquí")) }
            ) {
                shown.forEachIndexed { i, f ->
                    com.korkoor.pardos.ui.profile.AvatarFramed(
                        f.avatarId, Modifier.offset(x = (pin * 0.6f) * i).size(pin).shadow(4.dp, CircleShape), ring = 2.dp, animate = false
                    )
                }
                if (friends.size > 3) {
                    Text("+${friends.size - 3}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Navy, modifier = Modifier.align(Alignment.CenterEnd))
                }
            }
        }

        // Etiqueta "JUGAR" que rebota junto al nivel actual
        if (isCurrent) {
            val labelOnRight = myX < 0.5f
            val labelW = 74.dp
            Row(
                modifier = Modifier
                    .offset(
                        x = if (labelOnRight) maxWidth * myX + nodeSize / 2 + 8.dp else maxWidth * myX - nodeSize / 2 - 8.dp - labelW,
                        y = (ROW_HEIGHT - 28.dp) / 2 + (bob * 4f - 2f).dp
                    )
                    .width(labelW).height(28.dp)
                    .shadow(6.dp, RoundedCornerShape(14.dp), spotColor = color)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(color, color.darker(0.82f))))
                    .clickable(onClick = onClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                Text("JUGAR", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.sp)
            }
        }
    }
}

// =====================================================================================
//  BANNER DE CAPÍTULO
// =====================================================================================

@Composable
internal fun ChapterBanner(banner: MapItem.Banner) {
    val theme = chapterTheme(banner.chapter)
    val color = theme.color
    val complete = banner.completed >= CHAPTER_SIZE
    val shape = RoundedCornerShape(28.dp)
    androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth()) {
    ChapterLandmark(banner.chapter, Modifier.padding(top = 10.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 18.dp)
            .shadow(12.dp, shape, spotColor = color)
            .clip(shape)
            .background(Brush.linearGradient(listOf(lerp(color, Color.White, 0.12f), color.darker(0.72f))))
            .then(if (complete) Modifier.border(2.5.dp, Color(0xFFFFE08A), shape) else Modifier)
    ) {
        Text(
            "${banner.chapter + 1}", fontSize = 96.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.10f),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 18.dp)
        )
        Column(Modifier.padding(horizontal = 22.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("CAPÍTULO ${banner.chapter + 1}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.78f), letterSpacing = 3.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(theme.name, fontSize = 21.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
                if (complete) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFFFFE08A), modifier = Modifier.size(30.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            // barra de avance del capítulo
            Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f))) {
                Box(
                    Modifier.fillMaxHeight()
                        .fillMaxWidth((banner.completed.toFloat() / CHAPTER_SIZE).coerceIn(0f, 1f).coerceAtLeast(0.02f))
                        .background(Color.White, CircleShape)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Star, contentDescription = null, tint = Color(0xFFFFE08A), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("${banner.stars}/${banner.maxStars}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                Spacer(Modifier.weight(1f))
                Text("${banner.completed}/$CHAPTER_SIZE niveles", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.85f))
            }
        }
    }
    }
}

// =====================================================================================
//  COFRE DEL CAPÍTULO (dibujado)
// =====================================================================================

@Composable
internal fun ChapterChest(chest: MapItem.Chest, onClick: () -> Unit) {
    val theme = chapterTheme(chest.chapter)
    val state = when {
        chest.claimed -> 2
        chest.ready -> 1
        else -> 0
    }
    val lineColor = if (chest.ready) theme.color.copy(alpha = 0.9f) else if (chapterIsNight(chest.chapter)) Color.White.copy(alpha = 0.30f) else Navy.copy(alpha = 0.16f)
    val x = nodeX(com.korkoor.pardos.domain.rewards.ChapterRewards.lastLevelOf(chest.chapter))

    val clock = rememberInfiniteTransition(label = "chest")
    val bounce by clock.animateFloat(
        0f, if (state == 1) -7f else 0f, infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bounce"
    )
    val spin by clock.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "spin")

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .drawBehind {
                drawScenery(theme.scenery, theme.color, left = x > 0.5f, seed = chest.chapter * 97 + 5)
                val x0 = size.width * x
                if (chest.ready) {
                    drawLine(Color.White.copy(alpha = 0.75f), Offset(x0, size.height / 2f), Offset(x0, 0f), strokeWidth = 14.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(lineColor, Offset(x0, size.height / 2f), Offset(x0, 0f), strokeWidth = 8.dp.toPx(), cap = StrokeCap.Round)
                } else {
                    drawLine(
                        lineColor, Offset(x0, size.height / 2f), Offset(x0, 0f), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 14.dp.toPx()))
                    )
                }
                // rayos de luz detrás del cofre listo
                if (state == 1) {
                    val c = Offset(x0, size.height / 2f)
                    rotate(spin, pivot = c) {
                        for (i in 0 until 8) {
                            val a = (i * 45f) * PI.toFloat() / 180f
                            drawLine(
                                Gold.copy(alpha = 0.35f), Offset(c.x + cos(a) * 38.dp.toPx(), c.y + sin(a) * 38.dp.toPx()),
                                Offset(c.x + cos(a) * 62.dp.toPx(), c.y + sin(a) * 62.dp.toPx()), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round
                            )
                        }
                    }
                    drawCircle(Gold.copy(alpha = 0.18f), radius = 46.dp.toPx(), center = c)
                }
            }
    ) {
        val size = 84.dp
        Box(
            modifier = Modifier
                .offset(x = maxWidth * x - size / 2, y = (ROW_HEIGHT - size) / 2 + bounce.dp)
                .size(size)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            com.korkoor.pardos.ui.design.TreasureChest(
                type = if (chest.chapter >= 3) com.korkoor.pardos.domain.collection.ChestType.RARE else com.korkoor.pardos.domain.collection.ChestType.COMMON,
                state = when (state) { 1 -> com.korkoor.pardos.ui.design.ChestState.READY; 2 -> com.korkoor.pardos.ui.design.ChestState.OPEN; else -> com.korkoor.pardos.ui.design.ChestState.LOCKED },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
