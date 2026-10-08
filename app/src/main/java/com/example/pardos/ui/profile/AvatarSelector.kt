package com.korkoor.pardos.ui.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.korkoor.pardos.R
import com.korkoor.pardos.data.local.EconomyManager
import com.korkoor.pardos.domain.shop.AvatarSource
import com.korkoor.pardos.domain.shop.Avatars
import com.korkoor.pardos.ui.design.*

fun getAvatarResource(avatarId: Int): Int {
    return when (avatarId) {
        1 -> R.drawable.avatar_1
        2 -> com.korkoor.pardos.R.drawable.avatar_2
        3 -> com.korkoor.pardos.R.drawable.avatar_3
        4 -> com.korkoor.pardos.R.drawable.avatar_4
        5 -> com.korkoor.pardos.R.drawable.avatar_5
        6 -> com.korkoor.pardos.R.drawable.avatar_6
        7 -> com.korkoor.pardos.R.drawable.avatar_7
        8 -> com.korkoor.pardos.R.drawable.avatar_8
        9 -> com.korkoor.pardos.R.drawable.avatar_9
        10 -> com.korkoor.pardos.R.drawable.avatar_10
        else -> R.drawable.avatar_1 // Por si las moscas
    }
}

/**
 * Selector de avatar: una vista previa grande arriba, la colección debajo y un botón que cambia según el caso
 * (usar, comprar con monedas o "se gana en el pase"). Los comprados quedan para siempre.
 */
@Composable
fun AvatarSelectorDialog(
    currentAvatarId: Int,
    onAvatarSelected: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val economy = remember { EconomyManager(context) }
    val coins by economy.coins.collectAsState()
    val owned by economy.ownedAvatars.collectAsState()
    var preview by remember { mutableIntStateOf(currentAvatarId) }
    var message by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableIntStateOf(0) }
    val filters = listOf("TODOS", "ANIMALES", "TEMPORADA", "CLÁSICOS")
    val shown = remember(filter) {
        when (filter) {
            1 -> Avatars.all.filter { it.animal != null && it.source == AvatarSource.SHOP }
            2 -> Avatars.all.filter { it.source == AvatarSource.SEASON }
            3 -> Avatars.classics
            else -> Avatars.all
        }
    }

    val def = Avatars.byId(preview)
    val isOwned = Avatars.isOwned(preview, owned)

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            shape = RoundedCornerShape(32.dp),
            color = Cream,
            shadowElevation = 24.dp
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("IDENTIDAD ZEN", fontSize = 11.sp, fontWeight = FontWeight.Black, color = InkSecondary, letterSpacing = 2.sp)
                Spacer(Modifier.height(10.dp))

                // vista previa
                AvatarFramed(preview, Modifier.size(116.dp).shadow(14.dp, CircleShape, spotColor = Navy), ring = 5.dp)
                Spacer(Modifier.height(8.dp))
                Text(if (def.animal == null) "Avatar clásico" else def.name, fontSize = 17.sp, fontWeight = FontWeight.Black, color = Navy)
                Text(
                    when {
                        isOwned && preview == currentAvatarId -> "EN USO"
                        isOwned -> "TUYO"
                        def.source == AvatarSource.SEASON -> "EXCLUSIVO DEL PASE"
                        else -> "${def.coinPrice} MONEDAS"
                    },
                    fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp,
                    color = when { isOwned -> Sage; def.source == AvatarSource.SEASON -> Violet; else -> Gold }
                )

                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    filters.forEachIndexed { i, label ->
                        val sel = i == filter
                        Text(
                            label, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.4.sp,
                            color = if (sel) Color.White else Navy, maxLines = 1, softWrap = false, textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (sel) Navy else Color.White)
                                .border(1.dp, if (sel) Navy else Navy.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .clickable { filter = i }.padding(vertical = 8.dp)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.height(280.dp)
                ) {
                    items(shown) { a ->
                        val has = Avatars.isOwned(a.id, owned)
                        val selected = preview == a.id
                        val sc by animateFloatAsState(if (selected) 1.12f else 1f, spring(dampingRatio = 0.5f), label = "av")
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.aspectRatio(1f)) {
                            Box(
                                Modifier.fillMaxSize().scale(sc)
                                    .shadow(if (selected) 8.dp else 0.dp, CircleShape)
                                    .clip(CircleShape)
                                    .border(if (selected) 3.dp else 0.dp, if (selected) Navy else Color.Transparent, CircleShape)
                                    .clickable { preview = a.id; message = null }
                            ) {
                                AvatarFramed(a.id, Modifier.fillMaxSize(), ring = 3.dp, animate = false)
                                if (!has) {
                                    // velo para lo que aún no es tuyo
                                    Box(Modifier.fillMaxSize().clip(CircleShape).background(Color.White.copy(alpha = 0.55f)))
                                }
                            }
                            // insignia: candado / pase / en uso
                            when {
                                a.id == currentAvatarId -> Badge(Sage) { Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(12.dp)) }
                                !has && a.source == AvatarSource.SEASON -> Badge(Violet) { Icon(Icons.Rounded.WorkspacePremium, null, tint = Color.White, modifier = Modifier.size(12.dp)) }
                                !has -> Badge(Navy.copy(alpha = 0.75f)) { Icon(Icons.Rounded.Lock, null, tint = Color.White, modifier = Modifier.size(11.dp)) }
                            }
                        }
                    }
                }

                message?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Terracotta, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(14.dp))

                when {
                    isOwned -> PrimaryButton(
                        if (preview == currentAvatarId) "LISTO" else "USAR ESTE AVATAR",
                        onClick = { if (preview != currentAvatarId) onAvatarSelected(preview); onDismissRequest() },
                        height = 52.dp
                    )
                    def.source == AvatarSource.SEASON -> PrimaryButton(
                        "SE GANA EN EL PASE DE TEMPORADA", onClick = { message = "Míralo en el Pase de temporada: sale como premio." },
                        enabled = false, height = 52.dp, fontSize = 11.sp
                    )
                    else -> PrimaryButton(
                        if (coins >= def.coinPrice) "COMPRAR · ${def.coinPrice} MONEDAS" else "TE FALTAN ${def.coinPrice - coins} MONEDAS",
                        onClick = {
                            when (economy.buyAvatar(def.id)) {
                                is Avatars.Purchase.Ok -> { onAvatarSelected(def.id); onDismissRequest() }
                                Avatars.Purchase.NotEnoughCoins -> message = "Te faltan monedas."
                                else -> Unit
                            }
                        },
                        enabled = coins >= def.coinPrice, height = 52.dp
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Cerrar", fontSize = 12.sp, fontWeight = FontWeight.Black, color = InkSecondary,
                    modifier = Modifier.clickable(onClick = onDismissRequest).padding(10.dp)
                )
            }
        }
    }
}

@Composable
private fun BoxScope.Badge(color: Color, content: @Composable () -> Unit) {
    Box(
        Modifier.align(Alignment.BottomEnd).size(20.dp).shadow(3.dp, CircleShape).background(color, CircleShape).border(1.5.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) { content() }
}
