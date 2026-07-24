package com.gps19.app

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.StatFs
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.*
import com.gps19.core.engine.*
import androidx.hilt.work.HiltWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

/**
 * MaintenanceWorker: A "Second Line of Defense" to ensure the tracking/viewing service remains active.
 * July.24.04:
 * - Issue #539: Background Start Hardening. Implemented setForeground() during 
 *   recovery to ensure API 34+ compliance for background-to-foreground transitions.
 * July.22.07:
 * - Issue #108: Startup Recovery Race Hardening.
 */
@HiltWorker
class MaintenanceWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: MainRepository,
    private val timeProvider: TimeProvider,
    private val notificationManager: AppNotificationManager
) : CoroutineWorker(context, params) {
    
    private val cachedPkgName = applicationContext.packageName

    companion object {
        private const val WORK_NAME = "GPS_Maintenance"
        private const val RECOVERY_THRESHOLD_MS = 180000L // 3 minutes
        private const val RECOVERY_GRACE_PERIOD_MS = 60000L // 1 minute startup protection

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<MaintenanceWorker>(15, TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
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
        val isSystemActive = repository.isSystemActiveFlow.firstOrNull() ?: repository.getBoolean(MainRepository.IS_SYSTEM_ACTIVE_KEY, false)
        
        val now = timeProvider.currentTimeMillis()
        val lastTick = repository.getLong(MainRepository.LAST_SERVICE_TICK_TS_KEY, 0L)
        val appStartTime = repository.getLong(MainRepository.APP_START_TIME_KEY, 0L)
        
        val silenceDurationMs = now - lastTick
        val appUptimeMs = now - appStartTime
        
        val isNetworkAlive = isNetworkAvailable(applicationContext)
        val networkStatus = if (isNetworkAlive) "ALIVE" else "DEAD"

        Log.d("GPS19", "MAINTENANCE: Periodic check. Mode: $savedMode, Active: $isSystemActive, Silence: ${silenceDurationMs/1000}s, Uptime: ${appUptimeMs/1000}s, Net: $networkStatus")

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

                if (isStorageCritical()) {
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
                    setPackage(cachedPkgName)
                }
                
                try {
                    ContextCompat.startForegroundService(applicationContext, serviceIntent)
                    Log.i("GPS19", "MAINTENANCE: Recovery intent sent successfully")
                } catch (e: Exception) {
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

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun isStorageCritical(): Boolean {
        return try {
            val stat = StatFs(applicationContext.filesDir.path)
            val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
            val megabytesAvailable = bytesAvailable / (1024 * 1024)
            megabytesAvailable < SYSTEM_STORAGE_CRITICAL_THRESHOLD_MB
        } catch (e: Exception) {
            false
        }
    }
}
