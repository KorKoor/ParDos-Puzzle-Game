package com.korkoor.pardos.domain.meta

/**
 * Copia de seguridad como texto: el estado del jugador en base 64 con un control al final. Se comparte por WhatsApp/Notas y se
 * pega al volver a instalar (importante en iPhone con Sideloadly, donde la app se reinstala cada 7 días).
 */
internal object MetaBackup {
    const val PREFIX = "PARDOS1"
    private const val TABLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    fun encode(state: String): String {
        val bytes = state.encodeToByteArray()
        val sb = StringBuilder()
        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i].toInt() and 0xFF
            val b1 = if (i + 1 < bytes.size) bytes[i + 1].toInt() and 0xFF else -1
            val b2 = if (i + 2 < bytes.size) bytes[i + 2].toInt() and 0xFF else -1
            sb.append(TABLE[b0 shr 2])
            sb.append(TABLE[((b0 and 3) shl 4) or (if (b1 >= 0) b1 shr 4 else 0)])
            sb.append(if (b1 >= 0) TABLE[((b1 and 15) shl 2) or (if (b2 >= 0) b2 shr 6 else 0)] else '=')
            sb.append(if (b2 >= 0) TABLE[b2 and 63] else '=')
            i += 3
        }
        val body = sb.toString()
        return PREFIX + "." + body + "." + checksum(body)
    }

    /** Devuelve el estado guardado o `null` si el texto no es una copia válida (se busca dentro de cualquier mensaje pegado). */
    fun decode(text: String): String? {
        val start = text.indexOf(PREFIX + ".")
        if (start < 0) return null
        val rest = text.substring(start + PREFIX.length + 1)
        val cleaned = StringBuilder()
        for (ch in rest) {
            if (ch.isLetterOrDigit() || ch == '+' || ch == '/' || ch == '=' || ch == '.') cleaned.append(ch) else break
        }
        val parts = cleaned.toString().split('.')
        if (parts.size < 2) return null
        val body = parts[0]
        if (checksum(body) != parts[1]) return null
        val out = ArrayList<Byte>()
        var i = 0
        while (i + 4 <= body.length) {
            val c0 = TABLE.indexOf(body[i])
            val c1 = TABLE.indexOf(body[i + 1])
            val c2 = if (body[i + 2] == '=') -1 else TABLE.indexOf(body[i + 2])
            val c3 = if (body[i + 3] == '=') -1 else TABLE.indexOf(body[i + 3])
            if (c0 < 0 || c1 < 0) return null
            out.add(((c0 shl 2) or (c1 shr 4)).toByte())
            if (c2 >= 0) out.add((((c1 and 15) shl 4) or (c2 shr 2)).toByte())
            if (c3 >= 0 && c2 >= 0) out.add((((c2 and 3) shl 6) or c3).toByte())
            i += 4
        }
        return out.toByteArray().decodeToString()
    }

    private fun checksum(body: String): String {
        var h = 5381L
        for (c in body) h = (h * 33 + c.code) and 0xFFFFFFFFL
        return h.toString(16)
    }
}
