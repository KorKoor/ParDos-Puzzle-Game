package com.korkoor.pardos.ui.game

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.lifecycle.*
import androidx.room.Room
import com.korkoor.pardos.data.local.AppDatabase
import com.korkoor.pardos.domain.achievements.gameAchievements
import com.korkoor.pardos.domain.achievements.Achievement
import com.korkoor.pardos.domain.logic.*
import com.korkoor.pardos.domain.model.*
import com.korkoor.pardos.domain.level.*
import com.korkoor.pardos.ui.game.components.FloatingScoreModel
import com.korkoor.pardos.ui.game.logic.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import kotlin.random.Random
import kotlin.math.max
import kotlin.math.abs
import androidx.core.content.edit

private const val COOLDOWN_MS = 15 * 60 * 1000L // 15 minutos

class GameViewModel(application: Application) : AndroidViewModel(application) {

    // 🔊 GESTOR DE SONIDOS
    private val soundManager = SoundManager(application)

    // 1. ESTADOS DE COMPOSE
    var showLevelSummary by mutableStateOf(false)
        private set

    private val _comboCount = mutableStateOf(0)
    val comboCount: State<Int> = _comboCount

    // 🎈 LISTA DE PUNTOS FLOTANTES
    val floatingScores = mutableStateListOf<FloatingScoreModel>()

    var loadingAdType by mutableStateOf<String?>(null)
        private set

    var currentMultiplierBase by mutableIntStateOf(2)
        private set

    var currentMode by mutableStateOf<GameMode>(GameMode.CLASICO)
        private set

    var accessibilitySpawnAssist by mutableStateOf(false)
        private set

    var activeAchievementPopup by mutableStateOf<Achievement?>(null)
        private set
    var isSelectModeActive by mutableStateOf(false)
        private set

    var pendingPowerUpType by mutableStateOf<String?>(null) // "MANUAL_MERGE" o "SINGLE_CLEAN"
        private set

    var firstSelectedTileId by mutableStateOf<String?>(null)
        private set
    // --- DESHACER ---
    private data class UndoSnapshot(
        val tiles: List<TileModel>, val score: Int, val moves: Int,
        val stats: com.korkoor.pardos.domain.level.GoalStats = com.korkoor.pardos.domain.level.GoalStats(),
        val stormStones: List<com.korkoor.pardos.domain.level.StormStone> = emptyList(),
        val blocked: List<com.korkoor.pardos.domain.level.Cell> = emptyList()
    )
    private var lastSnapshot: UndoSnapshot? = null
    var canUndo by mutableStateOf(false)
        private set
    val undoCount get() = economy.undos

    /** Vuelve a la jugada anterior gastando un "Deshacer". No se permite en duelo ni en carrera (sería ventaja). */
    fun undoLastMove(): Boolean {
        val snap = lastSnapshot ?: return false
        if (currentMode == GameMode.DUELO || currentMode == GameMode.CARRERA) return false
        if (isMoving || _boardState.value.isGameOver || _boardState.value.isLevelCompleted) return false
        if (!economy.useUndo()) return false
        // En las tormentas se vuelve también a las piedras de esa jugada: así nunca queda una piedra sobre una ficha
        if (snap.blocked != _boardState.value.blocked) {
            gameEngine = GameEngine(boardSize = _boardState.value.boardSize, random = rng, blocked = snap.blocked.toSet())
        }
        _boardState.update {
            it.copy(tiles = snap.tiles, score = snap.score, moveCount = snap.moves, goalStats = snap.stats, stormStones = snap.stormStones, blocked = snap.blocked)
        }
        lastSnapshot = null
        canUndo = false
        return true
    }

    // --- TIEMPO EXTRA ---
    val extraTimeCount get() = economy.extraTimes

    /** Suma [Economy.EXTRA_TIME_SECONDS] al reloj gastando un "Tiempo extra". No se permite en duelo (sería ventaja). */
    fun useExtraTime(): Boolean {
        val state = _boardState.value
        val max = state.maxTime ?: return false
        if (currentMode == GameMode.DUELO || state.isGameOver || state.isLevelCompleted) return false
        if (!economy.useExtraTime()) return false
        val bonusMs = com.korkoor.pardos.domain.economy.Economy.EXTRA_TIME_SECONDS * 1000L
        // Se suma a las dos cifras: así el tiempo usado (máx - restante) no cambia
        _boardState.update { it.copy(elapsedTime = it.elapsedTime + bonusMs, maxTime = max + bonusMs) }
        return true
    }

    // --- DUELO LOCAL ---
    var duelPlayer by mutableIntStateOf(1)
        private set
    var duelPhase by mutableStateOf(DuelPhase.PLAYING)
        private set
    val duelScores = mutableStateListOf(0, 0)
    private var duelSeed = 0L

    // --- DUELO A DISTANCIA (por código) ---
    /** NONE = duelo local; CREATOR = juego mi reto para compartirlo; CHALLENGED = acepto el reto de otro. */
    var remoteRole by mutableStateOf(RemoteRole.NONE)
        private set
    /** El reto que acepté, o el que acabo de crear (con mi puntaje) para compartir. */
    var remoteChallenge by mutableStateOf<com.korkoor.pardos.domain.logic.RemoteChallenge?>(null)
        private set
    var remoteOutcome by mutableStateOf<com.korkoor.pardos.domain.logic.RemoteOutcome?>(null)
        private set
    var remoteReward by mutableStateOf<com.korkoor.pardos.domain.logic.RemoteDuel.Reward?>(null)
        private set
    var remoteFirstTime by mutableStateOf(true)
        private set

    // --- MODO CARRERA ---
    var raceStage by mutableIntStateOf(1)
        private set
    var raceStagesCleared by mutableIntStateOf(0)
        private set
    /** Segundos ganados al superar una etapa; la UI lo muestra un instante. */
    var raceBonusFlash by mutableStateOf<Int?>(null)
        private set
    // Monedas ganadas en la última victoria (para mostrarlas en el resumen)
    var lastCoinsEarned by mutableIntStateOf(0)
        private set
    private val economy = com.korkoor.pardos.data.local.EconomyManager(application)
    private val rewardsManager = com.korkoor.pardos.data.local.RewardsManager(application)
    private val retention = com.korkoor.pardos.data.local.RetentionManager(application)
    private val collection = com.korkoor.pardos.data.local.CollectionManager(application)

    /** Extras de la última partida (primera victoria del día, hucha, puntos de pase) para el resumen. */
    var lastGameBonus by mutableStateOf(com.korkoor.pardos.data.local.GameBonus())
        private set

    var lastCleanTime by mutableLongStateOf(0L)
    var lastMergeTime by mutableLongStateOf(0L)

    private val KEY_PROFILE_SETUP = "is_profile_setup_complete"

    // Estado para avisarle a la UI de Compose que debe mostrar la pantalla de creación
    var showProfileSetupRedirect by mutableStateOf(false)
        private set

    // 🔥 TEMA DEL DESAFÍO DIARIO
    var dailyChallengeThemeIndex by mutableStateOf<Int?>(null)
        private set

    // Control para saber si el juego ya empezó (primer movimiento)
    var isGameStarted by mutableStateOf(false)
        private set

    // Control del sistema de ayuda (Piedad)
    var isPityModeActive by mutableStateOf(false)
        private set

    // --- FLOW: ímpetu del nivel, racha de victorias y ayuda adaptativa (ver domain/flow) ---
    data class FlowCallout(val tier: com.korkoor.pardos.domain.flow.FlowTier, val id: Int)

    /** Jugadas seguidas que fusionan en este nivel. */
    var flowStreak by mutableIntStateOf(0)
        private set
    var flowCallout by mutableStateOf<FlowCallout?>(null)
        private set
    private var peakFlowTier = com.korkoor.pardos.domain.flow.FlowTier.CALM
    private var calloutSeq = 0

    /** Niveles de campaña ganados seguidos (persistente; perder la enfría a la mitad). */
    var winStreak by mutableIntStateOf(0)
        private set
    var lastStreakBonusPct by mutableIntStateOf(0)
        private set
    var lastFlowBonusPct by mutableIntStateOf(0)
        private set
    var lastStreakMilestone by mutableStateOf<com.korkoor.pardos.domain.flow.WinStreak.Milestone?>(null)
        private set

    /** Ayuda a la vista cuando un nivel se atasca ("Te echamos una mano…") y el "casi" de la última derrota. */
    var assistMessage by mutableStateOf<String?>(null)
        private set
    var nearMissMessage by mutableStateOf<String?>(null)
        private set
    private var activeAssist = com.korkoor.pardos.domain.flow.AssistPolicy.forAttempts(0)
    private val KEY_WIN_STREAK = "campaign_win_streak"

    private fun isCampaignRun() = currentMode == GameMode.CLASICO && dailyChallengeThemeIndex == null

    /** Derrota: cuenta el intento (para la ayuda), enfría la racha y prepara la frase del "casi". */
    private fun registerLoss() {
        val s = _boardState.value
        val level = s.currentLevel
        prefs.edit().putInt("$KEY_ATTEMPTS$level", prefs.getInt("$KEY_ATTEMPTS$level", 0) + 1).apply()
        if (isCampaignRun()) {
            winStreak = com.korkoor.pardos.domain.flow.WinStreak.afterLoss(winStreak)
            prefs.edit().putInt(KEY_WIN_STREAK, winStreak).apply()
        }
        nearMissMessage = com.korkoor.pardos.domain.flow.NearMiss.message(s.goal, s.levelLimit, s.goalCount, s.tiles, s.score, s.goalStats, s.outOfMoves)
    }

    // 🔥 TIEMPO REAL: Variable para guardar la hora exacta de inicio del sistema
    private var realStartTime: Long = 0L

    // 2. PREFERENCIAS
    private val prefs = application.getSharedPreferences("pardos_storage", Context.MODE_PRIVATE)
    private val levelStore = com.korkoor.pardos.data.local.LevelProgressStore(prefs)
    private val KEY_TABLES_LEVEL = com.korkoor.pardos.data.local.LevelProgressStore.KEY_TABLES_LEVEL
    private val KEY_LAST_UNLOCKED = com.korkoor.pardos.data.local.LevelProgressStore.KEY_LAST_UNLOCKED
    private val KEY_SAVED_SCORE = "saved_score_level"
    // Nueva llave para contar intentos fallidos
    private val KEY_ATTEMPTS = "attempts_fail_level_"
    init { winStreak = prefs.getInt("campaign_win_streak", 0) }

    // 3. ESTADOS DE FLUJO
    private val _currentTimeProvider = MutableStateFlow(System.currentTimeMillis())
    val currentTimeProvider: StateFlow<Long> = _currentTimeProvider

    private val _levels = MutableStateFlow<List<LevelInfo>>(emptyList())
    val levels: StateFlow<List<LevelInfo>> = _levels.asStateFlow()

    private val _unlockedAchievements = MutableStateFlow<Set<String>>(emptySet())
    val unlockedAchievements: StateFlow<Set<String>> = _unlockedAchievements

    // 4. ESTADO INICIAL
    private val initialLevel = prefs.getInt(KEY_LAST_UNLOCKED, 1)
    private val initialTarget = ProgressionEngine.calculateTargetForLevel(initialLevel)
    private val initialSize = ProgressionEngine.calculateBoardSize(initialTarget)

    // 🔥 NUEVO: EL GESTOR DE MISIONES
    private val missionManager = com.korkoor.pardos.data.local.MissionManager(application)

    private val _boardState = MutableStateFlow(
        BoardState(
            currentLevel = initialLevel,
            levelLimit = initialTarget,
            boardSize = initialSize,
            tiles = emptyList(),
            gameMode = GameMode.CLASICO
        )
    )
    val boardState = _boardState.asStateFlow()

    // Azar de la partida: con semilla (reto diario, duelos) es reproducible; sin semilla, aleatorio
    private var rng: Random = Random.Default

    /** Reglas del nivel de campaña en curso (`null` en los demás modos). Lo único que el juego lee de ellas es esto. */
    private var activeSpec: LevelSpec? = null
    var currentSeed: Long? = null
        private set
    private var gameEngine = GameEngine(boardSize = 3)
    private var isMoving = false
    private var timerJob: Job? = null

    val shouldBlurBackground: Boolean
        get() = showLevelSummary || _boardState.value.isGameOver

    private val timerManager = GameTimerManager(
        scope = viewModelScope,
        onTick = { newTime: Long ->
            _boardState.update { it.copy(elapsedTime = newTime) }
        },
        onTimeUp = {
            handleGameOver()
        }
    )

    // 5. BASE DE DATOS
    private val db = Room.databaseBuilder(application, AppDatabase::class.java, "pardos-db")
        .fallbackToDestructiveMigration() // VITAL para la actualización de versión
        .build()
    private val recordDao = db.recordDao()

    val allRecords = recordDao.getAllRecords().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // 6. BLOQUE DE INICIALIZACIÓN
    init {
        // 🔥 LIMPIEZA NUCLEAR DE PROGRESO (Campaña V2) 🔥
        // Esto borra ABSOLUTAMENTE TODO de las SharedPreferences (estrellas, logros, niveles)
        val migrationKey = "campaign_v2_reset_total"
        if (!prefs.getBoolean(migrationKey, false)) {
            prefs.edit().apply {
                clear() // Borra todo el contenido
                putBoolean(migrationKey, true) // Marcamos que ya se limpió
                apply()
            }
        }

        loadLevelsWithProgress()

        viewModelScope.launch {
            while (true) {
                delay(1000)
                _currentTimeProvider.value = System.currentTimeMillis()
            }
        }

        viewModelScope.launch {
            val unlockedSet = mutableSetOf<String>()
            gameAchievements.all.forEach { achievement ->
                if (prefs.getBoolean("ach_${achievement.id}", false)) {
                    unlockedSet.add(achievement.id)
                }
            }
            _unlockedAchievements.value = unlockedSet
        }

        startNewGame(GameMode.CLASICO)
        playMenuMusic()

        // Activar/desactivar la música desde Ajustes tiene efecto al instante
        viewModelScope.launch {
            com.korkoor.pardos.data.local.SettingsManager(getApplication()).musicEnabled.drop(1).collect { on ->
                if (on) soundManager.playMenuMusic(getApplication()) else soundManager.stopMenuMusic()
            }
        }
    }

    // --- FUNCIONES DE SONIDO PÚBLICAS ---
    fun playMenuMusic() {
        soundManager.playMenuMusic(getApplication())
    }

    fun stopMenuMusic() {
        soundManager.stopMenuMusic()
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }

    // --- FUNCIONES DE APOYO ---

    fun resetGameSession() {
        remoteRole = RemoteRole.NONE
        dailyChallengeThemeIndex = null
        timerJob?.cancel()
        timerManager.stop()
        isMoving = false
        isGameStarted = false
        realStartTime = 0L // Reiniciamos el reloj real
        floatingScores.clear()
        playMenuMusic()
    }

    fun updateAccessibilitySpawnAssist(enabled: Boolean) {
        accessibilitySpawnAssist = enabled
    }

    fun refreshCurrentLevelDifficulty() {
        if (currentMode == GameMode.CLASICO) {
            val currentState = _boardState.value
            val spec = LevelCatalog.spec(currentState.currentLevel)
            if (activeSpec != spec) {
                Log.d("GAME_FIX", "Aplicando las reglas del nivel ${spec.id}")
                startCampaignLevel(spec.id, currentState.score)
            }
        }
    }

    /** Solo para pruebas en depuración: fija un límite de movimientos en la partida actual. */
    fun debugSetMoveLimit(limit: Int) = _boardState.update { it.copy(moveLimit = limit) }

    /** Empieza (o reinicia) un nivel de la campaña con todas sus reglas: tablero, piedras, límites y meta. */
    fun startCampaignLevel(level: Int, initialScore: Int = 0) {
        currentMode = GameMode.CLASICO
        dailyChallengeThemeIndex = null
        currentMultiplierBase = 2
        val spec = LevelCatalog.spec(level)
        setupCustomGame(
            size = spec.boardSize,
            target = spec.goalValue,
            allowPowerUps = true,
            difficulty = "Zen",
            level = spec.id,
            // Una meta de puntos empieza siempre de cero (si no, el puntaje del nivel anterior la regalaría)
            initialScore = if (spec.goal == LevelGoal.SCORE) 0 else initialScore,
            isCustom = false,
            spec = spec
        )
    }

    // 🔥 FIX: Función pública para recargar datos en el menú
    fun loadLevelsWithProgress() {
        _levels.value = levelStore.loadCampaignLevels()
    }

    /**
     * Único reloj del juego. Todos los tiempos del estado están en MILISEGUNDOS.
     * Cuenta atrás si hay maxTime, hacia arriba si no. Respeta bonus de tiempo y
     * evita bucles duplicados (un solo Job).
     */
    fun startLevelTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var last = System.currentTimeMillis()
            while (isActive) {
                delay(250)
                val now = System.currentTimeMillis()
                val delta = now - last
                last = now

                val state = _boardState.value
                if (state.isGameOver || state.isLevelCompleted || state.isPaused) continue

                if (state.maxTime != null) {
                    val next = (state.elapsedTime - delta).coerceAtLeast(0L)
                    _boardState.update { it.copy(elapsedTime = next) }
                    if (next <= 0L) {
                        isGameStarted = false
                        handleGameOver()
                        break
                    }
                } else {
                    _boardState.update { it.copy(elapsedTime = it.elapsedTime + delta) }
                }
            }
        }
    }

    private fun handleGameOver() {
        timerJob?.cancel()
        _boardState.update { it.copy(isGameOver = true) }
        prefs.edit().remove(KEY_SAVED_SCORE).apply()

        // 💀 PIEDAD: Si pierdes, aumentamos el contador de intentos (y se enfría la racha)
        registerLoss()

        // 🔥 INTEGRACIÓN MISIONES DIARIAS: Partida jugada (incluso si se pierde) 🔥
        missionManager.updateProgress(MissionType.PLAY_GAMES, 1)
        lastGameBonus = retention.onGameFinished(won = false, dailyChallenge = dailyChallengeThemeIndex != null)

        if (currentMode == GameMode.CARRERA) finishRace()
        if (currentMode == GameMode.DUELO) finishDuelRound()

        soundManager.playGameOver()
    }

    // ============================ DUELO LOCAL ============================

    /** Empieza un duelo nuevo: el jugador 1 juega primero con una semilla que luego repetirá el jugador 2. */
    fun startDuel() {
        remoteRole = RemoteRole.NONE
        currentMode = GameMode.DUELO
        dailyChallengeThemeIndex = null
        currentMultiplierBase = 2
        duelSeed = System.nanoTime()
        duelPlayer = 1
        duelScores[0] = 0
        duelScores[1] = 0
        duelPhase = DuelPhase.PLAYING
        setupDuelRound()
    }

    private fun setupDuelRound() {
        val rules = com.korkoor.pardos.domain.logic.DuelRules
        setupCustomGame(
            size = rules.BOARD_SIZE,
            target = rules.TARGET,
            allowPowerUps = false,
            difficulty = "Normal",
            level = duelPlayer,           // la cabecera muestra "JUGADOR n"
            initialScore = 0,
            isCustom = false,
            seed = duelSeed,              // misma tabla para los dos
            timeLimitMs = rules.ROUND_MS
        )
        // Sin anuncio de segunda oportunidad ni mano de tutorial en duelo
        _boardState.update { it.copy(secondChanceUsed = true, showTutorialHand = false) }
    }

    fun duelStartSecondPlayer() {
        duelPlayer = 2
        duelPhase = DuelPhase.PLAYING
        setupDuelRound()
    }

    fun duelRematch() = startDuel()

    // ----- Duelo a distancia -----

    private fun beginRemote(role: RemoteRole, seed: Long, challenge: com.korkoor.pardos.domain.logic.RemoteChallenge?) {
        currentMode = GameMode.DUELO
        dailyChallengeThemeIndex = null
        currentMultiplierBase = 2
        remoteRole = role
        remoteChallenge = challenge
        remoteOutcome = null
        remoteReward = null
        remoteFirstTime = true
        duelSeed = seed
        duelPlayer = 1
        duelScores[0] = 0
        duelScores[1] = challenge?.score ?: 0
        duelPhase = DuelPhase.PLAYING
        setupDuelRound()
    }

    /** Lanzo mi propio reto: juego 60 s con una semilla nueva y luego comparto el código. */
    fun startRemoteCreate() =
        beginRemote(RemoteRole.CREATOR, com.korkoor.pardos.domain.logic.RemoteDuel.normalizeSeed(System.nanoTime()), null)

    /** Acepto el reto de otra persona: mismo tablero, mismos 60 s. */
    fun startRemoteAccept(challenge: com.korkoor.pardos.domain.logic.RemoteChallenge) =
        beginRemote(RemoteRole.CHALLENGED, challenge.seed, challenge)

    private fun finishRemoteRound(score: Int) {
        val manager = com.korkoor.pardos.data.local.RemoteDuelManager(getApplication())
        duelScores[0] = score
        when (remoteRole) {
            RemoteRole.CREATOR -> {
                val name = com.korkoor.pardos.data.local.ProfileManager(getApplication()).getProfile().name
                remoteChallenge = com.korkoor.pardos.domain.logic.RemoteChallenge(duelSeed, score, name)
                remoteReward = manager.finishAsCreator()
            }
            RemoteRole.CHALLENGED -> {
                val ch = remoteChallenge
                if (ch != null) {
                    val result = manager.finishAsChallenged(ch, score)
                    remoteOutcome = result.outcome
                    remoteReward = result.reward
                    remoteFirstTime = result.firstTime
                }
            }
            RemoteRole.NONE -> Unit
        }
        duelPhase = DuelPhase.RESULT
    }

    private fun finishDuelRound() {
        if (remoteRole != RemoteRole.NONE) {
            finishRemoteRound(_boardState.value.score)
            return
        }
        duelScores[duelPlayer - 1] = _boardState.value.score
        duelPhase = if (duelPlayer == 1) DuelPhase.HANDOVER else DuelPhase.RESULT
    }

    // =========================== MODO CARRERA ===========================

    /** El jugador rechaza la segunda oportunidad: en Carrera muestra el resultado; en niveles, reinicia. */
    fun declineSecondChance() {
        if (currentMode == GameMode.CARRERA) {
            _boardState.update { it.copy(secondChanceUsed = true) }
        } else {
            retryLevel()
        }
    }

    /** Empieza una carrera desde la etapa 1 con el tiempo inicial. */
    fun startRace() {
        currentMode = GameMode.CARRERA
        dailyChallengeThemeIndex = null
        currentMultiplierBase = 2
        raceStage = 1
        raceStagesCleared = 0
        raceBonusFlash = null
        lastCoinsEarned = 0
        val first = com.korkoor.pardos.domain.logic.RaceRules.stage(1)
        setupCustomGame(
            size = first.boardSize,
            target = first.target,
            allowPowerUps = true,
            difficulty = "Normal",
            level = 1,
            initialScore = 0,
            isCustom = false,
            // El tope (maxTime) es el máximo acumulable; se arranca con el tiempo inicial
            timeLimitMs = com.korkoor.pardos.domain.logic.RaceRules.MAX_TIME_MS
        )
        _boardState.update { it.copy(elapsedTime = com.korkoor.pardos.domain.logic.RaceRules.START_TIME_MS, currentLevel = 1) }
    }

    /** Etapa superada: suma tiempo y pasa a la siguiente SIN detener el reloj. */
    private fun handleRaceStageCleared() {
        val rules = com.korkoor.pardos.domain.logic.RaceRules
        val cleared = raceStage
        raceStagesCleared = cleared
        raceStage = cleared + 1
        val next = rules.stage(raceStage)

        val remaining = _boardState.value.elapsedTime
        val newTime = rules.timeAfterStage(remaining, cleared)
        val gainedSec = ((newTime - remaining) / 1000L).toInt()

        soundManager.playWin()
        missionManager.updateProgress(MissionType.WIN_LEVELS, 1)

        isMoving = false
        floatingScores.clear()
        gameEngine = GameEngine(boardSize = next.boardSize, random = rng)
        _boardState.update {
            it.copy(
                currentLevel = raceStage,
                levelLimit = next.target,
                boardSize = next.boardSize,
                tiles = emptyList(),
                moveCount = 0,
                elapsedTime = newTime,
                isLevelCompleted = false,
                isGameOver = false,
                combo = 0
            )
        }
        spawnInitialTiles(raceStage, next.target)

        raceBonusFlash = gainedSec
        viewModelScope.launch {
            delay(1600)
            raceBonusFlash = null
        }
    }

    /** Fin de la carrera: guarda el récord y entrega monedas según las etapas superadas. */
    private fun finishRace() {
        val stages = raceStagesCleared
        val base = com.korkoor.pardos.domain.logic.RaceRules.coinsFor(stages)
        lastCoinsEarned = com.korkoor.pardos.domain.events.EventCalendar.apply(
            base,
            com.korkoor.pardos.domain.collection.AlbumBonus.combine(
                com.korkoor.pardos.domain.events.EventCalendar.coinMultiplier(com.korkoor.pardos.data.local.LocalDay.today()),
                collection.owned.value, collection.foil.value
            )
        )
        lastCoinsEarned = economy.applyWinBonuses(lastCoinsEarned)
        economy.addCoins(lastCoinsEarned)
        if (stages > 0) {
            viewModelScope.launch {
                try {
                    recordDao.insertRecord(
                        Record(score = stages, level = stages, mode = "CARRERA", date = System.currentTimeMillis())
                    )
                } catch (e: Exception) {
                    Log.e("DATABASE_ERROR", "Error al guardar récord de carrera", e)
                }
            }
        }
    }

    fun startNewGame(mode: GameMode) {
        if (mode == GameMode.CARRERA) {
            startRace()
            return
        }
        if (mode == GameMode.DUELO) {
            startDuel()
            return
        }
        currentMode = mode

        if (mode != GameMode.DESAFIO) {
            dailyChallengeThemeIndex = null
        }

        if (mode == GameMode.TABLAS) {
            val tablesLevel = prefs.getInt(KEY_TABLES_LEVEL, 1)
            currentMultiplierBase = (3..9).random()

            val logicMultiplier = if (tablesLevel <= 2) 8 else if (tablesLevel <= 4) 16 else 32
            val targetForTables = currentMultiplierBase * logicMultiplier

            setupCustomGame(
                size = 4,
                target = targetForTables,
                allowPowerUps = true,
                difficulty = "Zen", // 🔥 Forzado a Zen
                level = tablesLevel,
                initialScore = 0,
                isCustom = false // 🔥 Aseguramos que no es custom
            )
        } else {
            currentMultiplierBase = 2

            val levelToStart = if (mode == GameMode.CLASICO) {
                prefs.getInt(KEY_LAST_UNLOCKED, 1)
            } else {
                1
            }

            val savedScore = if (mode == GameMode.CLASICO) prefs.getInt(KEY_SAVED_SCORE, 0) else 0

            if (mode == GameMode.CLASICO) {
                startCampaignLevel(levelToStart, savedScore)
                return
            }

            val correctTarget = ProgressionEngine.calculateTargetForLevel(levelToStart)
            val correctSize = ProgressionEngine.calculateBoardSize(correctTarget)

            // 🔥 REGLA DE ORO: Si es CLASICO, la dificultad es SIEMPRE Zen
            val forcedDifficulty = if (mode == GameMode.CLASICO) "Zen" else if (mode == GameMode.DESAFIO || mode == GameMode.RAPIDO) "Normal" else "Zen"

            setupCustomGame(
                size = correctSize,
                target = correctTarget,
                allowPowerUps = true,
                difficulty = forcedDifficulty,
                level = levelToStart,
                initialScore = savedScore,
                isCustom = false // 🔥 Importante para resetear determinedMode
            )
        }
    }

    fun setupCustomGame(
        size: Int,
        target: Int,
        allowPowerUps: Boolean = true,
        difficulty: String = "Zen",
        level: Int = 1,
        initialScore: Int = 0,
        isCustom: Boolean = false,
        seed: Long? = null,
        timeLimitMs: Long? = null,
        spec: LevelSpec? = null
    ) {
        // 1. LIMPIEZA TOTAL DE ESTADOS PREVIOS
        timerJob?.cancel()
        timerManager.stop()
        isMoving = false
        isGameStarted = false
        realStartTime = 0L
        comboJob?.cancel()
        floatingScores.clear()
        _comboCount.value = 0
        showLevelSummary = false

        // 2. INICIALIZACIÓN DEL MOTOR
        lastSnapshot = null
        canUndo = false
        currentSeed = seed
        rng = seed?.let { Random(it) } ?: Random.Default
        activeSpec = spec
        gameEngine = GameEngine(boardSize = size, random = rng, blocked = spec?.stoneSet ?: emptySet())

        // 3. DETERMINACIÓN DEL MODO (FIX: Evita que la campaña herede el modo Desafío)
        val determinedMode = if (isCustom) {
            if (difficulty != "Zen") GameMode.DESAFIO else GameMode.CUSTOM
        } else {
            // Si no es custom, respetamos el modo de campaña actual (CLASICO o TABLAS)
            currentMode
        }

        // 4. CÁLCULO DE TIEMPO (Solo se activa en Desafío o Custom con dificultad)
        val timeLimitSeconds = timeLimitMs ?: when {
            spec?.timeLimitMs != null -> spec.timeLimitMs   // niveles contrarreloj de la campaña
            determinedMode == GameMode.DESAFIO || (isCustom && difficulty != "Zen") ->
                ProgressionEngine.calculateTimeLimitForTarget(target, isCampaign = false)
            else -> null   // la campaña no tiene reloj salvo en los niveles contrarreloj
        }

        this.currentMode = determinedMode

        // 5. SISTEMA DE PIEDAD (PITY MODE)
        val attempts = prefs.getInt("$KEY_ATTEMPTS$level", 0)
        activeAssist = if (spec != null && !isCustom && isCampaignRun()) com.korkoor.pardos.domain.flow.AssistPolicy.forAttempts(attempts)
        else com.korkoor.pardos.domain.flow.AssistPolicy.forAttempts(0)
        isPityModeActive = activeAssist.tier > 0
        val assistFactor = activeAssist.limitFactor
        // Deshacer gratis: solo los que faltan por dar en este nivel (nunca dos veces)
        val grantedKey = "assist_granted_$level"
        val extraUndos = com.korkoor.pardos.domain.flow.AssistPolicy.newUndos(prefs.getInt(grantedKey, 0), activeAssist)
        if (extraUndos > 0) {
            economy.addUndos(extraUndos)
            prefs.edit().putInt(grantedKey, activeAssist.totalFreeUndos).apply()
        }
        assistMessage = activeAssist.message
        nearMissMessage = null
        flowStreak = 0
        flowCallout = null
        peakFlowTier = com.korkoor.pardos.domain.flow.FlowTier.CALM
        lastStreakMilestone = null
        lastStreakBonusPct = 0
        lastFlowBonusPct = 0

        // 6. ACTUALIZACIÓN DEL ESTADO DEL TABLERO
        _boardState.update {
            it.copy(
                currentLevel = level,
                levelLimit = target,
                boardSize = size,
                score = initialScore,
                gameMode = determinedMode,
                allowPowerUps = allowPowerUps,
                isGameOver = false,
                isLevelCompleted = false,
                starsEarned = 0,
                tiles = emptyList(), // Se llenarán en spawnInitialTiles
                maxTime = timeLimitSeconds?.let { if (spec?.timeLimitMs != null) (it * assistFactor).toLong() else it },
                // Si no hay tiempo límite, el tiempo transcurrido debe ser 0 para no contar
                elapsedTime = timeLimitSeconds?.let { if (spec?.timeLimitMs != null) (it * assistFactor).toLong() else it } ?: 0L,
                showTutorialHand = (level == 1 && initialScore == 0),
                secondChanceUsed = false,
                moveCount = 0,
                goal = spec?.goal ?: LevelGoal.REACH_TILE,
                goalCount = spec?.goalCount ?: 1,
                blocked = spec?.stones.orEmpty(),
                goalStats = com.korkoor.pardos.domain.level.GoalStats(),
                twist = spec?.twist ?: com.korkoor.pardos.domain.level.Twist.NONE,
                storm = spec?.storm,
                stormStones = emptyList(),
                moveLimit = spec?.moveLimit?.let { (it * assistFactor).toInt() },
                levelKind = spec?.kind,
                levelTitle = spec?.title,
                levelTip = spec?.tip,
                outOfMoves = false,
                merges = 0
            )
        }

        // 7. GENERACIÓN DE FICHAS INICIALES
        spawnInitialTiles(level, spec?.scaleTile ?: target)
    }

    fun onMove(direction: Direction, onHapticFeedback: (HapticFeedbackType) -> Unit) {
        val state = _boardState.value

        // ⏳ GESTIÓN DE TIEMPO
        if (!isGameStarted) {
            isGameStarted = true
            realStartTime = System.currentTimeMillis()
            startLevelTimer()
        }

        if (state.showTutorialHand) {
            _boardState.update { it.copy(showTutorialHand = false) }
        }

        if (isMoving || state.isLevelCompleted || state.isGameOver) return

        viewModelScope.launch {
            val currentState = _boardState.value
            val currentTiles = currentState.tiles

            // 🛠️ ELIMINADO MULTIPLICADOR: Ahora la fusión es estándar (x1)
            // Niveles "del revés": el deslizamiento del dedo se traduce a la dirección real del tablero
            val (movedTiles, scoreGained) = gameEngine.move(currentTiles, currentState.twist.apply(direction), 1)

            if (hasBoardChanged(currentTiles, movedTiles)) {
                isMoving = true

                // Animaciones y Sonido
                val mergesCount = (currentTiles.size - movedTiles.size).coerceAtLeast(0)
                if (mergesCount > 0) {
                    onHapticFeedback(HapticFeedbackType.LongPress)
                    registerMerge()
                    soundManager.playBetterPop(combo = _comboCount.value)

                    // 🔥 INTEGRACIÓN MISIONES: Contabiliza los pares combinados
                    missionManager.updateProgress(MissionType.MERGE_PAIRS, mergesCount)
                    retention.onMerges(mergesCount)
                }

                // FLOW: las jugadas seguidas que fusionan encienden el aura del tablero
                val newFlow = com.korkoor.pardos.domain.flow.FlowMeter.next(flowStreak, mergesCount > 0)
                com.korkoor.pardos.domain.flow.FlowMeter.tierUp(flowStreak, newFlow)?.let { tier ->
                    flowCallout = FlowCallout(tier, ++calloutSeq)
                    onHapticFeedback(HapticFeedbackType.LongPress)
                    soundManager.playBetterPop(combo = 8 + tier.ordinal * 2)
                    val id = calloutSeq
                    viewModelScope.launch { delay(1300); if (flowCallout?.id == id) flowCallout = null }
                }
                flowStreak = newFlow
                com.korkoor.pardos.domain.flow.FlowMeter.tierOf(newFlow).let { if (it.ordinal > peakFlowTier.ordinal) peakFlowTier = it }
                if (assistMessage != null) assistMessage = null

                delay(80)

                // 🎲 GENERACIÓN INTELIGENTE (Aparición normal)
                val finalTiles = movedTiles.toMutableList()
                val newValue = pickNewTileValue(currentState.levelLimit)
                gameEngine.spawnTileWithSpecificValue(movedTiles, newValue, 1)?.let {
                    finalTiles.add(it)
                }

                // ✨ EVOLUCIÓN ESPONTÁNEA (Solo 4, 8, 16 de vez en cuando)
                // Probabilidad del 15% para que ocurra
                if (rng.nextInt(1, 101) <= 15 + activeAssist.luckyBoostPct) {
                    val luckyCandidates = finalTiles.filter { it.value == 4 || it.value == 8 || it.value == 16 }
                    if (luckyCandidates.isNotEmpty()) {
                        val luckyTile = luckyCandidates.random(rng)
                        val index = finalTiles.indexOf(luckyTile)
                        if (index != -1) {
                            val evolvedValue = luckyTile.value * 2
                            finalTiles[index] = luckyTile.copy(value = evolvedValue)

                            // Feedback visual y sonoro de la evolución
                            soundManager.playBetterPop(combo = 5)
                            addFloatingScore(evolvedValue, luckyTile.col, luckyTile.row)
                        }
                    }
                }

                val maxTileValue = finalTiles.maxOfOrNull { it.value } ?: 0

                // 🔥 INTEGRACIÓN MISIONES: Actualiza el bloque de mayor valor conseguido
                if (maxTileValue > 0) {
                    missionManager.updateProgress(MissionType.REACH_BLOCK, maxTileValue)
                    retention.onTileReached(maxTileValue)
                }

                val newScore = currentState.score + scoreGained
                val newStats = currentState.goalStats.after(mergesCount, if (currentState.goal == LevelGoal.COMBO) currentState.levelLimit else 0)
                val reachedTarget = LevelRules.isGoalReached(
                    currentState.goal, currentState.levelLimit, currentState.goalCount, finalTiles, newScore, newStats
                )

                // Instantánea para "Deshacer" (solo guardamos la última jugada)
                lastSnapshot = UndoSnapshot(
                    currentState.tiles, currentState.score, currentState.moveCount,
                    currentState.goalStats, currentState.stormStones, currentState.blocked
                )
                canUndo = true

                // Tormenta: tras cada jugada pueden irse piedras temporales y caer otra nueva
                var stormStones = currentState.stormStones
                var blockedNow = currentState.blocked
                currentState.storm?.let { storm ->
                    if (!reachedTarget) {
                        stormStones = com.korkoor.pardos.domain.level.StormRules.step(
                            storm, currentState.moveCount + 1, stormStones, finalTiles,
                            activeSpec?.stoneSet ?: emptySet(), currentState.boardSize, rng
                        )
                        blockedNow = (activeSpec?.stones.orEmpty() + stormStones.map { it.cell }).distinct()
                        if (blockedNow != currentState.blocked) {
                            gameEngine = GameEngine(boardSize = currentState.boardSize, random = rng, blocked = blockedNow.toSet())
                        }
                    }
                }

                _boardState.update {
                    it.copy(
                        tiles = finalTiles, score = newScore, moveCount = it.moveCount + 1, merges = it.merges + mergesCount,
                        goalStats = newStats, stormStones = stormStones, blocked = blockedNow
                    )
                }

                if (currentState.gameMode == GameMode.CLASICO) {
                    prefs.edit().putInt(KEY_SAVED_SCORE, newScore).apply()
                }

                // 🏆 REGLA DE ORO: Si ya ganaste, paramos TODO aquí para evitar el crash
                if (reachedTarget) {
                    isMoving = false
                    isGameStarted = false // Detener el hilo del tiempo
                    handleLevelVictory(maxTileValue)
                    return@launch
                } else if (currentState.moveLimit?.let { currentState.moveCount + 1 >= it } == true) {
                    // Se acabaron los movimientos sin llegar a la meta
                    _boardState.update { it.copy(outOfMoves = true) }
                    handleGameOver()
                    isMoving = false
                    return@launch
                } else if (gameEngine.isGameOver(finalTiles)) {
                    handleGameOver()
                    isMoving = false
                    return@launch
                }

                isMoving = false

                // ✨ AYUDA DIVINA (Mantenida exactamente igual)
                if (ProgressionEngine.shouldTriggerDivineHelp(activeSpec?.scaleTile ?: currentState.levelLimit, rng)) {
                    delay(150) // Pausa dramática
                    _boardState.update { current ->
                        val tiles = current.tiles.toMutableList()

                        // Solo fichas <= 25% de la meta (Balance justo)
                        val limitThreshold = (current.levelLimit * 0.25).toInt()
                        val candidates = tiles.filter { ProgressionEngine.isValueEligibleForDivineHelp(it.value) }

                        if (candidates.isNotEmpty()) {
                            val luckyTile = candidates.random(rng)
                            val index = tiles.indexOf(luckyTile)
                            if (index != -1) {
                                val newVal = luckyTile.value * 2
                                tiles[index] = luckyTile.copy(value = newVal)

                                soundManager.playBetterPop(combo = 10)
                                addFloatingScore(newVal, luckyTile.col, luckyTile.row)

                                // 🔥 INTEGRACIÓN MISIONES: Si la ayuda divina crea un bloque alto, lo registramos
                                missionManager.updateProgress(MissionType.REACH_BLOCK, newVal)
                            }
                        }
                        current.copy(tiles = tiles)
                    }
                }
            }
        }
    }

    private fun stopTimer() {
        isGameStarted = false
    }

    fun addFloatingScore(value: Int, col: Int, row: Int) {
        floatingScores.add(FloatingScoreModel(value = value, col = col, row = row))
    }

    fun removeFloatingScore(id: String) {
        floatingScores.removeIf { it.id == id }
    }

    fun useSecondChance() {
        val currentBoard = _boardState.value.tiles
        val threshold = currentMultiplierBase * 2
        val filteredTiles = currentBoard.filter { it.value > threshold }

        _boardState.value = _boardState.value.copy(
            tiles = filteredTiles,
            isGameOver = false
        )
        // 🔊 Al revivir
        playMenuMusic()
    }

    fun activateSelectMode(type: String) {
        isSelectModeActive = true
        pendingPowerUpType = type
        firstSelectedTileId = null
    }

    fun cancelSelectMode() {
        isSelectModeActive = false
        pendingPowerUpType = null
        firstSelectedTileId = null
    }

    fun handleTileClick(tileId: String) {
        if (!isSelectModeActive) return

        when (pendingPowerUpType) {
            "SINGLE_CLEAN" -> {
                _boardState.update { state ->
                    val updatedTiles = state.tiles.filter { it.id != tileId }
                    // Registramos que el tablero cambió para efectos visuales
                    state.copy(tiles = updatedTiles)
                }
                soundManager.playBetterPop(combo = 5) // Sonido de limpieza
                cancelSelectMode()
            }

            "MANUAL_MERGE" -> {
                if (firstSelectedTileId == null) {
                    // Primer paso: Seleccionamos la ficha y le damos feedback al usuario
                    firstSelectedTileId = tileId
                    // Podrías disparar una vibración ligera aquí
                } else {
                    val firstId = firstSelectedTileId!!
                    if (firstId == tileId) {
                        cancelSelectMode() // Si toca la misma, cancelamos
                        return
                    }

                    _boardState.update { state ->
                        val tiles = state.tiles.toMutableList()
                        val t1 = tiles.find { it.id == firstId }
                        val t2 = tiles.find { it.id == tileId }

                        // Verificamos que ambas existan y tengan el mismo valor
                        if (t1 != null && t2 != null && t1.value == t2.value) {
                            val newValue = t1.value * 2

                            // Efecto de fusión: eliminamos la primera y duplicamos la segunda
                            tiles.remove(t1)
                            val indexT2 = tiles.indexOf(t2)
                            if (indexT2 != -1) {
                                tiles[indexT2] = t2.copy(value = newValue)

                                // Añadimos puntuación flotante en la posición de la fusión
                                addFloatingScore(newValue, t2.col, t2.row)
                                soundManager.playBetterPop(combo = 10)
                            }

                            state.copy(tiles = tiles, score = state.score + newValue)
                        } else {
                            // Si no son iguales, no hacemos nada (o podrías sonar un error)
                            state
                        }
                    }
                    cancelSelectMode()
                }
            }
        }
    }

    private fun handleLevelVictory(maxTile: Int) {
        val currentState = _boardState.value
        if (currentMode == GameMode.CARRERA) {
            if (maxTile >= currentState.levelLimit) handleRaceStageCleared()
            return
        }
        val targetReached = LevelRules.isGoalReached(
            currentState.goal, currentState.levelLimit, currentState.goalCount, currentState.tiles, currentState.score, currentState.goalStats
        )
        if (!targetReached) return

        // 1. Detenemos los relojes inmediatamente
        timerJob?.cancel()
        timerManager.stop()
        stopTimer()

        // 🏆 VICTORIA: Limpiamos intentos fallidos
        val level = currentState.currentLevel
        prefs.edit().remove("$KEY_ATTEMPTS$level").remove("assist_granted_$level").apply()

        var finalStars = 0
        var finalTimeUsed = 0L
        var stateForAchievements: BoardState? = null

        _boardState.update { state ->
            val totalLimit = state.maxTime ?: 0L

            if (totalLimit > 0) {
                // MODO DESAFÍO: El tiempo usado es el límite total menos lo que sobró
                finalTimeUsed = (totalLimit - state.elapsedTime).coerceAtLeast(0L)
                finalStars = activeSpec?.let { LevelRules.stars(it, state.moveCount, finalTimeUsed) }
                    ?: ProgressionEngine.calculateStars(finalTimeUsed, state.levelLimit)
            } else {
                // 🔥 MODO CAMPAÑA CORREGIDO:
                // Usamos directamente el elapsedTime del estado, que ya lleva
                // la cuenta exacta de los milisegundos jugados.
                finalTimeUsed = state.elapsedTime
                // En la campaña las estrellas premian la eficiencia (menos movimientos); en el resto, 3 como siempre
                finalStars = activeSpec?.let { LevelRules.stars(it, state.moveCount, finalTimeUsed) } ?: 3
            }

            val assuredStars = finalStars.coerceAtLeast(1)

            val newState = state.copy(
                isLevelCompleted = true,
                starsEarned = assuredStars,
                isGameOver = false,
                elapsedTime = finalTimeUsed // Le pasamos el tiempo final real a la UI
            )

            stateForAchievements = newState
            newState
        }

        if (_boardState.value.starsEarned > 0) {
            val currentLvl = _boardState.value.currentLevel

            // 🪙 Monedas: se calculan ANTES de guardar el progreso (para saber si es primera vez)
            val firstClear = levelStore.starsFor(currentMode, currentLvl) == 0
            val rawCoins = com.korkoor.pardos.domain.rewards.CoinRewards.forLevelWin(
                _boardState.value.starsEarned, firstClear
            ) + (activeSpec?.let { LevelRules.coinBonus(it) } ?: 0)
            // FLOW: la racha de victorias y el mejor ímpetu del nivel suben las monedas (solo campaña)
            var streakPct = 0
            var flowPct = 0
            lastStreakMilestone = null
            if (isCampaignRun()) {
                winStreak = com.korkoor.pardos.domain.flow.WinStreak.afterWin(winStreak)
                prefs.edit().putInt(KEY_WIN_STREAK, winStreak).apply()
                streakPct = com.korkoor.pardos.domain.flow.WinStreak.coinBonusPct(winStreak)
                flowPct = com.korkoor.pardos.domain.flow.FlowMeter.coinBonusPct(peakFlowTier)
                com.korkoor.pardos.domain.flow.WinStreak.milestoneAt(winStreak)?.let { m ->
                    economy.addGems(m.gems)
                    if (m.undos > 0) economy.addUndos(m.undos)
                    lastStreakMilestone = m
                }
            }
            lastStreakBonusPct = streakPct
            lastFlowBonusPct = flowPct
            val baseCoins = rawCoins * (100 + streakPct + flowPct) / 100
            // Eventos programados (fin de semana dorado, semana festival...)
            lastCoinsEarned = com.korkoor.pardos.domain.events.EventCalendar.apply(
                baseCoins,
                com.korkoor.pardos.domain.collection.AlbumBonus.combine(
                com.korkoor.pardos.domain.events.EventCalendar.coinMultiplier(com.korkoor.pardos.data.local.LocalDay.today()),
                collection.owned.value, collection.foil.value
            )
            )
            lastCoinsEarned = economy.applyWinBonuses(lastCoinsEarned)
            economy.addCoins(lastCoinsEarned)
            coinsDoubled = false

            // Retención: pase de temporada, semanales, hucha y bonus de la primera victoria del día
            val isDaily = dailyChallengeThemeIndex != null
            lastGameBonus = retention.onGameFinished(won = true, dailyChallenge = isDaily)
            retention.onStars(_boardState.value.starsEarned)
            if (isDaily) retention.onDailyChallengeCompleted()

            saveLevelProgress(
                level = currentLvl,
                stars = _boardState.value.starsEarned,
                finalTime = finalTimeUsed,
                finalMoves = _boardState.value.moveCount
            )

            stateForAchievements?.let { checkAchievements(it) }
            saveRecord()
            prefs.edit().remove(KEY_SAVED_SCORE).apply()

            // --- CÓDIGO PARA PERFIL Y NUBE ---
            val profileManager = com.korkoor.pardos.data.local.ProfileManager(getApplication())
            profileManager.addXpForLevelVictory(_boardState.value.starsEarned)

            // PERSISTENCIA DE CAMPAÑA
            profileManager.updateCampaignLevel(currentLvl + 1)

            // --- MISIONES DIARIAS ---
            val finalTimeSecs = (finalTimeUsed / 1000).toInt()
            missionManager.updateProgress(MissionType.PLAY_GAMES, 1)
            missionManager.updateProgress(MissionType.WIN_LEVELS, 1)
            missionManager.updateProgress(MissionType.EARN_STARS, _boardState.value.starsEarned)
            if (finalTimeSecs > 0) {
                missionManager.updateProgress(MissionType.WIN_UNDER_TIME, finalTimeSecs)
            }

            soundManager.playWin()
        }

        // --- LÓGICA DE REDIRECCIÓN ---
        viewModelScope.launch {
            delay(800) // Pausa dramática para que se vea la última ficha fusionarse

            val currentLvl = _boardState.value.currentLevel

            // 1. Leemos la bandera local
            var isProfileSetup = prefs.getBoolean("is_profile_setup_complete", false)

            // 2. 🔥 ESCUDO ANTI-VETERANOS: Si la bandera dice "false", verificamos si recuperó datos
            if (!isProfileSetup) {
                val profileManager = com.korkoor.pardos.data.local.ProfileManager(getApplication())
                val profile = profileManager.getProfile()

                // Si ya se cambió el nombre o si su nivel de campaña es mayor a 2, ya había configurado el perfil
                if (profile.name != "Jugador Zen" || profile.currentCampaignLevel > 2) {
                    isProfileSetup = true
                    // Reparamos la bandera local silenciosamente
                    prefs.edit().putBoolean("is_profile_setup_complete", true).apply()
                }
            }

            // 3. Decidimos a dónde enviarlo
            if (currentLvl >= 2 && !isProfileSetup) {
                showProfileSetupRedirect = true
            } else {
                showLevelSummary = true
            }
        }
    }

    fun onProfileSetupCompleted() {
        // 1. Marcamos que ya nunca más se le debe pedir esto
        prefs.edit().putBoolean("is_profile_setup_complete", true).apply()

        // 2. Cerramos la pantalla de perfil
        showProfileSetupRedirect = false

        // 3. ¡Le mostramos las estrellas de su victoria que quedaron pendientes!
        showLevelSummary = true
    }

    private fun applyComboTimeBonus(combo: Int) {
        val bonusSeconds = when {
            combo >= 4 -> 10L
            combo >= 3 -> 7L
            combo >= 2 -> 4L
            else -> 0L
        }

        if (bonusSeconds > 0) {
            _boardState.update { state ->
                val limit = state.maxTime
                if (limit != null) {
                    val newTime = (state.elapsedTime + bonusSeconds * 1000L).coerceAtMost(limit)
                    state.copy(elapsedTime = newTime)
                } else {
                    state
                }
            }
        }
    }

    @SuppressLint("UseKtx")
    private fun checkGameState(tiles: List<TileModel>) {
        if (_boardState.value.isLevelCompleted) return

        if (gameEngine.isGameOver(tiles)) {
            timerManager.stop()
            stopTimer()
            _boardState.update { it.copy(isGameOver = true) }
            registerLoss()

            soundManager.playGameOver()
        }
    }

    private fun saveLevelProgress(level: Int, stars: Int, finalTime: Long, finalMoves: Int) {
        levelStore.recordResult(currentMode, level, stars, finalTime, finalMoves)
        if (currentMode == GameMode.CLASICO) loadLevelsWithProgress()
    }

    fun getBestStats(level: Int): Pair<Int, Long> = levelStore.bestStats(currentMode, level)

    fun retryLevel() {
        if (currentMode == GameMode.DUELO) {
            isMoving = false
            isGameStarted = false
            timerJob?.cancel()
            if (remoteRole != RemoteRole.NONE) {
                // mismo tablero otra vez (el premio de un código solo se cobra la primera vez)
                duelPhase = DuelPhase.PLAYING
                remoteOutcome = null
                remoteReward = null
                setupDuelRound()
            } else {
                startDuel()
            }
            return
        }
        if (currentMode == GameMode.CARRERA) {
            showLevelSummary = false
            isMoving = false
            isGameStarted = false
            timerJob?.cancel()
            startRace()
            playMenuMusic()
            return
        }
        val levelToRetry = _boardState.value.currentLevel
        val arePowerUpsAllowed = _boardState.value.allowPowerUps

        if (currentMode == GameMode.CLASICO) {
            showLevelSummary = false
            isMoving = false
            isGameStarted = false
            timerJob?.cancel()
            floatingScores.clear()
            prefs.edit().remove(KEY_SAVED_SCORE).apply()
            startCampaignLevel(levelToRetry)
            playMenuMusic()
            return
        }

        val target = ProgressionEngine.calculateTargetForLevel(levelToRetry)
        val size = ProgressionEngine.calculateBoardSize(target)

        showLevelSummary = false
        isMoving = false
        isGameStarted = false
        timerJob?.cancel()
        floatingScores.clear()

        prefs.edit().remove(KEY_SAVED_SCORE).apply()

        setupCustomGame(
            size = size,
            target = target,
            allowPowerUps = arePowerUpsAllowed,
            difficulty = if (currentMode == GameMode.DESAFIO) "Normal" else "Zen",
            level = levelToRetry,
            initialScore = 0
        )
        playMenuMusic()
    }

    private fun checkAchievements(manualState: BoardState? = null) {
        val currentState = manualState ?: _boardState.value
        if (currentState.tiles.isEmpty() && currentState.score == 0) return

        gameAchievements.all.forEach { achievement ->
            val key = "ach_${achievement.id}"
            if (!prefs.getBoolean(key, false) && achievement.condition(currentState)) {
                prefs.edit().putBoolean(key, true).apply()
                // 💰 El logro paga monedas (y gemas/cofre según su rareza)
                rewardsManager.payAchievement(achievement.id)
                viewModelScope.launch {
                    _unlockedAchievements.update { it + achievement.id }
                    activeAchievementPopup = achievement
                    delay(4000)
                    activeAchievementPopup = null
                }
            }
        }
    }

    fun nextLevel() {
        val currentState = _boardState.value
        val nextLv = currentState.currentLevel + 1

        viewModelScope.launch {
            showLevelSummary = false
            floatingScores.clear()
            playMenuMusic()

            if (currentMode == GameMode.TABLAS) {
                prefs.edit().putInt(KEY_TABLES_LEVEL, nextLv).apply()
                delay(300)
                startNewGame(GameMode.TABLAS)
                return@launch
            }

            if (currentMode == GameMode.CLASICO) {
                prefs.edit().remove(KEY_SAVED_SCORE).apply()
                delay(300)
                startCampaignLevel(nextLv, currentState.score)
                return@launch
            }

            val newTarget = currentState.levelLimit * 2
            val newSize = currentState.boardSize

            delay(300)
            setupCustomGame(
                size = newSize,
                target = newTarget,
                level = nextLv,
                initialScore = 0,
                isCustom = true
            )
        }
    }

    private fun saveRecord() {
        val currentState = _boardState.value
        viewModelScope.launch {
            try {
                val modeNameForDb = when (currentMode) {
                    GameMode.TABLAS -> "$currentMultiplierBase"
                    else -> currentMode.name
                }
                val newRecord = Record(
                    score = currentState.score,
                    level = currentState.currentLevel,
                    mode = modeNameForDb,
                    date = System.currentTimeMillis()
                )
                recordDao.insertRecord(newRecord)
            } catch (e: Exception) {
                Log.e("DATABASE_ERROR", "Error al guardar récord", e)
            }
        }
    }

    private fun spawnInitialTiles(level: Int, target: Int) {
        val currentTiles = mutableListOf<TileModel>()
        val boardSize = _boardState.value.boardSize

        // Niveles con ventaja: salen con fichas altas ya colocadas
        activeSpec?.takeIf { it.startTiles.isNotEmpty() }?.let { spec ->
            val placed = spec.startTiles.map { TileModel(TileModel.generateId(), it.value, it.row, it.col, isNew = true) }
            _boardState.update { it.copy(tiles = placed) }
            return
        }

        // 🚀 LÓGICA DE CANTIDAD: Más fichas para tableros más grandes
        // 3x3 -> 2 fichas | 4x4 -> 3 fichas | 5x5 y 6x6 -> 4 fichas
        val initialTilesCount = when {
            boardSize >= 5 -> 4
            boardSize == 4 -> 3
            else -> 2
        }

        repeat(initialTilesCount) {
            // 🎲 Obtenemos valores inteligentes según el target del nivel
            val newValue = pickNewTileValue(target)

            // Spawneamos la ficha evitando posiciones ocupadas por las anteriores
            val newTile = gameEngine.spawnTileWithSpecificValue(
                currentTiles,
                newValue,
                currentMultiplierBase
            )

            newTile?.let { currentTiles.add(it) }
        }

        // Actualizamos el estado con la lista completa de fichas iniciales
        _boardState.update { it.copy(tiles = currentTiles.toList()) }
    }

    fun onLevelCompleted() { checkAchievements() }

    private val _ticker = MutableStateFlow(System.currentTimeMillis())
    init {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                _ticker.value = System.currentTimeMillis()
            }
        }
    }

    fun getRemainingTime(lastUseTime: Long, now: Long): String {
        if (lastUseTime == 0L) return ""
        val elapsed = now - lastUseTime
        val remaining = COOLDOWN_MS - elapsed
        if (remaining <= 0) return ""
        val minutes = (remaining / 1000) / 60
        val seconds = (remaining / 1000) % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    /** Cuánto falta de la recarga, de 1 (recién usado) a 0 (listo). Para el anillo del botón. */
    fun cooldownFraction(lastUseTime: Long, now: Long): Float {
        if (lastUseTime == 0L) return 0f
        return ((COOLDOWN_MS - (now - lastUseTime)).toFloat() / COOLDOWN_MS).coerceIn(0f, 1f)
    }

    fun isPowerUpAvailable(lastUseTime: Long, now: Long): Boolean {
        if (lastUseTime == 0L) return true
        val elapsed = now - lastUseTime
        return elapsed >= COOLDOWN_MS
    }

    fun resetPowerUpCooldown(type: String) {
        if (type == "CLEAN") lastCleanTime = 0L
        if (type == "MERGE") lastMergeTime = 0L
    }

    fun useCleanPowerUp() {
        val currentTiles = _boardState.value.tiles
        if (currentTiles.isEmpty()) return
        val topTiles = currentTiles.sortedByDescending { it.value }.take(3)
        _boardState.update { it.copy(tiles = topTiles) }
        lastCleanTime = System.currentTimeMillis()
    }

    fun useMergePowerUp() {
        val currentTiles = _boardState.value.tiles
        val pair = currentTiles.groupBy { it.value }.values.firstOrNull { it.size >= 2 }
        pair?.let {
            executeManualMerge(it[0], it[1])
            lastMergeTime = System.currentTimeMillis()
        }
    }

    private fun executeManualMerge(first: TileModel, second: TileModel) {
        _boardState.update { state ->
            val list = state.tiles.toMutableList()
            val t1 = list.find { it.id == first.id }
            val t2 = list.find { it.id == second.id }
            if (t1 != null && t2 != null) {
                val newValue = t2.value * 2
                list.remove(t1)
                list.remove(t2)
                list.add(t2.copy(value = newValue))
                state.copy(tiles = list, score = state.score + newValue)
            } else state
        }
        checkGameState(_boardState.value.tiles)
    }

    private var comboJob: Job? = null
    fun registerMerge() {
        comboJob?.cancel()
        _comboCount.value += 1
        comboJob = viewModelScope.launch {
            delay(1000) // FIX: El combo dura 1 segundo
            _comboCount.value = 0 // FIX: Se reinicia a 0
        }
    }

    /** Ya se duplicaron las monedas de esta victoria (una vez por partida). */
    var coinsDoubled by mutableStateOf(false)
        private set

    /** Tras ver el anuncio: suma el bono (tope definido en Economy) y lo refleja en el resumen. */
    fun grantDoubleCoins() {
        if (coinsDoubled || lastCoinsEarned <= 0) return
        coinsDoubled = true
        val bonus = rewardsManager.doubleCoins(lastCoinsEarned)
        lastCoinsEarned += bonus
    }

    fun grantAdReward(type: String) {
        viewModelScope.launch {
            isMoving = false
            when (type) {
                "CLEAN" -> resetPowerUpCooldown("CLEAN")
                "MERGE" -> resetPowerUpCooldown("MERGE")
                "REVIVE" -> {
                    val currentState = _boardState.value
                    val currentTiles = currentState.tiles

                    // 🧹 Limpieza: nos quedamos con la mitad de las mejores fichas
                    // Si se acabaron los movimientos el tablero se queda como está y se regalan más movimientos
                    val tilesToKeepCount = (currentTiles.size / 2).coerceAtLeast(2)
                    val cleanedTiles = if (currentState.outOfMoves) currentTiles
                    else currentTiles.sortedByDescending { it.value }.take(tilesToKeepCount)

                    // 🔥 SÚPER BALANCE: Si el tablero estaba vacío, generamos fichas inteligentes
                    val finalTiles = cleanedTiles.ifEmpty {
                        val v1 = pickNewTileValue(currentState.levelLimit)
                        val v2 = pickNewTileValue(currentState.levelLimit)

                        val t1 = gameEngine.spawnTileWithSpecificValue(emptyList(), v1, currentMultiplierBase)
                        val t2 = gameEngine.spawnTileWithSpecificValue(listOfNotNull(t1), v2, currentMultiplierBase)
                        listOfNotNull(t1, t2)
                    }

                    _boardState.update { state ->
                        // ⏳ BALANCE DE TIEMPO: +30 segundos al revivir (el reloj trabaja en milisegundos)
                        val bonusTimeSec = 30_000L
                        val newTime = if (state.maxTime != null) {
                            (state.elapsedTime + bonusTimeSec).coerceAtMost(state.maxTime!!)
                        } else {
                            state.elapsedTime
                        }

                        state.copy(
                            tiles = finalTiles,
                            isGameOver = false,
                            secondChanceUsed = true,
                            showTutorialHand = true,
                            elapsedTime = newTime,
                            moveLimit = if (state.outOfMoves) state.moveLimit?.plus(((state.moveLimit ?: 0) * 15 / 100).coerceAtLeast(8)) else state.moveLimit,
                            outOfMoves = false
                        )
                    }
                    isGameStarted = true
                    startLevelTimer()
                }
            }
        }
    }

    fun setupDailyChallenge() {
        // Misma configuración Y mismas fichas para todos los jugadores del mismo día
        val daily = com.korkoor.pardos.domain.logic.DailyChallenge.forDay(com.korkoor.pardos.data.local.LocalDay.today())
        dailyChallengeThemeIndex = daily.themeIndex
        currentMode = GameMode.DESAFIO
        setupCustomGame(
            size = daily.boardSize,
            target = daily.spec.goalValue,
            allowPowerUps = false,
            difficulty = "Normal",
            level = 1,
            isCustom = true,
            seed = daily.seed,
            spec = daily.spec
        )
    }

    private fun pickNewTileValue(target: Int): Int {
        val spec = activeSpec
        // En los niveles de campaña la escala (y el reparto de 2/4/8) la da el nivel; si no, la meta
        val scale = spec?.scaleTile ?: target
        if (!accessibilitySpawnAssist) return SpawnRules.pick(spec?.spawn ?: SpawnStyle.NORMAL, scale, rng)

        val rand = rng.nextDouble()
        return when {
            scale >= 1024 && rand < 0.08 -> 8
            rand < 0.22 -> 4
            else -> 2
        }
    }

    private fun hasBoardChanged(old: List<TileModel>, new: List<TileModel>): Boolean {
        if (old.size != new.size) return true
        return old.sortedBy { it.id }.map { it.row to it.col to it.value } != new.sortedBy { it.id }.map { it.row to it.col to it.value }
    }

    // 🔥🔥🔥🔥 NUEVAS FUNCIONES DE CICLO DE VIDA (PAUSA Y RESUME) 🔥🔥🔥🔥

    fun pauseGame() {
        // Detenemos el loop del juego rompiendo la condición 'while (isGameStarted)'
        isGameStarted = false
        // Detenemos cualquier Job de timer pendiente
        timerJob?.cancel()
        // Detenemos la música
        soundManager.stopMenuMusic() // Usamos este método para pausar si SoundManager no tiene 'pause' explícito
    }

    fun resumeGame() {
        // Solo reanudamos si el juego NO ha terminado
        val state = _boardState.value
        if (state.isGameOver || state.isLevelCompleted) return

        // Reactivamos la música (asumiendo que en modo juego usas la de menú o ambiente)
        soundManager.playMenuMusic(getApplication())

        // Reactivamos el Timer si estábamos a mitad de partida
        // Heurística: Si hay tiempo transcurrido o fichas en el tablero, reanudamos
        if (!isGameStarted && state.tiles.isNotEmpty()) {
            isGameStarted = true

            // 🧠 RECALCULO INTELIGENTE DEL TIEMPO REAL
            // Para que 'elapsed = Now - Start' siga dando el valor correcto,
            // tenemos que "fingir" un nuevo StartTime basado en lo que ya llevábamos jugado.
            val now = System.currentTimeMillis()

            realStartTime = if (state.maxTime != null) {
                // Modo Contrarreloj: elapsed es "tiempo restante".
                // Tiempo usado = Max - Restante
                // Start = Now - TiempoUsado
                val timeUsed = state.maxTime!! - state.elapsedTime
                now - timeUsed
            } else {
                // Modo Campaña: elapsed es "tiempo jugado".
                // Start = Now - TiempoJugado
                now - state.elapsedTime
            }

            // Reiniciamos el loop del tiempo
            startLevelTimer()
        }
    }
}


/** Rol en un duelo a distancia. */
enum class RemoteRole { NONE, CREATOR, CHALLENGED }

/** Fases del duelo local: jugando, entrega del teléfono al jugador 2, y resultado final. */
enum class DuelPhase { PLAYING, HANDOVER, RESULT }
