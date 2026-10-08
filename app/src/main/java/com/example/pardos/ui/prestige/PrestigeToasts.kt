package com.korkoor.pardos.ui.prestige

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.data.local.PrestigeBus
import com.korkoor.pardos.data.local.PrestigeEvent
import com.korkoor.pardos.domain.prestige.Platinum
import com.korkoor.pardos.domain.prestige.TrophyTier
import com.korkoor.pardos.ui.design.Gold
import com.korkoor.pardos.ui.design.Navy
import com.korkoor.pardos.ui.design.PrimaryButton
import com.korkoor.pardos.ui.rewards.CardDialog
import kotlinx.coroutines.delay

/**
 * Celebraciones de prestigio, siempre encima de todo: un letrero breve para hitos y títulos, y una tarjeta grande al subir de
 * rango o conseguir el Platino. Si caen varias a la vez se enseñan una detrás de otra.
 */
@Composable
fun PrestigeToastHost() {
    val queue = remember { mutableStateListOf<PrestigeEvent>() }
    var current by remember { mutableStateOf<PrestigeEvent?>(null) }
    LaunchedEffect(Unit) { PrestigeBus.events.collect { queue.add(it) } }
    LaunchedEffect(queue.size, current) {
        if (current == null && queue.isNotEmpty()) current = queue.removeAt(0)
    }
    when (val ev = current) {
        is PrestigeEvent.RankUp -> RankUpDialog(ev) { current = null }
        PrestigeEvent.PlatinumEarned -> PlatinumDialog { current = null }
        is PrestigeEvent.MilestoneDone, is PrestigeEvent.TitleUnlocked, is PrestigeEvent.Backfill -> {
            LaunchedEffect(ev) { delay(3300); current = null }
            Box(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp), contentAlignment = Alignment.TopCenter) {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) { -it } + fadeIn(tween(150)),
                    exit = slideOutVertically(tween(250)) { -it } + fadeOut(tween(250))
                ) { Toast(ev) }
            }
        }
        null -> Unit
    }
}

@Composable
private fun Toast(ev: PrestigeEvent) {
    val (kicker, title, sub) = when (ev) {
        is PrestigeEvent.MilestoneDone -> Triple(
            "¡HITO LOGRADO!", ev.milestone.title,
            "+${ev.milestone.coins} monedas" + (if (ev.milestone.gems > 0) " · +${ev.milestone.gems} gemas" else "") + (if (ev.milestone.chest != null) " · cofre" else "")
        )
        is PrestigeEvent.TitleUnlocked -> Triple("¡TÍTULO NUEVO!", ev.title.name, "Úsalo en Prestigio → Títulos")
        is PrestigeEvent.Backfill -> Triple(
            "¡TU HISTORIAL CUENTA!", "${ev.count} hitos ya logrados",
            "+${ev.coins} monedas" + if (ev.gems > 0) " · +${ev.gems} gemas" else ""
        )
        else -> Triple("", "", "")
    }
    Row(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF241547), Color(0xFF3A2570))))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrophyMedal(TrophyTier.GOLD, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(kicker, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Gold, letterSpacing = 2.sp)
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1)
            Text(sub, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
        }
    }
}

@Composable
private fun RankUpDialog(ev: PrestigeEvent.RankUp, onDismiss: () -> Unit) {
    val r = ev.rank
    CardDialog(onDismiss) {
        Text("¡SUBES DE RANGO!", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Gold, letterSpacing = 3.sp)
        Spacer(Modifier.height(10.dp))
        RankBadge(r, 150.dp)
        Spacer(Modifier.height(8.dp))
        Text(r.title, fontSize = 32.sp, fontWeight = FontWeight.Black, color = Navy)
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Gold.copy(alpha = 0.14f)).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("TU PREMIO", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Navy.copy(alpha = 0.5f), letterSpacing = 2.sp)
            Text(
                "+${r.rewardCoins} monedas · +${r.rewardGems} gemas" + if (r.rewardChest != null) " · un cofre" else "",
                fontSize = 14.sp, fontWeight = FontWeight.Black, color = Navy, textAlign = TextAlign.Center
            )
        }
        r.next?.let {
            Text("Siguiente: ${it.title} a los ${it.minScore} puntos", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Navy.copy(alpha = 0.55f), modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(14.dp))
        PrimaryButton(text = "¡Genial!", onClick = onDismiss)
    }
}

@Composable
private fun PlatinumDialog(onDismiss: () -> Unit) {
    CardDialog(onDismiss) {
        Text("¡PLATINO!", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF6A4CE0), letterSpacing = 6.sp)
        Spacer(Modifier.height(8.dp))
        PlatinumTrophy(1f, 160.dp, earned = true)
        Text("Lo has conseguido todo", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Navy, textAlign = TextAlign.Center)
        Text("Todos los logros y todos los hitos de ParDos. Muy pocos llegan hasta aquí.", fontSize = 12.sp, color = Navy.copy(alpha = 0.6f), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(10.dp))
        Text(
            "+${Platinum.REWARD_COINS} monedas · +${Platinum.REWARD_GEMS} gemas · 2 cofres épicos · título PLATINO",
            fontSize = 13.sp, fontWeight = FontWeight.Black, color = Navy, textAlign = TextAlign.Center,
            modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFFBBCBF2).copy(alpha = 0.4f)).padding(12.dp)
        )
        Spacer(Modifier.height(14.dp))
        PrimaryButton(text = "¡Soy de platino!", onClick = onDismiss)
    }
}
