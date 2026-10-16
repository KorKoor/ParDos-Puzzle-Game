package com.korkoor.pardos.data.local

import android.content.Context
import com.korkoor.pardos.domain.level.Bestiary
import com.korkoor.pardos.domain.level.WeeklyBoss
import com.korkoor.pardos.domain.model.GameMode
import com.korkoor.pardos.domain.shop.Banners

/** Jefe de la semana: elige el jefe (entre los ya vencidos) y entrega la recompensa la primera vez que se vence esa semana. */
class WeeklyBossStore(context: Context) {
    private val appContext = context.applicationContext
    private val storage = appContext.getSharedPreferences("pardos_storage", Context.MODE_PRIVATE)
    private val prefs = appContext.getSharedPreferences("pardos_weekly_boss", Context.MODE_PRIVATE)

    private val week: Long get() = WeeklyBoss.weekOf(LocalDay.today().toLong())

    /** Nivel del jefe de esta semana, o null si todavía no venció ningún jefe. */
    fun currentLevel(): Int? {
        val progress = LevelProgressStore(storage)
        val beaten = Bestiary.bosses.flatMap { it.levels }.filter { progress.starsFor(GameMode.CLASICO, it) > 0 }
        return WeeklyBoss.levelFor(week, beaten)
    }

    fun isClaimed(): Boolean = prefs.getBoolean("claimed_$week", false)

    /** Se llama al ganar un nivel de campaña. Devuelve un texto con lo recibido si era el jefe de la semana y aún no estaba cobrado. */
    fun onLevelWon(level: Int): String? {
        if (isClaimed() || currentLevel() != level) return null
        prefs.edit().putBoolean("claimed_$week", true).apply()
        val economy = EconomyManager(appContext)
        val banner = WeeklyBoss.bannerFor(week)
        val had = banner in economy.ownedBanners.value
        economy.grantBanner(banner)
        economy.addCoins(WeeklyBoss.REWARD_COINS)
        val name = runCatching { Banners.byId(banner).name }.getOrDefault("banner")
        return if (had) "Jefe de la semana vencido: +${WeeklyBoss.REWARD_COINS} monedas" else "Jefe de la semana vencido: +${WeeklyBoss.REWARD_COINS} monedas y el banner \"$name\""
    }
}
