package com.korkoor.pardos

import com.korkoor.pardos.domain.level.GoalStats
import com.korkoor.pardos.domain.level.LevelBuilders
import com.korkoor.pardos.domain.level.LevelCatalog
import com.korkoor.pardos.domain.level.LevelGenerator
import com.korkoor.pardos.domain.level.LevelGoal
import com.korkoor.pardos.domain.level.LevelKind
import com.korkoor.pardos.domain.level.LevelRules
import com.korkoor.pardos.domain.level.LevelValidator
import com.korkoor.pardos.domain.level.SpawnRules
import com.korkoor.pardos.domain.level.SpawnStyle
import com.korkoor.pardos.domain.level.Storm
import com.korkoor.pardos.domain.level.StormRules
import com.korkoor.pardos.domain.level.StormStone
import com.korkoor.pardos.domain.level.Twist
import com.korkoor.pardos.domain.level.ladderRungs
import com.korkoor.pardos.domain.logic.Direction
import com.korkoor.pardos.domain.model.TileModel
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Los tipos de nivel nuevos: escalera, maratón, combo, pesadas, del revés y tormenta. */
class NewMechanicsTest {
    private fun t(v: Int, i: Int) = TileModel("t$i", v, i / 4, i % 4)

    // ---------------------------------------------------------------- metas

    @Test fun ladderNeedsEveryRungAtTheSameTime() {
        assertEquals(listOf(16, 32, 64, 128), ladderRungs(128, 4))
        val full = listOf(t(16, 0), t(32, 1), t(64, 2), t(128, 3))
        assertTrue(LevelRules.isGoalReached(LevelGoal.LADDER, 128, 4, full, 0))
        assertFalse(LevelRules.isGoalReached(LevelGoal.LADDER, 128, 4, full.drop(1), 0), "falta el 16")
        assertFalse(LevelRules.isGoalReached(LevelGoal.LADDER, 128, 4, listOf(t(16, 0), t(32, 1), t(64, 2), t(256, 3)), 0), "falta el 128 exacto")
        assertEquals("Reúne 16-32-64-128 a la vez", LevelBuilders.ladder(1, 128, 4).goalText())
    }

    @Test fun mergesAndComboUseTheRunStats() {
        assertFalse(LevelRules.isGoalReached(LevelGoal.MERGES, 50, 1, emptyList(), 0, GoalStats(merges = 49)))
        assertTrue(LevelRules.isGoalReached(LevelGoal.MERGES, 50, 1, emptyList(), 0, GoalStats(merges = 50)))
        var stats = GoalStats()
        stats = stats.after(2, 3)
        stats = stats.after(3, 3)
        assertEquals(1, stats.comboHits)
        assertEquals(3, stats.bestChain)
        assertEquals(5, stats.merges)
        assertTrue(LevelRules.isGoalReached(LevelGoal.COMBO, 3, 1, emptyList(), 0, stats))
        assertFalse(LevelRules.isGoalReached(LevelGoal.COMBO, 3, 2, emptyList(), 0, stats), "hay que repetirlo")
        assertEquals("Fusiona 3 pares de golpe, 2 veces", LevelBuilders.combo(1, 3, 2).goalText())
    }

    @Test fun progressStaysBetweenZeroAndOne() {
        for (goal in LevelGoal.entries) {
            val p = LevelRules.progress(goal, 64, 3, listOf(t(16, 0), t(32, 1)), 100, GoalStats(merges = 500, bestChain = 9, comboHits = 7))
            assertTrue(p in 0f..1f, "$goal → $p")
        }
        assertEquals(2f / 3f, LevelRules.progress(LevelGoal.LADDER, 64, 3, listOf(t(16, 0), t(32, 1)), 0, GoalStats()), 0.001f)
    }

    // ---------------------------------------------------------------- giros de controles

    @Test fun everyTwistIsABijection() {
        for (tw in Twist.entries) {
            val mapped = Direction.entries.map { tw.apply(it) }
            assertEquals(Direction.entries.toSet(), mapped.toSet(), "$tw no es biyectivo")
        }
        assertEquals(Direction.RIGHT, Twist.MIRROR_H.apply(Direction.LEFT))
        assertEquals(Direction.UP, Twist.MIRROR_H.apply(Direction.UP))
        assertEquals(Direction.DOWN, Twist.FLIP.apply(Direction.UP))
        assertEquals(Direction.RIGHT, Twist.SPIN.apply(Direction.UP))
        assertEquals(Direction.UP, Twist.NONE.apply(Direction.UP))
    }

    // ---------------------------------------------------------------- pesadas

    @Test fun heavySpawnsMostlyBigTiles() {
        val values = (0 until 4000).map { SpawnRules.pick(SpawnStyle.HEAVY, 256, Random(it)) }
        assertTrue(values.toSet().all { it in setOf(2, 4, 8) })
        assertTrue(values.count { it == 2 } < values.size * 0.12)
        assertTrue(SpawnRules.expected(SpawnStyle.HEAVY, 256) > 4.5)
    }

    // ---------------------------------------------------------------- tormenta

    @Test fun stormDropsOnEmptyCellsAndStonesExpire() {
        val storm = Storm(everyMoves = 3, lifeMoves = 4, maxStones = 2)
        val tiles = (0 until 8).map { t(2 shl (it % 4), it) }   // las 2 primeras filas ocupadas
        val rnd = Random(5)
        var stones = emptyList<StormStone>()
        for (move in 1..2) stones = StormRules.step(storm, move, stones, tiles, emptySet(), 4, rnd)
        assertTrue(stones.isEmpty(), "no cae nada antes del movimiento 3")
        stones = StormRules.step(storm, 3, stones, tiles, emptySet(), 4, rnd)
        assertEquals(1, stones.size)
        val occupied = tiles.map { it.row to it.col }.toSet()
        assertTrue(stones.single().cell !in occupied, "nunca cae sobre una ficha")
        stones = StormRules.step(storm, 6, stones, tiles, emptySet(), 4, rnd)
        assertTrue(stones.any { it.born == 3 }, "la piedra del movimiento 3 sigue viva en el 6 y cayó otra")
        stones = StormRules.step(storm, 7, stones, tiles, emptySet(), 4, rnd)
        assertTrue(stones.none { it.born == 3 }, "la piedra del movimiento 3 se fue en el 7")
    }

    @Test fun stormNeverChokesTheBoardAndRespectsTheMaximum() {
        val storm = Storm(everyMoves = 1, lifeMoves = 50, maxStones = 3)
        var stones = emptyList<StormStone>()
        val rnd = Random(9)
        for (move in 1..40) stones = StormRules.step(storm, move, stones, emptyList(), emptySet(), 4, rnd)
        assertEquals(3, stones.size)
        // con el tablero casi lleno no cae nada
        val crowded = (0 until 13).map { t(2 shl (it % 5), it) }
        val none = StormRules.step(Storm(1, 5, 4), 1, emptyList(), crowded, emptySet(), 4, Random(1))
        assertTrue(none.isEmpty())
    }

    // ---------------------------------------------------------------- calendario

    @Test fun newKindsArriveOnTheirChapterAndNotBefore() {
        for (kind in LevelGenerator.UNLOCK.keys) {
            val unlock = LevelGenerator.UNLOCK.getValue(kind)
            val before = (1 until unlock).flatMap { ch -> LevelGenerator.plan(ch) }
            // los niveles 12 y 14 del primer capítulo (tutorial escrito a mano) ya presentan Gemelas y Contrarreloj
            if (unlock > 2) assertTrue(kind !in before, "$kind aparece antes de su capítulo $unlock")
            assertTrue(kind in LevelGenerator.plan(unlock), "$kind debuta en el capítulo $unlock")
        }
    }

    @Test fun twistsAreIntroducedOneByOne() {
        assertTrue(LevelGenerator.twistsFor(9).isEmpty())
        assertEquals(listOf(Twist.MIRROR_H), LevelGenerator.twistsFor(10))
        assertEquals(4, LevelGenerator.twistsFor(34).size)
        for (id in 1..LevelCatalog.TOTAL_LEVELS) {
            val spec = LevelCatalog.spec(id)
            if (spec.twist != Twist.NONE) assertTrue(spec.twist in LevelGenerator.twistsFor((id - 1) / 20), "nivel $id usa un giro que aún no existe")
        }
    }

    @Test fun difficultyKeepsRisingAcrossTheCampaign() {
        assertTrue(LevelGenerator.sprintSlack(0) > LevelGenerator.sprintSlack(40))
        assertTrue(LevelGenerator.sprintSlack(40) > LevelGenerator.sprintSlack(100))
        assertTrue(LevelGenerator.clockSeconds(0) > LevelGenerator.clockSeconds(60))
        val early = LevelGenerator.stormFor(20, false)
        val late = LevelGenerator.stormFor(110, false)
        assertTrue(late.everyMoves < early.everyMoves && late.maxStones > early.maxStones)
        val m = LevelCatalog.all().filter { it.kind == LevelKind.MARATHON }
        assertTrue(m.last().goalValue > m.first().goalValue)
    }

    @Test fun theLongRunHasEveryNewMechanic() {
        val all = LevelCatalog.all()
        assertTrue(all.any { it.kind == LevelKind.STORM && it.storm != null })
        assertTrue(all.any { it.kind == LevelKind.TWIST && it.twist == Twist.SPIN })
        assertTrue(all.any { it.goal == LevelGoal.COMBO && it.goalValue >= 4 })
        assertTrue(all.any { it.goal == LevelGoal.LADDER && it.goalCount == 5 })
        assertTrue(all.any { it.spawn == SpawnStyle.HEAVY && it.boardSize == 6 })
        assertTrue(all.any { it.boardSize == 6 && it.stones.isNotEmpty() })
        assertTrue(all.flatMap { LevelValidator.problems(it) }.isEmpty())
    }
}
