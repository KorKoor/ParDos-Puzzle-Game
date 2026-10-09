package com.korkoor.pardos.domain.shop

/**
 * Cuándo se enseña un anuncio de pantalla completa (intersticial).
 *
 * Pocos y en pausas naturales: solo tras ganar un nivel, nunca tras perder, nunca en la primera sesión, nunca seguidos ni encima
 * de un anuncio con premio que el jugador acaba de elegir ver, y nunca para quien tiene VIP. El objetivo es ganar dinero sin
 * espantar a quien juega: los anuncios con premio (que el jugador pide) son la fuente principal.
 */
object AdPolicy {
    /** Los primeros niveles son para enamorarse del juego: sin intersticiales. */
    const val MIN_CAMPAIGN_LEVEL = 8
    /** Mínimo de tiempo desde que se instaló la app. */
    const val MIN_INSTALL_AGE_MS = 20 * 60_000L
    /** Victorias entre un intersticial y el siguiente. */
    const val WINS_BETWEEN = 3
    /** Separación mínima entre dos anuncios de cualquier tipo. */
    const val MIN_GAP_MS = 180_000L
    /** Tope diario de intersticiales. */
    const val MAX_PER_DAY = 6
    /** Si el jugador acaba de ver un anuncio con premio, el siguiente intersticial espera este tiempo. */
    const val AFTER_REWARDED_MS = 240_000L

    data class State(
        val vip: Boolean,
        /** Nivel de campaña que acaba de ganar (0 si no es de campaña). */
        val campaignLevel: Int,
        val winsSinceLastAd: Int,
        val msSinceLastAd: Long,
        val msSinceLastRewarded: Long,
        val shownToday: Int,
        val installAgeMs: Long,
        /** Solo se muestra justo después de ganar. */
        val afterWin: Boolean,
        /** Jefe vencido, hito de racha o premio grande: ese momento es de celebración y no se interrumpe. */
        val bigMoment: Boolean = false
    )

    fun shouldShowInterstitial(s: State): Boolean =
        !s.vip && s.afterWin && !s.bigMoment &&
            s.campaignLevel >= MIN_CAMPAIGN_LEVEL &&
            s.installAgeMs >= MIN_INSTALL_AGE_MS &&
            s.winsSinceLastAd >= WINS_BETWEEN &&
            s.msSinceLastAd >= MIN_GAP_MS &&
            s.msSinceLastRewarded >= AFTER_REWARDED_MS &&
            s.shownToday < MAX_PER_DAY
}

/** Premios por ver anuncios (el jugador los pide) y su tope diario: así el anuncio es un trato justo, no una trampa. */
object AdRewards {
    /** Pocas gemas por anuncio: que no compita con el VIP (5 ◆ al día) ni con los packs, pero que valga la pena ver uno. */
    const val FREE_GEMS = 3
    const val FREE_GEMS_PER_DAY = 3

    /** Una carta extra al abrir un cofre. */
    const val EXTRA_CARD_PER_DAY = 3

    /** Fichas de intercambio. */
    const val TOKEN_PER_DAY = 2

    /** Puntos de temporada. */
    const val SEASON_BOOST_POINTS = 40
    const val SEASON_BOOST_PER_DAY = 2

    fun left(used: Int, cap: Int): Int = (cap - used).coerceAtLeast(0)
}

/** Continuar una partida perdida: con anuncio o con gemas (tienen que ser visibles las dos vías). El precio es el mismo que en iPhone. */
object ContinueOffer {
    const val GEMS = com.korkoor.pardos.domain.economy.Economy.REVIVE_PRICE_GEMS

    fun canPayWithGems(gems: Int): Boolean = gems >= GEMS

    /** Gemas que le faltan para pagar (0 si le alcanza): sirve para decir "te faltan X" y llevarlo a la tienda. */
    fun missing(gems: Int): Int = (GEMS - gems).coerceAtLeast(0)
}
