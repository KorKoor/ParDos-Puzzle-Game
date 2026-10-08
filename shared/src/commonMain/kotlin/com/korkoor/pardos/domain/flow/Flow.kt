package com.korkoor.pardos.domain.flow

import com.korkoor.pardos.domain.level.GoalStats
import com.korkoor.pardos.domain.level.LevelCatalog
import com.korkoor.pardos.domain.level.LevelGoal
import com.korkoor.pardos.domain.level.LevelKind
import com.korkoor.pardos.domain.level.ladderRungs
import com.korkoor.pardos.domain.model.TileModel
import com.korkoor.pardos.domain.rewards.ChapterRewards

/**
 * Reglas del "estado de flow": cuatro piezas puras (probadas en `FlowTest`) que mantienen a la persona en el punto justo
 * entre aburrirse y frustrarse.
 *  1. [FlowMeter]: el ímpetu dentro del nivel (jugadas seguidas que fusionan) y su avisos.
 *  2. [WinStreak]: la racha de niveles ganados, con un bono de monedas que se enfría a la mitad (no se pierde de golpe) al perder.
 *  3. [AssistPolicy]: si un nivel se atasca, la ayuda sube poco a poco y a la vista (deshacer gratis, algo más de margen).
 *  4. [NearMiss] y [NextLevelTeaser]: el "casi lo logras" que invita a reintentar y el "un nivel más" que invita a seguir.
 */

/** Escalón de ímpetu dentro de un nivel. [minStreak] = jugadas seguidas que fusionan para alcanzarlo. */
enum class FlowTier(val minStreak: Int, val callout: String?, val coinBonusPct: Int) {
    CALM(0, null, 0),
    WARM(4, "¡En racha!", 5),
    HOT(8, "¡Imparable!", 10),
    FLOW(14, "¡FLOW!", 20)
}

object FlowMeter {
    fun tierOf(streak: Int): FlowTier = FlowTier.entries.last { streak >= it.minStreak }

    /** Una jugada que fusiona suma ímpetu; una que solo desliza lo apaga (pero no castiga: solo vuelve a empezar). */
    fun next(streak: Int, merged: Boolean): Int = if (merged) streak + 1 else 0

    /** El escalón nuevo si al pasar de [before] a [after] se sube de nivel (para avisar con un destello), o `null`. */
    fun tierUp(before: Int, after: Int): FlowTier? {
        val a = tierOf(after)
        return if (a.ordinal > tierOf(before).ordinal) a else null
    }

    /** Intensidad visual 0..1 del aura del tablero: crece despacio y llena a las 20 jugadas seguidas. */
    fun aura(streak: Int): Float = (streak / 20f).coerceIn(0f, 1f)

    /** Porcentaje extra de monedas por el mejor escalón alcanzado en el nivel. */
    fun coinBonusPct(peak: FlowTier): Int = peak.coinBonusPct
}

/** Racha de niveles ganados seguidos. */
object WinStreak {
    const val MAX_BONUS_STREAK = 10
    const val BONUS_PER_WIN_PCT = 5

    fun afterWin(streak: Int): Int = streak + 1

    /** Perder enfría la racha a la mitad (redondeando hacia abajo): duele lo justo para querer recuperarla, sin hundir la sesión. */
    fun afterLoss(streak: Int): Int = streak / 2

    /** Bono de monedas de la racha: +5 % por victoria seguida hasta +50 %. */
    fun coinBonusPct(streak: Int): Int = streak.coerceIn(0, MAX_BONUS_STREAK) * BONUS_PER_WIN_PCT

    data class Milestone(val streak: Int, val gems: Int, val undos: Int)

    val MILESTONES: List<Milestone> = listOf(
        Milestone(5, gems = 3, undos = 0),
        Milestone(10, gems = 5, undos = 1),
        Milestone(20, gems = 10, undos = 2),
        Milestone(40, gems = 20, undos = 3),
        Milestone(80, gems = 40, undos = 5)
    )

    /** Premio al llegar justo a [streak] victorias seguidas (solo una vez por racha), o `null`. */
    fun milestoneAt(streak: Int): Milestone? = MILESTONES.firstOrNull { it.streak == streak }

    /** Próximo hito que falta por alcanzar, para enseñar "faltan N". */
    fun nextMilestone(streak: Int): Milestone? = MILESTONES.firstOrNull { it.streak > streak }
}

/**
 * Ayuda que va creciendo cuando un mismo nivel se atasca. Siempre es visible para la persona (no se oculta): lo que se
 * busca es que un mal día no la haga abandonar, no que gane sin querer.
 */
object AssistPolicy {
    data class Assist(
        val tier: Int,
        /** Deshacer gratis acumulados que se han dado hasta este escalón. */
        val totalFreeUndos: Int,
        /** Margen extra de movimientos y de tiempo (1,0 = ninguno). */
        val limitFactor: Double,
        /** Puntos porcentuales extra de "evolución espontánea" de una ficha. */
        val luckyBoostPct: Int,
        val message: String?
    )

    fun tierFor(failedAttempts: Int): Int = when {
        failedAttempts >= 6 -> 3
        failedAttempts >= 4 -> 2
        failedAttempts >= 2 -> 1
        else -> 0
    }

    fun forAttempts(failedAttempts: Int): Assist = when (tierFor(failedAttempts)) {
        1 -> Assist(1, 1, 1.0, 0, "Te echamos una mano: +1 deshacer")
        2 -> Assist(2, 2, 1.12, 5, "Te damos algo de margen: +1 deshacer y un poco más de ventaja")
        3 -> Assist(3, 3, 1.25, 10, "Ánimo, ya casi: más margen y ayuda extra")
        else -> Assist(0, 0, 1.0, 0, null)
    }

    /** Deshacer gratis que toca dar ahora: los del escalón actual menos los ya dados antes en este nivel. */
    fun newUndos(grantedTotal: Int, assist: Assist): Int = (assist.totalFreeUndos - grantedTotal).coerceAtLeast(0)
}

/** El "casi lo logras" de las derrotas: una frase concreta que empuja a reintentar. */
object NearMiss {
    fun message(
        goal: LevelGoal, goalValue: Int, goalCount: Int, tiles: List<TileModel>, score: Int, stats: GoalStats, outOfMoves: Boolean
    ): String? {
        val values = tiles.map { it.value }
        return when (goal) {
            LevelGoal.REACH_TILE -> {
                val count = goalCount.coerceAtLeast(1)
                val have = values.count { it >= goalValue }
                val half = values.count { it == goalValue / 2 }
                when {
                    count == 1 && values.any { it == goalValue / 2 } && half >= 2 -> "¡Estabas a una fusión de la meta!"
                    count == 1 && values.maxOrNull() == goalValue / 2 -> "¡Casi! Tenías el ${goalValue / 2}: te faltó un paso"
                    count > 1 && have == count - 1 && half >= 2 -> "¡Te faltó una fusión para la segunda ficha!"
                    outOfMoves && (values.maxOrNull() ?: 0) >= goalValue / 4 && goalValue >= 8 -> "¡Casi! Llegaste a ${values.maxOrNull()}"
                    else -> null
                }
            }
            LevelGoal.SCORE -> if (goalValue > 0 && score * 100 >= goalValue * 80) "¡Te faltaron ${goalValue - score} puntos!" else null
            LevelGoal.MERGES -> {
                val left = goalValue - stats.merges
                if (left in 1..(goalValue / 5)) "¡Te faltaron solo $left fusiones!" else null
            }
            LevelGoal.LADDER -> {
                val present = values.toSet()
                val missing = ladderRungs(goalValue, goalCount).filter { it !in present }
                if (missing.size == 1) "¡Te faltó solo el peldaño ${missing.first()}!" else null
            }
            LevelGoal.HARVEST -> {
                val left = goalCount - stats.harvested
                if (left in 1..(goalCount / 4).coerceAtLeast(1)) "¡Te faltaron solo $left fichas de $goalValue!" else null
            }
            LevelGoal.COMBO -> if (stats.bestChain >= goalValue - 1 && stats.bestChain > 0) "¡Casi! Lograste un combo de ${stats.bestChain}" else null
        }
    }
}

/** Qué viene después del nivel que se acaba de ganar: la razón para pulsar "siguiente". */
object NextLevelTeaser {
    data class Teaser(
        val nextLevel: Int,
        val kind: LevelKind,
        val title: String,
        val isBoss: Boolean,
        /** Niveles que faltan para el cofre del capítulo (0 = el cofre se abre con este mismo nivel). */
        val levelsToChest: Int
    )

    fun after(level: Int): Teaser {
        val next = LevelCatalog.spec(level + 1)
        val chestLevel = ChapterRewards.lastLevelOf(ChapterRewards.chapterOf(level))
        return Teaser(level + 1, next.kind, next.title, next.isBoss, (chestLevel - level).coerceAtLeast(0))
    }
}
