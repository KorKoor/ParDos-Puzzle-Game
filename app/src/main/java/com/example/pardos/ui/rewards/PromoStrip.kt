package com.korkoor.pardos.ui.rewards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.data.local.AdFrequency
import com.korkoor.pardos.data.local.EconomyManager
import com.korkoor.pardos.data.local.LocalDay
import com.korkoor.pardos.domain.economy.Economy
import com.korkoor.pardos.domain.shop.AdRewards
import com.korkoor.pardos.domain.shop.DailyOffers
import com.korkoor.pardos.domain.shop.MenuPromo
import com.korkoor.pardos.domain.shop.OfferItem
import com.korkoor.pardos.domain.shop.PromoKind
import com.korkoor.pardos.domain.shop.VipPerks
import com.korkoor.pardos.ui.collection.chestName
import com.korkoor.pardos.ui.design.CozyText
import com.korkoor.pardos.ui.design.Gold
import com.korkoor.pardos.ui.design.GemBlue
import com.korkoor.pardos.ui.design.IconTile
import com.korkoor.pardos.ui.design.InkSecondary
import com.korkoor.pardos.ui.design.JellyRow
import com.korkoor.pardos.ui.design.Navy
import com.korkoor.pardos.ui.design.Radius
import com.korkoor.pardos.ui.design.Terracotta
import com.korkoor.pardos.ui.design.WatchAdButton
import com.korkoor.pardos.ui.design.lighten
import com.korkoor.pardos.ui.shop.skinName

/**
 * Tarjeta de ofertas del menú. Enseña una sola cosa a la vez y va rotando entre visitas: el pack inicial (hasta que lo compra),
 * el VIP (sin anuncios), la oferta del día de la tienda y las gemas gratis a cambio de un anuncio. Todo es verdad: sin cuentas
 * atrás inventadas, y lo que ya tiene el jugador no se le vuelve a ofrecer.
 */
@Composable
fun PromoStrip(
    campaignLevel: Int,
    starterPrice: String?,
    onBuyStarter: () -> Unit,
    onShop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val economy = remember { EconomyManager(context) }
    val adFreq = remember { AdFrequency(context) }
    val vip by economy.isVip.collectAsState()
    val visit = remember { adFreq.nextMenuVisit() }
    var tick by remember { mutableIntStateOf(0) }
    val freeGemsLeft = remember(tick) { AdRewards.left(adFreq.usedToday(AdFrequency.SLOT_FREE_GEMS), AdRewards.FREE_GEMS_PER_DAY) }
    // Se calcula una vez por visita: no cambia mientras el jugador mira el menú (excepto al cobrar las gemas)
    val kind = remember(visit, vip, campaignLevel) {
        MenuPromo.pick(MenuPromo.State(vip, economy.isStarterClaimed(), campaignLevel, freeGemsLeft, visit))
    }
    val showKind = if (kind == PromoKind.FREE_GEMS && freeGemsLeft == 0) PromoKind.DAILY_OFFER else kind

    Column(modifier = modifier.fillMaxWidth()) {
        when (showKind) {
            PromoKind.STARTER_PACK -> OfferRow(
                color = Gold, icon = Icons.Rounded.CardGiftcard, tag = "PACK INICIAL",
                title = "${Economy.STARTER_GEMS} ◆ + ${Economy.STARTER_RARE_CHESTS} cofres raros + skin",
                text = "Compra única · el mejor valor",
                action = starterPrice ?: "VER",
                onClick = { if (starterPrice != null) onBuyStarter() else onShop() }
            )
            PromoKind.VIP -> VipRow(onShop)
            PromoKind.DAILY_OFFER -> {
                val offer = remember { DailyOffers.forDayAvoiding(LocalDay.today(), economy.ownedSkins.value) }
                val name = when (val item = offer.item) {
                    is OfferItem.SkinOffer -> skinName(item.skin)
                    is OfferItem.ChestOffer -> chestName(item.type)
                }
                OfferRow(
                    color = Terracotta, icon = Icons.Rounded.LocalOffer, tag = "OFERTA DEL DÍA",
                    title = "$name  -${offer.discountPercent}%",
                    text = "Hoy · mañana cambia",
                    action = "VER", onClick = onShop
                )
            }
            PromoKind.FREE_GEMS -> FreeGemsRow(freeGemsLeft) {
                (context as? android.app.Activity)?.let { act ->
                    com.korkoor.pardos.ui.game.logic.AdManager.showRewardedAd(act) {
                        economy.addGems(AdRewards.FREE_GEMS)
                        adFreq.consume(AdFrequency.SLOT_FREE_GEMS)
                        com.korkoor.pardos.audio.GameAudio.play(com.korkoor.pardos.audio.Sfx.GEM)
                        tick++
                    }
                }
            }
        }
    }
}

@Composable
private fun OfferRow(color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, tag: String, title: String, text: String, action: String, onClick: () -> Unit) {
    JellyRow(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(Radius.XLarge), fill = color.lighten(0.90f), lip = color.copy(alpha = 0.45f), lipHeight = 6.dp,
        borderColor = color.copy(alpha = 0.5f),
        padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
        IconTile(icon, color, size = 46.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(tag, fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.sp, maxLines = 1, softWrap = false,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(color).padding(horizontal = 6.dp, vertical = 2.dp))
            Spacer(Modifier.height(3.dp))
            CozyText(title, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Navy, maxLines = 1)
            Text(text, fontSize = 11.sp, color = InkSecondary, lineHeight = 14.sp, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.clip(RoundedCornerShape(14.dp)).background(color).padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(action, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1)
        }
    }
}

@Composable
private fun VipRow(onShop: () -> Unit) {
    JellyRow(
        modifier = Modifier.fillMaxWidth(),
        onClick = onShop,
        shape = RoundedCornerShape(Radius.XLarge), fill = Navy, lip = Color(0xFF1E2036), lipHeight = 6.dp,
        brush = Brush.verticalGradient(listOf(Color(0xFF5E628C), Color(0xFF4B4F73), Navy)),
        padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Box(Modifier.size(46.dp).background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            com.korkoor.pardos.ui.design.Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = Color(0xFFF2CC8F), modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("JUEGA SIN ANUNCIOS", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.sp, maxLines = 1)
            CozyText("VIP: +${VipPerks.COIN_BONUS_PERCENT}% monedas y ${VipPerks.DAILY_GEMS} ◆ cada día. Para siempre.", fontSize = 11.sp, color = Color.White.copy(alpha = 0.78f), maxLines = 2)
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFFF2CC8F)).padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text("VER", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Navy)
        }
    }
}

@Composable
private fun FreeGemsRow(left: Int, onWatch: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        WatchAdButton(
            label = "+${AdRewards.FREE_GEMS} ◆ GRATIS",
            sublabel = "Ver un anuncio corto · te quedan $left hoy",
            onClick = onWatch,
            modifier = Modifier.fillMaxWidth(),
            color = GemBlue,
            tag = null,
            minHeight = 60.dp
        )
    }
}
