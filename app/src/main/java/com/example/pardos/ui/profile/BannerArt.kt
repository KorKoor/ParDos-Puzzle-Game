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

        BannerPattern.BATS -> {
            for (i in 0 until 26) drawCircle(Color.White.copy(alpha = 0.25f + 0.55f * ((sin((t * 2 * PI + i * 1.3).toDouble()).toFloat() + 1f) / 2f)), 0.7f + r.nextFloat() * 1.1f, Offset(r.nextFloat() * w, r.nextFloat() * h * 0.7f))
            drawCircle(accent.copy(alpha = 0.18f), 34f, Offset(w * 0.8f, 36f)); drawCircle(accent.copy(alpha = 0.3f), 24f, Offset(w * 0.8f, 36f)); drawCircle(Color(0xFFFFE9A8), 16f, Offset(w * 0.8f, 36f))
            drawCircle(Color(0xFFE6C97A).copy(alpha = 0.5f), 3f, Offset(w * 0.8f - 5f, 33f)); drawCircle(Color(0xFFE6C97A).copy(alpha = 0.5f), 2f, Offset(w * 0.8f + 5f, 42f))
            for (i in 0 until 5) {
                val prog = (r.nextFloat() + t * (0.5f + 0.25f * i)) % 1f
                bannerBat(w * (0.1f + 0.2f * i) + sin((prog * 6.28f + i).toDouble()).toFloat() * 8f, 14f + (i % 3) * 18f + sin((prog * 12.5f).toDouble()).toFloat() * 4f, 1.1f - 0.1f * i, Color(0xFF0A0618), 1f - 0.35f * ((t * 6f + i) % 2f).let { if (it > 1f) 2f - it else it })
            }
            hills(w, h, Color(0xFF0A0618), 0.95f, 98f, 1.3f)
            var tx = 8f
            while (tx < w) { drawRoundRect(Color(0xFF0A0618), Offset(tx, 82f), Size(7f, 22f), CornerRadius(3.5f)); tx += 26f + r.nextFloat() * 22f }
        }
        BannerPattern.PUMPKINS -> {
            for (i in 0 until 20) drawCircle(Color.White.copy(alpha = 0.2f + 0.5f * ((sin((t * 2 * PI + i * 1.1).toDouble()).toFloat() + 1f) / 2f)), 0.7f + r.nextFloat(), Offset(r.nextFloat() * w, r.nextFloat() * h * 0.55f))
            drawCircle(Color(0xFFFFE9A8).copy(alpha = 0.9f), 13f, Offset(w * 0.15f, 28f)); drawCircle(Color(0xFFFFE9A8).copy(alpha = 0.2f), 22f, Offset(w * 0.15f, 28f))
            hills(w, h, Color(0xFF1A0E26), 0.95f, 88f, 2.1f)
            var px = 14f; var k = 0
            while (px < w) {
                val sc = 0.8f + (k % 3) * 0.28f
                bannerPumpkin(px, 96f - sc * 4f, sc, accent, t, k)
                px += 30f + (k % 2) * 16f; k++
            }
        }
        BannerPattern.MOUNTAINS -> {
            val snow = def.isDark.not()
            drawCircle(Color.White.copy(alpha = if (snow) 0.6f else 0.3f), 14f, Offset(w * 0.78f, 26f)); drawCircle(Color.White.copy(alpha = 0.2f), 22f, Offset(w * 0.78f, 26f))
            for (layer in 0..2) {
                val col = lerp(accent, c(def.bottom), 0.55f - layer * 0.2f).copy(alpha = 0.5f + layer * 0.25f)
                val path = Path().apply {
                    moveTo(0f, h); lineTo(0f, 70f + layer * 14f)
                    var x = 0f; var up = true; var kk = 0
                    while (x < w + 30f) {
                        val peak = 36f + layer * 12f + ((kk * 37 + def.id * 11) % 22)
                        lineTo(x + 24f, peak + (if (layer == 2) 14f else 0f)); lineTo(x + 56f, 78f + layer * 14f)
                        x += 56f; kk++; up = !up
                    }
                    lineTo(w, h); close()
                }
                drawPath(path, col)
            }
            for (i in 0 until 3) { val cx = ((w * (0.1f + 0.35f * i) + t * 30f * (i + 1)) % (w + 60f)) - 20f; drawRoundRect(Color.White.copy(alpha = 0.55f), Offset(cx, 20f + i * 12f), Size(36f, 8f), CornerRadius(4f)) }
        }
        BannerPattern.FOREST -> {
            val dark = def.isDark
            if (dark) for (i in 0 until 14) {
                val prog = (r.nextFloat() + t * (1 + i % 2)) % 1f
                val a = (sin((prog * 3.14f).toDouble()).toFloat()).coerceIn(0f, 1f)
                drawCircle(accent.copy(alpha = 0.18f * a), 6f, Offset(r.nextFloat() * w, 40f + r.nextFloat() * 70f - prog * 12f)); drawCircle(accent.copy(alpha = 0.95f * a), 1.4f, Offset(r.nextFloat() * w, 40f + r.nextFloat() * 70f))
            } else drawCircle(Color.White.copy(alpha = 0.5f), 13f, Offset(w * 0.82f, 24f))
            for (layer in 0..2) {
                val col = lerp(accent, Color.Black, 0.1f + layer * 0.22f).copy(alpha = 0.55f + layer * 0.2f)
                var x = -6f + layer * 9f
                while (x < w + 10f) {
                    val ph = 22f + ((x.toInt() * 7 + layer * 13) % 12) + layer * 6f
                    val base = 76f + layer * 20f
                    drawPath(Path().apply { moveTo(x - 9f, base); lineTo(x, base - ph - 10f); lineTo(x + 9f, base); close() }, col)
                    drawPath(Path().apply { moveTo(x - 7f, base - 9f); lineTo(x, base - ph); lineTo(x + 7f, base - 9f); close() }, col)
                    x += 17f + (layer * 3f)
                }
                drawRect(col, Offset(0f, 76f + layer * 20f), Size(w, h))
            }
        }
        BannerPattern.CLOUDS -> {
            drawCircle(Color.White.copy(alpha = 0.55f), 15f, Offset(w * 0.2f, 30f)); drawCircle(Color.White.copy(alpha = 0.25f), 26f, Offset(w * 0.2f, 30f))
            for (i in 0 until 5) {
                val sp = 0.4f + (i % 3) * 0.3f
                val cx = ((w * (i * 0.23f) + t * 70f * sp) % (w + 80f)) - 30f
                val cy = 24f + i * 17f; val sc = 0.9f + (i % 3) * 0.3f
                drawCircle(Color.White.copy(alpha = 0.9f), 8f * sc, Offset(cx, cy)); drawCircle(Color.White.copy(alpha = 0.9f), 11f * sc, Offset(cx + 10f * sc, cy - 4f * sc))
                drawCircle(Color.White.copy(alpha = 0.9f), 8f * sc, Offset(cx + 20f * sc, cy)); drawRoundRect(Color.White.copy(alpha = 0.9f), Offset(cx - 5f * sc, cy), Size(32f * sc, 8f * sc), CornerRadius(4f * sc))
            }
        }
        BannerPattern.HEARTS -> for (i in 0 until 18) {
            val prog = (r.nextFloat() + t * (if (i % 2 == 0) 1 else 2)) % 1f
            val x = r.nextFloat() * w + sin((prog * 6f + i).toDouble()).toFloat() * 6f; val y = h + 10f - prog * (h + 24f); val s = 4f + r.nextFloat() * 6f
            bannerHeart(x, y, s, (if (i % 3 == 0) Color.White else accent).copy(alpha = 0.7f * (1f - prog * 0.5f)))
        }
        BannerPattern.FIREWORKS -> {
            for (i in 0 until 22) drawCircle(Color.White.copy(alpha = 0.2f + 0.5f * ((sin((t * 2 * PI + i).toDouble()).toFloat() + 1f) / 2f)), 0.6f + r.nextFloat(), Offset(r.nextFloat() * w, r.nextFloat() * h))
            val cols = listOf(accent, Color(0xFFFF6F91), Color(0xFF7FE0FF), Color(0xFFB69CFF))
            for (b in 0 until 3) {
                val ph = (t * 1.5f + b * 0.33f) % 1f
                val cx = w * (0.2f + 0.3f * b); val cy = 34f + (b % 2) * 18f
                val rad = 8f + ph * 30f; val al = (1f - ph).coerceIn(0f, 1f)
                for (k in 0 until 14) {
                    val ang = k * (2f * PI.toFloat() / 14f)
                    val x0 = cx + cos(ang) * rad * 0.6f; val y0 = cy + sin(ang) * rad * 0.6f
                    drawLine(cols[(b + k) % cols.size].copy(alpha = al), Offset(x0, y0), Offset(cx + cos(ang) * rad, cy + sin(ang) * rad), strokeWidth = 1.6f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    drawCircle(Color.White.copy(alpha = al), 1.2f, Offset(cx + cos(ang) * rad, cy + sin(ang) * rad))
                }
            }
            hills(w, h, Color.Black, 0.55f, 104f, 0.6f)
        }
        BannerPattern.GALAXY -> {
            for (k in 0..2) {
                val cx = w * (0.25f + 0.28f * k); val cy = 40f + k * 22f
                drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.42f), accent.copy(alpha = 0f)), Offset(cx, cy), 46f), 46f, Offset(cx, cy))
            }
            for (i in 0 until 60) {
                val tw = 0.25f + 0.75f * ((sin((t * 2 * PI * (1 + i % 3) + i).toDouble()).toFloat() + 1f) / 2f)
                drawCircle(Color.White.copy(alpha = tw), 0.5f + r.nextFloat() * 1.3f, Offset(r.nextFloat() * w, r.nextFloat() * h))
            }
            drawCircle(lerp(accent, Color.White, 0.2f), 9f, Offset(w * 0.86f, 30f)); drawCircle(Color(0xFF07041A).copy(alpha = 0.35f), 9f, Offset(w * 0.86f + 3f, 31f))
            drawOval(Color.White.copy(alpha = 0.55f), Offset(w * 0.86f - 17f, 28f), Size(34f, 5f), style = Stroke(1.2f))
        }
        BannerPattern.ZEN -> {
            for (row in 0..5) {
                val y = 50f + row * 12f
                val path = Path().apply { moveTo(0f, y); var x = 0f; while (x <= w + 6f) { lineTo(x, y + sin((x / 26f + row * 0.8f + t * 2 * PI * 0.2).toDouble()).toFloat() * 2.4f); x += 6f } }
                drawPath(path, accent.copy(alpha = 0.45f), style = Stroke(1.5f))
            }
            for (k in 0..2) drawOval(accent.copy(alpha = 0.35f), Offset(w * 0.7f - k * 9f, 70f - k * 6f), Size(18f + k * 18f, 8f + k * 8f), style = Stroke(1.2f))
            val bx = w * 0.7f
            drawOval(lerp(accent, Color.Black, 0.45f), Offset(bx - 14f, 78f), Size(28f, 14f)); drawOval(lerp(accent, Color.Black, 0.3f), Offset(bx - 10f, 68f), Size(20f, 11f)); drawOval(lerp(accent, Color.Black, 0.2f), Offset(bx - 6f, 60f), Size(12f, 8f))
            for (i in 0 until 8) {
                val prog = (r.nextFloat() + t) % 1f; val px = r.nextFloat() * w + sin((prog * 6f + i).toDouble()).toFloat() * 8f; val py = prog * (h + 10f) - 5f
                rotate(prog * 300f, pivot = Offset(px, py)) { drawOval(accent.copy(alpha = 0.7f), Offset(px - 3.4f, py - 2f), Size(6.8f, 4f)) }
            }
        }
        BannerPattern.STRIPES -> {
            rotate(-20f, pivot = Offset(w / 2f, h / 2f)) {
                var x = -w * 0.4f; var k = 0
                while (x < w * 1.4f) { if (k % 2 == 0) drawRect(accent.copy(alpha = 0.28f), Offset(x + (t * 44f) % 44f, -h), Size(22f, h * 3f)); x += 22f; k++ }
            }
            for (i in 0 until 10) drawCircle(Color.White.copy(alpha = 0.5f), 2f + r.nextFloat() * 2f, Offset(r.nextFloat() * w, r.nextFloat() * h))
        }
        BannerPattern.CIRCUIT -> {
            val grid = 16f
            var y = 12f
            while (y < h) {
                var x = 0f
                while (x < w) {
                    if (r.nextFloat() > 0.55f) drawLine(accent.copy(alpha = 0.35f), Offset(x, y), Offset(x + grid * (1 + r.nextInt(3)), y), strokeWidth = 1.3f)
                    if (r.nextFloat() > 0.65f) drawLine(accent.copy(alpha = 0.3f), Offset(x, y), Offset(x, y + grid * (1 + r.nextInt(2))), strokeWidth = 1.3f)
                    if (r.nextFloat() > 0.8f) { drawCircle(accent.copy(alpha = 0.8f), 2.4f, Offset(x, y)); drawCircle(c(def.top), 1.1f, Offset(x, y)) }
                    x += grid
                }
                y += grid
            }
            for (i in 0 until 4) { val prog = (i * 0.25f + t) % 1f; drawCircle(Color.White.copy(alpha = 0.9f), 2f, Offset(prog * w, 12f + (i * 32) % h)); drawCircle(accent.copy(alpha = 0.4f), 5f, Offset(prog * w, 12f + (i * 32) % h)) }
        }
        BannerPattern.LANTERNS -> {
            for (i in 0 until 7) {
                val x = w * (0.08f + 0.14f * i) + sin((t * 2 * PI + i).toDouble()).toFloat() * 3f
                val len = 18f + (i % 3) * 16f; val sc = 0.8f + (i % 2) * 0.3f
                drawLine(Color.White.copy(alpha = 0.35f), Offset(x, 0f), Offset(x, len), strokeWidth = 1f)
                drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.5f), accent.copy(alpha = 0f)), Offset(x, len + 11f * sc), 26f * sc), 26f * sc, Offset(x, len + 11f * sc))
                drawRoundRect(lerp(accent, Color.White, 0.05f), Offset(x - 8f * sc, len), Size(16f * sc, 22f * sc), CornerRadius(8f * sc))
                drawLine(Color.Black.copy(alpha = 0.25f), Offset(x, len), Offset(x, len + 22f * sc), strokeWidth = 1.1f); drawLine(Color.White.copy(alpha = 0.4f), Offset(x - 3f * sc, len + 3f * sc), Offset(x - 3f * sc, len + 19f * sc), strokeWidth = 1f)
                drawRect(Color(0xFF4A1F1F), Offset(x - 4f * sc, len - 2f), Size(8f * sc, 3f)); drawRect(Color(0xFF4A1F1F), Offset(x - 4f * sc, len + 22f * sc - 1f), Size(8f * sc, 3f))
                drawLine(accent, Offset(x, len + 25f * sc), Offset(x, len + 34f * sc), strokeWidth = 1.4f)
            }
            for (i in 0 until 14) drawCircle(accent.copy(alpha = 0.5f * ((sin((t * 2 * PI + i * 2).toDouble()).toFloat() + 1f) / 2f)), 0.9f, Offset(r.nextFloat() * w, 70f + r.nextFloat() * 50f))
        }
        BannerPattern.CONFETTI -> {
            val cols = listOf(accent, Color(0xFFFFD36E), Color(0xFF7FE0FF), Color(0xFFB69CFF), Color(0xFF8EE3B0))
            for (i in 0 until 34) {
                val prog = (r.nextFloat() + t * (1 + i % 2)) % 1f
                val x = r.nextFloat() * w + sin((prog * 5f + i).toDouble()).toFloat() * 6f; val y = prog * (h + 12f) - 6f
                rotate(prog * 540f + i * 29f, pivot = Offset(x, y)) { drawRoundRect(cols[i % cols.size].copy(alpha = 0.85f), Offset(x - 3f, y - 1.6f), Size(6f, 3.2f), CornerRadius(1f)) }
            }
        }
        BannerPattern.RAIN -> {
            val dark = def.isDark
            for (i in 0 until 3) {
                val cx = w * (0.15f + 0.35f * i); drawCircle(accent.copy(alpha = 0.35f), 16f, Offset(cx, 12f)); drawCircle(accent.copy(alpha = 0.35f), 12f, Offset(cx + 14f, 14f)); drawCircle(accent.copy(alpha = 0.35f), 12f, Offset(cx - 14f, 16f))
            }
            for (i in 0 until 40) {
                val prog = (r.nextFloat() + t * (2 + i % 3)) % 1f
                val x = r.nextFloat() * w - prog * 14f; val y = prog * (h + 20f) - 10f
                drawLine(accent.copy(alpha = 0.55f), Offset(x, y), Offset(x - 3f, y + 9f), strokeWidth = 1.2f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
            if (dark) {
                val flash = ((t * 3f) % 1f)
                if (flash < 0.08f) drawRect(Color.White.copy(alpha = 0.18f), size = Size(w, h))
                val bolt = Path().apply { moveTo(w * 0.62f, 20f); lineTo(w * 0.58f, 46f); lineTo(w * 0.63f, 46f); lineTo(w * 0.57f, 78f) }
                drawPath(bolt, Color.White.copy(alpha = if (flash < 0.12f) 0.95f else 0.0f), style = Stroke(2f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
            hills(w, h, accent, 0.18f, 108f, 1.9f)
        }
        BannerPattern.SUNSET -> {
            val hz = 74f
            drawCircle(accent.copy(alpha = 0.25f), 44f, Offset(w * 0.5f, hz)); drawCircle(accent.copy(alpha = 0.5f), 30f, Offset(w * 0.5f, hz)); drawCircle(accent, 20f, Offset(w * 0.5f, hz))
            drawRect(c(def.bottom), Offset(0f, hz), Size(w, h - hz))
            for (k in 0..6) {
                val y = hz + 4f + k * 6.5f; val len = 34f - k * 3.5f + sin((t * 2 * PI + k).toDouble()).toFloat() * 3f
                drawRoundRect(accent.copy(alpha = 0.7f - k * 0.08f), Offset(w * 0.5f - len / 2f, y), Size(len, 2.2f), CornerRadius(1.1f))
            }
            for (i in 0 until 3) bannerBird(((t * 60f + i * 70f) % (w + 40f)) - 20f, 22f + i * 9f + sin((t * 12f + i).toDouble()).toFloat() * 2f, c(def.ink).copy(alpha = 0.55f))
        }
        BannerPattern.CRYSTALS -> {
            for (i in 0 until 26) drawCircle(Color.White.copy(alpha = 0.2f + 0.5f * ((sin((t * 2 * PI * (1 + i % 2) + i).toDouble()).toFloat() + 1f) / 2f)), 0.6f + r.nextFloat(), Offset(r.nextFloat() * w, r.nextFloat() * h * 0.7f))
            var x = 4f; var k = 0
            while (x < w) {
                val ch = 30f + ((k * 29 + def.id * 7) % 42); val cw = 10f + ((k * 13) % 12)
                val col = lerp(accent, c(def.top), (k % 3) * 0.28f)
                drawPath(Path().apply { moveTo(x, h); lineTo(x + cw * 0.2f, h - ch * 0.78f); lineTo(x + cw * 0.5f, h - ch); lineTo(x + cw * 0.8f, h - ch * 0.78f); lineTo(x + cw, h); close() }, col.copy(alpha = 0.55f))
                drawPath(Path().apply { moveTo(x + cw * 0.5f, h - ch); lineTo(x + cw * 0.8f, h - ch * 0.78f); lineTo(x + cw, h); lineTo(x + cw * 0.5f, h); close() }, Color.White.copy(alpha = 0.18f))
                drawLine(Color.White.copy(alpha = 0.55f), Offset(x + cw * 0.5f, h - ch), Offset(x + cw * 0.5f, h), strokeWidth = 0.9f)
                if (k % 3 == 0) sparkleBanner(x + cw * 0.5f, h - ch - 4f, 3.4f, Color.White.copy(alpha = 0.4f + 0.55f * ((sin((t * 2 * PI + k).toDouble()).toFloat() + 1f) / 2f)))
                x += cw + 3f; k++
            }
        }
    }
}

private fun DrawScope.bannerBat(cx: Float, cy: Float, sc: Float, color: Color, flap: Float) {
    val f = 0.4f + 0.6f * flap
    drawPath(Path().apply {
        moveTo(cx, cy); quadraticTo(cx - 6f * sc, cy - 9f * sc * f, cx - 14f * sc, cy - 3f * sc * f); quadraticTo(cx - 10f * sc, cy, cx - 8f * sc, cy + 4f * sc)
        quadraticTo(cx - 4f * sc, cy, cx, cy + 3f * sc); quadraticTo(cx + 4f * sc, cy, cx + 8f * sc, cy + 4f * sc); quadraticTo(cx + 10f * sc, cy, cx + 14f * sc, cy - 3f * sc * f)
        quadraticTo(cx + 6f * sc, cy - 9f * sc * f, cx, cy); close()
    }, color)
}

private fun DrawScope.bannerPumpkin(cx: Float, cy: Float, sc: Float, glow: Color, t: Float, k: Int) {
    val body = Color(0xFFF28A1F)
    drawCircle(glow.copy(alpha = 0.16f), 20f * sc, Offset(cx, cy))
    drawOval(lerp(body, Color.Black, 0.18f), Offset(cx - 15f * sc, cy - 10f * sc), Size(18f * sc, 20f * sc)); drawOval(lerp(body, Color.Black, 0.18f), Offset(cx - 3f * sc, cy - 10f * sc), Size(18f * sc, 20f * sc))
    drawOval(body, Offset(cx - 11f * sc, cy - 11f * sc), Size(22f * sc, 22f * sc))
    drawRoundRect(Color(0xFF5A8A3A), Offset(cx - 2f * sc, cy - 15f * sc), Size(4f * sc, 6f * sc), CornerRadius(1.5f * sc))
    val on = 0.75f + 0.25f * sin((t * 2 * PI * 2 + k).toDouble()).toFloat()
    val eye = Color(0xFFFFD34A).copy(alpha = on)
    drawPath(Path().apply { moveTo(cx - 7f * sc, cy - 1f * sc); lineTo(cx - 2f * sc, cy - 1f * sc); lineTo(cx - 4.5f * sc, cy - 6f * sc); close() }, eye)
    drawPath(Path().apply { moveTo(cx + 2f * sc, cy - 1f * sc); lineTo(cx + 7f * sc, cy - 1f * sc); lineTo(cx + 4.5f * sc, cy - 6f * sc); close() }, eye)
    drawPath(Path().apply { moveTo(cx - 7f * sc, cy + 3f * sc); lineTo(cx - 3.5f * sc, cy + 6f * sc); lineTo(cx, cy + 3f * sc); lineTo(cx + 3.5f * sc, cy + 6f * sc); lineTo(cx + 7f * sc, cy + 3f * sc); lineTo(cx + 5f * sc, cy + 8f * sc); lineTo(cx - 5f * sc, cy + 8f * sc); close() }, eye)
}

private fun DrawScope.bannerHeart(cx: Float, cy: Float, r: Float, color: Color) {
    drawPath(Path().apply {
        moveTo(cx, cy + r)
        cubicTo(cx - r * 1.6f, cy - r * 0.2f, cx - r * 0.7f, cy - r * 1.3f, cx, cy - r * 0.4f)
        cubicTo(cx + r * 0.7f, cy - r * 1.3f, cx + r * 1.6f, cy - r * 0.2f, cx, cy + r)
        close()
    }, color)
}

private fun DrawScope.sparkleBanner(cx: Float, cy: Float, r: Float, color: Color) {
    drawPath(Path().apply { moveTo(cx, cy - r); quadraticTo(cx, cy, cx + r, cy); quadraticTo(cx, cy, cx, cy + r); quadraticTo(cx, cy, cx - r, cy); quadraticTo(cx, cy, cx, cy - r); close() }, color)
}

private fun DrawScope.bannerBird(cx: Float, cy: Float, color: Color) {
    drawPath(Path().apply { moveTo(cx - 6f, cy); quadraticTo(cx - 3f, cy - 4f, cx, cy); quadraticTo(cx + 3f, cy - 4f, cx + 6f, cy) }, color, style = Stroke(1.4f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
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
