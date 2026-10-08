package com.korkoor.pardos.ui.menu

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.R
import com.korkoor.pardos.data.local.EconomyManager
import com.korkoor.pardos.data.local.LocalDay
import com.korkoor.pardos.data.local.MissionManager
import com.korkoor.pardos.data.local.ProfileManager
import com.korkoor.pardos.data.local.RetentionManager
import com.korkoor.pardos.domain.economy.Economy
import com.korkoor.pardos.domain.model.DailyMission
import com.korkoor.pardos.domain.model.DailyMissionPlan
import com.korkoor.pardos.domain.model.MissionType
import com.korkoor.pardos.domain.model.PerfectDays
import com.korkoor.pardos.ui.design.*
import kotlinx.coroutines.delay
import java.util.Calendar

/** Milisegundos hasta la próxima medianoche local (cuando se renuevan las misiones). */
private fun msUntilMidnight(now: Long): Long {
    val c = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    return c.timeInMillis - now
}

/** "5 h 12 min" / "42 min": sin segundos, la tarjeta se refresca cada 30 s. */
private fun formatLeft(ms: Long): String {
    val totalMin = (ms / 60_000).coerceAtLeast(1)
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) "$h h ${m.toString().padStart(2, '0')} min" else "$m min"
}

private fun missionKind(t: MissionType): CozyKind = when (t) {
    MissionType.PLAY_GAMES -> CozyKind.DICE
    MissionType.WIN_LEVELS -> CozyKind.TROPHY
    MissionType.MERGE_PAIRS -> CozyKind.SPARKLES
    MissionType.REACH_BLOCK -> CozyKind.CROWN
    MissionType.EARN_STARS -> CozyKind.STAR
    MissionType.WIN_UNDER_TIME -> CozyKind.TIMER
    MissionType.WIN_NO_POWERUPS -> CozyKind.SHIELD
}

private fun tierColor(t: DailyMissionPlan.Tier): Color = when (t) {
    DailyMissionPlan.Tier.LIGHT -> Sage
    DailyMissionPlan.Tier.MEDIUM -> Gold
    DailyMissionPlan.Tier.HARD -> Terracotta
}

private fun tierName(t: DailyMissionPlan.Tier): String = when (t) {
    DailyMissionPlan.Tier.LIGHT -> "FÁCIL"
    DailyMissionPlan.Tier.MEDIUM -> "MEDIA"
    DailyMissionPlan.Tier.HARD -> "DIFÍCIL"
}

/**
 * Misiones del día: tres retos (uno fácil, uno medio, uno difícil) con progreso, premios visibles, cuenta atrás para que
 * se renueven y la racha de días perfectos (cobrar las tres seguidas) con su siguiente premio.
 */
@Composable
fun DailyMissionsCard(
    missionManager: MissionManager,
    profileManager: ProfileManager
) {
    val context = LocalContext.current
    val retention = remember { RetentionManager(context) }
    var missions by remember { mutableStateOf(emptyList<DailyMission>()) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(refreshTrigger) { missions = missionManager.getTodayMissions() }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(30_000) } }

    if (missions.isEmpty()) return

    val claimedCount = missions.count { missionManager.isMissionClaimed(it.id) }
    val readyCount = missions.count { it.isCompleted && !missionManager.isMissionClaimed(it.id) }
    val perfect = remember(refreshTrigger) { retention.perfectDays() }
    val perfectToday = remember(refreshTrigger) { retention.perfectToday() }
    val nextMilestone = PerfectDays.nextMilestone(perfect)

    JellyColumn(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(32.dp),
        padding = PaddingValues(18.dp)
    ) {
        // ---- Cabecera: título + cuenta atrás + anillo de progreso ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SectionLabel("Misiones del día")
                Spacer(Modifier.height(6.dp))
                Text(
                    "Se renuevan en ${formatLeft(msUntilMidnight(now))}",
                    fontSize = 12.sp, color = InkSecondary, fontWeight = FontWeight.Bold
                )
            }
            ProgressRing(done = claimedCount, total = missions.size, ready = readyCount)
        }
        Spacer(Modifier.height(14.dp))

        // ---- Las tres misiones ----
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            missions.forEachIndexed { index, mission ->
                val isClaimed = missionManager.isMissionClaimed(mission.id)
                MissionRow(
                    mission = mission,
                    isClaimed = isClaimed,
                    modifier = Modifier.staggerIn(index),
                    onClaim = {
                        missionManager.claimMissionReward(mission.id)
                        val coins = com.korkoor.pardos.domain.events.EventCalendar.apply(
                            com.korkoor.pardos.domain.rewards.CoinRewards.forMission(mission.xpReward),
                            com.korkoor.pardos.domain.events.EventCalendar.coinMultiplier(LocalDay.today())
                        )
                        EconomyManager(context).addCoins(coins)

                        // XP real al perfil
                        val profile = profileManager.getProfile()
                        var finalXp = profile.currentXp + mission.xpReward
                        var newLevel = profile.playerLevel
                        var nextLimit = profile.xpToNextLevel
                        while (finalXp >= nextLimit) { finalXp -= nextLimit; newLevel++; nextLimit += 50 }
                        profileManager.saveProfile(profile.copy(currentXp = finalXp, playerLevel = newLevel, xpToNextLevel = nextLimit))

                        // Retención: puntos de pase, bonus por cobrar las tres y racha de días perfectos
                        val allClaimed = missions.all { missionManager.isMissionClaimed(it.id) }
                        val bonus = retention.onDailyMissionClaimed(allClaimed)
                        val milestone = retention.lastPerfectMilestone.also { retention.lastPerfectMilestone = null }
                        Toast.makeText(
                            context,
                            when {
                                milestone != null -> "¡Racha perfecta de ${milestone.days} días! +${milestone.gems} gemas"
                                bonus -> "¡Día perfecto! Cofre + ${Economy.DAILY_MISSIONS_BONUS_GEMS} gemas de regalo"
                                else -> "+$coins monedas y +${mission.xpReward} XP"
                            },
                            Toast.LENGTH_SHORT
                        ).show()
                        refreshTrigger++
                    }
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---- Premio por las tres + racha perfecta ----
        JellyRow(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            fill = if (perfectToday) Sage.lighten(0.86f) else Gold.lighten(0.88f),
            lip = (if (perfectToday) Sage else Gold).copy(alpha = 0.45f),
            lipHeight = 4.dp,
            padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CozyIcon(CozyKind.CHEST, Modifier.size(40.dp).then(if (readyCount > 0 || claimedCount == missions.size) Modifier else Modifier))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (perfectToday) "¡Día perfecto!" else "Cobra las 3 y llévate un cofre",
                    fontSize = 13.sp, fontWeight = FontWeight.Black, color = Navy
                )
                CozyText(
                    if (perfectToday) "Cofre + ${Economy.DAILY_MISSIONS_BONUS_GEMS}◆ ya en tu inventario"
                    else "+${Economy.DAILY_MISSIONS_BONUS_GEMS}◆ y un cofre común",
                    fontSize = 11.sp, color = InkSecondary
                )
            }
            Text("$claimedCount/${missions.size}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = if (perfectToday) Sage else Navy.copy(alpha = 0.6f))
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            Image(
                painter = painterResource(R.drawable.ico_fire),
                contentDescription = null,
                modifier = Modifier.size(34.dp).then(if (perfect > 0) Modifier.breathing(0.08f, 900) else Modifier),
                alpha = if (perfect > 0) 1f else 0.35f
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (perfect > 0) "Racha perfecta: $perfect ${if (perfect == 1) "día" else "días"}" else "Empieza tu racha perfecta",
                    fontSize = 13.sp, fontWeight = FontWeight.Black, color = Navy
                )
                CozyText(
                    nextMilestone?.let { m ->
                        val left = m.days - perfect
                        "Faltan $left ${if (left == 1) "día" else "días"} para +${m.gems}◆"
                    } ?: "¡Has llegado al último hito!",
                    fontSize = 11.sp, color = InkSecondary
                )
            }
            // puntitos de hitos
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                PerfectDays.milestones.forEach { m ->
                    Box(
                        Modifier.size(if (perfect >= m.days) 10.dp else 8.dp)
                            .background(if (perfect >= m.days) Terracotta else Navy.copy(alpha = 0.14f), CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressRing(done: Int, total: Int, ready: Int) {
    val progress by animateFloatAsState(done.toFloat() / total.coerceAtLeast(1), tween(700, easing = FastOutSlowInEasing), label = "missionRing")
    Box(Modifier.size(58.dp).then(if (ready > 0) Modifier.breathing(0.06f, 800) else Modifier), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx()
            val inset = stroke / 2
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(Navy.copy(alpha = 0.09f), -90f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(
                Brush.sweepGradient(listOf(Sage, Gold, Sage)), -90f, 360f * progress, false, Offset(inset, inset), arc,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Text("$done/$total", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Navy)
    }
}

@Composable
private fun MissionRow(
    mission: DailyMission,
    isClaimed: Boolean,
    onClaim: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tier = DailyMissionPlan.tierOf(mission)
    val color = tierColor(tier)
    val ready = mission.isCompleted && !isClaimed
    val ratio = if (mission.targetValue > 0) (mission.currentProgress.toFloat() / mission.targetValue).coerceIn(0f, 1f) else 0f
    val animated by animateFloatAsState(ratio, tween(900, easing = FastOutSlowInEasing), label = "missionProgress")
    val coins = com.korkoor.pardos.domain.rewards.CoinRewards.forMission(mission.xpReward)

    JellyRow(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        fill = when {
            isClaimed -> Color(0xFFF1ECE0)
            ready -> color.lighten(0.84f)
            else -> Color.White
        },
        lip = when {
            isClaimed -> Color(0xFFDCD1BB)
            ready -> color.copy(alpha = 0.55f)
            else -> defaultLip(Color.White)
        },
        lipHeight = if (isClaimed) 3.dp else 4.dp,
        borderColor = if (ready) color else null,
        padding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Medalla del tipo de misión
        Box(
            Modifier.size(48.dp).background(
                Brush.verticalGradient(
                    if (isClaimed) listOf(Sage.lighten(0.2f), Sage) else listOf(color.lighten(0.35f), color.copy(alpha = 0.85f))
                ),
                CircleShape
            ),
            contentAlignment = Alignment.Center
        ) {
            if (isClaimed) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
            } else {
                Box(Modifier.size(34.dp).background(Color.White.copy(alpha = 0.92f), CircleShape), contentAlignment = Alignment.Center) {
                    CozyIcon(missionKind(mission.type), Modifier.size(26.dp))
                }
            }
        }
        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(color, CircleShape))
                Spacer(Modifier.width(5.dp))
                Text(tierName(tier), fontSize = 9.sp, fontWeight = FontWeight.Black, color = color, letterSpacing = 1.2.sp)
            }
            Text(
                mission.description, fontSize = 13.sp, fontWeight = FontWeight.Black,
                color = if (isClaimed) Navy.copy(alpha = 0.45f) else Navy, maxLines = 2, lineHeight = 16.sp
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // barra con relieve
                Box(Modifier.weight(1f).height(10.dp).background(Navy.copy(alpha = 0.08f), CircleShape)) {
                    Box(
                        Modifier.fillMaxHeight().fillMaxWidth(animated.coerceAtLeast(if (ratio > 0f) 0.08f else 0f))
                            .background(Brush.verticalGradient(listOf(color.lighten(0.25f), color)), CircleShape)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text("${mission.currentProgress.coerceAtMost(mission.targetValue)}/${mission.targetValue}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Navy.copy(alpha = 0.5f))
            }
        }
        Spacer(Modifier.width(10.dp))

        // Premio / cobrar
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when {
                isClaimed -> Text("COBRADO", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Sage, letterSpacing = 1.sp)
                ready -> ToyButton(
                    onClick = onClaim,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Sage),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(40.dp).breathing(0.05f, 700)
                ) { Text("COBRAR", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.sp) }
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CozyIcon(CozyKind.COIN, Modifier.size(16.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("$coins", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Navy)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text("+${mission.xpReward} XP", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Terracotta)
                }
            }
        }
    }
}
