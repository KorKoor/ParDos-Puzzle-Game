package com.korkoor.pardos.ui.rewards

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.R
import com.korkoor.pardos.ui.design.*

/**
 * Bienvenida a la Noche de brujas: se enseña una vez por año al abrir el menú en octubre. Cuenta que toda la app se vistió de fiesta
 * y, si ya tienes la skin de brujas, ofrece ponértela con un toque.
 */
@Composable
fun HalloweenWelcomeDialog(ownsSkin: Boolean, skinEquipped: Boolean, onEquip: () -> Unit, onDismiss: () -> Unit) {
    CardDialog(onDismiss) {
        Box(Modifier.wobble(6f, 1600)) {
            Image(painterResource(R.drawable.ico_pumpkin), contentDescription = null, modifier = Modifier.size(92.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text("NOCHE DE BRUJAS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Sage, letterSpacing = 3.sp)
        Text("¡Llegó la fiesta más oscura!", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Navy, textAlign = TextAlign.Center)
        Text("Hasta el 2 de noviembre", fontSize = 12.sp, color = InkSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            WelcomeLine(R.drawable.ico_ghost, "Toda la app se vistió de Halloween")
            WelcomeLine(R.drawable.ico_skull, "Gana 3 partidas y llévate la skin Noche de brujas")
            WelcomeLine(R.drawable.ico_bat, "Avisos, cofres y mapa embrujados")
        }
        Spacer(Modifier.height(18.dp))
        if (ownsSkin && !skinEquipped) {
            DialogButton("Ponerme la skin de brujas", Sage, onClick = onEquip)
            Spacer(Modifier.height(6.dp))
            ToyTextButton(onClick = onDismiss) { Text("Más tarde", fontSize = 12.sp, fontWeight = FontWeight.Black, color = InkSecondary) }
        } else {
            DialogButton("¡Truco o trato!", Sage, onClick = onDismiss)
        }
    }
}

@Composable
private fun WelcomeLine(art: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(art), contentDescription = null, modifier = Modifier.size(34.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy)
    }
}
