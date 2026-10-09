package com.korkoor.pardos.domain.shop

/**
 * Cuándo pedirle al jugador una reseña en la tienda. Las reseñas buenas son lo que más ayuda a que el juego se encuentre (y a que
 * se pueda vivir de él), pero pedirlas mal las vuelve malas: solo en un momento feliz, nunca al empezar, nunca tras perder, nunca
 * pegada a un anuncio y pocas veces en total. La ventana la dibuja Google Play (que además tiene su propio tope).
 */
object RatePolicy {
    /** Ya conoce el juego: no se pide en las primeras partidas. */
    const val MIN_CAMPAIGN_LEVEL = 12
    /** Ha vuelto al menos otro día: no se pide en la primera sesión. */
    const val MIN_INSTALL_DAYS = 2
    const val MIN_DAYS_BETWEEN_ASKS = 60
    const val MAX_ASKS = 3
    /** Racha mínima de victorias seguidas para que cuente como momento feliz (salvo un jefe vencido). */
    const val MIN_WIN_STREAK = 3

    data class State(
        val campaignLevel: Int,
        val installDays: Int,
        /** Días desde la última vez que se pidió (null = nunca). */
        val daysSinceLastAsk: Int?,
        val asks: Int,
        val winStreak: Int,
        val threeStarWin: Boolean,
        val bossWin: Boolean,
        /** Justo ahora se va a enseñar (o se acaba de enseñar) un anuncio: no se juntan. */
        val adNow: Boolean,
        val vip: Boolean = false
    )

    fun shouldAsk(s: State): Boolean =
        s.asks < MAX_ASKS &&
            s.campaignLevel >= MIN_CAMPAIGN_LEVEL &&
            s.installDays >= MIN_INSTALL_DAYS &&
            (s.daysSinceLastAsk == null || s.daysSinceLastAsk >= MIN_DAYS_BETWEEN_ASKS) &&
            !s.adNow &&
            (s.bossWin || (s.threeStarWin && s.winStreak >= MIN_WIN_STREAK))
}
