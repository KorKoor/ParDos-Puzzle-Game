package com.korkoor.pardos

import com.korkoor.pardos.domain.rewards.StreakCalculator
import com.korkoor.pardos.domain.rewards.StreakChange
import com.korkoor.pardos.domain.rewards.StreakRepair
import com.korkoor.pardos.domain.rewards.StreakState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StreakRepairTest {
    private val today = 20_000
    private fun state(streak: Int, missed: Int) = StreakState(streak, best = streak, lastDay = today - 1 - missed)

    @Test fun offeredOnlyForWorthwhileRecentLosses() {
        assertTrue(StreakRepair.canOffer(state(5, missed = 1), today))
        assertTrue(StreakRepair.canOffer(state(5, missed = 2), today))
        assertFalse(StreakRepair.canOffer(state(5, missed = 3), today), "demasiado tiempo")
        assertFalse(StreakRepair.canOffer(state(2, missed = 1), today), "racha corta")
        assertFalse(StreakRepair.canOffer(state(5, missed = 0), today), "no se perdió: es el día siguiente")
        assertFalse(StreakRepair.canOffer(StreakState(), today), "sin historial")
    }

    @Test fun offerMatchesTheCalculatorReset() {
        // Siempre que se ofrece, el calculador (sin escudos) reinicia la racha
        for (missed in 1..2) {
            val s = state(6, missed)
            assertTrue(StreakRepair.canOffer(s, today))
            assertEquals(StreakChange.RESET, StreakCalculator.onOpen(s, today, freezes = 0).change)
        }
    }

    @Test fun gemCostScalesAndIsClamped() {
        assertEquals(StreakRepair.MIN_GEMS, StreakRepair.gemCost(3))
        assertEquals(20, StreakRepair.gemCost(10))
        assertEquals(StreakRepair.MAX_GEMS, StreakRepair.gemCost(500))
    }

    @Test fun adCooldownIsOnePerWeek() {
        assertTrue(StreakRepair.adAvailable(0, today))
        assertFalse(StreakRepair.adAvailable(today - 3, today))
        assertTrue(StreakRepair.adAvailable(today - StreakRepair.AD_COOLDOWN_DAYS, today))
    }

    @Test fun repairContinuesTheStreakAndKeepsBest() {
        val before = StreakState(streak = 9, best = 12, lastDay = today - 3)
        val after = StreakRepair.repaired(before, today)
        assertEquals(10, after.streak)
        assertEquals(12, after.best)
        assertEquals(today, after.lastDay)
        assertEquals(10, StreakRepair.repaired(StreakState(9, 9, today - 2), today).best.coerceAtMost(10))
    }
}
