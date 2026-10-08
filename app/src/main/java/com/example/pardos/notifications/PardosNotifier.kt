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
import com.korkoor.pardos.MainActivity
import com.korkoor.pardos.R

/** Canales: el jugador puede silenciar una categoría sin apagar todas desde los ajustes del sistema. */
object NotificationChannels {
    const val REWARDS = "pardos_rewards"
    const val STREAK = "pardos_streak"
    const val EVENTS = "pardos_events"
    const val GAME = "pardos_game"

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val m = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        fun make(id: String, name: String, desc: String, importance: Int) {
            if (m.getNotificationChannel(id) == null) {
                m.createNotificationChannel(NotificationChannel(id, name, importance).apply { description = desc })
            }
        }
        make(REWARDS, "Premios y cofres", "Cofre gratis, ruleta, pase de temporada y hucha", NotificationManager.IMPORTANCE_DEFAULT)
        make(STREAK, "Racha y regresos", "Tu racha en riesgo y recordatorios para volver", NotificationManager.IMPORTANCE_DEFAULT)
        make(EVENTS, "Fiestas y ligas", "Fiestas con skin, ligas y misiones semanales", NotificationManager.IMPORTANCE_DEFAULT)
        make(GAME, "Partida", "Tus poderes recargados", NotificationManager.IMPORTANCE_LOW)
    }

    /** Canal al que pertenece cada aviso (la clave la define `ReminderPlanner`). */
    fun forKey(key: String): String = when {
        key == "powerup_ready" -> GAME
        key == "racha_riesgo" || key.startsWith("regreso") -> STREAK
        key == "season_end" || key == "weekly_end" || key == "league_end" || key.startsWith("event_") -> EVENTS
        else -> REWARDS
    }
}

/** A qué pantalla debe llevar tocar un aviso. */
object NotificationRoute {
    const val EXTRA = "open_screen"

    /** Diálogos del menú que se abren al llegar (cofre gratis, hucha). Los consume `RetentionSection`. */
    @Volatile var pendingDialog: String? = null

    fun forKey(key: String): String = when {
        key == "free_chest" -> "chest"
        key == "piggy_full" -> "piggy"
        key == "wheel" -> "wheel"
        key == "season_claim" || key == "season_end" -> "season"
        key == "league_end" -> "friends"
        key.startsWith("event_") -> "shop"
        else -> "menu"
    }
}

object PardosNotifier {
    private const val TAG = "PardosNotifier"

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Muestra un aviso ya. [key] decide el canal y la pantalla a la que lleva. */
    fun show(context: Context, title: String, message: String, id: Int, key: String): Boolean {
        if (!canPost(context)) {
            Log.w(TAG, "Aviso omitido: POST_NOTIFICATIONS no concedido")
            return false
        }
        NotificationChannels.ensure(context)
        val route = NotificationRoute.forKey(key)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(NotificationRoute.EXTRA, route)
        }
        // Cada aviso lleva su propio PendingIntent (el id lo distingue); si no, Android reutilizaría el extra del primero
        val pending = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, NotificationChannels.forKey(key))
            .setSmallIcon(R.drawable.ic_stat_pardos)
            .setColor(0xFFE8892B.toInt())
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        return try {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(id, notification)
            Log.d(TAG, "Aviso publicado id=$id key=$key canal=${NotificationChannels.forKey(key)} ruta=$route")
            true
        } catch (se: SecurityException) {
            Log.e(TAG, "SecurityException publicando id=$id: ${se.message}")
            false
        }
    }
}
