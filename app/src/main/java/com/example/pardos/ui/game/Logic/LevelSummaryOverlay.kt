package com.korkoor.pardos.ui.game.components

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

import com.korkoor.pardos.ui.design.ToyButton
import com.korkoor.pardos.ui.design.ToyTextButton
import com.korkoor.pardos.ui.design.JellySurface
import com.korkoor.pardos.ui.design.actionColor

import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.PlayCircle
import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import com.korkoor.pardos.ui.design.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.R
import com.korkoor.pardos.ui.theme.GameTheme
import kotlinx.coroutines.delay

@Composable
fun LevelSummaryOverlay(
    modeName: String,
    base: Int,
    moves: Int,
    timeElapsed: Long,
    bestMoves: Int,
    bestTime: Long,
    stars: Int,
    currentTheme: GameTheme,
    coinsEarned: Int = 0,
    canDouble: Boolean = false,
    onDouble: () -> Unit = {},
    /** VIP: duplicar las monedas es gratis y sin anuncio. */
    vip: Boolean = false,
    /** Mejoró su marca en un nivel que ya había superado. */
    newRecord: Boolean = false,
    bonus: com.korkoor.pardos.data.local.GameBonus = com.korkoor.pardos.data.local.GameBonus(),
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    /** Avance automático (sin que el jugador toque): nunca debe mostrar un anuncio de pantalla completa. */
    onAutoNext: () -> Unit = onDismiss,
    onShare: (() -> Unit)? = null,
    nextGoal: com.korkoor.pardos.domain.retention.NextGoal? = null,
    /** Cómo conseguir las 3 estrellas (solo se enseña si faltó alguna). */
    starHint: String? = null,
    // --- Flow: racha, hito, "siguiente nivel" y avance automático ---
    streak: Int = 0,
    streakBonusPct: Int = 0,
    flowBonusPct: Int = 0,
    milestone: com.korkoor.pardos.domain.flow.WinStreak.Milestone? = null,
    teaser: com.korkoor.pardos.domain.flow.NextLevelTeaser.Teaser? = null,
    nextKindSeen: Boolean = true,
    /** Milisegundos hasta pasar solo al siguiente nivel (null = no hay avance automático). */
    autoNextMs: Int? = null
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var isVisible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 200f),
        label = "CardPop"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "CardFade"
    )

    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Avance automático: tras ver las estrellas empieza una cuenta atrás; cualquier toque la cancela
    var autoCancelled by remember { mutableStateOf(false) }
    var autoProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(autoNextMs, autoCancelled) {
        if (autoNextMs != null && !autoCancelled) {
            delay(2400)
            val steps = (autoNextMs / 50).coerceAtLeast(1)
            for (i in 1..steps) {
                autoProgress = i / steps.toFloat()
                delay(50)
            }
            onAutoNext()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(enabled = false) {}
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        autoCancelled = true
                        autoProgress = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // 🚀 EL FIX HERMOSO: Disparamos el confeti AQUÍ para que se vea encima del fondo negro
        // Si tu función se llama diferente (ej. ConfettiEffect), cámbiala aquí:
        if (stars > 0) {
            VictoryConfetti()
        }

        JellySurface(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = 10.dp)
                .fillMaxWidth(if (isLandscape) 0.85f else 0.88f)
                .scale(scale)
                .alpha(alpha)
                .shadow(30.dp, RoundedCornerShape(32.dp), spotColor = currentTheme.actionColor.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(32.dp),
            color = Color.White,
            border = BorderStroke(2.dp, Brush.linearGradient(
                colors = listOf(Color.White, currentTheme.actionColor.copy(alpha = 0.3f))
            ))
        ) {
            Column(
                modifier = Modifier.padding(
                    vertical = if (isLandscape) 20.dp else 32.dp,
                    horizontal = 24.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ... (Todo el resto de tu código de Column se mantiene igual)
                if (isLandscape) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AnimatedStarsRow(stars, currentTheme)
                            Spacer(Modifier.height(12.dp))
                            VictoryHeader(stars, modeName, base, currentTheme)
                            if (newRecord) RecordBadge()
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(180.dp)
                                .background(Color.LightGray.copy(alpha = 0.4f))
                        )

                        Column(
                            modifier = Modifier.weight(1.3f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Column(
                                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                StatsRow(moves, timeElapsed)
                                StarHintLine(starHint)
                                CoinsEarnedChip(coinsEarned, canDouble, vip, onDouble)
                                BonusChips(bonus)
                                com.korkoor.pardos.ui.game.components.FlowSummaryExtras(
                                    streak, streakBonusPct, flowBonusPct, milestone, teaser, nextKindSeen,
                                    if (autoCancelled) 0f else autoProgress, currentTheme.actionColor
                                )
                                NextGoalRow(nextGoal, currentTheme)
                                Spacer(Modifier.height(16.dp))
                                PersonalRecordsBox(currentTheme, bestMoves, bestTime)
                            }
                            Spacer(Modifier.height(14.dp))
                            ActionButtons(currentTheme, onRetry, onDismiss, onShare)
                        }
                    }
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(currentTheme.accentColor.copy(alpha = 0.2f), Color.Transparent)
                                    )
                                )
                        )
                        AnimatedStarsRow(stars, currentTheme)
                    }

                    Spacer(Modifier.height(8.dp))
                    VictoryHeader(stars, modeName, base, currentTheme)
                    if (newRecord) RecordBadge()
                    Spacer(modifier = Modifier.height(14.dp))
                    StatsRow(moves, timeElapsed)
                    // Si no cabe todo (teléfonos pequeños), lo de en medio se desplaza; el botón SIGUIENTE siempre queda a la vista
                    Column(
                        modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        StarHintLine(starHint)
                        CoinsEarnedChip(coinsEarned, canDouble, vip, onDouble)
                        BonusChips(bonus)
                        com.korkoor.pardos.ui.game.components.FlowSummaryExtras(
                            streak, streakBonusPct, flowBonusPct, milestone, teaser, nextKindSeen,
                            if (autoCancelled) 0f else autoProgress, currentTheme.actionColor
                        )
                        NextGoalRow(nextGoal, currentTheme)
                        // Con el "siguiente nivel" a la vista se omiten los récords (se guardan igual en el perfil)
                        if (teaser == null) {
                            Spacer(modifier = Modifier.height(18.dp))
                            PersonalRecordsBox(currentTheme, bestMoves, bestTime)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    ActionButtons(currentTheme, onRetry, onDismiss, onShare)
                }
            }
        }
    }
}

/** "¡NUEVO RÉCORD!": brota con un rebote y suena su fanfarria corta cuando ya se vieron las estrellas y las monedas. */
@Composable
private fun RecordBadge() {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(2000L)
        com.korkoor.pardos.audio.GameAudio.play(com.korkoor.pardos.audio.Sfx.NEW_BEST)
        pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 380f))
    }
    Spacer(Modifier.height(8.dp))
    com.korkoor.pardos.ui.design.CozyText(
        text = "★ ¡NUEVO RÉCORD!", fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp,
        color = Color(0xFFB07A10),
        modifier = Modifier
            .graphicsLayer { scaleX = pop.value; scaleY = pop.value; alpha = pop.value.coerceIn(0f, 1f) }
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFF2B84B).copy(alpha = 0.22f))
            .padding(horizontal = 14.dp, vertical = 5.dp)
    )
}

@Composable
private fun StarHintLine(text: String?) {
    if (text == null) return
    Spacer(Modifier.height(8.dp))
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = com.korkoor.pardos.ui.design.Navy.copy(alpha = 0.5f),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}

@Composable
private fun VictoryHeader(stars: Int, modeName: String, base: Int, currentTheme: GameTheme) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (com.korkoor.pardos.ui.design.Season.halloween) (if (stars >= 2) "¡MONSTRUOSO!" else "¡BUEN TRUCO!")
            else if (stars >= 2) stringResource(R.string.victory_mastered).uppercase()
            else stringResource(R.string.victory_well_done).uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = currentTheme.actionColor,
            letterSpacing = 3.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (modeName.contains("Tabla") || modeName.contains("Multi"))
                stringResource(R.string.mode_tables_with_value, base)
            else modeName,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            color = com.korkoor.pardos.ui.design.Navy,
            letterSpacing = (-0.5).sp
        )
    }
}

@Composable
private fun StatsRow(moves: Int, timeElapsed: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatDetail(
            label = stringResource(R.string.moves_label),
            value = moves.toString(),
            color = com.korkoor.pardos.ui.design.Navy,
            delayMillis = 400
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(40.dp)
                .background(Color.LightGray.copy(alpha = 0.5f))
        )
        StatDetail(
            label = stringResource(R.string.time_label),
            value = formatTime(timeElapsed),
            color = com.korkoor.pardos.ui.design.Navy,
            delayMillis = 600
        )
    }
}

@Composable
private fun AnimatedStarsRow(stars: Int, currentTheme: GameTheme) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val isFilled = index < stars
            var startAnim by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay(350L + index * 330L)
                startAnim = true
                // Cada estrella llena suena una nota más alta que la anterior
                if (isFilled) com.korkoor.pardos.audio.GameAudio.play(when (index) { 0 -> com.korkoor.pardos.audio.Sfx.STAR_1; 1 -> com.korkoor.pardos.audio.Sfx.STAR_2; else -> com.korkoor.pardos.audio.Sfx.STAR_3 })
            }

            val scale by animateFloatAsState(
                targetValue = if (startAnim && isFilled) 1f else if (startAnim) 0.8f else 0f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f),
                label = "StarAnim"
            )

            val rotation by animateFloatAsState(
                targetValue = if (startAnim && isFilled) 0f else -30f,
                animationSpec = tween(500),
                label = "StarRot"
            )

            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (isFilled) Color(0xFFFFD700) else Color.LightGray.copy(alpha = 0.3f),
                    modifier = Modifier
                        .size(58.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            rotationZ = rotation
                        }
                        .padding(horizontal = 4.dp)
                        .then(if (isFilled) Modifier.shadow(8.dp, CircleShape, spotColor = Color(0xFFFFD700)) else Modifier)
                )
            }
        }
    }
}

@Composable
private fun PersonalRecordsBox(currentTheme: GameTheme, bestMoves: Int, bestTime: Long) {
    // 🔥 FIX: Manejamos los guiones como String y evitamos pasarlos a stringResource
    // para prevenir la IllegalFormatConversionException (%d != String)
    val displayTime = if (bestTime <= 0L || bestTime == Long.MAX_VALUE) "--:--" else formatTime(bestTime)
    val displayMoves = if (bestMoves <= 0 || bestMoves == Int.MAX_VALUE) "--" else bestMoves.toString()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(currentTheme.actionColor.copy(alpha = 0.08f))
            .border(1.dp, currentTheme.actionColor.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(vertical = 14.dp, horizontal = 16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.personal_records).uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = currentTheme.actionColor.copy(alpha = 0.8f),
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(6.dp))

            // 🛠️ FIX FINAL: Concatenamos los valores ya formateados como String
            // Esto evita que el sistema truene si el XML espera un número (%d)
            Text(
                text = "$displayMoves mov.  |  $displayTime",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = com.korkoor.pardos.ui.design.Navy.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun ActionButtons(currentTheme: GameTheme, onRetry: () -> Unit, onDismiss: () -> Unit, onShare: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = onRetry,
            modifier = Modifier
                .width(60.dp)
                .height(60.dp),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(2.dp, currentTheme.actionColor.copy(alpha = 0.2f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = currentTheme.actionColor
            )
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = stringResource(R.string.retry),
                modifier = Modifier.size(24.dp)
            )
        }

        if (onShare != null) {
            OutlinedButton(
                onClick = onShare,
                modifier = Modifier
                    .width(60.dp)
                    .height(60.dp),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(2.dp, currentTheme.actionColor.copy(alpha = 0.2f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = currentTheme.actionColor
                )
            ) {
                Icon(Icons.Default.Share, contentDescription = "Compartir", modifier = Modifier.size(24.dp))
            }
        }

        ToyButton(
            onClick = onDismiss,
            modifier = Modifier
                .weight(1f)
                .height(60.dp)
                .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = currentTheme.actionColor.copy(alpha = 0.4f)),
            colors = ButtonDefaults.buttonColors(containerColor = currentTheme.actionColor),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                text = stringResource(R.string.next_button),
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                letterSpacing = 1.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun StatDetail(
    label: String,
    value: String,
    color: Color,
    delayMillis: Int = 0
) {
    var startAnim by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        startAnim = true
    }

    val alpha by animateFloatAsState(targetValue = if (startAnim) 1f else 0f, animationSpec = tween(500), label = "StatAlpha")
    val offsetY by animateFloatAsState(targetValue = if (startAnim) 0f else 20f, animationSpec = spring(dampingRatio = 0.6f), label = "StatSlide")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .graphicsLayer {
                this.alpha = alpha
                translationY = offsetY
            }
            .clearAndSetSemantics { contentDescription = "$value, $label" }
    ) {
        Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = color, letterSpacing = (-1).sp)
        Text(text = label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color.copy(alpha = 0.4f), letterSpacing = 1.5.sp)
    }
}

fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"

    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return if (minutes > 99) {
        "99:59"
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

@Composable
private fun CoinsEarnedChip(coins: Int, canDouble: Boolean, vip: Boolean, onDouble: () -> Unit) {
    if (coins <= 0) return
    // Las monedas suben contando, con un tic cada vez más agudo, y al final tintinean
    var shown by remember { mutableIntStateOf(0) }
    LaunchedEffect(coins) {
        val from = shown.coerceAtMost(coins)
        if (from == 0) delay(1250L)
        val steps = 12
        for (i in 1..steps) {
            shown = from + (coins - from) * i / steps
            com.korkoor.pardos.audio.GameAudio.play(com.korkoor.pardos.audio.Sfx.XP_TICK, 1f, 0.9f + 0.05f * i)
            delay(48)
        }
        shown = coins
        com.korkoor.pardos.audio.GameAudio.play(com.korkoor.pardos.audio.Sfx.COIN)
    }
    // Se ofreció duplicar y ya se hizo: se enseña como "hecho" en lugar de dejar un hueco
    var offered by remember { mutableStateOf(false) }
    LaunchedEffect(canDouble) { if (canDouble) offered = true }

    Spacer(modifier = Modifier.height(14.dp))
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFE0A93B).copy(alpha = 0.14f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.MonetizationOn,
                contentDescription = null,
                tint = Color(0xFFE0A93B),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "+$shown", fontSize = 18.sp, fontWeight = FontWeight.Black, color = com.korkoor.pardos.ui.design.Navy)
            if (offered && !canDouble) {
                Spacer(modifier = Modifier.width(8.dp))
                Text("x2", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF6C63FF),
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF6C63FF).copy(alpha = 0.14f)).padding(horizontal = 8.dp, vertical = 2.dp))
            }
        }
        if (canDouble) {
            val extra = com.korkoor.pardos.domain.economy.Economy.doubleBonus(coins)
            Spacer(modifier = Modifier.height(10.dp))
            // Llamativo pero honesto: se ve exactamente cuánto se gana y que se pide con un anuncio (o gratis con VIP)
            com.korkoor.pardos.ui.design.WatchAdButton(
                label = "DUPLICAR MONEDAS",
                sublabel = if (vip) "VIP: gratis, sin anuncio" else "+$extra ● viendo un anuncio corto",
                tag = "x2",
                onClick = onDouble,
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF6C63FF),
                pulse = true,
                minHeight = 52.dp,
                adFree = vip
            )
        }
    }
}


/** Extras de retención de la partida: primera victoria del día, hucha y puntos del pase. */
@Composable
private fun BonusChips(bonus: com.korkoor.pardos.data.local.GameBonus) {
    val chips = buildList {
        if (bonus.firstWinCoins > 0) add("Primera victoria del día  +${bonus.firstWinCoins} ●" to Color(0xFFE0A93B))
        if (bonus.piggyGems > 0) add("Hucha  +${bonus.piggyGems} ◆" to Color(0xFF4E8FA6))
        if (bonus.seasonPoints > 0) add("Pase de temporada  +${bonus.seasonPoints}" to Color(0xFF6C63FF))
        bonus.newSkins.forEach { add("¡Skin ${it.displayName} desbloqueada!" to Color(0xFFE0A93B)) }
    }
    if (chips.isEmpty()) return
    Spacer(modifier = Modifier.height(10.dp))
    // Las fichas pequeñas comparten fila cuando caben (ahorra altura en la pantalla de victoria)
    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        chips.forEach { (text, color) ->
            com.korkoor.pardos.ui.design.CozyText(
                text = text, fontSize = 11.sp, fontWeight = FontWeight.Black, color = color,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
    }
}


/** Una línea con la meta más cercana: invita a jugar "una más". */
@Composable
private fun NextGoalRow(goal: com.korkoor.pardos.domain.retention.NextGoal?, theme: GameTheme) {
    if (goal == null) return
    Spacer(Modifier.height(10.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.actionColor.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("SIGUIENTE META", fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, color = com.korkoor.pardos.ui.design.Navy.copy(alpha = 0.5f))
            Text(goal.title, fontSize = 14.sp, fontWeight = FontWeight.Black, color = com.korkoor.pardos.ui.design.Navy, maxLines = 1)
            Text(goal.detail, fontSize = 11.sp, color = com.korkoor.pardos.ui.design.Navy.copy(alpha = 0.6f), maxLines = 1)
        }
        if (goal.kind != com.korkoor.pardos.domain.retention.GoalKind.CHEST_READY) {
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier.width(56.dp).height(8.dp).clip(CircleShape).background(com.korkoor.pardos.ui.design.Navy.copy(alpha = 0.10f))
            ) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(goal.progress.coerceAtLeast(0.04f)).background(theme.actionColor, CircleShape))
            }
        }
    }
}
