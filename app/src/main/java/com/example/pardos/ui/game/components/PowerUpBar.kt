package com.example.pardos.ui.game.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pardos.ui.game.GameViewModel

/*@Composable
fun PowerUpButton(
    label: String,
    icon: ImageVector,
    color: Color,
    lastUseTime: Long,
    viewModel: GameViewModel,
    onUse: () -> Unit,
    onWatchAd: () -> Unit
) {
    val isAvailable = viewModel.isPowerUpAvailable(lastUseTime)
    val remainingText = viewModel.getRemainingTime(lastUseTime)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = { if (isAvailable) onUse() else onWatchAd() },
            color = if (isAvailable) Color.White.copy(alpha = 0.85f) else Color.DarkGray.copy(alpha = 0.1f),
            shape = RoundedCornerShape(20.dp),
            shadowElevation = if (isAvailable) 6.dp else 0.dp,
            modifier = Modifier.size(68.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isAvailable) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(30.dp))
                } else {
                    // ⏳ Estado de Cooldown
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(remainingText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Icon(Icons.Default.PlayCircle, null, tint = color, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        Text(
            text = if (isAvailable) label else "ANUNCIO",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = if (isAvailable) Color(0xFF3D405B) else color,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun PowerUpButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    // 💡 Añadimos una pequeña interacción de escala
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.92f else 1f, label = "scale")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.scale(scale) // Aplicamos la escala a todo el botón
    ) {
        Surface(
            onClick = onClick,
            interactionSource = interactionSource, // Conectamos la interacción
            color = Color.White.copy(alpha = 0.85f),
            shape = RoundedCornerShape(20.dp),
            shadowElevation = if (isPressed) 2.dp else 6.dp, // Bajamos la sombra al presionar
            modifier = Modifier.size(68.dp)
        ){
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(30.dp))

                // Indicador sutil de que requiere ver un video
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = color.copy(alpha = 0.4f),
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                )
            }
        }
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF3D405B).copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 8.dp),
            letterSpacing = 1.sp
        )
    }
}*/