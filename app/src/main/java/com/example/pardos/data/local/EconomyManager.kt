package com.korkoor.pardos.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.TimeZone

/** Día local (días desde epoch en la zona horaria del jugador). Evita romper rachas por UTC. */
object LocalDay {
    fun today(now: Long = System.currentTimeMillis()): Int =
        ((now + TimeZone.getDefault().getOffset(now)) / 86_400_000L).toInt()
}

/**
 * Monedas (se ganan jugando), gemas (premium, también se compran) y escudos de racha.
 * Los StateFlow son compartidos entre instancias para que la UI siempre vea el valor actual.
 */
class EconomyManager(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("pardos_economy", Context.MODE_PRIVATE)

    init {
        if (!loaded) {
            _coins.value = prefs.getInt(KEY_COINS, 0)
            _gems.value = prefs.getInt(KEY_GEMS, 0)
            _freezes.value = prefs.getInt(KEY_FREEZES, 0)
            loaded = true
        }
    }

    val coins: StateFlow<Int> = _coins.asStateFlow()
    val gems: StateFlow<Int> = _gems.asStateFlow()
    val streakFreezes: StateFlow<Int> = _freezes.asStateFlow()

    fun addCoins(amount: Int) {
        if (amount <= 0) return
        _coins.value += amount
        prefs.edit().putInt(KEY_COINS, _coins.value).apply()
    }

    fun addGems(amount: Int) {
        if (amount <= 0) return
        _gems.value += amount
        prefs.edit().putInt(KEY_GEMS, _gems.value).apply()
    }

    fun addStreakFreezes(amount: Int) {
        if (amount <= 0) return
        _freezes.value += amount
        prefs.edit().putInt(KEY_FREEZES, _freezes.value).apply()
    }

    fun setStreakFreezes(value: Int) {
        _freezes.value = value.coerceAtLeast(0)
        prefs.edit().putInt(KEY_FREEZES, _freezes.value).apply()
    }

    /** Gasta monedas. Devuelve false (sin cambios) si no alcanzan. */
    fun spendCoins(amount: Int): Boolean {
        if (amount < 0 || _coins.value < amount) return false
        _coins.value -= amount
        prefs.edit().putInt(KEY_COINS, _coins.value).apply()
        return true
    }

    fun spendGems(amount: Int): Boolean {
        if (amount < 0 || _gems.value < amount) return false
        _gems.value -= amount
        prefs.edit().putInt(KEY_GEMS, _gems.value).apply()
        return true
    }

    private companion object {
        const val KEY_COINS = "coins"
        const val KEY_GEMS = "gems"
        const val KEY_FREEZES = "streak_freezes"
        val _coins = MutableStateFlow(0)
        val _gems = MutableStateFlow(0)
        val _freezes = MutableStateFlow(0)
        var loaded = false
    }
}
