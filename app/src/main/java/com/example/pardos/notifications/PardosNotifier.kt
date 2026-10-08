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
    const val DAILY = "pardos_daily"

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
        make(DAILY, "Misiones y reto diario", "Misiones nuevas, premios por cobrar y tu racha perfecta", NotificationManager.IMPORTANCE_DEFAULT)
    }

    /** Canal al que pertenece cada aviso (la clave la define `ReminderPlanner`). */
    fun forKey(key: String): String = when {
        key == "powerup_ready" -> GAME
        key == "racha_riesgo" || key.startsWith("regreso") -> STREAK
        key == "season_end" || key == "weekly_end" || key == "league_end" || key.startsWith("event_") -> EVENTS
        key == "daily_missions" || key == "missions_claim" || key == "perfect_streak" || key == "daily_challenge" -> DAILY
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
        key == "chests_unopened" -> "album"
        key == "daily_challenge" -> "menu"
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
            .apply { largeIconFor(context, key, halloween = false)?.let { setLargeIcon(it) } }
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


/** Icono 3D grande de cada aviso (en lugar de emojis en el texto). En noche de brujas, versión embrujada. */
private fun largeIconFor(context: Context, key: String, @Suppress("UNUSED_PARAMETER") halloween: Boolean): android.graphics.Bitmap? {
    val spooky = com.korkoor.pardos.domain.retention.SeasonalCopy.isHalloweenWindow(com.korkoor.pardos.data.local.LocalDay.today())
    val res = when {
        key == "free_chest" || key == "chests_unopened" -> if (spooky) R.drawable.ico_pumpkin else R.drawable.ico_gift
        key == "wheel" -> R.drawable.ico_party
        key == "racha_riesgo" || key == "perfect_streak" -> if (spooky) R.drawable.ico_ghost else R.drawable.ico_fire
        key == "regalo_diario" -> if (spooky) R.drawable.ico_candy else R.drawable.ico_gift
        key.startsWith("regreso") -> if (spooky) R.drawable.ico_bat else R.drawable.ico_sparkles
        key == "season_claim" || key == "season_end" -> if (spooky) R.drawable.ico_web else R.drawable.ico_crown
        key == "missions_claim" || key == "daily_missions" || key == "daily_challenge" -> R.drawable.ico_star
        key == "league_end" -> R.drawable.ico_crown
        key.startsWith("event_") -> R.drawable.ico_party
        key == "piggy_full" -> R.drawable.ico_sparkles
        else -> return null
    }
    return android.graphics.BitmapFactory.decodeResource(context.resources, res)
}
