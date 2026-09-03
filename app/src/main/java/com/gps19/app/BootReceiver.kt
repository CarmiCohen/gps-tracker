package com.gps19.app

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import android.content.BroadcastReceiver
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OutOfQuotaPolicy
import com.gps19.core.engine.TimeProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber

/**
 * BootReceiver: Triggered when the device restarts.
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
 * Sep.03.101:
 * - Issue #897 Enforcement: Fixed InvalidForegroundServiceTypeException on 
 *   Target SDK 35 by explicitly declaring FOREGROUND_SERVICE_TYPE_SPECIAL_USE (R897).
 */
@HiltWorker
class BootServiceStartWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: MainRepository,
    private val notificationManager: AppNotificationManager,
    private val timeProvider: TimeProvider
) : CoroutineWorker(context, params) {

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // R897: Special Use is required for internal system-bridge workers on A15+
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else 0

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationManager.getNotificationId(),
                notificationManager.buildForegroundNotification("System revival in progress..."),
                type
            )
        } else {
            ForegroundInfo(
                notificationManager.getNotificationId(),
                notificationManager.buildForegroundNotification("System revival in progress...")
            )
        }
    }

    override suspend fun doWork(): Result {
        val isSystemActive = repository.isSystemActiveFlow.firstOrNull() ?: repository.getBoolean(IS_SYSTEM_ACTIVE_KEY, false)
        val appMode = repository.appModeFlow.firstOrNull() ?: repository.getAppMode()
        
        if (isSystemActive && appMode != null) {
            Timber.i("BootWorker: Restarting service in $appMode mode (System Active)")
            
            repository.setAppStartTime(timeProvider.currentTimeMillis())

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
