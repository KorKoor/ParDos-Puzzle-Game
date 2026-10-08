package com.korkoor.pardos.ui.game.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.domain.tower.TowerRules
import com.korkoor.pardos.ui.design.Cream
import com.korkoor.pardos.ui.design.Gold
import com.korkoor.pardos.ui.design.JellySurface
import com.korkoor.pardos.ui.design.Navy
import com.korkoor.pardos.ui.design.PrimaryButton
import com.korkoor.pardos.ui.design.Sage
import com.korkoor.pardos.ui.design.Terracotta
import com.korkoor.pardos.ui.design.Violet
import kotlinx.coroutines.delay

/** Corazones de la torre (llenos y vacíos). */
@Composable
fun TowerHearts(hearts: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(TowerRules.START_HEARTS.coerceAtLeast(hearts)) { i ->
            Icon(
                if (i < hearts) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = null,
                tint = if (i < hearts) Terracotta else Navy.copy(alpha = 0.3f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** Resumen al superar un piso: lo ganado, los corazones y el siguiente piso (con cuenta atrás que cualquier toque pausa). */
@Composable
fun TowerSummaryOverlay(
    floor: Int,
    reward: TowerRules.FloorReward?,
    hearts: Int,
    best: Int,
    runCoins: Int,
    autoNextMs: Int?,
    onNext: () -> Unit,
    onExit: () -> Unit
) {
    var cancelled by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(autoNextMs, cancelled) {
        if (autoNextMs != null && !cancelled) {
            delay(1800)
            val steps = (autoNextMs / 50).coerceAtLeast(1)
            for (i in 1..steps) { progress = i / steps.toFloat(); delay(50) }
            onNext()
        }
    }
    val boss = TowerRules.isBossFloor(floor)
    val nextBoss = TowerRules.isBossFloor(floor + 1)
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))
            .clickable(enabled = false) {}
            .androidxPointerCancel { cancelled = true; progress = 0f },
        contentAlignment = Alignment.Center
    ) {
        JellySurface(modifier = Modifier.fillMaxWidth(0.86f).padding(16.dp), color = Cream, shape = RoundedCornerShape(32.dp), shadowElevation = 16.dp) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Layers, null, tint = Violet, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(6.dp))
                Text(if (boss) "¡JEFE DERROTADO!" else "¡PISO SUPERADO!", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Violet, letterSpacing = 3.sp)
                Text("Piso $floor", fontSize = 40.sp, fontWeight = FontWeight.Black, color = Navy)
                if (best > 0) Text("Récord: piso $best", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy.copy(alpha = 0.5f))
                Spacer(Modifier.height(14.dp))
                reward?.let { r ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("+${r.coins} monedas", Gold)
                        if (r.gems > 0) Chip("+${r.gems} gemas", Color(0xFF4FA3C7))
                    }
                    if (r.heart && hearts < TowerRules.MAX_HEARTS + 1) {
                        Spacer(Modifier.height(8.dp))
                        Text("¡Recuperas un corazón!", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Terracotta)
                    }
                }
                Spacer(Modifier.height(12.dp))
                TowerHearts(TowerRules.heartsAfterClear(floor, hearts))
                Spacer(Modifier.height(10.dp))
                Text(
                    if (nextBoss) "Siguiente: ¡piso ${floor + 1}, JEFE!" else "Siguiente: piso ${floor + 1}",
                    fontSize = 13.sp, fontWeight = FontWeight.Black, color = if (nextBoss) Terracotta else Navy.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(16.dp))
                PrimaryButton(text = "Subir al piso ${floor + 1}", onClick = onNext)
                if (autoNextMs != null && progress > 0f) {
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.width(160.dp).height(4.dp).clip(RoundedCornerShape(50)).background(Navy.copy(alpha = 0.08f))) {
                        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(4.dp).background(Violet, RoundedCornerShape(50)))
                    }
                    Text("Sube solo · toca para pausar", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Navy.copy(alpha = 0.45f))
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Bajar y cobrar ($runCoins monedas hasta ahora)",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy.copy(alpha = 0.55f), textAlign = TextAlign.Center,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { cancelled = true; onExit() }.padding(8.dp)
                )
            }
        }
    }
}

/** Fin de la subida: sin corazones. Enseña hasta dónde llegaste, el récord y lo ganado. */
@Composable
fun TowerEndOverlay(floor: Int, best: Int, runCoins: Int, runGems: Int, newRecord: Boolean, onRetry: () -> Unit, onExit: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "towerEnd").animateFloat(0.96f, 1.06f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "p")
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
        JellySurface(modifier = Modifier.fillMaxWidth(0.86f).padding(16.dp), color = Cream, shape = RoundedCornerShape(32.dp), shadowElevation = 16.dp) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("FIN DE LA SUBIDA", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Violet, letterSpacing = 3.sp)
                Spacer(Modifier.height(6.dp))
                Text("Piso $floor", fontSize = 56.sp, fontWeight = FontWeight.Black, color = Navy, modifier = Modifier.scale(if (newRecord) pulse else 1f))
                Text(
                    if (newRecord) "¡Nuevo récord!" else "Tu récord: piso $best",
                    fontSize = 14.sp, fontWeight = FontWeight.Black, color = if (newRecord) Gold else Navy.copy(alpha = 0.55f)
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("$runCoins monedas", Gold)
                    if (runGems > 0) Chip("$runGems gemas", Color(0xFF4FA3C7))
                }
                Text("Ya están en tu cuenta", fontSize = 11.sp, color = Navy.copy(alpha = 0.45f), modifier = Modifier.padding(top = 6.dp))
                Spacer(Modifier.height(20.dp))
                PrimaryButton(text = "Volver a subir", onClick = onRetry)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Salir", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Navy.copy(alpha = 0.55f),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onExit() }.padding(10.dp)
                )
            }
        }
    }
}

@Composable
private fun Chip(text: String, color: Color) {
    Text(
        text, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Navy,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.18f)).padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

/** Cualquier toque sobre la tarjeta pausa la cuenta atrás (usa el pase "Initial" para ver el toque antes que los botones). */
private fun Modifier.androidxPointerCancel(onTouch: () -> Unit): Modifier =
    this.then(
        Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                    onTouch()
                }
            }
        }
    )
