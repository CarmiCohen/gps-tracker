package com.gps19.app

import android.os.SystemClock
import timber.log.Timber
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * MbrainHardwareManager: JNI Bridge for vendor-specific hardware optimizations.
 * July.25.10:
 * - Issue #580b: Native Signal Latency Audit. Added execution time monitoring 
 *   for JNI calls to prevent tick loop jitter on budget hardware.
 * July.25.05:
 * - Issue #580: Hardening. Implemented ReentrantLock to prevent JNI signal 
 *   collisions during rapid FGS transitions.
 */
object MbrainHardwareManager {

    private var isLibraryLoaded = false
    private val jniLock = ReentrantLock()
    
    private const val NATIVE_LATENCY_THRESHOLD_MS = 50L

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

    private inline fun <T> measureLatency(tag: String, block: () -> T): T {
        val start = SystemClock.elapsedRealtime()
        val result = block()
        val duration = SystemClock.elapsedRealtime() - start
        
        if (duration > NATIVE_LATENCY_THRESHOLD_MS) {
            Timber.w("FORENSIC ALERT: Native $tag latency spike detected (${duration}ms). Threshold: ${NATIVE_LATENCY_THRESHOLD_MS}ms")
        }
        return result
    }

    /**
     * Initializes the Mbrain engine with vendor-specific parameters.
     * Thread-safe wrapper for native init.
     */
    fun initMbrain(deviceId: String, flags: Int): Int {
        if (!isLibraryLoaded) return -1
        return jniLock.withLock {
            measureLatency("initMbrain") {
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
    fun punchHardware(): Int {
        if (!isLibraryLoaded) return -1
        return jniLock.withLock {
            measureLatency("punchHardware") {
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
    fun setPowerBudget(budgetLevel: Int): Int {
        if (!isLibraryLoaded) return -1
        return jniLock.withLock {
            measureLatency("setPowerBudget") {
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
