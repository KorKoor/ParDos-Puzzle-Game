package com.korkoor.pardos.ui.menu

import android.media.MediaPlayer
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.R
import com.korkoor.pardos.ui.design.DarkSystemBars
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

private val SkyTop = Color(0xFF0E0726)
private val SkyMid = Color(0xFF2A1257)
private val SkyLow = Color(0xFF6E2A6A)
private val Horizon = Color(0xFFFF8A3D)
private val Cream = Color(0xFFFFE3B8)
private val Ink = Color(0xFF0B0519)

/** Iconos 3D que viven dentro de la escena (se cargan una sola vez). */
private class NightArt(
    val pumpkin: ImageBitmap, val ghost: ImageBitmap, val candy: ImageBitmap, val lollipop: ImageBitmap,
    val spider: ImageBitmap, val bat: ImageBitmap, val skull: ImageBitmap, val cat: ImageBitmap, val web: ImageBitmap
)

/** Posiciones que no cambian entre fotogramas (se sortean una sola vez). */
private class NightLayout {
    private val r = Random(31)
    val stars = List(90) { Triple(r.nextFloat(), r.nextFloat() * 0.55f, r.nextFloat()) }
    private val e = Random(77)
    val embers = List(30) { Triple(e.nextFloat(), 0.3f + e.nextFloat() * 0.5f, e.nextFloat()) }
}

/**
 * Inicio de Noche de brujas: una escena de cuento en capas. Arriba, una guirnalda de luces con calabazas; una luna enorme
 * con murciélagos que la cruzan; abajo, la casona con humo en su colina, un camino con farolillos, tumbas y dos montones de
 * calabazas encendidas a los lados. En el centro, el tablero de ParDos vestido de Halloween y el título de siempre.
 * Mismo sonido y duración que el inicio normal.
 */
@Composable
internal fun HalloweenSplash(onAnimationFinished: () -> Unit) {
    val context = LocalContext.current

    val art = NightArt(
        ImageBitmap.imageResource(R.drawable.ico_pumpkin), ImageBitmap.imageResource(R.drawable.ico_ghost),
        ImageBitmap.imageResource(R.drawable.ico_candy), ImageBitmap.imageResource(R.drawable.ico_lollipop),
        ImageBitmap.imageResource(R.drawable.ico_spider), ImageBitmap.imageResource(R.drawable.ico_bat),
        ImageBitmap.imageResource(R.drawable.ico_skull), ImageBitmap.imageResource(R.drawable.ico_blackcat),
        ImageBitmap.imageResource(R.drawable.ico_web)
    )
    val layout = remember { NightLayout() }

    var started by remember { mutableStateOf(false) }
    // Iconos de las barras del sistema: oscuros sobre el papel cálido del arranque y claros cuando entra la noche
    DarkSystemBars(started)
    val pop by animateFloatAsState(
        if (started) 1f else 0.2f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow), label = "logoPop"
    )
    val textAlpha by animateFloatAsState(if (started) 1f else 0f, tween(1100, delayMillis = 450), label = "textFade")

    // La ventana del sistema arranca en papel cálido: la noche aparece con un fundido suave en lugar de un salto
    val nightIn by animateFloatAsState(if (started) 1f else 0f, tween(650), label = "nightIn")

    val infinite = rememberInfiniteTransition(label = "night")
    val clock = infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "clock")
    val flicker = infinite.animateFloat(0.78f, 1.1f, infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "flame")
    val bob = infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")

    LaunchedEffect(Unit) {
        delay(100)
        started = true
        try {
            val mp = MediaPlayer.create(context, R.raw.move_pop)
            mp.setOnCompletionListener { it.release() }
            mp.start()
        } catch (e: Exception) { e.printStackTrace() }
        delay(3200)
        onAnimationFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(SkyTop, SkyMid, SkyLow))),
        contentAlignment = Alignment.Center
    ) {
        // Los valores animados se leen dentro del dibujo: solo se repinta el lienzo, no se recompone la pantalla
        Canvas(Modifier.fillMaxSize()) { nightScene(art, layout, clock.value, bob.value, flicker.value) }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.offset(y = (-84).dp)) {
            // La identidad de ParDos (tablero con dos fichas "2") vestida de Halloween: el mismo dibujo del icono
            Image(
                painter = painterResource(R.drawable.ic_halloween_fg),
                contentDescription = "ParDos",
                modifier = Modifier
                    .size(290.dp)
                    .graphicsLayer { translationY = (bob.value * 10f - 5f).dp.toPx(); scaleX = pop; scaleY = pop }
            )
            Text(
                text = "PARDOS",
                style = TextStyle(
                    fontSize = 54.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp, color = Color(0xFFFFA23A),
                    shadow = Shadow(Color(0xFFFF6A00).copy(alpha = 0.85f), Offset.Zero, 34f)
                ),
                modifier = Modifier.alpha(pop.coerceIn(0f, 1f))
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "NOCHE DE BRUJAS",
                style = TextStyle(
                    fontSize = 15.sp, fontWeight = FontWeight.Black, color = Cream, letterSpacing = 5.sp,
                    shadow = Shadow(Color(0xFF1A0A33), Offset(0f, 3f), 8f)
                ),
                modifier = Modifier.alpha(textAlpha)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Math Zen Puzzle",
                style = TextStyle(fontSize = 13.sp, color = Color(0xFFEBDDFF), letterSpacing = 1.5.sp, shadow = Shadow(Color(0xFF1A0A33), Offset(0f, 2f), 6f)),
                modifier = Modifier.alpha(textAlpha)
            )
        }

        Box(Modifier.fillMaxSize().alpha(1f - nightIn).background(Color(0xFFFFF4E3)))

        // Créditos sobre una franja oscura para que se lean entre las calabazas
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .alpha(textAlpha)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Ink.copy(alpha = 0.75f))))
                .padding(top = 28.dp, bottom = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val soft = Shadow(Color(0xFF0B0518), Offset(0f, 2f), 6f)
            Text("Desarrollada por", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFEBDDFF).copy(alpha = 0.85f), shadow = soft))
            Spacer(Modifier.height(4.dp))
            Text("Carlos García Huerta", style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFE9C9), shadow = soft))
            Spacer(Modifier.height(8.dp))
            Text("KorKoor Studios", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFB347), letterSpacing = 2.sp, shadow = soft))
        }
    }
}

// ============================ Escena de fondo ============================

private fun DrawScope.icon(img: ImageBitmap, center: Offset, size: Float, alpha: Float = 1f, degrees: Float = 0f) {
    rotate(degrees, center) {
        drawImage(
            img, dstOffset = IntOffset((center.x - size / 2).toInt(), (center.y - size / 2).toInt()),
            dstSize = IntSize(size.toInt(), size.toInt()), alpha = alpha
        )
    }
}

/** Resplandor suave de un foco de luz (calabaza, ventana, farol). */
private fun DrawScope.glow(center: Offset, radius: Float, color: Color, alpha: Float) {
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), center, radius), radius, center)
}

private fun DrawScope.nightScene(art: NightArt, lay: NightLayout, t: Float, bob: Float, flick: Float) {
    val w = size.width
    val h = size.height
    val tau = (2 * PI).toFloat()

    // resplandor naranja del horizonte, más fuerte detrás del tablero (como si lo encendieran las calabazas)
    glow(Offset(w * 0.5f, h * 0.86f), w * 1.1f, Horizon, 0.36f)
    glow(Offset(w * 0.5f, h * 0.34f), w * 0.62f, Color(0xFFFF7A2E), 0.20f * flick)

    // estrellas que titilan
    lay.stars.forEachIndexed { i, (nx, ny, ph) ->
        val tw = 0.2f + 0.8f * ((sin(((t * (1 + i % 3)) + ph) * tau) + 1f) / 2f)
        val r = 1f + ph * 2.2f
        val x = nx * w
        val y = ny * h
        drawCircle(Color.White.copy(alpha = tw * 0.9f), r, Offset(x, y))
        if (i % 11 == 0) {
            drawLine(Color.White.copy(alpha = tw * 0.5f), Offset(x - r * 3, y), Offset(x + r * 3, y), strokeWidth = 1.2f)
            drawLine(Color.White.copy(alpha = tw * 0.5f), Offset(x, y - r * 3), Offset(x, y + r * 3), strokeWidth = 1.2f)
        }
    }

    // luna enorme con halo y cráteres
    val moon = Offset(w * 0.72f, h * 0.2f)
    val mr = w * 0.17f
    glow(moon, mr * 3.8f, Color(0xFFFFD98A), 0.5f)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF4CF), Color(0xFFFFD98A)), Offset(moon.x - mr * 0.3f, moon.y - mr * 0.3f), mr * 1.5f), mr, moon)
    drawCircle(Color(0x33C9A24E), mr * 0.24f, Offset(moon.x - mr * 0.32f, moon.y + mr * 0.2f))
    drawCircle(Color(0x33C9A24E), mr * 0.16f, Offset(moon.x + mr * 0.36f, moon.y + mr * 0.45f))
    drawCircle(Color(0x28C9A24E), mr * 0.12f, Offset(moon.x + mr * 0.2f, moon.y - mr * 0.42f))

    // nubes oscuras que tapan la luna a ratos
    for (k in 0..2) {
        val cx = ((t * (0.35f + k * 0.2f) + k * 0.37f) % 1.5f - 0.25f) * w
        val cy = h * (0.17f + k * 0.07f)
        drawOval(Color(0xFF241046).copy(alpha = 0.62f), Offset(cx - w * 0.22f, cy - h * 0.012f), Size(w * 0.44f, h * 0.028f))
        drawOval(Color(0xFF241046).copy(alpha = 0.5f), Offset(cx - w * 0.11f, cy - h * 0.027f), Size(w * 0.22f, h * 0.034f))
    }

    // murciélagos que cruzan delante de la luna
    val bats = listOf(Triple(0.00f, 0.17f, 1.1f), Triple(0.35f, 0.24f, 0.8f), Triple(0.62f, 0.14f, 1.25f), Triple(0.82f, 0.3f, 0.7f))
    bats.forEachIndexed { i, (phase, baseY, sc) ->
        val prog = (t * (1.0f + i * 0.13f) + phase) % 1.3f - 0.15f
        val x = (if (i % 2 == 0) prog else 1f - prog) * w
        val y = h * baseY + sin((prog * 6f + i).toDouble()).toFloat() * h * 0.022f
        val flap = 0.35f + 0.65f * abs(sin((t * tau * 14 + i)))
        bat(Offset(x, y), w * 0.012f * sc * 3.4f, flap, Ink, flip = i % 2 != 0)
    }

    // guirnalda de luces con calabazas, colgada de lado a lado
    garland(art, w, h, t, y0 = h * 0.018f, sag = h * 0.05f, bulbs = 11, pumpkinsAt = listOf(0.5f))
    garland(art, w, h, t + 0.3f, y0 = h * 0.04f, sag = h * 0.036f, bulbs = 8, pumpkinsAt = listOf(0.25f, 0.8f), from = 0.0f, to = 1.0f, small = true)

    // telaraña en la esquina y araña bajando por su hilo
    icon(art.web, Offset(w * 0.07f, h * 0.085f), w * 0.3f, alpha = 0.5f, degrees = -8f)
    val spiderY = h * (0.15f + 0.05f * bob)
    val spiderX = w * 0.17f
    drawLine(Color.White.copy(alpha = 0.4f), Offset(spiderX, 0f), Offset(spiderX, spiderY - w * 0.03f), strokeWidth = 2f)
    icon(art.spider, Offset(spiderX, spiderY), w * 0.09f, degrees = (bob - 0.5f) * 10f)

    // fantasmas translúcidos flotando, con su brillo
    ghost(art, Offset(w * 0.13f, h * 0.43f + (bob - 0.5f) * h * 0.02f), w * 0.15f, 0.92f, (bob - 0.5f) * 8f)
    ghost(art, Offset(w * 0.9f, h * 0.5f + (0.5f - bob) * h * 0.016f), w * 0.1f, 0.5f, -(bob - 0.5f) * 10f)

    // colinas lejanas con la casona encima
    val farTop = h * 0.74f
    val far = Path().apply {
        moveTo(0f, h); lineTo(0f, farTop)
        var x = 0f
        while (x <= w + 8f) { lineTo(x, farTop + sin((x / w * 4.2f).toDouble()).toFloat() * h * 0.016f); x += 8f }
        lineTo(w, h); close()
    }
    drawPath(far, Brush.verticalGradient(listOf(Color(0xFF3F1C74), Color(0xFF22103F)), farTop - h * 0.02f, h))
    manor(w * 0.79f, farTop + h * 0.05f, w * 0.31f, flick, t)

    // niebla entre colinas
    for (k in 0..2) {
        val fx = ((t * (0.3f + 0.1f * k) + k * 0.31f) % 1.6f - 0.3f) * w
        val fy = h * (0.77f + 0.035f * k)
        drawOval(Brush.horizontalGradient(listOf(Color.Transparent, Color(0x449B7AD6), Color.Transparent), fx - w * 0.3f, fx + w * 0.3f), Offset(fx - w * 0.3f, fy - h * 0.012f), Size(w * 0.6f, h * 0.026f))
    }

    // colina cercana con el camino que sube a la casona
    val nearTop = h * 0.855f
    val near = Path().apply {
        moveTo(0f, h); lineTo(0f, nearTop)
        var x = 0f
        while (x <= w + 8f) { lineTo(x, nearTop + sin((x / w * 3.1f + 1.4f).toDouble()).toFloat() * h * 0.013f); x += 8f }
        lineTo(w, h); close()
    }
    drawPath(near, Brush.verticalGradient(listOf(Color(0xFF1B0E38), Color(0xFF0F0722)), h * 0.84f, h))
    val path = Path().apply {
        moveTo(w * 0.47f, h); cubicTo(w * 0.45f, h * 0.9f, w * 0.7f, h * 0.84f, w * 0.74f, h * 0.775f)
        lineTo(w * 0.78f, h * 0.775f); cubicTo(w * 0.78f, h * 0.84f, w * 0.58f, h * 0.9f, w * 0.57f, h)
        close()
    }
    drawPath(path, Brush.verticalGradient(listOf(Color(0x55FFB347), Color(0x221A0A33)), h * 0.77f, h))
    // farolillos a lo largo del camino
    listOf(0.86f to 0.54f, 0.82f to 0.6f, 0.8f to 0.67f).forEachIndexed { i, (fy, fx) ->
        val c = Offset(w * fx, h * fy)
        val f = 0.75f + 0.25f * sin((flick * 9f + i).toDouble()).toFloat()
        glow(c, w * 0.06f, Color(0xFFFFB347), 0.55f * f)
        drawCircle(Color(0xFFFFE08A), w * 0.007f, c)
        drawLine(Ink, Offset(c.x, c.y + w * 0.004f), Offset(c.x, c.y + w * 0.04f), strokeWidth = w * 0.006f, cap = StrokeCap.Round)
    }

    // árbol seco a la izquierda con un gato negro en una rama y un farol colgando
    val tree = Ink
    val bx = w * 0.06f
    val by = h * 0.89f
    drawLine(tree, Offset(bx, by), Offset(bx + w * 0.012f, by - h * 0.2f), strokeWidth = w * 0.024f, cap = StrokeCap.Round)
    listOf(
        Triple(0.012f, -0.17f, -0.08f to -0.24f), Triple(0.012f, -0.14f, 0.1f to -0.22f),
        Triple(0.01f, -0.1f, -0.07f to -0.14f), Triple(0.011f, -0.19f, 0.03f to -0.27f), Triple(0.011f, -0.12f, 0.13f to -0.15f)
    ).forEach { (dx, dy, end) ->
        drawLine(tree, Offset(bx + w * dx, by + h * dy), Offset(bx + w * end.first, by + h * end.second), strokeWidth = w * 0.011f, cap = StrokeCap.Round)
    }
    icon(art.cat, Offset(bx + w * 0.1f, by - h * 0.15f), w * 0.1f, alpha = 0.95f, degrees = -6f)
    val lantern = Offset(bx + w * 0.1f, by - h * 0.217f + (bob - 0.5f) * 4f)
    drawLine(Color.White.copy(alpha = 0.3f), Offset(lantern.x, by - h * 0.235f), Offset(lantern.x, lantern.y - w * 0.012f), strokeWidth = 2f)
    glow(lantern, w * 0.07f, Color(0xFFFFB347), 0.6f * flick)
    drawRoundRect(Color(0xFFFFC866), Offset(lantern.x - w * 0.01f, lantern.y - w * 0.014f), Size(w * 0.02f, w * 0.028f), CornerRadius(w * 0.006f))

    // lápidas
    listOf(0.30f to 0.04f, 0.37f to 0.032f, 0.93f to 0.036f).forEach { (px, tw) ->
        val x = w * px
        val top = h * 0.885f
        val p = Path().apply {
            moveTo(x - w * tw / 2, h * 0.93f); lineTo(x - w * tw / 2, top + w * tw / 2)
            arcTo(androidx.compose.ui.geometry.Rect(x - w * tw / 2, top, x + w * tw / 2, top + w * tw), 180f, 180f, false)
            lineTo(x + w * tw / 2, h * 0.93f); close()
        }
        drawPath(p, Color(0xFF140A2C))
        drawLine(Color(0xFF3A2468), Offset(x - w * tw * 0.18f, top + w * tw * 0.7f), Offset(x + w * tw * 0.18f, top + w * tw * 0.7f), strokeWidth = 3f, cap = StrokeCap.Round)
    }

    // montones de calabazas encendidas a los lados (el centro queda libre para los créditos)
    pumpkinPile(art, Offset(w * 0.15f, h * 0.925f), w * 0.22f, flick, bob, listOf(Triple(0f, 0f, 1f), Triple(0.62f, 0.12f, 0.58f), Triple(-0.58f, 0.2f, 0.42f)))
    pumpkinPile(art, Offset(w * 0.86f, h * 0.935f), w * 0.17f, flick, bob, listOf(Triple(0f, 0f, 1f), Triple(-0.7f, 0.1f, 0.55f)))
    icon(art.skull, Offset(w * 0.3f, h * 0.935f), w * 0.05f, alpha = 0.9f, degrees = -8f)
    icon(art.candy, Offset(w * 0.6f, h * 0.9f), w * 0.05f, alpha = 0.9f, degrees = -18f + bob * 8f)

    // brasas / luciérnagas que suben
    lay.embers.forEachIndexed { i, (sx, speed, ph) ->
        val prog = (t * speed * 3f + ph) % 1f
        val x = sx * w + sin((prog * 5f + i).toDouble()).toFloat() * w * 0.03f
        val y = h * (0.97f - prog * 0.6f)
        val a = (sin(prog * PI.toFloat()) * 0.85f).coerceIn(0f, 1f)
        val col = if (i % 3 == 0) Color(0xFFFFE08A) else Color(0xFFFFA03A)
        drawCircle(col.copy(alpha = a * 0.35f), 7f, Offset(x, y))
        drawCircle(col.copy(alpha = a), 2.6f, Offset(x, y))
    }

    // velo bajo de niebla y viñeta para centrar la mirada
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0x336A4A9A)), h * 0.86f, h), Offset(0f, h * 0.86f), Size(w, h * 0.14f))
    drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0x55050210)), Offset(w / 2, h / 2), h * 0.78f), Offset.Zero, size)
}

/** Cuerda de luces que cuelga entre los dos lados con bombillas que parpadean y, si se pide, calabazas colgadas. */
private fun DrawScope.garland(
    art: NightArt, w: Float, h: Float, t: Float, y0: Float, sag: Float, bulbs: Int, pumpkinsAt: List<Float>,
    from: Float = 0f, to: Float = 1f, small: Boolean = false
) {
    val p0 = Offset(w * from - 8f, y0)
    val p1 = Offset(w * to + 8f, y0)
    val ctrl = Offset((p0.x + p1.x) / 2f, y0 + sag * 2f)
    val rope = Path().apply { moveTo(p0.x, p0.y); quadraticBezierTo(ctrl.x, ctrl.y, p1.x, p1.y) }
    drawPath(rope, Color(0xFF05020D), style = Stroke(width = 4f, cap = StrokeCap.Round))
    fun at(u: Float): Offset {
        val a = (1 - u) * (1 - u); val b = 2 * (1 - u) * u; val c = u * u
        return Offset(a * p0.x + b * ctrl.x + c * p1.x, a * p0.y + b * ctrl.y + c * p1.y)
    }
    val colors = listOf(Color(0xFFFFB347), Color(0xFFB27BFF), Color(0xFFFF7A59), Color(0xFFFFE08A), Color(0xFF7DE0A6))
    for (i in 0 until bulbs) {
        val u = (i + 0.5f) / bulbs
        val c = at(u)
        val tw = 0.55f + 0.45f * sin(((t * 6f) + i * 1.7f).toDouble()).toFloat()
        val col = colors[i % colors.size]
        glow(Offset(c.x, c.y + 10f), if (small) 22f else 30f, col, 0.45f * tw)
        drawCircle(col.copy(alpha = 0.55f + 0.45f * tw), if (small) 4f else 5.5f, Offset(c.x, c.y + 8f))
        drawLine(Color(0xFF05020D), c, Offset(c.x, c.y + 5f), strokeWidth = 3f)
    }
    pumpkinsAt.forEach { u ->
        val c = at(u)
        val s = w * (if (small) 0.075f else 0.1f)
        drawLine(Color(0xFF05020D), c, Offset(c.x, c.y + s * 0.3f), strokeWidth = 3f)
        glow(Offset(c.x, c.y + s * 0.55f), s * 0.9f, Color(0xFFFFA03A), 0.4f)
        icon(art.pumpkin, Offset(c.x, c.y + s * 0.62f), s, degrees = sin((t * 6f + u * 9f).toDouble()).toFloat() * 4f)
    }
}

/** Fantasma con aura: se ve flotar en la oscuridad, no pegado. */
private fun DrawScope.ghost(art: NightArt, c: Offset, size: Float, alpha: Float, degrees: Float) {
    glow(c, size * 1.1f, Color(0xFFB9A2FF), 0.28f * alpha)
    icon(art.ghost, c, size, alpha = alpha, degrees = degrees)
}

/** Calabazas con luz propia: un resplandor en el suelo y cada una con su parpadeo. */
private fun DrawScope.pumpkinPile(art: NightArt, base: Offset, size: Float, flick: Float, bob: Float, items: List<Triple<Float, Float, Float>>) {
    glow(Offset(base.x, base.y - size * 0.1f), size * 1.5f * flick, Color(0xFFFFA03A), 0.42f)
    items.forEachIndexed { i, (dx, dy, k) ->
        val s = size * k
        val c = Offset(base.x + dx * size * 0.62f, base.y + dy * size * 0.4f - s * 0.12f)
        drawOval(Color(0x66000000), Offset(c.x - s * 0.45f, c.y + s * 0.34f), Size(s * 0.9f, s * 0.2f))
        icon(art.pumpkin, c, s, degrees = (bob - 0.5f) * (4f + i) + (if (i % 2 == 0) -2f else 3f))
    }
}

/** Casa embrujada: cuerpo, torres, chimenea con humo y ventanas que parpadean como velas, con luz de luna en los bordes. */
private fun DrawScope.manor(cx: Float, baseY: Float, width: Float, flick: Float, t: Float) {
    val ink = Color(0xFF0C0620)
    val rim = Color(0x55C9B4FF)
    val x0 = cx - width / 2
    val bodyH = width * 0.36f
    // borde de luz de luna (una copia desplazada, detrás)
    drawRect(rim, Offset(x0 - 2f, baseY - bodyH - 2f), Size(width + 4f, bodyH + 6f))
    // cuerpo
    drawRect(ink, Offset(x0, baseY - bodyH), Size(width, bodyH + 6f))
    // techo central a dos aguas
    val roof = Path().apply {
        moveTo(x0 - width * 0.03f, baseY - bodyH); lineTo(cx, baseY - bodyH - width * 0.22f); lineTo(x0 + width * 1.03f, baseY - bodyH); close()
    }
    drawPath(roof, rim, style = Stroke(width = 4f))
    drawPath(roof, ink)
    // chimenea con humo que sube y se deshace
    val chx = x0 + width * 0.66f
    drawRect(ink, Offset(chx, baseY - bodyH - width * 0.2f), Size(width * 0.06f, width * 0.16f))
    for (k in 0..3) {
        val u = (t * 3f + k * 0.25f) % 1f
        drawCircle(Color(0xFFBBA6E8).copy(alpha = 0.32f * (1f - u)), width * (0.03f + 0.05f * u), Offset(chx + width * (0.03f + 0.04f * u), baseY - bodyH - width * (0.22f + 0.3f * u)))
    }
    // torres
    listOf(0.12f to 0.62f, 0.88f to 0.74f).forEach { (fx, th) ->
        val tx = x0 + width * fx
        val tw = width * 0.15f
        val tTop = baseY - bodyH - width * th * 0.34f
        drawRect(rim, Offset(tx - tw / 2 - 2f, tTop - 2f), Size(tw + 4f, baseY - tTop))
        drawRect(ink, Offset(tx - tw / 2, tTop), Size(tw, baseY - tTop))
        drawPath(Path().apply { moveTo(tx - tw * 0.72f, tTop); lineTo(tx, tTop - width * 0.2f); lineTo(tx + tw * 0.72f, tTop); close() }, ink)
        drawRoundRect(Color(0xFFFFB347).copy(alpha = 0.55f * flick + 0.25f), Offset(tx - tw * 0.18f, tTop + tw * 0.4f), Size(tw * 0.36f, tw * 0.6f), CornerRadius(tw * 0.18f))
    }
    // ventanas del cuerpo, cada una parpadea distinto
    listOf(0.24f, 0.4f, 0.6f, 0.76f).forEachIndexed { i, fx ->
        val on = if (i == 2) 0.35f else 0.55f * (0.65f + 0.35f * sin((flick * 6f + i).toDouble()).toFloat()) + 0.3f
        val wx = x0 + width * fx
        glow(Offset(wx, baseY - bodyH * 0.6f), width * 0.07f, Color(0xFFFFB347), 0.35f * on)
        drawRoundRect(Color(0xFFFFB347).copy(alpha = on.coerceIn(0.2f, 1f)), Offset(wx - width * 0.025f, baseY - bodyH * 0.8f), Size(width * 0.05f, bodyH * 0.38f), CornerRadius(width * 0.025f))
    }
    // puerta
    drawRoundRect(Color(0xFF2A1450), Offset(cx - width * 0.045f, baseY - bodyH * 0.42f), Size(width * 0.09f, bodyH * 0.44f), CornerRadius(width * 0.045f))
}

private fun DrawScope.bat(c: Offset, s: Float, flap: Float, color: Color, flip: Boolean) {
    val k = if (flip) -1f else 1f
    val p = Path().apply {
        moveTo(c.x, c.y - s * 0.15f)
        cubicTo(c.x + k * s * 0.12f, c.y - s * 0.32f, c.x + k * s * 0.26f, c.y - s * 0.34f, c.x + k * s * 0.36f, c.y - s * 0.26f)
        cubicTo(c.x + k * s * 0.5f, c.y - s * 0.5f * flap - s * 0.02f, c.x + k * s * 0.8f, c.y - s * 0.54f * flap, c.x + k * s * 1.05f, c.y - s * 0.36f * flap)
        cubicTo(c.x + k * s * 0.92f, c.y - s * 0.26f * flap, c.x + k * s * 0.88f, c.y - s * 0.1f, c.x + k * s * 0.94f, c.y + s * 0.06f)
        cubicTo(c.x + k * s * 0.76f, c.y - s * 0.04f, c.x + k * s * 0.64f, c.y, c.x + k * s * 0.56f, c.y + s * 0.12f)
        cubicTo(c.x + k * s * 0.46f, c.y, c.x + k * s * 0.36f, c.y - s * 0.02f, c.x + k * s * 0.26f, c.y + s * 0.06f)
        lineTo(c.x + k * s * 0.16f, c.y + s * 0.3f); lineTo(c.x + k * s * 0.06f, c.y + s * 0.12f); lineTo(c.x, c.y + s * 0.14f)
        lineTo(c.x - k * s * 0.06f, c.y + s * 0.12f); lineTo(c.x - k * s * 0.16f, c.y + s * 0.3f); lineTo(c.x - k * s * 0.26f, c.y + s * 0.06f)
        cubicTo(c.x - k * s * 0.36f, c.y - s * 0.02f, c.x - k * s * 0.46f, c.y, c.x - k * s * 0.56f, c.y + s * 0.12f)
        cubicTo(c.x - k * s * 0.64f, c.y, c.x - k * s * 0.76f, c.y - s * 0.04f, c.x - k * s * 0.94f, c.y + s * 0.06f)
        cubicTo(c.x - k * s * 0.88f, c.y - s * 0.1f, c.x - k * s * 0.92f, c.y - s * 0.26f * flap, c.x - k * s * 1.05f, c.y - s * 0.36f * flap)
        cubicTo(c.x - k * s * 0.8f, c.y - s * 0.54f * flap, c.x - k * s * 0.5f, c.y - s * 0.5f * flap - s * 0.02f, c.x - k * s * 0.36f, c.y - s * 0.26f)
        cubicTo(c.x - k * s * 0.26f, c.y - s * 0.34f, c.x - k * s * 0.12f, c.y - s * 0.32f, c.x, c.y - s * 0.15f)
        close()
    }
    drawPath(p, color)
}
