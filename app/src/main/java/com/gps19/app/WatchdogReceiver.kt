package com.gps19.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.gps19.core.engine.TimeProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

/**
 * WatchdogReceiver: Responds to watchdog alarms to ensure the service stays active.
 * v9.3.6: Migrated to Hilt @AndroidEntryPoint.
 */
@AndroidEntryPoint
class WatchdogReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: MainRepository
    @Inject lateinit var timeProvider: TimeProvider

    override fun onReceive(context: Context, intent: Intent?) {
        // Removed super.onReceive as it is an abstract method in BroadcastReceiver
        
        val action = intent?.action
        Timber.d("Watchdog Receiver: Action $action received")
        
        if (action == ACTION_ALARM_WAKEUP) {
            try {
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
