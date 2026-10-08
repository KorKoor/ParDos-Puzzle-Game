package com.korkoor.pardos

import com.korkoor.pardos.domain.logic.Direction
import com.korkoor.pardos.domain.logic.GameEngine
import com.korkoor.pardos.domain.model.TileModel
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameEngineStonesTest {
    private fun t(r: Int, c: Int, v: Int) = TileModel("t${r}_${c}_$v", v, r, c)
    private fun cells(tiles: List<TileModel>) = tiles.map { Triple(it.row, it.col, it.value) }.sortedWith(compareBy({ it.first }, { it.second }))

    @Test fun aStoneStopsTilesAndSplitsTheRow() {
        val engine = GameEngine(4, Random(1), setOf(0 to 2))
        // [2][2][#][2] → izquierda: el 2+2 se fusiona, el 2 de detrás de la piedra se queda donde está
        val (tiles, score) = engine.move(listOf(t(0, 0, 2), t(0, 1, 2), t(0, 3, 2)), Direction.LEFT)
        assertEquals(listOf(Triple(0, 0, 4), Triple(0, 3, 2)), cells(tiles))
        assertEquals(4, score)
    }

    @Test fun tilesNeverMergeAcrossAStone() {
        val engine = GameEngine(4, Random(1), setOf(0 to 1))
        val (tiles, score) = engine.move(listOf(t(0, 0, 2), t(0, 2, 2)), Direction.LEFT)
        assertEquals(listOf(Triple(0, 0, 2), Triple(0, 2, 2)), cells(tiles))
        assertEquals(0, score)
    }

    @Test fun slidesRightAgainstAStone() {
        val engine = GameEngine(4, Random(1), setOf(1 to 3))
        val (tiles, _) = engine.move(listOf(t(1, 0, 8)), Direction.RIGHT)
        assertEquals(listOf(Triple(1, 2, 8)), cells(tiles))
    }

    @Test fun verticalMovesAlsoStopAtStones() {
        val engine = GameEngine(4, Random(1), setOf(2 to 1))
        val (down, _) = engine.move(listOf(t(0, 1, 4), t(1, 1, 4)), Direction.DOWN)
        assertEquals(listOf(Triple(1, 1, 8)), cells(down))
        val (up, _) = engine.move(listOf(t(3, 1, 4), t(1, 1, 2)), Direction.UP)
        assertEquals(listOf(Triple(0, 1, 2), Triple(3, 1, 4)), cells(up))
    }

    @Test fun newTilesNeverAppearOnStones() {
        val stones = setOf(0 to 0, 0 to 1, 0 to 2, 0 to 3, 1 to 0, 1 to 1)
        val engine = GameEngine(4, Random(7), stones)
        repeat(200) {
            val tile = engine.spawnTileWithSpecificValue(emptyList(), 2)!!
            assertFalse((tile.row to tile.col) in stones)
        }
    }

    @Test fun spawnReturnsNullWhenOnlyStonesAreLeft() {
        val engine = GameEngine(2, Random(1), setOf(0 to 0, 0 to 1))
        val tiles = listOf(t(1, 0, 2), t(1, 1, 4))
        assertEquals(null, engine.spawnTileWithSpecificValue(tiles, 2))
    }

    @Test fun gameOverCountsStonesAsFilledAndNeverMergeable() {
        val engine = GameEngine(3, Random(1), setOf(1 to 1))
        // 8 casillas libres, todas llenas y sin pares: fin de partida
        val full = listOf(
            t(0, 0, 2), t(0, 1, 4), t(0, 2, 2),
            t(1, 0, 4), t(1, 2, 4),
            t(2, 0, 2), t(2, 1, 4), t(2, 2, 2)
        )
        assertTrue(engine.isGameOver(full))
        // un hueco libre: aún se puede jugar
        assertFalse(engine.isGameOver(full.drop(1)))
        // dos fichas iguales vecinas: se puede jugar
        assertFalse(engine.isGameOver(full.map { if (it.row == 0 && it.col == 1) it.copy(value = 2) else it }))
    }

    @Test fun twoAdjacentStonesDoNotCountAsAMatch() {
        val engine = GameEngine(2, Random(1), setOf(0 to 0, 0 to 1))
        assertTrue(engine.isGameOver(listOf(t(1, 0, 2), t(1, 1, 4))))
    }

    @Test fun freeCellsDiscountStones() {
        assertEquals(14, GameEngine(4, Random(1), setOf(0 to 0, 3 to 3)).freeCells)
        assertEquals(16, GameEngine(4).freeCells)
    }

    /** Sin piedras, el motor nuevo se comporta exactamente como el original (se compara contra una copia del algoritmo anterior). */
    @Test fun withoutStonesItMatchesTheOriginalAlgorithm() {
        val rnd = Random(2024)
        repeat(300) {
            val size = 3 + rnd.nextInt(3)
            val engine = GameEngine(size, Random(1))
            val cellsAll = (0 until size).flatMap { r -> (0 until size).map { c -> r to c } }.shuffled(rnd)
            val count = rnd.nextInt(1, size * size + 1)
            val tiles = cellsAll.take(count).mapIndexed { i, (r, c) -> TileModel("x$i", 1 shl (1 + rnd.nextInt(3)), r, c) }
            for (dir in Direction.entries) {
                val (got, gotScore) = engine.move(tiles, dir)
                val (want, wantScore) = legacyMove(size, tiles, dir)
                assertEquals(wantScore, gotScore, "score $dir $tiles")
                assertEquals(want.sortedBy { it.id }, got.sortedBy { it.id }, "tiles $dir $tiles")
            }
        }
    }

    // ---- copia del algoritmo anterior a las piedras ----
    private fun legacyMove(boardSize: Int, tiles: List<TileModel>, direction: Direction): Pair<List<TileModel>, Int> {
        val grouped = when (direction) {
            Direction.LEFT, Direction.RIGHT -> tiles.groupBy { it.row }
            Direction.UP, Direction.DOWN -> tiles.groupBy { it.col }
        }
        val result = mutableListOf<TileModel>()
        var total = 0
        for (i in 0 until boardSize) {
            val line = grouped[i] ?: emptyList()
            val sorted = when (direction) {
                Direction.LEFT, Direction.UP -> line.sortedBy { if (direction == Direction.LEFT) it.col else it.row }
                Direction.RIGHT, Direction.DOWN -> line.sortedByDescending { if (direction == Direction.RIGHT) it.col else it.row }
            }
            val merged = mutableListOf<TileModel>()
            var gained = 0
            val skip = mutableSetOf<Int>()
            for (k in sorted.indices) {
                if (k in skip) continue
                val cur = sorted[k]
                val next = sorted.getOrNull(k + 1)
                if (next != null && cur.value == next.value) {
                    gained += cur.value * 2
                    merged.add(cur.copy(value = cur.value * 2, isMerged = true, isNew = false))
                    skip.add(k + 1)
                } else merged.add(cur.copy(isMerged = false, isNew = false))
            }
            val placed = merged.mapIndexed { index, tile ->
                val pos = when (direction) { Direction.LEFT, Direction.UP -> index; else -> boardSize - 1 - index }
                if (direction == Direction.LEFT || direction == Direction.RIGHT) tile.copy(col = pos, row = i) else tile.copy(row = pos, col = i)
            }
            result.addAll(placed)
            total += gained
        }
        return result to total
    }
}
