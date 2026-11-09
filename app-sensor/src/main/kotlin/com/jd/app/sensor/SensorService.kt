package com.jd.app.sensor

import android.Manifest
import android.app.NotificationManager
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
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
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
    private val movementCheck = MovementCheck()
    private var isReportingNoFix = false // Sensing loop only; logs each change between a current fix and NO_FIX.

    // Raw IMU caches, written on the motion sensor thread.
    private var lastAccel = DoubleArray(3)
    private var lastGravity = DoubleArray(3)
    // Tilt, lift (ISSUE-NOV5.2-O11 Group 5), and vibration (Group 7) of the resting tracker; its gravity estimate also
    // replaces the gravity sensor that phones like the A15 lack, whose vibration value was always 0 before Nov9.0.
    private val motionSentinel = MotionSentinel()
    private var motionThread: HandlerThread? = null
    private var hasGravitySensor = false
    private var hasBarometer = false
    private var publishedMotionArmed: Boolean? = null // Motion sensor thread only.
    private var publishedVibrationArmed: Boolean? = null // Motion sensor thread only.
    
    // Adaptive Sampling & Throttling State
    private var currentIntervalMs = 1000L
    @Volatile private var lastLocation: Location? = null // Written on the main looper, read by the sensing loop.
    private val sequenceIdCounter = java.util.concurrent.atomic.AtomicLong(0L)
    private var initJob: Job? = null
    
    private var lastBatteryLevel = -1
    private var lastIsCharging = false
    private var lastTempCelsius = -1f
    // The Monitor user's Home Points, enforced here so the Monitor does no per-fix work (ISSUE-NOV8.13-O51).
    @Volatile private var geofence = GeofenceConfig() // Written by the geofence collector, read by the sensing loop.
    private var geofenceState = SentinelLogic.GeofenceState.UNKNOWN // Sensing loop only.
    @Volatile private var geofenceChanged = false
    // Noise, tilt, and lift found on their own threads, each sent by a following sensing tick with the current position.
    private val pendingAlerts = java.util.concurrent.ConcurrentLinkedQueue<PendingAlert>()
    private val acousticMonitor = AcousticMonitor(
        onAlert = { level, floor -> onAcousticAlert(level, floor) },
        onState = { serviceStatus.setMicrophone(it) }
    )

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
            parseBatteryIntent(intent)
        }
    }

    private fun parseBatteryIntent(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        lastBatteryLevel = if (level >= 0 && scale > 0) level * 100 / scale else -1
        
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        lastIsCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                         status == BatteryManager.BATTERY_STATUS_FULL
                         
        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        lastTempCelsius = if (tempTenths != Int.MIN_VALUE) tempTenths / 10.0f else -1f
        
        refreshSamplingRate()
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
        
        initJob = serviceScope.launch(Dispatchers.IO) {
            try {
                forensicBuffer.initialize()
                sequenceIdCounter.set(forensicBuffer.nextSequenceId())
                SystemLogger.i("SensorService", "ForensicBuffer initialized asynchronously. Next seq: ${sequenceIdCounter.get()}")
            } catch (e: Exception) {
                SystemLogger.e("SensorService", "Async ForensicBuffer initialization failed: ${e.message}")
            }
        }
        
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
        setupGeofenceListener()
        watchMotionSensitivity()
        registerSensors()
        startLocationUpdates()
        connectSignaling()
        
        isInitialized = true
        val startMs = try { android.os.SystemClock.elapsedRealtime() } catch (_: Throwable) { System.currentTimeMillis() }
        serviceStatus.markRunning(startMs)
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
            val sticky = registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (sticky != null) {
                parseBatteryIntent(sticky)
            }
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
                initJob?.join()
                SystemLogger.i("Backfill", "Received request for range ${request.startSequenceId} to ${request.endSequenceId}")
                val updates = forensicBuffer.readRange(request.startSequenceId, request.endSequenceId)
                val sent = answerBackfill(request.requestId, updates, signalingClient::sendBackfillResponse)
                if (!sent) {
                    SystemLogger.w("Backfill", "Send failed for ${request.requestId}. Remaining chunks dropped; the Monitor will ask again.")
                }
            }
            .launchIn(serviceScope)
    }

    /** Stores each geofence the Monitor sends, and enforces the stored one, also after a restart while offline. */
    private fun setupGeofenceListener() {
        signalingClient.geofenceFlow
            .onEach { config ->
                val stored = sensorDataStore.saveGeofence(config)
                SystemLogger.i("Geofence", "Received ${config.points.size} home points, radius ${config.radiusMeters} m, rev ${config.revision} (stored: $stored)")
            }
            .launchIn(serviceScope)
        sensorDataStore.geofence
            .onEach { config ->
                geofence = config
                geofenceChanged = true // The sensing loop restarts the state from the next fix, without a transition.
            }
            .launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            SystemLogger.i("SensorService", "Stop action received via intent. Stopping service...")
            stopSelf()
            return START_NOT_STICKY
        }

        SystemLogger.i("SensorService", "onStartCommand received. Handling FGS activation...")
        if (intent?.action == ACTION_RESTORE_ACCESS) {
            // A notification tap lets a background-started service take location and microphone (ISSUE-NOV8.103-O60).
            SystemLogger.i("SensorService", "Restart notification tapped: taking location and microphone")
        }
        
        if (hasRequiredPermissions()) {
            val start = startForegroundServiceInternal()
            if (start == null) {
                SystemLogger.e("SensorService", "No foreground service type allowed. Self-stopping.")
                stopSelf()
                return START_NOT_STICKY
            }
            initializeServiceComponents()
            applyForegroundStart(start)
            
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

    /**
     * Applies what this start command obtained. Every start command re-checks, so opening the app or tapping the
     * restart notification restores what a background start was refused; a start command that is still refused posts the
     * notification again, so a tap that did not help leaves it in place (ISSUE-NOV8.103-O60).
     */
    private fun applyForegroundStart(start: ForegroundStart) {
        val refused = RefusedAccess(location = !start.hasLocation, microphone = !start.hasMicrophone && hasMicrophonePermission())
        // Requested again once location is regained, so the updates resume at once.
        if (start.hasLocation && serviceStatus.refusedAccess.value.location) startLocationUpdates()
        serviceStatus.setRefusedAccess(refused)
        if (refused.location) SystemLogger.w("SensorService", "Started in the background without location: tracking resumes after a notification tap or opening the Sensor app")
        updateMicrophone(start.hasMicrophone)
        updateRestartNotification(refused)
    }

    /** Listens while the foreground service holds the microphone type. */
    private fun updateMicrophone(canUseMicrophone: Boolean) {
        when {
            canUseMicrophone -> acousticMonitor.start(this)
            !hasMicrophonePermission() -> {
                acousticMonitor.stop()
                serviceStatus.setMicrophone(MicrophoneState.NO_PERMISSION)
            }
            else -> {
                acousticMonitor.stop()
                serviceStatus.setMicrophone(MicrophoneState.NEEDS_APP_OPEN)
                SystemLogger.w("Acoustic", "Started in the background: Android allows the microphone only after a notification tap or opening the Sensor app")
            }
        }
    }

    /** "Tracking off" or "Noise detection off - tap to enable": the tap starts this service from a notification, which may take both. */
    private fun updateRestartNotification(refused: RefusedAccess) {
        val manager = getSystemService(NotificationManager::class.java)
        val title = restartNotificationTitle(refused) ?: return manager.cancel(RESTART_NOTIFICATION_ID)
        val restore = PendingIntent.getForegroundService(
            this, RESTART_NOTIFICATION_ID,
            Intent(this, SensorService::class.java).setAction(ACTION_RESTORE_ACCESS),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(this, SensorApp.WARNING_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(
                if (refused.location) "Location and noise detection stopped when the phone restarted"
                else "The microphone stopped when the phone restarted"
            )
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentIntent(restore)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            manager.notify(RESTART_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            SystemLogger.e("SensorService", "Restart notification failed: ${e.message}")
        }
    }

    private fun hasMicrophonePermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun onAcousticAlert(levelDbfs: Double, floorDbfs: Double) {
        queueAlert(ACOUSTIC_ALERT_STATUS, "Acoustic alert: level ${levelDbfs.toInt()} dBFS, background ${floorDbfs.toInt()} dBFS")
    }

    private fun queueAlert(status: String, logLine: String) {
        pendingAlerts.add(PendingAlert(status, logLine))
        orchestrator.preemptLoop("sensing_loop") // Sent now, not after up to 10 s of a slow sampling interval.
    }

    /**
     * The Sensor's own `Tilt Sensitivity Threshold` and `Vibration Sensitivity Threshold` sliders set the tilt angle and
     * the vibration limit (user decision for tilt, 2026-11-09; vibration follows it, ISSUE-NOV5.2-O11 Group 7).
     */
    private fun watchMotionSensitivity() {
        sensorDataStore.alertSettings
            .onEach { settings ->
                val degrees = tiltThresholdForSensitivity(settings.tiltSensitivity)
                if (degrees != motionSentinel.tiltThresholdDegrees) SystemLogger.i("Motion", "Tilt limit ${degrees.toInt()} deg")
                motionSentinel.tiltThresholdDegrees = degrees
                val vibration = vibrationThresholdForSensitivity(settings.vibrationSensitivity)
                if (vibration != motionSentinel.vibrationThreshold) {
                    SystemLogger.i("Motion", "Vibration limit ${"%.2f".format(java.util.Locale.US, vibration / STANDARD_GRAVITY)} g")
                }
                motionSentinel.vibrationThreshold = vibration
            }
            .launchIn(serviceScope)
    }

    /**
     * The accelerometer runs at about 50 Hz (`SENSOR_DELAY_GAME`), which the lift integration needs; the barometer, where
     * there is one, at the normal rate. Events arrive on their own thread, not the main one.
     */
    private fun registerSensors() {
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        val pressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        hasGravitySensor = gravity != null
        hasBarometer = pressure != null
        val handler = Handler(HandlerThread("MotionSensors").also { it.start(); motionThread = it }.looper)

        accel?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME, handler) }
        gravity?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME, handler) }
        pressure?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL, handler) }
        serviceStatus.setMotion(MotionState(isRunning = accel != null, hasBarometer = hasBarometer))
        SystemLogger.i(
            "Motion",
            "Accelerometer: ${accel != null}, gravity sensor: $hasGravitySensor, barometer: $hasBarometer. Tilt and lift arm after ${MOTION_ARM_AFTER_STILL_MS / 1000} s of rest"
        )
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
        val speed = if (lastLocation != null && lastLocation!!.hasSpeed()) lastLocation!!.speed else 0f
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
        val eventMs = event.timestamp / 1_000_000L // Monotonic sensor time.
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                lastAccel[0] = event.values[0].toDouble()
                lastAccel[1] = event.values[1].toDouble()
                lastAccel[2] = event.values[2].toDouble()
                motionSentinel.onAccelerometer(lastAccel[0], lastAccel[1], lastAccel[2], eventMs).forEach(::onMotionAlert)
                if (motionSentinel.isArmed != publishedMotionArmed) {
                    publishedMotionArmed = motionSentinel.isArmed
                    publishMotion()
                    SystemLogger.i("Motion", if (motionSentinel.isArmed) "Tilt and lift armed" else "Tilt and lift waiting for the phone to rest")
                }
                if (motionSentinel.isVibrationArmed != publishedVibrationArmed) {
                    publishedVibrationArmed = motionSentinel.isVibrationArmed
                    publishMotion()
                    SystemLogger.i("Motion", if (motionSentinel.isVibrationArmed) "Vibration armed" else "Vibration waiting for the phone to rest")
                }
            }
            Sensor.TYPE_GRAVITY -> {
                lastGravity[0] = event.values[0].toDouble()
                lastGravity[1] = event.values[1].toDouble()
                lastGravity[2] = event.values[2].toDouble()
            }
            Sensor.TYPE_PRESSURE -> motionSentinel.onPressure(event.values[0].toDouble(), eventMs)?.let(::onMotionAlert)
        }
    }

    private fun publishMotion() {
        serviceStatus.setMotion(
            MotionState(isRunning = true, isArmed = motionSentinel.isArmed, hasBarometer = hasBarometer, isVibrationArmed = motionSentinel.isVibrationArmed)
        )
    }

    private fun onMotionAlert(alert: MotionSentinel.Alert) {
        when (alert.kind) {
            MotionSentinel.Kind.TILT -> queueAlert(TILT_ALERT_STATUS, "Tilt alert: ${alert.detail}")
            MotionSentinel.Kind.LIFT -> queueAlert(LIFT_ALERT_STATUS, "Lift alert: ${alert.detail}")
            MotionSentinel.Kind.VIBRATION -> queueAlert(VIBRATION_ALERT_STATUS, "Vibration alert: ${alert.detail}")
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
            initJob?.join()
            val update = PoolRegistry.locationUpdatePool.acquire()
            try {
                val gravity = if (hasGravitySensor) lastGravity else motionSentinel.gravity() ?: lastGravity
                val worldAccel = SensingEngine.rotateToWorldSpace(
                    lastAccel[0], lastAccel[1], lastAccel[2],
                    gravity[0], gravity[1], gravity[2]
                )
                
                update.sensorSnapshot.vibration = worldAccel[0].toFloat()
                update.timestamp = System.currentTimeMillis()
                update.monotonicNanos = System.nanoTime()
                update.sequenceId = sequenceIdCounter.getAndIncrement()
                update.status = "VALID"
                update.batteryLevel = lastBatteryLevel
                update.isCharging = lastIsCharging
                update.tempCelsius = lastTempCelsius
                update.isNoiseDetectionOff = serviceStatus.microphone.value.isNoiseDetectionOff // ISSUE-NOV8.103-O60
                
                val hasPhysicalMotion = update.sensorSnapshot.vibration > 0.12f
                // A fix older than FIX_STALE_AFTER_MS is no longer the current position: send NO_FIX without one.
                val currentFix = lastLocation?.takeIf { isFixFresh(it.elapsedRealtimeNanos, SystemClock.elapsedRealtimeNanos()) }
                if (currentFix == null) update.status = NO_FIX_STATUS
                if ((currentFix == null) != isReportingNoFix) {
                    isReportingNoFix = currentFix == null
                    if (isReportingNoFix) {
                        SystemLogger.w("SensorService", "No current GPS fix (none, or older than ${FIX_STALE_AFTER_MS}ms): reporting $NO_FIX_STATUS")
                    } else {
                        SystemLogger.i("SensorService", "GPS fix current again")
                    }
                }
                // Only the tick that carries a new fix is checked; repeated ticks of the same fix stay VALID.
                val validation = currentFix?.let { loc ->
                    update.lat = loc.latitude
                    update.lng = loc.longitude
                    update.accuracy = loc.accuracy
                    update.speed = if (loc.hasSpeed()) loc.speed else -1f
                    update.bearing = loc.bearing
                    update.altitude = loc.altitude

                    if (geofenceChanged) {
                        geofenceChanged = false
                        geofenceState = SentinelLogic.GeofenceState.UNKNOWN
                    }
                    val zone = SentinelLogic.evaluateHomeZone(loc.latitude, loc.longitude, loc.accuracy, geofence, geofenceState)
                    geofenceState = zone.currentState
                    if (zone.transition == SentinelLogic.GeofenceTransition.EXIT) {
                        reportViolation(
                            update, GEOFENCE_BREACH_STATUS,
                            "Geofence breach: ${zone.distanceToCenter.toInt()} m from the nearest home point (radius ${geofence.radiusMeters.toInt()} m, accuracy ${loc.accuracy.toInt()} m)"
                        )
                    }

                    movementCheck.check(
                        fixForMovementCheck(loc.latitude, loc.longitude, loc.accuracy, update.speed, loc.elapsedRealtimeNanos),
                        hasPhysicalMotion
                    )
                }

                if (validation != null && validation.status != SentinelLogic.SentinelStatus.VALID) {
                    reportViolation(update, validation.status.name, "Violation detected: ${validation.status} - ${validation.reason}")
                }

                // A tick that already carries a violation keeps it; a queued alert follows on the next tick.
                if (!update.isCritical) {
                    pendingAlerts.poll()?.let { reportViolation(update, it.status, it.logLine) }
                    if (pendingAlerts.isNotEmpty()) orchestrator.preemptLoop("sensing_loop")
                }
                
                forensicBuffer.writeTrace(update, isCritical = update.isCritical)
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

    /** Marks [update] as violation [status] and sends the violation event. */
    private fun reportViolation(update: LocationUpdate, status: String, logLine: String) {
        update.status = status
        update.isCritical = true
        SystemLogger.w("SensorService", logLine)
        // Built before launching: the pooled update is zeroed on release, before the launched send runs.
        val violation = ViolationEvent(type = status, location = update.snapshot(), nonce = java.util.UUID.randomUUID().toString())
        serviceScope.launch { signalingClient.sendViolation(violation) }
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

    /**
     * Starts the foreground service and returns which while-in-use types it holds, or null if Android allowed none.
     * Android 11+ gives a background service the microphone only through its type, and Android 14+ refuses the location
     * and microphone types to a service started in the background (after a reboot) unless the app may use location all
     * the time (location only). The service then runs on with the types it was allowed, connected to the relay, until
     * a notification tap or opening the Sensor app restores the rest (ISSUE-NOV8.103-O60).
     */
    private fun startForegroundServiceInternal(): ForegroundStart? {
        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, AdminActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, SensorApp.CHANNEL_ID)
            .setContentTitle("System Maintenance")
            .setContentText("Integrity Monitoring Active")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
            
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return try {
                startForeground(1, notification)
                ForegroundStart(hasLocation = true, hasMicrophone = hasMicrophonePermission())
            } catch (e: Exception) {
                SystemLogger.e("SensorService", "Foreground service start failed: ${e.message}")
                null
            }
        }
        var base = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) base = base or ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
        val location = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        val microphone = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hasMicrophonePermission()) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
        // The full set first, then without the microphone, then without location: each refusal throws before anything changes.
        val attempts = listOfNotNull((base or location or microphone).takeIf { microphone != 0 }, base or location, base)
        for (type in attempts) {
            try {
                startForeground(1, notification, type)
                return ForegroundStart(
                    hasLocation = type and location != 0,
                    // Before Android 11 the microphone needs no service type.
                    hasMicrophone = if (microphone != 0) type and microphone != 0 else Build.VERSION.SDK_INT < Build.VERSION_CODES.R && hasMicrophonePermission()
                )
            } catch (e: Exception) {
                SystemLogger.w("SensorService", "Foreground service type 0x${type.toString(16)} refused: ${e.message}")
            }
        }
        return null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private data class PendingAlert(val status: String, val logLine: String)

    /** The while-in-use service types a start command obtained (ISSUE-NOV8.103-O60). */
    private class ForegroundStart(val hasLocation: Boolean, val hasMicrophone: Boolean)

    companion object {
        const val ACTION_STOP_SERVICE = "com.jd.app.sensor.ACTION_STOP_SERVICE"
        /** Sent by a tap on the restart notification (ISSUE-NOV8.103-O60). */
        const val ACTION_RESTORE_ACCESS = "com.jd.app.sensor.ACTION_RESTORE_ACCESS"
        private const val RESTART_NOTIFICATION_ID = 2 // The foreground service notification is 1.
        private const val WAKE_LOCK_TIMEOUT_MS = 10 * 60 * 1000L
    }

    override fun onDestroy() {
        SystemLogger.i("SensorService", "onDestroy called. Cleaning up...")
        acousticMonitor.stop()
        getSystemService(NotificationManager::class.java).cancel(RESTART_NOTIFICATION_ID)
        serviceScope.cancel()
        orchestrator.cancelAll()
        fixStore.clear()
        serviceStatus.markStopped()
        if (isInitialized) {
            sensorManager.unregisterListener(this)
            motionThread?.quitSafely()
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
