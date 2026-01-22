package com.korkoor.pardos.ui.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.domain.model.BoardState
/*
@Composable
fun FooterSection(
    state: BoardState,
    timerContent: @Composable () -> Unit
) {
    // Contenedor principal del pie de página
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tarjeta de Puntuación Actual
        InfoCard(
            label = "PUNTOS",
            value = "${state.score}",
            containerColor = Color.White.copy(alpha = 0.9f)
        )

        // Aquí se renderiza el TimerDisplay que pasamos desde GameScreen
        Box(contentAlignment = Alignment.Center) {
            timerContent()
        }

        // Tarjeta de Meta del Nivel (Ej. 16, 32, 64...)
        InfoCard(
            label = "META",
            value = "${state.levelLimit}",
            containerColor = Color.White.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun InfoCard(
    label: String,
    value: String,
    containerColor: Color
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.width(80.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3D405B).copy(alpha = 0.5f),
                letterSpacing = 1.sp
            )
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF3D405B)
            )
        }
    }
}
*/