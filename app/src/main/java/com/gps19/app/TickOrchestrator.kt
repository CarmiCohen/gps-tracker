package com.gps19.app

import android.os.SystemClock
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

/**
 * TickOrchestrator: Centralized lifecycle authority for background services.
 * Encapsulates initialization state gates and manages lifecycle-bound jobs
 * to maintain strict structured concurrency and simplify background task lifecycles.
 * 
 * Oct.6.3:
 * - Issue #AUDIT-1006-2: Added preemptLoop() to force immediate tick by 
 *   canceling current delay in periodic loops.
 * Oct.5.8:
 * - Issue #1293: Enhanced with internal periodic loop management and 
 *   dynamic interval support to reduce service-level boilerplate.
 * - Issue #1425 Consistency: Timing logic uses monotonic elapsedRealtime 
 *   to ensure interval stability during system clock adjustments.
 */
class TickOrchestrator {
    private val initializationDeferred = CompletableDeferred<Unit>()
    private val managedJobs = ConcurrentHashMap<String, Job>()
    private val preemptionTriggers = ConcurrentHashMap<String, AtomicBoolean>()

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
        val trigger = AtomicBoolean(false)
        preemptionTriggers[name] = trigger

        val job = scope.launch {
            initializationDeferred.await()
            if (initialDelay > 0) delay(initialDelay)
            
            while (isActive) {
                val startTime = SystemClock.elapsedRealtime()
                block()
                
                val elapsed = SystemClock.elapsedRealtime() - startTime
                val nextInterval = intervalProvider()
                val remaining = max(10L, nextInterval - elapsed)
                
                // Issue #AUDIT-1006-2: Preemptible delay
                try {
                    withTimeout(remaining) {
                        while (!trigger.get()) {
                            delay(10)
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    // Normal timeout
                } finally {
                    trigger.set(false)
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
        preemptionTriggers[name]?.set(true)
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
        preemptionTriggers.remove(name)
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
        preemptionTriggers.clear()
    }
}
