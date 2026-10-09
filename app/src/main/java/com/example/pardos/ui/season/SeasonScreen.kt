package com.korkoor.pardos.ui.season

import com.korkoor.pardos.domain.shop.AdRewards
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.data.local.EconomyManager
import com.korkoor.pardos.data.local.LocalDay
import com.korkoor.pardos.data.local.RetentionManager
import com.korkoor.pardos.domain.collection.ChestType
import com.korkoor.pardos.domain.retention.SeasonCalendar
import com.korkoor.pardos.domain.retention.SeasonPass
import com.korkoor.pardos.domain.retention.SeasonReward
import com.korkoor.pardos.domain.shop.Avatars
import com.korkoor.pardos.domain.shop.Banners
import com.korkoor.pardos.ui.collection.chestColor
import com.korkoor.pardos.ui.design.*
import com.korkoor.pardos.ui.game.components.AmbientParticles
import com.korkoor.pardos.ui.profile.AvatarFramed
import com.korkoor.pardos.ui.profile.ProfileBanner
import com.korkoor.pardos.ui.shop.SkinTilePreview
import kotlinx.coroutines.delay

/** Las líneas de un premio: icono, texto y color. Sirve para pintar la vía gratis y la premium. */
internal data class RewardLine(val icon: ImageVector, val text: String, val color: Color, val avatarId: Int = 0)

internal fun rewardLines(r: SeasonReward): List<RewardLine> = buildList {
    if (r.coins > 0) add(RewardLine(Icons.Rounded.MonetizationOn, "${r.coins}", Gold))
    if (r.gems > 0) add(RewardLine(Icons.Rounded.Diamond, "${r.gems}", GemBlue))
    r.chest?.let {
        add(RewardLine(Icons.Rounded.Inventory2, when (it) { ChestType.COMMON -> "Cofre"; ChestType.RARE -> "Cofre raro"; ChestType.EPIC -> "Cofre épico" }, chestColor(it)))
    }
    if (r.freezes > 0) add(RewardLine(Icons.Rounded.Shield, "Escudo", Sage))
    if (r.undos > 0) add(RewardLine(Icons.AutoMirrored.Rounded.Undo, "x${r.undos}", Terracotta))
    r.skin?.let { add(RewardLine(Icons.Rounded.Palette, it.displayName, Violet)) }
    if (r.avatar != 0) add(RewardLine(Icons.Rounded.Palette, Avatars.byId(r.avatar).name, Terracotta, avatarId = r.avatar))
    if (r.banner != 0) add(RewardLine(Icons.Rounded.Palette, Banners.byId(r.banner).name, Violet))
    r.fx?.let { add(RewardLine(Icons.Rounded.AutoAwesome, "Efecto ${it.displayName}", Violet)) }
    if (r.tokens > 0) add(RewardLine(Icons.Rounded.SwapHoriz, "${r.tokens} ficha${if (r.tokens > 1) "s" else ""}", Violet))
}

/** Colores de la pantalla: salen de la skin exclusiva de la temporada, así cada mes se siente distinto. */
private class Pal(
    val top: Color, val bottom: Color, val ink: Color, val accent: Color,
    val dark: Boolean, val tile: Color, val tileEdge: Color
)

@Composable
fun SeasonScreen(
    onBack: () -> Unit,
    premiumPrice: String?,
    onBuyPremium: () -> Unit
) {
    val context = LocalContext.current
    val retention = remember { RetentionManager(context) }
    val economy = remember { EconomyManager(context) }
    val points by retention.seasonPoints.collectAsState()
    val premium by retention.seasonPremium.collectAsState()
    val claimed by retention.seasonClaimed.collectAsState()
    val gems by economy.gems.collectAsState()
    val vip by economy.isVip.collectAsState()
    val adFreq = remember { com.korkoor.pardos.data.local.AdFrequency(context) }
    var adTick by remember { mutableIntStateOf(0) }
    val boostLeft = remember(adTick) { AdRewards.left(adFreq.usedToday(com.korkoor.pardos.data.local.AdFrequency.SLOT_SEASON), AdRewards.SEASON_BOOST_PER_DAY) }
    var message by remember { mutableStateOf<String?>(null) }

    val today = remember { LocalDay.today() }
    val seasonId = remember { SeasonCalendar.seasonId(today) }
    val daysLeft = remember { SeasonCalendar.daysLeft(today) }
    val seasonSkin = remember { SeasonCalendar.skinFor(seasonId) }
    val st = seasonSkin.style
    val pal = remember(seasonSkin) {
        val top = Color(st.bgTop ?: 0xFFF3EFE6)
        val bottom = Color(st.bgBottom ?: 0xFFFFFBF5)
        val dark = (top.luminance() + bottom.luminance()) / 2f < 0.4f
        Pal(
            top, bottom, Color(st.ink ?: 0xFF3D405B), Color(st.accent ?: 0xFFE0A93B), dark,
            tile = if (dark) Color.White.copy(alpha = 0.09f) else Color.White.copy(alpha = 0.88f),
            tileEdge = if (dark) Color.White.copy(alpha = 0.16f) else Navy.copy(alpha = 0.08f)
        )
    }

    // Con un fondo oscuro, los iconos de las barras del sistema pasan a claros mientras esta pantalla está abierta
    com.korkoor.pardos.ui.design.DarkSystemBars(pal.dark)

    val tier = SeasonPass.tierFor(points)
    val inTier = SeasonPass.pointsInTier(points)
    val claimable = remember(points, premium, claimed) { retention.claimableTierCount() }

    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        // Abre el pase donde hay algo por cobrar (o en tu nivel actual)
        val target = (1..SeasonPass.TIERS).firstOrNull { retention.canClaimTier(it, false) || retention.canClaimTier(it, true) }
            ?: (tier + 1).coerceAtMost(SeasonPass.TIERS)
        if (target > 2) listState.scrollToItem(FIXED_ITEMS + target - 2)
    }
    LaunchedEffect(message) {
        if (message != null) { delay(3200); message = null }
    }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(pal.top, pal.bottom)))) {
        AmbientParticles(kind = st.particles, tint = Color(st.particleTint), modifier = Modifier.fillMaxSize(), density = 0.7f)

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            SeasonTopBar(pal, SeasonCalendar.name(seasonId), daysLeft, gems, onBack)

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 130.dp)
            ) {
                item(key = "hero") { SeasonHero(pal, tier, inTier, points, seasonId, premium) }
                item(key = "stars") {
                    Spacer(Modifier.height(16.dp))
                    StarRewards(pal, seasonId, tier, premium, claimed)
                }
                item(key = "cta") {
                    Spacer(Modifier.height(16.dp))
                    PremiumCard(pal, seasonId, seasonSkin.displayName, premium, premiumPrice, onBuyPremium)
                    if (tier < SeasonPass.TIERS && boostLeft > 0) {
                        Spacer(Modifier.height(12.dp))
                        com.korkoor.pardos.ui.design.WatchAdButton(
                            label = "IMPULSO DEL PASE",
                            sublabel = if (vip) "VIP: +${AdRewards.SEASON_BOOST_POINTS} puntos sin anuncio · te quedan $boostLeft hoy" else "+${AdRewards.SEASON_BOOST_POINTS} puntos viendo un anuncio · te quedan $boostLeft hoy",
                            tag = "+${AdRewards.SEASON_BOOST_POINTS}",
                            adFree = vip,
                            color = pal.accent,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val grant = {
                                    retention.addSeasonPoints(AdRewards.SEASON_BOOST_POINTS)
                                    adFreq.consume(com.korkoor.pardos.data.local.AdFrequency.SLOT_SEASON)
                                    adTick++
                                    com.korkoor.pardos.audio.GameAudio.play(com.korkoor.pardos.audio.Sfx.SEASON_TIER)
                                    message = "+${AdRewards.SEASON_BOOST_POINTS} puntos del pase"
                                }
                                if (vip) grant() else (context as? android.app.Activity)?.let { act -> com.korkoor.pardos.ui.game.logic.AdManager.showRewardedAd(act) { grant() } }
                            }
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("RECOMPENSAS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = pal.ink.copy(alpha = 0.6f), letterSpacing = 3.sp)
                    Spacer(Modifier.height(10.dp))
                    TrackHeader(pal)
                }
                itemsIndexed((1..SeasonPass.TIERS).toList(), key = { _, t -> "tier$t" }) { _, t ->
                    TierRow(
                        pal = pal, tier = t, currentTier = tier, inTier = inTier, premiumOwned = premium,
                        free = SeasonPass.freeReward(t, seasonId), premiumReward = SeasonPass.premiumReward(t, seasonId),
                        freeClaimed = "f$t" in claimed, premiumClaimed = "p$t" in claimed,
                        onClaimFree = { retention.claimTier(t, false)?.let { message = "¡Premio del nivel $t reclamado!" } },
                        onClaimPremium = {
                            if (premium) retention.claimTier(t, true)?.let { message = "¡Premio premium del nivel $t!" }
                            else message = "Activa el pase premium para cobrar esta vía"
                        }
                    )
                }
            }
        }

        // ---- Barra inferior fija: reclamar todo + saltar nivel ----
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, pal.bottom.copy(alpha = 0.92f), pal.bottom)))
                .navigationBarsPadding().padding(horizontal = 18.dp).padding(top = 26.dp, bottom = 14.dp)
        ) {
            AnimatedVisibility(
                visible = message != null,
                enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                Box(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(16.dp)).background(Navy).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(message ?: "", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                val shape = RoundedCornerShape(20.dp)
                Box(
                    modifier = Modifier.weight(1f).height(56.dp)
                        .then(if (claimable > 0) Modifier.shadow(10.dp, shape, spotColor = Gold) else Modifier)
                        .clip(shape)
                        .background(if (claimable > 0) Brush.horizontalGradient(listOf(Color(0xFFF2B84B), Color(0xFFE0842F))) else Brush.linearGradient(listOf(pal.ink.copy(alpha = 0.10f), pal.ink.copy(alpha = 0.10f))))
                        .clickable(enabled = claimable > 0) {
                            val got = retention.claimAllTiers()
                            val c = got.sumOf { it.coins }
                            val g = got.sumOf { it.gems }
                            message = buildString {
                                append(if (got.size == 1) "Reclamaste 1 premio" else "Reclamaste ${got.size} premios")
                                append(": +$c monedas")
                                if (g > 0) append(", +$g gemas")
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CardGiftcard, null, tint = if (claimable > 0) Color.White else pal.ink.copy(alpha = 0.4f), modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (claimable > 0) "RECLAMAR TODO ($claimable)" else "NADA POR RECLAMAR",
                            fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp,
                            color = if (claimable > 0) Color.White else pal.ink.copy(alpha = 0.45f)
                        )
                    }
                }
                val skipCost = SeasonPass.skipCostGems(points)
                val canSkip = tier < SeasonPass.TIERS && gems >= skipCost
                Column(
                    modifier = Modifier.height(56.dp).clip(shape)
                        .background(if (canSkip) GemBlue.copy(alpha = if (pal.dark) 0.30f else 0.18f) else pal.ink.copy(alpha = 0.08f))
                        .clickable(enabled = canSkip) { message = if (retention.buyTier()) "¡Subiste un nivel del pase!" else "No te alcanzan las gemas" }
                        .padding(horizontal = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    Text("SUBIR NIVEL", fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, color = pal.ink.copy(alpha = if (canSkip) 0.7f else 0.35f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$skipCost", fontSize = 15.sp, fontWeight = FontWeight.Black, color = pal.ink.copy(alpha = if (canSkip) 1f else 0.4f))
                        Spacer(Modifier.width(3.dp))
                        Icon(Icons.Rounded.Diamond, null, tint = GemBlue, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

private const val FIXED_ITEMS = 3

// ============================ Barra superior ============================

@Composable
private fun SeasonTopBar(pal: Pal, month: String, daysLeft: Int, gems: Int, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(onClick = onBack, shape = CircleShape, color = Color.White, shadowElevation = 6.dp, modifier = Modifier.size(46.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Atrás", tint = Navy) }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("PASE DE TEMPORADA", fontSize = 10.sp, fontWeight = FontWeight.Black, color = pal.ink.copy(alpha = 0.6f), letterSpacing = 3.sp)
            Text(month, fontSize = 22.sp, fontWeight = FontWeight.Black, color = pal.ink)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.clip(RoundedCornerShape(14.dp)).background(if (daysLeft <= 3) Terracotta else pal.ink.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Timer, null, tint = if (daysLeft <= 3) Color.White else pal.ink, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text("$daysLeft ${if (daysLeft == 1) "día" else "días"}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (daysLeft <= 3) Color.White else pal.ink)
            }
            Row(
                Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White).padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Diamond, null, tint = GemBlue, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text("$gems", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Navy)
            }
        }
    }
}

// ============================ Héroe: anillo de progreso ============================

@Composable
private fun SeasonHero(pal: Pal, tier: Int, inTier: Int, points: Int, seasonId: Int, premium: Boolean) {
    val progress = if (tier >= SeasonPass.TIERS) 1f else inTier / SeasonPass.POINTS_PER_TIER.toFloat()
    val animated by animateFloatAsState(progress, tween(900, easing = FastOutSlowInEasing), label = "ring")
    val shape = RoundedCornerShape(30.dp)
    Row(
        modifier = Modifier.fillMaxWidth().clip(shape)
            .background(if (pal.dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.78f))
            .border(1.dp, pal.tileEdge, shape)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(128.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 13.dp.toPx()
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(pal.ink.copy(alpha = 0.12f), -90f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                if (animated > 0.005f) {
                    drawArc(
                        Brush.sweepGradient(listOf(Color(0xFFFFE08A), Color(0xFFF2B84B), Color(0xFFE0842F), Color(0xFFFFE08A))),
                        -90f, 360f * animated, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("NIVEL", fontSize = 9.sp, fontWeight = FontWeight.Black, color = pal.ink.copy(alpha = 0.6f), letterSpacing = 2.sp)
                Text("$tier", fontSize = 40.sp, fontWeight = FontWeight.Black, color = pal.ink, lineHeight = 42.sp)
                Text("de ${SeasonPass.TIERS}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = pal.ink.copy(alpha = 0.55f))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (tier >= SeasonPass.TIERS) "¡Pase completado!" else "Siguiente nivel",
                fontSize = 15.sp, fontWeight = FontWeight.Black, color = pal.ink
            )
            Text(
                if (tier >= SeasonPass.TIERS) "Lo conseguiste todo este mes" else "$inTier / ${SeasonPass.POINTS_PER_TIER} puntos",
                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = pal.ink.copy(alpha = 0.65f)
            )
            Spacer(Modifier.height(10.dp))
            if (tier < SeasonPass.TIERS) {
                val next = tier + 1
                Text("PRÓXIMO PREMIO", fontSize = 9.sp, fontWeight = FontWeight.Black, color = pal.ink.copy(alpha = 0.5f), letterSpacing = 1.5.sp)
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MiniReward(SeasonPass.freeReward(next, seasonId), pal, Sage)
                    MiniReward(SeasonPass.premiumReward(next, seasonId), pal, Gold, locked = !premium)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("$points / ${SeasonPass.TOTAL_POINTS} pts en total", fontSize = 10.sp, color = pal.ink.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun MiniReward(r: SeasonReward, pal: Pal, accent: Color, locked: Boolean = false) {
    val lines = rewardLines(r).take(2)
    Row(
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = if (pal.dark) 0.22f else 0.16f)).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        lines.forEachIndexed { i, l ->
            if (i > 0) Spacer(Modifier.width(6.dp))
            Icon(l.icon, null, tint = l.color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(3.dp))
            Text(l.text, fontSize = 11.sp, fontWeight = FontWeight.Black, color = pal.ink, maxLines = 1)
        }
        if (locked) { Spacer(Modifier.width(5.dp)); Icon(Icons.Rounded.Lock, null, tint = pal.ink.copy(alpha = 0.5f), modifier = Modifier.size(11.dp)) }
    }
}

// ============================ Premios estrella ============================

private data class StarItem(val tier: Int, val premium: Boolean, val title: String, val reward: SeasonReward)

@Composable
private fun StarRewards(pal: Pal, seasonId: Int, tier: Int, premiumOwned: Boolean, claimed: Set<String>) {
    val items = remember(seasonId) {
        fun special(r: SeasonReward) = r.skin != null || r.avatar != 0 || r.banner != 0 || r.fx != null
        fun title(r: SeasonReward): String = r.skin?.displayName ?: r.avatar.takeIf { it != 0 }?.let { Avatars.byId(it).name }
            ?: r.banner.takeIf { it != 0 }?.let { Banners.byId(it).name } ?: r.fx?.displayName ?: "Cofre épico"
        val out = mutableListOf<StarItem>()
        for (t in 1..SeasonPass.TIERS) {
            val f = SeasonPass.freeReward(t, seasonId)
            val p = SeasonPass.premiumReward(t, seasonId)
            if (special(f)) out += StarItem(t, false, title(f), f)
            if (special(p) || p.chest == com.korkoor.pardos.domain.collection.ChestType.EPIC) out += StarItem(t, true, title(p), p)
        }
        out.sortedBy { it.tier }
    }
    Text("PREMIOS ESTRELLA", fontSize = 11.sp, fontWeight = FontWeight.Black, color = pal.ink.copy(alpha = 0.6f), letterSpacing = 3.sp)
    Spacer(Modifier.height(10.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 6.dp)) {
        items(items) { it ->
            val reached = tier >= it.tier
            val done = (if (it.premium) "p" else "f") + it.tier in claimed
            val locked = it.premium && !premiumOwned
            val accent = if (it.premium) Gold else Sage
            val shape = RoundedCornerShape(24.dp)
            Column(
                modifier = Modifier.width(136.dp).clip(shape)
                    .background(if (pal.dark) Color.White.copy(alpha = 0.09f) else Color.White.copy(alpha = 0.88f))
                    .border(if (reached && !done && !locked) 2.dp else 1.dp, if (reached && !done && !locked) accent else pal.tileEdge, shape)
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.fillMaxWidth().height(84.dp), contentAlignment = Alignment.Center) {
                    RewardHero(it.reward, reached, Modifier.alpha(if (done) 0.5f else 1f))
                    if (done) Icon(Icons.Rounded.CheckCircle, null, tint = accent, modifier = Modifier.align(Alignment.TopEnd).size(18.dp))
                    else if (locked) Icon(Icons.Rounded.Lock, null, tint = pal.ink.copy(alpha = 0.55f), modifier = Modifier.align(Alignment.TopEnd).size(15.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(it.title, fontSize = 12.sp, fontWeight = FontWeight.Black, color = pal.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(
                    "NV ${it.tier}${if (it.premium) " · PREMIUM" else ""}", fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp,
                    color = Color.White,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(accent).padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/** El dibujo grande de un premio: skin, avatar, banner o cofre (lo especial se ve, no solo se nombra). */
@Composable
private fun RewardHero(r: SeasonReward, reached: Boolean, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        val skin = r.skin
        val chest = r.chest
        when {
            skin != null -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(2, 16, 128).forEach { SkinTilePreview(skin, it, size = 34.dp) }
            }
            r.avatar != 0 -> AvatarFramed(r.avatar, Modifier.size(78.dp), ring = 4.dp)
            r.banner != 0 -> ProfileBanner(r.banner, Modifier.fillMaxWidth().height(70.dp).clip(RoundedCornerShape(16.dp)))
            chest != null -> TreasureChest(chest, if (reached) ChestState.READY else ChestState.LOCKED, Modifier.size(78.dp))
        }
    }
}

// ============================ Tarjeta premium ============================

@Composable
private fun PremiumCard(pal: Pal, seasonId: Int, skinName: String, premium: Boolean, price: String?, onBuy: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    if (premium) {
        Row(
            Modifier.fillMaxWidth().clip(shape).background(Brush.horizontalGradient(listOf(Sage, SageDark))).padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.WorkspacePremium, null, tint = Color.White, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("PREMIUM ACTIVO", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.5.sp)
                Text("Cobras las dos vías de premios este mes", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
            }
        }
        return
    }
    val shimmer by rememberInfiniteTransition(label = "shimmer").animateFloat(
        -0.4f, 1.4f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "sh"
    )
    val premiumRewards = (1..SeasonPass.TIERS).map { SeasonPass.premiumReward(it, seasonId) }
    val avatarIds = premiumRewards.map { it.avatar }.filter { it != 0 }
    val bannerIds = premiumRewards.map { it.banner }.filter { it != 0 }
    val banner = Banners.byId(bannerIds.first())
    Column(
        modifier = Modifier.fillMaxWidth().shadow(14.dp, shape, spotColor = Gold).clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFFF7C65A), Color(0xFFE88A3C), Color(0xFFD65A55))))
            .drawWithContent {
                drawContent()
                val x = size.width * shimmer
                drawRect(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.32f), Color.Transparent), Offset(x - 140f, 0f), Offset(x + 40f, size.height)))
            }
            .clickable(enabled = price != null, onClick = onBuy)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.WorkspacePremium, null, tint = Color.White, modifier = Modifier.size(34.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("PASE PREMIUM", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.5.sp)
                Text("Una sola compra por temporada", fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f))
            }
            Box(Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White).padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(price ?: "Pronto", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFFB8531F))
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            avatarIds.take(3).forEach { AvatarFramed(it, Modifier.size(40.dp), ring = 3.dp, animate = false) }
            ProfileBanner(banner, Modifier.width(62.dp).height(40.dp).clip(RoundedCornerShape(12.dp)), animate = false)
            Column(Modifier.weight(1f)) {
                Text("Skin $skinName", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text("${avatarIds.size} avatares · ${bannerIds.size} banners exclusivos", fontSize = 10.sp, color = Color.White.copy(alpha = 0.9f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("+ el doble de premios y cofres épicos", fontSize = 10.sp, color = Color.White.copy(alpha = 0.9f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ============================ Riel de niveles ============================

@Composable
private fun TrackHeader(pal: Pal) {
    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(Sage, CircleShape)); Spacer(Modifier.width(6.dp))
            Text("GRATIS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Sage, letterSpacing = 2.sp)
        }
        Spacer(Modifier.width(60.dp))
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.WorkspacePremium, null, tint = Gold, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(5.dp))
            Text("PREMIUM", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Gold, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun TierRow(
    pal: Pal, tier: Int, currentTier: Int, inTier: Int, premiumOwned: Boolean,
    free: SeasonReward, premiumReward: SeasonReward,
    freeClaimed: Boolean, premiumClaimed: Boolean,
    onClaimFree: () -> Unit, onClaimPremium: () -> Unit
) {
    val reached = tier <= currentTier
    val isNext = tier == currentTier + 1
    val frac = inTier / SeasonPass.POINTS_PER_TIER.toFloat()
    val railOn = Brush.verticalGradient(listOf(Color(0xFFF2B84B), Color(0xFFE0842F)))
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).drawBehind {
            val cx = size.width / 2f
            val w = 6.dp.toPx()
            val mid = size.height / 2f
            // mitad de arriba: iluminada si el nivel está alcanzado; la de abajo, si lo está el siguiente
            val off = pal.ink.copy(alpha = 0.14f)
            if (tier > 1) drawLine(if (reached) Color(0xFFF2B84B) else off, Offset(cx, 0f), Offset(cx, mid), w)
            if (tier < SeasonPass.TIERS) {
                drawLine(off, Offset(cx, mid), Offset(cx, size.height), w)
                val lit = when { tier + 1 <= currentTier -> 1f; tier == currentTier -> (frac * 2f).coerceIn(0f, 1f); else -> 0f }
                if (lit > 0f) drawLine(Color(0xFFF2B84B), Offset(cx, mid), Offset(cx, mid + (size.height - mid) * lit), w)
            }
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RewardTile(pal, free, reached, freeClaimed, locked = false, accent = Sage, modifier = Modifier.weight(1f).fillMaxHeight().padding(vertical = 5.dp), onClick = onClaimFree)
        TierNode(pal, tier, reached, isNext, frac, milestone = tier % 5 == 0, final = tier == SeasonPass.TIERS)
        RewardTile(pal, premiumReward, reached, premiumClaimed, locked = !premiumOwned, accent = Gold, modifier = Modifier.weight(1f).fillMaxHeight().padding(vertical = 5.dp), onClick = onClaimPremium)
    }
}

@Composable
private fun TierNode(pal: Pal, tier: Int, reached: Boolean, isNext: Boolean, frac: Float, milestone: Boolean, final: Boolean) {
    val size = if (milestone) 52.dp else 44.dp
    val pulse by rememberInfiniteTransition(label = "node").animateFloat(
        1f, if (isNext) 1.1f else 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "np"
    )
    Box(Modifier.width(60.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.size(size).scale(pulse)
                .then(if (reached) Modifier.shadow(8.dp, CircleShape, spotColor = Color(0xFFE0842F)) else Modifier)
                .clip(CircleShape)
                .background(
                    when {
                        reached -> Brush.verticalGradient(listOf(Color(0xFFFFD36E), Color(0xFFE0842F)))
                        else -> Brush.linearGradient(listOf(if (pal.dark) Color(0xFF2A2742) else Color.White, if (pal.dark) Color(0xFF2A2742) else Color.White))
                    }
                )
                .border(if (isNext) 3.dp else 2.dp, if (reached) Color.White else if (isNext) Color(0xFFF2B84B) else pal.ink.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isNext) {
                Canvas(Modifier.fillMaxSize().padding(2.dp)) {
                    drawArc(Color(0xFFF2B84B), -90f, 360f * frac, false, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                }
            }
            if (final) Icon(Icons.Rounded.EmojiEvents, null, tint = if (reached) Color.White else Gold, modifier = Modifier.size(24.dp))
            else Text("$tier", fontSize = if (milestone) 17.sp else 14.sp, fontWeight = FontWeight.Black, color = if (reached) Color.White else pal.ink.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun RewardTile(
    pal: Pal, reward: SeasonReward, reached: Boolean, claimed: Boolean, locked: Boolean, accent: Color,
    modifier: Modifier, onClick: () -> Unit
) {
    val canClaim = reached && !claimed && !locked
    val shape = RoundedCornerShape(20.dp)
    // rebote al cobrar
    val bounce = remember { androidx.compose.animation.core.Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(claimed) {
        if (first) { first = false } else if (claimed) {
            bounce.snapTo(0.88f); bounce.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium))
        }
    }
    val glow by rememberInfiniteTransition(label = "tile").animateFloat(
        0.4f, 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "tg"
    )
    val special = reward.skin != null || reward.avatar != 0 || reward.banner != 0 || reward.chest != null || reward.fx != null
    Box(
        modifier = modifier.scale(bounce.value).clip(shape)
            .background(
                when {
                    claimed -> accent.copy(alpha = 0.10f)
                    canClaim -> accent.copy(alpha = if (pal.dark) 0.22f else 0.16f)
                    else -> pal.tile
                }
            )
            .border(if (canClaim) 2.dp else 1.dp, if (canClaim) accent.copy(alpha = glow) else if (accent == Gold && !claimed) accent.copy(alpha = 0.35f) else pal.tileEdge, shape)
            .clickable(enabled = reached && !claimed, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.then(if (claimed) Modifier.alpha(0.5f) else Modifier)
        ) {
            if (special) RewardHero(reward, reached, Modifier.fillMaxWidth().heightIn(max = 78.dp))
            val lines = rewardLines(reward).filter { it.avatarId == 0 && it.icon != Icons.Rounded.Palette && !(reward.chest != null && it.icon == Icons.Rounded.Inventory2) }
            lines.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    pair.forEach { line ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(line.icon, null, tint = line.color, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(line.text, fontSize = 12.sp, fontWeight = FontWeight.Black, color = pal.ink, maxLines = 1)
                        }
                    }
                }
            }
            val named = reward.skin?.displayName ?: reward.avatar.takeIf { it != 0 }?.let { Avatars.byId(it).name } ?: reward.banner.takeIf { it != 0 }?.let { Banners.byId(it).name } ?: reward.fx?.displayName
            if (named != null) Text(named, fontSize = 10.sp, fontWeight = FontWeight.Black, color = pal.ink.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (reward.chest != null) Text(rewardLines(reward).first { it.icon == Icons.Rounded.Inventory2 }.text, fontSize = 10.sp, fontWeight = FontWeight.Black, color = pal.ink.copy(alpha = 0.75f), maxLines = 1)
            if (canClaim) {
                Text(
                    "RECLAMAR", fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, color = Color.White,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(accent).padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        if (claimed) {
            Icon(Icons.Rounded.CheckCircle, null, tint = accent, modifier = Modifier.align(Alignment.TopEnd).size(17.dp))
        } else if (locked) {
            Icon(Icons.Rounded.Lock, null, tint = pal.ink.copy(alpha = 0.5f), modifier = Modifier.align(Alignment.TopEnd).size(14.dp))
        }
    }
}
