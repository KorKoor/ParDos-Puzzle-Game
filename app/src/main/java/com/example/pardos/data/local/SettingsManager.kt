package com.korkoor.pardos.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Preferencias del jugador: sonido, música, vibración y avisos. Los StateFlow son compartidos para que
 * el sonido, la vibración y las notificaciones lean siempre el valor actual sin recargar nada.
 */
class SettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("pardos_settings", Context.MODE_PRIVATE)

    init {
        if (!loaded) {
            _sound.value = prefs.getBoolean(K_SOUND, true)
            _music.value = prefs.getBoolean(K_MUSIC, true)
            _haptics.value = prefs.getBoolean(K_HAPTICS, true)
            _notifications.value = prefs.getBoolean(K_NOTIFICATIONS, true)
            _autoNext.value = prefs.getBoolean(K_AUTONEXT, true)
            loaded = true
        }
    }

    /** Efectos de sonido del tablero, victoria y derrota. */
    val soundEnabled: StateFlow<Boolean> = _sound.asStateFlow()
    /** Música del menú. */
    val musicEnabled: StateFlow<Boolean> = _music.asStateFlow()
    /** Vibración al mover y al fusionar. */
    val hapticsEnabled: StateFlow<Boolean> = _haptics.asStateFlow()
    /** Avisos para volver (cofre listo, racha, liga…). */
    val notificationsEnabled: StateFlow<Boolean> = _notifications.asStateFlow()

    /** Pasar solo al siguiente nivel unos segundos después de ganar (cualquier toque lo pausa). */
    val autoNextEnabled: StateFlow<Boolean> = _autoNext.asStateFlow()

    fun setAutoNext(on: Boolean) { _autoNext.value = on; prefs.edit().putBoolean(K_AUTONEXT, on).apply() }
    fun setSound(on: Boolean) { _sound.value = on; prefs.edit().putBoolean(K_SOUND, on).apply() }
    fun setMusic(on: Boolean) { _music.value = on; prefs.edit().putBoolean(K_MUSIC, on).apply() }
    fun setHaptics(on: Boolean) { _haptics.value = on; prefs.edit().putBoolean(K_HAPTICS, on).apply() }
    fun setNotifications(on: Boolean) { _notifications.value = on; prefs.edit().putBoolean(K_NOTIFICATIONS, on).apply() }

    private companion object {
        const val K_SOUND = "sound"
        const val K_MUSIC = "music"
        const val K_HAPTICS = "haptics"
        const val K_NOTIFICATIONS = "notifications"
        const val K_AUTONEXT = "auto_next"
        val _sound = MutableStateFlow(true)
        val _music = MutableStateFlow(true)
        val _haptics = MutableStateFlow(true)
        val _notifications = MutableStateFlow(true)
        val _autoNext = MutableStateFlow(true)
        var loaded = false
    }
}
