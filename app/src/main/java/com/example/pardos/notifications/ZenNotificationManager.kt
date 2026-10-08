package com.korkoor.pardos.notifications

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ZenNotificationManager(private val context: Context) {

    companion object {
        private const val TAG = "ZenNotificationManager"
    }

    private val workManager = WorkManager.getInstance(context)

    /**
     * Pocas notificaciones, con sentido. Se programan al salir del juego (hoy ya jugaste),
     * así que los avisos de racha y regalo apuntan a MAÑANA y solo avisan de algo real.
     */
    fun scheduleAllNotifications() {
        Log.d(TAG, "scheduleAllNotifications")
        cancelAllNotifications()

        val streak = com.korkoor.pardos.data.local.ProfileManager(context).getProfile().currentStreak
        val nextDay = streak + 1

        // Poderes recargados (a los 16 min, cuando ambos cooldowns ya pasaron)
        schedule("powerup_ready", "Tus poderes están listos", "Varita y Fusión recargadas. Vuelve al tablero.", 16, TimeUnit.MINUTES, 1)

        // Regalo del día siguiente (mañana 10:00)
        scheduleAtHour(
            "regalo_diario", "Tu regalo de hoy te espera",
            "Día $nextDay de racha: entra y reclama tus monedas.",
            10, 0, 2, dayOffset = 1
        )

        // Racha en riesgo (mañana 20:00) — solo si hay una racha que proteger
        if (streak >= 2) {
            scheduleAtHour(
                "racha_riesgo", "Tu racha de $streak días está en riesgo",
                "Solo te toma 2 minutos. Juega hoy y no la pierdas.",
                20, 0, 3, dayOffset = 1
            )
        }

        // Regresos: 3 y 7 días sin entrar
        schedule("regreso_3d", "Tus fichas te extrañan", "Hay misiones nuevas y un regalo esperándote.", 3, TimeUnit.DAYS, 4)
        schedule("regreso_7d", "Ha pasado una semana", "Vuelve a un tablero tranquilo: una partida Zen y listo.", 7, TimeUnit.DAYS, 5)
    }

    fun cancelAllNotifications() {
        Log.d(TAG, "cancelAllNotifications")
        workManager.cancelAllWork()
    }

    private fun schedule(tag: String, title: String, message: String, duration: Long, unit: TimeUnit, id: Int) {
        val data = Data.Builder()
            .putString("title", title)
            .putString("message", message)
            .putInt("id", id)
            .build()

        val request = OneTimeWorkRequestBuilder<ZenNotificationWorker>()
            .setInitialDelay(duration, unit)
            .setInputData(data)
            .addTag(tag)
            .build()

        Log.d(TAG, "Enqueue tag=$tag id=$id delay=$duration $unit")
        workManager.enqueueUniqueWork(tag, ExistingWorkPolicy.REPLACE, request)
    }

    private fun scheduleAtHour(tag: String, title: String, message: String, targetHour: Int, targetMinute: Int, id: Int, dayOffset: Int = 0) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
        }

        target.add(Calendar.DAY_OF_YEAR, dayOffset)
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        val delayMs = target.timeInMillis - now.timeInMillis
        schedule(tag, title, message, delayMs, TimeUnit.MILLISECONDS, id)
    }
}
