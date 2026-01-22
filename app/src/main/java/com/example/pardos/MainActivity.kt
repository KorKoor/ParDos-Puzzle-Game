package com.example.pardos

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
import com.example.pardos.domain.model.GameMode
import com.example.pardos.domain.logic.ProgressionEngine
import com.example.pardos.ui.game.AchievementsScreen
import com.example.pardos.ui.game.GameScreen
import com.example.pardos.ui.game.GameViewModel
import com.example.pardos.ui.menu.CustomLevelScreen
import com.example.pardos.ui.menu.MenuScreen
import com.example.pardos.ui.menu.ModeSelectionScreen
import com.example.pardos.ui.menu.LevelSelectorScreen
import com.example.pardos.ui.records.RecordsScreen
import com.example.pardos.ui.theme.PardosTheme
import com.example.pardos.ui.theme.ThemeViewModel

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
        com.example.pardos.ui.game.logic.AdManager.initialize(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            PardosTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Menu) }
                val currentTheme = themeViewModel.currentTheme

                // Observamos datos del ViewModel
                val allLevels by gameViewModel.levels.collectAsState()
                val savedRecords by gameViewModel.allRecords.collectAsState(initial = emptyList())
                val unlockedIds by gameViewModel.unlockedAchievements.collectAsState()

                Surface(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "MainNavigation"
                    ) { target ->
                        when (target) {
                            Screen.Menu -> MenuScreen(
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

                            Screen.LevelSelector -> LevelSelectorScreen(
                                levels = allLevels,
                                currentTheme = currentTheme,
                                onLevelSelected = { selectedLevel ->
                                    gameViewModel.setupCustomGame(
                                        size = ProgressionEngine.calculateBoardSize(selectedLevel.target),
                                        target = selectedLevel.target,
                                        difficulty = selectedLevel.difficultyName,
                                        level = selectedLevel.id
                                        // Aquí no ponemos isCustom=true porque queremos que cuente como campaña (probablemente)
                                    )
                                    currentScreen = Screen.Game
                                },
                                onBack = { currentScreen = Screen.ModeSelection }
                            )

                            Screen.Game -> GameScreen(
                                viewModel = gameViewModel,
                                themeViewModel = themeViewModel,
                                onBackToMenu = { currentScreen = Screen.Menu }
                            )

                            Screen.CustomLevel -> CustomLevelScreen(
                                onStartCustom = { size, targetVal, allowPowerUps, difficulty ->
                                    // 🔥🔥 AQUÍ ESTABA EL ERROR 🔥🔥
                                    // Tienes que pasar 'isCustom = true' para que el ViewModel sepa
                                    // que debe cambiar el modo a GameMode.CUSTOM.
                                    gameViewModel.setupCustomGame(
                                        size = size,
                                        target = targetVal,
                                        allowPowerUps = allowPowerUps,
                                        difficulty = difficulty,
                                        isCustom = true // 👈 ¡ESTA LÍNEA ARREGLA EL BUG!
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

                BackHandler(enabled = currentScreen != Screen.Menu) {
                    currentScreen = when (currentScreen) {
                        Screen.LevelSelector -> Screen.ModeSelection
                        Screen.Game -> Screen.ModeSelection
                        else -> Screen.Menu
                    }
                }
            }
        }
    }
}