package com.gps19.app

import com.gps19.core.engine.JNI_RET_EINTR
import com.gps19.core.engine.JNI_RET_NOT_INITIALIZED
import com.gps19.core.engine.LATENCY_THRESHOLD_JNI_MS
import com.gps19.core.engine.LatencyMonitor
import com.gps19.core.engine.TimeProvider
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
 * Oct.3.1:
 * - Issue #SIMP-1510-1: Native FastPath Convergence. Added n12/n13 for 
 *   stationary detection and vibration floor EMA offloading to eliminate 
 *   JVM floating-point math from hot paths.
 * Oct.2.15:
 * - Issue #1176: Native FastPath. Integrated n10/n11 for JNI-based high-frequency 
 *   sensor spike detection (Acoustic/Light) to eliminate JVM overhead (R-ID 257).
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

    private val sharedStateBuffer: ByteBuffer = ByteBuffer.allocateDirect(64).apply {
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

    /**
     * recordSensorPulse: Low-latency pulse recording for high-frequency sensor events.
     * Offloads tracking to JNI to avoid heap churn.
     */
    fun recordSensorPulse(nowRt: Long) {
        if (isLibraryLoaded.get()) n7(nowRt)
    }

    /**
     * getSensorAuditHz: Returns the calculated sensor frequency from the native pulse buffer.
     */
    fun getSensorAuditHz(): Double {
        return if (isLibraryLoaded.get()) n8() else 0.0
    }

    /**
     * resetSensorAudit: Resets the native pulse trackers.
     */
    fun resetSensorAudit() {
        if (isLibraryLoaded.get()) n9()
    }

    /**
     * updateFastPathConfig: Configures the native FastPath parameters for a specific sensor type.
     */
    fun updateFastPathConfig(type: Int, baseline: Double, threshold: Double, minThreshold: Double, debounceMs: Long): Int {
        return if (isLibraryLoaded.get()) n10(type, baseline, threshold, minThreshold, debounceMs) else -1
    }

    /**
     * evaluateFastPath: Evaluates a sensor value against the native FastPath logic.
     * Returns true if a spike is detected.
     */
    fun evaluateFastPath(type: Int, value: Double, nowRt: Long, alpha: Double): Boolean {
        return if (isLibraryLoaded.get()) n11(type, value, nowRt, alpha) != 0 else false
    }

    /**
     * isStationaryNative: Native offloading of stationary detection math (Issue #SIMP-1510-1).
     */
    fun isStationaryNative(vibration: Double, adaptiveFloor: Double, cpuLoad: Double): Boolean {
        return if (isLibraryLoaded.get()) n12(vibration, adaptiveFloor, cpuLoad) != 0 else {
            // Fallback to JVM logic if native is unavailable
            val loadFactor = if (cpuLoad > 0.85) 2.0 else 1.0
            val dynamicGate = (adaptiveFloor * 1.5 * loadFactor).coerceIn(0.05, 0.12 * loadFactor)
            vibration < dynamicGate
        }
    }

    /**
     * updateVibrationFloorNative: Native offloading of vibration floor EMA (Issue #SIMP-1510-1).
     */
    fun updateVibrationFloorNative(currentFloor: Double, vibration: Double, isWarming: Boolean, cpuLoad: Double): Double {
        return if (isLibraryLoaded.get()) n13(currentFloor, vibration, if (isWarming) 1 else 0, cpuLoad) else {
            // Fallback to JVM logic if native is unavailable
            if (vibration.isNaN() || vibration <= 0.0 || cpuLoad > 0.85) return currentFloor
            val alpha = if (vibration < currentFloor) {
                if (isWarming) 0.5 else 0.01 // Simplified for fallback
            } else if (vibration < 1.0) {
                if (isWarming) 0.1 else 0.001
            } else 0.0
            (currentFloor * (1.0 - alpha)) + (vibration * alpha)
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
}
