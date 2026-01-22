package com.example.pardos.domain.logic

import com.example.pardos.domain.model.TileModel
import java.util.UUID
import kotlin.random.Random

enum class Direction { UP, DOWN, LEFT, RIGHT }

class GameEngine(val boardSize: Int) {

    /**
     * Mueve y combina las fichas.
     * @param multiplier: El valor base (2 para clásico, 3-9 para tablas).
     */
    fun move(
        tiles: List<TileModel>,
        direction: Direction,
        multiplier: Int = 2 // 🔹 Valor por defecto 2 para no romper lo anterior
    ): Pair<List<TileModel>, Int> {
        val grouped = when (direction) {
            Direction.LEFT, Direction.RIGHT -> tiles.groupBy { it.row }
            Direction.UP, Direction.DOWN -> tiles.groupBy { it.col }
        }

        val resultTiles = mutableListOf<TileModel>()
        var totalScoreGained = 0

        for (i in 0 until boardSize) {
            val line = grouped[i] ?: emptyList()
            // Pasamos el multiplier a processLine
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

            // Lógica de fusión dinámica basada en el multiplier
            if (next != null && current.value == next.value) {
                val newValue = current.value * 2 // Sigue siendo x2 porque sumamos dos iguales (3+3=6, 6+6=12)
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
     * Genera una nueva ficha basada en la tabla actual.
     */
    fun spawnTile(
        currentTiles: List<TileModel>,
        fourProbability: Double = 0.1,
        multiplier: Int = 2 // 🔹 Inyectamos la base de la tabla
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
            // Si multiplier es 7, saldrá un 7 (o un 14 con poca probabilidad)
            val spawnValue = if (Math.random() < (1.0 - fourProbability)) multiplier else multiplier * 2

            TileModel(
                id = UUID.randomUUID().toString(),
                value = spawnValue,
                row = r,
                col = c
            )
        }
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