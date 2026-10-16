package com.korkoor.pardos.data.local

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Código para compartir una repetición por chat: "PDR1-" + texto comprimido en base64. Lleva el tablero de cada jugada (hasta [MAX_FRAMES]),
 * así quien lo recibe la ve jugada a jugada y la compara con la suya. No lleva datos personales.
 */
object ReplayCode {
    private const val PREFIX = "PDR1-"
    private const val MAX_FRAMES = 150
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

    fun encode(replay: SavedGameReplay): String {
        val frames = replay.frames.takeLast(MAX_FRAMES)
        val sb = StringBuilder()
        sb.append(replay.boardSize).append(';').append(replay.target).append(';').append(replay.finalScore).append(';').append(replay.savedAt)
        for (f in frames) {
            sb.append('\n').append(f.move).append(',').append(f.direction.replace(",", "").replace("\n", "")).append(',').append(f.score).append(',')
            sb.append(f.tiles.joinToString(";") { "${it.row} ${it.col} ${it.value}" })
        }
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(sb.toString().toByteArray(Charsets.UTF_8)) }
        return PREFIX + b64(out.toByteArray())
    }

    /** Devuelve null si el código está mal copiado o no es de ParDos. */
    fun decode(code: String): SavedGameReplay? = try {
        val clean = code.trim().replace(Regex("\\s"), "")
        if (!clean.startsWith(PREFIX)) null else {
            val bytes = unb64(clean.removePrefix(PREFIX))
            val text = GZIPInputStream(ByteArrayInputStream(bytes)).use { String(it.readBytes(), Charsets.UTF_8) }
            val lines = text.split('\n')
            val head = lines[0].split(';')
            val size = head[0].toInt()
            val frames = lines.drop(1).filter { it.isNotBlank() }.take(MAX_FRAMES).map { line ->
                val p = line.split(',', limit = 4)
                val tiles = p[3].split(';').filter { it.isNotBlank() }.map { t ->
                    val n = t.trim().split(' ')
                    ReplayTile(n[0].toInt(), n[1].toInt(), n[2].toInt())
                }.filter { it.row in 0 until size && it.col in 0 until size }
                ReplayFrame(p[0].toInt(), p[1], p[2].toInt(), tiles)
            }
            if (size !in 3..6 || frames.isEmpty()) null else SavedGameReplay(size, head[1].toInt(), head[2].toInt(), frames, head[3].toLong())
        }
    } catch (_: Exception) { null }

    private fun b64(data: ByteArray): String {
        val sb = StringBuilder()
        var i = 0
        while (i < data.size) {
            val b0 = data[i].toInt() and 0xFF
            val b1 = if (i + 1 < data.size) data[i + 1].toInt() and 0xFF else 0
            val b2 = if (i + 2 < data.size) data[i + 2].toInt() and 0xFF else 0
            val n = (b0 shl 16) or (b1 shl 8) or b2
            sb.append(ALPHABET[(n shr 18) and 63]).append(ALPHABET[(n shr 12) and 63])
            if (i + 1 < data.size) sb.append(ALPHABET[(n shr 6) and 63])
            if (i + 2 < data.size) sb.append(ALPHABET[n and 63])
            i += 3
        }
        return sb.toString()
    }

    private fun unb64(s: String): ByteArray {
        val out = ByteArrayOutputStream()
        var i = 0
        while (i < s.length) {
            val chunk = s.substring(i, minOf(i + 4, s.length))
            var n = 0
            for (c in chunk) n = (n shl 6) or ALPHABET.indexOf(c).also { require(it >= 0) }
            n = n shl (6 * (4 - chunk.length))
            out.write((n shr 16) and 0xFF)
            if (chunk.length > 2) out.write((n shr 8) and 0xFF)
            if (chunk.length > 3) out.write(n and 0xFF)
            i += 4
        }
        return out.toByteArray()
    }
}
