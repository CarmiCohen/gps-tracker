package com.gps19.app

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * TickOrchestrator: Centralized lifecycle authority for background services.
 * Encapsulates initialization state gates and manages lifecycle-bound jobs
 * to maintain strict structured concurrency and simplify background task lifecycles.
 */
class TickOrchestrator {
    private val initializationDeferred = CompletableDeferred<Unit>()
    private val managedJobs = ConcurrentHashMap<String, Job>()

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
     * Alias for launchJob, specifically for periodic loops.
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
    }
}
