package com.korkoor.pardos.ui.menu

import com.korkoor.pardos.ui.design.CozyText

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import com.korkoor.pardos.ui.design.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.data.local.LocalDay
import com.korkoor.pardos.data.local.RetentionManager
import com.korkoor.pardos.data.local.WeeklyProgress
import com.korkoor.pardos.domain.retention.WeeklyMissions
import com.korkoor.pardos.domain.retention.WeeklyType
import com.korkoor.pardos.domain.social.WeekCalendar
import com.korkoor.pardos.ui.design.*

private fun weeklyIcon(t: WeeklyType): ImageVector = when (t) {
    WeeklyType.PLAY_GAMES -> Icons.Rounded.PlayCircle
    WeeklyType.WIN_LEVELS -> Icons.Rounded.EmojiEvents
    WeeklyType.DAILY_CHALLENGES -> Icons.Rounded.Star
    WeeklyType.MERGE_PAIRS -> Icons.Rounded.JoinInner
    WeeklyType.EARN_STARS -> Icons.Rounded.StarRate
    WeeklyType.OPEN_CHESTS -> Icons.Rounded.Inventory2
    WeeklyType.LOGIN_DAYS -> Icons.Rounded.EventAvailable
    WeeklyType.REACH_TILE -> Icons.Rounded.LooksOne
}

/** Metas de la semana: más largas que las diarias, con un cofre raro por completarlas todas. */
@Composable
fun WeeklyMissionsCard(retention: RetentionManager, modifier: Modifier = Modifier) {
    val tick by retention.tick.collectAsState()
    val items = remember(tick) { retention.weekly() }
    val bonusReady = remember(tick) { retention.canClaimWeeklyBonus() }
    val bonusClaimed = remember(tick) { retention.isWeeklyBonusClaimed() }
    val today = remember { LocalDay.today() }
    val daysLeft = remember { WeekCalendar.daysLeft(today) }
    var toast by remember { mutableStateOf<String?>(null) }

    JellySurface(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(32.dp),
        color = Color.White,
        shadowElevation = 12.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("MISIONES SEMANALES", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Navy, letterSpacing = 1.5.sp)
                    Text("Se renuevan en $daysLeft ${if (daysLeft == 1) "día" else "días"}", fontSize = 10.sp, color = InkSecondary)
                }
                Icon(Icons.Rounded.DateRange, null, tint = Violet)
            }
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items.forEach { w -> WeeklyRow(w) { toast = retention.claimWeekly(w.mission.id)?.let { "+${it.coins} monedas · +puntos de pase" } } }
            }
            Spacer(Modifier.height(14.dp))
            // Premio por completar todas
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(if (bonusReady) Violet.copy(alpha = 0.14f) else Navy.copy(alpha = 0.04f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconTile(Icons.Rounded.Inventory2, Violet, size = 40.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Completa las ${WeeklyMissions.PER_WEEK}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Navy)
                    Text("Cofre raro + ${WeeklyMissions.COMPLETION_GEMS} gemas", fontSize = 11.sp, color = InkSecondary)
                }
                when {
                    bonusClaimed -> Text("COBRADO", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Sage, letterSpacing = 1.sp)
                    bonusReady -> Box(
                        Modifier.clip(RoundedCornerShape(14.dp)).background(Violet)
                            .clickable { if (retention.claimWeeklyBonus()) toast = "¡Cofre raro y ${WeeklyMissions.COMPLETION_GEMS} gemas!" }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) { Text("RECLAMAR", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White) }
                    else -> Text("${items.count { it.claimed }}/${items.size}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = InkSecondary)
                }
            }
            toast?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Sage)
            }
        }
    }
}

@Composable
private fun WeeklyRow(w: WeeklyProgress, onClaim: () -> Unit) {
    val progress = (w.progress.toFloat() / w.mission.target).coerceIn(0f, 1f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(44.dp).background(if (w.claimed) Sage.copy(alpha = 0.12f) else Violet.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(if (w.claimed) Icons.Rounded.Check else weeklyIcon(w.mission.type), null, tint = if (w.claimed) Sage else Violet, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(w.mission.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Navy, maxLines = 2, lineHeight = 16.sp)
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(Navy.copy(alpha = 0.08f))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(progress.coerceAtLeast(0.02f)).background(if (w.done) Sage else Violet, CircleShape))
                }
                Spacer(Modifier.width(8.dp))
                Text("${w.progress.coerceAtMost(w.mission.target)}/${w.mission.target}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = InkSecondary)
            }
        }
        Spacer(Modifier.width(12.dp))
        when {
            w.claimed -> Text("COBRADO", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Sage, letterSpacing = 1.sp)
            w.done -> Box(
                Modifier.clip(RoundedCornerShape(14.dp)).background(Sage).clickable(onClick = onClaim).padding(horizontal = 12.dp, vertical = 8.dp)
            ) { Text("RECLAMAR", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White) }
            else -> CozyText("+${w.mission.coins} ●", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Gold)
        }
    }
}
