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

    var activeAchievementPopup by mutableStateOf<Achievement?>(null)
        private set
    var isSelectModeActive by mutableStateOf(false)
        private set

    var pendingPowerUpType by mutableStateOf<String?>(null) // "MANUAL_MERGE" o "SINGLE_CLEAN"
        private set

    var firstSelectedTileId by mutableStateOf<String?>(null)
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

    // 🔥 TIEMPO REAL: Variable para guardar la hora exacta de inicio del sistema
    private var realStartTime: Long = 0L

    // 2. PREFERENCIAS
    private val prefs = application.getSharedPreferences("pardos_storage", Context.MODE_PRIVATE)
    private val KEY_LAST_LEVEL = "last_reached_level"
    private val KEY_TABLES_LEVEL = "last_reached_tables_level"
    private val KEY_LAST_UNLOCKED = "last_unlocked_level"
    private val KEY_SAVED_SCORE = "saved_score_level"
    // Nueva llave para contar intentos fallidos
    private val KEY_ATTEMPTS = "attempts_fail_level_"

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
        dailyChallengeThemeIndex = null
        timerJob?.cancel()
        timerManager.stop()
        isMoving = false
        isGameStarted = false
        realStartTime = 0L // Reiniciamos el reloj real
        floatingScores.clear()
        playMenuMusic()
    }

    fun refreshCurrentLevelDifficulty() {
        if (currentMode == GameMode.CLASICO) {
            val currentState = _boardState.value
            val currentLevel = currentState.currentLevel
            val expectedTarget = ProgressionEngine.calculateTargetForLevel(currentLevel)

            if (currentState.levelLimit != expectedTarget) {
                Log.d("GAME_FIX", "Corrigiendo dificultad para Nivel $currentLevel")
                setupCustomGame(
                    size = ProgressionEngine.calculateBoardSize(expectedTarget),
                    target = expectedTarget,
                    level = currentLevel,
                    initialScore = currentState.score
                )
            }
        }
    }

    // 🔥 FIX: Función pública para recargar datos en el menú
    fun loadLevelsWithProgress() {
        val baseLevels = LevelRepository.getGeneratedLevels()
        val unlockedUntil = prefs.getInt(KEY_LAST_UNLOCKED, 1)

        val updatedLevels = baseLevels.map { level ->
            val stars = prefs.getInt("stars_level_${level.id}", 0)
            val bestTime = prefs.getLong("best_time_level_${level.id}", 0L)
            val bestMoves = prefs.getInt("best_moves_level_${level.id}", 0)

            level.copy(
                starsEarned = stars,
                bestTime = bestTime,
                bestMoves = bestMoves,
                isLocked = level.id > unlockedUntil
            )
        }
        _levels.value = updatedLevels
    }

    fun startLevelTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _boardState.update { state ->
                    // FIX: Si maxTime existe, es cuenta atrás. Si no, cuenta adelante.
                    val isTimedLevel = state.maxTime != null

                    val nextTime = if (isTimedLevel) {
                        (state.elapsedTime - 1).coerceAtLeast(0L)
                    } else {
                        state.elapsedTime + 1
                    }

                    if (isTimedLevel && nextTime <= 0L) {
                        this@launch.cancel()
                        handleGameOver()
                        state.copy(elapsedTime = 0L, isGameOver = true)
                    } else {
                        state.copy(elapsedTime = nextTime)
                    }
                }
            }
        }
    }

    private fun handleGameOver() {
        timerJob?.cancel()
        _boardState.update { it.copy(isGameOver = true) }
        prefs.edit().remove(KEY_SAVED_SCORE).apply()

        // 💀 PIEDAD: Si pierdes, aumentamos el contador de intentos
        val level = _boardState.value.currentLevel
        val currentAttempts = prefs.getInt("$KEY_ATTEMPTS$level", 0)
        prefs.edit().putInt("$KEY_ATTEMPTS$level", currentAttempts + 1).apply()

        // 🔥 INTEGRACIÓN MISIONES DIARIAS: Partida jugada (incluso si se pierde) 🔥
        missionManager.updateProgress(MissionType.PLAY_GAMES, 1)

        soundManager.playGameOver()
    }

    fun startNewGame(mode: GameMode) {
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
        isCustom: Boolean = false
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
        gameEngine = GameEngine(boardSize = size)

        // 3. DETERMINACIÓN DEL MODO (FIX: Evita que la campaña herede el modo Desafío)
        val determinedMode = if (isCustom) {
            if (difficulty != "Zen") GameMode.DESAFIO else GameMode.CUSTOM
        } else {
            // Si no es custom, respetamos el modo de campaña actual (CLASICO o TABLAS)
            currentMode
        }

        // 4. CÁLCULO DE TIEMPO (Solo se activa en Desafío o Custom con dificultad)
        val timeLimitSeconds = if (determinedMode == GameMode.DESAFIO || (isCustom && difficulty != "Zen")) {
            ProgressionEngine.calculateTimeLimitForTarget(target, isCampaign = false)
        } else {
            null // Campaña siempre es Zen/Sin tiempo
        }

        this.currentMode = determinedMode

        // 5. SISTEMA DE PIEDAD (PITY MODE)
        val attempts = prefs.getInt("$KEY_ATTEMPTS$level", 0)
        isPityModeActive = attempts >= 5

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
                maxTime = timeLimitSeconds,
                // Si no hay tiempo límite, el tiempo transcurrido debe ser 0 para no contar
                elapsedTime = timeLimitSeconds ?: 0L,
                showTutorialHand = (level == 1 && initialScore == 0),
                secondChanceUsed = false,
                moveCount = 0
            )
        }

        // 7. GENERACIÓN DE FICHAS INICIALES
        spawnInitialTiles(level, target)
    }

    fun onMove(direction: Direction, onHapticFeedback: (HapticFeedbackType) -> Unit) {
        val state = _boardState.value

        // ⏳ GESTIÓN DE TIEMPO
        if (!isGameStarted) {
            isGameStarted = true
            realStartTime = System.currentTimeMillis()

            viewModelScope.launch {
                while (isGameStarted) {
                    val elapsed = System.currentTimeMillis() - realStartTime
                    _boardState.update { current ->
                        if (current.maxTime != null) {
                            val remaining = current.maxTime - elapsed
                            if (remaining <= 0) {
                                handleGameOver()
                                isGameStarted = false
                                current.copy(elapsedTime = 0L)
                            } else {
                                current.copy(elapsedTime = remaining)
                            }
                        } else {
                            current.copy(elapsedTime = elapsed) // Modo Campaña (Sin tiempo)
                        }
                    }
                    delay(1000)
                }
            }
        }

        if (state.showTutorialHand) {
            _boardState.update { it.copy(showTutorialHand = false) }
        }

        if (isMoving || state.isLevelCompleted || state.isGameOver) return

        viewModelScope.launch {
            val currentState = _boardState.value
            val currentTiles = currentState.tiles

            // 🛠️ ELIMINADO MULTIPLICADOR: Ahora la fusión es estándar (x1)
            val (movedTiles, scoreGained) = gameEngine.move(currentTiles, direction, 1)

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
                }

                delay(80)

                // 🎲 GENERACIÓN INTELIGENTE (Aparición normal)
                val finalTiles = movedTiles.toMutableList()
                val newValue = ProgressionEngine.getNewTileValue(currentState.levelLimit)
                gameEngine.spawnTileWithSpecificValue(movedTiles, newValue, 1)?.let {
                    finalTiles.add(it)
                }

                // ✨ EVOLUCIÓN ESPONTÁNEA (Solo 4, 8, 16 de vez en cuando)
                // Probabilidad del 15% para que ocurra
                if ((1..100).random() <= 15) {
                    val luckyCandidates = finalTiles.filter { it.value == 4 || it.value == 8 || it.value == 16 }
                    if (luckyCandidates.isNotEmpty()) {
                        val luckyTile = luckyCandidates.random()
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
                }

                val reachedTarget = maxTileValue >= currentState.levelLimit
                val newScore = currentState.score + scoreGained

                _boardState.update { it.copy(tiles = finalTiles, score = newScore, moveCount = it.moveCount + 1) }

                if (currentState.gameMode == GameMode.CLASICO) {
                    prefs.edit().putInt(KEY_SAVED_SCORE, newScore).apply()
                }

                // 🏆 REGLA DE ORO: Si ya ganaste, paramos TODO aquí para evitar el crash
                if (reachedTarget) {
                    isMoving = false
                    isGameStarted = false // Detener el hilo del tiempo
                    handleLevelVictory(maxTileValue)
                    return@launch
                } else if (gameEngine.isGameOver(finalTiles)) {
                    handleGameOver()
                    isMoving = false
                    return@launch
                }

                isMoving = false

                // ✨ AYUDA DIVINA (Mantenida exactamente igual)
                if (ProgressionEngine.shouldTriggerDivineHelp(currentState.levelLimit)) {
                    delay(150) // Pausa dramática
                    _boardState.update { current ->
                        val tiles = current.tiles.toMutableList()

                        // Solo fichas <= 25% de la meta (Balance justo)
                        val limitThreshold = (current.levelLimit * 0.25).toInt()
                        val candidates = tiles.filter { ProgressionEngine.isValueEligibleForDivineHelp(it.value) }

                        if (candidates.isNotEmpty()) {
                            val luckyTile = candidates.random()
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
        val targetReached = maxTile >= currentState.levelLimit
        if (!targetReached) return

        // 1. Detenemos los relojes inmediatamente
        timerJob?.cancel()
        timerManager.stop()
        stopTimer()

        // 🏆 VICTORIA: Limpiamos intentos fallidos
        val level = currentState.currentLevel
        prefs.edit().remove("$KEY_ATTEMPTS$level").apply()

        var finalStars = 0
        var finalTimeUsed = 0L
        var stateForAchievements: BoardState? = null

        _boardState.update { state ->
            val totalLimit = state.maxTime ?: 0L

            if (totalLimit > 0) {
                // MODO DESAFÍO: El tiempo usado es el límite total menos lo que sobró
                finalTimeUsed = (totalLimit - state.elapsedTime).coerceAtLeast(0L)
                finalStars = ProgressionEngine.calculateStars(finalTimeUsed, state.levelLimit)
            } else {
                // 🔥 MODO CAMPAÑA CORREGIDO:
                // Usamos directamente el elapsedTime del estado, que ya lleva
                // la cuenta exacta de los milisegundos jugados.
                finalTimeUsed = state.elapsedTime
                finalStars = 3
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
                    val newTime = (state.elapsedTime + bonusSeconds).coerceAtMost(limit)
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
            val level = _boardState.value.currentLevel
            val currentAttempts = prefs.getInt("$KEY_ATTEMPTS$level", 0)
            prefs.edit { putInt("$KEY_ATTEMPTS$level", currentAttempts + 1) }

            soundManager.playGameOver()
        }
    }

    private fun saveLevelProgress(level: Int, stars: Int, finalTime: Long, finalMoves: Int) {
        val editor = prefs.edit()

        val prefix = when (currentMode) {
            GameMode.CLASICO -> ""
            GameMode.TABLAS -> "tablas_"
            GameMode.DESAFIO -> "daily_"
            else -> "custom_"
        }

        val starKey = "${prefix}stars_level_$level"
        val previousStars = prefs.getInt(starKey, 0)
        if (stars > previousStars) {
            editor.putInt(starKey, stars)
        }

        val timeKey = "${prefix}best_time_level_$level"
        val prevTime = prefs.getLong(timeKey, Long.MAX_VALUE)
        val validPrevTime = if (prevTime == 0L) Long.MAX_VALUE else prevTime

        if (finalTime > 0 && finalTime < validPrevTime) {
            editor.putLong(timeKey, finalTime)
        }

        val movesKey = "${prefix}best_moves_level_$level"
        val prevMoves = prefs.getInt(movesKey, Int.MAX_VALUE)
        val validPrevMoves = if (prevMoves == 0) Int.MAX_VALUE else prevMoves
        if (finalMoves > 0 && finalMoves < validPrevMoves) {
            editor.putInt(movesKey, finalMoves)
        }

        if (currentMode == GameMode.CLASICO) {
            val nextLevelToUnlock = level + 1
            val currentMaxUnlocked = prefs.getInt(KEY_LAST_UNLOCKED, 1)
            val newMax = max(currentMaxUnlocked, nextLevelToUnlock)
            editor.putInt(KEY_LAST_UNLOCKED, newMax)

            val currentReached = prefs.getInt(KEY_LAST_LEVEL, 1)
            val newReached = max(currentReached, nextLevelToUnlock)
            editor.putInt(KEY_LAST_LEVEL, newReached)
            editor.commit()
        }
        else if (currentMode == GameMode.TABLAS) {
            val nextLevelToUnlock = level + 1
            val currentTableLevel = prefs.getInt(KEY_TABLES_LEVEL, 1)
            val newMax = max(currentTableLevel, nextLevelToUnlock)
            editor.putInt(KEY_TABLES_LEVEL, newMax)
            editor.commit()
        }
        else {
            editor.apply()
        }

        if (currentMode == GameMode.CLASICO) {
            loadLevelsWithProgress()
        }
    }

    fun getBestStats(level: Int): Pair<Int, Long> {
        val prefix = when (currentMode) {
            GameMode.CLASICO -> ""
            GameMode.TABLAS -> "tablas_"
            GameMode.DESAFIO -> "daily_"
            else -> "custom_"
        }
        val bMoves = prefs.getInt("${prefix}best_moves_level_$level", 0)
        val bTime = prefs.getLong("${prefix}best_time_level_$level", 0L)
        return Pair(bMoves, bTime)
    }

    fun retryLevel() {
        val levelToRetry = _boardState.value.currentLevel
        val arePowerUpsAllowed = _boardState.value.allowPowerUps

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
                val newTarget = ProgressionEngine.calculateTargetForLevel(nextLv)
                val newSize = ProgressionEngine.calculateBoardSize(newTarget)
                delay(300)
                setupCustomGame(
                    size = newSize,
                    target = newTarget,
                    level = nextLv,
                    initialScore = currentState.score
                )
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

        // 🚀 LÓGICA DE CANTIDAD: Más fichas para tableros más grandes
        // 3x3 -> 2 fichas | 4x4 -> 3 fichas | 5x5 y 6x6 -> 4 fichas
        val initialTilesCount = when {
            boardSize >= 5 -> 4
            boardSize == 4 -> 3
            else -> 2
        }

        repeat(initialTilesCount) {
            // 🎲 Obtenemos valores inteligentes según el target del nivel
            val newValue = ProgressionEngine.getNewTileValue(target)

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
                    val tilesToKeepCount = (currentTiles.size / 2).coerceAtLeast(2)
                    val cleanedTiles = currentTiles.sortedByDescending { it.value }.take(tilesToKeepCount)

                    // 🔥 SÚPER BALANCE: Si el tablero estaba vacío, generamos fichas inteligentes
                    val finalTiles = cleanedTiles.ifEmpty {
                        val v1 = ProgressionEngine.getNewTileValue(currentState.levelLimit)
                        val v2 = ProgressionEngine.getNewTileValue(currentState.levelLimit)

                        val t1 = gameEngine.spawnTileWithSpecificValue(emptyList(), v1, currentMultiplierBase)
                        val t2 = gameEngine.spawnTileWithSpecificValue(listOfNotNull(t1), v2, currentMultiplierBase)
                        listOfNotNull(t1, t2)
                    }

                    _boardState.update { state ->
                        // ⏳ BALANCE DE TIEMPO: Añadimos 30 segundos (30000ms) si el tiempo es crítico
                        val bonusTimeMs = 30000L
                        val newTime = if (state.maxTime != null && state.elapsedTime <= 5000L) {
                            state.elapsedTime + bonusTimeMs
                        } else {
                            state.elapsedTime
                        }

                        state.copy(
                            tiles = finalTiles,
                            isGameOver = false,
                            secondChanceUsed = true,
                            showTutorialHand = true,
                            elapsedTime = newTime
                        )
                    }
                    // Nota: Asegúrate de que startLevelTimer o tu lógica de onMove maneje la reanudación
                }
            }
        }
    }

    fun setupDailyChallenge() {
        val calendar = Calendar.getInstance()
        val dateSeed = calendar.get(Calendar.YEAR) * 10000 + (calendar.get(Calendar.MONTH) + 1) * 100 + calendar.get(Calendar.DAY_OF_MONTH)
        val randomWithSeed = Random(dateSeed.toLong())
        val dailySize = if (randomWithSeed.nextInt(100) % 2 == 0) 4 else 5
        val dailyTarget = if (randomWithSeed.nextBoolean()) 1024 else 2048
        val randomTheme = randomWithSeed.nextInt(0, 6)
        dailyChallengeThemeIndex = randomTheme
        currentMode = GameMode.DESAFIO
        setupCustomGame(size = dailySize, target = dailyTarget, allowPowerUps = false, difficulty = "Normal", level = 1, isCustom = true)
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
                val timeUsed = state.maxTime - state.elapsedTime
                now - timeUsed
            } else {
                // Modo Campaña: elapsed es "tiempo jugado".
                // Start = Now - TiempoJugado
                now - state.elapsedTime
            }

            // Reiniciamos el loop del tiempo
            viewModelScope.launch {
                while (isGameStarted) {
                    val elapsed = System.currentTimeMillis() - realStartTime
                    _boardState.update { current ->
                        if (current.maxTime != null) {
                            val remaining = current.maxTime - elapsed
                            if (remaining <= 0) {
                                handleGameOver()
                                isGameStarted = false
                                current.copy(elapsedTime = 0L)
                            } else {
                                current.copy(elapsedTime = remaining)
                            }
                        } else {
                            current.copy(elapsedTime = elapsed)
                        }
                    }
                    delay(1000)
                }
            }
        }
    }
}