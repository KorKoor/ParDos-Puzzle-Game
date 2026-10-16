package com.korkoor.pardos.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.data.local.LevelProgressStore
import com.korkoor.pardos.domain.level.Bestiary
import com.korkoor.pardos.domain.model.GameMode
import com.korkoor.pardos.ui.design.InkSecondary
import com.korkoor.pardos.ui.design.InkTertiary
import com.korkoor.pardos.ui.design.JellySurface
import com.korkoor.pardos.ui.design.Navy
import com.korkoor.pardos.ui.game.components.BossEmblem
import com.korkoor.pardos.ui.game.components.tone
import com.korkoor.pardos.ui.rewards.CardDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Fichas del bestiario: cada jefe o guardián se desbloquea al ganar el nivel donde aparece. */
private data class BestiarySnapshot(val entries: List<Pair<Bestiary.Entry, Boolean>>)

@Composable
private fun rememberBestiary(): BestiarySnapshot? {
    val context = LocalContext.current
    var snap by remember { mutableStateOf<BestiarySnapshot?>(null) }
    LaunchedEffect(Unit) {
        snap = withContext(Dispatchers.Default) {
            val store = LevelProgressStore(context.getSharedPreferences("pardos_storage", android.content.Context.MODE_PRIVATE))
            val all = Bestiary.bosses + Bestiary.guardians
            BestiarySnapshot(all.map { it to it.defeated { lv -> store.starsFor(GameMode.CLASICO, lv) } })
        }
    }
    return snap
}

/** Tarjeta que resume el bestiario y lo abre. */
@Composable
fun BestiaryCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val snap = rememberBestiary()
    JellySurface(modifier = modifier.fillMaxWidth(), onClick = onClick, shape = RoundedCornerShape(20.dp), color = androidx.compose.ui.graphics.Color.White) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("BESTIARIO", fontSize = 11.sp, fontWeight = FontWeight.Black, color = InkTertiary, letterSpacing = 2.sp)
                Text("Jefes y guardianes", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Navy)
                Text("Véncelos para completar sus fichas.", fontSize = 12.sp, color = InkSecondary)
            }
            val done = snap?.entries?.count { it.second } ?: 0
            val total = snap?.entries?.size ?: 0
            Text(if (snap == null) "…" else "$done/$total", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Navy)
        }
    }
}

@Composable
fun BestiaryDialog(onDismiss: () -> Unit) {
    val snap = rememberBestiary()
    CardDialog(onDismiss) {
        Text("BESTIARIO", fontSize = 11.sp, fontWeight = FontWeight.Black, color = InkTertiary, letterSpacing = 3.sp)
        Text("Jefes y guardianes", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Navy)
        Spacer(Modifier.height(10.dp))
        if (snap == null) {
            Text("Cargando…", fontSize = 13.sp, color = InkSecondary)
        } else {
            LazyColumn(Modifier.fillMaxWidth().height(380.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(snap.entries) { (entry, done) -> BestiaryRow(entry, done) }
            }
        }
        Spacer(Modifier.height(8.dp))
        com.korkoor.pardos.ui.rewards.DialogButton("Cerrar", Navy, onClick = onDismiss)
    }
}

@Composable
private fun BestiaryRow(entry: Bestiary.Entry, done: Boolean) {
    val tone = entry.motif.tone()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (done) tone.copy(alpha = 0.10f) else Navy.copy(alpha = 0.05f)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
            if (done) BossEmblem(entry.motif, size = 52.dp)
            else Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Navy.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
                Text("?", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Navy.copy(alpha = 0.3f))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (done) entry.name else "???",
                fontSize = 15.sp, fontWeight = FontWeight.Black, color = if (done) Navy else Navy.copy(alpha = 0.4f)
            )
            Text(
                if (done) "“${entry.taunt}”" else "Aparece en el nivel ${entry.firstLevel}",
                fontSize = 12.sp, fontStyle = if (done) FontStyle.Italic else FontStyle.Normal, color = if (done) InkSecondary else InkTertiary
            )
            Text(if (entry.guardian) "Guardián" else "Jefe", fontSize = 10.sp, fontWeight = FontWeight.Black, color = tone.copy(alpha = if (done) 1f else 0.5f), letterSpacing = 1.sp)
        }
    }
}
