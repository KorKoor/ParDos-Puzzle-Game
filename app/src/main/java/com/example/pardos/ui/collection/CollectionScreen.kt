package com.korkoor.pardos.ui.collection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.korkoor.pardos.data.local.CollectionManager
import com.korkoor.pardos.data.local.EconomyManager
import com.korkoor.pardos.data.local.ProfileManager
import com.korkoor.pardos.data.local.TradeInbox
import com.korkoor.pardos.data.local.TradeManager
import com.korkoor.pardos.data.local.TradeOutgoing
import com.korkoor.pardos.domain.collection.*
import com.korkoor.pardos.domain.model.UserProfile
import com.korkoor.pardos.ui.design.*
import com.korkoor.pardos.ui.profile.AvatarFramed

fun rarityColor(r: Rarity): Color = when (r) {
    Rarity.COMMON -> Color(0xFF8FA39A)
    Rarity.RARE -> GemBlue
    Rarity.EPIC -> Violet
    Rarity.LEGENDARY -> Gold
}

fun rarityName(r: Rarity): String = when (r) {
    Rarity.COMMON -> "Común"; Rarity.RARE -> "Rara"; Rarity.EPIC -> "Épica"; Rarity.LEGENDARY -> "Legendaria"
}

fun chestName(t: ChestType): String = when (t) {
    ChestType.COMMON -> "Cofre común"; ChestType.RARE -> "Cofre raro"; ChestType.EPIC -> "Cofre épico"
}

fun chestColor(t: ChestType): Color = when (t) {
    ChestType.COMMON -> Color(0xFF8FA39A); ChestType.RARE -> GemBlue; ChestType.EPIC -> Violet
}

/** Décimas de punto a texto: 12 -> "1,2"; 30 -> "3". */
private fun tenths(t: Int): String = if (t % 10 == 0) "${t / 10}" else "${t / 10},${t % 10}"

private val PurpleDeep = Color(0xFF241547)
private val PurpleMid = Color(0xFF3A2570)

/** Lo que se está enseñando al abrir un cofre o un sobre. */
private data class Opening(val title: String, val drops: List<Drop>, val chest: ChestType? = null)

@Composable
fun CollectionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { CollectionManager(context) }
    val economy = remember { EconomyManager(context) }
    val trades = remember { TradeManager(context) }
    val owned by manager.owned.collectAsState()
    val copies by manager.copies.collectAsState()
    val shards by manager.shards.collectAsState()
    val tokens by manager.tokens.collectAsState()
    val chests by manager.chests.collectAsState()
    val claimed by manager.claimedSeries.collectAsState()
    val foil by manager.foil.collectAsState()
    val showcase by manager.showcase.collectAsState()
    val showcaseSlots by manager.showcaseSlots.collectAsState()
    val coinsNow by economy.coins.collectAsState()
    val gemsNow by economy.gems.collectAsState()
    val haptic = rememberGameHaptics()

    var opening by remember { mutableStateOf<Opening?>(null) }
    var detail by remember { mutableStateOf<Collectible?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var incoming by remember { mutableStateOf<List<TradeInbox>>(emptyList()) }
    var outgoing by remember { mutableStateOf(trades.outgoing()) }
    var friends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }

    LaunchedEffect(Unit) {
        trades.syncOutgoing { notes ->
            outgoing = trades.outgoing()
            if (notes.isNotEmpty()) toast = notes.joinToString("\n")
        }
        trades.fetchIncoming { incoming = it }
        ProfileManager(context).getFriendsProfiles { friends = it }
    }

    val total = CollectibleCatalog.all.size
    val have = owned.size
    val spareTotal = Copies.totalSpares(copies)
    val seriesDone = Series.entries.count { CollectibleCatalog.isSeriesComplete(it, owned) }

    fun openPack(series: Series, withGems: Boolean) {
        if (withGems && gemsNow < SeriesPackRules.GEMS) { toast = "Te faltan ${SeriesPackRules.GEMS - gemsNow} gemas."; return }
        if (!withGems && coinsNow < SeriesPackRules.COINS) { toast = "Te faltan ${SeriesPackRules.COINS - coinsNow} monedas."; return }
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        manager.openSeriesPack(series, withGems)?.let { opening = Opening("Sobre · ${series.nameEs}", it) } ?: run { toast = "Esa serie ya está completa." }
    }

    Box(modifier = Modifier.fillMaxSize().pardosBackdrop()) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            PardosTopBar(eyebrow = "Álbum", title = "Colección", onBack = onBack) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    ResourceChip(Icons.Rounded.AutoFixHigh, "$shards", Violet)
                    ResourceChip(Icons.Rounded.SwapHoriz, "$tokens", Color(0xFF6A4CE0))
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { AlbumHero(have, total, seriesDone, owned, foil, manager.isAlbumClaimable, CollectibleCatalog.isAlbumComplete(owned)) {
                    manager.claimAlbum(); toast = "¡Skin Oro Real y 50 gemas!"
                } }

                // --- Pistas: lo que te falta para cerrar series ---
                val hints = AlbumHints.closest(owned, 3)
                if (hints.isNotEmpty()) {
                    item {
                        HintsCard(
                            hints, essence = shards, coins = coinsNow, gems = gemsNow,
                            onPiece = { detail = it },
                            onPack = { s, g -> openPack(s, g) },
                            onTrade = { tab = 2 }
                        )
                    }
                }

                // --- Cofres sin abrir ---
                item {
                    SectionLabel("Tus cofres")
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ChestType.entries.forEach { t ->
                            val n = chests[t] ?: 0
                            ChestSlot(t, n, Modifier.weight(1f)) {
                                if (n > 0) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    manager.openChest(t)?.let { opening = Opening(chestName(t), it.drops, t) }
                                }
                            }
                        }
                    }
                    Text(
                        "Gánalos con logros, rachas, capítulos o cómpralos en la tienda. Las cartas repetidas se guardan: véndelas, recíclalas o cámbialas con amigos.",
                        fontSize = 11.sp, color = InkSecondary, lineHeight = 15.sp, modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // --- Esencia ---
                item {
                    val packsToday = manager.shardPacksToday()
                    val canBuy = ShardShop.canBuy(gemsNow, packsToday)
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.Medium)).background(Violet.copy(alpha = 0.10f)).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, tint = Violet, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Esencia: $shards", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Navy)
                            Text("Crea cartas que te faltan o hazlas Brillantes. Hoy: $packsToday/${ShardShop.MAX_PACKS_PER_DAY} packs", fontSize = 11.sp, color = InkSecondary, lineHeight = 14.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(14.dp))
                                .background(if (canBuy) Violet else Navy.copy(alpha = 0.08f))
                                .clickable(enabled = canBuy) {
                                    toast = if (manager.buyShardPack()) "+${ShardShop.SHARDS_PER_PACK} de esencia" else "No se pudo comprar"
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("+${ShardShop.SHARDS_PER_PACK}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (canBuy) Color.White else Navy.copy(alpha = 0.35f))
                            Spacer(Modifier.width(6.dp))
                            CozyText("${ShardShop.GEMS_PER_PACK} ◆", fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (canBuy) Color.White else Navy.copy(alpha = 0.35f))
                        }
                    }
                }

                toast?.let { msg ->
                    item {
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.Medium)).background(Sage.copy(alpha = 0.16f))
                                .clickable { toast = null }.padding(14.dp)
                        ) { Text(msg, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Navy) }
                    }
                }

                // --- Pestañas ---
                item {
                    Tabs(
                        listOf("ÁLBUM" to null, "REPETIDAS" to spareTotal.takeIf { it > 0 }, "INTERCAMBIO" to incoming.size.takeIf { it > 0 }),
                        selected = tab, onSelect = { if (it != tab) com.korkoor.pardos.audio.GameAudio.play(com.korkoor.pardos.audio.Sfx.UI_TAB); tab = it }
                    )
                }

                when (tab) {
                    0 -> {
                        item {
                            val labels = listOf("TODAS", "CASI LISTAS", "INCOMPLETAS", "COMPLETAS")
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                labels.forEachIndexed { i, l -> FilterChip(l, i == filter) { filter = i } }
                            }
                        }
                        val list = Series.entries.filter { s ->
                            val (h, t) = CollectibleCatalog.progress(s, owned)
                            when (filter) {
                                1 -> t - h in 1..3
                                2 -> h < t
                                3 -> h == t
                                else -> true
                            }
                        }.let { l -> if (filter == 1) l.sortedBy { CollectibleCatalog.progress(it, owned).let { (h, t) -> t - h } } else l }
                        if (list.isEmpty()) item { Text("Nada por aquí todavía.", fontSize = 13.sp, color = InkSecondary, modifier = Modifier.padding(8.dp)) }
                        items(list, key = { it.id }) { series ->
                            SeriesBlock(
                                series = series, owned = owned, copies = copies, foil = foil,
                                claimed = series.id in claimed, claimable = manager.isSeriesClaimable(series),
                                onClaim = { manager.claimSeries(series)?.let { (c, g) -> toast = "¡Serie ${series.nameEs} completa! +$c monedas y +$g gemas" } },
                                onPick = { detail = it },
                                onPack = { openPack(series, false) }
                            )
                        }
                    }
                    1 -> item {
                        SparesTab(
                            copies = copies,
                            sellTenths = PerkRules.tenths(PerkKind.SELL, owned, foil),
                            onPick = { detail = it },
                            onSellAll = { r ->
                                val (n, c) = manager.sellAllUpTo(r)
                                toast = if (n > 0) "Vendiste $n cartas por $c monedas." else "No tienes repetidas de esa rareza."
                            }
                        )
                    }
                    else -> item {
                        TradeTab(
                            manager = manager, trades = trades, tokens = tokens, copies = copies, owned = owned, gemsNow = gemsNow,
                            friends = friends, incoming = incoming, outgoing = outgoing,
                            onToast = { toast = it },
                            onChanged = { outgoing = trades.outgoing() },
                            onIncoming = { incoming = it }
                        )
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }

        // --- Apertura de cofre / sobre ---
        opening?.let { o -> OpeningDialog(o, manager) { opening = null } }

        // --- Detalle de una carta ---
        detail?.let { c ->
            CollectibleDialog(
                c = c, owned = c.id in owned, foil = c.id in foil, copies = Copies.count(copies, c.id), shards = shards,
                exhibited = c.id in showcase, showcaseFull = showcase.size >= showcaseSlots && c.id !in showcase,
                sellTenths = PerkRules.tenths(PerkKind.SELL, owned, foil),
                onCraft = { if (manager.craft(c)) toast = "¡Creaste ${c.nameEs}!"; detail = null },
                onFoil = { if (manager.upgradeFoil(c)) toast = "¡${c.nameEs} ahora es Brillante!"; detail = null },
                onSell = { manager.sell(c.id)?.let { toast = "Vendiste ${c.nameEs} por $it monedas." } },
                onRecycle = { manager.recycle(c.id)?.let { toast = "Reciclaste ${c.nameEs}: +$it de esencia." } },
                onTrade = { detail = null; tab = 2 },
                onShowcase = {
                    manager.setShowcase(if (c.id in showcase) showcase - c.id else showcase + c.id)
                },
                onDismiss = { detail = null }
            )
        }
    }
}

// ============================== Piezas pequeñas ==============================

@Composable
private fun ResourceChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Navy)
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp, maxLines = 1, softWrap = false,
        color = if (selected) Color.White else Navy,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (selected) Navy else Color.White)
            .border(1.dp, if (selected) Navy else Navy.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
private fun Tabs(items: List<Pair<String, Int?>>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Navy.copy(alpha = 0.06f)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEachIndexed { i, (label, badge) ->
            val sel = i == selected
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (sel) Color.White else Color.Transparent)
                    .clickable { onSelect(i) }.padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp, color = if (sel) Navy else Navy.copy(alpha = 0.5f), maxLines = 1)
                if (badge != null) {
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "$badge", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White,
                        modifier = Modifier.clip(CircleShape).background(Color(0xFF6A4CE0)).padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressBar(progress: Float, color: Color) {
    val anim by animateFloatAsState(progress.coerceIn(0f, 1f), tween(700), label = "p")
    Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Navy.copy(alpha = 0.08f))) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(anim.coerceAtLeast(0.02f)).background(color, CircleShape))
    }
}

// ============================== Cabecera ==============================

@Composable
private fun AlbumHero(have: Int, total: Int, seriesDone: Int, owned: Set<String>, foil: Set<String>, claimable: Boolean, complete: Boolean, onClaim: () -> Unit) {
    val bonus = AlbumBonus.breakdown(owned, foil)
    val perks = PerkRules.totals(owned, foil)
    val progress by animateFloatAsState(have.toFloat() / total, tween(900), label = "ring")
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(listOf(PurpleDeep, PurpleMid, Color(0xFF1E2A5A)))).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val sw = size.width * 0.11f
                    drawArc(Color.White.copy(alpha = 0.12f), -90f, 360f, false, Offset(sw / 2, sw / 2), Size(size.width - sw, size.height - sw), style = Stroke(sw))
                    drawArc(
                        Brush.sweepGradient(listOf(Color(0xFFFFD36E), Color(0xFFFF8CC0), Color(0xFF9BE3FF), Color(0xFFFFD36E))),
                        -90f, 360f * progress, false, Offset(sw / 2, sw / 2), Size(size.width - sw, size.height - sw), style = Stroke(sw, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$have", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text("de $total", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.65f))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("TU COLECCIÓN", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFD36E), letterSpacing = 2.sp)
                Text("$seriesDone / ${Series.entries.size} series", fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Rounded.MonetizationOn, null, tint = Color(0xFFFFD36E), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("+${bonus.total}% monedas", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
                Text(
                    "piezas +${bonus.pieces} · series +${bonus.series} · brillantes +${bonus.foil} · mejoras +${bonus.perks}" + if (bonus.album > 0) " · álbum +${bonus.album}" else "",
                    fontSize = 9.5.sp, color = Color.White.copy(alpha = 0.7f), lineHeight = 12.sp, modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        // mejoras activas de las piezas de alta rareza
        Spacer(Modifier.height(12.dp))
        Text("MEJORAS DE TUS CARTAS ÉPICAS Y LEGENDARIAS", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.6f), letterSpacing = 1.5.sp)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PerkKind.entries.forEach { k ->
                val t = perks[k] ?: 0
                val label = when (k) {
                    PerkKind.COINS -> "Monedas"; PerkKind.XP -> "XP"; PerkKind.SELL -> "Venta"; PerkKind.LUCK -> "Suerte"; PerkKind.TOKENS -> "Fichas"
                }
                Text(
                    "$label +${tenths(t)}%", fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false,
                    color = if (t > 0) Color.White else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = if (t > 0) 0.16f else 0.06f)).padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
        if (complete) {
            Spacer(Modifier.height(12.dp))
            if (claimable) PrimaryButton("RECLAMAR ÁLBUM COMPLETO", onClick = onClaim, height = 48.dp, fontSize = 12.sp)
            else Text("¡Álbum completo! Skin Oro Real desbloqueada", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8EE3B0))
        } else {
            Text("Completa las ${Series.entries.size} series para ganar la skin Oro Real y +${AlbumBonus.FULL_ALBUM}% de monedas.", fontSize = 10.sp, color = Color.White.copy(alpha = 0.65f), modifier = Modifier.padding(top = 10.dp))
        }
    }
}

// ============================== Pistas ==============================

@Composable
private fun HintsCard(
    hints: List<AlbumHint>, essence: Int, coins: Int, gems: Int,
    onPiece: (Collectible) -> Unit, onPack: (Series, Boolean) -> Unit, onTrade: () -> Unit
) {
    val first = hints.first()
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White).border(2.dp, Gold.copy(alpha = 0.6f), RoundedCornerShape(24.dp)).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(first.series.glyph, fontSize = 26.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (first.missing.size == 1) "¡Te falta 1 carta para cerrar ${first.series.nameEs}!" else "Te faltan ${first.missing.size} cartas para cerrar ${first.series.nameEs}",
                    fontSize = 14.sp, fontWeight = FontWeight.Black, color = Navy, lineHeight = 17.sp
                )
                Text("Premio al completarla: ${first.series.rewardCoins} monedas y ${first.series.rewardGems} gemas", fontSize = 10.5.sp, color = InkSecondary)
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(first.missing, key = { it.id }) { c ->
                Column(Modifier.width(74.dp).clickable { onPiece(c) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    CollectibleCard(c, Modifier.fillMaxWidth(), owned = false, showName = false, animate = false)
                    Text(rarityName(c.rarity), fontSize = 9.sp, fontWeight = FontWeight.Black, color = rarityColor(c.rarity), modifier = Modifier.padding(top = 3.dp))
                    Text(if (essence >= c.rarity.craftCost) "Crear ya" else "${c.rarity.craftCost} esencia", fontSize = 8.5.sp, color = if (essence >= c.rarity.craftCost) Sage else InkSecondary, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionPill("SOBRE · ${SeriesPackRules.COINS}", Icons.Rounded.MonetizationOn, Gold, coins >= SeriesPackRules.COINS, Modifier.weight(1f)) { onPack(first.series, false) }
            ActionPill("SOBRE · ${SeriesPackRules.GEMS}", Icons.Rounded.Diamond, GemBlue, gems >= SeriesPackRules.GEMS, Modifier.weight(1f)) { onPack(first.series, true) }
            ActionPill("CAMBIAR", Icons.Rounded.SwapHoriz, Color(0xFF6A4CE0), true, Modifier.weight(1.1f), onTrade)
        }
        Text("El sobre trae 3 cartas de esa serie y siempre incluye una nueva.", fontSize = 9.5.sp, color = InkSecondary, modifier = Modifier.padding(top = 6.dp))
        if (hints.size > 1) {
            Spacer(Modifier.height(8.dp))
            Text(
                "También cerca: " + hints.drop(1).joinToString(" · ") { "${it.series.nameEs} (${it.missing.size})" },
                fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = InkSecondary
            )
        }
    }
}

@Composable
private fun ActionPill(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(14.dp)).background(if (enabled) color.copy(alpha = 0.16f) else Navy.copy(alpha = 0.05f))
            .border(1.dp, if (enabled) color else Navy.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (enabled) color else Navy.copy(alpha = 0.3f), modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(3.dp))
        Text(text, fontSize = 9.sp, fontWeight = FontWeight.Black, color = if (enabled) Navy else Navy.copy(alpha = 0.4f), maxLines = 1, softWrap = false)
    }
}

// ============================== Cofres ==============================

@Composable
private fun ChestSlot(type: ChestType, count: Int, modifier: Modifier, onClick: () -> Unit) {
    val color = chestColor(type)
    val bounce by rememberInfiniteTransition(label = "cs").animateFloat(
        0f, if (count > 0) -4f else 0f,
        infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b"
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.Large))
            .background(Color.White)
            .border(if (count > 0) 2.dp else 1.dp, if (count > 0) color else Navy.copy(alpha = 0.08f), RoundedCornerShape(Radius.Large))
            .clickable(enabled = count > 0, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.offset(y = bounce.dp).size(64.dp), contentAlignment = Alignment.Center) {
            TreasureChest(
                type = type, state = if (count > 0) ChestState.READY else ChestState.LOCKED,
                modifier = Modifier.fillMaxSize(), animate = count > 0
            )
        }
        Spacer(Modifier.height(6.dp))
        Text("x$count", fontSize = 16.sp, fontWeight = FontWeight.Black, color = if (count > 0) Navy else Navy.copy(alpha = 0.3f))
        Text(
            when (type) { ChestType.COMMON -> "COMÚN"; ChestType.RARE -> "RARO"; ChestType.EPIC -> "ÉPICO" },
            fontSize = 9.sp, fontWeight = FontWeight.Black, color = color, letterSpacing = 1.5.sp
        )
    }
}

// ============================== Series ==============================

@Composable
private fun SeriesBlock(
    series: Series, owned: Set<String>, copies: Map<String, Int>, foil: Set<String>, claimed: Boolean, claimable: Boolean,
    onClaim: () -> Unit, onPick: (Collectible) -> Unit, onPack: () -> Unit
) {
    val (have, total) = CollectibleCatalog.progress(series, owned)
    val done = have == total
    val accent = Color(series.accent)
    PardosCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(Radius.Large), elevation = 5.dp) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(46.dp).clip(CircleShape).background(Brush.verticalGradient(listOf(Color(series.top), Color(series.bottom)))).border(2.dp, accent.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text(series.glyph, fontSize = 22.sp) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(series.nameEs, fontSize = 15.sp, fontWeight = FontWeight.Black, color = Navy)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$have/$total", fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (done) Sage else InkSecondary)
                        Spacer(Modifier.width(6.dp))
                        Text("mejora: ${series.perk.labelEs}", fontSize = 9.5.sp, color = InkTertiary, maxLines = 1)
                    }
                    Spacer(Modifier.height(5.dp))
                    ProgressBar(have.toFloat() / total, if (done) Sage else accent)
                }
                Spacer(Modifier.width(8.dp))
                when {
                    claimed -> Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Sage, modifier = Modifier.size(26.dp))
                    claimable -> Text(
                        "RECLAMAR",
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Gold).clickable(onClick = onClaim).padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.sp
                    )
                    else -> Column(horizontalAlignment = Alignment.End) {
                        Text("${series.rewardCoins}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Gold)
                        CozyText("${series.rewardGems} ◆", fontSize = 10.sp, fontWeight = FontWeight.Black, color = GemBlue)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CollectibleCatalog.inSeries(series), key = { it.id }) { c ->
                    Box(Modifier.width(78.dp).clickable { onPick(c) }) {
                        CollectibleCard(c, Modifier.fillMaxWidth(), owned = c.id in owned, foil = c.id in foil, copies = Copies.count(copies, c.id), animate = false)
                    }
                }
            }
            if (!done && have > 0) {
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(12.dp)).background(Gold.copy(alpha = 0.14f)).clickable(onClick = onPack).padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = Gold, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Sobre de esta serie · ${SeriesPackRules.COINS} monedas", fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = Navy)
                }
            }
        }
    }
}

// ============================== Repetidas ==============================

@Composable
private fun SparesTab(copies: Map<String, Int>, sellTenths: Int, onPick: (Collectible) -> Unit, onSellAll: (Rarity) -> Unit) {
    val spares = remember(copies) {
        Copies.spares(copies).mapNotNull { (id, n) -> CollectibleCatalog.byId(id)?.let { it to n } }
            .sortedWith(compareByDescending<Pair<Collectible, Int>> { it.first.rarity.ordinal }.thenBy { it.first.id })
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (spares.isEmpty()) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🗂️", fontSize = 34.sp)
                Text("Aún no tienes repetidas", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Navy, modifier = Modifier.padding(top = 6.dp))
                Text(
                    "Cuando un cofre te dé una carta que ya tienes, se guarda aquí. Podrás venderla por monedas, reciclarla en esencia o cambiarla con un amigo por una que te falte.",
                    fontSize = 12.sp, color = InkSecondary, textAlign = TextAlign.Center, lineHeight = 16.sp, modifier = Modifier.padding(top = 6.dp)
                )
            }
            return
        }
        val totalCoins = spares.sumOf { (c, n) -> SellRules.value(c, sellTenths) * n }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White).padding(14.dp)) {
            Text("${spares.sumOf { it.second }} cartas repetidas · valen $totalCoins monedas", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Navy)
            Text("Tu primera copia de cada carta nunca se vende: se queda en el álbum.", fontSize = 10.5.sp, color = InkSecondary, modifier = Modifier.padding(top = 2.dp))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val c1 = SellRules.bulk(copies, Rarity.COMMON).sumOf { (c, n) -> SellRules.value(c, sellTenths) * n }
                val c2 = SellRules.bulk(copies, Rarity.RARE).sumOf { (c, n) -> SellRules.value(c, sellTenths) * n }
                ActionPill("VENDER COMUNES · $c1", Icons.Rounded.MonetizationOn, Gold, c1 > 0, Modifier.weight(1f)) { onSellAll(Rarity.COMMON) }
                ActionPill("COMUNES Y RARAS · $c2", Icons.Rounded.MonetizationOn, Gold, c2 > 0, Modifier.weight(1f)) { onSellAll(Rarity.RARE) }
            }
            if (sellTenths > 0) Text("Tus cartas épicas y legendarias suman +${tenths(sellTenths)}% al vender.", fontSize = 10.sp, color = Sage, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
        }
        spares.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (c, n) ->
                    Column(Modifier.weight(1f).clickable { onPick(c) }, horizontalAlignment = Alignment.CenterHorizontally) {
                        CollectibleCard(c, Modifier.fillMaxWidth(), copies = n + 1, animate = false)
                        Text("+${SellRules.value(c, sellTenths)} 🪙  ·  ${c.rarity.tradeTokens} 🎟", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = InkSecondary, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ============================== Intercambio ==============================

@Composable
private fun TradeTab(
    manager: CollectionManager, trades: TradeManager, tokens: Int, copies: Map<String, Int>, owned: Set<String>, gemsNow: Int,
    friends: List<UserProfile>, incoming: List<TradeInbox>, outgoing: List<TradeOutgoing>,
    onToast: (String) -> Unit, onChanged: () -> Unit, onIncoming: (List<TradeInbox>) -> Unit
) {
    var openFriend by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // fichas
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(PurpleDeep, PurpleMid))).padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎟", fontSize = 26.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Fichas de intercambio: $tokens", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text("Cambiar una carta cuesta: común 1 · rara 2 · épica 4 · legendaria 8", fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.75f), lineHeight = 13.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Las ganas: 1 al día al entrar, con las misiones del día y de la semana, en el Pase, al subir de rango y con las mejoras de tus cartas.", fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.7f), lineHeight = 13.sp)
            Spacer(Modifier.height(8.dp))
            val bought = manager.tokensBoughtToday()
            val can = TokenRules.canBuy(gemsNow, bought, tokens)
            val ctx = LocalContext.current
            val vipNow by remember { EconomyManager(ctx) }.isVip.collectAsState()
            val adFreq = remember { com.korkoor.pardos.data.local.AdFrequency(ctx) }
            var adTick by remember { mutableIntStateOf(0) }
            val adLeft = remember(adTick) { com.korkoor.pardos.domain.shop.AdRewards.left(adFreq.usedToday(com.korkoor.pardos.data.local.AdFrequency.SLOT_TOKEN), com.korkoor.pardos.domain.shop.AdRewards.TOKEN_PER_DAY) }
            if (adLeft > 0 && tokens < TokenRules.MAX_STOCK) {
                com.korkoor.pardos.ui.design.WatchAdButton(
                    label = "FICHA GRATIS", sublabel = if (vipNow) "VIP: sin anuncio · te quedan $adLeft hoy" else "Ver un anuncio · te quedan $adLeft hoy",
                    tag = "+1", color = Color(0xFF8B6FF0), minHeight = 48.dp, adFree = vipNow,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val grant = {
                            manager.addTokens(1)
                            com.korkoor.pardos.audio.GameAudio.play(com.korkoor.pardos.audio.Sfx.COIN)
                            adFreq.consume(com.korkoor.pardos.data.local.AdFrequency.SLOT_TOKEN)
                            adTick++
                            onToast("+1 ficha de intercambio")
                        }
                        if (vipNow) grant()
                        else (ctx as? android.app.Activity)?.let { act -> com.korkoor.pardos.ui.game.logic.AdManager.showRewardedAd(act) { grant() } }
                    }
                )
                Spacer(Modifier.height(8.dp))
            }
            Row(
                Modifier.clip(RoundedCornerShape(14.dp)).background(if (can) Color.White else Color.White.copy(alpha = 0.15f))
                    .clickable(enabled = can) { onToast(if (manager.buyToken()) "+1 ficha de intercambio" else "No se pudo comprar") }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Comprar 1 ficha · ${TokenRules.GEMS_PER_TOKEN} gemas (hoy $bought/${TokenRules.MAX_BOUGHT_PER_DAY})", fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (can) PurpleDeep else Color.White.copy(alpha = 0.5f))
            }
        }

        if (!trades.available) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White).padding(16.dp)) {
                Text("Inicia sesión para intercambiar", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Navy)
                Text("Los intercambios son con tus amigos, que viven en tu cuenta de Google. Entra desde Perfil.", fontSize = 12.sp, color = InkSecondary, modifier = Modifier.padding(top = 4.dp))
            }
            return
        }

        // propuestas recibidas
        if (incoming.isNotEmpty()) {
            SectionLabel("Te proponen")
            incoming.forEach { inbox ->
                val give = CollectibleCatalog.byId(inbox.give)
                val want = CollectibleCatalog.byId(inbox.want)
                if (give != null && want != null) {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White).border(2.dp, Color(0xFF6A4CE0).copy(alpha = 0.5f), RoundedCornerShape(20.dp)).padding(12.dp)) {
                        Text("${inbox.fromName} te ofrece:", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Navy)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.width(78.dp)) { CollectibleCard(give, Modifier.fillMaxWidth(), owned = true, animate = false, showName = true) }
                            Icon(Icons.Rounded.SwapHoriz, null, tint = Color(0xFF6A4CE0), modifier = Modifier.size(28.dp))
                            Box(Modifier.width(78.dp)) { CollectibleCard(want, Modifier.fillMaxWidth(), owned = true, animate = false, showName = true) }
                            Spacer(Modifier.weight(1f))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("ACEPTAR", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Sage).clickable {
                                    trades.accept(inbox) { err ->
                                        onToast(err ?: "¡Intercambio hecho! Recibiste ${give.nameEs}.")
                                        if (err == null) onIncoming(incoming - inbox)
                                    }
                                }.padding(horizontal = 12.dp, vertical = 8.dp))
                                Text("RECHAZAR", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Navy, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Navy.copy(alpha = 0.08f)).clickable {
                                    trades.decline(inbox) { ok -> if (ok) onIncoming(incoming - inbox) }
                                }.padding(horizontal = 12.dp, vertical = 8.dp))
                            }
                        }
                        Text("Entregas ${want.nameEs} (una de tus repetidas) y te quedas ${give.nameEs}.", fontSize = 10.5.sp, color = InkSecondary, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }

        // mis propuestas
        if (outgoing.isNotEmpty()) {
            SectionLabel("Esperando respuesta")
            outgoing.forEach { o ->
                val give = CollectibleCatalog.byId(o.give); val want = CollectibleCatalog.byId(o.want)
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${give?.glyph ?: "?"} → ${want?.glyph ?: "?"}", fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${give?.nameEs} por ${want?.nameEs}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Navy, maxLines = 1)
                        Text("A ${o.toName}", fontSize = 10.5.sp, color = InkSecondary)
                    }
                    Text("CANCELAR", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = Terracotta, modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable {
                        trades.cancel(o) { ok -> onToast(if (ok) "Propuesta cancelada: recuperaste tu carta y tus fichas." else "No se pudo cancelar."); onChanged() }
                    }.padding(8.dp))
                }
            }
        }

        // amigos
        SectionLabel("Cambiar con un amigo")
        if (friends.isEmpty()) {
            Text("Agrega amigos en la sección Amigos para empezar a cambiar cartas.", fontSize = 12.sp, color = InkSecondary)
        }
        friends.forEach { f ->
            val theirOwned = remember(f.albumBits) { AlbumBits.decode(f.albumBits) }
            val theirSpares = remember(f.spareBits) { AlbumBits.decode(f.spareBits).toSet() }
            val suggestions = remember(copies, f.albumBits, f.spareBits) { TradeRules.suggestions(copies, theirSpares, theirOwned) }
            val shares = f.albumBits.isNotEmpty()
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White).padding(12.dp)) {
                Row(Modifier.clickable { openFriend = if (openFriend == f.uid) null else f.uid }, verticalAlignment = Alignment.CenterVertically) {
                    AvatarFramed(f.avatarId, Modifier.size(42.dp), ring = 3.dp, animate = false)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(f.name, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Navy, maxLines = 1)
                        Text(
                            if (!shares) "Todavía no comparte su álbum (necesita la última versión)" else "${theirOwned.size} cartas · ${suggestions.size} cambios posibles",
                            fontSize = 10.5.sp, color = if (suggestions.isNotEmpty()) Sage else InkSecondary, fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(if (openFriend == f.uid) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = InkSecondary)
                }
                if (openFriend == f.uid) {
                    Spacer(Modifier.height(10.dp))
                    if (suggestions.isEmpty()) {
                        Text(
                            if (!shares) "Cuando actualice la app podrás ver qué le sobra y qué le falta."
                            else "Ahora mismo no hay un cambio que les convenga a los dos. Cuando tengas repetidas que a ${f.name} le falten (y él tenga las tuyas), aparecerán aquí.",
                            fontSize = 11.5.sp, color = InkSecondary, lineHeight = 15.sp
                        )
                    }
                    suggestions.forEach { s ->
                        val cost = TradeRules.cost(s.give.rarity)
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(56.dp)) { CollectibleCard(s.give, Modifier.fillMaxWidth(), animate = false, showName = false) }
                            Icon(Icons.Rounded.SwapHoriz, null, tint = Color(0xFF6A4CE0), modifier = Modifier.size(22.dp).padding(horizontal = 2.dp))
                            Box(Modifier.width(56.dp)) { CollectibleCard(s.want, Modifier.fillMaxWidth(), animate = false, showName = false) }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${s.give.nameEs} por ${s.want.nameEs}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Navy, maxLines = 2, lineHeight = 13.sp)
                                Text(rarityName(s.give.rarity), fontSize = 9.sp, fontWeight = FontWeight.Black, color = rarityColor(s.give.rarity))
                            }
                            val enough = tokens >= cost
                            Text(
                                "PROPONER · $cost 🎟", fontSize = 9.sp, fontWeight = FontWeight.Black, color = if (enough) Color.White else Navy.copy(alpha = 0.4f), maxLines = 1,
                                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(if (enough) Color(0xFF6A4CE0) else Navy.copy(alpha = 0.08f)).clickable(enabled = enough) {
                                    trades.propose(f.uid, f.name, TradeOffer(s.give.id, s.want.id)) { err ->
                                        onToast(err ?: "¡Propuesta enviada a ${f.name}! Te avisaremos cuando responda.")
                                        onChanged()
                                    }
                                }.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================== Detalle de una carta ==============================

@Composable
private fun CollectibleDialog(
    c: Collectible, owned: Boolean, foil: Boolean, copies: Int, shards: Int, exhibited: Boolean, showcaseFull: Boolean, sellTenths: Int,
    onCraft: () -> Unit, onFoil: () -> Unit, onSell: () -> Unit, onRecycle: () -> Unit, onTrade: () -> Unit, onShowcase: () -> Unit, onDismiss: () -> Unit
) {
    val color = rarityColor(c.rarity)
    val canCraft = CraftRules.canCraft(c, if (owned) setOf(c.id) else emptySet(), shards)
    val canFoil = FoilRules.canUpgrade(c, if (owned) setOf(c.id) else emptySet(), if (foil) setOf(c.id) else emptySet(), shards)
    val spare = (copies - 1).coerceAtLeast(0)
    Dialog(onDismissRequest = onDismiss) {
        JellyColumn(
            modifier = Modifier.popIn(), shape = RoundedCornerShape(Radius.XLarge), fill = Cream, lipHeight = 8.dp,
            padding = PaddingValues(18.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.width(190.dp)) { CollectibleCard(c, Modifier.fillMaxWidth(), owned = owned, foil = foil, copies = copies, showName = false) }
            Spacer(Modifier.height(10.dp))
            Text(if (owned) c.nameEs else "???", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Navy)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(rarityName(c.rarity).uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = color, letterSpacing = 1.5.sp)
                Text("·", color = InkTertiary)
                Text("${c.series.glyph} ${c.series.nameEs}  #${c.number}", fontSize = 11.sp, color = InkSecondary)
            }
            if (owned) {
                Text(c.descEs, fontSize = 12.5.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = Navy.copy(alpha = 0.75f), textAlign = TextAlign.Center, lineHeight = 17.sp, modifier = Modifier.padding(top = 6.dp))
            } else {
                Text("Todavía no la tienes. Mira cómo conseguirla abajo.", fontSize = 12.sp, color = InkSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            }
            c.perk?.let { perk ->
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF6A4CE0).copy(alpha = if (owned) 0.12f else 0.05f)).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = Color(0xFF6A4CE0).copy(alpha = if (owned) 1f else 0.4f), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(if (owned) "MEJORA ACTIVA" else "MEJORA AL CONSEGUIRLA", fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF6A4CE0), letterSpacing = 1.5.sp)
                        Text(perk.label(foil), fontSize = 12.5.sp, fontWeight = FontWeight.Black, color = Navy)
                        if (!foil) Text("Brillante: ${perk.label(true)}", fontSize = 9.5.sp, color = InkSecondary)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (owned) {
                Text(
                    if (spare > 0) "Tienes $copies copias: $spare repetida${if (spare > 1) "s" else ""}." else "Solo tienes la copia del álbum.",
                    fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = if (spare > 0) Color(0xFF6A4CE0) else InkSecondary
                )
                Spacer(Modifier.height(8.dp))
                PrimaryButton(
                    text = if (exhibited) "QUITAR DE MI VITRINA" else if (showcaseFull) "VITRINA LLENA" else "EXHIBIR EN MI PERFIL",
                    onClick = onShowcase, enabled = exhibited || !showcaseFull, height = 46.dp, fontSize = 11.sp
                )
                if (spare > 0) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ActionPill("VENDER +${SellRules.value(c, sellTenths)}", Icons.Rounded.MonetizationOn, Gold, true, Modifier.weight(1f), onSell)
                        ActionPill("RECICLAR +${c.rarity.shardValue}", Icons.Rounded.AutoFixHigh, Violet, true, Modifier.weight(1f), onRecycle)
                        ActionPill("CAMBIAR", Icons.Rounded.SwapHoriz, Color(0xFF6A4CE0), true, Modifier.weight(0.8f), onTrade)
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (foil) {
                    Text("✦ BRILLANTE", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Gold, letterSpacing = 2.sp)
                } else {
                    PrimaryButton(text = "HACER BRILLANTE · ${FoilRules.cost(c)} esencia", onClick = onFoil, enabled = canFoil, height = 46.dp, fontSize = 11.sp)
                    Text(
                        if (canFoil) "Brilla en tu álbum, duplica su mejora y suma monedas extra." else "Te faltan ${FoilRules.cost(c) - shards} de esencia. Cada ${FoilRules.PER_PERCENT} brillantes = +1% de monedas.",
                        fontSize = 10.5.sp, color = InkSecondary, modifier = Modifier.padding(top = 6.dp), textAlign = TextAlign.Center
                    )
                }
            } else {
                PrimaryButton(text = "CREAR · ${c.rarity.craftCost} esencia", onClick = onCraft, enabled = canCraft, height = 46.dp, fontSize = 11.sp)
                Text(
                    if (canCraft) "Gasta tu esencia para conseguirla ya." else "Te faltan ${c.rarity.craftCost - shards} de esencia. Sale de cofres, sobres de serie o cambiándola con un amigo.",
                    fontSize = 10.5.sp, color = InkSecondary, modifier = Modifier.padding(top = 6.dp), textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ============================== Apertura ==============================

@Composable
private fun OpeningDialog(opening: Opening, manager: CollectionManager, onDone: () -> Unit) {
    val context = LocalContext.current
    val vip by remember { EconomyManager(context) }.isVip.collectAsState()
    val adFreq = remember { com.korkoor.pardos.data.local.AdFrequency(context) }
    // Una carta más a cambio de un anuncio (solo en cofres; con tope diario)
    var extra by remember { mutableStateOf<Drop?>(null) }
    var adTick by remember { mutableIntStateOf(0) }
    val extraLeft = remember(adTick) { com.korkoor.pardos.domain.shop.AdRewards.left(adFreq.usedToday(com.korkoor.pardos.data.local.AdFrequency.SLOT_EXTRA_CARD), com.korkoor.pardos.domain.shop.AdRewards.EXTRA_CARD_PER_DAY) }
    val drops = opening.drops + listOfNotNull(extra)
    // Las cartas se revelan una a una
    var revealed by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(extra) {
        val e = extra ?: return@LaunchedEffect
        kotlinx.coroutines.delay(450L)
        revealed = opening.drops.size + 1
        com.korkoor.pardos.audio.GameAudio.card(e.collectible.rarity.ordinal, e.isNew)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    LaunchedEffect(Unit) {
        com.korkoor.pardos.audio.GameAudio.play(com.korkoor.pardos.audio.Sfx.CHEST_SHAKE)
        kotlinx.coroutines.delay(500L)
        com.korkoor.pardos.audio.GameAudio.play(com.korkoor.pardos.audio.Sfx.CHEST_OPEN)
        for (i in 1..drops.size) {
            val rarity = drops[i - 1].collectible.rarity
            // Suspense: las piezas buenas se hacen esperar un poco más
            kotlinx.coroutines.delay(if (rarity.ordinal >= Rarity.EPIC.ordinal) 950L else 550L)
            revealed = i
            com.korkoor.pardos.audio.GameAudio.card(rarity.ordinal, drops[i - 1].isNew)
            haptic.performHapticFeedback(if (rarity.ordinal >= Rarity.RARE.ordinal) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove)
        }
    }
    val all = revealed >= drops.size
    val best = drops.maxOf { it.collectible.rarity.ordinal }
    val cardsScroll = rememberScrollState()
    // Al llegar la carta extra (queda en una fila nueva) la lista baja sola para enseñarla
    LaunchedEffect(extra) {
        if (extra != null) {
            kotlinx.coroutines.delay(600L)
            cardsScroll.animateScrollTo(cardsScroll.maxValue)
        }
    }
    Dialog(onDismissRequest = { if (all) onDone() }) {
        JellyColumn(
            modifier = Modifier.popIn(), shape = RoundedCornerShape(Radius.XLarge), fill = Cream, lipHeight = 8.dp,
            padding = PaddingValues(16.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(opening.title.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black, color = InkSecondary, letterSpacing = 2.sp)
            Text(
                when {
                    !all -> "Abriendo…"
                    best == Rarity.LEGENDARY.ordinal -> "¡LEGENDARIA!"
                    best == Rarity.EPIC.ordinal -> "¡Carta épica!"
                    else -> "¡Tu botín!"
                },
                fontSize = 22.sp, fontWeight = FontWeight.Black,
                color = if (all && best == Rarity.LEGENDARY.ordinal) Gold else Navy
            )
            Spacer(Modifier.height(12.dp))
            // Las cartas se desplazan si no caben (cofre épico + carta extra = 5); los botones de abajo no se mueven
            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(cardsScroll),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                drops.chunked(2).forEachIndexed { rowIdx, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                        row.forEachIndexed { colIdx, drop ->
                            DropCard(drop, visible = rowIdx * 2 + colIdx < revealed, modifier = Modifier.weight(1f))
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            val chestType = opening.chest
            if (all && chestType != null && extra == null && extraLeft > 0) {
                com.korkoor.pardos.ui.design.WatchAdButton(
                    label = "UNA CARTA MÁS",
                    sublabel = if (vip) "VIP: sin anuncio · te quedan $extraLeft hoy" else "Ver un anuncio · te quedan $extraLeft hoy",
                    tag = "+1",
                    adFree = vip,
                    color = Color(0xFF6A4CE0),
                    minHeight = 50.dp,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    onClick = {
                        val grant = {
                            extra = manager.grantExtraCard(chestType, opening.drops.map { it.collectible.id }.toSet())
                            adFreq.consume(com.korkoor.pardos.data.local.AdFrequency.SLOT_EXTRA_CARD)
                            adTick++
                        }
                        if (vip) grant() else (context as? android.app.Activity)?.let { act -> com.korkoor.pardos.ui.game.logic.AdManager.showRewardedAd(act) { grant() } }
                    }
                )
            }
            val repeats = drops.count { !it.isNew }
            if (all && repeats > 0) {
                Text("$repeats repetida${if (repeats > 1) "s" else ""} guardada${if (repeats > 1) "s" else ""} en la pestaña Repetidas: véndelas, recíclalas o cámbialas.", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6A4CE0), textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(4.dp))
            PrimaryButton(if (all) "GENIAL" else "...", onClick = onDone, enabled = all, height = 50.dp)
        }
    }
}

@Composable
private fun DropCard(drop: Drop, visible: Boolean, modifier: Modifier) {
    val c = drop.collectible
    // Estallido de partículas al revelar: más vistoso cuanto más rara es la pieza
    val burstFx = when (c.rarity) {
        Rarity.COMMON -> com.korkoor.pardos.domain.shop.MergeFx.CLASSIC
        Rarity.RARE -> com.korkoor.pardos.domain.shop.MergeFx.SPARKS
        Rarity.EPIC -> com.korkoor.pardos.domain.shop.MergeFx.STARS
        Rarity.LEGENDARY -> com.korkoor.pardos.domain.shop.MergeFx.FIREWORKS
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = visible, modifier = Modifier.fillMaxWidth(), enter = scaleIn(spring(dampingRatio = 0.5f)) + fadeIn()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CollectibleCard(c, Modifier.fillMaxWidth(), animate = true)
                Text(
                    when { drop.bonus -> "¡EXTRA DE LA SUERTE!"; drop.isNew -> "¡NUEVA!"; else -> "repetida" },
                    fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp,
                    color = when { drop.bonus -> Gold; drop.isNew -> Sage; else -> InkTertiary }, modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        com.korkoor.pardos.ui.game.components.MergeBurst(burstFx, if (visible) 1 else 0, 1024, 96.dp, Modifier.zIndex(5f))
    }
}
