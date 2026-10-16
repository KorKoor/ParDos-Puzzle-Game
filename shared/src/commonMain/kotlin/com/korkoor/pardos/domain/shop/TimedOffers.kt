package com.korkoor.pardos.domain.shop

import com.korkoor.pardos.domain.economy.Economy

/**
 * Ofertas de tiempo limitado HONESTAS: salen en días fijos del calendario (no se reinician ni se reabren al volver a entrar), el reloj
 * enseña el final real, se pagan con gemas del propio jugador (nada de compras con dinero por sorpresa), el ahorro que se anuncia
 * se calcula con los precios normales de lo que lleva, y cada una se puede comprar una sola vez por ventana.
 */
data class OfferContents(val undos: Int = 0, val extraTimes: Int = 0, val freezes: Int = 0, val rareChests: Int = 0) {
    /** Cuánto costaría todo por separado, en monedas. */
    val coinValue: Int
        get() = undos * Economy.UNDO_PRICE_COINS + extraTimes * Economy.EXTRA_TIME_PRICE_COINS +
            freezes * Economy.STREAK_FREEZE_PRICE_COINS + rareChests * Economy.RARE_CHEST_COINS

    /** Lo mismo, en gemas (al cambio normal de la tienda), redondeado hacia arriba. */
    val gemValue: Int get() = (coinValue + CoinShop.COINS_PER_GEM - 1) / CoinShop.COINS_PER_GEM
}

data class TimedOffer(
    val id: String,
    val title: String,
    val blurb: String,
    val contents: OfferContents,
    val priceGems: Int,
    /** Primer y último día (inclusive) de la ventana, contados desde 1970. */
    val firstDay: Long,
    val lastDay: Long
) {
    /** Identifica esta ventana concreta: comprar hoy no impide comprar la del fin de semana siguiente. */
    val windowId: String get() = "$id-$firstDay"

    /** Ahorro real frente a comprar todo por separado. */
    val savingPercent: Int get() = ((1.0 - priceGems.toDouble() / contents.gemValue) * 100).toInt().coerceAtLeast(0)

    fun activeOn(epochDay: Long): Boolean = epochDay in firstDay..lastDay
}

object TimedOffers {
    /** Día de la semana: 0 = lunes … 6 = domingo. */
    fun weekday(epochDay: Long): Int = (((epochDay + 3) % 7) + 7).toInt() % 7

    private val weekendContents = OfferContents(undos = 5, extraTimes = 3, freezes = 1, rareChests = 1)
    private val midweekContents = OfferContents(undos = 10, extraTimes = 5)

    /** Gemas que se piden: un 20 % menos que el valor real de lo que lleva, redondeado hacia arriba. */
    private fun priceFor(c: OfferContents): Int = (c.gemValue * 80 + 99) / 100

    /** Ofertas activas en [epochDay]: el pack del fin de semana (sábado y domingo) y el relámpago del miércoles. */
    fun activeOn(epochDay: Long): List<TimedOffer> {
        val wd = weekday(epochDay)
        val list = mutableListOf<TimedOffer>()
        if (wd >= 5) {
            val sat = epochDay - (wd - 5)
            list += TimedOffer(
                "weekend", "Pack de fin de semana", "Solo el sábado y el domingo: ayudas para los niveles difíciles y un cofre raro.",
                weekendContents, priceFor(weekendContents), sat, sat + 1
            )
        }
        if (wd == 2) {
            list += TimedOffer(
                "midweek", "Relámpago de mitad de semana", "Solo hoy: deshacer y tiempo extra de sobra.",
                midweekContents, priceFor(midweekContents), epochDay, epochDay
            )
        }
        return list
    }
}

/**
 * Ayuda cuando un nivel se está atascando: tras [MIN_LOSSES] derrotas seguidas en el mismo nivel se ofrece UNA vez (por nivel) un
 * paquete pequeño al precio normal, sin descuento inventado, y nunca más de [MAX_PER_DAY] veces al día. Es una ayuda opcional:
 * el nivel se puede ganar sin ella y siempre se dice. Quien no quiere pagar sigue teniendo la vía gratuita del anuncio.
 */
object StruggleHelp {
    const val MIN_LOSSES = 3
    const val MAX_PER_DAY = 2
    val contents = OfferContents(undos = 2, extraTimes = 1)

    /** Precio normal en gemas (sin descuento). */
    val priceGems: Int get() = contents.gemValue

    fun shouldOffer(consecutiveLosses: Int, alreadyOfferedForThisLevel: Boolean, offeredToday: Int): Boolean =
        consecutiveLosses >= MIN_LOSSES && !alreadyOfferedForThisLevel && offeredToday < MAX_PER_DAY
}
