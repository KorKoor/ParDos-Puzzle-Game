package com.korkoor.pardos.ui.game.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.korkoor.pardos.domain.logic.Direction
import kotlin.math.roundToInt

@Composable
fun TutorialHand(direction: Direction) {
    // --- CONFIGURACIÓN DE COLORES ---
    val handColor = Color.White
    // Sombra oscura pero sutil
    val shadowColor = Color.Black.copy(alpha = 0.25f)
    val trailColorStart = Color.White.copy(alpha = 0.8f)
    val trailColorEnd = Color.White.copy(alpha = 0.0f)
    val touchPointColor = Color(0xFFFFF176)

    val infiniteTransition = rememberInfiniteTransition(label = "RealShadowAnim")
    val animDuration = 2000

    // --- TIMELINE MAESTRO ---
    val t by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(animDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "masterTime"
    )

    // --- FÍSICAS ---
    val isTouching = t in 0.05f..0.85f

    // Escala (Squish)
    val handScale = when {
        t < 0.1f -> 1.0f + (0.2f * (t / 0.1f))
        t < 0.2f -> 1.2f - (0.3f * ((t - 0.1f) / 0.1f))
        t < 0.8f -> 0.9f
        t < 0.9f -> 0.9f + (0.1f * ((t - 0.8f) / 0.1f))
        else -> 1.0f
    }

    // Movimiento Cúbico
    val moveProgress = if (t < 0.2f) 0f else if (t > 0.7f) 1f else {
        val localT = (t - 0.2f) / 0.5f
        1f - (1f - localT) * (1f - localT) * (1f - localT)
    }

    // Opacidad Global
    val globalAlpha = when {
        t < 0.1f -> t / 0.1f
        t > 0.85f -> 1f - ((t - 0.85f) / 0.15f)
        else -> 1f
    }

    // --- CÁLCULO DE SOMBRA DINÁMICA ---
    // Si toca, la sombra está cerca (poco offset). Si levanta, lejos (mucho offset).
    val shadowOffsetDp = if (isTouching) 3.dp else 10.dp
    // La sombra se vuelve más nítida cuando se acerca al "suelo"
    val shadowAlphaFactor = if (isTouching) 1.5f else 1.0f

    // --- GEOMETRÍA ---
    val distance = 140f
    val startX = 0f
    val startY = 0f

    val endX = when (direction) {
        Direction.RIGHT -> distance
        Direction.LEFT -> -distance
        else -> 0f
    }
    val endY = when (direction) {
        Direction.DOWN -> distance
        Direction.UP -> -distance
        else -> 0f
    }

    val currentX = startX + (endX * moveProgress)
    val currentY = startY + (endY * moveProgress)

    val rotation = when (direction) {
        Direction.RIGHT -> -25f
        Direction.LEFT -> 25f
        Direction.UP -> 0f
        Direction.DOWN -> 180f
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(240.dp) // Un poco más espacio para la sombra
            .alpha(globalAlpha)
    ) {
        // --- CAPA 1: EL TRAZO ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = center
            if (moveProgress > 0.01f) {
                val pathStart = Offset(centerOffset.x + startX, centerOffset.y + startY)
                val pathEnd = Offset(centerOffset.x + currentX, centerOffset.y + currentY)

                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(trailColorStart, trailColorEnd),
                        start = pathStart,
                        end = pathEnd
                    ),
                    start = pathStart,
                    end = pathEnd,
                    strokeWidth = 24.dp.toPx(),
                    cap = StrokeCap.Round,
                    alpha = 0.4f * (1f - moveProgress) + 0.2f
                )
            }
        }

        // --- CAPA 2: PUNTO DE CONTACTO (GLOW) ---
        if (isTouching) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(currentX.roundToInt(), currentY.roundToInt()) }
                    .size(30.dp)
                    .graphicsLayer {
                        alpha = 0.6f
                        scaleX = if (moveProgress < 0.1f) 1.5f else 1f
                        scaleY = if (moveProgress < 0.1f) 1.5f else 1f
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(touchPointColor, Color.Transparent)
                        )
                    )
                }
            }
        }

        // --- CAPA 3: EL CONJUNTO MANO + SOMBRA ---
        // Usamos un Box contenedor que se mueve, rota y escala como una unidad.
        Box(
            modifier = Modifier
                .offset { IntOffset(currentX.roundToInt(), currentY.roundToInt()) }
                .size(64.dp)
                .graphicsLayer {
                    scaleX = handScale
                    scaleY = handScale
                    rotationZ = rotation
                }
        ) {
            // 🔥 SUB-CAPA 3.1: LA SOMBRA REALISTA (Icono negro detrás) 🔥
            // Se dibuja primero (detrás) con un ligero desplazamiento.
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = null,
                tint = shadowColor.copy(alpha = shadowColor.alpha * shadowAlphaFactor),
                modifier = Modifier
                    .fillMaxSize()
                    // El desplazamiento dinámico crea la sensación de altura
                    .offset(x = shadowOffsetDp, y = shadowOffsetDp)
            )

            // 🔥 SUB-CAPA 3.2: LA MANO BLANCA (Icono principal delante) 🔥
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = null,
                tint = handColor,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}