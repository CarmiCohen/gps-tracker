package com.jd.app.sensor

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.jd.core.shared.SystemLogger
import dagger.hilt.android.HiltAndroidApp

/**
 * SensorApp: Optimized for background persistence.
 * Hardened for Revol30.1: Initializing SystemLogger delegate for forensic visibility.
 */
@HiltAndroidApp
class SensorApp : Application() {

    companion object {
        const val CHANNEL_ID = "sensor_service_channel"
        /** Warnings the Sensor user must act on, such as noise detection off after a restart (ISSUE-NOV8.103-O60). */
        const val WARNING_CHANNEL_ID = "sensor_warning_channel"
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize SystemLogger delegate to bridge to Logcat (Point 62 compliance)
        SystemLogger.logDelegate = { level, tag, message ->
            when(level) {
                "I" -> Log.i("REV_$tag", message)
                "W" -> Log.w("REV_$tag", message)
                "E" -> Log.e("REV_$tag", message)
                else -> Log.v("REV_$tag", message)
            }
        }

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "GPS Sensor Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val warningChannel = NotificationChannel(
                WARNING_CHANNEL_ID,
                "GPS Sensor Warnings",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(warningChannel)
        }
    }
}
