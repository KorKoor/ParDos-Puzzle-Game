package com.korkoor.pardos

import com.korkoor.pardos.domain.flow.FlowTier
import com.korkoor.pardos.domain.flow.Frenzy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FrenzyTest {
    @Test fun onlyTheMoveThatCrossesFlowTriggersIt() {
        val flow = FlowTier.FLOW.minStreak
        assertTrue(Frenzy.triggeredBy(flow - 1, flow))
        assertFalse(Frenzy.triggeredBy(flow, flow + 1), "ya estaba en FLOW")
        assertFalse(Frenzy.triggeredBy(flow - 2, flow - 1))
        assertFalse(Frenzy.triggeredBy(5, 0))
    }

    @Test fun doublesPointsForExactlyThreeMoves() {
        var left = Frenzy.MOVES
        val gains = mutableListOf<Int>()
        repeat(5) {
            gains += Frenzy.score(10, left)
            left = Frenzy.afterMove(left)
        }
        assertEquals(listOf(20, 20, 20, 10, 10), gains)
        assertEquals(0, Frenzy.afterMove(0))
    }
}
