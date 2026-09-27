package com.gps19.app

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * TickOrchestrator: Centralized lifecycle authority for background services.
 * Encapsulates initialization state gates and manages periodic loop executions
 * to maintain strict structured concurrency and simplify background task lifecycles.
 */
class TickOrchestrator {
    private val initializationDeferred = CompletableDeferred<Unit>()
    private val activeJobs = ConcurrentHashMap<String, Job>()

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
     * Launches a lifecycle-managed loop that automatically waits for service initialization.
     */
    fun launchLoop(
        name: String,
        scope: CoroutineScope,
        block: suspend CoroutineScope.() -> Unit
    ) {
        activeJobs[name]?.cancel()
        activeJobs[name] = scope.launch {
            initializationDeferred.await()
            block()
        }
    }

    /**
     * Cancels a specific background loop.
     */
    fun cancelLoop(name: String) {
        activeJobs.remove(name)?.cancel()
    }

    /**
     * Checks whether a loop is actively executing.
     */
    fun isLoopActive(name: String): Boolean {
        return activeJobs[name]?.isActive == true
    }

    /**
     * Atomic cancellation of all orchestrated loops.
     */
    fun cancelAll() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
    }
}
