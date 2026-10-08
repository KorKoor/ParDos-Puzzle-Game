package com.korkoor.pardos.domain.logic

import kotlin.random.Random

/** Configuración del reto diario. Es la misma para cualquier jugador que comparta el día. */
data class DailyConfig(
    val day: Int,
    val seed: Long,
    val boardSize: Int,
    val target: Int,
    val themeIndex: Int
)

object DailyChallenge {
    /** Número de temas disponibles (índices 0..THEME_COUNT-1). */
    const val THEME_COUNT = 5

    fun forDay(epochDay: Int): DailyConfig {
        // La semilla del tablero y la de la configuración salen del día, pero son independientes
        val seed = epochDay.toLong() * 7919L + 104729L
        val config = Random(seed xor 0x5DEECE66DL)
        val size = if (config.nextBoolean()) 4 else 5
        val target = if (config.nextBoolean()) 1024 else 2048
        val theme = config.nextInt(THEME_COUNT)
        return DailyConfig(epochDay, seed, size, target, theme)
    }
}
