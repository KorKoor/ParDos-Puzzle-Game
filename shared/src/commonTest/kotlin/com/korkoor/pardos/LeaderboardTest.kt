package com.korkoor.pardos

import com.korkoor.pardos.domain.social.Leaderboard
import com.korkoor.pardos.domain.social.RankEntry
import com.korkoor.pardos.domain.social.WeekCalendar
import kotlin.test.Test
import kotlin.test.assertEquals

class LeaderboardTest {
    @Test fun weekStartsOnMonday() {
        // Día 4 de epoch = lunes 5 de enero de 1970; día 3 = domingo
        assertEquals(WeekCalendar.weekId(4), WeekCalendar.weekId(10)) // lunes..domingo misma semana
        assertEquals(WeekCalendar.weekId(10) + 1, WeekCalendar.weekId(11)) // lunes siguiente
        assertEquals(WeekCalendar.weekId(4) - 1, WeekCalendar.weekId(3)) // domingo anterior
    }

    @Test fun ordersByStarsDescending() {
        val r = Leaderboard.weekly(
            listOf(
                RankEntry("a", "Ana", 5, weekId = 10),
                RankEntry("b", "Beto", 9, weekId = 10),
                RankEntry("c", "Cami", 7, weekId = 10)
            ),
            currentWeek = 10
        )
        assertEquals(listOf("b", "c", "a"), r.map { it.entry.uid })
        assertEquals(listOf(1, 2, 3), r.map { it.position })
    }

    @Test fun oldWeekCountsAsZero() {
        val r = Leaderboard.weekly(
            listOf(
                RankEntry("a", "Ana", 99, weekId = 9),
                RankEntry("b", "Beto", 1, weekId = 10)
            ),
            currentWeek = 10
        )
        assertEquals("b", r.first().entry.uid)
        assertEquals(0, r.last().score)
    }

    @Test fun tiesShareThePosition() {
        val r = Leaderboard.weekly(
            listOf(
                RankEntry("a", "Ana", 6, weekId = 10),
                RankEntry("b", "Beto", 6, weekId = 10),
                RankEntry("c", "Cami", 2, weekId = 10)
            ),
            currentWeek = 10
        )
        assertEquals(listOf(1, 1, 3), r.map { it.position })
    }
}
