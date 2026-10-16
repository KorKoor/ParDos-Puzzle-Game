package com.korkoor.pardos

import com.korkoor.pardos.domain.shop.StruggleHelp
import com.korkoor.pardos.domain.shop.TimedOffers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimedOffersTest {
    // 2026-03-02 fue lunes: días desde 1970
    private val monday = 20_514L

    @Test fun mondayIsMondayInTheModel() = assertEquals(0, TimedOffers.weekday(monday))

    @Test fun weekendPackLivesOnlyOnSaturdayAndSunday() {
        for (d in 0..6) {
            val offers = TimedOffers.activeOn(monday + d)
            assertEquals(d >= 5, offers.any { it.id == "weekend" }, "día $d")
        }
    }

    @Test fun midweekFlashIsOnlyOnWednesday() {
        for (d in 0..6) assertEquals(d == 2, TimedOffers.activeOn(monday + d).any { it.id == "midweek" }, "día $d")
    }

    @Test fun theSameWindowKeepsTheSameIdSoItCannotBeBoughtTwice() {
        val sat = TimedOffers.activeOn(monday + 5).first { it.id == "weekend" }
        val sun = TimedOffers.activeOn(monday + 6).first { it.id == "weekend" }
        assertEquals(sat.windowId, sun.windowId)
        assertTrue(sat.activeOn(monday + 5) && sat.activeOn(monday + 6) && !sat.activeOn(monday + 7))
        val nextSat = TimedOffers.activeOn(monday + 12).first { it.id == "weekend" }
        assertFalse(sat.windowId == nextSat.windowId, "la semana siguiente es otra ventana")
    }

    @Test fun announcedSavingIsRealAndModest() {
        TimedOffers.activeOn(monday + 5).plus(TimedOffers.activeOn(monday + 2)).forEach {
            assertTrue(it.priceGems in 1 until it.contents.gemValue, "${it.id}: cuesta menos que por separado")
            assertTrue(it.savingPercent in 15..25, "${it.id}: ahorro ${it.savingPercent} %")
        }
    }

    @Test fun struggleHelpIsRareFairAndOptional() {
        assertFalse(StruggleHelp.shouldOffer(2, false, 0))
        assertTrue(StruggleHelp.shouldOffer(3, false, 0))
        assertFalse(StruggleHelp.shouldOffer(9, true, 0), "una vez por nivel")
        assertFalse(StruggleHelp.shouldOffer(9, false, StruggleHelp.MAX_PER_DAY), "tope diario")
        assertEquals(StruggleHelp.contents.gemValue, StruggleHelp.priceGems, "precio normal, sin descuento inventado")
        assertTrue(StruggleHelp.priceGems <= 15, "una ayuda pequeña: ${StruggleHelp.priceGems} gemas")
    }
}
