package com.korkoor.pardos.ui.shop

import com.korkoor.pardos.ui.design.*

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.korkoor.pardos.ui.theme.GameTheme
import com.korkoor.pardos.ui.game.components.tileLook
import com.korkoor.pardos.domain.shop.SkinInventory
import com.korkoor.pardos.domain.shop.TileSkin
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.data.billing.BillingManager
import com.korkoor.pardos.data.local.EconomyManager
import com.korkoor.pardos.domain.shop.CoinShop
import com.korkoor.pardos.domain.shop.ShopCatalog
import com.korkoor.pardos.ui.menu.CurrencyPill


@Composable
fun ShopScreen(
    activity: Activity,
    billing: BillingManager,
    economy: EconomyManager,
    onBack: () -> Unit
) {
    val coins by economy.coins.collectAsState()
    val gems by economy.gems.collectAsState()
    val freezes by economy.streakFreezes.collectAsState()
    val isVip by economy.isVip.collectAsState()
    val equippedSkin by economy.equippedSkin.collectAsState()
    val ownedSkins by economy.ownedSkins.collectAsState()
    val prices by billing.prices.collectAsState()
    val message by billing.message.collectAsState()
    var localMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { billing.connect() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF3EFE6), Cream)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 32.dp)
        ) {
            // Barra superior
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(onClick = onBack, shape = CircleShape, color = Color.White, shadowElevation = 6.dp, modifier = Modifier.size(46.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = Navy)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Text("TIENDA", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Navy, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                CurrencyPill(Icons.Rounded.MonetizationOn, coins, Gold)
                Spacer(Modifier.width(8.dp))
                CurrencyPill(Icons.Rounded.Diamond, gems, GemBlue)
            }

            (message ?: localMessage)?.let { msg ->
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Sage.copy(alpha = 0.15f))
                        .clickable { billing.clearMessage(); localMessage = null }
                        .padding(14.dp)
                ) {
                    Text(msg, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Navy)
                }
            }

            // --- VIP ---
            Spacer(Modifier.height(22.dp))
            VipCard(
                owned = isVip,
                price = prices[ShopCatalog.VIP_FOREVER],
                onBuy = { billing.purchase(activity, ShopCatalog.VIP_FOREVER) }
            )

            // --- SKINS DE FICHAS ---
            SectionTitle("SKINS DE FICHAS")
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 4.dp)
            ) {
                items(TileSkin.entries.toList()) { skin ->
                    val owned = skin.isFree || skin.id in ownedSkins
                    SkinCard(
                        skin = skin,
                        owned = owned,
                        equipped = skin == equippedSkin,
                        canAfford = coins >= skin.coinPrice && gems >= skin.gemPrice,
                        onClick = {
                            if (owned) {
                                economy.equipSkin(skin)
                                localMessage = "Skin equipada: ${skinName(skin)}"
                            } else {
                                when (economy.buySkin(skin)) {
                                    is SkinInventory.Purchase.Ok -> {
                                        economy.equipSkin(skin)
                                        localMessage = "¡${skinName(skin)} desbloqueada y equipada!"
                                    }
                                    SkinInventory.Purchase.NotEnoughCoins -> localMessage = "Te faltan monedas"
                                    SkinInventory.Purchase.NotEnoughGems -> localMessage = "Te faltan gemas"
                                    SkinInventory.Purchase.AlreadyOwned -> Unit
                                    SkinInventory.Purchase.NotPurchasable -> localMessage = "Esta skin es exclusiva"
                                }
                            }
                        }
                    )
                }
            }

            // --- GEMAS ---
            SectionTitle("GEMAS")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    ShopCatalog.GEMS_SMALL to "Puñado",
                    ShopCatalog.GEMS_MEDIUM to "Bolsa",
                    ShopCatalog.GEMS_LARGE to "Cofre"
                ).forEach { (id, label) ->
                    val product = ShopCatalog.byId(id)!!
                    GemPackCard(
                        label = label,
                        gems = product.gems,
                        price = prices[id],
                        highlight = id == ShopCatalog.GEMS_MEDIUM,
                        modifier = Modifier.weight(1f),
                        onClick = { billing.purchase(activity, id) }
                    )
                }
            }

            // --- CON MONEDAS ---
            SectionTitle("CON MONEDAS")
            ShopRow(
                icon = Icons.Rounded.Shield,
                color = Sage,
                title = "Escudo de racha",
                subtitle = "Salva tu racha si faltas un día · Tienes $freezes/${CoinShop.MAX_STREAK_FREEZES}",
                priceText = "${CoinShop.STREAK_FREEZE_PRICE_COINS}",
                priceIcon = Icons.Rounded.MonetizationOn,
                priceColor = Gold,
                enabled = CoinShop.canBuyStreakFreeze(coins, freezes),
                onClick = {
                    localMessage = if (economy.buyStreakFreeze()) "¡Escudo de racha añadido!" else "No se pudo comprar"
                }
            )
            Spacer(Modifier.height(10.dp))
            ShopRow(
                icon = Icons.Rounded.SwapHoriz,
                color = GemBlue,
                title = "Cambiar gemas",
                subtitle = "5 gemas = ${CoinShop.coinsForGems(5)} monedas",
                priceText = "5",
                priceIcon = Icons.Rounded.Diamond,
                priceColor = GemBlue,
                enabled = gems >= 5,
                onClick = {
                    localMessage = if (economy.exchangeGems(5)) "+${CoinShop.coinsForGems(5)} monedas" else "No tienes gemas suficientes"
                }
            )

            Spacer(Modifier.height(18.dp))
            Text(
                text = "Las compras se procesan con Google Play. Las gemas y el VIP se recuperan al reinstalar con la misma cuenta.",
                fontSize = 10.sp,
                color = Navy.copy(alpha = 0.4f),
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Spacer(Modifier.height(26.dp))
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Navy.copy(alpha = 0.5f), letterSpacing = 3.sp)
    Spacer(Modifier.height(12.dp))
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
            Box(
                modifier = Modifier.size(54.dp).background(Color.White.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = Color(0xFFF2CC8F), modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("PASE VIP", fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 2.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Poderes Escoba y Fusión manual gratis, sin anuncios. Para siempre.",
                    fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f), lineHeight = 16.sp
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (owned) Sage else Color(0xFFF2CC8F))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = if (owned) "ACTIVO" else price ?: "—",
                    fontSize = 13.sp, fontWeight = FontWeight.Black,
                    color = if (owned) Color.White else Navy
                )
            }
        }
    }
}

@Composable
private fun GemPackCard(
    label: String,
    gems: Int,
    price: String?,
    highlight: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
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
        if (highlight) {
            Text("POPULAR", fontSize = 8.sp, fontWeight = FontWeight.Black, color = GemBlue, letterSpacing = 1.sp)
        } else {
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier.size(52.dp).background(GemBlue.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Diamond, contentDescription = null, tint = GemBlue, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text("$gems", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Navy)
        Text(label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Navy.copy(alpha = 0.45f), letterSpacing = 1.sp)
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(if (price != null) Sage else Navy.copy(alpha = 0.08f))
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text(
                text = price ?: "Pronto",
                fontSize = 11.sp, fontWeight = FontWeight.Black,
                color = if (price != null) Color.White else Navy.copy(alpha = 0.4f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ShopRow(
    icon: ImageVector,
    color: Color,
    title: String,
    subtitle: String,
    priceText: String,
    priceIcon: ImageVector,
    priceColor: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(46.dp).background(color.copy(alpha = 0.14f), RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Navy)
            Text(subtitle, fontSize = 11.sp, color = Navy.copy(alpha = 0.55f), lineHeight = 14.sp)
        }
        Spacer(Modifier.width(10.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (enabled) priceColor.copy(alpha = 0.16f) else Navy.copy(alpha = 0.06f))
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(priceIcon, contentDescription = null, tint = if (enabled) priceColor else Navy.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
            Text(priceText, fontSize = 13.sp, fontWeight = FontWeight.Black, color = if (enabled) Navy else Navy.copy(alpha = 0.35f))
        }
    }
}


private fun skinName(skin: TileSkin): String = mapOf(
    TileSkin.JELLY to "Gelatina", TileSkin.FLAT to "Papel", TileSkin.WOOD to "Madera",
    TileSkin.SAKURA to "Cerezo", TileSkin.FOREST to "Bosque", TileSkin.AUTUMN to "Otoño",
    TileSkin.GLASS to "Hielo", TileSkin.CANDY to "Dulces", TileSkin.NEON to "Neón",
    TileSkin.OCEAN to "Océano", TileSkin.SPACE to "Galaxia", TileSkin.GOLD to "Oro Real"
)[skin] ?: skin.id

@Composable
private fun SkinCard(
    skin: TileSkin,
    owned: Boolean,
    equipped: Boolean,
    canAfford: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .width(150.dp)
            .clip(shape)
            .background(Color.White)
            .border(if (equipped) 2.dp else 1.dp, if (equipped) Sage else Navy.copy(alpha = 0.08f), shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Vista previa sobre un mini tablero
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Navy.copy(alpha = 0.06f))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(2, 16, 128).forEach { v -> SkinTilePreview(skin, v) }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(skinName(skin), fontSize = 15.sp, fontWeight = FontWeight.Black, color = Navy)
        Spacer(Modifier.height(8.dp))

        val (label, bg, fg) = when {
            equipped -> Triple("EQUIPADA", Sage, Color.White)
            owned -> Triple("EQUIPAR", Sage.copy(alpha = 0.16f), Sage)
            skin.gemPrice > 0 -> Triple("${skin.gemPrice} ◆", if (canAfford) GemBlue.copy(alpha = 0.16f) else Navy.copy(alpha = 0.06f), if (canAfford) GemBlue else Navy.copy(alpha = 0.35f))
            else -> Triple("${skin.coinPrice} ●", if (canAfford) Gold.copy(alpha = 0.18f) else Navy.copy(alpha = 0.06f), if (canAfford) Gold else Navy.copy(alpha = 0.35f))
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(bg)
                .padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Black, color = fg, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun SkinTilePreview(skin: TileSkin, value: Int) {
    val look = tileLook(skin, value, GameTheme.Zen)
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .size(36.dp)
            .then(if (look.glow != null) Modifier.shadow(6.dp, shape, spotColor = look.glow, ambientColor = look.glow) else Modifier)
            .clip(shape)
            .background(look.background)
            .then(if (look.border != null) Modifier.border(1.5.dp, look.border, shape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (look.gloss) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0f to Color.White.copy(alpha = 0.30f), 0.5f to Color.Transparent)
                )
            )
        }
        Text("$value", fontSize = if (value >= 100) 11.sp else 14.sp, fontWeight = FontWeight.Black, color = look.text)
    }
}
