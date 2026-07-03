package com.gps19.app

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.StatFs
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.gps19.core.engine.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * MaintenanceWorker: A "Second Line of Defense" to ensure the tracking/viewing service remains active.
 * v8.9.87:
 * - Issue #005 Hardening: Cached packageName to prevent repetitive getPackageName() 
 *   system log spam on Samsung devices.
 * v8.8.21: Migrated to TimeProvider for all timing logic.
 */
@HiltWorker
class MaintenanceWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: MainRepository,
    private val timeProvider: TimeProvider
) : CoroutineWorker(context, params) {
    
    // Rationale: Cache packageName to prevent repetitive getPackageName() log spam.
    private val cachedPkgName = applicationContext.packageName

    override suspend fun doWork(): Result {
        val savedMode = repository.getAppMode()
        
        val recoveryThresholdMs = 180000L
        val lastTick = repository.getLong(MainRepository.LAST_SERVICE_TICK_TS_KEY, 0L)
        val now = timeProvider.currentTimeMillis()
        val silenceDurationMs = now - lastTick
        
        val isNetworkAlive = isNetworkAvailable(applicationContext)
        val networkStatus = if (isNetworkAlive) "ALIVE" else "DEAD"

        Log.d("GPS19", "MAINTENANCE: Periodic check. Mode: $savedMode, Silence: ${silenceDurationMs/1000}s, Net: $networkStatus")

        if (savedMode != null) {
            if (lastTick == 0L || silenceDurationMs > recoveryThresholdMs) {
                
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

    companion object {
        private const val WORK_NAME = "GPS_Maintenance"

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
}
