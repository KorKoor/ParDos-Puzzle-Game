package com.example.pardos.ui.game.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pardos.domain.model.BoardState

/*@Composable
fun GameStatusOverlays(
    state: BoardState,
    onNextLevel: () -> Unit,
    onRestart: () -> Unit,
    formattedTime: String
) {
    val isVisible = state.isLevelCompleted || state.isGameOver

    // Animación del fondo oscurecido (Scrim)
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(400)),
        exit = fadeOut(animationSpec = tween(400))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF3D405B).copy(alpha = 0.8f)), // Fondo semi-transparente
            contentAlignment = Alignment.Center
        ) {
            // --- MODAL DE VICTORIA ---
            if (state.isLevelCompleted) {
                ModalContent(
                    title = "¡EXCELENTE!",
                    subtitle = "Nivel superado en $formattedTime",
                    emoji = "⭐",
                    buttonText = "SIGUIENTE NIVEL",
                    buttonColor = Color(0xFF81B29A),
                    onAction = onNextLevel
                )
            }

            // --- MODAL DE GAME OVER ---
            if (state.isGameOver) {
                val gameOverMessage = if (state.elapsedTime <= 0) "¡Se acabó el tiempo!" else "Sin movimientos"

                ModalContent(
                    title = "FIN DEL JUEGO",
                    subtitle = "$gameOverMessage\nPuntaje final: ${state.score}",
                    emoji = "🧩",
                    buttonText = "REINTENTAR",
                    buttonColor = Color(0xFFE07A5F),
                    onAction = onRestart
                )
            }
        }
    }
}

@Composable
private fun ModalContent(
    title: String,
    subtitle: String,
    emoji: String,
    buttonText: String,
    buttonColor: Color,
    onAction: () -> Unit
) {
    // Animación de entrada para la tarjeta
    val transitionState = remember { MutableTransitionState(false).apply { targetState = true } }

    AnimatedVisibility(
        visibleState = transitionState,
        enter = scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFCF0)), // Color crema Pardos
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = emoji, fontSize = 56.sp)

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF3D405B),
                    letterSpacing = 1.sp
                )

                Text(
                    text = subtitle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = Color(0xFF3D405B).copy(alpha = 0.7f),
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Medium
                )

                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text = buttonText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}*/