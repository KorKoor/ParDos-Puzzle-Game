package com.korkoor.pardos.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.korkoor.pardos.MainActivity
import com.korkoor.pardos.R
import com.korkoor.pardos.R.drawable.ic_launcher_foreground

class ZenNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val title = inputData.getString("title") ?: "ParDos: Zen Math"
        val message = inputData.getString("message") ?: "¡Es hora de jugar!"
        val notificationId = inputData.getInt("id", 0)

        showNotification(title, message, notificationId)

        return Result.success()
    }

    private fun showNotification(title: String, message: String, notificationId: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "pardos_retention_channel"

        // En Android 8.0+ es obligatorio crear un "Canal" de notificaciones
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "ParDos Alertas",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones para mantener tu racha y misiones"
            }
            manager.createNotificationChannel(channel)
        }

        // Intent para que al tocar la notificación, se abra tu juego
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Construimos la estética de la notificación
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(ic_launcher_foreground) // Reemplaza luego con tu icono transparente (.png)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(notificationId, notification)
    }
}