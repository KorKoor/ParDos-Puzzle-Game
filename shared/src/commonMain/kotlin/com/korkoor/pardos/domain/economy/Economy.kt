package com.korkoor.pardos.domain.economy

import com.korkoor.pardos.domain.collection.ChestType
import com.korkoor.pardos.domain.collection.Rarity

/**
 * Reglas de la economía. Dos monedas y una esencia:
 *  - Monedas: se ganan jugando; para skins comunes/raras, cofres comunes y consumibles.
 *  - Gemas: premium; se ganan poco jugando (cofres de capítulo, hitos, logros) y se compran.
 *  - Esencia: se obtiene de piezas repetidas; sirve para CREAR piezas que te faltan.
 * Todos los números viven aquí para poder balancearlos con pruebas (ver `EconomyBalanceTest` y docs/ECONOMIA.md).
 *
 * Regla de oro (v3.1): las monedas valen la mitad que antes y las gemas dos tercios. Los precios dentro del juego (skins, avatares,
 * banners, cofres, efectos) se duplicaron en monedas y subieron un 50 % en gemas; los premios gratis más generosos se recortaron un poco.
 * Las compras reales (packs de gemas, VIP, pack inicial) NO se tocaron: así los productos de Play Console y App Store siguen igual.
 */
object Economy {

    // ---------------- Precios de cofres ----------------
    const val COMMON_CHEST_COINS = 850
    const val RARE_CHEST_COINS = 2_600
    const val RARE_CHEST_GEMS = 45
    const val EPIC_CHEST_GEMS = 120

    // ---------------- Consumibles ----------------
    const val UNDO_PRICE_COINS = 100
    const val EXTRA_TIME_PRICE_COINS = 150
    const val STREAK_FREEZE_PRICE_COINS = 700
    const val MAX_STREAK_FREEZES = 3
    /** Poderes manuales (Escoba y Unir): en Android piden un anuncio; en iPhone se pagan con monedas. */
    const val MANUAL_POWER_PRICE_COINS = 150
    /** Espera entre usos de Limpiar y Fusión. */
    const val POWER_COOLDOWN_MS: Long = 15L * 60 * 1000
    /** Segunda oportunidad al perder: en Android se logra viendo un anuncio; en iPhone cuesta gemas. */
    const val REVIVE_PRICE_GEMS = 15
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

    // ---------------- Monetización amable ----------------
    /** Primera compra de cada pack de gemas: se multiplican por esto. */
    const val FIRST_PURCHASE_MULTIPLIER = 2
    /** VIP: gemas que se pueden reclamar cada día y % extra de monedas por victoria. */
    const val VIP_DAILY_GEMS = 5
    const val VIP_COIN_BONUS_PERCENT = 20
    /** Impulso de monedas: victorias que dura, % extra y precio en gemas. */
    const val COIN_BOOST_WINS = 5
    const val COIN_BOOST_PERCENT = 50
    const val COIN_BOOST_PRICE_GEMS = 30

    // ---------------- Efectos de fusión (cosméticos) ----------------
    const val MERGE_FX_PRICE_COMMON = 1_400
    const val MERGE_FX_PRICE_RARE = 3_000
    const val MERGE_FX_GEMS_EPIC = 110
    const val MERGE_FX_GEMS_LEGENDARY = 210

    // ---------------- Retención ----------------
    /** Espera entre cofres gratis. */
    const val FREE_CHEST_COOLDOWN_MS: Long = 4L * 60 * 60 * 1000
    /** Cada cuántos cofres gratis uno es raro. */
    const val FREE_CHEST_RARE_EVERY = 5
    const val FREE_CHEST_SKIP_MAX_GEMS = 12
    /** Esperas que se pueden saltar viendo anuncios por día. */
    const val FREE_CHEST_AD_SKIPS_PER_DAY = 3

    const val WHEEL_FREE_SPINS = 1
    const val WHEEL_AD_SPINS = 2

    /** Bonus por la primera victoria de cada día. */
    const val FIRST_WIN_BONUS_COINS = 40
    /** Días sin entrar a partir de los cuales hay regalo de regreso. */
    const val COMEBACK_MIN_DAYS = 3

    /** Misiones diarias: premio extra por cobrar las tres. */
    const val DAILY_MISSIONS_BONUS_GEMS = 1

    // Pase de temporada
    const val SEASON_TIERS = 30
    const val SEASON_POINTS_PER_TIER = 100
    /** Gemas por saltar un nivel del pase. */
    const val SEASON_TIER_SKIP_GEMS = 60

    // Duelo a distancia (por código)
    const val REMOTE_DUEL_PLAY_COINS = 25
    const val REMOTE_DUEL_WIN_COINS = 60
    const val REMOTE_DUEL_WIN_GEMS = 2
    const val REMOTE_DUEL_CREATE_COINS = 15
    /** Retos lanzados al día que dan premio (evita crear retos solo para cobrar). */
    const val REMOTE_DUEL_CREATE_PER_DAY = 3

    // Avatares (se compran con monedas; los de temporada solo salen en el pase)
    const val AVATAR_PRICE_BASIC = 1_200
    const val AVATAR_PRICE_COZY = 1_800
    const val AVATAR_PRICE_NICE = 2_200
    const val AVATAR_PRICE_RARE = 3_000
    const val AVATAR_PRICE_EPIC = 5_000
    const val AVATAR_PRICE_LEGENDARY = 7_000

    // Banners de perfil
    const val BANNER_PRICE_BASIC = 1_000
    const val BANNER_PRICE_COZY = 1_400
    const val BANNER_PRICE_NICE = 1_800
    const val BANNER_PRICE_RARE = 2_200
    const val BANNER_PRICE_PRIME = 2_600
    const val BANNER_GEMS_NICE = 106
    const val BANNER_GEMS_RARE = 110
    const val BANNER_GEMS_EPIC = 135
    const val BANNER_GEMS_LEGENDARY = 165

    // Hucha
    const val PIGGY_CAP = 300
    const val PIGGY_MIN_TO_BREAK = 40
}
