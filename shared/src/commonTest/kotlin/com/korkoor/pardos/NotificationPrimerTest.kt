package com.korkoor.pardos

import com.korkoor.pardos.domain.retention.NotificationPrimer
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationPrimerTest {
    private fun ask(
        needed: Boolean = true, granted: Boolean = false, enabled: Boolean = true, won: Boolean = true,
        last: Int? = null, count: Int = 0, today: Int = 100
    ) = NotificationPrimer.shouldAsk(needed, granted, enabled, won, last, count, today)

    @Test fun asksAfterTheFirstWinWhenNeverAsked() = assertTrue(ask())

    @Test fun neverAsksBeforeTheFirstWinOrWhenNotNeeded() {
        assertFalse(ask(won = false))
        assertFalse(ask(needed = false))
    }

    @Test fun neverAsksWhoAlreadyAcceptedOrTurnedThemOffInSettings() {
        assertFalse(ask(granted = true))
        assertFalse(ask(enabled = false))
    }

    @Test fun spacesTheAsksAndStopsAfterThree() {
        assertFalse(ask(last = 99, count = 1, today = 100), "ayer ya se preguntó")
        assertFalse(ask(last = 98, count = 1, today = 100))
        assertTrue(ask(last = 97, count = 1, today = 100))
        assertFalse(ask(last = 10, count = NotificationPrimer.MAX_ASKS, today = 100), "ya insistimos bastante")
    }
}
