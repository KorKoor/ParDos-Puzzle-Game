package com.korkoor.pardos

import com.korkoor.pardos.domain.logic.Direction
import com.korkoor.pardos.domain.logic.GameEngine
import com.korkoor.pardos.domain.model.TileModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameEngineTest {
    private val engine = GameEngine(4)
    private fun t(r: Int, c: Int, v: Int) = TileModel(TileModel.generateId(), v, r, c)

    @Test fun mergesEqualNeighboursLeft() {
        val (tiles, score) = engine.move(listOf(t(0, 0, 2), t(0, 1, 2)), Direction.LEFT)
        assertEquals(1, tiles.size)
        assertEquals(4, tiles[0].value)
        assertEquals(0, tiles[0].col)
        assertEquals(4, score)
    }

    @Test fun mergesOncePerMove() {
        val (tiles, _) = engine.move(listOf(t(0, 0, 2), t(0, 1, 2), t(0, 2, 4)), Direction.LEFT)
        assertEquals(listOf(4, 4), tiles.sortedBy { it.col }.map { it.value })
    }

    @Test fun slidesRight() {
        val (tiles, _) = engine.move(listOf(t(1, 0, 8)), Direction.RIGHT)
        assertEquals(3, tiles[0].col)
    }

    @Test fun gameOverOnlyWhenFullAndNoMerges() {
        val full = (0 until 4).flatMap { r -> (0 until 4).map { c -> t(r, c, if ((r + c) % 2 == 0) 2 else 4) } }
        assertTrue(engine.isGameOver(full))
        assertFalse(engine.isGameOver(full.drop(1)))
    }
}
