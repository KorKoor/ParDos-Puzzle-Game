package com.example.pardos.ui.game.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun EpicFusionFlash(active: Boolean) {
    // Usamos un trigger basado en el cambio de 'active'
    var trigger by remember { mutableStateOf(0) }
    LaunchedEffect(active) {
        if (active) trigger++
    }

    if (active) {
        val animProgress by animateFloatAsState(
            targetValue = 1f,
            // Usamos el trigger en la key para que la animación reinicie siempre
            animationSpec = tween(500, easing = FastOutSlowInEasing),
            label = "flash",
            finishedListener = { /* Opcional: callback al terminar */ }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(0.8f + (animProgress * 1.2f)) // Explosión más grande
                .alpha(1f - animProgress)
                .background(
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.8f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
    }
}

@Composable
fun VictoryConfetti() {
    val colors = listOf(Color(0xFF81B29A), Color(0xFFF2CC8F), Color(0xFFE07A5F), Color(0xFF3D405B))
    val particles = remember { List(50) { ImprovedConfettiState() } }
    val startTime = remember { System.currentTimeMillis() }

    val infiniteTransition = rememberInfiniteTransition(label = "victory")
    val elapsed by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing)),
        label = "time"
    )

    Canvas(modifier = Modifier.fillMaxSize().zIndex(100f)) {
        particles.forEachIndexed { i, p ->
            val time = elapsed + p.delay
            val gravity = 400f
            // Ecuación de caída + oscilación senoidal para el viento
            val yPos = (p.startY + (time * p.speed)) % (size.height + 100f)
            val xPos = (size.width * p.xRel) + (kotlin.math.sin(time / 20f) * 20f)

            rotate(time * p.rotationSpeed, pivot = Offset(xPos, yPos)) {
                drawRoundRect(
                    color = colors[i % colors.size].copy(alpha = 0.8f),
                    topLeft = Offset(xPos, yPos),
                    size = Size(12.dp.toPx(), 6.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
            }
        }
    }
}

private class ImprovedConfettiState {
    val xRel = java.util.Random().nextFloat()
    val startY = java.util.Random().nextFloat() * -500f
    val speed = 2f + java.util.Random().nextFloat() * 4f
    val delay = java.util.Random().nextFloat() * 1000f
    val rotationSpeed = java.util.Random().nextFloat() * 5f
}

private class ConfettiState {
    val xRel = Random.nextFloat()
    val startY = Random.nextFloat() * -1000f
    val speed = 0.5f + Random.nextFloat()
    val rotation = (Random.nextFloat() * 360f) * (if(Random.nextBoolean()) 1f else -1f)
}

@Composable
fun FusionParticleEffect(tileX: Dp, tileY: Dp, tileSize: Dp) {
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(400)
        visible = false
    }

    if (visible) {
        val anim = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            anim.animateTo(1f, animationSpec = tween(400, easing = LinearOutSlowInEasing))
        }

        Box(modifier = Modifier.size(tileSize).offset(tileX, tileY)) {
            // Dibujamos 8 partículas en círculo expandiéndose
            repeat(8) { i ->
                val angle = (i * 45) * (PI / 180f)
                val distance = anim.value * tileSize.value * 0.8f

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(
                            x = (kotlin.math.cos(angle) * distance).dp,
                            y = (kotlin.math.sin(angle) * distance).dp
                        )
                        .size((6 * (1f - anim.value)).dp) // Se encogen al alejarse
                        .alpha(1f - anim.value)
                        .background(Color.White, CircleShape)
                )
            }
        }
    }
}