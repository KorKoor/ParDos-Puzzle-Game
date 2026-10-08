package com.korkoor.pardos.domain.level

import com.korkoor.pardos.domain.model.TileModel
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.random.Random

/** Reparto de las fichas nuevas. Para `NORMAL` es exactamente el de `ProgressionEngine.getNewTileValue`. */
object SpawnRules {
    /** Valor de la ficha nueva dado un número uniforme [u] en [0, 1). */
    fun valueFor(style: SpawnStyle, scale: Int, u: Double): Int = when (style) {
        SpawnStyle.NORMAL -> when {
            scale >= 2048 && u < 0.04 -> 16
            scale >= 1024 && u < 0.06 -> 8
            u < (if (scale <= 128) 0.06 else 0.15) -> 4
            else -> 2
        }
        SpawnStyle.FOURS -> when {
            scale >= 1024 && u < 0.04 -> 8
            u < 0.42 -> 4
            else -> 2
        }
        SpawnStyle.HEAVY -> when {
            u < 0.08 -> 2
            u < 0.60 -> 4
            scale >= 1024 && u >= 0.96 -> 16
            else -> 8
        }
    }

    fun pick(style: SpawnStyle, scale: Int, random: Random): Int = valueFor(style, scale, random.nextDouble())

    /** Valor medio de una ficha nueva (cada movimiento válido suma esto al tablero). */
    fun expected(style: SpawnStyle, scale: Int): Double {
        val steps = 1000
        var sum = 0.0
        for (i in 0 until steps) sum += valueFor(style, scale, (i + 0.5) / steps)
        return sum / steps
    }
}

/**
 * Cuentas que sirven para ajustar los niveles sin adivinar: cada movimiento válido añade una ficha nueva, así que
 * llegar a una meta cuesta casi siempre el mismo número de movimientos (la suma necesaria entre lo que aporta cada ficha
 * nueva). Lo medido con un bot: se gana en 1,05–1,2 veces ese mínimo; el resto es no atascarse.
 */
object LevelMath {
    fun log2(v: Int): Int = 31 - v.countLeadingZeroBits()

    /** Puntos que se acumulan al construir una ficha desde fichas de 2 (cada fusión suma el valor resultante). */
    fun tileScore(tile: Int): Int = tile * (log2(tile) - 1)

    /** Suma de fichas con la que se llega a [score] puntos construyendo bien. */
    fun sumForScore(score: Int): Int {
        var lo = 4.0
        var hi = score.toDouble().coerceAtLeast(8.0)
        repeat(40) {
            val mid = (lo + hi) / 2
            if (mid * (ln(mid) / ln(2.0) - 1) >= score) hi = mid else lo = mid
        }
        return ceil(hi).toInt()
    }

    /** Fichas con las que se empieza: las del nivel, o las 2–4 normales. */
    fun initialTileCount(boardSize: Int): Int = when {
        boardSize >= 5 -> 4
        boardSize == 4 -> 3
        else -> 2
    }

    /** Suma de fichas que hay que construir para la meta (las metas de contar fusiones no la usan). */
    fun neededSum(spec: LevelSpec): Int = when (spec.goal) {
        LevelGoal.REACH_TILE -> spec.goalValue * spec.goalCount
        LevelGoal.SCORE -> sumForScore(spec.goalValue)
        LevelGoal.LADDER -> ladderRungs(spec.goalValue, spec.goalCount).sum()
        LevelGoal.HARVEST -> spec.goalValue * spec.goalCount
        LevelGoal.MERGES, LevelGoal.COMBO -> 0
    }

    /** Menor número de movimientos en el que cabe ganar el nivel. */
    fun minMoves(spec: LevelSpec): Int {
        // Cada movimiento válido añade una ficha y cada fusión quita una: en una partida normal hay ~1 fusión por movimiento
        if (spec.goal == LevelGoal.MERGES) return spec.goalValue
        // Un combo de N pares necesita preparar el tablero: se estima unos 12 movimientos por cada par exigido y repetición
        if (spec.goal == LevelGoal.COMBO) return spec.goalCount * (10 + 6 * spec.goalValue)
        val avg = SpawnRules.expected(spec.spawn, spec.scaleTile) * spec.dropsPerMove
        val needed = neededSum(spec)
        val initial = if (spec.startTiles.isNotEmpty()) spec.startTiles.sumOf { it.value }.toDouble()
        else LevelMath.initialTileCount(spec.boardSize) * avg
        return ceil((needed - initial) / avg).toInt().coerceAtLeast(1)
    }

    /** Holguras para las estrellas: las metas de contar fusiones son más apretadas porque casi siempre se fusiona 1 vez por jugada. */
    private fun threeStarFactor(spec: LevelSpec) = when (spec.goal) { LevelGoal.MERGES -> 1.12; LevelGoal.COMBO -> 1.6; else -> 1.3 }
    private fun twoStarFactor(spec: LevelSpec) = when (spec.goal) { LevelGoal.MERGES -> 1.35; LevelGoal.COMBO -> 2.4; else -> 1.75 }

    /** Con este número de movimientos o menos: 3 estrellas. */
    fun threeStarMoves(spec: LevelSpec): Int = ceil(minMoves(spec) * threeStarFactor(spec)).toInt()

    /** Con este número o menos: 2 estrellas. */
    fun twoStarMoves(spec: LevelSpec): Int = ceil(minMoves(spec) * twoStarFactor(spec)).toInt()

    /** Movimientos permitidos si el nivel es un sprint: el mínimo por la holgura (1,75 generoso · 1,5 normal) y 6 de margen. */
    fun moveLimit(spec: LevelSpec, slack: Double): Int = ceil(minMoves(spec) * slack).toInt() + 6   // +6: en metas pequeñas unas pocas fichas sueltas pesan mucho

    /** Tiempo para un nivel contrarreloj: [secondsPerMove] por cada movimiento mínimo, en milisegundos. */
    fun timeLimitMs(spec: LevelSpec, secondsPerMove: Double): Long =
        (((minMoves(spec) * secondsPerMove).toLong() + 4) / 5 * 5) * 1000L   // múltiplos de 5 s

    /** Mayor ficha razonable con [freeCells] casillas libres (un tope de seguridad, no el objetivo). */
    fun practicalCap(freeCells: Int): Int = 1 shl (freeCells - 2).coerceIn(4, 20)

    /** Valor de ficha que ya dejaría ganado el nivel: sirve para el tablero inicial de los niveles con ventaja. */
    fun startSum(tiles: List<StartTile>): Int = tiles.sumOf { it.value }

    fun hasReachedTile(tiles: List<TileModel>, value: Int, count: Int): Boolean {
        var n = 0
        for (t in tiles) if (t.value >= value && ++n >= count) return true
        return false
    }
}
