package com.korkoor.pardos.ui.menu

import android.media.MediaPlayer
import com.korkoor.pardos.ui.design.pardosBackdrop
import com.korkoor.pardos.ui.design.deepen
import com.korkoor.pardos.ui.design.JellyCard
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.R // Asegúrate de importar tu R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 🎨 PALETA DE COLORES "COFFEE ZEN"
private val CoffeeCream = Color(0xFFFFFBF5) // Fondo crema suave
private val CoffeeLatte = Color(0xFFF3EFE6) // Círculo decorativo
private val CoffeeDark = com.korkoor.pardos.ui.design.Navy  // Texto principal (Café expreso)
private val CoffeeMedium = Color(0xFF6B9E86) // Texto secundario (Moca)
private val GoldAccent = Color(0xFFF2CC8F)  // Detalles sutiles

/** Inicio de la app: en Noche de brujas (octubre) sale el de Halloween; el resto del año, el crema de siempre. */
@Composable
fun AnimatedSplashScreen(onAnimationFinished: () -> Unit) {
    val halloween = remember {
        com.korkoor.pardos.domain.retention.SeasonalCopy.isHalloweenWindow(com.korkoor.pardos.data.local.LocalDay.today())
    }
    if (halloween) HalloweenSplash(onAnimationFinished) else ClassicSplash(onAnimationFinished)
}

/** Ficha de juguete que flota alrededor del logo durante el inicio. */
@Composable
private fun SplashTile(value: Int, size: androidx.compose.ui.unit.Dp, tilt: Float, phase: Int, modifier: Modifier = Modifier, show: Float) {
    val bob by rememberInfiniteTransition(label = "splashTile$value").animateFloat(
        initialValue = -5f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(1900 + phase * 310, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob"
    )
    val (top, bottom, ink) = when (value) {
        2 -> Triple(Color(0xFFFFF6E8), Color(0xFFF6E3C4), Color(0xFF7A4B2A))
        4 -> Triple(Color(0xFFFCE2B6), Color(0xFFF4C98A), Color(0xFF7A4B2A))
        8 -> Triple(Color(0xFFF7B98A), Color(0xFFEE9560), Color.White)
        16 -> Triple(Color(0xFFEB8F78), Color(0xFFDB6A57), Color.White)
        else -> Triple(Color(0xFF8FC2A8), Color(0xFF6B9E86), Color.White)
    }
    JellyCard(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                translationY = bob.dp.toPx() + (1f - show) * 60.dp.toPx()
                rotationZ = tilt + bob * 0.9f
                alpha = show.coerceIn(0f, 1f)
                scaleX = 0.6f + 0.4f * show; scaleY = scaleX
            },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(size * 0.26f),
        fill = bottom, lip = bottom.deepen(0.35f), lipHeight = 5.dp,
        brush = Brush.verticalGradient(listOf(top, bottom))
    ) {
        Text("$value", fontSize = (size.value * 0.44f).sp, fontWeight = FontWeight.Black, color = ink, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun ClassicSplash(onAnimationFinished: () -> Unit) {
    val context = LocalContext.current

    var startAnimation by remember { mutableStateOf(false) }

    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.3f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessLow),
        label = "LogoSpring"
    )
    val textA by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(1100, delayMillis = 700),
        label = "TextFade"
    )
    val tilesIn by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessVeryLow),
        label = "TilesIn"
    )

    val infinite = rememberInfiniteTransition(label = "splash")
    val clock by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "clock")
    val bob by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(2300, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")

    // Las letras de PARDOS caen una tras otra
    val letters = remember { "PARDOS".map { it to Animatable(0f) } }
    LaunchedEffect(Unit) {
        delay(100)
        startAnimation = true
        try {
            val mediaPlayer = MediaPlayer.create(context, R.raw.move_pop)
            mediaPlayer.setOnCompletionListener { it.release() }
            mediaPlayer.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        letters.forEachIndexed { i, (_, anim) ->
            launch {
                delay(450L + i * 90L)
                anim.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 320f))
            }
        }
        delay(3000)
        onAnimationFinished()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().pardosBackdrop()) {
        val w = maxWidth
        val h = maxHeight
        // pétalos y motas que flotan
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val rnd = kotlin.random.Random(12)
            repeat(22) { i ->
                val sp = 0.25f + rnd.nextFloat() * 0.5f
                val ph = rnd.nextFloat()
                val prog = (clock * sp * 2f + ph) % 1f
                val x = size.width * ((rnd.nextFloat() + 0.06f * kotlin.math.sin((prog * 6 + i).toDouble()).toFloat()).coerceIn(0f, 1f))
                val y = size.height * (1.05f - prog * 1.15f)
                val a = (kotlin.math.sin(prog * Math.PI.toFloat()) * 0.7f).coerceIn(0f, 1f)
                val col = if (i % 3 == 0) Color(0xFFF2CC8F) else if (i % 3 == 1) Color(0xFFE9A5B5) else Color(0xFFB6D4C4)
                drawOval(col.copy(alpha = a * 0.55f), androidx.compose.ui.geometry.Offset(x, y), androidx.compose.ui.geometry.Size(9f + (i % 4) * 3f, 6f + (i % 3) * 2f))
            }
        }

        // fichas de juguete alrededor del logo
        SplashTile(2, 54.dp, -10f, 0, Modifier.offset(x = w * 0.10f, y = h * 0.30f), tilesIn)
        SplashTile(4, 50.dp, 8f, 1, Modifier.offset(x = w * 0.78f, y = h * 0.26f), tilesIn)
        SplashTile(8, 58.dp, 12f, 2, Modifier.offset(x = w * 0.80f, y = h * 0.50f), tilesIn)
        SplashTile(16, 46.dp, -14f, 3, Modifier.offset(x = w * 0.08f, y = h * 0.52f), tilesIn)
        SplashTile(32, 52.dp, 6f, 4, Modifier.offset(x = w * 0.14f, y = h * 0.14f), tilesIn)

        Column(
            modifier = Modifier.align(Alignment.Center).offset(y = (-26).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // sombra bajo el logo flotante + logo
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(R.drawable.ic_logo_classic),
                    contentDescription = "ParDos",
                    modifier = Modifier
                        .size(300.dp)
                        .graphicsLayer { translationY = (bob * 12f - 6f).dp.toPx(); scaleX = logoScale; scaleY = logoScale }
                )
            }

            Row {
                letters.forEach { (ch, anim) ->
                    Text(
                        text = ch.toString(),
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Black,
                        color = CoffeeDark,
                        letterSpacing = 4.sp,
                        modifier = Modifier.graphicsLayer {
                            alpha = anim.value.coerceIn(0f, 1f)
                            translationY = (1f - anim.value) * 48.dp.toPx()
                        }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            // subrayado que se dibuja de izquierda a derecha
            Box(
                Modifier.width(150.dp * textA).height(4.dp)
                    .background(Brush.horizontalGradient(listOf(Color(0xFFE07A5F), Color(0xFFF2CC8F), Color(0xFF6B9E86))), CircleShape)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Math Zen Puzzle",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = CoffeeMedium,
                letterSpacing = 2.sp,
                modifier = Modifier.alpha(textA)
            )
        }

        // créditos
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 46.dp).alpha(textA),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Desarrollada por", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CoffeeDark.copy(alpha = 0.5f), letterSpacing = 0.5.sp)
            Spacer(Modifier.height(4.dp))
            Text("Carlos García Huerta", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CoffeeDark)
            Spacer(Modifier.height(16.dp))
            JellyCard(shape = CircleShape, lipHeight = 4.dp) {
                Text(
                    text = "KorKoor Studios", fontSize = 12.sp, fontWeight = FontWeight.Black, color = CoffeeMedium, letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
                )
            }
        }
    }
}
