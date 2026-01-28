package com.example.pardos.ui.game.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.ui.theme.GameTheme

@Composable
fun SupportCreatorDialog(onDismiss: () -> Unit, currentTheme: GameTheme) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(16.dp),
        shape = RoundedCornerShape(32.dp),
        color = Color.White.copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "HECHO CON ☕ POR KOR/n/tCarlos García Huerta :)",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = currentTheme.accentColor
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "ParDos es un proyecto independiente. Si te relaja jugar, considera apoyarme para seguir creando contenido sin anuncios intrusivos.",
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = Color(0xFF3D405B).copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(24.dp))

            // Botón de Redes
            OutlinedButton(
                onClick = { /* Abrir link Instagram/TikTok */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("SÍGUEME EN REDES", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))

            // Botón de "Café" (Vía Google Play IAP)
            Button(
                onClick = { /* Trigger IAP */ },
                colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accentColor),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "INVÍTAME UN CAFÉ (APOYAR)",
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
