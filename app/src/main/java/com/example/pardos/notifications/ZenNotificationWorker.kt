package com.korkoor.pardos.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.korkoor.pardos.MainActivity
import com.korkoor.pardos.R.drawable.ic_launcher_foreground

class ZenNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    companion object {
        private const val TAG = "ZenNotificationWorker"
    }

    override fun doWork(): Result {
        val title = inputData.getString("title") ?: "ParDos: Zen Math"
        val message = inputData.getString("message") ?: "Es hora de jugar"
        val notificationId = inputData.getInt("id", 0)

        Log.d(TAG, "doWork id=$notificationId title=$title")
        showNotification(title, message, notificationId)

        return Result.success()
    }

    private fun showNotification(title: String, message: String, notificationId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                Log.w(TAG, "Notification skipped: POST_NOTIFICATIONS not granted")
                return
            }
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "pardos_retention_channel"

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

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            manager.notify(notificationId, notification)
            Log.d(TAG, "Notification posted id=$notificationId")
        } catch (se: SecurityException) {
            Log.e(TAG, "SecurityException posting notification id=$notificationId: ${se.message}")
        }
    }
}
