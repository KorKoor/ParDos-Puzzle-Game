package com.korkoor.pardos

import com.korkoor.pardos.domain.social.SyncPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SyncPolicyTest {
    private val min = 60_000L

    @Test fun neverUploadsWhenNothingChanged() {
        assertFalse(SyncPolicy.shouldUpload(dirty = true, contentChanged = false, lastUploadMs = 0, nowMs = 10 * min))
        assertFalse(SyncPolicy.shouldUpload(dirty = false, contentChanged = true, lastUploadMs = 0, nowMs = 10 * min))
    }

    @Test fun firstUploadIsImmediateAfterwardsRespectsTheGap() {
        assertTrue(SyncPolicy.shouldUpload(true, true, lastUploadMs = 0, nowMs = 1_000))
        val last = 100 * min
        assertFalse(SyncPolicy.shouldUpload(true, true, last, last + 30_000))
        assertTrue(SyncPolicy.shouldUpload(true, true, last, last + SyncPolicy.MIN_UPLOAD_GAP_MS))
    }

    @Test fun waitTimeNeverNegative() {
        assertEquals(0L, SyncPolicy.waitBeforeUpload(0, 5 * min))
        assertEquals(40_000L, SyncPolicy.waitBeforeUpload(100 * min, 100 * min + 20_000))
        assertEquals(0L, SyncPolicy.waitBeforeUpload(100 * min, 200 * min))
    }

    @Test fun friendsCacheRules() {
        val ids = setOf("a", "b")
        val t0 = 1_000_000L
        assertTrue(SyncPolicy.friendsCacheValid(ids, ids, t0, t0 + 5 * min, force = false))
        assertFalse(SyncPolicy.friendsCacheValid(ids, ids, t0, t0 + 5 * min, force = true), "forzado")
        assertFalse(SyncPolicy.friendsCacheValid(ids, ids, t0, t0 + SyncPolicy.FRIENDS_CACHE_MS, force = false), "vencida")
        assertFalse(SyncPolicy.friendsCacheValid(null, ids, t0, t0, force = false), "sin caché")
        assertFalse(SyncPolicy.friendsCacheValid(ids, ids + "c", t0, t0 + min, force = false), "cambió la lista de amigos")
        assertFalse(SyncPolicy.friendsCacheValid(ids, ids, t0 + min, t0, force = false), "reloj hacia atrás")
    }

    @Test fun cloudCheckOnlyWhenThereIsAReason() {
        val day = SyncPolicy.CLOUD_CHECK_MS
        assertTrue(SyncPolicy.shouldCheckCloud(isFreshProfile = true, lastCheckMs = 5, nowMs = 10), "reinstalación")
        assertTrue(SyncPolicy.shouldCheckCloud(false, 0, 10), "nunca se miró")
        assertFalse(SyncPolicy.shouldCheckCloud(false, 1_000, 1_000 + day - 1))
        assertTrue(SyncPolicy.shouldCheckCloud(false, 1_000, 1_000 + day))
    }

    @Test fun hashChangesWithContentButNotWithRepeats() {
        val a = SyncPolicy.contentHash(listOf("Ana", 5, 120, null, listOf("x")))
        assertEquals(a, SyncPolicy.contentHash(listOf("Ana", 5, 120, null, listOf("x"))))
        assertNotEquals(a, SyncPolicy.contentHash(listOf("Ana", 5, 121, null, listOf("x"))))
        assertNotEquals(a, SyncPolicy.contentHash(listOf("Ana", 5, 120, null, listOf("y"))))
    }

    @Test fun oneUploadPerSessionAtMost() {
        assertEquals(3, SyncPolicy.estimatedDailyWrites(3))
        assertEquals(0, SyncPolicy.estimatedDailyWrites(-1))
    }
}
