package com.korkoor.pardos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.korkoor.pardos.domain.logic.ProgressionEngine
import com.korkoor.pardos.domain.model.GameMode
import com.korkoor.pardos.notifications.ZenNotificationManager
import com.korkoor.pardos.ui.game.AchievementsScreen
import com.korkoor.pardos.ui.game.GameScreen
import com.korkoor.pardos.ui.game.GameViewModel
import com.korkoor.pardos.ui.menu.AnimatedSplashScreen
import com.korkoor.pardos.ui.menu.CustomLevelScreen
import com.korkoor.pardos.ui.menu.LevelSelectorScreen
import com.korkoor.pardos.ui.menu.MenuScreen
import com.korkoor.pardos.ui.menu.ModeSelectionScreen
import com.korkoor.pardos.ui.records.RecordsScreen
import com.korkoor.pardos.ui.theme.PardosTheme
import com.korkoor.pardos.ui.theme.ThemeViewModel

sealed class Screen {
    data object Splash : Screen()
    data object Menu : Screen()
    data object AccessibilityGame : Screen()
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

    companion object {
        private const val TAG = "MainActivity"
        private const val NOTIFICATION_REQUEST_CODE = 101
    }

    private val gameViewModel: GameViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()
    private lateinit var notificationManager: ZenNotificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate - app launch start")

        notificationManager = ZenNotificationManager(this)
        requestNotificationPermissionIfNeeded()

        com.korkoor.pardos.ui.game.logic.AdManager.initialize(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val profileManager = com.korkoor.pardos.data.local.ProfileManager(this)
        profileManager.syncFromFirebase { cloudProfile ->
            if (cloudProfile != null) {
                val localProfile = profileManager.getProfile()
                if (cloudProfile.playerLevel >= localProfile.playerLevel) {
                    Log.d(TAG, "Restoring cloud profile at level=${cloudProfile.playerLevel}")
                    profileManager.saveProfile(cloudProfile)
                    gameViewModel.loadLevelsWithProgress()
                } else {
                    Log.d(TAG, "Keeping local profile. local=${localProfile.playerLevel} cloud=${cloudProfile.playerLevel}")
                }
            } else {
                Log.d(TAG, "No cloud profile found. Checking legacy migration")
                val oldPrefs = getSharedPreferences("pardos_prefs", MODE_PRIVATE)
                val legacyLevel = oldPrefs.getInt("last_unlocked_level", 1)
                profileManager.migrateLegacyProgressIfNeeded(legacyLevel)
            }

            profileManager.checkAndUpdateStreak()
        }

        setContent {
            PardosTheme {
                val lifecycleOwner = LocalLifecycleOwner.current
                val accessibilityManager = remember {
                    getSystemService(AccessibilityManager::class.java)
                }
                var isScreenReaderEnabled by remember {
                    mutableStateOf(isScreenReaderActive(accessibilityManager))
                }

                LaunchedEffect(Unit) {
                    Log.d(TAG, "Initial screenReaderEnabled=$isScreenReaderEnabled")
                }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_PAUSE -> gameViewModel.pauseGame()
                            Lifecycle.Event.ON_RESUME -> gameViewModel.resumeGame()
                            else -> Unit
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                DisposableEffect(accessibilityManager) {
                    if (accessibilityManager == null) {
                        onDispose { }
                    } else {
                        val touchListener = AccessibilityManager.TouchExplorationStateChangeListener {
                            isScreenReaderEnabled = isScreenReaderActive(accessibilityManager)
                            Log.d(TAG, "Touch exploration changed -> screenReaderEnabled=$isScreenReaderEnabled")
                        }
                        val stateListener = AccessibilityManager.AccessibilityStateChangeListener {
                            isScreenReaderEnabled = isScreenReaderActive(accessibilityManager)
                            Log.d(TAG, "Accessibility state changed -> screenReaderEnabled=$isScreenReaderEnabled")
                        }

                        accessibilityManager.addTouchExplorationStateChangeListener(touchListener)
                        accessibilityManager.addAccessibilityStateChangeListener(stateListener)

                        onDispose {
                            accessibilityManager.removeTouchExplorationStateChangeListener(touchListener)
                            accessibilityManager.removeAccessibilityStateChangeListener(stateListener)
                        }
                    }
                }

                var currentScreen by remember {
                    mutableStateOf<Screen>(if (isScreenReaderEnabled) Screen.AccessibilityGame else Screen.Splash)
                }
                val currentTheme = themeViewModel.currentTheme

                LaunchedEffect(isScreenReaderEnabled) {
                    if (isScreenReaderEnabled && currentScreen != Screen.AccessibilityGame) {
                        Log.d(TAG, "Switching to accessibility mode")
                        gameViewModel.resetGameSession()
                        currentScreen = Screen.AccessibilityGame
                    }
                }

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
                        transitionSpec = { fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(600)) },
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

                            Screen.AccessibilityGame -> {
                                com.korkoor.pardos.ui.game.AccessibleGameScreen(
                                    viewModel = gameViewModel,
                                    onExitApp = { finish() }
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
                                    currentScreen = if (gameViewModel.currentMode == GameMode.CLASICO) {
                                        Screen.LevelSelector
                                    } else {
                                        Screen.Menu
                                    }
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

                            Screen.Profile -> com.korkoor.pardos.ui.profile.ProfileScreen(onBack = { currentScreen = Screen.Menu })
                            Screen.Friends -> com.korkoor.pardos.ui.profile.FriendsScreen(onBack = { currentScreen = Screen.Menu })
                        }
                    }
                }

                BackHandler(enabled = currentScreen != Screen.Menu && currentScreen != Screen.Splash && currentScreen != Screen.AccessibilityGame) {
                    Log.d(TAG, "Back pressed on screen=$currentScreen")
                    when (currentScreen) {
                        Screen.Game -> {
                            gameViewModel.resetGameSession()
                            currentScreen = if (gameViewModel.currentMode == GameMode.CLASICO) {
                                Screen.LevelSelector
                            } else {
                                Screen.ModeSelection
                            }
                        }
                        Screen.LevelSelector -> currentScreen = Screen.ModeSelection
                        else -> currentScreen = Screen.Menu
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_REQUEST_CODE) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "POST_NOTIFICATIONS result granted=$granted")
        }
    }

    override fun onResume() {
        super.onResume()
        if (::notificationManager.isInitialized) {
            Log.d(TAG, "onResume -> cancelAllNotifications")
            notificationManager.cancelAllNotifications()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::notificationManager.isInitialized) {
            Log.d(TAG, "onPause -> scheduleAllNotifications")
            notificationManager.scheduleAllNotifications()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Log.d(TAG, "Notification runtime permission not required on SDK ${Build.VERSION.SDK_INT}")
            return
        }

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        Log.d(TAG, "POST_NOTIFICATIONS granted=$granted")
        if (!granted) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_REQUEST_CODE)
            Log.d(TAG, "Requested POST_NOTIFICATIONS permission")
        }
    }

    private fun isScreenReaderActive(manager: AccessibilityManager?): Boolean {
        if (manager == null) return false
        return manager.isEnabled && manager.isTouchExplorationEnabled
    }
}
