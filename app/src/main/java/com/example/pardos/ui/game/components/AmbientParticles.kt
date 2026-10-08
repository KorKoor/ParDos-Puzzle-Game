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
        ParticleKind.HEARTS -> 14
        ParticleKind.BATS -> 8
        ParticleKind.MARIGOLD -> 14
        ParticleKind.FLOWERS -> 16
        ParticleKind.CONFETTI, ParticleKind.FLAG_CONFETTI -> 34
        ParticleKind.FIREWORKS -> 5
        ParticleKind.SNOWFLAKE -> 18
        ParticleKind.SPARKLES -> 22
        ParticleKind.METEORS -> 5
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
                ParticleKind.HEARTS -> {
                    val x = w * (p.x + sin((prog + p.phase) * 2 * PI).toFloat() * p.sway * 2)
                    val y = h * (1.05f - prog * 1.1f)
                    drawHeart(Offset(x, y), unit * 1.5f * p.size, tint.copy(alpha = 0.5f * (1f - (prog - 0.8f).coerceAtLeast(0f) * 5f)))
                }
                ParticleKind.BATS -> {
                    val dir = if (p.hue % 2 == 0) 1f else -1f
                    val x = if (dir > 0) w * (-0.15f + prog * 1.3f) else w * (1.15f - prog * 1.3f)
                    val y = h * (0.08f + (p.phase * 0.55f)) + sin((prog * 6f + p.phase) * 2 * PI).toFloat() * unit * 2f
                    val flap = 0.5f + 0.5f * sin((t * 2 * PI * loops * 14 + p.phase * 6.28).toFloat())
                    drawBat(Offset(x, y), unit * 2.6f * p.size, flap, tint.copy(alpha = 0.8f))
                }
                ParticleKind.MARIGOLD -> {
                    val x = w * (p.x + sin((prog + p.phase) * 2 * PI).toFloat() * p.sway * 3)
                    drawFlower(
                        Offset(x, h * prog), unit * 2.7f * p.size, p.spin + prog * 200f, petals = 9, layers = 2,
                        outer = tint.copy(alpha = 0.85f), inner = Color(0xFFFFD35A).copy(alpha = 0.9f), core = Color(0xFFB8580A)
                    )
                }
                ParticleKind.FLOWERS -> {
                    val colors = listOf(tint, Color(0xFFFFD36E), Color(0xFFA8E6CF), Color(0xFFFFFFFF), Color(0xFFC8A2FF))
                    val x = w * (p.x + sin((prog + p.phase) * 2 * PI).toFloat() * p.sway * 3)
                    drawFlower(
                        Offset(x, h * prog), unit * 2.2f * p.size, p.spin + prog * 160f, petals = 5, layers = 1,
                        outer = colors[p.hue].copy(alpha = 0.85f), inner = colors[p.hue], core = Color(0xFFFFB703)
                    )
                }
                ParticleKind.CONFETTI, ParticleKind.FLAG_CONFETTI -> {
                    val colors = if (kind == ParticleKind.FLAG_CONFETTI)
                        listOf(Color(0xFF2FA866), Color(0xFFFFFFFF), Color(0xFFD7263D), Color(0xFFE9C46A), Color(0xFF2FA866))
                    else listOf(tint, Color(0xFFFFD36E), Color(0xFF9BE3D0), Color(0xFFB9A6FF), Color(0xFFFF9BB5))
                    val x = w * (p.x + sin((prog + p.phase) * 2 * PI).toFloat() * p.sway * 2)
                    val y = h * prog
                    val flutter = kotlin.math.abs(kotlin.math.cos((prog * 9f + p.phase * 6.28f)))
                    rotate(p.spin + prog * 360f, pivot = Offset(x, y)) {
                        drawRect(
                            colors[p.hue].copy(alpha = 0.9f), topLeft = Offset(x - unit * 0.8f, y - unit * 0.45f * flutter),
                            size = Size(unit * 1.6f * p.size, unit * 0.9f * flutter * p.size + 1f)
                        )
                    }
                }
                ParticleKind.FIREWORKS -> {
                    val colors = listOf(tint, Color(0xFFFFFFFF), Color(0xFFFF8FB1), Color(0xFF7FDBFF), tint)
                    val local = (prog * 1.6f)
                    if (local < 1f) {
                        val cx = w * (0.12f + p.x * 0.76f)
                        val cy = h * (0.12f + ((p.phase * 3.7f) % 1f) * 0.4f)
                        val e = 1f - (1f - local) * (1f - local) // ease-out: explota rápido y se frena
                        val radius = unit * 16f * p.size * e
                        val alpha = (1f - local).coerceIn(0f, 1f)
                        for (k in 0 until 14) {
                            val ang = (k / 14f) * 2f * PI.toFloat() + p.spin
                            val px = cx + kotlin.math.cos(ang) * radius
                            val py = cy + kotlin.math.sin(ang) * radius + unit * 4f * local * local
                            drawCircle(colors[p.hue].copy(alpha = alpha), radius = unit * 0.45f * (1f - local * 0.5f), center = Offset(px, py))
                            drawCircle(colors[p.hue].copy(alpha = alpha * 0.3f), radius = unit * 1.0f, center = Offset(px, py))
                        }
                    }
                }
                ParticleKind.SNOWFLAKE -> {
                    val x = w * (p.x + sin((prog + p.phase) * 2 * PI).toFloat() * p.sway * 2)
                    drawSnowflake(Offset(x, h * prog), unit * 1.7f * p.size, p.spin + prog * 240f, Color.White.copy(alpha = 0.85f))
                }
                ParticleKind.SPARKLES -> {
                    val twinkle = ((sin((t * 2 * PI * loops * 2 + p.phase * 6.28).toFloat()) + 1f) / 2f)
                    drawSparkle(
                        Offset(w * p.x, h * ((p.phase * 7) % 1f)), unit * 1.8f * p.size * (0.3f + 0.7f * twinkle),
                        tint.copy(alpha = 0.25f + 0.75f * twinkle)
                    )
                }
                ParticleKind.METEORS -> {
                    val local = (prog * 2.8f)
                    if (local < 1f) {
                        val hx = w * (p.x * 0.9f + 0.25f) - local * w * 0.55f
                        val hy = -h * 0.05f + local * h * 0.75f
                        val len = unit * 14f * p.size
                        val ux = 0.57f; val uy = -0.82f // la cola va hacia arriba y a la derecha
                        for (k in 0 until 8) {
                            val f = k / 8f
                            drawCircle(
                                tint.copy(alpha = (1f - f) * 0.8f * (1f - local * 0.4f)), radius = unit * (0.45f - f * 0.3f),
                                center = Offset(hx + ux * len * f, hy + uy * len * f)
                            )
                        }
                        drawCircle(tint.copy(alpha = 0.3f), radius = unit * 1.2f, center = Offset(hx, hy))
                    }
                }
                else -> Unit
            }
        }
    }
}

private fun DrawScope.drawHeart(c: Offset, s: Float, color: Color) {
    val path = Path().apply {
        moveTo(c.x, c.y + s * 0.9f)
        cubicTo(c.x - s * 1.3f, c.y + s * 0.1f, c.x - s * 0.8f, c.y - s * 0.9f, c.x, c.y - s * 0.35f)
        cubicTo(c.x + s * 0.8f, c.y - s * 0.9f, c.x + s * 1.3f, c.y + s * 0.1f, c.x, c.y + s * 0.9f)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawBat(c: Offset, s: Float, flap: Float, color: Color) {
    val up = s * (0.4f + 0.8f * flap)
    val tip = s * (0.1f + 0.5f * flap)
    val path = Path().apply {
        moveTo(c.x, c.y)
        quadraticTo(c.x - s * 0.6f, c.y - up, c.x - s * 1.7f, c.y - tip)
        quadraticTo(c.x - s * 1.2f, c.y + s * 0.1f, c.x - s * 0.9f, c.y + s * 0.35f)
        quadraticTo(c.x - s * 0.6f, c.y + s * 0.05f, c.x - s * 0.35f, c.y + s * 0.4f)
        quadraticTo(c.x - s * 0.15f, c.y + s * 0.1f, c.x, c.y + s * 0.35f)
        quadraticTo(c.x + s * 0.15f, c.y + s * 0.1f, c.x + s * 0.35f, c.y + s * 0.4f)
        quadraticTo(c.x + s * 0.6f, c.y + s * 0.05f, c.x + s * 0.9f, c.y + s * 0.35f)
        quadraticTo(c.x + s * 1.2f, c.y + s * 0.1f, c.x + s * 1.7f, c.y - tip)
        quadraticTo(c.x + s * 0.6f, c.y - up, c.x, c.y)
        close()
    }
    drawPath(path, color)
    // Cuerpo y orejitas
    drawCircle(color, radius = s * 0.28f, center = Offset(c.x, c.y + s * 0.05f))
    drawPath(Path().apply {
        moveTo(c.x - s * 0.22f, c.y - s * 0.1f); lineTo(c.x - s * 0.12f, c.y - s * 0.5f); lineTo(c.x - s * 0.02f, c.y - s * 0.12f); close()
        moveTo(c.x + s * 0.22f, c.y - s * 0.1f); lineTo(c.x + s * 0.12f, c.y - s * 0.5f); lineTo(c.x + s * 0.02f, c.y - s * 0.12f); close()
    }, color)
}

/** Flor de [layers] anillos de pétalos redondos (cempasúchil: 2 anillos; flor sencilla: 1). */
private fun DrawScope.drawFlower(
    c: Offset, r: Float, angle: Float, petals: Int, layers: Int, outer: Color, inner: Color, core: Color
) {
    rotate(angle, pivot = c) {
        for (layer in 0 until layers) {
            val ringR = r * (1f - layer * 0.35f)
            val petalR = ringR * (if (petals > 6) 0.34f else 0.42f)
            val color = if (layer == 0) outer else inner
            for (k in 0 until petals) {
                val a = (k.toFloat() / petals) * 2f * PI.toFloat() + layer * 0.3f
                drawCircle(color, radius = petalR, center = Offset(c.x + kotlin.math.cos(a) * ringR * 0.62f, c.y + kotlin.math.sin(a) * ringR * 0.62f))
            }
        }
        drawCircle(core, radius = r * 0.22f, center = c)
    }
}

private fun DrawScope.drawSnowflake(c: Offset, r: Float, angle: Float, color: Color) {
    rotate(angle, pivot = c) {
        val stroke = r * 0.14f
        for (k in 0 until 3) {
            val a = k * (PI.toFloat() / 3f)
            val dx = kotlin.math.cos(a) * r
            val dy = kotlin.math.sin(a) * r
            drawLine(color, Offset(c.x - dx, c.y - dy), Offset(c.x + dx, c.y + dy), strokeWidth = stroke)
            // Ramitas cerca de las puntas
            for (sign in listOf(-1f, 1f)) {
                val bx = c.x + dx * 0.6f * sign
                val by = c.y + dy * 0.6f * sign
                val perpX = -kotlin.math.sin(a) * r * 0.28f
                val perpY = kotlin.math.cos(a) * r * 0.28f
                drawLine(color, Offset(bx - perpX, by - perpY), Offset(bx + perpX, by + perpY), strokeWidth = stroke * 0.8f)
            }
        }
    }
}

private fun DrawScope.drawSparkle(c: Offset, r: Float, color: Color) {
    val path = Path().apply {
        moveTo(c.x, c.y - r)
        quadraticTo(c.x, c.y, c.x + r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y + r)
        quadraticTo(c.x, c.y, c.x - r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y - r)
        close()
    }
    drawPath(path, color)
    drawCircle(color.copy(alpha = color.alpha * 0.25f), radius = r * 0.9f, center = c)
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
