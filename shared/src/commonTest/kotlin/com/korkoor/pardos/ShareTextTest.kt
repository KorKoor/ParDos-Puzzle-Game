package com.korkoor.pardos

import com.korkoor.pardos.domain.retention.Civil
import com.korkoor.pardos.domain.social.ShareText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShareTextTest {

    @Test fun dayLabelUsesSpanishMonth() {
        assertEquals("7 oct", ShareText.dayLabel(Civil.toEpochDay(2026, 10, 7)))
        assertEquals("1 ene", ShareText.dayLabel(Civil.toEpochDay(2027, 1, 1)))
        assertEquals("31 dic", ShareText.dayLabel(Civil.toEpochDay(2026, 12, 31)))
    }

    @Test fun clockFormatsMinutesAndSeconds() {
        assertEquals("0:00", ShareText.clock(0))
        assertEquals("1:05", ShareText.clock(65_000))
        assertEquals("12:34", ShareText.clock(754_000))
        assertEquals("0:00", ShareText.clock(-5))
    }

    @Test fun starsLineAlwaysHasThreeSlots() {
        for (s in -1..5) {
            val line = ShareText.starsLine(s)
            assertEquals(ShareText.MAX_STARS, line.count { it == '⭐' } + line.split("▫️").size - 1)
        }
        assertEquals(3, ShareText.starsLine(3).count { it == '⭐' })
        assertEquals(0, ShareText.starsLine(0).count { it == '⭐' })
    }

    @Test fun dailyVictoryMentionsDateAndStore() {
        val day = Civil.toEpochDay(2026, 10, 7)
        val text = ShareText.victory("Clásico", 3, 512, 134, 102_000, 5, epochDay = day)
        assertTrue("Reto diario 7 oct" in text)
        assertTrue("ficha 512" in text && "134 mov" in text && "1:42" in text)
        assertTrue("Racha de 5 días" in text)
        assertTrue(text.endsWith(ShareText.STORE_URL))
    }

    @Test fun normalVictoryUsesModeAndHidesShortStreak() {
        val text = ShareText.victory("Zen", 2, 128, 40, 30_000, 1)
        assertTrue(text.startsWith("ParDos · Zen"))
        assertFalse("Racha" in text)
        assertFalse("Reto diario" in text)
    }

    @Test fun inviteContainsCode() {
        val text = ShareText.invite("ABCD2345")
        assertTrue("ABCD2345" in text && ShareText.STORE_URL in text)
    }
}
