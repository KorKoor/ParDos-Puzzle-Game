package com.korkoor.pardos

import com.korkoor.pardos.domain.collection.Rarity
import com.korkoor.pardos.domain.prestige.Metric
import com.korkoor.pardos.domain.prestige.Platinum
import com.korkoor.pardos.domain.prestige.PrestigeMilestones
import com.korkoor.pardos.domain.prestige.PrestigeRank
import com.korkoor.pardos.domain.prestige.PrestigeScore
import com.korkoor.pardos.domain.prestige.PrestigeStats
import com.korkoor.pardos.domain.prestige.ProfileTitles
import com.korkoor.pardos.domain.prestige.RankEntry
import com.korkoor.pardos.domain.prestige.Rivals
import com.korkoor.pardos.domain.prestige.TitleUnlock
import com.korkoor.pardos.domain.prestige.TrophyTier
import com.korkoor.pardos.domain.prestige.trophyTierOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PrestigeTest {
    @Test fun trophiesFollowTheAchievementRarity() {
        assertEquals(TrophyTier.BRONZE, trophyTierOf(Rarity.COMMON))
        assertEquals(TrophyTier.DIAMOND, trophyTierOf(Rarity.LEGENDARY))
        assertTrue(TrophyTier.entries.zipWithNext().all { (a, b) -> a.points < b.points })
    }

    @Test fun scoreGrowsWithEverythingYouAchieve() {
        val base = PrestigeStats()
        val s0 = PrestigeScore.score(base)
        assertEquals(5, s0, "solo el nivel de jugador 1")
        fun more(s: PrestigeStats) = assertTrue(PrestigeScore.score(s) > s0, "$s")
        more(base.copy(trophiesByTier = mapOf(TrophyTier.BRONZE to 1)))
        more(base.copy(piecesByRarity = mapOf(Rarity.COMMON to 1)))
        more(base.copy(foil = 1))
        more(base.copy(seriesComplete = 1))
        more(base.copy(albumComplete = true))
        more(base.copy(campaignStars = 4))
        more(base.copy(towerBest = 1))
        more(base.copy(playerLevel = 2))
        more(base.copy(milestonesDone = 1))
        more(base.copy(platinum = true))
        // las piezas raras valen más que las comunes, y el álbum completo es un salto grande
        assertTrue(PrestigeScore.score(base.copy(piecesByRarity = mapOf(Rarity.LEGENDARY to 1))) > PrestigeScore.score(base.copy(piecesByRarity = mapOf(Rarity.COMMON to 5))))
    }

    @Test fun ranksAreOrderedAndReachableWithoutEverything() {
        assertTrue(PrestigeRank.entries.zipWithNext().all { (a, b) -> a.minScore < b.minScore && a.rewardGems <= b.rewardGems })
        assertEquals(PrestigeRank.NOVICE, PrestigeRank.forScore(0))
        assertEquals(PrestigeRank.APPRENTICE, PrestigeRank.forScore(150))
        assertEquals(PrestigeRank.MYTHIC, PrestigeRank.forScore(1_000_000))
        assertEquals(10, PrestigeRank.pointsToNext(140))
        assertEquals(0, PrestigeRank.pointsToNext(50_000))
        assertEquals(0.5f, PrestigeRank.progress(75), 0.001f)
        assertEquals(1f, PrestigeRank.progress(50_000))
    }

    @Test fun aCasualMonthReachesMasterAndTheLegendNeedsTheWholeGame() {
        // un mes jugando (sin completar nada): rango Experto/Maestro
        val month = PrestigeStats(
            trophiesByTier = mapOf(TrophyTier.BRONZE to 20, TrophyTier.SILVER to 12, TrophyTier.GOLD to 5),
            piecesByRarity = mapOf(Rarity.COMMON to 18, Rarity.RARE to 10, Rarity.EPIC to 3),
            foil = 3, campaignStars = 1500, campaignLevel = 600, towerBest = 25, playerLevel = 40, milestonesDone = 22
        )
        assertTrue(PrestigeRank.forScore(PrestigeScore.score(month)) in listOf(PrestigeRank.EXPERT, PrestigeRank.MASTER, PrestigeRank.GRANDMASTER))
        // sin Platino ni álbum no se llega a Mítico
        val almost = month.copy(
            trophiesByTier = mapOf(TrophyTier.BRONZE to 30, TrophyTier.SILVER to 25, TrophyTier.GOLD to 15, TrophyTier.DIAMOND to 12),
            piecesByRarity = mapOf(Rarity.COMMON to 36, Rarity.RARE to 18, Rarity.EPIC to 9, Rarity.LEGENDARY to 9),
            foil = 30, seriesComplete = 7, campaignStars = 4000, towerBest = 60, playerLevel = 80, milestonesDone = 55
        )
        assertTrue(PrestigeScore.score(almost) < PrestigeRank.MYTHIC.minScore)
        val everything = almost.copy(albumComplete = true, seriesComplete = 9, foil = 72, platinum = true, milestonesDone = PrestigeMilestones.all.size,
            campaignStars = 7000, towerBest = 100, piecesByRarity = mapOf(Rarity.COMMON to 36, Rarity.RARE to 18, Rarity.EPIC to 9, Rarity.LEGENDARY to 9))
        assertEquals(PrestigeRank.MYTHIC, PrestigeRank.forScore(PrestigeScore.score(everything)))
    }

    @Test fun milestonesAreWellFormed() {
        val all = PrestigeMilestones.all
        assertTrue(all.size in 60..90, "hay ${all.size} hitos")
        assertEquals(all.size, all.map { it.id }.toSet().size, "ids únicos")
        for (m in all) {
            assertTrue(m.target > 0 && m.coins > 0 && m.title.isNotBlank() && m.description.isNotBlank(), m.id)
        }
        // dentro de cada métrica los premios crecen con el objetivo
        for ((metric, list) in all.groupBy { it.metric }) {
            val sorted = list.sortedBy { it.target }
            assertTrue(sorted.zipWithNext().all { (a, b) -> a.coins < b.coins && a.gems <= b.gems }, "premios crecientes en $metric")
        }
        assertEquals(Metric.entries.toSet(), all.map { it.metric }.toSet(), "todas las métricas tienen hitos")
    }

    @Test fun milestoneProgressUsesTheRightStat() {
        val s = PrestigeStats(piecesByRarity = mapOf(Rarity.COMMON to 4, Rarity.EPIC to 2), towerBest = 7, kindsWon = 5)
        val pieces5 = PrestigeMilestones.byId("pieces_5")!!
        assertEquals(6, s.value(Metric.PIECES))
        assertTrue(pieces5.isDone(s))
        assertFalse(PrestigeMilestones.byId("pieces_12")!!.isDone(s))
        assertEquals(0.5f, PrestigeMilestones.byId("pieces_12")!!.fraction(s), 0.001f)
        assertTrue(PrestigeMilestones.byId("tower_5")!!.isDone(s) && !PrestigeMilestones.byId("tower_10")!!.isDone(s))
        assertTrue(PrestigeMilestones.byId("kinds_5")!!.isDone(s))
        // lo siguiente enseña lo más cercano primero y salta lo ya hecho
        val next = PrestigeMilestones.nextUp(s, done = setOf("pieces_5"), count = 3)
        assertEquals(3, next.size)
        assertTrue(next.none { it.id == "pieces_5" })
        assertTrue(next.zipWithNext().all { (a, b) -> a.fraction(s) >= b.fraction(s) })
    }

    @Test fun everyTitleIsEarnableAndTheCatalogIsConsistent() {
        val titles = ProfileTitles.all
        assertEquals(titles.size, titles.map { it.id }.toSet().size)
        assertEquals(TitleUnlock.Free, ProfileTitles.byId(ProfileTitles.DEFAULT_ID).unlock)
        assertEquals(ProfileTitles.DEFAULT_ID, ProfileTitles.byId("no existe").id)
        for (t in titles) {
            val u = t.unlock
            if (u is TitleUnlock.Milestone) assertNotNull(PrestigeMilestones.byId(u.id), "el título ${t.id} apunta a un hito que no existe")
            if (u is TitleUnlock.Gems) assertTrue(u.price in 100..1000)
            assertTrue(ProfileTitles.howTo(t).isNotBlank())
        }
        // hay títulos para cada rango (menos Novato), uno de Platino y varios a la venta
        assertEquals(PrestigeRank.entries.size - 1, titles.count { it.unlock is TitleUnlock.Rank })
        assertEquals(1, titles.count { it.unlock == TitleUnlock.PlatinumTrophy })
        assertTrue(titles.count { it.unlock is TitleUnlock.Gems } >= 3)
        assertTrue(titles.size >= 28)
    }

    @Test fun titlesUnlockWithRankMilestonesAndPlatinum() {
        val t = ProfileTitles.byId("rank_expert")
        assertFalse(ProfileTitles.isEarned(t, PrestigeRank.ADEPT, emptySet(), false))
        assertTrue(ProfileTitles.isEarned(t, PrestigeRank.MASTER, emptySet(), false))
        assertTrue(ProfileTitles.isEarned(ProfileTitles.byId("t_collector"), PrestigeRank.NOVICE, setOf("pieces_24"), false))
        assertFalse(ProfileTitles.isEarned(ProfileTitles.byId("t_platinum"), PrestigeRank.MYTHIC, emptySet(), false))
        assertTrue(ProfileTitles.isEarned(ProfileTitles.byId("t_platinum"), PrestigeRank.NOVICE, emptySet(), true))
        assertFalse(ProfileTitles.isEarned(ProfileTitles.byId("g_supreme"), PrestigeRank.MYTHIC, emptySet(), true), "los de gemas se compran")
    }

    @Test fun platinumNeedsEverythingAndShowsProgress() {
        assertEquals(60 to 158, Platinum.progress(20, 82, 40, 76))
        assertFalse(Platinum.isEarned(81, 82, 76, 76))
        assertFalse(Platinum.isEarned(82, 82, 75, 76))
        assertTrue(Platinum.isEarned(82, 82, 76, 76))
        assertFalse(Platinum.isEarned(0, 0, 0, 0), "sin logros no hay platino")
    }

    @Test fun rivalryFindsTheNeighboursAndTheGaps() {
        val me = RankEntry("me", "Yo", 500, isMe = true)
        val friends = listOf(RankEntry("a", "Ana", 900), RankEntry("b", "Beto", 520), RankEntry("c", "Cora", 300), RankEntry("d", "Dani", 100))
        val (sorted, r) = Rivals.rank(me, friends)
        assertEquals(listOf("a", "b", "me", "c", "d"), sorted.map { it.uid })
        assertEquals(3, r.position)
        assertEquals("b", r.above?.uid)
        assertEquals("c", r.below?.uid)
        assertEquals(21, r.gapToAbove, "para superar a Beto hacen falta 21")
        assertEquals(200, r.gapToBelow)
        // primero y último
        val (_, top) = Rivals.rank(RankEntry("me", "Yo", 5_000), friends)
        assertEquals(1, top.position)
        assertNull(top.above)
        assertEquals(0, top.gapToAbove)
        val (_, last) = Rivals.rank(RankEntry("me", "Yo", 1), friends)
        assertNull(last.below)
        // sin amigos eres el primero
        assertEquals(1, Rivals.rank(me, emptyList()).second.position)
    }
}
