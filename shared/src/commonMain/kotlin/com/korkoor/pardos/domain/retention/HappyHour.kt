package com.korkoor.pardos.domain.retention

/**
 * Hora feliz diaria: cada día hay una ventana de tiempo (60 minutos entre semana, 90 el fin de semana) en la que las victorias
 * dan el doble de monedas. La ventana sale del número de día (siempre la misma para el mismo día, sin servidor) y cae entre
 * las 11:00 y las 22:00, nunca de madrugada.
 */
object HappyHour {
    const val EARLIEST_START = 11 * 60
    const val LATEST_START = 20 * 60 + 30
    const val WEEKDAY_MINUTES = 60
    const val WEEKEND_MINUTES = 90
    /** Porcentaje extra de monedas durante la hora feliz (100 = el doble). */
    const val EXTRA_COINS_PCT = 100

    enum class Phase { UPCOMING, ACTIVE, ENDED }

    data class Window(val startMin: Int, val endMin: Int)

    /**
     * [minutes]: si está ACTIVE, cuántos minutos le quedan; si es UPCOMING, cuántos faltan; si ya terminó (ENDED),
     * cuántos faltan para la ventana de mañana, que es la que trae [window].
     */
    data class State(val phase: Phase, val window: Window, val minutes: Int)

    /** Lunes = 0 … domingo = 6 (el día 0 de 1970 fue jueves). */
    fun weekday(epochDay: Int): Int = ((epochDay + 3) % 7 + 7) % 7

    fun isWeekend(epochDay: Int): Boolean = weekday(epochDay) >= 5

    private fun mix(day: Int): Int {
        var h = day * 374761393 + 668265263
        h = (h xor (h ushr 13)) * 1274126177
        h = h xor (h ushr 16)
        return h and 0x7FFFFFFF
    }

    fun windowFor(epochDay: Int): Window {
        val slots = (LATEST_START - EARLIEST_START) / 5 + 1
        val start = EARLIEST_START + (mix(epochDay) % slots) * 5
        val length = if (isWeekend(epochDay)) WEEKEND_MINUTES else WEEKDAY_MINUTES
        return Window(start, start + length)
    }

    fun stateAt(epochDay: Int, minuteOfDay: Int): State {
        val w = windowFor(epochDay)
        return when {
            minuteOfDay < w.startMin -> State(Phase.UPCOMING, w, w.startMin - minuteOfDay)
            minuteOfDay < w.endMin -> State(Phase.ACTIVE, w, w.endMin - minuteOfDay)
            else -> {
                val next = windowFor(epochDay + 1)
                State(Phase.ENDED, next, 1440 - minuteOfDay + next.startMin)
            }
        }
    }

    fun isActive(epochDay: Int, minuteOfDay: Int): Boolean = stateAt(epochDay, minuteOfDay).phase == Phase.ACTIVE

    /** Monedas con el bono si ahora es hora feliz. */
    fun applyCoins(coins: Int, epochDay: Int, minuteOfDay: Int): Int =
        if (coins > 0 && isActive(epochDay, minuteOfDay)) coins + coins * EXTRA_COINS_PCT / 100 else coins
}
