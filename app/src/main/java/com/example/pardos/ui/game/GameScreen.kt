@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.korkoor.pardos.ui.game

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
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
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
import com.korkoor.pardos.domain.model.nameResId
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

@SuppressLint("UnusedContentLambdaTargetStateParameter", "UnusedBoxWithConstraintsScope")
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    themeViewModel: ThemeViewModel,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.boardState.collectAsStateWithLifecycle()
    val currentTheme = themeViewModel.currentTheme
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val activity = context as? Activity

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isLargeGrid = state.boardSize >= 5

    var selectedShapeType by rememberSaveable { mutableStateOf("Cuadrado") }
    var showExitDialog by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }
    var displayedLevel by remember { mutableIntStateOf(state.currentLevel) }

    LaunchedEffect(state.currentLevel) {
        if (state.currentLevel > displayedLevel) {
            displayedLevel = state.currentLevel
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshCurrentLevelDifficulty()
    }

    // 🔥 OPTIMIZACIÓN: Audio ultra rápido sin lag ni consumir mucha RAM
    val audioManager = remember { GameAudioManager(context) }

    // 🔥 OPTIMIZACIÓN: Solo recalcula el gradiente si el tema cambia
    val bgGradient = remember(currentTheme) { Brush.verticalGradient(colors = currentTheme.colors) }

    val isTimeLow = state.maxTime != null && state.elapsedTime <= 10_000L

    val shouldBlur = viewModel.showLevelSummary || state.isGameOver || showExitDialog || showThemeMenu
    val blurRadius by animateDpAsState(
        targetValue = if (shouldBlur) 16.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "DynamicBlur"
    )

    LaunchedEffect(state.levelLimit) { themeViewModel.updateLevel(state.levelLimit) }

    DisposableEffect(Unit) {
        audioManager.initialize()
        onDispose { audioManager.release() }
    }

    BackHandler(enabled = !state.isLevelCompleted) {
        if (state.moveCount > 0) showExitDialog = true else onBackToMenu()
    }

    Column(modifier = modifier.fillMaxSize().background(bgGradient)) {

        Box(modifier = Modifier.weight(1f)) {

            PicnicBackgroundOptimized(
                color = if (isTimeLow) Color(0xFFE07A5F).copy(alpha = 0.15f)
                else currentTheme.accentColor.copy(alpha = 0.05f)
            )

            SakuraBackgroundAnimation(density = 0.5f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(blurRadius)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val maxHeight = maxHeight
                    val maxWidth = maxWidth

                    if (isLandscape) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                GameTopBar(
                                    selectedShapeType = selectedShapeType,
                                    onShapeSelected = { selectedShapeType = it },
                                    onBackToMenu = { if (state.moveCount > 0) showExitDialog = true else onBackToMenu() }
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                androidx.compose.animation.AnimatedVisibility(
                                    visible = true,
                                    enter = slideInHorizontally { -it } + fadeIn()
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(32.dp))
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                showThemeMenu = true
                                            }
                                            .padding(8.dp)
                                    ) {
                                        GameHeader(
                                            state = state.copy(currentLevel = if (state.currentLevel > displayedLevel) state.currentLevel else displayedLevel),
                                            currentTheme = currentTheme
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(2.2f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                val boardSize = minOf(maxHeight.value, maxWidth.value * 0.7f).dp * (if (isLargeGrid) 0.98f else 0.92f)

                                Box(
                                    modifier = Modifier
                                        .size(boardSize)
                                        .shadow(30.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    GameBoard(
                                        state = state,
                                        selectedShapeType = selectedShapeType,
                                        viewModel = viewModel,
                                        haptic = haptic,
                                        currentTheme = currentTheme,
                                        onMoveSound = { audioManager.playMoveSound() },
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    ComboIndicator(
                                        count = viewModel.comboCount.value,
                                        accentColor = currentTheme.accentColor
                                    )

                                    if (state.showTutorialHand) {
                                        TutorialHand(direction = Direction.RIGHT)
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                StatCard(
                                    icon = Icons.Default.Flag,
                                    value = state.moveCount.toString(),
                                    label = stringResource(R.string.moves_label),
                                    color = Color(0xFF81B29A)
                                )
                                Spacer(Modifier.height(12.dp))

                                if (state.score > 0) {
                                    StatCard(
                                        icon = Icons.Default.Flag,
                                        value = state.score.toString(),
                                        label = stringResource(R.string.points_label),
                                        color = Color(0xFFE07A5F)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                }

                                TimerDisplay(
                                    seconds = state.elapsedTime,
                                    isLowTime = state.gameMode == GameMode.DESAFIO && state.elapsedTime in 1..10_000,
                                    modifier = Modifier.scale(0.9f)
                                )

                                Spacer(Modifier.height(24.dp))

                                if (state.allowPowerUps && !viewModel.showLevelSummary && !state.isGameOver && !state.isLevelCompleted) {
                                    PowerUpSection(viewModel, haptic, activity)
                                }
                            }
                        }

                    } else {
                        // --- MODO VERTICAL (PORTRAIT) ---
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .navigationBarsPadding(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            androidx.compose.animation.AnimatedVisibility(visible = true, enter = slideInVertically { -it } + fadeIn()) {
                                GameTopBar(
                                    selectedShapeType = selectedShapeType,
                                    onShapeSelected = { selectedShapeType = it },
                                    onBackToMenu = { if (state.moveCount > 0) showExitDialog = true else onBackToMenu() }
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = if (isLargeGrid) 10.dp else 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(32.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showThemeMenu = true
                                        }
                                        .padding(bottom = if (isLargeGrid) 8.dp else 16.dp)
                                ) {
                                    AnimatedContent(
                                        targetState = if (state.currentLevel > displayedLevel) state.currentLevel else displayedLevel,
                                        transitionSpec = {
                                            slideInVertically { height -> height } + fadeIn() togetherWith
                                                    slideOutVertically { height -> -height } + fadeOut()
                                        },
                                        label = "HeaderTransition"
                                    ) { targetLevel ->
                                        GameHeader(
                                            state = state.copy(currentLevel = targetLevel),
                                            currentTheme = currentTheme
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .size(12.dp, 3.dp)
                                            .background(currentTheme.accentColor.copy(alpha = 0.4f), CircleShape)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .aspectRatio(1f)
                                        .fillMaxWidth(if (isLargeGrid) 0.98f else 0.92f)
                                        .shadow(30.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    GameBoard(
                                        state = state,
                                        selectedShapeType = selectedShapeType,
                                        viewModel = viewModel,
                                        haptic = haptic,
                                        currentTheme = currentTheme,
                                        onMoveSound = { audioManager.playMoveSound() },
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    ComboIndicator(
                                        count = viewModel.comboCount.value,
                                        accentColor = currentTheme.accentColor
                                    )

                                    if (state.showTutorialHand) {
                                        TutorialHand(direction = Direction.RIGHT)
                                    }
                                }
                            }

                            androidx.compose.animation.AnimatedVisibility(visible = true, enter = slideInVertically { it } + fadeIn()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    GameFooter(state = state)

                                    if (state.allowPowerUps && !viewModel.showLevelSummary && !state.isGameOver && !state.isLevelCompleted) {
                                        PowerUpSection(viewModel, haptic, activity, Modifier.fillMaxWidth())
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- OVERLAYS Y POPUPS ---
            if (state.isLevelCompleted) VictoryConfetti()

            androidx.compose.animation.AnimatedVisibility(
                visible = viewModel.showLevelSummary,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                val stats = viewModel.getBestStats(state.currentLevel)
                LevelSummaryOverlay(
                    modeName = stringResource(state.gameMode.nameResId),
                    base = viewModel.currentMultiplierBase,
                    moves = state.moveCount,
                    // handleLevelVictory ya deja en elapsedTime el tiempo USADO (no el restante)
                    timeElapsed = state.elapsedTime,
                    bestMoves = stats.first,
                    bestTime = stats.second,
                    stars = state.starsEarned,
                    currentTheme = currentTheme,
                    onRetry = { viewModel.retryLevel() },
                    onDismiss = { viewModel.nextLevel() }
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = state.isGameOver,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                if (viewModel.loadingAdType == "REVIVE") {
                    AdLoadingOverlay(currentTheme)
                }
                else if (state.secondChanceUsed == false) {
                    SecondChanceOverlay(
                        onUseSecondChance = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            activity?.let { act ->
                                AdManager.showRewardedAd(act) {
                                    viewModel.grantAdReward("REVIVE")
                                }
                            }
                        },
                        onCancel = { viewModel.retryLevel() },
                        currentTheme = currentTheme
                    )
                }
                else {
                    GameOverOverlay(onRestart = { viewModel.retryLevel() }, currentTheme = currentTheme)
                }
            }

            AchievementManagerPopup(viewModel = viewModel)

            if (showExitDialog) {
                ExitGameDialog(
                    onConfirm = { onBackToMenu() },
                    onDismiss = { showExitDialog = false },
                    currentTheme = currentTheme
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showThemeMenu,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { showThemeMenu = false },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .padding(16.dp),
                        color = Color.White.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(32.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.visual_style),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF3D405B),
                                letterSpacing = 2.sp
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "COLORES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3D405B).copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ThemeSelector(viewModel = themeViewModel)

                            Spacer(modifier = Modifier.height(32.dp))

                            Button(
                                onClick = { showThemeMenu = false },
                                colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accentColor),
                                shape = RoundedCornerShape(32.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            ) {
                                Text(
                                    stringResource(R.string.ready),
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
            if (viewModel.showProfileSetupRedirect) {
                ProfileSetupDialog(
                    onProfileSaved = { nombreIngresado, avatarSeleccionadoId ->
                        val profileManager = ProfileManager(context)
                        val perfilActual = profileManager.getProfile()
                        profileManager.saveProfile(
                            perfilActual.copy(
                                name = nombreIngresado,
                                avatarId = avatarSeleccionadoId
                            )
                        )
                        viewModel.onProfileSetupCompleted()
                    }
                )
            }
            if (viewModel.isSelectModeActive) {
                SelectionModeOverlay(
                    isManualMerge = viewModel.pendingPowerUpType == "MANUAL_MERGE",
                    accentColor = currentTheme.accentColor,
                    onCancel = { viewModel.cancelSelectMode() }
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// COMPONENTES AUXILIARES Y OPTIMIZACIONES
// -----------------------------------------------------------------------------

@Composable
fun FloatingScore(
    score: FloatingScoreModel,
    tileSize: Dp,
    onFinished: (String) -> Unit
) {
    val animState = remember { Animatable(0f) }

    LaunchedEffect(score.id) {
        animState.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
        onFinished(score.id)
    }

    val floatUpDistance = 80.dp
    val currentOffset = tileSize * 0.2f - (floatUpDistance * animState.value)
    val currentAlpha = 1f - animState.value
    val currentScale = 0.5f + (animState.value * 0.5f)

    val xPos = (tileSize * score.col) + (tileSize / 3)
    val yPos = (tileSize * score.row) + (tileSize / 2)

    Text(
        text = "+${score.value}",
        color = Color(0xFF3D405B).copy(alpha = currentAlpha),
        fontSize = 24.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
            .offset(x = xPos, y = yPos + currentOffset)
            .scale(currentScale)
            .alpha(currentAlpha)
    )
}

fun Long.formatTime(): String {
    val totalSeconds = this / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

val GameMode.displayResId: Int
    get() = when (this) {
        GameMode.CLASICO -> R.string.mode_classic
        GameMode.DESAFIO -> R.string.mode_challenge
        GameMode.ZEN -> R.string.mode_zen
        GameMode.TABLAS -> R.string.mode_tables
        else -> R.string.mode_classic
    }

internal val GameMode.color: Color
    get() = when (this) {
        GameMode.CLASICO -> Color(0xFF81B29A)
        GameMode.DESAFIO -> Color(0xFFE07A5F)
        GameMode.ZEN -> Color(0xFF6C63FF)
        GameMode.TABLAS -> Color(0xFF6C63FF)
        else -> Color(0xFF3D405B)
    }

internal fun HapticFeedback.performHapticFeedback() {
    try {
        performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
    } catch (e: Exception) {
    }
}