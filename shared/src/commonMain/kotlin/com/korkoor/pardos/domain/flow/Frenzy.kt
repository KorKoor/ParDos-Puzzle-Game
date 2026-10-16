package com.korkoor.pardos.domain.flow

/**
 * Frenesí: al llegar al escalón FLOW del ímpetu, las siguientes [MOVES] jugadas dan el doble de puntos. Es una recompensa por jugar
 * bien (no se compra ni se pide): se enciende solo, dura poco y se ve en pantalla.
 */
object Frenzy {
    const val MOVES = 3
    const val MULTIPLIER = 2

    /** ¿Esta jugada acaba de llevar la racha al escalón FLOW? (solo la jugada que lo cruza, no las siguientes) */
    fun triggeredBy(streakBefore: Int, streakAfter: Int): Boolean = FlowMeter.tierUp(streakBefore, streakAfter) == FlowTier.FLOW

    /** Puntos de una jugada: dobles mientras quedan jugadas de frenesí. */
    fun score(gained: Int, movesLeft: Int): Int = if (movesLeft > 0) gained * MULTIPLIER else gained

    /** Jugadas de frenesí que quedan tras gastar una (nunca baja de 0). */
    fun afterMove(movesLeft: Int): Int = (movesLeft - 1).coerceAtLeast(0)
}
