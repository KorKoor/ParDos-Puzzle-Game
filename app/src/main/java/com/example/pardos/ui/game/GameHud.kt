package com.korkoor.pardos.ui.game

import androidx.compose.material.icons.rounded.CallMerge
import androidx.compose.material.icons.rounded.Whatshot
import com.korkoor.pardos.ui.design.accent
import com.korkoor.pardos.ui.design.*
import com.korkoor.pardos.ui.theme.inkColor
import androidx.compose.material.icons.rounded.SwipeRight
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Star
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import com.korkoor.pardos.ui.game.components.getTileColor
import com.korkoor.pardos.ui.game.components.getTileTextColor
import FloatingScore
import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import com.korkoor.pardos.ui.design.Icon
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import com.korkoor.pardos.ui.design.actionColor
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pardos.ui.game.components.SakuraBackgroundAnimation
import com.korkoor.pardos.R
import com.korkoor.pardos.domain.achievements.AchievementPopUp
import com.korkoor.pardos.domain.logic.Direction
import com.korkoor.pardos.domain.model.BoardState
import com.korkoor.pardos.domain.model.GameMode
import com.korkoor.pardos.ui.game.components.*
import com.korkoor.pardos.ui.game.logic.AdManager
import com.korkoor.pardos.ui.game.menu.PicnicBackgroundOptimized
import com.korkoor.pardos.ui.profile.ProfileSetupDialog
import com.korkoor.pardos.ui.theme.GameTheme
import com.korkoor.pardos.ui.theme.ThemeSelector
import com.korkoor.pardos.ui.theme.ThemeViewModel
import com.korkoor.pardos.data.local.ProfileManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun GameHeader(
    state: BoardState,
    currentTheme: GameTheme,
    modifier: Modifier = Modifier,
    extraTimes: Int = 0,
    onExtraTime: (() -> Unit)? = null,
    /** Reemplaza "JUGADOR n" (p. ej. "TU RETO" en duelos a distancia). */
    titleOverride: String? = null,
    /** Niveles de campaña ganados seguidos (0 = no se enseña). */
    winStreak: Int = 0,
    /** Corazones que quedan en la torre (null = no se está en la torre). */
    towerHearts: Int? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = ((currentTheme as? com.korkoor.pardos.ui.theme.GameTheme.Skinned)?.skin?.displayName ?: currentTheme.name).uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = currentTheme.inkColor.copy(alpha = 0.5f),
            letterSpacing = 4.sp
        )

        AnimatedLevelDisplay(
            level = state.currentLevel,
            textColor = currentTheme.inkColor,
            titleOverride = titleOverride,
            labelRes = when (state.gameMode) {
                GameMode.CARRERA -> R.string.stage_label
                GameMode.DUELO -> R.string.player_label
                else -> R.string.level_label
            }
        )

        Spacer(Modifier.height(6.dp))

        // En el duelo no hay meta que mostrar: solo cuenta el puntaje
        if (state.gameMode != GameMode.DUELO) {
            ObjectiveCard(
                targetPiece = state.levelLimit,
                boardSize = state.boardSize,
                progress = state.levelProgress,
                theme = currentTheme,
                goal = state.goal,
                goalCount = state.goalCount,
                score = state.score,
                goalStats = state.goalStats,
                tileValues = state.tiles.mapTo(HashSet()) { it.value }
            )
            LevelRuleChips(state, Modifier.padding(top = 6.dp))
            if (winStreak >= 2) FlamePill(winStreak, Modifier.padding(top = 6.dp))
            if (towerHearts != null) TowerHearts(towerHearts, Modifier.padding(top = 6.dp))
        }

        if (state.maxTime != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                Box(modifier = Modifier.scale(0.8f)) {
                    TimeDisplay(
                        elapsedTime = state.elapsedTime,
                        accentColor = currentTheme.accentColor,
                        textColor = currentTheme.inkColor
                    )
                }
                if (onExtraTime != null && extraTimes > 0 && !state.isGameOver && !state.isLevelCompleted && state.gameMode != GameMode.DUELO) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "+${com.korkoor.pardos.domain.economy.Economy.EXTRA_TIME_SECONDS}s · $extraTimes",
                        fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(currentTheme.actionColor)
                            .clickable(onClick = onExtraTime)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun TimerDisplay(
    seconds: Long,
    isLowTime: Boolean,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFFE07A5F),
    textColor: Color = com.korkoor.pardos.ui.design.Navy
) {
    val animatedTextColor by animateColorAsState(
        targetValue = if (isLowTime) accentColor else textColor.copy(alpha = 0.7f),
        animationSpec = tween(300),
        label = "TimerColor"
    )

    val isCritical = seconds <= 5_000 && isLowTime

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isCritical) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val shakeOffset by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake"
    )

    Row(
        modifier = modifier
            .scale(scale)
            .graphicsLayer {
                if (isCritical) translationX = shakeOffset
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Timer,
            contentDescription = stringResource(R.string.time_label),
            tint = animatedTextColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = seconds.formatTime(),
            color = animatedTextColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
internal fun TimeDisplay(
    elapsedTime: Long,
    modifier: Modifier = Modifier,
    accentColor: Color,
    textColor: Color
) {
    val isUrgent = elapsedTime <= 10_000
    val isCritical = elapsedTime <= 5_000

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isCritical) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val shakeOffset by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake"
    )

    Column(
        modifier = modifier
            .padding(top = 16.dp)
            .graphicsLayer {
                if (isCritical) {
                    scaleX = scale
                    scaleY = scale
                    translationX = shakeOffset
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isUrgent) stringResource(R.string.hurry_up) else stringResource(R.string.time_remaining),
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isUrgent) Color(0xFFE07A5F) else textColor.copy(alpha = 0.55f),
            letterSpacing = 1.2.sp
        )

        Text(
            text = elapsedTime.formatTime(),
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            color = if (isUrgent) Color(0xFFE07A5F) else textColor
        )
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")

@Composable
internal fun GameBoard(
    state: BoardState,
    selectedShapeType: String,
    viewModel: GameViewModel,
    haptic: HapticFeedback,
    currentTheme: GameTheme,
    onMoveSound: () -> Unit,
    modifier: Modifier = Modifier,
    guide: BoardGuide? = null
) {
    val gridSize = state.boardSize
    val isLargeGrid = gridSize >= 4

    val boardPadding = when {
        gridSize >= 5 -> 0.dp
        gridSize == 4 -> 2.dp
        else -> 8.dp
    }

    Box(
        modifier = modifier
            .padding(boardPadding)
            .background(
                color = currentTheme.surfaceColor.copy(alpha = 0.25f),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = if (isLargeGrid) 1.dp else 3.dp,
                color = currentTheme.accentColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // 🔥 OPTIMIZACIÓN: Solo se recrea el display si cambia el tamaño
        key(gridSize) {
            BoardDisplay(
                state = state,
                viewModel = viewModel,
                shapeType = selectedShapeType,
                haptic = haptic,
                currentTheme = currentTheme,
                onMoveSound = onMoveSound,
                modifier = Modifier.fillMaxSize(),
                guide = guide
            )
        }

        viewModel.floatingScores.forEach { score ->
            key(score.id) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val tileSize = maxWidth / gridSize

                    FloatingScore(
                        score = score,
                        tileSize = tileSize,
                        onFinished = { id -> viewModel.removeFloatingScore(id) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun GameFooter(
    state: BoardState,
    modifier: Modifier = Modifier
) {
    // En modos con tiempo, el reloj grande ya está en el encabezado: aquí solo mostramos movimientos y puntos
    val showClock = state.maxTime == null
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatCard(
            icon = Icons.Rounded.SwipeRight,
            value = state.moveCount.toString(),
            label = stringResource(R.string.moves_label),
            color = Sage,
            modifier = Modifier.weight(1f)
        )
        if (showClock) {
            StatCard(
                icon = Icons.Rounded.Timer,
                value = state.elapsedTime.formatTime(),
                label = stringResource(R.string.time_label),
                color = GemBlue,
                modifier = Modifier.weight(1f),
                animateValue = false
            )
        }
        StatCard(
            icon = Icons.Rounded.Star,
            value = state.score.toString(),
            label = stringResource(R.string.points_label),
            color = Gold,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
internal fun AnimatedLevelDisplay(
    level: Int,
    modifier: Modifier = Modifier,
    textColor: Color,
    labelRes: Int = R.string.level_label,
    titleOverride: String? = null
) {
    AnimatedContent(
        targetState = level,
        transitionSpec = {
            slideInVertically { height -> height } + fadeIn() togetherWith
                    slideOutVertically { height -> -height } + fadeOut()
        },
        label = "LevelSlotAnimation"
    ) { targetLevel ->
        Text(
            text = titleOverride ?: stringResource(labelRes, targetLevel),
            modifier = modifier,
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            color = textColor,
            letterSpacing = 1.sp
        )
    }
}

@Composable
internal fun ObjectiveCard(
    targetPiece: Int,
    boardSize: Int,
    progress: Float,
    theme: GameTheme,
    modifier: Modifier = Modifier,
    goal: com.korkoor.pardos.domain.level.LevelGoal = com.korkoor.pardos.domain.level.LevelGoal.REACH_TILE,
    goalCount: Int = 1,
    score: Int = 0,
    goalStats: com.korkoor.pardos.domain.level.GoalStats = com.korkoor.pardos.domain.level.GoalStats(),
    tileValues: Set<Int> = emptySet()
) {
    val isScore = goal == com.korkoor.pardos.domain.level.LevelGoal.SCORE
    val isMerges = goal == com.korkoor.pardos.domain.level.LevelGoal.MERGES
    val isCombo = goal == com.korkoor.pardos.domain.level.LevelGoal.COMBO
    val isLadder = goal == com.korkoor.pardos.domain.level.LevelGoal.LADDER
    val isHarvest = goal == com.korkoor.pardos.domain.level.LevelGoal.HARVEST
    val plainTile = !isScore && !isMerges && !isCombo
    // La barra usa escala logarítmica: duplicar la ficha mayor siempre se siente como un avance parejo
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 120f),
        label = "ObjectiveProgress"
    )
    JellySurface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mini ficha con la meta (o una estrella si la meta son puntos); "×2" cuando hacen falta dos fichas
            Box(contentAlignment = Alignment.TopEnd) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                isMerges -> com.korkoor.pardos.domain.level.LevelKind.MARATHON.accent()
                                isCombo -> com.korkoor.pardos.domain.level.LevelKind.COMBO.accent()
                                isScore -> Gold
                                else -> getTileColor(targetPiece, theme)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(0f to Color.White.copy(alpha = 0.30f), 0.5f to Color.Transparent)
                        )
                    )
                    if (isMerges) {
                        Icon(Icons.Rounded.CallMerge, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    } else if (isCombo) {
                        Icon(Icons.Rounded.Whatshot, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    } else if (isScore) {
                        Icon(Icons.Rounded.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = "$targetPiece",
                            fontSize = if (targetPiece >= 1000) 12.sp else 15.sp,
                            fontWeight = FontWeight.Black,
                            color = getTileTextColor(targetPiece)
                        )
                    }
                }
                if (goalCount > 1 && plainTile && !isLadder) {
                    Text(
                        text = "×$goalCount",
                        fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White,
                        modifier = Modifier.offset(x = 6.dp, y = (-6).dp).clip(RoundedCornerShape(50)).background(Navy).padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = when {
                        isScore -> "PUNTOS  $score / $targetPiece"
                        isMerges -> "FUSIONES  ${goalStats.merges} / $targetPiece"
                        isCombo -> "COMBO DE $targetPiece  ·  MEJOR ${goalStats.bestChain}" + if (goalCount > 1) "  ·  ${goalStats.comboHits.coerceAtMost(goalCount)}/$goalCount" else ""
                        isLadder -> "ESCALERA"
                        isHarvest -> "COSECHA  ${goalStats.harvested} / $goalCount"
                        else -> stringResource(R.string.objective_title).uppercase()
                    },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Navy.copy(alpha = 0.45f),
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(5.dp))
                if (isLadder) {
                    // Cada peldaño se enciende cuando ya está en el tablero
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.width(120.dp)) {
                        com.korkoor.pardos.domain.level.ladderRungs(targetPiece, goalCount).forEach { rung ->
                            val on = rung in tileValues
                            Box(
                                modifier = Modifier.weight(1f).height(18.dp).clip(RoundedCornerShape(5.dp))
                                    .background(if (on) getTileColor(rung, theme) else Navy.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("$rung", fontSize = 8.sp, fontWeight = FontWeight.Black, color = if (on) getTileTextColor(rung) else Navy.copy(alpha = 0.4f), maxLines = 1)
                            }
                        }
                    }
                } else Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Navy.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animated.coerceAtLeast(0.03f))
                            .background(Brush.horizontalGradient(listOf(SageLight, Gold)), CircleShape)
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = "${boardSize}×$boardSize",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = Navy.copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
internal fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    animateValue: Boolean = true
) {
    JellySurface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(5.dp))
                if (animateValue) {
                    BouncingText(text = value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Navy)
                } else {
                    Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Navy)
                }
            }
            Text(
                text = label.uppercase(),
                fontSize = 9.sp,
                color = Navy.copy(alpha = 0.45f),
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )
        }
    }
}
