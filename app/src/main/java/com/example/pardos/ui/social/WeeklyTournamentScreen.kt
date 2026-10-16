package com.korkoor.pardos.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Groups
import com.korkoor.pardos.ui.design.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.data.local.ProfileManager
import com.korkoor.pardos.data.local.TournamentSnapshot
import com.korkoor.pardos.data.local.TournamentRepository
import com.korkoor.pardos.domain.logic.CompetitiveRound
import com.korkoor.pardos.domain.logic.Direction
import com.korkoor.pardos.ui.design.*
import kotlinx.coroutines.delay

@Composable
fun WeeklyTournamentScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val profile = remember { ProfileManager(context) }
    val repository = remember { TournamentRepository() }
    var snapshot by remember { mutableStateOf<TournamentSnapshot?>(null) }
    var round by remember { mutableStateOf<CompetitiveRound?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var remaining by remember { mutableIntStateOf(60) }
    var redraw by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var resultScore by remember { mutableIntStateOf(-1) }
    var communityPoints by remember { mutableLongStateOf(0L) }

    fun submitRound(active: CompetitiveRound) {
        val run = snapshot ?: return
        busy = true
        repository.submit(run.roundId, active.actions, active.score, profile.getProfile().name) { submitted ->
            submitted.onSuccess { (score, points) ->
                resultScore = score
                communityPoints = points
                error = null
                repository.load { refreshed ->
                    refreshed.onSuccess { latest -> snapshot = latest.copy(roundId = run.roundId) }
                }
            }
                .onFailure { error = it.localizedMessage ?: "No se pudo guardar el resultado." }
            busy = false
        }
    }

    LaunchedEffect(Unit) {
        if (profile.signedInUid() != null) {
            busy = true
            repository.load { result ->
                result.onSuccess { snapshot = it; communityPoints = it.communityPoints }
                    .onFailure { error = it.localizedMessage ?: "No se pudo cargar el torneo." }
                busy = false
            }
        }
    }
    LaunchedEffect(round) {
        val active = round ?: return@LaunchedEffect
        val start = System.currentTimeMillis()
        while (remaining > 0 && !active.isOver && resultScore < 0) {
            delay(250)
            remaining = (60 - ((System.currentTimeMillis() - start) / 1000L).toInt()).coerceAtLeast(0)
            redraw++
        }
        if (resultScore < 0 && (active.isOver || remaining == 0)) {
            submitRound(active)
        }
    }

    val currentRound = round
    Box(Modifier.fillMaxSize().pardosBackdrop()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                JellySurface(onClick = onBack, shape = androidx.compose.foundation.shape.CircleShape, color = Color.White,
                    shadowElevation = 5.dp, modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Atrás", tint = Navy) }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("TORNEO SEMANAL", fontSize = 17.sp, fontWeight = FontWeight.Black, color = Navy, letterSpacing = 1.5.sp)
                    Text(snapshot?.week?.let { "Semana desde $it" } ?: "Rivales de la comunidad", fontSize = 11.sp, color = InkSecondary)
                }
            }

            if (profile.signedInUid() == null) {
                PardosCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), elevation = 5.dp) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Inicia sesión para competir", fontSize = 18.sp, color = Navy, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(8.dp))
                        Text("El torneo guarda tu resultado y lo compara con jugadores reales.", color = InkSecondary, textAlign = TextAlign.Center)
                    }
                }
            } else if (busy && snapshot == null) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Text("Cargando torneo…", color = InkSecondary) }
            } else {
                snapshot?.let { event ->
                    if (currentRound == null) {
                        PardosCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), elevation = 6.dp) {
                            Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.Groups, null, tint = Violet, modifier = Modifier.size(36.dp))
                                Text("Una ronda · 60 segundos", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Navy)
                                Text("Guarda tu puntaje y la repetición de la partida. En el plan gratuito el puntaje no se valida en servidor.", fontSize = 12.sp, color = InkSecondary, textAlign = TextAlign.Center)
                                Spacer(Modifier.height(12.dp))
                                if (event.alreadyPlayed) Text("Ya participaste esta semana.", color = Sage, fontWeight = FontWeight.Bold)
                                else if (busy) Text("Preparando tu ronda…", color = InkSecondary, fontWeight = FontWeight.Bold)
                                else PrimaryButton("Jugar torneo", onClick = {
                                    busy = true
                                    error = null
                                    repository.start { started ->
                                        started.onSuccess { (seed, roundId) ->
                                            snapshot = event.copy(roundId = roundId)
                                            remaining = 60
                                            resultScore = -1
                                            round = CompetitiveRound(seed)
                                        }.onFailure { error = it.localizedMessage ?: "No se pudo iniciar el torneo." }
                                        busy = false
                                    }
                                })
                            }
                        }
                    } else {
                        Text("${remaining}s restantes  ·  ${currentRound.score} puntos", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                            fontSize = 20.sp, fontWeight = FontWeight.Black, color = Navy)
                        Spacer(Modifier.height(10.dp))
                        TournamentBoard(currentRound.board, redraw)
                        Spacer(Modifier.height(12.dp))
                        TournamentControls(enabled = remaining > 0 && !currentRound.isOver && !busy) { direction ->
                            if (currentRound.move(direction)) redraw++
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(if (busy) "Guardando resultado…" else if (resultScore >= 0) "Resultado guardado: $resultScore" else "Desliza con los botones para combinar fichas",
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Sage, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        if (resultScore < 0 && error != null && (remaining == 0 || currentRound.isOver) && !busy) {
                            TextButton(onClick = { submitRound(currentRound) }) { Text("Reintentar guardado", color = Terracotta, fontWeight = FontWeight.Bold) }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    PardosCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = 4.dp) {
                        Column(Modifier.padding(15.dp)) {
                            Text("RETO DE TODA LA COMUNIDAD", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Violet, letterSpacing = 1.sp)
                            Spacer(Modifier.height(5.dp))
                            Text("${communityPoints.coerceAtMost(event.communityGoal)} / ${event.communityGoal} puntos", color = Navy, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { (communityPoints.toFloat() / event.communityGoal.coerceAtLeast(1)).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(), color = Violet
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("CLASIFICACIÓN", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Navy, letterSpacing = 1.5.sp)
                    Column(Modifier.weight(1f).fillMaxWidth().padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        if (event.leaderboard.isEmpty()) Text("Aún no hay resultados esta semana.", color = InkSecondary, fontSize = 12.sp)
                        event.leaderboard.take(8).forEachIndexed { index, item ->
                            Row(Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.75f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("${index + 1}.", color = Violet, fontWeight = FontWeight.Black, modifier = Modifier.width(32.dp))
                                Text(item.name, color = Navy, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
                                Text("${item.score}", color = Navy, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
            error?.let { Text(it, color = Terracotta, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp)) }
        }
    }
}

@Composable
private fun TournamentBoard(values: List<Int>, redraw: Int) {
    key(redraw) {
        Column(Modifier.fillMaxWidth().aspectRatio(1f).background(Color.White, RoundedCornerShape(22.dp)).padding(7.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(4) { row ->
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(4) { col ->
                        val value = values[row * 4 + col]
                        val color = when (value) {
                            0 -> Color(0xFFECEBF0); 2, 4 -> Color(0xFFF3E7D6); 8, 16 -> Color(0xFFECC68E)
                            32, 64 -> Color(0xFFE98E69); else -> Color(0xFFD95F55)
                        }
                        Box(Modifier.weight(1f).fillMaxHeight().background(color, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            if (value > 0) Text("$value", fontSize = 22.sp, fontWeight = FontWeight.Black, color = if (value >= 32) Color.White else Navy, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TournamentControls(enabled: Boolean, onMove: (Direction) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TournamentArrow(Icons.Rounded.ArrowUpward, "Arriba", enabled) { onMove(Direction.UP) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TournamentArrow(Icons.AutoMirrored.Rounded.ArrowBack, "Izquierda", enabled) { onMove(Direction.LEFT) }
            TournamentArrow(Icons.Rounded.ArrowDownward, "Abajo", enabled) { onMove(Direction.DOWN) }
            TournamentArrow(Icons.Rounded.ArrowForward, "Derecha", enabled) { onMove(Direction.RIGHT) }
        }
    }
}

@Composable
private fun TournamentArrow(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(48.dp).background(Color.White, RoundedCornerShape(15.dp))) {
        Icon(icon, description, tint = if (enabled) Navy else InkTertiary)
    }
}

