package com.korkoor.pardos.domain.achievements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.R
@Composable
fun AchievementPopUp(achievement: Achievement) {
    // Eliminamos el Box fillMaxSize para que no bloquee los clics en el tablero
    Surface(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .height(85.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF3D405B),
        shadowElevation = 15.dp,
        border = BorderStroke(1.dp, achievement.color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(achievement.color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = achievement.icon,
                    contentDescription = null,
                    tint = achievement.color,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    // ✅ Usamos la etiqueta general de "Logro Desbloqueado"
                    text = stringResource(R.string.achievement_unlocked),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = achievement.color,
                    letterSpacing = 1.5.sp
                )
                Text(
                    // ✅ Usamos el ID del título del logro específico
                    text = stringResource(achievement.titleResId),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1
                )
            }
        }
    }
}

