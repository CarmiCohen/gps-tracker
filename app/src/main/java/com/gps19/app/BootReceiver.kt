package com.gps19.app

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import android.content.BroadcastReceiver
import androidx.work.CoroutineWorker
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber

/**
 * BootReceiver: Triggered when the device restarts.
 * July.17.02:
 * - Added check for isSystemActive to prevent unintended engine starts on landing page.
 * v9.5.0:
 * - Hilt removed. Manual DI.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED || 
            intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent?.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            Timber.d("Boot detected, scheduling service start via WorkManager")
            val workRequest = OneTimeWorkRequestBuilder<BootServiceStartWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}

/**
 * Worker that bridges the boot broadcast to the Foreground Service.
 */
class BootServiceStartWorker(
    val context: Context,
    params: WorkerParameters,
    private val repository: MainRepository
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val appMode = repository.appModeFlow.firstOrNull() ?: repository.getAppMode()
        val isSystemActive = repository.isSystemActiveFlow.firstOrNull() ?: repository.getBoolean(MainRepository.IS_SYSTEM_ACTIVE_KEY, false)
        
        if (appMode != null && isSystemActive) {
            Timber.i("BootWorker: Restarting service in $appMode mode (System Active)")
            val serviceClass = if (appMode == "tracker") TrackerService::class.java else ViewerService::class.java
            val serviceIntent = Intent(context, serviceClass)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                return Result.success()
            } catch (e: Exception) {
                Timber.e(e, "Failed to start service from BootWorker")
                return Result.retry()
            }
        } else {
            Timber.d("BootWorker: System inactive or no mode found, skipping service start. Mode: $appMode, Active: $isSystemActive")
        }
        return Result.success()
    }
}
