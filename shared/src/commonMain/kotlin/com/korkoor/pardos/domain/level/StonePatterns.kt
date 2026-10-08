package com.korkoor.pardos.domain.level

import kotlin.random.Random

/**
 * Colocación de las piedras de un nivel.
 *  - [severity] mide cuánto estorban: 1 casi nada (rincones), 2 molestan (una piedra suelta), 3 parten el tablero.
 *  - [maxExp] es la ficha más alta (como potencia de 2) con la que un bot sencillo sigue ganando casi siempre
 *    (medido con `LevelFeasibilityTest`); el generador nunca pide más que eso con ese patrón.
 */
data class StonePattern(val id: String, val name: String, val size: Int, val cells: List<Cell>, val severity: Int, val maxExp: Int)

object StonePatterns {
    private fun p(id: String, name: String, size: Int, severity: Int, maxExp: Int, vararg cells: Cell) =
        StonePattern(id, name, size, cells.toList(), severity, maxExp)

    val all: List<StonePattern> = listOf(
        // ---- 4×4 · gravedad 1 ----
        p("4-esq-nw", "Rincón", 4, 1, 9, 0 to 0),
        p("4-esq-ne", "Rincón", 4, 1, 9, 0 to 3),
        p("4-esq-sw", "Rincón", 4, 1, 9, 3 to 0),
        p("4-esq-se", "Rincón", 4, 1, 9, 3 to 3),
        p("4-dos-nwse", "Dos rincones", 4, 1, 9, 0 to 0, 3 to 3),
        p("4-dos-nesw", "Dos rincones", 4, 1, 8, 0 to 3, 3 to 0),
        p("4-tres-rincones", "Tres rincones", 4, 1, 8, 0 to 0, 0 to 3, 3 to 0),
        p("4-escuadra", "Esquina doble", 4, 1, 8, 0 to 0, 0 to 1),
        // ---- 4×4 · gravedad 2 ----
        p("4-cuatro", "Cuatro rincones", 4, 2, 8, 0 to 0, 0 to 3, 3 to 0, 3 to 3),
        p("4-pilar-a", "Pilar", 4, 2, 8, 1 to 1),
        p("4-pilar-b", "Pilar", 4, 2, 8, 1 to 2),
        p("4-pilar-c", "Pilar", 4, 2, 8, 2 to 1),
        p("4-pilar-d", "Pilar", 4, 2, 8, 2 to 2),
        p("4-borde-n", "Borde", 4, 2, 8, 0 to 1),
        p("4-borde-s", "Borde", 4, 2, 8, 3 to 2),
        p("4-borde-w", "Borde", 4, 2, 8, 2 to 0),
        p("4-borde-e", "Borde", 4, 2, 8, 1 to 3),
        p("4-linea-borde", "Dintel", 4, 2, 8, 0 to 1, 0 to 2),
        p("4-cuna", "Cuña", 4, 2, 8, 0 to 0, 0 to 1, 1 to 0),
        p("4-dos-borde", "Dos bordes", 4, 2, 7, 0 to 1, 3 to 2),
        p("4-rincon-borde", "Rincón y borde", 4, 2, 8, 0 to 0, 3 to 2),
        p("4-pilar-borde", "Pilar lejano", 4, 2, 8, 1 to 1, 3 to 3),
        // ---- 4×4 · gravedad 3 ----
        p("4-rincon-pilar", "Rincón y pilar", 4, 3, 7, 0 to 0, 2 to 2),
        p("4-columna", "Columna", 4, 3, 7, 1 to 1, 2 to 1),
        p("4-diag", "Diagonal", 4, 3, 6, 1 to 1, 2 to 2),
        p("4-diag-b", "Diagonal", 4, 3, 6, 1 to 2, 2 to 1),
        // ---- 5×5 · gravedad 1 ----
        p("5-centro", "Roca central", 5, 1, 10, 2 to 2),
        p("5-esq-dos", "Dos rincones", 5, 1, 10, 0 to 0, 4 to 4),
        p("5-esq-dos-b", "Dos rincones", 5, 1, 10, 0 to 4, 4 to 0),
        p("5-cuatro", "Cuatro rincones", 5, 1, 10, 0 to 0, 0 to 4, 4 to 0, 4 to 4),
        p("5-esquinas-dobles", "Esquinas dobles", 5, 1, 10, 0 to 0, 0 to 1, 4 to 3, 4 to 4),
        // ---- 5×5 · gravedad 2 ----
        p("5-ventanas", "Ventanas", 5, 2, 10, 1 to 1, 3 to 3),
        p("5-columna", "Dos columnas", 5, 2, 10, 1 to 2, 3 to 2),
        p("5-diag", "Diagonal", 5, 2, 10, 0 to 0, 1 to 1, 3 to 3, 4 to 4),
        p("5-cruz", "Cruz", 5, 2, 10, 2 to 0, 2 to 4, 0 to 2, 4 to 2),
        p("5-escalera", "Escalera", 5, 2, 9, 0 to 0, 1 to 1, 2 to 2),
        p("5-cuatro-rinc-centro", "Rincones y centro", 5, 2, 9, 0 to 0, 0 to 4, 4 to 0, 4 to 4, 2 to 2),
        p("5-pilares", "Cuatro pilares", 5, 2, 8, 1 to 1, 1 to 3, 3 to 1, 3 to 3),
        p("5-cuatro-bordes", "Cuatro bordes", 5, 2, 8, 0 to 2, 2 to 0, 2 to 4, 4 to 2, 2 to 2),
        // ---- 5×5 · gravedad 3 ----
        p("5-muralla", "Muralla", 5, 3, 8, 2 to 0, 2 to 1, 2 to 3, 2 to 4),

        // ================= Patrones nuevos =================
        // ---- 4×4 · gravedad 1 ----
        p("4-dos-norte", "Dos rincones norte", 4, 1, 9, 0 to 0, 0 to 3),
        p("4-dos-sur", "Dos rincones sur", 4, 1, 9, 3 to 0, 3 to 3),
        p("4-dos-oeste", "Dos rincones oeste", 4, 1, 8, 0 to 0, 3 to 0),
        p("4-dos-este", "Dos rincones este", 4, 1, 9, 0 to 3, 3 to 3),
        // ---- 4×4 · gravedad 2 ----
        p("4-cuna-ne", "Cuña", 4, 2, 8, 0 to 3, 0 to 2, 1 to 3),
        p("4-cuna-sw", "Cuña", 4, 2, 7, 3 to 0, 3 to 1, 2 to 0),
        p("4-cuna-se", "Cuña", 4, 2, 8, 3 to 3, 3 to 2, 2 to 3),
        p("4-bordes-cruzados", "Bordes cruzados", 4, 2, 7, 1 to 0, 2 to 3),
        p("4-pilar-rincon", "Pilar y rincón", 4, 2, 7, 1 to 2, 3 to 0),
        p("4-dintel-sur", "Umbral", 4, 2, 8, 3 to 1, 3 to 2),
        p("4-flancos", "Flancos", 4, 2, 8, 1 to 0, 2 to 0),
        // ---- 4×4 · gravedad 3 ----
        p("4-muro-este", "Muro", 4, 3, 7, 1 to 2, 2 to 2),
        p("4-puente", "Puente", 4, 3, 6, 1 to 1, 1 to 2),
        p("4-puente-sur", "Puente", 4, 3, 7, 2 to 1, 2 to 2),
        // ---- 5×5 · gravedad 1 ----
        p("5-rincon-grande", "Rincón grande", 5, 1, 10, 0 to 0, 0 to 1, 1 to 0),
        p("5-dos-cunas", "Dos cuñas", 5, 1, 10, 0 to 0, 0 to 1, 1 to 0, 4 to 4, 4 to 3, 3 to 4),
        // ---- 5×5 · gravedad 2 ----
        p("5-cuatro-cunas", "Cuatro cuñas", 5, 2, 8, 0 to 0, 0 to 1, 1 to 0, 0 to 4, 0 to 3, 1 to 4, 4 to 0, 3 to 0, 4 to 1, 4 to 4, 3 to 4, 4 to 3),
        p("5-isla", "Isla", 5, 2, 8, 2 to 1, 2 to 2, 2 to 3),
        p("5-columnas-laterales", "Columnas laterales", 5, 2, 9, 1 to 0, 2 to 0, 3 to 0, 1 to 4, 2 to 4, 3 to 4),
        // ---- 5×5 · gravedad 3 ----
        p("5-cruz-central", "Cruz central", 5, 3, 7, 2 to 2, 1 to 2, 3 to 2, 2 to 1, 2 to 3),
        p("5-aspa", "Aspa", 5, 3, 7, 0 to 0, 1 to 1, 3 to 3, 4 to 4, 0 to 4, 1 to 3, 3 to 1, 4 to 0),
        p("5-pasillos", "Pasillos", 5, 3, 7, 1 to 1, 1 to 2, 1 to 3, 3 to 1, 3 to 2, 3 to 3),
        // ---- 6×6 · gravedad 1 ----
        p("6-cuatro", "Cuatro rincones", 6, 1, 10, 0 to 0, 0 to 5, 5 to 0, 5 to 5),
        p("6-pilares", "Cuatro pilares", 6, 1, 10, 1 to 1, 1 to 4, 4 to 1, 4 to 4),
        p("6-dos", "Dos rincones", 6, 1, 10, 0 to 0, 5 to 5),
        // ---- 6×6 · gravedad 2 ----
        p("6-centro", "Corazón", 6, 2, 10, 2 to 2, 2 to 3, 3 to 2, 3 to 3),
        p("6-diagonales", "Diagonales", 6, 2, 10, 0 to 0, 1 to 1, 4 to 4, 5 to 5),
        p("6-marcos", "Ventanas laterales", 6, 2, 10, 0 to 2, 0 to 3, 5 to 2, 5 to 3, 2 to 0, 3 to 0, 2 to 5, 3 to 5),
        p("6-islas", "Islas", 6, 2, 10, 1 to 1, 1 to 2, 4 to 3, 4 to 4),
        // ---- 6×6 · gravedad 3 ----
        p("6-aspa", "Aspa", 6, 3, 8, 1 to 1, 2 to 2, 3 to 3, 4 to 4, 1 to 4, 2 to 3, 3 to 2, 4 to 1),
        p("6-muralla", "Muralla", 6, 3, 9, 2 to 0, 2 to 1, 2 to 2, 3 to 3, 3 to 4)
    )

    fun byId(id: String): StonePattern =
        all.firstOrNull { it.id == id } ?: throw IllegalArgumentException("Patrón de piedras desconocido: $id")

    fun forSize(size: Int, maxSeverity: Int = 3): List<StonePattern> =
        all.filter { it.size == size && it.severity <= maxSeverity }

    /** Un patrón al azar de ese tamaño y gravedad exacta (o menor si no hay). */
    fun pick(size: Int, severity: Int, random: Random): StonePattern? {
        val exact = all.filter { it.size == size && it.severity == severity }
        if (exact.isNotEmpty()) return exact.random(random)
        return forSize(size, severity).randomOrNull(random)
    }

    /** Exponente de la ficha meta para ese patrón: el pedido, sin pasar de lo que el patrón admite. */
    fun exponentFor(requested: Int, pattern: StonePattern): Int = minOf(requested, pattern.maxExp).coerceAtLeast(6)
}
