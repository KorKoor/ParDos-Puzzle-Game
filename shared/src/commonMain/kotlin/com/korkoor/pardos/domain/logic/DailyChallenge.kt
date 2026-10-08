package com.korkoor.pardos.domain.logic

import com.korkoor.pardos.domain.level.LevelBuilders
import com.korkoor.pardos.domain.level.LevelKind
import com.korkoor.pardos.domain.level.LevelMath
import com.korkoor.pardos.domain.level.LevelSpec
import com.korkoor.pardos.domain.level.StonePatterns
import kotlin.random.Random

/** Configuración del reto diario. Es la misma para cualquier jugador que comparta el día. */
data class DailyConfig(
    val day: Int,
    val seed: Long,
    val boardSize: Int,
    /** Ficha de referencia del día (1.024 o 2.048); la meta real de este reto está en [spec]. */
    val target: Int,
    val themeIndex: Int,
    /** Reglas del día (cada día de la semana tiene su tipo de reto). */
    val spec: LevelSpec
)

object DailyChallenge {
    /** Número de temas disponibles (índices 0..THEME_COUNT-1). */
    const val THEME_COUNT = 5

    /** Un tipo de reto por día de la semana (lunes = 0): hay motivo para volver cada día y los domingos se descansa. */
    val WEEK: List<LevelKind> = listOf(
        LevelKind.STONES, LevelKind.SPRINT, LevelKind.FOURS, LevelKind.SCORE, LevelKind.TWINS, LevelKind.HEADSTART, LevelKind.ZEN
    )

    /** Día de la semana de un día de época (0 = lunes). El día 0 (1-1-1970) fue jueves. */
    fun weekday(epochDay: Int): Int = ((epochDay + 3) % 7 + 7) % 7

    fun forDay(epochDay: Int): DailyConfig {
        // La semilla del tablero y la de la configuración salen del día, pero son independientes
        val seed = epochDay.toLong() * 7919L + 104729L
        val config = Random(seed xor 0x5DEECE66DL)
        val size = if (config.nextBoolean()) 4 else 5
        val target = if (config.nextBoolean()) 1024 else 2048
        val theme = config.nextInt(THEME_COUNT)
        val spec = specFor(WEEK[weekday(epochDay)], size, target, config)
        return DailyConfig(epochDay, seed, size, target, theme, spec)
    }

    /** Reto del tipo [kind] sobre un tablero de [size] con la ficha de referencia [target] (1.024 o 2.048). */
    internal fun specFor(kind: LevelKind, size: Int, target: Int, random: Random): LevelSpec = when (kind) {
        LevelKind.STONES -> {
            val pattern = StonePatterns.pick(size, 2, random) ?: StonePatterns.pick(size, 1, random)!!
            val tile = 1 shl StonePatterns.exponentFor(LevelMath.log2(target), pattern)
            LevelBuilders.stones(1, tile, pattern)
        }
        LevelKind.SPRINT -> LevelBuilders.sprint(1, target / 2, size, slack = 1.5)
        LevelKind.FOURS -> LevelBuilders.fours(1, target, size)
        LevelKind.SCORE -> LevelBuilders.score(1, target / 4, size)
        LevelKind.TWINS -> LevelBuilders.twins(1, target / 4, size)
        LevelKind.HEADSTART -> LevelBuilders.headStart(1, target, size)
        else -> LevelBuilders.zen(1, target, size)
    }
}
