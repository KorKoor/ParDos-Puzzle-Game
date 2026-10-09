package com.korkoor.pardos.domain.meta

/** JSON mínimo (sin librerías) para entregar el estado a Swift. */
internal class Raw(val json: String)

internal fun obj(vararg pairs: Pair<String, Any?>): String =
    pairs.joinToString(",", "{", "}") { (k, v) -> "\"$k\":${jvalue(v)}" }

/** Objeto JSON ya armado, para meterlo dentro de listas sin que se vuelva a escapar como texto. */
internal fun robj(vararg pairs: Pair<String, Any?>): Raw = Raw(obj(*pairs))

internal fun arr(items: Iterable<Any?>): String = items.joinToString(",", "[", "]") { jvalue(it) }

internal fun jvalue(v: Any?): String = when (v) {
    null -> "null"
    is Raw -> v.json
    is Boolean, is Int, is Long -> v.toString()
    is Double -> if (v.isNaN() || v.isInfinite()) "0" else v.toString()
    is String -> jquote(v)
    is Iterable<*> -> v.joinToString(",", "[", "]") { jvalue(it) }
    else -> jquote(v.toString())
}

internal fun jquote(s: String): String {
    val sb = StringBuilder("\"")
    for (ch in s) {
        when {
            ch == '"' -> sb.append("\\\"")
            ch == '\\' -> sb.append("\\\\")
            ch == '\n' -> sb.append("\\n")
            ch == '\r' -> sb.append("\\r")
            ch == '\t' -> sb.append("\\t")
            ch.code < 0x20 -> sb.append("\\u").append(ch.code.toString(16).padStart(4, '0'))
            else -> sb.append(ch)
        }
    }
    return sb.append('"').toString()
}

/** Argb en Long (0xFFRRGGBB) → entero de 24 bits RGB (Swift lo lee como `Color(hex:)`). */
internal fun rgb(argb: Long): Int = (argb and 0xFFFFFF).toInt()
