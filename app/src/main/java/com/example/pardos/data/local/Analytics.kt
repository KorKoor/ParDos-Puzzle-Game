package com.korkoor.pardos.data.local

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Analítica de retención (Firebase Analytics). Solo eventos de uso sin datos personales: sirven para ver en qué parte del juego se van
 * los jugadores (tutorial, niveles, misiones, anuncios con premio, regreso). Si falta google-services.json o Firebase falla, no pasa nada.
 *
 * Eventos: tutorial_complete, level_start, level_end, mission_claim, ad_reward, ad_unavailable, return_after_days.
 */
object Analytics {
    private const val TAG = "Analytics"
    private var client: FirebaseAnalytics? = null

    /** Se llama una vez al abrir la app. */
    fun init(context: Context) {
        if (client != null) return
        client = try {
            FirebaseAnalytics.getInstance(context.applicationContext)
        } catch (t: Throwable) {
            Log.w(TAG, "Analítica no disponible: ${t.message}")
            null
        }
    }

    private fun log(name: String, vararg params: Pair<String, Any>) {
        val c = client ?: return
        try {
            val b = Bundle()
            for ((k, v) in params) when (v) {
                is Int -> b.putLong(k, v.toLong())
                is Long -> b.putLong(k, v)
                is Boolean -> b.putLong(k, if (v) 1L else 0L)
                else -> b.putString(k, v.toString().take(100))
            }
            c.logEvent(name, b)
        } catch (t: Throwable) {
            Log.w(TAG, "No se pudo registrar $name: ${t.message}")
        }
    }

    fun tutorialComplete() = log("tutorial_complete")

    fun levelStart(mode: String, level: Int) = log("level_start", "mode" to mode, "level" to level)

    fun levelEnd(mode: String, level: Int, won: Boolean, moves: Int) =
        log("level_end", "mode" to mode, "level" to level, "won" to won, "moves" to moves)

    fun missionClaim(missionId: Int) = log("mission_claim", "mission_id" to missionId)

    /** [placement] dice desde qué botón se vio el anuncio (tienda, deshacer, ruleta…). */
    fun adReward(placement: String) = log("ad_reward", "placement" to placement)

    fun adUnavailable(placement: String) = log("ad_unavailable", "placement" to placement)

    fun welcomeBackStep(goal: String) = log("welcome_back_goal", "goal" to goal)

    fun welcomeBackDone() = log("welcome_back_done")

    /** Al volver tras [daysAway] días sin abrir el juego (se llama solo si fueron 2 o más). */
    fun returnAfterDays(daysAway: Int) {
        val bucket = when {
            daysAway < 4 -> "2-3"
            daysAway < 8 -> "4-7"
            daysAway < 15 -> "8-14"
            else -> "15+"
        }
        log("return_after_days", "days_away" to daysAway, "bucket" to bucket)
    }
}
