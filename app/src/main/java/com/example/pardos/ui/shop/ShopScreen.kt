package com.korkoor.pardos.ui.shop

import com.korkoor.pardos.ui.design.CozyText

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.*
import com.korkoor.pardos.ui.design.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.data.billing.BillingManager
import com.korkoor.pardos.data.local.CollectionManager
import com.korkoor.pardos.data.local.EconomyManager
import com.korkoor.pardos.data.local.LocalDay
import com.korkoor.pardos.domain.collection.ChestType
import com.korkoor.pardos.domain.economy.Economy
import com.korkoor.pardos.domain.shop.*
import com.korkoor.pardos.ui.collection.chestColor
import com.korkoor.pardos.ui.collection.chestName
import com.korkoor.pardos.ui.design.*
import com.korkoor.pardos.ui.game.components.tileLook
import com.korkoor.pardos.ui.menu.CurrencyPill
import com.korkoor.pardos.ui.theme.GameTheme

private enum class ShopTab(val label: String, val icon: ImageVector) {
    FEATURED("Destacado", Icons.Rounded.AutoAwesome),
    SKINS("Skins", Icons.Rounded.Palette),
    CHESTS("Cofres", Icons.Rounded.Inventory2),
    GEMS("Gemas", Icons.Rounded.Diamond)
}

fun skinName(skin: TileSkin): String = skin.displayName

@Composable
fun ShopScreen(
    activity: Activity,
    billing: BillingManager,
    economy: EconomyManager,
    onBack: () -> Unit,
    onStudio: () -> Unit = {},
    onSeason: () -> Unit = {}
) {
    val context = LocalContext.current
    val collection = remember { CollectionManager(context) }
    val retention = remember { com.korkoor.pardos.data.local.RetentionManager(context) }
    val studioConfig by economy.studioConfig.collectAsState()
    val piggy by retention.piggy.collectAsState()
    val coins by economy.coins.collectAsState()
    val gems by economy.gems.collectAsState()
    val freezes by economy.streakFreezes.collectAsState()
    val undos by economy.undos.collectAsState()
    val extraTimes by economy.extraTimes.collectAsState()
    val isVip by economy.isVip.collectAsState()
    val equippedSkin by economy.equippedSkin.collectAsState()
    val ownedSkins by economy.ownedSkins.collectAsState()
    val chests by collection.chests.collectAsState()
    val prices by billing.prices.collectAsState()
    val message by billing.message.collectAsState()
    var localMessage by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf(ShopTab.FEATURED) }

    val today = remember { LocalDay.today() }
    val liveEvents = remember(today) { com.korkoor.pardos.domain.events.EventCalendar.activeOn(today) }
    var eventDialog by remember { mutableStateOf<com.korkoor.pardos.domain.events.GameEvent?>(null) }
    eventDialog?.let { ev -> com.korkoor.pardos.ui.rewards.EventSkinDialog(ev, retention) { eventDialog = null } }
    val offer = remember(today) { DailyOffers.forDay(today) }

    LaunchedEffect(Unit) { billing.connect() }

    Box(modifier = Modifier.fillMaxSize().background(ScreenBackground)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            PardosTopBar(title = "Tienda", onBack = onBack) {
                CurrencyPill(Icons.Rounded.MonetizationOn, coins, Gold)
                Spacer(Modifier.width(8.dp))
                CurrencyPill(Icons.Rounded.Diamond, gems, GemBlue)
            }

            // Pestañas
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                items(ShopTab.entries.toList()) { t ->
                    val sel = t == tab
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (sel) Navy else Color.White)
                            .clickable { tab = t }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(t.icon, contentDescription = null, tint = if (sel) Color.White else Navy.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(t.label, fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (sel) Color.White else Navy)
                    }
                }
            }

            (message ?: localMessage)?.let { msg ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Sage.copy(alpha = 0.15f))
                        .clickable { billing.clearMessage(); localMessage = null }
                        .padding(14.dp)
                ) { Text(msg, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Navy) }
            }

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp)
            ) {
                when (tab) {
                    ShopTab.FEATURED -> {
                        Spacer(Modifier.height(8.dp))
                        // --- Oferta del día ---
                        DailyOfferCard(
                            offer = offer,
                            coins = coins, gems = gems,
                            ownedSkins = ownedSkins,
                            onBuy = {
                                when (val item = offer.item) {
                                    is OfferItem.SkinOffer -> when (economy.buySkin(item.skin, offer.discountPercent)) {
                                        is SkinInventory.Purchase.Ok -> { economy.equipSkin(item.skin); localMessage = "¡${skinName(item.skin)} desbloqueada y equipada!" }
                                        SkinInventory.Purchase.NotEnoughCoins -> localMessage = "Te faltan monedas"
                                        SkinInventory.Purchase.NotEnoughGems -> localMessage = "Te faltan gemas"
                                        SkinInventory.Purchase.AlreadyOwned -> localMessage = "Ya tienes esta skin"
                                        SkinInventory.Purchase.NotPurchasable -> Unit
                                    }
                                    is OfferItem.ChestOffer -> {
                                        val price = (ShopPrices.chestCoins(item.type) ?: ShopPrices.chestGems(item.type))!!.discounted(offer.discountPercent)
                                        localMessage = if (collection.buyChest(item.type, price)) "¡${chestName(item.type)} comprado! Ábrelo en el Álbum" else "No te alcanza para esta oferta"
                                    }
                                }
                            }
                        )

                        Spacer(Modifier.height(16.dp))
                        // --- Pack inicial ---
                        if (!economy.isStarterClaimed()) {
                            StarterPackCard(price = prices[ShopCatalog.STARTER_PACK]) { billing.purchase(activity, ShopCatalog.STARTER_PACK) }
                            Spacer(Modifier.height(16.dp))
                        }

                        PromoCard(
                            icon = Icons.Rounded.WorkspacePremium, color = Gold,
                            title = "PASE DE TEMPORADA", tag = "NUEVO CADA MES",
                            text = "30 niveles de premios gratis y una vía premium con skin exclusiva.",
                            action = "VER", onClick = onSeason
                        )
                        Spacer(Modifier.height(12.dp))
                        StudioPromo(owned = TileSkin.STUDIO.id in ownedSkins, price = prices[ShopCatalog.SKIN_STUDIO], config = studioConfig, onClick = onStudio)
                        Spacer(Modifier.height(12.dp))
                        VipCard(owned = isVip, price = prices[ShopCatalog.VIP_FOREVER], onBuy = { billing.purchase(activity, ShopCatalog.VIP_FOREVER) })

                        SectionTitle("CONSUMIBLES")
                        ShopRow(
                            icon = Icons.AutoMirrored.Rounded.Undo, color = Terracotta,
                            title = "Deshacer x3", subtitle = "Vuelve atrás un movimiento · Tienes $undos",
                            priceText = "${Economy.UNDO_PRICE_COINS * 3}", priceIcon = Icons.Rounded.MonetizationOn, priceColor = Gold,
                            enabled = coins >= Economy.UNDO_PRICE_COINS * 3,
                            onClick = { localMessage = if (economy.buyUndos(3)) "+3 Deshacer" else "No te alcanza" }
                        )
                        Spacer(Modifier.height(10.dp))
                        ShopRow(
                            icon = Icons.Rounded.Timer, color = GemBlue,
                            title = "Tiempo extra x3", subtitle = "+${Economy.EXTRA_TIME_SECONDS}s en modos con reloj · Tienes $extraTimes",
                            priceText = "${Economy.EXTRA_TIME_PRICE_COINS * 3}", priceIcon = Icons.Rounded.MonetizationOn, priceColor = Gold,
                            enabled = coins >= Economy.EXTRA_TIME_PRICE_COINS * 3,
                            onClick = { localMessage = if (economy.buyExtraTimes(3)) "+3 Tiempo extra" else "No te alcanza" }
                        )
                        Spacer(Modifier.height(10.dp))
                        ShopRow(
                            icon = Icons.Rounded.Shield, color = Sage,
                            title = "Escudo de racha", subtitle = "Salva tu racha si faltas un día · Tienes $freezes/${CoinShop.MAX_STREAK_FREEZES}",
                            priceText = "${CoinShop.STREAK_FREEZE_PRICE_COINS}", priceIcon = Icons.Rounded.MonetizationOn, priceColor = Gold,
                            enabled = CoinShop.canBuyStreakFreeze(coins, freezes),
                            onClick = { localMessage = if (economy.buyStreakFreeze()) "¡Escudo de racha añadido!" else "No se pudo comprar" }
                        )
                        Spacer(Modifier.height(10.dp))
                        ShopRow(
                            icon = Icons.Rounded.SwapHoriz, color = GemBlue,
                            title = "Cambiar gemas", subtitle = "5 gemas = ${CoinShop.coinsForGems(5)} monedas",
                            priceText = "5", priceIcon = Icons.Rounded.Diamond, priceColor = GemBlue,
                            enabled = gems >= 5,
                            onClick = { localMessage = if (economy.exchangeGems(5)) "+${CoinShop.coinsForGems(5)} monedas" else "No tienes gemas suficientes" }
                        )
                    }

                    ShopTab.SKINS -> {
                        Spacer(Modifier.height(8.dp))
                        Text("Cada skin cambia las fichas, el fondo y las partículas del juego.", fontSize = 12.sp, color = InkSecondary)
                        Spacer(Modifier.height(12.dp))
                        StudioPromo(owned = TileSkin.STUDIO.id in ownedSkins, price = prices[ShopCatalog.SKIN_STUDIO], config = studioConfig, onClick = onStudio)
                        Spacer(Modifier.height(14.dp))
                        TileSkin.entries.filter { it != TileSkin.STUDIO && it.source != SkinSource.EVENT && it.source != SkinSource.HIDDEN }.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                                row.forEach { skin ->
                                    val owned = skin.isFree || skin.id in ownedSkins
                                    SkinCard(
                                        skin = skin, owned = owned, equipped = skin == equippedSkin,
                                        canAfford = coins >= skin.coinPrice && gems >= skin.gemPrice,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            if (owned) {
                                                economy.equipSkin(skin); localMessage = "Skin equipada: ${skinName(skin)}"
                                            } else when (economy.buySkin(skin)) {
                                                is SkinInventory.Purchase.Ok -> { economy.equipSkin(skin); localMessage = "¡${skinName(skin)} desbloqueada y equipada!" }
                                                SkinInventory.Purchase.NotEnoughCoins -> localMessage = "Te faltan monedas"
                                                SkinInventory.Purchase.NotEnoughGems -> localMessage = "Te faltan gemas"
                                                SkinInventory.Purchase.NotPurchasable -> localMessage = when (skin.source) {
                                                    SkinSource.SEASON -> "Se consigue en el Pase de temporada"
                                                    else -> "Se consigue completando el Álbum"
                                                }
                                                SkinInventory.Purchase.AlreadyOwned -> Unit
                                            }
                                        }
                                    )
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }

                        // ---- Fiestas del año ----
                        SectionTitle("FIESTAS DEL AÑO")
                        Text(
                            "Se ganan jugando durante cada fiesta (${EventSkins.WINS_REQUIRED} victorias). Si te la pierdes, vuelve el año siguiente.",
                            fontSize = 12.sp, color = InkSecondary, lineHeight = 16.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        TileSkin.events.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                                row.forEach { skin ->
                                    val type = EventSkins.eventFor(skin)
                                    val live = type?.let { t -> liveEvents.firstOrNull { it.type == t } }
                                    val owned = skin.id in ownedSkins
                                    SkinCard(
                                        skin = skin, owned = owned, equipped = skin == equippedSkin, canAfford = true,
                                        modifier = Modifier.weight(1f),
                                        status = if (live != null) "EN CURSO · ${retention.eventWins(live)}/${EventSkins.WINS_REQUIRED}" else type?.let { EventSkins.dateRange(it)?.uppercase() },
                                        statusColor = if (live != null) Terracotta else null,
                                        onClick = {
                                            when {
                                                owned -> { economy.equipSkin(skin); localMessage = "Skin equipada: ${skinName(skin)}" }
                                                live != null -> eventDialog = live
                                                else -> localMessage = "Disponible del ${type?.let { EventSkins.dateRange(it) }} (cada año)"
                                            }
                                        }
                                    )
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }

                        // ---- Secretas ----
                        SectionTitle("SECRETAS")
                        Text("Nadie te dice cómo conseguirlas. Descúbrelas jugando.", fontSize = 12.sp, color = InkSecondary)
                        Spacer(Modifier.height(12.dp))
                        TileSkin.hidden.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                                row.forEach { skin ->
                                    val owned = skin.id in ownedSkins
                                    if (owned) {
                                        SkinCard(
                                            skin = skin, owned = true, equipped = skin == equippedSkin, canAfford = true,
                                            modifier = Modifier.weight(1f),
                                            onClick = { economy.equipSkin(skin); localMessage = "Skin equipada: ${skinName(skin)}" }
                                        )
                                    } else {
                                        HiddenSkinCard(
                                            hint = HiddenSkins.hint(skin), modifier = Modifier.weight(1f),
                                            onClick = { localMessage = "Pista: ${HiddenSkins.hint(skin)}" }
                                        )
                                    }
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    ShopTab.CHESTS -> {
                        Spacer(Modifier.height(8.dp))
                        Text("Los cofres traen piezas del Álbum. Hay garantías: nunca pasarás muchos cofres sin una pieza épica.", fontSize = 12.sp, color = InkSecondary, lineHeight = 16.sp)
                        Spacer(Modifier.height(12.dp))
                        ChestType.entries.forEach { t ->
                            val gemPrice = ShopPrices.chestGems(t)
                            ChestRow(
                                type = t, owned = chests[t] ?: 0,
                                coinPrice = ShopPrices.chestCoins(t), gemPrice = gemPrice,
                                coins = coins, gems = gems,
                                onBuyCoins = {
                                    ShopPrices.chestCoins(t)?.let { p ->
                                        localMessage = if (collection.buyChest(t, p)) "¡${chestName(t)} comprado!" else "Te faltan monedas"
                                    }
                                },
                                onBuyGems = {
                                    gemPrice?.let { p ->
                                        localMessage = if (collection.buyChest(t, p)) "¡${chestName(t)} comprado!" else "Te faltan gemas"
                                    }
                                }
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                    }

                    ShopTab.GEMS -> {
                        Spacer(Modifier.height(8.dp))
                        Text("Las gemas sirven para skins premium y cofres raros y épicos.", fontSize = 12.sp, color = InkSecondary)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(ShopCatalog.GEMS_SMALL to "Puñado", ShopCatalog.GEMS_MEDIUM to "Bolsa", ShopCatalog.GEMS_LARGE to "Cofre").forEach { (id, label) ->
                                val product = ShopCatalog.byId(id)!!
                                GemPackCard(label, product.gems, prices[id], highlight = id == ShopCatalog.GEMS_MEDIUM, modifier = Modifier.weight(1f)) {
                                    billing.purchase(activity, id)
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        PiggyShopCard(
                            gems = piggy, price = prices[ShopCatalog.PIGGY_BREAK],
                            onBreak = { billing.purchase(activity, ShopCatalog.PIGGY_BREAK) }
                        )
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "Las compras se procesan con Google Play. Las gemas y el VIP se recuperan al reinstalar con la misma cuenta.",
                            fontSize = 10.sp, color = InkTertiary, lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Spacer(Modifier.height(24.dp))
    SectionLabel(text)
    Spacer(Modifier.height(12.dp))
}

// ============================ Oferta del día ============================

@Composable
private fun DailyOfferCard(
    offer: DailyOffer, coins: Int, gems: Int, ownedSkins: Set<String>, onBuy: () -> Unit
) {
    val shape = RoundedCornerShape(Radius.XLarge)
    val title: String
    val original: Price
    val owned: Boolean
    when (val item = offer.item) {
        is OfferItem.SkinOffer -> { title = skinName(item.skin); original = ShopPrices.skin(item.skin); owned = item.skin.id in ownedSkins }
        is OfferItem.ChestOffer -> { title = chestName(item.type); original = (ShopPrices.chestCoins(item.type) ?: ShopPrices.chestGems(item.type))!!; owned = false }
    }
    val price = original.discounted(offer.discountPercent)
    val canAfford = coins >= price.coins && gems >= price.gems

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, shape, spotColor = Terracotta)
            .clip(shape)
            .background(Brush.linearGradient(listOf(Terracotta, Terracotta.darker(0.8f))))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("OFERTA DEL DÍA", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.85f), letterSpacing = 3.sp, modifier = Modifier.weight(1f))
            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text("-${offer.discountPercent}%", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Terracotta)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            when (val item = offer.item) {
                is OfferItem.SkinOffer -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(2, 16, 128).forEach { v -> SkinTilePreview(item.skin, v) }
                }
                is OfferItem.ChestOffer -> Box(Modifier.size(78.dp)) {
                    com.korkoor.pardos.ui.design.TreasureChest(item.type, com.korkoor.pardos.ui.design.ChestState.READY, Modifier.fillMaxSize())
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text("Cambia cada día", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
            }
        }
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth().height(50.dp)
                .clip(RoundedCornerShape(Radius.Medium))
                .background(if (owned) Color.White.copy(alpha = 0.25f) else Color.White)
                .clickable(enabled = !owned && canAfford, onClick = onBuy),
            contentAlignment = Alignment.Center
        ) {
            CozyText(
                text = when {
                    owned -> "YA LA TIENES"
                    price.gems > 0 -> "${price.gems} ◆   (antes ${original.gems})"
                    else -> "${price.coins} ●   (antes ${original.coins})"
                },
                fontSize = 13.sp, fontWeight = FontWeight.Black,
                color = if (owned) Color.White else if (canAfford) Terracotta else Navy.copy(alpha = 0.35f),
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun StarterPackCard(price: String?, onBuy: () -> Unit) {
    val shape = RoundedCornerShape(Radius.XLarge)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Gold, shape)
            .clip(shape)
            .background(Gold.copy(alpha = 0.10f))
            .clickable(enabled = price != null, onClick = onBuy)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(Icons.Rounded.CardGiftcard, Gold, size = 50.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("PACK INICIAL", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Navy, letterSpacing = 1.5.sp)
                Text("Una sola vez · el mejor valor de la tienda", fontSize = 11.sp, color = InkSecondary)
            }
            Box(Modifier.clip(RoundedCornerShape(14.dp)).background(Gold).padding(horizontal = 14.dp, vertical = 9.dp)) {
                Text(price ?: "Pronto", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("${Economy.STARTER_GEMS} gemas", "${Economy.STARTER_RARE_CHESTS} cofres raros", "Skin Cerezo").forEach {
                Text(
                    it, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Navy,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color.White).padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun VipCard(owned: Boolean, price: String?, onBuy: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF4B4F73), Navy)))
            .clickable(enabled = !owned && price != null, onClick = onBuy)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(54.dp).background(Color.White.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = Color(0xFFF2CC8F), modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("PASE VIP", fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 2.sp)
                Spacer(Modifier.height(4.dp))
                Text("Poderes con anuncio gratis y x2 monedas sin ver anuncios. Para siempre.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f), lineHeight = 16.sp)
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (owned) Sage else Color(0xFFF2CC8F)).padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(if (owned) "ACTIVO" else price ?: "Pronto", fontSize = 13.sp, fontWeight = FontWeight.Black, color = if (owned) Color.White else Navy)
            }
        }
    }
}

// ============================ Cofres ============================

@Composable
private fun ChestRow(
    type: ChestType, owned: Int, coinPrice: Price?, gemPrice: Price?,
    coins: Int, gems: Int, onBuyCoins: () -> Unit, onBuyGems: () -> Unit
) {
    val color = chestColor(type)
    PardosCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(Radius.Large), elevation = 5.dp) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp)) {
                com.korkoor.pardos.ui.design.TreasureChest(type, com.korkoor.pardos.ui.design.ChestState.READY, Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(chestName(type), fontSize = 15.sp, fontWeight = FontWeight.Black, color = Navy)
                Text(
                    when (type) {
                        ChestType.COMMON -> "2 cartas"
                        ChestType.RARE -> "3 cartas · 1 rara o mejor"
                        ChestType.EPIC -> "4 cartas · 1 épica o mejor"
                    } + " · Tienes $owned",
                    fontSize = 11.sp, color = InkSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                coinPrice?.let { PriceChip("${it.coins}", Icons.Rounded.MonetizationOn, Gold, coins >= it.coins, onBuyCoins) }
                gemPrice?.let { PriceChip("${it.gems}", Icons.Rounded.Diamond, GemBlue, gems >= it.gems, onBuyGems) }
            }
        }
    }
}

@Composable
private fun PriceChip(text: String, icon: ImageVector, color: Color, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) color.copy(alpha = 0.16f) else Navy.copy(alpha = 0.06f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) color else Navy.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Black, color = if (enabled) Navy else Navy.copy(alpha = 0.35f))
    }
}

// ============================ Skins ============================

@Composable
private fun SkinCard(
    skin: TileSkin, owned: Boolean, equipped: Boolean, canAfford: Boolean,
    modifier: Modifier, onClick: () -> Unit,
    status: String? = null, statusColor: Color? = null
) {
    val shape = RoundedCornerShape(24.dp)
    val st = skin.style
    Column(
        modifier = modifier
            .clip(shape)
            .background(Color.White)
            .border(if (equipped) 2.dp else 1.dp, if (equipped) Sage else Navy.copy(alpha = 0.08f), shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Vista previa con el FONDO real de la skin
        Box(
            modifier = Modifier
                .fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(
                    if (st.changesTheme) Brush.verticalGradient(listOf(Color(st.bgTop!!), Color(st.bgBottom!!)))
                    else Brush.verticalGradient(listOf(Color(0xFFF9F9F9), Color(0xFFF3F0E9)))
                )
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf(2, 16, 128).forEach { v -> SkinTilePreview(skin, v) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(skinName(skin), fontSize = 14.sp, fontWeight = FontWeight.Black, color = Navy)
        Text(
            when (skin.rarity) { SkinRarity.COMMON -> "COMÚN"; SkinRarity.RARE -> "RARA"; SkinRarity.EPIC -> "ÉPICA"; SkinRarity.LEGENDARY -> "LEGENDARIA" },
            fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp,
            color = when (skin.rarity) { SkinRarity.COMMON -> InkTertiary; SkinRarity.RARE -> GemBlue; SkinRarity.EPIC -> Violet; SkinRarity.LEGENDARY -> Gold }
        )
        Spacer(Modifier.height(8.dp))

        val label: String
        val bg: Color
        val fg: Color
        when {
            equipped -> { label = "EQUIPADA"; bg = Sage; fg = Color.White }
            owned -> { label = "EQUIPAR"; bg = Sage.copy(alpha = 0.16f); fg = Sage }
            status != null -> { label = status; bg = (statusColor ?: Navy).copy(alpha = if (statusColor != null) 0.16f else 0.06f); fg = statusColor ?: InkTertiary }
            skin.source == SkinSource.SEASON -> { label = "PASE PREMIUM"; bg = Violet.copy(alpha = 0.16f); fg = Violet }
            skin.exclusive -> { label = "ÁLBUM COMPLETO"; bg = Gold.copy(alpha = 0.18f); fg = Gold }
            skin.gemPrice > 0 -> { label = "${skin.gemPrice} ◆"; bg = if (canAfford) GemBlue.copy(alpha = 0.16f) else Navy.copy(alpha = 0.06f); fg = if (canAfford) GemBlue else Navy.copy(alpha = 0.35f) }
            else -> { label = "${skin.coinPrice} ●"; bg = if (canAfford) Gold.copy(alpha = 0.18f) else Navy.copy(alpha = 0.06f); fg = if (canAfford) Gold else Navy.copy(alpha = 0.35f) }
        }
        Box(Modifier.clip(RoundedCornerShape(14.dp)).background(bg).padding(horizontal = 14.dp, vertical = 7.dp)) {
            CozyText(label, fontSize = 11.sp, fontWeight = FontWeight.Black, color = fg, letterSpacing = 1.sp)
        }
    }
}

/** Skin secreta aún sin descubrir: silueta misteriosa y una pista poética. */
@Composable
private fun HiddenSkinCard(hint: String, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Color.White)
            .border(1.dp, Navy.copy(alpha = 0.08f), shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF14142B), Color(0xFF3A2A5C))))
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(3) {
                    Box(
                        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.07f))
                            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text("?", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.55f)) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("SECRETA", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Navy, letterSpacing = 2.sp)
        Spacer(Modifier.height(4.dp))
        Text(hint, fontSize = 10.sp, color = InkSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 13.sp, maxLines = 4)
    }
}

@Composable
fun SkinTilePreview(skin: TileSkin, value: Int, size: androidx.compose.ui.unit.Dp = 36.dp) {
    val look = tileLook(skin, value, GameTheme.Zen)
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .size(size)
            .then(if (look.glow != null) Modifier.shadow(6.dp, shape, spotColor = look.glow, ambientColor = look.glow) else Modifier)
            .clip(shape)
            .background(look.background)
            .then(if (look.border != null) Modifier.border(1.5.dp, look.border, shape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (look.gloss) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.30f), 0.5f to Color.Transparent)))
        }
        Text("$value", fontSize = if (value >= 100) 11.sp else 14.sp, fontWeight = FontWeight.Black, color = look.text)
    }
}

// ============================ Gemas / filas ============================

@Composable
private fun GemPackCard(label: String, gems: Int, price: String?, highlight: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Color.White)
            .border(if (highlight) 2.dp else 1.dp, if (highlight) GemBlue else Navy.copy(alpha = 0.08f), shape)
            .clickable(enabled = price != null, onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (highlight) Text("MEJOR VALOR", fontSize = 8.sp, fontWeight = FontWeight.Black, color = GemBlue, letterSpacing = 1.sp) else Spacer(Modifier.height(10.dp))
        Spacer(Modifier.height(6.dp))
        Box(Modifier.size(52.dp).background(GemBlue.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Diamond, contentDescription = null, tint = GemBlue, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text("$gems", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Navy)
        Text(label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Navy.copy(alpha = 0.45f), letterSpacing = 1.sp)
        Spacer(Modifier.height(12.dp))
        Box(Modifier.clip(RoundedCornerShape(14.dp)).background(if (price != null) Sage else Navy.copy(alpha = 0.08f)).padding(horizontal = 12.dp, vertical = 7.dp)) {
            Text(price ?: "Pronto", fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (price != null) Color.White else Navy.copy(alpha = 0.4f), maxLines = 1)
        }
    }
}

@Composable
private fun ShopRow(
    icon: ImageVector, color: Color, title: String, subtitle: String,
    priceText: String, priceIcon: ImageVector, priceColor: Color, enabled: Boolean, onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(icon, color, size = 46.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Navy)
            Text(subtitle, fontSize = 11.sp, color = InkSecondary, lineHeight = 14.sp)
        }
        Spacer(Modifier.width(10.dp))
        PriceChip(priceText, priceIcon, priceColor, enabled, onClick)
    }
}


// ============================ Promos ============================

@Composable
private fun PromoCard(icon: ImageVector, color: Color, title: String, tag: String, text: String, action: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(Radius.XLarge)
    Row(
        modifier = Modifier.fillMaxWidth().clip(shape).background(color.copy(alpha = 0.10f))
            .border(1.5.dp, color.copy(alpha = 0.5f), shape).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(icon, color, size = 50.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(tag, fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.sp, maxLines = 1, softWrap = false,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(color).padding(horizontal = 6.dp, vertical = 2.dp))
            Spacer(Modifier.height(3.dp))
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Navy, letterSpacing = 1.sp, maxLines = 1)
            Text(text, fontSize = 11.sp, color = InkSecondary, lineHeight = 14.sp)
        }
        Spacer(Modifier.width(8.dp))
        Text(action, fontSize = 12.sp, fontWeight = FontWeight.Black, color = color)
    }
}

/** Studio: la skin de pago que el jugador diseña. La vista previa usa los colores de SU diseño actual. */
@Composable
private fun StudioPromo(owned: Boolean, price: String?, config: StudioConfig, onClick: () -> Unit) {
    val shape = RoundedCornerShape(Radius.XLarge)
    val live = remember(config) { config.toStyle() }
    val inkColor = Color(live.ink ?: 0xFFFFFFFF)
    val bg = Brush.linearGradient(listOf(Color(live.bgTop ?: 0xFF3D405B), Color(live.bgBottom ?: 0xFF6C63FF)))
    Column(
        modifier = Modifier.fillMaxWidth().shadow(8.dp, shape, spotColor = Violet).clip(shape).background(bg).clickable(onClick = onClick).padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("STUDIO", fontSize = 18.sp, fontWeight = FontWeight.Black, color = inkColor, letterSpacing = 3.sp)
                Text("Diseña tus propias fichas: acabado, colores, fondo y partículas", fontSize = 11.sp, color = inkColor.copy(alpha = 0.8f), lineHeight = 14.sp)
            }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White).padding(horizontal = 14.dp, vertical = 9.dp)) {
                Text(if (owned) "EDITAR" else price ?: "PROBAR", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Navy)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(2, 8, 32, 128, 512).forEach { v ->
                val look = tileLook(live, v, GameTheme.Zen)
                val sh = RoundedCornerShape(10.dp)
                Box(
                    Modifier.size(40.dp).clip(sh).background(look.background).then(if (look.border != null) Modifier.border(1.5.dp, look.border, sh) else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    if (look.gloss) Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.30f), 0.5f to Color.Transparent)))
                    Text("$v", fontSize = if (v >= 100) 11.sp else 14.sp, fontWeight = FontWeight.Black, color = look.text)
                }
            }
        }
    }
}

@Composable
private fun PiggyShopCard(gems: Int, price: String?, onBreak: () -> Unit) {
    val shape = RoundedCornerShape(Radius.Large)
    Row(
        modifier = Modifier.fillMaxWidth().clip(shape).background(Color.White).border(1.dp, GemBlue.copy(alpha = 0.35f), shape).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(Icons.Rounded.Savings, GemBlue, size = 50.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("HUCHA", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Navy, letterSpacing = 1.sp)
            Text("Guarda $gems gemas ganadas jugando · rómpela cuando quieras", fontSize = 11.sp, color = InkSecondary, lineHeight = 14.sp)
        }
        Spacer(Modifier.width(8.dp))
        val can = com.korkoor.pardos.domain.retention.PiggyBank.canBreak(gems) && price != null
        Box(
            Modifier.clip(RoundedCornerShape(14.dp)).background(if (can) GemBlue else Navy.copy(alpha = 0.08f)).clickable(enabled = can, onClick = onBreak)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) { Text(price ?: "Pronto", fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (can) Color.White else Navy.copy(alpha = 0.4f)) }
    }
}
