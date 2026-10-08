package com.korkoor.pardos

import com.korkoor.pardos.domain.logic.DailyChallenge
import com.korkoor.pardos.domain.logic.Direction
import com.korkoor.pardos.domain.logic.GameEngine
import com.korkoor.pardos.domain.logic.ProgressionEngine
import com.korkoor.pardos.domain.model.TileModel
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DeterminismTest {
    /** Juega una partida corta con la secuencia de movimientos dada y devuelve el tablero resultante. */
    private fun play(seed: Long, moves: List<Direction>): List<Triple<Int, Int, Int>> {
        val rng = Random(seed)
        val engine = GameEngine(4, rng)
        var tiles = mutableListOf<TileModel>()
        repeat(3) {
            engine.spawnTileWithSpecificValue(tiles, ProgressionEngine.getNewTileValue(256, rng))?.let { tiles.add(it) }
        }
        for (m in moves) {
            val (moved, _) = engine.move(tiles, m)
            tiles = moved.toMutableList()
            engine.spawnTileWithSpecificValue(tiles, ProgressionEngine.getNewTileValue(256, rng))?.let { tiles.add(it) }
        }
        return tiles.map { Triple(it.row, it.col, it.value) }.sortedWith(compareBy({ it.first }, { it.second }))
    }

    private val moves = List(25) { Direction.entries[it % 4] }

    @Test fun sameSeedSameGame() {
        assertEquals(play(42L, moves), play(42L, moves))
    }

    @Test fun differentSeedDifferentGame() {
        assertNotEquals(play(1L, moves), play(2L, moves))
    }

    @Test fun dailyChallengeIsTheSameForEveryone() {
        assertEquals(DailyChallenge.forDay(20733), DailyChallenge.forDay(20733))
    }

    @Test fun dailyChallengeChangesAcrossDaysAndStaysInRange() {
        val configs = (20000..20060).map { DailyChallenge.forDay(it) }
        assertTrue(configs.map { it.seed }.toSet().size == configs.size)
        configs.forEach {
            assertTrue(it.boardSize in 4..5)
            assertTrue(it.target == 1024 || it.target == 2048)
            assertTrue(it.themeIndex in 0 until DailyChallenge.THEME_COUNT)
        }
        // A lo largo de 60 días aparecen ambas variantes de tamaño y de meta
        assertEquals(setOf(4, 5), configs.map { it.boardSize }.toSet())
        assertEquals(setOf(1024, 2048), configs.map { it.target }.toSet())
    }

    @Test fun dailyChallengeHasADifferentRuleEachWeekdayAndIsPlayable() {
        val days = (20000..20400).map { DailyChallenge.forDay(it) }
        // el tipo de reto sigue al día de la semana
        days.forEach { assertEquals(DailyChallenge.kindFor(it.day), it.spec.kind) }
        assertEquals(DailyChallenge.WEEKS.flatten().toSet(), days.map { it.spec.kind }.toSet())
        // los domingos siempre se descansa y cada semana trae retos distintos
        assertTrue(DailyChallenge.WEEKS.all { it.last() == com.korkoor.pardos.domain.level.LevelKind.ZEN })
        assertEquals(3, DailyChallenge.WEEKS.size)
        // el día 0 (1-1-1970) fue jueves
        assertEquals(3, DailyChallenge.weekday(0))
        // y todos los retos son válidos
        val problems = days.flatMap { com.korkoor.pardos.domain.level.LevelValidator.problems(it.spec) }
        assertTrue(problems.isEmpty(), problems.take(10).joinToString("; "))
        assertTrue(days.all { it.spec.boardSize == it.boardSize })
    }
}
