@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.korkoor.pardos.ui.game

import com.korkoor.pardos.ui.design.ToyButton
import com.korkoor.pardos.ui.design.ToyTextButton
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
import com.korkoor.pardos.ui.theme.particleKind
import com.korkoor.pardos.ui.theme.inkColor
import com.korkoor.pardos.ui.theme.particleTintColor
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
    modifier: Modifier = Modifier,
    /** Precio con formato local del pack pequeño de gemas y cómo comprarlo sin salir de la partida (al faltar gemas para continuar). */
    gemPackPrice: String? = null,
    onBuyGemPack: () -> Unit = {}
) {
    val state by viewModel.boardState.collectAsStateWithLifecycle()
    val currentTheme = themeViewModel.currentTheme
    com.korkoor.pardos.ui.design.DarkSystemBars(remember(currentTheme) { com.korkoor.pardos.ui.design.isDarkBackground(currentTheme.colors) })
    val haptic = com.korkoor.pardos.ui.design.rememberGameHaptics()
    val context = LocalContext.current
    val activity = context as? Activity
    val adEconomy = remember { com.korkoor.pardos.data.local.EconomyManager(context) }
    val gemsNow by adEconomy.gems.collectAsState()
    val isVipNow by adEconomy.isVip.collectAsState()

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

    // Tarjeta de "nueva regla": la primera vez que aparece cada tipo de nivel (y en cada jefe), antes del primer movimiento
    val ruleSeen = remember { context.getSharedPreferences("pardos_levels", android.content.Context.MODE_PRIVATE) }
    val ruleKey = state.levelKind?.let {
        when {
            it == com.korkoor.pardos.domain.level.LevelKind.BOSS -> "boss_${state.currentLevel}"
            it == com.korkoor.pardos.domain.level.LevelKind.TWIST -> "twist_${state.twist.name}"
            else -> "kind_${it.name}"
        }
    }
    var ruleDismissed by remember(ruleKey) { mutableStateOf(false) }
    val showRuleCard = ruleKey != null && !ruleDismissed && state.moveCount == 0 && !state.isLevelCompleted && !state.isGameOver &&
        state.levelKind != com.korkoor.pardos.domain.level.LevelKind.ZEN && !ruleSeen.getBoolean(ruleKey, false)

    // Tutorial del primer nivel (solo la primera vez) y pista con la mano cuando te quedas parado en los primeros niveles
    val tutorialPrefs = remember { context.getSharedPreferences("pardos_levels", android.content.Context.MODE_PRIVATE) }
    var tutorialDone by remember {
        mutableStateOf(
            tutorialPrefs.getBoolean("tutorial_v2_done", false) ||
                context.getSharedPreferences("pardos_storage", android.content.Context.MODE_PRIVATE).getInt("stars_level_1", 0) > 0
        )
    }
    val inCampaign = state.gameMode == GameMode.CLASICO && viewModel.dailyChallengeThemeIndex == null
    val tutorialOn = !tutorialDone && inCampaign && state.currentLevel == 1 && !state.isGameOver && !state.isLevelCompleted && !viewModel.showLevelSummary
    // Estrellas en vivo: solo campaña sin reloj (los límites salen de la misma regla que las estrellas finales)
    val starLimits = remember(state.currentLevel, inCampaign, viewModel.towerActive) {
        if (!inCampaign || viewModel.towerActive) null else {
            val sp = com.korkoor.pardos.domain.level.LevelCatalog.spec(state.currentLevel)
            if ((sp.timeLimitMs ?: 0L) > 0L) null
            else com.korkoor.pardos.domain.level.LevelMath.threeStarMoves(sp) to com.korkoor.pardos.domain.level.LevelMath.twoStarMoves(sp)
        }
    }
    val goalLine = com.korkoor.pardos.domain.level.goalText(state.goal, state.levelLimit, state.goalCount)
    val coachStep = if (tutorialOn) remember(state.tiles, state.moveCount, state.merges) {
        com.korkoor.pardos.domain.logic.TutorialCoach.step(
            state.boardSize, state.blocked.toSet(), state.tiles, state.moveCount, state.merges, goalLine
        )
    } else null
    LaunchedEffect(state.moveCount, state.merges, tutorialOn) {
        if (tutorialOn && com.korkoor.pardos.domain.logic.TutorialCoach.isFinished(state.moveCount, state.merges)) {
            tutorialPrefs.edit().putBoolean("tutorial_v2_done", true).apply()
            tutorialDone = true
        }
    }
    // Pista: si pasan 7 s sin mover en los primeros niveles, la mano enseña una jugada útil
    var idleHint by remember { mutableStateOf(false) }
    val hintEligible = inCampaign && state.currentLevel <= 15 && !tutorialOn && !state.isGameOver && !state.isLevelCompleted &&
        !viewModel.showLevelSummary && !viewModel.isSelectModeActive && !showRuleCard
    LaunchedEffect(state.moveCount, state.currentLevel, hintEligible) {
        idleHint = false
        if (hintEligible) {
            delay(7000)
            idleHint = true
        }
    }
    val guide: BoardGuide? = when {
        tutorialOn && !showRuleCard -> coachStep?.direction?.let { BoardGuide(it, coachStep.cells) }
        idleHint && state.tiles.isNotEmpty() -> com.korkoor.pardos.domain.logic.MoveAdvisor
            .suggest(state.boardSize, state.blocked.toSet(), state.tiles)?.let { BoardGuide(it.direction, it.mergeCells) }
        else -> null
    }


    // 🔥 OPTIMIZACIÓN: Solo recalcula el gradiente si el tema cambia
    val bgGradient = remember(currentTheme) { Brush.verticalGradient(colors = currentTheme.colors) }

    val isTimeLow = state.maxTime != null && state.elapsedTime <= 10_000L
    val extraTimes by viewModel.extraTimeCount.collectAsState()
    val autoNextOn by remember { com.korkoor.pardos.data.local.SettingsManager(context).autoNextEnabled }.collectAsState()
    val remoteTitle = when (viewModel.remoteRole) {
        RemoteRole.CREATOR -> "TU RETO"
        RemoteRole.CHALLENGED -> "EL RETO"
        RemoteRole.NONE -> if (viewModel.towerActive) com.korkoor.pardos.domain.tower.TowerRules.floorLabel(viewModel.towerFloor)
            else if (viewModel.dailyChallengeThemeIndex != null) "RETO DIARIO" else null
    }

    val shouldBlur = viewModel.showLevelSummary || state.isGameOver || showExitDialog || showThemeMenu
    val blurRadius by animateDpAsState(
        targetValue = if (shouldBlur) 16.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "DynamicBlur"
    )

    LaunchedEffect(state.levelLimit) { themeViewModel.updateLevel(state.levelLimit) }

    BackHandler(enabled = !state.isLevelCompleted) {
        if (state.moveCount > 0) showExitDialog = true else onBackToMenu()
    }

    Column(modifier = modifier.fillMaxSize().background(bgGradient)) {

        Box(modifier = Modifier.weight(1f)) {

            PicnicBackgroundOptimized(
                color = if (isTimeLow) Color(0xFFE07A5F).copy(alpha = 0.15f)
                else currentTheme.accentColor.copy(alpha = 0.05f)
            )

            com.korkoor.pardos.ui.game.components.AmbientParticles(
                kind = currentTheme.particleKind,
                tint = currentTheme.particleTintColor
            )

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
                                            currentTheme = currentTheme,
                                            extraTimes = extraTimes,
                                            onExtraTime = { viewModel.useExtraTime() },
                                            titleOverride = remoteTitle,
                                            winStreak = if (inCampaign) viewModel.winStreak else 0,
                                            towerHearts = if (viewModel.towerActive) viewModel.towerHearts else null,
                                            starLimits = if (tutorialOn) null else starLimits
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
                                        .impactShake(viewModel.impact)
                                        .shadow(30.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    GameBoard(
                                        state = state,
                                        selectedShapeType = selectedShapeType,
                                        viewModel = viewModel,
                                        haptic = haptic,
                                        currentTheme = currentTheme,
                                        onMoveSound = { },
                                        modifier = Modifier.fillMaxSize(),
                                        guide = guide
                                    )

                                    ComboIndicator(
                                        count = viewModel.comboCount.value,
                                        accentColor = currentTheme.accentColor
                                    )

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

                                if (coachStep != null) {
                                    CoachCard(coachStep, Modifier.padding(horizontal = 0.dp))
                                } else if (state.allowPowerUps && !viewModel.showLevelSummary && !state.isGameOver && !state.isLevelCompleted) {
                                    PowerUpSection(viewModel, haptic, activity, labelColor = currentTheme.inkColor)
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
                                            currentTheme = currentTheme,
                                            extraTimes = extraTimes,
                                            onExtraTime = { viewModel.useExtraTime() },
                                            titleOverride = remoteTitle,
                                            winStreak = if (inCampaign) viewModel.winStreak else 0,
                                            towerHearts = if (viewModel.towerActive) viewModel.towerHearts else null,
                                            starLimits = if (tutorialOn) null else starLimits
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
                                        .impactShake(viewModel.impact)
                                        .shadow(30.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    GameBoard(
                                        state = state,
                                        selectedShapeType = selectedShapeType,
                                        viewModel = viewModel,
                                        haptic = haptic,
                                        currentTheme = currentTheme,
                                        onMoveSound = { },
                                        modifier = Modifier.fillMaxSize(),
                                        guide = guide
                                    )

                                    ComboIndicator(
                                        count = viewModel.comboCount.value,
                                        accentColor = currentTheme.accentColor
                                    )

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

                                    if (coachStep != null) {
                                        Spacer(Modifier.height(14.dp))
                                        CoachCard(coachStep)
                                    } else if (state.allowPowerUps && !viewModel.showLevelSummary && !state.isGameOver && !state.isLevelCompleted) {
                                        PowerUpSection(viewModel, haptic, activity, Modifier.fillMaxWidth(), labelColor = currentTheme.inkColor)
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
                if (viewModel.towerActive) {
                    TowerSummaryOverlay(
                        floor = viewModel.towerFloor,
                        reward = viewModel.lastTowerReward,
                        hearts = viewModel.towerHearts,
                        best = viewModel.towerBest,
                        runCoins = viewModel.towerRunCoins,
                        autoNextMs = if (autoNextOn) 4000 else null,
                        onNext = { viewModel.nextLevel() },
                        onExit = { onBackToMenu() }
                    )
                } else {
                val stats = viewModel.getBestStats(state.currentLevel)
                // Jefe vencido o hito de racha: momento de celebración, sin anuncio encima
                val summaryBigMoment = viewModel.lastStreakMilestone != null ||
                    com.korkoor.pardos.domain.level.LevelCatalog.spec(state.currentLevel).isBoss
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
                    coinsEarned = viewModel.lastCoinsEarned,
                    canDouble = !viewModel.coinsDoubled && viewModel.lastCoinsEarned > 0,
                    bonus = viewModel.lastGameBonus,
                    onDouble = {
                        // VIP: gratis, sin anuncio
                        if (isVipNow) viewModel.grantDoubleCoins()
                        else activity?.let { act -> AdManager.showRewardedAd(act) { viewModel.grantDoubleCoins() } }
                    },
                    vip = isVipNow,
                    newRecord = viewModel.lastRunWasRecord,
                    onRetry = { viewModel.retryLevel() },
                    onAutoNext = { viewModel.nextLevel() },
                    onDismiss = {
                        // Pausa natural tras ganar: si toca, un anuncio de pantalla completa (nunca con VIP, nunca tras perder)
                        val adDue = inCampaign && activity != null && AdManager.isInterstitialDue(context, state.currentLevel, isVipNow, summaryBigMoment)
                        if (inCampaign && activity != null) AdManager.showInterstitialIfDue(activity, state.currentLevel, isVipNow, bigMoment = summaryBigMoment) { viewModel.nextLevel() }
                        else viewModel.nextLevel()
                        // Momento feliz (3 estrellas en racha o jefe vencido): una reseña, pero nunca pegada a un anuncio
                        if (inCampaign && activity != null) {
                            com.korkoor.pardos.data.local.RatePrompt.maybeAsk(
                                activity, state.currentLevel, viewModel.winStreak, state.starsEarned >= 3,
                                com.korkoor.pardos.domain.level.LevelCatalog.spec(state.currentLevel).isBoss, adDue, isVipNow
                            )
                        }
                    },
                    nextGoal = remember { com.korkoor.pardos.data.local.RetentionManager(context).nextGoal() },
                    starHint = if (state.gameMode == GameMode.CLASICO && state.starsEarned in 1..2)
                        com.korkoor.pardos.domain.level.LevelRules.threeStarHint(com.korkoor.pardos.domain.level.LevelCatalog.spec(state.currentLevel))
                    else null,
                    streak = if (inCampaign) viewModel.winStreak else 0,
                    streakBonusPct = viewModel.lastStreakBonusPct,
                    flowBonusPct = viewModel.lastFlowBonusPct,
                    milestone = viewModel.lastStreakMilestone,
                    teaser = if (inCampaign) remember(state.currentLevel) { com.korkoor.pardos.domain.flow.NextLevelTeaser.after(state.currentLevel) } else null,
                    nextKindSeen = if (inCampaign) ruleSeen.getBoolean("kind_${com.korkoor.pardos.domain.level.LevelCatalog.spec(state.currentLevel + 1).kind.name}", false) else true,
                    autoNextMs = if (inCampaign && autoNextOn && viewModel.lastStreakMilestone == null &&
                        com.korkoor.pardos.domain.flow.NextLevelTeaser.after(state.currentLevel).levelsToChest > 0 &&
                        // si toca un anuncio, el jugador pasa tocando SIGUIENTE: nada de anuncios que aparezcan solos
                        !remember(state.currentLevel) { AdManager.isInterstitialDue(context, state.currentLevel, isVipNow, summaryBigMoment) }) 4500 else null,
                    onShare = {
                        val text = com.korkoor.pardos.domain.social.ShareText.victory(
                            modeName = context.getString(state.gameMode.nameResId),
                            stars = state.starsEarned,
                            targetTile = state.levelLimit,
                            moves = state.moveCount,
                            timeMs = state.elapsedTime,
                            streak = com.korkoor.pardos.data.local.ProfileManager(context).getProfile().currentStreak,
                            epochDay = if (viewModel.dailyChallengeThemeIndex != null) com.korkoor.pardos.data.local.LocalDay.today() else null
                        )
                        val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(android.content.Intent.createChooser(send, null))
                    }
                )
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = state.isGameOver,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                if (state.gameMode == GameMode.DUELO && viewModel.remoteRole != RemoteRole.NONE) {
                    com.korkoor.pardos.ui.social.RemoteDuelOverlay(
                        role = viewModel.remoteRole,
                        myScore = viewModel.duelScores[0],
                        challenge = viewModel.remoteChallenge,
                        outcome = viewModel.remoteOutcome,
                        reward = viewModel.remoteReward,
                        firstTime = viewModel.remoteFirstTime,
                        onShare = { viewModel.remoteChallenge?.let { com.korkoor.pardos.ui.social.shareChallenge(context, it) } },
                        onRetryBoard = { viewModel.retryLevel() },
                        onChallengeBack = { viewModel.startRemoteCreate() },
                        onExit = { onBackToMenu() }
                    )
                } else if (state.gameMode == GameMode.DUELO) {
                    DuelOverlay(
                        phase = viewModel.duelPhase,
                        scores = viewModel.duelScores,
                        onNext = { viewModel.duelStartSecondPlayer() },
                        onRematch = { viewModel.duelRematch() },
                        onExit = { onBackToMenu() }
                    )
                } else if (viewModel.loadingAdType == "REVIVE") {
                    AdLoadingOverlay(currentTheme)
                }
                else if (viewModel.towerActive && viewModel.towerHearts <= 0) {
                    TowerEndOverlay(
                        floor = viewModel.towerFloor,
                        best = viewModel.towerBest,
                        runCoins = viewModel.towerRunCoins,
                        runGems = viewModel.towerRunGems,
                        newRecord = viewModel.towerNewRecord,
                        onRetry = { viewModel.retryLevel() },
                        onExit = { onBackToMenu() }
                    )
                }
                else if (state.secondChanceUsed == false && !viewModel.towerActive) {
                    SecondChanceOverlay(
                        onUseSecondChance = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (isVipNow) viewModel.grantAdReward("REVIVE")
                            else activity?.let { act ->
                                AdManager.showRewardedAd(act) {
                                    viewModel.grantAdReward("REVIVE")
                                }
                            }
                        },
                        onCancel = { viewModel.declineSecondChance() },
                        currentTheme = currentTheme,
                        reason = state.gameOverReason(),
                        gems = gemsNow,
                        vip = isVipNow,
                        onPayGems = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.reviveWithGems()
                        },
                        gemPackPrice = gemPackPrice,
                        gemPackGems = com.korkoor.pardos.domain.shop.ShopCatalog.byId(com.korkoor.pardos.domain.shop.ShopCatalog.GEMS_TINY)?.let {
                            com.korkoor.pardos.domain.shop.GemPacks.gemsForPurchase(it, adEconomy.isFirstPurchase(it.id))
                        } ?: 0,
                        onBuyGems = onBuyGemPack
                    )
                }
                else {
                    GameOverOverlay(
                        onRestart = { viewModel.retryLevel() },
                        currentTheme = currentTheme,
                        isRace = state.gameMode == GameMode.CARRERA,
                        stagesCleared = viewModel.raceStagesCleared,
                        coinsEarned = viewModel.lastCoinsEarned,
                        nearMiss = viewModel.nearMissMessage,
                        footnote = if (viewModel.towerActive) "Pierdes un corazón · te quedan ${viewModel.towerHearts}" else null,
                        reason = state.gameOverReason()
                    )
                }
            }

            AchievementManagerPopup(viewModel = viewModel)

            // Ayuda adaptativa a la vista: "Te echamos una mano…" al empezar un nivel que se atascó
            AssistBanner(viewModel.assistMessage, Modifier.align(Alignment.TopCenter).padding(top = 92.dp))
            PhaseBannerUi(viewModel.phaseBanner, Modifier.align(Alignment.Center))
            BlockedHintUi(viewModel.blockedHint, Modifier.align(Alignment.TopCenter).padding(top = 150.dp))

            if (showRuleCard) {
                NewRuleDialog(state) {
                    ruleKey?.let { ruleSeen.edit().putBoolean(it, true).apply() }
                    ruleDismissed = true
                }
            }

            // Modo Carrera: aviso de tiempo ganado al superar una etapa
            androidx.compose.animation.AnimatedVisibility(
                visible = viewModel.raceBonusFlash != null,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 150.dp),
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = fadeOut() + slideOutVertically { -it / 2 }
            ) {
                val sec = viewModel.raceBonusFlash ?: 0
                Row(
                    modifier = Modifier
                        .shadow(10.dp, RoundedCornerShape(20.dp), spotColor = com.korkoor.pardos.ui.design.Gold)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.horizontalGradient(listOf(com.korkoor.pardos.ui.design.Sand, com.korkoor.pardos.ui.design.Gold)))
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.race_time_bonus, sec),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

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
                                color = com.korkoor.pardos.ui.design.Navy,
                                letterSpacing = 2.sp
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "COLORES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = com.korkoor.pardos.ui.design.Navy.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ThemeSelector(viewModel = themeViewModel)

                            Spacer(modifier = Modifier.height(32.dp))

                            ToyButton(
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
        color = com.korkoor.pardos.ui.design.Navy.copy(alpha = currentAlpha),
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
        else -> com.korkoor.pardos.ui.design.Navy
    }

internal fun HapticFeedback.performHapticFeedback() {
    try {
        performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
    } catch (e: Exception) {
    }
}
