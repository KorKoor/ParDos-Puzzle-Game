package com.korkoor.pardos

import com.korkoor.pardos.domain.rewards.ChapterRewards
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChapterRewardsTest {
    @Test fun chapterBoundaries() {
        assertEquals(0, ChapterRewards.chapterOf(1))
        assertEquals(0, ChapterRewards.chapterOf(20))
        assertEquals(1, ChapterRewards.chapterOf(21))
        assertEquals(20, ChapterRewards.lastLevelOf(0))
        assertEquals(40, ChapterRewards.lastLevelOf(1))
    }

    @Test fun rewardsGrowAndAlwaysGiveGems() {
        var prev = 0
        for (c in 0..30) {
            val r = ChapterRewards.forChapter(c)
            assertTrue(r.coins > prev)
            assertTrue(r.gems >= 2)
            prev = r.coins
        }
    }
}
