package com.korkoor.pardos.domain.logic

import com.korkoor.pardos.domain.model.TileModel
import kotlin.random.Random

enum class Direction { UP, DOWN, LEFT, RIGHT }

/**
 * Motor del tablero. Con una [random] con semilla, dos partidas con los mismos movimientos
 * generan exactamente las mismas fichas (retos diarios iguales para todos, duelos, repeticiones).
 *
 * [blocked] son las piedras del nivel, como pares (fila, columna): no se mueven, no reciben fichas nuevas y parten cada
 * fila o columna en tramos independientes (una ficha no puede saltar una piedra ni fusionarse al otro lado).
 */
class GameEngine(
    val boardSize: Int,
    private val random: Random = Random.Default,
    val blocked: Set<Pair<Int, Int>> = emptySet()
) {

    /** Casillas que pueden ocupar fichas. */
    val freeCells: Int get() = boardSize * boardSize - blocked.count { (r, c) -> r in 0 until boardSize && c in 0 until boardSize }

    /**
     * Mueve y combina las fichas.
     */
    fun move(
        tiles: List<TileModel>,
        direction: Direction,
        multiplier: Int = 2
    ): Pair<List<TileModel>, Int> {
        val byCell = HashMap<Pair<Int, Int>, TileModel>(tiles.size * 2)
        tiles.forEach { byCell[it.row to it.col] = it }

        val resultTiles = mutableListOf<TileModel>()
        var totalScoreGained = 0

        for (lineIndex in 0 until boardSize) {
            // Casillas de la línea, ordenadas desde el lado hacia el que se empuja (donde se apilan las fichas)
            val cells = lineCells(lineIndex, direction)
            var segment = ArrayList<Pair<Int, Int>>(boardSize)
            for (cell in cells) {
                if (cell in blocked) {
                    totalScoreGained += slideSegment(segment, byCell, resultTiles)
                    segment = ArrayList(boardSize)
                } else {
                    segment.add(cell)
                }
            }
            totalScoreGained += slideSegment(segment, byCell, resultTiles)
        }

        return Pair(resultTiles, totalScoreGained)
    }

    /** Casillas de la fila/columna [index], con la primera siendo la del borde hacia el que se empuja. */
    private fun lineCells(index: Int, direction: Direction): List<Pair<Int, Int>> = when (direction) {
        Direction.LEFT -> (0 until boardSize).map { index to it }
        Direction.RIGHT -> (boardSize - 1 downTo 0).map { index to it }
        Direction.UP -> (0 until boardSize).map { it to index }
        Direction.DOWN -> (boardSize - 1 downTo 0).map { it to index }
    }

    /** Desliza y fusiona un tramo sin piedras. Devuelve los puntos ganados y añade las fichas resultantes a [out]. */
    private fun slideSegment(
        segment: List<Pair<Int, Int>>,
        byCell: Map<Pair<Int, Int>, TileModel>,
        out: MutableList<TileModel>
    ): Int {
        if (segment.isEmpty()) return 0
        val line = segment.mapNotNull { byCell[it] }
        var score = 0
        var slot = 0
        var i = 0
        while (i < line.size) {
            val current = line[i]
            val next = line.getOrNull(i + 1)
            val (row, col) = segment[slot++]
            if (next != null && current.value == next.value) {
                val newValue = current.value * 2
                score += newValue
                out.add(current.copy(value = newValue, row = row, col = col, isMerged = true, isNew = false))
                i += 2
            } else {
                out.add(current.copy(row = row, col = col, isMerged = false, isNew = false))
                i += 1
            }
        }
        return score
    }

    /**
     * 🔥 SÚPER BALANCE: Genera una ficha con un valor específico decidido por el motor.
     * Esta función resuelve los errores de "Unresolved reference" en el GameViewModel.
     */
    fun spawnTileWithSpecificValue(
        currentTiles: List<TileModel>,
        value: Int,
        multiplier: Int = 2
    ): TileModel? {
        val occupiedPositions = currentTiles.map { it.row to it.col }.toSet()
        val emptyPositions = mutableListOf<Pair<Int, Int>>()

        for (r in 0 until boardSize) {
            for (c in 0 until boardSize) {
                val cell = r to c
                if (cell !in occupiedPositions && cell !in blocked) {
                    emptyPositions.add(cell)
                }
            }
        }

        return emptyPositions.randomOrNull(random)?.let { (r, c) ->
            TileModel(
                id = TileModel.generateId(),
                value = value, // El valor ya viene balanceado (2, 4, 8, 16)
                row = r,
                col = c,
                isNew = true
            )
        }
    }

    /**
     * Mantenemos spawnTile original por compatibilidad, pero redirigiendo a la lógica base.
     */
    fun spawnTile(
        currentTiles: List<TileModel>,
        fourProbability: Double = 0.1,
        multiplier: Int = 2
    ): TileModel? {
        val spawnValue = if (random.nextDouble() < (1.0 - fourProbability)) multiplier else multiplier * 2
        return spawnTileWithSpecificValue(currentTiles, spawnValue, multiplier)
    }

    fun isGameOver(tiles: List<TileModel>): Boolean {
        if (tiles.size < freeCells) return false

        // Las piedras valen 0 (las fichas siempre son > 0): nunca se emparejan con nada
        val grid = Array(boardSize) { IntArray(boardSize) { 0 } }
        tiles.forEach {
            if (it.row < boardSize && it.col < boardSize) {
                grid[it.row][it.col] = it.value
            }
        }

        for (r in 0 until boardSize) {
            for (c in 0 until boardSize) {
                val current = grid[r][c]
                if (current == 0) continue
                if (c + 1 < boardSize && grid[r][c + 1] == current) return false
                if (r + 1 < boardSize && grid[r + 1][c] == current) return false
            }
        }
        return true
    }
}
