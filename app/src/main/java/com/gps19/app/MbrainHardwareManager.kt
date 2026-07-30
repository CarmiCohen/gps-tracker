package com.gps19.app

import com.gps19.core.engine.LATENCY_THRESHOLD_JNI_MS
import com.gps19.core.engine.LatencyMonitor
import com.gps19.core.engine.TimeProvider
import timber.log.Timber
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * MbrainHardwareManager: JNI Bridge for vendor-specific hardware optimizations.
 * July.30.25:
 * - Issue #627: Performance: Startup ANR Optimization. Offloaded native library 
 *   loading to background coroutines via explicit loadLibrary() call. Removed init block.
 * July.29.01:
 * - Issue #623: Structural: Latency Monitor Metric Cleanup.
 */
object MbrainHardwareManager {

    private var isLibraryLoaded = false
    private val jniLock = ReentrantLock()

    /**
     * loadLibrary: Explicitly load the native SDK.
     * Should be called from a background thread (e.g. Dispatchers.IO) to avoid startup ANRs.
     */
    fun loadLibrary() {
        if (isLibraryLoaded) return
        jniLock.withLock {
            if (isLibraryLoaded) return
            try {
                System.loadLibrary("mbrainSDK")
                isLibraryLoaded = true
                Timber.i("libmbrainSDK loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Timber.e("libmbrainSDK load failed: ${e.message}")
            } catch (e: Exception) {
                Timber.e("Unexpected error loading libmbrainSDK: ${e.message}")
            }
        }
    }

    fun initMbrain(timeProvider: TimeProvider, deviceId: String, flags: Int): Int {
        if (!isLibraryLoaded) return -1
        return jniLock.withLock {
            LatencyMonitor.measureAndAudit(
                timeProvider = timeProvider,
                thresholdMs = LATENCY_THRESHOLD_JNI_MS,
                operation = "Native initMbrain",
                type = LatencyMonitor.AuditType.PERFORMANCE,
                onSpike = { message, _ -> Timber.w(message) }
            ) {
                try {
                    nativeInitMbrain(deviceId, flags)
                } catch (e: UnsatisfiedLinkError) {
                    Timber.e("Native method initMbrain not found")
                    -2
                }
            }
        }
    }

    fun punchHardware(timeProvider: TimeProvider): Int {
        if (!isLibraryLoaded) return -1
        return jniLock.withLock {
            LatencyMonitor.measureAndAudit(
                timeProvider = timeProvider,
                thresholdMs = LATENCY_THRESHOLD_JNI_MS,
                operation = "Native punchHardware",
                type = LatencyMonitor.AuditType.PERFORMANCE,
                onSpike = { message, _ -> Timber.w(message) }
            ) {
                try {
                    nativePunchHardware()
                } catch (e: UnsatisfiedLinkError) {
                    Timber.e("Native method punchHardware not found")
                    -2
                }
            }
        }
    }

    fun setPowerBudget(timeProvider: TimeProvider, budgetLevel: Int): Int {
        if (!isLibraryLoaded) return -1
        return jniLock.withLock {
            LatencyMonitor.measureAndAudit(
                timeProvider = timeProvider,
                thresholdMs = LATENCY_THRESHOLD_JNI_MS,
                operation = "Native setPowerBudget",
                type = LatencyMonitor.AuditType.PERFORMANCE,
                onSpike = { message, _ -> Timber.w(message) }
            ) {
                try {
                    nativeSetPowerBudget(budgetLevel)
                } catch (e: UnsatisfiedLinkError) {
                    Timber.e("Native method setPowerBudget not found")
                    -2
                }
            }
        }
    }

    fun isAvailable(): Boolean = isLibraryLoaded

    @JvmStatic private external fun nativeInitMbrain(deviceId: String, flags: Int): Int
    @JvmStatic private external fun nativePunchHardware(): Int
    @JvmStatic private external fun nativeSetPowerBudget(budgetLevel: Int): Int
}
