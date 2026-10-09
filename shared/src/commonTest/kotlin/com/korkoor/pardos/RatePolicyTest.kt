package com.korkoor.pardos

import com.korkoor.pardos.domain.shop.RatePolicy
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RatePolicyTest {
    private val happy = RatePolicy.State(
        campaignLevel = 30, installDays = 5, daysSinceLastAsk = null, asks = 0,
        winStreak = 4, threeStarWin = true, bossWin = false, adNow = false
    )

    @Test fun asksAtAHappyMoment() = assertTrue(RatePolicy.shouldAsk(happy))

    @Test fun aBossWinIsAHappyMomentEvenWithoutAStreak() {
        assertTrue(RatePolicy.shouldAsk(happy.copy(winStreak = 0, threeStarWin = false, bossWin = true)))
        assertFalse(RatePolicy.shouldAsk(happy.copy(winStreak = 0, threeStarWin = false, bossWin = false)))
    }

    @Test fun needsThreeStarsAndAStreakOtherwise() {
        assertFalse(RatePolicy.shouldAsk(happy.copy(threeStarWin = false)))
        assertFalse(RatePolicy.shouldAsk(happy.copy(winStreak = RatePolicy.MIN_WIN_STREAK - 1)))
    }

    @Test fun neverInTheFirstSessionNorTheFirstLevels() {
        assertFalse(RatePolicy.shouldAsk(happy.copy(installDays = RatePolicy.MIN_INSTALL_DAYS - 1)))
        assertFalse(RatePolicy.shouldAsk(happy.copy(campaignLevel = RatePolicy.MIN_CAMPAIGN_LEVEL - 1)))
    }

    @Test fun rarelyAndNotMoreThanAFewTimesInTotal() {
        assertFalse(RatePolicy.shouldAsk(happy.copy(daysSinceLastAsk = RatePolicy.MIN_DAYS_BETWEEN_ASKS - 1, asks = 1)))
        assertTrue(RatePolicy.shouldAsk(happy.copy(daysSinceLastAsk = RatePolicy.MIN_DAYS_BETWEEN_ASKS, asks = 1)))
        assertFalse(RatePolicy.shouldAsk(happy.copy(daysSinceLastAsk = 400, asks = RatePolicy.MAX_ASKS)))
    }

    @Test fun neverGluedToAnAd() = assertFalse(RatePolicy.shouldAsk(happy.copy(adNow = true)))
}
