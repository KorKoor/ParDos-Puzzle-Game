package com.korkoor.pardos

import com.korkoor.pardos.domain.collection.ChestType
import com.korkoor.pardos.domain.economy.Economy
import com.korkoor.pardos.domain.retention.Civil
import com.korkoor.pardos.domain.retention.Comeback
import com.korkoor.pardos.domain.retention.DailyWheel
import com.korkoor.pardos.domain.retention.FreeChest
import com.korkoor.pardos.domain.retention.PiggyBank
import com.korkoor.pardos.domain.retention.PlayerLevelRewards
import com.korkoor.pardos.domain.retention.SeasonCalendar
import com.korkoor.pardos.domain.retention.SeasonPass
import com.korkoor.pardos.domain.retention.WeeklyMissions
import com.korkoor.pardos.domain.shop.TileSkin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RetentionTest {

    // ---------- Cofre gratis ----------
    @Test fun firstFreeChestIsImmediatelyReady() {
        assertTrue(FreeChest.isReady(0L, 1_000L))
    }

    @Test fun freeChestWaitsForTheCooldown() {
        val last = 1_000_000L
        assertFalse(FreeChest.isReady(last, last + FreeChest.COOLDOWN_MS - 1))
        assertTrue(FreeChest.isReady(last, last + FreeChest.COOLDOWN_MS))
        assertEquals(FreeChest.COOLDOWN_MS / 2, FreeChest.remainingMs(last, last + FreeChest.COOLDOWN_MS / 2))
    }

    @Test fun rewindingTheClockDoesNotGiveAFreeChest() {
        val last = 5_000_000L
        assertFalse(FreeChest.isReady(last, last - 3_600_000L))
        assertEquals(FreeChest.COOLDOWN_MS, FreeChest.remainingMs(last, last - 3_600_000L))
    }

    @Test fun everyFifthFreeChestIsRare() {
        val types = (0 until 10).map { FreeChest.typeFor(it) }
        assertEquals(listOf(4, 9), types.withIndex().filter { it.value == ChestType.RARE }.map { it.index })
    }

    @Test fun skipCostScalesWithWaitAndIsCapped() {
        assertEquals(0, FreeChest.skipCostGems(0))
        assertEquals(1, FreeChest.skipCostGems(60_000))
        assertEquals(2, FreeChest.skipCostGems(31 * 60_000L))
        assertEquals(Economy.FREE_CHEST_SKIP_MAX_GEMS, FreeChest.skipCostGems(FreeChest.COOLDOWN_MS))
    }

    // ---------- Ruleta ----------
    @Test fun wheelPicksEverySliceEventually() {
        val rng = Random(1)
        val seen = (0 until 5_000).map { DailyWheel.pick(rng) }.toSet()
        assertEquals(DailyWheel.slices.indices.toSet(), seen)
    }

    @Test fun wheelFollowsItsWeights() {
        val rng = Random(7)
        val n = 40_000
        val counts = IntArray(DailyWheel.slices.size)
        repeat(n) { counts[DailyWheel.pick(rng)]++ }
        val total = DailyWheel.slices.sumOf { it.weight }
        DailyWheel.slices.forEachIndexed { i, s ->
            val expected = n.toDouble() * s.weight / total
            assertTrue(kotlin.math.abs(counts[i] - expected) < expected * 0.15 + 40, "casilla $i: ${counts[i]} vs $expected")
        }
    }

    @Test fun wheelIsWorthAboutADayOfCasualPlay() {
        val ev = DailyWheel.expectedCoinValue()
        assertTrue(ev in 120.0..320.0, "valor medio por giro: $ev")
    }

    @Test fun wheelAllowanceCountsFreeAndAdSpins() {
        assertTrue(DailyWheel.allowance(0, 0).canSpin)
        assertEquals(0, DailyWheel.allowance(1, 0).freeLeft)
        assertEquals(Economy.WHEEL_AD_SPINS, DailyWheel.allowance(1, 0).adLeft)
        assertFalse(DailyWheel.allowance(1, Economy.WHEEL_AD_SPINS).canSpin)
    }

    // ---------- Calendario y temporadas ----------
    @Test fun civilDatesRoundTrip() {
        listOf(0, 1, 59, 365, 10_957, 19_723, 20_454, 20_733, 25_000).forEach { d ->
            val c = Civil.fromEpochDay(d)
            assertEquals(d, Civil.toEpochDay(c.year, c.month, c.day))
        }
        assertEquals(1970, Civil.fromEpochDay(0).year)
        assertEquals(1, Civil.fromEpochDay(0).month)
        assertEquals(1, Civil.fromEpochDay(0).day)
    }

    @Test fun knownDatesAreCorrect() {
        // 2000-03-01 = día 11017; 2024-02-29 = día 19782
        assertEquals(11_017, Civil.toEpochDay(2000, 3, 1))
        val leap = Civil.fromEpochDay(19_782)
        assertEquals(Triple(2024, 2, 29), Triple(leap.year, leap.month, leap.day))
    }

    @Test fun seasonsAreMonthsAndAdvance() {
        val oct1 = Civil.toEpochDay(2026, 10, 1)
        val id = SeasonCalendar.seasonId(oct1)
        assertEquals(id, SeasonCalendar.seasonId(oct1 + 30))
        assertEquals(id + 1, SeasonCalendar.seasonId(oct1 + 31))
        assertEquals(oct1, SeasonCalendar.startDay(id))
        assertEquals(31, SeasonCalendar.endDay(id) - SeasonCalendar.startDay(id))
        assertEquals(31, SeasonCalendar.daysLeft(oct1))
        assertEquals(1, SeasonCalendar.daysLeft(oct1 + 30))
        assertEquals("Octubre", SeasonCalendar.name(id))
        // Cambio de año
        val dec = SeasonCalendar.seasonId(Civil.toEpochDay(2026, 12, 15))
        assertEquals(dec + 1, SeasonCalendar.seasonId(Civil.toEpochDay(2027, 1, 2)))
        assertEquals("Enero", SeasonCalendar.name(dec + 1))
    }

    @Test fun seasonSkinRotatesAmongExclusiveSkins() {
        val skins = (0 until 6).map { SeasonCalendar.skinFor(2026 * 12 + it) }
        assertEquals(TileSkin.seasonal.toSet(), skins.toSet())
        assertTrue(skins.all { it.exclusive })
        assertEquals(skins[0], skins[TileSkin.seasonal.size])
    }

    // ---------- Pase de temporada ----------
    @Test fun tiersFollowPoints() {
        assertEquals(0, SeasonPass.tierFor(0))
        assertEquals(0, SeasonPass.tierFor(99))
        assertEquals(1, SeasonPass.tierFor(100))
        assertEquals(30, SeasonPass.tierFor(100_000))
        assertEquals(37, SeasonPass.pointsInTier(237))
        assertEquals(0, SeasonPass.pointsInTier(SeasonPass.TOTAL_POINTS))
    }

    @Test fun everyTierPaysSomething() {
        val season = 2026 * 12 + 9
        (1..SeasonPass.TIERS).forEach { t ->
            assertFalse(SeasonPass.freeReward(t).isEmpty, "gratis $t")
            assertFalse(SeasonPass.premiumReward(t, season).isEmpty, "premium $t")
        }
    }

    @Test fun premiumTrackEndsWithTheSeasonSkinAndIsRicherThanFree() {
        val season = 2026 * 12 + 9
        assertEquals(SeasonCalendar.skinFor(season), SeasonPass.premiumReward(SeasonPass.TIERS, season).skin)
        assertNull(SeasonPass.freeReward(SeasonPass.TIERS).skin)
        val free = (1..SeasonPass.TIERS).sumOf { SeasonPass.freeReward(it).coinValue }
        val premium = (1..SeasonPass.TIERS).sumOf { SeasonPass.premiumReward(it, season).coinValue }
        assertTrue(premium > free * 2, "premium $premium vs gratis $free")
    }

    @Test fun freeTrackIsGenerousButNotInflationary() {
        val total = SeasonPass.totalFree()
        assertTrue(total.coins in 1_200..3_000, "monedas gratis: ${total.coins}")
        assertTrue(total.gems in 10..40, "gemas gratis: ${total.gems}")
    }

    // ---------- Misiones semanales ----------
    @Test fun weeklyMissionsAreDeterministicAndDistinct() {
        assertEquals(WeeklyMissions.forWeek(2900), WeeklyMissions.forWeek(2900))
        (2900..3000).forEach { w ->
            val m = WeeklyMissions.forWeek(w)
            assertEquals(WeeklyMissions.PER_WEEK, m.size)
            assertEquals(m.size, m.map { it.type }.toSet().size, "tipos distintos en la semana $w")
        }
    }

    @Test fun weeklyMissionsVaryAcrossWeeks() {
        val seen = (2900..3000).flatMap { WeeklyMissions.forWeek(it) }.map { it.id }.toSet()
        assertTrue(seen.size >= 8, "variedad: ${seen.size}")
    }

    // ---------- Hucha ----------
    @Test fun piggyBankFillsUpToItsCap() {
        assertEquals(1, PiggyBank.deposit(0, PiggyBank.gemsForWin(false)))
        assertEquals(3, PiggyBank.gemsForWin(true))
        assertEquals(PiggyBank.CAP, PiggyBank.deposit(PiggyBank.CAP - 1, 5))
        assertEquals(10, PiggyBank.deposit(10, -4))
        assertFalse(PiggyBank.canBreak(PiggyBank.MIN_TO_BREAK - 1))
        assertTrue(PiggyBank.canBreak(PiggyBank.MIN_TO_BREAK))
    }

    // ---------- Nivel de jugador ----------
    @Test fun levelRewardsGrowAndMilestonesBringChests() {
        assertNull(PlayerLevelRewards.forLevel(2).chest)
        assertEquals(ChestType.COMMON, PlayerLevelRewards.forLevel(5).chest)
        assertEquals(ChestType.RARE, PlayerLevelRewards.forLevel(10).chest)
        assertTrue(PlayerLevelRewards.forLevel(20).coins > PlayerLevelRewards.forLevel(3).coins)
        assertTrue(PlayerLevelRewards.forLevel(5).gems > 0)
        assertEquals(0, PlayerLevelRewards.forLevel(6).gems)
    }

    @Test fun betweenCoversEveryCrossedLevelOnce() {
        assertEquals((6..9).toList(), PlayerLevelRewards.between(5, 9).map { it.level })
        assertTrue(PlayerLevelRewards.between(9, 9).isEmpty())
        assertEquals(listOf(2, 3), PlayerLevelRewards.between(0, 3).map { it.level })
    }

    // ---------- Regreso ----------
    @Test fun comebackGiftNeedsARealAbsence() {
        assertNull(Comeback.giftFor(0))
        assertNull(Comeback.giftFor(Economy.COMEBACK_MIN_DAYS - 1))
        assertNotNull(Comeback.giftFor(Economy.COMEBACK_MIN_DAYS))
        assertTrue(Comeback.giftFor(10)!!.coins > Comeback.giftFor(3)!!.coins)
    }

    @Test
    fun seasonSkipCostScalesWithRemainingProgress() {
        assertEquals(Economy.SEASON_TIER_SKIP_GEMS, SeasonPass.skipCostGems(0))
        assertEquals(10, SeasonPass.skipCostGems(SeasonPass.POINTS_PER_TIER - 1))
        assertTrue(SeasonPass.skipCostGems(SeasonPass.POINTS_PER_TIER / 2) < Economy.SEASON_TIER_SKIP_GEMS)
        assertEquals(0, SeasonPass.skipCostGems(SeasonPass.TOTAL_POINTS))
    }
}
