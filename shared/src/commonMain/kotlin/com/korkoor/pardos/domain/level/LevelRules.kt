package com.korkoor.pardos.domain.level

import com.korkoor.pardos.domain.model.TileModel

/**
 * Lo que se cuenta durante la partida para las metas que no se ven en el tablero: fusiones totales, el mayor número de
 * pares fusionados en un solo movimiento y cuántas veces se logró el combo pedido.
 */
data class GoalStats(val merges: Int = 0, val bestChain: Int = 0, val comboHits: Int = 0) {
    /** Estadística tras un movimiento que fusionó [pairs] pares; [comboNeeded] es el tamaño del combo de la meta (0 si no hay). */
    fun after(pairs: Int, comboNeeded: Int): GoalStats = GoalStats(
        merges = merges + pairs,
        bestChain = maxOf(bestChain, pairs),
        comboHits = comboHits + if (comboNeeded > 0 && pairs >= comboNeeded) 1 else 0
    )
}

/** Reglas de partida de un nivel: cuándo se gana, cuántas estrellas da y cuánto paga. */
object LevelRules {

    /** ¿El tablero cumple la meta? Se comprueba tras cada movimiento (y tras la ficha nueva). */
    fun isGoalReached(
        goal: LevelGoal, goalValue: Int, goalCount: Int, tiles: List<TileModel>, score: Int, stats: GoalStats = GoalStats()
    ): Boolean = when (goal) {
        LevelGoal.SCORE -> score >= goalValue
        LevelGoal.REACH_TILE -> LevelMath.hasReachedTile(tiles, goalValue, goalCount.coerceAtLeast(1))
        LevelGoal.LADDER -> {
            val present = HashSet<Int>()
            tiles.forEach { present.add(it.value) }
            ladderRungs(goalValue, goalCount.coerceAtLeast(1)).all { it in present }
        }
        LevelGoal.MERGES -> stats.merges >= goalValue
        LevelGoal.COMBO -> stats.comboHits >= goalCount.coerceAtLeast(1)
    }

    fun isGoalReached(spec: LevelSpec, tiles: List<TileModel>, score: Int, stats: GoalStats = GoalStats()): Boolean =
        isGoalReached(spec.goal, spec.goalValue, spec.goalCount, tiles, score, stats)

    /** Cuánto del tamaño de combo hay que lograr por movimiento (0 si la meta no es un combo). */
    fun comboSize(spec: LevelSpec): Int = if (spec.goal == LevelGoal.COMBO) spec.goalValue else 0

    /**
     * Avance hacia la meta de 0 a 1 para la barra del nivel (cada tipo de meta lo mide a su manera).
     */
    fun progress(goal: LevelGoal, goalValue: Int, goalCount: Int, tiles: List<TileModel>, score: Int, stats: GoalStats): Float = when (goal) {
        LevelGoal.SCORE -> if (goalValue <= 0) 0f else (score.toFloat() / goalValue).coerceIn(0f, 1f)
        LevelGoal.MERGES -> if (goalValue <= 0) 0f else (stats.merges.toFloat() / goalValue).coerceIn(0f, 1f)
        LevelGoal.COMBO -> {
            val need = goalCount.coerceAtLeast(1)
            val partial = if (goalValue <= 0) 0f else (stats.bestChain.toFloat() / goalValue).coerceIn(0f, 1f)
            ((stats.comboHits.coerceAtMost(need) + if (stats.comboHits >= need) 0f else partial * 0.8f) / need).coerceIn(0f, 1f)
        }
        LevelGoal.LADDER -> {
            val rungs = ladderRungs(goalValue, goalCount.coerceAtLeast(1))
            val present = tiles.map { it.value }.toSet()
            if (rungs.isEmpty()) 0f else rungs.count { it in present }.toFloat() / rungs.size
        }
        LevelGoal.REACH_TILE -> 0f // lo calcula el tablero en escala logarítmica
    }

    /**
     * Estrellas de una victoria.
     *  - Con reloj: según el tiempo gastado (mitad del límite o menos: 3; hasta el 80 %: 2).
     *  - Sin reloj: según los movimientos usados frente al mínimo posible ([LevelMath.threeStarMoves] / `twoStarMoves`).
     *  - Con las dos reglas, la peor de las dos.
     * Ganar siempre da al menos 1.
     */
    fun stars(spec: LevelSpec, moves: Int, timeUsedMs: Long): Int {
        val byMoves = when {
            moves <= LevelMath.threeStarMoves(spec) -> 3
            moves <= LevelMath.twoStarMoves(spec) -> 2
            else -> 1
        }
        val limit = spec.timeLimitMs
        if (limit == null || limit <= 0L) return byMoves
        val ratio = timeUsedMs.toDouble() / limit
        val byTime = when {
            ratio <= 0.5 -> 3
            ratio <= 0.8 -> 2
            else -> 1
        }
        return if (spec.hasMoveLimit) minOf(byMoves, byTime) else byTime
    }

    /** Monedas extra por superar niveles con reglas: los jefes pagan más. */
    fun coinBonus(spec: LevelSpec): Int = when (spec.kind) {
        LevelKind.BOSS -> 30
        LevelKind.ZEN, LevelKind.SCORE, LevelKind.FOURS, LevelKind.HEADSTART -> 0
        else -> 5
    }

    /** Frase que explica cómo conseguir las 3 estrellas, para la tarjeta del nivel. */
    fun threeStarHint(spec: LevelSpec): String {
        val limit = spec.timeLimitMs
        return when {
            limit != null && !spec.hasMoveLimit -> "Tres estrellas si terminas en menos de ${formatClock(limit / 2)}"
            else -> "Tres estrellas si terminas en ${LevelMath.threeStarMoves(spec)} movimientos o menos"
        }
    }
}
