package com.korkoor.pardos

import com.korkoor.pardos.domain.economy.Economy
import com.korkoor.pardos.domain.logic.RemoteChallenge
import com.korkoor.pardos.domain.logic.RemoteDuel
import com.korkoor.pardos.domain.logic.RemoteDuelRecord
import com.korkoor.pardos.domain.logic.RemoteOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RemoteDuelTest {
    private val ch = RemoteChallenge(seed = 123_456_789_012L, score = 1240, name = "Carlos")

    @Test fun codeRoundTrips() {
        val code = RemoteDuel.encode(ch)
        assertTrue(code.startsWith("PD1-"))
        val back = RemoteDuel.decode(code)!!
        assertEquals(ch.seed, back.seed)
        assertEquals(1240, back.score)
        assertEquals("CARLOS", back.name)
    }

    @Test fun codeIsFoundInsideAMessageAndIsCaseInsensitive() {
        val msg = "Hola! mira esto:\n${RemoteDuel.encode(ch).lowercase()}\nsuerte 😉"
        assertEquals(1240, RemoteDuel.decode(msg)?.score)
        assertEquals(1240, RemoteDuel.decode(RemoteDuel.shareText(ch, "https://x.y"))?.score)
    }

    @Test fun tamperedOrBrokenCodesAreRejected() {
        val code = RemoteDuel.encode(ch)
        // cambiar el puntaje a mano invalida el control
        val parts = code.split('-').toMutableList()
        parts[2] = "ZZZ"
        assertNull(RemoteDuel.decode(parts.joinToString("-")))
        // un carácter mal escrito
        val typo = code.replaceFirst("PD1-", "PD1-0")
        assertNull(RemoteDuel.decode(typo))
        assertNull(RemoteDuel.decode("nada que ver"))
        assertNull(RemoteDuel.decode(""))
        assertNull(RemoteDuel.decode("PD1-1-2-3"))
    }

    @Test fun differentChallengesGiveDifferentCodes() {
        assertNotEquals(RemoteDuel.encode(ch), RemoteDuel.encode(ch.copy(score = 1241)))
        assertNotEquals(RemoteDuel.encode(ch), RemoteDuel.encode(ch.copy(seed = ch.seed + 1)))
    }

    @Test fun seedsAreNormalizedToFitTheCode() {
        for (raw in listOf(0L, 1L, -1L, Long.MAX_VALUE, Long.MIN_VALUE, 987654321987654321L)) {
            val seed = RemoteDuel.normalizeSeed(raw)
            assertTrue(seed >= 0)
            val back = RemoteDuel.decode(RemoteDuel.encode(RemoteChallenge(seed, 10, "A")))
            assertEquals(seed, back?.seed, "semilla $raw")
        }
    }

    @Test fun namesAreCleanedAndNeverEmpty() {
        assertEquals("JOSE_LUIS", RemoteDuel.cleanName("José Luis"))
        assertEquals("NINO", RemoteDuel.cleanName("Niño"))
        assertEquals("AMIGO", RemoteDuel.cleanName("😀😀"))
        assertEquals("AMIGO", RemoteDuel.cleanName("   "))
        assertEquals(12, RemoteDuel.cleanName("abcdefghijklmnopqrstuvwxyz").length)
        assertEquals("A_B", RemoteDuel.cleanName("a - - b"))
        assertEquals("Jose Luis", RemoteDuel.displayName("JOSE_LUIS"))
        // un nombre raro nunca rompe el código
        val weird = RemoteChallenge(5, 7, "¡¡Ñandú--Rey!!")
        assertEquals("NANDURE", RemoteDuel.decode(RemoteDuel.encode(weird))?.name?.take(7))
    }

    @Test fun outcomeAndRewards() {
        assertEquals(RemoteOutcome.WIN, RemoteDuel.outcome(10, 5))
        assertEquals(RemoteOutcome.LOSE, RemoteDuel.outcome(5, 10))
        assertEquals(RemoteOutcome.TIE, RemoteDuel.outcome(7, 7))
        val win = RemoteDuel.rewardFor(RemoteOutcome.WIN)
        val lose = RemoteDuel.rewardFor(RemoteOutcome.LOSE)
        val tie = RemoteDuel.rewardFor(RemoteOutcome.TIE)
        assertTrue(win.coins > tie.coins && tie.coins > lose.coins && lose.coins > 0)
        assertTrue(win.gems > 0 && lose.gems == 0)
    }

    @Test fun creatorRewardIsCappedPerDay() {
        assertEquals(Economy.REMOTE_DUEL_CREATE_COINS, RemoteDuel.createReward(0).coins)
        assertEquals(Economy.REMOTE_DUEL_CREATE_COINS, RemoteDuel.createReward(Economy.REMOTE_DUEL_CREATE_PER_DAY - 1).coins)
        assertEquals(0, RemoteDuel.createReward(Economy.REMOTE_DUEL_CREATE_PER_DAY).coins)
    }

    private fun rec(o: RemoteOutcome) = RemoteDuelRecord("X", 1, 1, o, 1)

    @Test fun statsAndStreaks() {
        // más reciente primero: W W L W W W L
        val h = listOf(RemoteOutcome.WIN, RemoteOutcome.WIN, RemoteOutcome.LOSE, RemoteOutcome.WIN, RemoteOutcome.WIN, RemoteOutcome.WIN, RemoteOutcome.LOSE).map(::rec)
        val s = RemoteDuel.stats(h)
        assertEquals(5, s.wins); assertEquals(2, s.losses); assertEquals(0, s.ties); assertEquals(7, s.played)
        assertEquals(2, s.currentStreak)
        assertEquals(3, s.bestStreak)
        assertEquals(0, RemoteDuel.stats(emptyList()).played)
    }

    @Test fun historyRoundTripsAndSurvivesGarbage() {
        val h = listOf(
            RemoteDuelRecord("Ana", 120, 90, RemoteOutcome.WIN, 20_000),
            RemoteDuelRecord("Luis", 50, 80, RemoteOutcome.LOSE, 19_999)
        )
        assertEquals(h, RemoteDuel.decodeHistory(RemoteDuel.encodeHistory(h)))
        assertTrue(RemoteDuel.decodeHistory(null).isEmpty())
        assertTrue(RemoteDuel.decodeHistory("").isEmpty())
        assertEquals(1, RemoteDuel.decodeHistory("basura;Ana,1,2,WIN,3;x,y").size)
    }
}
