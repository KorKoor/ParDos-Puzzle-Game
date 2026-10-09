package com.korkoor.pardos.domain.meta

/**
 * Memoria del jugador para la versión de iPhone: un diccionario de texto que se guarda entero (un solo valor en UserDefaults).
 * Android usa SharedPreferences por área; aquí todo va junto y es puro Kotlin, así se prueba en Windows.
 *
 * Formato de [export]: una línea por clave, `clave<TAB>valor`, con `\`, salto de línea y tabulación escapados en el valor.
 */
class MetaStore {
    private val map = LinkedHashMap<String, String>()

    fun has(key: String): Boolean = map.containsKey(key)

    fun str(key: String, def: String = ""): String = map[key] ?: def
    fun setStr(key: String, value: String) { map[key] = value }

    fun int(key: String, def: Int = 0): Int = map[key]?.toIntOrNull() ?: def
    fun setInt(key: String, value: Int) { map[key] = value.toString() }
    fun addInt(key: String, delta: Int): Int { val v = int(key) + delta; setInt(key, v); return v }

    fun long(key: String, def: Long = 0L): Long = map[key]?.toLongOrNull() ?: def
    fun setLong(key: String, value: Long) { map[key] = value.toString() }

    fun bool(key: String, def: Boolean = false): Boolean = map[key]?.let { it == "1" } ?: def
    fun setBool(key: String, value: Boolean) { map[key] = if (value) "1" else "0" }

    fun double(key: String, def: Double = 0.0): Double = map[key]?.toDoubleOrNull() ?: def
    fun setDouble(key: String, value: Double) { map[key] = value.toString() }

    fun strSet(key: String): Set<String> = (map[key] ?: "").split(',').filter { it.isNotEmpty() }.toSet()
    fun setStrSet(key: String, value: Collection<String>) { map[key] = value.joinToString(",") }
    fun addToStrSet(key: String, item: String) { setStrSet(key, strSet(key) + item) }

    fun intSet(key: String): Set<Int> = (map[key] ?: "").split(',').mapNotNull { it.toIntOrNull() }.toSet()
    fun setIntSet(key: String, value: Collection<Int>) { map[key] = value.joinToString(",") }
    fun addToIntSet(key: String, item: Int) { setIntSet(key, intSet(key) + item) }

    fun remove(key: String) { map.remove(key) }
    fun removeWithPrefix(prefix: String) { map.keys.filter { it.startsWith(prefix) }.forEach { map.remove(it) } }
    fun clear() { map.clear() }
    val size: Int get() = map.size

    fun export(): String {
        val sb = StringBuilder()
        for ((k, v) in map) {
            sb.append(k).append('\t')
            for (ch in v) {
                when (ch) {
                    '\\' -> sb.append("\\\\")
                    '\n' -> sb.append("\\n")
                    '\t' -> sb.append("\\t")
                    else -> sb.append(ch)
                }
            }
            sb.append('\n')
        }
        return sb.toString()
    }

    fun import(raw: String) {
        map.clear()
        for (line in raw.split('\n')) {
            val tab = line.indexOf('\t')
            if (tab <= 0) continue
            val key = line.substring(0, tab)
            val enc = line.substring(tab + 1)
            val sb = StringBuilder()
            var i = 0
            while (i < enc.length) {
                val ch = enc[i]
                if (ch == '\\' && i + 1 < enc.length) {
                    when (enc[i + 1]) {
                        'n' -> sb.append('\n')
                        't' -> sb.append('\t')
                        else -> sb.append(enc[i + 1])
                    }
                    i += 2
                } else {
                    sb.append(ch)
                    i++
                }
            }
            map[key] = sb.toString()
        }
    }
}
