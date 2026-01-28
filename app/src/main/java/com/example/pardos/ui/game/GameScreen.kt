@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.korkoor.pardos.ui.game

import FloatingScore
import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Configuration
import android.media.MediaPlayer
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
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
import com.korkoor.pardos.ui.game.components.*
import com.korkoor.pardos.ui.game.logic.AdManager
import com.korkoor.pardos.ui.game.menu.PicnicBackgroundOptimized
import com.korkoor.pardos.ui.theme.GameTheme
import com.korkoor.pardos.ui.theme.ThemeSelector
import com.korkoor.pardos.ui.theme.ThemeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ✅ DEFINICIÓN DE ENUM AL INICIO PARA EVITAR ERRORES DE REFERENCIA
// Asegúrate de que ShapeType esté definido en este paquete o impórtalo correctamente si está en otro archivo.

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

    // Estado para la forma seleccionada
    var selectedShapeType by rememberSaveable { mutableStateOf("Cuadrado") }

    var showExitDialog by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }

    // Evita retroceso visual en el nivel mostrado
    var displayedLevel by remember { mutableIntStateOf(state.currentLevel) }

    LaunchedEffect(state.currentLevel) {
        if (state.currentLevel > displayedLevel) {
            displayedLevel = state.currentLevel
        }
    }

    // Refrescar dificultad al iniciar
    LaunchedEffect(Unit) {
        viewModel.refreshCurrentLevelDifficulty()
    }

    val audioManager = remember { GameAudioManager(context) }
    val bgGradient = remember(currentTheme) { Brush.verticalGradient(colors = currentTheme.colors) }
    val isTimeLow = state.maxTime != null && state.elapsedTime <= 10L

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

    Box(modifier = modifier.fillMaxSize().background(bgGradient)) {

        // 1. Fondo Estático
        PicnicBackgroundOptimized(
            color = if (isTimeLow) Color(0xFFE07A5F).copy(alpha = 0.15f)
            else currentTheme.accentColor.copy(alpha = 0.05f)
        )

        // 🌸 2. EFECTO SAKURA
        SakuraBackgroundAnimation(density = 0.5f)

        // 3. Contenido del Juego (Con Blur dinámico)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(blurRadius)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val maxHeight = maxHeight
                val maxWidth = maxWidth

                if (isLandscape) {
                    // --- MODO HORIZONTAL (LANDSCAPE) MEJORADO ---
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // COLUMNA IZQUIERDA: Menú y Header
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

                            AnimatedVisibility(
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

                        // COLUMNA CENTRAL: Tablero
                        Box(
                            modifier = Modifier
                                .weight(1.8f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            val boardSize = minOf(maxHeight.value, maxWidth.value * 0.6f).dp * 0.95f

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

                        // COLUMNA DERECHA: Estadísticas y PowerUps
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
                                isLowTime = state.gameMode == GameMode.DESAFIO && state.elapsedTime in 1..10,
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
                        AnimatedVisibility(visible = true, enter = slideInVertically { -it } + fadeIn()) {
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
                                .padding(horizontal = 20.dp),
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
                                    .padding(bottom = 16.dp)
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

                        AnimatedVisibility(visible = true, enter = slideInVertically { it } + fadeIn()) {
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

        AnimatedVisibility(
            visible = viewModel.showLevelSummary,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            val stats = viewModel.getBestStats(state.currentLevel)
            LevelSummaryOverlay(
                modeName = stringResource(state.gameMode.nameResId),
                base = viewModel.currentMultiplierBase,
                moves = state.moveCount,
                timeElapsed = if (state.maxTime != null) (state.maxTime!! - state.elapsedTime) else state.elapsedTime,
                bestMoves = stats.first,
                bestTime = stats.second,
                stars = state.starsEarned,
                currentTheme = currentTheme,
                onRetry = { viewModel.retryLevel() },
                onDismiss = { viewModel.nextLevel() }
            )
        }

        AnimatedVisibility(
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

        AnimatedVisibility(
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

                        // SOLO MOSTRAMOS EL SELECTOR DE COLORES (TEMAS)
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
    }
}

// -----------------------------------------------------------------------------
// COMPONENTES AUXILIARES
// -----------------------------------------------------------------------------

// ✨ NUEVO: COMPONENTE DE TEXTO FLOTANTE PARA PUNTAJES
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

// (ShapeSelector y ShapeOptionItem se eliminaron de aquí porque ya no se usan en este archivo para el menú,
// pero si los usas en GameTopBar, asegúrate de que sigan existiendo en components)

@Composable
fun BouncingText(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    color: Color,
    fontWeight: FontWeight
) {
    var previousText by remember { mutableStateOf(text) }
    val scale = remember { Animatable(1f) }

    LaunchedEffect(text) {
        if (text != previousText) {
            previousText = text
            scale.animateTo(1.2f, animationSpec = tween(100))
            scale.animateTo(1f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f))
        }
    }

    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color,
        modifier = Modifier.scale(scale.value)
    )
}

@Composable
fun PowerUpSection(viewModel: GameViewModel, haptic: HapticFeedback, activity: Activity?, modifier: Modifier = Modifier) {
    val currentTime by viewModel.currentTimeProvider.collectAsState()
    PowerUpBar(
        viewModel = viewModel,
        modifier = modifier,
        onCleanClick = {
            if (viewModel.isPowerUpAvailable(viewModel.lastCleanTime, currentTime)) {
                viewModel.useCleanPowerUp()
            } else {
                activity?.let { act ->
                    AdManager.showRewardedAd(act) {
                        viewModel.grantAdReward("CLEAN")
                    }
                }
            }
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        },
        onMergeClick = {
            if (viewModel.isPowerUpAvailable(viewModel.lastMergeTime, currentTime)) {
                viewModel.useMergePowerUp()
            } else {
                activity?.let { act ->
                    AdManager.showRewardedAd(act) {
                        viewModel.grantAdReward("MERGE")
                    }
                }
            }
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    )
}

@Composable
fun AdLoadingOverlay(currentTheme: GameTheme) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = currentTheme.accentColor)
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.preparing_revive),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun GameOverOverlay(
    onRestart: () -> Unit,
    currentTheme: GameTheme
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(16.dp),
            color = Color(0xFFF5F0E6),
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.board_full),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF5D4037)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.no_moves),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF5D4037).copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8D6E63)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.retry), color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ComboIndicator(count: Int, accentColor: Color) {
    androidx.compose.animation.AnimatedVisibility(
        visible = count > 1,
        enter = scaleIn(animationSpec = spring(Spring.DampingRatioMediumBouncy)) + fadeIn() + expandIn(),
        exit = scaleOut() + fadeOut()
    ) {
        Text(
            text = stringResource(R.string.combo_multiplier, count),
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 58.sp,
                fontWeight = FontWeight.Black,
                color = accentColor,
                shadow = Shadow(
                    Color.Black.copy(alpha = 0.5f),
                    offset = Offset(4f, 6.dp.value),
                    blurRadius = 12f
                )
            ),
            modifier = Modifier.graphicsLayer {
                rotationZ = -5f
            }
        )
    }
}

@Composable
private fun AchievementManagerPopup(viewModel: GameViewModel) {
    androidx.compose.animation.AnimatedVisibility(
        visible = viewModel.activeAchievementPopup != null,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 16.dp),
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        viewModel.activeAchievementPopup?.let { achievement ->
            AchievementPopUp(achievement = achievement)
        }
    }
}

@Composable
fun PowerUpBar(
    onCleanClick: () -> Unit,
    onMergeClick: () -> Unit,
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PowerUpButton(
            label = stringResource(R.string.clean_powerup),
            icon = Icons.Default.AutoFixHigh,
            color = Color(0xFF81B29A),
            lastUseTime = viewModel.lastCleanTime,
            viewModel = viewModel,
            onClick = onCleanClick
        )

        PowerUpButton(
            label = stringResource(R.string.merge_powerup),
            icon = Icons.Default.AutoAwesome,
            color = Color(0xFFF2CC8F),
            lastUseTime = viewModel.lastMergeTime,
            viewModel = viewModel,
            onClick = onMergeClick
        )
    }
}

@Composable
private fun PowerUpButton(
    label: String,
    icon: ImageVector,
    color: Color,
    lastUseTime: Long,
    viewModel: GameViewModel,
    onClick: () -> Unit
) {
    val currentTime by viewModel.currentTimeProvider.collectAsState()

    val isAvailable = viewModel.isPowerUpAvailable(lastUseTime, currentTime)
    val remainingText = viewModel.getRemainingTime(lastUseTime, currentTime)

    val scale by animateFloatAsState(
        targetValue = if (isAvailable) 1f else 0.95f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "PowerUpScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .animateContentSize()
    ) {
        Surface(
            onClick = onClick,
            color = if (isAvailable) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.3f),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isAvailable) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = remainingText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = stringResource(R.string.ad_label),
                            tint = color.copy(alpha = 0.9f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Text(
            text = if (isAvailable) label else stringResource(R.string.ad_label),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = if (isAvailable) Color.White.copy(alpha = 0.7f) else color.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 6.dp),
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun GameHeader(
    state: BoardState,
    currentTheme: GameTheme,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = currentTheme.accentColor.copy(alpha = 0.12f),
            border = BorderStroke(0.5.dp, currentTheme.accentColor.copy(alpha = 0.3f)),
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Text(
                text = currentTheme.name.uppercase(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                fontSize = 7.sp,
                fontWeight = FontWeight.Black,
                color = currentTheme.mainTextColor,
                letterSpacing = 1.5.sp
            )
        }

        Box(modifier = Modifier.scale(0.9f)) {
            AnimatedLevelDisplay(
                level = state.currentLevel,
                textColor = currentTheme.mainTextColor,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        Box(modifier = Modifier.scale(0.85f)) {
            ObjectiveCard(
                targetPiece = state.levelLimit,
                boardSize = state.boardSize,
                backgroundColor = currentTheme.surfaceColor,
                modifier = Modifier.padding(top = 0.dp)
            )
        }

        if (state.maxTime != null) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .scale(0.8f)
            ) {
                TimeDisplay(
                    elapsedTime = state.elapsedTime,
                    accentColor = currentTheme.accentColor,
                    textColor = currentTheme.mainTextColor
                )
            }
        }
    }
}
@Composable
private fun TimerDisplay(
    seconds: Long,
    isLowTime: Boolean,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFFE07A5F),
    textColor: Color = Color(0xFF3D405B)
) {
    // Animación de color: Rojo si es tiempo bajo, gris oscuro si es normal
    val animatedTextColor by animateColorAsState(
        targetValue = if (isLowTime) accentColor else textColor.copy(alpha = 0.7f),
        animationSpec = tween(300),
        label = "TimerColor"
    )

    val isCritical = seconds <= 5 && isLowTime

    // 1️⃣ ANIMACIÓN DE ESCALA (Pulso cardíaco)
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

    // 2️⃣ ANIMACIÓN DE TEMBLOR (Shake)
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
            .scale(scale) // Aplicamos el pulso
            .graphicsLayer {
                if (isCritical) translationX = shakeOffset // Aplicamos el temblor
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
private fun TimeDisplay(
    elapsedTime: Long,
    modifier: Modifier = Modifier,
    accentColor: Color,
    textColor: Color
) {
    val isUrgent = elapsedTime <= 10
    val isCritical = elapsedTime <= 5

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
            color = if (isUrgent) Color(0xFFE07A5F) else Color(0xFF3D405B).copy(alpha = 0.4f),
            letterSpacing = 1.2.sp
        )

        Text(
            text = elapsedTime.formatTime(),
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            color = if (isUrgent) Color(0xFFE07A5F) else Color(0xFF3D405B)
        )
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun GameBoard(
    state: BoardState,
    selectedShapeType: String,
    viewModel: GameViewModel,
    haptic: HapticFeedback,
    currentTheme: GameTheme,
    onMoveSound: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ✅ BOX CONSTRAINTS PARA ESCALADO PERFECTO
    Box(
        modifier = modifier
            .padding(8.dp)
            .background(
                color = currentTheme.surfaceColor.copy(alpha = 0.25f),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 3.dp,
                color = currentTheme.accentColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // 🛠️ BUG FIX: "key" fuerza la recomposición al cambiar la forma
        key(selectedShapeType) {
            BoardDisplay(
                state = state,
                viewModel = viewModel,
                shapeType = selectedShapeType,
                haptic = haptic,
                currentTheme = currentTheme,
                onMoveSound = onMoveSound,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 🔥 NUEVA CAPA DE PUNTOS FLOTANTES
        // Se dibuja encima del tablero pero dentro del área de juego
        viewModel.floatingScores.forEach { score ->
            key(score.id) {
                // Obtenemos el ancho de cada celda aproximado (asumiendo que BoardDisplay llena el Box)
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val tileSize = maxWidth / state.boardSize
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
private fun GameFooter(
    state: BoardState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Contador de movimientos
        StatCard(
            icon = Icons.Default.Flag,
            value = state.moveCount.toString(),
            label = stringResource(R.string.moves_label),
            color = Color(0xFF81B29A)
        )

        // Temporizador
        Box(modifier = Modifier.padding(horizontal = 8.dp)) {
            TimerDisplay(
                seconds = state.elapsedTime,
                isLowTime = state.gameMode == GameMode.DESAFIO && state.elapsedTime in 1..10,
                modifier = Modifier.scale(0.85f)
            )
        }

        // Puntuación
        if (state.score > 0) {
            StatCard(
                icon = Icons.Default.Flag,
                value = state.score.toString(),
                label = stringResource(R.string.points_label),
                color = Color(0xFFE07A5F)
            )
        }
    }
}

@Composable
private fun AnimatedLevelDisplay(
    level: Int,
    modifier: Modifier = Modifier,
    textColor: Color
) {
    // ✨ ANIMACIÓN: Usamos AnimatedContent para efecto "slot machine" al subir de nivel
    AnimatedContent(
        targetState = level,
        transitionSpec = {
            slideInVertically { height -> height } + fadeIn() togetherWith
                    slideOutVertically { height -> -height } + fadeOut()
        },
        label = "LevelSlotAnimation"
    ) { targetLevel ->
        Text(
            text = stringResource(R.string.level_label, targetLevel),
            modifier = modifier,
            fontSize = 42.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF3D405B)
        )
    }
}
@Composable
private fun ObjectiveCard(
    targetPiece: Int,
    boardSize: Int,
    modifier: Modifier = Modifier,
    backgroundColor: Color
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF3D405B).copy(alpha = 0.05f),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = stringResource(R.string.objective_title),
                modifier = Modifier.size(18.dp),
                tint = Color(0xFFE07A5F)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.goal_label, targetPiece, boardSize),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF3D405B).copy(alpha = 0.7f),
                letterSpacing = 0.5.sp
            )
        }
    }
}
@Composable
private fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(6.dp))
            // ✨ ANIMACIÓN: Usamos BouncingText para que los números reboten al cambiar
            BouncingText(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3D405B)
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF3D405B).copy(alpha = 0.5f),
            fontWeight = FontWeight.Medium
        )
    }
}

private class GameAudioManager(private val context: android.content.Context) {
    private var movePlayer: MediaPlayer? = null
    private var victoryPlayer: MediaPlayer? = null

    fun initialize() {
        try {
            movePlayer = MediaPlayer.create(context, com.korkoor.pardos.R.raw.move_pop)?.apply {
                setVolume(0.7f, 0.7f)
            }
            victoryPlayer = MediaPlayer.create(context, com.korkoor.pardos.R.raw.victory_sound)?.apply {
                setVolume(0.8f, 0.8f)
            }
        } catch (e: Exception) {
            android.util.Log.e("GameAudio", "Error initializing audio", e)
        }
    }

    fun playMoveSound() {
        movePlayer?.apply {
            try {
                if (isPlaying) {
                    pause()
                    seekTo(0)
                }
                start()
            } catch (e: Exception) {
                android.util.Log.e("GameAudio", "Error playing move sound", e)
            }
        }
    }

    fun playVictorySound() {
        victoryPlayer?.apply {
            try {
                if (isPlaying) {
                    pause()
                    seekTo(0)
                }
                start()
            } catch (e: Exception) {
                android.util.Log.e("GameAudio", "Error playing victory sound", e)
            }
        }
    }

    fun release() {
        try {
            movePlayer?.release()
            victoryPlayer?.release()
        } catch (e: Exception) {
            android.util.Log.e("GameAudio", "Error releasing audio", e)
        } finally {
            movePlayer = null
            victoryPlayer = null
        }
    }
}

// ============================================================================
// EXTENSIONES DE UTILIDAD
// ============================================================================

private fun Long.formatTime(): String {
    val totalSeconds = if (this < 0) 0 else this
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

private val GameMode.color: Color
    get() = when (this) {
        GameMode.CLASICO -> Color(0xFF81B29A)
        GameMode.DESAFIO -> Color(0xFFE07A5F)
        GameMode.ZEN -> Color(0xFF6C63FF)
        GameMode.TABLAS -> Color(0xFF6C63FF)
        else -> Color(0xFF3D405B)
    }

private fun HapticFeedback.performHapticFeedback() {
    try {
        performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
    } catch (e: Exception) {
    }
}