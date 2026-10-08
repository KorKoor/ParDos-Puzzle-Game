package com.korkoor.pardos

import com.korkoor.pardos.domain.events.EventCalendar
import com.korkoor.pardos.domain.events.EventType
import com.korkoor.pardos.domain.retention.Civil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventCalendarTest {
    // Día 4 de epoch = lunes 5 de enero de 1970
    private val monday = 4

    @Test fun dayOfWeekMapsMondayToSunday() {
        assertEquals(0, EventCalendar.dayOfWeek(monday))
        assertEquals(2, EventCalendar.dayOfWeek(monday + 2)) // miércoles
        assertEquals(5, EventCalendar.dayOfWeek(monday + 5)) // sábado
        assertEquals(6, EventCalendar.dayOfWeek(monday + 6)) // domingo
        assertEquals(3, EventCalendar.dayOfWeek(0))          // 1/1/1970 fue jueves
    }

    @Test fun weekendGivesDoubleCoinsOnlyOnSatSun() {
        // Elegimos un lunes que NO sea de semana festival para aislar el efecto
        val m = (0..200).map { monday + it * 7 }.first { !EventCalendar.isFestivalWeek(it) }
        assertEquals(1.0, EventCalendar.coinMultiplier(m + 4)) // viernes
        assertEquals(2.0, EventCalendar.coinMultiplier(m + 5)) // sábado
        assertEquals(2.0, EventCalendar.coinMultiplier(m + 6)) // domingo
    }

    @Test fun xpWednesdayOnlyOnWednesday() {
        val m = (0..200).map { monday + it * 7 }.first { !EventCalendar.isFestivalWeek(it) }
        assertEquals(1.0, EventCalendar.xpMultiplier(m + 1))
        assertEquals(2.0, EventCalendar.xpMultiplier(m + 2))
        assertEquals(1.0, EventCalendar.xpMultiplier(m + 3))
    }

    @Test fun festivalWeekHappensOncePerCycleAndCoversWholeWeek() {
        val weeks = (0 until 40).map { monday + it * 7 }
        val festivals = weeks.filter { EventCalendar.isFestivalWeek(it) }
        assertEquals(40 / EventCalendar.FESTIVAL_CYCLE_WEEKS, festivals.size)
        val f = festivals.first()
        for (d in 0..6) {
            val events = EventCalendar.activeOn(f + d)
            assertTrue(events.any { it.type == EventType.FESTIVAL_WEEK }, "día $d")
            assertEquals(2, EventCalendar.starMultiplier(f + d))
        }
    }

    @Test fun bonusesDoNotStack() {
        // Fin de semana dentro de semana festival: x2 (el mejor), no x3
        val f = (0..200).map { monday + it * 7 }.first { EventCalendar.isFestivalWeek(it) }
        assertEquals(2.0, EventCalendar.coinMultiplier(f + 5))
    }

    @Test fun daysLeftCountsDownToZero() {
        val m = (0..200).map { monday + it * 7 }.first { !EventCalendar.isFestivalWeek(it) }
        val saturday = EventCalendar.activeOn(m + 5).single()
        assertEquals(1, saturday.daysLeft(m + 5))
        assertEquals(0, saturday.daysLeft(m + 6))
    }

    @Test fun applyRoundsUpAndNeverReducesTheAmount() {
        assertEquals(15, EventCalendar.apply(10, 1.5))
        assertEquals(8, EventCalendar.apply(5, 1.5))   // 7.5 -> 8
        assertEquals(10, EventCalendar.apply(10, 1.0))
        assertEquals(7, EventCalendar.apply(7, 0.5))   // nunca baja del original
    }

    @Test fun halloweenCoversItsDatesOnly() {
        fun on(m: Int, d: Int) = EventCalendar.activeOn(Civil.toEpochDay(2026, m, d)).map { it.type }
        assertTrue(EventType.HALLOWEEN !in on(10, 24))
        assertTrue(EventType.HALLOWEEN in on(10, 25))
        assertTrue(EventType.HALLOWEEN in on(10, 31))
        assertTrue(EventType.HALLOWEEN !in on(11, 1))
        assertTrue(EventType.DAY_OF_THE_DEAD in on(11, 1))
        assertTrue(EventType.DAY_OF_THE_DEAD in on(11, 2))
        assertTrue(EventType.DAY_OF_THE_DEAD !in on(11, 3))
    }

    @Test fun newYearCrossesTheYearBoundary() {
        fun on(y: Int, m: Int, d: Int) = EventCalendar.activeOn(Civil.toEpochDay(y, m, d)).map { it.type }
        assertTrue(EventType.NEW_YEAR !in on(2026, 12, 30))
        assertTrue(EventType.NEW_YEAR in on(2026, 12, 31))
        assertTrue(EventType.NEW_YEAR in on(2027, 1, 1))
        assertTrue(EventType.NEW_YEAR in on(2027, 1, 2))
        assertTrue(EventType.NEW_YEAR !in on(2027, 1, 3))
    }

    @Test fun seasonalEventKnowsItsLastDay() {
        val e = EventCalendar.seasonalOn(Civil.toEpochDay(2026, 12, 20)).single { it.type == EventType.CHRISTMAS }
        assertEquals(6, e.daysLeft(Civil.toEpochDay(2026, 12, 20)))
        assertEquals(0, e.daysLeft(Civil.toEpochDay(2026, 12, 26)))
    }

    @Test fun seasonalCoinBonusNeverStacksWithWeekend() {
        // Un sábado dentro de Halloween: manda el mayor (fin de semana x2), no x2 * x1.5
        val saturdayInHalloween = (25..31).map { Civil.toEpochDay(2026, 10, it) }.first { EventCalendar.dayOfWeek(it) == 5 }
        assertEquals(2.0, EventCalendar.coinMultiplier(saturdayInHalloween))
        val weekdayInHalloween = (25..31).map { Civil.toEpochDay(2026, 10, it) }.first { EventCalendar.dayOfWeek(it) in 0..4 && !EventCalendar.isFestivalWeek(it) }
        assertEquals(1.5, EventCalendar.coinMultiplier(weekdayInHalloween))
    }
}
