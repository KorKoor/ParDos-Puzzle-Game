package com.korkoor.pardos.ui.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.domain.flow.SideQuest
import com.korkoor.pardos.ui.design.Icon
import com.korkoor.pardos.ui.design.InkSecondary
import com.korkoor.pardos.ui.design.Navy

/** Resultado del reto bonus de la última victoria (lo escribe el juego al ganar, lo lee el resumen). */
object QuestHud {
    var result by mutableStateOf<Pair<SideQuest, Boolean>?>(null)
    /** Texto del premio del jefe de la semana, si se acaba de vencer. */
    var weekly by mutableStateOf<String?>(null)
}

private val QuestTone = Color(0xFF2A9D8F)

/** Píldora con el reto bonus del nivel, junto a las reglas. */
@Composable
fun QuestPill(quest: SideQuest, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.92f)).background(QuestTone.copy(alpha = 0.12f))
            .border(1.dp, QuestTone.copy(alpha = 0.45f), RoundedCornerShape(50)).padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Flag, contentDescription = null, tint = QuestTone, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text("Reto: ${quest.title}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Navy)
    }
}

/** Frenesí: puntos dobles unas jugadas tras llegar a FLOW. */
@Composable
fun FrenzyPill(movesLeft: Int, modifier: Modifier = Modifier) {
    if (movesLeft <= 0) return
    val tone = Color(0xFFE0603A)
    Row(
        modifier = modifier.clip(RoundedCornerShape(50)).background(tone.copy(alpha = 0.92f)).padding(horizontal = 11.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("¡FRENESÍ! ×2 · $movesLeft", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.sp)
    }
}

/** Línea del resumen de victoria: qué reto había y si se cumplió. */
@Composable
fun QuestResultRow(modifier: Modifier = Modifier) {
    QuestHud.weekly?.let { text ->
        Row(
            modifier = modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFFD9A400).copy(alpha = 0.16f)).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFFD9A400), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy)
        }
    }
    val (quest, done) = QuestHud.result ?: return
    Row(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(if (done) QuestTone.copy(alpha = 0.14f) else Navy.copy(alpha = 0.05f)).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(if (done) Icons.Rounded.CheckCircle else Icons.Rounded.Flag, contentDescription = null, tint = if (done) QuestTone else Navy.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            if (done) "Reto cumplido: ${quest.title}  +${quest.coins}" else "Reto de hoy: ${quest.title} · ${quest.hint}",
            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (done) Navy else InkSecondary
        )
    }
}
