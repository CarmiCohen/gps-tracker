package com.gps19.app

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager as AndroidSensorManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.acos
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * AppSensorManager: Manages IMU and Environmental sensors.
 * v8.9.71:
 * - Forensic UI Expansion: Exposed proximityDebounceMs and vibrationRollingSum 
 *   for real-time stationary scaling verification.
 * v8.9.70:
 * - Forensic Audit Optimization: Migrated to zero-allocation buffers for matrix math.
 * - Performance: Replaced vibration list average with rolling sum (O(1)).
 * - Performance: Throttled updateOrientation with dirty flags.
 * v8.9.69:
 * - Issue #012: Adaptive Proximity Debounce logic.
 */
@Singleton
class AppSensorManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
    private val timeProvider: TimeProvider
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as AndroidSensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val linearAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val barometer = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
    private val proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private var sensorThread: HandlerThread? = null
    private var sensorHandler: Handler? = null
    private val hasLoggedThreadInfo = AtomicBoolean(false)

    // Performance Audit: Pre-allocated buffers for zero-allocation fast path
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
    
    // Performance Audit: Rolling sum for O(1) vibration average
    private val vibrationCircularBuffer = FloatArray(VIBRATION_WINDOW_SIZE)
    private var vibrationCircularIdx = 0
    
    var vibrationRollingSum = 0f
        private set

    private var vibrationBufferCount = 0

    private var internalPeakDb: Double = 0.0
    private var internalMinDb: Double = 100.0
    private var internalPeakVibration: Float = 0f
    private var internalPeakVerticalVelocity: Float = 0f
    private var internalPeakVerticalVelocityTs: Long = 0L
    private var internalPeakVerticalDisplacement: Float = 0f
    
    @Volatile
    private var isMonitoring = false
    @Volatile
    private var isAcousticRunning = false
    private var acousticThread: Thread? = null

    @Volatile
    private var isHighLoad = false

    data class SensorSnapshot(
        val ts: Long,
        val lux: Float,
        val vibe: Float,
        val proxIdx: Float,
        val tilt: Float,
        val lift: Float,
        val acoustic: Double,
        val isSitDetected: Boolean = false
    )
    private val sensorSampleBuffer = ConcurrentLinkedQueue<SensorSnapshot>()
    private var lastBufferRecordTs = 0L

    private var secPeakLux = 0f
    private var secPeakVibe = 0f
    private var secMinProxIdx = 1f
    private var secPeakTilt = 0f
    private var secPeakLift = 0f
    private var secPeakDb = 0.0
    
    @Volatile
    private var secSitDetected = false

    private var fastPathFloor: Double = -1.0
    private var fastPathSpikeThreshold: Double = ACOUSTIC_THRESHOLD_DB_JUMP
    private var fastPathMinDb: Double = ACOUSTIC_MIN_THRESHOLD_DB
    private var onAcousticSpike: (() -> Unit)? = null

    private var fastPathLightBaseline: Float = -1.0f
    private var fastPathLightSpikeThreshold: Float = LIGHT_THRESHOLD_LUX_JUMP
    private var onLightSpike: (() -> Unit)? = null
    
    private var lastAcousticSpikeTs: Long = 0L
    private var lastLightSpikeTs: Long = 0L

    var lastFastPathAcousticSpikeTs: Long = 0L
        private set

    private var sessionStartTs = 0L
    val isWarming: Boolean get() = (timeProvider.elapsedRealtime() - sessionStartTs < SENSOR_WARMING_MS)

    private var onHardwareFailure: ((String) -> Unit)? = null

    var currentVibrationIndex: Float = 0f
        private set
    
    var adaptiveVibrationFloor: Float = VIBRATION_STATIONARY_THRESHOLD
        private set

    var currentCompassHeading: Float = 0f
        private set

    var currentPressure: Float = 0f
        private set

    var absoluteAltitude: Float = 0f
        private set

    var relativeAltitude: Float = 0f
        private set

    private var proximityJob: Job? = null
    private var rawProximityNear: Boolean = true

    var isProximityNear: Boolean = true
        private set

    var proximityIdx: Float = 1.0f
        private set

    var currentProximityCm: Float = -1.0f
        private set

    var debouncedProximityCm: Float = -1.0f
        private set

    var proximityDebounceMs: Long = 0L
        private set

    var currentLux: Float = 0f
        private set

    var currentTiltDegrees: Float = 0f
        private set

    var currentAcousticDb: Double = 0.0
        private set
        
    var latestAcousticDb: Double = 0.0
        private set

    var currentVerticalVelocity: Float = 0f
        private set

    var currentVerticalDisplacement: Float = 0f
        private set

    private var lastLinearAccelTs: Long = 0L
    private var stationaryStartTs: Long = 0L

    private var emaPressure: Float = 0f
    private var lastBaroZeroingTs: Long = 0L

    private var initialRotationMatrix = FloatArray(9)
    private var hasInitialRotation = false

    private var plungePhase = 0 
    private var plungeMatched = false
    private var lastPlungePhaseTs = 0L

    fun start() {
        sessionStartTs = timeProvider.elapsedRealtime()
        lastBaroZeroingTs = sessionStartTs
        hasLoggedThreadInfo.set(false)

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
        startAcousticMonitoring()
    }

    fun stop() {
        stopAcousticMonitoring()
        sensorManager.unregisterListener(this)
        proximityJob?.cancel()
        sensorThread?.quitSafely()
        sensorThread = null
        sensorHandler = null
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

    fun setLightFastPath(baseline: Float, spikeThreshold: Float, onSpike: () -> Unit) {
        synchronized(this) {
            this.fastPathLightBaseline = baseline
            this.fastPathLightSpikeThreshold = spikeThreshold
            this.onLightSpike = onSpike
        }
    }

    fun setHighLoad(high: Boolean) {
        this.isHighLoad = high
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (hasLoggedThreadInfo.compareAndSet(false, true)) {
            val threadName = Thread.currentThread().name
            val isMain = Looper.myLooper() == Looper.getMainLooper()
            Timber.i("Forensic: AppSensorManager offloading verified. Thread: $threadName (isMain: $isMain)")
        }

        val now = timeProvider.elapsedRealtime()
        val wallNow = timeProvider.currentTimeMillis()
        
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, gravityBuffer, 0, 3)
                hasGravity = true
                processVibration(event.values[0], event.values[1], event.values[2])
                updateOrientation()
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                processLinearAcceleration(event.values, event.timestamp)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, geomagneticBuffer, 0, 3)
                hasGeomagnetic = true
                updateOrientation()
            }
            Sensor.TYPE_PRESSURE -> {
                processPressure(event.values[0])
            }
            Sensor.TYPE_PROXIMITY -> {
                val maxRange = proximity?.maximumRange ?: 5f
                val value = event.values[0]
                val newValue = value < maxRange
                
                currentProximityCm = value
                if (debouncedProximityCm == -1.0f) debouncedProximityCm = value
                
                proximityIdx = (1.0f - (value / maxRange)).coerceIn(0f, 1f)
                if (proximityIdx < secMinProxIdx) secMinProxIdx = proximityIdx

                if (newValue != rawProximityNear) {
                    if (!newValue && isA15Device() && currentLux <= 0.01f && isStationary()) {
                        Timber.d("Proximity: Suppressing 'Far' transition on A15 in darkness (Stationary Virtual Sensor Protection)")
                        return
                    }

                    rawProximityNear = newValue
                    proximityJob?.cancel()
                    
                    val baseDebounceMs = if (isStationary()) {
                        if (isA15Device()) PROXIMITY_DEBOUNCE_STATIONARY_A15_MS else PROXIMITY_DEBOUNCE_STATIONARY_MS
                    } else {
                        PROXIMITY_DEBOUNCE_MOVING_MS
                    }

                    // Issue #012: Adaptive Proximity Debounce
                    var calcDebounceMs = baseDebounceMs
                    if (isStationary() && stationaryStartTs > 0) {
                        val hoursStationary = (now - stationaryStartTs) / 3600000f
                        val stationaryExtra = (hoursStationary * PROXIMITY_STATIONARY_SCALING_MS_PER_HOUR).toLong()
                        calcDebounceMs += stationaryExtra
                    }
                    if (isHighLoad) {
                        calcDebounceMs = (calcDebounceMs * PROXIMITY_STRESS_SCALING_MULTIPLIER).toLong()
                    }
                    calcDebounceMs = calcDebounceMs.coerceAtMost(PROXIMITY_DEBOUNCE_MAX_MS)
                    proximityDebounceMs = calcDebounceMs
                    
                    proximityJob = scope.launch {
                        delay(calcDebounceMs)
                        if (isActive && isProximityNear != rawProximityNear) {
                            isProximityNear = rawProximityNear
                            debouncedProximityCm = value
                            Timber.d("Proximity state debounced to: $isProximityNear (Cm: $debouncedProximityCm, Debounce: ${calcDebounceMs}ms)")
                        }
                    }
                }
            }
            Sensor.TYPE_LIGHT -> {
                val lux = event.values[0]
                currentLux = lux
                if (lux > secPeakLux) secPeakLux = lux
                
                synchronized(this) {
                    if (fastPathLightBaseline < 0) {
                        fastPathLightBaseline = lux
                    } else {
                        val alpha = SentinelValidator.accelerateAlpha(LUX_EMA_FAST, isWarming)
                        fastPathLightBaseline = (fastPathLightBaseline * (1f - alpha)) + (lux * alpha)
                        
                        if (!isWarming && onLightSpike != null && (lux - fastPathLightBaseline) > fastPathLightSpikeThreshold) {
                            if (now - lastLightSpikeTs > SPIKE_DEBOUNCE_MS) {
                                lastLightSpikeTs = now
                                onLightSpike?.invoke()
                            }
                        }
                    }
                }
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                processRotation(event.values)
            }
        }
        
        if (now - lastBufferRecordTs >= TICK_INTERVAL_MS) {
            val sitForForensics: Boolean
            synchronized(this) {
                sitForForensics = secSitDetected
                secSitDetected = false 
            }

            sensorSampleBuffer.add(SensorSnapshot(
                ts = wallNow,
                lux = secPeakLux,
                vibe = secPeakVibe,
                proxIdx = secMinProxIdx,
                tilt = secPeakTilt,
                lift = secPeakLift,
                acoustic = secPeakDb,
                isSitDetected = sitForForensics
            ))
            lastBufferRecordTs = now
            
            secPeakLux = currentLux
            secPeakVibe = currentVibrationIndex
            secMinProxIdx = proximityIdx
            secPeakTilt = currentTiltDegrees
            secPeakLift = abs(relativeAltitude)
            secPeakDb = latestAcousticDb
            
            while (sensorSampleBuffer.size > 0 && (wallNow - (sensorSampleBuffer.peek()?.ts ?: wallNow)) > SENSOR_SAMPLE_BUFFER_MAX_AGE_MS) {
                sensorSampleBuffer.poll()
            }
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
                    if (isMonitoring) {
                        Timber.w("Invalid buffer size for AudioRecord")
                        onHardwareFailure?.invoke("AudioRecord: Invalid buffer size (Retrying...)")
                    }
                    try { Thread.sleep(ACOUSTIC_RECOVERY_DELAY_MS) } catch (ie: InterruptedException) { break }
                    continue
                }
                
                var audioRecord: AudioRecord? = null
                try {
                    var attempts = 0
                    while (attempts < ACOUSTIC_INIT_RETRY_COUNT && isMonitoring) {
                        val record = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
                        if (record.state == AudioRecord.STATE_INITIALIZED) {
                            audioRecord = record
                            break
                        }
                        record.release()
                        attempts++
                        try { Thread.sleep(ACOUSTIC_INIT_RETRY_DELAY_MS) } catch (ie: InterruptedException) { break }
                    }

                    if (!isMonitoring) break

                    if (audioRecord == null || audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                        if (isMonitoring) {
                            Timber.w("AudioRecord: Failed to initialize. Retrying in ${ACOUSTIC_RECOVERY_DELAY_MS/1000}s...")
                            onHardwareFailure?.invoke("AudioRecord: Init failed (Retrying...)")
                        }
                        try { Thread.sleep(ACOUSTIC_RECOVERY_DELAY_MS) } catch (ie: InterruptedException) { break }
                        continue
                    }

                    try {
                        audioRecord.startRecording()
                    } catch (e: IllegalStateException) {
                        if (isMonitoring) {
                            Timber.w(e, "AudioRecord.startRecording() failed")
                            onHardwareFailure?.invoke("AudioRecord: Mic occupied (Retrying...)")
                        }
                        try { audioRecord.release() } catch (ex: Exception) {}
                        try { Thread.sleep(ACOUSTIC_RECOVERY_DELAY_MS) } catch (ie: InterruptedException) { break }
                        continue
                    }

                    if (audioRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                        if (isMonitoring) {
                            Timber.w("AudioRecord failed to enter recording state")
                            onHardwareFailure?.invoke("AudioRecord: Contention (Retrying...)")
                        }
                        try { audioRecord.release() } catch (ex: Exception) {}
                        try { Thread.sleep(ACOUSTIC_RECOVERY_DELAY_MS) } catch (ie: InterruptedException) { break }
                        continue
                    }

                    isAcousticRunning = true
                    Timber.d("Acoustic Monitoring Started")

                    val buffer = ShortArray(bufferSize)
                    while (isMonitoring && !Thread.currentThread().isInterrupted) {
                        val read = audioRecord.read(buffer, 0, bufferSize)
                        if (read > 0) {
                            var maxAmp = 0
                            for (i in 0 until read) {
                                val a = Math.abs(buffer[i].toInt())
                                if (a > maxAmp) maxAmp = a
                            }
                            val db = if (maxAmp > 0) 20 * log10(maxAmp.toDouble() / 1.0) else 0.0
                            synchronized(this) {
                                latestAcousticDb = db
                                if (db > internalPeakDb) internalPeakDb = db
                                if (db < internalMinDb) internalMinDb = db
                                if (db > secPeakDb) secPeakDb = db

                                // Issue #010: Acoustic/Vibration Coherence for Fast Path.
                                // If A15, require vibration > 0.01g to validate spike.
                                val vibrationForCoherence = currentVibrationIndex
                                val isMuzzledByA15Noise = isA15Device() && vibrationForCoherence < 0.01f

                                if (!isWarming && !isMuzzledByA15Noise && fastPathFloor >= 0 && (db - fastPathFloor) > fastPathSpikeThreshold && db >= fastPathMinDb) {
                                    val nowRt = timeProvider.elapsedRealtime()
                                    synchronized(this@AppSensorManager) {
                                        if (nowRt - lastAcousticSpikeTs > SPIKE_DEBOUNCE_MS) {
                                            lastAcousticSpikeTs = nowRt
                                            lastFastPathAcousticSpikeTs = nowRt 
                                            onAcousticSpike?.invoke()
                                        }
                                    }
                                }
                            }
                        } else if (read < 0) {
                            if (!isMonitoring) break
                            val errorMsg = when(read) {
                                AudioRecord.ERROR_INVALID_OPERATION -> "INVALID_OPERATION"
                                AudioRecord.ERROR_BAD_VALUE -> "BAD_VALUE"
                                AudioRecord.ERROR_DEAD_OBJECT -> "DEAD_OBJECT"
                                else -> "Unknown error $read"
                            }
                            Timber.w("AudioRecord.read() error: $errorMsg")
                            onHardwareFailure?.invoke("AudioRecord: Hardware error - $errorMsg (Retrying...)")
                            break
                        }
                    }
                    try { audioRecord.stop() } catch (ex: Exception) {}
                } catch (e: Exception) {
                    if (isMonitoring) {
                        Timber.w(e, "Acoustic monitoring failed")
                        onHardwareFailure?.invoke("AudioRecord: Exception - ${e.message} (Retrying...)")
                    }
                } finally {
                    isAcousticRunning = false
                    try { audioRecord?.release() } catch (ex: Exception) {}
                    if (isMonitoring) {
                        try { Thread.sleep(ACOUSTIC_GENERIC_RECOVERY_DELAY_MS) } catch (ie: InterruptedException) { }
                    }
                }
            }
        }.apply { 
            name = "AcousticMonitor"
            priority = Thread.MIN_PRIORITY
            start() 
        }
    }

    private fun stopAcousticMonitoring() {
        isMonitoring = false
        isAcousticRunning = false
        acousticThread?.interrupt()
        acousticThread = null
    }

    fun isAcousticMonitoringActive(): Boolean = isAcousticRunning

    fun consumeAcousticPeak(): Double {
        synchronized(this) {
            val p = internalPeakDb
            internalPeakDb = 0.0
            currentAcousticDb = p
            return p
        }
    }

    fun consumeAcousticMin(): Double {
        synchronized(this) {
            val m = if (internalMinDb >= 100.0) -1.0 else internalMinDb
            internalMinDb = 100.0
            return m
        }
    }

    fun getSensorSamples(fromTs: Long, toTs: Long): List<SensorSnapshot> {
        return sensorSampleBuffer.filter { it.ts in fromTs..toTs }
    }

    fun getAcousticSamples(fromTs: Long, toTs: Long): List<Pair<Long, Double>> {
        return sensorSampleBuffer.filter { it.ts in fromTs..toTs }.map { it.ts to it.acoustic }
    }

    fun consumePeakVibration(): Float {
        synchronized(this) {
            val p = internalPeakVibration
            internalPeakVibration = 0f
            return p
        }
    }

    fun consumePeakVerticalVelocity(): Float {
        synchronized(this) {
            val p = internalPeakVerticalVelocity
            internalPeakVerticalVelocity = 0f
            return p
        }
    }

    fun consumePeakVerticalVelocityTs(): Long {
        synchronized(this) {
            val p = internalPeakVerticalVelocityTs
            internalPeakVerticalVelocityTs = 0L
            return p
        }
    }

    fun consumePeakVerticalDisplacement(): Float {
        synchronized(this) {
            val p = internalPeakVerticalDisplacement
            internalPeakVerticalDisplacement = 0f
            return p
        }
    }

    fun consumePlungeMatched(): Boolean {
        synchronized(this) {
            val m = !isWarming && plungeMatched
            plungeMatched = false
            return m
        }
    }

    private fun processVibration(x: Float, y: Float, z: Float) {
        val dx = x - lastAccelX
        val dy = y - lastAccelY
        val dz = z - lastAccelZ
        val delta = (sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()) / GRAVITY_EARTH

        synchronized(this) {
            if (delta > internalPeakVibration) internalPeakVibration = delta
            adaptiveVibrationFloor = SentinelValidator.updateVibrationFloor(adaptiveVibrationFloor, delta, isWarming)
        }

        lastAccelX = x; lastAccelY = y; lastAccelZ = z
        
        // Performance Audit: Rolling sum Circular Buffer (O(1))
        val oldVal = vibrationCircularBuffer[vibrationCircularIdx]
        vibrationCircularBuffer[vibrationCircularIdx] = delta
        vibrationRollingSum = vibrationRollingSum - oldVal + delta
        vibrationCircularIdx = (vibrationCircularIdx + 1) % VIBRATION_WINDOW_SIZE
        if (vibrationBufferCount < VIBRATION_WINDOW_SIZE) vibrationBufferCount++
        
        currentVibrationIndex = if (vibrationBufferCount > 0) vibrationRollingSum / vibrationBufferCount else 0f
        if (currentVibrationIndex > secPeakVibe) secPeakVibe = currentVibrationIndex
        
        val now = timeProvider.elapsedRealtime()

        if (plungePhase == 2) {
            if (isStationary()) {
                synchronized(this) { 
                    plungeMatched = true
                    secSitDetected = true 
                }
                plungePhase = 0
            } else if (now - lastPlungePhaseTs > CHAIR_PLUNGE_PHASE_TIMEOUT_MS) {
                plungePhase = 0
            }
        }

        if (isStationary()) {
            if (stationaryStartTs == 0L) stationaryStartTs = now
            else if (now - stationaryStartTs > MUZZLE_HYSTERESIS_MS) {
                currentVerticalVelocity = 0f
                currentVerticalDisplacement = 0f
                if (plungePhase != 2) plungePhase = 0 
            }
        } else {
            stationaryStartTs = 0L
        }
    }

    private fun processLinearAcceleration(values: FloatArray, timestampNs: Long) {
        if (!hasGravity) return
        if (lastLinearAccelTs == 0L) {
            lastLinearAccelTs = timestampNs
            return
        }
        val dt = (timestampNs - lastLinearAccelTs) / 1_000_000_000f
        lastLinearAccelTs = timestampNs
        
        if (dt > 0 && dt < 0.2f) {
            val dot = values[0] * gravityBuffer[0] + values[1] * gravityBuffer[1] + values[2] * gravityBuffer[2]
            val gravMag = sqrt(gravityBuffer[0]*gravityBuffer[0] + gravityBuffer[1]*gravityBuffer[1] + gravityBuffer[2]*gravityBuffer[2])
            val vz_accel = if (gravMag > 0.1f) dot / gravMag else 0f

            currentVerticalVelocity += vz_accel * dt
            currentVerticalDisplacement += currentVerticalVelocity * dt 
            
            if (abs(currentVerticalVelocity) > VERTICAL_VELOCITY_MAX_MPS) {
                currentVerticalVelocity = if (currentVerticalVelocity > 0) VERTICAL_VELOCITY_MAX_MPS else -VERTICAL_VELOCITY_MAX_MPS
            }

            val now = timeProvider.elapsedRealtime()
            val wallNow = timeProvider.currentTimeMillis()
            if (plungePhase > 0 && now - lastPlungePhaseTs > CHAIR_PLUNGE_PHASE_TIMEOUT_MS) plungePhase = 0

            when (plungePhase) {
                0 -> {
                    if (!isWarming && currentVerticalVelocity < -CHAIR_PLUNGE_VELOCITY_THRESHOLD) {
                        plungePhase = 1
                        lastPlungePhaseTs = now
                        currentVerticalDisplacement = 0f 
                    }
                }
                1 -> {
                    val timeInPhase = now - lastPlungePhaseTs
                    if (timeInPhase > CHAIR_PLUNGE_WINDOW_MS) {
                        plungePhase = 0 
                    } else if (currentVerticalVelocity > -CHAIR_PLUNGE_VELOCITY_THRESHOLD * 0.2f) {
                        if (abs(currentVerticalDisplacement) > CHAIR_PLUNGE_DISTANCE_THRESHOLD) {
                            plungePhase = 2
                            lastPlungePhaseTs = now
                        } else {
                            plungePhase = 0
                        }
                    }
                }
            }

            synchronized(this) {
                if (abs(currentVerticalVelocity) > abs(internalPeakVerticalVelocity)) {
                    internalPeakVerticalVelocity = currentVerticalVelocity
                    internalPeakVerticalVelocityTs = wallNow
                }
                if (abs(currentVerticalDisplacement) > abs(internalPeakVerticalDisplacement)) {
                    internalPeakVerticalDisplacement = currentVerticalDisplacement
                }
            }
        }
    }

    private fun processPressure(pressure: Float) {
        if (emaPressure == 0f) emaPressure = pressure
        currentPressure = pressure
        
        val alpha = SentinelValidator.accelerateAlpha(1f - BARO_EMA_SLOW, isWarming)
        emaPressure = (emaPressure * (1f - alpha)) + (pressure * alpha)
        
        val now = timeProvider.elapsedRealtime()
        val stationaryDuration = if (stationaryStartTs > 0L) now - stationaryStartTs else 0L
        
        if (now - lastBaroZeroingTs > BARO_ZEROING_INTERVAL_MS && stationaryDuration >= PASSIVE_ZEROING_STATIONARY_MS) {
            emaPressure = pressure
            lastBaroZeroingTs = now
        }

        val currentAlt = AndroidSensorManager.getAltitude(AndroidSensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressure)
        val baselineAlt = AndroidSensorManager.getAltitude(AndroidSensorManager.PRESSURE_STANDARD_ATMOSPHERE, emaPressure)
        
        absoluteAltitude = currentAlt
        relativeAltitude = if (isWarming) 0f else currentAlt - baselineAlt
        
        if (abs(relativeAltitude) > secPeakLift) secPeakLift = abs(relativeAltitude)
    }

    private fun processRotation(rotationVector: FloatArray) {
        AndroidSensorManager.getRotationMatrixFromVector(currentRotationVectorMatrixBuffer, rotationVector)
        
        val now = timeProvider.elapsedRealtime()
        if (!hasInitialRotation) {
            if (!isWarming && stationaryStartTs != 0L && (now - stationaryStartTs > ROTATION_INIT_STATIONARY_MS)) {
                System.arraycopy(currentRotationVectorMatrixBuffer, 0, initialRotationMatrix, 0, 9)
                hasInitialRotation = true
            }
            return
        }
        val dotProduct = (initialRotationMatrix[2] * currentRotationVectorMatrixBuffer[2]) +
                         (initialRotationMatrix[5] * currentRotationVectorMatrixBuffer[5]) +
                         (initialRotationMatrix[8] * currentRotationVectorMatrixBuffer[8])
        val cosTheta = dotProduct.coerceIn(-1.0f, 1.0f)
        currentTiltDegrees = if (isWarming) 0f else Math.toDegrees(acos(cosTheta.toDouble())).toFloat()
        if (currentTiltDegrees > secPeakTilt) secPeakTilt = currentTiltDegrees
    }

    private fun updateOrientation() {
        if (hasGravity && hasGeomagnetic) {
            // Performance Audit: Zero-allocation orientation update
            if (AndroidSensorManager.getRotationMatrix(rotationMatrixBuffer, inclinationMatrixBuffer, gravityBuffer, geomagneticBuffer)) {
                AndroidSensorManager.getOrientation(rotationMatrixBuffer, orientationBuffer)
                currentCompassHeading = (Math.toDegrees(orientationBuffer[0].toDouble()).toFloat() + 360) % 360
            }
        }
    }

    fun isStationary(): Boolean = SentinelValidator.isStationary(currentVibrationIndex, adaptiveVibrationFloor)

    fun resetBaseline() {
        emaPressure = currentPressure
        relativeAltitude = 0f
        absoluteAltitude = AndroidSensorManager.getAltitude(AndroidSensorManager.PRESSURE_STANDARD_ATMOSPHERE, currentPressure)
        hasInitialRotation = false
        stationaryStartTs = 0L
        currentVerticalVelocity = 0f
        currentVerticalDisplacement = 0f
        plungePhase = 0
        plungeMatched = false
        secSitDetected = false
        sessionStartTs = timeProvider.elapsedRealtime()
        lastBaroZeroingTs = sessionStartTs
        adaptiveVibrationFloor = VIBRATION_STATIONARY_THRESHOLD
        debouncedProximityCm = -1.0f
        proximityDebounceMs = 0L
        
        // Reset vibration rolling state
        vibrationCircularIdx = 0
        vibrationRollingSum = 0f
        vibrationBufferCount = 0
        vibrationCircularBuffer.fill(0f)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
