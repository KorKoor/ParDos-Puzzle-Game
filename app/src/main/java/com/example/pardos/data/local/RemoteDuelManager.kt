package com.korkoor.pardos.data.local

import android.content.Context
import android.content.SharedPreferences
import com.korkoor.pardos.domain.logic.RemoteChallenge
import com.korkoor.pardos.domain.logic.RemoteDuel
import com.korkoor.pardos.domain.logic.RemoteDuelRecord
import com.korkoor.pardos.domain.logic.RemoteOutcome

/** Resultado de aceptar un reto: quién ganó, qué se cobró y si es la primera vez que se juega ese código. */
data class RemoteResult(val outcome: RemoteOutcome, val reward: RemoteDuel.Reward, val firstTime: Boolean)

/**
 * Historial, estadísticas y premios de los duelos a distancia. Cada código solo paga (y cuenta en las
 * estadísticas) la primera vez que se juega, para que no se pueda repetir el mismo reto hasta ganar.
 */
class RemoteDuelManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences("pardos_remote_duel", Context.MODE_PRIVATE)
    private val economy = EconomyManager(appContext)

    fun history(): List<RemoteDuelRecord> = RemoteDuel.decodeHistory(prefs.getString(K_HISTORY, null))

    fun stats(): RemoteDuel.Stats = RemoteDuel.stats(history())

    fun isPlayed(challenge: RemoteChallenge): Boolean = RemoteDuel.encode(challenge) in played()

    private fun played(): Set<String> = prefs.getStringSet(K_PLAYED, emptySet()) ?: emptySet()

    /** Se llama al terminar de jugar un reto recibido. */
    fun finishAsChallenged(challenge: RemoteChallenge, myScore: Int): RemoteResult {
        val code = RemoteDuel.encode(challenge)
        val outcome = RemoteDuel.outcome(myScore, challenge.score)
        val first = code !in played()
        if (!first) return RemoteResult(outcome, RemoteDuel.Reward(0, 0), false)

        val reward = RemoteDuel.rewardFor(outcome)
        economy.addCoins(reward.coins)
        economy.addGems(reward.gems)
        val record = RemoteDuelRecord(RemoteDuel.displayName(challenge.name), myScore, challenge.score, outcome, LocalDay.today())
        // más reciente primero, máximo 30
        val newHistory = (listOf(record) + history()).take(30)
        prefs.edit()
            .putString(K_HISTORY, RemoteDuel.encodeHistory(newHistory))
            .putStringSet(K_PLAYED, played() + code)
            .apply()
        return RemoteResult(outcome, reward, true)
    }

    /** Se llama al terminar de jugar el reto propio (el que se va a compartir). Devuelve lo cobrado. */
    fun finishAsCreator(): RemoteDuel.Reward {
        val today = LocalDay.today()
        val count = if (prefs.getInt(K_CREATED_DAY, -1) == today) prefs.getInt(K_CREATED_COUNT, 0) else 0
        val reward = RemoteDuel.createReward(count)
        economy.addCoins(reward.coins)
        prefs.edit().putInt(K_CREATED_DAY, today).putInt(K_CREATED_COUNT, count + 1).apply()
        return reward
    }

    private companion object {
        const val K_HISTORY = "history"
        const val K_PLAYED = "played_codes"
        const val K_CREATED_DAY = "created_day"
        const val K_CREATED_COUNT = "created_count"
    }
}
