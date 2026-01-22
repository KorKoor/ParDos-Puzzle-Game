package com.example.pardos.ui.game.menu

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

/**
 * Un fondo aesthetic optimizado que dibuja un patrón de cuadrícula inclinado
 * y figuras geométricas con movimiento infinito.
 */
@Composable
fun PicnicBackgroundOptimized(color: Color) { // 👈 Cambiado el nombre a 'color' para consistencia
    // Animación infinita para la rotación sutil de las figuras decorativas
    val infiniteTransition = rememberInfiniteTransition(label = "BackgroundParticles")
    val animValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing)
        ),
        label = "Rotation"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        // --- 1. PATRÓN DE CUADRÍCULA (MANTEL) ---
        rotate(-15f, pivot = center) {
            val step = 32.dp.toPx() // Aumentamos un poco el tamaño para un look más "clean"
            val extraSpace = size.maxDimension * 1.5f

            val startX = -extraSpace / 2
            val startY = -extraSpace / 2
            val endX = size.width + extraSpace / 2
            val endY = size.height + extraSpace / 2

            var xPos = startX
            while (xPos < endX) {
                var yPos = startY
                while (yPos < endY) {
                    val xIndex = ((xPos - startX) / step).toInt()
                    val yIndex = ((yPos - startY) / step).toInt()

                    if ((xIndex + yIndex) % 2 != 0) {
                        drawRect(
                            color = color, // 👈 Usa el color pasado por parámetro
                            topLeft = Offset(xPos, yPos),
                            size = Size(step, step)
                        )
                    }
                    yPos += step
                }
                xPos += step
            }
        }

        // --- 2. FIGURAS GEOMÉTRICAS FLOTANTES ---
        // Usamos el mismo color del parámetro con una opacidad mínima para coherencia estética
        val particleColor = color.copy(alpha = color.alpha * 1.5f)

        fun drawFloatingShape(
            pivotX: Float,
            pivotY: Float,
            speed: Float,
            sizePx: Float,
            isCircle: Boolean
        ) {
            rotate(animValue * speed, pivot = Offset(size.width * pivotX, size.height * pivotY)) {
                val centerOffset = Offset(size.width * pivotX, size.height * pivotY)
                val topLeft = Offset(centerOffset.x - sizePx / 2, centerOffset.y - sizePx / 2)

                if (isCircle) {
                    drawCircle(
                        color = particleColor,
                        radius = sizePx / 2,
                        center = centerOffset,
                        style = Stroke(width = 1.5f)
                    )
                } else {
                    drawRoundRect(
                        color = particleColor,
                        topLeft = topLeft,
                        size = Size(sizePx, sizePx),
                        cornerRadius = CornerRadius(sizePx * 0.3f),
                        style = Stroke(width = 1.5f)
                    )
                }
            }
        }

        val positions = listOf(
            Pair(0.1f, 0.2f), Pair(0.85f, 0.15f), Pair(0.5f, 0.1f),
            Pair(0.25f, 0.45f), Pair(0.75f, 0.55f), Pair(0.1f, 0.8f),
            Pair(0.9f, 0.85f), Pair(0.45f, 0.95f)
        )

        positions.forEachIndexed { i, pos ->
            drawFloatingShape(
                pivotX = pos.first,
                pivotY = pos.second,
                speed = if (i % 2 == 0) 0.8f else -0.8f,
                sizePx = 40f + (i * 10f),
                isCircle = i % 2 == 0
            )
        }
    }
}