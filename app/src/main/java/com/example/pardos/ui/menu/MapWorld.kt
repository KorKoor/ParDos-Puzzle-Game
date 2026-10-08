package com.korkoor.pardos.ui.menu

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.unit.dp
import com.korkoor.pardos.domain.shop.ParticleKind
import com.korkoor.pardos.ui.design.Paper
import com.korkoor.pardos.ui.design.darker
import com.korkoor.pardos.ui.game.components.AmbientParticles
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

// =====================================================================================
//  FONDO VIVO DEL MAPA: cielo del capítulo, sol o luna, nubes, relieve con parallax y partículas
// =====================================================================================

private fun particlesFor(kind: SceneryKind): ParticleKind = when (kind) {
    SceneryKind.DUNES, SceneryKind.GOLD_DUNES -> ParticleKind.SAND
    SceneryKind.PINES, SceneryKind.HILLS, SceneryKind.BAMBOO -> ParticleKind.LEAVES
    SceneryKind.RIVER -> ParticleKind.BUBBLES
    SceneryKind.CLOUDS, SceneryKind.MOUNTAINS -> ParticleKind.SNOW
    SceneryKind.LAVENDER -> ParticleKind.FIREFLIES
    SceneryKind.LANTERNS -> ParticleKind.EMBERS
    SceneryKind.CHERRY -> ParticleKind.PETALS
    SceneryKind.MOON -> ParticleKind.STARS
    SceneryKind.GRAVEYARD, SceneryKind.HAUNTED -> ParticleKind.BATS
    SceneryKind.DEAD_FOREST -> ParticleKind.FIREFLIES
    SceneryKind.PUMPKINS -> ParticleKind.EMBERS
}

/** En Noche de brujas todos los mundos son de noche (menos el faro, que es de niebla). */
private fun isNight(kind: SceneryKind) =
    kind == SceneryKind.MOON || kind == SceneryKind.LANTERNS || kind == SceneryKind.GRAVEYARD || kind == SceneryKind.HAUNTED ||
        kind == SceneryKind.DEAD_FOREST || (com.korkoor.pardos.ui.design.Season.halloween && kind != SceneryKind.CLOUDS)

/** ¿El capítulo es de noche? (la cabecera cambia a texto claro). */
internal fun chapterIsNight(chapter: Int) = isNight(chapterTheme(chapter).scenery)

/** Color de lo más alto del cielo del capítulo (para que la cabecera se funda con él). */
internal fun skyTopColor(chapter: Int): Color {
    val theme = chapterTheme(chapter)
    return if (isNight(theme.scenery)) lerp(theme.color, Color(0xFF0E1230), 0.78f) else lerp(theme.color, Color.White, 0.80f)
}

/**
 * Capa fija detrás del mapa. [scrollPx] es lo que lleva recorrido el mapa: mueve el relieve más despacio
 * que los nodos, y eso da profundidad.
 */
@Composable
fun MapBackdrop(chapter: Int, scrollPx: () -> Float, modifier: Modifier = Modifier) {
    val theme = chapterTheme(chapter)
    val night = isNight(theme.scenery)

    val spooky = com.korkoor.pardos.ui.design.Season.halloween
    val top by animateColorAsState(
        if (night) lerp(theme.color, if (spooky) Color(0xFF14082E) else Color(0xFF0E1230), 0.80f) else lerp(theme.color, Color.White, 0.80f), tween(900), label = "skyTop"
    )
    val bottom by animateColorAsState(
        if (night) lerp(theme.color, if (spooky) Color(0xFF3A1450) else Color(0xFF1B2250), 0.58f) else lerp(theme.color, Paper, 0.72f), tween(900), label = "skyBottom"
    )
    val tint by animateColorAsState(theme.color, tween(900), label = "skyTint")

    val clock = rememberInfiniteTransition(label = "world")
    val cloudsA by clock.animateFloat(0f, 1f, infiniteRepeatable(tween(70_000, easing = LinearEasing)), label = "cloudA")
    val cloudsB by clock.animateFloat(0f, 1f, infiniteRepeatable(tween(110_000, easing = LinearEasing)), label = "cloudB")
    val sunBob by clock.animateFloat(0f, 1f, infiniteRepeatable(tween(5200, easing = LinearEasing), RepeatMode.Reverse), label = "sun")

    Box(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(top, bottom)))) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // sol o luna
            val sc = Offset(w * 0.80f, h * 0.13f + sin(sunBob * PI.toFloat()) * 6.dp.toPx())
            if (night) {
                val moonTint = if (spooky) Color(0xFFFFD28A) else Color(0xFFFFF4C2)
                val moonR = if (spooky) 32.dp.toPx() else 24.dp.toPx()
                drawCircle(moonTint.copy(alpha = if (spooky) 0.22f else 0.16f), (if (spooky) 84.dp else 62.dp).toPx(), sc)
                drawCircle(moonTint.copy(alpha = 0.95f), moonR, sc)
                drawCircle(Color(0xFFE6D58F).copy(alpha = 0.5f), 5.dp.toPx(), Offset(sc.x - 7.dp.toPx(), sc.y - 4.dp.toPx()))
                drawCircle(Color(0xFFE6D58F).copy(alpha = 0.5f), 3.5f.dp.toPx(), Offset(sc.x + 8.dp.toPx(), sc.y + 7.dp.toPx()))
            } else {
                drawCircle(Color(0xFFFFE08A).copy(alpha = 0.22f), 74.dp.toPx(), sc)
                drawCircle(Color(0xFFFFE08A).copy(alpha = 0.38f), 48.dp.toPx(), sc)
                drawCircle(Color(0xFFFFEFB0).copy(alpha = 0.95f), 26.dp.toPx(), sc)
            }

            // nubes que cruzan lentamente (más claras de día, tenues de noche)
            fun cloud(xFrac: Float, y: Float, s: Float, alpha: Float) {
                val x = (xFrac * (w + 260.dp.toPx())) - 130.dp.toPx()
                val c = (if (night) Color(0xFFB8C0F0) else Color.White).copy(alpha = alpha)
                drawCircle(c, 16.dp.toPx() * s, Offset(x, y))
                drawCircle(c, 22.dp.toPx() * s, Offset(x + 20.dp.toPx() * s, y + 3.dp.toPx() * s))
                drawCircle(c, 15.dp.toPx() * s, Offset(x + 42.dp.toPx() * s, y + 5.dp.toPx() * s))
                drawRoundRect(c, Offset(x - 14.dp.toPx() * s, y + 4.dp.toPx() * s), Size(70.dp.toPx() * s, 18.dp.toPx() * s), CornerRadius(9.dp.toPx() * s))
            }
            cloud(cloudsA, h * 0.20f, 1.0f, if (night) 0.18f else 0.65f)
            cloud((cloudsA + 0.52f) % 1f, h * 0.41f, 0.7f, if (night) 0.12f else 0.45f)
            cloud(cloudsB, h * 0.63f, 1.25f, if (night) 0.10f else 0.38f)

            // relieve en dos capas con parallax: bandas onduladas que suben y bajan con el scroll
            fun ridge(speed: Float, baseFrac: Float, amp: Float, period: Float, alpha: Float, color: Color) {
                val shift = (scrollPx() * speed) % period
                var y = -period + shift + h * baseFrac % period
                while (y < h + period) {
                    val p = Path().apply {
                        moveTo(0f, y + period)
                        var x = 0f
                        lineTo(0f, y)
                        while (x <= w + 20f) {
                            lineTo(x, y + sin(x / w * 2 * PI.toFloat() * 1.3f) * amp)
                            x += 24f
                        }
                        lineTo(w, y + period); close()
                    }
                    drawPath(p, color.copy(alpha = alpha))
                    y += period
                }
            }
            ridge(0.10f, 0.30f, 16.dp.toPx(), 320.dp.toPx(), 0.07f, tint)
            ridge(0.22f, 0.62f, 22.dp.toPx(), 240.dp.toPx(), 0.08f, tint.darker(0.75f))
        }
        // partículas del mundo (fijas en pantalla)
        AmbientParticles(kind = particlesFor(theme.scenery), tint = lerp(tint, Color.White, if (night) 0.4f else 0.1f), density = 0.55f)
        // Noche de brujas: murciélagos que cruzan el cielo en todos los mundos
        if (spooky && particlesFor(theme.scenery) != ParticleKind.BATS) AmbientParticles(kind = ParticleKind.BATS, tint = Color(0xFF120726), density = 0.45f)
    }
}

// =====================================================================================
//  MONUMENTOS: una ilustración grande por capítulo, sobre el banner
// =====================================================================================

@Composable
internal fun ChapterLandmark(chapter: Int, modifier: Modifier = Modifier) {
    val theme = chapterTheme(chapter)
    val clock = rememberInfiniteTransition(label = "landmark")
    val t by clock.animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "t")
    Canvas(modifier.fillMaxWidth().height(116.dp)) {
        val s = size.height * 0.95f
        val onRight = chapter % 2 == 0
        val cx = if (onRight) size.width * 0.78f else size.width * 0.22f
        landmark(theme.scenery, theme.color, cx, size.height, s, t)
    }
}

private val wood = Color(0xFF8A5A33)
private val woodLight = Color(0xFFB98456)
private val cream = Color(0xFFFFF6E0)

private fun DrawScope.ground(cx: Float, base: Float, s: Float, tone: Color) {
    drawOval(tone.darker(0.7f).copy(alpha = 0.22f), Offset(cx - s * 0.62f, base - s * 0.07f), Size(s * 1.24f, s * 0.14f))
}

private fun DrawScope.curvedRoof(cx: Float, y: Float, w: Float, h: Float, color: Color) {
    val p = Path().apply {
        moveTo(cx - w / 2 - w * 0.08f, y + h)
        quadraticTo(cx - w * 0.25f, y + h * 0.9f, cx, y)
        quadraticTo(cx + w * 0.25f, y + h * 0.9f, cx + w / 2 + w * 0.08f, y + h)
        close()
    }
    drawPath(p, color)
    drawPath(p, color.darker(0.7f), style = Stroke(width = w * 0.02f, join = StrokeJoin.Round))
}

private fun DrawScope.landmark(kind: SceneryKind, tone: Color, cx: Float, base: Float, s: Float, t: Float) {
    ground(cx, base, s, tone)
    when (kind) {
        SceneryKind.DUNES -> zenGarden(cx, base, s, tone)
        SceneryKind.PINES -> cabin(cx, base, s)
        SceneryKind.RIVER -> bridge(cx, base, s, tone, t)
        SceneryKind.HILLS -> teaHouse(cx, base, s, tone)
        SceneryKind.CLOUDS -> lighthouse(cx, base, s, t)
        SceneryKind.LAVENDER -> windmill(cx, base, s, tone, t)
        SceneryKind.LANTERNS -> marketStall(cx, base, s, t)
        SceneryKind.MOUNTAINS -> peak(cx, base, s, tone, t)
        SceneryKind.CHERRY -> torii(cx, base, s, tone)
        SceneryKind.GOLD_DUNES -> pyramids(cx, base, s, tone)
        SceneryKind.MOON -> sailboat(cx, base, s, tone, t)
        SceneryKind.BAMBOO -> pagoda(cx, base, s, tone)
        SceneryKind.GRAVEYARD -> graveyardGate(cx, base, s, tone, t)
        SceneryKind.DEAD_FOREST -> hauntedForest(cx, base, s, tone, t)
        SceneryKind.PUMPKINS -> bigPumpkin(cx, base, s, t)
        SceneryKind.HAUNTED -> manorLandmark(cx, base, s, tone, t)
    }
}

// ---- Monumentos de Noche de brujas ----

private fun DrawScope.ghostShape(cx: Float, cy: Float, s: Float, alpha: Float, t: Float) {
    val body = Color.White.copy(alpha = alpha)
    drawCircle(Color(0xFFB9A2FF).copy(alpha = 0.25f * alpha), s * 0.75f, Offset(cx, cy))
    val p = Path().apply {
        moveTo(cx - s * 0.32f, cy + s * 0.36f)
        lineTo(cx - s * 0.32f, cy - s * 0.05f)
        arcTo(androidx.compose.ui.geometry.Rect(cx - s * 0.32f, cy - s * 0.42f, cx + s * 0.32f, cy + s * 0.22f), 180f, 180f, false)
        lineTo(cx + s * 0.32f, cy + s * 0.36f)
        for (k in 0..3) {
            val x1 = cx + s * (0.32f - (k + 0.5f) * 0.16f)
            val x2 = cx + s * (0.32f - (k + 1f) * 0.16f)
            quadraticTo(x1, cy + s * (0.5f + 0.05f * sin(t * 6.283f + k)), x2, cy + s * 0.36f)
        }
        close()
    }
    drawPath(p, body)
    drawCircle(Color(0xFF2A1450), s * 0.045f, Offset(cx - s * 0.1f, cy - s * 0.08f))
    drawCircle(Color(0xFF2A1450), s * 0.045f, Offset(cx + s * 0.1f, cy - s * 0.08f))
    drawOval(Color(0xFF2A1450), Offset(cx - s * 0.05f, cy + s * 0.04f), Size(s * 0.1f, s * 0.13f))
}

private fun DrawScope.graveyardGate(cx: Float, base: Float, s: Float, tone: Color, t: Float) {
    val ink = Color(0xFF241046)
    // lápidas
    fun tomb(x: Float, w: Float, h: Float, c: Color) {
        val p = Path().apply {
            moveTo(x - w / 2, base); lineTo(x - w / 2, base - h + w / 2)
            arcTo(androidx.compose.ui.geometry.Rect(x - w / 2, base - h, x + w / 2, base - h + w), 180f, 180f, false)
            lineTo(x + w / 2, base); close()
        }
        drawPath(p, c)
        drawLine(Color.White.copy(alpha = 0.3f), Offset(x - w * 0.22f, base - h * 0.55f), Offset(x + w * 0.22f, base - h * 0.55f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
    }
    tomb(cx - s * 0.5f, s * 0.26f, s * 0.42f, Color(0xFF6C5A9A))
    tomb(cx + s * 0.5f, s * 0.24f, s * 0.36f, Color(0xFF5E4D8C))
    tomb(cx + s * 0.18f, s * 0.2f, s * 0.28f, Color(0xFF7A68AA))
    // verja y arco
    for (i in -2..2) drawLine(ink, Offset(cx + i * s * 0.1f, base), Offset(cx + i * s * 0.1f, base - s * 0.34f), strokeWidth = s * 0.025f, cap = StrokeCap.Round)
    drawLine(ink, Offset(cx - s * 0.22f, base - s * 0.2f), Offset(cx + s * 0.22f, base - s * 0.2f), strokeWidth = s * 0.025f)
    drawArc(ink, 180f, 180f, false, Offset(cx - s * 0.24f, base - s * 0.58f), Size(s * 0.48f, s * 0.48f), style = Stroke(s * 0.03f))
    // fantasma que sube y baja
    ghostShape(cx - s * 0.05f, base - s * (0.62f + 0.06f * sin(t * 6.283f)), s * 0.55f, 0.9f, t)
}

private fun DrawScope.hauntedForest(cx: Float, base: Float, s: Float, tone: Color, t: Float) {
    val ink = Color(0xFF1B1030)
    fun tree(x: Float, h: Float, lean: Float) {
        drawLine(ink, Offset(x, base), Offset(x + lean * h * 0.1f, base - h * 0.75f), strokeWidth = h * 0.09f, cap = StrokeCap.Round)
        listOf(Triple(-0.3f, 0.7f, 0.42f), Triple(0.32f, 0.82f, 0.55f), Triple(-0.22f, 0.95f, 0.66f), Triple(0.24f, 1.0f, 0.74f)).forEach { (dx, ty, fy) ->
            drawLine(ink, Offset(x + lean * h * 0.05f, base - h * fy), Offset(x + dx * h, base - h * ty), strokeWidth = h * 0.04f, cap = StrokeCap.Round)
        }
        drawCircle(Color(0xFFFFE08A).copy(alpha = 0.55f + 0.4f * sin(t * 6.283f * 2f)), h * 0.022f, Offset(x - h * 0.02f, base - h * 0.45f))
        drawCircle(Color(0xFFFFE08A).copy(alpha = 0.55f + 0.4f * sin(t * 6.283f * 2f)), h * 0.022f, Offset(x + h * 0.05f, base - h * 0.45f))
    }
    tree(cx - s * 0.42f, s * 0.95f, -0.4f)
    tree(cx + s * 0.38f, s * 0.78f, 0.5f)
    // búho en una rama
    val ox = cx - s * 0.08f
    val oy = base - s * 0.5f
    drawOval(Color(0xFF3A2A58), Offset(ox - s * 0.09f, oy - s * 0.12f), Size(s * 0.18f, s * 0.22f))
    drawCircle(Color(0xFFFFE08A), s * 0.035f, Offset(ox - s * 0.04f, oy - s * 0.05f))
    drawCircle(Color(0xFFFFE08A), s * 0.035f, Offset(ox + s * 0.04f, oy - s * 0.05f))
    drawLine(ink, Offset(ox - s * 0.2f, oy + s * 0.1f), Offset(ox + s * 0.2f, oy + s * 0.1f), strokeWidth = s * 0.03f, cap = StrokeCap.Round)
    // luciérnagas
    for (k in 0..4) {
        val a = (t + k * 0.2f) % 1f
        drawCircle(Color(0xFFB8FFD0).copy(alpha = 0.7f * sin(a * 3.1416f)), s * 0.018f, Offset(cx + s * (-0.6f + k * 0.3f), base - s * (0.2f + 0.5f * a)))
    }
}

private fun DrawScope.bigPumpkin(cx: Float, base: Float, s: Float, t: Float) {
    val flick = 0.85f + 0.15f * sin(t * 6.283f * 3f)
    drawCircle(Color(0xFFFFA03A).copy(alpha = 0.28f * flick), s * 0.85f, Offset(cx, base - s * 0.34f))
    // calabazas pequeñas
    fun small(x: Float, r: Float) {
        drawOval(Color(0xFFC85F1B), Offset(x - r, base - r * 1.5f), Size(r * 2f, r * 1.6f))
        drawOval(Color(0xFFE8772E), Offset(x - r * 0.62f, base - r * 1.55f), Size(r * 1.24f, r * 1.65f))
        drawLine(Color(0xFF4F7A3A), Offset(x, base - r * 1.5f), Offset(x + r * 0.2f, base - r * 1.9f), strokeWidth = r * 0.2f, cap = StrokeCap.Round)
    }
    small(cx - s * 0.62f, s * 0.2f)
    small(cx + s * 0.64f, s * 0.17f)
    // la grande, con su cara encendida
    val r = s * 0.46f
    val c = Offset(cx, base - r * 0.8f)
    drawOval(Color(0xFFC85F1B), Offset(c.x - r, c.y - r * 0.78f), Size(r * 2f, r * 1.62f))
    drawOval(Color(0xFFE8772E), Offset(c.x - r * 0.62f, c.y - r * 0.82f), Size(r * 1.24f, r * 1.7f))
    drawOval(Color(0xFFC85F1B), Offset(c.x - r * 0.2f, c.y - r * 0.84f), Size(r * 0.4f, r * 1.74f))
    drawLine(Color(0xFF4F7A3A), Offset(c.x, c.y - r * 0.8f), Offset(c.x + r * 0.2f, c.y - r * 1.12f), strokeWidth = r * 0.16f, cap = StrokeCap.Round)
    val face = Color(0xFFFFE08A).copy(alpha = flick)
    drawPath(Path().apply { moveTo(c.x - r * 0.55f, c.y - r * 0.05f); lineTo(c.x - r * 0.2f, c.y - r * 0.05f); lineTo(c.x - r * 0.38f, c.y - r * 0.42f); close() }, face)
    drawPath(Path().apply { moveTo(c.x + r * 0.55f, c.y - r * 0.05f); lineTo(c.x + r * 0.2f, c.y - r * 0.05f); lineTo(c.x + r * 0.38f, c.y - r * 0.42f); close() }, face)
    drawPath(Path().apply {
        moveTo(c.x - r * 0.6f, c.y + r * 0.22f); lineTo(c.x - r * 0.32f, c.y + r * 0.5f); lineTo(c.x - r * 0.12f, c.y + r * 0.28f)
        lineTo(c.x + r * 0.12f, c.y + r * 0.5f); lineTo(c.x + r * 0.32f, c.y + r * 0.28f); lineTo(c.x + r * 0.6f, c.y + r * 0.22f)
        lineTo(c.x + r * 0.34f, c.y + r * 0.66f); lineTo(c.x - r * 0.34f, c.y + r * 0.66f); close()
    }, face)
}

private fun DrawScope.manorLandmark(cx: Float, base: Float, s: Float, tone: Color, t: Float) {
    val ink = Color(0xFF1B1030)
    val flick = 0.7f + 0.3f * sin(t * 6.283f * 3f)
    val w = s * 1.1f
    val bodyH = s * 0.4f
    drawRect(ink, Offset(cx - w / 2, base - bodyH), Size(w, bodyH))
    drawPath(Path().apply { moveTo(cx - w / 2 - s * 0.05f, base - bodyH); lineTo(cx, base - bodyH - s * 0.3f); lineTo(cx + w / 2 + s * 0.05f, base - bodyH); close() }, ink)
    listOf(-0.42f to 0.7f, 0.42f to 0.85f).forEach { (fx, th) ->
        val tx = cx + w * fx
        val top = base - bodyH - s * th * 0.35f
        drawRect(ink, Offset(tx - s * 0.09f, top), Size(s * 0.18f, base - top))
        drawPath(Path().apply { moveTo(tx - s * 0.13f, top); lineTo(tx, top - s * 0.24f); lineTo(tx + s * 0.13f, top); close() }, ink)
        drawRoundRect(Color(0xFFFFB347).copy(alpha = flick), Offset(tx - s * 0.03f, top + s * 0.07f), Size(s * 0.06f, s * 0.1f), CornerRadius(s * 0.03f))
    }
    listOf(-0.2f, 0f, 0.2f).forEachIndexed { i, fx ->
        drawRoundRect(Color(0xFFFFB347).copy(alpha = if (i == 1) 0.4f else flick), Offset(cx + w * fx - s * 0.035f, base - bodyH * 0.8f), Size(s * 0.07f, s * 0.12f), CornerRadius(s * 0.035f))
    }
    drawRoundRect(Color(0xFF3A2268), Offset(cx - s * 0.05f, base - s * 0.2f), Size(s * 0.1f, s * 0.2f), CornerRadius(s * 0.05f))
    // murciélagos alrededor
    for (k in 0..1) {
        val a = (t * 2f + k * 0.5f) % 1f
        val bx = cx + s * (-0.7f + 1.4f * a)
        val by = base - s * (0.95f + 0.12f * sin(a * 6.283f + k))
        drawPath(Path().apply {
            moveTo(bx, by); quadraticTo(bx - s * 0.06f, by - s * 0.08f * (0.4f + 0.6f * abs(sin(t * 6.283f * 4f))), bx - s * 0.14f, by)
            quadraticTo(bx - s * 0.07f, by + s * 0.02f, bx, by + s * 0.04f)
            quadraticTo(bx + s * 0.07f, by + s * 0.02f, bx + s * 0.14f, by)
            quadraticTo(bx + s * 0.06f, by - s * 0.08f * (0.4f + 0.6f * abs(sin(t * 6.283f * 4f))), bx, by); close()
        }, ink)
    }
}

private fun DrawScope.zenGarden(cx: Float, base: Float, s: Float, tone: Color) {
    // arena rastrillada
    for (i in 0..3) {
        drawArc(Color(0xFFD9C8A0).copy(alpha = 0.8f), 180f, 180f, false, Offset(cx - s * (0.62f - i * 0.1f), base - s * (0.3f - i * 0.04f)), Size(s * (1.24f - i * 0.2f), s * (0.32f - i * 0.05f)), style = Stroke(s * 0.025f, cap = StrokeCap.Round))
    }
    // piedras apiladas
    fun stone(x: Float, y: Float, w: Float, h: Float, c: Color) {
        drawOval(c, Offset(x - w / 2, y - h), Size(w, h))
        drawOval(Color.White.copy(alpha = 0.3f), Offset(x - w * 0.25f, y - h * 0.9f), Size(w * 0.3f, h * 0.25f))
    }
    stone(cx, base - s * 0.1f, s * 0.5f, s * 0.26f, Color(0xFF8E9690))
    stone(cx + s * 0.02f, base - s * 0.3f, s * 0.36f, s * 0.2f, Color(0xFF9DA5A0))
    stone(cx + s * 0.0f, base - s * 0.46f, s * 0.22f, s * 0.15f, Color(0xFFB0B8B2))
    // bonsái
    drawRoundRect(wood, Offset(cx - s * 0.5f, base - s * 0.2f), Size(s * 0.05f, s * 0.12f), CornerRadius(s * 0.02f))
    drawCircle(tone.copy(alpha = 0.85f), s * 0.11f, Offset(cx - s * 0.47f, base - s * 0.27f))
    drawCircle(tone.darker(0.8f).copy(alpha = 0.7f), s * 0.07f, Offset(cx - s * 0.4f, base - s * 0.24f))
}

private fun DrawScope.cabin(cx: Float, base: Float, s: Float) {
    fun pine(x: Float, h: Float) {
        drawRect(wood, Offset(x - h * 0.04f, base - h * 0.2f), Size(h * 0.08f, h * 0.2f))
        for (k in 0..2) {
            val top = base - h + k * h * 0.26f
            val half = h * (0.2f + k * 0.08f)
            val tri = Path().apply { moveTo(x, top); lineTo(x + half, top + h * 0.4f); lineTo(x - half, top + h * 0.4f); close() }
            drawPath(tri, Color(0xFF3F9468).darker(0.9f - k * 0.05f))
        }
    }
    pine(cx - s * 0.5f, s * 0.7f)
    pine(cx + s * 0.55f, s * 0.55f)
    // casa
    drawRoundRect(woodLight, Offset(cx - s * 0.3f, base - s * 0.4f), Size(s * 0.6f, s * 0.4f), CornerRadius(s * 0.03f))
    drawRoundRect(wood.copy(alpha = 0.35f), Offset(cx - s * 0.3f, base - s * 0.4f), Size(s * 0.6f, s * 0.05f), CornerRadius(s * 0.02f))
    val roof = Path().apply { moveTo(cx - s * 0.38f, base - s * 0.38f); lineTo(cx, base - s * 0.72f); lineTo(cx + s * 0.38f, base - s * 0.38f); close() }
    drawPath(roof, Color(0xFFB5463A)); drawPath(roof, Color(0xFF8A2F27), style = Stroke(s * 0.025f, join = StrokeJoin.Round))
    drawRoundRect(Color(0xFF6B3F1D), Offset(cx - s * 0.07f, base - s * 0.24f), Size(s * 0.14f, s * 0.24f), CornerRadius(s * 0.06f, s * 0.06f))
    drawRoundRect(Color(0xFFFFE08A), Offset(cx + s * 0.13f, base - s * 0.32f), Size(s * 0.1f, s * 0.1f), CornerRadius(s * 0.02f))
    drawRoundRect(Color(0xFFFFE08A), Offset(cx - s * 0.23f, base - s * 0.32f), Size(s * 0.1f, s * 0.1f), CornerRadius(s * 0.02f))
    // chimenea con humo
    drawRect(Color(0xFF8A5A33), Offset(cx + s * 0.16f, base - s * 0.72f), Size(s * 0.07f, s * 0.2f))
    for (i in 0..2) drawCircle(Color.White.copy(alpha = 0.55f - i * 0.15f), s * (0.05f + i * 0.02f), Offset(cx + s * (0.2f + i * 0.04f), base - s * (0.8f + i * 0.1f)))
}

private fun DrawScope.bridge(cx: Float, base: Float, s: Float, tone: Color, t: Float) {
    // agua
    for (i in 0..2) {
        val p = Path()
        for (k in 0..24) {
            val x = cx - s * 0.7f + k * s * 0.058f
            val y = base - s * (0.06f + i * 0.07f) + sin((k * 0.5f + i + t * 6.283f)) * s * 0.012f
            if (k == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        drawPath(p, tone.copy(alpha = 0.55f - i * 0.12f), style = Stroke(s * 0.03f, cap = StrokeCap.Round))
    }
    // arco del puente
    drawArc(wood, 180f, 180f, false, Offset(cx - s * 0.5f, base - s * 0.42f), Size(s * 1.0f, s * 0.7f), style = Stroke(s * 0.1f))
    drawArc(woodLight, 180f, 180f, false, Offset(cx - s * 0.5f, base - s * 0.42f), Size(s * 1.0f, s * 0.7f), style = Stroke(s * 0.04f))
    // barandilla
    for (i in 0..6) {
        val f = i / 6f
        val x = cx - s * 0.46f + f * s * 0.92f
        val y = base - s * 0.07f - sin(f * PI.toFloat()) * s * 0.35f
        drawLine(wood, Offset(x, y), Offset(x, y - s * 0.12f), strokeWidth = s * 0.03f, cap = StrokeCap.Round)
    }
    // nenúfares
    drawCircle(Color(0xFF7FC8A0), s * 0.07f, Offset(cx - s * 0.62f, base - s * 0.1f))
    drawCircle(Color(0xFFFFB6C8), s * 0.03f, Offset(cx - s * 0.62f, base - s * 0.12f))
}

private fun DrawScope.teaHouse(cx: Float, base: Float, s: Float, tone: Color) {
    // loma
    drawArc(tone.copy(alpha = 0.45f), 180f, 180f, true, Offset(cx - s * 0.7f, base - s * 0.3f), Size(s * 1.4f, s * 0.6f))
    // pabellón de dos techos
    drawRoundRect(cream, Offset(cx - s * 0.22f, base - s * 0.28f), Size(s * 0.44f, s * 0.2f), CornerRadius(s * 0.02f))
    drawRoundRect(Color(0xFF6B3F1D), Offset(cx - s * 0.05f, base - s * 0.22f), Size(s * 0.1f, s * 0.14f), CornerRadius(s * 0.03f))
    curvedRoof(cx, base - s * 0.48f, s * 0.6f, s * 0.22f, Color(0xFF4F6B3A))
    drawRoundRect(cream, Offset(cx - s * 0.14f, base - s * 0.52f), Size(s * 0.28f, s * 0.12f), CornerRadius(s * 0.02f))
    curvedRoof(cx, base - s * 0.72f, s * 0.42f, s * 0.2f, Color(0xFF4F6B3A))
    // arbustos de té
    for (i in 0..4) {
        drawCircle(tone.darker(0.75f).copy(alpha = 0.8f), s * 0.07f, Offset(cx - s * 0.6f + i * s * 0.07f, base - s * 0.1f))
        drawCircle(tone.darker(0.75f).copy(alpha = 0.8f), s * 0.07f, Offset(cx + s * 0.35f + i * s * 0.07f, base - s * 0.1f))
    }
}

private fun DrawScope.lighthouse(cx: Float, base: Float, s: Float, t: Float) {
    // rocas y olas
    drawOval(Color(0xFF7A8290), Offset(cx - s * 0.4f, base - s * 0.18f), Size(s * 0.8f, s * 0.2f))
    drawOval(Color(0xFF9AA2B0).copy(alpha = 0.7f), Offset(cx - s * 0.3f, base - s * 0.17f), Size(s * 0.25f, s * 0.07f))
    // torre con franjas
    val tower = Path().apply {
        moveTo(cx - s * 0.15f, base - s * 0.14f); lineTo(cx - s * 0.1f, base - s * 0.75f)
        lineTo(cx + s * 0.1f, base - s * 0.75f); lineTo(cx + s * 0.15f, base - s * 0.14f); close()
    }
    drawPath(tower, Color.White)
    val stripes = listOf(0.28f to 0.46f, 0.58f to 0.7f)
    stripes.forEach { (a, b) ->
        val p = Path().apply {
            val ax = s * (0.15f - 0.05f * (a - 0.14f) / 0.61f); val bx = s * (0.15f - 0.05f * (b - 0.14f) / 0.61f)
            moveTo(cx - ax, base - s * a); lineTo(cx - bx, base - s * b); lineTo(cx + bx, base - s * b); lineTo(cx + ax, base - s * a); close()
        }
        drawPath(p, Color(0xFFD94F4F))
    }
    // lámpara y luz giratoria
    drawRoundRect(Color(0xFF55606E), Offset(cx - s * 0.14f, base - s * 0.8f), Size(s * 0.28f, s * 0.06f), CornerRadius(s * 0.02f))
    drawRoundRect(Color(0xFFFFE680), Offset(cx - s * 0.09f, base - s * 0.9f), Size(s * 0.18f, s * 0.11f), CornerRadius(s * 0.03f))
    curvedRoof(cx, base - s * 1.0f, s * 0.26f, s * 0.12f, Color(0xFFD94F4F))
    val pulse = 0.5f + 0.5f * sin(t * 6.283f)
    val beam = Path().apply {
        moveTo(cx, base - s * 0.85f); lineTo(cx - s * 0.9f, base - s * (0.98f - 0.06f * pulse)); lineTo(cx - s * 0.9f, base - s * (0.72f + 0.04f * pulse)); close()
    }
    drawPath(beam, Color(0xFFFFF1B0).copy(alpha = 0.22f + 0.2f * pulse))
}

private fun DrawScope.windmill(cx: Float, base: Float, s: Float, tone: Color, t: Float) {
    // campo de lavanda
    for (i in 0..8) {
        val x = cx - s * 0.7f + i * s * 0.17f
        drawLine(Color(0xFF6FA06E), Offset(x, base - s * 0.02f), Offset(x, base - s * 0.2f), strokeWidth = s * 0.025f, cap = StrokeCap.Round)
        drawOval(tone.copy(alpha = 0.9f), Offset(x - s * 0.03f, base - s * 0.3f), Size(s * 0.06f, s * 0.13f))
    }
    // torre
    val body = Path().apply {
        moveTo(cx - s * 0.17f, base - s * 0.04f); lineTo(cx - s * 0.1f, base - s * 0.58f)
        lineTo(cx + s * 0.1f, base - s * 0.58f); lineTo(cx + s * 0.17f, base - s * 0.04f); close()
    }
    drawPath(body, cream); drawPath(body, wood.copy(alpha = 0.4f), style = Stroke(s * 0.02f, join = StrokeJoin.Round))
    drawRoundRect(Color(0xFF6B3F1D), Offset(cx - s * 0.05f, base - s * 0.2f), Size(s * 0.1f, s * 0.17f), CornerRadius(s * 0.05f, s * 0.05f))
    curvedRoof(cx, base - s * 0.74f, s * 0.3f, s * 0.18f, Color(0xFF7A5C9E))
    // aspas girando
    val hub = Offset(cx, base - s * 0.6f)
    rotate(t * 360f, pivot = hub) {
        for (i in 0..3) {
            rotate(i * 90f, pivot = hub) {
                drawRoundRect(wood, Offset(hub.x - s * 0.02f, hub.y - s * 0.5f), Size(s * 0.04f, s * 0.5f), CornerRadius(s * 0.01f))
                drawRoundRect(Color.White.copy(alpha = 0.9f), Offset(hub.x + s * 0.02f, hub.y - s * 0.48f), Size(s * 0.12f, s * 0.26f), CornerRadius(s * 0.015f))
            }
        }
    }
    drawCircle(wood, s * 0.04f, hub)
}

private fun DrawScope.marketStall(cx: Float, base: Float, s: Float, t: Float) {
    // mostrador
    drawRoundRect(wood, Offset(cx - s * 0.42f, base - s * 0.2f), Size(s * 0.84f, s * 0.2f), CornerRadius(s * 0.03f))
    drawRoundRect(Color(0xFFFFD36E), Offset(cx - s * 0.36f, base - s * 0.3f), Size(s * 0.2f, s * 0.1f), CornerRadius(s * 0.05f))
    drawRoundRect(Color(0xFFE5576B), Offset(cx - s * 0.1f, base - s * 0.28f), Size(s * 0.16f, s * 0.08f), CornerRadius(s * 0.04f))
    drawRoundRect(Color(0xFF6BB08A), Offset(cx + s * 0.14f, base - s * 0.29f), Size(s * 0.18f, s * 0.09f), CornerRadius(s * 0.04f))
    // postes
    drawRect(wood, Offset(cx - s * 0.42f, base - s * 0.7f), Size(s * 0.04f, s * 0.5f))
    drawRect(wood, Offset(cx + s * 0.38f, base - s * 0.7f), Size(s * 0.04f, s * 0.5f))
    // toldo a rayas con festón
    val n = 8
    for (i in 0 until n) {
        val x = cx - s * 0.5f + i * (s * 1.0f / n)
        val c = if (i % 2 == 0) Color(0xFFE5576B) else Color.White
        drawRect(c, Offset(x, base - s * 0.8f), Size(s / n, s * 0.14f))
        drawCircle(c, s / n / 2f, Offset(x + s / n / 2f, base - s * 0.66f))
    }
    // farolillos que se balancean
    for (i in 0..2) {
        val x = cx - s * 0.3f + i * s * 0.3f
        val sway = sin((t * 6.283f) + i) * s * 0.02f
        drawLine(Color(0xFF55524B).copy(alpha = 0.5f), Offset(x, base - s * 0.66f), Offset(x + sway, base - s * 0.5f), strokeWidth = s * 0.015f)
        drawCircle(Color(0xFFFF9A3C).copy(alpha = 0.25f), s * 0.12f, Offset(x + sway, base - s * 0.45f))
        drawCircle(Color(0xFFFF9A3C), s * 0.06f, Offset(x + sway, base - s * 0.45f))
    }
}

private fun DrawScope.peak(cx: Float, base: Float, s: Float, tone: Color, t: Float) {
    fun mountain(x: Float, w: Float, h: Float, c: Color, capH: Float) {
        val p = Path().apply { moveTo(x - w / 2, base - s * 0.05f); lineTo(x, base - h); lineTo(x + w / 2, base - s * 0.05f); close() }
        drawPath(p, c)
        val shade = Path().apply { moveTo(x, base - h); lineTo(x + w / 2, base - s * 0.05f); lineTo(x + w * 0.1f, base - s * 0.05f); close() }
        drawPath(shade, c.darker(0.82f).copy(alpha = 0.5f))
        val cap = Path().apply {
            moveTo(x, base - h); lineTo(x + w * 0.16f, base - h + capH); lineTo(x + w * 0.05f, base - h + capH * 0.7f)
            lineTo(x - w * 0.05f, base - h + capH * 1.0f); lineTo(x - w * 0.16f, base - h + capH); close()
        }
        drawPath(cap, Color.White)
    }
    mountain(cx - s * 0.45f, s * 0.7f, s * 0.55f, tone.darker(0.9f).copy(alpha = 0.85f), s * 0.14f)
    mountain(cx + s * 0.1f, s * 1.0f, s * 0.95f, tone, s * 0.26f)
    // bandera ondeando en la cima
    val top = Offset(cx + s * 0.1f, base - s * 0.95f)
    drawLine(Color(0xFF55524B), top, Offset(top.x, top.y - s * 0.22f), strokeWidth = s * 0.02f, cap = StrokeCap.Round)
    val wave = sin(t * 6.283f * 2) * s * 0.02f
    val flag = Path().apply {
        moveTo(top.x, top.y - s * 0.22f); quadraticTo(top.x + s * 0.1f, top.y - s * 0.2f + wave, top.x + s * 0.18f, top.y - s * 0.17f)
        quadraticTo(top.x + s * 0.1f, top.y - s * 0.12f - wave, top.x, top.y - s * 0.11f); close()
    }
    drawPath(flag, Color(0xFFE5576B))
}

private fun DrawScope.torii(cx: Float, base: Float, s: Float, tone: Color) {
    val red = Color(0xFFD94F4F)
    // cerezo
    drawLine(wood, Offset(cx - s * 0.55f, base - s * 0.04f), Offset(cx - s * 0.52f, base - s * 0.4f), strokeWidth = s * 0.07f, cap = StrokeCap.Round)
    listOf(Offset(-0.52f, -0.5f) to 0.22f, Offset(-0.68f, -0.4f) to 0.17f, Offset(-0.38f, -0.42f) to 0.18f, Offset(-0.55f, -0.64f) to 0.15f).forEach { (o, r) ->
        drawCircle(tone.copy(alpha = 0.85f), s * r, Offset(cx + s * o.x, base + s * o.y))
    }
    // torii
    drawRoundRect(red, Offset(cx - s * 0.3f, base - s * 0.62f), Size(s * 0.07f, s * 0.62f), CornerRadius(s * 0.015f))
    drawRoundRect(red, Offset(cx + s * 0.23f, base - s * 0.62f), Size(s * 0.07f, s * 0.62f), CornerRadius(s * 0.015f))
    drawRoundRect(red, Offset(cx - s * 0.32f, base - s * 0.5f), Size(s * 0.64f, s * 0.06f), CornerRadius(s * 0.015f))
    val top = Path().apply {
        moveTo(cx - s * 0.46f, base - s * 0.7f); quadraticTo(cx, base - s * 0.62f, cx + s * 0.46f, base - s * 0.7f)
        lineTo(cx + s * 0.42f, base - s * 0.62f); quadraticTo(cx, base - s * 0.55f, cx - s * 0.42f, base - s * 0.62f); close()
    }
    drawPath(top, Color(0xFF3F3A47)); drawPath(top, red, style = Stroke(s * 0.02f))
}

private fun DrawScope.pyramids(cx: Float, base: Float, s: Float, tone: Color) {
    val sand = Color(0xFFE8C37A)
    fun pyramid(x: Float, w: Float, h: Float) {
        val p = Path().apply { moveTo(x - w / 2, base - s * 0.04f); lineTo(x, base - h); lineTo(x + w / 2, base - s * 0.04f); close() }
        drawPath(p, sand)
        val shade = Path().apply { moveTo(x, base - h); lineTo(x + w / 2, base - s * 0.04f); lineTo(x + w * 0.05f, base - s * 0.04f); close() }
        drawPath(shade, Color(0xFFC99A4A).copy(alpha = 0.7f))
    }
    pyramid(cx - s * 0.25f, s * 0.9f, s * 0.65f)
    pyramid(cx + s * 0.3f, s * 0.6f, s * 0.42f)
    // palmera
    drawLine(wood, Offset(cx + s * 0.62f, base - s * 0.04f), Offset(cx + s * 0.58f, base - s * 0.5f), strokeWidth = s * 0.05f, cap = StrokeCap.Round)
    for (i in 0..4) {
        val a = (-160f + i * 35f) * PI.toFloat() / 180f
        val tip = Offset(cx + s * 0.58f + kotlin.math.cos(a) * s * 0.3f, base - s * 0.5f + sin(a) * s * 0.2f + s * 0.06f)
        val leaf = Path().apply { moveTo(cx + s * 0.58f, base - s * 0.5f); quadraticTo((cx + s * 0.58f + tip.x) / 2, tip.y - s * 0.1f, tip.x, tip.y); quadraticTo((cx + s * 0.58f + tip.x) / 2, tip.y + s * 0.0f, cx + s * 0.58f, base - s * 0.48f) }
        drawPath(leaf, Color(0xFF3F9468))
    }
}

private fun DrawScope.sailboat(cx: Float, base: Float, s: Float, tone: Color, t: Float) {
    val bob = sin(t * 6.283f) * s * 0.02f
    // mar
    for (i in 0..2) {
        val p = Path()
        for (k in 0..26) {
            val x = cx - s * 0.8f + k * s * 0.06f
            val y = base - s * (0.05f + i * 0.07f) + sin((k * 0.5f + i - t * 6.283f)) * s * 0.014f
            if (k == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        drawPath(p, Color.White.copy(alpha = 0.45f - i * 0.1f), style = Stroke(s * 0.025f, cap = StrokeCap.Round))
    }
    rotate(sin(t * 6.283f) * 3f, pivot = Offset(cx, base - s * 0.1f)) {
        val b = bob
        // casco
        val hull = Path().apply { moveTo(cx - s * 0.4f, base - s * 0.2f + b); lineTo(cx + s * 0.4f, base - s * 0.2f + b); lineTo(cx + s * 0.28f, base - s * 0.08f + b); lineTo(cx - s * 0.28f, base - s * 0.08f + b); close() }
        drawPath(hull, Color(0xFFE5576B)); drawRect(Color.White.copy(alpha = 0.8f), Offset(cx - s * 0.38f, base - s * 0.2f + b), Size(s * 0.76f, s * 0.025f))
        // mástil y velas
        drawLine(wood, Offset(cx, base - s * 0.2f + b), Offset(cx, base - s * 0.85f + b), strokeWidth = s * 0.025f, cap = StrokeCap.Round)
        val sail1 = Path().apply { moveTo(cx + s * 0.02f, base - s * 0.82f + b); lineTo(cx + s * 0.34f, base - s * 0.25f + b); lineTo(cx + s * 0.02f, base - s * 0.25f + b); close() }
        val sail2 = Path().apply { moveTo(cx - s * 0.02f, base - s * 0.7f + b); lineTo(cx - s * 0.3f, base - s * 0.25f + b); lineTo(cx - s * 0.02f, base - s * 0.25f + b); close() }
        drawPath(sail1, Color(0xFFFFF6E0)); drawPath(sail2, Color(0xFFE9E4F5))
    }
}

private fun DrawScope.pagoda(cx: Float, base: Float, s: Float, tone: Color) {
    // bambú a los lados
    for (side in listOf(-1f, 1f)) {
        for (k in 0..1) {
            val x = cx + side * (s * 0.55f + k * s * 0.12f)
            drawRoundRect(tone.copy(alpha = 0.85f), Offset(x - s * 0.03f, base - s * (0.75f - k * 0.1f)), Size(s * 0.06f, s * (0.72f - k * 0.1f)), CornerRadius(s * 0.03f))
            var y = base - s * 0.2f
            while (y > base - s * 0.7f) { drawLine(tone.darker(0.7f), Offset(x - s * 0.03f, y), Offset(x + s * 0.03f, y), strokeWidth = s * 0.012f); y -= s * 0.17f }
        }
    }
    // tres pisos
    val tiers = listOf(Triple(0.46f, 0.0f, Color(0xFFB5463A)), Triple(0.36f, 0.26f, Color(0xFFB5463A)), Triple(0.26f, 0.5f, Color(0xFFB5463A)))
    tiers.forEach { (w, h, roof) ->
        val y = base - s * (0.14f + h)
        drawRoundRect(cream, Offset(cx - s * w / 2, y - s * 0.14f), Size(s * w, s * 0.14f), CornerRadius(s * 0.015f))
        drawRoundRect(Color(0xFF6B3F1D), Offset(cx - s * 0.03f, y - s * 0.12f), Size(s * 0.06f, s * 0.1f), CornerRadius(s * 0.03f))
        curvedRoof(cx, y - s * 0.26f, s * (w + 0.22f), s * 0.13f, roof)
    }
    drawLine(Color(0xFFD9A21B), Offset(cx, base - s * 0.9f), Offset(cx, base - s * 1.0f), strokeWidth = s * 0.02f, cap = StrokeCap.Round)
    drawCircle(Color(0xFFFFD36E), s * 0.03f, Offset(cx, base - s * 1.0f))
}
