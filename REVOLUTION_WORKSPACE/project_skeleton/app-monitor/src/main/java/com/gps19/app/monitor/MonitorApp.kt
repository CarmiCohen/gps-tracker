package com.gps19.app.monitor

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * MonitorApp: Corrected for High-Importance Alert Channels (Point 164).
 */
@HiltAndroidApp
class MonitorApp : Application() {

    companion object {
        const val ALARM_CHANNEL_ID = "critical_violation_channel"
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
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
                setSound(null, audioAttributes) // Sound is managed by AlarmOverlayActivity
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
