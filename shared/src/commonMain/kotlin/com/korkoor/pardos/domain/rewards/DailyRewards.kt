package com.korkoor.pardos.domain.rewards

/** Recompensa de un día del calendario. */
data class Reward(
    val coins: Int,
    val gems: Int = 0,
    val isChest: Boolean = false
)

/** Calendario de 7 días: sube cada día y el séptimo es un cofre. Luego se repite. */
object DailyRewards {
    val cycle: List<Reward> = listOf(
        Reward(coins = 50),
        Reward(coins = 75),
        Reward(coins = 100),
        Reward(coins = 125),
        Reward(coins = 150, gems = 1),
        Reward(coins = 200, gems = 2),
        Reward(coins = 350, gems = 5, isChest = true)
    )

    /** Día dentro del ciclo (1..7) para una racha dada (>= 1). */
    fun dayInCycle(streak: Int): Int = ((streak.coerceAtLeast(1) - 1) % cycle.size) + 1

    fun rewardForStreak(streak: Int): Reward = cycle[dayInCycle(streak) - 1]
}

/** Estado persistente de la racha. Los días son "días locales desde epoch". */
data class StreakState(
    val streak: Int = 0,
    val best: Int = 0,
    val lastDay: Int = 0
)

enum class StreakChange { NONE, STARTED, CONTINUED, RESET, SAVED_BY_FREEZE }

data class StreakResult(
    val state: StreakState,
    val change: StreakChange,
    val freezesLeft: Int
)

object StreakCalculator {
    /**
     * Se llama al abrir el juego (una vez por día cuenta).
     * [freezes]: escudos de racha; uno cubre exactamente un día perdido.
     */
    fun onOpen(state: StreakState, today: Int, freezes: Int = 0): StreakResult {
        if (state.lastDay == 0) {
            return StreakResult(StreakState(1, maxOf(1, state.best), today), StreakChange.STARTED, freezes)
        }
        val gap = today - state.lastDay
        return when {
            gap <= 0 -> StreakResult(state, StreakChange.NONE, freezes)
            gap == 1 -> {
                val s = state.streak + 1
                StreakResult(StreakState(s, maxOf(s, state.best), today), StreakChange.CONTINUED, freezes)
            }
            gap == 2 && freezes > 0 -> {
                val s = state.streak + 1
                StreakResult(StreakState(s, maxOf(s, state.best), today), StreakChange.SAVED_BY_FREEZE, freezes - 1)
            }
            else -> StreakResult(StreakState(1, state.best, today), StreakChange.RESET, freezes)
        }
    }
}
