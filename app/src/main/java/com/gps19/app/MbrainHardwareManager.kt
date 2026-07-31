package com.gps19.app

import com.gps19.core.engine.JNI_RET_EINTR
import com.gps19.core.engine.JNI_RET_NOT_INITIALIZED
import com.gps19.core.engine.LATENCY_THRESHOLD_JNI_MS
import com.gps19.core.engine.LatencyMonitor
import com.gps19.core.engine.TimeProvider
import timber.log.Timber
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

/**
 * MbrainHardwareManager: JNI Bridge for vendor-specific hardware optimizations.
 * July.30.47:
 * - Issue #659: Stability: JNI Initialization Integrity. Implemented R659 with 
 *   proactive state verification and background re-initialization. Added 
 *   JNI_RET_NOT_INITIALIZED handling.
 * July.30.25:
 * - Issue #627: Performance: Startup ANR Optimization. Offloaded native library 
 *   loading to background coroutines.
 */
object MbrainHardwareManager {

    private var isLibraryLoaded = false
    private val jniLock = ReentrantLock()
    private const val MAX_JNI_RETRIES = 3

    /**
     * loadLibrary: Explicitly load the native SDK.
     * Should be called from a background thread to avoid startup ANRs.
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

    /**
     * executeNativeWithRetry: Hardens JNI calls against EINTR interruptions and 
     * missing initialization state (Issue #659).
     */
    private inline fun executeNativeWithRetry(
        timeProvider: TimeProvider,
        operation: String,
        crossinline block: () -> Int
    ): Int {
        if (!isLibraryLoaded) {
            triggerBackgroundReinit()
            return JNI_RET_NOT_INITIALIZED
        }
        
        return jniLock.withLock {
            var result: Int
            var attempts = 0
            
            LatencyMonitor.measureAndAudit(
                timeProvider = timeProvider,
                thresholdMs = LATENCY_THRESHOLD_JNI_MS,
                operation = operation,
                type = LatencyMonitor.AuditType.PERFORMANCE,
                onSpike = { message, _ -> Timber.w(message) }
            ) {
                do {
                    result = try {
                        block()
                    } catch (e: UnsatisfiedLinkError) {
                        Timber.e("Native method for $operation not found. Triggering re-init.")
                        isLibraryLoaded = false
                        triggerBackgroundReinit()
                        return@measureAndAudit JNI_RET_NOT_INITIALIZED
                    } catch (e: Exception) {
                        Timber.e(e, "Unexpected native error in $operation")
                        return@measureAndAudit -1
                    }
                    attempts++
                } while (result == JNI_RET_EINTR && attempts < MAX_JNI_RETRIES)
                
                if (result == JNI_RET_EINTR) {
                    Timber.e("Native operation $operation failed after maximum retries due to EINTR")
                }
                result
            }
        }
    }

    private fun triggerBackgroundReinit() {
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            loadLibrary()
        }
    }

    fun initMbrain(timeProvider: TimeProvider, deviceId: String, flags: Int): Int {
        return executeNativeWithRetry(timeProvider, "Native initMbrain") {
            nativeInitMbrain(deviceId, flags)
        }
    }

    fun punchHardware(timeProvider: TimeProvider): Int {
        return executeNativeWithRetry(timeProvider, "Native punchHardware") {
            nativePunchHardware()
        }
    }

    fun setPowerBudget(timeProvider: TimeProvider, budgetLevel: Int): Int {
        return executeNativeWithRetry(timeProvider, "Native setPowerBudget") {
            nativeSetPowerBudget(budgetLevel)
        }
    }

    fun isAvailable(): Boolean = isLibraryLoaded

    @JvmStatic private external fun nativeInitMbrain(deviceId: String, flags: Int): Int
    @JvmStatic private external fun nativePunchHardware(): Int
    @JvmStatic private external fun nativeSetPowerBudget(budgetLevel: Int): Int
}
