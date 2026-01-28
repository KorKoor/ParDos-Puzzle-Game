// FloatingScore.kt

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.UUID

// Modelo de datos para un texto flotante
data class FloatingScoreModel(
    val id: String = UUID.randomUUID().toString(),
    val value: Int,
    val col: Int,
    val row: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun FloatingScore(
    score: FloatingScoreModel,
    tileSize: Dp,
    onFinished: (String) -> Unit
) {
    // Estado de la animación
    val animState = remember { Animatable(0f) }

    LaunchedEffect(score.id) {
        // Anima de 0 a 1 y luego avisa que terminó
        animState.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
        onFinished(score.id)
    }

    // Cálculos de animación
    val floatUpDistance = 60.dp // Cuánto sube el texto
    val currentOffset = tileSize * 0.2f - (floatUpDistance * animState.value) // Sube
    val currentAlpha = 1f - animState.value // Se desvanece
    val currentScale = 0.5f + (animState.value * 0.5f) // Crece un poco

    val xPos = (tileSize * score.col) + (tileSize / 4) // Centrado horizontal aprox
    val yPos = (tileSize * score.row) + (tileSize / 2) // Centrado vertical inicial

    Text(
        text = "+${score.value}",
        color = Color(0xFF3D405B).copy(alpha = currentAlpha),
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
            .offset(x = xPos, y = yPos + currentOffset)
            .alpha(currentAlpha)
    )
}