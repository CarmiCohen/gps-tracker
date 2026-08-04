package com.gps19.app

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.*
import com.gps19.core.engine.*
import androidx.hilt.work.HiltWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

/**
 * MaintenanceWorker: A "Second Line of Defense" to ensure the tracking/viewing service remains active.
 * Aug.04.115:
 * - Issue #729: Forensic Audit: Automated Database Integrity Validation. 
 *   Integrated checkDatabaseIntegrity() with charging-aware execution.
 * JAug.04.111:
 * - Issue #721: Performance Hardening. Refactored to use GpsApplication.PACKAGE_NAME 
 *   shadow-cache to eliminate repetitive getPackageName() calls on Samsung A15.
 */
@HiltWorker
class MaintenanceWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: MainRepository,
    private val timeProvider: TimeProvider,
    private val notificationManager: AppNotificationManager,
    private val systemStatusProvider: SystemStatusProvider
) : CoroutineWorker(context, params) {

    companion object {
        private const val LAST_INTEGRITY_CHECK_TS_KEY = "last_integrity_check_ts"
        private const val INTEGRITY_CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 Hours

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<MaintenanceWorker>(15, TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                MAINTENANCE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.d("GPS19", "MAINTENANCE: Work scheduled")
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            notificationManager.getNotificationId(),
            notificationManager.buildForegroundNotification("System maintenance check...")
        )
    }

    override suspend fun doWork(): Result {
        val savedMode = repository.getAppMode()
        val isSystemActive = repository.isSystemActiveFlow.firstOrNull() ?: repository.getBoolean(IS_SYSTEM_ACTIVE_KEY, false)
        
        val now = timeProvider.currentTimeMillis()
        val lastTick = repository.getLong(LAST_SERVICE_TICK_TS_KEY, 0L)
        val appStartTime = repository.getLong(APP_START_TIME_KEY, 0L)
        
        val silenceDurationMs = now - lastTick
        val appUptimeMs = now - appStartTime
        
        val isNetworkAlive = systemStatusProvider.isLocalOnline()
        val networkStatus = if (isNetworkAlive) "ALIVE" else "DEAD"

        Log.d("GPS19", "MAINTENANCE: Periodic check. Mode: $savedMode, Active: $isSystemActive, Silence: ${silenceDurationMs/1000}s, Uptime: ${appUptimeMs/1000}s, Net: $networkStatus")

        // Issue #729: Automated Database Integrity Check
        performIntegrityAudit(now)

        if (appUptimeMs < RECOVERY_GRACE_PERIOD_MS) {
            Log.d("GPS19", "MAINTENANCE: Within startup grace period (${appUptimeMs/1000}s). Skipping recovery check.")
            return Result.success()
        }

        if (savedMode != null && isSystemActive) {
            if (lastTick == 0L || silenceDurationMs > RECOVERY_THRESHOLD_MS) {
                
                // Issue #539: Elevate to foreground to ensure background-start exemption on API 34+
                try {
                    setForeground(getForegroundInfo())
                } catch (e: Exception) {
                    Log.w("GPS19", "MAINTENANCE: Could not set foreground, attempting recovery anyway.")
                }

                if (systemStatusProvider.getStorageStatus().isCritical) {
                    val storageMsg = "MAINTENANCE: Recovery ABORTED. Storage is CRITICAL."
                    Log.e("GPS19", storageMsg)
                    repository.addLog(LogEntry(
                        id = "SYSTEM",
                        timestamp = now,
                        message = storageMsg,
                        type = "ERROR",
                        isImportant = true,
                        isSpecial = true,
                        specialColor = FORENSIC_PINK_COLOR
                    ))
                    return Result.success()
                }

                val recoveryMsg = "MAINTENANCE: Service RECOVERY triggered ($savedMode). Silence: ${silenceDurationMs/1000}s"
                Log.w("GPS19", recoveryMsg)
                
                repository.addLog(LogEntry(
                    id = "SYSTEM",
                    timestamp = now,
                    message = recoveryMsg,
                    type = "RECOVERY",
                    isImportant = true,
                    isSpecial = true,
                    specialColor = FORENSIC_PINK_COLOR
                ))

                val serviceClass = if (savedMode == "tracker") TrackerService::class.java else ViewerService::class.java
                val serviceIntent = Intent(applicationContext, serviceClass).apply {
                    setPackage(GpsApplication.PACKAGE_NAME)
                }
                
                try {
                    ContextCompat.startForegroundService(applicationContext, serviceIntent)
                    Log.i("GPS19", "MAINTENANCE: Recovery intent sent successfully")
                    repository.saveBoolean(IS_RECOVERY_PENDING_KEY, false)
                } catch (e: Exception) {
                    // Issue #626: Handle background start restriction on Android 12+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && 
                        e.toString().contains("ForegroundServiceStartNotAllowedException")) {
                        Log.w("GPS19", "MAINTENANCE: Foreground start restricted. Flagging recovery pending.")
                        repository.saveBoolean(IS_RECOVERY_PENDING_KEY, true)
                        // Issue #629: Forensic Latency Audit
                        repository.saveLong(RECOVERY_BLOCKED_TS_KEY, now)
                        return Result.success()
                    }

                    val errorMsg = "MAINTENANCE: Recovery FAILED: ${e.message}"
                    Log.e("GPS19", errorMsg)
                    repository.addLog(LogEntry(
                        id = "SYSTEM",
                        timestamp = now,
                        message = errorMsg,
                        type = "ERROR",
                        isImportant = true,
                        isSpecial = true,
                        specialColor = FORENSIC_PINK_COLOR
                    ))
                }
            } else {
                Log.d("GPS19", "MAINTENANCE: Service is healthy (${silenceDurationMs/1000}s ago)")
            }
        } else if (savedMode != null && !isSystemActive) {
            Log.d("GPS19", "MAINTENANCE: System is INACTIVE. Skipping recovery check.")
        }

        return Result.success()
    }

    private suspend fun performIntegrityAudit(now: Long) {
        val lastCheck = repository.getLong(LAST_INTEGRITY_CHECK_TS_KEY, 0L)
        val batteryStatus = systemStatusProvider.observeBatteryStatus().first()
        val isCharging = batteryStatus.isCharging
        
        // Check every 24h, OR more frequently if charging and 12h have passed
        val interval = if (isCharging) INTEGRITY_CHECK_INTERVAL_MS / 2 else INTEGRITY_CHECK_INTERVAL_MS
        val shouldCheck = (now - lastCheck >= interval)
        
        if (shouldCheck) {
            Log.i("GPS19", "MAINTENANCE: Starting Database Integrity Audit (Charging: $isCharging)...")
            val result = repository.checkDatabaseIntegrity()
            val isOk = result.equals("ok", ignoreCase = true)
            
            val msg = if (isOk) {
                "MAINTENANCE: Database Integrity Audit PASSED."
            } else {
                "MAINTENANCE: Database Integrity Audit FAILED: $result"
            }
            
            Log.w("GPS19", msg)
            repository.addLog(LogEntry(
                id = "SYSTEM",
                timestamp = now,
                message = msg,
                type = "SYSTEM",
                isImportant = !isOk,
                isSpecial = !isOk,
                specialColor = if (isOk) null else FORENSIC_PINK_COLOR
            ), initiallySynced = true)
            
            repository.saveLong(LAST_INTEGRITY_CHECK_TS_KEY, now)
        }
    }
}
