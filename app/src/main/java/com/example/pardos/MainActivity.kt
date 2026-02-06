package com.korkoor.pardos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.korkoor.pardos.domain.model.GameMode
import com.korkoor.pardos.domain.logic.ProgressionEngine
import com.korkoor.pardos.ui.game.AchievementsScreen
import com.korkoor.pardos.ui.game.GameScreen
import com.korkoor.pardos.ui.game.GameViewModel
import com.korkoor.pardos.ui.menu.CustomLevelScreen
import com.korkoor.pardos.ui.menu.MenuScreen
import com.korkoor.pardos.ui.menu.ModeSelectionScreen
import com.korkoor.pardos.ui.menu.LevelSelectorScreen
import com.korkoor.pardos.ui.records.RecordsScreen
import com.korkoor.pardos.ui.theme.PardosTheme
import com.korkoor.pardos.ui.theme.ThemeViewModel

sealed class Screen {
    data object Menu : Screen()
    data object ModeSelection : Screen()
    data object Game : Screen()
    data object CustomLevel : Screen()
    data object Records : Screen()
    data object Achievements : Screen()
    data object LevelSelector : Screen()
}

class MainActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Asegúrate de que esta línea no cause crash si no tienes AdManager, si lo tienes déjala.
        com.korkoor.pardos.ui.game.logic.AdManager.initialize(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            PardosTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Menu) }
                val currentTheme = themeViewModel.currentTheme

                LaunchedEffect(gameViewModel.dailyChallengeThemeIndex) {
                    gameViewModel.dailyChallengeThemeIndex?.let { index ->
                        themeViewModel.selectThemeByIndex(index)
                    }
                }

                // Observamos los niveles. Cuando loadLevelsWithProgress se ejecute, esto se actualizará solo.
                val allLevels by gameViewModel.levels.collectAsState()
                val savedRecords by gameViewModel.allRecords.collectAsState(initial = emptyList())
                val unlockedIds by gameViewModel.unlockedAchievements.collectAsState()

                Surface(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "MainNavigation"
                    ) { target ->
                        when (target) {
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
                                    themeViewModel = themeViewModel
                                )
                            }

                            Screen.ModeSelection -> ModeSelectionScreen(
                                onModeSelected = { mode ->
                                    if (mode == GameMode.CLASICO) {
                                        currentScreen = Screen.LevelSelector
                                    } else {
                                        gameViewModel.startNewGame(mode)
                                        currentScreen = Screen.Game
                                    }
                                },
                                onBack = { currentScreen = Screen.Menu },
                                currentTheme = currentTheme
                            )

                            // 🔥 AQUI ESTÁ LA SOLUCIÓN DEFINITIVA 🔥
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

                                // 👇 ESTO CONECTA LA PANTALLA CON LA BASE DE DATOS 👇
                                onRefresh = { gameViewModel.loadLevelsWithProgress() }
                            )

                            Screen.Game -> GameScreen(
                                viewModel = gameViewModel,
                                themeViewModel = themeViewModel,
                                onBackToMenu = {
                                    gameViewModel.resetGameSession()
                                    // Si estábamos en Clásico, volvemos al mapa, si no al menú
                                    if (gameViewModel.currentMode == GameMode.CLASICO) {
                                        currentScreen = Screen.LevelSelector
                                    } else {
                                        currentScreen = Screen.Menu
                                    }
                                }
                            )

                            Screen.CustomLevel -> CustomLevelScreen(
                                onStartCustom = { size, targetVal, allowPowerUps, difficulty ->
                                    gameViewModel.setupCustomGame(
                                        size = size,
                                        target = targetVal,
                                        allowPowerUps = allowPowerUps,
                                        difficulty = difficulty,
                                        isCustom = true
                                    )
                                    currentScreen = Screen.Game
                                },
                                onBack = { currentScreen = Screen.Menu },
                                currentTheme = currentTheme
                            )

                            Screen.Records -> RecordsScreen(
                                records = savedRecords,
                                onBack = { currentScreen = Screen.Menu },
                                currentTheme = currentTheme
                            )

                            Screen.Achievements -> AchievementsScreen(
                                unlockedIds = unlockedIds,
                                currentTheme = currentTheme,
                                onBack = { currentScreen = Screen.Menu }
                            )
                        }
                    }
                }

                // Manejo del botón físico "Atrás" de Android
                BackHandler(enabled = currentScreen != Screen.Menu) {
                    when (currentScreen) {
                        Screen.Game -> {
                            gameViewModel.resetGameSession()
                            if (gameViewModel.currentMode == GameMode.CLASICO) {
                                currentScreen = Screen.LevelSelector
                            } else {
                                currentScreen = Screen.ModeSelection
                            }
                        }
                        Screen.LevelSelector -> currentScreen = Screen.ModeSelection
                        Screen.ModeSelection -> currentScreen = Screen.Menu
                        Screen.CustomLevel -> currentScreen = Screen.Menu
                        Screen.Records -> currentScreen = Screen.Menu
                        Screen.Achievements -> currentScreen = Screen.Menu
                        else -> currentScreen = Screen.Menu
                    }
                }
            }
        }
    }
}