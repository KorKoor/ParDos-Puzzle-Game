package com.korkoor.pardos

import com.korkoor.pardos.domain.social.FriendCode
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FriendCodeTest {
    @Test fun generatedCodesAreValidAndUnambiguous() {
        val rng = Random(7)
        repeat(500) {
            val c = FriendCode.generate(rng)
            assertTrue(FriendCode.isValid(c), c)
            assertFalse(c.any { it in "01OIL" }, c)
        }
    }

    @Test fun codesAreDiverse() {
        val rng = Random(1)
        assertEquals(1000, (1..1000).map { FriendCode.generate(rng) }.toSet().size)
    }

    @Test fun normalizeCleansUserInput() {
        assertEquals("ABCD2345", FriendCode.normalize(" abcd-2345 "))
        assertTrue(FriendCode.isValid(FriendCode.normalize("abcd 2345")))
    }

    @Test fun invalidCodesAreRejected() {
        assertFalse(FriendCode.isValid("SHORT"))
        assertFalse(FriendCode.isValid("ABCDEFG0")) // contiene 0
    }
}
