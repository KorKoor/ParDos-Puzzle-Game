package com.korkoor.pardos

import com.korkoor.pardos.domain.retention.Civil
import com.korkoor.pardos.domain.retention.Reminder
import com.korkoor.pardos.domain.retention.SeasonalCopy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SeasonalCopyTest {
    private fun day(y: Int, m: Int, d: Int) = Civil.toEpochDay(y, m, d)

    @Test fun halloweenWindowCoversOctoberAndTheFirstDaysOfNovember() {
        assertTrue(SeasonalCopy.isHalloweenWindow(day(2026, 10, 1)))
        assertTrue(SeasonalCopy.isHalloweenWindow(day(2026, 10, 31)))
        assertTrue(SeasonalCopy.isHalloweenWindow(day(2026, 11, 2)))
        assertFalse(SeasonalCopy.isHalloweenWindow(day(2026, 11, 3)))
        assertFalse(SeasonalCopy.isHalloweenWindow(day(2026, 9, 30)))
        assertFalse(SeasonalCopy.isHalloweenWindow(day(2026, 12, 25)))
    }

    private fun r(key: String, title: String = "t", body: String = "b") = Reminder(key, 1, title, body, 1_000L)

    @Test fun spookyKeepsEverythingButTheText() {
        val original = r("free_chest")
        val s = SeasonalCopy.spooky(original)
        assertNotEquals(original.title, s.title)
        assertEquals(original.key, s.key)
        assertEquals(original.id, s.id)
        assertEquals(original.delayMs, s.delayMs)
    }

    @Test fun unknownKeysAreLeftAlone() {
        val o = r("powerup_ready", "Tus poderes están listos", "Varita y Fusión recargadas.")
        assertEquals(o, SeasonalCopy.spooky(o))
    }

    @Test fun dailyGiftKeepsTheStreakDay() {
        val s = SeasonalCopy.spooky(r("regalo_diario", "Tu regalo de hoy te espera", "Día 12 de racha: entra y reclama tus monedas."))
        assertTrue(s.body.contains("Día 12"), s.body)
    }

    @Test fun lastChanceKeepsTheEventName() {
        val s = SeasonalCopy.spooky(r("event_last_chance", "Última oportunidad: Noche de brujas", "Te faltan 2 victorias"))
        assertTrue(s.title.contains("Noche de brujas"), s.title)
    }

    @Test fun applyOnlyChangesTextWhenItIsHalloween() {
        val list = listOf(r("free_chest"), r("wheel"))
        assertEquals(list, SeasonalCopy.apply(list, halloween = false))
        assertTrue(SeasonalCopy.apply(list, halloween = true).all { it.title != "t" })
    }
}
