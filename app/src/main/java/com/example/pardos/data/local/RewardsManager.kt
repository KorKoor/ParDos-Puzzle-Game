package com.korkoor.pardos.data.local

import android.content.Context
import com.korkoor.pardos.domain.achievements.gameAchievements
import com.korkoor.pardos.domain.collection.ChestType
import com.korkoor.pardos.domain.collection.Rarity
import com.korkoor.pardos.domain.economy.Economy

/**
 * Recompensas por hitos: logros, racha y "duplicar con anuncio".
 * Cada premio se entrega UNA sola vez (se recuerda en preferencias).
 */
class RewardsManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("pardos_rewards", Context.MODE_PRIVATE)
    private val economy = EconomyManager(appContext)
    private val collection = CollectionManager(appContext)

    // ---------------- Logros ----------------

    /** Rareza (y por tanto premio) de cada logro, según su posición dentro de su categoría. */
    private val achievementRarity: Map<String, Rarity> by lazy {
        val byCategory = gameAchievements.all.groupBy { categoryOf(it.id) }
        buildMap {
            byCategory.values.forEach { list ->
                list.forEachIndexed { i, a -> put(a.id, Economy.achievementRarity(i, list.size)) }
            }
        }
    }

    fun rarityOf(achievementId: String): Rarity = achievementRarity[achievementId] ?: Rarity.COMMON

    fun rewardFor(achievementId: String): Economy.AchievementReward =
        Economy.achievementReward(rarityOf(achievementId))

    /** ¿Ya se pagó el premio de este logro? */
    fun isAchievementPaid(id: String): Boolean = prefs.getBoolean("ach_paid_$id", false)

    /**
     * Cobra el premio de un logro recién desbloqueado. Devuelve el premio entregado o null si ya estaba pagado.
     * Los logros épicos y legendarios regalan además un cofre.
     */
    fun payAchievement(id: String): Economy.AchievementReward? {
        if (isAchievementPaid(id)) return null
        prefs.edit().putBoolean("ach_paid_$id", true).apply()
        val r = rewardFor(id)
        economy.addCoins(r.coins)
        economy.addGems(r.gems)
        when (rarityOf(id)) {
            Rarity.EPIC -> collection.addChests(ChestType.COMMON, 1)
            Rarity.LEGENDARY -> collection.addChests(ChestType.RARE, 1)
            else -> Unit
        }
        return r
    }

    /**
     * Paga los logros que ya estaban desbloqueados antes de existir este sistema, para que nadie
     * pierda lo ganado. Se ejecuta una vez. Devuelve el total entregado.
     */
    fun backfillAchievements(unlockedIds: Set<String>): Pair<Int, Int> {
        if (prefs.getBoolean("ach_backfilled", false)) return 0 to 0
        var coins = 0
        var gems = 0
        unlockedIds.forEach { id ->
            payAchievement(id)?.let { coins += it.coins; gems += it.gems }
        }
        prefs.edit().putBoolean("ach_backfilled", true).apply()
        return coins to gems
    }

    // ---------------- Hitos de racha ----------------

    /** Entrega el hito de racha de hoy si corresponde (3, 7, 14, 30, 60, 100 días). Una sola vez. */
    fun claimStreakMilestone(streak: Int): Economy.StreakMilestone? {
        val m = Economy.milestoneFor(streak) ?: return null
        val key = "streak_milestone_${m.days}"
        if (prefs.getBoolean(key, false)) return null
        prefs.edit().putBoolean(key, true).apply()
        economy.addCoins(m.coins)
        economy.addGems(m.gems)
        m.chest?.let { collection.addChests(it, 1) }
        return m
    }

    // ---------------- Cofre de capítulo → cofre de colección ----------------

    /** Las recompensas de capítulo ahora también regalan un cofre de la colección. */
    fun grantChapterChest(chapter: Int) {
        collection.addChests(if (chapter >= 3) ChestType.RARE else ChestType.COMMON, 1)
    }

    // ---------------- Duplicar monedas con anuncio ----------------

    /** Monedas extra al ver el anuncio tras ganar. Se puede cobrar una vez por partida. */
    fun doubleCoins(earned: Int): Int {
        val bonus = Economy.doubleBonus(earned)
        economy.addCoins(bonus)
        return bonus
    }

    private fun categoryOf(id: String): String = when {
        id == "first_win" || id == "getting_warmed_up" || id.startsWith("level_") ||
            id.startsWith("classic_") || id.startsWith("challenge_") || id.startsWith("zen_") -> "progress"
        id.startsWith("tile_") || id in setOf("lucky_seven", "thirteen", "double_double", "quad_squad", "power_of_two", "fibonacci") -> "tiles"
        id == "speedrun" || id.startsWith("speed_") || id in setOf("time_60", "time_45", "time_30", "time_15", "rapid_fire", "efficiency_king") -> "speed"
        id == "century" || id.startsWith("moves_") || id.endsWith("min") -> "endurance"
        id.startsWith("score_") -> "score"
        id.startsWith("combo_") -> "combo"
        else -> "special"
    }
}
