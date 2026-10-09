package com.korkoor.pardos.domain.retention

/**
 * Calendario de conexión del mes: una casilla por día con su premio. Se cobra la casilla de hoy; los días en que no se entró
 * se pueden recuperar (una vez gratis al mes y luego con gemas) hasta [MAX_BACK_DAYS] días atrás, así faltar un día no arruina el mes.
 * Todo son cuentas con el número de día desde 1970 (sin librerías de fechas, para que funcione igual en Android y en iPhone).
 */
object LoginCalendar {
    const val RECOVER_GEMS = 3
    const val FREE_RECOVERIES = 1
    const val MAX_BACK_DAYS = 7

    data class Civil(val year: Int, val month: Int, val day: Int)

    /** Premio de una casilla. [chest] es COMMON, RARE o EPIC (o null). [big] marca las casillas grandes. */
    data class Prize(val coins: Int, val gems: Int, val chest: String?, val big: Boolean)

    /** Por cuántas casillas cobradas en el mes se entrega un cofre extra: 10 → común, 20 → raro. */
    val MONTH_BONUS: Map<Int, String> = mapOf(10 to "COMMON", 20 to "RARE")

    fun civilFromDays(epochDay: Int): Civil {
        val z = epochDay + 719468
        val era = (if (z >= 0) z else z - 146096) / 146097
        val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = if (mp < 10) mp + 3 else mp - 9
        return Civil(if (m <= 2) y + 1 else y, m, d)
    }

    fun daysFromCivil(year: Int, month: Int, day: Int): Int {
        val y = if (month <= 2) year - 1 else year
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val doy = (153 * (if (month > 2) month - 3 else month + 9) + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }

    fun isLeap(year: Int): Boolean = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

    fun daysInMonth(year: Int, month: Int): Int = when (month) {
        2 -> if (isLeap(year)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }

    /** Día (desde 1970) en que empieza el mes de [epochDay]. */
    fun monthStart(epochDay: Int): Int = epochDay - (civilFromDays(epochDay).day - 1)

    fun prizeFor(day: Int, daysInMonth: Int): Prize {
        val last = day == daysInMonth
        val coins = 40 + 10 * minOf(day, 20) + (if (day % 7 == 0) 100 else 0)
        var gems = when (day) {
            5 -> 2
            10 -> 3
            15 -> 4
            20 -> 5
            25 -> 6
            else -> 0
        }
        if (last) gems += 10
        val chest = when {
            last -> "EPIC"
            day == 7 -> "COMMON"
            day == 14 -> "RARE"
            day == 21 -> "EPIC"
            else -> null
        }
        return Prize(coins, gems, chest, big = chest != null)
    }

    /** ¿Se puede recuperar [day] estando hoy en [today] (día del mes)? Solo días pasados del mes, hasta [MAX_BACK_DAYS] atrás. */
    fun canRecover(day: Int, today: Int): Boolean = day in 1 until today && today - day <= MAX_BACK_DAYS
}
