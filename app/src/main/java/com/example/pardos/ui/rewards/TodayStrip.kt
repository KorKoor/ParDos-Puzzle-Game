package com.korkoor.pardos.ui.rewards

import com.korkoor.pardos.ui.design.CozyText

import android.app.Activity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import com.korkoor.pardos.ui.design.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.korkoor.pardos.data.local.EconomyManager
import com.korkoor.pardos.data.local.RetentionManager
import com.korkoor.pardos.domain.collection.ChestType
import com.korkoor.pardos.domain.economy.Economy
import com.korkoor.pardos.domain.retention.ComebackGift
import com.korkoor.pardos.domain.retention.FreeChest
import com.korkoor.pardos.domain.retention.LevelReward
import com.korkoor.pardos.domain.retention.PiggyBank
import com.korkoor.pardos.domain.retention.SeasonPass
import com.korkoor.pardos.ui.collection.chestColor
import com.korkoor.pardos.ui.collection.chestName
import com.korkoor.pardos.ui.design.*
import com.korkoor.pardos.ui.game.logic.AdManager
import kotlinx.coroutines.delay

internal fun formatWait(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "${h}h ${m.toString().padStart(2, '0')}m" else "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}

/** Reloj que se actualiza cada segundo mientras [active]. */
@Composable
private fun rememberNowMs(active: Boolean): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(active) {
        while (active) {
            now = System.currentTimeMillis()
            delay(1000)
        }
        now = System.currentTimeMillis()
    }
    return now
}

/**
 * Franja "Tu día": cuatro atajos con lo que está esperando al jugador. Cada uno enseña su estado
 * (listo, tiempo que falta, nivel...) y un punto cuando hay algo por reclamar.
 */
@Composable
fun TodayStrip(
    retention: RetentionManager,
    onChest: () -> Unit,
    onWheel: () -> Unit,
    onSeason: () -> Unit,
    onPiggy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tick by retention.tick.collectAsState()
    val last by retention.freeChestLast.collectAsState()
    val points by retention.seasonPoints.collectAsState()
    val piggy by retention.piggy.collectAsState()
    val ready0 = remember(last, tick) { retention.isFreeChestReady() }
    val now = rememberNowMs(active = !ready0)
    val remaining = remember(now, last, tick) { retention.freeChestRemainingMs(now) }
    val chestReady = remaining == 0L
    val wheel = remember(tick) { retention.wheelAllowance() }
    val tier = SeasonPass.tierFor(points)
    val claimable = remember(tick, points) { retention.claimableTierCount() + retention.weeklyClaimableCount() }

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TodayTile(
            icon = Icons.Rounded.Inventory2, color = Terracotta, title = "COFRE",
            status = if (chestReady) "¡LISTO!" else formatWait(remaining), hot = chestReady,
            modifier = Modifier.weight(1f), onClick = onChest
        )
        TodayTile(
            icon = Icons.Rounded.Casino, color = Violet, title = "RULETA",
            status = if (wheel.freeLeft > 0) "GRATIS" else if (wheel.adLeft > 0) "+${wheel.adLeft} con ad" else "Mañana", hot = wheel.freeLeft > 0,
            modifier = Modifier.weight(1f), onClick = onWheel
        )
        TodayTile(
            icon = Icons.Rounded.WorkspacePremium, color = Gold, title = "PASE",
            status = "Nivel $tier", hot = claimable > 0, badge = claimable.takeIf { it > 0 },
            modifier = Modifier.weight(1f), onClick = onSeason
        )
        TodayTile(
            icon = Icons.Rounded.Savings, color = GemBlue, title = "HUCHA",
            status = "$piggy ◆", hot = false,
            modifier = Modifier.weight(1f), onClick = onPiggy
        )
    }
}

@Composable
private fun TodayTile(
    icon: ImageVector, color: Color, title: String, status: String, hot: Boolean,
    modifier: Modifier, onClick: () -> Unit, badge: Int? = null
) {
    val pulse by rememberInfiniteTransition(label = "tile").animateFloat(
        initialValue = 1f, targetValue = if (hot) 1.05f else 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "tilePulse"
    )
    val shape = RoundedCornerShape(20.dp)
    Box(modifier = modifier.scale(pulse)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(if (hot) color.copy(alpha = 0.14f) else Color.White)
                .border(if (hot) 2.dp else 1.dp, if (hot) color else Navy.copy(alpha = 0.07f), shape)
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconTile(icon, color, size = 38.dp)
            Spacer(Modifier.height(6.dp))
            Text(title, fontSize = 9.sp, fontWeight = FontWeight.Black, color = Navy.copy(alpha = 0.5f), letterSpacing = 1.5.sp)
            CozyText(status, fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (hot) color else Navy, maxLines = 1)
        }
        if (badge != null) {
            Box(
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp).size(20.dp).background(Terracotta, CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("$badge", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White) }
        } else if (hot) {
            Box(Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp).size(12.dp).background(Terracotta, CircleShape))
        }
    }
}

// ============================ Diálogos ============================

@Composable
internal fun CardDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.XLarge)).background(Color.White).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Composable
internal fun DialogButton(text: String, color: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(Radius.Medium))
            .background(if (enabled) color else Navy.copy(alpha = 0.08f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, color = if (enabled) Color.White else Navy.copy(alpha = 0.35f))
    }
}

/** Cofre gratis por tiempo: reclamar si toca, o saltar la espera con anuncio / gemas. */
@Composable
fun FreeChestDialog(retention: RetentionManager, onOpenAlbum: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val economy = remember { EconomyManager(context) }
    val gems by economy.gems.collectAsState()
    val isVip by economy.isVip.collectAsState()
    val tick by retention.tick.collectAsState()
    val last by retention.freeChestLast.collectAsState()
    var claimed by remember { mutableStateOf<ChestType?>(null) }
    val ready0 = remember(last, tick) { retention.isFreeChestReady() }
    val now = rememberNowMs(active = !ready0)
    val remaining = remember(now, last, tick) { retention.freeChestRemainingMs(now) }
    val ready = remaining == 0L
    val nextType = remember(tick) { retention.nextFreeChestType() }

    CardDialog(onDismiss) {
        val shown = claimed
        if (shown != null) {
            Box(Modifier.size(110.dp)) { com.korkoor.pardos.ui.design.TreasureChest(shown, com.korkoor.pardos.ui.design.ChestState.OPEN, Modifier.fillMaxSize()) }
            Spacer(Modifier.height(8.dp))
            Text("¡${chestName(shown)} para ti!", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Navy)
            Text("Está en tu inventario. Ábrelo en el Álbum.", fontSize = 12.sp, color = InkSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            DialogButton("Abrir en el Álbum", Sage) { onDismiss(); onOpenAlbum() }
            Spacer(Modifier.height(8.dp))
            Text("Cerrar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InkSecondary, modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp))
        } else if (ready) {
            Box(Modifier.size(110.dp)) { com.korkoor.pardos.ui.design.TreasureChest(nextType, com.korkoor.pardos.ui.design.ChestState.READY, Modifier.fillMaxSize()) }
            Spacer(Modifier.height(8.dp))
            Text("¡Tu cofre gratis está listo!", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Navy, textAlign = TextAlign.Center)
            Text("Hoy toca un ${chestName(nextType).lowercase()}", fontSize = 12.sp, color = InkSecondary)
            Spacer(Modifier.height(16.dp))
            DialogButton("Reclamar", Terracotta) { claimed = retention.claimFreeChest() }
        } else {
            IconTile(Icons.Rounded.Timer, Terracotta, size = 72.dp, shape = CircleShape)
            Spacer(Modifier.height(12.dp))
            Text("Siguiente cofre gratis en", fontSize = 13.sp, color = InkSecondary)
            Text(formatWait(remaining), fontSize = 34.sp, fontWeight = FontWeight.Black, color = Navy)
            Text("Cada 4 horas hay un cofre. ¡El quinto es raro!", fontSize = 11.sp, color = InkTertiary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            val adLeft = retention.adSkipsLeftToday()
            DialogButton(
                if (isVip) "Saltar la espera (VIP)" else "Saltar con anuncio ($adLeft)", Violet, enabled = adLeft > 0
            ) {
                if (isVip) retention.skipFreeChestWithAd()
                else activity?.let { act -> AdManager.showRewardedAd(act) { retention.skipFreeChestWithAd() } }
            }
            Spacer(Modifier.height(8.dp))
            val cost = FreeChest.skipCostGems(remaining)
            DialogButton("Saltar con $cost gemas", GemBlue, enabled = gems >= cost) { retention.skipFreeChestWithGems() }
        }
    }
}

/** Hucha: se llena jugando y se rompe con una compra opcional. */
@Composable
fun PiggyDialog(retention: RetentionManager, price: String?, onBuy: () -> Unit, onDismiss: () -> Unit) {
    val piggy by retention.piggy.collectAsState()
    CardDialog(onDismiss) {
        IconTile(Icons.Rounded.Savings, GemBlue, size = 72.dp, shape = CircleShape)
        Spacer(Modifier.height(12.dp))
        Text("Tu hucha", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Navy)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$piggy", fontSize = 38.sp, fontWeight = FontWeight.Black, color = Navy)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Rounded.Diamond, null, tint = GemBlue, modifier = Modifier.size(28.dp))
            Text(" / ${PiggyBank.CAP}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = InkSecondary)
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(Navy.copy(alpha = 0.08f))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth((piggy / PiggyBank.CAP.toFloat()).coerceIn(0.02f, 1f)).background(GemBlue, CircleShape))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Cada victoria deja gemas en la hucha (3 si es el reto diario). Rómpela cuando quieras y se te entregan todas.",
            fontSize = 12.sp, color = InkSecondary, textAlign = TextAlign.Center, lineHeight = 16.sp
        )
        Spacer(Modifier.height(16.dp))
        val can = PiggyBank.canBreak(piggy) && price != null
        DialogButton(
            when {
                piggy < PiggyBank.MIN_TO_BREAK -> "Llénala hasta ${PiggyBank.MIN_TO_BREAK}"
                price == null -> "Pronto"
                else -> "Romper por $price"
            }, GemBlue, enabled = can
        ) { onBuy() }
        Spacer(Modifier.height(6.dp))
        Text("Cerrar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InkSecondary, modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp))
    }
}

/** Premios por subir de nivel de jugador (puede ser más de uno). */
@Composable
fun LevelUpDialog(rewards: List<LevelReward>, onDismiss: () -> Unit) {
    if (rewards.isEmpty()) return
    val top = rewards.last().level
    CardDialog(onDismiss) {
        IconTile(Icons.Rounded.EmojiEvents, Gold, size = 72.dp, shape = CircleShape)
        Spacer(Modifier.height(12.dp))
        Text("¡Nivel $top!", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Navy)
        Text(if (rewards.size > 1) "Subiste ${rewards.size} niveles" else "Subiste de nivel", fontSize = 12.sp, color = InkSecondary)
        Spacer(Modifier.height(14.dp))
        val coins = rewards.sumOf { it.coins }
        val gems = rewards.sumOf { it.gems }
        val chests = rewards.mapNotNull { it.chest }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            RewardChip(Icons.Rounded.MonetizationOn, "+$coins", Gold)
            if (gems > 0) RewardChip(Icons.Rounded.Diamond, "+$gems", GemBlue)
            if (chests.isNotEmpty()) RewardChip(Icons.Rounded.Inventory2, "x${chests.size}", Terracotta)
        }
        Spacer(Modifier.height(18.dp))
        DialogButton("¡Genial!", Sage, onClick = onDismiss)
    }
}

/** Regalo por volver tras unos días sin jugar. */
@Composable
fun ComebackDialog(gift: ComebackGift, daysAway: Int, onDismiss: () -> Unit) {
    CardDialog(onDismiss) {
        IconTile(Icons.Rounded.CardGiftcard, Terracotta, size = 72.dp, shape = CircleShape)
        Spacer(Modifier.height(12.dp))
        Text("¡Te extrañábamos!", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Navy)
        Text("Pasaron $daysAway días. Esto es para ti:", fontSize = 12.sp, color = InkSecondary)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            RewardChip(Icons.Rounded.MonetizationOn, "+${gift.coins}", Gold)
            RewardChip(Icons.Rounded.Inventory2, chestName(gift.chest), chestColor(gift.chest))
            if (gift.freezes > 0) RewardChip(Icons.Rounded.Shield, "Escudo", Sage)
        }
        Spacer(Modifier.height(18.dp))
        DialogButton("Gracias", Sage, onClick = onDismiss)
    }
}

@Composable
internal fun RewardChip(icon: ImageVector, text: String, color: Color) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.14f)).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Navy, maxLines = 1, softWrap = false)
    }
}
