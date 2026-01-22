package com.korkoor.pardos.ui.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
/*
@Composable
fun GameModal(
    title: String,
    subtitle: String,
    buttonText: String,
    color: Color,
    onAction: () -> Unit
) {
    // Fondo oscuro con un toque de color del estado para mayor inmersión
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF3D405B).copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(36.dp), // Bordes más circulares para look soft
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(0.9f)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 40.dp, horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF3D405B),
                    letterSpacing = 1.sp
                )

                Text(
                    text = subtitle,
                    modifier = Modifier.padding(top = 12.dp, bottom = 32.dp),
                    color = Color.Gray,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )

                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = color),
                    shape = RoundedCornerShape(24.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    Text(
                        text = buttonText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        letterSpacing = 2.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
*/