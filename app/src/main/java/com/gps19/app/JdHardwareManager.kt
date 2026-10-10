package com.gps19.app

import com.gps19.core.engine.AcousticBatch
import com.gps19.core.engine.GnssHealthBatch
import com.gps19.core.engine.JNI_RET_EINTR
import com.gps19.core.engine.JNI_RET_NOT_INITIALIZED
import com.gps19.core.engine.LATENCY_THRESHOLD_JNI_MS
import com.gps19.core.engine.LatencyMonitor
import com.gps19.core.engine.PROXIMITY_DEBOUNCE_MAX_MS
import com.gps19.core.engine.PROXIMITY_DEBOUNCE_MOVING_MS
import com.gps19.core.engine.PROXIMITY_DEBOUNCE_STATIONARY_MS
import com.gps19.core.engine.PROXIMITY_EMA_ALPHA
import com.gps19.core.engine.PROXIMITY_STATIONARY_SCALING_MS_PER_HOUR
import com.gps19.core.engine.PROXIMITY_STRESS_SCALING_MULTIPLIER
import com.gps19.core.engine.ProximityBatch
import com.gps19.core.engine.SystemPressureBatch
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
import kotlin.math.*

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
 * Oct.10.8:
 * - Issue #SIMP-1014-2: Pressure Consolidation. Finalized n21-n24 JNI implementations.
 *   Increased sharedStateBuffer to 2048 to prevent GNSS batch overflow and corrected
 *   output offsets for satellite evaluation.
 * Oct.10.7:
 * - Issue #SIMP-1011-3: Proximity Decoupling. Centralized Proximity health 
 *   evaluation fallback in processProximityBatchNative.
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

    // Issue #SIMP-1014-2: Increased to 2048 bytes for GNSS/Acoustic/Pressure batching arrays
    private val sharedStateBuffer: ByteBuffer = ByteBuffer.allocateDirect(2048).apply {
        order(ByteOrder.nativeOrder())
    }

    // Kotlin Fallback State (Issue #SIMP-1011-2)
    private class FallbackFastPath(
        var baseline: Double = -1.0,
        var threshold: Double = 0.0,
        var minThreshold: Double = -1.0,
        var debounceMs: Long = 5000L,
        var lastSpikeRt: Long = 0L
    )
    private val fallbackFastPaths = mapOf(
        FASTPATH_ACOUSTIC to FallbackFastPath(),
        FASTPATH_LIGHT to FallbackFastPath(),
        FASTPATH_STATIONARY to FallbackFastPath()
    )

    // Issue #SIMP-1014-2: Pressure Fallback State
    private var lastMemLevel = 0
    private var lastStorageLevel = 0

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

    fun resetSensorAudit() {
        if (isLibraryLoaded.get()) n9()
        lastMemLevel = 0
        lastStorageLevel = 0
    }

    fun updateFastPathConfig(type: Int, baseline: Double, threshold: Double, minThreshold: Double, debounceMs: Long): Int {
        if (isLibraryLoaded.get()) return n10(type, baseline, threshold, minThreshold, debounceMs)
        
        fallbackFastPaths[type]?.let {
            it.baseline = baseline
            it.threshold = threshold
            it.minThreshold = minThreshold
            it.debounceMs = debounceMs
            return 0
        }
        return -1
    }

    fun evaluateFastPath(type: Int, value: Double, nowRt: Long, alpha: Double): Boolean {
        if (isLibraryLoaded.get()) return n11(type, value, nowRt, alpha) != 0
        
        val fp = fallbackFastPaths[type] ?: return false
        if (fp.baseline < 0) { fp.baseline = value; return false }
        if (alpha > 0.0) fp.baseline = (fp.baseline * (1.0 - alpha)) + (value * alpha)

        if ((value - fp.baseline) > fp.threshold && value >= fp.minThreshold) {
            if (nowRt - fp.lastSpikeRt > fp.debounceMs) {
                fp.lastSpikeRt = nowRt
                return true
            }
        }
        return false
    }

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
            sharedStateBuffer.putDouble(batch.snr)
            sharedStateBuffer.putDouble(batch.thermal)
            sharedStateBuffer.putDouble(batch.heap)
            sharedStateBuffer.putLong(batch.nowRt)
            
            val res = n19()
            if (res == 0) {
                batch.delta = sharedStateBuffer.getDouble(128)
                batch.nextFloor = sharedStateBuffer.getDouble(136)
                batch.nextHpf = sharedStateBuffer.getDouble(144)
                batch.nextEnergy = sharedStateBuffer.getDouble(152)
                batch.isStationary = sharedStateBuffer.getInt(160) != 0
                batch.isSuspiciousNoise = sharedStateBuffer.getInt(164) != 0
                batch.isMemoryPressureThrottled = sharedStateBuffer.getInt(168) != 0
                batch.stationaryDuration = sharedStateBuffer.getLong(172)
                batch.muzzleResetTriggered = sharedStateBuffer.getInt(180) != 0
                batch.isJammingCandidate = sharedStateBuffer.getInt(184) != 0
                return true
            }
        }
        return false
    }

    /**
     * processGnssBatchNative: Consolidated JNI GNSS path (Issue #SIMP-1011-1).
     * Includes Kotlin fallback to ensure consistent health evaluation when JNI is absent.
     */
    fun processGnssBatchNative(batch: GnssHealthBatch): Boolean {
        if (isLibraryLoaded.get()) {
            synchronized(sharedStateBuffer) {
                sharedStateBuffer.clear()
                sharedStateBuffer.putInt(batch.count)
                for (i in 0 until 64) sharedStateBuffer.putInt(batch.svid[i])
                for (i in 0 until 64) sharedStateBuffer.putFloat(batch.cn0[i])
                for (i in 0 until 64) sharedStateBuffer.putInt(if (batch.usedInFix[i]) 1 else 0)
                for (i in 0 until 64) sharedStateBuffer.putInt(batch.constellation[i])
                
                val res = n21()
                if (res == 0) {
                    // Oct.10.8: Offsets aligned for 2048 buffer safety (outputs at 1040)
                    batch.satellitesInView = sharedStateBuffer.getInt(1040)
                    batch.satellitesUsed = sharedStateBuffer.getInt(1044)
                    batch.averageSnr = sharedStateBuffer.getDouble(1048)
                    return true
                }
            }
        }
        
        // Architecture Rule 2: Fallback logic migrated from HardwareSuite for decoupling
        batch.satellitesInView = batch.count
        var used = 0; var snrSum = 0.0; var snrCount = 0
        for (i in 0 until batch.count) {
            if (batch.usedInFix[i]) used++
            val snr = batch.cn0[i].toDouble()
            if (snr > 0.0) { snrSum += snr; snrCount++ }
        }
        batch.satellitesUsed = used
        batch.averageSnr = if (snrCount > 0) snrSum / snrCount else 0.0
        return true
    }

    /**
     * processAcousticBatchNative: Consolidated JNI audio path (Issue #SIMP-1011-2).
     * Includes Kotlin fallback to ensure consistent dB calculation and spike evaluation.
     */
    fun processAcousticBatchNative(batch: AcousticBatch, buffer: ShortArray): Boolean {
        if (isLibraryLoaded.get()) {
            synchronized(sharedStateBuffer) {
                sharedStateBuffer.clear()
                sharedStateBuffer.putInt(batch.readCount)
                sharedStateBuffer.putDouble(batch.baseAlpha)
                sharedStateBuffer.putDouble(batch.vibrationRollingSum)
                sharedStateBuffer.putLong(batch.nowRt)
                sharedStateBuffer.putInt(if (batch.isWarming) 1 else 0)
                
                val res = n22(buffer)
                if (res == 0) {
                    batch.maxAmp = sharedStateBuffer.getInt(128)
                    batch.db = sharedStateBuffer.getDouble(132)
                    batch.isSpike = sharedStateBuffer.getInt(140) != 0
                    batch.lastSpikeRt = sharedStateBuffer.getLong(144)
                    return true
                }
            }
        }
        
        // Architecture Rule 2: Fallback logic migrated from HardwareSuite for decoupling
        var maxAmp = 0
        for (i in 0 until batch.readCount) {
            val a = abs(buffer[i].toInt())
            if (a > maxAmp) maxAmp = a
        }
        batch.maxAmp = maxAmp
        batch.db = if (maxAmp > 0) 20 * log10(maxAmp.toDouble()) else 0.0
        
        val alpha = computeAdaptiveAcousticAlphaNative(batch.baseAlpha, batch.vibrationRollingSum)
        batch.isSpike = evaluateFastPath(FASTPATH_ACOUSTIC, batch.db, batch.nowRt, if (batch.isWarming) 0.0 else alpha)
        if (batch.isSpike) {
            batch.lastSpikeRt = batch.nowRt
        }
        
        return true
    }

    /**
     * processProximityBatchNative: Consolidated JNI proximity path (Issue #SIMP-1011-3).
     * Includes Kotlin fallback to ensure consistent index calculation and debouncing.
     */
    fun processProximityBatchNative(batch: ProximityBatch): Boolean {
        if (isLibraryLoaded.get()) {
            synchronized(sharedStateBuffer) {
                sharedStateBuffer.clear()
                sharedStateBuffer.putDouble(batch.distance)
                sharedStateBuffer.putDouble(batch.maxRange)
                sharedStateBuffer.putLong(batch.nowRt)
                sharedStateBuffer.putInt(if (batch.isStationary) 1 else 0)
                sharedStateBuffer.putLong(batch.stationaryDurationMs)
                sharedStateBuffer.putInt(if (batch.isHighLoad) 1 else 0)
                sharedStateBuffer.putDouble(batch.currentIdx)
                sharedStateBuffer.putInt(if (batch.rawNear) 1 else 0)
                sharedStateBuffer.putInt(if (batch.isFlickering) 1 else 0)
                
                val res = n23()
                if (res == 0) {
                    batch.nextIdx = sharedStateBuffer.getDouble(128)
                    batch.nextRawNear = sharedStateBuffer.getInt(136) != 0
                    batch.debounceMs = sharedStateBuffer.getLong(140)
                    return true
                }
            }
        }

        // Architecture Rule 2: Fallback logic migrated from HardwareSuite for decoupling
        val newValue = batch.distance < batch.maxRange
        val rawIdx = (1.0 - (batch.distance / batch.maxRange)).coerceIn(0.0, 1.0)
        batch.nextIdx = (batch.currentIdx * (1.0 - PROXIMITY_EMA_ALPHA)) + (rawIdx * PROXIMITY_EMA_ALPHA)
        
        if (newValue != batch.rawNear) {
            // Guard against display flicker during stationary phase
            if (!newValue && batch.isFlickering && batch.isStationary) {
                batch.nextRawNear = batch.rawNear // No change
                batch.debounceMs = 0
            } else {
                batch.nextRawNear = newValue
                var calcDebounceMs = if (batch.isStationary) PROXIMITY_DEBOUNCE_STATIONARY_MS else PROXIMITY_DEBOUNCE_MOVING_MS
                if (batch.isStationary && batch.stationaryDurationMs > 0L) {
                    calcDebounceMs += ((batch.stationaryDurationMs / 3600000.0) * PROXIMITY_STATIONARY_SCALING_MS_PER_HOUR).toLong()
                }
                if (batch.isHighLoad) {
                    calcDebounceMs = (calcDebounceMs * PROXIMITY_STRESS_SCALING_MULTIPLIER).toLong()
                }
                batch.debounceMs = calcDebounceMs.coerceAtMost(PROXIMITY_DEBOUNCE_MAX_MS)
            }
        } else {
            batch.nextRawNear = batch.rawNear
            batch.debounceMs = 0
        }
        
        return true
    }

    /**
     * processSystemPressureNative: Consolidated JNI pressure path (Issue #SIMP-1014-2).
     * Includes Kotlin fallback to ensure consistent memory and storage evaluation with hysteresis.
     */
    fun processSystemPressureNative(batch: SystemPressureBatch): Boolean {
        if (isLibraryLoaded.get()) {
            synchronized(sharedStateBuffer) {
                sharedStateBuffer.clear()
                sharedStateBuffer.putDouble(batch.heapMb)
                sharedStateBuffer.putDouble(batch.memPressureThresholdMb)
                sharedStateBuffer.putDouble(batch.memCriticalThresholdMb)
                sharedStateBuffer.putDouble(batch.memHysteresisOffsetMb)
                sharedStateBuffer.putDouble(batch.storageAvailableMb)
                sharedStateBuffer.putDouble(batch.storageLowThresholdMb)
                sharedStateBuffer.putDouble(batch.storageCriticalThresholdMb)
                sharedStateBuffer.putDouble(batch.storageHysteresisOffsetMb)
                
                val res = n24()
                if (res == 0) {
                    // Read outputs from offset 64
                    batch.currentMemLevel = sharedStateBuffer.getInt(64)
                    batch.needsMemFlush = sharedStateBuffer.getInt(68) != 0
                    batch.currentStorageLevel = sharedStateBuffer.getInt(72)
                    batch.needsStoragePrune = sharedStateBuffer.getInt(76) != 0
                    return true
                }
            }
        }

        // Architecture Rule 2: Fallback logic migrated from IntegrityMonitor for decoupling
        var currentMem = 0
        if (batch.heapMb >= batch.memCriticalThresholdMb) {
            currentMem = 2
        } else if (batch.heapMb >= batch.memPressureThresholdMb) {
            currentMem = 1
        }
        
        // Hysteresis for memory
        if (currentMem < lastMemLevel) {
            val threshold = if (lastMemLevel == 2) batch.memCriticalThresholdMb else batch.memPressureThresholdMb
            if (batch.heapMb > threshold - batch.memHysteresisOffsetMb) {
                currentMem = lastMemLevel
            }
        }
        
        batch.currentMemLevel = currentMem
        batch.needsMemFlush = (currentMem != 0 && currentMem != lastMemLevel)
        lastMemLevel = currentMem

        // Storage logic
        var currentStorage = 0
        if (batch.storageAvailableMb <= batch.storageCriticalThresholdMb) {
            currentStorage = 2
        } else if (batch.storageAvailableMb <= batch.storageLowThresholdMb) {
            currentStorage = 1
        }
        
        // Hysteresis for storage
        if (currentStorage < lastStorageLevel) {
            val threshold = if (lastStorageLevel == 2) batch.storageCriticalThresholdMb else batch.storageLowThresholdMb
            if (batch.storageAvailableMb < threshold + batch.storageHysteresisOffsetMb) {
                currentStorage = lastStorageLevel
            }
        }

        batch.currentStorageLevel = currentStorage
        batch.needsStoragePrune = (currentStorage != 0 && currentStorage != lastStorageLevel)
        lastStorageLevel = currentStorage
        
        return true
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

    fun calculateVibrationDeltaNative(x: Double, y: Double, z: Double, lx: Double, ly: Double, lz: Double): Double {
        return if (isLibraryLoaded.get()) n16(x, y, z, lx, ly, lz) else {
            val dx = x - lx; val dy = y - ly; val dz = z - lz
            Math.sqrt(dx * dx + dy * dy + dz * dz) / 9.80665
        }
    }

    fun isShockViolatedNative(peakShock: Double, adaptiveFloor: Double, sensitivity: Float, cpuLoad: Double): Boolean {
        return if (isLibraryLoaded.get()) n17(peakShock, adaptiveFloor, sensitivity, cpuLoad) != 0 else {
            val loadFactor = if (cpuLoad > 0.85) 1.5 else 1.0
            val baseThreshold = (0.2 + (1.4 - 0.2) * (1.0 - sensitivity)) * loadFactor
            val dynamicThreshold = max(baseThreshold, adaptiveFloor * 7.0 * loadFactor)
            peakShock > dynamicThreshold
        }
    }

    fun isVibrationSuspiciousNative(vibration: Double, adaptiveFloor: Double, sensitivity: Float, cpuLoad: Double): Boolean {
        return if (isLibraryLoaded.get()) n18(vibration, adaptiveFloor, sensitivity, cpuLoad) != 0 else {
            val loadFactor = if (cpuLoad > 0.85) 1.5 else 1.0
            val baseThreshold = (0.05 + (0.45 - 0.05) * (1.0 - sensitivity)) * loadFactor
            val dynamicThreshold = max(baseThreshold, adaptiveFloor * 2.5 * loadFactor)
            vibration > dynamicThreshold
        }
    }

    fun computeAdaptiveAcousticAlphaNative(baseAlpha: Double, vibrationRollingSum: Double): Double {
        return if (isLibraryLoaded.get()) n20(baseAlpha, vibrationRollingSum) else {
            var factor = 1.0
            if (vibrationRollingSum > 0.5) {
                factor = max(0.01, 1.0 - ((vibrationRollingSum - 0.5) / 1.0))
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
    @JvmStatic private external fun n21(): Int
    @JvmStatic private external fun n22(buffer: ShortArray): Int
    @JvmStatic private external fun n23(): Int
    @JvmStatic private external fun n24(): Int
}
