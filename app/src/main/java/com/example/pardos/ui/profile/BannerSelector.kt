package com.korkoor.pardos.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.korkoor.pardos.data.local.EconomyManager
import com.korkoor.pardos.domain.shop.BannerSource
import com.korkoor.pardos.domain.shop.Banners
import com.korkoor.pardos.ui.design.*

/**
 * Cabecera del perfil: el banner elegido con el avatar (y su marco) asomando por abajo.
 * Se usa igual en el perfil y en la vista previa del selector.
 */
@Composable
fun ProfileHero(
    bannerId: Int, avatarId: Int, modifier: Modifier = Modifier,
    bannerHeight: androidx.compose.ui.unit.Dp = 132.dp, avatarSize: androidx.compose.ui.unit.Dp = 104.dp,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
    onAvatarClick: (() -> Unit)? = null,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    Box(modifier = modifier.fillMaxWidth().height(bannerHeight + avatarSize / 2)) {
        ProfileBanner(bannerId, Modifier.fillMaxWidth().height(bannerHeight).clip(shape))
        Box(Modifier.fillMaxWidth().height(bannerHeight)) { overlay() }
        AvatarFramed(
            avatarId,
            Modifier.align(Alignment.BottomCenter).size(avatarSize).shadow(10.dp, CircleShape)
                .then(if (onAvatarClick != null) Modifier.clickable(onClick = onAvatarClick) else Modifier),
            ring = 5.dp
        )
    }
}

@Composable
fun BannerSelectorDialog(
    currentBannerId: Int,
    avatarId: Int,
    onBannerSelected: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val economy = remember { EconomyManager(context) }
    val coins by economy.coins.collectAsState()
    val gems by economy.gems.collectAsState()
    val owned by economy.ownedBanners.collectAsState()
    var preview by remember { mutableIntStateOf(currentBannerId) }
    var message by remember { mutableStateOf<String?>(null) }

    val def = Banners.byId(preview)
    val isOwned = Banners.isOwned(preview, owned)

    Dialog(onDismissRequest = onDismissRequest) {
        JellySurface(shape = RoundedCornerShape(32.dp), color = Cream, shadowElevation = 24.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TU BANNER", fontSize = 11.sp, fontWeight = FontWeight.Black, color = InkSecondary, letterSpacing = 2.sp)
                Spacer(Modifier.height(10.dp))

                // vista previa real: así se verá en tu perfil
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White)) {
                    ProfileHero(
                        bannerId = preview, avatarId = avatarId,
                        bannerHeight = 96.dp, avatarSize = 64.dp, shape = RoundedCornerShape(24.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(def.name, fontSize = 17.sp, fontWeight = FontWeight.Black, color = Navy)
                Text(
                    when {
                        isOwned && preview == currentBannerId -> "EN USO"
                        isOwned -> "TUYO"
                        def.source == BannerSource.SEASON -> "EXCLUSIVO DEL PASE"
                        def.gemPrice > 0 -> "${def.gemPrice} GEMAS"
                        else -> "${def.coinPrice} MONEDAS"
                    },
                    fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp,
                    color = when { isOwned -> Sage; def.source == BannerSource.SEASON -> Violet; def.gemPrice > 0 -> GemBlue; else -> Gold }
                )

                Spacer(Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.height(250.dp)
                ) {
                    items(Banners.all) { b ->
                        val has = Banners.isOwned(b.id, owned)
                        val selected = preview == b.id
                        Box(
                            Modifier.fillMaxWidth().height(70.dp).clip(RoundedCornerShape(18.dp))
                                .border(if (selected) 3.dp else 1.dp, if (selected) Navy else Navy.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
                                .clickable { preview = b.id; message = null }
                        ) {
                            ProfileBanner(b, Modifier.fillMaxSize(), animate = false)
                            if (!has) Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.45f)))
                            Text(
                                b.name, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(b.ink), maxLines = 1,
                                modifier = Modifier.align(Alignment.BottomStart).padding(start = 9.dp, bottom = 6.dp)
                            )
                            val badge: @Composable () -> Unit = {
                                when {
                                    b.id == currentBannerId -> Pin(Sage) { androidx.compose.material3.Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(12.dp)) }
                                    !has && b.source == BannerSource.SEASON -> Pin(Violet) { androidx.compose.material3.Icon(Icons.Rounded.WorkspacePremium, null, tint = Color.White, modifier = Modifier.size(12.dp)) }
                                    !has && b.gemPrice > 0 -> Pin(GemBlue) { androidx.compose.material3.Icon(Icons.Rounded.Diamond, null, tint = Color.White, modifier = Modifier.size(12.dp)) }
                                    !has -> Pin(Navy.copy(alpha = 0.75f)) { androidx.compose.material3.Icon(Icons.Rounded.Lock, null, tint = Color.White, modifier = Modifier.size(11.dp)) }
                                }
                            }
                            Box(Modifier.align(Alignment.TopEnd).padding(6.dp)) { badge() }
                        }
                    }
                }

                message?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Terracotta, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(12.dp))

                when {
                    isOwned -> PrimaryButton(
                        if (preview == currentBannerId) "LISTO" else "USAR ESTE BANNER",
                        onClick = { if (preview != currentBannerId) onBannerSelected(preview); onDismissRequest() }, height = 52.dp
                    )
                    def.source == BannerSource.SEASON -> PrimaryButton(
                        "SE GANA EN EL PASE DE TEMPORADA", onClick = { message = "Míralo en el Pase de temporada: sale como premio." },
                        enabled = false, height = 52.dp, fontSize = 11.sp
                    )
                    else -> {
                        val can = coins >= def.coinPrice && gems >= def.gemPrice
                        PrimaryButton(
                            if (def.gemPrice > 0) "COMPRAR · ${def.gemPrice} GEMAS" else "COMPRAR · ${def.coinPrice} MONEDAS",
                            onClick = {
                                when (economy.buyBanner(def.id)) {
                                    is Banners.Purchase.Ok -> { onBannerSelected(def.id); onDismissRequest() }
                                    Banners.Purchase.NotEnoughCoins -> message = "Te faltan monedas."
                                    Banners.Purchase.NotEnoughGems -> message = "Te faltan gemas."
                                    else -> Unit
                                }
                            },
                            enabled = can, height = 52.dp
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("Cerrar", fontSize = 12.sp, fontWeight = FontWeight.Black, color = InkSecondary, modifier = Modifier.clickable(onClick = onDismissRequest).padding(10.dp))
            }
        }
    }
}

@Composable
private fun Pin(color: Color, content: @Composable () -> Unit) {
    Box(
        Modifier.size(20.dp).shadow(3.dp, CircleShape).background(color, CircleShape).border(1.5.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) { content() }
}
