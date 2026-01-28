package com.example.pardos.ui.game.components

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlinx.coroutines.isActive
import kotlin.random.Random

private data class SakuraParticle(
    var x: Float,
    var y: Float,
    val size: Float,
    val speedY: Float,
    val speedX: Float,
    var rotation: Float,
    val rotationSpeed: Float,
    val colorAlpha: Float
)

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun SakuraBackgroundAnimation(
    modifier: Modifier = Modifier,
    density: Float = 0.4f // Controla la cantidad de pétalos (0.1 a 1.0 recomendado)
) {
    val sakuraPink = Color(0xFFFFB7C5) // Color rosa sakura aesthetic

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        // Calculamos cuántas partículas crear basado en el tamaño de pantalla y densidad
        // coerceIn asegura que no sean demasiadas ni muy pocas
        val particleCount = (width * height * 0.0001f * density).toInt().coerceIn(20, 150)

        // Estado mutable para la lista de partículas
        val particles = remember { mutableStateListOf<SakuraParticle>() }

        // Inicialización de partículas
        LaunchedEffect(width, height) {
            particles.clear()
            repeat(particleCount) {
                particles.add(
                    SakuraParticle(
                        x = Random.nextFloat() * width,
                        y = Random.nextFloat() * height * -1f, // Empiezan arriba fuera de pantalla
                        size = Random.nextFloat() * 15f + 10f, // Tamaño variable
                        speedY = Random.nextFloat() * 2f + 1f, // Velocidad de caída
                        speedX = (Random.nextFloat() - 0.5f) * 1.5f, // Deriva lateral suave
                        rotation = Random.nextFloat() * 360f,
                        rotationSpeed = (Random.nextFloat() - 0.5f) * 5f, // Velocidad de rotación
                        colorAlpha = Random.nextFloat() * 0.4f + 0.4f // Transparencia variable
                    )
                )
            }
        }

        // Bucle de animación
        LaunchedEffect(Unit) {
            while (isActive) {
                // Actualización de estado (sincronizado con los frames de la pantalla)
                withFrameNanos { time ->
                    particles.forEach { p ->
                        p.y += p.speedY
                        p.x += p.speedX
                        p.rotation += p.rotationSpeed

                        // Si sale por abajo, reciclar arriba
                        if (p.y > height + p.size) {
                            p.y = -p.size
                            p.x = Random.nextFloat() * width
                        }
                        // Si sale por los lados (raro pero posible), envolver
                        if (p.x > width + p.size) p.x = -p.size
                        else if (p.x < -p.size) p.x = width + p.size
                    }
                }
            }
        }

        // Dibujado en Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                rotate(degrees = p.rotation, pivot = Offset(p.x, p.y)) {
                    // Dibujamos un óvalo que parece un pétalo
                    drawOval(
                        color = sakuraPink.copy(alpha = p.colorAlpha),
                        topLeft = Offset(p.x - p.size / 2, p.y - p.size / 4),
                        size = Size(p.size, p.size / 1.8f)
                    )
                }
            }
        }
    }
}