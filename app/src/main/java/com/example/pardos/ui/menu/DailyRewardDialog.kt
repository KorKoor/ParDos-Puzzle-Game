package com.korkoor.pardos.ui.menu

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.korkoor.pardos.domain.rewards.DailyRewards
import com.korkoor.pardos.domain.rewards.Reward

private val Sage = Color(0xFF6B9E86)
private val Terracotta = Color(0xFFE07A5F)
private val Navy = Color(0xFF3D405B)
private val Gold = Color(0xFFE0A93B)
private val GemBlue = Color(0xFF4E8FA6)

@Composable
fun CurrencyPill(icon: ImageVector, value: Int, color: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.5.dp, color.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(5.dp))
        Text(text = "$value", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Navy)
    }
}

/** Chip que invita a reclamar el regalo del día; late suavemente. */
@Composable
fun ClaimGiftChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "gift").animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "giftPulse"
    )
    Row(
        modifier = modifier
            .scale(pulse)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(Terracotta, Color(0xFFE8956B))))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.CardGiftcard, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("REGALO", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.sp)
    }
}

/**
 * Calendario de 7 días. [streak] es la racha actual; el día del ciclo de hoy se resalta,
 * los anteriores aparecen completados.
 */
@Composable
fun DailyRewardDialog(
    streak: Int,
    reward: Reward,
    onClaim: () -> Unit,
    onDismiss: () -> Unit
) {
    val today = DailyRewards.dayInCycle(streak)
    var claimed by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = Color(0xFFFFFBF5),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = Terracotta, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (streak == 1) "¡Primer día!" else "¡Racha de $streak días!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Navy
                )
                Text(
                    text = "Vuelve cada día y las recompensas crecen",
                    fontSize = 12.sp,
                    color = Navy.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                )

                // Días 1..4 y 5..7
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..4).forEach { day -> DayTile(day, today, claimed, Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (5..7).forEach { day -> DayTile(day, today, claimed, Modifier.weight(1f), wide = day == 7) }
                }

                Spacer(Modifier.height(22.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (claimed) Brush.linearGradient(listOf(Navy.copy(alpha = 0.15f), Navy.copy(alpha = 0.15f)))
                            else Brush.linearGradient(listOf(Color(0xFF7FB69C), Color(0xFF5A8C74)))
                        )
                        .clickable {
                            if (!claimed) {
                                claimed = true
                                onClaim()
                            } else onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (claimed) "¡LISTO!" else "RECLAMAR +${reward.coins}" + if (reward.gems > 0) "  +${reward.gems}◆" else "",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = if (claimed) Navy.copy(alpha = 0.6f) else Color.White,
                        letterSpacing = 1.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DayTile(day: Int, today: Int, claimed: Boolean, modifier: Modifier, wide: Boolean = false) {
    val reward = DailyRewards.cycle[day - 1]
    val isToday = day == today
    val isPast = day < today || (isToday && claimed)

    val pulse by rememberInfiniteTransition(label = "tile$day").animateFloat(
        initialValue = 1f,
        targetValue = if (isToday && !claimed) 1.05f else 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "tilePulse$day"
    )

    val accent = when {
        reward.isChest -> Gold
        isToday -> Sage
        else -> Navy
    }
    Column(
        modifier = modifier
            .scale(pulse)
            .height(92.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                when {
                    isToday -> accent.copy(alpha = 0.16f)
                    isPast -> Navy.copy(alpha = 0.05f)
                    else -> Color.White
                }
            )
            .border(
                width = if (isToday) 2.dp else 1.dp,
                color = if (isToday) accent else Navy.copy(alpha = 0.08f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text("DÍA $day", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Navy.copy(alpha = 0.5f), letterSpacing = 1.sp)
        Box(
            modifier = Modifier.size(34.dp).background((if (isPast) Sage else Gold).copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    isPast -> Icons.Rounded.Check
                    reward.isChest -> Icons.Rounded.Inventory2
                    else -> Icons.Rounded.MonetizationOn
                },
                contentDescription = null,
                tint = if (isPast) Sage else if (reward.isChest) Gold else Gold,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = "${reward.coins}" + if (reward.gems > 0) " +${reward.gems}◆" else "",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = if (isPast) Navy.copy(alpha = 0.35f) else Navy
        )
    }
}
