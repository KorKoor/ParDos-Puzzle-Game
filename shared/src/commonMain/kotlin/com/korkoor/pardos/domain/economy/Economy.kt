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
}
