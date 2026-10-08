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

    @Test fun friendTtlDependsOnActivity() {
        val now = 100L * 24 * 60 * min
        val day = 24 * 60 * min
        assertEquals(SyncPolicy.FRIENDS_CACHE_MS, SyncPolicy.friendTtlMs(now - 60 * min, now), "juega hoy")
        assertEquals(6 * 60 * min, SyncPolicy.friendTtlMs(now - 5 * day, now), "hace días")
        assertEquals(day, SyncPolicy.friendTtlMs(now - 30 * day, now), "inactivo")
        assertEquals(day, SyncPolicy.friendTtlMs(0, now), "sin dato")
    }

    @Test fun onlyStaleFriendsAreFetched() {
        val now = 100L * 24 * 60 * min
        val day = 24 * 60 * min
        val ids = listOf("a", "b", "c", "d")
        val fetched = mapOf("a" to now - 5 * min, "b" to now - 20 * min, "c" to now - 20 * min)
        val lastPlay = mapOf("a" to now - min, "b" to now - min, "c" to now - 30 * day)
        // a: activo y fresco; b: activo y vencido; c: inactivo y fresco; d: nunca leído
        assertEquals(listOf("b", "d"), SyncPolicy.friendsToFetch(ids, fetched, lastPlay, now, force = false))
        assertEquals(ids, SyncPolicy.friendsToFetch(ids, fetched, lastPlay, now, force = true))
        assertEquals(emptyList(), SyncPolicy.friendsToFetch(listOf("a"), fetched, lastPlay, now, force = false))
        assertEquals(listOf("a"), SyncPolicy.friendsToFetch(listOf("a"), mapOf("a" to now + day), lastPlay, now, force = false), "reloj hacia atrás")
    }

    @Test fun uploadBackoffGrowsAndCaps() {
        assertEquals(0L, SyncPolicy.uploadBackoffMs(0))
        assertEquals(min, SyncPolicy.uploadBackoffMs(1))
        assertEquals(2 * min, SyncPolicy.uploadBackoffMs(2))
        assertEquals(4 * min, SyncPolicy.uploadBackoffMs(3))
        assertEquals(6 * 60 * min, SyncPolicy.uploadBackoffMs(50))
    }

    @Test fun friendLookupsAreLimited() {
        val now = 10L * 60 * min
        assertTrue(SyncPolicy.canLookupFriend(emptyList(), now))
        assertFalse(SyncPolicy.canLookupFriend(listOf(now - 500), now), "muy seguidas")
        assertTrue(SyncPolicy.canLookupFriend(listOf(now - 2_000), now))
        val many = (1..SyncPolicy.MAX_LOOKUPS_PER_HOUR).map { now - 10 * min + it * 2_000L }
        assertFalse(SyncPolicy.canLookupFriend(many, now), "tope por hora")
        assertTrue(SyncPolicy.canLookupFriend(many.map { it - 60 * min }, now), "las viejas no cuentan")
    }

    @Test fun friendLimit() {
        assertFalse(SyncPolicy.friendLimitReached(SyncPolicy.MAX_FRIENDS - 1))
        assertTrue(SyncPolicy.friendLimitReached(SyncPolicy.MAX_FRIENDS))
    }
}
