package com.korkoor.pardos

/** Lector de JSON mínimo para las pruebas: comprueba que lo que se entrega a Swift está bien formado. */
internal class TestJson(private val s: String) {
    private var i = 0
    fun parse(): Any? { val v = value(); ws(); require(i == s.length) { "basura al final en $i" }; return v }
    private fun ws() { while (i < s.length && s[i].isWhitespace()) i++ }
    private fun value(): Any? {
        ws()
        return when (val c = s[i]) {
            '{' -> { i++; val m = LinkedHashMap<String, Any?>(); ws(); if (s[i] == '}') { i++; return m }
                while (true) { ws(); val k = str(); ws(); expect(':'); m[k] = value(); ws(); if (s[i] == ',') i++ else { expect('}'); break } }; m }
            '[' -> { i++; val l = ArrayList<Any?>(); ws(); if (s[i] == ']') { i++; return l }
                while (true) { l.add(value()); ws(); if (s[i] == ',') i++ else { expect(']'); break } }; l }
            '"' -> str()
            't' -> { i += 4; true }
            'f' -> { i += 5; false }
            'n' -> { i += 4; null }
            else -> { val st = i; while (i < s.length && (s[i].isDigit() || s[i] in "+-.eE")) i++; require(i > st) { "carácter '$c' en $i" }; s.substring(st, i).toDouble() }
        }
    }
    private fun expect(c: Char) { require(s[i] == c) { "se esperaba '$c' en $i y hay '${s[i]}'" }; i++ }
    private fun str(): String {
        expect('"'); val sb = StringBuilder()
        while (s[i] != '"') {
            if (s[i] == '\\') { i++; when (s[i]) { 'n' -> sb.append('\n'); 'r' -> sb.append('\r'); 't' -> sb.append('\t'); 'u' -> { sb.append(s.substring(i + 1, i + 5).toInt(16).toChar()); i += 4 }; else -> sb.append(s[i]) } }
            else sb.append(s[i])
            i++
        }
        i++; return sb.toString()
    }
}

@Suppress("UNCHECKED_CAST")
internal fun jsonObject(json: String): Map<String, Any?> = TestJson(json).parse() as Map<String, Any?>

@Suppress("UNCHECKED_CAST")
internal fun jsonArray(json: String): List<Any?> = TestJson(json).parse() as List<Any?>

internal fun Map<String, Any?>.int(k: String): Int = (this[k] as Double).toInt()
internal fun Map<String, Any?>.list(k: String): List<*> = this[k] as List<*>
@Suppress("UNCHECKED_CAST")
internal fun Map<String, Any?>.map(k: String): Map<String, Any?> = this[k] as Map<String, Any?>
