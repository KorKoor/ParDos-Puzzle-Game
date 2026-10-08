package com.korkoor.pardos.domain.rewards

/** Reglas de monedas por jugar. Se mantienen aquí para poder probarlas y compartirlas con iOS. */
object CoinRewards {
    /** Monedas al ganar un nivel: base + estrellas + bonus por primera vez. */
    fun forLevelWin(stars: Int, firstClear: Boolean): Int {
        val s = stars.coerceIn(1, 3)
        return 10 + 10 * s + if (firstClear) 20 else 0
    }

    /** Monedas al reclamar una misión diaria (la mitad de la XP). */
    fun forMission(xpReward: Int): Int = (xpReward / 2).coerceAtLeast(5)
}
