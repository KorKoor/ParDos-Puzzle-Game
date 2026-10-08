package com.korkoor.pardos.domain.shop

import com.korkoor.pardos.domain.economy.Economy
import kotlin.math.roundToInt

/** Cálculos de los packs de gemas: cuánto "extra" ofrece cada uno y la bonificación de la primera compra. */
object GemPacks {
    /** Packs de gemas, del más barato al más caro. */
    val packs: List<StoreProduct> get() = ShopCatalog.products.filter { it.kind == ProductKind.GEMS }.sortedBy { it.usdCents }

    private fun gemsPerDollar(p: StoreProduct) = p.gems * 100.0 / p.usdCents

    /** % de gemas extra frente al pack más barato (0 para el más barato). */
    fun bonusPercent(p: StoreProduct): Int {
        val base = packs.firstOrNull() ?: return 0
        if (p.usdCents <= 0 || base.usdCents <= 0) return 0
        return ((gemsPerDollar(p) / gemsPerDollar(base) - 1.0) * 100).roundToInt().coerceAtLeast(0)
    }

    /** El pack con más gemas por dólar de entre los que no son el más caro (el "recomendado"). */
    val bestValueId: String get() = ShopCatalog.GEMS_MEDIUM

    /** Primera compra de cada pack: doble de gemas. Después, el normal. */
    fun gemsForPurchase(p: StoreProduct, firstTime: Boolean): Int = if (firstTime) p.gems * Economy.FIRST_PURCHASE_MULTIPLIER else p.gems
}

/** Ventajas del VIP que se sienten cada día (además de los poderes con anuncio gratis y x2 monedas). */
object VipPerks {
    const val DAILY_GEMS = Economy.VIP_DAILY_GEMS
    const val COIN_BONUS_PERCENT = Economy.VIP_COIN_BONUS_PERCENT

    fun canClaimDaily(vip: Boolean, lastClaimDay: Int, today: Int): Boolean = vip && today != lastClaimDay

    /** Monedas de una victoria con el extra del VIP. */
    fun coinsWithBonus(coins: Int, vip: Boolean): Int =
        if (!vip || coins <= 0) coins else coins + (coins * COIN_BONUS_PERCENT + 99) / 100
}

/** Impulso de monedas: las próximas victorias dan más monedas. Se compra con gemas (sumidero de gemas). */
object CoinBoost {
    const val WINS = Economy.COIN_BOOST_WINS
    const val PRICE_GEMS = Economy.COIN_BOOST_PRICE_GEMS
    const val BONUS_PERCENT = Economy.COIN_BOOST_PERCENT
    /** Máximo de victorias acumuladas por comprar varias veces. */
    const val MAX_WINS = WINS * 3

    /** Monedas de una victoria con el impulso activo ([winsLeft] > 0). */
    fun apply(coins: Int, winsLeft: Int): Int =
        if (winsLeft <= 0 || coins <= 0) coins else coins + (coins * BONUS_PERCENT + 99) / 100

    fun addWins(current: Int): Int = (current + WINS).coerceAtMost(MAX_WINS)

    fun canBuy(gems: Int, winsLeft: Int): Boolean = gems >= PRICE_GEMS && winsLeft < MAX_WINS
}
