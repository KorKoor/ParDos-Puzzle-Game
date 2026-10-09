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
import com.korkoor.pardos.audio.uiTapSounds
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
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
import com.korkoor.pardos.ui.menu.AccessibleMenuScreen
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
    data object Prestige : Screen()
    data object Gallery : Screen()
    data object LevelSelector : Screen()
    data object Profile : Screen()
    data object Friends : Screen()
    data object Shop : Screen()
    data object Collection : Screen()
    data object Multiplayer : Screen()
    data object Season : Screen()
    data object Wheel : Screen()
    data object Studio : Screen()
    data object Settings : Screen()
}

/** Nivel de profundidad de cada pantalla, para animar entrar/volver. */
private fun Screen.navDepth(): Int = when (this) {
    Screen.Splash -> 0
    Screen.Menu -> 1
    Screen.ModeSelection, Screen.CustomLevel, Screen.Records, Screen.Achievements, Screen.Prestige, Screen.Gallery,
    Screen.Profile, Screen.Friends, Screen.Shop, Screen.Collection, Screen.Multiplayer, Screen.AccessibilityGame,
    Screen.Season, Screen.Wheel -> 2
    Screen.Studio, Screen.Settings -> 3
    Screen.LevelSelector -> 3
    Screen.Game -> 4
}

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val NOTIFICATION_REQUEST_CODE = 101
    }

    private val gameViewModel: GameViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()
    private lateinit var notificationManager: ZenNotificationManager
    private val billingManager by lazy { com.korkoor.pardos.data.billing.BillingManager(this) }

    /** Pantalla a la que llevar al terminar el inicio (viene de tocar un aviso). */
    private var routeFromNotification: String? = null

    /** Sube (nunca baja) el nivel de campaña del perfil hasta el último nivel desbloqueado de verdad. */
    private fun healCampaignLevel(profileManager: com.korkoor.pardos.data.local.ProfileManager) {
        val reached = getSharedPreferences("pardos_storage", MODE_PRIVATE)
            .getInt(com.korkoor.pardos.data.local.LevelProgressStore.KEY_LAST_UNLOCKED, 1)
        profileManager.updateCampaignLevel(reached)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate - app launch start")

        // Solo en builds de depuración: `--ei debug_day_offset N` salta N días para probar fiestas
        if ((applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            com.korkoor.pardos.data.local.LocalDay.debugOffsetDays = intent.getIntExtra("debug_day_offset", 0)
        }

        routeFromNotification = intent?.getStringExtra(com.korkoor.pardos.notifications.NotificationRoute.EXTRA)
        notificationManager = ZenNotificationManager(this)
        // Sonido: efectos y música (se carga en segundo plano)
        com.korkoor.pardos.audio.GameAudio.init(this)
        // El permiso de avisos ya no se pide al abrir: lo pide `NotificationPrimerDialog` tras la primera victoria

        com.korkoor.pardos.ui.game.logic.AdManager.initialize(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        val profileManager = com.korkoor.pardos.data.local.ProfileManager(this)
        profileManager.checkAndUpdateStreak()
        if (!profileManager.shouldCheckCloud()) {
            // Ya se consultó hoy y el perfil local tiene progreso: no se gasta una lectura en cada arranque
            healCampaignLevel(profileManager)
        } else profileManager.syncFromFirebase { cloudProfile ->
            if (cloudProfile != null) {
                val localProfile = profileManager.getProfile()
                val freshLocal = localProfile.playerLevel == 1 && localProfile.currentXp == 0 && localProfile.name == "Jugador Zen"
                val cloudAhead = cloudProfile.playerLevel > localProfile.playerLevel ||
                    (cloudProfile.playerLevel == localProfile.playerLevel &&
                        (cloudProfile.currentCampaignLevel > localProfile.currentCampaignLevel || cloudProfile.currentXp > localProfile.currentXp))
                // Solo se restaura si la nube va por delante: guardar un perfil idéntico provocaría una subida inútil
                if (cloudAhead || freshLocal) {
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

            // El perfil de la nube puede traer un nivel de campaña menor que el progreso real guardado
            healCampaignLevel(profileManager)
            profileManager.checkAndUpdateStreak()
        }
        healCampaignLevel(profileManager)

        setContent {
            com.korkoor.pardos.ui.design.CozyClockProvider {
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
                            Lifecycle.Event.ON_PAUSE -> { gameViewModel.pauseGame(); com.korkoor.pardos.audio.GameAudio.onAppPause() }
                            Lifecycle.Event.ON_RESUME -> { gameViewModel.resumeGame(); com.korkoor.pardos.audio.GameAudio.onAppResume() }
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

                var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
                // Sonido de navegación y música según la pantalla: menú (melodía) o partida (música por capas)
                var lastScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
                LaunchedEffect(currentScreen) {
                    val from = lastScreen
                    lastScreen = currentScreen
                    when (currentScreen) {
                        Screen.Splash -> Unit
                        Screen.Game, Screen.AccessibilityGame -> com.korkoor.pardos.audio.GameAudio.music.game(gameViewModel.musicSet)
                        else -> com.korkoor.pardos.audio.GameAudio.music.menu()
                    }
                    if (from != Screen.Splash && currentScreen != Screen.Splash && from != currentScreen) {
                        val depth = currentScreen.navDepth() - from.navDepth()
                        when {
                            currentScreen == Screen.Game || currentScreen == Screen.AccessibilityGame -> com.korkoor.pardos.audio.GameAudio.play(com.korkoor.pardos.audio.Sfx.UI_WHOOSH)
                            depth > 0 -> com.korkoor.pardos.audio.GameAudio.play(com.korkoor.pardos.audio.Sfx.UI_OPEN)
                            depth < 0 -> com.korkoor.pardos.audio.GameAudio.play(com.korkoor.pardos.audio.Sfx.UI_CLOSE)
                        }
                    }
                }
                // La skin equipada trae su propia temática (fondo, texto, partículas)
                val skinEconomy = remember { com.korkoor.pardos.data.local.EconomyManager(this@MainActivity) }
                val equippedSkin by skinEconomy.equippedSkin.collectAsState()
                val studioConfig by skinEconomy.studioConfig.collectAsState()
                LaunchedEffect(equippedSkin, studioConfig) { themeViewModel.applySkin(equippedSkin) }

                // Compras: se conecta al abrir para tener precios y recuperar compras pendientes
                val storePrices by billingManager.prices.collectAsState()
                LaunchedEffect(Unit) { billingManager.connect() }
                // Pantallas con texto oscuro sobre el fondo usan un tema claro (las skins oscuras solo aplican al juego y al menú)
                val currentTheme = themeViewModel.uiTheme

                LaunchedEffect(gameViewModel.dailyChallengeThemeIndex) {
                    gameViewModel.dailyChallengeThemeIndex?.let { index ->
                        themeViewModel.selectThemeByIndex(index)
                    }
                }

                val allLevels by gameViewModel.levels.collectAsState()
                val savedRecords by gameViewModel.allRecords.collectAsState(initial = emptyList())
                val unlockedIds by gameViewModel.unlockedAchievements.collectAsState()

                Surface(modifier = Modifier.fillMaxSize().uiTapSounds()) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            // Profundidad: entrar a una pantalla más profunda desliza hacia la izquierda,
                            // volver desliza hacia la derecha. Splash y juego usan fundido.
                            val from = initialState.navDepth()
                            val to = targetState.navDepth()
                            when {
                                initialState == Screen.Splash || targetState == Screen.Splash ->
                                    fadeIn(tween(500)) togetherWith fadeOut(tween(500))
                                targetState == Screen.Game || initialState == Screen.Game ->
                                    (fadeIn(tween(350)) + scaleIn(initialScale = 0.96f, animationSpec = tween(350))) togetherWith
                                        fadeOut(tween(250))
                                to > from ->
                                    (slideInHorizontally(tween(320)) { it / 4 } + fadeIn(tween(320))) togetherWith
                                        (slideOutHorizontally(tween(320)) { -it / 6 } + fadeOut(tween(220)))
                                else ->
                                    (slideInHorizontally(tween(320)) { -it / 4 } + fadeIn(tween(320))) togetherWith
                                        (slideOutHorizontally(tween(320)) { it / 6 } + fadeOut(tween(220)))
                            }
                        },
                        label = "MainNavigation"
                    ) { target ->
                        when (target) {
                            Screen.Splash -> AnimatedSplashScreen(onAnimationFinished = {
                                // Solo en builds de depuración: `--ei debug_level N` abre ese nivel de campaña directamente
                                val debuggable = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
                                val debugGallery = if (debuggable) intent?.getIntExtra("debug_gallery", 0) ?: 0 else 0
                                if (debugGallery > 0) { currentScreen = Screen.Gallery; return@AnimatedSplashScreen }
                                val debugLevel = if (debuggable) intent?.getIntExtra("debug_level", 0) ?: 0 else 0
                                // Torre en depuración: `--ei debug_tower PISO [--ei debug_hearts N] [--es debug_tower_end win|lose]`
                                val debugTower = if (debuggable) intent?.getIntExtra("debug_tower", 0) ?: 0 else 0
                                if (debugTower > 0) {
                                    gameViewModel.updateAccessibilitySpawnAssist(false)
                                    gameViewModel.debugStartTower(debugTower, intent?.getIntExtra("debug_hearts", 3) ?: 3, intent?.getStringExtra("debug_tower_end"))
                                    currentScreen = Screen.Game
                                    return@AnimatedSplashScreen
                                }
                                if (debugLevel > 0) {
                                    gameViewModel.updateAccessibilitySpawnAssist(false)
                                    gameViewModel.startCampaignLevel(debugLevel)
                                    intent?.getIntExtra("debug_move_limit", 0)?.takeIf { it > 0 }?.let { gameViewModel.debugSetMoveLimit(it) }
                                    currentScreen = Screen.Game
                                    return@AnimatedSplashScreen
                                }
                                currentScreen = when (routeFromNotification) {
                                    "wheel" -> Screen.Wheel
                                    "season" -> Screen.Season
                                    "friends" -> Screen.Friends
                                    "album" -> Screen.Collection
                                    "shop" -> Screen.Shop
                                    "chest", "piggy" -> {
                                        com.korkoor.pardos.notifications.NotificationRoute.pendingDialog = routeFromNotification
                                        Screen.Menu
                                    }
                                    else -> Screen.Menu
                                }
                                routeFromNotification = null
                            })

                            Screen.Menu -> {
                                SideEffect { gameViewModel.resetGameSession() }
                                val onPlayAction = {
                                    if (isScreenReaderEnabled) {
                                        gameViewModel.updateAccessibilitySpawnAssist(true)
                                        gameViewModel.setupCustomGame(
                                            size = 4,
                                            target = 128,
                                            allowPowerUps = false,
                                            difficulty = "Zen",
                                            level = 1,
                                            initialScore = 0,
                                            isCustom = true
                                        )
                                        currentScreen = Screen.AccessibilityGame
                                    } else {
                                        gameViewModel.updateAccessibilitySpawnAssist(false)
                                        currentScreen = Screen.ModeSelection
                                    }
                                }
                                val onCustomAction = { currentScreen = Screen.CustomLevel }
                                val onRecordsAction = { currentScreen = Screen.Records }
                                val onAchievementsAction = { currentScreen = Screen.Achievements }
                                val onDailyChallengeAction = {
                                    gameViewModel.updateAccessibilitySpawnAssist(isScreenReaderEnabled)
                                    gameViewModel.setupDailyChallenge()
                                    currentScreen = if (isScreenReaderEnabled) Screen.AccessibilityGame else Screen.Game
                                }
                                val onProfileAction = { currentScreen = Screen.Profile }
                                val onFriendsAction = { currentScreen = Screen.Friends }

                                if (isScreenReaderEnabled) {
                                    AccessibleMenuScreen(
                                        onPlayClick = onPlayAction,
                                        onCustomClick = onCustomAction,
                                        onRecordsClick = onRecordsAction,
                                        onAchievementsClick = onAchievementsAction,
                                        onDailyChallengeClick = onDailyChallengeAction,
                                        onProfileClick = onProfileAction,
                                        onFriendsClick = onFriendsAction
                                    )
                                } else {
                                    MenuScreen(
                                        onPlayClick = onPlayAction,
                                        onCustomClick = onCustomAction,
                                        onRecordsClick = onRecordsAction,
                                        onAchievementsClick = onAchievementsAction,
                                        onDailyChallengeClick = onDailyChallengeAction,
                                        onProfileClick = onProfileAction,
                                        onFriendsClick = onFriendsAction,
                                        themeViewModel = themeViewModel,
                                        onShopClick = { currentScreen = Screen.Shop },
                                        onCollectionClick = { currentScreen = Screen.Collection },
                                        onMultiplayerClick = { currentScreen = Screen.Multiplayer },
                                        onSeasonClick = { currentScreen = Screen.Season },
                                        onWheelClick = { currentScreen = Screen.Wheel },
                                        piggyPrice = storePrices[com.korkoor.pardos.domain.shop.ShopCatalog.PIGGY_BREAK],
                                        onBuyPiggy = { billingManager.purchase(this@MainActivity, com.korkoor.pardos.domain.shop.ShopCatalog.PIGGY_BREAK) }
                                    )
                                }
                            }

                            Screen.AccessibilityGame -> {
                                SideEffect { gameViewModel.updateAccessibilitySpawnAssist(true) }
                                com.korkoor.pardos.ui.game.AccessibleGameScreen(
                                    viewModel = gameViewModel,
                                    onExitApp = { currentScreen = Screen.Menu }
                                )
                            }

                            Screen.ModeSelection -> ModeSelectionScreen(
                                onModeSelected = { mode ->
                                    gameViewModel.updateAccessibilitySpawnAssist(false)
                                    if (mode == GameMode.CLASICO) {
                                        currentScreen = Screen.LevelSelector
                                    } else {
                                        gameViewModel.startNewGame(mode)
                                        currentScreen = Screen.Game
                                    }
                                },
                                onTowerSelected = {
                                    gameViewModel.updateAccessibilitySpawnAssist(false)
                                    gameViewModel.startTower()
                                    currentScreen = Screen.Game
                                },
                                onBack = { currentScreen = Screen.Menu },
                                currentTheme = currentTheme
                            )

                            Screen.LevelSelector -> LevelSelectorScreen(
                                levels = allLevels,
                                currentTheme = currentTheme,
                                onLevelSelected = { selectedLevel ->
                                    gameViewModel.updateAccessibilitySpawnAssist(false)
                                    gameViewModel.startCampaignLevel(selectedLevel.id)
                                    currentScreen = Screen.Game
                                },
                                onBack = { currentScreen = Screen.ModeSelection },
                                onRefresh = { gameViewModel.loadLevelsWithProgress() }
                            )

                            Screen.Game -> GameScreen(
                                viewModel = gameViewModel,
                                themeViewModel = themeViewModel,
                                onBackToMenu = {
                                    gameViewModel.updateAccessibilitySpawnAssist(false)
                                    gameViewModel.resetGameSession()
                                    currentScreen = when (gameViewModel.currentMode) {
                                        GameMode.CLASICO -> Screen.LevelSelector
                                        GameMode.DUELO -> Screen.Multiplayer
                                        else -> Screen.Menu
                                    }
                                }
                            )

                            Screen.CustomLevel -> CustomLevelScreen(
                                onStartCustom = { size, targetVal, allowPowerUps, difficulty ->
                                    gameViewModel.updateAccessibilitySpawnAssist(isScreenReaderEnabled)
                                    gameViewModel.setupCustomGame(size, targetVal, allowPowerUps, difficulty, isCustom = true)
                                    currentScreen = if (isScreenReaderEnabled) Screen.AccessibilityGame else Screen.Game
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
                                onBack = { currentScreen = Screen.Menu },
                                onPrestige = { currentScreen = Screen.Prestige }
                            )

                            Screen.Gallery -> com.korkoor.pardos.ui.profile.CosmeticsGallery(
                                intent?.getIntExtra("debug_gallery", 1) ?: 1, intent?.getIntExtra("debug_page", 0) ?: 0
                            )

                            Screen.Prestige -> com.korkoor.pardos.ui.prestige.PrestigeScreen(
                                onBack = { currentScreen = Screen.Menu },
                                onTrophies = { currentScreen = Screen.Achievements },
                                onAlbum = { currentScreen = Screen.Collection }
                            )

                            Screen.Profile -> com.korkoor.pardos.ui.profile.ProfileScreen(
                                onBack = { currentScreen = Screen.Menu },
                                onRecords = { currentScreen = Screen.Records },
                                onSettings = { currentScreen = Screen.Settings },
                                onPrestige = { currentScreen = Screen.Prestige }
                            )
                            Screen.Settings -> com.korkoor.pardos.ui.settings.SettingsScreen(onBack = { currentScreen = Screen.Profile })
                            Screen.Friends -> com.korkoor.pardos.ui.profile.FriendsScreen(onBack = { currentScreen = Screen.Menu })
                            Screen.Collection -> com.korkoor.pardos.ui.collection.CollectionScreen(onBack = { currentScreen = Screen.Menu })

                            Screen.Multiplayer -> com.korkoor.pardos.ui.social.MultiplayerHub(
                                onBack = { currentScreen = Screen.Menu },
                                onLocalDuel = {
                                    gameViewModel.updateAccessibilitySpawnAssist(false)
                                    gameViewModel.startNewGame(GameMode.DUELO)
                                    currentScreen = Screen.Game
                                },
                                onFriends = { currentScreen = Screen.Friends },
                                onDailyChallenge = {
                                    gameViewModel.updateAccessibilitySpawnAssist(false)
                                    gameViewModel.setupDailyChallenge()
                                    currentScreen = Screen.Game
                                },
                                onCreateChallenge = {
                                    gameViewModel.updateAccessibilitySpawnAssist(false)
                                    gameViewModel.startRemoteCreate()
                                    currentScreen = Screen.Game
                                },
                                onAcceptChallenge = { challenge ->
                                    gameViewModel.updateAccessibilitySpawnAssist(false)
                                    gameViewModel.startRemoteAccept(challenge)
                                    currentScreen = Screen.Game
                                }
                            )
                            Screen.Shop -> com.korkoor.pardos.ui.shop.ShopScreen(
                                activity = this@MainActivity,
                                billing = billingManager,
                                economy = com.korkoor.pardos.data.local.EconomyManager(this@MainActivity),
                                onBack = { currentScreen = Screen.Menu },
                                onStudio = { currentScreen = Screen.Studio },
                                onSeason = { currentScreen = Screen.Season }
                            )

                            Screen.Season -> com.korkoor.pardos.ui.season.SeasonScreen(
                                onBack = { currentScreen = Screen.Menu },
                                premiumPrice = storePrices[com.korkoor.pardos.domain.shop.ShopCatalog.SEASON_PASS],
                                onBuyPremium = { billingManager.purchase(this@MainActivity, com.korkoor.pardos.domain.shop.ShopCatalog.SEASON_PASS) }
                            )

                            Screen.Wheel -> com.korkoor.pardos.ui.rewards.WheelScreen(onBack = { currentScreen = Screen.Menu })

                            Screen.Studio -> com.korkoor.pardos.ui.studio.StudioScreen(
                                onBack = { currentScreen = Screen.Shop },
                                price = storePrices[com.korkoor.pardos.domain.shop.ShopCatalog.SKIN_STUDIO],
                                onBuy = { billingManager.purchase(this@MainActivity, com.korkoor.pardos.domain.shop.ShopCatalog.SKIN_STUDIO) }
                            )
                        }
                    }
                    // Celebraciones de prestigio (hitos, títulos, rangos y Platino) por encima de cualquier pantalla
                    com.korkoor.pardos.ui.prestige.PrestigeToastHost()
                }

                BackHandler(enabled = currentScreen != Screen.Menu && currentScreen != Screen.Splash && currentScreen != Screen.AccessibilityGame) {
                    Log.d(TAG, "Back pressed on screen=$currentScreen")
                    when (currentScreen) {
                        Screen.Game -> {
                            gameViewModel.resetGameSession()
                            currentScreen = when (gameViewModel.currentMode) {
                                GameMode.CLASICO -> Screen.LevelSelector
                                GameMode.DUELO -> Screen.Multiplayer
                                else -> Screen.ModeSelection
                            }
                        }
                        Screen.LevelSelector -> currentScreen = Screen.ModeSelection
                        Screen.Studio -> currentScreen = Screen.Shop
                        Screen.Settings -> currentScreen = Screen.Profile
                        else -> currentScreen = Screen.Menu
                    }
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
        // Hitos, rango y Platino al día cada vez que vuelves a la app
        try { com.korkoor.pardos.data.local.PrestigeManager(this).refresh() } catch (e: Exception) { Log.w(TAG, "prestigio: ${e.message}") }
        if (::notificationManager.isInitialized) {
            Log.d(TAG, "onResume -> cancelAllNotifications")
            notificationManager.cancelAllNotifications()
        }
    }

    override fun onPause() {
        super.onPause()
        // Sube los cambios pendientes del perfil (una sola escritura por sesión como máximo)
        com.korkoor.pardos.data.local.ProfileManager(this).flushPendingSync()
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
