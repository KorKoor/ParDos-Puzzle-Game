package com.korkoor.pardos.ui.rewards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Diamond
import com.korkoor.pardos.ui.design.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.data.local.EconomyManager
import com.korkoor.pardos.data.local.RetentionManager
import com.korkoor.pardos.domain.events.GameEvent
import com.korkoor.pardos.domain.shop.EventSkins
import com.korkoor.pardos.domain.shop.SkinRarity
import com.korkoor.pardos.domain.shop.TileSkin
import com.korkoor.pardos.ui.design.*
import com.korkoor.pardos.ui.game.components.AmbientParticles
import com.korkoor.pardos.ui.shop.SkinTilePreview

fun rarityLabel(r: SkinRarity): String = when (r) {
    SkinRarity.COMMON -> "COMÚN"; SkinRarity.RARE -> "RARA"; SkinRarity.EPIC -> "ÉPICA"; SkinRarity.LEGENDARY -> "LEGENDARIA"
}

fun rarityColor(r: SkinRarity): Color = when (r) {
    SkinRarity.COMMON -> InkTertiary; SkinRarity.RARE -> GemBlue; SkinRarity.EPIC -> Violet; SkinRarity.LEGENDARY -> Gold
}

/** Escaparate de una skin: su fondo real, sus partículas animadas y unas fichas de muestra. */
@Composable
fun SkinShowcase(skin: TileSkin, modifier: Modifier = Modifier, height: Dp = 170.dp) {
    val st = skin.style
    val top = st.bgTop?.let { Color(it) } ?: Color(0xFFF9F9F9)
    val bottom = st.bgBottom?.let { Color(it) } ?: Color(0xFFF3F0E9)
    Box(
        modifier = modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(top, bottom))),
        contentAlignment = Alignment.Center
    ) {
        AmbientParticles(kind = st.particles, tint = Color(st.particleTint), modifier = Modifier.matchParentSize(), density = 0.8f)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(2, 8, 32, 128).forEach { SkinTilePreview(skin, it, 46.dp) } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(256, 512, 1024, 2048).forEach { SkinTilePreview(skin, it, 46.dp) } }
        }
    }
}

/** Skin de una fiesta: cómo conseguirla, tu progreso y la opción de comprarla con gemas mientras dura. */
@Composable
fun EventSkinDialog(event: GameEvent, retention: RetentionManager, onDismiss: () -> Unit) {
    val skin = EventSkins.skinFor(event.type) ?: return
    val context = LocalContext.current
    val economy = remember { EconomyManager(context) }
    val gems by economy.gems.collectAsState()
    val owned by economy.ownedSkins.collectAsState()
    val equipped by economy.equippedSkin.collectAsState()
    val tick by retention.tick.collectAsState()
    val wins = remember(tick) { retention.eventWins(event) }
    val has = skin.id in owned
    val today = remember { com.korkoor.pardos.data.local.LocalDay.today() }
    val left = event.daysLeft(today)

    CardDialog(onDismiss) {
        Text("SKIN DE ${EventSkins.eventName(event.type).uppercase()}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = InkSecondary, letterSpacing = 2.sp)
        Spacer(Modifier.height(10.dp))
        SkinShowcase(skin)
        Spacer(Modifier.height(12.dp))
        Text(skin.displayName, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Navy)
        Text(rarityLabel(skin.rarity), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, color = rarityColor(skin.rarity))
        Spacer(Modifier.height(12.dp))

        if (has) {
            Text("¡Ya es tuya para siempre!", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Sage)
            Spacer(Modifier.height(14.dp))
            if (equipped == skin) DialogButton("Equipada", Sage, enabled = false) {}
            else DialogButton("Equipar ahora", Sage) { economy.equipSkin(skin); onDismiss() }
        } else {
            Text(
                "Gana ${EventSkins.WINS_REQUIRED} niveles durante el evento y es tuya.",
                fontSize = 13.sp, color = InkSecondary, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            val progress = (wins.toFloat() / EventSkins.WINS_REQUIRED).coerceIn(0f, 1f)
            androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(Navy.copy(alpha = 0.08f))) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxHeight().fillMaxWidth(progress.coerceAtLeast(0.03f)).background(Terracotta, CircleShape))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "$wins/${EventSkins.WINS_REQUIRED} victorias · " + if (left == 0) "termina hoy" else "termina en $left ${if (left == 1) "día" else "días"}",
                fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Terracotta
            )
            Spacer(Modifier.height(14.dp))
            DialogButton("Jugar ahora", Sage, onClick = onDismiss)
            Spacer(Modifier.height(8.dp))
            DialogButton("Obtener con ${EventSkins.GEM_PRICE} gemas", GemBlue, enabled = gems >= EventSkins.GEM_PRICE) {
                if (retention.buyEventSkin(event)) { economy.equipSkin(skin) }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Si no la consigues, vuelve el año que viene.", fontSize = 10.sp, color = InkTertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Celebración al desbloquear una o varias skins (de fiesta o secretas). */
@Composable
fun SkinUnlockedDialog(skins: List<TileSkin>, onDismiss: () -> Unit) {
    if (skins.isEmpty()) return
    val context = LocalContext.current
    val economy = remember { EconomyManager(context) }
    val skin = skins.first()
    CardDialog(onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = Gold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                if (skin.source == com.korkoor.pardos.domain.shop.SkinSource.HIDDEN) "¡SKIN SECRETA DESCUBIERTA!" else "¡SKIN DESBLOQUEADA!",
                fontSize = 11.sp, fontWeight = FontWeight.Black, color = Gold, letterSpacing = 1.5.sp
            )
        }
        Spacer(Modifier.height(10.dp))
        SkinShowcase(skin, height = 190.dp)
        Spacer(Modifier.height(12.dp))
        Text(skin.displayName, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Navy)
        Text(rarityLabel(skin.rarity), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, color = rarityColor(skin.rarity))
        if (skins.size > 1) {
            Spacer(Modifier.height(6.dp))
            Text("y ${skins.size - 1} más en tu colección", fontSize = 12.sp, color = InkSecondary)
        }
        Spacer(Modifier.height(16.dp))
        DialogButton("Equipar ahora", Sage) { economy.equipSkin(skin); onDismiss() }
        Spacer(Modifier.height(6.dp))
        Text("Más tarde", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InkSecondary, modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp))
    }
}
