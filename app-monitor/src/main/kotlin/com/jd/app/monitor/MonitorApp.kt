package com.jd.app.monitor

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.jd.core.shared.SystemLogger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * MonitorApp: Hardened for Revol30.1.
 * Standardizes Alert Channels (Point 164) and initializes forensic logging.
 * Supplies the Hilt worker factory, which @HiltWorker classes need to be created (ISSUE-NOV5.2-O14).
 */
@HiltAndroidApp
class MonitorApp : Application(), Configuration.Provider {

    companion object {
        /** High importance; the siren is played by [SirenPlayer], so the channel itself is silent. */
        const val ALARM_CHANNEL_ID = "monitor_alarm"
        /** Low importance, for the ongoing tracking session notification. */
        const val SESSION_CHANNEL_ID = "monitor_session"
        /** Shared by both until Nov7.2; the session service had lowered it to low importance. */
        const val LEGACY_CHANNEL_ID = "critical_violation_channel"
    }

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        
        // Initialize SystemLogger delegate to bridge to Logcat (Point 62)
        SystemLogger.logDelegate = { level, tag, message ->
            when(level) {
                "I" -> Log.i("REV_$tag", message)
                "W" -> Log.w("REV_$tag", message)
                "E" -> Log.e("REV_$tag", message)
                else -> Log.v("REV_$tag", message)
            }
        }

        createMonitorNotificationChannels(this)
        TrailPruningWorker.schedule(this)
    }
}

/**
 * The only definition of the Monitor channels. Calling it again recreates the same importances, so no caller can
 * lower the alarm channel, as the session service did with its own definition until Nov7.2.
 */
fun createMonitorNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.deleteNotificationChannel(MonitorApp.LEGACY_CHANNEL_ID)
    manager.createNotificationChannel(
        NotificationChannel(MonitorApp.ALARM_CHANNEL_ID, "Tracker Alarms", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Full-screen alarms about the paired tracker. The app plays the siren itself."
            setBypassDnd(true)
            enableLights(true)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
    )
    manager.createNotificationChannel(
        NotificationChannel(MonitorApp.SESSION_CHANNEL_ID, "Tracking Session", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Shown while the Monitor receives a paired tracker."
        }
    )
}
