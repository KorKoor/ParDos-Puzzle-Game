package com.korkoor.pardos.domain.social

/** Calendario de semanas (lunes a domingo) a partir de días locales desde epoch. */
object WeekCalendar {
    /** El día 0 de epoch fue jueves, por eso el +3 alinea el inicio de semana en lunes. */
    fun weekId(epochDay: Int): Int = (epochDay + 3) / 7
}

/** Participante del ranking semanal. */
data class RankEntry(
    val uid: String,
    val name: String,
    val weeklyStars: Int,
    /** Semana en la que se registraron esas estrellas (para ignorar datos viejos). */
    val weekId: Int,
    val isMe: Boolean = false
)

data class RankedPlayer(val position: Int, val entry: RankEntry, val score: Int)

object Leaderboard {
    /**
     * Ordena por estrellas de la semana actual (las de semanas anteriores cuentan 0).
     * Empates comparten posición; a igualdad de puntos, quien está "yo" no se prioriza,
     * se ordena por nombre para que el orden sea estable.
     */
    fun weekly(entries: List<RankEntry>, currentWeek: Int): List<RankedPlayer> {
        val scored = entries
            .map { it to if (it.weekId == currentWeek) it.weeklyStars.coerceAtLeast(0) else 0 }
            .sortedWith(compareByDescending<Pair<RankEntry, Int>> { it.second }.thenBy { it.first.name.lowercase() })

        var lastScore = Int.MIN_VALUE
        var lastPosition = 0
        return scored.mapIndexed { index, (entry, score) ->
            val position = if (score == lastScore) lastPosition else index + 1
            lastScore = score
            lastPosition = position
            RankedPlayer(position, entry, score)
        }
    }
}
