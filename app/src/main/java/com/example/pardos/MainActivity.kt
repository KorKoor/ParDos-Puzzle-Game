package com.korkoor.pardos

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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.firebase.auth.FirebaseAuth
import com.korkoor.pardos.domain.model.GameMode
import com.korkoor.pardos.domain.logic.ProgressionEngine
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

// 1. Agregamos las pantallas posibles (Incluyendo Perfil y Amigos)
sealed class Screen {
    data object Splash : Screen()
    data object Menu : Screen()
    data object ModeSelection : Screen()
    data object Game : Screen()
    data object CustomLevel : Screen()
    data object Records : Screen()
    data object Achievements : Screen()
    data object LevelSelector : Screen()
    data object Profile : Screen() // 🔥 NUEVO
    data object Friends : Screen() // 🔥 NUEVO
}

class MainActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializamos AdManager
        com.korkoor.pardos.ui.game.logic.AdManager.initialize(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Inicialización silenciosa de Firebase Auth
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FIREBASE_AUTH", "Usuario fantasma creado con UID: ${auth.currentUser?.uid}")
                }
            }
        }

        // Actualizamos la racha del jugador al abrir la app
        val profileManager = com.korkoor.pardos.data.local.ProfileManager(this)
        profileManager.checkAndUpdateStreak()

        setContent {
            PardosTheme {
                // 🚀 CICLO DE VIDA: DETECTOR DE SEGUNDO PLANO
                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_PAUSE -> {
                                gameViewModel.pauseGame()
                            }
                            Lifecycle.Event.ON_RESUME -> {
                                gameViewModel.resumeGame()
                            }
                            else -> {}
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                // --- ESTADO DE NAVEGACIÓN ---
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
                            Screen.Splash -> {
                                AnimatedSplashScreen(
                                    onAnimationFinished = {
                                        currentScreen = Screen.Menu
                                    }
                                )
                            }

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
                                    // 🔥 AQUÍ CONECTAMOS LOS NUEVOS BOTONES DEL MENÚ 🔥
                                    onProfileClick = { currentScreen = Screen.Profile },
                                    onFriendsClick = { currentScreen = Screen.Friends },
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

                            // 🔥 PANTALLA DE PERFIL 🔥
                            Screen.Profile -> {
                                // Llamamos a tu ProfileScreen. Le agregamos un botón simple para volver
                                // mientras no le ponemos un TopAppBar oficial.
                                Box(modifier = Modifier.fillMaxSize()) {
                                    com.korkoor.pardos.ui.profile.ProfileScreen()

                                    // Botón provisional para volver atrás (puedes ajustarlo después)
                                    androidx.compose.material3.IconButton(
                                        onClick = { currentScreen = Screen.Menu },
                                        modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                                    ) {
                                        androidx.compose.material3.Text("🔙", fontSize = 24.sp)
                                    }
                                }
                            }

                            // 🔥 PANTALLA DE AMIGOS (En construcción) 🔥
                            Screen.Friends -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Próximamente: Lista de Amigos")

                                    // Botón provisional para volver atrás
                                    androidx.compose.material3.IconButton(
                                        onClick = { currentScreen = Screen.Menu },
                                        modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                                    ) {
                                        androidx.compose.material3.Text("🔙", fontSize = 24.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Manejo del botón físico "Atrás"
                BackHandler(enabled = currentScreen != Screen.Menu && currentScreen != Screen.Splash) {
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
                        Screen.Profile -> currentScreen = Screen.Menu // 🔥 NUEVO
                        Screen.Friends -> currentScreen = Screen.Menu // 🔥 NUEVO
                        else -> currentScreen = Screen.Menu
                    }
                }
            }
        }
    }
}