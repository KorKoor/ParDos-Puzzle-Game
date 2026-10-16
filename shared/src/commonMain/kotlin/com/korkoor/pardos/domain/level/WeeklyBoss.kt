package com.korkoor.pardos.domain.level

/**
 * Jefe de la semana: cada semana se elige UNO de los jefes que el jugador ya venció (así nunca salta niveles ni toca su progreso)
 * para volver a vencerlo. La primera vez que lo logra esa semana recibe monedas y un banner cosmético (si ya lo tiene, solo las monedas).
 * La elección es la misma todos los días de la semana y no depende de nada que el jugador pueda reiniciar.
 */
object WeeklyBoss {
    const val REWARD_COINS = 300

    /** Banners cosméticos que se reparten por rotación (ver `Banners`). */
    val bannerPool: List<Int> = listOf(30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 43, 44, 45, 46, 47, 48, 49)

    /** Semana (empieza el lunes) de un día contado desde 1970. */
    fun weekOf(epochDay: Long): Long = (epochDay + 3) / 7

    /** Nivel del jefe de la semana entre los jefes ya vencidos ([defeatedBossLevels]); null si aún no venció ninguno. */
    fun levelFor(week: Long, defeatedBossLevels: Collection<Int>): Int? {
        val sorted = defeatedBossLevels.distinct().sorted()
        if (sorted.isEmpty()) return null
        return sorted[(((week * 7919L) % sorted.size) + sorted.size).toInt() % sorted.size]
    }

    fun bannerFor(week: Long): Int = bannerPool[(((week % bannerPool.size) + bannerPool.size) % bannerPool.size).toInt()]
}
