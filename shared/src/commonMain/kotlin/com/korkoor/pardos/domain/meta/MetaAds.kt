package com.korkoor.pardos.domain.meta

import com.korkoor.pardos.domain.collection.TokenRules
import com.korkoor.pardos.domain.shop.AdRewards

/**
 * Premios por ver un anuncio (o gratis para VIP: la app decide si muestra el anuncio; aquí solo se paga y se llevan los topes del día).
 * Cada tipo de premio tiene un tope diario para que no se pueda abusar.
 */
internal class AdOps(
    private val s: MetaStore,
    private val wallet: Wallet,
    private val col: CollectionOps,
    private val ret: Retention,
    private val clock: Clock
) {
    private fun used(key: String): Int = if (s.int("ad_" + key + "_day", -1) == clock.today) s.int("ad_" + key + "_n") else 0

    private fun consume(key: String) {
        val n = used(key) + 1
        s.setInt("ad_" + key + "_day", clock.today)
        s.setInt("ad_" + key + "_n", n)
    }

    fun gemsLeft(): Int = AdRewards.left(used("gems"), AdRewards.FREE_GEMS_PER_DAY)
    fun tokensLeft(): Int = if (col.tokens >= TokenRules.MAX_STOCK) 0 else AdRewards.left(used("token"), AdRewards.TOKEN_PER_DAY)
    fun seasonLeft(): Int = AdRewards.left(used("season"), AdRewards.SEASON_BOOST_PER_DAY)

    fun freeGems(): Boolean {
        if (gemsLeft() <= 0) return false
        consume("gems")
        wallet.addGems(AdRewards.FREE_GEMS)
        return true
    }

    fun token(): Boolean {
        if (tokensLeft() <= 0) return false
        consume("token")
        col.addTokens(1)
        return true
    }

    fun seasonBoost(): Boolean {
        if (seasonLeft() <= 0) return false
        consume("season")
        ret.addSeasonPoints(AdRewards.SEASON_BOOST_POINTS)
        return true
    }

    /** Guarda lo que dio el regalo de hoy para poder duplicarlo después. */
    fun rememberGift(coins: Int, gems: Int) {
        s.setInt("gift_last_day", clock.today)
        s.setInt("gift_last_coins", coins)
        s.setInt("gift_last_gems", gems)
    }

    fun canDoubleGift(): Boolean = s.int("gift_last_day", -1) == clock.today && s.int("gift_double_day", -1) != clock.today

    /** Entrega otra vez el regalo de hoy (solo una vez al día). Devuelve (monedas, gemas) o null. */
    fun doubleGift(): Pair<Int, Int>? {
        if (!canDoubleGift()) return null
        val coins = s.int("gift_last_coins")
        val gems = s.int("gift_last_gems")
        wallet.addCoins(coins)
        wallet.addGems(gems)
        s.setInt("gift_double_day", clock.today)
        return coins to gems
    }
}
