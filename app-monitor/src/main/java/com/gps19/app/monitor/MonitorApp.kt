package com.gps19.app.monitor

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.os.Build
import android.util.Log
import com.gps19.core.shared.SystemLogger
import dagger.hilt.android.HiltAndroidApp

/**
 * MonitorApp: Hardened for Revol30.1.
 * Standardizes Alert Channels (Point 164) and initializes forensic logging.
 */
@HiltAndroidApp
class MonitorApp : Application() {

    companion object {
        const val ALARM_CHANNEL_ID = "critical_violation_channel"
    }

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

        createAlarmNotificationChannel()
    }

    private fun createAlarmNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                ALARM_CHANNEL_ID,
                "Critical Theft Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Used for high-priority theft and sensor violation alerts."
                setBypassDnd(true)
                enableLights(true)
                enableVibration(true)
                setSound(null, audioAttributes)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
