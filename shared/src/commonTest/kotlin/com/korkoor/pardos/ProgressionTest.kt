package com.korkoor.pardos

import com.korkoor.pardos.domain.logic.ProgressionEngine
import com.korkoor.pardos.domain.model.BoardState
import com.korkoor.pardos.domain.model.GameMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProgressionTest {
    @Test fun campaignTargetsArePowersOfTwoAndNeverDecrease() {
        var prev = 0
        for (level in 1..300) {
            val t = ProgressionEngine.calculateTargetForLevel(level)
            assertTrue(t > 0 && (t and (t - 1)) == 0, "level $level target $t")
            assertTrue(t >= prev, "level $level decreased")
            prev = t
        }
    }

    @Test fun boardSizeStaysInSupportedRange() {
        for (level in 1..300) {
            val size = ProgressionEngine.calculateBoardSize(ProgressionEngine.calculateTargetForLevel(level))
            assertTrue(size in BoardState.MIN_BOARD_SIZE..BoardState.MAX_BOARD_SIZE)
        }
    }

    @Test fun starsDependOnTimeInMilliseconds() {
        assertEquals(3, ProgressionEngine.calculateStars(60_000L, 64))
        assertEquals(1, ProgressionEngine.calculateStars(600_000L, 64))
    }

    @Test fun timedModesUseMilliseconds() {
        val state = BoardState.initial(GameMode.DESAFIO)
        assertEquals(180_000L, state.maxTime)
        assertEquals(180_000L, state.elapsedTime)
    }
}
