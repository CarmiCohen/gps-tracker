package com.gps19.app

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.gps19.core.engine.TimeProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber

/**
 * BootServiceStartWorker: Bridges the boot broadcast to the Foreground Service.
 * Oct.26.0:
 * - Isolation Hardening: Moved to a dedicated file to ensure HiltWorkerFactory 
 *   correctly generates the assisted injection stubs and resolves instantiation 
 *   failures seen on A15 hardware.
 * - API 34 Compatibility: Added explicit guard for SPECIAL_USE foreground type.
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
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // R897: Special Use is required for internal system-bridge workers on A15+
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Fallback to minimal type for older OS versions
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
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
            
            repository.setAppStartTime(timeProvider.currentTimeMillis)

            val serviceIntent = Intent(applicationContext, MonitorService::class.java)
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
