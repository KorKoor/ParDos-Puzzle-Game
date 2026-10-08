package com.korkoor.pardos.ui.collection

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.domain.collection.Collectible
import com.korkoor.pardos.domain.collection.Rarity
import com.korkoor.pardos.domain.collection.SeriesMotif
import com.korkoor.pardos.ui.design.LocalCozyClock
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** El emoji de la pieza si este teléfono sabe dibujarlo; si no (teléfonos antiguos), el de su serie. */
private val glyphCache = HashMap<String, Boolean>()

private fun supportsGlyph(g: String): Boolean = glyphCache.getOrPut(g) {
    try { android.graphics.Paint().hasGlyph(g) } catch (e: Throwable) { true }
}

fun glyphOf(c: Collectible): String = if (supportsGlyph(c.glyph)) c.glyph else c.series.glyph

private fun Long.c() = Color(this)

/** Colores del marco de cada rareza. */
private fun frameColors(r: Rarity): List<Color> = when (r) {
    Rarity.COMMON -> listOf(Color(0xFFD9D3C4), Color(0xFFB9B2A0), Color(0xFFD9D3C4))
    Rarity.RARE -> listOf(Color(0xFF9BD0FF), Color(0xFF3F8FE0), Color(0xFFB8E0FF), Color(0xFF3F8FE0))
    Rarity.EPIC -> listOf(Color(0xFFD9B8FF), Color(0xFF8A4FE0), Color(0xFFF0DCFF), Color(0xFF8A4FE0))
    Rarity.LEGENDARY -> listOf(Color(0xFFFFF1B5), Color(0xFFE0A93B), Color(0xFFFFFAD6), Color(0xFFC98A1B), Color(0xFFFFF1B5))
}

/**
 * Carta de una pieza del álbum: fondo de su serie con un tono propio (cada pieza se ve distinta), dibujo animado del motivo,
 * marco de su rareza, el emoji grande con halo, número, estrellas y copias. Sin tenerla: silueta misteriosa.
 *
 * El tamaño sale del ancho disponible (proporción 3:4), así sirve igual de mini en una lista que enorme en el detalle.
 */
@Composable
fun CollectibleCard(
    c: Collectible,
    modifier: Modifier = Modifier,
    owned: Boolean = true,
    foil: Boolean = false,
    copies: Int = 1,
    showName: Boolean = true,
    animate: Boolean = true
) {
    val clock = LocalCozyClock.current
    val glyph = remember(c.id) { glyphOf(c) }
    val series = c.series
    val tint = remember(c.id) { ((c.number * 37 + c.series.ordinal * 11) % 100) / 100f }
    val top = remember(c.id) { lerp(series.top.c(), series.accent.c(), 0.10f + tint * 0.22f) }
    val bottom = remember(c.id) { lerp(series.bottom.c(), series.accent.c(), 0.05f + (1f - tint) * 0.2f) }
    val isDarkBg = remember(c.id) { ((top.red + bottom.red) * 0.299f + (top.green + bottom.green) * 0.587f + (top.blue + bottom.blue) * 0.114f) / 2f < 0.45f }
    val inkColor = if (isDarkBg) Color.White else Color(0xFF2B2B3A)

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val wDp = maxWidth
        val u = wDp.value / 100f
        val shape = RoundedCornerShape((wDp * 0.1f))
        val t = if (animate) clock.value else 0.25f
        val frame = frameColors(c.rarity)
        val frameWidth = (if (c.rarity == Rarity.COMMON) 1.5f else 2.4f + c.rarity.ordinal * 0.4f) * u

        Box(
            Modifier.fillMaxWidth().aspectRatioCard().clip(shape)
                .background(if (owned) Brush.verticalGradient(listOf(top, bottom)) else Brush.verticalGradient(listOf(Color(0xFF3A3F55), Color(0xFF232637))))
        ) {
            // motivo de la serie
            Canvas(Modifier.fillMaxSize()) {
                drawMotif(series.motif, if (owned) series.accent.c() else Color(0xFF8A90AA), size.width, size.height, t, c.number + series.ordinal * 13, owned)
            }
            // halo detrás del emoji
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2f; val cy = size.height * 0.42f; val r = size.width * 0.42f
                drawCircle(
                    Brush.radialGradient(
                        listOf((if (owned) Color.White else Color(0xFF8A90AA)).copy(alpha = if (owned) 0.55f else 0.12f), Color.Transparent),
                        Offset(cx, cy), r
                    ), r, Offset(cx, cy)
                )
                if (owned && c.rarity.ordinal >= Rarity.EPIC.ordinal) {
                    // destellos que giran despacio alrededor
                    for (i in 0 until 6) {
                        val ang = (i * 60f + t * 360f * (if (c.rarity == Rarity.LEGENDARY) 1f else 0.5f)) * PI.toFloat() / 180f
                        val rr = size.width * 0.36f
                        val a = 0.35f + 0.55f * ((sin((t * 2 * PI * 2 + i).toDouble()).toFloat() + 1f) / 2f)
                        sparkle(cx + cos(ang) * rr, cy + sin(ang) * rr * 0.9f, size.width * 0.035f, Color.White.copy(alpha = a))
                    }
                }
            }
            // emoji (o silueta)
            val emojiSize = (wDp.value * 0.5f).sp
            Box(Modifier.fillMaxSize().padding(bottom = (if (showName) wDp * 0.22f else wDp * 0.04f)), contentAlignment = Alignment.Center) {
                if (owned) {
                    Text(glyph, fontSize = emojiSize, textAlign = TextAlign.Center)
                } else {
                    Text(
                        glyph, fontSize = emojiSize, textAlign = TextAlign.Center,
                        modifier = Modifier.drawWithContent {
                            drawIntoCanvas { canvas ->
                                val paint = Paint().apply { colorFilter = ColorFilter.tint(Color(0xFF14172A), BlendMode.SrcIn) }
                                canvas.saveLayer(androidx.compose.ui.geometry.Rect(Offset.Zero, size), paint)
                                drawContent()
                                canvas.restore()
                            }
                        }
                    )
                    Text("?", fontSize = (wDp.value * 0.2f).sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.55f))
                }
            }
            // nombre
            if (showName) {
                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = if (owned) 0.12f else 0.3f)).padding(horizontal = wDp * 0.05f, vertical = wDp * 0.035f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (owned) c.nameEs else "???", fontSize = (wDp.value * 0.105f).sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = if (owned) inkColor else Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center
                    )
                    Text(
                        "★".repeat(c.rarity.ordinal + 1), fontSize = (wDp.value * 0.085f).sp, color = if (owned) rarityColor(c.rarity) else Color.White.copy(alpha = 0.35f), maxLines = 1
                    )
                }
            }
            // número
            Text(
                "#" + c.number.toString().padStart(2, '0'), fontSize = (wDp.value * 0.085f).sp, fontWeight = FontWeight.Black,
                color = (if (owned) inkColor else Color.White).copy(alpha = 0.55f), modifier = Modifier.align(Alignment.TopStart).padding(wDp * 0.06f)
            )
            // copias
            if (owned && copies > 1) {
                Text(
                    "x$copies", fontSize = (wDp.value * 0.1f).sp, fontWeight = FontWeight.Black, color = Color.White,
                    modifier = Modifier.align(Alignment.TopEnd).padding(wDp * 0.05f).clip(RoundedCornerShape(wDp * 0.06f))
                        .background(Color(0xFF6A4CE0)).padding(horizontal = wDp * 0.05f, vertical = wDp * 0.015f)
                )
            }
            // brillo holográfico del Brillante
            if (owned && foil) {
                Canvas(Modifier.fillMaxSize()) {
                    val x = size.width * ((t * 1.6f) % 1.6f - 0.3f)
                    drawRect(
                        Brush.linearGradient(
                            listOf(Color.Transparent, Color(0xFFFFB3F0).copy(alpha = 0.35f), Color(0xFF9BE3FF).copy(alpha = 0.4f), Color(0xFFFFF1B5).copy(alpha = 0.35f), Color.Transparent),
                            Offset(x - size.width * 0.5f, 0f), Offset(x + size.width * 0.2f, size.height)
                        )
                    )
                }
                Text("✦", fontSize = (wDp.value * 0.12f).sp, color = Color(0xFFFFF1B5), modifier = Modifier.align(Alignment.BottomEnd).padding(wDp * 0.05f).padding(bottom = wDp * 0.2f))
            }
            // marco de la rareza (encima de todo)
            Canvas(Modifier.fillMaxSize()) {
                val brush = if (!owned) Brush.linearGradient(listOf(Color(0xFF5A6080), Color(0xFF3A3F55)))
                else if (c.rarity == Rarity.LEGENDARY || foil) Brush.sweepGradient(frame, center = Offset(size.width / 2, size.height / 2))
                else Brush.linearGradient(frame, Offset.Zero, Offset(size.width, size.height))
                val rot = if (owned && (c.rarity == Rarity.LEGENDARY || foil) && animate) t * 360f else 0f
                rotate(rot) {
                    drawRoundRect(brush, Offset(frameWidth / 2f, frameWidth / 2f), Size(size.width - frameWidth, size.height - frameWidth), CornerRadius(size.width * 0.1f), style = Stroke(frameWidth))
                }
            }
        }
    }
}

private fun Modifier.aspectRatioCard(): Modifier = this.aspectRatio(3f / 4f)

private fun DrawScope.sparkle(cx: Float, cy: Float, r: Float, color: Color) {
    drawPath(Path().apply {
        moveTo(cx, cy - r); quadraticTo(cx, cy, cx + r, cy); quadraticTo(cx, cy, cx, cy + r); quadraticTo(cx, cy, cx - r, cy); quadraticTo(cx, cy, cx, cy - r); close()
    }, color)
}

/** Dibujo suave de fondo según el motivo de la serie. */
private fun DrawScope.drawMotif(m: SeriesMotif, accent: Color, w: Float, h: Float, t: Float, seed: Int, owned: Boolean) {
    val r = Random(seed * 7919 + m.ordinal)
    val a = if (owned) 1f else 0.5f
    when (m) {
        SeriesMotif.DOTS -> {
            var y = h * 0.06f; var row = 0
            while (y < h) { var x = if (row % 2 == 0) w * 0.08f else w * 0.2f; while (x < w) { drawCircle(accent.copy(alpha = 0.2f * a), w * 0.018f, Offset(x, y)); x += w * 0.24f }; y += h * 0.09f; row++ }
        }
        SeriesMotif.RAYS -> rotate(t * 40f, pivot = Offset(w / 2f, h * 0.42f)) {
            for (i in 0 until 12) {
                val a0 = i * 30f * PI.toFloat() / 180f; val a1 = a0 + 14f * PI.toFloat() / 180f
                drawPath(Path().apply { moveTo(w / 2f, h * 0.42f); lineTo(w / 2f + cos(a0) * w * 1.4f, h * 0.42f + sin(a0) * w * 1.4f); lineTo(w / 2f + cos(a1) * w * 1.4f, h * 0.42f + sin(a1) * w * 1.4f); close() }, accent.copy(alpha = 0.16f * a))
            }
        }
        SeriesMotif.WAVES -> for (k in 0..2) {
            val base = h * (0.62f + k * 0.11f)
            drawPath(Path().apply {
                moveTo(0f, h); var x = 0f
                while (x <= w + 4f) { lineTo(x, base + sin((x / (w * 0.22f) + t * 2 * PI * (k + 1) * (if (k % 2 == 0) 1 else -1)).toDouble()).toFloat() * h * 0.018f); x += w * 0.04f }
                lineTo(w, h); close()
            }, accent.copy(alpha = (0.14f + k * 0.05f) * a))
        }
        SeriesMotif.STARS -> {
            for (i in 0 until 26) drawCircle(Color.White.copy(alpha = (0.2f + 0.6f * ((sin((t * 2 * PI * (1 + i % 3) + i).toDouble()).toFloat() + 1f) / 2f)) * a), w * (0.006f + r.nextFloat() * 0.012f), Offset(r.nextFloat() * w, r.nextFloat() * h))
        }
        SeriesMotif.LEAVES -> for (i in 0 until 9) {
            val x = r.nextFloat() * w; val y = r.nextFloat() * h; val s = w * (0.05f + r.nextFloat() * 0.04f)
            rotate(r.nextFloat() * 360f + t * 30f, pivot = Offset(x, y)) {
                drawPath(Path().apply { moveTo(x, y - s); quadraticTo(x + s * 0.7f, y, x, y + s); quadraticTo(x - s * 0.7f, y, x, y - s) }, accent.copy(alpha = 0.18f * a))
            }
        }
        SeriesMotif.SNOW -> for (i in 0 until 18) {
            val prog = (r.nextFloat() + t * (1 + i % 2)) % 1f
            drawCircle(Color.White.copy(alpha = 0.7f * a), w * (0.008f + r.nextFloat() * 0.014f), Offset(r.nextFloat() * w + sin((prog * 6f + i).toDouble()).toFloat() * w * 0.03f, prog * h))
        }
        SeriesMotif.DIAMONDS -> for (i in 0 until 14) {
            val x = r.nextFloat() * w; val y = r.nextFloat() * h; val s = w * (0.025f + r.nextFloat() * 0.03f)
            rotate(45f, pivot = Offset(x, y)) { drawRoundRect(accent.copy(alpha = 0.2f * a), Offset(x - s, y - s), Size(s * 2, s * 2), CornerRadius(s * 0.2f)) }
        }
        SeriesMotif.BUBBLES -> for (i in 0 until 10) {
            val prog = (r.nextFloat() + t) % 1f; val rad = w * (0.025f + r.nextFloat() * 0.05f)
            val x = r.nextFloat() * w + sin((prog * 6f + i).toDouble()).toFloat() * w * 0.03f; val y = h * 1.05f - prog * h * 1.1f
            drawCircle(Color.White.copy(alpha = 0.16f * a), rad, Offset(x, y)); drawCircle(Color.White.copy(alpha = 0.5f * a), rad, Offset(x, y), style = Stroke(w * 0.006f))
        }
        SeriesMotif.HILLS -> for (k in 0..1) drawPath(Path().apply {
            moveTo(0f, h); var x = 0f
            while (x <= w + 4f) { lineTo(x, h * (0.74f + k * 0.1f) + sin((x / (w * 0.3f) + k * 2f).toDouble()).toFloat() * h * 0.03f); x += w * 0.04f }
            lineTo(w, h); close()
        }, accent.copy(alpha = (0.16f + k * 0.1f) * a))
        SeriesMotif.EMBERS -> {
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, accent.copy(alpha = 0.3f * a)), h * 0.5f, h), Offset(0f, h * 0.5f), Size(w, h * 0.5f))
            for (i in 0 until 14) { val prog = (r.nextFloat() + t * (1 + i % 2)) % 1f; drawCircle(accent.copy(alpha = (1f - prog) * 0.9f * a), w * (0.008f + r.nextFloat() * 0.012f), Offset(r.nextFloat() * w + sin((prog * 7f + i).toDouble()).toFloat() * w * 0.03f, h * (1f - prog))) }
        }
        SeriesMotif.RAIN -> for (i in 0 until 22) {
            val prog = (r.nextFloat() + t * (2 + i % 3)) % 1f; val x = r.nextFloat() * w - prog * w * 0.1f; val y = prog * h * 1.1f - h * 0.05f
            drawLine(accent.copy(alpha = 0.35f * a), Offset(x, y), Offset(x - w * 0.02f, y + h * 0.05f), strokeWidth = w * 0.008f)
        }
        SeriesMotif.HEARTS -> for (i in 0 until 10) {
            val prog = (r.nextFloat() + t * (1 + i % 2)) % 1f; val x = r.nextFloat() * w; val y = h * 1.05f - prog * h * 1.1f; val s = w * (0.025f + r.nextFloat() * 0.03f)
            drawPath(Path().apply { moveTo(x, y + s); cubicTo(x - s * 1.6f, y - s * 0.2f, x - s * 0.7f, y - s * 1.3f, x, y - s * 0.4f); cubicTo(x + s * 0.7f, y - s * 1.3f, x + s * 1.6f, y - s * 0.2f, x, y + s); close() }, accent.copy(alpha = 0.3f * a * (1f - prog * 0.6f)))
        }
        SeriesMotif.CONFETTI -> {
            val cols = listOf(accent, Color(0xFFFFD36E), Color(0xFF7FE0FF), Color(0xFFFF8CC0), Color(0xFF8EE3B0))
            for (i in 0 until 20) {
                val prog = (r.nextFloat() + t * (1 + i % 2)) % 1f; val x = r.nextFloat() * w; val y = prog * h * 1.1f - h * 0.05f
                rotate(prog * 540f + i * 31f, pivot = Offset(x, y)) { drawRoundRect(cols[i % cols.size].copy(alpha = 0.55f * a), Offset(x - w * 0.02f, y - w * 0.01f), Size(w * 0.04f, w * 0.02f), CornerRadius(w * 0.004f)) }
            }
        }
        SeriesMotif.STRIPES -> rotate(-20f, pivot = Offset(w / 2f, h / 2f)) {
            var x = -w; var k = 0
            while (x < w * 2f) { if (k % 2 == 0) drawRect(accent.copy(alpha = 0.13f * a), Offset(x + (t * w * 0.2f) % (w * 0.2f), -h), Size(w * 0.1f, h * 3f)); x += w * 0.1f; k++ }
        }
        SeriesMotif.CRYSTALS -> {
            var x = 0f; var k = 0
            while (x < w) {
                val ch = h * (0.12f + ((k * 29 + seed) % 22) / 100f); val cw = w * (0.08f + ((k * 13) % 8) / 100f)
                drawPath(Path().apply { moveTo(x, h); lineTo(x + cw * 0.2f, h - ch * 0.8f); lineTo(x + cw * 0.5f, h - ch); lineTo(x + cw * 0.8f, h - ch * 0.8f); lineTo(x + cw, h); close() }, accent.copy(alpha = 0.25f * a))
                x += cw + w * 0.01f; k++
            }
        }
        SeriesMotif.CHECKER -> {
            val s = w * 0.12f; var yy = 0; var y = 0f
            while (y < h) { var x = if (yy % 2 == 0) 0f else s; while (x < w) { drawRect(accent.copy(alpha = 0.1f * a), Offset(x, y), Size(s, s)); x += s * 2 }; y += s; yy++ }
        }
    }
}
