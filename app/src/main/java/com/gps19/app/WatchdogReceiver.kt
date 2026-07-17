package com.gps19.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.gps19.core.engine.TimeProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject

/**
 * WatchdogReceiver: Responds to watchdog alarms to ensure the service stays active.
 * v9.3.30:
 * - ANR Hardening (#092): Replaced runBlocking with goAsync() to prevent 
 *   Main thread starvation during repository IO.
 */
@AndroidEntryPoint
class WatchdogReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: MainRepository
    @Inject lateinit var timeProvider: TimeProvider

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        Timber.d("Watchdog Receiver: Action $action received")
        
        if (action == ACTION_ALARM_WAKEUP) {
            val pendingResult = goAsync()
            
            scope.launch {
                try {
                    val appMode = repository.getAppMode() ?: "tracker"

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
                    
                    withContext(Dispatchers.Main) {
                        ContextCompat.startForegroundService(context, serviceIntent)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Watchdog recovery failed")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
