package com.korkoor.pardos.ui.rewards

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.domain.retention.WhatsNew
import com.korkoor.pardos.ui.design.*

private fun iconFor(key: String): Pair<ImageVector, Color> = when (key) {
    "map" -> Icons.Rounded.Map to Sage
    "duel" -> Icons.Rounded.Bolt to Terracotta
    "chest" -> Icons.Rounded.Inventory2 to Gold
    "league" -> Icons.Rounded.EmojiEvents to Violet
    else -> Icons.Rounded.Favorite to GemBlue
}

/** "Novedades": se muestra una sola vez tras actualizar (solo a quien ya jugaba). */
@Composable
fun WhatsNewDialog(onDismiss: () -> Unit) {
    CardDialog(onDismiss) {
        CozyIcon(com.korkoor.pardos.ui.design.CozyKind.PARTY, Modifier.size(64.dp))
        Spacer(Modifier.height(8.dp))
        Text("¡Novedades en ParDos!", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Navy, textAlign = TextAlign.Center)
        Text("Esto es lo nuevo desde tu última visita", fontSize = 12.sp, color = InkSecondary)
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            WhatsNew.items.forEach { item ->
                val (icon, color) = iconFor(item.icon)
                Row(verticalAlignment = Alignment.Top) {
                    IconTile(icon, color, size = 40.dp, shape = CircleShape)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Navy)
                        Text(item.body, fontSize = 12.sp, color = InkSecondary, lineHeight = 16.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        DialogButton("¡A jugar!", Sage, onClick = onDismiss)
    }
}
