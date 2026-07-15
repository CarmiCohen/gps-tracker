package com.gps19.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.gps19.core.engine.CapabilityStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AppNotificationManager: Manages system notifications and full-screen alarm intents.
 * July.1.12:
 * - Issue #502: Device Independency. Genericized overlay blocking logic using PermissionState.
 */
@Singleton
class AppNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val statusProvider: SystemStatusProvider
) {

    private val channelId = "location_service_channel"
    private val alarmChannelId = "alarm_service_channel"
    private val notificationId = 1919
    private val alarmNotificationId = 1920
    
    private val cachedPkgName = context.packageName

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(
                NotificationChannel(channelId, "Service", NotificationManager.IMPORTANCE_LOW)
            )
            manager?.createNotificationChannel(
                NotificationChannel(alarmChannelId, "Alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                    setSound(null, null) 
                    enableVibration(true)
                }
            )
        }
    }

    fun buildForegroundNotification(contentText: String = "Monitoring system active."): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_jd_logo)
            .setContentTitle("GPS Tracker Active")
            .setContentText(contentText)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun updatePulse(sats: Int, battery: Int, isSecure: Boolean, isPowerSave: Boolean) {
        val status = if (isSecure) "SECURE" else "VIOLATION"
        val powerSaveTag = if (isPowerSave) " [LOW POWER]" else ""
        val msg = String.format(Locale.getDefault(), "Status: %s | Sats: %d | Batt: %d%%%s", status, sats, battery, powerSaveTag)
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, buildForegroundNotification(msg))
    }

    fun updateAlarmNotification(causes: String, showPermissionAction: Boolean = false) {
        val intent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra("causes", causes)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, alarmChannelId)
            .setSmallIcon(R.drawable.ic_jd_logo)
            .setContentTitle("CRITICAL ALARM")
            .setContentText(causes)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (showPermissionAction) {
            val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", cachedPkgName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val settingsPendingIntent = PendingIntent.getActivity(
                context, 1, settingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "FIX BACKGROUND RESTRICTION", settingsPendingIntent)
            builder.setContentText("$causes (Overlay Hidden - Tap to fix)")
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(alarmNotificationId, builder.build())
    }

    suspend fun showAlarmOverlay(causes: String) {
        val perms = statusProvider.getPermissionState()
        val canDraw = perms.isOverlayGranted
        
        // v9.4.0: Abstracting hardware-specific restriction detection.
        val isHardwareBlocked = perms.hasBackgroundRestriction && perms.backgroundStatus != CapabilityStatus.GRANTED
        
        val effectivelyBlocked = !canDraw || isHardwareBlocked

        updateAlarmNotification(causes, showPermissionAction = effectivelyBlocked)

        if (!effectivelyBlocked) {
            val intent = Intent(context, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra("causes", causes)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
            }
        }
    }

    fun cancelAlarm() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(alarmNotificationId)
    }

    fun getNotificationId() = notificationId
}
