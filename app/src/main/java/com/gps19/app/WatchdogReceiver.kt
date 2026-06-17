package com.gps19.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import timber.log.Timber

/**
 * WatchdogReceiver: Responds to watchdog alarms to ensure the service stays active.
 * v8.8.21: Migrated to TimeProvider for timing consistency.
 */
class WatchdogReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        Timber.d("Watchdog Receiver: Action $action received")
        
        if (action == ACTION_ALARM_WAKEUP) {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    GpsApplication.GpsApplicationEntryPoint::class.java
                )
                val repository = entryPoint.repository()
                val timeProvider = entryPoint.timeProvider()
                
                val appMode = runBlocking { repository.getAppMode() } ?: "tracker"

                repository.addLog(LogEntry(
                    timestamp = timeProvider.currentTimeMillis(),
                    message = "SYSTEM RECOVERY: Watchdog triggered service restart ($appMode).",
                    type = "system",
                    isImportant = true,
                    isSpecial = true,
                    specialColor = 0xFFF472B6.toInt()
                ))

                val serviceClass = if (appMode == "tracker") TrackerService::class.java else ViewerService::class.java
                val serviceIntent = Intent(context, serviceClass).apply {
                    this.action = "WATCHDOG_WAKEUP"
                }
                ContextCompat.startForegroundService(context, serviceIntent)
            } catch (e: Exception) {
                Timber.e(e, "Watchdog recovery failed")
            }
        }
    }
}
