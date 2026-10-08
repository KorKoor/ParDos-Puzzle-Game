package com.korkoor.pardos.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import com.korkoor.pardos.ui.design.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.data.local.LocalDay
import com.korkoor.pardos.data.local.RemoteDuelManager
import com.korkoor.pardos.domain.logic.RemoteChallenge
import com.korkoor.pardos.domain.logic.RemoteDuel
import com.korkoor.pardos.domain.logic.RemoteOutcome
import com.korkoor.pardos.ui.design.*

/**
 * Centro social: todo lo que se juega o se compara con otras personas vive aquí,
 * separado de los modos para un solo jugador.
 */
@Composable
fun MultiplayerHub(
    onBack: () -> Unit,
    onLocalDuel: () -> Unit,
    onFriends: () -> Unit,
    onDailyChallenge: () -> Unit,
    onCreateChallenge: () -> Unit = {},
    onAcceptChallenge: (RemoteChallenge) -> Unit = {}
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val manager = remember { RemoteDuelManager(context) }
    val history = remember { manager.history() }
    val stats = remember { RemoteDuel.stats(history) }

    var input by remember { mutableStateOf("") }
    val typed = remember(input) { RemoteDuel.decode(input) }
    var detected by remember { mutableStateOf<RemoteChallenge?>(null) }

    // Si el portapapeles trae un reto que aún no se juega, se ofrece directamente
    LaunchedEffect(Unit) {
        val clip = clipboard.getText()?.text.orEmpty()
        RemoteDuel.decode(clip)?.let { if (!manager.isPlayed(it)) detected = it }
    }

    Box(modifier = Modifier.fillMaxSize().pardosBackdrop()) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            PardosTopBar(eyebrow = "Juega con otros", title = "Multijugador", onBack = onBack)

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Reto detectado en el portapapeles
                detected?.let { ch ->
                    ChallengeBanner(
                        challenge = ch,
                        onPlay = { detected = null; onAcceptChallenge(ch) },
                        onDismiss = { detected = null }
                    )
                }

                // ---- Destacado: duelo a distancia ----
                PardosCard(onClick = onCreateChallenge, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Radius.XLarge), elevation = 10.dp) {
                    Column(
                        modifier = Modifier.background(Brush.linearGradient(listOf(Terracotta, Terracotta.darker(0.78f)))).padding(22.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(54.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(18.dp)),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Rounded.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp)) }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("RETA A ALGUIEN", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 2.sp)
                                Text("Duelo a distancia · 60 segundos", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                            }
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Juegas una ronda, te sale un código y se lo mandas a quien quieras. Jugará el mismo tablero que tú: " +
                                "gana quien sume más. Sin cuentas, sin esperas.",
                            fontSize = 12.sp, color = Color.White.copy(alpha = 0.92f), lineHeight = 17.sp
                        )
                    }
                }

                // ---- Tengo un código ----
                PardosCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Radius.Large), elevation = 6.dp) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconTile(Icons.Rounded.QrCode2, Violet, size = 44.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Tengo un código", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Navy)
                                Text("Pega el mensaje del reto o solo el código", fontSize = 12.sp, color = InkSecondary)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ToyTextField(
                                value = input, onValueChange = { input = it.take(400) }, placeholder = "PD1-…",
                                accent = if (typed != null) Sage else Violet,
                                textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Violet.copy(alpha = 0.14f))
                                    .clickable { clipboard.getText()?.text?.let { input = it } },
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Rounded.ContentPaste, contentDescription = "Pegar", tint = Violet) }
                        }
                        if (input.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            if (typed != null) {
                                val played = manager.isPlayed(typed)
                                Text(
                                    "Reto de ${RemoteDuel.displayName(typed.name)}: ${typed.score} puntos" + if (played) " · ya lo jugaste" else "",
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (played) InkSecondary else Sage
                                )
                                Spacer(Modifier.height(10.dp))
                                PrimaryButton("Aceptar el reto", onClick = { onAcceptChallenge(typed) }, icon = Icons.Rounded.PlayArrow)
                            } else {
                                Text("No encuentro un código válido. Revisa que esté completo.", fontSize = 12.sp, color = Terracotta, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // ---- Tus duelos ----
                if (stats.played > 0) {
                    SectionLabel("Tus duelos")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile("GANADOS", "${stats.wins}", Sage, Modifier.weight(1f))
                        StatTile("PERDIDOS", "${stats.losses}", GemBlue, Modifier.weight(1f))
                        StatTile("RACHA", "${stats.currentStreak}", Gold, Modifier.weight(1f))
                    }
                    PardosCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Radius.Large), elevation = 4.dp) {
                        Column(Modifier.padding(vertical = 6.dp)) {
                            history.take(5).forEach { r ->
                                val (icon, color) = when (r.outcome) {
                                    RemoteOutcome.WIN -> Icons.Rounded.EmojiEvents to Sage
                                    RemoteOutcome.LOSE -> Icons.Rounded.SentimentNeutral to GemBlue
                                    RemoteOutcome.TIE -> Icons.Rounded.Balance to Gold
                                }
                                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(34.dp).background(color.copy(alpha = 0.14f), CircleShape), contentAlignment = Alignment.Center) {
                                        Icon(icon, null, tint = color, modifier = Modifier.size(19.dp))
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("vs ${r.opponent}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy, maxLines = 1)
                                        Text(
                                            ago(LocalDay.today() - r.day), fontSize = 11.sp, color = InkTertiary
                                        )
                                    }
                                    Text("${r.myScore} – ${r.theirScore}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Navy)
                                }
                            }
                        }
                    }
                }

                SectionLabel("Más formas de jugar")
                HubRow(Icons.Rounded.Groups, Violet, "Duelo local", "2 jugadores en 1 teléfono: misma tabla, 60 s cada uno.", onLocalDuel)
                HubRow(Icons.Rounded.Star, Gold, "Reto diario", "El mismo tablero para todo el mundo hoy. ¿Quién llega más lejos?", onDailyChallenge)
                HubRow(Icons.Rounded.Group, GemBlue, "Amigos y ranking", "Agrega amigos con tu código y compitan por estrellas cada semana.", onFriends)
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

private fun ago(days: Int): String = when {
    days <= 0 -> "hoy"
    days == 1 -> "ayer"
    else -> "hace $days días"
}

@Composable
private fun ChallengeBanner(challenge: RemoteChallenge, onPlay: () -> Unit, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.Large))
            .background(Brush.horizontalGradient(listOf(Sage, Sage.darker(0.8f)))).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp).background(Color.White.copy(alpha = 0.22f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Bolt, null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("¡Te retaron!", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text(
                "${RemoteDuel.displayName(challenge.name)} hizo ${challenge.score} puntos",
                fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f), maxLines = 1
            )
        }
        Box(
            Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White).clickable(onClick = onPlay).padding(horizontal = 14.dp, vertical = 9.dp)
        ) { Text("JUGAR", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Sage, letterSpacing = 1.sp) }
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Rounded.Close, "Cerrar", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(22.dp).clickable(onClick = onDismiss))
    }
}

@Composable
private fun StatTile(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(Radius.Medium)).background(color.copy(alpha = 0.12f)).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Navy)
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Black, color = color.darker(0.8f), letterSpacing = 1.5.sp)
    }
}

@Composable
private fun HubRow(icon: ImageVector, color: Color, title: String, desc: String, onClick: () -> Unit) {
    PardosCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Radius.Large), elevation = 6.dp) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon, color, size = 52.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Black, color = Navy)
                Text(desc, fontSize = 12.sp, color = InkSecondary, lineHeight = 16.sp)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
        }
    }
}
