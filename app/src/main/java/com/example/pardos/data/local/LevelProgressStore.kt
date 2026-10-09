package com.korkoor.pardos.data.local

import android.content.SharedPreferences
import com.korkoor.pardos.domain.model.GameMode
import com.korkoor.pardos.domain.model.LevelInfo
import com.korkoor.pardos.domain.model.LevelRepository
import kotlin.math.max

/**
 * Persistencia del progreso por nivel (estrellas, mejores marcas, niveles desbloqueados).
 * Sacado de GameViewModel para que el ViewModel solo orqueste el juego.
 * Los tiempos se guardan en milisegundos.
 */
class LevelProgressStore(private val prefs: SharedPreferences) {

    companion object {
        const val KEY_LAST_LEVEL = "last_reached_level"
        const val KEY_TABLES_LEVEL = "last_reached_tables_level"
        const val KEY_LAST_UNLOCKED = "last_unlocked_level"
    }

    val lastUnlockedLevel: Int get() = prefs.getInt(KEY_LAST_UNLOCKED, 1)
    val tablesLevel: Int get() = prefs.getInt(KEY_TABLES_LEVEL, 1)

    private fun prefix(mode: GameMode) = when (mode) {
        GameMode.CLASICO -> ""
        GameMode.TABLAS -> "tablas_"
        GameMode.DESAFIO -> "daily_"
        else -> "custom_"
    }

    /** Niveles de campaña con estrellas, mejores marcas y bloqueo aplicados. */
    fun loadCampaignLevels(): List<LevelInfo> {
        val unlockedUntil = lastUnlockedLevel
        return LevelRepository.getGeneratedLevels().map { level ->
            level.copy(
                starsEarned = prefs.getInt("stars_level_${level.id}", 0),
                bestTime = prefs.getLong("best_time_level_${level.id}", 0L),
                bestMoves = prefs.getInt("best_moves_level_${level.id}", 0),
                isLocked = level.id > unlockedUntil
            )
        }
    }

    /** Estrellas ya conseguidas en un nivel (0 si nunca se ha ganado). */
    fun starsFor(mode: GameMode, level: Int): Int = prefs.getInt("${prefix(mode)}stars_level_$level", 0)

    /** Mejores (movimientos, tiempo) de un nivel; 0 si no hay marca. */
    fun bestStats(mode: GameMode, level: Int): Pair<Int, Long> {
        val p = prefix(mode)
        return Pair(
            prefs.getInt("${p}best_moves_level_$level", 0),
            prefs.getLong("${p}best_time_level_$level", 0L)
        )
    }

    /**
     * Guarda el resultado de un nivel solo si mejora lo anterior y desbloquea el siguiente.
     * Devuelve true si es un **récord personal** al repetir un nivel ya superado (menos movimientos o más estrellas que antes).
     */
    fun recordResult(mode: GameMode, level: Int, stars: Int, finalTime: Long, finalMoves: Int): Boolean {
        val p = prefix(mode)
        val editor = prefs.edit()
        var personalBest = false

        val starKey = "${p}stars_level_$level"
        val previousStars = prefs.getInt(starKey, 0)
        if (stars > previousStars) editor.putInt(starKey, stars)

        val timeKey = "${p}best_time_level_$level"
        val prevTime = prefs.getLong(timeKey, Long.MAX_VALUE).let { if (it == 0L) Long.MAX_VALUE else it }
        if (finalTime in 1 until prevTime) editor.putLong(timeKey, finalTime)

        val movesKey = "${p}best_moves_level_$level"
        val prevMoves = prefs.getInt(movesKey, Int.MAX_VALUE).let { if (it == 0) Int.MAX_VALUE else it }
        if (finalMoves in 1 until prevMoves) {
            editor.putInt(movesKey, finalMoves)
            // solo cuenta si ya lo había superado antes (la primera vez no hay nada que "batir")
            if (previousStars > 0 && prevMoves != Int.MAX_VALUE) personalBest = true
        }
        if (previousStars in 1 until stars) personalBest = true

        val next = level + 1
        when (mode) {
            GameMode.CLASICO -> {
                editor.putInt(KEY_LAST_UNLOCKED, max(prefs.getInt(KEY_LAST_UNLOCKED, 1), next))
                editor.putInt(KEY_LAST_LEVEL, max(prefs.getInt(KEY_LAST_LEVEL, 1), next))
                editor.commit()
            }
            GameMode.TABLAS -> {
                editor.putInt(KEY_TABLES_LEVEL, max(prefs.getInt(KEY_TABLES_LEVEL, 1), next))
                editor.commit()
            }
            else -> editor.apply()
        }
        return personalBest
    }
}
