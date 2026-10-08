package com.korkoor.pardos.ui.menu

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.data.local.MissionManager
import com.korkoor.pardos.data.local.ProfileManager
import com.korkoor.pardos.domain.model.DailyMission
import com.korkoor.pardos.domain.model.MissionType

@Composable
fun DailyMissionsCard(
    missionManager: MissionManager,
    profileManager: ProfileManager
) {
    val context = LocalContext.current
    var missions by remember { mutableStateOf(emptyList<DailyMission>()) }
    var refreshTrigger by remember { mutableIntStateOf(0) } // Para forzar recarga al reclamar

    // --- PALETA ZEN ---
    val cremaCard = Color(0xFFFFFFFF)
    val cafeOscuro = Color(0xFF5D4037)
    val cafeSuave = Color(0xFF8D6E63)
    val terracota = Color(0xFFE07A5F)
    val verdeZen = Color(0xFF81B29A)

    // Cargamos las misiones
    LaunchedEffect(refreshTrigger) {
        missions = missionManager.getTodayMissions()
    }

    if (missions.isNotEmpty()) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(32.dp),
            color = cremaCard,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, cafeSuave.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // CABECERA
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "MISIONES DIARIAS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = cafeOscuro,
                        letterSpacing = 1.5.sp
                    )
                    Icon(Icons.Rounded.EventAvailable, contentDescription = null, tint = terracota)
                }

                // LISTA DE MISIONES
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    missions.forEach { mission ->
                        val isClaimed = missionManager.isMissionClaimed(mission.id)
                        MissionItemRow(
                            mission = mission,
                            isClaimed = isClaimed,
                            verdeZen = verdeZen,
                            cafeOscuro = cafeOscuro,
                            terracota = terracota,
                            onClaim = {
                                // 1. Reclamar en el Manager para apagar el botón
                                missionManager.claimMissionReward(mission.id)

                                // 2. Sumar la XP real al perfil
                                val profile = profileManager.getProfile()
                                // Súper truco: Usamos la misma función de XP de victoria, pero directa
                                val newXp = profile.currentXp + mission.xpReward
                                var newLevel = profile.playerLevel
                                var nextLimit = profile.xpToNextLevel

                                var finalXp = newXp
                                while (finalXp >= nextLimit) {
                                    finalXp -= nextLimit
                                    newLevel++
                                    nextLimit += 50
                                }

                                profileManager.saveProfile(
                                    profile.copy(
                                        currentXp = finalXp,
                                        playerLevel = newLevel,
                                        xpToNextLevel = nextLimit
                                    )
                                )

                                Toast.makeText(context, "¡+${mission.xpReward} XP Reclamados!", Toast.LENGTH_SHORT).show()
                                refreshTrigger++ // Refresca la UI
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MissionItemRow(
    mission: DailyMission,
    isClaimed: Boolean,
    verdeZen: Color,
    cafeOscuro: Color,
    terracota: Color,
    onClaim: () -> Unit
) {
    // Progreso dinámico y seguro
    val progressRatio = if (mission.targetValue > 0) {
        (mission.currentProgress.toFloat() / mission.targetValue.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progressRatio,
        animationSpec = tween(1000),
        label = "ProgressAnim"
    )

    // Selección de Icono por tipo
    val icon = when (mission.type) {
        MissionType.PLAY_GAMES -> Icons.Rounded.PlayCircle
        MissionType.WIN_LEVELS -> Icons.Rounded.EmojiEvents
        MissionType.MERGE_PAIRS -> Icons.Rounded.JoinInner
        MissionType.REACH_BLOCK -> Icons.Rounded.LooksOne
        MissionType.EARN_STARS -> Icons.Rounded.Star
        MissionType.WIN_UNDER_TIME -> Icons.Rounded.Timer
        MissionType.WIN_NO_POWERUPS -> Icons.Rounded.Block
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ICONO CIRCULAR
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = if (isClaimed) verdeZen.copy(alpha = 0.1f) else Color(0xFFFDF8F1),
            border = BorderStroke(1.dp, if (isClaimed) verdeZen else Color.LightGray.copy(alpha = 0.3f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isClaimed) Icons.Rounded.Check else icon,
                    contentDescription = null,
                    tint = if (isClaimed) verdeZen else cafeOscuro.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // TEXTOS Y BARRA
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mission.description,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = cafeOscuro,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Barra de progreso animada
                LinearProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (mission.isCompleted) verdeZen else terracota,
                    trackColor = Color.LightGray.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Texto progreso
                Text(
                    text = "${mission.currentProgress}/${mission.targetValue}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = cafeOscuro.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // BOTÓN / RECOMPENSA
        if (isClaimed) {
            Text(
                text = "COBRADO",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = verdeZen,
                letterSpacing = 1.sp
            )
        } else if (mission.isCompleted) {
            Button(
                onClick = onClaim,
                colors = ButtonDefaults.buttonColors(containerColor = verdeZen),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("RECLAMAR", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        } else {
            Surface(
                color = terracota.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "+${mission.xpReward} XP",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = terracota
                )
            }
        }
    }
}