"""Parche de una sola vez: premios por anuncio en la logica compartida (ruleta, cofre gratis, racha, regalo x2, gemas, ficha, pase)."""
import io

BASE = r"C:\Users\carlo\Documents\Android\ParDos-Puzzle-Game\shared\src\commonMain\kotlin\com\korkoor\pardos\domain\meta"


def edit(name, pairs):
    path = BASE + "\\" + name
    t = io.open(path, encoding="utf-8").read()
    for old, new in pairs:
        if old not in t:
            raise SystemExit("no encontrado en " + name + ": " + old[:70])
        t = t.replace(old, new, 1)
    io.open(path, "w", encoding="utf-8").write(t)


edit("MetaRetention.kt", [
    ("""    fun repairStreak(): Boolean {
        val lost = pendingRepair()
        if (lost <= 0) return false
        if (!wallet.spendGems(repairCost())) return false
        val fixed""", """    fun repairStreak(): Boolean = repairStreakInternal(withGems = true)

    /** Recupera la racha viendo un anuncio (sin gemas). */
    fun repairStreakWithAd(): Boolean = repairStreakInternal(withGems = false)

    private fun repairStreakInternal(withGems: Boolean): Boolean {
        val lost = pendingRepair()
        if (lost <= 0) return false
        if (withGems && !wallet.spendGems(repairCost())) return false
        val fixed"""),
    ("""    fun skipFreeChestWithGems(): Boolean {""", """    /** Salta la espera del cofre gratis viendo un anuncio. */
    fun skipFreeChestWithAd(): Boolean {
        if (freeChestRemainingMs() <= 0L) return false
        s.setLong("free_chest_last", 0L)
        return true
    }

    fun skipFreeChestWithGems(): Boolean {"""),
    ("""    private fun applyWheelPrize(slice: WheelSlice) {""", """    /** Giro extra de la ruleta a cambio de un anuncio (cuando ya no quedan giros gratis). */
    fun spinWheelWithAd(random: Random): Int? {
        if (wheelAllowance().adLeft <= 0) return null
        s.setInt("wheel_free", wheelUsed("wheel_free"))
        s.setInt("wheel_ad", wheelUsed("wheel_ad") + 1)
        s.setInt("wheel_day", today)
        val index = DailyWheel.pick(random)
        applyWheelPrize(DailyWheel.slices[index])
        return index
    }

    private fun applyWheelPrize(slice: WheelSlice) {"""),
])

io.open(BASE + r"\MetaAds.kt", "w", encoding="utf-8").write('''package com.korkoor.pardos.domain.meta

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
''')

edit("MetaSession.kt", [
    ("    private val happy = HappyHourOps(store, clock)\n", "    private val happy = HappyHourOps(store, clock)\n    private val ads = AdOps(store, wallet, col, ret, clock)\n"),
    ("""        val r = ret.claimDailyReward() ?: return no("Ya lo reclamaste hoy")
        return ok(""", """        val r = ret.claimDailyReward() ?: return no("Ya lo reclamaste hoy")
        ads.rememberGift(r.coins, r.gems)
        return ok("""),
    ("""    /** Cobra la casilla de hoy del calendario del mes. */""", """    // ---- premios por anuncio (la app muestra el anuncio y después llama aquí; VIP llama directo) ----

    fun adFreeGems(): String = if (ads.freeGems()) ok("gems" to com.korkoor.pardos.domain.shop.AdRewards.FREE_GEMS) else no("Hoy ya no quedan gemas gratis")
    fun adToken(): String = if (ads.token()) ok() else no("Hoy ya no quedan fichas gratis")
    fun adSeasonBoost(): String = if (ads.seasonBoost()) ok("points" to com.korkoor.pardos.domain.shop.AdRewards.SEASON_BOOST_POINTS) else no("Hoy ya no quedan impulsos")
    fun adSkipFreeChest(): String = if (ret.skipFreeChestWithAd()) ok() else no("El cofre ya está listo")
    fun adRepairStreak(): String = if (ret.repairStreakWithAd()) ok() else no("No hay racha que recuperar")
    fun adDoubleGift(): String {
        val r = ads.doubleGift() ?: return no("Hoy ya duplicaste el regalo")
        return ok("coins" to r.first, "gems" to r.second)
    }
    fun adWheelSpin(seed: Long): String {
        val i = ret.spinWheelWithAd(Random(seed)) ?: return no("No quedan giros")
        return ok("index" to i)
    }

    /** Cobra la casilla de hoy del calendario del mes. */"""),
    ("""            "happyHour" to happy.json(), "calendar" to calendar.json(),""", """            "happyHour" to happy.json(), "calendar" to calendar.json(),
            "ads" to Raw(obj(
                "gems" to ads.gemsLeft(), "gemsAmount" to com.korkoor.pardos.domain.shop.AdRewards.FREE_GEMS, "token" to ads.tokensLeft(),
                "season" to ads.seasonLeft(), "seasonPoints" to com.korkoor.pardos.domain.shop.AdRewards.SEASON_BOOST_POINTS,
                "wheel" to ret.wheelAllowance().adLeft, "doubleGift" to ads.canDoubleGift()
            )),"""),
])
print("ok")
