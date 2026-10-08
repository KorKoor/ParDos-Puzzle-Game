package com.korkoor.pardos

import com.korkoor.pardos.domain.level.LevelCatalog
import com.korkoor.pardos.domain.level.LevelKind
import com.korkoor.pardos.domain.level.LevelRules
import com.korkoor.pardos.domain.level.LevelSpec
import com.korkoor.pardos.domain.level.SpawnRules
import com.korkoor.pardos.domain.model.TileModel
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Un bot juega muestras del catálogo con las reglas de cada nivel. Si un tipo de nivel es demasiado duro (o imposible), el
 * bot no lo gana casi nunca y esta prueba lo dice, con el nivel y la tasa de victorias, antes de que lo sufra un jugador.
 * El bot no tiene deshacer, ni ayudas, ni las evoluciones espontáneas: una persona lo hace mejor.
 */
class LevelFeasibilityTest {

    private data class Sig(
        val kind: LevelKind, val size: Int, val goal: Any, val value: Int, val count: Int, val stones: Set<Pair<Int, Int>>,
        val moves: Int?, val time: Long?, val spawn: Any, val startSum: Int, val storm: Any?
    )

    private fun sig(s: LevelSpec) = Sig(s.kind, s.boardSize, s.goal, s.goalValue, s.goalCount, s.stoneSet, s.moveLimit, s.timeLimitMs, s.spawn, s.startTiles.sumOf { it.value }, s.storm)

    private fun winRate(spec: LevelSpec, seeds: Int): Pair<Int, Int> {
        // El reloj se traduce a movimientos con una persona lenta: 1 movimiento por segundo
        val cap = spec.moveLimit ?: spec.timeLimitMs?.let { (it / 1000).toInt() } ?: 4000
        val start = spec.startTiles.mapIndexed { i, s -> TileModel("s$i", s.value, s.row, s.col, isNew = true) }
        var wins = 0
        for (seed in 1..seeds) {
            val run = GreedyBot.play(
                size = spec.boardSize, blocked = spec.stoneSet, seed = seed * 7L + spec.id, maxMoves = cap, startTiles = start,
                spawn = { SpawnRules.pick(spec.spawn, spec.scaleTile, it) },
                targetTile = if (spec.goal == com.korkoor.pardos.domain.level.LevelGoal.REACH_TILE) spec.goalValue else null,
                storm = spec.storm, comboSize = LevelRules.comboSize(spec),
                goalReached = { tiles, score, stats -> LevelRules.isGoalReached(spec, tiles, score, stats) }
            )
            if (run.reached) wins++
        }
        return wins to seeds
    }

    @Test fun botWinsASampleOfTheCampaign() {
        val seeds = 6
        val sample = LevelCatalog.all()
        val unique = sample.distinctBy { sig(it) }
        val report = StringBuilder()
        val bad = mutableListOf<String>()
        var totalWins = 0
        for (spec in unique) {
            val (wins, n) = winRate(spec, seeds)
            // El bot no planifica: en los combos y en la última parte (reglas más duras) se le pide menos que a una persona
            val need = when {
                spec.goal == com.korkoor.pardos.domain.level.LevelGoal.COMBO -> 2
                spec.isBoss -> 3
                else -> 3
            }
            val line = "FEAS #${spec.id} ${spec.kind} ${spec.title} ${spec.boardSize}x${spec.boardSize} ${spec.goalText()} stones=${spec.stones.size} " +
                "moves=${spec.moveLimit} time=${spec.timeLimitMs?.div(1000)} wins=$wins/$n"
            println(line)
            totalWins += wins
            if (wins < need) bad.add(line)
        }
        println("FEAS total ${unique.size} niveles distintos, victorias $totalWins/${unique.size * seeds}")
        assertTrue(bad.isEmpty(), "Niveles demasiado difíciles para el bot:\n" + bad.joinToString("\n") + report)
    }
}
