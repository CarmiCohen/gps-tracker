package com.gps19.app

import android.app.Notification
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.gps19.core.engine.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.math.max

/**
 * BaseMonitorService: Common infrastructure for Tracker and Viewer services.
 * Aug.29.03:
 * - Issue #760 Hardening: Migrated from GpsManager and AppSensorManager to 
 *   the unified HardwareProvider (R760).
 */
@AndroidEntryPoint
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

    @Inject lateinit var hardwareProvider: HardwareProvider
    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var systemStatusProvider: SystemStatusProvider
    @Inject lateinit var forensicUseCase: ServiceForensicUseCase
    @Inject lateinit var integrityMonitor: IntegrityMonitor
    @Inject lateinit var alarmManager: AppAlarmManager
    @Inject lateinit var historyManager: HistoryManager
    @Inject lateinit var locationProcessor: LocationProcessor
    @Inject lateinit var commandRouter: CommandRouter
    
    @Inject lateinit var serviceBehaviorUseCase: ServiceBehaviorUseCase
    
    protected val cachedPkgName: String get() = GpsApplication.PACKAGE_NAME

    protected var serviceStartRealtime = 0L 
    protected var serviceStartWall = 0L 
    protected var lastServiceTickTs = 0L 
    protected var lastServiceTickRealtime = 0L 
    protected var serviceTickCounter = 0
    
    protected val isUiForeground = AtomicBoolean(false)
    protected var lastUiPulseTs = 0L
    
    protected var tickJob: Job? = null
    protected var heartbeatJob: Job? = null
    protected var fgsUpdateJob: Job? = null
    
    protected val transientDropDetected = AtomicBoolean(false)

    private var lastFgsUpdateRealtime = 0L
    private val FGS_UPDATE_THROTTLE_MS = 5000L

    protected var isSystemActive = false
        private set

    protected val serviceExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable is CancellationException) return@CoroutineExceptionHandler
        logManager.logServiceEvent("CRITICAL: Coroutine failure in Service: ${throwable.message}", isImportant = true, isSpecial = true, specialColor = FORENSIC_PINK_COLOR)
        stopSelf() 
    }

    override fun onCreate() {
        super.onCreate()
        
        serviceStartRealtime = timeProvider.elapsedRealtime()
        serviceStartWall = timeProvider.currentTimeMillis()
        
        onServicePreInit()
        startServiceForeground()
        
        lifecycleScope.launch(Dispatchers.Default + serviceExceptionHandler) {
            launch {
                repository.isSystemActiveFlow.collectLatest { active ->
                    isSystemActive = active
                }
            }
            
            onServiceInitialize()

            launch(Dispatchers.IO) {
                systemMonitor.acquireWakeLock()
                
                delay(LANDING_PAGE_PAUSE_MS) 
                repository.proactivePruning()
                
                systemMonitor.systemMonitorEvents.collect { event ->
                    if (event is SystemMonitorEvent.WatchdogScheduled) {
                        logManager.logWatchdogPulse(event.success, event.skippedCount)
                    }
                }
            }
        }
    }

    abstract fun startServiceForeground()
    abstract fun updateForegroundServiceType()
    abstract suspend fun processTick(now: Long, nowRt: Long)
    abstract fun getRequiredTickInterval(): Long

    protected abstract suspend fun onHeartbeat(now: Long, nowRt: Long)
    protected abstract fun onServicePreInit()
    protected abstract suspend fun onServiceInitialize()

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

    protected fun startHeartbeatLoop() {
        heartbeatJob?.cancel()
        heartbeatJob = lifecycleScope.launch(Dispatchers.Default + serviceExceptionHandler) {
            while (isActive) {
                val now = timeProvider.currentTimeMillis()
                val nowRt = timeProvider.elapsedRealtime()
                onHeartbeat(now, nowRt)
                delay(NOTIFICATION_THROTTLE_MS)
            }
        }
    }

    protected fun isUiVisible(): Boolean {
        return isUiForeground.get() && (timeProvider.currentTimeMillis() - lastUiPulseTs < UI_PULSE_TIMEOUT_MS)
    }

    protected fun isRecentUiPulse(): Boolean {
        return (timeProvider.currentTimeMillis() - lastUiPulseTs < UI_PULSE_TIMEOUT_MS)
    }

    protected fun safeStartForeground(id: Int, notification: Notification, type: Int = 0, force: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        if (!force && lastFgsUpdateRealtime != 0L && (now - lastFgsUpdateRealtime < FGS_UPDATE_THROTTLE_MS)) return
        
        lastFgsUpdateRealtime = now
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
        heartbeatJob?.cancel()
        fgsUpdateJob?.cancel()
        
        // Issue #760: Hardened cleanup sequence.
        // 1. Unregister all hardware callbacks synchronously via unified provider.
        hardwareProvider.stop()

        // Issue #320/249: Deterministic native hardware release.
        if (JdHardwareManager.isAvailable()) {
            try {
                JdHardwareManager.releaseHardware(timeProvider)
            } catch (e: Exception) {
                Timber.e(e, "Error releasing native hardware bridge")
            }
        }

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
