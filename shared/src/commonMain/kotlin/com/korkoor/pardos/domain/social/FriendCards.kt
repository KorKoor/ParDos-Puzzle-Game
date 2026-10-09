package com.korkoor.pardos.domain.social

import com.korkoor.pardos.domain.logic.RemoteDuel

/**
 * Tarjeta de amigo sin servidor: un código corto con tu nombre, avatar, banner y marcas, que se comparte por WhatsApp o Mensajes.
 * Quien lo pega te agrega a su lista y ve tus datos de ese día (si quieres que se actualicen, vuelve a compartir tu tarjeta:
 * llega con el mismo identificador y reemplaza la anterior).
 *
 * Formato: `PF1-<id>-<avatar>-<banner>-<nivel>-<prestigio>-<estrellas>-<piezas>-<torre>-<liga>-<día>-<NOMBRE>-<control>`
 * con los números en base 36. El control detecta errores al copiar; no es seguridad real.
 */
data class FriendCard(
    val id: String,
    val name: String,
    val avatar: Int,
    val banner: Int,
    val level: Int,
    val prestige: Int,
    val stars: Int,
    val pieces: Int,
    val tower: Int,
    val league: Int,
    val day: Int
)

object FriendCards {
    const val PREFIX = "PF1"
    const val MAX_FRIENDS = 100
    private const val DIGITS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private val PATTERN = Regex("PF1-[A-Z2-9]{8}(-[0-9A-Z]{1,8}){9}-[0-9A-Z_]{1,12}-[0-9A-Z]{2}")

    private fun b36(value: Int): String {
        var v = value.coerceAtLeast(0).toLong()
        if (v == 0L) return "0"
        val sb = StringBuilder()
        while (v > 0) { sb.append(DIGITS[(v % 36).toInt()]); v /= 36 }
        return sb.reverse().toString()
    }

    private fun fromB36(text: String): Int? {
        var acc = 0L
        for (c in text) {
            val d = DIGITS.indexOf(c)
            if (d < 0) return null
            acc = acc * 36 + d
            if (acc > Int.MAX_VALUE) return null
        }
        return acc.toInt()
    }

    private fun checksum(payload: String): String {
        var h = 2166136261L
        for (c in payload) { h = (h xor c.code.toLong()) * 16777619L and 0xFFFFFFFFL }
        val v = (h % 1296).toInt()
        return "${DIGITS[v / 36]}${DIGITS[v % 36]}"
    }

    fun encode(c: FriendCard): String {
        val payload = listOf(
            PREFIX, c.id, b36(c.avatar), b36(c.banner), b36(c.level), b36(c.prestige), b36(c.stars), b36(c.pieces), b36(c.tower),
            b36(c.league), b36(c.day), RemoteDuel.cleanName(c.name)
        ).joinToString("-")
        return "$payload-${checksum(payload)}"
    }

    /** Busca una tarjeta dentro de cualquier texto pegado. */
    fun decode(text: String): FriendCard? {
        val match = PATTERN.find(text.uppercase()) ?: return null
        val code = match.value
        val payload = code.substring(0, code.length - 3)
        if (checksum(payload) != code.takeLast(2)) return null
        val p = payload.split('-')
        if (p.size != 12) return null
        val n = (2..10).map { fromB36(p[it]) ?: return null }
        return FriendCard(
            id = p[1], name = RemoteDuel.displayName(p[11]), avatar = n[0], banner = n[1], level = n[2],
            prestige = n[3], stars = n[4], pieces = n[5], tower = n[6], league = n[7], day = n[8]
        )
    }

    fun inviteText(code: String): String =
        "¡Agrégame en ParDos! 🧩\nAbre ParDos → Perfil → Amigos, pega este código y ya somos amigos:\n\n$code"
}
