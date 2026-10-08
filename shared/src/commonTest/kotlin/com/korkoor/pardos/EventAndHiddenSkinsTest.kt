package com.korkoor.pardos

import com.korkoor.pardos.domain.events.EventCalendar
import com.korkoor.pardos.domain.events.EventType
import com.korkoor.pardos.domain.retention.Civil
import com.korkoor.pardos.domain.retention.ReminderInput
import com.korkoor.pardos.domain.retention.ReminderPlanner
import com.korkoor.pardos.domain.retention.UpcomingEvent
import com.korkoor.pardos.domain.shop.DayPart
import com.korkoor.pardos.domain.shop.EventSkins
import com.korkoor.pardos.domain.shop.HiddenSkins
import com.korkoor.pardos.domain.shop.PlayerStats
import com.korkoor.pardos.domain.shop.SkinInventory
import com.korkoor.pardos.domain.shop.SkinSource
import com.korkoor.pardos.domain.shop.TileSkin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EventAndHiddenSkinsTest {

    // ---------- Skins de evento ----------

    @Test fun everyCalendarFeastHasItsOwnSkinAndViceVersa() {
        val feastTypes = EventCalendar.seasonal.map { it.type }
        feastTypes.forEach { assertNotNull(EventSkins.skinFor(it), "$it no tiene skin") }
        // Una skin distinta por fiesta
        assertEquals(feastTypes.size, feastTypes.map { EventSkins.skinFor(it) }.toSet().size)
        // Y toda skin de evento pertenece a una fiesta
        TileSkin.events.forEach { assertNotNull(EventSkins.eventFor(it), "${it.id} sin fiesta") }
        assertEquals(feastTypes.size, TileSkin.events.size)
    }

    @Test fun weeklyEventsHaveNoSkin() {
        assertNull(EventSkins.skinFor(EventType.WEEKEND_GOLD))
        assertNull(EventSkins.skinFor(EventType.XP_WEDNESDAY))
        assertNull(EventSkins.skinFor(EventType.FESTIVAL_WEEK))
    }

    @Test fun eventSkinsCannotBeBoughtWithNormalCurrency() {
        TileSkin.events.forEach {
            assertEquals(SkinSource.EVENT, it.source)
            assertTrue(it.exclusive)
            assertTrue(SkinInventory().buy(it, coins = 999_999, gems = 999_999) is SkinInventory.Purchase.NotPurchasable)
        }
    }

    @Test fun datesAreNeverOverlappingAndAllResolve() {
        // Ningún día del año activa dos fiestas de calendario a la vez
        val start = Civil.toEpochDay(2026, 1, 1)
        for (d in start until start + 365) {
            val feasts = EventCalendar.seasonalOn(d)
            assertTrue(feasts.size <= 1, "día $d con ${feasts.size} fiestas")
        }
        EventCalendar.seasonal.forEach { assertNotNull(EventSkins.dateRange(it.type)) }
        assertEquals("25–31 oct", EventSkins.dateRange(EventType.HALLOWEEN))
        assertEquals("31 dic – 2 ene", EventSkins.dateRange(EventType.NEW_YEAR))
    }

    @Test fun mexicanIndependenceAndSpringAreInTheCalendar() {
        fun on(m: Int, d: Int) = EventCalendar.activeOn(Civil.toEpochDay(2026, m, d)).map { it.type }
        assertTrue(EventType.INDEPENDENCE in on(9, 15))
        assertTrue(EventType.INDEPENDENCE in on(9, 16))
        assertFalse(EventType.INDEPENDENCE in on(9, 17))
        assertTrue(EventType.SPRING in on(3, 20))
        assertTrue(EventType.SPRING in on(4, 3))
        assertTrue(EventType.SUMMER in on(6, 21))
        assertTrue(EventType.SUMMER in on(7, 5))
    }

    @Test fun upcomingFeastsAreListedByProximity() {
        val oct7 = Civil.toEpochDay(2026, 10, 7)
        val up = EventCalendar.upcoming(oct7, 30)
        assertEquals(EventType.HALLOWEEN, up.first().first)
        assertEquals(18, up.first().second)
        assertTrue(up.map { it.first }.contains(EventType.DAY_OF_THE_DEAD))
        // Lo que ya empezó no cuenta como "próximo"
        assertTrue(EventCalendar.upcoming(Civil.toEpochDay(2026, 10, 25), 3).none { it.first == EventType.HALLOWEEN })
        // El cruce de año funciona: desde el 20 de dic se ve el Año nuevo
        assertTrue(EventCalendar.upcoming(Civil.toEpochDay(2026, 12, 20), 14).any { it.first == EventType.NEW_YEAR })
        // y desde el 25 de dic también (empieza el 31)
        assertTrue(EventCalendar.upcoming(Civil.toEpochDay(2026, 12, 25), 14).any { it.first == EventType.NEW_YEAR })
    }

    @Test fun winsLeftNeverNegative() {
        assertEquals(3, EventSkins.winsLeft(0))
        assertEquals(1, EventSkins.winsLeft(2))
        assertEquals(0, EventSkins.winsLeft(10))
    }

    // ---------- Skins secretas ----------

    @Test fun everyHiddenSkinHasExactlyOneRuleAndAHint() {
        val hidden = TileSkin.hidden
        assertEquals(hidden.toSet(), HiddenSkins.defs.map { it.skin }.toSet())
        assertEquals(hidden.size, HiddenSkins.defs.size)
        HiddenSkins.defs.forEach { assertTrue(it.hint.length > 15, it.skin.id) }
        hidden.forEach { assertEquals(SkinSource.HIDDEN, it.source); assertTrue(it.exclusive) }
    }

    @Test fun hintsNeverRevealTheExactNumbers() {
        HiddenSkins.defs.forEach { d ->
            assertFalse(d.hint.any { it.isDigit() }, "${d.skin.id} revela números")
        }
    }

    @Test fun freshPlayerUnlocksNothing() {
        assertTrue(HiddenSkins.unlocked(PlayerStats()).isEmpty())
    }

    @Test fun eachConditionUnlocksItsOwnSkinOnly() {
        fun only(stats: PlayerStats, skin: TileSkin) = assertEquals(listOf(skin), HiddenSkins.unlocked(stats), skin.id)
        only(PlayerStats(nightWins = HiddenSkins.NIGHT_WINS), TileSkin.NIGHT_OWL)
        only(PlayerStats(dawnWins = HiddenSkins.DAWN_WINS), TileSkin.DAWN)
        only(PlayerStats(bestStreak = HiddenSkins.STREAK_DAYS), TileSkin.PHOENIX)
        only(PlayerStats(bestTile = HiddenSkins.TILE), TileSkin.CROWN)
        only(PlayerStats(albumComplete = true), TileSkin.PRISM)
        only(PlayerStats(daysPlayed = HiddenSkins.DAYS_PLAYED), TileSkin.JADE)
        only(PlayerStats(totalStars = HiddenSkins.STARS), TileSkin.METEOR)
        only(PlayerStats(totalWins = HiddenSkins.WINS), TileSkin.OBSIDIAN)
    }

    @Test fun justBelowTheThresholdUnlocksNothing() {
        assertTrue(HiddenSkins.unlocked(PlayerStats(nightWins = HiddenSkins.NIGHT_WINS - 1)).isEmpty())
        assertTrue(HiddenSkins.unlocked(PlayerStats(bestTile = 1024)).isEmpty())
        assertTrue(HiddenSkins.unlocked(PlayerStats(totalStars = HiddenSkins.STARS - 1)).isEmpty())
    }

    @Test fun alreadyOwnedSkinsAreNotReportedAgain() {
        val stats = PlayerStats(bestTile = 4096, totalWins = 500)
        val first = HiddenSkins.newlyUnlocked(stats, owned = emptySet())
        assertEquals(setOf(TileSkin.CROWN, TileSkin.OBSIDIAN), first.toSet())
        assertTrue(HiddenSkins.newlyUnlocked(stats, owned = first.map { it.id }.toSet()).isEmpty())
    }

    @Test fun dayPartBoundaries() {
        assertEquals(DayPart.NIGHT, DayPart.of(0))
        assertEquals(DayPart.NIGHT, DayPart.of(4 * 60 + 59))
        assertEquals(DayPart.DAWN, DayPart.of(5 * 60))
        assertEquals(DayPart.DAWN, DayPart.of(7 * 60 + 59))
        assertEquals(DayPart.OTHER, DayPart.of(8 * 60))
        assertEquals(DayPart.OTHER, DayPart.of(23 * 60 + 59))
    }

    // ---------- Todas las skins nuevas ----------

    @Test fun newSkinsAreWellFormed() {
        (TileSkin.events + TileSkin.hidden).forEach { skin ->
            val st = skin.style
            assertEquals(12, st.tilePalette?.size, skin.id)
            assertTrue(st.changesTheme, "${skin.id} debe traer fondo propio")
            assertTrue(st.ink != null && st.accent != null && st.surface != null, skin.id)
            assertTrue(st.particles != com.korkoor.pardos.domain.shop.ParticleKind.NONE, "${skin.id} sin partículas")
            assertEquals(0, skin.coinPrice)
            assertEquals(0, skin.gemPrice)
        }
    }

    // ---------- Avisos ----------

    private fun input(
        eventName: String = "", eventDaysLeft: Int = Int.MAX_VALUE, eventWinsLeft: Int = 0,
        upcoming: List<UpcomingEvent> = emptyList(), minuteOfDay: Int = 12 * 60
    ) = ReminderInput(
        nowMs = 10_000_000_000L, minuteOfDay = minuteOfDay, streak = 0, freeChestLastMs = 0L,
        seasonDaysLeft = 20, seasonClaimable = 0, seasonTierReached = 3, weeklyOpen = 0, daysLeftInWeek = 5,
        wheelFreeLeft = 0, piggyGems = 0,
        eventName = eventName, eventSkinName = "Noche de Brujas", eventDaysLeft = eventDaysLeft,
        eventWinsLeft = eventWinsLeft, upcomingEvents = upcoming
    )

    @Test fun lastChanceReminderOnlyWhenSkinIsStillMissingAndTimeIsRunningOut() {
        fun has(i: ReminderInput) = ReminderPlanner.plan(i).any { it.key == "event_last_chance" }
        assertTrue(has(input("Noche de brujas", eventDaysLeft = 1, eventWinsLeft = 2)))
        assertTrue(has(input("Noche de brujas", eventDaysLeft = 0, eventWinsLeft = 1)))
        assertFalse(has(input("Noche de brujas", eventDaysLeft = 4, eventWinsLeft = 2)), "faltan muchos días")
        assertFalse(has(input("Noche de brujas", eventDaysLeft = 0, eventWinsLeft = 0)), "ya la tiene")
        assertFalse(has(input("", eventDaysLeft = 0, eventWinsLeft = 2)), "sin evento")
        assertFalse(has(input("Noche de brujas", eventDaysLeft = 0, eventWinsLeft = 2, minuteOfDay = 20 * 60)), "ya pasó la hora hoy")
    }

    @Test fun upcomingEventsScheduleAtTenInTheMorningAndKeepUniqueIds() {
        val plan = ReminderPlanner.plan(
            input(upcoming = listOf(
                UpcomingEvent(1, "Día de Muertos", "Cempasúchil", 25),
                UpcomingEvent(2, "Noche de brujas", "Noche de Brujas", 3),
                UpcomingEvent(3, "Navidad", "Navidad", 70),
                UpcomingEvent(4, "Año nuevo", "Nochevieja", 80)
            ))
        )
        val starts = plan.filter { it.key.startsWith("event_start_") }
        assertEquals(3, starts.size, "máximo 3 avisos de fiestas")
        assertEquals(plan.size, plan.map { it.id }.toSet().size)
        assertEquals(plan.size, plan.map { it.key }.toSet().size)
        assertTrue(starts.all { it.delayMs > 0 })
        // El más cercano va primero (id 20)
        assertEquals("event_start_2", starts.first { it.id == 20 }.key)
        assertTrue(starts.first { it.id == 20 }.title.contains("Noche de brujas"))
    }
}
