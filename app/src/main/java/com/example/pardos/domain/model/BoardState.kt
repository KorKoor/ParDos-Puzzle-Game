package com.example.pardos.domain.model

import com.example.pardos.R
import androidx.compose.ui.graphics.Color

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
    val elapsedTime: Long = 0L,
    val bestScore: Int = 0,
    val combo: Int = 0,
    val allowPowerUps: Boolean = true,
    val starsEarned: Int = 0, // Calificación de desempeño (0-3)
    val showTutorialHand: Boolean = false,
    val secondChanceUsed: Boolean = false
) {
    // --- PROPIEDADES CALCULADAS ---

    val isTimeLow: Boolean
        get() = maxTime?.let { elapsedTime in 1..10 } ?: false

    val remainingTime: Long
        get() = elapsedTime

    val isActive: Boolean
        get() = !isGameOver && !isLevelCompleted && !isPaused

    val levelProgress: Float
        get() {
            val maxTileValue = tiles.maxOfOrNull { it.value } ?: 2
            return (maxTileValue.toFloat() / levelLimit).coerceIn(0f, 1f)
        }

    val emptySpaces: Int
        get() = (boardSize * boardSize) - tiles.size

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
        starsEarned = 0
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
        starsEarned = 0
    )

    // --- VALIDACIÓN ---

    fun validate(): BoardStateValidation {
        val errors = mutableListOf<String>()

        if (boardSize !in MIN_BOARD_SIZE..MAX_BOARD_SIZE) {
            errors.add("Tamaño de tablero inválido: $boardSize")
        }
        if (tiles.size > boardSize * boardSize) {
            errors.add("Demasiadas fichas: ${tiles.size}")
        }
        if (tiles.any { it.row >= boardSize || it.col >= boardSize }) {
            errors.add("Fichas fuera de los límites")
        }
        if (tiles.groupBy { it.row to it.col }.any { it.value.size > 1 }) {
            errors.add("Múltiples fichas en la misma posición")
        }
        if (currentLevel < 1) errors.add("Nivel inválido: $currentLevel")
        if (levelLimit < 2 || !isPowerOfTwo(levelLimit)) errors.add("Límite inválido: $levelLimit")
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

/**
 * Definición de los modos de juego y sus parámetros base.
 */
enum class GameMode(
    val initialTarget: Int,
    val timeLimit: Long?
) {
    CLASICO(64, null),
    DESAFIO(128, 180L),
    ZEN(2048, null),
    RAPIDO(64, 60L),
    TABLAS(0, 150L);

    // ✅ Propiedad para el nombre (ID de recurso)
    val nameResId: Int
        get() = when (this) {
            CLASICO -> R.string.mode_classic
            DESAFIO -> R.string.mode_challenge
            ZEN -> R.string.mode_zen
            RAPIDO -> R.string.mode_fast
            TABLAS -> R.string.mode_tables
        }

    // ✅ Propiedad para la descripción (ID de recurso)
    val descriptionResId: Int
        get() = when (this) {
            CLASICO -> R.string.mode_classic_desc
            DESAFIO -> R.string.mode_challenge_desc
            ZEN -> R.string.mode_zen_desc
            RAPIDO -> R.string.mode_fast_desc
            TABLAS -> R.string.mode_tables_desc
        }

    val color: Color
        get() = when (this) {
            CLASICO -> Color(0xFF81B29A)
            DESAFIO -> Color(0xFFE07A5F)
            ZEN -> Color(0xFF6C63FF)
            RAPIDO -> Color(0xFFF4A261)
            TABLAS -> Color(0xFF3D405B)
        }
}