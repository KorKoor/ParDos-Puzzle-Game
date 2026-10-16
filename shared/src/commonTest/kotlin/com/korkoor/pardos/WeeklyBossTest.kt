package com.korkoor.pardos

import com.korkoor.pardos.domain.level.WeeklyBoss
import com.korkoor.pardos.domain.shop.Banners
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WeeklyBossTest {
    @Test fun noBossBeatenMeansNoWeeklyBoss() = assertNull(WeeklyBoss.levelFor(2900, emptyList()))

    @Test fun alwaysPicksOneOfTheBeatenBossesAndKeepsItAllWeek() {
        val beaten = listOf(10, 20, 30, 40, 50)
        val monday = 20_514L
        val week = WeeklyBoss.weekOf(monday)
        for (d in 0..6) assertEquals(week, WeeklyBoss.weekOf(monday + d))
        assertTrue(WeeklyBoss.levelFor(week, beaten) in beaten)
        assertEquals(WeeklyBoss.levelFor(week, beaten), WeeklyBoss.levelFor(week, beaten.reversed() + beaten))
    }

    @Test fun rotatesAcrossWeeks() {
        val beaten = (1..12).map { it * 10 }
        val picks = (0 until 24).map { WeeklyBoss.levelFor(3000L + it, beaten) }.toSet()
        assertTrue(picks.size >= 8, "rota entre muchos jefes: ${picks.size}")
    }

    @Test fun bannerPoolPointsToRealBanners() {
        val ids = Banners.all.map { it.id }.toSet()
        WeeklyBoss.bannerPool.forEach { assertTrue(it in ids, "el banner $it existe") }
        assertTrue((0 until 40).map { WeeklyBoss.bannerFor(3000L + it) }.toSet().size == WeeklyBoss.bannerPool.size)
    }
}
