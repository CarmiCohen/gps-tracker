package com.gps19.app.monitor

import android.content.Intent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.gps19.core.shared.SystemLogger

/**
 * MonitorMessagingService: Handles high-priority FCM Data Messages (Point 13).
 * Triggers the Alarm Overlay even if the app is in the background or device is locked.
 */
class MonitorMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        SystemLogger.i("FCM", "Critical Violation Data Received: ${remoteMessage.data["type"]}")
        
        // Trigger the High-Priority Alarm Overlay immediately
        val intent = Intent(this, AlarmOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            putExtra("violation_type", remoteMessage.data["type"])
        }
        startActivity(intent)
    }
}
