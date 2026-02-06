package com.korkoor.pardos.domain.logic

import com.korkoor.pardos.domain.model.TileModel
import java.util.UUID
import kotlin.random.Random

enum class Direction { UP, DOWN, LEFT, RIGHT }

class GameEngine(val boardSize: Int) {

    /**
     * Mueve y combina las fichas.
     */
    fun move(
        tiles: List<TileModel>,
        direction: Direction,
        multiplier: Int = 2
    ): Pair<List<TileModel>, Int> {
        val grouped = when (direction) {
            Direction.LEFT, Direction.RIGHT -> tiles.groupBy { it.row }
            Direction.UP, Direction.DOWN -> tiles.groupBy { it.col }
        }

        val resultTiles = mutableListOf<TileModel>()
        var totalScoreGained = 0

        for (i in 0 until boardSize) {
            val line = grouped[i] ?: emptyList()
            val (mergedLine, score) = processLine(line, direction, multiplier)

            val finalLine = mergedLine.map { tile ->
                when (direction) {
                    Direction.LEFT, Direction.RIGHT -> tile.copy(row = i)
                    Direction.UP, Direction.DOWN -> tile.copy(col = i)
                }
            }

            resultTiles.addAll(finalLine)
            totalScoreGained += score
        }

        return Pair(resultTiles, totalScoreGained)
    }

    private fun processLine(
        line: List<TileModel>,
        direction: Direction,
        multiplier: Int
    ): Pair<List<TileModel>, Int> {
        val sorted = when (direction) {
            Direction.LEFT, Direction.UP -> line.sortedBy { if (direction == Direction.LEFT) it.col else it.row }
            Direction.RIGHT, Direction.DOWN -> line.sortedByDescending { if (direction == Direction.RIGHT) it.col else it.row }
        }

        val result = mutableListOf<TileModel>()
        var scoreGained = 0
        val skipIndexes = mutableSetOf<Int>()

        for (i in sorted.indices) {
            if (i in skipIndexes) continue

            val current = sorted[i]
            val next = sorted.getOrNull(i + 1)

            if (next != null && current.value == next.value) {
                val newValue = current.value * 2
                scoreGained += newValue
                result.add(current.copy(value = newValue, isMerged = true))
                skipIndexes.add(i + 1)
            } else {
                result.add(current.copy(isMerged = false))
            }
        }

        return Pair(repositionLine(result, direction), scoreGained)
    }

    private fun repositionLine(line: List<TileModel>, direction: Direction): List<TileModel> {
        return line.mapIndexed { index, tile ->
            val newPos = when (direction) {
                Direction.LEFT, Direction.UP -> index
                Direction.RIGHT, Direction.DOWN -> boardSize - 1 - index
            }
            if (direction == Direction.LEFT || direction == Direction.RIGHT) {
                tile.copy(col = newPos)
            } else {
                tile.copy(row = newPos)
            }
        }
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
                if (!occupiedPositions.contains(r to c)) {
                    emptyPositions.add(r to c)
                }
            }
        }

        return emptyPositions.randomOrNull()?.let { (r, c) ->
            TileModel(
                id = UUID.randomUUID().toString(),
                value = value, // El valor ya viene balanceado (2, 4, 8, 16)
                row = r,
                col = c
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
        val spawnValue = if (Random.nextDouble() < (1.0 - fourProbability)) multiplier else multiplier * 2
        return spawnTileWithSpecificValue(currentTiles, spawnValue, multiplier)
    }

    fun isGameOver(tiles: List<TileModel>): Boolean {
        if (tiles.size < boardSize * boardSize) return false

        val grid = Array(boardSize) { IntArray(boardSize) { 0 } }
        tiles.forEach {
            if (it.row < boardSize && it.col < boardSize) {
                grid[it.row][it.col] = it.value
            }
        }

        for (r in 0 until boardSize) {
            for (c in 0 until boardSize) {
                val current = grid[r][c]
                if (c + 1 < boardSize && grid[r][c + 1] == current) return false
                if (r + 1 < boardSize && grid[r + 1][c] == current) return false
            }
        }
        return true
    }
}