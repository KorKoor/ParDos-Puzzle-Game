package com.korkoor.pardos.data.local

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory
import com.korkoor.pardos.domain.shop.RatePolicy

/**
 * Pide una reseña con la ventana nativa de Google Play, solo en un momento feliz (reglas en [RatePolicy]). Se cuenta como "pedida"
 * en cuanto se intenta —aunque Play decida no enseñarla por su propio tope— para no insistir. En depuración la ventana no se muestra.
 */
object RatePrompt {
    private const val TAG = "RatePrompt"
    private const val PREFS = "pardos_rate"
    private const val K_LAST_DAY = "last_ask_day"
    private const val K_COUNT = "ask_count"

    fun maybeAsk(activity: Activity, campaignLevel: Int, winStreak: Int, threeStarWin: Boolean, bossWin: Boolean, adNow: Boolean, vip: Boolean) {
        try {
            val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val today = LocalDay.today()
            val last = prefs.getInt(K_LAST_DAY, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
            val state = RatePolicy.State(
                campaignLevel = campaignLevel,
                installDays = AdFrequency(activity).installAgeDays(),
                daysSinceLastAsk = last?.let { today - it },
                asks = prefs.getInt(K_COUNT, 0),
                winStreak = winStreak,
                threeStarWin = threeStarWin,
                bossWin = bossWin,
                adNow = adNow,
                vip = vip
            )
            if (!RatePolicy.shouldAsk(state)) return
            prefs.edit().putInt(K_LAST_DAY, today).putInt(K_COUNT, state.asks + 1).apply()
            val manager = ReviewManagerFactory.create(activity)
            manager.requestReviewFlow().addOnCompleteListener { task ->
                if (task.isSuccessful && !activity.isFinishing) manager.launchReviewFlow(activity, task.result)
                else Log.d(TAG, "Sin ventana de reseña: ${task.exception?.message}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Reseña: ${e.message}")
        }
    }
}
