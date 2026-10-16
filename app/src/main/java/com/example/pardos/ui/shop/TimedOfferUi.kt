package com.korkoor.pardos.ui.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.data.local.LocalDay
import com.korkoor.pardos.data.local.TimedOfferStore
import com.korkoor.pardos.domain.shop.OfferContents
import com.korkoor.pardos.domain.shop.StruggleHelp
import com.korkoor.pardos.domain.shop.TimedOffer
import com.korkoor.pardos.domain.shop.TimedOffers
import com.korkoor.pardos.ui.design.GemBlue
import com.korkoor.pardos.ui.design.InkSecondary
import com.korkoor.pardos.ui.design.InkTertiary
import com.korkoor.pardos.ui.design.JellySurface
import com.korkoor.pardos.ui.design.Navy
import com.korkoor.pardos.ui.rewards.CardDialog
import com.korkoor.pardos.ui.rewards.DialogButton
import kotlinx.coroutines.delay

private fun OfferContents.lines(): List<String> = buildList {
    if (undos > 0) add("$undos × Deshacer")
    if (extraTimes > 0) add("$extraTimes × Tiempo extra")
    if (freezes > 0) add("$freezes × Escudo de racha")
    if (rareChests > 0) add(if (rareChests == 1) "1 cofre raro" else "$rareChests cofres raros")
}

private fun formatLeft(ms: Long): String {
    val totalMin = ms / 60_000L
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h >= 1) "${h} h ${m} min" else "${m} min"
}

/**
 * Ofertas de tiempo limitado de la tienda. Salen en días fijos, el reloj es el real, se pagan con gemas y cada una se compra una vez.
 * Si hoy no hay ninguna, no se dibuja nada.
 */
@Composable
fun TimedOfferCards(gems: Int, onMessage: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember(context) { TimedOfferStore(context) }
    val offers = remember { TimedOffers.activeOn(LocalDay.today().toLong()) }
    if (offers.isEmpty()) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        offers.forEach { offer -> OfferCard(offer, store, gems, onMessage) }
    }
}

@Composable
private fun OfferCard(offer: TimedOffer, store: TimedOfferStore, gems: Int, onMessage: (String) -> Unit) {
    var bought by remember(offer.windowId) { androidx.compose.runtime.mutableStateOf(store.isBought(offer)) }
    var left by remember { mutableLongStateOf(store.remainingMs(offer)) }
    LaunchedEffect(offer.windowId) {
        while (true) {
            left = store.remainingMs(offer)
            delay(30_000)
        }
    }
    JellySurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color.White) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("OFERTA POR TIEMPO LIMITADO", fontSize = 10.sp, fontWeight = FontWeight.Black, color = InkTertiary, letterSpacing = 2.sp)
                    Text(offer.title, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Navy)
                }
                Text(if (left > 0) "Termina en ${formatLeft(left)}" else "Terminó", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InkSecondary)
            }
            Spacer(Modifier.height(4.dp))
            Text(offer.blurb, fontSize = 12.sp, color = InkSecondary)
            Spacer(Modifier.height(8.dp))
            Text(offer.contents.lines().joinToString("  ·  "), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Navy)
            Spacer(Modifier.height(4.dp))
            Text(
                "Por separado: ${offer.contents.gemValue} ◆ · Aquí: ${offer.priceGems} ◆ (ahorras ${offer.savingPercent} %). Solo se compra una vez.",
                fontSize = 11.sp, color = InkTertiary
            )
            Spacer(Modifier.height(10.dp))
            when {
                bought -> Text("Ya la compraste. ¡Que la disfrutes!", fontSize = 13.sp, fontWeight = FontWeight.Black, color = GemBlue)
                left <= 0 -> Text("Esta oferta ya terminó.", fontSize = 13.sp, color = InkTertiary)
                else -> DialogButton(
                    if (gems >= offer.priceGems) "COMPRAR · ${offer.priceGems} ◆" else "Te faltan ${offer.priceGems - gems} ◆",
                    GemBlue, enabled = gems >= offer.priceGems
                ) {
                    if (store.buy(offer)) { bought = true; onMessage("¡${offer.title} comprada!") } else onMessage("No se pudo comprar la oferta")
                }
            }
        }
    }
}

/**
 * Ayuda opcional tras varias derrotas seguidas en el mismo nivel. Precio normal (sin descuento inventado), una vez por nivel,
 * con la salida clara de "No, gracias" y recordando que el nivel se puede ganar sin esto.
 */
@Composable
fun StruggleHelpDialog(level: Int, gems: Int, onDismiss: () -> Unit, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { TimedOfferStore(context) }
    LaunchedEffect(level) { store.markHelpOffered(level) }
    CardDialog(onDismiss) {
        Text("¿UNA MANO?", fontSize = 11.sp, fontWeight = FontWeight.Black, color = InkTertiary, letterSpacing = 3.sp)
        Text("Este nivel se está resistiendo", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Navy, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Si quieres, puedes llevarte ${StruggleHelp.contents.lines().joinToString(" y ")} por su precio normal. " +
                "No hace falta: el nivel se puede ganar sin esto, y ver un anuncio al perder también te da otra oportunidad gratis.",
            fontSize = 13.sp, color = InkSecondary, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        DialogButton(
            if (gems >= StruggleHelp.priceGems) "Llevar la ayuda · ${StruggleHelp.priceGems} ◆" else "Te faltan ${StruggleHelp.priceGems - gems} ◆",
            GemBlue, enabled = gems >= StruggleHelp.priceGems
        ) {
            if (store.buyHelp()) onMessage("¡Ayuda lista! Suerte en el nivel") else onMessage("No se pudo comprar")
            onDismiss()
        }
        Spacer(Modifier.height(6.dp))
        DialogButton("No, gracias", Navy.copy(alpha = 0.6f), onClick = onDismiss)
    }
}
