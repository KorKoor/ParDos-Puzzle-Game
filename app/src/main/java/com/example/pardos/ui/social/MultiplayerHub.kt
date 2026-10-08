package com.korkoor.pardos.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onDailyChallenge: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(ScreenBackground)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            PardosTopBar(eyebrow = "Juega con otros", title = "Multijugador", onBack = onBack)

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Destacado: duelo local
                PardosCard(onClick = onLocalDuel, modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(Radius.XLarge), elevation = 10.dp) {
                    Column(
                        modifier = Modifier
                            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Terracotta, Terracotta.darker(0.82f))))
                            .padding(22.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(54.dp).background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f), androidx.compose.foundation.shape.RoundedCornerShape(18.dp)),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Rounded.Groups, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(32.dp)) }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("DUELO LOCAL", fontSize = 18.sp, fontWeight = FontWeight.Black, color = androidx.compose.ui.graphics.Color.White, letterSpacing = 2.sp)
                                Text("2 jugadores · 1 teléfono", fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f))
                            }
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(30.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Misma tabla, 60 segundos cada uno. Gana quien sume más puntos.",
                            fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f), lineHeight = 17.sp
                        )
                    }
                }

                HubRow(Icons.Rounded.Star, Gold, "Reto diario", "El mismo tablero para todo el mundo hoy. ¿Quién llega más lejos?", onDailyChallenge)
                HubRow(Icons.Rounded.Group, GemBlue, "Amigos y ranking", "Agrega amigos con tu código y compitan por estrellas cada semana.", onFriends)

                // Lo que viene (sin prometer fechas)
                PardosCard(modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(Radius.Large), elevation = 0.dp, color = Navy.copy(alpha = 0.04f)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconTile(Icons.Rounded.Lock, Navy.copy(alpha = 0.4f), size = 42.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Retos entre amigos", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Navy.copy(alpha = 0.55f))
                            Text("En camino: desafía a un amigo con la misma tabla.", fontSize = 11.sp, color = InkTertiary)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun HubRow(icon: ImageVector, color: androidx.compose.ui.graphics.Color, title: String, desc: String, onClick: () -> Unit) {
    PardosCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(Radius.Large), elevation = 6.dp) {
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
