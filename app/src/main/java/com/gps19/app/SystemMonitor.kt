package com.gps19.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SystemMonitor: Manages system-level resources like WakeLocks and 
 * Watchdog Alarms to ensure service longevity.
 * v9.3.3:
 * - Issue #058: Hilt Migration. Added @Inject constructor and setter for watchdog listener.
 * v8.9.87:
 * - Issue #005 Hardening: Cached packageName to eliminate repetitive getPackageName() 
 *   system log spam on Samsung devices.
 */
@Singleton
class SystemMonitor @Inject constructor(
    @ApplicationContext private val context: Context, 
    private val timeProvider: TimeProvider
) {
    private var onLogWatchdog: ((Boolean, Int) -> Unit)? = null

    private var wakeLock: PowerManager.WakeLock? = null
    private var lastScheduledWatchdogTs = 0L
    private var nextExpectedExpiryTs = 0L
    private var skippedCounter = 0
    
    // Rationale: Cache packageName to prevent repetitive getPackageName() log spam.
    private val cachedPkgName = context.packageName

    var jumpStateStartTs = 0L
    var gpsStallStartTs = 0L

    fun setWatchdogListener(listener: (Boolean, Int) -> Unit) {
        this.onLogWatchdog = listener
    }

    fun acquireWakeLock() {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (wakeLock == null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GPSTracker:MainLock").apply {
                    setReferenceCounted(false)
                }
            }
            
            wakeLock?.acquire(WAKELOCK_TIMEOUT_MS) 
            Timber.d("WakeLock acquired/renewed for ${WAKELOCK_TIMEOUT_MS/1000}s")
        } catch (e: Exception) {
            Timber.e(e, "Failed to acquire WakeLock")
        }
    }

    fun renewWakeLock() {
        acquireWakeLock()
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
        
        onLogWatchdog?.invoke(true, skippedCounter)
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
