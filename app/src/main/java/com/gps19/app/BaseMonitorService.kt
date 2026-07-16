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
 * v9.5.0:
 * - Issue #503: Hilt Removal. Manual dependency injection via AppContainer.
 */
abstract class BaseMonitorService : LifecycleService() {

    lateinit var configManager: ConfigManager
    lateinit var logManager: LogManager
    lateinit var networkManager: AppNetworkManager
    lateinit var networkManagerWrapper: RemoteUpdateWrapper
    lateinit var repository: MainRepository
    lateinit var telemetryRepository: TelemetryRepository
    lateinit var offlineRepository: OfflineRepository
    lateinit var timeProvider: TimeProvider
    
    lateinit var systemMonitor: SystemMonitor
    lateinit var notificationManager: AppNotificationManager

    // Common Core Components
    lateinit var gpsManager: GpsManager
    lateinit var sessionManager: SessionManager
    lateinit var systemStatusProvider: SystemStatusProvider
    lateinit var forensicUseCase: ServiceForensicUseCase
    lateinit var integrityMonitor: IntegrityMonitor
    lateinit var alarmManager: AppAlarmManager
    lateinit var historyManager: HistoryManager
    lateinit var locationProcessor: LocationProcessor
    lateinit var syncManager: SyncManager
    lateinit var commandRouter: CommandRouter
    lateinit var remoteHandler: RemoteHandler
    
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
        injectDependencies()
        
        serviceStartRealtime = timeProvider.elapsedRealtime()
        
        systemMonitor.setWatchdogListener { set, skipped ->
            if (this::logManager.isInitialized) {
                logManager.logWatchdogPulse(set, skipped)
            }
        }

        systemMonitor.acquireWakeLock()
        
        startServiceForeground()
    }

    protected open fun injectDependencies() {
        val container = (application as GpsApplication).container
        configManager = container.configManager
        logManager = container.logManager
        networkManager = container.appNetworkManager
        networkManagerWrapper = container.remoteUpdateWrapper
        repository = container.mainRepository
        telemetryRepository = container.telemetryRepository
        offlineRepository = container.offlineRepository
        timeProvider = container.timeProvider
        systemMonitor = container.systemMonitor
        notificationManager = container.appNotificationManager
        gpsManager = container.gpsManager
        sessionManager = container.sessionManager
        systemStatusProvider = container.systemStatusProvider
        forensicUseCase = container.serviceForensicUseCase
        integrityMonitor = container.integrityMonitor
        alarmManager = container.appAlarmManager
        historyManager = container.historyManager
        locationProcessor = container.locationProcessor
        syncManager = container.syncManager
        commandRouter = container.commandRouter
        remoteHandler = container.remoteHandler
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
                if (this@BaseMonitorService::repository.isInitialized) {
                    repository.flushHistory()
                }
            }
        }

        if (this::systemMonitor.isInitialized) {
            systemMonitor.cancelWatchdogAlarm()
            systemMonitor.releaseWakeLock()
        }
        if (this::networkManager.isInitialized) networkManager.stop()
        if (this::commandRouter.isInitialized) commandRouter.unregister()

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
