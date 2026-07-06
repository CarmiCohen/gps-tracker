package com.gps19.app

import android.app.Notification
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.gps19.core.engine.TICK_INTERVAL_MS
import com.gps19.core.engine.TICK_INTERVAL_SLOW_MS
import com.gps19.core.engine.UI_PULSE_TIMEOUT_MS
import com.gps19.core.engine.TimeProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.math.max

/**
 * BaseMonitorService: Common infrastructure for Tracker and Viewer services.
 * v9.1.4:
 * - Issue #045: Android 15 Foreground Service Hardening. Implemented state-aware 
 *   type enforcement to prevent SecurityException on background starts.
 * v8.9.89:
 * - Issue #005 Hardening: Added cachedPkgName to prevent repetitive getPackageName() 
 *   system log spam on Samsung G990/A155 devices.
 */
@AndroidEntryPoint
abstract class BaseMonitorService : LifecycleService() {

    @Inject lateinit var configManager: ConfigManager
    @Inject lateinit var logManager: LogManager
    @Inject lateinit var networkManager: AppNetworkManager
    @Inject lateinit var repository: MainRepository
    @Inject lateinit var telemetryRepository: TelemetryRepository
    @Inject lateinit var offlineRepository: OfflineRepository
    @Inject lateinit var timeProvider: TimeProvider
    
    protected lateinit var systemMonitor: SystemMonitor
    protected lateinit var notificationManager: AppNotificationManager
    
    // Rationale: Cache packageName to prevent repetitive getPackageName() log spam.
    protected val cachedPkgName by lazy { packageName }

    protected var serviceStartRealtime = 0L
    protected var lastServiceTickTs = 0L
    protected var lastServiceTickRealtime = 0L
    protected var serviceTickCounter = 0
    
    protected val isUiForeground = AtomicBoolean(false)
    protected var lastUiPulseTs = 0L
    
    protected var tickJob: Job? = null
    protected var fgsUpdateJob: Job? = null
    
    protected val transientDropDetected = AtomicBoolean(false)

    protected val serviceExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable is CancellationException) return@CoroutineExceptionHandler
        logManager.logServiceEvent("CRITICAL: Coroutine failure in Service: ${throwable.message}", important = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR)
        stopSelf() 
    }

    override fun onCreate() {
        super.onCreate()
        serviceStartRealtime = timeProvider.elapsedRealtime()
        notificationManager = AppNotificationManager(this)
        
        systemMonitor = SystemMonitor(this, timeProvider) { set, skipped ->
            if (this::logManager.isInitialized) {
                logManager.logWatchdogPulse(set, skipped)
            }
        }

        systemMonitor.acquireWakeLock()
        
        startServiceForeground()
    }

    abstract fun startServiceForeground()
    abstract fun updateForegroundServiceType()
    abstract suspend fun processTick(now: Long, nowRealtime: Long)
    abstract fun getRequiredTickInterval(): Long

    protected fun startTickLoop() {
        tickJob?.cancel()
        tickJob = lifecycleScope.launch(Dispatchers.Default + serviceExceptionHandler) {
            while (isActive) { 
                val startTime = timeProvider.elapsedRealtime()
                val now = timeProvider.currentTimeMillis()
                val nowRealtime = timeProvider.elapsedRealtime()
                
                systemMonitor.scheduleWatchdogAlarm()
                
                processTick(now, nowRealtime) 
                
                val elapsed = timeProvider.elapsedRealtime() - startTime
                val interval = getRequiredTickInterval()
                val remaining = max(50L, interval - elapsed)
                delay(remaining) 
            } 
        }
    }

    protected fun isUiVisible(): Boolean {
        return isUiForeground.get() && (timeProvider.currentTimeMillis() - lastUiPulseTs < UI_PULSE_TIMEOUT_MS)
    }

    protected fun isRecentUiPulse(): Boolean {
        return (timeProvider.currentTimeMillis() - lastUiPulseTs < UI_PULSE_TIMEOUT_MS)
    }

    protected fun safeStartForeground(id: Int, notification: Notification, type: Int = 0) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val enforcedType = if (type == 0) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else type
                startForeground(id, notification, enforcedType)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && type != 0) {
                startForeground(id, notification, type)
            } else {
                startForeground(id, notification)
            }
        } catch (e: Exception) {
            val isStartDenied = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && 
                    e.javaClass.name.contains("ForegroundServiceStartNotAllowedException")
            
            val logMsg = when {
                isStartDenied -> "Foreground start denied (Background restriction): ${e.message}"
                e is SecurityException -> "Foreground start failed (Security/Mismatch): ${e.message}"
                else -> "Foreground start failed: ${e.message}"
            }
            Timber.e(e, logMsg)
            
            if (this::logManager.isInitialized) {
                logManager.logServiceEvent("RECOVERY_WARN: $logMsg", important = true)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        tickJob?.cancel()
        fgsUpdateJob?.cancel()
        
        runBlocking {
            withTimeoutOrNull(1000) {
                repository.flushHistory()
            }
        }

        if (this::systemMonitor.isInitialized) {
            systemMonitor.cancelWatchdogAlarm()
            systemMonitor.releaseWakeLock()
        }
        if (this::networkManager.isInitialized) networkManager.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    protected fun formatDurationHoursMinutes(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return "%02d:%02d".format(hours, minutes)
    }
}
