package com.gps19.app.monitor

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.gps19.core.shared.SystemLogger

/**
 * MonitorSessionService: Active only while the user is actively tracking.
 * Ensures the "User Trail" is preserved if the app is put in the background (Point 11).
 */
class MonitorSessionService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, MonitorApp.ALARM_CHANNEL_ID)
            .setContentTitle("Active Tracking Session")
            .setContentText("Recording your position relative to the sensor.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()

        startForeground(1001, notification)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
