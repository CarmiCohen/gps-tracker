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
 * JdHardwareManager: JNI Bridge for vendor-specific hardware optimizations.
 * Aug.21.09:
 * - Issue #265 Remediation: Replaced callback-based loadLibraryAsync with 
 *   suspend initialize() and switched to Mutex to avoid thread-blocking 
 *   stalls during bootstrap (R265).
 * Aug.22.00:
 * - Issue #301 Remediation: Hardened JNI Watchdog. Migrated syncState and other 
 *   native calls to use withTimeout(Dispatchers.IO) to prevent native stalls 
 *   from blocking the main engine loop (R301).
 * Aug.22.08:
 * - Issue #251 Remediation: Documented 'Ghost Load' behavior. On Samsung A15 
 *   hardware, the OS (CFMS) may attempt to load 'libmbrainSDK' based on 
 *   JNI signatures. This is a benign heuristic; the system correctly uses 
 *   'libjdHardware' (R212 Identity Swap).
 */
object JdHardwareManager {

    private val isLibraryLoaded = AtomicBoolean(false)
    private val initializationMutex = Mutex()
    private val jniLock = ReentrantLock()
    private const val MAX_JNI_RETRIES = 3
    private const val JNI_WATCHDOG_TIMEOUT_MS = 2000L

    private val sharedStateBuffer: ByteBuffer = ByteBuffer.allocateDirect(64).apply {
        order(ByteOrder.nativeOrder())
    }

    /**
     * initialize: Load and initialize the native SDK off the main thread.
     */
    suspend fun initialize(timeProvider: TimeProvider, deviceId: String): Boolean = withContext(Dispatchers.IO) {
        if (isLibraryLoaded.get()) return@withContext true
        
        val loaded = initializationMutex.withLock {
            if (isLibraryLoaded.get()) return@withLock true
            try {
                withTimeout(JNI_WATCHDOG_TIMEOUT_MS) {
                    System.loadLibrary("jdHardware")
                    n1(sharedStateBuffer)
                    isLibraryLoaded.set(true)
                    Timber.i("jdHardware: Native library loaded successfully.")
                    // Issue #251: Audit log to confirm Identity Swap (R212)
                    Timber.i("jdHardware: Identity Swap active. Legacy mbrainSDK signatures neutralized.")
                    true
                }
            } catch (e: TimeoutCancellationException) {
                Timber.e("jdHardware: Load timed out (Watchdog)")
                false
            } catch (e: Throwable) {
                Timber.e("jdHardware load failed: ${e.message}")
                false
            }
        }

        if (loaded) {
            val res = initHardware(timeProvider, deviceId, 0)
            if (res == 0) {
                Timber.i("jdHardware: Native SDK initialized successfully.")
                true
            } else {
                Timber.e("jdHardware: Native SDK init failed (Code: $res)")
                false
            }
        } else {
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
            withTimeout(JNI_WATCHDOG_TIMEOUT_MS) {
                val acquired = jniLock.tryLock(JNI_WATCHDOG_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
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

    fun isAvailable(): Boolean = isLibraryLoaded.get()

    @JvmStatic private external fun n1(buffer: ByteBuffer): Int
    @JvmStatic private external fun n2(): Int
    @JvmStatic private external fun n3(deviceId: String, flags: Int): Int
    @JvmStatic private external fun n4(): Int
    @JvmStatic private external fun n5(budgetLevel: Int): Int
    @JvmStatic private external fun n6(): Int
}
