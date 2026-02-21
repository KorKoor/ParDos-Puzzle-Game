package com.korkoor.pardos.ui.menu

import android.media.MediaPlayer
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
private val CoffeeCream = Color(0xFFFDFBF7) // Fondo crema suave
private val CoffeeLatte = Color(0xFFEFEBE9) // Círculo decorativo
private val CoffeeDark = Color(0xFF4E342E)  // Texto principal (Café expreso)
private val CoffeeMedium = Color(0xFF795548) // Texto secundario (Moca)
private val GoldAccent = Color(0xFFD7CCC8)  // Detalles sutiles

@Composable
fun AnimatedSplashScreen(onAnimationFinished: () -> Unit) {
    val context = LocalContext.current

    // Estados de animación
    var startAnimation by remember { mutableStateOf(false) }

    // 1. ANIMACIÓN DE ESCALA (REBOTE / POP)
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.3f, // Empieza pequeño, crece al 100%
        animationSpec = spring(
            dampingRatio = 0.4f, // Menos amortiguación = Más rebote
            stiffness = Spring.StiffnessLow
        ),
        label = "LogoSpring"
    )

    // 2. ANIMACIÓN DE TEXTOS (FADE IN)
    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(1200, delayMillis = 300), // Aparece un poco después del pop
        label = "TextFade"
    )

    // CONTROLADOR DE TIEMPO Y SONIDO
    LaunchedEffect(Unit) {
        // A) Esperamos un instante pequeño para que la UI se dibuje
        delay(100)

        // B) Iniciamos la animación visual
        startAnimation = true

        // C) 🔊 REPRODUCIMOS EL POP (Asegúrate de tener 'pop.mp3' en res/raw)
        // Usamos un try-catch por si el archivo no existe para que no crashee
        try {
            val mediaPlayer = MediaPlayer.create(context, R.raw.move_pop) // 👈 TU ARCHIVO AQUÍ
            mediaPlayer.setOnCompletionListener { it.release() }
            mediaPlayer.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // D) Esperamos 3 segundos y vamos al menú
        delay(3000)
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CoffeeCream), // Fondo color crema
        contentAlignment = Alignment.Center
    ) {
        // Elemento decorativo de fondo (Círculo sutil)
        Surface(
            modifier = Modifier
                .size(300.dp)
                .scale(scale) // Se mueve con el rebote
                .alpha(0.5f),
            shape = CircleShape,
            color = CoffeeLatte
        ) {}

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // LOGO / TÍTULO CON REBOTE
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "PARDOS",
                    fontSize = 56.sp, // Más grande e imponente
                    fontWeight = FontWeight.Black,
                    color = CoffeeDark,
                    letterSpacing = 6.sp,
                    modifier = Modifier
                        .scale(scale) // Aplica el resorte aquí
                        // Sombra suave para darle profundidad 3D
                        .shadow(0.dp, spotColor = CoffeeMedium)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // SUBTÍTULO ELEGANTE
            Text(
                text = "Math Zen Puzzle",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = CoffeeMedium,
                letterSpacing = 1.sp,
                modifier = Modifier.alpha(alpha)
            )
        }

        // FOOTER (CRÉDITOS)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .alpha(alpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Desarrollada por",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = CoffeeMedium.copy(alpha = 0.6f),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Carlos García Huerta",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = CoffeeDark
            )

            Spacer(modifier = Modifier.height(24.dp))

            // MARCA KORKOOR (Estilo Sello)
            Box(
                modifier = Modifier
                    .background(
                        color = CoffeeDark.copy(alpha = 0.05f),
                        shape = CircleShape
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "KorKoor Studios",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = CoffeeMedium,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}