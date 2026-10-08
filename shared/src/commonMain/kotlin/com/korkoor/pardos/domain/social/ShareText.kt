package com.korkoor.pardos.domain.social

import com.korkoor.pardos.domain.retention.Civil

/**
 * Texto para compartir un resultado (WhatsApp, Instagram, mensajes…). Todo el formato vive aquí para que
 * Android e iOS compartan exactamente lo mismo; la app solo abre el selector del sistema.
 */
object ShareText {
    const val STORE_URL = "https://play.google.com/store/apps/details?id=com.korkoor.pardos"
    const val MAX_STARS = 3

    private val monthsShort = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

    /** "7 oct" a partir de un día local desde epoch. */
    fun dayLabel(epochDay: Int): String {
        val d = Civil.fromEpochDay(epochDay)
        return "${d.day} ${monthsShort[d.month - 1]}"
    }

    /** m:ss */
    fun clock(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
    }

    /** ⭐⭐▫ (estrellas ganadas y vacías hasta [MAX_STARS]). */
    fun starsLine(stars: Int): String {
        val s = stars.coerceIn(0, MAX_STARS)
        return "⭐".repeat(s) + "▫️".repeat(MAX_STARS - s)
    }

    /**
     * Resultado de una victoria. Si [epochDay] no es null es el reto diario (todos jugaron el mismo
     * tablero, así que la comparación es justa y el mensaje invita a probarlo).
     */
    fun victory(
        modeName: String,
        stars: Int,
        targetTile: Int,
        moves: Int,
        timeMs: Long,
        streak: Int,
        epochDay: Int? = null
    ): String = buildString {
        if (epochDay != null) appendLine("ParDos · Reto diario ${dayLabel(epochDay)}") else appendLine("ParDos · $modeName")
        appendLine("${starsLine(stars)}  ficha $targetTile · $moves mov · ${clock(timeMs)}")
        if (streak >= 2) appendLine("🔥 Racha de $streak días")
        appendLine(if (epochDay != null) "¿Puedes hacerlo mejor? Es el mismo tablero para todos." else "¿Puedes superarme?")
        append(STORE_URL)
    }

    /** Invitación genérica con el código de amigo. */
    fun invite(friendCode: String): String =
        "Juega conmigo a ParDos, el puzle de sumar fichas 🧩\nMi código de amigo: $friendCode\n$STORE_URL"
}
