package com.gps19.app

import android.os.SystemClock
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

/**
 * TickOrchestrator: Centralized lifecycle authority for background services.
 * Encapsulates initialization state gates and manages lifecycle-bound jobs
 * to maintain strict structured concurrency and simplify background task lifecycles.
 * 
 * Oct.6.7:
 * - Issue #AUDIT-1006-8: SIMP-1426-5 (Strategic Simplification). Migrated 
 *   preemption logic from AtomicBoolean polling to Channel-based signals. 
 *   This eliminates the 10ms polling loop in periodic tasks, improving 
 *   hot-path CPU efficiency and responsiveness (Rule 2.3).
 * Oct.6.3:
 * - Issue #AUDIT-1006-2: Added preemptLoop() to force immediate tick by 
 *   canceling current delay in periodic loops.
 */
class TickOrchestrator {
    private val initializationDeferred = CompletableDeferred<Unit>()
    private val managedJobs = ConcurrentHashMap<String, Job>()
    private val preemptionSignals = ConcurrentHashMap<String, Channel<Unit>>()

    /**
     * Signal that the parent service has finished its pre-initialization setup.
     */
    fun completeInitialization() {
        initializationDeferred.complete(Unit)
    }

    /**
     * Awaits service initialization before proceeding.
     */
    suspend fun awaitInitialization() {
        initializationDeferred.await()
    }

    /**
     * Launches a lifecycle-managed job that automatically waits for service initialization.
     * Replaces any existing job with the same name.
     */
    fun launchJob(
        name: String,
        scope: CoroutineScope,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        managedJobs[name]?.cancel()
        val job = scope.launch {
            initializationDeferred.await()
            block()
        }
        managedJobs[name] = job
        return job
    }

    /**
     * Launches a periodic loop with dynamic interval support and preemption.
     * 
     * @param name Unique identifier for the loop.
     * @param scope CoroutineScope to launch in.
     * @param intervalProvider Function returning the interval in ms for the next tick.
     * @param initialDelay Delay before the first execution.
     * @param block The work to perform on each tick.
     */
    fun launchPeriodicLoop(
        name: String,
        scope: CoroutineScope,
        initialDelay: Long = 0L,
        intervalProvider: () -> Long,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        managedJobs[name]?.cancel()
        
        // Issue #AUDIT-1006-8: Efficient signaling channel
        val signal = Channel<Unit>(capacity = Channel.CONFLATED)
        preemptionSignals[name] = signal

        val job = scope.launch {
            initializationDeferred.await()
            if (initialDelay > 0) delay(initialDelay)
            
            while (isActive) {
                val startTime = SystemClock.elapsedRealtime()
                block()
                
                val elapsed = SystemClock.elapsedRealtime() - startTime
                val nextInterval = intervalProvider()
                val remaining = max(10L, nextInterval - elapsed)
                
                // Issue #AUDIT-1006-8: Efficient wait with preemption support
                withTimeoutOrNull(remaining) {
                    signal.receive()
                }
            }
        }
        managedJobs[name] = job
        return job
    }

    /**
     * preemptLoop: Forces the specified loop to wake up and execute its next tick immediately.
     */
    fun preemptLoop(name: String) {
        preemptionSignals[name]?.trySend(Unit)
    }

    /**
     * Alias for launchJob, specifically for legacy periodic loops.
     * @deprecated Use [launchPeriodicLoop] for managed timing.
     */
    fun launchLoop(
        name: String,
        scope: CoroutineScope,
        block: suspend CoroutineScope.() -> Unit
    ) {
        launchJob(name, scope, block)
    }

    /**
     * Cancels a specific managed job.
     */
    fun cancelJob(name: String) {
        managedJobs.remove(name)?.cancel()
        preemptionSignals.remove(name)
    }

    /**
     * Alias for cancelJob.
     */
    fun cancelLoop(name: String) {
        cancelJob(name)
    }

    /**
     * Checks whether a job is actively executing.
     */
    fun isJobActive(name: String): Boolean {
        return managedJobs[name]?.isActive == true
    }

    /**
     * Alias for isJobActive.
     */
    fun isLoopActive(name: String): Boolean {
        return isJobActive(name)
    }

    /**
     * Atomic cancellation of all orchestrated jobs and loops.
     */
    fun cancelAll() {
        managedJobs.values.forEach { it.cancel() }
        managedJobs.clear()
        preemptionSignals.clear()
    }
}
