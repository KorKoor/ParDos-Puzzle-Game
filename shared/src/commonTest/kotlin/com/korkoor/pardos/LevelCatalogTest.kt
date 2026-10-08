package com.korkoor.pardos

import com.korkoor.pardos.domain.level.LevelCatalog
import com.korkoor.pardos.domain.level.LevelGenerator
import com.korkoor.pardos.domain.level.LevelGoal
import com.korkoor.pardos.domain.level.LevelKind
import com.korkoor.pardos.domain.level.LevelMath
import com.korkoor.pardos.domain.level.LevelRules
import com.korkoor.pardos.domain.level.LevelValidator
import com.korkoor.pardos.domain.level.SpawnRules
import com.korkoor.pardos.domain.level.SpawnStyle
import com.korkoor.pardos.domain.level.StonePatterns
import com.korkoor.pardos.domain.logic.ProgressionEngine
import com.korkoor.pardos.domain.model.LevelRepository
import com.korkoor.pardos.domain.model.TileModel
import com.korkoor.pardos.domain.rewards.ChapterRewards
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LevelCatalogTest {
    private val chapters = LevelCatalog.TOTAL_LEVELS / LevelCatalog.LEVELS_PER_CHAPTER

    @Test fun chaptersMatchTheChestRhythm() = assertEquals(ChapterRewards.LEVELS_PER_CHAPTER, LevelCatalog.LEVELS_PER_CHAPTER)

    @Test fun specsAreDeterministic() {
        for (id in listOf(1, 7, 20, 21, 57, 480, 1999, 2400)) assertEquals(LevelCatalog.spec(id), LevelCatalog.spec(id))
    }

    @Test fun everyLevelOfTheCampaignIsValid() {
        val problems = LevelCatalog.all().flatMap { LevelValidator.problems(it) }
        assertTrue(problems.isEmpty(), "Niveles con problemas:\n" + problems.take(30).joinToString("\n"))
    }

    @Test fun idsMatchPosition() {
        LevelCatalog.all().forEachIndexed { i, spec -> assertEquals(i + 1, spec.id) }
    }

    @Test fun repositoryExposesTheCatalog() {
        val infos = LevelRepository.getGeneratedLevels()
        assertEquals(LevelCatalog.TOTAL_LEVELS, infos.size)
        assertFalse(infos[0].isLocked)
        assertTrue(infos.drop(1).all { it.isLocked })
        assertEquals(LevelCatalog.spec(30), infos[29].spec)
    }

    // ---------------------------------------------------------------- variedad

    @Test fun everyChapterPlanFollowsTheBeatSheet() {
        for (ch in 0 until chapters) {
            val plan = LevelGenerator.plan(ch)
            assertEquals(20, plan.size)
            assertEquals(LevelKind.ZEN, plan[0], "capítulo $ch abre con Zen")
            assertEquals(LevelKind.BOSS, plan[9], "capítulo $ch jefe pequeño")
            assertEquals(LevelKind.BOSS, plan[19], "capítulo $ch jefe final")
            assertEquals(2, plan.count { it == LevelKind.BOSS }, "capítulo $ch solo dos jefes")
            for (i in 1 until 20) {
                assertTrue(plan[i] != plan[i - 1], "capítulo $ch repite ${plan[i]} en ${i + 1}")
                if (plan[i].isDemanding && plan[i - 1].isDemanding) assertTrue(plan[i] == LevelKind.BOSS && i == 9 || plan[i - 1] == LevelKind.BOSS && i == 10 || plan[i] == LevelKind.BOSS,
                    "capítulo $ch junta dos niveles exigentes en ${i + 1}")
            }
            assertTrue(plan[4] != LevelKind.ZEN && plan[14] != LevelKind.ZEN, "capítulo $ch: los niveles 5 y 15 son especiales")
        }
    }

    @Test fun noChapterIsMonotone() {
        // Desde el capítulo 3 hay mucha variedad: al menos 6 tipos distintos de 9 posibles
        for (ch in 2 until chapters) {
            val kinds = LevelGenerator.plan(ch).toSet()
            assertTrue(kinds.size >= 6, "capítulo $ch solo tiene ${kinds.size} tipos: $kinds")
        }
        for (ch in 1 until 2) assertTrue(LevelGenerator.plan(ch).toSet().size >= 5)
    }

    @Test fun zenStaysAMinorityAcrossTheCampaign() {
        val kinds = LevelCatalog.all().map { it.kind }
        val zen = kinds.count { it == LevelKind.ZEN }.toDouble() / kinds.size
        assertTrue(zen in 0.08..0.35, "Zen es el ${(zen * 100).toInt()} % de los niveles")
        for (kind in LevelKind.entries) assertTrue(kinds.count { it == kind } > (if (LevelGenerator.unlockOf(kind) >= 14) 35 else 60), "$kind aparece muy poco")
    }

    @Test fun boardsAndTargetsVaryInsideTheCampaign() {
        val window = LevelCatalog.all().filter { it.id in 100..400 }
        assertTrue(window.map { it.boardSize }.toSet().containsAll(setOf(3, 4, 5)), "tableros distintos")
        assertTrue(window.map { it.goalValue }.toSet().size >= 6, "metas distintas")
        assertTrue(window.mapNotNull { it.stones.takeIf { s -> s.isNotEmpty() } }.toSet().size >= 8, "distribuciones de piedras distintas")
        assertTrue(window.any { it.goal == LevelGoal.SCORE } && window.any { it.spawn == SpawnStyle.FOURS })
    }

    @Test fun firstChapterIntroducesOneRuleAtATime() {
        val first = LevelKind.entries.associateWith { kind -> LevelCatalog.all(20).firstOrNull { it.kind == kind }?.id }
        assertEquals(1, first[LevelKind.ZEN])
        assertEquals(3, first[LevelKind.SCORE])
        assertEquals(5, first[LevelKind.FOURS])
        assertEquals(6, first[LevelKind.HEADSTART])
        assertEquals(7, first[LevelKind.STONES])
        assertEquals(8, first[LevelKind.SPRINT])
        assertEquals(10, first[LevelKind.BOSS])
        assertEquals(12, first[LevelKind.TWINS])
        assertEquals(14, first[LevelKind.CLOCK])
    }

    @Test fun earlyDifficultyGrowsSlowly() {
        // Las metas de los 10 primeros niveles nunca superan 128 y el tablero 3×3 solo se usa con metas pequeñas
        LevelCatalog.all(10).forEach { assertTrue(it.goalValue <= 128 || it.goal == LevelGoal.SCORE, "nivel ${it.id}") }
        LevelCatalog.all(60).filter { it.boardSize == 3 }.forEach { assertTrue(it.scaleTile <= 128, "nivel ${it.id} 3×3 con ${it.scaleTile}") }
    }

    @Test fun bossesPayMoreAndCarryTheirOwnName() {
        val bosses = LevelCatalog.all().filter { it.isBoss }
        assertEquals(chapters * 2, bosses.size)
        assertTrue(bosses.all { it.title != LevelKind.BOSS.label && LevelRules.coinBonus(it) > LevelRules.coinBonus(LevelCatalog.spec(1)) })
        assertTrue(bosses.map { it.title }.toSet().size >= 6)
        // Los jefes tienen al menos dos reglas a la vez
        assertTrue(bosses.all { it.ruleChips().size + (if (it.goalCount > 1) 1 else 0) + (if (it.goal == LevelGoal.SCORE) 1 else 0) >= 1 })
    }

    @Test fun everyStonePatternIsValid() {
        for (p in StonePatterns.all) {
            assertTrue(LevelValidator.isConnected(p.size, p.cells.toSet()), "${p.id} aísla casillas")
            assertTrue(p.cells.all { (r, c) -> r in 0 until p.size && c in 0 until p.size }, "${p.id} fuera del tablero")
            assertEquals(p.cells.size, p.cells.toSet().size)
            assertTrue(p.severity in 1..3)
        }
        assertEquals(StonePatterns.all.size, StonePatterns.all.map { it.id }.toSet().size)
        for (size in listOf(4, 5)) for (sev in 1..3) assertTrue(StonePatterns.forSize(size).any { it.severity == sev }, "falta $size/$sev")
    }

    // ---------------------------------------------------------------- reglas

    @Test fun spawnRulesMatchTheProgressionEngine() {
        for (target in listOf(32, 64, 128, 256, 512, 1024, 2048, 4096)) {
            repeat(500) { seed ->
                val want = ProgressionEngine.getNewTileValue(target, Random(seed))
                val got = SpawnRules.pick(SpawnStyle.NORMAL, target, Random(seed))
                assertEquals(want, got, "target $target seed $seed")
            }
        }
    }

    @Test fun foursRainDropsMoreFoursAndSpeedsUpTheLevel() {
        for (scale in listOf(64, 256, 1024)) {
            assertTrue(SpawnRules.expected(SpawnStyle.FOURS, scale) > SpawnRules.expected(SpawnStyle.NORMAL, scale) + 0.4)
        }
        val values = (0 until 2000).map { SpawnRules.pick(SpawnStyle.FOURS, 256, Random(it)) }.toSet()
        assertEquals(setOf(2, 4), values)
    }

    @Test fun minimumMovesAreRoughlyHalfTheTarget() {
        val m = LevelMath.minMoves(com.korkoor.pardos.domain.level.LevelBuilders.zen(1, 256, 4))
        assertTrue(m in 100..125, "256 → $m")
        val fast = LevelMath.minMoves(com.korkoor.pardos.domain.level.LevelBuilders.fours(1, 256, 4))
        assertTrue(fast < m * 0.85, "lluvia de 4 acorta: $fast vs $m")
    }

    @Test fun scoreGoalsMatchWhatABotScores() {
        // medido: llegar a 128 da ≈ 800 puntos, a 256 ≈ 1.800, a 512 ≈ 4.100
        assertEquals(768, LevelMath.tileScore(128))
        assertEquals(1792, LevelMath.tileScore(256))
        val sum = LevelMath.sumForScore(1792)
        assertTrue(sum in 250..262, "suma para 1792 puntos = $sum")
    }

    @Test fun starsFollowMovesAndClock() {
        val zen = com.korkoor.pardos.domain.level.LevelBuilders.zen(1, 128, 4)
        val min = LevelMath.minMoves(zen)
        assertEquals(3, LevelRules.stars(zen, LevelMath.threeStarMoves(zen), 0))
        assertEquals(2, LevelRules.stars(zen, LevelMath.threeStarMoves(zen) + 1, 0))
        assertEquals(2, LevelRules.stars(zen, LevelMath.twoStarMoves(zen), 0))
        assertEquals(1, LevelRules.stars(zen, LevelMath.twoStarMoves(zen) + 1, 0))
        assertEquals(1, LevelRules.stars(zen, min * 10, 0))

        val clock = com.korkoor.pardos.domain.level.LevelBuilders.clock(1, 128, 4)
        val limit = clock.timeLimitMs!!
        assertEquals(3, LevelRules.stars(clock, 500, limit / 2))
        assertEquals(2, LevelRules.stars(clock, 500, limit * 7 / 10))
        assertEquals(1, LevelRules.stars(clock, 500, limit * 95 / 100))
    }

    @Test fun goalCheckHandlesTilesTwinsAndScore() {
        fun t(v: Int, i: Int) = TileModel("t$i", v, i / 4, i % 4)
        assertTrue(LevelRules.isGoalReached(LevelGoal.REACH_TILE, 64, 1, listOf(t(8, 0), t(64, 1)), 0))
        assertFalse(LevelRules.isGoalReached(LevelGoal.REACH_TILE, 64, 1, listOf(t(8, 0), t(32, 1)), 0))
        assertFalse(LevelRules.isGoalReached(LevelGoal.REACH_TILE, 64, 2, listOf(t(64, 0), t(32, 1)), 0))
        assertTrue(LevelRules.isGoalReached(LevelGoal.REACH_TILE, 64, 2, listOf(t(64, 0), t(128, 1), t(2, 2)), 0))
        assertTrue(LevelRules.isGoalReached(LevelGoal.SCORE, 500, 1, emptyList(), 500))
        assertFalse(LevelRules.isGoalReached(LevelGoal.SCORE, 500, 1, listOf(t(1024, 0)), 499))
    }

    @Test fun textsDescribeTheGoal() {
        val l1 = LevelCatalog.spec(1)
        assertEquals("Llega a 32", l1.goalText())
        assertEquals("Suma 350 puntos", LevelCatalog.spec(3).goalText())
        assertEquals("Ten 2 fichas de 64 a la vez", LevelCatalog.spec(12).goalText())
        assertTrue(LevelCatalog.spec(8).ruleChips().any { it.endsWith("movimientos") })
        assertTrue(LevelCatalog.spec(7).ruleChips().contains("1 piedra"))
        assertTrue(LevelCatalog.spec(14).ruleChips().any { it.contains(":") })
    }
}
