package com.korkoor.pardos

import com.korkoor.pardos.domain.level.LevelCatalog
import com.korkoor.pardos.domain.level.LevelKind
import com.korkoor.pardos.domain.tower.TowerRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TowerTest {
    @Test fun everyFloorMapsToARealCampaignLevelAndIsStable() {
        for (floor in 1..200) {
            val id = TowerRules.levelIdFor(floor)
            assertTrue(id in 1..LevelCatalog.TOTAL_LEVELS, "piso $floor → nivel $id")
            assertEquals(id, TowerRules.levelIdFor(floor), "el mismo piso da siempre el mismo nivel")
        }
    }

    @Test fun bossFloorsPlayBossesAndTheRestDoNot() {
        for (floor in 1..120) {
            val spec = LevelCatalog.spec(TowerRules.levelIdFor(floor))
            assertEquals(TowerRules.isBossFloor(floor), spec.kind == LevelKind.BOSS, "piso $floor ${spec.kind}")
            if (!TowerRules.isBossFloor(floor)) assertTrue((TowerRules.levelIdFor(floor) - 1) % 20 != 0, "nunca el nivel de bienvenida")
        }
    }

    @Test fun difficultyRisesWithTheFloorsUntilTheEndOfTheCampaign() {
        val chapters = (1..120).map { TowerRules.chapterFor(it) }
        assertTrue(chapters.zipWithNext().all { (a, b) -> b >= a })
        assertTrue(chapters.first() in 2..3, "los primeros pisos son amables")
        assertEquals(LevelCatalog.TOTAL_LEVELS / 20 - 1, chapters.last(), "se queda en el último capítulo")
    }

    @Test fun floorsMixManyKindsOfLevels() {
        val kinds = (1..80).map { LevelCatalog.spec(TowerRules.levelIdFor(it)).kind }.toSet()
        assertTrue(kinds.size >= 10, "solo ${kinds.size} tipos en 80 pisos: $kinds")
    }

    @Test fun rewardsGrowAndBossesPayAndHeal() {
        val r = (1..30).map { TowerRules.rewardFor(it) }
        assertTrue(r.zipWithNext().all { (a, b) -> b.coins >= a.coins - 60 })
        assertTrue(TowerRules.rewardFor(5).gems > 0 && TowerRules.rewardFor(5).heart)
        assertEquals(0, TowerRules.rewardFor(4).gems)
        assertEquals(TowerRules.START_HEARTS, TowerRules.heartsAfterClear(4, TowerRules.START_HEARTS))
        assertEquals(TowerRules.START_HEARTS + 1, TowerRules.heartsAfterClear(5, TowerRules.START_HEARTS))
        assertEquals(TowerRules.MAX_HEARTS, TowerRules.heartsAfterClear(10, TowerRules.MAX_HEARTS))
    }
}
