package com.korkoor.pardos

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.korkoor.pardos.domain.model.GameMode
import com.korkoor.pardos.domain.logic.ProgressionEngine
import com.korkoor.pardos.notifications.ZenNotificationManager
import com.korkoor.pardos.ui.game.AchievementsScreen
import com.korkoor.pardos.ui.game.GameScreen
import com.korkoor.pardos.ui.game.GameViewModel
import com.korkoor.pardos.ui.menu.AnimatedSplashScreen
import com.korkoor.pardos.ui.menu.CustomLevelScreen
import com.korkoor.pardos.ui.menu.MenuScreen
import com.korkoor.pardos.ui.menu.ModeSelectionScreen
import com.korkoor.pardos.ui.menu.LevelSelectorScreen
import com.korkoor.pardos.ui.records.RecordsScreen
import com.korkoor.pardos.ui.theme.PardosTheme
import com.korkoor.pardos.ui.theme.ThemeViewModel

sealed class Screen {
    data object Splash : Screen()
    data object Menu : Screen()
    data object ModeSelection : Screen()
    data object Game : Screen()
    data object CustomLevel : Screen()
    data object Records : Screen()
    data object Achievements : Screen()
    data object LevelSelector : Screen()
    data object Profile : Screen()
    data object Friends : Screen()
}

class MainActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()
    private lateinit var notificationManager: ZenNotificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        notificationManager = ZenNotificationManager(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        com.korkoor.pardos.ui.game.logic.AdManager.initialize(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val profileManager = com.korkoor.pardos.data.local.ProfileManager(this)

        // 🔥 RESCATE DE DATOS BLINDADO Y MIGRACIÓN LEGACY 🔥
        // 1. Primero preguntamos a Firebase
        profileManager.syncFromFirebase { cloudProfile ->

            // 2. Si hay datos en la nube, los evaluamos
            if (cloudProfile != null) {
                val localProfile = profileManager.getProfile()

                // Si la nube tiene más nivel que el local, GANÓ LA NUBE.
                if (cloudProfile.playerLevel >= localProfile.playerLevel) {
                    Log.d("MainActivity", "🏆 ¡Restaurando perfil desde la nube! Nivel: ${cloudProfile.playerLevel}")
                    profileManager.saveProfile(cloudProfile)

                    // Refrescamos niveles en el ViewModel
                    gameViewModel.loadLevelsWithProgress()
                }
            } else {
                Log.d("MainActivity", "📱 No hay datos en la nube. Revisando si es un jugador veterano...")

                // 🔥 MIGRACIÓN LEGACY: Buscamos si el jugador tenía progreso en la versión anterior
                // PON AQUÍ EL NOMBRE DE TUS SHAREDPREFERENCES VIEJAS Y LA LLAVE DE TU NIVEL
                val oldPrefs = getSharedPreferences("pardos_prefs", MODE_PRIVATE)
                val legacyLevel = oldPrefs.getInt("last_unlocked_level", 1)

                // Intentamos migrar su progreso viejo al nuevo sistema de Perfiles
                profileManager.migrateLegacyProgressIfNeeded(legacyLevel)
            }

            // 3. AHORA SÍ, revisamos la racha (solo después de haber decidido qué perfil gana)
            profileManager.checkAndUpdateStreak()
        }

        setContent {
            PardosTheme {
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_PAUSE -> gameViewModel.pauseGame()
                            Lifecycle.Event.ON_RESUME -> gameViewModel.resumeGame()
                            else -> {}
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
                val currentTheme = themeViewModel.currentTheme

                LaunchedEffect(gameViewModel.dailyChallengeThemeIndex) {
                    gameViewModel.dailyChallengeThemeIndex?.let { index ->
                        themeViewModel.selectThemeByIndex(index)
                    }
                }

                val allLevels by gameViewModel.levels.collectAsState()
                val savedRecords by gameViewModel.allRecords.collectAsState(initial = emptyList())
                val unlockedIds by gameViewModel.unlockedAchievements.collectAsState()

                Surface(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(600))
                        },
                        label = "MainNavigation"
                    ) { target ->
                        when (target) {
                            Screen.Splash -> AnimatedSplashScreen(onAnimationFinished = { currentScreen = Screen.Menu })

                            Screen.Menu -> {
                                SideEffect { gameViewModel.resetGameSession() }
                                MenuScreen(
                                    onPlayClick = { currentScreen = Screen.ModeSelection },
                                    onCustomClick = { currentScreen = Screen.CustomLevel },
                                    onRecordsClick = { currentScreen = Screen.Records },
                                    onAchievementsClick = { currentScreen = Screen.Achievements },
                                    onDailyChallengeClick = {
                                        gameViewModel.setupDailyChallenge()
                                        currentScreen = Screen.Game
                                    },
                                    onProfileClick = { currentScreen = Screen.Profile },
                                    onFriendsClick = { currentScreen = Screen.Friends },
                                    themeViewModel = themeViewModel
                                )
                            }

                            Screen.ModeSelection -> ModeSelectionScreen(
                                onModeSelected = { mode ->
                                    if (mode == GameMode.CLASICO) currentScreen = Screen.LevelSelector
                                    else { gameViewModel.startNewGame(mode); currentScreen = Screen.Game }
                                },
                                onBack = { currentScreen = Screen.Menu },
                                currentTheme = currentTheme
                            )

                            Screen.LevelSelector -> LevelSelectorScreen(
                                levels = allLevels,
                                currentTheme = currentTheme,
                                onLevelSelected = { selectedLevel ->
                                    gameViewModel.setupCustomGame(
                                        size = ProgressionEngine.calculateBoardSize(selectedLevel.target),
                                        target = selectedLevel.target,
                                        difficulty = selectedLevel.difficultyName,
                                        level = selectedLevel.id
                                    )
                                    currentScreen = Screen.Game
                                },
                                onBack = { currentScreen = Screen.ModeSelection },
                                onRefresh = { gameViewModel.loadLevelsWithProgress() }
                            )

                            Screen.Game -> GameScreen(
                                viewModel = gameViewModel,
                                themeViewModel = themeViewModel,
                                onBackToMenu = {
                                    gameViewModel.resetGameSession()
                                    currentScreen = if (gameViewModel.currentMode == GameMode.CLASICO) Screen.LevelSelector else Screen.Menu
                                }
                            )

                            Screen.CustomLevel -> CustomLevelScreen(
                                onStartCustom = { size, targetVal, allowPowerUps, difficulty ->
                                    gameViewModel.setupCustomGame(size, targetVal, allowPowerUps, difficulty, isCustom = true)
                                    currentScreen = Screen.Game
                                },
                                onBack = { currentScreen = Screen.Menu },
                                currentTheme = currentTheme
                            )

                            Screen.Records -> RecordsScreen(records = savedRecords, onBack = { currentScreen = Screen.Menu }, currentTheme = currentTheme)

                            Screen.Achievements -> AchievementsScreen(unlockedIds = unlockedIds, currentTheme = currentTheme, onBack = { currentScreen = Screen.Menu })

                            Screen.Profile -> com.korkoor.pardos.ui.profile.ProfileScreen(onBack = { currentScreen = Screen.Menu })

                            Screen.Friends -> com.korkoor.pardos.ui.profile.FriendsScreen(onBack = { currentScreen = Screen.Menu })
                        }
                    }
                }

                BackHandler(enabled = currentScreen != Screen.Menu && currentScreen != Screen.Splash) {
                    when (currentScreen) {
                        Screen.Game -> {
                            gameViewModel.resetGameSession()
                            currentScreen = if (gameViewModel.currentMode == GameMode.CLASICO) Screen.LevelSelector else Screen.ModeSelection
                        }
                        Screen.LevelSelector -> currentScreen = Screen.ModeSelection
                        else -> currentScreen = Screen.Menu
                    }
                }
            }
        }
    }

    // 🔥 EL JUGADOR REGRESÓ: Cancelamos el spam
    override fun onResume() {
        super.onResume()
        if (::notificationManager.isInitialized) {
            notificationManager.cancelAllNotifications()
        }
    }

    // 🔥 EL JUGADOR SE FUE: Plantamos las minas de retención
    override fun onPause() {
        super.onPause()
        if (::notificationManager.isInitialized) {
            notificationManager.scheduleAllNotifications()
        }
    }
}