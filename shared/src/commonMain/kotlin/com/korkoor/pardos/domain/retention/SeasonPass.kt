package com.korkoor.pardos.domain.retention

import com.korkoor.pardos.domain.collection.ChestType
import com.korkoor.pardos.domain.economy.Economy
import com.korkoor.pardos.domain.shop.TileSkin

data class CivilDate(val year: Int, val month: Int, val day: Int)

/** Calendario civil puro (algoritmos de Howard Hinnant): sirve en Android e iOS sin java.time. */
object Civil {
    fun fromEpochDay(epochDay: Int): CivilDate {
        val z = epochDay + 719_468
        val era = (if (z >= 0) z else z - 146_096) / 146_097
        val doe = z - era * 146_097
        val yoe = (doe - doe / 1_460 + doe / 36_524 - doe / 146_096) / 365
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = if (mp < 10) mp + 3 else mp - 9
        val y = yoe + era * 400
        return CivilDate(if (m <= 2) y + 1 else y, m, d)
    }

    fun toEpochDay(year: Int, month: Int, day: Int): Int {
        val y = if (month <= 2) year - 1 else year
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val doy = (153 * (if (month > 2) month - 3 else month + 9) + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146_097 + doe - 719_468
    }
}

/** Las temporadas duran un mes natural. El id es único y creciente (año*12 + mes). */
object SeasonCalendar {
    private val monthNames = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )

    fun seasonId(epochDay: Int): Int {
        val c = Civil.fromEpochDay(epochDay)
        return c.year * 12 + (c.month - 1)
    }

    fun startDay(seasonId: Int): Int = Civil.toEpochDay(seasonId / 12, seasonId % 12 + 1, 1)

    /** Primer día de la temporada siguiente (exclusivo). */
    fun endDay(seasonId: Int): Int = startDay(seasonId + 1)

    /** Días que quedan, contando hoy. */
    fun daysLeft(epochDay: Int): Int = endDay(seasonId(epochDay)) - epochDay

    fun name(seasonId: Int): String = monthNames[seasonId % 12]

    /** La skin exclusiva rota cada mes entre las de temporada. */
    fun skinFor(seasonId: Int): TileSkin = TileSkin.seasonal[seasonId.mod(TileSkin.seasonal.size)]
}

data class SeasonReward(
    val coins: Int = 0,
    val gems: Int = 0,
    val chest: ChestType? = null,
    val freezes: Int = 0,
    val undos: Int = 0,
    val skin: TileSkin? = null,
    /** Id de un avatar exclusivo (0 = ninguno). */
    val avatar: Int = 0,
    /** Id de un banner de perfil exclusivo (0 = ninguno). */
    val banner: Int = 0,
    /** Efecto de fusión exclusivo (null = ninguno). */
    val fx: com.korkoor.pardos.domain.shop.MergeFx? = null
) {
    val isEmpty: Boolean get() = coins == 0 && gems == 0 && chest == null && freezes == 0 && undos == 0 && skin == null && avatar == 0 && banner == 0 && fx == null

    /** Valor aproximado en monedas (para balancear). */
    val coinValue: Int
        get() = coins + gems * Economy.GEM_COINS_VALUE +
            when (chest) { ChestType.EPIC -> Economy.EPIC_CHEST_GEMS * Economy.GEM_COINS_VALUE; ChestType.RARE -> Economy.RARE_CHEST_COINS; ChestType.COMMON -> Economy.COMMON_CHEST_COINS; null -> 0 } +
            freezes * Economy.STREAK_FREEZE_PRICE_COINS + undos * Economy.UNDO_PRICE_COINS
}

/** Puntos de temporada que da cada acción. Se calibran para que una temporada dure ~3-4 semanas jugando a diario. */
object SeasonPoints {
    const val GAME_PLAYED = 4
    const val GAME_WON = 10
    const val DAILY_MISSION = 25
    const val ALL_DAILY_MISSIONS = 20
    const val DAILY_CHALLENGE = 30
    const val DAILY_LOGIN = 15
    const val WHEEL_SPIN = 5
    const val FREE_CHEST = 5
    const val WEEKLY_MISSION = 60
    /** Tope diario de puntos por jugar/ganar, para que no sea un pase de "grindeo" infinito. */
    const val GAME_DAILY_CAP = 120
}

object SeasonPass {
    const val TIERS = Economy.SEASON_TIERS
    const val POINTS_PER_TIER = Economy.SEASON_POINTS_PER_TIER
    const val TOTAL_POINTS = TIERS * POINTS_PER_TIER

    /** Niveles completados con [points] puntos (0..30). */
    fun tierFor(points: Int): Int = (points / POINTS_PER_TIER).coerceIn(0, TIERS)

    /** Puntos acumulados dentro del nivel actual (0..99); 0 si ya se llegó al final. */
    fun pointsInTier(points: Int): Int = if (points >= TOTAL_POINTS) 0 else points % POINTS_PER_TIER

    /** Premio gratis del nivel [tier] (1..30). */
    /** Niveles donde cae un avatar exclusivo: uno en la vía gratis y otro en la premium. */
    const val FREE_AVATAR_TIER = 15
    const val PREMIUM_AVATAR_TIER = 12
    /** Niveles donde cae un banner de perfil exclusivo (gratis / premium). */
    const val FREE_BANNER_TIER = 9
    const val PREMIUM_BANNER_TIER = 22
    /** Niveles donde cae un efecto de fusión: uno de muestra en la vía gratis y el exclusivo (Rayo) en la premium. */
    const val FREE_FX_TIER = 25
    const val PREMIUM_FX_TIER = 26
    val FREE_FX = com.korkoor.pardos.domain.shop.MergeFx.SPARKS
    val PREMIUM_FX = com.korkoor.pardos.domain.shop.MergeFx.LIGHTNING

    fun freeReward(tier: Int, seasonId: Int = 0): SeasonReward {
        require(tier in 1..TIERS)
        val chest = when {
            tier == TIERS -> ChestType.RARE
            tier % 5 == 0 -> ChestType.COMMON
            else -> null
        }
        return SeasonReward(
            coins = 20 + 3 * tier,
            gems = if (tier == TIERS) 10 else if (tier % 5 == 0) 3 else 0,
            chest = chest,
            freezes = if (tier == 10 || tier == 20) 1 else 0,
            undos = if (tier == 7 || tier == 17 || tier == 27) 2 else 0,
            avatar = if (tier == FREE_AVATAR_TIER) com.korkoor.pardos.domain.shop.Avatars.seasonFree(seasonId).id else 0,
            banner = if (tier == FREE_BANNER_TIER) com.korkoor.pardos.domain.shop.Banners.seasonFree(seasonId).id else 0,
            fx = if (tier == FREE_FX_TIER) FREE_FX else null
        )
    }

    /** Premio de la vía premium del nivel [tier] (1..30). El último trae la skin exclusiva de la temporada. */
    fun premiumReward(tier: Int, seasonId: Int): SeasonReward {
        require(tier in 1..TIERS)
        val chest = when {
            tier == TIERS || tier == 20 -> ChestType.EPIC
            tier % 5 == 0 -> ChestType.RARE
            else -> null
        }
        return SeasonReward(
            coins = 40 + 6 * tier,
            gems = (if (tier % 3 == 0) 4 else 0) + (if (tier % 5 == 0) 5 else 0),
            chest = chest,
            freezes = if (tier == 8 || tier == 18 || tier == 28) 1 else 0,
            undos = if (tier == 4 || tier == 14 || tier == 24) 3 else 0,
            skin = if (tier == TIERS) SeasonCalendar.skinFor(seasonId) else null,
            avatar = if (tier == PREMIUM_AVATAR_TIER) com.korkoor.pardos.domain.shop.Avatars.seasonPremium(seasonId).id else 0,
            banner = if (tier == PREMIUM_BANNER_TIER) com.korkoor.pardos.domain.shop.Banners.seasonPremium(seasonId).id else 0,
            fx = if (tier == PREMIUM_FX_TIER) PREMIUM_FX else null
        )
    }

    fun totalFree(seasonId: Int = 0): SeasonReward = sum((1..TIERS).map { freeReward(it, seasonId) })
    fun totalPremium(seasonId: Int): SeasonReward = sum((1..TIERS).map { premiumReward(it, seasonId) })

    private fun sum(list: List<SeasonReward>) = SeasonReward(
        coins = list.sumOf { it.coins }, gems = list.sumOf { it.gems },
        freezes = list.sumOf { it.freezes }, undos = list.sumOf { it.undos }
    )

    /** Gemas por saltar al siguiente nivel: proporcional a lo que falta (mínimo 10, máximo [Economy.SEASON_TIER_SKIP_GEMS]). */
    fun skipCostGems(currentPoints: Int): Int {
        if (tierFor(currentPoints) >= TIERS) return 0
        val remaining = POINTS_PER_TIER - pointsInTier(currentPoints)
        val cost = (Economy.SEASON_TIER_SKIP_GEMS * remaining + POINTS_PER_TIER - 1) / POINTS_PER_TIER
        return cost.coerceIn(10, Economy.SEASON_TIER_SKIP_GEMS)
    }
}
