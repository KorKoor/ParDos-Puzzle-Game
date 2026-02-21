package com.korkoor.pardos.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ZenNotificationManager(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    // Llama a esto cuando el jugador CIERRA o MINIMIZA la app
    fun scheduleAllNotifications() {
        // Primero cancelamos todo lo anterior para no mandar notificaciones duplicadas
        cancelAllNotifications()

        // --- 1. MICRO-RETENCIÓN ---
        schedule("powerup_clean", "✨ Tu Varita Mágica está lista", "El poder de limpieza se ha recargado. Vuelve al tablero.", 15, TimeUnit.MINUTES, 1)
        schedule("powerup_merge", "🌟 Poder de Fusión disponible", "Combina estratégicamente. Tu poder está al 100%.", 16, TimeUnit.MINUTES, 2)
        schedule("revancha", "😤 El tablero te reta de nuevo", "Toma un respiro y vuelve a intentarlo. Tú puedes superar este nivel.", 1, TimeUnit.HOURS, 3)
        schedule("partida_pendiente", "🧩 Tu partida te está esperando", "Dejaste tus fichas a medias. Entra y termina lo que empezaste.", 2, TimeUnit.HOURS, 4)

        // --- 2. EL HÁBITO DIARIO (Calculando horas exactas) ---
        scheduleAtHour("despertar", "☕ Buenos días, jugador Zen", "Tus 3 nuevas misiones diarias acaban de llegar. ¡Gana tu XP de hoy!", 9, 0, 5)
        scheduleAtHour("almuerzo", "🍱 Hora de un respiro", "Despeja tu mente con una partida rápida de ParDos.", 13, 30, 6)
        scheduleAtHour("salvavidas", "🔥 Alerta Roja: Tu Racha", "Solo te toma 2 minutos. Juega ahora y no pierdas tus días acumulados.", 20, 0, 7)
        scheduleAtHour("recompensas", "🎁 Olvidaste tu botín", "Tienes XP esperando a ser reclamada. ¡Entra antes de que termine el día!", 21, 0, 8)
        scheduleAtHour("nocturno", "🌙 Relaja tu mente antes de dormir", "Combina un par de fichas en modo Zen para descansar mejor.", 22, 30, 9)

        // --- 3. PROGRESIÓN ---
        // Asumimos que lo de la XP y el fin de semana lo mandamos al día siguiente y el sábado
        schedule("nivel_cerca", "📈 Estás a punto de ascender", "Te falta muy poca XP para subir de nivel. ¡Entra y consíguelo hoy!", 24, TimeUnit.HOURS, 10)

        // --- 4. REACTIVACIÓN A LARGO PLAZO ---
        schedule("racha_perdida", "😢 Tu fuego se apagó", "Perdiste tu racha, pero hoy es un gran día para iniciar una nueva.", 2, TimeUnit.DAYS, 13)
        schedule("soborno", "💎 Te extrañamos en el tablero", "Tus fichas están frías. Entra hoy y supera un nivel para calentar motores.", 5, TimeUnit.DAYS, 14)
        schedule("gran_retorno", "🕰️ Ha pasado mucho tiempo...", "El arte de combinar te llama. ¿Echamos una partida rápida sin estrés?", 14, TimeUnit.DAYS, 15)
    }

    // Llama a esto cuando el jugador ABRE la app
    fun cancelAllNotifications() {
        workManager.cancelAllWork()
    }

    // Programador general por delay
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

        workManager.enqueueUniqueWork(tag, ExistingWorkPolicy.REPLACE, request)
    }

    // Programador inteligente que calcula cuánto falta para una hora específica
    private fun scheduleAtHour(tag: String, title: String, message: String, targetHour: Int, targetMinute: Int, id: Int) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
        }

        // Si la hora de hoy ya pasó, lo programamos para esa hora mañana
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        val delayMs = target.timeInMillis - now.timeInMillis

        schedule(tag, title, message, delayMs, TimeUnit.MILLISECONDS, id)
    }
}