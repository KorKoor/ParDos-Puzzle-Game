package com.korkoor.pardos

import com.korkoor.pardos.domain.logic.RaceRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RaceRulesTest {
    @Test fun stagesGetHarderAndNeverRegress() {
        var prevTarget = 0
        var prevSize = 0
        for (n in 1..15) {
            val s = RaceRules.stage(n)
            assertTrue(s.target >= prevTarget, "meta baja en etapa $n")
            assertTrue(s.boardSize >= prevSize, "tablero baja en etapa $n")
            assertTrue(s.target and (s.target - 1) == 0, "meta no es potencia de 2: ${s.target}")
            assertTrue(s.boardSize in 3..6)
            prevTarget = s.target
            prevSize = s.boardSize
        }
    }

    @Test fun lateStagesStayAtTheCap() {
        assertEquals(RaceRules.stage(9), RaceRules.stage(9).copy(number = 9))
        assertEquals(RaceRules.stage(9).target, RaceRules.stage(40).target)
    }

    @Test fun invalidStageNumberFallsBackToFirst() {
        assertEquals(1, RaceRules.stage(0).number)
        assertEquals(3, RaceRules.stage(-5).boardSize)
    }

    @Test fun bonusGrowsButIsCapped() {
        assertTrue(RaceRules.timeBonusMs(1) < RaceRules.timeBonusMs(5))
        assertEquals(30_000L, RaceRules.timeBonusMs(50))
    }

    @Test fun timeNeverExceedsTheMaximum() {
        assertEquals(RaceRules.MAX_TIME_MS, RaceRules.timeAfterStage(170_000L, 6))
        assertEquals(10_000L + RaceRules.timeBonusMs(2), RaceRules.timeAfterStage(10_000L, 2))
    }

    @Test fun coinsScaleWithStagesAndNeverGoNegative() {
        assertEquals(0, RaceRules.coinsFor(-3))
        assertEquals(100, RaceRules.coinsFor(5))
    }
}
