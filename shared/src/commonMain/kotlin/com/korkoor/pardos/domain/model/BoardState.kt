package com.korkoor.pardos.domain.model

import com.korkoor.pardos.domain.level.BossPhase
import com.korkoor.pardos.domain.level.Cell
import com.korkoor.pardos.domain.level.GoalStats
import com.korkoor.pardos.domain.level.LevelGoal
import com.korkoor.pardos.domain.level.LevelKind
import com.korkoor.pardos.domain.level.LevelRules
import com.korkoor.pardos.domain.level.Storm
import com.korkoor.pardos.domain.level.StormStone
import com.korkoor.pardos.domain.level.Twist
import kotlin.math.log2


/**
 * Estado inmutable del tablero de juego.
 * Controla la fuente de verdad de la interfaz de usuario.
 */
data class BoardState(
    val tiles: List<TileModel> = emptyList(),
    val score: Int = 0,
    val moveCount: Int = 0,
    val currentLevel: Int = 1,
    val levelLimit: Int = 64,
    val boardSize: Int = 4,
    val gameMode: GameMode = GameMode.CLASICO,
    val isGameOver: Boolean = false,
    val isLevelCompleted: Boolean = false,
    val isPaused: Boolean = false,
    val maxTime: Long? = null,
    val elapsedTime: Long = 0L,   // cronómetro ascendente
    val timeLeft: Long? = null,   // cuenta regresiva
    val bestScore: Int = 0,
    val combo: Int = 0,
    val allowPowerUps: Boolean = true,
    val starsEarned: Int = 0,
    val showTutorialHand: Boolean = false,
    val secondChanceUsed: Boolean = false,
    // --- Reglas del nivel de campaña (ver domain/level) ---
    /** Qué hay que conseguir: [levelLimit] es la ficha meta, o los puntos si la meta es de puntos. */
    val goal: LevelGoal = LevelGoal.REACH_TILE,
    /** Cuántas fichas de [levelLimit] hacen falta a la vez. */
    val goalCount: Int = 1,
    /** Piedras: casillas bloqueadas. */
    val blocked: List<Cell> = emptyList(),
    /** Movimientos permitidos en total; `null` = sin límite. */
    val moveLimit: Int? = null,
    /** Tipo de nivel y su nombre (solo en campaña). */
    val levelKind: LevelKind? = null,
    val levelTitle: String? = null,
    val levelTip: String? = null,
    /** La partida acabó por quedarse sin movimientos (no por tablero lleno). */
    val outOfMoves: Boolean = false,
    /** Fusiones hechas en esta partida (las usa el tutorial para saber cuándo ya sabe jugar). */
    val merges: Int = 0,
    /** Cuentas para las metas de maratón y combo (fusiones, mejor cadena, veces que se logró el combo). */
    val goalStats: GoalStats = GoalStats(),
    /** Giro de los controles del nivel. */
    val twist: Twist = Twist.NONE,
    /** Tormenta del nivel y las piedras temporales que hay ahora (ya incluidas en [blocked]). */
    val storm: Storm? = null,
    val stormStones: List<StormStone> = emptyList(),
    /** Jefes: la segunda fase (si la hay) y en cuál se está. */
    val phase2: BossPhase? = null,
    val phase: Int = 1
)
{
    // --- PROPIEDADES CALCULADAS ---

    val isTimeLow: Boolean
        get() = maxTime?.let { elapsedTime in 1..10_000 } ?: false

    val remainingTime: Long
        get() = elapsedTime

    val isActive: Boolean
        get() = !isGameOver && !isLevelCompleted && !isPaused

    /**
     * Avance hacia la meta en escala logarítmica: duplicar la ficha mayor siempre suma lo mismo
     * (con 2 de 64 la barra no queda casi vacía como con una escala lineal).
     */
    val levelProgress: Float
        get() {
            if (goal != LevelGoal.REACH_TILE) return LevelRules.progress(goal, levelLimit, goalCount, tiles, score, goalStats)
            if (levelLimit <= 2) return 0f
            val best = tiles.map { it.value }.sortedDescending().take(goalCount.coerceAtLeast(1))
            if (best.isEmpty()) return 0f
            val per = best.sumOf { v -> if (v <= 1) 0.0 else (log2(v.toFloat()) / log2(levelLimit.toFloat())).coerceIn(0f, 1f).toDouble() }
            return (per / goalCount.coerceAtLeast(1)).toFloat()
        }

    /** Movimientos que quedan (`null` si el nivel no tiene límite). */
    val movesLeft: Int?
        get() = moveLimit?.let { (it - moveCount).coerceAtLeast(0) }

    val emptySpaces: Int
        get() = (boardSize * boardSize) - blocked.size - tiles.size

    val hasMovesAvailable: Boolean
        get() = emptySpaces > 0 || canMerge()

    val highestTile: TileModel?
        get() = tiles.maxByOrNull { it.value }

    // --- LÓGICA DE NEGOCIO ---

    private fun canMerge(): Boolean {
        val tileMap = tiles.associateBy { it.row to it.col }
        return tiles.any { tile ->
            val adjacent = listOf(
                (tile.row - 1) to tile.col,
                (tile.row + 1) to tile.col,
                tile.row to (tile.col - 1),
                tile.row to (tile.col + 1)
            )
            adjacent.any { pos -> tileMap[pos]?.value == tile.value }
        }
    }

    // --- OPERACIONES DE ESTADO (COPIAS INMUTABLES) ---

    fun withUpdatedStats(
        newScore: Int? = null,
        newMoves: Int? = null,
        newTime: Long? = null,
        newCombo: Int? = null
    ): BoardState = copy(
        score = newScore ?: score,
        moveCount = newMoves ?: moveCount,
        elapsedTime = newTime ?: elapsedTime,
        combo = newCombo ?: combo
    )

    fun completeLevel(): BoardState = copy(
        isLevelCompleted = true,
        isPaused = true
    )

    fun gameOver(): BoardState = copy(
        isGameOver = true,
        isPaused = true
    )

    fun nextLevel(newLimit: Int, newBoardSize: Int? = null): BoardState = copy(
        currentLevel = currentLevel + 1,
        levelLimit = newLimit,
        boardSize = newBoardSize ?: boardSize,
        tiles = emptyList(),
        isLevelCompleted = false,
        isPaused = false,
        moveCount = 0,
        combo = 0,
        starsEarned = 0,
        goalStats = GoalStats()
    )

    fun reset(): BoardState = copy(
        tiles = emptyList(),
        score = 0,
        moveCount = 0,
        isGameOver = false,
        isLevelCompleted = false,
        isPaused = false,
        elapsedTime = maxTime ?: 0L,
        combo = 0,
        starsEarned = 0,
        outOfMoves = false
    )

    // --- VALIDACIÓN ---

    fun validate(): BoardStateValidation {
        val errors = mutableListOf<String>()

        if (boardSize !in MIN_BOARD_SIZE..MAX_BOARD_SIZE) {
            errors.add("Tamaño de tablero inválido: $boardSize")
        }
        if (tiles.size > boardSize * boardSize - blocked.size) {
            errors.add("Demasiadas fichas: ${tiles.size}")
        }
        if (tiles.any { it.row >= boardSize || it.col >= boardSize }) {
            errors.add("Fichas fuera de los límites")
        }
        if (tiles.groupBy { it.row to it.col }.any { it.value.size > 1 }) {
            errors.add("Múltiples fichas en la misma posición")
        }
        if (tiles.any { (it.row to it.col) in blocked }) {
            errors.add("Ficha sobre una piedra")
        }
        if (currentLevel < 1) errors.add("Nivel inválido: $currentLevel")
        if (levelLimit < 2 || ((goal == LevelGoal.REACH_TILE || goal == LevelGoal.LADDER) && !isPowerOfTwo(levelLimit))) errors.add("Límite inválido: $levelLimit")
        if (score < 0) errors.add("Puntuación negativa: $score")
        if (moveCount < 0) errors.add("Movimientos negativos: $moveCount")
        if (elapsedTime < 0) errors.add("Tiempo negativo: $elapsedTime")
        if (maxTime != null && maxTime < 0) errors.add("Tiempo máximo negativo: $maxTime")

        return BoardStateValidation(errors.isEmpty(), errors)
    }

    companion object {
        const val MIN_BOARD_SIZE = 3
        const val MAX_BOARD_SIZE = 8

        fun initial(mode: GameMode, boardSize: Int = 4): BoardState {
            require(boardSize in MIN_BOARD_SIZE..MAX_BOARD_SIZE) {
                "Board size must be between $MIN_BOARD_SIZE and $MAX_BOARD_SIZE"
            }
            return BoardState(
                gameMode = mode,
                boardSize = boardSize,
                levelLimit = mode.initialTarget,
                maxTime = mode.timeLimit,
                elapsedTime = mode.timeLimit ?: 0L
            )
        }

        private fun isPowerOfTwo(n: Int): Boolean = n > 0 && (n and (n - 1)) == 0
    }
}

/**
 * Clase auxiliar para resultados de validación.
 */
data class BoardStateValidation(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
) {
    fun throwIfInvalid() {
        if (!isValid) throw IllegalStateException("Estado de tablero inválido:\n${errors.joinToString("\n")}")
    }
}
/** timeLimit en MILISEGUNDOS. */
enum class GameMode(
    val initialTarget: Int,
    val timeLimit: Long?
) {
    CLASICO(64, null),
    DESAFIO(128, 180_000L),
    ZEN(2048, null),
    RAPIDO(64, 60_000L),
    TABLAS(0, 150_000L),
    CARRERA(16, 90_000L),
    DUELO(4096, 60_000L),

    // 🔥 NUEVO MODO AGREGADO (Arregla el bug del tablero 3x3)
    CUSTOM(2048, null);
}
