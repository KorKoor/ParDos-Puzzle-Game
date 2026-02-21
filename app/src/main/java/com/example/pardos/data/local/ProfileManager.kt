package com.korkoor.pardos.data.local

import android.content.Context
import android.content.SharedPreferences
import com.korkoor.pardos.domain.model.UserProfile

class ProfileManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("pardos_profile", Context.MODE_PRIVATE)

    fun getProfile(): UserProfile {
        return UserProfile(
            name = prefs.getString("user_name", "Jugador Zen") ?: "Jugador Zen",
            avatarId = prefs.getInt("avatar_id", 1),
            playerLevel = prefs.getInt("player_level", 1),
            currentXp = prefs.getInt("current_xp", 0),
            currentStreak = prefs.getInt("current_streak", 0),
            bestStreak = prefs.getInt("best_streak", 0)
        )
    }

    fun saveProfile(profile: UserProfile) {
        prefs.edit().apply {
            putString("user_name", profile.name)
            putInt("avatar_id", profile.avatarId)
            apply()
        }
    }

    // 🔥 LA MAGIA DE LA RACHA 🔥
    fun checkAndUpdateStreak() {
        val currentDay = (System.currentTimeMillis() / (1000 * 60 * 60 * 24)).toInt() // Días exactos desde 1970
        val lastPlayDay = prefs.getInt("last_play_day", 0)

        var currentStreak = prefs.getInt("current_streak", 0)
        var bestStreak = prefs.getInt("best_streak", 0)

        if (lastPlayDay == 0) {
            // Primerísima vez que abre el juego en su vida
            currentStreak = 1
            bestStreak = 1
        } else if (currentDay - lastPlayDay == 1) {
            // Jugó exactamente ayer, la racha aumenta 🔥
            currentStreak += 1
            if (currentStreak > bestStreak) bestStreak = currentStreak
        } else if (currentDay - lastPlayDay > 1) {
            // Pasó más de un día sin jugar, se rompe la racha 💔
            currentStreak = 1
        }
        // Nota: Si currentDay == lastPlayDay, significa que ya abrió el juego hoy, la racha se queda igual.

        // Guardamos los resultados
        prefs.edit().apply {
            putInt("current_streak", currentStreak)
            putInt("best_streak", bestStreak)
            putInt("last_play_day", currentDay)
            apply()
        }
    }
}