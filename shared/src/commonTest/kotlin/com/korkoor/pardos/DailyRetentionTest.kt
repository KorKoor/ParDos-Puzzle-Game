package com.korkoor.pardos

import com.korkoor.pardos.domain.meta.MetaSession
import com.korkoor.pardos.domain.retention.HappyHour
import com.korkoor.pardos.domain.retention.LoginCalendar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DailyRetentionTest {

    // ---------------------------------------------------------------- hora feliz

    @Test
    fun happyHourWindowsAreSaneAndStable() {
        for (day in 19000..19400) {
            val w = HappyHour.windowFor(day)
            assertTrue(w.startMin >= HappyHour.EARLIEST_START && w.startMin <= HappyHour.LATEST_START, "inicio fuera de rango en $day")
            assertEquals(0, w.startMin % 5)
            val expected = if (HappyHour.isWeekend(day)) HappyHour.WEEKEND_MINUTES else HappyHour.WEEKDAY_MINUTES
            assertEquals(expected, w.endMin - w.startMin)
            assertTrue(w.endMin <= 23 * 60, "termina muy tarde en $day")
            assertEquals(w, HappyHour.windowFor(day))
        }
    }

    @Test
    fun happyHourWeekendIsSaturdayAndSunday() {
        // 2026-10-10 es sábado, 2026-10-11 domingo, 2026-10-12 lunes
        val saturday = LoginCalendar.daysFromCivil(2026, 10, 10)
        assertTrue(HappyHour.isWeekend(saturday))
        assertTrue(HappyHour.isWeekend(saturday + 1))
        assertFalse(HappyHour.isWeekend(saturday + 2))
        assertFalse(HappyHour.isWeekend(saturday - 1))
    }

    @Test
    fun happyHourPhasesFollowTheClock() {
        val day = 20000
        val w = HappyHour.windowFor(day)
        val before = HappyHour.stateAt(day, w.startMin - 10)
        assertEquals(HappyHour.Phase.UPCOMING, before.phase)
        assertEquals(10, before.minutes)
        val during = HappyHour.stateAt(day, w.startMin + 5)
        assertEquals(HappyHour.Phase.ACTIVE, during.phase)
        assertEquals(w.endMin - w.startMin - 5, during.minutes)
        val after = HappyHour.stateAt(day, w.endMin)
        assertEquals(HappyHour.Phase.ENDED, after.phase)
        assertEquals(HappyHour.windowFor(day + 1), after.window)
        assertEquals(1440 - w.endMin + after.window.startMin, after.minutes)
    }

    @Test
    fun happyHourDoublesCoinsOnlyWhenActive() {
        val day = 20000
        val w = HappyHour.windowFor(day)
        assertEquals(100, HappyHour.applyCoins(100, day, w.startMin - 1))
        assertEquals(200, HappyHour.applyCoins(100, day, w.startMin))
        assertEquals(100, HappyHour.applyCoins(100, day, w.endMin))
        assertEquals(0, HappyHour.applyCoins(0, day, w.startMin))
    }

    // ---------------------------------------------------------------- calendario

    @Test
    fun civilDateRoundTrips() {
        for (day in listOf(-1000, -1, 0, 1, 59, 60, 11016, 19000, 20000, 20500)) {
            val c = LoginCalendar.civilFromDays(day)
            assertEquals(day, LoginCalendar.daysFromCivil(c.year, c.month, c.day))
        }
        val c = LoginCalendar.civilFromDays(LoginCalendar.daysFromCivil(2028, 2, 29))
        assertEquals(LoginCalendar.Civil(2028, 2, 29), c)
        assertEquals(29, LoginCalendar.daysInMonth(2028, 2))
        assertEquals(28, LoginCalendar.daysInMonth(2026, 2))
        assertEquals(28, LoginCalendar.daysInMonth(1900, 2))
        assertEquals(29, LoginCalendar.daysInMonth(2000, 2))
    }

    @Test
    fun calendarPrizesAreBalanced() {
        var gems = 0
        var coins = 0
        for (d in 1..31) {
            val p = LoginCalendar.prizeFor(d, 31)
            assertTrue(p.coins in 50..340)
            gems += p.gems
            coins += p.coins
        }
        assertTrue(gems in 20..40, "gemas por mes: $gems")
        assertTrue(coins < 8000, "monedas por mes: $coins")
        assertEquals("EPIC", LoginCalendar.prizeFor(31, 31).chest)
        assertEquals("COMMON", LoginCalendar.prizeFor(7, 31).chest)
        assertEquals("RARE", LoginCalendar.prizeFor(14, 31).chest)
        assertEquals("EPIC", LoginCalendar.prizeFor(28, 28).chest)
    }

    private fun session(day: Int, minute: Int = 600): MetaSession {
        val m = MetaSession()
        m.tick(day, day * 86_400_000L + minute * 60_000L, minute)
        return m
    }

    private fun field(json: String, key: String): String {
        val at = json.indexOf("\"$key\":")
        assertTrue(at >= 0, "falta $key en $json")
        var i = at + key.length + 3
        val start = i
        while (i < json.length && json[i] != ',' && json[i] != '}' && json[i] != ']') i++
        return json.substring(start, i)
    }

    @Test
    fun calendarClaimsOncePerDayAndPaysCoins() {
        val day = LoginCalendar.daysFromCivil(2026, 10, 3)
        val m = session(day)
        val before = field(m.state(), "coins").toInt()
        val first = m.calendarClaim()
        assertTrue(first.contains("\"ok\":true"), first)
        val after = field(m.state(), "coins").toInt()
        assertEquals(LoginCalendar.prizeFor(3, 31).coins, after - before)
        val second = m.calendarClaim()
        assertTrue(second.contains("\"ok\":false"), second)
    }

    @Test
    fun calendarRecoversMissedDaysFirstOneFree() {
        val start = LoginCalendar.daysFromCivil(2026, 10, 1)
        val m = session(start + 5)   // día 6
        assertTrue(m.calendarClaim().contains("\"ok\":true"))
        // día 4 perdido: la primera recuperación es gratis
        val free = m.calendarRecover(4)
        assertTrue(free.contains("\"ok\":true"), free)
        assertEquals("0", field(free, "cost"))
        // día 3: ya cuesta gemas y no tenemos
        val broke = m.calendarRecover(3)
        assertTrue(broke.contains("\"ok\":false"), broke)
        // el mismo día no se recupera dos veces ni se recupera el futuro
        assertTrue(m.calendarRecover(4).contains("\"ok\":false"))
        assertTrue(m.calendarRecover(20).contains("\"ok\":false"))
    }

    @Test
    fun calendarResetsOnNewMonthAndFreezesIfClockGoesBack() {
        val oct = LoginCalendar.daysFromCivil(2026, 10, 15)
        val m = MetaSession()
        m.tick(oct, oct * 86_400_000L, 600)
        assertTrue(m.calendarClaim().contains("\"ok\":true"))
        val nov = LoginCalendar.daysFromCivil(2026, 11, 2)
        m.tick(nov, nov * 86_400_000L, 600)
        assertTrue(m.calendarClaim().contains("\"ok\":true"), "mes nuevo: casilla nueva")
        // el reloj vuelve a octubre: no se puede cobrar de nuevo
        m.tick(oct + 1, (oct + 1) * 86_400_000L, 600)
        val frozen = m.calendarClaim()
        assertTrue(frozen.contains("\"ok\":false"), frozen)
        assertTrue(m.state().contains("\"frozen\":true"))
    }

    @Test
    fun happyHourBoostsWinCoinsInsideTheWindow() {
        val day = 20000
        val w = HappyHour.windowFor(day)
        fun win(minute: Int): Int {
            val m = session(day, minute)
            val before = field(m.state(), "coins").toInt()
            val json = m.onWin(1, false, 2, 20, 30_000L, 64, 10, false, "ZEN", false, 0)
            assertTrue(json.contains("\"ok\":true"), json)
            return field(m.state(), "coins").toInt() - before
        }
        val normal = win(w.startMin - 30)
        val boosted = win(w.startMin + 2)
        assertTrue(boosted > normal, "hora feliz $boosted contra normal $normal")
        val st = session(day, w.startMin + 2).state()
        assertTrue(st.contains("\"phase\":\"ACTIVE\""), st)
        assertNotNull(Regex("\"happyHour\":\\{").find(st))
    }

    @Test
    fun oldSavesWithoutNewKeysStillLoad() {
        val m = MetaSession()
        m.load("coins\t100\n")
        m.tick(20000, 20000 * 86_400_000L, 600)
        val st = m.state()
        assertTrue(st.contains("\"calendar\":{"), st)
        assertTrue(st.contains("\"happyHour\":{"), st)
        val reminders = m.reminders()
        assertTrue(reminders.startsWith("["), reminders)
    }

    // ---------------------------------------------------------------- premios por anuncio

    @Test
    fun adRewardsRespectDailyLimits() {
        val m = session(20000)
        val gemsBefore = field(m.state(), "gems").toInt()
        var given = 0
        repeat(6) { if (m.adFreeGems().contains("\"ok\":true")) given++ }
        assertEquals(com.korkoor.pardos.domain.shop.AdRewards.FREE_GEMS_PER_DAY, given)
        assertEquals(gemsBefore + given * com.korkoor.pardos.domain.shop.AdRewards.FREE_GEMS, field(m.state(), "gems").toInt())
        // al día siguiente vuelven
        m.tick(20001, 20001 * 86_400_000L, 600)
        assertTrue(m.adFreeGems().contains("\"ok\":true"))
    }

    @Test
    fun adGiftDoublesOnlyOnceAndOnlyAfterClaiming() {
        val m = session(20000)
        assertTrue(m.adDoubleGift().contains("\"ok\":false"))
        val claim = m.claimDailyReward()
        assertTrue(claim.contains("\"ok\":true"), claim)
        val before = field(m.state(), "coins").toInt()
        val dbl = m.adDoubleGift()
        assertTrue(dbl.contains("\"ok\":true"), dbl)
        assertEquals(field(claim, "coins").toInt(), field(m.state(), "coins").toInt() - before)
        assertTrue(m.adDoubleGift().contains("\"ok\":false"))
    }

    @Test
    fun adWheelSpinOnlyAfterFreeSpinsAreUsed() {
        val m = session(20000)
        assertTrue(m.adWheelSpin(1L).contains("\"ok\":false"), "con giros gratis disponibles el anuncio no cuenta")
        while (m.spinWheel(7L).contains("\"ok\":true")) { /* gasta los gratis */ }
        val ad = m.adWheelSpin(2L)
        assertTrue(ad.contains("\"ok\":true"), ad)
    }

    @Test
    fun adSkipChestAndRepairNeedTheirSituation() {
        val m = session(20000)
        m.claimFreeChest()
        val skip = m.adSkipFreeChest()
        assertTrue(skip.contains("\"ok\":true"), skip)
        assertTrue(m.adSkipFreeChest().contains("\"ok\":false"))
        assertTrue(m.adRepairStreak().contains("\"ok\":false"))
    }

    // ---------------------------------------------------------------- serie destacada

    @Test
    fun featuredSeriesRotatesThroughAllSeriesWithoutRepeating() {
        val seen = (20000 until 20032).map { com.korkoor.pardos.domain.retention.FeaturedSeries.seriesFor(it) }
        // fuera de Noche de brujas: 32 dias seguidos, 32 series distintas
        val day = LoginCalendar.daysFromCivil(2026, 3, 1)
        val spring = (day until day + 32).map { com.korkoor.pardos.domain.retention.FeaturedSeries.seriesFor(it) }
        assertEquals(32, spring.toSet().size)
        assertTrue(seen.isNotEmpty())
    }

    @Test
    fun firstChestOfTheDayBringsAFeaturedExtraCard() {
        val day = LoginCalendar.daysFromCivil(2026, 3, 5)
        val m = session(day)
        val st = m.state()
        assertTrue(st.contains("\"featured\":{"), st)
        assertTrue(st.contains("\"bonusReady\":true"), st)
        val claim = m.claimFreeChest()
        assertTrue(claim.contains("\"ok\":true"), claim)
        val type = field(claim, "chest").trim('"')
        val r = m.openChest(type, 11L)
        assertTrue(r.contains("\"ok\":true"), r)
        assertTrue(Regex("\"bonus\":true").findAll(r).count() >= 1, "el primer cofre del dia trae la carta destacada: " + r)
        assertTrue(m.state().contains("\"bonusReady\":false"))
    }
}
