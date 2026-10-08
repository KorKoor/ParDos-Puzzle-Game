package com.korkoor.pardos

import com.korkoor.pardos.domain.collection.AlbumBonus
import com.korkoor.pardos.domain.collection.CollectibleCatalog
import com.korkoor.pardos.domain.collection.Series
import com.korkoor.pardos.domain.economy.Economy
import com.korkoor.pardos.domain.retention.ReminderInput
import com.korkoor.pardos.domain.retention.ReminderPlanner
import com.korkoor.pardos.domain.social.WeekCalendar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReminderAndAlbumTest {

    private val min = 60_000L
    private fun input(
        minuteOfDay: Int = 20 * 60,
        streak: Int = 0, freeChestLastMs: Long = 0L, nowMs: Long = 10_000_000_000L,
        seasonDaysLeft: Int = 20, seasonClaimable: Int = 0, weeklyOpen: Int = 0, daysLeftInWeek: Int = 5,
        wheelFreeLeft: Int = 0, piggy: Int = 0
    ) = ReminderInput(nowMs, minuteOfDay, streak, freeChestLastMs, seasonDaysLeft, seasonClaimable, 3, weeklyOpen, daysLeftInWeek, wheelFreeLeft, piggy)

    // ---------- Álbum ----------
    @Test fun emptyAlbumGivesNoBonus() = assertEquals(0, AlbumBonus.coinPercent(emptySet()))

    @Test fun eachSeriesCompleteAddsBonus() {
        val owned = CollectibleCatalog.inSeries(Series.GARDEN).map { it.id }.toSet()
        // 10 piezas no suman por piezas, la serie completa da +1 % y sus mejoras de monedas (Jardín da monedas) otro +1 %
        val b = AlbumBonus.breakdown(owned)
        assertEquals(0, b.pieces)
        assertEquals(1, b.series)
        assertEquals(1, b.perks)
        assertEquals(2, AlbumBonus.coinPercent(owned))
    }

    @Test fun fullAlbumReachesTheCap() {
        val all = CollectibleCatalog.all.map { it.id }.toSet()
        assertEquals(AlbumBonus.MAX_PERCENT, AlbumBonus.coinPercent(all, all), "con todas brillantes se llega al tope")
        // 8 % por piezas + 32 % por series + 8 % del álbum + 8 % de mejoras de monedas = 56 % (sin brillantes)
        assertEquals(8 + 32 + 8 + 8, AlbumBonus.breakdown(all).total)
    }

    @Test fun unknownIdsAreIgnored() = assertEquals(0, AlbumBonus.coinPercent((1..40).map { "x$it" }.toSet()))

    @Test fun albumBonusAddsToEventMultiplier() {
        val all = CollectibleCatalog.all.map { it.id }.toSet()
        assertEquals(2.60, AlbumBonus.combine(2.0, all, all), 1e-9)
        assertEquals(2.56, AlbumBonus.combine(2.0, all), 1e-9)
        assertEquals(1.0, AlbumBonus.combine(1.0, emptySet()), 1e-9)
    }

    // ---------- Semana ----------
    @Test fun daysLeftInWeekIsBetweenOneAndSeven() {
        for (d in 20_000..20_020) assertTrue(WeekCalendar.daysLeft(d) in 1..7)
        // El último día de la semana queda 1 y al siguiente se reinicia a 7
        val lastDay = (0..6).first { WeekCalendar.daysLeft(20_000 + it) == 1 } + 20_000
        assertEquals(7, WeekCalendar.daysLeft(lastDay + 1))
        assertEquals(WeekCalendar.weekId(lastDay) + 1, WeekCalendar.weekId(lastDay + 1))
    }

    // ---------- Recordatorios ----------
    @Test fun noReminderFallsInQuietHours() {
        for (minute in listOf(0, 8 * 60, 9 * 60, 12 * 60, 20 * 60, 21 * 60, 22 * 60, 23 * 60 + 59)) {
            val plan = ReminderPlanner.plan(input(minuteOfDay = minute, streak = 5, seasonClaimable = 2, weeklyOpen = 2, daysLeftInWeek = 2, wheelFreeLeft = 1, piggy = 250, seasonDaysLeft = 2))
            plan.filter { it.key != "powerup_ready" }.forEach { r ->
                val target = (((minute + r.delayMs / min) % 1440) + 1440) % 1440
                assertTrue(target in (9 * 60)..(21 * 60 + 29), "${r.key} cae a $target (hoy $minute)")
            }
        }
    }

    @Test fun reminderIdsAreUnique() {
        val plan = ReminderPlanner.plan(input(streak = 5, seasonClaimable = 2, weeklyOpen = 2, daysLeftInWeek = 2, wheelFreeLeft = 1, piggy = 250, seasonDaysLeft = 2))
        assertEquals(plan.size, plan.map { it.id }.toSet().size)
        assertEquals(plan.size, plan.map { it.key }.toSet().size)
        assertTrue(plan.all { it.delayMs > 0 })
    }

    @Test fun streakReminderOnlyWithAStreakToProtect() {
        assertNull(ReminderPlanner.plan(input(streak = 1)).firstOrNull { it.key == "racha_riesgo" })
        assertNotNull(ReminderPlanner.plan(input(streak = 4)).firstOrNull { it.key == "racha_riesgo" })
    }

    @Test fun freeChestReminderFiresWhenTheWaitEnds() {
        val now = 50_000_000_000L
        val last = now - Economy.FREE_CHEST_COOLDOWN_MS + 2 * 60 * 60_000L // faltan 2 h
        val r = ReminderPlanner.plan(input(minuteOfDay = 12 * 60, freeChestLastMs = last, nowMs = now)).first { it.key == "free_chest" }
        assertEquals(2 * 60 * 60_000L, r.delayMs)
    }

    @Test fun seasonAndWeeklyRemindersAreConditional() {
        val calm = ReminderPlanner.plan(input()).map { it.key }
        assertTrue("season_end" !in calm && "weekly_end" !in calm && "season_claim" !in calm && "piggy_full" !in calm && "wheel" !in calm)
        val busy = ReminderPlanner.plan(input(seasonDaysLeft = 3, seasonClaimable = 1, weeklyOpen = 2, daysLeftInWeek = 1, wheelFreeLeft = 1, piggy = 260)).map { it.key }
        assertTrue(listOf("season_end", "weekly_end", "season_claim", "piggy_full", "wheel").all { it in busy })
    }

    @Test fun delayToHourNeverNegative() {
        for (now in listOf(0, 600, 1200, 1439)) for (target in listOf(600, 1200)) for (off in 0..2)
            assertTrue(ReminderPlanner.delayToHour(now, target, off) > 0)
    }

    @Test fun leagueReminderOnlyWhenItMatters() {
        fun keys(name: String, toPromote: Int, risk: Boolean, daysLeft: Int) = ReminderPlanner.plan(
            input(daysLeftInWeek = daysLeft).copy(leagueName = name, leagueStarsToPromote = toPromote, leagueAtRisk = risk)
        ).map { it.key }
        assertTrue("league_end" !in keys("Oro", 30, false, 2), "lejos del ascenso y a salvo")
        assertTrue("league_end" !in keys("Oro", 5, false, 5), "faltan muchos días")
        assertTrue("league_end" in keys("Oro", 5, false, 2), "cerca de subir")
        assertTrue("league_end" in keys("Plata", 30, true, 1), "en riesgo de bajar")
        assertTrue("league_end" !in keys("", 5, true, 1), "sin liga no hay aviso")
    }

    @Test fun missionAndChestRemindersAreConditional() {
        val calm = ReminderPlanner.plan(input()).map { it.key }
        assertTrue("missions_claim" !in calm && "perfect_streak" !in calm && "daily_challenge" !in calm && "chests_unopened" !in calm)
        assertTrue("daily_missions" in calm, "las misiones nuevas se avisan siempre")
        val busy = ReminderPlanner.plan(
            input().copy(missionsClaimable = 2, perfectDays = 4, perfectToday = false, dailyChallengeOpen = true, unopenedChests = 3)
        ).map { it.key }
        assertTrue(listOf("missions_claim", "perfect_streak", "daily_challenge", "chests_unopened").all { it in busy })
        // si hoy ya completó el día, no se le recuerda la racha
        assertTrue("perfect_streak" !in ReminderPlanner.plan(input().copy(perfectDays = 4, perfectToday = true)).map { it.key })
    }

    @Test fun newRemindersNeverFallInQuietHoursAndIdsStayUnique() {
        for (minute in listOf(0, 8 * 60, 9 * 60, 12 * 60, 20 * 60, 21 * 60 + 40, 23 * 60 + 59)) {
            val plan = ReminderPlanner.plan(
                input(minuteOfDay = minute, streak = 5).copy(missionsClaimable = 1, perfectDays = 3, dailyChallengeOpen = true, unopenedChests = 2)
            )
            assertEquals(plan.size, plan.map { it.id }.toSet().size)
            assertEquals(plan.size, plan.map { it.key }.toSet().size)
            plan.filter { it.key in setOf("missions_claim", "perfect_streak", "daily_missions", "daily_challenge", "chests_unopened") }.forEach { r ->
                val target = (((minute + r.delayMs / min) % 1440) + 1440) % 1440
                assertTrue(target in (9 * 60)..(21 * 60 + 29), "${r.key} cae a $target (hoy $minute)")
            }
        }
    }
}
