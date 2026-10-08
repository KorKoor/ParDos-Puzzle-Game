package com.korkoor.pardos

import com.korkoor.pardos.domain.model.DailyMissionPlan
import com.korkoor.pardos.domain.model.PerfectDays
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DailyMissionPlanTest {
    @Test fun everyDayHasOneOfEachTierWithDifferentTypes() {
        (1..400).forEach { day ->
            val plan = DailyMissionPlan.forDay(day)
            assertEquals(DailyMissionPlan.PER_DAY, plan.size, "día $day")
            assertEquals(listOf(DailyMissionPlan.Tier.LIGHT, DailyMissionPlan.Tier.MEDIUM, DailyMissionPlan.Tier.HARD), plan.map { DailyMissionPlan.tierOf(it) })
            assertEquals(3, plan.map { it.type }.toSet().size, "tipos repetidos el día $day")
            assertEquals(3, plan.map { it.id }.toSet().size)
        }
    }

    @Test fun sameDaySameMissions() {
        assertEquals(DailyMissionPlan.forDay(20_000).map { it.id }, DailyMissionPlan.forDay(20_000).map { it.id })
    }

    @Test fun differentDaysVary() {
        val sets = (1..60).map { DailyMissionPlan.forDay(it).map { m -> m.id } }.toSet()
        assertTrue(sets.size > 30, "poca variedad: ${sets.size}")
    }

    @Test fun perfectStreakChainsAndBreaks() {
        assertEquals(1, PerfectDays.next(null, 0, 100))
        assertEquals(2, PerfectDays.next(99, 1, 100))
        assertEquals(1, PerfectDays.next(97, 5, 100), "se rompió")
        assertEquals(4, PerfectDays.next(100, 4, 100), "no cuenta dos veces el mismo día")
    }

    @Test fun milestonesAndAliveRule() {
        assertEquals(3, PerfectDays.milestoneFor(3)?.gems)
        assertNull(PerfectDays.milestoneFor(4))
        assertEquals(7, PerfectDays.nextMilestone(3)?.days)
        assertNull(PerfectDays.nextMilestone(30))
        assertEquals(5, PerfectDays.alive(99, 5, 100))
        assertEquals(5, PerfectDays.alive(100, 5, 100))
        assertEquals(0, PerfectDays.alive(97, 5, 100))
        assertTrue(PerfectDays.milestones.zipWithNext().all { (a, b) -> b.gems > a.gems && b.days > a.days })
    }
}
