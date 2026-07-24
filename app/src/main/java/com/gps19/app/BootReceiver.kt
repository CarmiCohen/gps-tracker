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
import androidx.work.ForegroundInfo
import androidx.work.OutOfQuotaPolicy
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber

/**
 * BootReceiver: Triggered when the device restarts.
 * July.24.04:
 * - Issue #539: Background Start Hardening. Migrated to Expedited Work Request
 *   to ensure API 34+ reliability for background-to-foreground transitions.
 * - Fix: Corrected ForegroundInfo construction using AppNotificationManager.
 * July.22.02:
 * - Issue #120: Hilt Hardening.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED || 
            intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent?.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            Timber.d("Boot detected, scheduling expedited service start via WorkManager")
            val workRequest = OneTimeWorkRequestBuilder<BootServiceStartWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            
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
    private val repository: MainRepository,
    private val notificationManager: AppNotificationManager
) : CoroutineWorker(context, params) {

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            notificationManager.getNotificationId(),
            notificationManager.buildForegroundNotification("System revival in progress...")
        )
    }

    override suspend fun doWork(): Result {
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
