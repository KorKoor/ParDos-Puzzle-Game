package com.korkoor.pardos

import com.korkoor.pardos.domain.retention.WhatsNew
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WhatsNewTest {
    @Test fun veteranUpdatingSeesItOnce() {
        assertTrue(WhatsNew.shouldShow(lastSeenVersion = 0, currentVersion = WhatsNew.SINCE_VERSION, isFreshInstall = false))
        assertTrue(WhatsNew.shouldShow(lastSeenVersion = 15, currentVersion = 17, isFreshInstall = false))
        // ya lo vio
        assertFalse(WhatsNew.shouldShow(lastSeenVersion = WhatsNew.SINCE_VERSION, currentVersion = WhatsNew.SINCE_VERSION, isFreshInstall = false))
        assertFalse(WhatsNew.shouldShow(lastSeenVersion = 20, currentVersion = 21, isFreshInstall = false))
    }

    @Test fun newPlayersAndOldBuildsNeverSeeIt() {
        assertFalse(WhatsNew.shouldShow(0, WhatsNew.SINCE_VERSION, isFreshInstall = true))
        assertFalse(WhatsNew.shouldShow(0, WhatsNew.SINCE_VERSION - 1, isFreshInstall = false))
    }

    @Test fun contentIsShortAndComplete() {
        assertTrue(WhatsNew.items.size in 3..WhatsNew.MAX_ITEMS)
        WhatsNew.items.forEach {
            assertTrue(it.title.length in 3..32, it.title)
            assertTrue(it.body.length in 20..120, it.title)
            assertTrue(it.icon.isNotBlank())
        }
        assertEquals(WhatsNew.items.size, WhatsNew.items.map { it.icon }.toSet().size, "iconos distintos")
    }
}
