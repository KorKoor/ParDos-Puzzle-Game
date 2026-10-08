package com.korkoor.pardos

import com.korkoor.pardos.domain.level.LevelBuilders
import com.korkoor.pardos.domain.level.LevelRules
import com.korkoor.pardos.domain.level.LevelSpec
import com.korkoor.pardos.domain.level.SpawnRules
import com.korkoor.pardos.domain.level.Storm
import com.korkoor.pardos.domain.level.StonePatterns
import com.korkoor.pardos.domain.model.TileModel
import kotlin.test.Test

/** Medición (no es una prueba): imprime cuánto gana el bot con distintos ajustes, para calibrar los niveles nuevos. */
class CalibrationTest {
    private fun rate(spec: LevelSpec, seeds: Int = 12): Int {
        val cap = spec.moveLimit ?: spec.timeLimitMs?.let { (it / 1000).toInt() } ?: 4000
        val start = spec.startTiles.mapIndexed { i, s -> TileModel("s$i", s.value, s.row, s.col, isNew = true) }
        var wins = 0
        for (seed in 1..seeds) {
            val run = GreedyBot.play(
                size = spec.boardSize, blocked = spec.stoneSet, seed = seed * 7L + 3, maxMoves = cap, startTiles = start,
                spawn = { SpawnRules.pick(spec.spawn, spec.scaleTile, it) },
                targetTile = if (spec.goal == com.korkoor.pardos.domain.level.LevelGoal.REACH_TILE) spec.goalValue else null,
                storm = spec.storm, comboSize = LevelRules.comboSize(spec), harvestValue = LevelRules.harvestValue(spec), drops = spec.dropsPerMove, allowed = spec.twist::allows, phase = spec.phase2,
                phaseSpawn = spec.phase2?.spawn?.let { st -> { r: kotlin.random.Random -> SpawnRules.pick(st, spec.scaleTile, r) } },
                goalReached = { t, sc, st -> LevelRules.isGoalReached(spec, t, sc, st) }
            )
            if (run.reached) wins++
        }
        return wins
    }

    /** Movimientos medios que gasta el bot en llegar a la meta (sin tope). */
    private fun avgMoves(spec: LevelSpec, seeds: Int = 12): String {
        val start = spec.startTiles.mapIndexed { i, s -> TileModel("s$i", s.value, s.row, s.col, isNew = true) }
        val ms = ArrayList<Int>()
        var fails = 0
        for (seed in 1..seeds) {
            val run = GreedyBot.play(
                size = spec.boardSize, blocked = spec.stoneSet, seed = seed * 7L + 3, maxMoves = 5000, startTiles = start,
                spawn = { SpawnRules.pick(spec.spawn, spec.scaleTile, it) }, storm = spec.storm, comboSize = LevelRules.comboSize(spec), harvestValue = LevelRules.harvestValue(spec), drops = spec.dropsPerMove, allowed = spec.twist::allows,
                goalReached = { t, sc, st -> LevelRules.isGoalReached(spec, t, sc, st) }
            )
            if (run.reached) ms.add(run.moves) else fails++
        }
        return "avg=${ms.average().toInt()} min=${ms.minOrNull()} max=${ms.maxOrNull()} fails=$fails"
    }

    @Test fun measurePatterns() {
        if ((System.getenv("CAL") ?: "") != "pat") return
        for (pat in StonePatterns.all) {
            val e = pat.maxExp
            val w = rate(LevelBuilders.stones(1, 1 shl e, pat), 10)
            val w1 = rate(LevelBuilders.stones(1, 1 shl (e - 1), pat), 10)
            println("CAL PAT ${pat.id} sev=${pat.severity} exp=$e -> $w/10   (exp-1: $w1/10)")
        }
    }

    @Test fun measureMarathon() {
        if ((System.getenv("CAL") ?: "") != "mm") return
        for (size in listOf(4, 5)) for (m in listOf(60, 100, 200, 300)) println("CAL MM size=$size merges=$m " + avgMoves(LevelBuilders.marathon(1, m, size, 100.0)))
    }

    private fun line(label: String, spec: LevelSpec) = println("CAL $label ${spec.goalText()} size=${spec.boardSize} limit=${spec.moveLimit ?: spec.timeLimitMs?.div(1000)} -> ${rate(spec)}/12")

    @Test fun calibrate() {
        val what = System.getProperty("cal") ?: System.getenv("CAL") ?: "all"
        if (what == "heavy" || what == "all") {
            for (size in listOf(4, 5)) for (e in 6..10) line("HEAVY", LevelBuilders.heavy(1, 1 shl e, size))
        }
        if (what == "ladder" || what == "all") {
            for ((top, steps) in listOf(64 to 3, 128 to 3, 128 to 4, 256 to 3, 256 to 4, 256 to 5, 512 to 4, 512 to 5, 1024 to 4, 1024 to 5)) {
                line("LADDER", LevelBuilders.ladder(1, top, steps, 4))
                if (steps >= 4) line("LADDER", LevelBuilders.ladder(1, top, steps, 5))
            }
        }
        if (what == "marathon" || what == "all") {
            for (m in listOf(160, 200, 240)) for (slack in listOf(1.12, 1.2)) line("MARATHON", LevelBuilders.marathon(1, m, 4, slack))
            for (m in listOf(260, 300, 340)) for (slack in listOf(1.12, 1.2)) line("MARATHON", LevelBuilders.marathon(1, m, 5, slack))
            for (pat in listOf("5-centro", "5-esq-dos", "5-ventanas", "5-isla")) line("MARATHON $pat", LevelBuilders.marathon(1, 300, 5, 1.2, StonePatterns.byId(pat)))
            for (pat in listOf("4-esq-nw", "4-dos-nwse")) line("MARATHON $pat", LevelBuilders.marathon(1, 200, 4, 1.2, StonePatterns.byId(pat)))
            line("MARATHON heavy 5", LevelBuilders.marathon(1, 300, 5, 1.2, null, com.korkoor.pardos.domain.level.SpawnStyle.HEAVY))
            line("MARATHON heavy 4", LevelBuilders.marathon(1, 200, 4, 1.2, null, com.korkoor.pardos.domain.level.SpawnStyle.HEAVY))
        }
        if (what == "combo" || what == "all") {
            for (size in listOf(4, 5)) for (pairs in listOf(3, 4)) for (times in listOf(1, 2, 3)) for (slack in listOf(2.2, 3.5, 5.0)) {
                line("COMBO", LevelBuilders.combo(1, pairs, times, size, slack))
            }
        }
        if (what == "storm" || what == "all") {
            for (e in 8..10) for (st in listOf(Storm(4, 5, 4), Storm(3, 4, 4), Storm(3, 6, 4), Storm(2, 4, 4), Storm(3, 8, 3))) line("STORM $st", LevelBuilders.storm(1, 1 shl e, 4, st))
        }
        if (what == "sprint" || what == "all") {
            for (e in 7..10) for (slack in listOf(1.5, 1.4, 1.3, 1.2, 1.1)) line("SPRINT", LevelBuilders.sprint(1, 1 shl e, 4, slack))
            for (e in 7..10) for (spc in listOf(1.9, 1.6, 1.4, 1.2)) line("CLOCK", LevelBuilders.clock(1, 1 shl e, 4, spc))
        }
        if (what == "new2" || what == "all") {
            for (size in listOf(4, 5)) for ((v, counts) in listOf(16 to listOf(8, 14, 24), 32 to listOf(6, 10, 18), 64 to listOf(4, 8, 14), 128 to listOf(3, 6, 10))) for (c in counts) line("HARVEST", LevelBuilders.harvest(1, v, c, size))
            for (size in listOf(4, 5)) for (e in 6..10) line("DOUBLE", LevelBuilders.doubleDrop(1, 1 shl e, size))
            for (tw in listOf(com.korkoor.pardos.domain.level.Twist.NO_UP, com.korkoor.pardos.domain.level.Twist.NO_DOWN, com.korkoor.pardos.domain.level.Twist.NO_LEFT, com.korkoor.pardos.domain.level.Twist.NO_RIGHT))
                for (e in 7..9) line("BLOCK $tw", LevelBuilders.twisted(LevelBuilders.zen(1, 1 shl e, 4), tw))
        }
        if (what == "big" || what == "all") {
            for (e in 8..11) { line("ZEN6", LevelBuilders.zen(1, 1 shl e, 6)); line("ZEN5", LevelBuilders.zen(1, 1 shl e, 5)) }
        }
    }
}
