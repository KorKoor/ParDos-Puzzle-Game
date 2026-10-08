package com.korkoor.pardos.ui.rewards

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.ui.design.*

/**
 * Antes de pedir el permiso del sistema se explica, con ejemplos reales, qué se le va a avisar. Se enseña tras la primera
 * victoria (ver `NotificationPrimer`): aceptar entonces es mucho más probable que al abrir la app por primera vez.
 */
@Composable
fun NotificationPrimerDialog(onAccept: () -> Unit, onLater: () -> Unit) {
    CardDialog(onLater) {
        Box(Modifier.wobble(5f, 1500)) {
            IconTile(Icons.Rounded.NotificationsActive, Gold, size = 72.dp, shape = androidx.compose.foundation.shape.CircleShape)
        }
        Spacer(Modifier.height(12.dp))
        Text("¿Te aviso cuando haya algo para ti?", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Navy, textAlign = TextAlign.Center)
        Text("Pocos avisos y nunca de madrugada.", fontSize = 12.sp, color = InkSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimerLine(CozyKind.CHEST, "Tu cofre gratis ya está listo")
            PrimerLine(CozyKind.FLAME, "Tu racha está en riesgo")
            PrimerLine(CozyKind.STAR, "Misiones nuevas y premios por cobrar")
            PrimerLine(CozyKind.PARTY, "Fiestas con skins exclusivas")
        }
        Spacer(Modifier.height(18.dp))
        DialogButton("Sí, avísame", Sage, onClick = onAccept)
        Spacer(Modifier.height(6.dp))
        ToyTextButton(onClick = onLater) {
            Text("Ahora no", fontSize = 12.sp, fontWeight = FontWeight.Black, color = InkSecondary)
        }
    }
}

@Composable
private fun PrimerLine(kind: CozyKind, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CozyIcon(kind, Modifier.size(30.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy)
    }
}
