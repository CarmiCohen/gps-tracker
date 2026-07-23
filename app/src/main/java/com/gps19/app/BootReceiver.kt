package com.gps19.app

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import android.content.BroadcastReceiver
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber

/**
 * BootReceiver: Triggered when the device restarts.
 * July.22.02:
 * - Issue #119: Boot Persistence Integrity. Enforced isSystemActive check.
 * - Issue #120: Hilt Hardening. Converted BootServiceStartWorker to @HiltWorker.
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
@HiltWorker
class BootServiceStartWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: MainRepository
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // Issue #119: isSystemActive is the single source of truth for revival
        val isSystemActive = repository.isSystemActiveFlow.firstOrNull() ?: repository.getBoolean(MainRepository.IS_SYSTEM_ACTIVE_KEY, false)
        val appMode = repository.appModeFlow.firstOrNull() ?: repository.getAppMode()
        
        if (isSystemActive && appMode != null) {
            Timber.i("BootWorker: Restarting service in $appMode mode (System Active)")
            val serviceClass = if (appMode == "tracker") TrackerService::class.java else ViewerService::class.java
            val serviceIntent = Intent(applicationContext, serviceClass)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    applicationContext.startForegroundService(serviceIntent)
                } else {
                    applicationContext.startService(serviceIntent)
                }
                return Result.success()
            } catch (e: Exception) {
                Timber.e(e, "Failed to start service from BootWorker")
                return Result.retry()
            }
        } else {
            Timber.d("BootWorker: Revival skipped. Active: $isSystemActive, Mode: $appMode")
        }
        return Result.success()
    }
}
