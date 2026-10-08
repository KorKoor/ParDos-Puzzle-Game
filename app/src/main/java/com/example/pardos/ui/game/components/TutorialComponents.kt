package com.korkoor.pardos.ui.game.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.R
import com.korkoor.pardos.domain.logic.CoachStep
import com.korkoor.pardos.domain.logic.Direction
import com.korkoor.pardos.ui.design.Gold
import com.korkoor.pardos.ui.design.InkSecondary
import com.korkoor.pardos.ui.design.JellyCard
import com.korkoor.pardos.ui.design.Navy
import com.korkoor.pardos.ui.design.Sage
import com.korkoor.pardos.ui.design.wobble
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Dónde y hacia dónde enseñar la mano sobre el tablero, y qué fichas resaltar. */
data class BoardGuide(val direction: Direction, val cells: Set<Pair<Int, Int>> = emptySet())

/** El punto de la imagen de la mano que toca la pantalla (la punta del dedo), en fracción del tamaño. */
private const val TIP_X = 0.39f
private const val TIP_Y = 0.07f

/**
 * Guía sobre el tablero: resalta las fichas que se van a fundir y una mano 3D que las desliza hacia donde toca, con un
 * toque que se hunde, una estela y la flecha del movimiento. Se dibuja dentro de la rejilla (misma geometría que las fichas).
 */
@Composable
fun GuideOverlay(guide: BoardGuide, tileSize: Dp, spacing: Dp, gridSize: Int, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val clock = rememberInfiniteTransition(label = "guide")
    val t by clock.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "t")
    val pulse by clock.animateFloat(0.35f, 1f, infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")

    val step = tileSize + spacing
    val tilePx = with(density) { tileSize.toPx() }
    val stepPx = with(density) { step.toPx() }
    val boardPx = tilePx * gridSize + with(density) { spacing.toPx() } * (gridSize - 1)

    // Punto de salida: el centro de las fichas resaltadas, o el centro del tablero
    val start = if (guide.cells.isNotEmpty()) {
        Offset(
            guide.cells.map { it.second }.average().toFloat() * stepPx + tilePx / 2f,
            guide.cells.map { it.first }.average().toFloat() * stepPx + tilePx / 2f
        )
    } else Offset(boardPx / 2f, boardPx / 2f)
    val (dx, dy) = when (guide.direction) {
        Direction.RIGHT -> 1f to 0f
        Direction.LEFT -> -1f to 0f
        Direction.DOWN -> 0f to 1f
        Direction.UP -> 0f to -1f
    }
    val travel = (stepPx * 1.55f).coerceAtMost(boardPx * 0.45f)

    // Línea de tiempo del gesto: aparece y se hunde, desliza, suelta y se desvanece
    val appear = (t / 0.12f).coerceIn(0f, 1f)
    val slide = ((t - 0.14f) / 0.46f).coerceIn(0f, 1f)
    val eased = slide * slide * (3f - 2f * slide)
    val fade = when {
        t < 0.12f -> appear
        t > 0.80f -> 1f - ((t - 0.80f) / 0.20f)
        else -> 1f
    }.coerceIn(0f, 1f)
    val press = when {
        t < 0.12f -> 1.18f - 0.26f * appear           // se acerca y se hunde
        t < 0.64f -> 0.92f
        t < 0.74f -> 0.92f + 0.10f * ((t - 0.64f) / 0.10f)
        else -> 1.02f
    }
    val tip = Offset(start.x + dx * travel * eased, start.y + dy * travel * eased)
    val rotation = when (guide.direction) {
        Direction.UP -> 0f; Direction.RIGHT -> 90f; Direction.DOWN -> 180f; Direction.LEFT -> -90f
    }

    Box(modifier.fillMaxSize()) {
        // 1) halos en las fichas que se fusionan
        guide.cells.forEach { (r, c) ->
            Box(
                Modifier
                    .offset(step * c, step * r)
                    .size(tileSize)
                    .background(Gold.copy(alpha = 0.10f + 0.12f * pulse), RoundedCornerShape(22))
                    .border(3.dp, Gold.copy(alpha = 0.35f + 0.65f * pulse), RoundedCornerShape(22))
            )
        }

        // 2) estela, flecha y onda del toque
        Canvas(Modifier.fillMaxSize()) {
            val a = fade
            if (slide > 0.01f) {
                val end = tip
                drawLine(
                    brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = 0.55f * a)), start, end),
                    start = start, end = end, strokeWidth = tilePx * 0.34f, cap = StrokeCap.Round
                )
                // punta de flecha al final del trazo
                val ax = -dy; val ay = dx   // perpendicular
                val s = tilePx * 0.22f
                val head = Path().apply {
                    moveTo(end.x + dx * s * 1.2f, end.y + dy * s * 1.2f)
                    lineTo(end.x - dx * s * 0.6f + ax * s, end.y - dy * s * 0.6f + ay * s)
                    lineTo(end.x - dx * s * 0.6f - ax * s, end.y - dy * s * 0.6f - ay * s)
                    close()
                }
                drawPath(head, Color.White.copy(alpha = 0.85f * a))
                drawPath(head, Gold.copy(alpha = 0.9f * a), style = Stroke(width = 3f, join = StrokeJoin.Round))
            }
            // onda al tocar
            val ring = (t / 0.30f).coerceIn(0f, 1f)
            if (t < 0.30f) {
                drawCircle(Color.White.copy(alpha = 0.6f * (1f - ring)), tilePx * (0.2f + 0.45f * ring), start, style = Stroke(width = 5f))
            }
            drawCircle(Gold.copy(alpha = 0.35f * a), tilePx * 0.22f, tip)
        }

        // 3) la mano
        val handSize = (tileSize * 1.15f).coerceIn(60.dp, 104.dp)
        val handPx = with(density) { handSize.toPx() }
        Box(
            Modifier
                .offset { IntOffset((tip.x - TIP_X * handPx).roundToInt(), (tip.y - TIP_Y * handPx).roundToInt()) }
                .size(handSize)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(TIP_X, TIP_Y)
                    rotationZ = rotation
                    scaleX = press; scaleY = press
                    alpha = fade
                }
        ) {
            // sombra suave apoyada en el suelo
            Image(
                painterResource(R.drawable.ico_hand), contentDescription = null,
                colorFilter = ColorFilter.tint(Color.Black.copy(alpha = 0.28f)),
                modifier = Modifier.fillMaxSize().offset(4.dp, 7.dp)
            )
            Image(painterResource(R.drawable.ico_hand), contentDescription = null, modifier = Modifier.fillMaxSize())
        }
    }
}

/**
 * Tarjeta del tutorial: la mano, qué hacer ahora y tres puntitos con el avance. Ocupa el sitio de los poderes
 * mientras se aprende (así no hay nada más que mirar).
 */
@Composable
internal fun CoachCard(step: CoachStep, modifier: Modifier = Modifier) {
    JellyCard(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(26.dp),
        fill = Color.White,
        lip = Sage.copy(alpha = 0.55f),
        lipHeight = 6.dp
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.wobble(6f, 1500)) {
                Image(painterResource(R.drawable.ico_hand), contentDescription = null, modifier = Modifier.size(54.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("TUTORIAL", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Sage, letterSpacing = 2.sp)
                    Spacer(Modifier.width(8.dp))
                    repeat(step.mergesNeeded) { i ->
                        Box(
                            Modifier.padding(end = 4.dp).size(8.dp)
                                .background(if (i < step.mergesDone) Gold else Navy.copy(alpha = 0.12f), CircleShape)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                AnimatedContent(
                    targetState = step.text,
                    transitionSpec = { (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut()) },
                    label = "coachText"
                ) { text ->
                    Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Navy, lineHeight = 19.sp)
                }
            }
        }
    }
}
