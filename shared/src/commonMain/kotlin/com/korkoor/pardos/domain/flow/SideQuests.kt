package com.korkoor.pardos.domain.flow

/**
 * Reto bonus de cada nivel de campaña: un mini objetivo OPCIONAL que se ve durante la partida y se comprueba al ganar. Da una
 * pequeña cantidad de monedas extra y sirve para jugar un mismo nivel de otra manera (más limpio, más rápido, con más ímpetu).
 * No cambia las estrellas ni obliga a nada: si no se cumple, no se pierde nada.
 */
enum class SideQuest(val title: String, val hint: String, val coins: Int) {
    FLOW("Ímpetu", "Llega a ¡Imparable! (8 fusiones seguidas)", 25),
    CLEAN("Mesa despejada", "Gana con la mitad del tablero libre", 20),
    SPARE("Sobrado", "Gana con un cuarto de tus movimientos sin usar", 25),
    PERFECT("Perfecto", "Consigue las 3 estrellas", 20),
    SWIFT("Veloz", "Gana en menos de 90 segundos", 20)
}

/** Datos de la partida ganada que hacen falta para saber si se cumplió el reto. */
data class QuestResult(
    val stars: Int,
    val moveLimit: Int?,
    val moveCount: Int,
    val emptyCells: Int,
    val freeCells: Int,
    val peakTier: FlowTier,
    val timeMs: Long
)

object SideQuests {
    /** Los primeros niveles son para aprender: sin retos bonus. */
    const val MIN_LEVEL = 4

    /** Reto de un nivel (siempre el mismo para el mismo nivel), o null si no toca (niveles de aprendizaje, jefes). */
    fun forLevel(level: Int, hasMoveLimit: Boolean, isBoss: Boolean): SideQuest? {
        if (level < MIN_LEVEL || isBoss) return null
        val options = SideQuest.entries.filter { it != SideQuest.SPARE || hasMoveLimit }
        // mezcla simple para que niveles seguidos no repitan el mismo reto
        val h = (level * 2654435761L + 12345L) and 0x7FFFFFFFL
        return options[(h % options.size).toInt()]
    }

    fun achieved(quest: SideQuest, r: QuestResult): Boolean = when (quest) {
        SideQuest.FLOW -> r.peakTier.ordinal >= FlowTier.HOT.ordinal
        SideQuest.CLEAN -> r.freeCells > 0 && r.emptyCells * 2 >= r.freeCells
        SideQuest.SPARE -> r.moveLimit != null && r.moveLimit - r.moveCount >= r.moveLimit / 4
        SideQuest.PERFECT -> r.stars >= 3
        SideQuest.SWIFT -> r.timeMs in 1 until 90_000L
    }
}
