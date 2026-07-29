package com.gps19.app

import com.gps19.core.engine.LATENCY_THRESHOLD_JNI_MS
import com.gps19.core.engine.LatencyMonitor
import com.gps19.core.engine.TimeProvider
import timber.log.Timber
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * MbrainHardwareManager: JNI Bridge for vendor-specific hardware optimizations.
 * July.29.01:
 * - Issue #623: Structural: Latency Monitor Metric Cleanup. Standardized spike 
 *   reporting strings for forensic auditing.
 * July.26.04:
 * - Issue #589: Performance Audit.
 */
object MbrainHardwareManager {

    private var isLibraryLoaded = false
    private val jniLock = ReentrantLock()

    init {
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

    fun initMbrain(timeProvider: TimeProvider, deviceId: String, flags: Int): Int {
        if (!isLibraryLoaded) return -1
        return jniLock.withLock {
            LatencyMonitor.measure(
                timeProvider = timeProvider,
                thresholdMs = LATENCY_THRESHOLD_JNI_MS,
                onSpike = { duration ->
                    Timber.w("Forensic Performance Audit: Native initMbrain spike (${duration}ms)")
                }
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
            LatencyMonitor.measure(
                timeProvider = timeProvider,
                thresholdMs = LATENCY_THRESHOLD_JNI_MS,
                onSpike = { duration ->
                    Timber.w("Forensic Performance Audit: Native punchHardware spike (${duration}ms)")
                }
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
            LatencyMonitor.measure(
                timeProvider = timeProvider,
                thresholdMs = LATENCY_THRESHOLD_JNI_MS,
                onSpike = { duration ->
                    Timber.w("Forensic Performance Audit: Native setPowerBudget spike (${duration}ms)")
                }
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
