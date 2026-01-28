package com.korkoor.pardos.ui.game

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

private const val COOLDOWN_MS = 15 * 60 * 1000L // 15 minutos

class GameViewModel(application: Application) : AndroidViewModel(application) {

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

    var lastCleanTime by mutableLongStateOf(0L)
    var lastMergeTime by mutableLongStateOf(0L)

    // 🔥 TEMA DEL DESAFÍO DIARIO
    var dailyChallengeThemeIndex by mutableStateOf<Int?>(null)
        private set

    // 2. PREFERENCIAS
    private val prefs = application.getSharedPreferences("pardos_storage", Context.MODE_PRIVATE)
    private val KEY_LAST_LEVEL = "last_reached_level"
    private val KEY_TABLES_LEVEL = "last_reached_tables_level"
    private val KEY_LAST_UNLOCKED = "last_unlocked_level"
    private val KEY_SAVED_SCORE = "saved_score_level"

    // 3. ESTADOS DE FLUJO
    private val _currentTimeProvider = MutableStateFlow(System.currentTimeMillis())
    val currentTimeProvider: StateFlow<Long> = _currentTimeProvider

    private val _levels = MutableStateFlow<List<LevelInfo>>(emptyList())
    val levels: StateFlow<List<LevelInfo>> = _levels.asStateFlow()

    private val _unlockedAchievements = MutableStateFlow<Set<String>>(emptySet())
    val unlockedAchievements: StateFlow<Set<String>> = _unlockedAchievements

    // 4. ESTADO INICIAL
    private val _boardState = MutableStateFlow(
        BoardState(
            currentLevel = 1,
            levelLimit = 64,
            boardSize = 3,
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
        .fallbackToDestructiveMigration()
        .build()
    private val recordDao = db.recordDao()

    val allRecords = recordDao.getAllRecords().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // 6. BLOQUE DE INICIALIZACIÓN
    init {
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
    }

    // --- FUNCIONES DE APOYO ---

    fun resetGameSession() {
        dailyChallengeThemeIndex = null
        timerJob?.cancel()
        timerManager.stop()
        isMoving = false
        floatingScores.clear()
    }

    // 🔥🔥 CORRECCIÓN CRÍTICA AQUÍ 🔥🔥
    // Antes forzábamos el nivel guardado. Ahora respetamos el nivel actual del estado (el que seleccionaste).
    fun refreshCurrentLevelDifficulty() {
        if (currentMode == GameMode.CLASICO) {
            val currentState = _boardState.value
            val currentLevel = currentState.currentLevel // Usamos el nivel ACTUAL, no el guardado

            // Calculamos cuál debería ser la meta para ESTE nivel
            val expectedTarget = ProgressionEngine.calculateTargetForLevel(currentLevel)

            // Solo corregimos si la meta es incorrecta (ej. Nivel 1 pidiendo 2048)
            if (currentState.levelLimit != expectedTarget) {
                Log.d("GAME_FIX", "Corrigiendo dificultad para Nivel $currentLevel")
                setupCustomGame(
                    size = ProgressionEngine.calculateBoardSize(expectedTarget),
                    target = expectedTarget,
                    level = currentLevel, // Mantenemos el nivel seleccionado
                    initialScore = currentState.score
                )
            }
        }
    }

    private fun loadLevelsWithProgress() {
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
                    val isChallenge = state.maxTime != null

                    val nextTime = if (isChallenge) {
                        (state.elapsedTime - 1).coerceAtLeast(0L)
                    } else {
                        state.elapsedTime + 1
                    }

                    if (isChallenge && nextTime <= 0L) {
                        this@launch.cancel()
                        prefs.edit().remove(KEY_SAVED_SCORE).apply()
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
                difficulty = "Zen",
                level = tablesLevel,
                initialScore = 0
            )
        } else {
            currentMultiplierBase = 2

            val levelToStart = if (mode == GameMode.CLASICO) {
                prefs.getInt(KEY_LAST_LEVEL, 1)
            } else {
                1
            }

            val savedScore = if (mode == GameMode.CLASICO) prefs.getInt(KEY_SAVED_SCORE, 0) else 0

            val correctTarget = ProgressionEngine.calculateTargetForLevel(levelToStart)
            val correctSize = ProgressionEngine.calculateBoardSize(correctTarget)

            setupCustomGame(
                size = correctSize,
                target = correctTarget,
                allowPowerUps = true,
                difficulty = if (mode == GameMode.DESAFIO || mode == GameMode.RAPIDO) "Normal" else "Zen",
                level = levelToStart,
                initialScore = savedScore
            )
        }
        startLevelTimer()
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
        timerJob?.cancel()
        timerManager.stop()
        isMoving = false
        comboJob?.cancel()
        floatingScores.clear()

        _comboCount.value = 0
        showLevelSummary = false

        gameEngine = GameEngine(boardSize = size)

        val timeLimitSeconds = when(difficulty) {
            "Normal" -> ProgressionEngine.calculateTimeLimitForTarget(target)
            "Extremo" -> ProgressionEngine.calculateTimeLimitForTarget(target) / 2
            else -> null
        }

        val baseMode = if (isCustom) GameMode.CUSTOM else currentMode
        val determinedMode = if (timeLimitSeconds != null) GameMode.DESAFIO else baseMode
        this.currentMode = determinedMode

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
                tiles = emptyList(),
                maxTime = timeLimitSeconds,
                elapsedTime = timeLimitSeconds ?: 0L,
                showTutorialHand = (level == 1 && initialScore == 0),
                secondChanceUsed = false
            )
        }

        spawnInitialTiles(level)

        timeLimitSeconds?.let { limit ->
            timerManager.startTimer(mode = determinedMode, initialTime = limit)
        }

        startLevelTimer()
    }

    fun onMove(direction: Direction, onHapticFeedback: (HapticFeedbackType) -> Unit) {
        val state = _boardState.value

        if (state.showTutorialHand) {
            _boardState.update { it.copy(showTutorialHand = false) }
        }

        if (isMoving || state.isLevelCompleted || state.isGameOver) return

        viewModelScope.launch {
            val currentState = _boardState.value
            val currentTiles = currentState.tiles

            val (movedTiles, scoreGained) = gameEngine.move(currentTiles, direction, currentMultiplierBase)

            if (hasBoardChanged(currentTiles, movedTiles)) {
                isMoving = true

                val mergesCount = (currentTiles.size - movedTiles.size).coerceAtLeast(0)

                if (mergesCount > 0) {
                    onHapticFeedback(HapticFeedbackType.LongPress)
                } else {
                    onHapticFeedback(HapticFeedbackType.TextHandleMove)
                }

                if (mergesCount > 1) {
                    applyComboTimeBonus(mergesCount)
                    registerMerge()
                }

                if (scoreGained > 0) {
                    val randomCol = (0 until currentState.boardSize).random()
                    val randomRow = (0 until currentState.boardSize).random()
                    addFloatingScore(scoreGained, randomCol, randomRow)
                }

                delay(80)
                val prob4 = ProgressionEngine.calculateFourProbability(currentState.currentLevel)
                val finalTiles = movedTiles.toMutableList()
                gameEngine.spawnTile(movedTiles, prob4, currentMultiplierBase)?.let {
                    finalTiles.add(it)
                }

                val maxTileValue = finalTiles.maxOfOrNull { it.value } ?: 0
                val reachedTarget = maxTileValue >= currentState.levelLimit

                val newScore = currentState.score + scoreGained

                _boardState.update { state ->
                    state.copy(
                        tiles = finalTiles,
                        score = newScore,
                        moveCount = state.moveCount + 1
                    )
                }

                if (currentState.gameMode == GameMode.CLASICO) {
                    prefs.edit().putInt(KEY_SAVED_SCORE, newScore).apply()
                }

                when {
                    reachedTarget -> {
                        handleLevelVictory(maxTileValue)
                    }
                    gameEngine.isGameOver(finalTiles) -> {
                        handleGameOver()
                    }
                }

                isMoving = false
            }
        }
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
    }

    private fun handleLevelVictory(maxTile: Int) {
        val targetReached = maxTile >= _boardState.value.levelLimit
        if (!targetReached) return

        timerJob?.cancel()
        timerManager.stop()

        var finalStars = 0
        var finalTimeUsed = 0L
        var stateForAchievements: BoardState? = null

        _boardState.update { state ->
            val totalLimit = state.maxTime ?: 0L
            val timeRemainingOrElapsed = state.elapsedTime

            if (totalLimit > 0) {
                finalStars = ProgressionEngine.calculateStars(timeRemainingOrElapsed, totalLimit)
                finalTimeUsed = totalLimit - timeRemainingOrElapsed
            } else {
                finalStars = 3
                finalTimeUsed = timeRemainingOrElapsed
            }

            val newState = state.copy(
                isLevelCompleted = true,
                starsEarned = finalStars,
                isGameOver = false
            )

            stateForAchievements = newState
            newState
        }

        if (finalStars > 0) {
            saveLevelProgress(
                level = _boardState.value.currentLevel,
                stars = finalStars,
                finalTime = finalTimeUsed,
                finalMoves = _boardState.value.moveCount
            )

            stateForAchievements?.let { checkAchievements(it) }
            saveRecord()
            prefs.edit().remove(KEY_SAVED_SCORE).apply()
        }

        viewModelScope.launch {
            delay(800)
            showLevelSummary = true
        }
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

    private fun checkGameState(tiles: List<TileModel>) {
        if (_boardState.value.isLevelCompleted) return

        if (gameEngine.isGameOver(tiles)) {
            timerManager.stop()
            _boardState.update { it.copy(isGameOver = true) }
        }
    }

    private fun saveLevelProgress(level: Int, stars: Int, finalTime: Long, finalMoves: Int) {
        val editor = prefs.edit()

        val starKey = "stars_level_$level"
        val previousStars = prefs.getInt(starKey, 0)
        if (stars > previousStars) {
            editor.putInt(starKey, stars)
        }

        val timeKey = "best_time_level_$level"
        val prevTime = prefs.getLong(timeKey, Long.MAX_VALUE)
        val validPrevTime = if (prevTime == 0L) Long.MAX_VALUE else prevTime

        if (finalTime > 0 && finalTime < validPrevTime) {
            editor.putLong(timeKey, finalTime)
        }

        val movesKey = "best_moves_level_$level"
        val prevMoves = prefs.getInt(movesKey, Int.MAX_VALUE)
        val validPrevMoves = if (prevMoves == 0) Int.MAX_VALUE else prevMoves

        if (finalMoves > 0 && finalMoves < validPrevMoves) {
            editor.putInt(movesKey, finalMoves)
        }

        val currentMaxUnlocked = prefs.getInt("last_unlocked_level", 1)
        if (level == currentMaxUnlocked) {
            editor.putInt("last_unlocked_level", level + 1)
        }

        editor.apply()
        loadLevelsWithProgress()
    }

    fun getBestStats(level: Int): Pair<Int, Long> {
        val bMoves = prefs.getInt("best_moves_level_$level", 0)
        val bTime = prefs.getLong("best_time_level_$level", 0L)
        return Pair(bMoves, bTime)
    }

    fun retryLevel() {
        val levelToRetry = _boardState.value.currentLevel
        val arePowerUpsAllowed = _boardState.value.allowPowerUps

        val target = ProgressionEngine.calculateTargetForLevel(levelToRetry)
        val size = ProgressionEngine.calculateBoardSize(target)

        showLevelSummary = false
        isMoving = false
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
        startLevelTimer()
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
        val nextLv = currentState.currentLevel + 1 // 🔥 Calculamos el siguiente nivel

        viewModelScope.launch {
            showLevelSummary = false
            floatingScores.clear()

            if (currentMode == GameMode.TABLAS) {
                prefs.edit().putInt(KEY_TABLES_LEVEL, nextLv).apply()
                delay(300)
                startNewGame(GameMode.TABLAS)
                return@launch
            }

            if (currentMode == GameMode.CLASICO) {
                // 🔥 FIX: Solo actualizamos el "Último Nivel" si avanzamos, no si rejugamos
                val currentMax = prefs.getInt(KEY_LAST_LEVEL, 1)
                if (nextLv > currentMax) {
                    prefs.edit().putInt(KEY_LAST_LEVEL, nextLv).apply()
                }
                prefs.edit().remove(KEY_SAVED_SCORE).apply()
            }

            val newTarget = ProgressionEngine.calculateTargetForLevel(nextLv)
            val newSize = ProgressionEngine.calculateBoardSize(newTarget)

            delay(300)

            // Pasamos explícitamente el nivel siguiente
            setupCustomGame(
                size = newSize,
                target = newTarget,
                level = nextLv,
                initialScore = currentState.score
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

    private fun spawnInitialTiles(level: Int) {
        val prob = ProgressionEngine.calculateFourProbability(level)
        val t1 = gameEngine.spawnTile(emptyList(), prob, currentMultiplierBase)
        val t2 = gameEngine.spawnTile(listOfNotNull(t1), prob, currentMultiplierBase)
        _boardState.update { it.copy(tiles = listOfNotNull(t1, t2)) }
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
            delay(2000)
            _comboCount.value = 1
        }
    }

    fun grantAdReward(type: String) {
        viewModelScope.launch {
            isMoving = false
            when (type) {
                "CLEAN" -> resetPowerUpCooldown("CLEAN")
                "MERGE" -> resetPowerUpCooldown("MERGE")
                "REVIVE" -> {
                    val currentTiles = _boardState.value.tiles
                    val tilesToKeepCount = (currentTiles.size / 2).coerceAtLeast(2)
                    val cleanedTiles = currentTiles.sortedByDescending { it.value }.take(tilesToKeepCount)
                    val finalTiles = if (cleanedTiles.isEmpty()) {
                        val prob = ProgressionEngine.calculateFourProbability(_boardState.value.currentLevel)
                        val t1 = gameEngine.spawnTile(emptyList(), prob, currentMultiplierBase)
                        val t2 = gameEngine.spawnTile(listOfNotNull(t1), prob, currentMultiplierBase)
                        listOfNotNull(t1, t2)
                    } else { cleanedTiles }

                    _boardState.update { currentState ->
                        val newTime = if (currentState.maxTime != null && currentState.elapsedTime <= 5) {
                            currentState.elapsedTime + 15
                        } else {
                            currentState.elapsedTime
                        }
                        currentState.copy(tiles = finalTiles, isGameOver = false, secondChanceUsed = true, showTutorialHand = true, elapsedTime = newTime)
                    }
                    startLevelTimer()
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
}