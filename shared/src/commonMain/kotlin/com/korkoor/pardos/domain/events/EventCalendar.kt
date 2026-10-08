package com.korkoor.pardos.domain.events

import com.korkoor.pardos.domain.retention.Civil
import com.korkoor.pardos.domain.social.WeekCalendar

/**
 * Eventos programados. Se calculan a partir del día local, sin servidor: todos los jugadores ven
 * lo mismo y se pueden probar sin esperar al calendario real.
 */
enum class EventType {
    WEEKEND_GOLD, XP_WEDNESDAY, FESTIVAL_WEEK,
    HALLOWEEN, DAY_OF_THE_DEAD, CHRISTMAS, NEW_YEAR, VALENTINE,
    SPRING, SUMMER, INDEPENDENCE
}

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

/** Fiesta con fechas fijas cada año (mes/día de inicio y de fin, ambos inclusive). */
data class SeasonalEventDef(
    val type: EventType,
    val startMonth: Int, val startDay: Int,
    val endMonth: Int, val endDay: Int,
    val coinMultiplier: Double = 1.5
)

object EventCalendar {
    val seasonal: List<SeasonalEventDef> = listOf(
        SeasonalEventDef(EventType.HALLOWEEN, 10, 25, 10, 31),
        SeasonalEventDef(EventType.DAY_OF_THE_DEAD, 11, 1, 11, 2),
        SeasonalEventDef(EventType.CHRISTMAS, 12, 18, 12, 26),
        SeasonalEventDef(EventType.NEW_YEAR, 12, 31, 1, 2),
        SeasonalEventDef(EventType.VALENTINE, 2, 12, 2, 14),
        SeasonalEventDef(EventType.SPRING, 3, 20, 4, 3),
        SeasonalEventDef(EventType.SUMMER, 6, 21, 7, 5),
        SeasonalEventDef(EventType.INDEPENDENCE, 9, 15, 9, 16)
    )

    /** Próximas fiestas: las que empiezan en los siguientes [withinDays] días (sin contar las que ya están activas). */
    fun upcoming(epochDay: Int, withinDays: Int = 14): List<Pair<EventType, Int>> {
        val year = Civil.fromEpochDay(epochDay).year
        return seasonal.mapNotNull { def ->
            (year..year + 1).firstNotNullOfOrNull { y ->
                val start = Civil.toEpochDay(y, def.startMonth, def.startDay)
                val daysUntil = start - epochDay
                if (daysUntil in 1..withinDays) def.type to daysUntil else null
            }
        }.sortedBy { it.second }
    }

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
        events += seasonalOn(epochDay)
        return events.sortedByDescending { it.coinMultiplier * it.xpMultiplier * it.starMultiplier }
    }

    /** Fiestas de calendario activas hoy. Las que cruzan el fin de año (31 dic – 2 ene) se resuelven en ambos años. */
    fun seasonalOn(epochDay: Int): List<GameEvent> {
        val year = Civil.fromEpochDay(epochDay).year
        return seasonal.mapNotNull { def ->
            (year - 1..year).firstNotNullOfOrNull { y ->
                val start = Civil.toEpochDay(y, def.startMonth, def.startDay)
                val end = Civil.toEpochDay(if (def.endMonth < def.startMonth) y + 1 else y, def.endMonth, def.endDay)
                if (epochDay in start..end) GameEvent(def.type, start, end, coinMultiplier = def.coinMultiplier) else null
            }
        }
    }

    // Los multiplicadores no se acumulan: se aplica el mejor, para que la economía no se dispare.
    fun coinMultiplier(epochDay: Int): Double = activeOn(epochDay).maxOfOrNull { it.coinMultiplier } ?: 1.0
    fun xpMultiplier(epochDay: Int): Double = activeOn(epochDay).maxOfOrNull { it.xpMultiplier } ?: 1.0
    fun starMultiplier(epochDay: Int): Int = activeOn(epochDay).maxOfOrNull { it.starMultiplier } ?: 1

    /** Aplica un multiplicador a una cantidad entera, redondeando hacia arriba (nunca se pierde nada). */
    fun apply(amount: Int, multiplier: Double): Int =
        kotlin.math.ceil(amount * multiplier - 1e-9).toInt().coerceAtLeast(amount)
}
