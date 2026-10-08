package com.korkoor.pardos.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import com.korkoor.pardos.domain.shop.BannerDef
import com.korkoor.pardos.domain.shop.BannerPattern
import com.korkoor.pardos.domain.shop.Banners
import com.korkoor.pardos.ui.design.LocalCozyClock
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Banner de perfil dibujado: degradado + un dibujo suave que se mueve despacio. */
@Composable
fun ProfileBanner(bannerId: Int, modifier: Modifier = Modifier, animate: Boolean = true) {
    ProfileBanner(Banners.byId(bannerId), modifier, animate)
}

@Composable
fun ProfileBanner(def: BannerDef, modifier: Modifier = Modifier, animate: Boolean = true) {
    val clock = LocalCozyClock.current
    Canvas(modifier) {
        // Espacio de diseño: 120 de alto; el ancho sale de la proporción real
        val u = size.height / 120f
        val t = if (animate) clock.value else 0.25f
        scale(u, u, pivot = Offset.Zero) { drawBanner(def, size.width / u, 120f, t) }
    }
}

private fun c(v: Long) = Color(v)

private fun DrawScope.drawBanner(def: BannerDef, w: Float, h: Float, t: Float) {
    val top = c(def.top)
    val bottom = c(def.bottom)
    val accent = c(def.accent)
    drawRect(Brush.verticalGradient(listOf(top, bottom), 0f, h), size = Size(w, h))
    val r = Random(def.id * 7919)

    when (def.pattern) {
        BannerPattern.CHECKER -> {
            val s = 20f
            var yy = 0
            var y = 0f
            while (y < h) {
                var x = if (yy % 2 == 0) 0f else s
                while (x < w) { drawRect(accent.copy(alpha = 0.22f), Offset(x, y), Size(s, s)); x += s * 2 }
                y += s; yy++
            }
        }
        BannerPattern.DOTS -> {
            var y = 10f
            var row = 0
            while (y < h + 10) {
                var x = if (row % 2 == 0) 8f else 20f
                while (x < w + 10) { drawCircle(accent.copy(alpha = 0.32f), 3.2f, Offset(x, y)); x += 24f }
                y += 18f; row++
            }
        }
        BannerPattern.HILLS -> {
            // sol y nubes suaves
            drawCircle(Color.White.copy(alpha = 0.55f), 15f, Offset(w * 0.82f, 28f))
            drawCircle(Color.White.copy(alpha = 0.25f), 24f, Offset(w * 0.82f, 28f))
            for (k in 0..1) {
                val cx = ((w * (0.15f + 0.45f * k) + t * 40f * (k + 1)) % (w + 60f)) - 20f
                drawRoundRect(Color.White.copy(alpha = 0.6f), Offset(cx, 22f + k * 14f), Size(40f, 10f), CornerRadius(5f))
            }
            hills(w, h, accent, 0.55f, 62f, 0.9f)
            hills(w, h, lerp(accent, Color.Black, 0.12f), 0.8f, 82f, 1.7f)
            hills(w, h, lerp(accent, Color.Black, 0.25f), 1.1f, 100f, 2.6f)
        }
        BannerPattern.PETALS, BannerPattern.LEAVES -> {
            val leaf = def.pattern == BannerPattern.LEAVES
            for (i in 0 until 16) {
                val px = r.nextFloat() * w
                val sp = 0.4f + r.nextFloat() * 0.6f
                val ph = r.nextFloat()
                val prog = (ph + t * (if (sp > 0.7f) 2 else 1)) % 1f
                val x = (px + sin((prog * 2 * PI + i).toFloat()) * 10f) % w
                val y = prog * (h + 20f) - 10f
                val ang = r.nextFloat() * 360f + prog * 220f
                val s = 4f + r.nextFloat() * 4f
                rotate(ang, pivot = Offset(x, y)) {
                    if (leaf) {
                        val p = Path().apply {
                            moveTo(x, y - s * 1.6f); quadraticTo(x + s, y, x, y + s * 1.6f); quadraticTo(x - s, y, x, y - s * 1.6f)
                        }
                        drawPath(p, (if (i % 3 == 0) lerp(accent, Color.Black, 0.2f) else accent).copy(alpha = 0.75f))
                    } else {
                        drawOval(accent.copy(alpha = 0.7f), Offset(x - s, y - s * 0.6f), Size(s * 2, s * 1.3f))
                    }
                }
            }
        }
        BannerPattern.WAVES -> {
            for (k in 0..2) {
                val base = 62f + k * 20f
                val p = Path().apply {
                    moveTo(0f, h)
                    var x = 0f
                    while (x <= w + 6f) {
                        lineTo(x, base + sin(((x / 38f) + t * 2 * PI * (1 + k) * (if (k % 2 == 0) 1 else -1)).toDouble()).toFloat() * (5f + k * 1.5f))
                        x += 6f
                    }
                    lineTo(w, h); close()
                }
                drawPath(p, lerp(accent, Color.White, 0.35f - k * 0.12f).copy(alpha = 0.55f + k * 0.15f))
            }
            drawCircle(Color.White.copy(alpha = 0.5f), 12f, Offset(w * 0.85f, 24f))
        }
        BannerPattern.BUBBLES -> {
            for (i in 0 until 14) {
                val x = r.nextFloat() * w
                val sp = if (r.nextBoolean()) 1 else 2
                val prog = (r.nextFloat() + t * sp) % 1f
                val rad = 3f + r.nextFloat() * 8f
                val cy = h + 10f - prog * (h + 20f)
                drawCircle(accent.copy(alpha = 0.16f), rad, Offset(x + sin((prog * 6f + i).toDouble()).toFloat() * 4f, cy))
                drawCircle(accent.copy(alpha = 0.55f), rad, Offset(x + sin((prog * 6f + i).toDouble()).toFloat() * 4f, cy), style = Stroke(1.2f))
                drawCircle(Color.White.copy(alpha = 0.7f), rad * 0.22f, Offset(x - rad * 0.3f, cy - rad * 0.3f))
            }
        }
        BannerPattern.SNOW -> {
            for (i in 0 until 26) {
                val px = r.nextFloat() * w
                val sp = if (i % 3 == 0) 2 else 1
                val prog = (r.nextFloat() + t * sp) % 1f
                val rad = 1.2f + r.nextFloat() * 2.8f
                drawCircle(Color.White.copy(alpha = if (c(def.bottom).red > 0.7f) 0.95f else 0.8f), rad, Offset(px + sin((prog * 6f + i).toDouble()).toFloat() * 5f, prog * (h + 8f) - 4f))
            }
            hills(w, h, Color.White, 0.35f, 100f, 3.2f)
        }
        BannerPattern.DIAMONDS -> {
            val cols = listOf(accent, Color(0xFF9BE3D0), Color(0xFFFFD36E), Color(0xFFB9A6FF))
            for (i in 0 until 22) {
                val x = r.nextFloat() * w
                val y = r.nextFloat() * h
                val s = 4f + r.nextFloat() * 6f
                rotate(45f + (t * 360f * (if (i % 2 == 0) 1 else -1)) % 360f, pivot = Offset(x, y)) {
                    drawRoundRect(cols[i % cols.size].copy(alpha = 0.55f), Offset(x - s, y - s), Size(s * 2, s * 2), CornerRadius(2f))
                }
            }
        }
        BannerPattern.RAYS -> {
            val cx = w * 0.78f
            val cy = h * 0.95f
            rotate(t * 360f / 3f, pivot = Offset(cx, cy)) {
                for (i in 0 until 14) {
                    val a0 = (i * 360f / 14f) * PI.toFloat() / 180f
                    val a1 = a0 + (360f / 28f) * PI.toFloat() / 180f
                    val p = Path().apply {
                        moveTo(cx, cy)
                        lineTo(cx + cos(a0) * 400f, cy + sin(a0) * 400f)
                        lineTo(cx + cos(a1) * 400f, cy + sin(a1) * 400f)
                        close()
                    }
                    drawPath(p, accent.copy(alpha = 0.22f))
                }
            }
            drawCircle(accent.copy(alpha = 0.55f), 20f, Offset(cx, cy))
            drawCircle(Color.White.copy(alpha = 0.7f), 12f, Offset(cx, cy))
        }
        BannerPattern.STARS -> {
            for (i in 0 until 38) {
                val x = r.nextFloat() * w
                val y = r.nextFloat() * h * 0.9f
                val tw = 0.3f + 0.7f * ((sin((t * 2 * PI * (1 + i % 3) + i).toDouble()).toFloat() + 1f) / 2f)
                drawCircle(accent.copy(alpha = tw), 0.7f + r.nextFloat() * 1.3f, Offset(x, y))
            }
            drawCircle(accent.copy(alpha = 0.9f), 11f, Offset(w * 0.84f, 30f))
            drawCircle(c(def.top), 9.5f, Offset(w * 0.84f + 4f, 27f))
        }
        BannerPattern.AURORA -> {
            for (k in 0..2) {
                val col = listOf(accent, Color(0xFF8EC5FF), Color(0xFFB69CFF))[k]
                val p = Path().apply {
                    moveTo(0f, 0f)
                    var x = 0f
                    while (x <= w + 8f) {
                        lineTo(x, 28f + k * 14f + sin(((x / 55f) + t * 2 * PI * (k + 1)).toDouble()).toFloat() * 11f)
                        x += 8f
                    }
                    lineTo(w, 0f); close()
                }
                drawPath(p, Brush.verticalGradient(listOf(col.copy(alpha = 0.0f), col.copy(alpha = 0.42f)), 0f, 70f))
            }
            for (i in 0 until 22) drawCircle(Color.White.copy(alpha = 0.3f + 0.5f * ((sin((t * 2 * PI + i * 2).toDouble()).toFloat() + 1f) / 2f)), 0.8f + r.nextFloat(), Offset(r.nextFloat() * w, r.nextFloat() * h * 0.6f))
        }
        BannerPattern.SKYLINE -> {
            drawCircle(accent.copy(alpha = 0.22f), 38f, Offset(w * 0.3f, 80f))
            drawCircle(accent.copy(alpha = 0.45f), 17f, Offset(w * 0.3f, 80f))
            var x = 0f
            while (x < w) {
                val bw = 14f + r.nextFloat() * 18f
                val bh = 24f + r.nextFloat() * 44f
                drawRect(Color(0xFF0F0A22), Offset(x, h - bh), Size(bw, bh))
                var wy = h - bh + 5f
                while (wy < h - 4f) {
                    var wx = x + 3f
                    while (wx < x + bw - 3f) {
                        if (r.nextFloat() > 0.45f) drawRect(accent.copy(alpha = 0.8f), Offset(wx, wy), Size(2.4f, 3f))
                        wx += 6f
                    }
                    wy += 8f
                }
                x += bw + 1f
            }
        }
        BannerPattern.EMBERS -> {
            val p = Path().apply {
                moveTo(0f, h)
                lineTo(0f, 84f); lineTo(w * 0.18f, 60f); lineTo(w * 0.3f, 80f); lineTo(w * 0.5f, 44f)
                lineTo(w * 0.62f, 74f); lineTo(w * 0.8f, 56f); lineTo(w, 82f); lineTo(w, h); close()
            }
            drawPath(p, Color(0xFF14090A))
            drawOval(accent.copy(alpha = 0.5f), Offset(w * 0.5f - 10f, 44f), Size(20f, 7f))
            for (i in 0 until 20) {
                val x = w * 0.5f + (r.nextFloat() - 0.5f) * w * 0.7f
                val prog = (r.nextFloat() + t * (1 + i % 2)) % 1f
                drawCircle(accent.copy(alpha = (1f - prog) * 0.9f), 0.9f + r.nextFloat() * 1.6f, Offset(x + sin((prog * 7f + i).toDouble()).toFloat() * 5f, 70f - prog * 80f))
            }
        }
    }
}

private fun DrawScope.hills(w: Float, h: Float, color: Color, alpha: Float, base: Float, seed: Float) {
    val p = Path().apply {
        moveTo(0f, h)
        var x = 0f
        while (x <= w + 8f) {
            lineTo(x, base + sin((x / 46f + seed).toDouble()).toFloat() * 9f + sin((x / 21f + seed * 2).toDouble()).toFloat() * 3f)
            x += 8f
        }
        lineTo(w, h); close()
    }
    drawPath(p, color.copy(alpha = alpha))
}
