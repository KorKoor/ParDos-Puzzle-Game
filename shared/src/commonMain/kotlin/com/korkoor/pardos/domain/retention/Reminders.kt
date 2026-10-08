package com.korkoor.pardos.domain.retention

import com.korkoor.pardos.domain.economy.Economy

/** Un aviso que la app debe programar. [delayMs] cuenta desde "ahora". */
data class Reminder(val key: String, val id: Int, val title: String, val body: String, val delayMs: Long)

/** Una fiesta que empieza pronto: [daysUntil] días desde hoy (1 = mañana). */
data class UpcomingEvent(val id: Int, val name: String, val skinName: String, val daysUntil: Int)

/** Lo que el planificador necesita saber del jugador. Todo en milisegundos y días locales. */
data class ReminderInput(
    val nowMs: Long,
    /** Hora local actual en minutos desde medianoche (0..1439). */
    val minuteOfDay: Int,
    val streak: Int,
    val freeChestLastMs: Long,
    val seasonDaysLeft: Int,
    val seasonClaimable: Int,
    val seasonTierReached: Int,
    val weeklyOpen: Int,
    val daysLeftInWeek: Int,
    val wheelFreeLeft: Int,
    val piggyGems: Int,
    /** Liga actual (vacío = sin liga) y cuánto falta esta semana. */
    val leagueName: String = "",
    val leagueStarsToPromote: Int = Int.MAX_VALUE,
    val leagueAtRisk: Boolean = false,
    /** Fiesta activa con skin por ganar: nombre, skin, días que quedan (0 = termina hoy) y victorias que faltan. */
    val eventName: String = "",
    val eventSkinName: String = "",
    val eventDaysLeft: Int = Int.MAX_VALUE,
    val eventWinsLeft: Int = 0,
    val upcomingEvents: List<UpcomingEvent> = emptyList()
)

/**
 * Decide qué avisos programar. Pocos y con sentido: cada uno avisa de algo real y nunca de madrugada.
 * La app solo traduce la lista a WorkManager (Android) o UNUserNotificationCenter (iOS).
 */
object ReminderPlanner {
    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE
    private const val DAY = 24 * HOUR

    /** Ventana en la que se permite molestar: 9:00 a 21:30. */
    const val QUIET_START_MIN = 21 * 60 + 30
    const val QUIET_END_MIN = 9 * 60

    /** Mueve un aviso que caería en horas de descanso a la mañana siguiente a las 9:00 (más un pequeño margen). */
    fun respectQuietHours(delayMs: Long, minuteOfDay: Int): Long {
        val targetMinute = (((minuteOfDay + delayMs / MINUTE) % (24 * 60)) + 24 * 60) % (24 * 60)
        val inQuiet = targetMinute >= QUIET_START_MIN || targetMinute < QUIET_END_MIN
        if (!inQuiet) return delayMs
        val wait = if (targetMinute >= QUIET_START_MIN) (24 * 60 - targetMinute) + QUIET_END_MIN else QUIET_END_MIN - targetMinute
        return delayMs + wait * MINUTE
    }

    fun plan(i: ReminderInput): List<Reminder> {
        val out = mutableListOf<Reminder>()

        // Poderes recargados: aviso corto, no pasa por horas de descanso (ocurre durante una sesión reciente)
        out += Reminder("powerup_ready", 1, "Tus poderes están listos", "Varita y Fusión recargadas. Vuelve al tablero.", 16 * MINUTE)

        // Cofre gratis: justo cuando termina la espera (si ya estaba listo, mañana por la mañana)
        val chestIn = (i.freeChestLastMs + Economy.FREE_CHEST_COOLDOWN_MS - i.nowMs).coerceAtLeast(0L)
        val chestDelay = if (chestIn == 0L) DAY else chestIn
        out += Reminder(
            "free_chest", 6, "Tu cofre gratis está listo",
            "Ábrelo y suma otra pieza a tu álbum.", respectQuietHours(chestDelay, i.minuteOfDay)
        )

        // Ruleta del día: solo si todavía no se usa
        if (i.wheelFreeLeft > 0) {
            out += Reminder(
                "wheel", 7, "Tu giro gratis te espera",
                "La ruleta de hoy sigue sin girar. ¿Qué premio te toca?", respectQuietHours(3 * HOUR, i.minuteOfDay)
            )
        }

        // Regalo del día siguiente
        out += Reminder(
            "regalo_diario", 2, "Tu regalo de hoy te espera",
            "Día ${i.streak + 1} de racha: entra y reclama tus monedas.",
            respectQuietHours(delayToHour(i.minuteOfDay, 10 * 60, dayOffset = 1), i.minuteOfDay)
        )

        // Racha en peligro: mañana a las 20:00, solo si hay racha que proteger
        if (i.streak >= 2) {
            out += Reminder(
                "racha_riesgo", 3, "Tu racha de ${i.streak} días está en riesgo",
                "Solo te toma 2 minutos. Juega hoy y no la pierdas.",
                delayToHour(i.minuteOfDay, 20 * 60, dayOffset = 1)
            )
        }

        // Premios del pase sin reclamar
        if (i.seasonClaimable > 0) {
            out += Reminder(
                "season_claim", 8, "Tienes ${i.seasonClaimable} premios del pase sin cobrar",
                "Llegaste al nivel ${i.seasonTierReached}. ¡Reclámalos!",
                respectQuietHours(DAY + 2 * HOUR, i.minuteOfDay)
            )
        }

        // Pase por terminar (últimos 3 días): aviso 20:00 del penúltimo día
        if (i.seasonDaysLeft in 2..3) {
            val days = i.seasonDaysLeft - 2
            out += Reminder(
                "season_end", 9, "El pase de temporada está por terminar",
                "Quedan ${i.seasonDaysLeft} días: cobra lo que te falta antes de que se reinicie.",
                delayToHour(i.minuteOfDay, 19 * 60, dayOffset = days)
            )
        }

        // Semanales por vencer
        if (i.weeklyOpen > 0 && i.daysLeftInWeek in 1..2) {
            out += Reminder(
                "weekly_end", 10, "Tus misiones semanales vencen pronto",
                "Te faltan ${i.weeklyOpen}. Aún llegas.",
                delayToHour(i.minuteOfDay, 18 * 60, dayOffset = (i.daysLeftInWeek - 1))
            )
        }

        // Liga: en los últimos dos días de la semana, si está en riesgo o el ascenso está cerca
        if (i.leagueName.isNotEmpty() && i.daysLeftInWeek in 1..2 && (i.leagueAtRisk || i.leagueStarsToPromote in 1..12)) {
            out += Reminder(
                "league_end",
                12,
                if (i.leagueAtRisk) "Tu liga ${i.leagueName} está en peligro" else "Estás cerca de subir de liga",
                if (i.leagueAtRisk) "Gana unas estrellas antes de que cierre la semana y no bajes."
                else "Te faltan ${i.leagueStarsToPromote} estrellas para subir. ¡Aún llegas!",
                delayToHour(i.minuteOfDay, 18 * 60 + 30, dayOffset = (i.daysLeftInWeek - 1))
            )
        }

        // Fiesta con skin: última oportunidad el último día (18:30) o el penúltimo
        if (i.eventName.isNotEmpty() && i.eventWinsLeft > 0 && i.eventDaysLeft in 0..1 &&
            !(i.eventDaysLeft == 0 && i.minuteOfDay >= 18 * 60 + 30)
        ) {
            out += Reminder(
                "event_last_chance", 13, "Última oportunidad: ${i.eventName}",
                "Te faltan ${i.eventWinsLeft} victorias para llevarte la skin ${i.eventSkinName}.",
                delayToHour(i.minuteOfDay, 18 * 60 + 30, dayOffset = i.eventDaysLeft)
            )
        }

        // Fiestas que empiezan pronto: aviso a las 10:00 del día de inicio (máx. 3, ids 20..22)
        i.upcomingEvents.filter { it.daysUntil >= 1 }.sortedBy { it.daysUntil }.take(3).forEachIndexed { idx, e ->
            out += Reminder(
                "event_start_${e.id}", 20 + idx, "¡Llega ${e.name}!",
                "Gana ${com.korkoor.pardos.domain.shop.EventSkins.WINS_REQUIRED} niveles y llévate la skin exclusiva ${e.skinName}.",
                delayToHour(i.minuteOfDay, 10 * 60, dayOffset = e.daysUntil)
            )
        }

        // Hucha a punto de llenarse
        if (i.piggyGems >= Economy.PIGGY_CAP * 8 / 10 && i.piggyGems < Economy.PIGGY_CAP) {
            out += Reminder(
                "piggy_full", 11, "Tu hucha casi está llena",
                "Tiene ${i.piggyGems} gemas guardadas. Rómpela antes de que se desborde.",
                respectQuietHours(2 * DAY, i.minuteOfDay)
            )
        }

        // Regresos
        out += Reminder("regreso_3d", 4, "Tus fichas te extrañan", "Hay misiones nuevas y un regalo esperándote.", respectQuietHours(3 * DAY, i.minuteOfDay))
        out += Reminder("regreso_7d", 5, "Ha pasado una semana", "Vuelve a un tablero tranquilo: una partida Zen y listo.", respectQuietHours(7 * DAY, i.minuteOfDay))

        return out
    }

    /** Milisegundos hasta las [targetMinute] (minutos desde medianoche) del día actual + [dayOffset]. Nunca negativo. */
    fun delayToHour(minuteOfDay: Int, targetMinute: Int, dayOffset: Int): Long {
        val ms = (dayOffset * 24 * 60 + targetMinute - minuteOfDay) * MINUTE
        return if (ms <= 0) ms + DAY else ms
    }
}
