package com.gps19.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.gps19.core.engine.TimeProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject

/**
 * WatchdogReceiver: Responds to watchdog alarms to ensure the service stays active.
 * July.30.26:
 * - Issue #626: Foreground Service Start Hardening. Added handling for 
 *   ForegroundServiceStartNotAllowedException with deferred recovery flagging.
 * July.21.00:
 * - Monotonic Rt Alignment: Synchronized with hardened engine timing.
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
            val nowRt = timeProvider.elapsedRealtime()
            
            scope.launch {
                try {
                    val appMode = repository.getAppMode() ?: "tracker"

                    repository.addLog(LogEntry(
                        timestamp = timeProvider.currentTimeMillis(),
                        message = "SYSTEM RECOVERY: Watchdog triggered service restart ($appMode) at Rt=$nowRt.",
                        type = "system",
                        isImportant = true,
                        isSpecial = true,
                        specialColor = 0xFFF472B6.toInt()
                    ))

                    val serviceClass = if (appMode == "tracker") TrackerService::class.java else ViewerService::class.java
                    val serviceIntent = Intent(context, serviceClass).apply {
                        this.action = "WATCHDOG_WAKEUP"
                        putExtra("WAKEUP_RT", nowRt)
                    }
                    
                    try {
                        withContext(Dispatchers.Main) {
                            ContextCompat.startForegroundService(context, serviceIntent)
                        }
                        repository.saveBoolean(IS_RECOVERY_PENDING_KEY, false)
                    } catch (e: Exception) {
                        // Issue #626: Handle background start restriction on Android 12+
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && 
                            e.toString().contains("ForegroundServiceStartNotAllowedException")) {
                            Timber.w("Watchdog: Foreground start restricted. Flagging recovery pending.")
                            repository.saveBoolean(IS_RECOVERY_PENDING_KEY, true)
                        } else {
                            throw e
                        }
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
