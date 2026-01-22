package com.korkoor.pardos.ui.game.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.korkoor.pardos.domain.logic.Direction
import kotlin.math.roundToInt

@Composable
fun TutorialHand(direction: Direction) {
    val infiniteTransition = rememberInfiniteTransition(label = "proHandAnimation")

    // 1. Animación de Progreso (0.0 a 1.0 para coordinar todo)
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    // 2. Lógica de Movimiento según dirección
    // Ajustamos la distancia (100dp es un buen estándar para una celda)
    val distance = 100f
    val (offsetX, offsetY) = when (direction) {
        Direction.RIGHT -> Pair(distance * progress, 0f)
        Direction.LEFT -> Pair(-distance * progress, 0f)
        Direction.UP -> Pair(0f, -distance * progress)
        Direction.DOWN -> Pair(0f, distance * progress)
    }

    // 3. Efecto de Escala (Simula la presión del dedo)
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000
                1.0f at 0           // Reposo
                1.3f at 300 with FastOutSlowInEasing // Presiona
                1.3f at 1500        // Mantiene durante el deslizamiento
                1.0f at 1800        // Suelta
            }
        ),
        label = "handScale"
    )

    // 4. Opacidad Suave
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000
                0f at 0
                1f at 300
                1f at 1500
                0f at 1800
            }
        ),
        label = "handAlpha"
    )

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.dp.roundToPx(), offsetY.dp.roundToPx()) }
            .alpha(alpha)
            .scale(scale)
    ) {
        Icon(
            imageVector = Icons.Default.TouchApp,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = Color.White
        )
    }
}