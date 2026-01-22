package com.korkoor.pardos.ui.game

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.domain.achievements.gameAchievements
import com.korkoor.pardos.domain.achievements.Achievement
import com.korkoor.pardos.ui.theme.GameTheme
import com.korkoor.pardos.R

@Composable
fun AchievementsScreen(
    unlockedIds: Set<String>,
    currentTheme: GameTheme,
    onBack: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val bgGradient = Brush.verticalGradient(colors = currentTheme.colors)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = if (isLandscape) 32.dp else 24.dp)
        ) {
            // --- CABECERA ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (isLandscape) 16.dp else 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color(0xFF3D405B)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = stringResource(R.string.menu_achievements),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF3D405B),
                    letterSpacing = 2.sp
                )
            }

            // Cálculos de progreso
            val totalAchievements = gameAchievements.all.size
            val unlockedCount = unlockedIds.size
            val progress = if (totalAchievements > 0) unlockedCount.toFloat() / totalAchievements else 0f

            if (isLandscape) {
                // --- DISEÑO HORIZONTAL (LANDSCAPE) ---
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Panel Izquierdo: Progreso
                    Box(modifier = Modifier.weight(0.8f)) {
                        AchievementHeaderProgress(
                            progress = progress,
                            count = unlockedCount,
                            total = totalAchievements,
                            accentColor = currentTheme.accentColor
                        )
                    }

                    // Panel Derecho: Grid de Logros
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.weight(1.2f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(gameAchievements.all) { achievement ->
                            AchievementCard(
                                achievement = achievement,
                                isUnlocked = unlockedIds.contains(achievement.id)
                            )
                        }
                    }
                }
            } else {
                // --- DISEÑO VERTICAL (PORTRAIT) ---
                AchievementHeaderProgress(
                    progress = progress,
                    count = unlockedCount,
                    total = totalAchievements,
                    accentColor = currentTheme.accentColor
                )

                Spacer(modifier = Modifier.height(24.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(gameAchievements.all) { achievement ->
                        AchievementCard(
                            achievement = achievement,
                            isUnlocked = unlockedIds.contains(achievement.id)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AchievementHeaderProgress(progress: Float, count: Int, total: Int, accentColor: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.6f),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.ach_collection_title),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3D405B).copy(alpha = 0.6f)
                )
                Text(
                    text = "$count / $total",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = accentColor,
                trackColor = Color(0xFF3D405B).copy(alpha = 0.08f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

@Composable
fun AchievementCard(achievement: Achievement, isUnlocked: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = if (isUnlocked) Color.White else Color.White.copy(alpha = 0.3f),
        shadowElevation = if (isUnlocked) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .alpha(if (isUnlocked) 1f else 0.5f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        if (isUnlocked) achievement.color.copy(alpha = 0.15f)
                        else Color.Gray.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = achievement.icon,
                    contentDescription = null,
                    tint = if (isUnlocked) achievement.color else Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = stringResource(achievement.titleResId),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF3D405B)
                )
                Text(
                    text = stringResource(achievement.descriptionResId),
                    fontSize = 11.sp,
                    color = Color(0xFF3D405B).copy(alpha = 0.6f),
                    lineHeight = 14.sp,
                    maxLines = 2
                )
            }
        }
    }
}