package com.korkoor.pardos.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.korkoor.pardos.domain.collection.ChestType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

enum class ChestState {
    /** Aún no se puede abrir: piedra gris y candado. */
    LOCKED,
    /** Listo: brilla, se sacude y suelta destellos. */
    READY,
    /** Abierto: luz dorada y tesoro adentro. */
    OPEN
}

/** Materiales de cada rareza. */
private class Mat(
    val woodLight: Color, val woodBase: Color, val woodDark: Color,
    val metal: Color, val metalLight: Color, val metalDark: Color,
    val gem: Color, val gemLight: Color, val glow: Color
)

private fun materialFor(type: ChestType): Mat = when (type) {
    ChestType.COMMON -> Mat(
        Color(0xFFE0B27A), Color(0xFFB87A3E), Color(0xFF7A4B1F),
        Color(0xFFC9A064), Color(0xFFF1D79B), Color(0xFF8A6A33),
        Color(0xFF7FC8A0), Color(0xFFC9F0DB), Color(0xFFFFD36E)
    )
    ChestType.RARE -> Mat(
        Color(0xFF8DB4D8), Color(0xFF5A86B5), Color(0xFF38587F),
        Color(0xFFD7DEE8), Color(0xFFFFFFFF), Color(0xFF8E9AAD),
        Color(0xFF4FB4DC), Color(0xFFBDEBFA), Color(0xFF9AD9FF)
    )
    ChestType.EPIC -> Mat(
        Color(0xFFB69BEA), Color(0xFF7C5CC4), Color(0xFF4A3490),
        Color(0xFFFFC83D), Color(0xFFFFEA9A), Color(0xFFB87510),
        Color(0xFFFF6B8A), Color(0xFFFFC2D0), Color(0xFFFFB0E0)
    )
}

private val lockedMat = Mat(
    Color(0xFFCFCBC2), Color(0xFFADA89E), Color(0xFF857F74),
    Color(0xFF9A958B), Color(0xFFC9C4BA), Color(0xFF6E6A62),
    Color(0xFF8A857B), Color(0xFFB5B0A6), Color(0xFFFFFFFF)
)

/**
 * Cofre del tesoro dibujado a mano: madera con vetas y remaches, correas de metal, tapa abovedada y cerradura con gema.
 * El material cambia con la rareza. Listo = se sacude y brilla; abierto = luz y tesoro. Un solo reloj compartido lo anima.
 */
@Composable
fun TreasureChest(
    type: ChestType,
    state: ChestState,
    modifier: Modifier = Modifier,
    animate: Boolean = true
) {
    val clock = LocalCozyClock.current
    // La tapa se abre con un resorte cuando el estado pasa a OPEN
    val open = remember { Animatable(if (state == ChestState.OPEN) 1f else 0f) }
    LaunchedEffect(state) {
        open.animateTo(if (state == ChestState.OPEN) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
    }

    Canvas(modifier) {
        val u = size.minDimension / 100f
        val t = if (animate) clock.value else 0.25f
        val mat = if (state == ChestState.LOCKED) lockedMat else materialFor(type)
        scale(u, u, pivot = Offset.Zero) { chest(mat, type, state, open.value, t) }
    }
}

private fun DrawScope.sparkle(c: Offset, r: Float, color: Color) {
    if (r <= 0.3f) return
    val p = Path().apply {
        moveTo(c.x, c.y - r)
        quadraticTo(c.x, c.y, c.x + r, c.y); quadraticTo(c.x, c.y, c.x, c.y + r)
        quadraticTo(c.x, c.y, c.x - r, c.y); quadraticTo(c.x, c.y, c.x, c.y - r); close()
    }
    drawCircle(color.copy(alpha = 0.22f), r * 0.9f, c)
    drawPath(p, color)
}

private fun tw(t: Float, phase: Float) = 0.5f + 0.5f * sin(((t + phase) * 2 * PI).toFloat())

private fun DrawScope.chest(m: Mat, type: ChestType, state: ChestState, open: Float, t: Float) {
    val ready = state == ChestState.READY
    val locked = state == ChestState.LOCKED

    // ---- aura y rayos (listo o abierto)
    if (ready || open > 0.05f) {
        val a = if (ready) 0.20f + 0.12f * tw(t, 0f) else 0.28f * open
        drawCircle(Brush.radialGradient(listOf(m.glow.copy(alpha = a * 1.6f), Color.Transparent), center = Offset(50f, 52f), radius = 62f), 62f, Offset(50f, 52f))
        if (open > 0.4f) {
            rotate(t * 90f, pivot = Offset(50f, 46f)) {
                for (i in 0 until 10) {
                    val ang = (i * 36f) * PI.toFloat() / 180f
                    drawLine(
                        m.glow.copy(alpha = 0.30f * open), Offset(50f + cos(ang) * 22f, 46f + sin(ang) * 22f),
                        Offset(50f + cos(ang) * 52f, 46f + sin(ang) * 52f), strokeWidth = 5f, cap = StrokeCap.Round
                    )
                }
            }
        }
    }

    // ---- sombra de suelo
    drawOval(Color.Black.copy(alpha = 0.16f), Offset(10f, 86f), Size(80f, 9f))

    // ---- cuerpo (más ancho arriba que abajo, con vetas)
    val body = Path().apply {
        moveTo(12f, 46f); lineTo(88f, 46f); lineTo(85f, 86f)
        quadraticTo(84f, 90f, 80f, 90f); lineTo(20f, 90f)
        quadraticTo(16f, 90f, 15f, 86f); close()
    }
    drawPath(body, Brush.verticalGradient(listOf(m.woodLight, m.woodBase, m.woodDark), 46f, 90f))
    // tablones
    for (y in listOf(58f, 70f, 82f)) {
        drawLine(m.woodDark.copy(alpha = 0.45f), Offset(15f, y), Offset(85f, y), strokeWidth = 1.6f)
        drawLine(Color.White.copy(alpha = 0.16f), Offset(15f, y + 1.5f), Offset(85f, y + 1.5f), strokeWidth = 1f)
    }
    // veta
    drawArc(m.woodDark.copy(alpha = 0.22f), 200f, 110f, false, Offset(34f, 60f), Size(20f, 7f), style = Stroke(1.2f, cap = StrokeCap.Round))
    drawPath(body, m.woodDark, style = Stroke(2.6f, join = StrokeJoin.Round))
    // reflejo lateral
    drawRoundRect(Color.White.copy(alpha = 0.18f), Offset(17f, 50f), Size(4f, 34f), CornerRadius(2f))

    // ---- interior luminoso y tesoro (si está abierto)
    if (open > 0.02f) {
        val inner = Path().apply { moveTo(17f, 47f); lineTo(83f, 47f); lineTo(81f, 58f); lineTo(19f, 58f); close() }
        drawPath(inner, Brush.verticalGradient(listOf(Color(0xFFFFF4C2), m.glow), 47f, 58f), alpha = open)
        val coin = Color(0xFFFFC83D)
        listOf(26f to 47f, 38f to 43f, 50f to 41f, 62f to 43f, 74f to 47f, 44f to 48f, 58f to 48f).forEachIndexed { i, (x, y) ->
            val h = (y - 8f * open + 8f)
            drawCircle(coin.copy(alpha = open), 7.5f, Offset(x, h - 2f * open))
            drawCircle(Color(0xFFB87510).copy(alpha = open), 7.5f, Offset(x, h - 2f * open), style = Stroke(1.6f))
            drawCircle(Color.White.copy(alpha = 0.55f * open), 2f, Offset(x - 2.2f, h - 4.4f * open))
        }
        // gema que sobresale
        val g = Offset(50f, 40f - 6f * open)
        val gem = Path().apply { moveTo(g.x, g.y - 7f); lineTo(g.x + 7f, g.y); lineTo(g.x, g.y + 8f); lineTo(g.x - 7f, g.y); close() }
        drawPath(gem, m.gem.copy(alpha = open)); drawPath(gem, Color.White.copy(alpha = 0.7f * open), style = Stroke(1.4f))
    }

    // ---- correas verticales con remaches
    for (x in listOf(24f, 66f)) {
        drawRoundRect(Brush.horizontalGradient(listOf(m.metalLight, m.metal, m.metalDark), x, x + 10f), Offset(x, 46f), Size(10f, 44f), CornerRadius(2.5f))
        drawRoundRect(m.metalDark.copy(alpha = 0.7f), Offset(x, 46f), Size(10f, 44f), CornerRadius(2.5f), style = Stroke(1.4f))
        for (y in listOf(54f, 70f, 85f)) {
            drawCircle(m.metalDark, 2.3f, Offset(x + 5f, y)); drawCircle(m.metalLight, 1f, Offset(x + 4.3f, y - 0.8f))
        }
    }

    // ---- tapa abovedada (gira sobre la bisagra trasera al abrirse)
    val lidAngle = -(open * 62f) + if (ready) -5f * max(0f, sin((t * 2 * PI * 3).toFloat())) * (if (t < 0.34f) 1f else 0f) else 0f
    rotate(lidAngle, pivot = Offset(14f, 48f)) {
        val lid = Path().apply {
            moveTo(10f, 50f); lineTo(10f, 38f)
            quadraticTo(50f, 4f, 90f, 38f); lineTo(90f, 50f); close()
        }
        drawPath(lid, Brush.verticalGradient(listOf(m.woodLight, m.woodBase, m.woodDark), 10f, 50f))
        drawPath(lid, m.woodDark, style = Stroke(2.6f, join = StrokeJoin.Round))
        // correas de la tapa
        for (x in listOf(24f, 66f)) {
            val p = Path().apply { moveTo(x, 50f); lineTo(x, 33f); quadraticTo(x + 5f, 29f, x + 10f, 33f); lineTo(x + 10f, 50f); close() }
            drawPath(p, Brush.horizontalGradient(listOf(m.metalLight, m.metal, m.metalDark), x, x + 10f))
            drawPath(p, m.metalDark.copy(alpha = 0.7f), style = Stroke(1.2f, join = StrokeJoin.Round))
        }
        // aro metálico del borde
        drawRoundRect(Brush.verticalGradient(listOf(m.metalLight, m.metal, m.metalDark), 43f, 51f), Offset(9f, 43f), Size(82f, 8f), CornerRadius(3f))
        drawRoundRect(m.metalDark.copy(alpha = 0.8f), Offset(9f, 43f), Size(82f, 8f), CornerRadius(3f), style = Stroke(1.3f))
        // brillo de la cúpula
        drawArc(Color.White.copy(alpha = 0.42f), 205f, 55f, false, Offset(22f, 14f), Size(56f, 40f), style = Stroke(3.2f, cap = StrokeCap.Round))
    }

    // ---- cerradura (se oculta al abrir)
    if (open < 0.6f) {
        val a = 1f - open / 0.6f
        val plate = Path().apply { moveTo(41f, 47f); lineTo(59f, 47f); lineTo(57f, 63f); quadraticTo(50f, 68f, 43f, 63f); close() }
        drawPath(plate, Brush.verticalGradient(listOf(m.metalLight, m.metal, m.metalDark), 47f, 66f), alpha = a)
        drawPath(plate, m.metalDark, style = Stroke(1.6f, join = StrokeJoin.Round), alpha = a)
        if (locked) {
            // candado
            drawArc(Color(0xFF6E6A62), 180f, 180f, false, Offset(44f, 40f), Size(12f, 12f), style = Stroke(3f, cap = StrokeCap.Round), alpha = a)
            drawRoundRect(Color(0xFF55524B), Offset(43f, 49f), Size(14f, 11f), CornerRadius(3f), alpha = a)
            drawCircle(Color(0xFFE0DCD2), 1.8f, Offset(50f, 54f), alpha = a)
        } else {
            // gema de la rareza
            val g = Offset(50f, 55f)
            val gem = Path().apply { moveTo(g.x, g.y - 6.5f); lineTo(g.x + 6f, g.y); lineTo(g.x, g.y + 7f); lineTo(g.x - 6f, g.y); close() }
            drawPath(gem, Brush.verticalGradient(listOf(m.gemLight, m.gem), g.y - 7f, g.y + 7f), alpha = a)
            drawPath(gem, Color.White.copy(alpha = 0.8f), style = Stroke(1.2f, join = StrokeJoin.Round), alpha = a)
            drawLine(Color.White.copy(alpha = 0.7f), Offset(g.x - 2f, g.y - 2.5f), Offset(g.x + 1f, g.y - 4.8f), strokeWidth = 1.4f, cap = StrokeCap.Round, alpha = a)
        }
    }

    // ---- destellos
    if (ready) {
        sparkle(Offset(88f, 20f), 7f * tw(t, 0.0f), Color(0xFFFFF4C2))
        sparkle(Offset(10f, 30f), 5f * tw(t, 0.4f), Color(0xFFFFF4C2))
        sparkle(Offset(78f, 6f), 4f * tw(t, 0.7f), Color.White)
        if (type == ChestType.EPIC) sparkle(Offset(20f, 8f), 5f * tw(t, 0.2f), m.gemLight)
    } else if (open > 0.5f) {
        sparkle(Offset(30f, 20f), 6f * tw(t, 0.1f), Color(0xFFFFF4C2))
        sparkle(Offset(72f, 14f), 7f * tw(t, 0.5f), Color(0xFFFFF4C2))
        sparkle(Offset(50f, 4f), 5f * tw(t, 0.8f), Color.White)
    }
}
