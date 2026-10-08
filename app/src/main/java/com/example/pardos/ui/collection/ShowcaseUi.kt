package com.korkoor.pardos.ui.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.korkoor.pardos.data.local.CollectionManager
import com.korkoor.pardos.data.local.EconomyManager
import com.korkoor.pardos.domain.collection.Collectible
import com.korkoor.pardos.domain.collection.CollectibleCatalog
import com.korkoor.pardos.domain.collection.Copies
import com.korkoor.pardos.domain.collection.Showcase
import com.korkoor.pardos.ui.design.Cream
import com.korkoor.pardos.ui.design.Gold
import com.korkoor.pardos.ui.design.GemBlue
import com.korkoor.pardos.ui.design.Icon
import com.korkoor.pardos.ui.design.InkSecondary
import com.korkoor.pardos.ui.design.JellyColumn
import com.korkoor.pardos.ui.design.Navy
import com.korkoor.pardos.ui.design.PrimaryButton
import com.korkoor.pardos.ui.design.Radius
import com.korkoor.pardos.ui.design.SectionLabel
import com.korkoor.pardos.ui.design.popIn

/**
 * Tu vitrina de cartas en el perfil: hasta 9 cartas que luces ante tus amigos (empiezas con 3 huecos y abres más con gemas).
 * Es el lugar para presumir esa legendaria Brillante que tanto costó.
 */
@Composable
fun CardShowcase(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val manager = remember { CollectionManager(context) }
    val economy = remember { EconomyManager(context) }
    val showcase by manager.showcase.collectAsState()
    val slots by manager.showcaseSlots.collectAsState()
    val owned by manager.owned.collectAsState()
    val foil by manager.foil.collectAsState()
    val copies by manager.copies.collectAsState()
    val gems by economy.gems.collectAsState()
    var picking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(modifier.fillMaxWidth()) {
        SectionLabel("Mi vitrina de cartas", Modifier.padding(start = 4.dp))
        Spacer(Modifier.height(10.dp))
        val cells = (0 until slots).map { showcase.getOrNull(it) }
        val nextCost = if (slots < Showcase.MAX_SLOTS) Showcase.unlockCost(slots + 1) else 0
        val total = cells.size + if (nextCost > 0) 1 else 0
        for (r in 0 until (total + 2) / 3) {
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (cI in 0 until 3) {
                    val idx = r * 3 + cI
                    Box(Modifier.weight(1f)) {
                        when {
                            idx < cells.size -> {
                                val id = cells[idx]
                                val c = id?.let { CollectibleCatalog.byId(it) }
                                if (c != null) Box(Modifier.clickable { picking = true }) {
                                    CollectibleCard(c, Modifier.fillMaxWidth(), foil = c.id in foil, copies = 1, animate = true)
                                } else {
                                    Box(
                                        Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(14.dp)).background(Color.White)
                                            .border(2.dp, Navy.copy(alpha = 0.12f), RoundedCornerShape(14.dp)).clickable { picking = true },
                                        contentAlignment = Alignment.Center
                                    ) { Icon(Icons.Rounded.Add, null, tint = Navy.copy(alpha = 0.3f), modifier = Modifier.size(28.dp)) }
                                }
                            }
                            idx == cells.size && nextCost > 0 -> Column(
                                Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(14.dp)).background(Navy.copy(alpha = 0.05f))
                                    .border(2.dp, GemBlue.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                    .clickable {
                                        message = if (manager.unlockShowcaseSlot()) "¡Nuevo hueco en tu vitrina!" else "Te faltan ${nextCost - gems} gemas para abrir otro hueco."
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Rounded.Lock, null, tint = GemBlue, modifier = Modifier.size(20.dp))
                                Text("$nextCost ◆", fontSize = 13.sp, fontWeight = FontWeight.Black, color = GemBlue, modifier = Modifier.padding(top = 4.dp))
                                Text("más espacio", fontSize = 9.sp, color = InkSecondary)
                            }
                        }
                    }
                }
            }
        }
        message?.let { Text(it, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Navy.copy(alpha = 0.7f), modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)) }
        if (showcase.isEmpty()) {
            Text("Elige tus mejores cartas para que tus amigos las vean.", fontSize = 11.sp, color = InkSecondary, modifier = Modifier.padding(start = 4.dp))
        }
        Text(
            "ELEGIR CARTAS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(14.dp)).background(Navy).clickable { picking = true }.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }

    if (picking) {
        Dialog(onDismissRequest = { picking = false }) {
            JellyColumn(
                modifier = Modifier.popIn(), shape = RoundedCornerShape(Radius.XLarge), fill = Cream, lipHeight = 8.dp,
                padding = PaddingValues(16.dp), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ELIGE HASTA $slots CARTAS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = InkSecondary, letterSpacing = 2.sp)
                Text("${showcase.size} / $slots en la vitrina", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Navy, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
                val list = remember(owned, foil) {
                    owned.mapNotNull { CollectibleCatalog.byId(it) }
                        .sortedWith(compareByDescending<Collectible> { it.id in foil }.thenByDescending { it.rarity.ordinal }.thenBy { it.id })
                }
                LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(360.dp)) {
                    items(list, key = { it.id }) { c ->
                        val on = c.id in showcase
                        val full = !on && showcase.size >= slots
                        Box(
                            Modifier.clip(RoundedCornerShape(14.dp))
                                .border(if (on) 3.dp else 0.dp, if (on) Gold else Color.Transparent, RoundedCornerShape(14.dp))
                                .clickable(enabled = !full) { manager.setShowcase(if (on) showcase - c.id else showcase + c.id) }
                        ) {
                            CollectibleCard(c, Modifier.fillMaxWidth(), foil = c.id in foil, copies = Copies.count(copies, c.id), animate = false)
                            if (full) Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.55f)))
                        }
                    }
                }
                if (list.isEmpty()) Text("Aún no tienes cartas: abre cofres para empezar tu colección.", fontSize = 12.sp, color = InkSecondary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                PrimaryButton("LISTO", onClick = { picking = false }, height = 48.dp)
            }
        }
    }
}

/** Las cartas de la vitrina de un amigo, en pequeñito (para su fila en Amigos). */
@Composable
fun MiniShowcase(ids: List<String>, modifier: Modifier = Modifier) {
    val cards = ids.mapNotNull { CollectibleCatalog.byId(it) }.take(3)
    if (cards.isEmpty()) return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        cards.forEach { Box(Modifier.width(26.dp)) { CollectibleCard(it, Modifier.fillMaxWidth(), showName = false, animate = false) } }
    }
}
