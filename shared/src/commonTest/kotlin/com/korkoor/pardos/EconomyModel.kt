package com.korkoor.pardos

import com.korkoor.pardos.domain.economy.Economy
import com.korkoor.pardos.domain.model.DailyMissionPlan
import com.korkoor.pardos.domain.model.MissionPool
import com.korkoor.pardos.domain.retention.DailyWheel
import com.korkoor.pardos.domain.retention.PlayerLevelRewards
import com.korkoor.pardos.domain.retention.SeasonPass
import com.korkoor.pardos.domain.retention.WeeklyMissions
import com.korkoor.pardos.domain.retention.WheelKind
import com.korkoor.pardos.domain.rewards.ChapterRewards
import com.korkoor.pardos.domain.rewards.CoinRewards
import com.korkoor.pardos.domain.rewards.DailyRewards
import com.korkoor.pardos.domain.shop.AdRewards

/**
 * Modelo de la economía: cuánto entra en un día "típico" y, con eso, cuántos días tarda cada compra.
 * Todo sale de las constantes reales del juego (nada está copiado a mano), así que si alguien cambia un premio o un precio,
 * las pruebas de [EconomyBalanceTest] dicen si el juego sigue sintiéndose igual de caro (o de barato).
 *
 * Supuestos (documentados en docs/ECONOMIA.md):
 *  - Casual: ~8 victorias de campaña al día (2 estrellas de media, 3 de cada 4 son primera vez), racha y "flow" suman un 15 %,
 *    cobra el regalo diario, las 3 misiones (7 de cada 10 días hace las tres), 1 giro de ruleta, las semanales y los premios de nivel.
 *  - Con anuncios: además ve los 3 anuncios de gemas, gira 2 veces más la ruleta y duplica las monedas de 4 victorias.
 */
object EconomyModel {
    data class Daily(val coins: Double, val gems: Double)

    const val WINS_PER_DAY = 8
    private const val BONUS_FLOW_AND_STREAK = 1.15
    private const val FIRST_CLEAR_SHARE = 0.75
    private const val PERFECT_MISSION_DAYS = 0.7
    private const val LEVEL_UPS_PER_DAY = 0.35
    private const val CHAPTERS_PER_DAY = WINS_PER_DAY * FIRST_CLEAR_SHARE / ChapterRewards.LEVELS_PER_CHAPTER

    fun coinsPerWin(): Double =
        (FIRST_CLEAR_SHARE * CoinRewards.forLevelWin(2, true) + (1 - FIRST_CLEAR_SHARE) * CoinRewards.forLevelWin(2, false)) * BONUS_FLOW_AND_STREAK

    private fun missionCoinsPerDay(): Double =
        DailyMissionPlan.Tier.entries.sumOf { tier ->
            MissionPool.allMissions.filter { DailyMissionPlan.tierOf(it) == tier }.map { CoinRewards.forMission(it.xpReward) }.average()
        }

    private fun wheelSpin(): Pair<Double, Double> {
        val total = DailyWheel.slices.sumOf { it.weight }.toDouble()
        val coins = DailyWheel.slices.filter { it.kind == WheelKind.COINS }.sumOf { it.amount * it.weight } / total
        val gems = DailyWheel.slices.filter { it.kind == WheelKind.GEMS }.sumOf { it.amount * it.weight } / total
        return coins to gems
    }

    private fun weeklyCoinsPerDay(): Double =
        WeeklyMissions.pool.map { it.coins }.average() * WeeklyMissions.PER_WEEK / 7.0

    private fun levelUpCoins(): Double = (5..25).map { PlayerLevelRewards.forLevel(it).coins }.average()
    private fun levelUpGems(): Double = (5..25).map { PlayerLevelRewards.forLevel(it).gems }.average()

    fun casual(): Daily {
        val (wheelCoins, wheelGems) = wheelSpin()
        val free = SeasonPass.totalFree()
        val coins = WINS_PER_DAY * coinsPerWin() +
            Economy.FIRST_WIN_BONUS_COINS +
            DailyRewards.cycle.map { it.coins }.average() +
            missionCoinsPerDay() +
            wheelCoins +
            weeklyCoinsPerDay() +
            LEVEL_UPS_PER_DAY * levelUpCoins() +
            CHAPTERS_PER_DAY * ChapterRewards.forChapter(3).coins +
            free.coins / SeasonPass.TIERS.toDouble()
        val gems = DailyRewards.cycle.map { it.gems }.average() +
            PERFECT_MISSION_DAYS * Economy.DAILY_MISSIONS_BONUS_GEMS +
            wheelGems +
            PERFECT_MISSION_DAYS * WeeklyMissions.COMPLETION_GEMS / 7.0 +
            free.gems / SeasonPass.TIERS.toDouble() +
            CHAPTERS_PER_DAY * ChapterRewards.forChapter(3).gems +
            LEVEL_UPS_PER_DAY * levelUpGems()
        return Daily(coins, gems)
    }

    fun withAds(): Daily {
        val base = casual()
        val (wheelCoins, wheelGems) = wheelSpin()
        val extraSpins = Economy.WHEEL_AD_SPINS
        val doubled = 4 * Economy.doubleBonus(coinsPerWin().toInt())
        return Daily(
            base.coins + extraSpins * wheelCoins + doubled,
            base.gems + extraSpins * wheelGems + AdRewards.FREE_GEMS * AdRewards.FREE_GEMS_PER_DAY
        )
    }
}
