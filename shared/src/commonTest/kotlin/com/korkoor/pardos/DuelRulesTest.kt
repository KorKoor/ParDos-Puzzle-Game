package com.korkoor.pardos

import com.korkoor.pardos.domain.logic.DuelRules
import com.korkoor.pardos.domain.logic.DuelWinner
import kotlin.test.Test
import kotlin.test.assertEquals

class DuelRulesTest {
    @Test fun higherScoreWins() {
        assertEquals(DuelWinner.PLAYER_1, DuelRules.winner(500, 120))
        assertEquals(DuelWinner.PLAYER_2, DuelRules.winner(0, 8))
    }

    @Test fun equalScoresAreATie() {
        assertEquals(DuelWinner.TIE, DuelRules.winner(0, 0))
        assertEquals(DuelWinner.TIE, DuelRules.winner(256, 256))
    }

    @Test fun marginIsSymmetric() {
        assertEquals(380, DuelRules.margin(500, 120))
        assertEquals(380, DuelRules.margin(120, 500))
        assertEquals(0, DuelRules.margin(7, 7))
    }
}
