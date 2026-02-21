package com.korkoor.pardos.data.local

import android.content.Context
import com.korkoor.pardos.domain.model.DailyMission
import com.korkoor.pardos.domain.model.MissionPool
import com.korkoor.pardos.domain.model.MissionType
import java.util.Calendar

class MissionManager(context: Context) {
    private val prefs = context.getSharedPreferences("pardos_missions", Context.MODE_PRIVATE)

    // Obtiene las 3 misiones de hoy
    fun getTodayMissions(): List<DailyMission> {
        checkAndRotateMissions()

        val activeIds = prefs.getString("active_mission_ids", "") ?: ""
        if (activeIds.isEmpty()) return emptyList()

        val idList = activeIds.split(",").mapNotNull { it.toIntOrNull() }

        // Reconstruimos las misiones con su progreso guardado
        return idList.mapNotNull { id ->
            MissionPool.allMissions.find { it.id == id }?.copy(
                currentProgress = prefs.getInt("mission_${id}_progress", 0),
                isCompleted = prefs.getBoolean("mission_${id}_completed", false)
            )
        }
    }

    // Comprueba si es un día nuevo para cambiar las misiones
    private fun checkAndRotateMissions() {
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val savedDay = prefs.getInt("last_mission_day", -1)

        if (currentDay != savedDay) {
            // ¡NUEVO DÍA! Elegimos 3 misiones al azar
            val newMissions = MissionPool.allMissions.shuffled().take(3)
            val idsString = newMissions.joinToString(",") { it.id.toString() }

            prefs.edit().apply {
                putInt("last_mission_day", currentDay)
                putString("active_mission_ids", idsString)
                // Limpiamos el progreso viejo
                MissionPool.allMissions.forEach {
                    remove("mission_${it.id}_progress")
                    remove("mission_${it.id}_completed")
                }
                apply()
            }
        }
    }

    // --- FUNCIONES PARA AUMENTAR EL PROGRESO DESDE EL JUEGO ---

    fun updateProgress(type: MissionType, amount: Int = 1) {
        val missions = getTodayMissions()
        var changed = false

        missions.filter { it.type == type && !it.isCompleted }.forEach { mission ->
            // Para REACH_BLOCK y WIN_UNDER_TIME, no sumamos, evaluamos la mejor marca
            val newProgress = when (type) {
                MissionType.REACH_BLOCK -> maxOf(mission.currentProgress, amount)
                MissionType.WIN_UNDER_TIME -> if (amount <= mission.targetValue) mission.targetValue else mission.currentProgress
                else -> mission.currentProgress + amount
            }

            if (newProgress != mission.currentProgress) {
                prefs.edit().putInt("mission_${mission.id}_progress", newProgress).apply()
                changed = true

                // ¿Se completó?
                if (newProgress >= mission.targetValue && !mission.isCompleted) {
                    prefs.edit().putBoolean("mission_${mission.id}_completed", true).apply()
                    // Aquí podrías llamar al ProfileManager para sumar la XP de recompensa
                    // profileManager.addXp(mission.xpReward)
                }
            }
        }
    }
    // Devuelve true si la recompensa ya fue cobrada
    fun isMissionClaimed(missionId: Int): Boolean {
        return prefs.getBoolean("mission_${missionId}_claimed", false)
    }

    // Marca la misión como cobrada para que no den XP infinita
    fun claimMissionReward(missionId: Int) {
        prefs.edit().putBoolean("mission_${missionId}_claimed", true).apply()
    }
}