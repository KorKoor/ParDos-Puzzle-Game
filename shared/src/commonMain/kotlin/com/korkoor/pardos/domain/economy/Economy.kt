package com.korkoor.pardos.domain.economy

import com.korkoor.pardos.domain.collection.ChestType
import com.korkoor.pardos.domain.collection.Rarity

/**
 * Reglas de la economía. Dos monedas y una esencia:
 *  - Monedas: se ganan jugando; para skins comunes/raras, cofres comunes y consumibles.
 *  - Gemas: premium; se ganan poco jugando (cofres de capítulo, hitos, logros) y se compran.
 *  - Esencia: se obtiene de piezas repetidas; sirve para CREAR piezas que te faltan.
 * Todos los números viven aquí para poder balancearlos con pruebas.
 */
object Economy {

    // ---------------- Precios de cofres ----------------
    const val COMMON_CHEST_COINS = 400
    const val RARE_CHEST_COINS = 1_200
    const val RARE_CHEST_GEMS = 30
    const val EPIC_CHEST_GEMS = 80

    // ---------------- Consumibles ----------------
    const val UNDO_PRICE_COINS = 60
    const val EXTRA_TIME_PRICE_COINS = 90
    const val STREAK_FREEZE_PRICE_COINS = 350
    const val MAX_STREAK_FREEZES = 3
    /** Segundos que regala "Tiempo extra" en modos con reloj. */
    const val EXTRA_TIME_SECONDS = 20

    // ---------------- Valores de referencia ----------------
    /** Una gema equivale a ~0.02 USD; sirve para poner precios coherentes. */
    const val GEM_COINS_VALUE = 40

    // ---------------- Logros ----------------
    data class AchievementReward(val coins: Int, val gems: Int)

    fun achievementReward(rarity: Rarity): AchievementReward = when (rarity) {
        Rarity.COMMON -> AchievementReward(20, 0)
        Rarity.RARE -> AchievementReward(50, 0)
        Rarity.EPIC -> AchievementReward(100, 1)
        Rarity.LEGENDARY -> AchievementReward(200, 5)
    }

    /** Rareza de un logro según su posición dentro de su categoría (las listas van de fácil a difícil). */
    fun achievementRarity(indexInCategory: Int, categorySize: Int): Rarity {
        if (categorySize <= 1) return Rarity.COMMON
        val p = indexInCategory.toDouble() / (categorySize - 1)
        return when {
            p < 0.40 -> Rarity.COMMON
            p < 0.70 -> Rarity.RARE
            p < 0.90 -> Rarity.EPIC
            else -> Rarity.LEGENDARY
        }
    }

    // ---------------- Hitos de racha ----------------
    data class StreakMilestone(val days: Int, val coins: Int, val gems: Int, val chest: ChestType?)

    val streakMilestones: List<StreakMilestone> = listOf(
        StreakMilestone(3, 100, 0, null),
        StreakMilestone(7, 200, 3, ChestType.COMMON),
        StreakMilestone(14, 400, 6, ChestType.RARE),
        StreakMilestone(30, 800, 15, ChestType.RARE),
        StreakMilestone(60, 1_500, 30, ChestType.EPIC),
        StreakMilestone(100, 3_000, 60, ChestType.EPIC)
    )

    /** Hito alcanzado exactamente hoy (o null). Se entrega una sola vez por hito. */
    fun milestoneFor(streak: Int): StreakMilestone? = streakMilestones.firstOrNull { it.days == streak }

    // ---------------- Duplicar monedas con anuncio ----------------
    /** Máximo de monedas que se pueden duplicar con un anuncio (evita abusos con premios enormes). */
    const val DOUBLE_COINS_CAP = 300

    fun doubleBonus(coinsEarned: Int): Int = coinsEarned.coerceIn(0, DOUBLE_COINS_CAP)

    // ---------------- Oferta diaria ----------------
    /** Descuentos posibles de la oferta del día. */
    val dailyOfferDiscounts = listOf(25, 30, 40)

    // ---------------- Pack inicial (compra real, una vez) ----------------
    const val STARTER_GEMS = 300
    const val STARTER_RARE_CHESTS = 3

    // ---------------- Retención ----------------
    /** Espera entre cofres gratis. */
    const val FREE_CHEST_COOLDOWN_MS: Long = 4L * 60 * 60 * 1000
    /** Cada cuántos cofres gratis uno es raro. */
    const val FREE_CHEST_RARE_EVERY = 5
    const val FREE_CHEST_SKIP_MAX_GEMS = 8
    /** Esperas que se pueden saltar viendo anuncios por día. */
    const val FREE_CHEST_AD_SKIPS_PER_DAY = 3

    const val WHEEL_FREE_SPINS = 1
    const val WHEEL_AD_SPINS = 2

    /** Bonus por la primera victoria de cada día. */
    const val FIRST_WIN_BONUS_COINS = 75
    /** Días sin entrar a partir de los cuales hay regalo de regreso. */
    const val COMEBACK_MIN_DAYS = 3

    /** Misiones diarias: premio extra por cobrar las tres. */
    const val DAILY_MISSIONS_BONUS_GEMS = 2

    // Pase de temporada
    const val SEASON_TIERS = 30
    const val SEASON_POINTS_PER_TIER = 100
    /** Gemas por saltar un nivel del pase. */
    const val SEASON_TIER_SKIP_GEMS = 40

    // Duelo a distancia (por código)
    const val REMOTE_DUEL_PLAY_COINS = 25
    const val REMOTE_DUEL_WIN_COINS = 60
    const val REMOTE_DUEL_WIN_GEMS = 2
    const val REMOTE_DUEL_CREATE_COINS = 15
    /** Retos lanzados al día que dan premio (evita crear retos solo para cobrar). */
    const val REMOTE_DUEL_CREATE_PER_DAY = 3

    // Avatares (se compran con monedas; los de temporada solo salen en el pase)
    const val AVATAR_PRICE_BASIC = 600
    const val AVATAR_PRICE_COZY = 900
    const val AVATAR_PRICE_NICE = 1_100
    const val AVATAR_PRICE_RARE = 1_500
    const val AVATAR_PRICE_EPIC = 2_500
    const val AVATAR_PRICE_LEGENDARY = 3_500

    // Banners de perfil
    const val BANNER_PRICE_BASIC = 500
    const val BANNER_PRICE_COZY = 700
    const val BANNER_PRICE_NICE = 900
    const val BANNER_PRICE_RARE = 1_100
    const val BANNER_PRICE_PRIME = 1_300
    const val BANNER_GEMS_NICE = 60
    const val BANNER_GEMS_RARE = 75
    const val BANNER_GEMS_EPIC = 90

    // Hucha
    const val PIGGY_CAP = 300
    const val PIGGY_MIN_TO_BREAK = 40
}
