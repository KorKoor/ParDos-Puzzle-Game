package com.korkoor.pardos

import com.korkoor.pardos.domain.rewards.DailyRewards
import com.korkoor.pardos.domain.rewards.StreakCalculator
import com.korkoor.pardos.domain.rewards.StreakChange
import com.korkoor.pardos.domain.rewards.StreakState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RewardsTest {
    @Test fun cycleRepeatsAndSeventhIsChest() {
        assertEquals(1, DailyRewards.dayInCycle(1))
        assertEquals(7, DailyRewards.dayInCycle(7))
        assertEquals(1, DailyRewards.dayInCycle(8))
        assertTrue(DailyRewards.rewardForStreak(7).isChest)
        assertTrue(DailyRewards.rewardForStreak(14).isChest)
    }

    @Test fun rewardsNeverDecreaseWithinACycle() {
        val coins = DailyRewards.cycle.map { it.coins }
        assertEquals(coins.sorted(), coins)
    }

    @Test fun firstOpenStartsStreak() {
        val r = StreakCalculator.onOpen(StreakState(), today = 20000)
        assertEquals(StreakChange.STARTED, r.change)
        assertEquals(1, r.state.streak)
    }

    @Test fun sameDayDoesNothing() {
        val s = StreakState(streak = 3, best = 5, lastDay = 20000)
        val r = StreakCalculator.onOpen(s, today = 20000)
        assertEquals(StreakChange.NONE, r.change)
        assertEquals(s, r.state)
    }

    @Test fun nextDayContinuesAndUpdatesBest() {
        val r = StreakCalculator.onOpen(StreakState(4, 4, 20000), today = 20001)
        assertEquals(StreakChange.CONTINUED, r.change)
        assertEquals(5, r.state.streak)
        assertEquals(5, r.state.best)
    }

    @Test fun missedDayResetsButKeepsBest() {
        val r = StreakCalculator.onOpen(StreakState(9, 9, 20000), today = 20003)
        assertEquals(StreakChange.RESET, r.change)
        assertEquals(1, r.state.streak)
        assertEquals(9, r.state.best)
    }

    @Test fun freezeCoversExactlyOneMissedDay() {
        val saved = StreakCalculator.onOpen(StreakState(6, 6, 20000), today = 20002, freezes = 1)
        assertEquals(StreakChange.SAVED_BY_FREEZE, saved.change)
        assertEquals(7, saved.state.streak)
        assertEquals(0, saved.freezesLeft)

        val tooLate = StreakCalculator.onOpen(StreakState(6, 6, 20000), today = 20003, freezes = 1)
        assertEquals(StreakChange.RESET, tooLate.change)
        assertEquals(1, tooLate.freezesLeft)
    }
}
