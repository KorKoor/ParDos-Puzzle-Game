package com.korkoor.pardos.ui.menu

import com.korkoor.pardos.ui.design.JellyCard
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.Animatable
import com.korkoor.pardos.ui.design.CozyText

import com.korkoor.pardos.ui.design.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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


@Composable
fun CurrencyPill(icon: ImageVector, value: Int, color: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    // El número "corre" hasta su nuevo valor y la píldora da un saltito cuando sube
    val shown by animateIntAsState(value, tween(650, easing = FastOutSlowInEasing), label = "pillCount")
    val bump = remember { Animatable(1f) }
    var last by remember { mutableIntStateOf(value) }
    LaunchedEffect(value) {
        if (value > last) {
            bump.snapTo(1.16f)
            bump.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 380f))
        }
        last = value
    }
    JellyCard(
        modifier = modifier.graphicsLayer { scaleX = bump.value; scaleY = bump.value },
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        lipHeight = 4.dp,
        fill = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(5.dp))
            Text(text = "$shown", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Navy)
        }
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
    onDismiss: () -> Unit,
    /** Duplicar el regalo ya reclamado (null = no se ofrece). Con anuncio, o gratis con VIP. */
    onDouble: (() -> Unit)? = null
) {
    val today = DailyRewards.dayInCycle(streak)
    var claimed by remember { mutableStateOf(false) }
    var doubled by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val vip = remember { com.korkoor.pardos.data.local.EconomyManager(context) }.isVip.collectAsState().value

    Dialog(onDismissRequest = onDismiss) {
        JellyCard(
            modifier = Modifier.popIn(),
            shape = RoundedCornerShape(32.dp),
            fill = Color(0xFFFFFBF5),
            lipHeight = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(com.korkoor.pardos.R.drawable.ico_fire),
                    contentDescription = null,
                    modifier = Modifier.size(68.dp).breathing(0.09f, 900)
                )
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

                JellyCard(
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    onClick = {
                        if (!claimed) {
                            claimed = true
                            onClaim()
                        } else onDismiss()
                    },
                    shape = RoundedCornerShape(20.dp),
                    fill = if (claimed) Color(0xFFE9E4D8) else Sage,
                    lip = if (claimed) Color(0xFFCDB894) else Color(0xFF3F6B57),
                    lipHeight = 6.dp,
                    brush = if (claimed) null else Brush.verticalGradient(listOf(SageLight, Sage, SageDark))
                ) {
                    CozyText(
                        modifier = Modifier.align(Alignment.Center),
                        text = if (claimed) "¡LISTO!" else "RECLAMAR +${reward.coins}" + if (reward.gems > 0) "  +${reward.gems}◆" else "",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = if (claimed) Navy.copy(alpha = 0.6f) else Color.White,
                        letterSpacing = 1.5.sp
                    )
                }

                // Justo después de cobrar es cuando más apetece duplicarlo: un solo toque (o un anuncio corto)
                if (claimed && onDouble != null) {
                    Spacer(Modifier.height(12.dp))
                    if (!doubled) {
                        WatchAdButton(
                            label = "DUPLICAR EL REGALO",
                            sublabel = if (vip) "VIP: gratis, sin anuncio" else "+${reward.coins} ●" + (if (reward.gems > 0) " +${reward.gems} ◆" else "") + " viendo un anuncio corto",
                            tag = "x2", color = Violet, pulse = true, adFree = vip, minHeight = 52.dp,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val grant = { onDouble(); doubled = true }
                                if (vip) grant()
                                else (context as? android.app.Activity)?.let { act -> com.korkoor.pardos.ui.game.logic.AdManager.showRewardedAd(act) { grant() } }
                            }
                        )
                    } else {
                        CozyText(
                            text = "¡Duplicado! +${reward.coins} ●" + if (reward.gems > 0) " +${reward.gems} ◆" else "",
                            fontSize = 13.sp, fontWeight = FontWeight.Black, color = Violet,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(Violet.copy(alpha = 0.12f)).padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
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
    JellyColumn(
        modifier = modifier.scale(pulse).height(100.dp),
        shape = RoundedCornerShape(20.dp),
        fill = when {
            isToday -> accent.lighten(0.84f)
            isPast -> Color(0xFFEFE9DD)
            else -> Color.White
        },
        lip = if (isToday) accent.copy(alpha = 0.55f) else Color(0xFFCDB894).copy(alpha = 0.5f),
        lipHeight = 5.dp,
        borderColor = if (isToday) accent else null, borderWidth = 2.dp,
        padding = PaddingValues(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        fillHeight = true
    ) {
        Text("DÍA $day", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Navy.copy(alpha = 0.5f), letterSpacing = 1.sp)
        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
            if (isPast) {
                Box(Modifier.size(30.dp).background(Sage.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Sage, modifier = Modifier.size(20.dp))
                }
            } else {
                CozyIcon(if (reward.isChest) CozyKind.CHEST else CozyKind.COIN, Modifier.size(if (isToday) 36.dp else 30.dp).then(if (isToday) Modifier.breathing(0.08f, 800) else Modifier))
            }
        }
        CozyText(
            text = "${reward.coins}" + if (reward.gems > 0) " +${reward.gems}◆" else "",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = if (isPast) Navy.copy(alpha = 0.35f) else Navy
        )
    }
}
