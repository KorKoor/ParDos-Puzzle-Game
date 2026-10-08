package com.korkoor.pardos.domain.logic

enum class DuelWinner { PLAYER_1, PLAYER_2, TIE }

/** Duelo local: dos jugadores, el mismo tablero (misma semilla), una ronda cronometrada cada uno. */
object DuelRules {
    const val ROUND_MS = 60_000L
    const val BOARD_SIZE = 4
    /** Meta inalcanzable en 60 s: la ronda siempre termina por tiempo o por tablero bloqueado. */
    const val TARGET = 4096

    fun winner(score1: Int, score2: Int): DuelWinner = when {
        score1 > score2 -> DuelWinner.PLAYER_1
        score2 > score1 -> DuelWinner.PLAYER_2
        else -> DuelWinner.TIE
    }

    /** Diferencia de puntos (siempre >= 0), para mostrar "ganó por N". */
    fun margin(score1: Int, score2: Int): Int = kotlin.math.abs(score1 - score2)
}
