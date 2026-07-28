package com.gps19.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SystemMonitorEvent: Reactive event container for system-level triggers.
 */
sealed class SystemMonitorEvent {
    data class WatchdogScheduled(val success: Boolean, val skippedCount: Int) : SystemMonitorEvent()
}

/**
 * SystemMonitor: Manages system-level resources like WakeLocks and 
 * Watchdog Alarms to ensure service longevity.
 * July.28.22:
 * - Issue #617: Global SharedFlow Audit. Hardened _systemMonitorEvents with 
 *   BufferOverflow.DROP_OLDEST to ensure non-blocking resource management (R617).
 * July.26.04:
 * - Issue #545c: Service Reactive Migration. Replaced legacy watchdog listener 
 *   with a SharedFlow (systemMonitorEvents) for reactive event dispatching.
 */
@Singleton
class SystemMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timeProvider: TimeProvider
) {
    private val _systemMonitorEvents = MutableSharedFlow<SystemMonitorEvent>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val systemMonitorEvents: SharedFlow<SystemMonitorEvent> = _systemMonitorEvents.asSharedFlow()

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var wakeLock: PowerManager.WakeLock? = null
    private var lastScheduledWatchdogTs = 0L
    private var nextExpectedExpiryTs = 0L
    private var skippedCounter = 0
    
    private var lastWakeLockRenewalTs = 0L
    private val WAKELOCK_RENEWAL_TTL_MS = 300_000L // 5 minutes

    private val cachedPkgName = context.packageName

    var jumpStateStartTs = 0L
    var gpsStallStartTs = 0L

    /**
     * acquireWakeLock: Acquires or renews the partial wake lock.
     */
    fun acquireWakeLock(force: Boolean = false) {
        val now = timeProvider.elapsedRealtime()
        if (!force && lastWakeLockRenewalTs != 0L && (now - lastWakeLockRenewalTs < WAKELOCK_RENEWAL_TTL_MS)) {
            return
        }

        try {
            if (wakeLock == null) {
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GPSTracker:MainLock").apply {
                    setReferenceCounted(false)
                }
            }
            
            wakeLock?.acquire(WAKELOCK_TIMEOUT_MS) 
            lastWakeLockRenewalTs = now
            Timber.d("WakeLock acquired/renewed for ${WAKELOCK_TIMEOUT_MS/1000}s")
        } catch (e: Exception) {
            Timber.e(e, "Failed to acquire WakeLock")
        }
    }

    fun renewWakeLock() {
        acquireWakeLock(force = false)
    }

    fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Timber.d("WakeLock released")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error releasing WakeLock")
        } finally {
            wakeLock = null
            lastWakeLockRenewalTs = 0L
        }
    }

    fun scheduleWatchdogAlarm(force: Boolean = false) {
        val now = timeProvider.elapsedRealtime()
        
        if (!force && lastScheduledWatchdogTs != 0L && (now - lastScheduledWatchdogTs) < SYSTEM_WATCHDOG_THROTTLE_MS) return
        
        val inDangerWindow = nextExpectedExpiryTs == 0L || (now + WATCHDOG_DANGER_WINDOW_MS >= nextExpectedExpiryTs)
        
        if (!force && !inDangerWindow) {
            skippedCounter++
            return
        }

        val alarmManagerService = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ACTION_ALARM_WAKEUP).setPackage(cachedPkgName)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val triggerAt = now + SYSTEM_WATCHDOG_INTERVAL_MS
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManagerService.canScheduleExactAlarms()) {
                    alarmManagerService.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
                } else {
                    alarmManagerService.setWindow(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, 15000L, pendingIntent)
                }
            } else {
                alarmManagerService.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to schedule watchdog alarm, using fallback")
            alarmManagerService.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
        }
        
        lastScheduledWatchdogTs = now
        nextExpectedExpiryTs = triggerAt
        
        _systemMonitorEvents.tryEmit(SystemMonitorEvent.WatchdogScheduled(true, skippedCounter))
        skippedCounter = 0
    }

    fun cancelWatchdogAlarm() {
        try {
            val alarmManagerService = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(ACTION_ALARM_WAKEUP).setPackage(cachedPkgName)
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, 
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManagerService.cancel(pendingIntent)
                pendingIntent.cancel()
                Timber.d("Watchdog alarm cancelled")
            }
            nextExpectedExpiryTs = 0L
        } catch (e: Exception) {
            Timber.e(e, "Failed to cancel watchdog alarm")
        }
    }
}
