package com.gps19.app

import com.gps19.core.engine.LATENCY_THRESHOLD_JNI_MS
import com.gps19.core.engine.LatencyMonitor
import com.gps19.core.engine.TimeProvider
import timber.log.Timber
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * MbrainHardwareManager: JNI Bridge for vendor-specific hardware optimizations.
 * July.26.04:
 * - Issue #589: Performance Audit. Synchronized JNI threshold with EngineConstants.
 * July.25.11:
 * - Issue #590: Refactored to use unified LatencyMonitor.
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

    /**
     * Initializes the Mbrain engine with vendor-specific parameters.
     * Thread-safe wrapper for native init.
     */
    fun initMbrain(timeProvider: TimeProvider, deviceId: String, flags: Int): Int {
        if (!isLibraryLoaded) return -1
        return jniLock.withLock {
            LatencyMonitor.measure(
                timeProvider = timeProvider,
                thresholdMs = LATENCY_THRESHOLD_JNI_MS,
                onSpike = { duration ->
                    Timber.w("FORENSIC ALERT: Native initMbrain latency spike detected (${duration}ms). Threshold: ${LATENCY_THRESHOLD_JNI_MS}ms")
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

    /**
     * Triggers a hardware-level "poke" to prevent aggressive CPU idling.
     * Synchronized to prevent overlapping pokes during FGS type re-evaluations.
     */
    fun punchHardware(timeProvider: TimeProvider): Int {
        if (!isLibraryLoaded) return -1
        return jniLock.withLock {
            LatencyMonitor.measure(
                timeProvider = timeProvider,
                thresholdMs = LATENCY_THRESHOLD_JNI_MS,
                onSpike = { duration ->
                    Timber.w("FORENSIC ALERT: Native punchHardware latency spike detected (${duration}ms). Threshold: ${LATENCY_THRESHOLD_JNI_MS}ms")
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

    /**
     * Sets the power budget for the radio/GNSS stack.
     */
    fun setPowerBudget(timeProvider: TimeProvider, budgetLevel: Int): Int {
        if (!isLibraryLoaded) return -1
        return jniLock.withLock {
            LatencyMonitor.measure(
                timeProvider = timeProvider,
                thresholdMs = LATENCY_THRESHOLD_JNI_MS,
                onSpike = { duration ->
                    Timber.w("FORENSIC ALERT: Native setPowerBudget latency spike detected (${duration}ms). Threshold: ${LATENCY_THRESHOLD_JNI_MS}ms")
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

    @JvmStatic
    private external fun nativeInitMbrain(deviceId: String, flags: Int): Int
    
    @JvmStatic
    private external fun nativePunchHardware(): Int
    
    @JvmStatic
    private external fun nativeSetPowerBudget(budgetLevel: Int): Int
}
