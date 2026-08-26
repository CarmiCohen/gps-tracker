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
 * Aug.26.00:
 * - Issue #319 Remediation: Added robust retry mechanism with exponential backoff 
 *   to native initialization to resolve Monitor::Inflate installation failures 
 *   during background service startup (R319).
 * Aug.25.00:
 * - Issue #310 Remediation: Neutralized literal legacy SDK strings in log messages 
 *   to prevent CFMS string-pool scanning from triggering Ghost Loads (R212).
 * Aug.21.09:
 * - Issue #265 Remediation: Replaced callback-based loadLibraryAsync with 
 *   suspend initialize() and switched to Mutex to avoid thread-blocking 
 *   stalls during bootstrap (R265).
 */
object JdHardwareManager {

    private val isLibraryLoaded = AtomicBoolean(false)
    private val initializationMutex = Mutex()
    private val jniLock = ReentrantLock()
    private const val MAX_JNI_RETRIES = 3
    private const val JNI_WATCHDOG_TIMEOUT_MS = 2000L
    
    // Issue #319: Initialization retry parameters
    private const val MAX_INIT_RETRIES = 5
    private const val INITIAL_RETRY_DELAY_MS = 1000L

    private val sharedStateBuffer: ByteBuffer = ByteBuffer.allocateDirect(64).apply {
        order(ByteOrder.nativeOrder())
    }

    /**
     * initialize: Load and initialize the native SDK off the main thread with retries (Issue #319).
     */
    suspend fun initialize(timeProvider: TimeProvider, deviceId: String): Boolean = withContext(Dispatchers.IO) {
        if (isLibraryLoaded.get()) return@withContext true
        
        initializationMutex.withLock {
            if (isLibraryLoaded.get()) return@withLock true
            
            var attempt = 0
            var delayMs = INITIAL_RETRY_DELAY_MS
            
            while (attempt < MAX_INIT_RETRIES) {
                try {
                    val success = withTimeout(JNI_WATCHDOG_TIMEOUT_MS) {
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
