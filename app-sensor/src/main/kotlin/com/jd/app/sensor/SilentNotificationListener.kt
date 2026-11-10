package com.jd.app.sensor

import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Sees notifications that vibrate without sound (ISSUE-NOV9.4-O65), which [OwnVibrationWatch] cannot see from the audio
 * mode or the playbacks. Optional: Android binds it only after the user grants "Notification access" to the Sensor
 * (Phone Setup, section 9); without the grant the Sensor behaves as before. Only the fact that a notification would
 * vibrate is used; its content is not read, stored, or sent.
 */
class SilentNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap) {
        val ranking = Ranking()
        val ranked = rankingMap.getRanking(sbn.key, ranking)
        val audioManager = getSystemService(AudioManager::class.java)
        val vibrates = notificationWouldVibrate(
            isOwnPackage = sbn.packageName == packageName,
            isOngoing = sbn.isOngoing || sbn.notification.flags and Notification.FLAG_FOREGROUND_SERVICE != 0,
            importance = if (ranked) ranking.importance else NotificationManager.IMPORTANCE_DEFAULT,
            channelVibrates = if (ranked) ranking.channel?.shouldVibrate() ?: true else true,
            ringerMode = audioManager.ringerMode,
            matchesInterruptionFilter = !ranked || ranking.matchesInterruptionFilter()
        )
        if (vibrates) OwnNotificationVibration.report()
    }

    companion object {
        /** True when the user has granted this listener notification access. */
        fun isGranted(context: Context): Boolean {
            val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
            val me = ComponentName(context, SilentNotificationListener::class.java)
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
        }
    }
}

/** Passes a vibrating notification from [SilentNotificationListener] to the running [OwnVibrationWatch], same process. */
object OwnNotificationVibration {
    @Volatile var listener: (() -> Unit)? = null
    fun report() { listener?.invoke() }
}

/**
 * True when a newly posted notification makes the phone vibrate: from another app, not an ongoing or foreground-service
 * one, important enough to alert (`IMPORTANCE_DEFAULT` or higher), its channel vibrates, the ringer is not silent, and
 * Do Not Disturb lets it through. In doubt it counts, since a missed knock for a few seconds costs less than a false one.
 */
fun notificationWouldVibrate(
    isOwnPackage: Boolean,
    isOngoing: Boolean,
    importance: Int,
    channelVibrates: Boolean,
    ringerMode: Int,
    matchesInterruptionFilter: Boolean
): Boolean =
    !isOwnPackage && !isOngoing &&
        importance >= NotificationManager.IMPORTANCE_DEFAULT &&
        channelVibrates &&
        ringerMode != AudioManager.RINGER_MODE_SILENT &&
        matchesInterruptionFilter
