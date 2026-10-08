package com.korkoor.pardos.domain.logic

import com.korkoor.pardos.domain.economy.Economy

enum class RemoteOutcome { WIN, LOSE, TIE }

/** Un reto: quién lo lanzó, el tablero (semilla) y los puntos que hizo en sus 60 s. */
data class RemoteChallenge(val seed: Long, val score: Int, val name: String)

/** Un duelo ya jugado, para el historial y las estadísticas. */
data class RemoteDuelRecord(
    val opponent: String,
    val myScore: Int,
    val theirScore: Int,
    val outcome: RemoteOutcome,
    val day: Int
)

/**
 * Duelo a distancia sin servidor: el reto viaja como un código corto dentro de un mensaje normal (WhatsApp, etc.).
 * Quien lo recibe juega el MISMO tablero (misma semilla) durante 60 s y se compara el puntaje.
 *
 * Formato: `PD1-<semilla>-<puntos>-<NOMBRE>-<control>`, todo en mayúsculas y base 36.
 * El control de 2 caracteres detecta errores al escribir y ediciones torpes del puntaje (no es seguridad real).
 */
object RemoteDuel {
    const val PREFIX = "PD1"
    const val MAX_NAME = 12
    private const val DIGITS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private val PATTERN = Regex("PD1-[0-9A-Z]{1,12}-[0-9A-Z]{1,8}-[0-9A-Z_]{1,12}-[0-9A-Z]{2}")

    // ---------- base 36 ----------
    private fun toBase36(value: Long): String {
        require(value >= 0)
        if (value == 0L) return "0"
        var v = value
        val sb = StringBuilder()
        while (v > 0) { sb.append(DIGITS[(v % 36).toInt()]); v /= 36 }
        return sb.reverse().toString()
    }

    private fun fromBase36(text: String): Long? {
        var acc = 0L
        for (c in text) {
            val d = DIGITS.indexOf(c)
            if (d < 0) return null
            acc = acc * 36 + d
            if (acc < 0) return null
        }
        return acc
    }

    /** Semillas positivas de hasta ~55 bits: caben en 11 caracteres base 36. */
    fun normalizeSeed(raw: Long): Long = raw and 0x3FFFFFFFFFFFFFL

    // ---------- nombre ----------
    private val accents = mapOf(
        'Á' to 'A', 'É' to 'E', 'Í' to 'I', 'Ó' to 'O', 'Ú' to 'U', 'Ü' to 'U', 'Ñ' to 'N',
        'À' to 'A', 'È' to 'E', 'Ì' to 'I', 'Ò' to 'O', 'Ù' to 'U', 'Â' to 'A', 'Ê' to 'E', 'Ô' to 'O'
    )

    /** Deja solo A-Z, 0-9 y guion bajo, en mayúsculas y sin acentos. Vacío → "AMIGO". */
    fun cleanName(raw: String): String {
        val out = StringBuilder()
        for (ch in raw.uppercase()) {
            val c = accents[ch] ?: ch
            when {
                c in 'A'..'Z' || c in '0'..'9' -> out.append(c)
                c == ' ' || c == '_' -> if (out.isNotEmpty() && out.last() != '_') out.append('_')
            }
            if (out.length >= MAX_NAME) break
        }
        val name = out.toString().trim('_')
        return name.ifEmpty { "AMIGO" }
    }

    /** Nombre bonito para mostrar: "CARLOS_G" → "Carlos G". */
    fun displayName(clean: String): String =
        clean.split('_').filter { it.isNotEmpty() }.joinToString(" ") { w -> w.lowercase().replaceFirstChar { it.uppercase() } }

    // ---------- código ----------
    private fun checksum(payload: String): String {
        var h = 2166136261L
        for (c in payload) { h = (h xor c.code.toLong()) * 16777619L and 0xFFFFFFFFL }
        val v = (h % 1296).toInt() // 36²
        return "${DIGITS[v / 36]}${DIGITS[v % 36]}"
    }

    fun encode(c: RemoteChallenge): String {
        val payload = "$PREFIX-${toBase36(normalizeSeed(c.seed))}-${toBase36(c.score.coerceAtLeast(0).toLong())}-${cleanName(c.name)}"
        return "$payload-${checksum(payload)}"
    }

    /** Busca un código dentro de cualquier texto (un mensaje entero pegado). Devuelve null si no hay uno válido. */
    fun decode(text: String): RemoteChallenge? {
        val match = PATTERN.find(text.uppercase()) ?: return null
        val code = match.value
        val payload = code.substring(0, code.length - 3)
        if (checksum(payload) != code.takeLast(2)) return null
        val parts = payload.split('-')
        if (parts.size != 4) return null
        val seed = fromBase36(parts[1]) ?: return null
        val score = fromBase36(parts[2]) ?: return null
        if (score > Int.MAX_VALUE) return null
        return RemoteChallenge(seed, score.toInt(), parts[3])
    }

    /** Mensaje listo para compartir. */
    fun shareText(c: RemoteChallenge, storeUrl: String): String {
        val code = encode(c)
        return "⚔️ ¡Te reto en ParDos! Hice ${c.score} puntos en 60 segundos.\n" +
            "¿Puedes superarme? Abre ParDos → Multijugador → «Tengo un código» y pega esto:\n\n" +
            "$code\n\n$storeUrl"
    }

    // ---------- resultado y premios ----------
    fun outcome(mine: Int, theirs: Int): RemoteOutcome = when {
        mine > theirs -> RemoteOutcome.WIN
        mine < theirs -> RemoteOutcome.LOSE
        else -> RemoteOutcome.TIE
    }

    data class Reward(val coins: Int, val gems: Int)

    /** Premio por aceptar un reto: jugar siempre paga algo, ganar paga más. */
    fun rewardFor(outcome: RemoteOutcome): Reward = when (outcome) {
        RemoteOutcome.WIN -> Reward(Economy.REMOTE_DUEL_PLAY_COINS + Economy.REMOTE_DUEL_WIN_COINS, Economy.REMOTE_DUEL_WIN_GEMS)
        RemoteOutcome.TIE -> Reward(Economy.REMOTE_DUEL_PLAY_COINS + Economy.REMOTE_DUEL_WIN_COINS / 2, 0)
        RemoteOutcome.LOSE -> Reward(Economy.REMOTE_DUEL_PLAY_COINS, 0)
    }

    /** Lo que se gana por lanzar un reto (hasta [Economy.REMOTE_DUEL_CREATE_PER_DAY] al día). */
    fun createReward(createdToday: Int): Reward =
        if (createdToday < Economy.REMOTE_DUEL_CREATE_PER_DAY) Reward(Economy.REMOTE_DUEL_CREATE_COINS, 0) else Reward(0, 0)

    // ---------- estadísticas ----------
    data class Stats(val wins: Int, val losses: Int, val ties: Int, val currentStreak: Int, val bestStreak: Int) {
        val played: Int get() = wins + losses + ties
    }

    /** [history] va del más reciente al más antiguo. */
    fun stats(history: List<RemoteDuelRecord>): Stats {
        var current = 0
        for (r in history) { if (r.outcome == RemoteOutcome.WIN) current++ else break }
        var best = 0
        var run = 0
        for (r in history.asReversed()) {
            if (r.outcome == RemoteOutcome.WIN) { run++; if (run > best) best = run } else run = 0
        }
        return Stats(
            wins = history.count { it.outcome == RemoteOutcome.WIN },
            losses = history.count { it.outcome == RemoteOutcome.LOSE },
            ties = history.count { it.outcome == RemoteOutcome.TIE },
            currentStreak = current, bestStreak = best
        )
    }

    // ---------- historial en texto (para guardarlo en cualquier plataforma) ----------
    fun encodeHistory(history: List<RemoteDuelRecord>): String =
        history.joinToString(";") { "${it.opponent},${it.myScore},${it.theirScore},${it.outcome.name},${it.day}" }

    fun decodeHistory(raw: String?): List<RemoteDuelRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(';').mapNotNull { row ->
            val p = row.split(',')
            if (p.size != 5) return@mapNotNull null
            try {
                RemoteDuelRecord(p[0], p[1].toInt(), p[2].toInt(), RemoteOutcome.valueOf(p[3]), p[4].toInt())
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}
