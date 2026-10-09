package com.korkoor.pardos.data.local

import android.content.Context
import android.content.SharedPreferences
import com.korkoor.pardos.domain.shop.AdPolicy

/**
 * Lleva la cuenta de los anuncios para no cansar al jugador: cuándo fue el último, cuántas victorias van desde entonces y los
 * topes diarios de lo que se gana viendo anuncios (gemas gratis, carta extra, ficha de intercambio...). Todo vive en el teléfono.
 */
class AdFrequency(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences("pardos_ads", Context.MODE_PRIVATE)

    init {
        // Primer arranque con esta versión: a partir de aquí se cuenta el tiempo "de bienvenida" sin anuncios de pantalla completa
        if (!prefs.contains(K_FIRST_SEEN)) prefs.edit().putLong(K_FIRST_SEEN, System.currentTimeMillis()).apply()
    }

    /** Una victoria más desde el último anuncio de pantalla completa. */
    fun onCampaignWin() { prefs.edit().putInt(K_WINS, prefs.getInt(K_WINS, 0) + 1).apply() }

    fun recordInterstitial(now: Long = System.currentTimeMillis(), day: Int = LocalDay.today()) {
        prefs.edit()
            .putLong(K_LAST_AD, now)
            .putInt(K_WINS, 0)
            .putInt(K_SHOWN_DAY, day)
            .putInt(K_SHOWN_COUNT, shownToday(day) + 1)
            .apply()
    }

    fun recordRewarded(now: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(K_LAST_AD, now).putLong(K_LAST_REWARDED, now).apply()
    }

    fun shownToday(day: Int = LocalDay.today()): Int = if (prefs.getInt(K_SHOWN_DAY, -1) == day) prefs.getInt(K_SHOWN_COUNT, 0) else 0

    fun state(vip: Boolean, campaignLevel: Int, afterWin: Boolean, bigMoment: Boolean = false, now: Long = System.currentTimeMillis()): AdPolicy.State {
        val first = prefs.getLong(K_FIRST_SEEN, now)
        // Si nunca se vio un anuncio, "hace mucho": que nada lo retenga salvo las demás reglas
        val last = prefs.getLong(K_LAST_AD, 0L)
        val lastRewarded = prefs.getLong(K_LAST_REWARDED, 0L)
        return AdPolicy.State(
            vip = vip,
            campaignLevel = campaignLevel,
            winsSinceLastAd = prefs.getInt(K_WINS, 0),
            msSinceLastAd = if (last == 0L) Long.MAX_VALUE / 2 else (now - last).coerceAtLeast(0),
            msSinceLastRewarded = if (lastRewarded == 0L) Long.MAX_VALUE / 2 else (now - lastRewarded).coerceAtLeast(0),
            shownToday = shownToday(),
            installAgeMs = (now - first).coerceAtLeast(0),
            afterWin = afterWin,
            bigMoment = bigMoment
        )
    }

    /** Cuenta una visita al menú y devuelve su número de hoy (0, 1, 2…): la tarjeta de ofertas rota con él. */
    fun nextMenuVisit(day: Int = LocalDay.today()): Int {
        val n = if (prefs.getInt(K_VISIT_DAY, -1) == day) prefs.getInt(K_VISIT_COUNT, 0) else 0
        prefs.edit().putInt(K_VISIT_DAY, day).putInt(K_VISIT_COUNT, n + 1).apply()
        return n
    }

    // ---------------- Topes diarios de lo que se gana viendo anuncios ----------------

    fun usedToday(slot: String, day: Int = LocalDay.today()): Int =
        if (prefs.getInt("$K_SLOT_DAY$slot", -1) == day) prefs.getInt("$K_SLOT_USED$slot", 0) else 0

    fun left(slot: String, cap: Int, day: Int = LocalDay.today()): Int = (cap - usedToday(slot, day)).coerceAtLeast(0)

    /** Cuenta un uso (se llama cuando el premio ya se entregó). */
    fun consume(slot: String, day: Int = LocalDay.today()) {
        prefs.edit().putInt("$K_SLOT_DAY$slot", day).putInt("$K_SLOT_USED$slot", usedToday(slot, day) + 1).apply()
    }

    companion object {
        private const val K_FIRST_SEEN = "first_seen"
        private const val K_LAST_AD = "last_ad"
        private const val K_LAST_REWARDED = "last_rewarded"
        private const val K_WINS = "wins_since_ad"
        private const val K_SHOWN_DAY = "shown_day"
        private const val K_SHOWN_COUNT = "shown_count"
        private const val K_VISIT_DAY = "visit_day"
        private const val K_VISIT_COUNT = "visit_count"
        private const val K_SLOT_DAY = "slot_day_"
        private const val K_SLOT_USED = "slot_used_"

        const val SLOT_FREE_GEMS = "free_gems"
        const val SLOT_EXTRA_CARD = "extra_card"
        const val SLOT_TOKEN = "token"
        const val SLOT_SEASON = "season_boost"
    }
}
