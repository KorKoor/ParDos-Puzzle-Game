package com.korkoor.pardos.domain.logic

/** Una etapa del Modo Carrera: tablero y meta. */
data class RaceStage(val number: Int, val boardSize: Int, val target: Int)

/**
 * Modo Carrera: etapas encadenadas contra un reloj global. Cada etapa superada añade tiempo.
 * El resultado es cuántas etapas completas antes de que se acabe el tiempo (o el tablero se bloquee).
 */
object RaceRules {
    const val START_TIME_MS = 90_000L
    const val MAX_TIME_MS = 180_000L

    /** Las primeras etapas son cortas y van subiendo; desde la 9 se estabiliza en el máximo. */
    fun stage(number: Int): RaceStage {
        val n = number.coerceAtLeast(1)
        return when (n) {
            1 -> RaceStage(n, 3, 16)
            2 -> RaceStage(n, 3, 32)
            3 -> RaceStage(n, 4, 64)
            4 -> RaceStage(n, 4, 128)
            5 -> RaceStage(n, 4, 256)
            6 -> RaceStage(n, 5, 256)
            7 -> RaceStage(n, 5, 512)
            8 -> RaceStage(n, 5, 1024)
            else -> RaceStage(n, 6, 2048)
        }
    }

    /** Tiempo extra al superar la etapa [number]: más generoso a medida que se complican. */
    fun timeBonusMs(number: Int): Long = (15_000L + 2_000L * number.coerceAtLeast(1)).coerceAtMost(30_000L)

    /** Tiempo restante tras superar una etapa: se suma el bono pero nunca pasa del máximo. */
    fun timeAfterStage(remainingMs: Long, clearedStage: Int): Long =
        (remainingMs + timeBonusMs(clearedStage)).coerceAtMost(MAX_TIME_MS)

    /** Monedas por terminar la carrera con [stagesCleared] etapas superadas (sin eventos). */
    fun coinsFor(stagesCleared: Int): Int = (stagesCleared.coerceAtLeast(0)) * 20
}
