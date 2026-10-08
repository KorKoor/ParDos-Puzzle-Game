package com.korkoor.pardos.notifications

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.korkoor.pardos.data.local.LocalDay
import com.korkoor.pardos.data.local.ProfileManager
import com.korkoor.pardos.data.local.RetentionManager
import com.korkoor.pardos.domain.retention.ReminderInput
import com.korkoor.pardos.domain.retention.ReminderPlanner
import com.korkoor.pardos.domain.retention.SeasonCalendar
import com.korkoor.pardos.domain.retention.SeasonPass
import com.korkoor.pardos.domain.social.WeekCalendar
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ZenNotificationManager(private val context: Context) {

    companion object {
        private const val TAG = "ZenNotificationManager"
    }

    private val workManager = WorkManager.getInstance(context)

    /**
     * Pocas notificaciones, con sentido. Se programan al salir del juego. Qué avisar y cuándo lo decide
     * `ReminderPlanner` (en :shared, con pruebas); aquí solo se traduce a WorkManager.
     */
    fun scheduleAllNotifications() {
        Log.d(TAG, "scheduleAllNotifications")
        cancelAllNotifications()
        if (!com.korkoor.pardos.data.local.SettingsManager(context).notificationsEnabled.value) return

        val retention = RetentionManager(context)
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        val today = LocalDay.today(now)
        val streak = ProfileManager(context).getProfile().currentStreak

        val ownedSkins = com.korkoor.pardos.data.local.EconomyManager(context).ownedSkins.value
        // Fiesta activa cuya skin todavía no es tuya
        val activeEvent = retention.activeSkinEvents().firstOrNull { e ->
            com.korkoor.pardos.domain.shop.EventSkins.skinFor(e.type)?.let { it.id !in ownedSkins } == true
        }

        val missionManager = com.korkoor.pardos.data.local.MissionManager(context)
        val todaysMissions = missionManager.getTodayMissions()
        val claimable = todaysMissions.count { it.isCompleted && !missionManager.isMissionClaimed(it.id) }
        val open = todaysMissions.count { !missionManager.isMissionClaimed(it.id) }

        val plan = ReminderPlanner.plan(
            ReminderInput(
                nowMs = now,
                minuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE),
                streak = streak,
                freeChestLastMs = retention.freeChestLast.value,
                seasonDaysLeft = SeasonCalendar.daysLeft(today),
                seasonClaimable = retention.claimableTierCount(),
                seasonTierReached = SeasonPass.tierFor(retention.seasonPoints.value),
                weeklyOpen = retention.weekly().count { !it.claimed },
                daysLeftInWeek = WeekCalendar.daysLeft(today),
                wheelFreeLeft = retention.wheelAllowance().freeLeft,
                piggyGems = retention.piggy.value,
                leagueName = retention.league.displayName,
                leagueStarsToPromote = com.korkoor.pardos.domain.retention.Leagues.starsToPromote(retention.league, retention.leagueWeekStars()),
                leagueAtRisk = com.korkoor.pardos.domain.retention.Leagues.atRisk(retention.league, retention.leagueWeekStars()),
                eventName = activeEvent?.let { com.korkoor.pardos.domain.shop.EventSkins.eventName(it.type) } ?: "",
                eventSkinName = activeEvent?.let { com.korkoor.pardos.domain.shop.EventSkins.skinFor(it.type)?.displayName } ?: "",
                eventDaysLeft = activeEvent?.daysLeft(today) ?: Int.MAX_VALUE,
                eventWinsLeft = activeEvent?.let { com.korkoor.pardos.domain.shop.EventSkins.winsLeft(retention.eventWins(it)) } ?: 0,
                missionsClaimable = claimable,
                missionsOpen = open,
                perfectDays = retention.perfectDays(),
                perfectToday = retention.perfectToday(),
                unopenedChests = com.korkoor.pardos.data.local.CollectionManager(context).totalChests,
                dailyChallengeOpen = !retention.dailyChallengeDoneToday(),
                upcomingEvents = com.korkoor.pardos.domain.events.EventCalendar.upcoming(today, 14).mapNotNull { (type, days) ->
                    val skin = com.korkoor.pardos.domain.shop.EventSkins.skinFor(type) ?: return@mapNotNull null
                    if (skin.id in ownedSkins) null
                    else com.korkoor.pardos.domain.retention.UpcomingEvent(
                        type.ordinal, com.korkoor.pardos.domain.shop.EventSkins.eventName(type), skin.displayName, days
                    )
                }
            )
        )
        // En noche de brujas los avisos cambian de voz (el texto lo decide `SeasonalCopy`, con pruebas)
        val halloween = com.korkoor.pardos.domain.retention.SeasonalCopy.isHalloweenWindow(today)
        com.korkoor.pardos.domain.retention.SeasonalCopy.apply(plan, halloween)
            .forEach { schedule(it.key, it.title, it.body, it.delayMs, TimeUnit.MILLISECONDS, it.id) }
    }

    fun cancelAllNotifications() {
        Log.d(TAG, "cancelAllNotifications")
        workManager.cancelAllWork()
    }

    private fun schedule(tag: String, title: String, message: String, duration: Long, unit: TimeUnit, id: Int) {
        val data = Data.Builder()
            .putString("key", tag)
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
}
