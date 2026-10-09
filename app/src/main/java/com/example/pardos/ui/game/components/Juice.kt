package com.korkoor.pardos.ui.game.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.audio.GameAudio
import com.korkoor.pardos.audio.Sfx
import com.korkoor.pardos.ui.design.CozyText
import com.korkoor.pardos.ui.design.Icon
import com.korkoor.pardos.ui.design.Navy
import kotlin.math.cos
import kotlin.math.sin

/** Un golpe del tablero: [strength] va de 0 a 1 (fusión de una ficha enorme = 1). [id] distingue un golpe del siguiente. */
data class BoardImpact(val id: Int, val strength: Float)

/**
 * Sacudida corta del tablero cuando algo pega fuerte (fusión grande, combo alto, jefe): un temblor que se apaga y un
 * "latido" de tamaño. Con las animaciones del sistema apagadas no se nota (la animación termina al instante).
 */
fun Modifier.impactShake(impact: BoardImpact?): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(impact?.id) {
        if (impact != null) {
            progress.snapTo(1f)
            progress.animateTo(0f, tween(360, easing = LinearOutSlowInEasing))
        }
    }
    val density = LocalDensity.current.density
    graphicsLayer {
        val p = progress.value
        val s = impact?.strength ?: 0f
        if (p > 0f && s > 0f) {
            val amp = (1.5f + 6.5f * s) * density * p
            translationX = sin(p * 40f) * amp
            translationY = cos(p * 33f) * amp * 0.6f
            val pulse = 1f + 0.02f * s * p
            scaleX = pulse
            scaleY = pulse
        }
    }
}

/**
 * Las estrellas que llevas ahora mismo en el nivel (se ven bajar si te pasas de jugadas). Solo en niveles sin reloj de la
 * campaña: así el jugador sabe en todo momento cuánto margen le queda para las 3 estrellas.
 * [compact]: versión pequeña para la esquina de la tarjeta de meta (estrellas arriba y el margen debajo, alineado a la derecha).
 */
@Composable
fun LiveStarMeter(
    moves: Int, threeStarMoves: Int, twoStarMoves: Int, ink: Color = Navy, modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val stars = when {
        moves <= threeStarMoves -> 3
        moves <= twoStarMoves -> 2
        else -> 1
    }
    val limit = when (stars) { 3 -> threeStarMoves; 2 -> twoStarMoves; else -> null }
    val margin = limit?.let { (it - moves).coerceAtLeast(0) }

    // Al perder una estrella: un sonido suave (las estrellas rebotan solas al apagarse)
    var previous by remember { mutableIntStateOf(stars) }
    LaunchedEffect(stars) {
        if (stars < previous) GameAudio.play(Sfx.UI_OFF)
        previous = stars
    }

    val marginText = when {
        margin == null -> if (compact) "asegurada" else "estrella asegurada"
        margin == 0 -> if (compact) "¡última!" else "¡última jugada de margen!"
        else -> if (compact) "$margin de margen" else "$margin ${if (margin == 1) "jugada" else "jugadas"} de margen"
    }
    val marginColor = if (margin != null && margin <= 2) Color(0xFFE07A5F) else ink.copy(alpha = 0.55f)
    val starSize = if (compact) 15.dp else 18.dp

    if (compact) {
        Column(modifier = modifier, horizontalAlignment = Alignment.End) {
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) { repeat(3) { i -> LiveStar(i < stars, starSize, ink, i) } }
            CozyText(text = marginText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.3.sp, color = marginColor, maxLines = 1)
        }
    } else {
        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            repeat(3) { i -> LiveStar(i < stars, starSize, ink, i) }
            Spacer(Modifier.width(8.dp))
            CozyText(text = marginText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp, color = marginColor)
        }
    }
}

@Composable
private fun LiveStar(on: Boolean, size: androidx.compose.ui.unit.Dp, ink: Color, index: Int) {
    val scale by animateFloatAsState(
        targetValue = if (on) 1f else 0.72f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 380f),
        label = "liveStar$index"
    )
    Icon(
        Icons.Default.Star, contentDescription = null,
        tint = if (on) Color(0xFFF2B84B) else ink.copy(alpha = 0.18f),
        modifier = Modifier.size(size).graphicsLayer { scaleX = scale; scaleY = scale }
    )
}
