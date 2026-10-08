package com.korkoor.pardos.domain.shop

import com.korkoor.pardos.domain.events.EventType

/**
 * Cada fiesta del calendario trae una skin exclusiva. Se gana jugando durante el evento (o se compra con gemas
 * mientras dura). Si no la consigues, vuelve el año siguiente: nunca se pierde para siempre.
 */
object EventSkins {
    /** Victorias necesarias durante el evento para llevarse la skin. */
    const val WINS_REQUIRED = 3
    /** Precio con gemas mientras el evento está activo. */
    const val GEM_PRICE = 100

    fun skinFor(type: EventType): TileSkin? = when (type) {
        EventType.VALENTINE -> TileSkin.VALENTINE
        EventType.SPRING -> TileSkin.SPRING
        EventType.SUMMER -> TileSkin.SUMMER
        EventType.INDEPENDENCE -> TileSkin.INDEPENDENCE
        EventType.HALLOWEEN -> TileSkin.HALLOWEEN
        EventType.DAY_OF_THE_DEAD -> TileSkin.MUERTOS
        EventType.CHRISTMAS -> TileSkin.CHRISTMAS
        EventType.NEW_YEAR -> TileSkin.NEWYEAR
        EventType.WEEKEND_GOLD, EventType.XP_WEDNESDAY, EventType.FESTIVAL_WEEK -> null
    }

    /** Evento al que pertenece una skin de fiesta (o null si no es de evento). */
    fun eventFor(skin: TileSkin): EventType? = EventType.entries.firstOrNull { skinFor(it) == skin }

    /** Nombre corto de la fiesta (para avisos y textos compartidos). */
    fun eventName(type: EventType): String = when (type) {
        EventType.VALENTINE -> "San Valentín"
        EventType.SPRING -> "Primavera"
        EventType.SUMMER -> "Verano"
        EventType.INDEPENDENCE -> "Fiestas patrias"
        EventType.HALLOWEEN -> "Noche de brujas"
        EventType.DAY_OF_THE_DEAD -> "Día de Muertos"
        EventType.CHRISTMAS -> "Navidad"
        EventType.NEW_YEAR -> "Año nuevo"
        EventType.WEEKEND_GOLD -> "Fin de semana dorado"
        EventType.XP_WEDNESDAY -> "Miércoles de experiencia"
        EventType.FESTIVAL_WEEK -> "Semana festival"
    }

    /** Fechas de la fiesta como texto corto ("25–31 oct"), o null si no es una fiesta fija. */
    fun dateRange(type: EventType): String? {
        val def = com.korkoor.pardos.domain.events.EventCalendar.seasonal.firstOrNull { it.type == type } ?: return null
        val months = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
        return if (def.startMonth == def.endMonth) "${def.startDay}–${def.endDay} ${months[def.startMonth - 1]}"
        else "${def.startDay} ${months[def.startMonth - 1]} – ${def.endDay} ${months[def.endMonth - 1]}"
    }

    /** Victorias que faltan (0 = ya las cumpliste). */
    fun winsLeft(wins: Int): Int = (WINS_REQUIRED - wins).coerceAtLeast(0)
}
