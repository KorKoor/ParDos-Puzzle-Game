package com.korkoor.pardos.ui.game.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.pardos.ui.game.components.SakuraBackgroundAnimation
import com.korkoor.pardos.domain.shop.ParticleKind
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Partículas ambientales de la temática activa. Cada partícula tiene posición, velocidad y fase
 * fijas (semilla estable) y se anima con un único reloj, así que cuesta una sola animación.
 */
@Composable
fun AmbientParticles(
    kind: ParticleKind,
    tint: Color,
    modifier: Modifier = Modifier,
    density: Float = 1f
) {
    if (kind == ParticleKind.NONE) return

    // PETALS conserva la animación original del juego
    if (kind == ParticleKind.PETALS) {
        SakuraBackgroundAnimation(density = 0.5f * density)
        return
    }

    val count = (when (kind) {
        ParticleKind.STARS -> 36
        ParticleKind.SNOW -> 34
        ParticleKind.BUBBLES -> 18
        ParticleKind.EMBERS -> 24
        ParticleKind.LEAVES -> 16
        ParticleKind.SPRINKLES -> 28
        else -> 20
    } * density).toInt().coerceAtLeast(6)

    val particles = remember(kind, count) {
        val r = Random(kind.ordinal * 977L)
        List(count) {
            Particle(
                x = r.nextFloat(), phase = r.nextFloat(), speed = 0.35f + r.nextFloat() * 0.9f,
                size = 0.6f + r.nextFloat() * 1.0f, sway = 0.01f + r.nextFloat() * 0.03f,
                spin = r.nextFloat() * 360f, hue = r.nextInt(5)
            )
        }
    }

    val t by rememberInfiniteTransition(label = "particles").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart),
        label = "particlesClock"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val unit = size.minDimension / 100f
        particles.forEach { p ->
            // `loops` hace que cada partícula complete un número entero de vueltas por ciclo (sin saltos)
            val loops = (p.speed * 2).toInt().coerceAtLeast(1)
            val prog = ((p.phase + t * loops) % 1f)
            when (kind) {
                ParticleKind.SNOW -> {
                    val x = w * (p.x + sin((prog + p.phase) * 2 * PI).toFloat() * p.sway)
                    drawCircle(Color.White.copy(alpha = 0.85f), radius = unit * 0.7f * p.size, center = Offset(x, h * prog))
                }
                ParticleKind.LEAVES -> {
                    val x = w * (p.x + sin((prog + p.phase) * 2 * PI).toFloat() * p.sway * 3)
                    drawLeaf(Offset(x, h * prog), unit * 2.2f * p.size, p.spin + prog * 360f, tint.copy(alpha = 0.7f))
                }
                ParticleKind.BUBBLES -> {
                    val x = w * (p.x + sin((prog + p.phase) * 2 * PI).toFloat() * p.sway)
                    val y = h * (1f - prog)
                    val r = unit * 1.6f * p.size
                    drawCircle(tint.copy(alpha = 0.22f), radius = r, center = Offset(x, y))
                    drawCircle(tint.copy(alpha = 0.6f), radius = r, center = Offset(x, y), style = Stroke(unit * 0.25f))
                    drawCircle(Color.White.copy(alpha = 0.7f), radius = r * 0.22f, center = Offset(x - r * 0.35f, y - r * 0.35f))
                }
                ParticleKind.STARS -> {
                    val twinkle = 0.25f + 0.75f * ((sin((t * 2 * PI * loops + p.phase * 6.28).toFloat()) + 1f) / 2f)
                    drawCircle(tint.copy(alpha = twinkle), radius = unit * 0.45f * p.size, center = Offset(w * p.x, h * ((p.phase * 7) % 1f)))
                }
                ParticleKind.EMBERS -> {
                    val x = w * (p.x + sin((prog + p.phase) * 2 * PI).toFloat() * p.sway * 2)
                    val y = h * (1f - prog)
                    drawCircle(tint.copy(alpha = (1f - prog) * 0.85f), radius = unit * 0.55f * p.size, center = Offset(x, y))
                }
                ParticleKind.FIREFLIES -> {
                    val pulse = 0.2f + 0.8f * ((sin((t * 2 * PI * loops * 3 + p.phase * 6.28).toFloat()) + 1f) / 2f)
                    val c = Offset(w * (p.x + sin((t * 2 * PI + p.phase * 6.28).toFloat()) * 0.03f), h * ((p.phase * 5) % 1f))
                    drawCircle(tint.copy(alpha = pulse * 0.25f), radius = unit * 2f * p.size, center = c)
                    drawCircle(tint.copy(alpha = pulse), radius = unit * 0.5f * p.size, center = c)
                }
                ParticleKind.SPRINKLES -> {
                    val colors = listOf(tint, Color(0xFFFFD36E), Color(0xFF9BE3D0), Color(0xFFB9A6FF), Color(0xFFFF9BB5))
                    val x = w * (p.x + sin((prog + p.phase) * 2 * PI).toFloat() * p.sway)
                    rotate(p.spin + prog * 540f, pivot = Offset(x, h * prog)) {
                        drawRoundRect(
                            colors[p.hue], topLeft = Offset(x - unit * 0.3f, h * prog - unit * 1.1f),
                            size = Size(unit * 0.6f, unit * 2.2f * p.size),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(unit * 0.3f)
                        )
                    }
                }
                else -> Unit
            }
        }
    }
}

private data class Particle(
    val x: Float, val phase: Float, val speed: Float, val size: Float,
    val sway: Float, val spin: Float, val hue: Int
)

private fun DrawScope.drawLeaf(center: Offset, length: Float, angle: Float, color: Color) {
    rotate(angle, pivot = center) {
        val path = Path().apply {
            moveTo(center.x, center.y - length)
            quadraticTo(center.x + length * 0.7f, center.y, center.x, center.y + length)
            quadraticTo(center.x - length * 0.7f, center.y, center.x, center.y - length)
            close()
        }
        drawPath(path, color)
    }
}
