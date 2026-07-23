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
import javax.inject.Inject
import kotlin.math.max

/**
 * BaseMonitorService: Common infrastructure for Tracker and Viewer services.
 * July.23.07:
 * - Issue #120b: Startup I/O Stabilization. Staggered proactivePruning (2000ms delay) 
 *   to prevent Room/IO contention during cold starts (R104b).
 * July.22.08:
 * - Issue #104b: Global Startup Maintenance Authority. Integrated proactivePruning into onCreate 
 *   to ensure background service starts also benefit from log pruning (R104).
 * July.22.04:
 * - Hilt Hardening: Standardized field injection.
 */
abstract class BaseMonitorService : LifecycleService() {

    @Inject lateinit var configManager: ConfigManager
    @Inject lateinit var logManager: LogManager
    @Inject lateinit var connectivitySuite: ConnectivitySuite
    @Inject lateinit var repository: MainRepository
    @Inject lateinit var telemetryRepository: TelemetryRepository
    @Inject lateinit var offlineRepository: OfflineRepository
    @Inject lateinit var timeProvider: TimeProvider
    
    @Inject lateinit var systemMonitor: SystemMonitor
    @Inject lateinit var notificationManager: AppNotificationManager

    @Inject lateinit var gpsManager: GpsManager
    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var systemStatusProvider: SystemStatusProvider
    @Inject lateinit var forensicUseCase: ServiceForensicUseCase
    @Inject lateinit var integrityMonitor: IntegrityMonitor
    @Inject lateinit var alarmManager: AppAlarmManager
    @Inject lateinit var historyManager: HistoryManager
    @Inject lateinit var locationProcessor: LocationProcessor
    @Inject lateinit var commandRouter: CommandRouter
    
    @Inject lateinit var appSensorManager: AppSensorManager
    @Inject lateinit var serviceBehaviorUseCase: ServiceBehaviorUseCase
    
    protected val cachedPkgName by lazy { packageName }

    protected var serviceStartRealtime = 0L // Monotonic
    protected var serviceStartWall = 0L // Wall-clock (Issue #102)
    protected var lastServiceTickTs = 0L // Wall-clock
    protected var lastServiceTickRealtime = 0L // Monotonic
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
        
        serviceStartRealtime = timeProvider.elapsedRealtime()
        serviceStartWall = timeProvider.currentTimeMillis()
        
        lifecycleScope.launch(Dispatchers.Default + serviceExceptionHandler) {
            startServiceForeground()
            
            // Issue #120b: Global Startup Maintenance Authority
            // Ensure proactive log pruning is triggered with a delay to prevent I/O bottlenecks during initialization.
            delay(2000L)
            repository.proactivePruning()
            
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
    abstract suspend fun processTick(now: Long, nowRt: Long)
    abstract fun getRequiredTickInterval(): Long

    protected fun startTickLoop() {
        tickJob?.cancel()
        tickJob = lifecycleScope.launch(Dispatchers.Default + serviceExceptionHandler) {
            while (isActive) { 
                val startTime = timeProvider.elapsedRealtime()
                val now = timeProvider.currentTimeMillis()
                val nowRt = timeProvider.elapsedRealtime()
                
                systemMonitor.scheduleWatchdogAlarm()
                processTick(now, nowRt) 
                
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
                repository.flushHistory()
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
