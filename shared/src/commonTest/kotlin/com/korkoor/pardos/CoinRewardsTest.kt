package com.korkoor.pardos

import com.korkoor.pardos.domain.rewards.CoinRewards
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoinRewardsTest {
    @Test fun moreStarsGiveMoreCoins() {
        assertTrue(CoinRewards.forLevelWin(3, false) > CoinRewards.forLevelWin(1, false))
    }

    @Test fun firstClearAddsBonus() {
        assertEquals(CoinRewards.forLevelWin(2, false) + 20, CoinRewards.forLevelWin(2, true))
    }

    @Test fun starsAreClamped() {
        assertEquals(CoinRewards.forLevelWin(1, false), CoinRewards.forLevelWin(0, false))
        assertEquals(CoinRewards.forLevelWin(3, false), CoinRewards.forLevelWin(9, false))
    }

    @Test fun missionRewardHasFloor() {
        assertEquals(5, CoinRewards.forMission(4))
        assertEquals(50, CoinRewards.forMission(100))
    }
}
