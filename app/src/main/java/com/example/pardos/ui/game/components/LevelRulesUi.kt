package com.korkoor.pardos.ui.game.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.SwapHoriz
import com.korkoor.pardos.domain.level.Twist
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.domain.level.LevelCatalog
import com.korkoor.pardos.domain.level.LevelKind
import com.korkoor.pardos.domain.level.LevelRules
import com.korkoor.pardos.domain.level.formatClock
import com.korkoor.pardos.domain.level.goalText
import com.korkoor.pardos.domain.model.BoardState
import com.korkoor.pardos.ui.design.*
import com.korkoor.pardos.ui.rewards.CardDialog
import com.korkoor.pardos.ui.rewards.DialogButton

/** Insignia redonda de un tipo de nivel: gradiente de su color y su icono en blanco. */
@Composable
fun KindBadge(kind: LevelKind, size: Dp, modifier: Modifier = Modifier) {
    val c = kind.accent()
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(c.lighten(0.22f), c)))
            .border(size * 0.04f, Color.White.copy(alpha = 0.55f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.align(Alignment.TopCenter).padding(top = size * 0.08f).width(size * 0.5f).height(size * 0.09f)
                .background(Color.White.copy(alpha = 0.40f), RoundedCornerShape(50))
        )
        Icon(kind.symbol(), contentDescription = kind.label, tint = Color.White, modifier = Modifier.size(size * 0.54f))
    }
}

/**
 * Reglas del nivel a la vista, bajo la meta: el tipo, los movimientos que quedan (en rojo y latiendo al final)
 * y cuántas piedras hay. En los niveles Zen sin reglas no aparece nada.
 */
@Composable
internal fun LevelRuleChips(state: BoardState, modifier: Modifier = Modifier) {
    val kind = state.levelKind ?: return
    val movesLeft = state.movesLeft
    if (kind == LevelKind.ZEN && movesLeft == null && state.blocked.isEmpty()) return

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (kind != LevelKind.ZEN) KindPill(kind, if (kind == LevelKind.BOSS) (state.levelTitle ?: kind.label) else kind.label)
        if (movesLeft != null) MovesPill(movesLeft, state.moveLimit ?: movesLeft)
        if (state.blocked.isNotEmpty() && kind != LevelKind.STONES) {
            SmallPill(Icons.Rounded.Terrain, "${state.blocked.size}", LevelKind.STONES.accent())
        }
        if (state.twist != Twist.NONE) SmallPill(Icons.Rounded.SwapHoriz, state.twist.label, LevelKind.TWIST.accent())
    }
}

@Composable
private fun KindPill(kind: LevelKind, title: String) {
    val c = kind.accent()
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.92f))
            .background(c.copy(alpha = 0.14f))
            .border(1.dp, c.copy(alpha = 0.45f), RoundedCornerShape(50))
            .padding(start = 4.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KindBadge(kind, 20.dp)
        Spacer(Modifier.width(6.dp))
        Text(title.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = c.darker(0.75f), letterSpacing = 1.sp, maxLines = 1)
    }
}

@Composable
private fun SmallPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.92f)).background(color.copy(alpha = 0.16f)).padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color.darker(0.8f), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Black, color = color.darker(0.75f))
    }
}

@Composable
private fun MovesPill(left: Int, limit: Int) {
    val low = left <= 5 || left * 100 <= limit * 12
    val critical = left <= 3
    val bg by animateColorAsState(if (low) Terracotta else Color.White.copy(alpha = 0.92f), tween(250), label = "movesBg")
    val pulse = rememberInfiniteTransition(label = "movesPulse").animateFloat(
        initialValue = 1f, targetValue = if (critical) 1.12f else 1f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p"
    )
    Row(
        modifier = Modifier.scale(pulse.value).clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 11.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$left", fontSize = 16.sp, fontWeight = FontWeight.Black, color = if (low) Color.White else Navy)
        Spacer(Modifier.width(5.dp))
        Text("MOV.", fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, color = if (low) Color.White.copy(alpha = 0.85f) else Navy.copy(alpha = 0.6f))
    }
}

/**
 * Tarjeta que explica el nivel antes de empezar. Sale la primera vez que aparece cada tipo de regla (y en cada jefe),
 * con el nombre, qué cambia, la meta y cómo conseguir las tres estrellas.
 */
@Composable
internal fun NewRuleDialog(state: BoardState, onStart: () -> Unit) {
    val kind = state.levelKind ?: return
    val spec = remember(state.currentLevel) { LevelCatalog.spec(state.currentLevel) }
    val c = kind.accent()
    CardDialog(onStart) {
        Box(Modifier.wobble(4f, 1700)) { KindBadge(kind, 84.dp) }
        Spacer(Modifier.height(10.dp))
        Text(if (kind == LevelKind.BOSS) "JEFE" else "NUEVA REGLA", fontSize = 11.sp, fontWeight = FontWeight.Black, color = c, letterSpacing = 3.sp)
        Text(if (kind == LevelKind.BOSS) (state.levelTitle ?: kind.label) else kind.label, fontSize = 26.sp, fontWeight = FontWeight.Black, color = Navy, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(kind.rule, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = InkSecondary, textAlign = TextAlign.Center)
        state.levelTip?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, fontSize = 12.sp, color = InkTertiary, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(14.dp))
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.copy(alpha = 0.10f)).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("META", fontSize = 9.sp, fontWeight = FontWeight.Black, color = InkTertiary, letterSpacing = 2.sp)
            Text(goalText(state.goal, state.levelLimit, state.goalCount), fontSize = 17.sp, fontWeight = FontWeight.Black, color = Navy, textAlign = TextAlign.Center)
            val rules = buildList {
                state.moveLimit?.let { add("$it movimientos") }
                state.maxTime?.let { add("Tiempo ${formatClock(it)}") }
                if (state.blocked.isNotEmpty() && state.storm == null) add(if (state.blocked.size == 1) "1 piedra" else "${state.blocked.size} piedras")
                if (state.twist != Twist.NONE) add(state.twist.hint)
                state.storm?.let { add("Cada ${it.everyMoves} mov. cae una piedra · dura ${it.lifeMoves}") }
            }
            if (rules.isNotEmpty()) Text(rules.joinToString("  ·  "), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.darker(0.7f))
            Text(LevelRules.threeStarHint(spec), fontSize = 11.sp, color = InkSecondary, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(16.dp))
        DialogButton("¡A jugar!", c.actionTone(), onClick = onStart)
    }
}


/** Por qué terminó la partida: cambia el título y el texto de las pantallas de derrota. */
enum class GameOverReason(private val normal: String, private val spooky: String, val body: String) {
    BOARD_FULL("¡Tablero lleno!", "¡Los fantasmas llenaron el tablero!", ""),
    OUT_OF_MOVES("¡Sin movimientos!", "¡Se acabaron los hechizos!", "Te quedaste sin movimientos antes de llegar a la meta. Prueba otro orden: cada jugada cuenta."),
    TIME_UP("¡Se acabó el tiempo!", "¡Dieron las doce!", "El reloj llegó a cero. Encadena fusiones: cada combinación te regala segundos.");

    val headline: String get() = if (com.korkoor.pardos.ui.design.Season.halloween) spooky else normal
}

fun BoardState.gameOverReason(): GameOverReason = when {
    outOfMoves -> GameOverReason.OUT_OF_MOVES
    maxTime != null && elapsedTime <= 0L -> GameOverReason.TIME_UP
    else -> GameOverReason.BOARD_FULL
}


/**
 * Plano en pequeño del tablero del nivel: las casillas, las piedras y las fichas con las que se empieza.
 * Se enseña en la tarjeta del nivel para que se vea el reto antes de jugarlo.
 */
@Composable
fun BoardMiniPreview(spec: com.korkoor.pardos.domain.level.LevelSpec, modifier: Modifier = Modifier, boardSize: Dp = 96.dp) {
    val c = spec.kind.accent()
    val stones = spec.stoneSet
    val starts = spec.startTiles.associate { (it.row to it.col) to it.value }
    Canvas(modifier.size(boardSize)) {
        val n = spec.boardSize
        val gap = size.width * 0.03f
        val cell = (size.width - gap * (n + 1)) / n
        drawRoundRect(Navy.copy(alpha = 0.08f), cornerRadius = CornerRadius(size.width * 0.12f), size = size)
        for (r in 0 until n) for (col in 0 until n) {
            val x = gap + col * (cell + gap)
            val y = gap + r * (cell + gap)
            val radius = CornerRadius(cell * 0.22f)
            when {
                (r to col) in stones -> {
                    drawRoundRect(Color(0xFF5E6678), Offset(x, y + cell * 0.05f), Size(cell, cell), radius)
                    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFC9CFDB), Color(0xFF8C95A8)), startY = y, endY = y + cell), Offset(x, y), Size(cell, cell), radius)
                    drawRoundRect(Color.White.copy(alpha = 0.35f), Offset(x + cell * 0.14f, y + cell * 0.12f), Size(cell * 0.42f, cell * 0.16f), CornerRadius(cell * 0.08f))
                }
                (r to col) in starts -> drawRoundRect(c.copy(alpha = 0.85f), Offset(x, y), Size(cell, cell), radius)
                else -> drawRoundRect(Color.White.copy(alpha = 0.85f), Offset(x, y), Size(cell, cell), radius)
            }
        }
    }
}
