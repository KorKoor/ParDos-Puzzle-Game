package com.korkoor.pardos

import com.korkoor.pardos.domain.collection.ChestType
import com.korkoor.pardos.domain.retention.League
import com.korkoor.pardos.domain.retention.LeagueOutcome
import com.korkoor.pardos.domain.retention.LeagueResult
import com.korkoor.pardos.domain.retention.Leagues
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LeaguesTest {

    @Test
    fun promoteWhenTargetReached() {
        val r = Leagues.evaluate(League.BRONZE, League.BRONZE.promoteStars)
        assertEquals(LeagueOutcome.PROMOTED, r.outcome)
        assertEquals(League.SILVER, r.to)
        assertTrue(r.gems > 0 && r.coins > 0)
        assertEquals(ChestType.COMMON, r.chest)
    }

    @Test
    fun stayBetweenKeepAndPromote() {
        val r = Leagues.evaluate(League.GOLD, League.GOLD.keepStars)
        assertEquals(LeagueOutcome.STAYED, r.outcome)
        assertEquals(League.GOLD, r.to)
        assertNull(r.chest)
    }

    @Test
    fun demoteBelowKeepButNeverFromBronze() {
        val r = Leagues.evaluate(League.GOLD, League.GOLD.keepStars - 1)
        assertEquals(LeagueOutcome.DEMOTED, r.outcome)
        assertEquals(League.SILVER, r.to)
        assertEquals(0, r.gems)

        val bronze = Leagues.evaluate(League.BRONZE, 0)
        assertEquals(LeagueOutcome.STAYED, bronze.outcome)
        assertEquals(League.BRONZE, bronze.to)
    }

    @Test
    fun topLeagueIsDefendedNotPromoted() {
        val held = Leagues.evaluate(League.DIAMOND, League.DIAMOND.promoteStars)
        assertEquals(LeagueOutcome.TOP_HELD, held.outcome)
        assertEquals(League.DIAMOND, held.to)
        assertTrue(held.gems > 0)
        // En la cima también se puede bajar si no juegas
        assertEquals(League.RUBY, Leagues.evaluate(League.DIAMOND, 0).to)
    }

    @Test
    fun thresholdsGrowAndAreCoherent() {
        val all = League.entries
        for (i in 1 until all.size) {
            assertTrue(all[i].promoteStars > all[i - 1].promoteStars, "umbral creciente")
            assertTrue(all[i].keepStars < all[i].promoteStars, "mantenerse es más fácil que subir")
        }
        assertEquals(0, League.BRONZE.keepStars)
    }

    @Test
    fun rewardsGrowWithLeague() {
        val up1 = Leagues.evaluate(League.BRONZE, League.BRONZE.promoteStars)
        val up2 = Leagues.evaluate(League.GOLD, League.GOLD.promoteStars)
        assertTrue(up2.gems > up1.gems)
        assertEquals(ChestType.EPIC, Leagues.evaluate(League.SAPPHIRE, League.SAPPHIRE.promoteStars).chest)
    }

    @Test
    fun progressAndStarsToPromote() {
        assertEquals(0f, Leagues.progress(League.SILVER, 0))
        assertEquals(1f, Leagues.progress(League.SILVER, 999))
        assertEquals(0.5f, Leagues.progress(League.SILVER, 15))
        assertEquals(5, Leagues.starsToPromote(League.SILVER, 25))
        assertEquals(0, Leagues.starsToPromote(League.SILVER, 100))
    }

    @Test
    fun riskFlag() {
        assertTrue(Leagues.atRisk(League.GOLD, 0))
        assertFalse(Leagues.atRisk(League.GOLD, League.GOLD.keepStars))
        assertFalse(Leagues.atRisk(League.BRONZE, 0))
    }

    @Test
    fun missedWeeksOnlyDemoteWithCap() {
        assertEquals(League.GOLD, Leagues.afterMissedWeeks(League.GOLD, 0))
        assertEquals(League.SILVER, Leagues.afterMissedWeeks(League.GOLD, 1))
        // Tope de 3 semanas: Rubí → Zafiro → Oro → Plata
        assertEquals(League.SILVER, Leagues.afterMissedWeeks(League.RUBY, 10))
        // Diamante → Rubí → Zafiro → Oro
        assertEquals(League.GOLD, Leagues.afterMissedWeeks(League.DIAMOND, 99))
        // Bronce es el suelo
        assertEquals(League.BRONZE, Leagues.afterMissedWeeks(League.BRONZE, 3))
    }

    @Test
    fun resultEncodeDecodeRoundTrip() {
        val r = Leagues.evaluate(League.SILVER, 35)
        assertEquals(r, LeagueResult.decode(r.encode()))
        val noChest = Leagues.evaluate(League.GOLD, 20)
        assertEquals(noChest, LeagueResult.decode(noChest.encode()))
        assertNull(LeagueResult.decode("basura"))
        assertNull(LeagueResult.decode(null))
    }
}
