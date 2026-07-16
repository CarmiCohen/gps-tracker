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
 * v9.5.0:
 * - Issue #503: Hilt Removal. Manual dependency injection.
 */
class AppSensorManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val timeProvider: TimeProvider
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as AndroidSensorManager
    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val linearAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val barometer = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
    private val proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    
    private val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    private var sensorThread: HandlerThread? = null
    private var sensorHandler: Handler? = null
    private val hasLoggedThreadInfo = AtomicBoolean(false)

    private var lastDisplayState = Display.STATE_UNKNOWN
    private var lastDisplayTransitionTs = 0L
    private val isDisplayFlickering = AtomicBoolean(false)

    private var lastStayAliveTs = 0L

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
                val now = timeProvider.elapsedRealtime()
                val delta = now - lastDisplayTransitionTs
                if (delta < 1000L) {
                    if (!isDisplayFlickering.get()) {
                        isDisplayFlickering.set(true)
                        Timber.w("Issue #037: Rapid Display Flickering detected. Engaging hardware muzzle.")
                    }
                } else {
                    isDisplayFlickering.set(false)
                }
                lastDisplayState = newState
                lastDisplayTransitionTs = now
            }
        }
    }

    private val gravityBuffer = FloatArray(3)
    private val geomagneticBuffer = FloatArray(3)
    private val gravityBufferDouble = DoubleArray(3)

    private val rotationMatrixBuffer = FloatArray(9)
    private val inclinationMatrixBuffer = FloatArray(9)
    private val orientationBuffer = FloatArray(3)
    private val currentRotationVectorMatrixBuffer = FloatArray(9)
    private var hasGravity = false
    private var hasGeomagnetic = false

    private var lastAccelX = 0.0
    private var lastAccelY = 0.0
    private var lastAccelZ = 0.0
    
    private val vibrationCircularBuffer = DoubleArray(VIBRATION_WINDOW_SIZE)
    private var vibrationCircularIdx = 0
    
    var vibrationRollingSum = 0.0
        private set

    private var vibrationBufferCount = 0

    private var internalPeakDb: Double = 0.0
    private var internalMinDb: Double = 100.0
    private var internalPeakVibration: Double = 0.0
    
    @Volatile
    private var isMonitoring = false
    @Volatile
    private var isAcousticRunning = false
    private var acousticThread: Thread? = null

    @Volatile
    private var isHighLoad = false

    data class SensorSnapshot(
        val ts: Long,
        val lux: Double,
        val vibe: Double,
        val proxIdx: Double,
        val tilt: Double,
        val lift: Double,
        val acoustic: Double
    )
    private val sensorSampleBuffer = ConcurrentLinkedQueue<SensorSnapshot>()
    private var lastBufferRecordTs = 0L

    private var secPeakLux = 0.0
    private var secPeakVibe = 0.0
    private var secMinProxIdx = 1.0
    private var secPeakTilt = 0.0
    private var secPeakLift = 0.0
    private var secPeakDb = 0.0
    
    private var fastPathFloor: Double = -1.0
    private var fastPathSpikeThreshold: Double = ACOUSTIC_THRESHOLD_DB_JUMP
    private var fastPathMinDb: Double = ACOUSTIC_MIN_THRESHOLD_DB
    private var onAcousticSpike: (() -> Unit)? = null

    private var fastPathLightBaseline: Double = -1.0
    private var fastPathLightSpikeThreshold: Double = LIGHT_THRESHOLD_LUX_JUMP
    private var onLightSpike: (() -> Unit)? = null
    
    private var lastAcousticSpikeTs: Long = 0L
    private var lastLightSpikeTs: Long = 0L

    var lastFastPathAcousticSpikeTs: Long = 0L
        private set

    private var sessionStartTs = 0L
    val isWarming: Boolean get() = (timeProvider.elapsedRealtime() - sessionStartTs < SENSOR_WARMING_MS)

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
    private var proximityMaxRange = 5.0

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

    private var stationaryStartTs: Long = 0L

    private var emaPressure: Double = 0.0
    private var lastBaroZeroingTs: Long = 0L

    private var initialRotationMatrix = FloatArray(9)
    private var hasInitialRotation = false

    fun start() {
        sessionStartTs = timeProvider.elapsedRealtime()
        lastBaroZeroingTs = sessionStartTs
        hasLoggedThreadInfo.set(false)
        proximityMaxRange = proximity?.maximumRange?.toDouble() ?: 5.0

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
        
        stepDetector?.let { sensorManager.registerListener(this, it, AndroidSensorManager.SENSOR_DELAY_NORMAL, sensorHandler) }

        displayManager.registerDisplayListener(displayListener, sensorHandler)
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        if (display != null) lastDisplayState = display.state

        startAcousticMonitoring()
    }

    fun stop() {
        stopAcousticMonitoring()
        sensorManager.unregisterListener(this)
        displayManager.unregisterDisplayListener(displayListener)
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

    override fun onSensorChanged(event: SensorEvent) {
        if (hasLoggedThreadInfo.compareAndSet(false, true)) {
            val threadName = Thread.currentThread().name
            val isMain = Looper.myLooper() == Looper.getMainLooper()
            Timber.i("Forensic: AppSensorManager offloading verified. Thread: $threadName (isMain: $isMain)")
        }

        val now = timeProvider.elapsedRealtime()
        val wallNow = timeProvider.currentTimeMillis()
        val values = event.values
        
        when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> {
                lastStayAliveTs = now
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val v0 = values[0].toDouble()
                val v1 = values[1].toDouble()
                val v2 = values[2].toDouble()
                
                gravityBuffer[0] = values[0]
                gravityBuffer[1] = values[1]
                gravityBuffer[2] = values[2]
                gravityBufferDouble[0] = v0
                gravityBufferDouble[1] = v1
                gravityBufferDouble[2] = v2
                
                hasGravity = true
                processVibration(v0, v1, v2)
                updateOrientation()
                
                if (stepDetector == null && now - lastStayAliveTs > 10000L) {
                    lastStayAliveTs = now
                    Timber.v("Stay-Alive Pulse (Accel Fallback)")
                }
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                geomagneticBuffer[0] = values[0]; geomagneticBuffer[1] = values[1]; geomagneticBuffer[2] = values[2]
                hasGeomagnetic = true; updateOrientation()
            }
            Sensor.TYPE_PRESSURE -> processPressure(values[0].toDouble())
            Sensor.TYPE_PROXIMITY -> {
                val value = values[0].toDouble()
                val newValue = value < proximityMaxRange
                currentProximityCm = value
                if (debouncedProximityCm == -1.0) debouncedProximityCm = value
                proximityIdx = (1.0 - (value / proximityMaxRange)).coerceIn(0.0, 1.0)
                if (proximityIdx < secMinProxIdx) secMinProxIdx = proximityIdx
                if (newValue != rawProximityNear) {
                    if (!newValue && isDisplayFlickering.get() && isStationary()) return
                    rawProximityNear = newValue; proximityJob?.cancel()
                    val baseDebounceMs = if (isStationary()) PROXIMITY_DEBOUNCE_STATIONARY_MS else PROXIMITY_DEBOUNCE_MOVING_MS
                    var calcDebounceMs = baseDebounceMs
                    if (isStationary() && stationaryStartTs > 0) {
                        val hoursStationary = (now - stationaryStartTs) / 3600000.0
                        calcDebounceMs += (hoursStationary * PROXIMITY_STATIONARY_SCALING_MS_PER_HOUR).toLong()
                    }
                    if (isHighLoad) calcDebounceMs = (calcDebounceMs * PROXIMITY_STRESS_SCALING_MULTIPLIER).toLong()
                    calcDebounceMs = calcDebounceMs.coerceAtMost(PROXIMITY_DEBOUNCE_MAX_MS)
                    proximityDebounceMs = calcDebounceMs
                    proximityJob = scope.launch {
                        delay(calcDebounceMs)
                        if (isActive && isProximityNear != rawProximityNear) {
                            isProximityNear = rawProximityNear; debouncedProximityCm = value
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
                            if (now - lastLightSpikeTs > SPIKE_DEBOUNCE_MS) { lastLightSpikeTs = now; onLightSpike?.invoke() }
                        }
                    }
                }
            }
            Sensor.TYPE_ROTATION_VECTOR -> processRotation(values)
        }
        
        if (now - lastBufferRecordTs >= TICK_INTERVAL_MS) {
            sensorSampleBuffer.add(SensorSnapshot(ts = wallNow, lux = secPeakLux, vibe = secPeakVibe, proxIdx = secMinProxIdx, tilt = secPeakTilt, lift = secPeakLift, acoustic = secPeakDb))
            lastBufferRecordTs = now
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
                    if (isMonitoring) onHardwareFailure?.invoke("AudioRecord: Invalid buffer size (Retrying...)")
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
                        if (isMonitoring) onHardwareFailure?.invoke("AudioRecord: Init failed (Retrying...)")
                        try { Thread.sleep(ACOUSTIC_RECOVERY_DELAY_MS) } catch (ie: InterruptedException) { break }
                        continue
                    }
                    try { audioRecord.startRecording() } catch (e: IllegalStateException) {
                        if (isMonitoring) onHardwareFailure?.invoke("AudioRecord: Mic occupied (Retrying...)")
                        try { audioRecord.release() } catch (ex: Exception) {}
                        try { Thread.sleep(ACOUSTIC_RECOVERY_DELAY_MS) } catch (ie: InterruptedException) { break }
                        continue
                    }
                    if (audioRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                        if (isMonitoring) onHardwareFailure?.invoke("AudioRecord: Contention (Retrying...)")
                        try { audioRecord.release() } catch (ex: Exception) {}
                        try { Thread.sleep(ACOUSTIC_RECOVERY_DELAY_MS) } catch (ie: InterruptedException) { break }
                        continue
                    }
                    isAcousticRunning = true
                    val buffer = ShortArray(bufferSize)
                    while (isMonitoring && !Thread.currentThread().isInterrupted) {
                        val read = audioRecord.read(buffer, 0, bufferSize)
                        if (read > 0) {
                            var maxAmp = 0; for (i in 0 until read) { val a = Math.abs(buffer[i].toInt()); if (a > maxAmp) maxAmp = a }
                            val db = if (maxAmp > 0) 20 * log10(maxAmp.toDouble() / 1.0) else 0.0
                            synchronized(this) {
                                currentAcousticDb = db; if (db > internalPeakDb) internalPeakDb = db
                                if (db < internalMinDb) internalMinDb = db; if (db > secPeakDb) secPeakDb = db
                                if (!isWarming && fastPathFloor >= 0 && (db - fastPathFloor) > fastPathSpikeThreshold && db >= fastPathMinDb) {
                                    val nowRt = timeProvider.elapsedRealtime()
                                    if (nowRt - lastAcousticSpikeTs > SPIKE_DEBOUNCE_MS) { lastAcousticSpikeTs = nowRt; lastFastPathAcousticSpikeTs = nowRt; onAcousticSpike?.invoke() }
                                }
                            }
                        } else if (read < 0) {
                            if (!isMonitoring) break
                            onHardwareFailure?.invoke("AudioRecord: Hardware error (Retrying...)"); break
                        }
                    }
                    try { audioRecord.stop() } catch (ex: Exception) {}
                } catch (e: Exception) {
                    if (isMonitoring) onHardwareFailure?.invoke("AudioRecord: Exception - ${e.message} (Retrying...)")
                } finally {
                    isAcousticRunning = false; try { audioRecord?.release() } catch (ex: Exception) {}
                    if (isMonitoring) try { Thread.sleep(ACOUSTIC_GENERIC_RECOVERY_DELAY_MS) } catch (ie: InterruptedException) { }
                }
            }
        }.apply { name = "AcousticMonitor"; priority = Thread.MIN_PRIORITY; start() }
    }

    private fun stopAcousticMonitoring() { isMonitoring = false; isAcousticRunning = false; acousticThread?.interrupt(); acousticThread = null }
    fun isAcousticMonitoringActive(): Boolean = isAcousticRunning
    fun consumeAcousticPeak(): Double { synchronized(this) { val p = internalPeakDb; internalPeakDb = 0.0; currentAcousticDb = p; return p } }
    fun consumeAcousticMin(): Double { synchronized(this) { val m = if (internalMinDb >= 100.0) -1.0 else internalMinDb; internalMinDb = 100.0; return m } }
    fun getSensorSamples(fromTs: Long, toTs: Long): List<SensorSnapshot> = sensorSampleBuffer.filter { it.ts in fromTs..toTs }
    fun getAcousticSamples(fromTs: Long, toTs: Long): List<Pair<Long, Double>> = sensorSampleBuffer.filter { it.ts in fromTs..toTs }.map { it.ts to it.acoustic }
    fun consumePeakVibration(): Double { synchronized(this) { val p = internalPeakVibration; internalPeakVibration = 0.0; return p } }

    private fun processVibration(x: Double, y: Double, z: Double) {
        val dx = x - lastAccelX; val dy = y - lastAccelY; val dz = z - lastAccelZ
        val delta = (sqrt(dx * dx + dy * dy + dz * dz)) / 9.80665
        synchronized(this) { if (delta > internalPeakVibration) internalPeakVibration = delta; adaptiveVibrationFloor = SentinelValidator.updateVibrationFloor(adaptiveVibrationFloor, delta, isWarming) }
        lastAccelX = x; lastAccelY = y; lastAccelZ = z
        val oldVal = vibrationCircularBuffer[vibrationCircularIdx]; vibrationCircularBuffer[vibrationCircularIdx] = delta; vibrationRollingSum = vibrationRollingSum - oldVal + delta; vibrationCircularIdx = (vibrationCircularIdx + 1) % VIBRATION_WINDOW_SIZE; if (vibrationBufferCount < VIBRATION_WINDOW_SIZE) vibrationBufferCount++
        currentVibrationIndex = if (vibrationBufferCount > 0) vibrationRollingSum / vibrationBufferCount else 0.0; if (currentVibrationIndex > secPeakVibe) secPeakVibe = currentVibrationIndex
        val now = timeProvider.elapsedRealtime()
        if (isStationary()) { if (stationaryStartTs == 0L) stationaryStartTs = now } else { stationaryStartTs = 0L }
    }

    private fun processPressure(pressure: Double) {
        if (emaPressure == 0.0) emaPressure = pressure
        currentPressure = pressure
        val alpha = SentinelValidator.accelerateAlpha(BARO_EMA_SLOW, isWarming)
        emaPressure = (emaPressure * (1.0 - alpha)) + (pressure * alpha)
        val now = timeProvider.elapsedRealtime(); val stationaryDuration = if (stationaryStartTs > 0L) now - stationaryStartTs else 0L
        if (now - lastBaroZeroingTs > BARO_ZEROING_INTERVAL_MS && stationaryDuration >= PASSIVE_ZEROING_STATIONARY_MS) { emaPressure = pressure; lastBaroZeroingTs = now }
        val currentAlt = AndroidSensorManager.getAltitude(AndroidSensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressure.toFloat()).toDouble()
        val baselineAlt = AndroidSensorManager.getAltitude(AndroidSensorManager.PRESSURE_STANDARD_ATMOSPHERE, emaPressure.toFloat()).toDouble()
        absoluteAltitude = currentAlt; relativeAltitude = if (isWarming) 0.0 else currentAlt - baselineAlt
        if (abs(relativeAltitude) > secPeakLift) secPeakLift = abs(relativeAltitude)
    }

    private fun processRotation(rotationVector: FloatArray) {
        AndroidSensorManager.getRotationMatrixFromVector(currentRotationVectorMatrixBuffer, rotationVector)
        val now = timeProvider.elapsedRealtime()
        if (!hasInitialRotation) { if (!isWarming && stationaryStartTs != 0L && (now - stationaryStartTs > ROTATION_INIT_STATIONARY_MS)) { System.arraycopy(currentRotationVectorMatrixBuffer, 0, initialRotationMatrix, 0, 9); hasInitialRotation = true }; return }
        val dotProduct = (initialRotationMatrix[2] * currentRotationVectorMatrixBuffer[2]) + (initialRotationMatrix[5] * currentRotationVectorMatrixBuffer[5]) + (initialRotationMatrix[8] * currentRotationVectorMatrixBuffer[8])
        val cosTheta = dotProduct.coerceIn(-1.0f, 1.0f); currentTiltDegrees = if (isWarming) 0.0 else Math.toDegrees(acos(cosTheta.toDouble()))
        if (currentTiltDegrees > secPeakTilt) secPeakTilt = currentTiltDegrees
    }

    private fun updateOrientation() { if (hasGravity && hasGeomagnetic) { if (AndroidSensorManager.getRotationMatrix(rotationMatrixBuffer, inclinationMatrixBuffer, gravityBuffer, geomagneticBuffer)) { AndroidSensorManager.getOrientation(rotationMatrixBuffer, orientationBuffer); currentCompassHeading = (Math.toDegrees(orientationBuffer[0].toDouble()) + 360.0) % 360.0 } } }
    fun isStationary(): Boolean = SentinelValidator.isStationary(currentVibrationIndex, adaptiveVibrationFloor)
    fun resetBaseline() { emaPressure = currentPressure; relativeAltitude = 0.0; absoluteAltitude = AndroidSensorManager.getAltitude(AndroidSensorManager.PRESSURE_STANDARD_ATMOSPHERE, currentPressure.toFloat()).toDouble(); hasInitialRotation = false; stationaryStartTs = 0L; sessionStartTs = timeProvider.elapsedRealtime(); lastBaroZeroingTs = sessionStartTs; adaptiveVibrationFloor = VIBRATION_STATIONARY_THRESHOLD; debouncedProximityCm = -1.0; proximityDebounceMs = 0L; vibrationCircularIdx = 0; vibrationRollingSum = 0.0; vibrationBufferCount = 0; vibrationCircularBuffer.fill(0.0) }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
