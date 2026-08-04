package com.gps19.app

import com.gps19.core.engine.JNI_RET_EINTR
import com.gps19.core.engine.JNI_RET_NOT_INITIALIZED
import com.gps19.core.engine.LATENCY_THRESHOLD_JNI_MS
import com.gps19.core.engine.LatencyMonitor
import com.gps19.core.engine.TimeProvider
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

/**
 * MbrainHardwareManager: JNI Bridge for vendor-specific hardware optimizations.
 * Aug.04.101:
 * - Issue #721: Performance: Renamed native library to jdMbrain to resolve 
 *   collision with Samsung system libraries (libmbrainSDK).
 * Aug.01.01:
 * - Issue #667: Forensic Audit: Memory Pressure. Implemented zero-copy state path 
 *   using DirectByteBuffer to eliminate GC churn during high-frequency JNI traffic.
 */
object MbrainHardwareManager {

    private var isLibraryLoaded = false
    private val jniLock = ReentrantLock()
    private const val MAX_JNI_RETRIES = 3

    private val sharedStateBuffer: ByteBuffer = ByteBuffer.allocateDirect(64).apply {
        order(ByteOrder.nativeOrder())
    }

    /**
     * loadLibrary: Explicitly load the native SDK.
     */
    fun loadLibrary() {
        if (isLibraryLoaded) return
        jniLock.withLock {
            if (isLibraryLoaded) return
            try {
                // Renamed to avoid collision with Samsung's internal libmbrainSDK.so
                System.loadLibrary("jdMbrain")
                isLibraryLoaded = true
                
                nativeRegisterSharedBuffer(sharedStateBuffer)
                Timber.i("jdMbrain loaded and shared buffer registered")
            } catch (e: UnsatisfiedLinkError) {
                Timber.e("jdMbrain load failed: ${e.message}")
            } catch (e: Exception) {
                Timber.e("Unexpected error loading jdMbrain: ${e.message}")
            }
        }
    }

    fun syncState(timeProvider: TimeProvider, heartbeatCount: Int, flags: Int): Int {
        return executeNativeWithRetry(timeProvider, "Native syncState") {
            sharedStateBuffer.clear()
            sharedStateBuffer.putInt(heartbeatCount)
            sharedStateBuffer.putInt(flags)
            nativeSyncState()
        }
    }

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

    @JvmStatic private external fun nativeRegisterSharedBuffer(buffer: ByteBuffer): Int
    @JvmStatic private external fun nativeSyncState(): Int
    @JvmStatic private external fun nativeInitMbrain(deviceId: String, flags: Int): Int
    @JvmStatic private external fun nativePunchHardware(): Int
    @JvmStatic private external fun nativeSetPowerBudget(budgetLevel: Int): Int
}
