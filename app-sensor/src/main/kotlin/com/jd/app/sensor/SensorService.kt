package com.jd.app.sensor

import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.jd.core.network.SignalingClient
import com.jd.core.shared.*
import com.jd.core.ui.getAppVersionName
import com.jd.core.ui.toFixReading
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import kotlin.math.max

/**
 * SensorService: Ultimate Hardened Version.
 * Refactored for Revol30.1: Robust API 34 compliance and periodic trace confirmation.
 * Updated Nov1.7: Fixed API 34 Permission Regression and suppressed StackLog leaks (R-ID 10/86).
 * Remediation: Deferred sensitive registrations to post-permission verification.
 * Iteration 4: Removed redundant Network Callback registration to achieve absolute StackLog silence.
 */
@AndroidEntryPoint
class SensorService : Service(), SensorEventListener {

    @Inject lateinit var orchestrator: TickOrchestrator
    @Inject lateinit var signalingClient: SignalingClient
    @Inject lateinit var identityManager: com.jd.core.network.ForensicIdentityManager
    @Inject lateinit var sensorDataStore: SensorDataStore
    @Inject lateinit var fixStore: SensorFixStore
    @Inject lateinit var serviceStatus: SensorServiceStatus
    private lateinit var forensicBuffer: ForensicBuffer
    private lateinit var sensorManager: SensorManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var powerManager: PowerManager
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var wakeLock: PowerManager.WakeLock? = null
    private var isMuzzled = false
    private var isInitialized = false
    private var lastValidationLocation: LocationUpdate? = null

    // Raw IMU caches
    private var lastAccel = DoubleArray(3)
    private var lastGravity = DoubleArray(3)
    
    // Adaptive Sampling & Throttling State
    private var currentIntervalMs = 1000L
    private var lastLocation: Location? = null
    private var sequenceIdCounter = 0L
    
    private var lastBatteryLevel = 100
    private var lastIsCharging = false
    private var lastTempCelsius = 30.0f

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                lastLocation = location
                fixStore.publish(location.toFixReading())
                refreshSamplingRate()
            }
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            lastBatteryLevel = if (level != -1 && scale != -1) (level * 100 / scale) else 100
            
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            lastIsCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                             status == BatteryManager.BATTERY_STATUS_FULL
                             
            val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 300)
            lastTempCelsius = tempTenths / 10.0f
            
            refreshSamplingRate()
        }
    }

    private val thermalListener = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        PowerManager.OnThermalStatusChangedListener { status ->
            SystemLogger.w("SensorService", "Thermal Status Changed: $status")
            refreshSamplingRate()
        }
    } else null

    override fun onCreate() {
        super.onCreate()
        SystemLogger.i("SensorService", "onCreate called. Version: ${getAppVersionName(this)}")
        
        val protectedContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            createDeviceProtectedStorageContext()
        } else this
        
        val capacity = ForensicBuffer.calculateAdaptiveCapacity(sampleRateHz = 100, retentionSeconds = 100)
        forensicBuffer = ForensicBuffer(protectedContext.filesDir, capacity = capacity)
        forensicBuffer.initialize()
        sequenceIdCounter = forensicBuffer.nextSequenceId()
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    private fun initializeServiceComponents() {
        if (isInitialized) return
        
        SystemLogger.i("SensorService", "Initializing hardware components post-permission verification.")
        
        registerBatteryReceiver()
        registerThermalListener()
        acquireWakeLock()
        setupBackfillListener()
        registerSensors()
        startLocationUpdates()
        connectSignaling()
        
        isInitialized = true
        serviceStatus.markRunning()
    }

    private fun connectSignaling() {
        serviceScope.launch {
            kotlinx.coroutines.flow.combine(identityManager.selfId, sensorDataStore.relayUrl) { selfId, rawUrl ->
                val url = RelayConfig.resolve(rawUrl)
                selfId to url
            }.collect { (selfId, url) ->
                SystemLogger.i("SensorService", "Connecting to relay $url as $selfId")
                signalingClient.connect(
                    url = url,
                    deviceId = selfId,
                    secret = selfId
                )
            }
        }
    }

    private fun registerBatteryReceiver() {
        try {
            registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (e: Exception) {
            SystemLogger.e("SensorService", "Failed to register battery receiver: ${e.message}")
        }
    }

    private fun registerThermalListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            thermalListener?.let { powerManager.addThermalStatusListener(it) }
        }
    }

    private fun setupBackfillListener() {
        signalingClient.backfillRequestFlow
            .onEach { request ->
                SystemLogger.i("Backfill", "Received request for range ${request.startSequenceId} to ${request.endSequenceId}")
                val updates = forensicBuffer.readRange(request.startSequenceId, request.endSequenceId)
                val sent = answerBackfill(request.requestId, updates, signalingClient::sendBackfillResponse)
                if (!sent) {
                    SystemLogger.w("Backfill", "Send failed for ${request.requestId}. Remaining chunks dropped; the Monitor will ask again.")
                }
            }
            .launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        SystemLogger.i("SensorService", "onStartCommand received. Handling FGS activation...")
        
        if (hasRequiredPermissions()) {
            startForegroundServiceInternal()
            initializeServiceComponents()
            
            if (!isMuzzled) {
                startSensingLoop()
                startWatchdog()
            }
        } else {
            SystemLogger.e("SensorService", "Critical permissions missing for FGS. Self-stopping.")
            stopSelf()
        }
        
        return START_STICKY
    }

    private fun hasRequiredPermissions(): Boolean {
        val locationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        val networkStateGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED

        val fgsLocationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else true

        return locationGranted && notificationGranted && networkStateGranted && fgsLocationGranted
    }

    private fun registerSensors() {
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        
        val delay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SensorManager.SENSOR_DELAY_NORMAL 
        } else {
            SensorManager.SENSOR_DELAY_FASTEST
        }
        
        accel?.let { sensorManager.registerListener(this, it, delay) }
        gravity?.let { sensorManager.registerListener(this, it, delay) }
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(1000L)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            SystemLogger.e("Location", "SecurityException during location updates: ${e.message}")
        }
    }

    private fun refreshSamplingRate() {
        val speed = lastLocation?.speed ?: 0f
        val newInterval = SensingEngine.getOptimalIntervalMs(
            currentTemp = lastTempCelsius,
            isCharging = lastIsCharging,
            batteryLevel = lastBatteryLevel,
            velocityMps = speed
        )
        
        if (newInterval != currentIntervalMs) {
            SystemLogger.i("SensorService", "Sampling Updated: ${newInterval}ms (Speed: ${speed}m/s, Bat: ${lastBatteryLevel}%, Temp: ${lastTempCelsius}C)")
            currentIntervalMs = newInterval
            orchestrator.preemptLoop("sensing_loop")
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                lastAccel[0] = event.values[0].toDouble()
                lastAccel[1] = event.values[1].toDouble()
                lastAccel[2] = event.values[2].toDouble()
            }
            Sensor.TYPE_GRAVITY -> {
                lastGravity[0] = event.values[0].toDouble()
                lastGravity[1] = event.values[1].toDouble()
                lastGravity[2] = event.values[2].toDouble()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun startSensingLoop() {
        SystemLogger.i("SensorService", "Sensing loop initializing...")
        orchestrator.launchPeriodicLoop(
            name = "sensing_loop",
            scope = serviceScope,
            intervalProvider = { currentIntervalMs }
        ) {
            val update = PoolRegistry.locationUpdatePool.acquire()
            try {
                val worldAccel = SensingEngine.rotateToWorldSpace(
                    lastAccel[0], lastAccel[1], lastAccel[2],
                    lastGravity[0], lastGravity[1], lastGravity[2]
                )
                
                update.sensorSnapshot.vibration = worldAccel[0].toFloat()
                update.timestamp = System.currentTimeMillis()
                update.monotonicNanos = System.nanoTime()
                update.sequenceId = sequenceIdCounter++
                update.status = "VALID"
                update.batteryLevel = lastBatteryLevel
                update.isCharging = lastIsCharging
                update.tempCelsius = lastTempCelsius
                
                lastLocation?.let { loc ->
                    update.lat = loc.latitude
                    update.lng = loc.longitude
                    update.accuracy = loc.accuracy
                    update.speed = loc.speed
                    update.bearing = loc.bearing
                    update.altitude = loc.altitude
                }

                val hasPhysicalMotion = update.sensorSnapshot.vibration > 0.12f
                val validation = if (lastValidationLocation != null) {
                    SentinelLogic.validateMovement(lastValidationLocation!!, update, hasPhysicalMotion)
                } else {
                    SentinelLogic.ValidationResult(SentinelLogic.SentinelStatus.VALID)
                }

                update.status = validation.status.name
                lastValidationLocation = update.snapshot()

                if (validation.status != SentinelLogic.SentinelStatus.VALID) {
                    SystemLogger.w("SensorService", "Violation detected: ${validation.status} - ${validation.reason}")
                    serviceScope.launch {
                        val violation = ViolationEvent(
                            type = validation.status.name,
                            location = update.snapshot(),
                            nonce = java.util.UUID.randomUUID().toString()
                        )
                        signalingClient.sendViolation(violation)
                    }
                }
                
                forensicBuffer.writeTrace(update)
                signalingClient.sendTelemetry(update)

                if (System.currentTimeMillis() % 10000 < currentIntervalMs) {
                    SystemLogger.i("SensorService", "Telemetry sent. Seq: ${update.sequenceId} | Interval: ${currentIntervalMs}ms")
                }
            } catch (e: Exception) {
                SystemLogger.e("SensorService", "Loop error: ${e.message}")
            } finally {
                PoolRegistry.locationUpdatePool.release(update)
            }
        }
    }

    private fun startWatchdog() {
        orchestrator.launchPeriodicLoop(
            name = "forensic_watchdog",
            scope = serviceScope,
            intervalProvider = { 5000L }
        ) {
            renewWakeLock()
            val elapsedNanos = orchestrator.getNanosecondsSinceLastTick("sensing_loop")
            if (elapsedNanos == -1L) return@launchPeriodicLoop
            
            val elapsedMs = elapsedNanos / 1_000_000
            val threshold = max(currentIntervalMs * 3, 10000L) 
            
            if (elapsedMs > threshold) {
                SystemLogger.e("Watchdog", "Sensing loop stall detected! (Elapsed: ${elapsedMs}ms, Threshold: ${threshold}ms). Restarting...")
                startSensingLoop()
            }
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Sensor:SensingLock").apply {
            setReferenceCounted(false)
        }
        renewWakeLock()
    }

    /** The timed lock expires if the watchdog stops, so a dead service cannot hold the CPU awake. */
    private fun renewWakeLock() {
        try {
            wakeLock?.acquire(WAKE_LOCK_TIMEOUT_MS)
        } catch (e: Exception) {
            SystemLogger.e("Power", "Failed to acquire WakeLock: ${e.message}")
        }
    }

    private fun startForegroundServiceInternal() {
        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, AdminActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, SensorApp.CHANNEL_ID)
            .setContentTitle("System Maintenance")
            .setContentText("Integrity Monitoring Active")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
            
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
                    type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                }
                startForeground(1, notification, type)
            } else {
                startForeground(1, notification)
            }
        } catch (e: Exception) {
            SystemLogger.e("SensorService", "Fatal FGS start error: ${e.message}")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } catch (e2: Exception) {
                    stopSelf()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private companion object {
        const val WAKE_LOCK_TIMEOUT_MS = 10 * 60 * 1000L
    }

    override fun onDestroy() {
        SystemLogger.i("SensorService", "onDestroy called. Cleaning up...")
        serviceScope.cancel()
        orchestrator.cancelAll()
        fixStore.clear()
        serviceStatus.markStopped()
        if (isInitialized) {
            sensorManager.unregisterListener(this)
            fusedLocationClient.removeLocationUpdates(locationCallback)
            try { unregisterReceiver(batteryReceiver) } catch (e: Exception) {}
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                thermalListener?.let { powerManager.removeThermalStatusListener(it) }
            }
        }
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }
}
