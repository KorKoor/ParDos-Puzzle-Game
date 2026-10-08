package com.korkoor.pardos

import com.korkoor.pardos.domain.economy.Economy
import com.korkoor.pardos.domain.retention.GoalInput
import com.korkoor.pardos.domain.retention.GoalKind
import com.korkoor.pardos.domain.retention.NextGoals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NextGoalTest {
    private val waiting = 3 * 60 * 60_000L
    private fun input(
        chest: Long = waiting,
        leagueToGo: Int = 0, weeklyProgress: Int = 0, seasonLeft: Int = 0, dailyDone: Boolean = true
    ) = GoalInput(
        chestRemainingMs = chest,
        leagueNextName = "Plata", leagueStarsToPromote = leagueToGo, leaguePromoteTarget = 20,
        weeklyTitle = "Gana 10 niveles", weeklyProgress = weeklyProgress, weeklyTarget = 10,
        seasonNextTier = 5, seasonPointsLeft = seasonLeft, seasonPointsPerTier = 100,
        dailyChallengeDone = dailyDone
    )

    @Test fun readyChestAlwaysWins() {
        val g = NextGoals.pick(input(chest = 0, leagueToGo = 3, weeklyProgress = 9, seasonLeft = 10, dailyDone = false))!!
        assertEquals(GoalKind.CHEST_READY, g.kind)
    }

    @Test fun leagueNearBeatsWeeklyAndSeason() {
        val g = NextGoals.pick(input(leagueToGo = 5, weeklyProgress = 9, seasonLeft = 10))!!
        assertEquals(GoalKind.LEAGUE, g.kind)
        assertEquals(0.75f, g.progress)
    }

    @Test fun farLeagueIsIgnored() {
        assertTrue(NextGoals.pick(input(leagueToGo = 15))!!.kind != GoalKind.LEAGUE)
        assertTrue(NextGoals.pick(input(leagueToGo = 0))!!.kind != GoalKind.LEAGUE)
    }

    @Test fun weeklyNeedsHalfwayAndNotDone() {
        assertEquals(GoalKind.WEEKLY, NextGoals.pick(input(weeklyProgress = 5))!!.kind)
        assertTrue(NextGoals.pick(input(weeklyProgress = 4))!!.kind != GoalKind.WEEKLY)
        assertTrue(NextGoals.pick(input(weeklyProgress = 10))!!.kind != GoalKind.WEEKLY)
    }

    @Test fun seasonTierWhenCloseOnly() {
        val g = NextGoals.pick(input(seasonLeft = 30))!!
        assertEquals(GoalKind.SEASON, g.kind)
        assertEquals(0.7f, g.progress)
        assertTrue(NextGoals.pick(input(seasonLeft = 80))!!.kind != GoalKind.SEASON)
    }

    @Test fun dailyChallengeThenChestTimer() {
        assertEquals(GoalKind.DAILY_CHALLENGE, NextGoals.pick(input(dailyDone = false))!!.kind)
        val g = NextGoals.pick(input(dailyDone = true))!!
        assertEquals(GoalKind.CHEST_TIMER, g.kind)
        assertEquals("En 3 h 0 min", g.detail)
    }

    @Test fun chestTimerProgressAndMinutes() {
        val almost = NextGoals.pick(input(chest = 90_000L))!!
        assertEquals("En 1 min", almost.detail)
        assertTrue(almost.progress > 0.99f)
        val fresh = NextGoals.pick(input(chest = Economy.FREE_CHEST_COOLDOWN_MS))!!
        assertEquals(0f, fresh.progress)
    }

    @Test fun alwaysReturnsSomething() {
        assertTrue(NextGoals.pick(GoalInput(chestRemainingMs = 1L)) != null)
    }
}
