package com.korkoor.pardos

import com.korkoor.pardos.domain.logic.CustomLevelCode
import com.korkoor.pardos.domain.logic.WeeklyBoard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WeeklyBoardTest {
    @Test
    fun everyPickHasAValidShareableCode() {
        WeeklyBoard.all.forEach { pick ->
            val code = CustomLevelCode.encode(pick.config)
            assertNotNull(code, pick.name)
            assertEquals(pick.config, CustomLevelCode.decode(code))
        }
    }

    @Test
    fun sameWeekSameBoardAndRotatesWeekly() {
        val monday = 20_000L - (20_000L + 3) % 7 // un lunes
        assertEquals(WeeklyBoard.forDay(monday), WeeklyBoard.forDay(monday + 6))
        val seen = (0 until 8).map { WeeklyBoard.forDay(monday + it * 7L).name }.toSet()
        assertEquals(8, seen.size)
        assertTrue(WeeklyBoard.forDay(monday) != WeeklyBoard.forDay(monday + 7))
    }
}
