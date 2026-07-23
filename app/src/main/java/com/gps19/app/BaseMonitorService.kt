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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.math.max

/**
 * BaseMonitorService: Common infrastructure for Tracker and Viewer services.
 * July.23.11:
 * - Issue #113: Throttled Foreground Service updates (5s) to prevent Main-thread 
 *   notification floods and ANRs during hardware recovery bursts.
 * - Added isSystemActive authority to suppress non-essential background noise.
 * July.23.07:
 * - Issue #120b: Startup I/O Stabilization. Staggered proactivePruning.
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

    // Issue #113: FGS Throttling
    private var lastFgsUpdateRealtime = 0L
    private val FGS_UPDATE_THROTTLE_MS = 5000L

    protected var isSystemActive = false
        private set

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
            
            // Sync with system active state
            repository.isSystemActiveFlow.collectLatest { active ->
                isSystemActive = active
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
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

    /**
     * safeStartForeground: Wrapped FGS start/update with internal throttling.
     * July.23.11: Added hard 5s throttle to prevent system flood.
     */
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
