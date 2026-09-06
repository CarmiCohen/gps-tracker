package com.gps19.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.display.DisplayManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.*
import android.view.Display
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * HardwareProvider: Unified authority for all device hardware (GNSS, Location, Sensors, Audio, Display).
 * Sep.06.31:
 * - Issue #927 RESOLVED: Safe-Mode Integration. implemented isSafeMode check in 
 *   checkRevivalLifecycle to prevent revival loops during recovery (R-ID 271).
 *   Fixed logic inversion in permission check and added job cancellation.
 * Sep.06.30:
 * - Issue #925 RESOLVED: Async Teardown Race Condition. Converted start() to 
 *   suspend and implemented join() on teardownJob to ensure deterministic 
 *   initialization after rapid stop/start sequences (R925).
 * Sep.06.20:
 * - Issue #924 (Part B): A15 Resource Throttling. Implemented dynamic GNSS throttling 
 *   (5000ms) triggered by High Load or MaliAnomaly on budget hardware (R-ID 267).
 */
@Singleton
class HardwareProvider @Inject constructor(
    @ShadowContext private val shadowContext: Context,
    @ApplicationScope private val scope: CoroutineScope,
    private val timeProvider: TimeProvider,
    private val systemMonitor: SystemMonitor,
    private val systemStatusProvider: SystemStatusProvider,
    private val forensicAuditor: ForensicAuditor
) : ManagedSensorListener() {

    private val locationManager by lazy { shadowContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(shadowContext) }
    private val sensorManager by lazy { shadowContext.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager }
    private val displayManager by lazy { shadowContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager }

    private var hardwareThread: HandlerThread? = null
    private var hardwareHandler: Handler? = null
    private val lifecycleLock = Any()
    private val isStarted = AtomicBoolean(false)
    private val isTeardownActive = AtomicBoolean(false)

    // --- GPS & GNSS State ---
    private var revivalCallback: ManagedLocationCallback? = null
    private var rawRevivalListener: ManagedLocationListener? = null
    private var activeLocationCallback: ManagedLocationCallback? = null
    var satellitesInView = 0; private set
    var satellitesUsed = 0; private set
    var averageSnr = 0.0; private set
    private var lastFixRt = 0L
    private var lastGnssEmitRt = 0L
    private var pendingEnterRt = 0L
    private var recoveryStartRt = 0L
    private val pollingIntervalFlow = MutableStateFlow(TICK_INTERVAL_MS)
    private var revivalAttemptCount = 0
    private var isHardwareLocked = false

    val maxGnssJitterMs get() = forensicAuditor.maxGnssJitterMs

    private val _revivalEvents = MutableSharedFlow<RevivalEvent>(extraBufferCapacity = 8)
    val revivalEvents = _revivalEvents.asSharedFlow()

    sealed class RevivalEvent {
        data class Attempt(val count: Int) : RevivalEvent()
        object HardwareLock : RevivalEvent()
        object Success : RevivalEvent()
        object RawBurstStarted : RevivalEvent()
        object RawBurstEnded : RevivalEvent()
        data class Footprint(val deltaMa: Int, val deltaTemp: Double, val durationMs: Long) : RevivalEvent()
    }

    data class LocationStatus(
        val isPending: Boolean = false,
        val reason: LocationPendingReason = LocationPendingReason.NONE,
        val lastFixRt: Long = 0L,
        val lastPendingDurationMs: Long = 0L,
        val recoveryConfirmed: Boolean = false
    )

    private val _locationStatus = MutableStateFlow(LocationStatus())
    val locationStatusFlow: StateFlow<LocationStatus> = _locationStatus.asStateFlow()

    private val snrBuffer = CircularStateBuffer(512, { EngineSnrSample() }, { it.ts = 0L; it.rt = 0L; it.snr = 0.0 })

    private val _internalGpsFlow = MutableSharedFlow<GpsUpdate>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private sealed class GpsUpdate {
        data class LocationUpdate(val location: Location) : GpsUpdate()
        data class GnssUpdate(val detail: GnssDetail) : GpsUpdate()
    }

    // --- Sensor State ---
    private val _sensorEvents = MutableSharedFlow<AppSensorEvent>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val sensorEvents: SharedFlow<AppSensorEvent> = _sensorEvents.asSharedFlow()

    private val accelerometer by lazy { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    private val linearAccel by lazy { sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) }
    private val magnetometer by lazy { sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) }
    private val barometer by lazy { sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) }
    private val proximity by lazy { sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) }
    private val light by lazy { sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) }
    private val rotationVector by lazy { sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
    private val stepDetector by lazy { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) }

    private var isStepDetectorRegistered = false
    private var recoveryJob: Job? = null
    private var registrationJob: Job? = null
    private var teardownJob: Job? = null
    private var revivalPulseJob: Job? = null
    private var lastDisplayState = Display.STATE_UNKNOWN
    private var lastDisplayTransitionRt = 0L
    private val isDisplayFlickering = AtomicBoolean(false)
    private var lastStayAliveRt = 0L

    private val gravityBuffer = FloatArray(3)
    private val geomagneticBuffer = FloatArray(3)
    private val rotationMatrixBuffer = FloatArray(9)
    private val inclinationMatrixBuffer = FloatArray(9)
    private val orientationBuffer = FloatArray(3)
    private val currentRotationVectorMatrixBuffer = FloatArray(9)
    private var hasGravity = false
    private var hasGeomagnetic = false
    private var lastAccelX = 0f; private var lastAccelY = 0f; private var lastAccelZ = 0f
    private val vibrationCircularBuffer = DoubleArray(VIBRATION_WINDOW_SIZE)
    private var vibrationCircularIdx = 0
    var vibrationRollingSum = 0.0; private set
    private var vibrationBufferCount = 0

    private var logicPeakDb = 0.0; private var logicMinDb = 100.0; private var logicPeakVibration = 0.0
    private var logicPeakVerticalVelocity = 0.0; private var logicPeakVerticalVelocityTs = 0L; private var logicPeakVerticalVelocityRt = 0L; private var logicPeakVerticalDisplacement = 0.0
    private var forensicPeakDb = 0.0; private var forensicMinDb = 100.0; private var forensicPeakVibration = 0.0
    private var forensicPeakVerticalVelocity = 0.0; private var forensicPeakVerticalVelocityTs = 0L; private var forensicPeakVerticalVelocityRt = 0L; private var forensicPeakVerticalDisplacement = 0.0

    private var lastRawVibe = 0.0; private var lastHpfValue = 0.0; var currentKineticEnergy = 0.0; private set

    @Volatile private var isMonitoring = false
    @Volatile private var isAcousticRunning = false
    private var acousticThread: Thread? = null
    @Volatile private var isHighLoad = false
    @Volatile private var maliAnomaly = false
    @Volatile private var powerSaveMode = false
    @Volatile private var isSafeMode = false

    private val logicSnapshotBuffer = CircularStateBuffer(2, { ForensicSnapshot() }, { it.reset() })
    private val forensicSnapshotBuffer = CircularStateBuffer(4, { ForensicSnapshot() }, { it.reset() })

    private val sensorBuffer = CircularStateBuffer(256, { EngineSensorSnapshot() }, {
        it.ts = 0L; it.rt = 0L; it.lux = 0.0; it.vibe = 0.0; it.proxIdx = 0.0; it.lift = 0.0; it.tilt = 0.0; it.isSitDetected = false; it.sitVzTs = 0L; it.sitVzRt = 0L; it.sitShock = 0.0; it.kineticEnergy = 0.0
    })
    private var lastBufferRecordRt = 0L

    private var secPeakLux = 0.0; private var secPeakVibe = 0.0; private var secSumProxIdx = 0.0; private var secProxCount = 0
    private var secPeakTilt = 0.0; private var secPeakLift = 0.0; private var secPeakDb = 0.0; private var secSitDetected = false; private var secPeakKinetic = 0.0
    
    private var fastPathFloor = -1.0; private var fastPathSpikeThreshold = ACOUSTIC_THRESHOLD_DB_JUMP
    private var fastPathMinDb = ACOUSTIC_MIN_THRESHOLD_DB; private var onAcousticSpike: (() -> Unit)? = null
    private var fastPathLightBaseline = -1.0; private var fastPathLightSpikeThreshold = LIGHT_THRESHOLD_LUX_JUMP; private var onLightSpike: (() -> Unit)? = null
    private var lastAcousticSpikeRt = 0L; private var lastLightSpikeRt = 0L

    var lastAcousticLockoutRt = 0L; private set
    private var sessionStartRt = 0L
    val isWarming get() = (timeProvider.elapsedRealtime() - sessionStartRt < SENSOR_WARMING_MS)

    var currentVibrationIndex = 0.0; private set
    var adaptiveVibrationFloor = VIBRATION_STATIONARY_THRESHOLD; private set
    var currentCompassHeading = 0.0; private set
    var currentPressure = 0.0; private set
    var absoluteAltitude = 0.0; private set
    var relativeAltitude = 0.0; private set

    private var proximityJob: Job? = null
    private var rawProximityNear = false; private var proximityMaxRange = 5f
    var isProximityNear = false; private set
    var proximityIdx = 0.0; private set
    var currentProximityCm = -1.0; private set
    var debouncedProximityCm = -1.0; private set
    var proximityDebounceMs = 0L; private set
    var currentLux = 0.0; private set
    var currentTiltDegrees = 0.0; private set
    var currentAcousticDb = 0.0; private set
    var currentVerticalVelocity = 0.0; private set
    var currentVerticalDisplacement = 0.0; private set

    private var lastLinearAccelTs = 0L; private var stationaryStartRt = 0L
    private var emaPressure = 0.0; private var lastBaroZeroingRt = 0L
    private var initialRotationMatrix = FloatArray(9); private var hasInitialRotation = false
    private var plungePhase = 0; private var plungeMatched = false; private var lastPlungePhaseRt = 0L

    private val _isUltraLongStationary = MutableStateFlow(false)
    val isUltraLongStationaryFlow: StateFlow<Boolean> = _isUltraLongStationary.asStateFlow()

    class ForensicSnapshot {
        var vibration = 0.0; var heading = 0.0; var baroAlt = 0.0; var lux = 0.0
        var isNear = true; var tiltDegrees = 0.0; var acousticDb = 0.0; var peakShock = 0.0
        var peakVerticalVelocity = 0.0; var peakVerticalVelocityTs = 0L; var peakVerticalVelocityRt = 0L
        var plungeMatched = false; var peakVerticalDisplacement = 0.0; var proximityIdx = 0.0
        var proximityCm = 0.0; var proximityDebounceMs = 0L; var vibrationRollingSum = 0.0
        var acousticPeak = 0.0; var acousticPeakMin = 0.0; var kineticEnergy = 0.0

        fun reset() {
            vibration = 0.0; heading = 0.0; baroAlt = 0.0; lux = 0.0
            isNear = true; tiltDegrees = 0.0; acousticDb = 0.0; peakShock = 0.0
            peakVerticalVelocity = 0.0; peakVerticalVelocityTs = 0L; peakVerticalVelocityRt = 0L
            plungeMatched = false; peakVerticalDisplacement = 0.0; proximityIdx = 0.0
            proximityCm = 0.0; proximityDebounceMs = 0L; vibrationRollingSum = 0.0
            acousticPeak = 0.0; acousticPeakMin = 0.0; kineticEnergy = 0.0
        }
    }

    private val gnssStatusCallback = object : ManagedGnssStatusCallback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            if (isTeardownActive.get()) return
            val nowRt = timeProvider.elapsedRealtime()
            forensicAuditor.recordGnssStatus(nowRt)
            
            satellitesInView = status.satelliteCount
            var used = 0; var snrSum = 0.0; var snrCount = 0
            for (i in 0 until status.satelliteCount) {
                if (status.usedInFix(i)) used++
                val snr = status.getCn0DbHz(i).toDouble()
                if (snr > 0.0) { snrSum += snr; snrCount++ }
            }
            satellitesUsed = used; averageSnr = if (snrCount > 0) snrSum / snrCount else 0.0
            val now = timeProvider.currentTimeMillis()
            
            synchronized(snrBuffer) {
                snrBuffer.next().apply {
                    this.ts = now
                    this.rt = nowRt
                    this.snr = averageSnr
                }
            }
            
            // Issue #924: Dynamic GNSS Throttling based on A15 and High Load/MaliAnomaly
            val currentInterval = if (systemStatusProvider.isA15Hardware() && (isHighLoad || maliAnomaly)) {
                GNSS_SAMPLING_INTERVAL_THROTTLED_MS
            } else {
                GNSS_SAMPLING_INTERVAL_MS
            }

            if (nowRt - lastGnssEmitRt >= currentInterval) {
                lastGnssEmitRt = nowRt
                val satList = mutableListOf<SatelliteInfo>()
                for (i in 0 until status.satelliteCount) {
                    satList.add(SatelliteInfo(svid = status.getSvid(i), cn0 = status.getCn0DbHz(i).toDouble(), usedInFix = status.usedInFix(i), constellation = status.getConstellationType(i)))
                }
                _internalGpsFlow.tryEmit(GpsUpdate.GnssUpdate(GnssDetail(satellites = satList.sortedByDescending { it.cn0 })))
            }
            updateLocationStatus()
        }
    }

    private val displayListener = object : ManagedDisplayListener() {
        override fun onDisplayChanged(displayId: Int) {
            if (isTeardownActive.get() || displayId != Display.DEFAULT_DISPLAY) return
            val display = displayManager.getDisplay(displayId) ?: return
            val newState = display.state
            if (newState != lastDisplayState) {
                val nowRt = timeProvider.elapsedRealtime()
                if (nowRt - lastDisplayTransitionRt < 1000L) {
                    if (!isDisplayFlickering.get()) { isDisplayFlickering.set(true); Timber.w("Forensic: Rapid Display Flickering detected.") }
                } else { isDisplayFlickering.set(false) }
                lastDisplayState = newState; lastDisplayTransitionRt = nowRt
            }
        }
    }

    init {
        scope.launch {
            while (isActive) {
                updateLocationStatus()
                checkRevivalLifecycle()
                updateStationaryExposure()
                delay(2000L)
            }
        }
    }

    suspend fun start() {
        // Issue #925: Await completion of any active teardown before re-initializing.
        teardownJob?.let {
            Timber.i("HardwareProvider: Awaiting active teardown completion (Issue #925).")
            it.join()
        }

        synchronized(lifecycleLock) {
            if (isStarted.getAndSet(true)) {
                isTeardownActive.set(false)
                return
            }
            
            isTeardownActive.set(false)
            sessionStartRt = timeProvider.elapsedRealtime(); lastBaroZeroingRt = sessionStartRt
            proximityMaxRange = proximity?.maximumRange ?: 5f
            
            if (hardwareThread == null || !hardwareThread!!.isAlive) {
                hardwareThread = HandlerThread("HardwareProviderThread").apply { start() }
                hardwareHandler = Handler(hardwareThread!!.looper)
                Timber.d("HardwareProvider: Unified hardware thread started.")
            }
            
            val handler = hardwareHandler
            if (handler != null && ContextCompat.checkSelfPermission(shadowContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                try {
                    locationManager.registerGnssStatusCallback(gnssStatusCallback, handler)
                    Timber.d("HardwareProvider: GNSS callback registered.")
                } catch (e: Exception) { Timber.e(e, "HardwareProvider: GNSS registration failed") }
            }

            displayManager.registerDisplayListener(displayListener, handler)
            val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            if (display != null) lastDisplayState = display.state

            registerSensors()
            startAcousticMonitoring()
        }
    }

    fun stop() {
        synchronized(lifecycleLock) {
            if (!isStarted.getAndSet(false)) return
            isTeardownActive.set(true)
            
            forensicAuditor.clearRevivalState()
            
            val stopStartTime = SystemClock.elapsedRealtime()
            Timber.i("HardwareProvider: Starting teardown sequence (R891/R908).")
            
            recoveryJob?.cancel(); recoveryJob = null
            registrationJob?.cancel(); registrationJob = null
            proximityJob?.cancel(); proximityJob = null
            revivalPulseJob?.cancel(); revivalPulseJob = null
            
            Timber.d("HardwareProvider: Stopping Acoustic Monitoring...")
            stopAcousticMonitoring()

            val handler = hardwareHandler
            val threadToQuit = hardwareThread
            
            val gnssStart = SystemClock.elapsedRealtime()
            Timber.d("HardwareProvider: Unregistering GNSS status callback...")
            try { gnssStatusCallback.unregister(locationManager, handler) } catch (e: Exception) { Timber.e(e, "GNSS status unregistration failed") }
            val gnssDuration = SystemClock.elapsedRealtime() - gnssStart
            
            val locStart = SystemClock.elapsedRealtime()
            Timber.d("HardwareProvider: Unregistering active/revival location callbacks...")
            activeLocationCallback?.unregister(fusedLocationClient, handler); activeLocationCallback = null
            revivalCallback?.unregister(fusedLocationClient, handler); revivalCallback = null
            rawRevivalListener?.unregister(locationManager, handler); rawRevivalListener = null
            val locDuration = SystemClock.elapsedRealtime() - locStart
            
            val sensorStart = SystemClock.elapsedRealtime()
            Timber.d("HardwareProvider: Unregistering all sensors...")
            this.unregister(sensorManager, handler)
            val sensorDuration = SystemClock.elapsedRealtime() - sensorStart
            
            val displayStart = SystemClock.elapsedRealtime()
            Timber.d("HardwareProvider: Unregistering display listener...")
            displayListener.unregister(displayManager, handler)
            val displayDuration = SystemClock.elapsedRealtime() - displayStart

            teardownJob?.cancel()
            teardownJob = scope.launch(Dispatchers.IO) {
                Timber.d("HardwareProvider: Entering 800ms forensic settling window (R908)...")
                delay(800)

                synchronized(lifecycleLock) {
                    if (!isStarted.get() && hardwareThread == threadToQuit) {
                        Timber.d("HardwareProvider: Quitting hardware thread...")
                        threadToQuit?.quitSafely()
                        try { 
                            threadToQuit?.join(1000)
                        } catch (e: InterruptedException) { Thread.currentThread().interrupt() }
                        hardwareThread = null; hardwareHandler = null
                    } else {
                        Timber.i("HardwareProvider: Teardown interrupted by restart. Retaining thread.")
                    }
                    teardownJob = null
                }
                
                val totalDuration = SystemClock.elapsedRealtime() - stopStartTime
                Timber.i("""
                    HardwareProvider: Teardown Summary (Issue #908 Verification):
                    - Total Teardown Time: ${totalDuration}ms
                    - GNSS Duration: ${gnssDuration}ms
                    - Location Duration: ${locDuration}ms
                    - Sensor Duration: ${sensorDuration}ms
                    - Display Duration: ${displayDuration}ms
                    - Settling Window: 800ms (Async)
                    - Status: Clean Teardown Completed.
                """.trimIndent())
            }
            
            isStepDetectorRegistered = false
        }
    }

    private fun registerSensors() {
        val delay = if (powerSaveMode) android.hardware.SensorManager.SENSOR_DELAY_NORMAL else android.hardware.SensorManager.SENSOR_DELAY_FASTEST
        accelerometer?.let { sensorManager.registerListener(this, it, delay, hardwareHandler) }
        linearAccel?.let { sensorManager.registerListener(this, it, delay, hardwareHandler) }
        magnetometer?.let { sensorManager.registerListener(this, it, android.hardware.SensorManager.SENSOR_DELAY_NORMAL, hardwareHandler) }
        barometer?.let { sensorManager.registerListener(this, it, android.hardware.SensorManager.SENSOR_DELAY_NORMAL, hardwareHandler) }
        proximity?.let { sensorManager.registerListener(this, it, android.hardware.SensorManager.SENSOR_DELAY_NORMAL, hardwareHandler) }
        light?.let { sensorManager.registerListener(this, it, android.hardware.SensorManager.SENSOR_DELAY_NORMAL, hardwareHandler) }
        rotationVector?.let { sensorManager.registerListener(this, it, delay, hardwareHandler) }
        attemptStepDetectorRegistration(); startStepDetectorRecoveryLoop()
    }

    private fun restartLocationUpdates() {
        if (!isStarted.get() || isSafeMode || ContextCompat.checkSelfPermission(shadowContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        
        revivalPulseJob?.cancel()
        revivalPulseJob = scope.launch(Dispatchers.Default) {
            synchronized(lifecycleLock) {
                if (!isStarted.get()) return@synchronized
                val handler = hardwareHandler ?: return@synchronized
                
                revivalCallback?.unregister(fusedLocationClient, handler)
                val callback = object : ManagedLocationCallback() { override fun onLocationResult(p0: LocationResult) {} }
                revivalCallback = callback
                val fastRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L).setMaxUpdates(5).build()
                try { fusedLocationClient.requestLocationUpdates(fastRequest, callback, handler.looper) } catch (e: Exception) { Timber.e(e, "HardwareProvider: Fused revival pulse failed") }

                rawRevivalListener?.unregister(locationManager, handler)
                val rawListener = object : ManagedLocationListener() { override fun onLocationChanged(location: Location) {} }
                rawRevivalListener = rawListener
                try {
                    _revivalEvents.tryEmit(RevivalEvent.RawBurstStarted)
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, rawListener, handler.looper)
                    scope.launch {
                        delay(10000)
                        synchronized(lifecycleLock) { 
                            if (rawRevivalListener == rawListener) { 
                                rawListener.unregister(locationManager, handler)
                                rawRevivalListener = null 
                                _revivalEvents.tryEmit(RevivalEvent.RawBurstEnded)
                            } 
                        }
                    }
                } catch (e: Exception) { 
                    Timber.e(e, "HardwareProvider: Raw GPS bypass failed") 
                    _revivalEvents.tryEmit(RevivalEvent.RawBurstEnded)
                }
            }
        }
    }

    private fun updateLocationStatus() {
        val nowRt = timeProvider.elapsedRealtime()
        val deltaSinceFix = if (lastFixRt > 0) nowRt - lastFixRt else nowRt
        var shouldEmitSuccess = false
        
        _locationStatus.update { current ->
            var nextPending = current.isPending; var nextReason = current.reason
            var recoveryConfirmed = current.recoveryConfirmed; var lastPendingDuration = current.lastPendingDurationMs
            if (deltaSinceFix > GPS_GAP_THRESHOLD_MS) {
                if (!nextPending) { pendingEnterRt = nowRt; nextPending = true; recoveryConfirmed = false }
                nextReason = when { satellitesInView == 0 -> LocationPendingReason.SIGNAL_LOSS; satellitesInView >= 4 && satellitesUsed < 4 -> LocationPendingReason.GPS_STALL; else -> LocationPendingReason.GPS_GAP }
                recoveryStartRt = 0L 
            } else if (nextPending) {
                if (recoveryStartRt == 0L) recoveryStartRt = nowRt
                val recoveryDuration = nowRt - recoveryStartRt
                if (recoveryDuration < LOCATION_RECOVERY_DEBOUNCE_MS) { if (nowRt - pendingEnterRt > 0) lastPendingDuration = nowRt - pendingEnterRt; nextReason = LocationPendingReason.NONE } 
                else { 
                    nextPending = false; nextReason = LocationPendingReason.NONE; recoveryConfirmed = true; recoveryStartRt = 0L 
                    shouldEmitSuccess = true
                    isHardwareLocked = false
                }
            } else { recoveryConfirmed = false; recoveryStartRt = 0L }
            current.copy(isPending = nextPending, reason = nextReason, lastFixRt = lastFixRt, lastPendingDurationMs = lastPendingDuration, recoveryConfirmed = recoveryConfirmed)
        }
        
        if (shouldEmitSuccess) {
            _revivalEvents.tryEmit(RevivalEvent.Success)
            forensicAuditor.computeEnergyFootprint(nowRt)?.let { _revivalEvents.tryEmit(it) }
        }
    }

    private fun updateStationaryExposure() {
        val nowRt = timeProvider.elapsedRealtime()
        val duration = if (stationaryStartRt > 0L) nowRt - stationaryStartRt else 0L
        val isUltra = isStationary() && duration > ULTRA_LONG_STATIONARY_DURATION_MS
        if (_isUltraLongStationary.value != isUltra) {
            _isUltraLongStationary.value = isUltra
            Timber.i("HardwareProvider: Ultra-Long Stationary State changed to $isUltra")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @SuppressLint("MissingPermission")
    private val hardwareObservationFlow = pollingIntervalFlow.flatMapLatest { interval ->
        callbackFlow<GpsUpdate> {
            start() 
            fusedLocationClient.lastLocation.addOnSuccessListener { loc -> if (loc != null) { lastFixRt = timeProvider.elapsedRealtime(); trySend(GpsUpdate.LocationUpdate(loc)); updateLocationStatus() } }
            val fusedCallback = object : ManagedLocationCallback() { override fun onLocationResult(result: LocationResult) { result.lastLocation?.let { lastFixRt = timeProvider.elapsedRealtime(); trySend(GpsUpdate.LocationUpdate(it)); updateLocationStatus() } } }
            val handler = synchronized(lifecycleLock) { hardwareHandler }
            synchronized(lifecycleLock) { activeLocationCallback?.unregister(fusedLocationClient, handler); activeLocationCallback = fusedCallback }
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval).setMinUpdateIntervalMillis(interval / 2).build()
            
            try { fusedLocationClient.requestLocationUpdates(request, fusedCallback, handler?.looper ?: Looper.getMainLooper()) } catch (e: Exception) { close(e) }
            
            val internalJob = _internalGpsFlow.onEach { trySend(it) }.launchIn(this)
            awaitClose { internalJob.cancel(); synchronized(lifecycleLock) { if (activeLocationCallback == fusedCallback) activeLocationCallback = null }; fusedCallback.unregister(fusedLocationClient, handler) }
        }
    }.shareIn(scope = scope, started = SharingStarted.WhileSubscribed(5000), replay = 1)

    fun getLocationFlow(): Flow<Location> = hardwareObservationFlow.filterIsInstance<GpsUpdate.LocationUpdate>().map { it.location }
    val gnssDetailFlow: Flow<GnssDetail?> = hardwareObservationFlow.filterIsInstance<GpsUpdate.GnssUpdate>().map { it.detail }

    fun setPollingInterval(intervalMs: Long) { if (pollingIntervalFlow.value != intervalMs) pollingIntervalFlow.value = intervalMs }
    fun resetGnssJitter() { forensicAuditor.resetGnssJitter() }

    fun getSnrSamples(fromRt: Long, toRt: Long): Sequence<EngineSnrSample> = sequence {
        val flyweight = EngineSnrSample()
        val snapshot = synchronized(snrBuffer) { snrBuffer.asSequence().toList() }
        for (sample in snapshot) {
            if (sample.rt in fromRt..toRt) {
                flyweight.ts = sample.ts
                flyweight.rt = sample.rt
                flyweight.snr = sample.snr
                yield(flyweight)
            }
        }
    }

    fun isScreenOn(): Boolean {
        if (lastDisplayState == Display.STATE_UNKNOWN) {
            val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            if (display != null) lastDisplayState = display.state
        }
        return lastDisplayState == Display.STATE_ON
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (isTeardownActive.get()) return
        val nowRt = timeProvider.elapsedRealtime(); val wallNow = timeProvider.currentTimeMillis(); val values = event.values
        when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> lastStayAliveRt = nowRt
            Sensor.TYPE_ACCELEROMETER -> {
                gravityBuffer[0] = values[0]; gravityBuffer[1] = values[1]; gravityBuffer[2] = values[2]; hasGravity = true
                processVibration(values[0], values[1], values[2]); updateOrientation()
                if (!isStepDetectorRegistered && nowRt - lastStayAliveRt > 10000L) { lastStayAliveRt = nowRt; systemMonitor.acquireWakeLock(force = true) }

                forensicAuditor.auditSensorRate(nowRt, isWarming)?.let { msg ->
                    _sensorEvents.tryEmit(AppSensorEvent.LogEvent(msg, false))
                }
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> processLinearAcceleration(values[0], values[1], values[2], event.timestamp)
            Sensor.TYPE_MAGNETIC_FIELD -> { geomagneticBuffer[0] = values[0]; geomagneticBuffer[1] = values[1]; geomagneticBuffer[2] = values[2]; hasGeomagnetic = true; updateOrientation() }
            Sensor.TYPE_PRESSURE -> processPressure(values[0])
            Sensor.TYPE_PROXIMITY -> {
                val value = values[0]; val newValue = value < proximityMaxRange; currentProximityCm = value.toDouble()
                if (debouncedProximityCm == -1.0) debouncedProximityCm = value.toDouble()
                val rawIdx = (1.0 - (value / proximityMaxRange)).toDouble().coerceIn(0.0, 1.0)
                proximityIdx = (proximityIdx * (1.0 - PROXIMITY_EMA_ALPHA)) + (rawIdx * PROXIMITY_EMA_ALPHA)
                secSumProxIdx += proximityIdx; secProxCount++
                if (newValue != rawProximityNear) {
                    if (!newValue && isDisplayFlickering.get() && isStationary()) return
                    rawProximityNear = newValue; proximityJob?.cancel()
                    var calcDebounceMs = if (isStationary()) PROXIMITY_DEBOUNCE_STATIONARY_MS else PROXIMITY_DEBOUNCE_MOVING_MS
                    if (isStationary() && stationaryStartRt > 0L) calcDebounceMs += (((nowRt - stationaryStartRt) / 3600000.0) * PROXIMITY_STATIONARY_SCALING_MS_PER_HOUR).toLong()
                    if (isHighLoad) calcDebounceMs = (calcDebounceMs * PROXIMITY_STRESS_SCALING_MULTIPLIER).toLong()
                    calcDebounceMs = calcDebounceMs.coerceAtMost(PROXIMITY_DEBOUNCE_MAX_MS); proximityDebounceMs = calcDebounceMs
                    proximityJob = scope.launch { delay(calcDebounceMs); if (isActive && isProximityNear != rawProximityNear) { isProximityNear = rawProximityNear; debouncedProximityCm = value.toDouble() } }
                }
            }
            Sensor.TYPE_LIGHT -> {
                val lux = values[0].toDouble(); currentLux = lux; if (lux > secPeakLux) secPeakLux = lux
                synchronized(this) {
                    if (fastPathLightBaseline < 0) { fastPathLightBaseline = lux } else {
                        val alpha = SentinelValidator.accelerateAlpha(LUX_EMA_FAST, isWarming); fastPathLightBaseline = (fastPathLightBaseline * (1.0 - alpha)) + (lux * alpha)
                        if (!isWarming && onLightSpike != null && (lux - fastPathLightBaseline) > fastPathLightSpikeThreshold) {
                            if (nowRt - lastLightSpikeRt > SPIKE_DEBOUNCE_MS) { lastLightSpikeRt = nowRt; onLightSpike?.invoke() }
                        }
                    }
                }
            }
            Sensor.TYPE_ROTATION_VECTOR -> processRotation(values)
        }
        
        if (nowRt - lastBufferRecordRt >= TICK_INTERVAL_MS) {
            synchronized(sensorBuffer) {
                sensorBuffer.next().apply {
                    this.ts = wallNow
                    this.rt = nowRt
                    this.lux = secPeakLux
                    this.vibe = secPeakVibe
                    this.proxIdx = if (secProxCount > 0) secSumProxIdx / secProxCount else proximityIdx
                    this.tilt = secPeakTilt
                    this.lift = secPeakLift
                    this.acoustic = secPeakDb
                    this.isSitDetected = secSitDetected
                    this.kineticEnergy = secPeakKinetic
                }
            }
            lastBufferRecordRt = nowRt; secPeakLux = currentLux; secPeakVibe = currentVibrationIndex; secSumProxIdx = 0.0; secProxCount = 0; secPeakTilt = currentTiltDegrees; secPeakLift = abs(relativeAltitude); secPeakDb = currentAcousticDb; secPeakKinetic = currentKineticEnergy; secSitDetected = false
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAcousticMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        acousticThread = Thread {
            while (isMonitoring) {
                val sampleRate = ACOUSTIC_SAMPLE_RATE; val bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                if (bufferSize <= 0) { if (isMonitoring) _sensorEvents.tryEmit(AppSensorEvent.HardwareFailure("AudioRecord: Invalid buffer size")); try { Thread.sleep(ACOUSTIC_RECOVERY_DELAY_MS) } catch (ie: InterruptedException) { break }; continue }
                var audioRecord: AudioRecord? = null
                try {
                    var attempts = 0
                    while (attempts < ACOUSTIC_INIT_RETRY_COUNT && isMonitoring) {
                        val record = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
                        if (record.state == AudioRecord.STATE_INITIALIZED) { audioRecord = record; break }
                        record.release(); attempts++; try { Thread.sleep(ACOUSTIC_INIT_RETRY_DELAY_MS) } catch (ie: InterruptedException) { break }
                    }
                    if (!isMonitoring || audioRecord == null) continue
                    try { audioRecord.startRecording() } catch (e: Exception) { try { audioRecord.release() } catch (ex: Exception) {}; try { Thread.sleep(ACOUSTIC_RECOVERY_DELAY_MS) } catch (ie: InterruptedException) { break }; continue }
                    isAcousticRunning = true; val buffer = ShortArray(bufferSize); var lastDutyCycleTransitionRt = timeProvider.elapsedRealtime(); var isInOffCycle = false
                    while (isMonitoring && !Thread.currentThread().isInterrupted) {
                        val nowRt = timeProvider.elapsedRealtime()
                        if (powerSaveMode) {
                            val adaptiveOffCycleMs = SentinelValidator.computeAdaptiveAcousticOffCycle(
                                isStationary = isStationary(),
                                stationaryStartRt = stationaryStartRt,
                                nowRt = nowRt
                            )

                            if (!isInOffCycle && (nowRt - lastDutyCycleTransitionRt > ACOUSTIC_DUTY_CYCLE_ON_MS)) { 
                                isInOffCycle = true; lastDutyCycleTransitionRt = nowRt; try { audioRecord.stop() } catch (e: Exception) {} 
                            }
                            else if (isInOffCycle && (nowRt - lastDutyCycleTransitionRt > adaptiveOffCycleMs)) { 
                                isInOffCycle = false; lastDutyCycleTransitionRt = nowRt; try { audioRecord.startRecording() } catch (e: Exception) { break } 
                            }
                        } else if (isInOffCycle) { isInOffCycle = false; lastDutyCycleTransitionRt = nowRt; try { audioRecord.startRecording() } catch (e: Exception) { break } }
                        if (isInOffCycle) { try { Thread.sleep(500) } catch (ie: InterruptedException) { break }; continue }
                        val read = audioRecord.read(buffer, 0, bufferSize)
                        if (read > 0) {
                            var maxAmp = 0; for (i in 0 until read) { val a = abs(buffer[i].toInt()); if (a > maxAmp) maxAmp = a }
                            val db = if (maxAmp > 0) 20 * log10(maxAmp.toDouble()) else 0.0
                            synchronized(this) {
                                currentAcousticDb = db; if (db > logicPeakDb) logicPeakDb = db; if (db < logicMinDb) logicMinDb = db; if (db > forensicPeakDb) forensicPeakDb = db; if (db < forensicMinDb) forensicMinDb = db; if (db > secPeakDb) secPeakDb = db
                                if (!isWarming && fastPathFloor >= 0 && (db - fastPathFloor) > fastPathSpikeThreshold && db >= fastPathMinDb) { val spikeRt = timeProvider.elapsedRealtime() ; if (spikeRt - lastAcousticSpikeRt > SPIKE_DEBOUNCE_MS) { lastAcousticSpikeRt = spikeRt; lastAcousticLockoutRt = spikeRt; onAcousticSpike?.invoke() } }
                            }
                        } else if (read < 0) { if (!isMonitoring) break; _sensorEvents.tryEmit(AppSensorEvent.HardwareFailure("AudioRecord: Hardware error")); break }
                    }
                    try { audioRecord.stop() } catch (ex: Exception) {}
                } catch (e: Exception) { if (isMonitoring) _sensorEvents.tryEmit(AppSensorEvent.HardwareFailure("AudioRecord: Exception - ${e.message}")) }
                finally { isAcousticRunning = false; try { audioRecord?.release() } catch (ex: Exception) {}; if (isMonitoring) try { Thread.sleep(ACOUSTIC_GENERIC_RECOVERY_DELAY_MS) } catch (ie: InterruptedException) { } }
            }
        }.apply { name = "AcousticMonitor"; priority = Thread.MIN_PRIORITY; start() }
    }

    private fun stopAcousticMonitoring() { isMonitoring = false; isAcousticRunning = false; acousticThread?.interrupt(); try { acousticThread?.join(1000) } catch (e: Exception) {}; acousticThread = null }

    fun isAcousticMonitoringEnabled() = isMonitoring
    fun isAcousticMonitoringActive() = isAcousticRunning

    fun consumeLogicSnapshot(): ForensicSnapshot {
        return LatencyMonitor.measureAndAudit<ForensicSnapshot>(timeProvider, LATENCY_THRESHOLD_SENSOR_PROCESS_MS, "consumeLogicSnapshot", LatencyMonitor.AuditType.PERFORMANCE, { m, _ -> _sensorEvents.tryEmit(AppSensorEvent.LogEvent(m, false)) }) {
            synchronized(logicSnapshotBuffer) {
                val snapshot = logicSnapshotBuffer.next()
                snapshot.apply { 
                    vibration = currentVibrationIndex; heading = currentCompassHeading; baroAlt = absoluteAltitude; lux = currentLux; isNear = isProximityNear; tiltDegrees = currentTiltDegrees; acousticDb = currentAcousticDb; peakShock = logicPeakVibration; peakVerticalVelocity = logicPeakVerticalVelocity; peakVerticalVelocityTs = logicPeakVerticalVelocityTs; peakVerticalVelocityRt = logicPeakVerticalVelocityRt; plungeMatched = !isWarming && plungeMatched; peakVerticalDisplacement = logicPeakVerticalDisplacement; proximityIdx = this@HardwareProvider.proximityIdx; proximityCm = currentProximityCm; proximityDebounceMs = this@HardwareProvider.proximityDebounceMs; vibrationRollingSum = this@HardwareProvider.vibrationRollingSum; acousticPeak = logicPeakDb; acousticPeakMin = if (logicMinDb >= 100.0) -1.0 else logicMinDb; kineticEnergy = this@HardwareProvider.currentKineticEnergy 
                }
                logicPeakVibration = 0.0; logicPeakVerticalVelocity = 0.0; logicPeakVerticalVelocityTs = 0L; logicPeakVerticalVelocityRt = 0L; logicPeakVerticalDisplacement = 0.0; plungeMatched = false; logicPeakDb = 0.0; logicMinDb = 100.0; snapshot
            }
        }
    }

    fun consumeForensicSnapshot(): ForensicSnapshot {
        return LatencyMonitor.measureAndAudit<ForensicSnapshot>(timeProvider, LATENCY_THRESHOLD_SENSOR_PROCESS_MS, "consumeForensicSnapshot", LatencyMonitor.AuditType.PERFORMANCE, { m, _ -> _sensorEvents.tryEmit(AppSensorEvent.LogEvent(m, false)) }) {
            synchronized(forensicSnapshotBuffer) {
                val snapshot = forensicSnapshotBuffer.next()
                snapshot.apply { 
                    vibration = currentVibrationIndex; heading = currentCompassHeading; baroAlt = absoluteAltitude; lux = currentLux; isNear = isProximityNear; tiltDegrees = currentTiltDegrees; acousticDb = currentAcousticDb; peakShock = forensicPeakVibration; peakVerticalVelocity = forensicPeakVerticalVelocity; peakVerticalVelocityTs = forensicPeakVerticalVelocityTs; peakVerticalVelocityRt = forensicPeakVerticalVelocityRt; plungeMatched = false; peakVerticalDisplacement = forensicPeakVerticalDisplacement; proximityIdx = this@HardwareProvider.proximityIdx; proximityCm = currentProximityCm; proximityDebounceMs = this@HardwareProvider.proximityDebounceMs; vibrationRollingSum = this@HardwareProvider.vibrationRollingSum; acousticPeak = forensicPeakDb; acousticPeakMin = if (logicMinDb >= 100.0) -1.0 else logicMinDb; kineticEnergy = this@HardwareProvider.currentKineticEnergy 
                }
                forensicPeakVibration = 0.0; forensicPeakVerticalVelocity = 0.0; forensicPeakVerticalVelocityTs = 0L; forensicPeakVerticalVelocityRt = 0L; forensicPeakVerticalDisplacement = 0.0; forensicPeakDb = 0.0; forensicMinDb = 100.0; snapshot
            }
        }
    }

    fun getSensorSamples(fromRt: Long, toRt: Long): Sequence<EngineSensorSnapshot> = sequence {
        val flyweight = EngineSensorSnapshot()
        val snapshot = synchronized(sensorBuffer) { sensorBuffer.asSequence().toList() }
        for (sample in snapshot) {
            if (sample.rt in fromRt..toRt) {
                flyweight.copyFrom(sample)
                yield(flyweight)
            }
        }
    }

    fun getAcousticSamples(fromRt: Long, toRt: Long): Sequence<EngineSnrSample> = sequence {
        val flyweight = EngineSnrSample()
        val snapshot = synchronized(sensorBuffer) { sensorBuffer.asSequence().toList() }
        for (sample in snapshot) {
            if (sample.rt in fromRt..toRt) {
                flyweight.apply { this.ts = sample.ts; this.rt = sample.rt; this.snr = sample.acoustic }
                yield(flyweight)
            }
        }
    }

    private fun processVibration(x: Float, y: Float, z: Float) {
        val dx = x.toDouble() - lastAccelX.toDouble(); val dy = y.toDouble() - lastAccelY.toDouble(); val dz = z.toDouble() - lastAccelZ.toDouble()
        val delta = sqrt(dx * dx + dy * dy + dz * dz) / GRAVITY_EARTH
        synchronized(this) { if (delta > logicPeakVibration) logicPeakVibration = delta; if (delta > forensicPeakVibration) forensicPeakVibration = delta; adaptiveVibrationFloor = SentinelValidator.updateVibrationFloor(adaptiveVibrationFloor, delta, isWarming); lastHpfValue = SentinelValidator.computeNextHpf(lastHpfValue, delta, lastRawVibe); currentKineticEnergy = SentinelValidator.computeNextEnergy(currentKineticEnergy, lastHpfValue); lastRawVibe = delta }
        lastAccelX = x; lastAccelY = y; lastAccelZ = z
        val oldVal = vibrationCircularBuffer[vibrationCircularIdx]; vibrationCircularBuffer[vibrationCircularIdx] = delta; vibrationRollingSum = vibrationRollingSum - oldVal + delta; vibrationCircularIdx = (vibrationCircularIdx + 1) % VIBRATION_WINDOW_SIZE; if (vibrationBufferCount < VIBRATION_WINDOW_SIZE) vibrationBufferCount++
        currentVibrationIndex = if (vibrationBufferCount > 0) vibrationRollingSum / vibrationBufferCount else 0.0
        if (currentVibrationIndex > secPeakVibe) secPeakVibe = currentVibrationIndex
        if (currentKineticEnergy > secPeakKinetic) secPeakKinetic = currentKineticEnergy
        val nowRt = timeProvider.elapsedRealtime()
        if (plungePhase == 2) { if (isStationary()) { synchronized(this) { plungeMatched = true; secSitDetected = true }; plungePhase = 0 } else if (nowRt - lastPlungePhaseRt > CHAIR_PLUNGE_PHASE_TIMEOUT_MS) plungePhase = 0 }
        if (isStationary()) { if (stationaryStartRt == 0L) stationaryStartRt = nowRt else if (nowRt - stationaryStartRt > MUZZLE_HYSTERESIS_MS) { currentVerticalVelocity = 0.0; currentVerticalDisplacement = 0.0; if (plungePhase != 2) plungePhase = 0 } } else { stationaryStartRt = 0L }
    }

    private fun processLinearAcceleration(val0: Float, val1: Float, val2: Float, timestampNs: Long) {
        if (!hasGravity) return
        if (lastLinearAccelTs == 0L) { lastLinearAccelTs = timestampNs; return }
        val dt = (timestampNs - lastLinearAccelTs).toDouble() / 1_000_000_000.0; lastLinearAccelTs = timestampNs
        if (dt > 0.0 && dt < 0.2) {
            val d0 = val0.toDouble(); val d1 = val1.toDouble(); val d2 = val2.toDouble(); val g0 = gravityBuffer[0].toDouble(); val g1 = gravityBuffer[1].toDouble(); val g2 = gravityBuffer[2].toDouble(); val dot = d0 * g0 + d1 * g1 + d2 * g2; val gravMag = sqrt(g0 * g0 + g1 * g1 + g2 * g2); val vz_accel = if (gravMag > 0.1) dot / gravMag else 0.0
            currentVerticalVelocity += vz_accel * dt; currentVerticalDisplacement += currentVerticalVelocity * dt 
            if (abs(currentVerticalVelocity) > VERTICAL_VELOCITY_MAX_MPS) currentVerticalVelocity = if (currentVerticalVelocity > 0) VERTICAL_VELOCITY_MAX_MPS else -VERTICAL_VELOCITY_MAX_MPS 
            val nowRt = timeProvider.elapsedRealtime(); val wallNow = timeProvider.currentTimeMillis()
            if (plungePhase > 0 && nowRt - lastPlungePhaseRt > CHAIR_PLUNGE_PHASE_TIMEOUT_MS) plungePhase = 0
            when (plungePhase) {
                0 -> if (!isWarming && currentVerticalVelocity < -CHAIR_PLUNGE_VELOCITY_THRESHOLD) { plungePhase = 1; lastPlungePhaseRt = nowRt; currentVerticalDisplacement = 0.0 }
                1 -> { if (nowRt - lastPlungePhaseRt > CHAIR_PLUNGE_WINDOW_MS) { plungePhase = 0 } else if (currentVerticalVelocity > -CHAIR_PLUNGE_VELOCITY_THRESHOLD * 0.2) { if (abs(currentVerticalDisplacement) > CHAIR_PLUNGE_DISTANCE_THRESHOLD) { plungePhase = 2; lastPlungePhaseRt = nowRt } else { plungePhase = 0 } } }
            }
            synchronized(this) { 
                if (abs(currentVerticalVelocity) > abs(logicPeakVerticalVelocity)) { logicPeakVerticalVelocity = currentVerticalVelocity; logicPeakVerticalVelocityTs = wallNow; logicPeakVerticalVelocityRt = nowRt }
                if (abs(currentVerticalDisplacement) > abs(logicPeakVerticalDisplacement)) logicPeakVerticalDisplacement = currentVerticalDisplacement 
                if (abs(currentVerticalVelocity) > abs(forensicPeakVerticalVelocity)) { forensicPeakVerticalVelocity = currentVerticalVelocity; forensicPeakVerticalVelocityTs = wallNow; forensicPeakVerticalVelocityRt = nowRt }
                if (abs(currentVerticalDisplacement) > abs(forensicPeakVerticalDisplacement)) forensicPeakVerticalDisplacement = currentVerticalDisplacement
            }
        }
    }

    private fun processPressure(pressure: Float) {
        val pressureDouble = pressure.toDouble(); if (emaPressure == 0.0) emaPressure = pressureDouble
        currentPressure = pressureDouble; val alpha = SentinelValidator.accelerateAlpha(1.0 - BARO_EMA_SLOW, isWarming); emaPressure = (emaPressure * (1.0 - alpha)) + (pressureDouble * alpha)
        val nowRt = timeProvider.elapsedRealtime(); val stationaryDuration = if (stationaryStartRt > 0L) nowRt - stationaryStartRt else 0L
        if (nowRt - lastBaroZeroingRt > BARO_ZEROING_INTERVAL_MS && stationaryDuration >= PASSIVE_ZEROING_STATIONARY_MS) { emaPressure = pressureDouble; lastBaroZeroingRt = nowRt }
        val currentAlt = android.hardware.SensorManager.getAltitude(android.hardware.SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressure).toDouble()
        val baselineAlt = android.hardware.SensorManager.getAltitude(android.hardware.SensorManager.PRESSURE_STANDARD_ATMOSPHERE, emaPressure.toFloat()).toDouble()
        absoluteAltitude = currentAlt; relativeAltitude = if (isWarming) 0.0 else currentAlt - baselineAlt
        if (abs(relativeAltitude) > secPeakLift) secPeakLift = abs(relativeAltitude)
    }

    private fun processRotation(rotationVector: FloatArray) {
        android.hardware.SensorManager.getRotationMatrixFromVector(currentRotationVectorMatrixBuffer, rotationVector); val nowRt = timeProvider.elapsedRealtime()
        if (!hasInitialRotation) { if (!isWarming && stationaryStartRt != 0L && (nowRt - stationaryStartRt > ROTATION_INIT_STATIONARY_MS)) { System.arraycopy(currentRotationVectorMatrixBuffer, 0, initialRotationMatrix, 0, 9); hasInitialRotation = true }; return }
        val dotProduct = (initialRotationMatrix[2] * currentRotationVectorMatrixBuffer[2]) + (initialRotationMatrix[5] * currentRotationVectorMatrixBuffer[5]) + (initialRotationMatrix[8] * currentRotationVectorMatrixBuffer[8])
        currentTiltDegrees = if (isWarming) 0.0 else Math.toDegrees(acos(dotProduct.coerceIn(-1.0f, 1.0f).toDouble()))
        if (currentTiltDegrees > secPeakTilt) secPeakTilt = currentTiltDegrees
    }

    private fun updateOrientation() { if (hasGravity && hasGeomagnetic) { if (android.hardware.SensorManager.getRotationMatrix(rotationMatrixBuffer, inclinationMatrixBuffer, gravityBuffer, geomagneticBuffer)) { android.hardware.SensorManager.getOrientation(rotationMatrixBuffer, orientationBuffer); currentCompassHeading = (Math.toDegrees(orientationBuffer[0].toDouble()) + 360.0) % 360.0 } } }
    
    fun isStationary() = SentinelValidator.isStationary(currentVibrationIndex, adaptiveVibrationFloor)
    
    fun setAcousticFastPath(floor: Double, spikeThreshold: Double, minDb: Double, onSpike: () -> Unit) { synchronized(this) { this.fastPathFloor = floor; this.fastPathSpikeThreshold = spikeThreshold; this.fastPathMinDb = minDb; this.onAcousticSpike = onSpike } }
    fun setLightFastPath(baseline: Double, spikeThreshold: Double, onSpike: () -> Unit) { synchronized(this) { this.fastPathLightBaseline = baseline; this.fastPathLightSpikeThreshold = spikeThreshold; this.onLightSpike = onSpike } }
    fun setHighLoad(high: Boolean) { this.isHighLoad = high }
    fun setMaliAnomaly(active: Boolean) { this.maliAnomaly = active }
    
    fun setSafeMode(active: Boolean) { 
        this.isSafeMode = active 
        if (active) {
            revivalPulseJob?.cancel()
            revivalPulseJob = null
            Timber.i("HardwareProvider: Safe Mode active. Revival pulses suppressed.")
        }
    }
    
    fun setPowerSaveMode(active: Boolean) {
        synchronized(lifecycleLock) {
            if (this.powerSaveMode != active) {
                this.powerSaveMode = active
                if (isStarted.get() && hardwareHandler != null) {
                    this.unregister(sensorManager, hardwareHandler)
                    registerSensors()
                }
            }
        }
    }

    fun resetBaseline() { emaPressure = currentPressure; relativeAltitude = 0.0; absoluteAltitude = android.hardware.SensorManager.getAltitude(android.hardware.SensorManager.PRESSURE_STANDARD_ATMOSPHERE, currentPressure.toFloat()).toDouble(); hasInitialRotation = false; stationaryStartRt = 0L; currentVerticalVelocity = 0.0; currentVerticalDisplacement = 0.0; plungePhase = 0; plungeMatched = false; secSitDetected = false; sessionStartRt = timeProvider.elapsedRealtime(); lastBaroZeroingRt = sessionStartRt; adaptiveVibrationFloor = VIBRATION_STATIONARY_THRESHOLD; debouncedProximityCm = -1.0; proximityDebounceMs = 0L; vibrationCircularIdx = 0; vibrationRollingSum = 0.0; vibrationBufferCount = 0; vibrationCircularBuffer.fill(0.0); lastRawVibe = 0.0; lastHpfValue = 0.0; currentKineticEnergy = 0.0; forensicAuditor.reset(); synchronized(sensorBuffer) { sensorBuffer.clear(); lastBufferRecordRt = 0L }; synchronized(snrBuffer) { snrBuffer.clear() }; synchronized(logicSnapshotBuffer) { logicSnapshotBuffer.clear() }; synchronized(forensicSnapshotBuffer) { forensicSnapshotBuffer.clear() } }

    private fun startStepDetectorRecoveryLoop() { recoveryJob?.cancel(); recoveryJob = scope.launch { while (isActive) { delay(300000L); if (!isStepDetectorRegistered) attemptStepRegistration() } } }
    private fun attemptStepDetectorRegistration() {
        val detector = stepDetector ?: return
        synchronized(lifecycleLock) {
            registrationJob?.cancel(); registrationJob = scope.launch(Dispatchers.IO) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !systemStatusProvider.isActivityRecognitionGranted()) { isStepDetectorRegistered = false; return@launch }
                val targetHandler = synchronized(lifecycleLock) { if (!isStarted.get()) return@launch; hardwareHandler } ?: return@launch
                withContext(targetHandler.asCoroutineDispatcher()) { 
                    unregister(sensorManager, detector, targetHandler)
                    synchronized(lifecycleLock) { 
                        if (isStarted.get()) isStepDetectorRegistered = sensorManager.registerListener(this@HardwareProvider, detector, android.hardware.SensorManager.SENSOR_DELAY_NORMAL, hardwareHandler) 
                    } 
                }
            }
        }
    }
    private fun attemptStepRegistration() = attemptStepDetectorRegistration()

    private fun checkRevivalLifecycle() {
        if (!isStarted.get() || isSafeMode) return
        val nowRt = timeProvider.elapsedRealtime(); val currentStatus = _locationStatus.value
        if (currentStatus.isPending) {
            val stallDuration = nowRt - pendingEnterRt
            val retryThreshold = (revivalAttemptCount + 1) * GPS_REVIVAL_RETRY_INTERVAL_MS
            
            forensicAuditor.captureRevivalStart(nowRt)

            if (stallDuration > retryThreshold) {
                if (revivalAttemptCount < MAX_REVIVAL_ATTEMPTS) {
                    revivalAttemptCount++
                    Timber.w("HardwareProvider: GNSS Recovery Pulse triggered (Attempt $revivalAttemptCount, Reason: ${currentStatus.reason})")
                    _revivalEvents.tryEmit(RevivalEvent.Attempt(revivalAttemptCount))
                    restartLocationUpdates()
                } else if (!isHardwareLocked) {
                    isHardwareLocked = true
                    Timber.e("HardwareProvider: MAX REVIVAL ATTEMPTS REACHED. GPS_HARDWARE_LOCK triggered.")
                    _revivalEvents.tryEmit(RevivalEvent.HardwareLock)
                    forensicAuditor.computeEnergyFootprint(nowRt)?.let { _revivalEvents.tryEmit(it) }
                }
            }
        } else { 
            revivalAttemptCount = 0; isHardwareLocked = false 
        }
    }
}
