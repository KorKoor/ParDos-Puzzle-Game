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

    fun scheduleAllNotifications() {
        Log.d(TAG, "scheduleAllNotifications")
        cancelAllNotifications()

        schedule("powerup_clean", "Tu Varita Magica esta lista", "El poder de limpieza se ha recargado. Vuelve al tablero.", 15, TimeUnit.MINUTES, 1)
        schedule("powerup_merge", "Poder de Fusion disponible", "Combina estrategicamente. Tu poder esta al 100%.", 16, TimeUnit.MINUTES, 2)
        schedule("revancha", "El tablero te reta de nuevo", "Toma un respiro y vuelve a intentarlo. Puedes superar este nivel.", 1, TimeUnit.HOURS, 3)
        schedule("partida_pendiente", "Tu partida te esta esperando", "Dejaste tus fichas a medias. Entra y termina lo que empezaste.", 2, TimeUnit.HOURS, 4)

        scheduleAtHour("despertar", "Buenos dias, jugador Zen", "Tus 3 nuevas misiones diarias acaban de llegar. Gana tu XP de hoy.", 9, 0, 5)
        scheduleAtHour("almuerzo", "Hora de un respiro", "Despeja tu mente con una partida rapida de ParDos.", 13, 30, 6)
        scheduleAtHour("salvavidas", "Alerta roja: Tu racha", "Solo te toma 2 minutos. Juega ahora y no pierdas tus dias acumulados.", 20, 0, 7)
        scheduleAtHour("recompensas", "Olvidaste tu botin", "Tienes XP esperando a ser reclamada. Entra antes de que termine el dia.", 21, 0, 8)
        scheduleAtHour("nocturno", "Relaja tu mente antes de dormir", "Combina un par de fichas en modo Zen para descansar mejor.", 22, 30, 9)

        schedule("nivel_cerca", "Estas a punto de ascender", "Te falta muy poca XP para subir de nivel. Entra y consiguelo hoy.", 24, TimeUnit.HOURS, 10)

        schedule("racha_perdida", "Tu fuego se apago", "Perdiste tu racha, pero hoy es un gran dia para iniciar una nueva.", 2, TimeUnit.DAYS, 13)
        schedule("soborno", "Te extranamos en el tablero", "Tus fichas estan frias. Entra hoy y supera un nivel para calentar motores.", 5, TimeUnit.DAYS, 14)
        schedule("gran_retorno", "Ha pasado mucho tiempo", "El arte de combinar te llama. Echamos una partida rapida sin estres.", 14, TimeUnit.DAYS, 15)
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

    private fun scheduleAtHour(tag: String, title: String, message: String, targetHour: Int, targetMinute: Int, id: Int) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
        }

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        val delayMs = target.timeInMillis - now.timeInMillis
        schedule(tag, title, message, delayMs, TimeUnit.MILLISECONDS, id)
    }
}
