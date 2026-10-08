package com.korkoor.pardos.ui.menu

import android.media.MediaPlayer
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale as dsScale
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.R
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

private val NightTop = Color(0xFF1B0F3A)
private val NightMid = Color(0xFF3A1B5C)
private val NightLow = Color(0xFF6A2D63)
private val PumpkinGlow = Color(0xFFFFA03A)

/**
 * Inicio de Noche de brujas: luna, murciélagos, fantasmas tiernos, el tablero de ParDos vestido de Halloween
 * (el mismo dibujo del icono) y el título con resplandor naranja. Usa el mismo sonido y la misma duración que el inicio normal.
 */
@Composable
internal fun HalloweenSplash(onAnimationFinished: () -> Unit) {
    val context = LocalContext.current

    // Iconos de la barra de estado claros sobre la noche (se restauran al salir)
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        val ctrl = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view) }
        val prevS = ctrl?.isAppearanceLightStatusBars
        val prevN = ctrl?.isAppearanceLightNavigationBars
        ctrl?.isAppearanceLightStatusBars = false
        ctrl?.isAppearanceLightNavigationBars = false
        onDispose {
            if (ctrl != null && prevS != null) ctrl.isAppearanceLightStatusBars = prevS
            if (ctrl != null && prevN != null) ctrl.isAppearanceLightNavigationBars = prevN
        }
    }

    var started by remember { mutableStateOf(false) }
    val pop by animateFloatAsState(
        if (started) 1f else 0.2f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow), label = "pumpkinPop"
    )
    val textAlpha by animateFloatAsState(if (started) 1f else 0f, tween(1200, delayMillis = 500), label = "textFade")

    val infinite = rememberInfiniteTransition(label = "night")
    val clock by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "clock")
    val flicker by infinite.animateFloat(0.82f, 1.12f, infiniteRepeatable(tween(430, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "flame")
    val bob by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")

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
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(NightTop, NightMid, NightLow))),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) { nightScene(clock, bob) }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            // La identidad de ParDos (tablero de madera con dos fichas "2") vestida de Halloween: el mismo dibujo del icono
            Image(
                painter = painterResource(R.drawable.ic_halloween_fg),
                contentDescription = "ParDos",
                modifier = Modifier.size(300.dp).offset(y = (bob * 8f - 4f).dp).scale(pop)
            )
            Spacer(Modifier.height(0.dp))
            Text(
                text = "PARDOS",
                style = TextStyle(
                    fontSize = 54.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp, color = Color(0xFFFFA23A),
                    shadow = Shadow(Color(0xFFFF6A00).copy(alpha = 0.75f), Offset.Zero, 28f)
                ),
                modifier = Modifier.alpha(pop.coerceIn(0f, 1f))
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "NOCHE DE BRUJAS",
                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD9A0), letterSpacing = 4.sp,
                modifier = Modifier.alpha(textAlpha)
            )
            Spacer(Modifier.height(4.dp))
            Text("Math Zen Puzzle", fontSize = 13.sp, color = Color(0xFFCDB8F0), letterSpacing = 1.sp, modifier = Modifier.alpha(textAlpha))
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp).alpha(textAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Desarrollada por", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFCDB8F0).copy(alpha = 0.75f))
            Spacer(Modifier.height(4.dp))
            Text("Carlos García Huerta", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFE9C9))
            Spacer(Modifier.height(10.dp))
            Text("KorKoor Studios", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFA23A), letterSpacing = 2.sp)
        }
    }
}

// ============================ Escena de fondo ============================

private fun DrawScope.nightScene(t: Float, bob: Float) {
    val w = size.width
    val h = size.height
    val rnd = Random(31)

    // estrellas que titilan
    repeat(70) { i ->
        val x = rnd.nextFloat() * w
        val y = rnd.nextFloat() * h * 0.62f
        val tw = 0.25f + 0.75f * ((sin(((t * (1 + i % 3)) + rnd.nextFloat()) * 2 * PI).toFloat() + 1f) / 2f)
        drawCircle(Color.White.copy(alpha = tw * 0.9f), 1.2f + rnd.nextFloat() * 2.2f, Offset(x, y))
    }

    // luna con resplandor
    val moon = Offset(w * 0.78f, h * 0.15f)
    val mr = w * 0.115f
    drawCircle(Brush.radialGradient(listOf(Color(0x77FFD98A), Color(0x00FFD98A)), moon, mr * 3.2f), mr * 3.2f, moon)
    drawCircle(Color(0xFFFFE9A8), mr, moon)
    drawCircle(Color(0x33D9B96A), mr * 0.24f, Offset(moon.x - mr * 0.3f, moon.y + mr * 0.2f))
    drawCircle(Color(0x33D9B96A), mr * 0.16f, Offset(moon.x + mr * 0.35f, moon.y + mr * 0.45f))
    drawCircle(Color(0x26D9B96A), mr * 0.12f, Offset(moon.x + mr * 0.2f, moon.y - mr * 0.4f))

    // nubes oscuras que pasan frente a la luna
    for (k in 0..1) {
        val cx = ((t * (0.6f + k * 0.4f) + k * 0.5f) % 1.4f - 0.2f) * w
        val cy = h * (0.13f + k * 0.07f)
        drawOval(Color(0xFF2A1650).copy(alpha = 0.55f), Offset(cx - w * 0.16f, cy - h * 0.012f), Size(w * 0.32f, h * 0.026f))
        drawOval(Color(0xFF2A1650).copy(alpha = 0.45f), Offset(cx - w * 0.08f, cy - h * 0.024f), Size(w * 0.16f, h * 0.03f))
    }

    // murciélagos
    val bats = listOf(
        Triple(0.00f, 0.20f, 1.0f), Triple(0.30f, 0.30f, 0.8f), Triple(0.55f, 0.13f, 1.2f), Triple(0.78f, 0.36f, 0.7f)
    )
    bats.forEachIndexed { i, (phase, baseY, sc) ->
        val prog = (t * (1.1f + i * 0.15f) + phase) % 1.3f - 0.15f
        val x = (if (i % 2 == 0) prog else 1f - prog) * w
        val y = h * baseY + sin((prog * 6f + i).toDouble()).toFloat() * h * 0.025f
        val flap = 0.35f + 0.65f * abs(sin((t * 2 * PI * 14 + i).toFloat()))
        bat(Offset(x, y), w * 0.012f * sc * 3.2f, flap, if (i == 2) Color(0xFF0A0614) else Color(0xFF0E0820), flip = i % 2 != 0)
    }

    // colinas, árbol, lápidas y fantasma
    val hillTop = h * 0.74f
    val far = Path().apply {
        moveTo(0f, h); lineTo(0f, hillTop)
        var x = 0f
        while (x <= w + 8f) { lineTo(x, hillTop + sin((x / w * 4.2f).toDouble()).toFloat() * h * 0.018f); x += 8f }
        lineTo(w, h); close()
    }
    drawPath(far, Color(0xFF2A1450))

    // fantasma flotando entre las colinas
    ghost(Offset(w * 0.22f, h * 0.755f + (bob - 0.5f) * h * 0.02f), w * 0.065f)
    ghost(Offset(w * 0.86f, h * 0.47f + (0.5f - bob) * h * 0.016f), w * 0.04f, alpha = 0.5f)

    val near = Path().apply {
        moveTo(0f, h); lineTo(0f, h * 0.84f)
        var x = 0f
        while (x <= w + 8f) { lineTo(x, h * 0.84f + sin((x / w * 3.1f + 1.4f).toDouble()).toFloat() * h * 0.014f); x += 8f }
        lineTo(w, h); close()
    }
    drawPath(near, Color(0xFF140A2A))

    // árbol seco a la izquierda
    val tree = Color(0xFF0B0618)
    val bx = w * 0.09f
    val by = h * 0.85f
    drawLine(tree, Offset(bx, by), Offset(bx + w * 0.012f, by - h * 0.17f), strokeWidth = w * 0.022f, cap = StrokeCap.Round)
    listOf(
        Triple(0.012f, -0.14f, -0.07f to -0.20f), Triple(0.012f, -0.12f, 0.08f to -0.19f),
        Triple(0.01f, -0.09f, -0.06f to -0.12f), Triple(0.011f, -0.16f, 0.02f to -0.23f)
    ).forEach { (dx, dy, end) ->
        drawLine(tree, Offset(bx + w * dx, by + h * dy), Offset(bx + w * end.first, by + h * end.second), strokeWidth = w * 0.01f, cap = StrokeCap.Round)
    }

    // lápidas
    listOf(0.79f to 0.045f, 0.88f to 0.036f, 0.95f to 0.04f).forEach { (px, tw) ->
        val x = w * px
        val top = h * 0.865f
        val p = Path().apply {
            moveTo(x - w * tw / 2, h * 0.9f); lineTo(x - w * tw / 2, top + w * tw / 2)
            arcTo(androidx.compose.ui.geometry.Rect(x - w * tw / 2, top, x + w * tw / 2, top + w * tw), 180f, 180f, false)
            lineTo(x + w * tw / 2, h * 0.9f); close()
        }
        drawPath(p, Color(0xFF0B0618))
    }

    // niebla baja
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0x336A4A9A)), h * 0.8f, h), Offset(0f, h * 0.8f), Size(w, h * 0.2f))
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

private fun DrawScope.ghost(c: Offset, s: Float, alpha: Float = 0.82f) {
    val p = Path().apply {
        moveTo(c.x - s, c.y + s * 1.25f)
        lineTo(c.x - s, c.y - s * 0.1f)
        cubicTo(c.x - s, c.y - s * 1.55f, c.x + s, c.y - s * 1.55f, c.x + s, c.y - s * 0.1f)
        lineTo(c.x + s, c.y + s * 1.25f)
        quadraticTo(c.x + s * 0.66f, c.y + s * 0.85f, c.x + s * 0.33f, c.y + s * 1.25f)
        quadraticTo(c.x, c.y + s * 0.85f, c.x - s * 0.33f, c.y + s * 1.25f)
        quadraticTo(c.x - s * 0.66f, c.y + s * 0.85f, c.x - s, c.y + s * 1.25f)
        close()
    }
    drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = alpha * 0.28f), Color.Transparent), Offset(c.x, c.y + s * 0.2f), s * 2.2f), s * 2.2f, Offset(c.x, c.y + s * 0.2f))
    drawPath(p, Color.White.copy(alpha = alpha))
    val ink = Color(0xFF2B1650).copy(alpha = alpha)
    // ojos grandes con brillo, mejillas rosas y sonrisa pequeña
    drawOval(ink, Offset(c.x - s * 0.52f, c.y - s * 0.4f), Size(s * 0.3f, s * 0.46f))
    drawOval(ink, Offset(c.x + s * 0.22f, c.y - s * 0.4f), Size(s * 0.3f, s * 0.46f))
    drawCircle(Color.White.copy(alpha = alpha), s * 0.07f, Offset(c.x - s * 0.4f, c.y - s * 0.3f))
    drawCircle(Color.White.copy(alpha = alpha), s * 0.07f, Offset(c.x + s * 0.34f, c.y - s * 0.3f))
    drawOval(Color(0xFFFF8FA3).copy(alpha = alpha * 0.7f), Offset(c.x - s * 0.78f, c.y + s * 0.1f), Size(s * 0.36f, s * 0.2f))
    drawOval(Color(0xFFFF8FA3).copy(alpha = alpha * 0.7f), Offset(c.x + s * 0.42f, c.y + s * 0.1f), Size(s * 0.36f, s * 0.2f))
    drawPath(
        Path().apply { moveTo(c.x - s * 0.16f, c.y + s * 0.2f); quadraticTo(c.x, c.y + s * 0.42f, c.x + s * 0.16f, c.y + s * 0.2f) },
        ink, style = Stroke(s * 0.07f, cap = StrokeCap.Round)
    )
}
