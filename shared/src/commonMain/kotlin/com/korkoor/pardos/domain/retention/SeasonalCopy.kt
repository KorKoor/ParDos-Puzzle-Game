package com.korkoor.pardos.domain.retention

/**
 * Textos de temporada para los avisos y para la pantalla de inicio. Puro y con pruebas: la app solo
 * pregunta "¿es temporada de brujas?" y aplica el texto.
 */
object SeasonalCopy {
    /** Noche de brujas: del 1 de octubre al 2 de noviembre (incluye el Día de Muertos). */
    fun isHalloweenWindow(epochDay: Int): Boolean {
        val c = Civil.fromEpochDay(epochDay)
        return c.month == 10 || (c.month == 11 && c.day <= 2)
    }

    /** Versión embrujada de un aviso. Los que no tienen versión especial se quedan igual. */
    fun spooky(r: Reminder): Reminder = when (r.key) {
        "free_chest" -> r.copy(title = "Tu cofre embrujado está listo", body = "Ábrelo antes de que se lo lleven los fantasmas.")
        "wheel" -> r.copy(title = "La ruleta de las brujas te espera", body = "Tu giro gratis de hoy sigue sin usar. ¿Truco o trato?")
        "regalo_diario" -> r.copy(title = "Tu dulce de hoy te espera", body = "Día ${dayFromTitle(r)} de racha: entra y reclama tus monedas.")
        "racha_riesgo" -> r.copy(title = "Tu racha está a punto de desaparecer", body = "Solo te toma 2 minutos. Juega hoy y no dejes que se la lleven.")
        "season_claim" -> r.copy(title = "Tienes premios del pase atrapados", body = r.body)
        "season_end" -> r.copy(title = "El pase de temporada se desvanece", body = r.body)
        "event_last_chance" -> r.copy(title = "Última noche: ${r.title.substringAfter(": ")}", body = r.body)
        "regreso_3d" -> r.copy(title = "Tus fichas te extrañan en la oscuridad", body = "Hay misiones nuevas y un regalo esperándote.")
        "regreso_7d" -> r.copy(title = "Ha pasado una semana", body = "Vuelve a un tablero tranquilo: una partida Zen y listo.")
        else -> r
    }

    /** El día de racha viene dentro del texto original ("Día N de racha..."); se rescata para no perderlo. */
    private fun dayFromTitle(r: Reminder): String =
        Regex("Día (\\d+)").find(r.body)?.groupValues?.get(1) ?: "?"

    fun apply(reminders: List<Reminder>, halloween: Boolean): List<Reminder> =
        if (halloween) reminders.map { spooky(it) } else reminders
}
