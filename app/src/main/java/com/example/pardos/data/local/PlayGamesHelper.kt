package com.korkoor.pardos.data.local

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk

/**
 * Servicios de Play Games: logros y clasificaciones en el perfil de Play Games del jugador. El inicio de sesión es automático (si el jugador
 * lo permitió en Play Games) y todo falla en silencio: sin sesión, sin red o sin identificadores configurados no pasa nada.
 *
 * Los identificadores reales se copian de Play Console (Servicios de Play Games > Logros / Tablas de clasificación) en [ACHIEVEMENTS] y
 * [LEADERBOARDS]. Mientras estén vacíos, [unlock] y [submitScore] no hacen nada. Las claves son los identificadores propios del juego.
 */
object PlayGamesHelper {
    private const val TAG = "PlayGames"

    /** id propio del logro del juego -> id de Play Games (por ejemplo "CgkI..."). */
    private val ACHIEVEMENTS: Map<String, String> = emptyMap()

    /** nombre de la clasificación -> id de Play Games. Claves previstas: "total_score", "best_streak", "weekly_stars". */
    private val LEADERBOARDS: Map<String, String> = emptyMap()

    private var started = false

    fun init(context: Context) {
        if (started) return
        started = true
        try {
            PlayGamesSdk.initialize(context.applicationContext)
        } catch (t: Throwable) {
            Log.w(TAG, "Play Games no disponible: ${t.message}")
        }
    }

    /** Desbloquea un logro. Si ya estaba desbloqueado, Play Games lo ignora. */
    fun unlock(activity: Activity, localId: String) {
        val id = ACHIEVEMENTS[localId] ?: return
        try {
            PlayGames.getAchievementsClient(activity).unlock(id)
        } catch (t: Throwable) {
            Log.w(TAG, "No se pudo desbloquear $localId: ${t.message}")
        }
    }

    fun submitScore(activity: Activity, board: String, score: Long) {
        val id = LEADERBOARDS[board] ?: return
        try {
            PlayGames.getLeaderboardsClient(activity).submitScore(id, score)
        } catch (t: Throwable) {
            Log.w(TAG, "No se pudo enviar el puntaje a $board: ${t.message}")
        }
    }

    /** Abre la lista de logros de Play Games. Devuelve false si no hay logros configurados. */
    fun showAchievements(activity: Activity): Boolean {
        if (ACHIEVEMENTS.isEmpty()) return false
        PlayGames.getAchievementsClient(activity).achievementsIntent
            .addOnSuccessListener { activity.startActivityForResult(it, 9001) }
            .addOnFailureListener { Log.w(TAG, "No se pudieron mostrar los logros: ${it.message}") }
        return true
    }
}
