package com.gps19.app

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager as AndroidSensorManager
import android.hardware.display.DisplayManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.Display
import com.gps19.core.engine.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.acos
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * AppSensorManager: Manages IMU, Environmental sensors, and Display state transitions.
 * July.23.02:
 * - Issue #526: Power Optimization. Implemented Acoustic Duty Cycling to reduce 
 *   energy footprint during stationary/stalled states.
 * July.23.01:
 * - Forensic Consolidation: Integrated consumeForensicSnapshot() (Issue #523).
 */
class AppSensorManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val timeProvider: TimeProvider,
    private val systemMonitor: SystemMonitor
) : SensorEventListener {

    private val sensorManager by lazy { context.getSystemService(Context.SENSOR_SERVICE) as AndroidSensorManager }
    private val displayManager by lazy { context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager }
    
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

    private var sensorThread: HandlerThread? = null
    private var sensorHandler: Handler? = null
    private val hasLoggedThreadInfo = AtomicBoolean(false)

    private var lastDisplayState = Display.STATE_UNKNOWN
    private var lastDisplayTransitionRt = 0L
    private val isDisplayFlickering = AtomicBoolean(false)

    private var lastStayAliveRt = 0L

    fun isScreenOn(): Boolean {
        if (lastDisplayState == Display.STATE_UNKNOWN) {
            val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            if (display != null) lastDisplayState = display.state
        }
        return lastDisplayState == Display.STATE_ON
    }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {}
        override fun onDisplayChanged(displayId: Int) {
            if (displayId != Display.DEFAULT_DISPLAY) return
            val display = displayManager.getDisplay(displayId) ?: return
            val newState = display.state
            
            if (newState != lastDisplayState) {
                val nowRt = timeProvider.elapsedRealtime()
                val delta = nowRt - lastDisplayTransitionRt
                if (delta < 1000L) {
                    if (!isDisplayFlickering.get()) {
                        isDisplayFlickering.set(true)
                        Timber.w("Issue #037: Rapid Display Flickering detected. Engaging hardware muzzle.")
                    }
                } else {
                    isDisplayFlickering.set(false)
                }
                lastDisplayState = newState
                lastDisplayTransitionRt = nowRt
            }
        }
    }

    private val gravityBuffer = FloatArray(3)
    private val geomagneticBuffer = FloatArray(3)

    private val rotationMatrixBuffer = FloatArray(9)
    private val inclinationMatrixBuffer = FloatArray(9)
    private val orientationBuffer = FloatArray(3)
    private val currentRotationVectorMatrixBuffer = FloatArray(9)
    private var hasGravity = false
    private var hasGeomagnetic = false

    private var lastAccelX = 0f
    private var lastAccelY = 0f
    private var lastAccelZ = 0f
    
    private val vibrationCircularBuffer = DoubleArray(VIBRATION_WINDOW_SIZE)
    private var vibrationCircularIdx = 0
    
    var vibrationRollingSum = 0.0
        private set

    private var vibrationBufferCount = 0

    private var internalPeakDb: Double = 0.0
    private var internalMinDb: Double = 100.0
    private var internalPeakVibration: Double = 0.0
    private var internalPeakVerticalVelocity: Double = 0.0
    private var internalPeakVerticalVelocityTs: Long = 0L
    private var internalPeakVerticalVelocityRt: Long = 0L
    private var internalPeakVerticalDisplacement: Double = 0.0
    
    @Volatile
    private var isMonitoring = false
    @Volatile
    private var isAcousticRunning = false
    private var acousticThread: Thread? = null

    @Volatile
    private var isHighLoad = false

    @Volatile
    private var powerSaveMode = false

    data class SensorSnapshot(
        val ts: Long,
        val rt: Long,
        val lux: Double,
        val vibe: Double,
        val proxIdx: Double,
        val tilt: Double,
        val lift: Double,
        val acoustic: Double,
        val isSitDetected: Boolean
    )

    data class ForensicSnapshot(
        val vibration: Double,
        val heading: Double,
        val baroAlt: Double,
        val lux: Double,
        val isNear: Boolean,
        val tiltDegrees: Double,
        val acousticDb: Double,
        val peakShock: Double,
        val peakVerticalVelocity: Double,
        val peakVerticalVelocityTs: Long,
        val peakVerticalVelocityRt: Long,
        val plungeMatched: Boolean,
        val peakVerticalDisplacement: Double,
        val proximityIdx: Double,
        val proximityCm: Double,
        val proximityDebounceMs: Long,
        val vibrationRollingSum: Double,
        val acousticPeak: Double,
        val acousticMin: Double
    )

    private val sensorSampleBuffer = ConcurrentLinkedQueue<SensorSnapshot>()
    private var lastBufferRecordRt = 0L

    private var secPeakLux = 0.0
    private var secPeakVibe = 0.0
    private var secMinProxIdx = 1.0
    private var secPeakTilt = 0.0
    private var secPeakLift = 0.0
    private var secPeakDb = 0.0
    private var secSitDetected = false
    
    private var fastPathFloor: Double = -1.0
    private var fastPathSpikeThreshold: Double = ACOUSTIC_THRESHOLD_DB_JUMP
    private var fastPathMinDb: Double = ACOUSTIC_MIN_THRESHOLD_DB
    private var onAcousticSpike: (() -> Unit)? = null

    private var fastPathLightBaseline: Double = -1.0
    private var fastPathLightSpikeThreshold: Double = LIGHT_THRESHOLD_LUX_JUMP
    private var onLightSpike: (() -> Unit)? = null
    
    private var lastAcousticSpikeRt: Long = 0L
    private var lastLightSpikeRt: Long = 0L

    var lastAcousticLockoutRt: Long = 0L
        private set

    private var sessionStartRt = 0L
    val isWarming: Boolean get() = (timeProvider.elapsedRealtime() - sessionStartRt < SENSOR_WARMING_MS)

    private var onHardwareFailure: ((String) -> Unit)? = null

    var currentVibrationIndex: Double = 0.0
        private set
    
    var adaptiveVibrationFloor: Double = VIBRATION_STATIONARY_THRESHOLD
        private set

    var currentCompassHeading: Double = 0.0
        private set

    var currentPressure: Double = 0.0
        private set

    var absoluteAltitude: Double = 0.0
        private set

    var relativeAltitude: Double = 0.0
        private set

    private var proximityJob: Job? = null
    private var rawProximityNear: Boolean = true
    private var proximityMaxRange = 5f

    var isProximityNear: Boolean = true
        private set

    var proximityIdx: Double = 1.0
        private set

    var currentProximityCm: Double = -1.0
        private set

    var debouncedProximityCm: Double = -1.0
        private set

    var proximityDebounceMs: Long = 0L
        private set

    var currentLux: Double = 0.0
        private set

    var currentTiltDegrees: Double = 0.0
        private set
        
    var currentAcousticDb: Double = 0.0
        private set

    var currentVerticalVelocity: Double = 0.0
        private set

    var currentVerticalDisplacement: Double = 0.0
        private set

    private var lastLinearAccelTs: Long = 0L
    private var stationaryStartRt: Long = 0L

    private var emaPressure: Double = 0.0
    private var lastBaroZeroingRt: Long = 0L

    private var initialRotationMatrix = FloatArray(9)
    private var hasInitialRotation = false

    private var plungePhase = 0 
    private var plungeMatched = false
    private var lastPlungePhaseRt = 0L

    fun start() {
        sessionStartRt = timeProvider.elapsedRealtime()
        lastBaroZeroingRt = sessionStartRt
        hasLoggedThreadInfo.set(false)
        proximityMaxRange = proximity?.maximumRange ?: 5f

        if (sensorThread == null) {
            sensorThread = HandlerThread("AppSensorThread").apply { start() }
            sensorHandler = Handler(sensorThread!!.looper)
        }

        accelerometer?.let { sensorManager.registerListener(this, it, AndroidSensorManager.SENSOR_DELAY_NORMAL, sensorHandler) }
        linearAccel?.let { sensorManager.registerListener(this, it, AndroidSensorManager.SENSOR_DELAY_FASTEST, sensorHandler) }
        magnetometer?.let { sensorManager.registerListener(this, it, AndroidSensorManager.SENSOR_DELAY_NORMAL, sensorHandler) }
        barometer?.let { sensorManager.registerListener(this, it, AndroidSensorManager.SENSOR_DELAY_NORMAL, sensorHandler) }
        proximity?.let { sensorManager.registerListener(this, it, AndroidSensorManager.SENSOR_DELAY_NORMAL, sensorHandler) }
        light?.let { sensorManager.registerListener(this, it, AndroidSensorManager.SENSOR_DELAY_NORMAL, sensorHandler) }
        rotationVector?.let { sensorManager.registerListener(this, it, AndroidSensorManager.SENSOR_DELAY_NORMAL, sensorHandler) }
        
        attemptStepDetectorRegistration()
        startStepDetectorRecoveryLoop()

        displayManager.registerDisplayListener(displayListener, sensorHandler)
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        if (display != null) lastDisplayState = display.state

        startAcousticMonitoring()
    }

    fun stop() {
        recoveryJob?.cancel()
        recoveryJob = null
        stopAcousticMonitoring()
        sensorManager.unregisterListener(this)
        displayManager.unregisterDisplayListener(displayListener)
        proximityJob?.cancel()
        sensorThread?.quitSafely()
        sensorThread = null
        sensorHandler = null
        isStepDetectorRegistered = false
    }

    private fun startStepDetectorRecoveryLoop() {
        recoveryJob?.cancel()
        recoveryJob = scope.launch {
            while (isActive) {
                delay(300000L) 
                if (!isStepDetectorRegistered) {
                    Timber.i("Issue #113: Step Detector registration stale. Attempting recovery...")
                    attemptStepRegistration()
                }
            }
        }
    }

    private fun attemptStepDetectorRegistration() {
        isStepDetectorRegistered = stepDetector?.let { 
            val registered = sensorManager.registerListener(this, it, AndroidSensorManager.SENSOR_DELAY_NORMAL, sensorHandler) 
            if (!registered) {
                Timber.e("Issue #098: Step Detector exists but registerListener failed.")
            } else {
                Timber.i("Issue #098: Step Detector registered successfully.")
            }
            registered
        } ?: false
    }

    private fun attemptStepRegistration() {
        attemptStepDetectorRegistration()
    }

    fun setHardwareFailureCallback(callback: (String) -> Unit) {
        this.onHardwareFailure = callback
    }

    fun setAcousticFastPath(floor: Double, spikeThreshold: Double, minDb: Double, onSpike: () -> Unit) {
        synchronized(this) {
            this.fastPathFloor = floor
            this.fastPathSpikeThreshold = spikeThreshold
            this.fastPathMinDb = minDb
            this.onAcousticSpike = onSpike
        }
    }

    fun setLightFastPath(baseline: Double, spikeThreshold: Double, onSpike: () -> Unit) {
        synchronized(this) {
            this.fastPathLightBaseline = baseline
            this.fastPathLightSpikeThreshold = spikeThreshold
            this.onLightSpike = onSpike
        }
    }

    fun setHighLoad(high: Boolean) {
        this.isHighLoad = high
    }

    fun setPowerSaveMode(active: Boolean) {
        if (this.powerSaveMode != active) {
            this.powerSaveMode = active
            Timber.i("Issue #526: Power Save Mode ${if (active) "ENABLED" else "DISABLED"}. Acoustic Duty Cycling triggered.")
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (hasLoggedThreadInfo.compareAndSet(false, true)) {
            val threadName = Thread.currentThread().name
            val isMain = Looper.myLooper() == Looper.getMainLooper()
            Timber.i("Forensic: AppSensorManager offloading verified. Thread: $threadName (isMain: $isMain)")
        }

        val nowRt = timeProvider.elapsedRealtime()
        val wallNow = timeProvider.currentTimeMillis()
        val values = event.values
        
        when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> {
                lastStayAliveRt = nowRt
            }
            Sensor.TYPE_ACCELEROMETER -> {
                gravityBuffer[0] = values[0]
                gravityBuffer[1] = values[1]
                gravityBuffer[2] = values[2]
                
                hasGravity = true
                processVibration(values[0], values[1], values[2])
                updateOrientation()
                
                if (!isStepDetectorRegistered && nowRt - lastStayAliveRt > 10000L) {
                    lastStayAliveRt = nowRt
                    systemMonitor.acquireWakeLock(force = true)
                    Timber.i("Issue #113: Stay-Alive Pulse (Accel Fallback Poked)")
                }
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                processLinearAcceleration(values[0], values[1], values[2], event.timestamp)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                geomagneticBuffer[0] = values[0]; geomagneticBuffer[1] = values[1]; geomagneticBuffer[2] = values[2]
                hasGeomagnetic = true; updateOrientation()
            }
            Sensor.TYPE_PRESSURE -> processPressure(values[0])
            Sensor.TYPE_PROXIMITY -> {
                val value = values[0]
                val newValue = value < proximityMaxRange
                currentProximityCm = value.toDouble()
                if (debouncedProximityCm == -1.0) debouncedProximityCm = value.toDouble()
                proximityIdx = (1.0 - (value / proximityMaxRange)).toDouble().coerceIn(0.0, 1.0)
                if (proximityIdx < secMinProxIdx) secMinProxIdx = proximityIdx
                if (newValue != rawProximityNear) {
                    if (!newValue && isDisplayFlickering.get() && isStationary()) return
                    rawProximityNear = newValue; proximityJob?.cancel()
                    val baseDebounceMs = if (isStationary()) PROXIMITY_DEBOUNCE_STATIONARY_MS else PROXIMITY_DEBOUNCE_MOVING_MS
                    var calcDebounceMs = baseDebounceMs
                    if (isStationary() && stationaryStartRt > 0) {
                        val hoursStationary = (nowRt - stationaryStartRt) / 3600000.0
                        calcDebounceMs += (hoursStationary * PROXIMITY_STATIONARY_SCALING_MS_PER_HOUR).toLong()
                    }
                    if (isHighLoad) calcDebounceMs = (calcDebounceMs * PROXIMITY_STRESS_SCALING_MULTIPLIER).toLong()
                    calcDebounceMs = calcDebounceMs.coerceAtMost(PROXIMITY_DEBOUNCE_MAX_MS)
                    proximityDebounceMs = calcDebounceMs
                    proximityJob = scope.launch {
                        delay(calcDebounceMs)
                        if (isActive && isProximityNear != rawProximityNear) {
                            isProximityNear = rawProximityNear; debouncedProximityCm = value.toDouble()
                        }
                    }
                }
            }
            Sensor.TYPE_LIGHT -> {
                val lux = values[0].toDouble()
                currentLux = lux
                if (lux > secPeakLux) secPeakLux = lux
                synchronized(this) {
                    if (fastPathLightBaseline < 0) { fastPathLightBaseline = lux } else {
                        val alpha = SentinelValidator.accelerateAlpha(LUX_EMA_FAST, isWarming)
                        fastPathLightBaseline = (fastPathLightBaseline * (1.0 - alpha)) + (lux * alpha)
                        if (!isWarming && onLightSpike != null && (lux - fastPathLightBaseline) > fastPathLightSpikeThreshold) {
                            if (nowRt - lastLightSpikeRt > SPIKE_DEBOUNCE_MS) { lastLightSpikeRt = nowRt; onLightSpike?.invoke() }
                        }
                    }
                }
            }
            Sensor.TYPE_ROTATION_VECTOR -> processRotation(values)
        }
        
        if (nowRt - lastBufferRecordRt >= TICK_INTERVAL_MS) {
            val sitForForensics: Boolean
            synchronized(this) { sitForForensics = secSitDetected; secSitDetected = false }
            sensorSampleBuffer.add(SensorSnapshot(ts = wallNow, rt = nowRt, lux = secPeakLux, vibe = secPeakVibe, proxIdx = secMinProxIdx, tilt = secPeakTilt, lift = secPeakLift, acoustic = secPeakDb, isSitDetected = sitForForensics))
            lastBufferRecordRt = nowRt
            secPeakLux = currentLux; secPeakVibe = currentVibrationIndex; secMinProxIdx = proximityIdx; secPeakTilt = currentTiltDegrees; secPeakLift = abs(relativeAltitude); secPeakDb = currentAcousticDb
            while (sensorSampleBuffer.size > 0 && (wallNow - (sensorSampleBuffer.peek()?.ts ?: wallNow)) > SENSOR_SAMPLE_BUFFER_MAX_AGE_MS) sensorSampleBuffer.poll()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAcousticMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        acousticThread = Thread {
            while (isMonitoring) {
                val sampleRate = ACOUSTIC_SAMPLE_RATE
                val bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                if (bufferSize <= 0) {
                    if (isMonitoring) onHardwareFailure?.invoke("AudioRecord: Invalid buffer size")
                    try { Thread.sleep(ACOUSTIC_RECOVERY_DELAY_MS) } catch (ie: InterruptedException) { break }
                    continue
                }
                var audioRecord: AudioRecord? = null
                try {
                    var attempts = 0
                    while (attempts < ACOUSTIC_INIT_RETRY_COUNT && isMonitoring) {
                        val record = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
                        if (record.state == AudioRecord.STATE_INITIALIZED) { audioRecord = record; break }
                        record.release(); attempts++
                        try { Thread.sleep(ACOUSTIC_INIT_RETRY_DELAY_MS) } catch (ie: InterruptedException) { break }
                    }
                    if (!isMonitoring) break
                    if (audioRecord == null || audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                        if (isMonitoring) onHardwareFailure?.invoke("AudioRecord: Init failed")
                        try { Thread.sleep(ACOUSTIC_RECOVERY_DELAY_MS) } catch (ie: InterruptedException) { break }
                        continue
                    }
                    try { audioRecord.startRecording() } catch (e: IllegalStateException) {
                        if (isMonitoring) onHardwareFailure?.invoke("AudioRecord: Mic occupied")
                        try { audioRecord.release() } catch (ex: Exception) {}
                        try { Thread.sleep(ACOUSTIC_RECOVERY_DELAY_MS) } catch (ie: InterruptedException) { break }
                        continue
                    }
                    if (audioRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                        if (isMonitoring) onHardwareFailure?.invoke("AudioRecord: Contention")
                        try { audioRecord.release() } catch (ex: Exception) {}
                        try { Thread.sleep(ACOUSTIC_RECOVERY_DELAY_MS) } catch (ie: InterruptedException) { break }
                        continue
                    }
                    isAcousticRunning = true
                    val buffer = ShortArray(bufferSize)
                    var lastDutyCycleTransitionRt = timeProvider.elapsedRealtime()
                    var isInOffCycle = false

                    while (isMonitoring && !Thread.currentThread().isInterrupted) {
                        val nowRt = timeProvider.elapsedRealtime()
                        
                        // Issue #526: Acoustic Duty Cycle Logic
                        if (powerSaveMode) {
                            if (!isInOffCycle && (nowRt - lastDutyCycleTransitionRt > ACOUSTIC_DUTY_CYCLE_ON_MS)) {
                                isInOffCycle = true; lastDutyCycleTransitionRt = nowRt
                                try { audioRecord.stop() } catch (e: Exception) {}
                            } else if (isInOffCycle && (nowRt - lastDutyCycleTransitionRt > ACOUSTIC_DUTY_CYCLE_OFF_MS)) {
                                isInOffCycle = false; lastDutyCycleTransitionRt = nowRt
                                try { audioRecord.startRecording() } catch (e: Exception) { break }
                            }
                        } else if (isInOffCycle) {
                            isInOffCycle = false; lastDutyCycleTransitionRt = nowRt
                            try { audioRecord.startRecording() } catch (e: Exception) { break }
                        }

                        if (isInOffCycle) {
                            try { Thread.sleep(500) } catch (ie: InterruptedException) { break }
                            continue
                        }

                        val read = audioRecord.read(buffer, 0, bufferSize)
                        if (read > 0) {
                            var maxAmp = 0; for (i in 0 until read) { val a = Math.abs(buffer[i].toInt()); if (a > maxAmp) maxAmp = a }
                            val db = if (maxAmp > 0) 20 * log10(maxAmp.toDouble() / 1.0) else 0.0
                            synchronized(this) {
                                currentAcousticDb = db; if (db > internalPeakDb) internalPeakDb = db
                                if (db < internalMinDb) internalMinDb = db; if (db > secPeakDb) secPeakDb = db
                                if (!isWarming && fastPathFloor >= 0 && (db - fastPathFloor) > fastPathSpikeThreshold && db >= fastPathMinDb) {
                                    val spikeRt = timeProvider.elapsedRealtime()
                                    if (spikeRt - lastAcousticSpikeRt > SPIKE_DEBOUNCE_MS) { lastAcousticSpikeRt = spikeRt; lastAcousticLockoutRt = spikeRt; onAcousticSpike?.invoke() }
                                }
                            }
                        } else if (read < 0) {
                            if (!isMonitoring) break
                            onHardwareFailure?.invoke("AudioRecord: Hardware error"); break
                        }
                    }
                    try { audioRecord.stop() } catch (ex: Exception) {}
                } catch (e: Exception) {
                    if (isMonitoring) onHardwareFailure?.invoke("AudioRecord: Exception - ${e.message}")
                } finally {
                    isAcousticRunning = false; try { audioRecord?.release() } catch (ex: Exception) {}
                    if (isMonitoring) try { Thread.sleep(ACOUSTIC_GENERIC_RECOVERY_DELAY_MS) } catch (ie: InterruptedException) { }
                }
            }
        }.apply { name = "AcousticMonitor"; priority = Thread.MIN_PRIORITY; start() }
    }

    private fun stopAcousticMonitoring() { isMonitoring = false; isAcousticRunning = false; acousticThread?.interrupt(); acousticThread = null }
    fun isAcousticMonitoringActive(): Boolean = isAcousticRunning

    fun consumeForensicSnapshot(): ForensicSnapshot {
        synchronized(this) {
            val snapshot = ForensicSnapshot(
                vibration = currentVibrationIndex,
                heading = currentCompassHeading,
                baroAlt = absoluteAltitude,
                lux = currentLux,
                isNear = isProximityNear,
                tiltDegrees = currentTiltDegrees,
                acousticDb = currentAcousticDb,
                peakShock = internalPeakVibration,
                peakVerticalVelocity = internalPeakVerticalVelocity,
                peakVerticalVelocityTs = internalPeakVerticalVelocityTs,
                peakVerticalVelocityRt = internalPeakVerticalVelocityRt,
                plungeMatched = !isWarming && plungeMatched,
                peakVerticalDisplacement = internalPeakVerticalDisplacement,
                proximityIdx = proximityIdx,
                proximityCm = currentProximityCm,
                proximityDebounceMs = proximityDebounceMs,
                vibrationRollingSum = vibrationRollingSum,
                acousticPeak = internalPeakDb,
                acousticMin = if (internalMinDb >= 100.0) -1.0 else internalMinDb
            )
            
            internalPeakVibration = 0.0
            internalPeakVerticalVelocity = 0.0
            internalPeakVerticalVelocityTs = 0L
            internalPeakVerticalVelocityRt = 0L
            internalPeakVerticalDisplacement = 0.0
            plungeMatched = false
            internalPeakDb = 0.0
            internalMinDb = 100.0
            
            return snapshot
        }
    }

    fun getSensorSamples(fromTs: Long, toTs: Long): List<SensorSnapshot> = sensorSampleBuffer.filter { it.ts in fromTs..toTs }
    fun getAcousticSamples(fromTs: Long, toTs: Long): List<Pair<Long, Double>> = sensorSampleBuffer.filter { it.ts in fromTs..toTs }.map { it.ts to it.acoustic }

    private fun processVibration(x: Float, y: Float, z: Float) {
        val dx = x - lastAccelX; val dy = y - lastAccelY; val dz = z - lastAccelZ
        val delta = (sqrt(dx * dx + dy * dy + dz * dz)) / 9.80665f
        synchronized(this) { if (delta.toDouble() > internalPeakVibration) internalPeakVibration = delta.toDouble(); adaptiveVibrationFloor = SentinelValidator.updateVibrationFloor(adaptiveVibrationFloor, delta.toDouble(), isWarming) }
        lastAccelX = x; lastAccelY = y; lastAccelZ = z
        val oldVal = vibrationCircularBuffer[vibrationCircularIdx]; vibrationCircularBuffer[vibrationCircularIdx] = delta.toDouble(); vibrationRollingSum = vibrationRollingSum - oldVal + delta.toDouble(); vibrationCircularIdx = (vibrationCircularIdx + 1) % VIBRATION_WINDOW_SIZE; if (vibrationBufferCount < VIBRATION_WINDOW_SIZE) vibrationBufferCount++
        currentVibrationIndex = if (vibrationBufferCount > 0) vibrationRollingSum / vibrationBufferCount else 0.0; if (currentVibrationIndex > secPeakVibe) secPeakVibe = currentVibrationIndex
        val nowRt = timeProvider.elapsedRealtime()
        if (plungePhase == 2) { if (isStationary()) { synchronized(this) { plungeMatched = true; secSitDetected = true }; plungePhase = 0 } else if (nowRt - lastPlungePhaseRt > CHAIR_PLUNGE_PHASE_TIMEOUT_MS) { plungePhase = 0 } }
        if (isStationary()) { if (stationaryStartRt == 0L) stationaryStartRt = nowRt else if (nowRt - stationaryStartRt > MUZZLE_HYSTERESIS_MS) { currentVerticalVelocity = 0.0; currentVerticalDisplacement = 0.0; if (plungePhase != 2) plungePhase = 0 } } else { stationaryStartRt = 0L }
    }

    private fun processLinearAcceleration(val0: Float, val1: Float, val2: Float, timestampNs: Long) {
        if (!hasGravity) return
        if (lastLinearAccelTs == 0L) { lastLinearAccelTs = timestampNs; return }
        val dt = (timestampNs - lastLinearAccelTs).toDouble() / 1_000_000_000.0
        lastLinearAccelTs = timestampNs
        if (dt > 0.0 && dt < 0.2) {
            val dot = val0 * gravityBuffer[0] + val1 * gravityBuffer[1] + val2 * gravityBuffer[2]
            val gravMag = sqrt(gravityBuffer[0] * gravityBuffer[0] + gravityBuffer[1] * gravityBuffer[1] + gravityBuffer[2] * gravityBuffer[2])
            val vz_accel = if (gravMag > 0.1f) dot / gravMag else 0.0f
            currentVerticalVelocity += vz_accel.toDouble() * dt; currentVerticalDisplacement += currentVerticalVelocity * dt 
            if (abs(currentVerticalVelocity) > VERTICAL_VELOCITY_MAX_MPS) currentVerticalVelocity = if (currentVerticalVelocity > 0) VERTICAL_VELOCITY_MAX_MPS else -VERTICAL_VELOCITY_MAX_MPS
            val nowRt = timeProvider.elapsedRealtime(); val wallNow = timeProvider.currentTimeMillis()
            if (plungePhase > 0 && nowRt - lastPlungePhaseRt > CHAIR_PLUNGE_PHASE_TIMEOUT_MS) plungePhase = 0
            when (plungePhase) {
                0 -> if (!isWarming && currentVerticalVelocity < -CHAIR_PLUNGE_VELOCITY_THRESHOLD) { plungePhase = 1; lastPlungePhaseRt = nowRt; currentVerticalDisplacement = 0.0 }
                1 -> { val timeInPhaseRt = nowRt - lastPlungePhaseRt; if (timeInPhaseRt > CHAIR_PLUNGE_WINDOW_MS) { plungePhase = 0 } else if (currentVerticalVelocity > -CHAIR_PLUNGE_VELOCITY_THRESHOLD * 0.2) { if (abs(currentVerticalDisplacement) > CHAIR_PLUNGE_DISTANCE_THRESHOLD) { plungePhase = 2; lastPlungePhaseRt = nowRt } else { plungePhase = 0 } } }
            }
            synchronized(this) { if (abs(currentVerticalVelocity) > abs(internalPeakVerticalVelocity)) { internalPeakVerticalVelocity = currentVerticalVelocity; internalPeakVerticalVelocityTs = wallNow; internalPeakVerticalVelocityRt = nowRt }; if (abs(currentVerticalDisplacement) > abs(internalPeakVerticalDisplacement)) internalPeakVerticalDisplacement = currentVerticalDisplacement }
        }
    }

    private fun processPressure(pressure: Float) {
        val pressureDouble = pressure.toDouble()
        if (emaPressure == 0.0) emaPressure = pressureDouble
        currentPressure = pressureDouble
        val alpha = SentinelValidator.accelerateAlpha(1.0 - BARO_EMA_SLOW, isWarming)
        emaPressure = (emaPressure * (1.0 - alpha)) + (pressureDouble * alpha)
        val nowRt = timeProvider.elapsedRealtime(); val stationaryDuration = if (stationaryStartRt > 0L) nowRt - stationaryStartRt else 0L
        if (nowRt - lastBaroZeroingRt > BARO_ZEROING_INTERVAL_MS && stationaryDuration >= PASSIVE_ZEROING_STATIONARY_MS) { emaPressure = pressureDouble; lastBaroZeroingRt = nowRt }
        val currentAlt = AndroidSensorManager.getAltitude(AndroidSensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressure).toDouble()
        val baselineAlt = AndroidSensorManager.getAltitude(AndroidSensorManager.PRESSURE_STANDARD_ATMOSPHERE, emaPressure.toFloat()).toDouble()
        absoluteAltitude = currentAlt; relativeAltitude = if (isWarming) 0.0 else currentAlt - baselineAlt
        if (abs(relativeAltitude) > secPeakLift) secPeakLift = abs(relativeAltitude)
    }

    private fun processRotation(rotationVector: FloatArray) {
        AndroidSensorManager.getRotationMatrixFromVector(currentRotationVectorMatrixBuffer, rotationVector)
        val nowRt = timeProvider.elapsedRealtime()
        if (!hasInitialRotation) { if (!isWarming && stationaryStartRt != 0L && (nowRt - stationaryStartRt > ROTATION_INIT_STATIONARY_MS)) { System.arraycopy(currentRotationVectorMatrixBuffer, 0, initialRotationMatrix, 0, 9); hasInitialRotation = true }; return }
        val dotProduct = (initialRotationMatrix[2] * currentRotationVectorMatrixBuffer[2]) + (initialRotationMatrix[5] * currentRotationVectorMatrixBuffer[5]) + (initialRotationMatrix[8] * currentRotationVectorMatrixBuffer[8])
        val cosTheta = dotProduct.coerceIn(-1.0f, 1.0f); currentTiltDegrees = if (isWarming) 0.0 else Math.toDegrees(acos(cosTheta.toDouble()))
        if (currentTiltDegrees > secPeakTilt) secPeakTilt = currentTiltDegrees
    }

    private fun updateOrientation() { if (hasGravity && hasGeomagnetic) { if (AndroidSensorManager.getRotationMatrix(rotationMatrixBuffer, inclinationMatrixBuffer, gravityBuffer, geomagneticBuffer)) { AndroidSensorManager.getOrientation(rotationMatrixBuffer, orientationBuffer); currentCompassHeading = (Math.toDegrees(orientationBuffer[0].toDouble()) + 360.0) % 360.0 } } }
    fun isStationary(): Boolean = SentinelValidator.isStationary(currentVibrationIndex, adaptiveVibrationFloor)
    fun resetBaseline() { emaPressure = currentPressure; relativeAltitude = 0.0; absoluteAltitude = AndroidSensorManager.getAltitude(AndroidSensorManager.PRESSURE_STANDARD_ATMOSPHERE, currentPressure.toFloat()).toDouble(); hasInitialRotation = false; stationaryStartRt = 0L; currentVerticalVelocity = 0.0; currentVerticalDisplacement = 0.0; plungePhase = 0; plungeMatched = false; secSitDetected = false; sessionStartRt = timeProvider.elapsedRealtime(); lastBaroZeroingRt = sessionStartRt; adaptiveVibrationFloor = VIBRATION_STATIONARY_THRESHOLD; debouncedProximityCm = -1.0; proximityDebounceMs = 0L; vibrationCircularIdx = 0; vibrationRollingSum = 0.0; vibrationBufferCount = 0; vibrationCircularBuffer.fill(0.0) }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
