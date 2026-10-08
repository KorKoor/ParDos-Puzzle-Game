package com.korkoor.pardos.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import com.korkoor.pardos.ui.design.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.domain.achievements.Achievement
/*
@Composable
fun AchievementCard(achievement: Achievement, isUnlocked: Boolean) {
    // 🎨 Colores dinámicos basados en el estado de desbloqueo
    val mainColor = if (isUnlocked) achievement.color else com.korkoor.pardos.ui.design.Navy.copy(alpha = 0.2f)
    val contentAlpha = if (isUnlocked) 1f else 0.4f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = if (isUnlocked) 0.9f else 0.9f), // Mantener visibilidad
        shadowElevation = if (isUnlocked) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(mainColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isUnlocked) achievement.icon else Icons.Filled.Lock,
                    contentDescription = null,
                    tint = mainColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    // ✅ Cambiamos achievement.title por stringResource
                    text = stringResource(achievement.titleResId),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = com.korkoor.pardos.ui.design.Navy.copy(alpha = contentAlpha)
                )
                Text(
                    // ✅ Cambiamos achievement.description por stringResource
                    text = stringResource(achievement.descriptionResId),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = com.korkoor.pardos.ui.design.Navy.copy(alpha = if (isUnlocked) 0.6f else 0.3f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}
*/