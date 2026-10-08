package com.korkoor.pardos

import com.korkoor.pardos.domain.level.GoalStats
import com.korkoor.pardos.domain.level.Storm
import com.korkoor.pardos.domain.level.StormRules
import com.korkoor.pardos.domain.level.StormStone
import com.korkoor.pardos.domain.logic.Direction
import com.korkoor.pardos.domain.logic.GameEngine
import com.korkoor.pardos.domain.model.TileModel
import kotlin.math.abs
import kotlin.math.ln
import kotlin.random.Random

/**
 * Jugador automático para calibrar los niveles (mira dos jugadas por delante con heurísticas clásicas: huecos libres,
 * monotonía, suavidad y la ficha mayor en una esquina). Si este bot gana a menudo, una persona gana casi siempre
 * (además tiene deshacer, ayudas y las evoluciones espontáneas que el bot no usa).
 */
object GreedyBot {

    data class Run(val reached: Boolean, val moves: Int, val score: Int, val maxTile: Int)

    fun tile(r: Int, c: Int, v: Int) = TileModel("t${r}_${c}_$v", v, r, c, isNew = true)

    fun play(
        size: Int,
        blocked: Set<Pair<Int, Int>>,
        seed: Long,
        maxMoves: Int,
        startTiles: List<TileModel> = emptyList(),
        spawn: (Random) -> Int,
        /** Ficha meta (si la hay): al acercarse, el bot prioriza fusionar para cerrar el nivel, como haría una persona. */
        targetTile: Int? = null,
        /** Tormenta del nivel (si la hay): las piedras temporales caen y se van como en la partida real. */
        storm: Storm? = null,
        /** Pares por movimiento que pide un nivel de combo (0 = no es combo): el bot los busca. */
        comboSize: Int = 0,
        /** Valor de la ficha que se cosecha (0 = no es una cosecha). */
        harvestValue: Int = 0,
        /** Fichas nuevas por jugada (2 = doble caída). */
        drops: Int = 1,
        /** Direcciones permitidas (los callejones prohíben una). */
        allowed: (Direction) -> Boolean = { true },
        /** Segunda fase de un jefe y el reparto de fichas que trae (si lo cambia). */
        phase: com.korkoor.pardos.domain.level.BossPhase? = null,
        phaseSpawn: ((Random) -> Int)? = null,
        goalReached: (tiles: List<TileModel>, score: Int, stats: GoalStats) -> Boolean
    ): Run {
        val rng = Random(seed)
        val stormRng = Random(seed xor 0x5DEECE66DL)
        var stones: List<StormStone> = emptyList()
        var curBlocked = blocked
        var engine = GameEngine(size, rng, curBlocked)
        var probe = GameEngine(size, Random(seed xor 0x9E3779B9L), curBlocked)
        var stats = GoalStats()
        var curStorm = storm
        var curAllowed = allowed
        var curSpawn = spawn
        var phased = false
        var tiles = startTiles.toList()
        if (tiles.isEmpty()) repeat(if (size >= 5) 4 else if (size == 4) 3 else 2) {
            engine.spawnTileWithSpecificValue(tiles, spawn(rng))?.let { t -> tiles = tiles + t }
        }
        var score = 0
        var moves = 0
        while (moves < maxMoves) {
            if (phase != null && !phased && moves >= phase.atMoves) {
                phased = true
                phase.storm?.let { curStorm = it }
                phase.twist?.let { tw -> curAllowed = tw::allows }
                phaseSpawn?.let { curSpawn = it }
            }
            var best: Pair<List<TileModel>, Int>? = null
            var bestValue = Double.NEGATIVE_INFINITY
            for (dir in Direction.entries) {
                if (!curAllowed(dir)) continue
                val (moved, gained) = engine.move(tiles, dir)
                if (sameBoard(tiles, moved)) continue
                // Dos jugadas: se promedia el mejor segundo movimiento tras unas cuantas apariciones posibles de un 2
                val closing = targetTile != null && (moved.maxOfOrNull { it.value } ?: 0) * 2 >= targetTile
                val pairs = (tiles.size - moved.size).coerceAtLeast(0)
                // Combo: además de fusionar ahora, prepara el tablero para que el siguiente deslizamiento fusione muchos pares
                val chainBonus = if (comboSize > 0) {
                    val potential = Direction.entries.maxOf { d -> (moved.size - probe.move(moved, d).first.size).coerceAtLeast(0) }
                    pairs * pairs * 14.0 + potential * potential * (if (potential >= comboSize) 400.0 else 22.0)
                } else 0.0
                val value = lookahead(probe, moved, size, curBlocked) + gained * (if (closing) 0.4 else 0.02) + chainBonus
                if (value > bestValue) { bestValue = value; best = moved to gained }
            }
            val (moved, gained) = best ?: return Run(false, moves, score, tiles.maxOfOrNull { it.value } ?: 0)
            moves++
            score += gained
            stats = stats.after(
                (tiles.size - moved.size).coerceAtLeast(0), comboSize,
                if (harvestValue > 0) moved.count { it.isMerged && it.value == harvestValue } else 0
            )
            tiles = moved
            if (goalReached(tiles, score, stats)) return Run(true, moves, score, tiles.maxOf { it.value })
            repeat(drops) { engine.spawnTileWithSpecificValue(tiles, curSpawn(rng))?.let { tiles = tiles + it } }
            if (goalReached(tiles, score, stats)) return Run(true, moves, score, tiles.maxOf { it.value })
            if (curStorm != null) {
                stones = StormRules.step(curStorm!!, moves, stones, tiles, blocked, size, stormRng)
                val now = StormRules.blockedNow(blocked, stones)
                if (now != curBlocked) {
                    curBlocked = now
                    engine = GameEngine(size, rng, curBlocked)
                    probe = GameEngine(size, Random(seed xor 0x9E3779B9L), curBlocked)
                }
            }
            if (engine.isGameOver(tiles)) break
        }
        return Run(false, moves, score, tiles.maxOfOrNull { it.value } ?: 0)
    }

    private fun lookahead(engine: GameEngine, after: List<TileModel>, size: Int, blocked: Set<Pair<Int, Int>>): Double {
        val occupied = after.map { it.row to it.col }.toSet()
        val empty = (0 until size).flatMap { r -> (0 until size).map { c -> r to c } }.filter { it !in occupied && it !in blocked }
        if (empty.isEmpty()) return evaluate(after, size, blocked) - 50.0
        val samples = if (empty.size <= 4) empty else empty.shuffled(Random(after.sumOf { it.value * (it.row * size + it.col + 1) }.toLong())).take(4)
        var total = 0.0
        for ((r, c) in samples) {
            val board = after + tile(r, c, 2)
            var best = evaluate(board, size, blocked) - 5.0
            for (dir in Direction.entries) {
                val (m, _) = engine.move(board, dir)
                if (sameBoard(board, m)) continue
                best = maxOf(best, evaluate(m, size, blocked))
            }
            total += best
        }
        return total / samples.size
    }

    private fun sameBoard(a: List<TileModel>, b: List<TileModel>): Boolean {
        if (a.size != b.size) return false
        val m = a.associate { (it.row to it.col) to it.value }
        return b.all { m[it.row to it.col] == it.value }
    }

    private fun evaluate(tiles: List<TileModel>, size: Int, blocked: Set<Pair<Int, Int>>): Double {
        val free = size * size - blocked.size - tiles.size
        val g = Array(size) { DoubleArray(size) { -1.0 } }   // -1 = vacío o piedra
        tiles.forEach { g[it.row][it.col] = log2(it.value) }
        var smooth = 0.0
        var mono = 0.0
        for (r in 0 until size) for (c in 0 until size) {
            if (g[r][c] < 0) continue
            if (c + 1 < size && g[r][c + 1] >= 0) smooth -= abs(g[r][c] - g[r][c + 1])
            if (r + 1 < size && g[r + 1][c] >= 0) smooth -= abs(g[r][c] - g[r + 1][c])
        }
        fun lineMono(values: List<Double>) {
            var inc = 0.0; var dec = 0.0
            val v = values.filter { it >= 0 }
            for (i in 0 until v.size - 1) { if (v[i] > v[i + 1]) dec += v[i] - v[i + 1] else inc += v[i + 1] - v[i] }
            mono -= minOf(inc, dec)
        }
        for (i in 0 until size) {
            lineMono((0 until size).map { g[i][it] })
            lineMono((0 until size).map { g[it][i] })
        }
        val max = tiles.maxOfOrNull { it.value } ?: 0
        val corners = listOf(0 to 0, 0 to size - 1, size - 1 to 0, size - 1 to size - 1).filter { it !in blocked }
        val cornerBonus = if (corners.any { (r, c) -> g[r][c] == log2(max) }) log2(max) else 0.0
        return free * 2.7 + mono * 1.0 + smooth * 0.5 + cornerBonus * 1.2
    }

    private fun log2(v: Int): Double = ln(v.toDouble()) / ln(2.0)
}
