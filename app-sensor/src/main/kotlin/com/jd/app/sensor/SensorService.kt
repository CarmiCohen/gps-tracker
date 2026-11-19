package com.jd.app.sensor

import android.Manifest
import android.app.ActivityManager
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
import android.location.LocationManager
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
import androidx.core.location.GnssStatusCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.*
import com.jd.core.network.SignalingClient
import com.jd.core.network.peerDeliveries
import com.jd.core.network.relayDeliveries
import com.jd.core.shared.*
import com.jd.core.ui.getAppVersionName
import com.jd.core.ui.readPhoneState
import com.jd.core.ui.toFixReading
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
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
    @Inject lateinit var eventLog: EventLog
    @Inject lateinit var infoSource: SensorInfoSource
    @Inject lateinit var graphHistory: GraphHistory
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
    // The phone's own ringing shakes it like a knock (ISSUE-NOV9.2-O62): vibration alerts pause while it rings.
    private var ownVibrationWatch: OwnVibrationWatch? = null
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
    private var lastIsPluggedIn = false // A charger is connected, even if the battery is held (ISSUE-NOV17.15-O114).
    private var lastTempCelsius = -1f
    // The Monitor user's Home Points, enforced here so the Monitor does no per-fix work (ISSUE-NOV8.13-O51).
    @Volatile private var geofence = GeofenceConfig() // Written by the geofence collector, read by the sensing loop.
    private var geofenceState = SentinelLogic.GeofenceState.UNKNOWN // Sensing loop only.
    @Volatile private var geofenceChanged = false
    // Noise, tilt, and lift found on their own threads, each sent by a following sensing tick with the current position.
    private val pendingAlerts = java.util.concurrent.ConcurrentLinkedQueue<PendingAlert>()
    // One alert per kind a minute, after each detector's own debouncing (ISSUE-NOV17.10-O111).
    private val alertCoolDown = AlertCoolDown()
    // The Monitor's switches and limits (ISSUE-NOV10.8-O83): written by the behaviour collector, read on the sensor threads.
    @Volatile private var sensorBehaviour = SensorBehaviour()
    // False until the stored behaviour is read, so a start does not open the microphone of a switched-off noise detection (ISSUE-NOV15.2-O97).
    @Volatile private var isBehaviourRead = false
    // What the Monitor is told about this phone's setup (ISSUE-NOV10.8-O83 part 2); null until first read.
    private val setupReport = MutableStateFlow<SensorSetupReport?>(null)
    // Main thread only: what the last start command obtained; the noise switch can change after it.
    private var hasForegroundStart = false
    private var isLocationRefused = false
    private var hasMicrophoneType = false
    private val acousticMonitor = AcousticMonitor(
        onAlert = { level, floor -> onAcousticAlert(level, floor) },
        onState = { serviceStatus.setMicrophone(it) }
    )

    // For the Info screens and the rows (ISSUE-NOV10.8-O84 part 2); written on their own threads, read anywhere.
    @Volatile private var satellites: List<SatelliteReading> = emptyList()
    private var isGnssRegistered = false // Main thread only.
    @Volatile private var lux: Float? = null
    @Volatile private var selfId = ""
    @Volatile private var isMonitorOnline = false

    // The minute's peaks for the Graphs (ISSUE-NOV10.8-O84 part 3): offered on the motion sensor thread, taken each minute.
    private val vibrationPeaks = PeakTracker()
    private val tiltPeaks = PeakTracker()

    private val gnssCallback = object : GnssStatusCompat.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatusCompat) {
            satellites = (0 until status.satelliteCount).map { i ->
                SatelliteReading(status.getSvid(i), status.getConstellationType(i), status.getCn0DbHz(i), status.usedInFix(i))
            }
        }

        override fun onStopped() {
            satellites = emptyList()
        }
    }

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
        
        val power = batteryPower(
            status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1),
            plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        )
        lastIsCharging = power.isCharging
        lastIsPluggedIn = power.isPluggedIn
                         
        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        lastTempCelsius = if (tempTenths != Int.MIN_VALUE) tempTenths / 10.0f else -1f
        
        refreshSamplingRate()
    }

    private val thermalListener = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        PowerManager.OnThermalStatusChangedListener { status ->
            eventLog.record(EventLevel.DETAIL, "Thermal status changed: $status")
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
                // An imported settings file sets a floor, so a reinstalled Sensor does not restart at 0 (ISSUE-NOV10.8-O80).
                sequenceIdCounter.set(maxOf(forensicBuffer.nextSequenceId(), sensorDataStore.sequenceFloor.first()))
                serviceStatus.setNextSequence(sequenceIdCounter.get())
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
        setupSensorBehaviourListener()
        setupSequenceFloor()
        setupSetupReporting()
        setupEventReporting()
        setupInfoAnswering()
        setupGraphs()
        registerSensors()
        startLocationUpdates()
        connectSignaling()
        infoSource.register(::buildInfo)
        
        isInitialized = true
        val bootTimeMs = System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime()
        eventLog.recordFrom { events -> unrecordedStopEvent(events, bootTimeMs, ::processExits) }
        eventLog.record(EventLevel.INFO, serviceStartedMessage(getAppVersionName(this)))
        val startMs = try { android.os.SystemClock.elapsedRealtime() } catch (_: Throwable) { System.currentTimeMillis() }
        serviceStatus.markRunning(startMs)
    }

    private fun connectSignaling() {
        serviceScope.launch {
            // A Sensor started at boot, before its first screen, still connects as `T` (ISSUE-NOV8.101-O59).
            identityManager.ensureIdentityGenerated(ForensicIdentity.PREFIX_SENSOR)
            combine(identityManager.selfId, sensorDataStore.relayUrl) { selfId, rawUrl ->
                val url = RelayConfig.resolve(rawUrl)
                selfId to url
            }.collect { (selfId, url) ->
                this@SensorService.selfId = selfId
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
                if (stored) eventLog.record(EventLevel.INFO, "Home points received: ${config.points.size}, radius ${config.radiusMeters.toInt()} m")
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
        
        if (hasRequiredSensorPermissions(this)) {
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

    /**
     * Applies what this start command obtained. Every start command re-checks, so opening the app or tapping the
     * restart notification restores what a background start was refused; a start command that is still refused posts the
     * notification again, so a tap that did not help leaves it in place (ISSUE-NOV8.103-O60).
     */
    private fun applyForegroundStart(start: ForegroundStart) {
        // Requested again once location is regained, so the updates resume at once.
        if (start.hasLocation && serviceStatus.refusedAccess.value.location) startLocationUpdates()
        if (!start.hasLocation) eventLog.record(EventLevel.WARNING, "Started in the background without location: tracking resumes after a notification tap or opening the Sensor app")
        hasForegroundStart = true
        isLocationRefused = !start.hasLocation
        hasMicrophoneType = start.hasMicrophone
        applyAccess()
    }

    /**
     * Main thread, after each start command and each change of the Monitor's noise switch (ISSUE-NOV10.8-O83): the
     * microphone is refused only while noise detection is on, so a switched-off microphone posts no tap-to-enable notification.
     */
    private fun applyAccess() {
        if (!hasForegroundStart || !isBehaviourRead) return // The first read of the stored behaviour applies it.
        val isNoiseOn = sensorBehaviour.noiseAlert
        val refused = RefusedAccess(location = isLocationRefused, microphone = isNoiseOn && !hasMicrophoneType && hasMicrophonePermission())
        serviceStatus.setRefusedAccess(refused)
        updateMicrophone(isNoiseOn, hasMicrophoneType)
        updateRestartNotification(refused)
    }

    /** Listens while noise detection is on and the foreground service holds the microphone type. */
    private fun updateMicrophone(isNoiseOn: Boolean, canUseMicrophone: Boolean) {
        when {
            !isNoiseOn -> {
                acousticMonitor.stop()
                serviceStatus.setMicrophone(MicrophoneState.TURNED_OFF)
            }
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

    private fun onAcousticAlert(levelDbfs: Double, referenceDbfs: Double) {
        queueAlert(ACOUSTIC_ALERT_STATUS, acousticAlertText(levelDbfs, referenceDbfs))
    }

    private fun queueAlert(status: String, logLine: String) {
        if (!sensorBehaviour.allows(status)) return // Switched off on the Monitor (ISSUE-NOV10.8-O83).
        if (!alertCoolDown.tryPass(status, SystemClock.elapsedRealtime())) {
            eventLog.record(EventLevel.DETAIL, "$logLine: not sent, the same kind alerted less than ${ALERT_COOL_DOWN_MS / 1000} s ago")
            return
        }
        pendingAlerts.add(PendingAlert(status, logLine))
        orchestrator.preemptLoop("sensing_loop") // Sent now, not after up to 10 s of a slow sampling interval.
    }

    /**
     * Stores each detection setting the Monitor sends, and applies the stored one, also after a restart while offline
     * (ISSUE-NOV10.8-O83): the Monitor's sliders set the tilt angle, the vibration limit and the noise jump, and its
     * switches stop the detection of a kind (user decisions 2026-11-15).
     */
    /**
     * A settings file imported while the service runs raises the sequence numbering at once (ISSUE-NOV10.8-O80,
     * ISSUE-NOV5.2-O16). Before the buffer's first number is read the start-up sets the counter, also from the floor.
     */
    private fun setupSequenceFloor() {
        sensorDataStore.sequenceFloor
            .distinctUntilChanged()
            .onEach { floor ->
                initJob?.join()
                if (floor > sequenceIdCounter.get()) {
                    sequenceIdCounter.accumulateAndGet(floor, ::maxOf)
                    serviceStatus.setNextSequence(sequenceIdCounter.get())
                    eventLog.record(EventLevel.INFO, "Sequence numbering continues at $floor (imported settings)")
                }
            }
            .launchIn(serviceScope)
    }

    private fun setupSensorBehaviourListener() {
        signalingClient.sensorBehaviourFlow
            .onEach { behaviour ->
                val stored = sensorDataStore.saveSensorBehaviour(behaviour)
                SystemLogger.i("SensorBehaviour", "Received rev ${behaviour.revision} (stored: $stored)")
                // A revision not stored comes from a Monitor that came online and sent what it holds: it gets the setup
                // report, also when another Monitor was already there. A stored one changes the report, which sends it.
                if (!stored) setupReport.value?.let { sendSetupReport(it) }
                // Every Monitor sends its settings when it comes online; one that joins while another Monitor keeps the
                // room's online status unchanged would otherwise not get the events (ISSUE-NOV10.8-O84).
                sendEvents(eventLog.events.value.newestForPeer())
            }
            .launchIn(serviceScope)
        sensorDataStore.sensorBehaviour
            .onEach { behaviour ->
                val previous = sensorBehaviour
                val isFirstRead = !isBehaviourRead
                sensorBehaviour = behaviour
                isBehaviourRead = true
                motionSentinel.tiltThresholdDegrees = tiltThresholdForSensitivity(behaviour.tiltSensitivity)
                motionSentinel.vibrationThreshold = vibrationThresholdForSensitivity(behaviour.vibrationSensitivity)
                acousticMonitor.jumpDb = noiseJumpForSensitivity(behaviour.noiseSensitivity)
                // Logged when the settings change, not on every start with the stored ones (ISSUE-NOV18.9-O121).
                if (!isFirstRead && behaviour.revision != previous.revision) {
                    eventLog.record(EventLevel.INFO, "Detection settings applied (rev ${behaviour.revision}): ${sensorBehaviourLines(behaviour).joinToString("; ")}")
                }
                if (isFirstRead || behaviour.noiseAlert != previous.noiseAlert) withContext(Dispatchers.Main) { applyAccess() }
            }
            .launchIn(serviceScope)
    }

    /**
     * Tells the Monitor what this phone's setup lacks and which detection settings are applied (ISSUE-NOV10.8-O83
     * part 2, ISSUE-NOV15.2-O97): the report is sent each time a Monitor comes online, and each time it changes while
     * this phone is connected to the relay, also with no Monitor online: the relay pushes a changed report to a Monitor
     * frozen in the background (ISSUE-NOV10.8-O86). Android announces no change of a permission or an exemption, so the
     * setup is read again every [SETUP_STATUS_CHECK_MS].
     */
    private fun setupSetupReporting() {
        val items = sensorSetupItems()
        val missingItems = flow {
            while (true) {
                emit(readSensorSetupStatus(this@SensorService).missingItems(items))
                delay(SETUP_STATUS_CHECK_MS)
            }
        }
        combine(missingItems, serviceStatus.microphone, serviceStatus.refusedAccess, sensorDataStore.sensorBehaviour) { missing, microphone, refused, behaviour ->
            sensorSetupReport(missing, microphone, refused, behaviour.revision)
        }
            .distinctUntilChanged()
            .onEach { setupReport.value = it }
            .launchIn(serviceScope)
        relayDeliveries(setupReport.filterNotNull(), signalingClient.isConnected, signalingClient.viewerStatusFlow)
            .onEach(::sendSetupReport)
            .launchIn(serviceScope)
        var previousReport: SensorSetupReport? = null
        setupReport.filterNotNull()
            .onEach { report ->
                setupChangeEvent(previousReport, report)?.let(::record)
                previousReport = report
            }
            .launchIn(serviceScope)
    }

    /**
     * Records the links to the relay and to the Monitors, and sends the newest events to the Monitors each time one comes
     * online and at each new event while one is (ISSUE-NOV10.8-O84).
     */
    private fun setupEventReporting() {
        signalingClient.isConnected.linkChanges().onEach { record(relayLinkEvent(it)) }.launchIn(serviceScope)
        signalingClient.viewerStatusFlow.linkChanges().onEach { record(monitorLinkEvent(it)) }.launchIn(serviceScope)
        signalingClient.viewerStatusFlow.onEach { isMonitorOnline = it }.launchIn(serviceScope)
        peerDeliveries(eventLog.events.map { it.newestForPeer() }.filter { it.events.isNotEmpty() }, signalingClient.viewerStatusFlow)
            .onEach(::sendEvents)
            .launchIn(serviceScope)
        // A Monitor that finds it missed events asks for all after the newest it has them up to (ISSUE-NOV15.4-O98 part 4).
        signalingClient.eventsRequestFlow
            .onEach { afterMs ->
                val chunks = eventLog.events.value.peerEventsAfter(afterMs)
                for (chunk in chunks) {
                    if (!signalingClient.sendSensorEvents(chunk.events, chunk.afterMs)) break
                }
                SystemLogger.i("SensorEvents", "Events request after $afterMs: ${chunks.sumOf { it.events.size }} events in ${chunks.size} messages")
            }
            .launchIn(serviceScope)
    }

    private suspend fun sendEvents(newest: PeerEvents) {
        if (newest.events.isEmpty()) return
        val sent = signalingClient.sendSensorEvents(newest.events, newest.afterMs)
        SystemLogger.i("SensorEvents", "${newest.events.size} newest events sent: $sent")
    }

    private fun record(event: LoggedEvent) = eventLog.record(event.level, event.message)

    /**
     * Sends this phone's Info to the Monitors every [SENSOR_INFO_SEND_INTERVAL_MS] for [SENSOR_INFO_ANSWER_WINDOW_MS] after
     * each request; an open Monitor Info screen asks again before the window ends (ISSUE-NOV10.8-O84 part 2). The
     * satellite list, most of the Info's size, goes with it only for [SENSOR_INFO_SATELLITE_WINDOW_MS] after a request
     * that asks for it, so one Monitor's open list is not ended by another Monitor's request (ISSUE-NOV15.5-O99 part 1).
     */
    private fun setupInfoAnswering() {
        serviceScope.launch {
            var satellitesUntilMs = 0L
            signalingClient.infoRequestFlow.collectLatest { wantsSatellites ->
                val requestedAtMs = SystemClock.elapsedRealtime()
                if (wantsSatellites) satellitesUntilMs = requestedAtMs + SENSOR_INFO_SATELLITE_WINDOW_MS
                val untilMs = requestedAtMs + SENSOR_INFO_ANSWER_WINDOW_MS
                while (SystemClock.elapsedRealtime() < untilMs) {
                    val info = buildInfo()
                    signalingClient.sendSensorInfo(if (SystemClock.elapsedRealtime() < satellitesUntilMs) info else info.withoutSatelliteList())
                    delay(SENSOR_INFO_SEND_INTERVAL_MS)
                }
            }
        }
    }

    /** This phone's Info now, for its own Info screen and the Monitors' (ISSUE-NOV10.8-O84 part 2). Any thread. */
    private fun buildInfo(): SensorInfo {
        val nowMs = System.currentTimeMillis()
        val nowElapsedMs = SystemClock.elapsedRealtime()
        val startedElapsedMs = serviceStatus.startTimeMillis.value
        val motion = serviceStatus.motion.value
        val microphone = serviceStatus.microphone.value
        val fix = lastLocation
        val home = geofence
        val hasAccelerometer = motion.isRunning
        val inView = satellites // one read, so the list and its summary agree
        val gnss = gnssSummary(inView)
        return SensorInfo(
            capturedAtMs = nowMs,
            sensorId = selfId,
            version = getAppVersionName(this),
            serviceStartedAtMs = if (startedElapsedMs > 0L) nowMs - (nowElapsedMs - startedElapsedMs) else 0L,
            motion = motionStatusText(motion, sensorBehaviour).substringAfter(": "),
            isMotionArmed = motion.isArmed && motion.isVibrationArmed,
            microphone = microphoneStatusText(microphone).substringAfter(": "),
            isMicrophoneFaulty = microphone.isNoiseDetectionOff,
            behaviourRevision = sensorBehaviour.revision,
            link = signalingClient.linkStats.value,
            isMonitorOnline = isMonitorOnline,
            queuedRows = signalingClient.queuedRowCount(),
            // The fix's age from the monotonic clock, as the GPS time may differ from the phone's clock.
            fixTimeMs = fix?.let { nowMs - (SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos) / 1_000_000L } ?: 0L,
            lat = fix?.latitude ?: 0.0,
            lng = fix?.longitude ?: 0.0,
            accuracyM = fix?.accuracy ?: 0f,
            speedMps = fix?.takeIf { it.hasSpeed() }?.speed,
            homeDistanceM = fix?.takeIf { home.isActive }?.let { loc ->
                home.points.minOf { SpatialMath.calculateDistance(loc.latitude, loc.longitude, it.lat, it.lng) }
            },
            homeRadiusM = home.takeIf { it.isActive }?.radiusMeters,
            satellites = inView,
            satellitesUsed = gnss.used,
            satellitesInView = gnss.inView,
            meanCn0DbHz = gnss.meanCn0DbHz,
            vibrationG = if (hasAccelerometer) (motionSentinel.vibration / STANDARD_GRAVITY).toFloat() else null,
            vibrationPeakG = if (hasAccelerometer) (motionSentinel.vibrationPeak / STANDARD_GRAVITY).toFloat() else null,
            vibrationLimitG = (motionSentinel.vibrationThreshold / STANDARD_GRAVITY).toFloat(),
            tiltDeg = motionSentinel.tiltDegrees?.toFloat(),
            tiltLimitDeg = motionSentinel.tiltThresholdDegrees.toFloat(),
            pressureHpa = motionSentinel.pressureHpa?.toFloat(),
            noiseDbfs = acousticMonitor.levelDbfs?.toFloat(),
            noiseFloorDbfs = acousticMonitor.floorDbfs?.toFloat(),
            lux = lux,
            phone = readPhoneState(this)
        )
    }

    /**
     * Takes one sample a minute for the Graphs, and answers each request of a Monitor's Graphs screen with the samples
     * newer than the newest it holds, oldest first, so a request cut short still leaves the Monitor without holes
     * (ISSUE-NOV10.8-O84 part 3).
     */
    private fun setupGraphs() {
        var previousLink: LinkStats? = null
        var previousMs = 0L
        graphSampleTimes()
            .onEach { timeMs ->
                val link = signalingClient.linkStats.value
                graphHistory.add(
                    sensorGraphSample(
                        timeMs = timeMs,
                        vibrationPeakMps2 = vibrationPeaks.take(),
                        tiltPeakDeg = tiltPeaks.take(),
                        noisePeakDbfs = acousticMonitor.levelPeaks.take(),
                        behaviour = sensorBehaviour,
                        vibrationLimitMps2 = motionSentinel.vibrationThreshold,
                        tiltLimitDeg = motionSentinel.tiltThresholdDegrees,
                        fixAccuracyM = lastLocation?.takeIf { isFixFresh(it.elapsedRealtimeNanos, SystemClock.elapsedRealtimeNanos()) }?.accuracy,
                        gnss = if (isGnssRegistered) gnssSummary(satellites) else null,
                        linkUpPercent = linkUpPercent(previousLink, previousMs, link, timeMs),
                        latencyMs = link.latencyMs,
                        queuedRows = signalingClient.queuedRowCount(),
                        batteryPercent = lastBatteryLevel,
                        isCharging = lastIsCharging,
                        temperatureC = lastTempCelsius
                    )
                )
                previousLink = link
                previousMs = timeMs
            }
            .launchIn(serviceScope)
        signalingClient.graphRequestFlow
            .onEach { request ->
                val newer = graphHistory.samples.value.filter { it.timeMs > request.afterMs }
                for (chunk in newer.chunked(GRAPH_SAMPLES_PER_MESSAGE)) {
                    if (!signalingClient.sendGraphSamples(chunk, request.compressed)) break
                }
                // The starts and stops among these samples, also those older than the newest events a Monitor has
                // (ISSUE-NOV15.6-O100 part 7); usually none, as the run that began before the request goes on.
                val lifecycle = eventLog.events.value.serviceLifecycleSince(request.afterMs)
                for (chunk in lifecycle.chunked(EventLog.PEER_EVENTS_SENT)) {
                    if (!signalingClient.sendSensorEvents(chunk)) break
                }
                val form = if (request.compressed) "compressed" else "plain"
                SystemLogger.i("SensorGraphs", "Graph request after ${request.afterMs}: ${newer.size} samples sent ($form), ${lifecycle.size} start and stop events")
            }
            .launchIn(serviceScope)
    }

    private suspend fun sendSetupReport(report: SensorSetupReport) {
        val sent = signalingClient.sendSensorSetup(report)
        SystemLogger.i(
            "SensorSetup",
            "Report sent: $sent (Monitor online: $isMonitorOnline, missing ${report.missing}, faults ${report.faults}, behaviour rev ${report.behaviourRevision})"
        )
    }

    /**
     * The accelerometer runs at about 50 Hz (`SENSOR_DELAY_GAME`), which the lift integration needs; the barometer, where
     * there is one, at the normal rate. Events arrive on their own thread, not the main one.
     */
    private fun registerSensors() {
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        val pressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        val light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) // Info only (ISSUE-NOV10.8-O84 part 2); reports on change.
        hasGravitySensor = gravity != null
        hasBarometer = pressure != null
        val handler = Handler(HandlerThread("MotionSensors").also { it.start(); motionThread = it }.looper)

        accel?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME, handler) }
        gravity?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME, handler) }
        pressure?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL, handler) }
        light?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL, handler) }
        ownVibrationWatch = OwnVibrationWatch(this) { isOwn ->
            motionSentinel.isOwnVibrationActive = isOwn
            acousticMonitor.isOwnSoundActive = isOwn
            SystemLogger.i("Motion", if (isOwn) "Phone ringing or notifying: vibration and noise ignored" else "Phone quiet: vibration and noise detected again")
        }.also { it.start(serviceScope, handler) }
        SystemLogger.i(
            "Motion",
            if (SilentNotificationListener.isGranted(this)) "Notification access granted: silent notifications are the phone's own vibration"
            else "Notification access not granted: a silent notification's vibration can count as a knock at the highest settings"
        )
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
        registerGnssStatus()
    }

    /** The satellites for the Info screens and the rows (ISSUE-NOV10.8-O84 part 2); once per service. Main thread. */
    private fun registerGnssStatus() {
        if (isGnssRegistered) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        val locationManager = getSystemService(LocationManager::class.java) ?: return
        try {
            isGnssRegistered = LocationManagerCompat.registerGnssStatusCallback(locationManager, ContextCompat.getMainExecutor(this), gnssCallback)
        } catch (e: SecurityException) {
            SystemLogger.e("Location", "GNSS status refused: ${e.message}")
        }
        if (!isGnssRegistered) SystemLogger.w("Location", "No GNSS status: the Info shows no satellites")
    }

    private fun refreshSamplingRate() {
        val speed = if (lastLocation != null && lastLocation!!.hasSpeed()) lastLocation!!.speed else 0f
        val newInterval = SensingEngine.getOptimalIntervalMs(
            currentTemp = lastTempCelsius,
            isCharging = lastIsPluggedIn, // On outside power the fast rate costs no battery (ISSUE-NOV17.15-O114).
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
                vibrationPeaks.offer(motionSentinel.vibration)
                motionSentinel.tiltDegrees?.let(tiltPeaks::offer)
                if (motionSentinel.isArmed != publishedMotionArmed) {
                    publishedMotionArmed = motionSentinel.isArmed
                    publishMotion()
                    eventLog.record(EventLevel.DETAIL, if (motionSentinel.isArmed) "Tilt and lift armed" else "Tilt and lift waiting for the phone to rest")
                }
                if (motionSentinel.isVibrationArmed != publishedVibrationArmed) {
                    publishedVibrationArmed = motionSentinel.isVibrationArmed
                    publishMotion()
                    eventLog.record(EventLevel.DETAIL, if (motionSentinel.isVibrationArmed) "Vibration armed" else "Vibration waiting for the phone to rest")
                }
            }
            Sensor.TYPE_GRAVITY -> {
                lastGravity[0] = event.values[0].toDouble()
                lastGravity[1] = event.values[1].toDouble()
                lastGravity[2] = event.values[2].toDouble()
            }
            Sensor.TYPE_PRESSURE -> motionSentinel.onPressure(event.values[0].toDouble(), eventMs)?.let(::onMotionAlert)
            Sensor.TYPE_LIGHT -> lux = event.values[0]
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
                serviceStatus.setNextSequence(update.sequenceId + 1) // For the settings file (ISSUE-NOV10.8-O80).
                update.status = "VALID"
                update.batteryLevel = lastBatteryLevel
                update.isCharging = lastIsCharging
                update.isPluggedIn = lastIsPluggedIn
                update.tempCelsius = lastTempCelsius
                update.isNoiseDetectionOff = serviceStatus.microphone.value.isNoiseDetectionOff // ISSUE-NOV8.103-O60
                // For the Monitor's Info now and the graphs later (ISSUE-NOV10.8-O84 part 2); unknown without GNSS status.
                if (isGnssRegistered) {
                    val gnss = gnssSummary(satellites)
                    update.satsUsed = gnss.used
                    update.satsInView = gnss.inView
                    update.meanCn0DbHz = gnss.meanCn0DbHz
                }
                update.noiseDbfs = acousticMonitor.levelDbfs?.toFloat()
                
                val hasPhysicalMotion = update.sensorSnapshot.vibration > 0.12f
                // A fix older than FIX_STALE_AFTER_MS is no longer the current position: send NO_FIX without one.
                val currentFix = lastLocation?.takeIf { isFixFresh(it.elapsedRealtimeNanos, SystemClock.elapsedRealtimeNanos()) }
                if (currentFix == null) update.status = NO_FIX_STATUS
                if ((currentFix == null) != isReportingNoFix) {
                    isReportingNoFix = currentFix == null
                    if (isReportingNoFix) {
                        eventLog.record(EventLevel.WARNING, "No current GPS fix (none, or older than ${FIX_STALE_AFTER_MS / 1000} s)")
                    } else {
                        eventLog.record(EventLevel.INFO, "GPS fix current again")
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
        eventLog.record(EventLevel.ALARM, logLine)
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
                eventLog.record(EventLevel.WARNING, "Sensing loop stalled for ${elapsedMs / 1000} s: restarted")
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
        private const val SETUP_STATUS_CHECK_MS = 30_000L
    }

    /** The Sensor process's recent ends as Android recorded them (API 30+), for [unrecordedStopEvent] (ISSUE-NOV15.4-O98). */
    private fun processExits(): List<ProcessExit> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptyList()
        return getSystemService(ActivityManager::class.java)
            ?.getHistoricalProcessExitReasons(packageName, 0, 16).orEmpty()
            .filter { it.processName == packageName }
            .map { ProcessExit(it.timestamp, it.reason) }
    }

    override fun onDestroy() {
        if (isInitialized) eventLog.record(EventLevel.WARNING, SERVICE_STOPPED)
        else SystemLogger.i("SensorService", "onDestroy before initialization")
        infoSource.unregister()
        acousticMonitor.stop()
        getSystemService(NotificationManager::class.java).cancel(RESTART_NOTIFICATION_ID)
        serviceScope.cancel()
        orchestrator.cancelAll()
        fixStore.clear()
        serviceStatus.markStopped()
        if (isInitialized) {
            sensorManager.unregisterListener(this)
            ownVibrationWatch?.stop()
            motionThread?.quitSafely()
            fusedLocationClient.removeLocationUpdates(locationCallback)
            if (isGnssRegistered) getSystemService(LocationManager::class.java)?.let { LocationManagerCompat.unregisterGnssStatusCallback(it, gnssCallback) }
            try { unregisterReceiver(batteryReceiver) } catch (e: Exception) {}
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                thermalListener?.let { powerManager.removeThermalStatusListener(it) }
            }
        }
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }
}
