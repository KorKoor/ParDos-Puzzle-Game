package com.korkoor.pardos

import com.korkoor.pardos.domain.flow.FlowTier
import com.korkoor.pardos.domain.flow.QuestResult
import com.korkoor.pardos.domain.flow.SideQuest
import com.korkoor.pardos.domain.flow.SideQuests
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SideQuestsTest {
    private val base = QuestResult(stars = 2, moveLimit = 40, moveCount = 25, emptyCells = 4, freeCells = 16, peakTier = FlowTier.WARM, timeMs = 120_000)

    @Test fun noQuestsInTheLearningLevelsOrBosses() {
        assertNull(SideQuests.forLevel(1, true, false))
        assertNull(SideQuests.forLevel(SideQuests.MIN_LEVEL - 1, true, false))
        assertNull(SideQuests.forLevel(10, true, true))
        assertNotNull(SideQuests.forLevel(SideQuests.MIN_LEVEL, true, false))
    }

    @Test fun theSameLevelAlwaysGetsTheSameQuest() {
        for (lv in 4..200) assertEquals(SideQuests.forLevel(lv, true, false), SideQuests.forLevel(lv, true, false))
    }

    @Test fun spareMovesOnlyWhenTheLevelHasALimit() {
        for (lv in 4..400) assertTrue(SideQuests.forLevel(lv, false, false) != SideQuest.SPARE)
    }

    @Test fun variedAcrossLevels() {
        val seen = (4..60).mapNotNull { SideQuests.forLevel(it, true, false) }.toSet()
        assertEquals(SideQuest.entries.toSet(), seen)
    }

    @Test fun eachQuestChecksItsOwnCondition() {
        assertFalse(SideQuests.achieved(SideQuest.FLOW, base))
        assertTrue(SideQuests.achieved(SideQuest.FLOW, base.copy(peakTier = FlowTier.HOT)))
        assertFalse(SideQuests.achieved(SideQuest.CLEAN, base))
        assertTrue(SideQuests.achieved(SideQuest.CLEAN, base.copy(emptyCells = 8)))
        assertTrue(SideQuests.achieved(SideQuest.SPARE, base.copy(moveCount = 30)))
        assertFalse(SideQuests.achieved(SideQuest.SPARE, base.copy(moveCount = 31)))
        assertFalse(SideQuests.achieved(SideQuest.SPARE, base.copy(moveLimit = null)))
        assertFalse(SideQuests.achieved(SideQuest.PERFECT, base))
        assertTrue(SideQuests.achieved(SideQuest.PERFECT, base.copy(stars = 3)))
        assertFalse(SideQuests.achieved(SideQuest.SWIFT, base))
        assertTrue(SideQuests.achieved(SideQuest.SWIFT, base.copy(timeMs = 60_000)))
    }

    @Test fun rewardsAreSmall() {
        assertTrue(SideQuest.entries.all { it.coins in 10..30 })
    }
}
