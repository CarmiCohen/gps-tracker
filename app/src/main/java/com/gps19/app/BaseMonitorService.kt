package com.gps19.app

import android.app.Notification
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.gps19.core.engine.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

/**
 * BaseMonitorService: Common infrastructure for Tracker and Viewer services.
 * July.16.24:
 * - Issue #526: Definitive performance hardening. All logic including foreground start
 *   is deferred to background scope to prevent Main thread hangs on Samsung A15.
 */
abstract class BaseMonitorService : LifecycleService() {

    private val container by lazy { (application as GpsApplication).container }

    val configManager by lazy { container.configManager }
    val logManager by lazy { container.logManager }
    val connectivitySuite by lazy { container.connectivitySuite }
    val repository by lazy { container.mainRepository }
    val telemetryRepository by lazy { container.telemetryRepository }
    val offlineRepository by lazy { container.offlineRepository }
    val timeProvider by lazy { container.timeProvider }
    
    val systemMonitor by lazy { container.systemMonitor }
    val notificationManager by lazy { container.appNotificationManager }

    val gpsManager by lazy { container.gpsManager }
    val sessionManager by lazy { container.sessionManager }
    val systemStatusProvider by lazy { container.systemStatusProvider }
    val forensicUseCase by lazy { container.serviceForensicUseCase }
    val integrityMonitor by lazy { container.integrityMonitor }
    val alarmManager by lazy { container.appAlarmManager }
    val historyManager by lazy { container.historyManager }
    val locationProcessor by lazy { container.locationProcessor }
    val commandRouter by lazy { container.commandRouter }
    
    protected val cachedPkgName by lazy { packageName }

    protected var serviceStartRealtime = 0L
    protected var lastServiceTickTs = 0L
    protected var lastServiceTickRealtime = 0L
    protected var serviceTickCounter = 0
    protected var lastNotificationUpdateTs = 0L
    
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
        
        // v9.5.4: Completely defer all logic off the Main thread. 
        // This prevents the "Lazy Cascade" from triggering DB/Manager init during startup.
        lifecycleScope.launch(Dispatchers.Default + serviceExceptionHandler) {
            // Android allows a short window to call startForeground after service start.
            // We use this window to let the UI finish its frame first.
            startServiceForeground()
            
            serviceStartRealtime = timeProvider.elapsedRealtime()
            
            systemMonitor.setWatchdogListener { set, skipped ->
                lifecycleScope.launch(Dispatchers.IO) {
                    logManager.logWatchdogPulse(set, skipped)
                }
            }

            systemMonitor.acquireWakeLock()
        }
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
            Timber.e(e, "Foreground start failed: ${e.message}")
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
                if (this@BaseMonitorService::repository.isInitialized) {
                    repository.flushHistory()
                }
            }
        }

        systemMonitor.cancelWatchdogAlarm()
        systemMonitor.releaseWakeLock()
        
        connectivitySuite.stop()
        commandRouter.unregister()

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
