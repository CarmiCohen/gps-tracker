package com.gps19.app

import com.gps19.core.engine.*
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * LedStatus: Type-safe abstraction for hardware LED states.
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
 * Oct.8.5:
 * - Issue #SIMP-1012-2: Native Proximity Scaling. Implemented processProximityBatchNative 
 *   to offload environment-aware proximity debouncing and index calculation (n23).
 * Oct.8.4:
 * - Issue #SIMP-1011-2: Acoustic JNI Offloading. Implemented processAcousticBatchNative 
 *   to offload RMS calculation and spike evaluation (n22).
 */
object JdHardwareManager {

    const val FLAG_POWER_SAVE = 0x01
    const val FLAG_GPS_STALE = 0x02
    const val FLAG_INTERNET_LOSS = 0x04
    const val FLAG_RELAY_LOSS = 0x08
    const val FLAG_PEER_STALE = 0x10

    const val FASTPATH_ACOUSTIC = 0
    const val FASTPATH_LIGHT = 1
    const val FASTPATH_STATIONARY = 2

    private val isLibraryLoaded = AtomicBoolean(false)
    private val initializationMutex = Mutex()
    private val jniLock = ReentrantLock()
    private const val MAX_JNI_RETRIES = 3
    private const val JNI_WATCH_DOG_TIMEOUT_MS = 2000L
    private const val MAX_INIT_RETRIES = 5
    private const val INITIAL_RETRY_DELAY_MS = 1000L

    private val sharedStateBuffer: ByteBuffer = ByteBuffer.allocateDirect(1024).apply {
        order(ByteOrder.nativeOrder())
    }

    suspend fun syncHardwareState(timeProvider: TimeProvider, tick: Int, status: LedStatus): Int {
        var flags = 0
        if (status.isPowerSave) flags = flags or FLAG_POWER_SAVE
        if (status.isGpsStale) flags = flags or FLAG_GPS_STALE
        if (status.isInternetLoss) flags = flags or FLAG_INTERNET_LOSS
        if (status.isRelayLoss) flags = flags or FLAG_RELAY_LOSS
        if (status.isPeerStale) flags = flags or FLAG_PEER_STALE
        return syncState(timeProvider, tick, flags)
    }

    suspend fun initialize(timeProvider: TimeProvider, deviceId: String): Boolean = withContext(Dispatchers.IO) {
        if (isLibraryLoaded.get()) return@withContext true
        initializationMutex.withLock {
            if (isLibraryLoaded.get()) return@withLock true
            var attempt = 0; var delayMs = INITIAL_RETRY_DELAY_MS
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
                        if (res == 0) { Timber.i("jdHardware: Native SDK initialized successfully."); true } 
                        else { Timber.e("jdHardware: Native SDK init failed (Code: $res)"); false }
                    }
                    if (success) return@withLock true
                } catch (e: Exception) { Timber.e(e, "jdHardware load/init failed (Attempt: ${attempt + 1})") }
                attempt++; if (attempt < MAX_INIT_RETRIES) { delay(delayMs); delayMs *= 2 }
            }
            Timber.e("jdHardware: Native SDK failed to initialize after $MAX_INIT_RETRIES attempts."); false
        }
    }

    suspend fun syncState(timeProvider: TimeProvider, heartbeatCount: Int, flags: Int): Int = withContext(Dispatchers.IO) {
        executeNativeWithTimeout(timeProvider, "Native syncState") {
            sharedStateBuffer.clear(); sharedStateBuffer.putInt(heartbeatCount); sharedStateBuffer.putInt(flags); n2()
        }
    }

    private suspend fun executeNativeWithTimeout(timeProvider: TimeProvider, operation: String, block: () -> Int): Int {
        if (!isLibraryLoaded.get()) return JNI_RET_NOT_INITIALIZED
        return try {
            withTimeout(JNI_WATCH_DOG_TIMEOUT_MS) {
                val acquired = jniLock.tryLock(JNI_WATCH_DOG_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
                if (!acquired) { Timber.e("jdHardware: Watchdog triggered for $operation (Lock contention)"); return@withTimeout -1 }
                try {
                    var result: Int; var attempts = 0
                    LatencyMonitor.measureAndAudit<Int>(timeProvider, LATENCY_THRESHOLD_JNI_MS, operation, LatencyMonitor.AuditType.PERFORMANCE, { m, _ -> Timber.w(m) }) {
                        do { result = try { block() } catch (e: Throwable) { -1 }; attempts++ } while (result == JNI_RET_EINTR && attempts < MAX_JNI_RETRIES)
                        result
                    }
                } finally { jniLock.unlock() }
            }
        } catch (e: TimeoutCancellationException) { Timber.e("jdHardware: Watchdog triggered for $operation (Native Hang)"); -1 }
    }

    fun initHardware(timeProvider: TimeProvider, deviceId: String, flags: Int): Int = runBlocking { executeNativeWithTimeout(timeProvider, "Native initHardware") { n3(deviceId, flags) } }
    fun releaseHardware(timeProvider: TimeProvider): Int = runBlocking { executeNativeWithTimeout(timeProvider, "Native releaseHardware") { n6() } }
    fun punchHardware(timeProvider: TimeProvider): Int = runBlocking { executeNativeWithTimeout(timeProvider, "Native punchHardware") { n4() } }
    fun recordSensorPulse(nowRt: Long) { if (isLibraryLoaded.get()) n7(nowRt) }
    fun getSensorAuditHz(): Double { return if (isLibraryLoaded.get()) n8() else 0.0 }
    fun resetSensorAudit() { if (isLibraryLoaded.get()) n9() }
    fun updateFastPathConfig(type: Int, baseline: Double, threshold: Double, minThreshold: Double, debounceMs: Long): Int { return if (isLibraryLoaded.get()) n10(type, baseline, threshold, minThreshold, debounceMs) else -1 }
    fun evaluateFastPath(type: Int, value: Double, nowRt: Long, alpha: Double): Boolean { return if (isLibraryLoaded.get()) n11(type, value, nowRt, alpha) != 0 else false }

    fun processVibrationBatchNative(batch: VibrationBatch): Boolean {
        if (!isLibraryLoaded.get()) return false
        synchronized(sharedStateBuffer) {
            sharedStateBuffer.clear(); sharedStateBuffer.putDouble(batch.x); sharedStateBuffer.putDouble(batch.y); sharedStateBuffer.putDouble(batch.z); sharedStateBuffer.putDouble(batch.lx); sharedStateBuffer.putDouble(batch.ly); sharedStateBuffer.putDouble(batch.lz); sharedStateBuffer.putDouble(batch.adaptiveFloor); sharedStateBuffer.putDouble(batch.cpuLoad); sharedStateBuffer.putInt(if (batch.isWarming) 1 else 0); sharedStateBuffer.putDouble(batch.lastRawVibe); sharedStateBuffer.putDouble(batch.lastHpfValue); sharedStateBuffer.putDouble(batch.currentEnergy); sharedStateBuffer.putDouble(batch.snr); sharedStateBuffer.putDouble(batch.thermal); sharedStateBuffer.putDouble(batch.heap); sharedStateBuffer.putLong(batch.nowRt)
            val res = n19()
            if (res == 0) { batch.delta = sharedStateBuffer.getDouble(128); batch.nextFloor = sharedStateBuffer.getDouble(136); batch.nextHpf = sharedStateBuffer.getDouble(144); batch.nextEnergy = sharedStateBuffer.getDouble(152); batch.isStationary = sharedStateBuffer.getInt(160) != 0; batch.isSuspiciousNoise = sharedStateBuffer.getInt(164) != 0; batch.isMemoryPressureThrottled = sharedStateBuffer.getInt(168) != 0; batch.stationaryDuration = sharedStateBuffer.getLong(172); batch.muzzleResetTriggered = sharedStateBuffer.getInt(180) != 0; batch.isJammingCandidate = sharedStateBuffer.getInt(184) != 0; return true }
        }
        return false
    }

    fun processGnssBatchNative(batch: GnssHealthBatch): Boolean {
        if (!isLibraryLoaded.get()) return false
        synchronized(sharedStateBuffer) {
            sharedStateBuffer.clear(); sharedStateBuffer.putInt(batch.count)
            for (i in 0 until 64) sharedStateBuffer.putInt(batch.svid[i])
            for (i in 0 until 64) sharedStateBuffer.putFloat(batch.cn0[i])
            for (i in 0 until 64) sharedStateBuffer.put(if (batch.usedInFix[i]) 1.toByte() else 0.toByte())
            for (i in 0 until 64) sharedStateBuffer.putInt(batch.constellation[i])
            val res = n21()
            if (res == 0) { batch.satellitesInView = sharedStateBuffer.getInt(840); batch.satellitesUsed = sharedStateBuffer.getInt(844); batch.averageSnr = sharedStateBuffer.getDouble(848); return true }
        }
        return false
    }

    fun processAcousticBatchNative(batch: AcousticBatch, buffer: ShortArray): Boolean {
        if (!isLibraryLoaded.get()) return false
        synchronized(sharedStateBuffer) {
            sharedStateBuffer.clear(); sharedStateBuffer.putInt(batch.readCount); sharedStateBuffer.putDouble(batch.baseAlpha); sharedStateBuffer.putDouble(batch.vibrationRollingSum); sharedStateBuffer.putLong(batch.nowRt); sharedStateBuffer.putInt(if (batch.isWarming) 1 else 0)
            val limit = Math.min(batch.readCount, 496); for (i in 0 until limit) { sharedStateBuffer.putShort(buffer[i]) }
            val res = n22()
            if (res == 0) { batch.maxAmp = sharedStateBuffer.getInt(1000); batch.db = sharedStateBuffer.getDouble(1004); batch.isSpike = sharedStateBuffer.getInt(1012) != 0; batch.lastSpikeRt = sharedStateBuffer.getLong(1016); return true }
        }
        return false
    }

    fun processProximityBatchNative(batch: ProximityBatch): Boolean {
        if (!isLibraryLoaded.get()) return false
        synchronized(sharedStateBuffer) {
            sharedStateBuffer.clear()
            sharedStateBuffer.putDouble(batch.distance) // 0
            sharedStateBuffer.putDouble(batch.maxRange) // 8
            sharedStateBuffer.putLong(batch.nowRt) // 16
            sharedStateBuffer.putInt(if (batch.isStationary) 1 else 0) // 24
            sharedStateBuffer.putLong(batch.stationaryDurationMs) // 28
            sharedStateBuffer.putInt(if (batch.isHighLoad) 1 else 0) // 36
            sharedStateBuffer.putDouble(batch.currentIdx) // 40
            sharedStateBuffer.putInt(if (batch.rawNear) 1 else 0) // 48
            sharedStateBuffer.putInt(if (batch.isFlickering) 1 else 0) // 52
            
            val res = n23()
            if (res == 0) {
                batch.nextIdx = sharedStateBuffer.getDouble(128)
                batch.nextRawNear = sharedStateBuffer.getInt(136) != 0
                batch.debounceMs = sharedStateBuffer.getLong(140)
                return true
            }
        }
        return false
    }

    fun isStationaryNative(vibration: Double, adaptiveFloor: Double, cpuLoad: Double): Boolean {
        return if (isLibraryLoaded.get()) n12(vibration, adaptiveFloor, cpuLoad) != 0 else { val loadFactor = if (cpuLoad > 0.85) 2.0 else 1.0; vibration < (adaptiveFloor * 1.5 * loadFactor).coerceIn(0.05, 0.12 * loadFactor) }
    }

    fun updateVibrationFloorNative(currentFloor: Double, vibration: Double, isWarming: Boolean, cpuLoad: Double): Double {
        return if (isLibraryLoaded.get()) n13(currentFloor, vibration, if (isWarming) 1 else 0, cpuLoad) else { if (vibration.isNaN() || vibration <= 0.0 || cpuLoad > 0.85) return currentFloor; val alpha = if (vibration < currentFloor) (if (isWarming) 0.5 else 0.1) else if (vibration < 1.0) (if (isWarming) 0.1 else 0.01) else 0.0; (currentFloor * (1.0 - alpha)) + (vibration * alpha) }
    }

    fun computeNextHpfNative(lastHpfValue: Double, currentRawVibe: Double, lastRawVibe: Double): Double {
        return if (isLibraryLoaded.get()) n14(lastHpfValue, currentRawVibe, lastRawVibe) else 0.9 * (lastHpfValue + currentRawVibe - lastRawVibe)
    }

    fun computeNextEnergyNative(currentEnergy: Double, hpfValue: Double): Double {
        return if (isLibraryLoaded.get()) n15(currentEnergy, hpfValue) else (currentEnergy * (1.0 - 0.1)) + (Math.abs(hpfValue) * 0.1)
    }

    fun calculateVibrationDeltaNative(x: Double, y: Double, z: Double, lx: Double, ly: Double, lz: Double): Double {
        return if (isLibraryLoaded.get()) n16(x, y, z, lx, ly, lz) else Math.sqrt((x - lx) * (x - lx) + (y - ly) * (y - ly) + (z - lz) * (z - lz)) / 9.80665
    }

    fun isShockViolatedNative(peakShock: Double, adaptiveFloor: Double, sensitivity: Float, cpuLoad: Double): Boolean {
        return if (isLibraryLoaded.get()) n17(peakShock, adaptiveFloor, sensitivity, cpuLoad) != 0 else { val loadFactor = if (cpuLoad > 0.85) 1.5 else 1.0; peakShock > Math.max((0.2 + (1.4 - 0.2) * (1.0 - sensitivity)) * loadFactor, adaptiveFloor * 7.0 * loadFactor) }
    }

    fun isVibrationSuspiciousNative(vibration: Double, adaptiveFloor: Double, sensitivity: Float, cpuLoad: Double): Boolean {
        return if (isLibraryLoaded.get()) n18(vibration, adaptiveFloor, sensitivity, cpuLoad) != 0 else { val loadFactor = if (cpuLoad > 0.85) 1.5 else 1.0; vibration > Math.max((0.05 + (0.45 - 0.05) * (1.0 - sensitivity)) * loadFactor, adaptiveFloor * 2.5 * loadFactor) }
    }

    fun computeAdaptiveAcousticAlphaNative(baseAlpha: Double, vibrationRollingSum: Double): Double {
        return if (isLibraryLoaded.get()) n20(baseAlpha, vibrationRollingSum) else { var factor = 1.0; if (vibrationRollingSum > 0.5) factor = Math.max(0.01, 1.0 - ((vibrationRollingSum - 0.5) / 1.0)); baseAlpha * factor }
    }

    fun isAvailable(): Boolean = isLibraryLoaded.get()

    @JvmStatic private external fun n1(buffer: ByteBuffer): Int
    @JvmStatic private external fun n2(): Int
    @JvmStatic private external fun n3(deviceId: String, flags: Int): Int
    @JvmStatic private external fun n4(): Int
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
    @JvmStatic private external fun n22(): Int
    @JvmStatic private external fun n23(): Int
}