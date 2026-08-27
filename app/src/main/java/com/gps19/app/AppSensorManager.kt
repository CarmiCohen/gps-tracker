package com.gps19.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager as AndroidSensorManager
import android.hardware.display.DisplayManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.Display
import androidx.core.content.ContextCompat
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * AppSensorManager: Manages IMU, Environmental sensors, and Display state transitions.
 * Aug.27.02:
 * - Issue #745 Hardening: Hardened lifecycle stop() to prevent BaseEventQueue leaks. 
 *   Ensured unregistration is queued on the sensor thread before quitting, and 
 *   added join() to acoustic monitoring shutdown (R745).
 * Aug.26.19:
 * - Issue #742 Hardening: Tracked step-detector registration job to ensure 
 *   proper cancellation during stop(), preventing BaseEventQueue leaks from 
 *   escaped async registrations (R742). Fixed kineticEnergy member declaration 
 *   and typo in acoustic monitoring status.
 */
@Singleton
class AppSensorManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
    private val timeProvider: TimeProvider,
    private val systemMonitor: SystemMonitor,
    private val systemStatusProvider: SystemStatusProvider
) : SensorEventListener {

    private val _sensorEvents = MutableSharedFlow<AppSensorEvent>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val sensorEvents: SharedFlow<AppSensorEvent> = _sensorEvents.asSharedFlow()

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
    private var registrationJob: Job? = null

    private var sensorThread: HandlerThread? = null
    private var sensorHandler: Handler? = null
    private val hasLoggedThreadInfo = AtomicBoolean(false)
    private val isStarted = AtomicBoolean(false)
    private val lifecycleLock = Any()

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
                if (nowRt - lastDisplayTransitionRt < 1000L) {
                    if (!isDisplayFlickering.get()) {
                        isDisplayFlickering.set(true)
                        Timber.w("Forensic: Rapid Display Flickering detected.")
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
    var vibrationRollingSum = 0.0; private set
    private var vibrationBufferCount = 0

    private var logicPeakDb = 0.0
    private var logicMinDb = 100.0
    private var logicPeakVibration = 0.0
    private var logicPeakVerticalVelocity = 0.0
    private var logicPeakVerticalVelocityTs = 0L
    private var logicPeakVerticalVelocityRt = 0L
    private var logicPeakVerticalDisplacement = 0.0
    
    private var forensicPeakDb = 0.0
    private var forensicMinDb = 100.0
    private var forensicPeakVibration = 0.0
    private var forensicPeakVerticalVelocity = 0.0
    private var forensicPeakVerticalVelocityTs = 0L
    private var forensicPeakVerticalVelocityRt = 0L
    private var forensicPeakVerticalDisplacement = 0.0

    private var lastRawVibe = 0.0
    private var lastHpfValue = 0.0
    private var currentKineticEnergy = 0.0

    @Volatile private var isMonitoring = false
    @Volatile private var isAcousticRunning = false
    private var acousticThread: Thread? = null

    @Volatile private var isHighLoad = false
    @Volatile private var powerSaveMode = false

    class ForensicSnapshot {
        var vibration = 0.0; var heading = 0.0; var baroAlt = 0.0; var lux = 0.0
        var isNear = true; var tiltDegrees = 0.0; var acousticDb = 0.0; var peakShock = 0.0
        var peakVerticalVelocity = 0.0; var peakVerticalVelocityTs = 0L; var peakVerticalVelocityRt = 0L
        var plungeMatched = false; var peakVerticalDisplacement = 0.0; var proximityIdx = 0.0
        var proximityCm = 0.0; var proximityDebounceMs = 0L; var vibrationRollingSum = 0.0
        var acousticPeak = 0.0; var acousticMin = 0.0; var kineticEnergy = 0.0

        fun reset() {
            vibration = 0.0; heading = 0.0; baroAlt = 0.0; lux = 0.0
            isNear = true; tiltDegrees = 0.0; acousticDb = 0.0; peakShock = 0.0
            peakVerticalVelocity = 0.0; peakVerticalVelocityTs = 0L; peakVerticalVelocityRt = 0L
            plungeMatched = false; peakVerticalDisplacement = 0.0; proximityIdx = 0.0
            proximityCm = 0.0; proximityDebounceMs = 0L; vibrationRollingSum = 0.0
            acousticPeak = 0.0; acousticMin = 0.0; kineticEnergy = 0.0
        }
    }

    private val logicSnapshotPool = Array(2) { ForensicSnapshot() }
    private var logicSnapshotIdx = 0
    
    private val forensicSnapshotPool = Array(4) { ForensicSnapshot() }
    private var forensicSnapshotIdx = 0

    private val bufferTs = LongArray(256); private val bufferRt = LongArray(256)
    private val bufferLux = DoubleArray(256); private val bufferVibe = DoubleArray(256)
    private val bufferProxIdx = DoubleArray(256); private val bufferTilt = DoubleArray(256)
    private val bufferLift = DoubleArray(256); private val bufferAcoustic = DoubleArray(256)
    private val bufferSit = BooleanArray(256); private val bufferKinetic = DoubleArray(256)
    private var bufferIdx = 0; private var bufferCount = 0
    private var lastBufferRecordRt = 0L

    private var secPeakLux = 0.0; private var secPeakVibe = 0.0
    private var secSumProxIdx = 0.0; private var secProxCount = 0
    private var secPeakTilt = 0.0; private var secPeakLift = 0.0; private var secPeakDb = 0.0
    private var secSitDetected = false; private var secPeakKinetic = 0.0
    
    private var fastPathFloor = -1.0; private var fastPathSpikeThreshold = ACOUSTIC_THRESHOLD_DB_JUMP
    private var fastPathMinDb = ACOUSTIC_MIN_THRESHOLD_DB; private var onAcousticSpike: (() -> Unit)? = null
    private var fastPathLightBaseline = -1.0; private var fastPathLightSpikeThreshold = LIGHT_THRESHOLD_LUX_JUMP
    private var onLightSpike: (() -> Unit)? = null
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

    fun start() {
        synchronized(lifecycleLock) {
            if (isStarted.getAndSet(true)) return
            sessionStartRt = timeProvider.elapsedRealtime(); lastBaroZeroingRt = sessionStartRt
            hasLoggedThreadInfo.set(false); proximityMaxRange = proximity?.maximumRange ?: 5f
            
            if (sensorThread == null) {
                sensorThread = HandlerThread("AppSensorThread").apply { start() }
                sensorHandler = Handler(sensorThread!!.looper)
            }

            displayManager.registerDisplayListener(displayListener, sensorHandler)
            val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            if (display != null) lastDisplayState = display.state

            registerSensors()
            startAcousticMonitoring()
        }
    }

    private fun registerSensors() {
        val delay = if (powerSaveMode) AndroidSensorManager.SENSOR_DELAY_NORMAL else AndroidSensorManager.SENSOR_DELAY_FASTEST
        
        accelerometer?.let { sensorManager.registerListener(this, it, delay, sensorHandler) }
        linearAccel?.let { sensorManager.registerListener(this, it, delay, sensorHandler) }
        magnetometer?.let { sensorManager.registerListener(this, it, AndroidSensorManager.SENSOR_DELAY_NORMAL, sensorHandler) }
        barometer?.let { sensorManager.registerListener(this, it, AndroidSensorManager.SENSOR_DELAY_NORMAL, sensorHandler) }
        proximity?.let { sensorManager.registerListener(this, it, AndroidSensorManager.SENSOR_DELAY_NORMAL, sensorHandler) }
        light?.let { sensorManager.registerListener(this, it, AndroidSensorManager.SENSOR_DELAY_NORMAL, sensorHandler) }
        rotationVector?.let { sensorManager.registerListener(this, it, delay, sensorHandler) }

        attemptStepDetectorRegistration(); startStepDetectorRecoveryLoop()
    }

    fun stop() {
        synchronized(lifecycleLock) {
            if (!isStarted.getAndSet(false)) return
            
            // Issue #745: Immediate cancellation of async jobs
            recoveryJob?.cancel(); recoveryJob = null
            registrationJob?.cancel(); registrationJob = null
            proximityJob?.cancel(); proximityJob = null
            
            stopAcousticMonitoring()
            
            // Issue #745: Ensure unregistration is processed on the sensor thread BEFORE it quits.
            // This prevents the native event queue from being leaked during disposal.
            sensorHandler?.post {
                try {
                    sensorManager.unregisterListener(this)
                    displayManager.unregisterDisplayListener(displayListener)
                } catch (e: Exception) {
                    Timber.e(e, "Error during sensor unregistration on thread")
                }
            }

            // Small delay to allow the unregistration message to be processed by the Looper
            sensorThread?.quitSafely()
            
            try {
                sensorThread?.join(1000)
            } catch (e: InterruptedException) {
                Timber.e("Sensor thread join interrupted")
            }
            
            sensorThread = null
            sensorHandler = null
            isStepDetectorRegistered = false
            
            Timber.i("AppSensorManager: Synchronous stop completed.")
        }
    }

    private fun startStepDetectorRecoveryLoop() {
        recoveryJob?.cancel()
        recoveryJob = scope.launch {
            while (isActive) {
                delay(300000L) 
                if (!isStepDetectorRegistered) { attemptStepRegistration() }
            }
        }
    }

    private fun attemptStepDetectorRegistration() {
        val detector = stepDetector ?: return
        
        synchronized(lifecycleLock) {
            registrationJob?.cancel()
            registrationJob = scope.launch(Dispatchers.IO) {
                val isGranted = systemStatusProvider.isActivityRecognitionGranted()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isGranted) {
                    isStepDetectorRegistered = false
                    return@launch
                }
                
                val targetHandler = synchronized(lifecycleLock) {
                    if (!isStarted.get()) return@launch
                    sensorHandler
                } ?: return@launch

                withContext(targetHandler.asCoroutineDispatcher()) {
                    sensorManager.unregisterListener(this@AppSensorManager, detector)
                    synchronized(lifecycleLock) {
                        if (isStarted.get()) {
                            isStepDetectorRegistered = sensorManager.registerListener(this@AppSensorManager, detector, AndroidSensorManager.SENSOR_DELAY_NORMAL, sensorHandler)
                        }
                    }
                }
            }
        }
    }

    private fun attemptStepRegistration() = attemptStepDetectorRegistration()

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

    fun setHighLoad(high: Boolean) { this.isHighLoad = high }

    fun setPowerSaveMode(active: Boolean) {
        synchronized(lifecycleLock) {
            if (this.powerSaveMode != active) {
                this.powerSaveMode = active
                if (isStarted.get() && sensorHandler != null) { 
                    sensorManager.unregisterListener(this)
                    registerSensors() 
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (hasLoggedThreadInfo.compareAndSet(false, true)) {
            Timber.i("Forensic: AppSensorManager offloading verified. Thread: ${Thread.currentThread().name}")
        }
        val nowRt = timeProvider.elapsedRealtime(); val wallNow = timeProvider.currentTimeMillis()
        val values = event.values
        when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> lastStayAliveRt = nowRt
            Sensor.TYPE_ACCELEROMETER -> {
                gravityBuffer[0] = values[0]; gravityBuffer[1] = values[1]; gravityBuffer[2] = values[2]
                hasGravity = true; processVibration(values[0], values[1], values[2]); updateOrientation()
                if (!isStepDetectorRegistered && nowRt - lastStayAliveRt > 10000L) {
                    lastStayAliveRt = nowRt; systemMonitor.acquireWakeLock(force = true)
                }
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> processLinearAcceleration(values[0], values[1], values[2], event.timestamp)
            Sensor.TYPE_MAGNETIC_FIELD -> { 
                geomagneticBuffer[0] = values[0]; geomagneticBuffer[1] = values[1]; geomagneticBuffer[2] = values[2]
                hasGeomagnetic = true; updateOrientation() 
            }
            Sensor.TYPE_PRESSURE -> processPressure(values[0])
            Sensor.TYPE_PROXIMITY -> {
                val value = values[0]; val newValue = value < proximityMaxRange
                currentProximityCm = value.toDouble(); if (debouncedProximityCm == -1.0) debouncedProximityCm = value.toDouble()
                
                val rawIdx = (1.0 - (value / proximityMaxRange)).toDouble().coerceIn(0.0, 1.0)
                proximityIdx = (proximityIdx * (1.0 - PROXIMITY_EMA_ALPHA)) + (rawIdx * PROXIMITY_EMA_ALPHA)
                
                secSumProxIdx += proximityIdx
                secProxCount++
                
                if (newValue != rawProximityNear) {
                    if (!newValue && isDisplayFlickering.get() && isStationary()) return
                    rawProximityNear = newValue; proximityJob?.cancel()
                    var calcDebounceMs = if (isStationary()) PROXIMITY_DEBOUNCE_STATIONARY_MS else PROXIMITY_DEBOUNCE_MOVING_MS
                    if (isStationary() && stationaryStartRt > 0L) {
                        calcDebounceMs += (((nowRt - stationaryStartRt) / 3600000.0) * PROXIMITY_STATIONARY_SCALING_MS_PER_HOUR).toLong()
                    }
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
            synchronized(this) {
                bufferTs[bufferIdx] = wallNow; bufferRt[bufferIdx] = nowRt; bufferLux[bufferIdx] = secPeakLux; bufferVibe[bufferIdx] = secPeakVibe
                bufferProxIdx[bufferIdx] = if (secProxCount > 0) secSumProxIdx / secProxCount else proximityIdx
                bufferTilt[bufferIdx] = secPeakTilt; bufferLift[bufferIdx] = secPeakLift
                bufferAcoustic[bufferIdx] = secPeakDb; bufferSit[bufferIdx] = secSitDetected; bufferKinetic[bufferIdx] = secPeakKinetic
                bufferIdx = (bufferIdx + 1) % 256; if (bufferCount < 256) bufferCount++; secSitDetected = false
            }
            lastBufferRecordRt = nowRt; secPeakLux = currentLux; secPeakVibe = currentVibrationIndex; secSumProxIdx = 0.0; secProxCount = 0; secPeakTilt = currentTiltDegrees; secPeakLift = abs(relativeAltitude); secPeakDb = currentAcousticDb; secPeakKinetic = currentKineticEnergy
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAcousticMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        acousticThread = Thread {
            while (isMonitoring) {
                val sampleRate = ACOUSTIC_SAMPLE_RATE; val bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                if (bufferSize <= 0) { 
                    if (isMonitoring) _sensorEvents.tryEmit(AppSensorEvent.HardwareFailure("AudioRecord: Invalid buffer size"))
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
                        if (isMonitoring) _sensorEvents.tryEmit(AppSensorEvent.HardwareFailure("AudioRecord: Init failed"))
                        try { Thread.sleep(ACOUSTIC_RECOVERY_DELAY_MS) } catch (ie: InterruptedException) { break }
                        continue 
                }
                    try { audioRecord.startRecording() } catch (e: IllegalStateException) {
                        if (isMonitoring) _sensorEvents.tryEmit(AppSensorEvent.HardwareFailure("AudioRecord: Mic occupied"))
                        try { audioRecord.release() } catch (ex: Exception) {}
                        try { Thread.sleep(ACOUSTIC_RECOVERY_DELAY_MS) } catch (ie: InterruptedException) { break }
                        continue
                    }
                    if (audioRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                        if (isMonitoring) _sensorEvents.tryEmit(AppSensorEvent.HardwareFailure("AudioRecord: Contention"))
                        try { audioRecord.release() } catch (ex: Exception) {}
                        try { Thread.sleep(ACOUSTIC_RECOVERY_DELAY_MS) } catch (ie: InterruptedException) { break }
                        continue
                    }
                    isAcousticRunning = true; val buffer = ShortArray(bufferSize); var lastDutyCycleTransitionRt = timeProvider.elapsedRealtime(); var isInOffCycle = false
                    while (isMonitoring && !Thread.currentThread().isInterrupted) {
                        val nowRt = timeProvider.elapsedRealtime()
                        if (powerSaveMode) {
                            if (!isInOffCycle && (nowRt - lastDutyCycleTransitionRt > ACOUSTIC_DUTY_CYCLE_ON_MS)) { 
                                isInOffCycle = true; lastDutyCycleTransitionRt = nowRt; try { audioRecord.stop() } catch (e: Exception) {} 
                            } else if (isInOffCycle && (nowRt - lastDutyCycleTransitionRt > ACOUSTIC_DUTY_CYCLE_OFF_MS)) { 
                                isInOffCycle = false; lastDutyCycleTransitionRt = nowRt; try { audioRecord.startRecording() } catch (e: Exception) { break } 
                            }
                        } else if (isInOffCycle) { 
                            isInOffCycle = false; lastDutyCycleTransitionRt = nowRt; try { audioRecord.startRecording() } catch (e: Exception) { break } 
                        }
                        if (isInOffCycle) { try { Thread.sleep(500) } catch (ie: InterruptedException) { break }; continue }
                        val read = audioRecord.read(buffer, 0, bufferSize)
                        if (read > 0) {
                            var maxAmp = 0; for (i in 0 until read) { val a = Math.abs(buffer[i].toInt()); if (a > maxAmp) maxAmp = a }
                            val db = if (maxAmp > 0) 20 * log10(maxAmp.toDouble() / 1.0) else 0.0
                            synchronized(this) {
                                currentAcousticDb = db
                                if (db > logicPeakDb) logicPeakDb = db
                                if (db < logicMinDb) logicMinDb = db
                                if (db > forensicPeakDb) forensicPeakDb = db
                                if (db < forensicMinDb) forensicMinDb = db
                                if (db > secPeakDb) secPeakDb = db
                                if (!isWarming && fastPathFloor >= 0 && (db - fastPathFloor) > fastPathSpikeThreshold && db >= fastPathMinDb) {
                                    val spikeRt = timeProvider.elapsedRealtime(); if (spikeRt - lastAcousticSpikeRt > SPIKE_DEBOUNCE_MS) { lastAcousticSpikeRt = spikeRt; lastAcousticLockoutRt = spikeRt; onAcousticSpike?.invoke() }
                                }
                            }
                        } else if (read < 0) { 
                            if (!isMonitoring) break; _sensorEvents.tryEmit(AppSensorEvent.HardwareFailure("AudioRecord: Hardware error")); break 
                        }
                    }
                    try { audioRecord.stop() } catch (ex: Exception) {}
                } catch (e: Exception) { if (isMonitoring) _sensorEvents.tryEmit(AppSensorEvent.HardwareFailure("AudioRecord: Exception - ${e.message}")) }
                finally { isAcousticRunning = false; try { audioRecord?.release() } catch (ex: Exception) {}; if (isMonitoring) try { Thread.sleep(ACOUSTIC_GENERIC_RECOVERY_DELAY_MS) } catch (ie: InterruptedException) { } }
            }
        }.apply { name = "AcousticMonitor"; priority = Thread.MIN_PRIORITY; start() }
    }

    private fun stopAcousticMonitoring() { 
        isMonitoring = false
        isAcousticRunning = false
        acousticThread?.interrupt()
        try {
            acousticThread?.join(1000)
        } catch (e: Exception) {
            Timber.e("Acoustic thread join failed")
        }
        acousticThread = null 
    }

    fun isAcousticMonitoringActive() = isAcousticRunning
    fun isAcousticMonitoringEnabled() = isMonitoring

    fun consumeLogicSnapshot(): ForensicSnapshot {
        return LatencyMonitor.measureAndAudit<ForensicSnapshot>(
            timeProvider, LATENCY_THRESHOLD_SENSOR_PROCESS_MS,
            "consumeLogicSnapshot",
            LatencyMonitor.AuditType.PERFORMANCE,
            { message, _ -> _sensorEvents.tryEmit(AppSensorEvent.LogEvent(message, false)) }
        ) {
            synchronized(this) {
                val snapshot = logicSnapshotPool[logicSnapshotIdx]
                logicSnapshotIdx = (logicSnapshotIdx + 1) % logicSnapshotPool.size
                
                snapshot.apply {
                    reset()
                    vibration = currentVibrationIndex; heading = currentCompassHeading; baroAlt = absoluteAltitude; lux = currentLux
                    isNear = isProximityNear; tiltDegrees = currentTiltDegrees; acousticDb = currentAcousticDb; peakShock = logicPeakVibration
                    peakVerticalVelocity = logicPeakVerticalVelocity; peakVerticalVelocityTs = logicPeakVerticalVelocityTs
                    peakVerticalVelocityRt = logicPeakVerticalVelocityRt; plungeMatched = !isWarming && plungeMatched; peakVerticalDisplacement = logicPeakVerticalDisplacement
                    proximityIdx = this@AppSensorManager.proximityIdx; proximityCm = currentProximityCm; proximityDebounceMs = this@AppSensorManager.proximityDebounceMs
                    vibrationRollingSum = this@AppSensorManager.vibrationRollingSum; acousticPeak = logicPeakDb
                    acousticMin = if (logicMinDb >= 100.0) -1.0 else logicMinDb; kineticEnergy = this@AppSensorManager.currentKineticEnergy
                }
                logicPeakVibration = 0.0; logicPeakVerticalVelocity = 0.0; logicPeakVerticalVelocityTs = 0L; logicPeakVerticalVelocityRt = 0L
                logicPeakVerticalDisplacement = 0.0; plungeMatched = false; logicPeakDb = 0.0; logicMinDb = 100.0
                snapshot
            }
        }
    }

    fun consumeForensicSnapshot(): ForensicSnapshot {
        return LatencyMonitor.measureAndAudit<ForensicSnapshot>(
            timeProvider, LATENCY_THRESHOLD_SENSOR_PROCESS_MS,
            "consumeForensicSnapshot",
            LatencyMonitor.AuditType.PERFORMANCE,
            { message, _ -> _sensorEvents.tryEmit(AppSensorEvent.LogEvent(message, false)) }
        ) {
            synchronized(this) {
                val snapshot = forensicSnapshotPool[forensicSnapshotIdx]
                forensicSnapshotIdx = (forensicSnapshotIdx + 1) % forensicSnapshotPool.size
                
                snapshot.apply {
                    reset()
                    vibration = currentVibrationIndex; heading = currentCompassHeading; baroAlt = absoluteAltitude; lux = currentLux
                    isNear = isProximityNear; tiltDegrees = currentTiltDegrees; acousticDb = currentAcousticDb; peakShock = forensicPeakVibration
                    peakVerticalVelocity = forensicPeakVerticalVelocity; peakVerticalVelocityTs = forensicPeakVerticalVelocityTs
                    peakVerticalVelocityRt = forensicPeakVerticalVelocityRt; plungeMatched = false; peakVerticalDisplacement = forensicPeakVerticalDisplacement
                    proximityIdx = this@AppSensorManager.proximityIdx; proximityCm = currentProximityCm; proximityDebounceMs = this@AppSensorManager.proximityDebounceMs
                    vibrationRollingSum = this@AppSensorManager.vibrationRollingSum; acousticPeak = forensicPeakDb
                    acousticMin = if (logicMinDb >= 100.0) -1.0 else logicMinDb; kineticEnergy = this@AppSensorManager.currentKineticEnergy
                }
                forensicPeakVibration = 0.0; forensicPeakVerticalVelocity = 0.0; forensicPeakVerticalVelocityTs = 0L; forensicPeakVerticalVelocityRt = 0L
                forensicPeakVerticalDisplacement = 0.0; forensicPeakDb = 0.0; forensicMinDb = 100.0
                snapshot
            }
        }
    }

    fun getSensorSamples(fromTs: Long, toTs: Long): Sequence<EngineSensorSnapshot> = sequence {
        LatencyMonitor.measureAndAudit<Unit>(
            timeProvider, LATENCY_THRESHOLD_SENSOR_PROCESS_MS,
            "getSensorSamples",
            LatencyMonitor.AuditType.PERFORMANCE,
            { message, _ -> _sensorEvents.tryEmit(AppSensorEvent.LogEvent(message, false)) }
        ) {
            val flyweight = EngineSensorSnapshot(); val c: Int; val startIdx: Int
            synchronized(this@AppSensorManager) { c = bufferCount; startIdx = (bufferIdx - c + 256) % 256 }
            for (i in 0 until c) {
                val idx = (startIdx + i) % 256; val ts: Long; val rt: Long; val lux: Double; val vibe: Double; val proxIdx: Double; val tilt: Double; val lift: Double; val acoustic: Double; val sit: Boolean; val kinetic: Double
                synchronized(this@AppSensorManager) { ts = bufferTs[idx]; rt = bufferRt[idx]; lux = bufferLux[idx]; vibe = bufferVibe[idx]; proxIdx = bufferProxIdx[idx]; tilt = bufferTilt[idx]; lift = bufferLift[idx]; acoustic = bufferAcoustic[idx]; sit = bufferSit[idx]; kinetic = bufferKinetic[idx] }
                if (ts in fromTs..toTs) {
                    flyweight.apply { this.ts = ts; this.rt = rt; this.lux = lux; this.vibe = vibe; this.proxIdx = proxIdx; this.tilt = tilt; this.lift = lift; this.acoustic = acoustic; this.isSitDetected = sit; this.kineticEnergy = kinetic }
                    yield(flyweight)
                }
            }
        }
    }

    fun getAcousticSamples(fromTs: Long, toTs: Long): Sequence<EngineSnrSample> = sequence {
        LatencyMonitor.measureAndAudit<Unit>(
            timeProvider, LATENCY_THRESHOLD_SENSOR_PROCESS_MS,
            "getAcousticSamples",
            LatencyMonitor.AuditType.PERFORMANCE,
            { message, _ -> _sensorEvents.tryEmit(AppSensorEvent.LogEvent(message, false)) }
        ) {
            val flyweight = EngineSnrSample(); val c: Int; val startIdx: Int
            synchronized(this@AppSensorManager) { c = bufferCount; startIdx = (bufferIdx - c + 256) % 256 }
            for (i in 0 until c) {
                val idx = (startIdx + i) % 256; val ts: Long; val rt: Long; val acoustic: Double
                synchronized(this@AppSensorManager) { ts = bufferTs[idx]; rt = bufferRt[idx]; acoustic = bufferAcoustic[idx] }
                if (ts in fromTs..toTs) {
                    flyweight.apply { this.ts = ts; this.rt = rt; this.snr = acoustic }
                    yield(flyweight)
                }
            }
        }
    }

    private fun processVibration(x: Float, y: Float, z: Float) {
        val dx = x.toDouble() - lastAccelX.toDouble(); val dy = y.toDouble() - lastAccelY.toDouble(); val dz = z.toDouble() - lastAccelZ.toDouble()
        val delta = sqrt(dx * dx + dy * dy + dz * dz) / GRAVITY_EARTH
        synchronized(this) { 
            if (delta > logicPeakVibration) logicPeakVibration = delta
            if (delta > forensicPeakVibration) forensicPeakVibration = delta
            adaptiveVibrationFloor = SentinelValidator.updateVibrationFloor(adaptiveVibrationFloor, delta, isWarming) 
            
            lastHpfValue = SentinelValidator.computeNextHpf(lastHpfValue, delta, lastRawVibe)
            currentKineticEnergy = SentinelValidator.computeNextEnergy(currentKineticEnergy, lastHpfValue)
            lastRawVibe = delta
        }
        lastAccelX = x; lastAccelY = y; lastAccelZ = z
        val oldVal = vibrationCircularBuffer[vibrationCircularIdx]; vibrationCircularBuffer[vibrationCircularIdx] = delta
        vibrationRollingSum = vibrationRollingSum - oldVal + delta; vibrationCircularIdx = (vibrationCircularIdx + 1) % VIBRATION_WINDOW_SIZE
        if (vibrationBufferCount < VIBRATION_WINDOW_SIZE) vibrationBufferCount++
        currentVibrationIndex = if (vibrationBufferCount > 0) vibrationRollingSum / vibrationBufferCount else 0.0
        if (currentVibrationIndex > secPeakVibe) secPeakVibe = currentVibrationIndex
        if (currentKineticEnergy > secPeakKinetic) secPeakKinetic = currentKineticEnergy
        val nowRt = timeProvider.elapsedRealtime()
        if (plungePhase == 2) { 
            if (isStationary()) { 
                synchronized(this) { plungeMatched = true; secSitDetected = true }; plungePhase = 0 
            } else if (nowRt - lastPlungePhaseRt > CHAIR_PLUNGE_PHASE_TIMEOUT_MS) { 
                plungePhase = 0 
            } 
        }
        if (isStationary()) { 
            if (stationaryStartRt == 0L) stationaryStartRt = nowRt 
            else if (nowRt - stationaryStartRt > MUZZLE_HYSTERESIS_MS) { 
                currentVerticalVelocity = 0.0; currentVerticalDisplacement = 0.0; if (plungePhase != 2) plungePhase = 0 
            } 
        } else { 
            stationaryStartRt = 0L 
        }
    }

    private fun processLinearAcceleration(val0: Float, val1: Float, val2: Float, timestampNs: Long) {
        if (!hasGravity) return
        if (lastLinearAccelTs == 0L) { lastLinearAccelTs = timestampNs; return }
        val dt = (timestampNs - lastLinearAccelTs).toDouble() / 1_000_000_000.0; lastLinearAccelTs = timestampNs
        if (dt > 0.0 && dt < 0.2) {
            val d0 = val0.toDouble(); val d1 = val1.toDouble(); val d2 = val2.toDouble()
            val g0 = gravityBuffer[0].toDouble(); val g1 = gravityBuffer[1].toDouble(); val g2 = gravityBuffer[2].toDouble()
            val dot = d0 * g0 + d1 * g1 + d2 * g2; val gravMag = sqrt(g0 * g0 + g1 * g1 + g2 * g2); val vz_accel = if (gravMag > 0.1) dot / gravMag else 0.0
            currentVerticalVelocity += vz_accel * dt; currentVerticalDisplacement += currentVerticalVelocity * dt 
            if (abs(currentVerticalVelocity) > VERTICAL_VELOCITY_MAX_MPS) { 
                currentVerticalVelocity = if (currentVerticalVelocity > 0) VERTICAL_VELOCITY_MAX_MPS else -VERTICAL_VELOCITY_MAX_MPS 
            }
            val nowRt = timeProvider.elapsedRealtime(); val wallNow = timeProvider.currentTimeMillis()
            if (plungePhase > 0 && nowRt - lastPlungePhaseRt > CHAIR_PLUNGE_PHASE_TIMEOUT_MS) plungePhase = 0
            when (plungePhase) {
                0 -> if (!isWarming && currentVerticalVelocity < -CHAIR_PLUNGE_VELOCITY_THRESHOLD) { plungePhase = 1; lastPlungePhaseRt = nowRt; currentVerticalDisplacement = 0.0 }
                1 -> { if (nowRt - lastPlungePhaseRt > CHAIR_PLUNGE_WINDOW_MS) { plungePhase = 0 } else if (currentVerticalVelocity > -CHAIR_PLUNGE_VELOCITY_THRESHOLD * 0.2) { if (abs(currentVerticalDisplacement) > CHAIR_PLUNGE_DISTANCE_THRESHOLD) { plungePhase = 2; lastPlungePhaseRt = nowRt } else { plungePhase = 0 } } }
            }
            synchronized(this) { 
                if (abs(currentVerticalVelocity) > abs(logicPeakVerticalVelocity)) { 
                    logicPeakVerticalVelocity = currentVerticalVelocity; logicPeakVerticalVelocityTs = wallNow; logicPeakVerticalVelocityRt = nowRt 
                }
                if (abs(currentVerticalDisplacement) > abs(logicPeakVerticalDisplacement)) { 
                    logicPeakVerticalDisplacement = currentVerticalDisplacement 
                } 
                if (abs(currentVerticalVelocity) > abs(forensicPeakVerticalVelocity)) { 
                    forensicPeakVerticalVelocity = currentVerticalVelocity; forensicPeakVerticalVelocityTs = wallNow; forensicPeakVerticalVelocityRt = nowRt 
                }
                if (abs(currentVerticalDisplacement) > abs(forensicPeakVerticalDisplacement)) { 
                    forensicPeakVerticalDisplacement = currentVerticalDisplacement
                } 
            }
        }
    }

    private fun processPressure(pressure: Float) {
        val pressureDouble = pressure.toDouble(); if (emaPressure == 0.0) emaPressure = pressureDouble
        currentPressure = pressureDouble; val alpha = SentinelValidator.accelerateAlpha(1.0 - BARO_EMA_SLOW, isWarming); emaPressure = (emaPressure * (1.0 - alpha)) + (pressureDouble * alpha)
        val nowRt = timeProvider.elapsedRealtime(); val stationaryDuration = if (stationaryStartRt > 0L) nowRt - stationaryStartRt else 0L
        if (nowRt - lastBaroZeroingRt > BARO_ZEROING_INTERVAL_MS && stationaryDuration >= PASSIVE_ZEROING_STATIONARY_MS) { 
            emaPressure = pressureDouble; lastBaroZeroingRt = nowRt 
        }
        val currentAlt = AndroidSensorManager.getAltitude(AndroidSensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressure).toDouble()
        val baselineAlt = AndroidSensorManager.getAltitude(AndroidSensorManager.PRESSURE_STANDARD_ATMOSPHERE, emaPressure.toFloat()).toDouble()
        absoluteAltitude = currentAlt; relativeAltitude = if (isWarming) 0.0 else currentAlt - baselineAlt
        if (abs(relativeAltitude) > secPeakLift) secPeakLift = abs(relativeAltitude)
    }

    private fun processRotation(rotationVector: FloatArray) {
        AndroidSensorManager.getRotationMatrixFromVector(currentRotationVectorMatrixBuffer, rotationVector); val nowRt = timeProvider.elapsedRealtime()
        if (!hasInitialRotation) { 
            if (!isWarming && stationaryStartRt != 0L && (nowRt - stationaryStartRt > ROTATION_INIT_STATIONARY_MS)) { 
                System.arraycopy(currentRotationVectorMatrixBuffer, 0, initialRotationMatrix, 0, 9); hasInitialRotation = true 
            }
            return 
        }
        val dotProduct = (initialRotationMatrix[2] * currentRotationVectorMatrixBuffer[2]) + (initialRotationMatrix[5] * currentRotationVectorMatrixBuffer[5]) + (initialRotationMatrix[8] * currentRotationVectorMatrixBuffer[8])
        currentTiltDegrees = if (isWarming) 0.0 else Math.toDegrees(acos(dotProduct.coerceIn(-1.0f, 1.0f).toDouble()))
        if (currentTiltDegrees > secPeakTilt) secPeakTilt = currentTiltDegrees
    }

    private fun updateOrientation() { 
        if (hasGravity && hasGeomagnetic) { 
            if (AndroidSensorManager.getRotationMatrix(rotationMatrixBuffer, inclinationMatrixBuffer, gravityBuffer, geomagneticBuffer)) { 
                AndroidSensorManager.getOrientation(rotationMatrixBuffer, orientationBuffer)
                currentCompassHeading = (Math.toDegrees(orientationBuffer[0].toDouble()) + 360.0) % 360.0 
            } 
        } 
    }
    
    fun isStationary() = SentinelValidator.isStationary(currentVibrationIndex, adaptiveVibrationFloor)
    
    fun resetBaseline() { 
        emaPressure = currentPressure; relativeAltitude = 0.0; absoluteAltitude = AndroidSensorManager.getAltitude(AndroidSensorManager.PRESSURE_STANDARD_ATMOSPHERE, currentPressure.toFloat()).toDouble()
        hasInitialRotation = false; stationaryStartRt = 0L; currentVerticalVelocity = 0.0; currentVerticalDisplacement = 0.0; plungePhase = 0; plungeMatched = false; secSitDetected = false; sessionStartRt = timeProvider.elapsedRealtime()
        lastBaroZeroingRt = sessionStartRt; adaptiveVibrationFloor = VIBRATION_STATIONARY_THRESHOLD; debouncedProximityCm = -1.0; proximityDebounceMs = 0L; vibrationCircularIdx = 0; vibrationRollingSum = 0.0; vibrationBufferCount = 0; vibrationCircularBuffer.fill(0.0); lastRawVibe = 0.0; lastHpfValue = 0.0; currentKineticEnergy = 0.0; synchronized(this) { bufferIdx = 0; bufferCount = 0 } 
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
