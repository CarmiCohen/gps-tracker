package com.gps19.app

import com.gps19.core.engine.JNI_RET_EINTR
import com.gps19.core.engine.JNI_RET_NOT_INITIALIZED
import com.gps19.core.engine.LATENCY_THRESHOLD_JNI_MS
import com.gps19.core.engine.LatencyMonitor
import com.gps19.core.engine.TimeProvider
import com.gps19.core.engine.VibrationBatch
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * LedStatus: Type-safe abstraction for hardware LED states (Idea #15).
 */
data class LedStatus(
    val isPowerSave: Boolean = false,
    val isGpsStale: Boolean = false,
    val isInternetLoss: Boolean = false,
    val isRelayLoss: Boolean = false,
    val isPeerStale: Boolean = false
)

/**
 * JdHardwareManager: JNI Bridge for vendor-specific hardware optimizations.
 * Oct.7.9:
 * - Issue #SIMP-1010-2: Muzzle Hysteresis Native Offloading. Updated 
 *   processVibrationBatchNative to pack nowRt (offset 116) and read muzzle 
 *   outputs (offsets 172/180).
 * Oct.7.8:
 * - Issue #SIMP-1010-1: Adaptive Acoustic Gating. Implemented n20 to calculate 
 *   adaptive alpha based on vibrationRollingSum.
 * Oct.7.6:
 * - Issue #SIMP-1007-16: JNI FastPath Expansion. Expanded processVibrationBatchNative 
 *   to pack forensic snapshots and read back native anomaly flags (offsets 164/168).
 */
object JdHardwareManager {

    // Issue #917: LED Status Flags (R338/R972)
    const val FLAG_POWER_SAVE = 0x01
    const val FLAG_GPS_STALE = 0x02
    const val FLAG_INTERNET_LOSS = 0x04
    const val FLAG_RELAY_LOSS = 0x08
    const val FLAG_PEER_STALE = 0x10

    // Issue #1176: FastPath Identifiers
    const val FASTPATH_ACOUSTIC = 0
    const val FASTPATH_LIGHT = 1
    const val FASTPATH_STATIONARY = 2

    private val isLibraryLoaded = AtomicBoolean(false)
    private val initializationMutex = Mutex()
    private val jniLock = ReentrantLock()
    private const val MAX_JNI_RETRIES = 3
    private const val JNI_WATCH_DOG_TIMEOUT_MS = 2000L
    
    // Issue #319: Initialization retry parameters
    private const val MAX_INIT_RETRIES = 5
    private const val INITIAL_RETRY_DELAY_MS = 1000L

    // Issue #1450: Expanded to 256 bytes for vibration batching
    private val sharedStateBuffer: ByteBuffer = ByteBuffer.allocateDirect(256).apply {
        order(ByteOrder.nativeOrder())
    }

    /**
     * syncHardwareState: High-level helper to consolidate LED flag construction (Idea #15).
     */
    suspend fun syncHardwareState(
        timeProvider: TimeProvider,
        tick: Int,
        status: LedStatus
    ): Int {
        var flags = 0
        if (status.isPowerSave) flags = flags or FLAG_POWER_SAVE
        if (status.isGpsStale) flags = flags or FLAG_GPS_STALE
        if (status.isInternetLoss) flags = flags or FLAG_INTERNET_LOSS
        if (status.isRelayLoss) flags = flags or FLAG_RELAY_LOSS
        if (status.isPeerStale) flags = flags or FLAG_PEER_STALE
        
        return syncState(timeProvider, tick, flags)
    }

    /**
     * initialize: Load and initialize the native SDK off the main thread with retries.
     */
    suspend fun initialize(timeProvider: TimeProvider, deviceId: String): Boolean = withContext(Dispatchers.IO) {
        if (isLibraryLoaded.get()) return@withContext true
        
        initializationMutex.withLock {
            if (isLibraryLoaded.get()) return@withLock true
            
            var attempt = 0
            var delayMs = INITIAL_RETRY_DELAY_MS
            
            while (attempt < MAX_INIT_RETRIES) {
                try {
                    val success = withTimeout(JNI_WATCH_DOG_TIMEOUT_MS) {
                        if (!isLibraryLoaded.get()) {
                            System.loadLibrary("jdHardware")
                            n1(sharedStateBuffer)
                            isLibraryLoaded.set(true)
                            Timber.i("jdHardware: Native library loaded successfully.")
                        }
                        
                        val res = n3(deviceId, 0)
                        if (res == 0) {
                            Timber.i("jdHardware: Native SDK initialized successfully on attempt ${attempt + 1}.")
                            true
                        } else {
                            Timber.e("jdHardware: Native SDK init failed (Code: $res, Attempt: ${attempt + 1})")
                            false
                        }
                    }
                    
                    if (success) return@withLock true
                    
                } catch (e: TimeoutCancellationException) {
                    Timber.e("jdHardware: Load/Init timed out (Attempt: ${attempt + 1})")
                } catch (e: Throwable) {
                    Timber.e("jdHardware load/init failed: ${e.message} (Attempt: ${attempt + 1})")
                }
                
                attempt++
                if (attempt < MAX_INIT_RETRIES) {
                    delay(delayMs)
                    delayMs *= 2 // Exponential backoff
                }
            }
            
            Timber.e("jdHardware: Native SDK failed to initialize after $MAX_INIT_RETRIES attempts.")
            false
        }
    }

    /**
     * syncState: Thread-safe native synchronization with watchdog.
     */
    suspend fun syncState(timeProvider: TimeProvider, heartbeatCount: Int, flags: Int): Int = withContext(Dispatchers.IO) {
        executeNativeWithTimeout(timeProvider, "Native syncState") {
            sharedStateBuffer.clear()
            sharedStateBuffer.putInt(heartbeatCount)
            sharedStateBuffer.putInt(flags)
            n2()
        }
    }

    private suspend fun executeNativeWithTimeout(
        timeProvider: TimeProvider,
        operation: String,
        block: () -> Int
    ): Int {
        if (!isLibraryLoaded.get()) return JNI_RET_NOT_INITIALIZED
        
        return try {
            withTimeout(JNI_WATCH_DOG_TIMEOUT_MS) {
                val acquired = jniLock.tryLock(JNI_WATCH_DOG_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
                if (!acquired) {
                    Timber.e("jdHardware: Watchdog triggered for $operation (Lock contention)")
                    return@withTimeout -1
                }

                try {
                    var result: Int
                    var attempts = 0
                    
                    LatencyMonitor.measureAndAudit<Int>(
                        timeProvider = timeProvider,
                        thresholdMs = LATENCY_THRESHOLD_JNI_MS,
                        operation = operation,
                        type = LatencyMonitor.AuditType.PERFORMANCE,
                        onSpike = { message, _ -> Timber.w(message) }
                    ) {
                        do {
                            result = try {
                                block()
                            } catch (e: Throwable) {
                                Timber.e(e, "Unexpected native error in $operation")
                                -1
                            }
                            attempts++
                        } while (result == JNI_RET_EINTR && attempts < MAX_JNI_RETRIES)
                        result
                    }
                } finally {
                    jniLock.unlock()
                }
            }
        } catch (e: TimeoutCancellationException) {
            Timber.e("jdHardware: Watchdog triggered for $operation (Native Hang)")
            -1
        }
    }

    fun initHardware(timeProvider: TimeProvider, deviceId: String, flags: Int): Int = runBlocking {
        executeNativeWithTimeout(timeProvider, "Native initHardware") {
            n3(deviceId, flags)
        }
    }

    fun releaseHardware(timeProvider: TimeProvider): Int = runBlocking {
        executeNativeWithTimeout(timeProvider, "Native releaseHardware") {
            n6()
        }
    }

    fun punchHardware(timeProvider: TimeProvider): Int = runBlocking {
        executeNativeWithTimeout(timeProvider, "Native punchHardware") {
            n4()
        }
    }

    fun recordSensorPulse(nowRt: Long) {
        if (isLibraryLoaded.get()) n7(nowRt)
    }

    fun getSensorAuditHz(): Double {
        return if (isLibraryLoaded.get()) n8() else 0.0
    }

    fun recordSensorAudit() {
        // Obsolete but kept for signature parity if needed
    }

    fun resetSensorAudit() {
        if (isLibraryLoaded.get()) n9()
    }

    fun updateFastPathConfig(type: Int, baseline: Double, threshold: Double, minThreshold: Double, debounceMs: Long): Int {
        return if (isLibraryLoaded.get()) n10(type, baseline, threshold, minThreshold, debounceMs) else -1
    }

    fun evaluateFastPath(type: Int, value: Double, nowRt: Long, alpha: Double): Boolean {
        return if (isLibraryLoaded.get()) n11(type, value, nowRt, alpha) != 0 else false
    }

    /**
     * processVibrationBatchNative: Consolidated 100Hz JNI call (Issue #1450).
     * Oct.7.9: Added nowRt (offset 116) and read muzzle outputs (offsets 172/180).
     */
    fun processVibrationBatchNative(batch: VibrationBatch): Boolean {
        if (!isLibraryLoaded.get()) return false
        
        synchronized(sharedStateBuffer) {
            sharedStateBuffer.clear()
            sharedStateBuffer.putDouble(batch.x)
            sharedStateBuffer.putDouble(batch.y)
            sharedStateBuffer.putDouble(batch.z)
            sharedStateBuffer.putDouble(batch.lx)
            sharedStateBuffer.putDouble(batch.ly)
            sharedStateBuffer.putDouble(batch.lz)
            sharedStateBuffer.putDouble(batch.adaptiveFloor)
            sharedStateBuffer.putDouble(batch.cpuLoad)
            sharedStateBuffer.putInt(if (batch.isWarming) 1 else 0)
            sharedStateBuffer.putDouble(batch.lastRawVibe)
            sharedStateBuffer.putDouble(batch.lastHpfValue)
            sharedStateBuffer.putDouble(batch.currentEnergy)
            
            // Oct.7.6 Forensic Expansion (Offset 92)
            sharedStateBuffer.putDouble(batch.snr)
            sharedStateBuffer.putDouble(batch.thermal)
            sharedStateBuffer.putDouble(batch.heap)

            // Oct.7.9: Time context (Offset 116)
            sharedStateBuffer.putLong(batch.nowRt)
            
            val res = n19()
            if (res == 0) {
                // Read outputs from offset 128
                batch.delta = sharedStateBuffer.getDouble(128)
                batch.nextFloor = sharedStateBuffer.getDouble(136)
                batch.nextHpf = sharedStateBuffer.getDouble(144)
                batch.nextEnergy = sharedStateBuffer.getDouble(152)
                batch.isStationary = sharedStateBuffer.getInt(160) != 0
                
                // Oct.7.6 Anomaly Flags (Offset 164)
                batch.isSuspiciousNoise = sharedStateBuffer.getInt(164) != 0
                batch.isMemoryPressureThrottled = sharedStateBuffer.getInt(168) != 0

                // Oct.7.9 Native Hysteresis (Offset 172)
                batch.stationaryDuration = sharedStateBuffer.getLong(172)
                batch.muzzleResetTriggered = sharedStateBuffer.getInt(180) != 0
                return true
            }
        }
        return false
    }

    fun isStationaryNative(vibration: Double, adaptiveFloor: Double, cpuLoad: Double): Boolean {
        return if (isLibraryLoaded.get()) n12(vibration, adaptiveFloor, cpuLoad) != 0 else {
            val loadFactor = if (cpuLoad > 0.85) 2.0 else 1.0
            val dynamicGate = (adaptiveFloor * 1.5 * loadFactor).coerceIn(0.05, 0.12 * loadFactor)
            vibration < dynamicGate
        }
    }

    fun updateVibrationFloorNative(currentFloor: Double, vibration: Double, isWarming: Boolean, cpuLoad: Double): Double {
        return if (isLibraryLoaded.get()) n13(currentFloor, vibration, if (isWarming) 1 else 0, cpuLoad) else {
            if (vibration.isNaN() || vibration <= 0.0 || cpuLoad > 0.85) return currentFloor
            val alpha = if (vibration < currentFloor) {
                if (isWarming) 0.5 else 0.1
            } else if (vibration < 1.0) {
                if (isWarming) 0.1 else 0.01
            } else 0.0
            (currentFloor * (1.0 - alpha)) + (vibration * alpha)
        }
    }

    fun computeNextHpfNative(lastHpfValue: Double, currentRawVibe: Double, lastRawVibe: Double): Double {
        return if (isLibraryLoaded.get()) n14(lastHpfValue, currentRawVibe, lastRawVibe) else {
            0.9 * (lastHpfValue + currentRawVibe - lastRawVibe)
        }
    }

    fun computeNextEnergyNative(currentEnergy: Double, hpfValue: Double): Double {
        return if (isLibraryLoaded.get()) n15(currentEnergy, hpfValue) else {
            (currentEnergy * (1.0 - 0.1)) + (Math.abs(hpfValue) * 0.1)
        }
    }

    /**
     * calculateVibrationDeltaNative: Native vector magnitude offloading (Issue #SIMP-1510-1).
     */
    fun calculateVibrationDeltaNative(x: Double, y: Double, z: Double, lx: Double, ly: Double, lz: Double): Double {
        return if (isLibraryLoaded.get()) n16(x, y, z, lx, ly, lz) else {
            val dx = x - lx; val dy = y - ly; val dz = z - lz
            Math.sqrt(dx * dx + dy * dy + dz * dz) / 9.80665
        }
    }

    /**
     * isShockViolatedNative: Native Shock Gate (Issue #SIMP-1510-1).
     */
    fun isShockViolatedNative(peakShock: Double, adaptiveFloor: Double, sensitivity: Float, cpuLoad: Double): Boolean {
        return if (isLibraryLoaded.get()) n17(peakShock, adaptiveFloor, sensitivity, cpuLoad) != 0 else {
            val loadFactor = if (cpuLoad > 0.85) 1.5 else 1.0
            val baseThreshold = (0.2 + (1.4 - 0.2) * (1.0 - sensitivity)) * loadFactor
            val dynamicThreshold = Math.max(baseThreshold, adaptiveFloor * 7.0 * loadFactor)
            peakShock > dynamicThreshold
        }
    }

    /**
     * isVibrationSuspiciousNative: Native Suspicious Gate (Issue #SIMP-1510-1).
     */
    fun isVibrationSuspiciousNative(vibration: Double, adaptiveFloor: Double, sensitivity: Float, cpuLoad: Double): Boolean {
        return if (isLibraryLoaded.get()) n18(vibration, adaptiveFloor, sensitivity, cpuLoad) != 0 else {
            val loadFactor = if (cpuLoad > 0.85) 1.5 else 1.0
            val baseThreshold = (0.05 + (0.45 - 0.05) * (1.0 - sensitivity)) * loadFactor
            val dynamicThreshold = Math.max(baseThreshold, adaptiveFloor * 2.5 * loadFactor)
            vibration > dynamicThreshold
        }
    }

    /**
     * computeAdaptiveAcousticAlphaNative: Native motion-aware alpha adjustment (Issue #SIMP-1010-1).
     */
    fun computeAdaptiveAcousticAlphaNative(baseAlpha: Double, vibrationRollingSum: Double): Double {
        return if (isLibraryLoaded.get()) n20(baseAlpha, vibrationRollingSum) else {
            var factor = 1.0
            if (vibrationRollingSum > 0.5) {
                factor = Math.max(0.01, 1.0 - ((vibrationRollingSum - 0.5) / 1.0))
            }
            baseAlpha * factor
        }
    }

    fun isAvailable(): Boolean = isLibraryLoaded.get()

    @JvmStatic private external fun n1(buffer: ByteBuffer): Int
    @JvmStatic private external fun n2(): Int
    @JvmStatic private external fun n3(deviceId: String, flags: Int): Int
    @JvmStatic private external fun n4(): Int
    @JvmStatic private external fun n5(budgetLevel: Int): Int
    @JvmStatic private external fun n6(): Int
    @JvmStatic private external fun n7(nowRt: Long): Int
    @JvmStatic private external fun n8(): Double
    @JvmStatic private external fun n9(): Int
    @JvmStatic private external fun n10(type: Int, baseline: Double, threshold: Double, minThreshold: Double, debounceMs: Long): Int
    @JvmStatic private external fun n11(type: Int, value: Double, nowRt: Long, alpha: Double): Int
    @JvmStatic private external fun n12(vibration: Double, adaptiveFloor: Double, cpuLoad: Double): Int
    @JvmStatic private external fun n13(currentFloor: Double, vibration: Double, isWarming: Int, cpuLoad: Double): Double
    @JvmStatic private external fun n14(lastHpfValue: Double, currentRawVibe: Double, lastRawVibe: Double): Double
    @JvmStatic private external fun n15(currentEnergy: Double, hpfValue: Double): Double
    @JvmStatic private external fun n16(x: Double, y: Double, z: Double, lx: Double, ly: Double, lz: Double): Double
    @JvmStatic private external fun n17(peak: Double, floor: Double, sens: Float, cpu: Double): Int
    @JvmStatic private external fun n18(vibe: Double, floor: Double, sens: Float, cpu: Double): Int
    @JvmStatic private external fun n19(): Int
    @JvmStatic private external fun n20(baseAlpha: Double, vibeRollingSum: Double): Double
}
