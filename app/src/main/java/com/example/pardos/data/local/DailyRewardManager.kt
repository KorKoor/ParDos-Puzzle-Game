package com.korkoor.pardos.data.local

import android.content.Context
import com.korkoor.pardos.domain.rewards.DailyRewards
import com.korkoor.pardos.domain.rewards.Reward

/** Gestiona el calendario de recompensas diarias (una por día local, según la racha). */
class DailyRewardManager(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("pardos_economy", Context.MODE_PRIVATE)
    private val economy = EconomyManager(context)
    private val profileManager = ProfileManager(context)

    /** Racha actual (mínimo 1 para poder mostrar el calendario). */
    val currentStreak: Int get() = profileManager.getProfile().currentStreak.coerceAtLeast(1)

    /** Día del ciclo (1..7) que toca hoy. */
    val dayInCycle: Int get() = DailyRewards.dayInCycle(currentStreak)

    fun isClaimable(): Boolean = prefs.getInt(KEY_CLAIMED_DAY, 0) != LocalDay.today()

    /** Recompensa que se obtendría hoy, o null si ya se reclamó. */
    fun pending(): Reward? = if (isClaimable()) DailyRewards.rewardForStreak(currentStreak) else null

    /** Entrega la recompensa de hoy una sola vez. */
    fun claim(): Reward? {
        val reward = pending() ?: return null
        prefs.edit().putInt(KEY_CLAIMED_DAY, LocalDay.today()).apply()
        economy.addCoins(reward.coins)
        economy.addGems(reward.gems)
        com.korkoor.pardos.audio.GameAudio.play(com.korkoor.pardos.audio.Sfx.DAILY)
        return reward
    }

    /** Ya se duplicó el regalo de hoy. */
    fun isDoubledToday(): Boolean = prefs.getInt(KEY_DOUBLED_DAY, 0) == LocalDay.today()

    /**
     * Duplica el regalo de hoy: el mismo premio otra vez. Solo después de reclamarlo y una sola vez al día.
     * [reward] es el premio que se acaba de entregar (el que enseñaba el calendario).
     */
    fun claimDouble(reward: Reward): Boolean {
        if (isClaimable() || isDoubledToday()) return false
        prefs.edit().putInt(KEY_DOUBLED_DAY, LocalDay.today()).apply()
        economy.addCoins(reward.coins)
        economy.addGems(reward.gems)
        com.korkoor.pardos.audio.GameAudio.play(com.korkoor.pardos.audio.Sfx.COINS)
        return true
    }

    private companion object {
        const val KEY_CLAIMED_DAY = "daily_reward_claimed_day"
        const val KEY_DOUBLED_DAY = "daily_reward_doubled_day"
    }
}
