package com.korkoor.pardos

import com.korkoor.pardos.domain.flow.AssistPolicy
import com.korkoor.pardos.domain.flow.FlowMeter
import com.korkoor.pardos.domain.flow.FlowTier
import com.korkoor.pardos.domain.flow.NearMiss
import com.korkoor.pardos.domain.flow.NextLevelTeaser
import com.korkoor.pardos.domain.flow.WinStreak
import com.korkoor.pardos.domain.level.GoalStats
import com.korkoor.pardos.domain.level.LevelGoal
import com.korkoor.pardos.domain.model.TileModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FlowTest {
    private fun t(v: Int, i: Int) = TileModel("t$i", v, i / 4, i % 4)

    @Test fun flowMeterTiersFollowTheStreak() {
        assertEquals(FlowTier.CALM, FlowMeter.tierOf(0))
        assertEquals(FlowTier.CALM, FlowMeter.tierOf(3))
        assertEquals(FlowTier.WARM, FlowMeter.tierOf(4))
        assertEquals(FlowTier.HOT, FlowMeter.tierOf(8))
        assertEquals(FlowTier.FLOW, FlowMeter.tierOf(14))
        assertEquals(FlowTier.FLOW, FlowMeter.tierOf(200))
    }

    @Test fun onlyMergingMovesBuildMomentum() {
        var s = 0
        repeat(5) { s = FlowMeter.next(s, merged = true) }
        assertEquals(5, s)
        assertEquals(0, FlowMeter.next(s, merged = false))
    }

    @Test fun tierUpIsAnnouncedOnce() {
        assertNull(FlowMeter.tierUp(2, 3))
        assertEquals(FlowTier.WARM, FlowMeter.tierUp(3, 4))
        assertNull(FlowMeter.tierUp(4, 5))
        assertEquals(FlowTier.FLOW, FlowMeter.tierUp(13, 14))
        assertNull(FlowMeter.tierUp(14, 0), "perder el ímpetu no se anuncia")
        assertTrue(FlowMeter.aura(0) == 0f && FlowMeter.aura(10) == 0.5f && FlowMeter.aura(99) == 1f)
        assertTrue(FlowTier.entries.zipWithNext().all { (a, b) -> a.coinBonusPct < b.coinBonusPct })
    }

    @Test fun winStreakCoolsInsteadOfBreaking() {
        assertEquals(6, WinStreak.afterWin(5))
        assertEquals(5, WinStreak.afterLoss(10))
        assertEquals(0, WinStreak.afterLoss(1))
        assertEquals(0, WinStreak.afterLoss(0))
    }

    @Test fun winStreakBonusIsCapped() {
        assertEquals(0, WinStreak.coinBonusPct(0))
        assertEquals(25, WinStreak.coinBonusPct(5))
        assertEquals(50, WinStreak.coinBonusPct(10))
        assertEquals(50, WinStreak.coinBonusPct(500))
    }

    @Test fun milestonesPayOnceAndGrow() {
        assertNull(WinStreak.milestoneAt(4))
        assertNotNull(WinStreak.milestoneAt(5))
        assertTrue(WinStreak.MILESTONES.zipWithNext().all { (a, b) -> a.streak < b.streak && a.gems < b.gems })
        assertEquals(5, WinStreak.nextMilestone(0)?.streak)
        assertEquals(10, WinStreak.nextMilestone(5)?.streak)
        assertNull(WinStreak.nextMilestone(80))
    }

    @Test fun assistGrowsGentlyAndStartsAtNothing() {
        assertEquals(0, AssistPolicy.tierFor(0))
        assertEquals(0, AssistPolicy.tierFor(1))
        assertEquals(1, AssistPolicy.tierFor(2))
        assertEquals(2, AssistPolicy.tierFor(4))
        assertEquals(3, AssistPolicy.tierFor(9))
        val a = (0..8).map { AssistPolicy.forAttempts(it) }
        assertTrue(a.zipWithNext().all { (x, y) -> y.limitFactor >= x.limitFactor && y.totalFreeUndos >= x.totalFreeUndos && y.luckyBoostPct >= x.luckyBoostPct })
        assertEquals(1.0, a[0].limitFactor)
        assertNull(a[0].message)
        assertNotNull(a[2].message)
        assertTrue(a.last().limitFactor <= 1.25, "la ayuda tiene techo")
    }

    @Test fun freeUndosAreNeverGrantedTwice() {
        val tier2 = AssistPolicy.forAttempts(4)
        assertEquals(2, AssistPolicy.newUndos(0, tier2))
        assertEquals(1, AssistPolicy.newUndos(1, tier2))
        assertEquals(0, AssistPolicy.newUndos(2, tier2))
        assertEquals(0, AssistPolicy.newUndos(5, tier2))
    }

    @Test fun nearMissNamesTheMissingStep() {
        val none = GoalStats()
        // a una fusión: dos fichas de 64 con meta 128
        assertNotNull(NearMiss.message(LevelGoal.REACH_TILE, 128, 1, listOf(t(64, 0), t(64, 1), t(2, 2)), 0, none, false))
        // lejos de la meta: nada que decir
        assertNull(NearMiss.message(LevelGoal.REACH_TILE, 1024, 1, listOf(t(8, 0), t(4, 1)), 0, none, false))
        assertNotNull(NearMiss.message(LevelGoal.SCORE, 1000, 1, emptyList(), 900, none, false))
        assertNull(NearMiss.message(LevelGoal.SCORE, 1000, 1, emptyList(), 300, none, false))
        assertEquals("¡Te faltaron solo 12 fusiones!", NearMiss.message(LevelGoal.MERGES, 100, 1, emptyList(), 0, GoalStats(merges = 88), true))
        assertNull(NearMiss.message(LevelGoal.MERGES, 100, 1, emptyList(), 0, GoalStats(merges = 10), true))
        assertEquals("¡Te faltó solo el peldaño 16!", NearMiss.message(LevelGoal.LADDER, 64, 3, listOf(t(32, 0), t(64, 1)), 0, none, false))
        assertNotNull(NearMiss.message(LevelGoal.COMBO, 4, 1, emptyList(), 0, GoalStats(bestChain = 3), true))
        assertNull(NearMiss.message(LevelGoal.COMBO, 4, 1, emptyList(), 0, GoalStats(bestChain = 1), true))
    }

    @Test fun teaserLooksAtTheNextLevelAndTheChest() {
        val t = NextLevelTeaser.after(9)
        assertEquals(10, t.nextLevel)
        assertTrue(t.isBoss)
        assertEquals(11, t.levelsToChest, "el cofre es el nivel 20")
        assertEquals(0, NextLevelTeaser.after(20).levelsToChest)
        assertEquals(19, NextLevelTeaser.after(21).levelsToChest)
    }
}
