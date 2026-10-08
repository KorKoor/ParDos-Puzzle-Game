package com.korkoor.pardos.domain.events

import com.korkoor.pardos.domain.social.WeekCalendar

/**
 * Eventos programados. Se calculan a partir del día local, sin servidor: todos los jugadores ven
 * lo mismo y se pueden probar sin esperar al calendario real.
 */
enum class EventType { WEEKEND_GOLD, XP_WEDNESDAY, FESTIVAL_WEEK }

data class GameEvent(
    val type: EventType,
    /** Primer y último día (inclusive) del evento, en días locales desde epoch. */
    val startDay: Int,
    val endDay: Int,
    val coinMultiplier: Double = 1.0,
    val xpMultiplier: Double = 1.0,
    /** Multiplicador de estrellas para el ranking semanal. */
    val starMultiplier: Int = 1
) {
    val id: String get() = type.name.lowercase()

    /** Días que faltan para que termine, contando hoy (0 = termina hoy). */
    fun daysLeft(today: Int): Int = (endDay - today).coerceAtLeast(0)
}

object EventCalendar {
    /** Cada cuántas semanas se repite la semana festival (la última de cada ciclo). */
    const val FESTIVAL_CYCLE_WEEKS = 4

    /** 0 = lunes … 6 = domingo. El día 0 de epoch fue jueves. */
    fun dayOfWeek(epochDay: Int): Int = ((epochDay + 3) % 7 + 7) % 7

    fun isFestivalWeek(epochDay: Int): Boolean =
        WeekCalendar.weekId(epochDay) % FESTIVAL_CYCLE_WEEKS == FESTIVAL_CYCLE_WEEKS - 1

    /** Eventos activos hoy, el más valioso primero. */
    fun activeOn(epochDay: Int): List<GameEvent> {
        val dow = dayOfWeek(epochDay)
        val weekStart = epochDay - dow
        val weekEnd = weekStart + 6
        val events = mutableListOf<GameEvent>()

        if (isFestivalWeek(epochDay)) {
            events += GameEvent(EventType.FESTIVAL_WEEK, weekStart, weekEnd, coinMultiplier = 1.5, starMultiplier = 2)
        }
        if (dow >= 5) { // sábado y domingo
            events += GameEvent(EventType.WEEKEND_GOLD, weekStart + 5, weekEnd, coinMultiplier = 2.0)
        }
        if (dow == 2) { // miércoles
            events += GameEvent(EventType.XP_WEDNESDAY, epochDay, epochDay, xpMultiplier = 2.0)
        }
        return events.sortedByDescending { it.coinMultiplier * it.xpMultiplier * it.starMultiplier }
    }

    // Los multiplicadores no se acumulan: se aplica el mejor, para que la economía no se dispare.
    fun coinMultiplier(epochDay: Int): Double = activeOn(epochDay).maxOfOrNull { it.coinMultiplier } ?: 1.0
    fun xpMultiplier(epochDay: Int): Double = activeOn(epochDay).maxOfOrNull { it.xpMultiplier } ?: 1.0
    fun starMultiplier(epochDay: Int): Int = activeOn(epochDay).maxOfOrNull { it.starMultiplier } ?: 1

    /** Aplica un multiplicador a una cantidad entera, redondeando hacia arriba (nunca se pierde nada). */
    fun apply(amount: Int, multiplier: Double): Int =
        kotlin.math.ceil(amount * multiplier - 1e-9).toInt().coerceAtLeast(amount)
}
