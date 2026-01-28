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

                            Screen.LevelSelector -> LevelSelectorScreen(
                                levels = allLevels,
                                currentTheme = currentTheme,
                                onLevelSelected = { selectedLevel ->
                                    // 🔥🔥 CORRECCIÓN: Al seleccionar un nivel, le decimos al ViewModel
                                    // que configure ese nivel Específico, sin forzar el máximo.
                                    // Y no ponemos 'isCustom=true' porque es parte de la campaña.
                                    gameViewModel.setupCustomGame(
                                        size = ProgressionEngine.calculateBoardSize(selectedLevel.target),
                                        target = selectedLevel.target,
                                        difficulty = selectedLevel.difficultyName,
                                        level = selectedLevel.id
                                    )
                                    currentScreen = Screen.Game
                                },
                                onBack = { currentScreen = Screen.ModeSelection }
                            )

                            Screen.Game -> GameScreen(
                                viewModel = gameViewModel,
                                themeViewModel = themeViewModel,
                                onBackToMenu = {
                                    gameViewModel.resetGameSession()
                                    currentScreen = Screen.Menu
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

                BackHandler(enabled = currentScreen != Screen.Menu) {
                    when (currentScreen) {
                        Screen.Game -> {
                            gameViewModel.resetGameSession()
                            currentScreen = Screen.ModeSelection
                        }
                        Screen.LevelSelector -> currentScreen = Screen.ModeSelection
                        else -> currentScreen = Screen.Menu
                    }
                }
            }
        }
    }
}