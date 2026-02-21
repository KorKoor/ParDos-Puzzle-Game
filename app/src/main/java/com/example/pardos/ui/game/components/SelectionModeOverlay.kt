package com.korkoor.pardos.ui.game.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.JoinFull
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.R

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

@Composable
fun SelectionModeOverlay(
    isManualMerge: Boolean,
    accentColor: Color,
    onCancel: () -> Unit
) {
    // 🚀 ESTADO LOCAL: Controla si el overlay es visible o no
    // Esto evita que al cerrarse se apague el PowerUp en el ViewModel
    var isDismissed by remember { mutableStateOf(false) }

    // Si ya se descartó visualmente, no dibujamos nada, pero el modo sigue activo
    if (isDismissed) return

    // 🚀 1. AUTO-DESCARTE VISUAL (5 SEGUNDOS)
    LaunchedEffect(Unit) {
        delay(5000)
        isDismissed = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "HandAnim")

    val translateY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HandBounce"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HandScale"
    )

    // 2. CONTENEDOR PRINCIPAL
    Box(
        modifier = Modifier
            .fillMaxSize()
            // 🚀 2. CIERRE VISUAL AL PRIMER CLICK
            // Detectamos el tap para poner isDismissed en true y liberar el tablero
            .pointerInput(Unit) {
                detectTapGestures(onTap = { isDismissed = true })
            },
        contentAlignment = Alignment.Center
    ) {
        // Fondo visual (El "Dim")
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        )

        // Contenido informativo
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(90.dp)
                    .graphicsLayer {
                        translationY = translateY
                        scaleX = scale
                        scaleY = scale
                    }
                    .shadow(30.dp, CircleShape, spotColor = accentColor)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = if (isManualMerge)
                    stringResource(R.string.powerup_manual_instruction_merge)
                else
                    stringResource(R.string.powerup_manual_instruction_clean),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp)
            )

            Spacer(modifier = Modifier.height(60.dp))

            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .size(70.dp)
                    .scale(scale)
                    .shadow(15.dp, CircleShape, spotColor = accentColor)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isManualMerge) Icons.Default.JoinFull else Icons.Default.CleaningServices,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 🚀 BOTÓN DE CANCELAR REAL
            // Este es el único que llama a onCancel() para apagar el modo en el ViewModel
            Surface(
                onClick = onCancel,
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = stringResource(android.R.string.cancel).uppercase(),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        }
    }
}