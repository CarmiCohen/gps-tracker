package com.gps19.core.engine

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.selects.select
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * SmartSignalingDispatcher: Unified reactive coordination layer for signaling.
 * Oct.6.13:
 * - Issue #SIMP-1426-8: Dynamic Conflation Pressure Adaptation. Implemented 
 *   dynamic scaling of conflation delays based on telemetry density to optimize 
 *   radio duty cycles during bursts.
 * Oct.6.12:
 * - SIMP-1426-7: Consolidated conflation jobs into a single unified conflation loop.
 * Oct.6.11:
 * - Issue #AUDIT-1006-10: Dispatcher Lifecycle Hardening. Added reinitialize().
 * Oct.6.9:
 * - Issue #AUDIT-1006-9 (SIMP-1426-6): Reactive Metrics.
 */
class SmartSignalingDispatcher(
    private var scope: CoroutineScope,
    private val isViolationProvider: () -> Boolean,
    private val jsonSink: (String, Map<String, Any?>) -> Unit,
    private val binarySink: (String, String, ByteArray) -> Unit,
    private val objectSink: (String, LocationUpdate) -> Unit,
    private val isConnectedProvider: () -> Boolean,
    private val timeProvider: TimeProvider = object : TimeProvider {
        override fun currentTimeMillis() = System.currentTimeMillis()
        override fun elapsedRealtime() = System.currentTimeMillis()
    },
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    sealed class Command {
        abstract val priority: SignalingPriority
        data class Json(val event: String, val data: Map<String, Any?>, override val priority: SignalingPriority) : Command()
        data class Binary(val event: String, val routingId: String, val data: ByteArray, override val priority: SignalingPriority) : Command()
        data class Object(val event: String, val update: LocationUpdate, override val priority: SignalingPriority) : Command()
    }

    private var highQueue = Channel<Command>(capacity = Channel.UNLIMITED)
    private var normalQueue = Channel<Command>(capacity = Channel.UNLIMITED)
    
    private val pendingLocationMap = AtomicReference<Map<String, Any?>?>(null)
    private val locationMapScheduledTs = AtomicLong(0)
    private val locationMapBurstCount = AtomicInteger(0)
    
    private val pendingLocationObject = AtomicReference<LocationUpdate?>(null)
    private val locationObjectScheduledTs = AtomicLong(0)
    private val locationObjectBurstCount = AtomicInteger(0)
    
    private val pendingLogMap = AtomicReference<Map<String, Any?>?>(null)
    private val logScheduledTs = AtomicLong(0)
    private val logBurstCount = AtomicInteger(0)

    private val conflationSignal = Channel<Unit>(capacity = Channel.CONFLATED)
    private var conflationJob: Job? = null

    private val framesReceived = AtomicLong(0)
    private val framesEmitted = AtomicLong(0)
    private val framesConflated = AtomicLong(0)

    private val _metricsFlow = MutableStateFlow(Metrics(0, 0, 0))
    val metricsFlow: StateFlow<Metrics> = _metricsFlow.asStateFlow()

    private var processorJob: Job? = null
    
    @Volatile
    private var lastNormalEmitTs = 0L

    init {
        startProcessor()
        startConflationLoop()
    }

    fun reinitialize(newScope: CoroutineScope) {
        if (processorJob?.isActive == true && !highQueue.isClosedForSend) return
        
        shutdown()
        this.scope = newScope
        highQueue = Channel(capacity = Channel.UNLIMITED)
        normalQueue = Channel(capacity = Channel.UNLIMITED)
        
        pendingLocationMap.set(null)
        locationMapScheduledTs.set(0)
        locationMapBurstCount.set(0)
        pendingLocationObject.set(null)
        locationObjectScheduledTs.set(0)
        locationObjectBurstCount.set(0)
        pendingLogMap.set(null)
        logScheduledTs.set(0)
        logBurstCount.set(0)
        
        startProcessor()
        startConflationLoop()
    }

    private fun startProcessor() {
        processorJob?.cancel()
        processorJob = scope.launch(dispatcher) {
            while (isActive) {
                while (!isConnectedProvider() && isActive) {
                    delay(1000)
                }
                if (!isActive) break

                try {
                    val command = highQueue.tryReceive().getOrNull() ?: select<Command> {
                        highQueue.onReceive { it }
                        normalQueue.onReceive { it }
                    }

                    if (command.priority == SignalingPriority.NORMAL) {
                        val now = timeProvider.currentTimeMillis()
                        val delayMs = if (isViolationProvider()) SIGNALING_EMIT_DELAY_VIOLATION_MS else SIGNALING_EMIT_DELAY_MS
                        val elapsed = now - lastNormalEmitTs
                        if (elapsed < delayMs) {
                            val waitTime = delayMs - elapsed
                            val highReady = withTimeoutOrNull(waitTime) {
                                highQueue.receive()
                            }
                            
                            if (highReady != null) {
                                emit(highReady)
                                normalQueue.trySend(command) // Re-queue original normal priority
                                continue
                            }
                        }
                        lastNormalEmitTs = timeProvider.currentTimeMillis()
                    }

                    emit(command)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    if (highQueue.isClosedForReceive || normalQueue.isClosedForReceive) break
                    delay(100)
                }
            }
        }
    }

    private fun startConflationLoop() {
        conflationJob?.cancel()
        conflationJob = scope.launch(dispatcher) {
            try {
                while (isActive) {
                    conflationSignal.receive()
                    
                    while (isActive) {
                        val now = timeProvider.currentTimeMillis()
                        var nextCheck = Long.MAX_VALUE
                        
                        // Check Location Map
                        val locMapTs = locationMapScheduledTs.get()
                        if (locMapTs > 0) {
                            if (now >= locMapTs) {
                                locationMapScheduledTs.set(0)
                                locationMapBurstCount.set(0)
                                pendingLocationMap.getAndSet(null)?.let {
                                    enqueue(Command.Json("location_update", it, SignalingPriority.NORMAL))
                                }
                            } else {
                                nextCheck = minOf(nextCheck, locMapTs - now)
                            }
                        }

                        // Check Location Object
                        val locObjTs = locationObjectScheduledTs.get()
                        if (locObjTs > 0) {
                            if (now >= locObjTs) {
                                locationObjectScheduledTs.set(0)
                                locationObjectBurstCount.set(0)
                                pendingLocationObject.getAndSet(null)?.let {
                                    enqueue(Command.Object("location_update_bin", it, SignalingPriority.NORMAL))
                                }
                            } else {
                                nextCheck = minOf(nextCheck, locObjTs - now)
                            }
                        }

                        // Check Log Map
                        val logTs = logScheduledTs.get()
                        if (logTs > 0) {
                            if (now >= logTs) {
                                logScheduledTs.set(0)
                                logBurstCount.set(0)
                                pendingLogMap.getAndSet(null)?.let {
                                    enqueue(Command.Json("log_update", it, SignalingPriority.NORMAL))
                                }
                            } else {
                                nextCheck = minOf(nextCheck, logTs - now)
                            }
                        }

                        if (nextCheck == Long.MAX_VALUE) break
                        
                        withTimeoutOrNull(nextCheck) {
                            conflationSignal.receive()
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException || e is ClosedReceiveChannelException) return@launch
                delay(1000) // Recovery
            }
        }
    }

    private fun emit(command: Command) {
        val emitted = framesEmitted.incrementAndGet()
        updateMetrics(emitted = emitted)
        when (command) {
            is Command.Json -> jsonSink(command.event, command.data)
            is Command.Binary -> binarySink(command.event, command.routingId, command.data)
            is Command.Object -> objectSink(command.event, command.update)
        }
    }

    fun dispatch(command: Command) {
        if (highQueue.isClosedForSend) return

        val received = framesReceived.incrementAndGet()
        updateMetrics(received = received)
        when (command) {
            is Command.Json -> {
                if (command.event == "location_update" && command.priority != SignalingPriority.HIGH) {
                    dispatchConflatedLocationMap(command.data)
                } else if (command.event == "log_update" && command.priority == SignalingPriority.NORMAL) {
                    dispatchConflatedLog(command.data)
                } else {
                    enqueue(command)
                }
            }
            is Command.Object -> {
                if (command.event == "location_update_bin" && command.priority != SignalingPriority.HIGH) {
                    dispatchConflatedLocationObject(command.update)
                } else {
                    enqueue(command)
                }
            }
            is Command.Binary -> enqueue(command)
        }
    }

    private fun enqueue(command: Command) {
        if (command.priority == SignalingPriority.HIGH) {
            highQueue.trySend(command)
        } else {
            normalQueue.trySend(command)
        }
    }

    private fun updateMetrics(received: Long? = null, emitted: Long? = null, conflated: Long? = null) {
        _metricsFlow.update { current ->
            Metrics(
                received = received ?: current.received,
                emitted = emitted ?: current.emitted,
                conflated = conflated ?: current.conflated
            )
        }
    }

    private fun dispatchConflatedLocationMap(incoming: Map<String, Any?>) {
        var wasConflated = false
        pendingLocationMap.updateAndGet { current ->
            if (current != null) wasConflated = true
            SignalingMessageConflator.conflate(current, incoming)
        }
        if (wasConflated) {
            val conf = framesConflated.incrementAndGet()
            updateMetrics(conflated = conf)
            locationMapBurstCount.incrementAndGet()
        }

        if (locationMapScheduledTs.get() == 0L) {
            val delayMs = calculateDelay(locationMapBurstCount.get())
            locationMapScheduledTs.set(timeProvider.currentTimeMillis() + delayMs)
            conflationSignal.trySend(Unit)
        } else {
            // Adjust schedule if pressure is high
            val currentScheduled = locationMapScheduledTs.get()
            val now = timeProvider.currentTimeMillis()
            val newDelay = calculateDelay(locationMapBurstCount.get())
            if (currentScheduled - now < newDelay / 2) { // Only extend if we're early in the window
                 locationMapScheduledTs.compareAndSet(currentScheduled, now + newDelay)
            }
        }
    }

    private fun dispatchConflatedLocationObject(incoming: LocationUpdate) {
        var wasConflated = false
        pendingLocationObject.updateAndGet { current ->
            if (current != null) wasConflated = true
            SignalingMessageConflator.conflateLocationUpdate(current, incoming)
        }
        if (wasConflated) {
            val conf = framesConflated.incrementAndGet()
            updateMetrics(conflated = conf)
            locationObjectBurstCount.incrementAndGet()
        }

        if (locationObjectScheduledTs.get() == 0L) {
            val delayMs = calculateDelay(locationObjectBurstCount.get())
            locationObjectScheduledTs.set(timeProvider.currentTimeMillis() + delayMs)
            conflationSignal.trySend(Unit)
        } else {
            val currentScheduled = locationObjectScheduledTs.get()
            val now = timeProvider.currentTimeMillis()
            val newDelay = calculateDelay(locationObjectBurstCount.get())
            if (currentScheduled - now < newDelay / 2) {
                 locationObjectScheduledTs.compareAndSet(currentScheduled, now + newDelay)
            }
        }
    }

    private fun dispatchConflatedLog(incoming: Map<String, Any?>) {
        val current = pendingLogMap.get()
        if (current != null) {
            val pendingMsg = current["message"] as? String
            val incomingMsg = incoming["message"] as? String
            if (pendingMsg != incomingMsg) {
                // Sequence break: Flush immediately
                val toSend = pendingLogMap.getAndSet(null)
                if (toSend != null) {
                    enqueue(Command.Json("log_update", toSend, SignalingPriority.NORMAL))
                    logScheduledTs.set(0)
                    logBurstCount.set(0)
                }
            } else {
                val conf = framesConflated.incrementAndGet()
                updateMetrics(conflated = conf)
                logBurstCount.incrementAndGet()
            }
        }

        pendingLogMap.updateAndGet { cur ->
            SignalingMessageConflator.conflateLogs(cur, incoming)
        }

        if (logScheduledTs.get() == 0L && pendingLogMap.get() != null) {
            val delayMs = calculateDelay(logBurstCount.get()) * 2 // Logs can afford more delay
            logScheduledTs.set(timeProvider.currentTimeMillis() + delayMs)
            conflationSignal.trySend(Unit)
        }
    }

    private fun calculateDelay(burstCount: Int): Long {
        val baseDelay = if (isViolationProvider()) SIGNALING_CONFLATION_DELAY_VIOLATION_MS else SIGNALING_CONFLATION_DELAY_MS
        if (burstCount < BURST_PRESSURE_THRESHOLD) return baseDelay
        
        // Dynamic scaling: extend delay by 100ms per frame over threshold, up to cap.
        val pressureBonus = (burstCount - BURST_PRESSURE_THRESHOLD) * 100L
        return minOf(baseDelay + pressureBonus, MAX_CONFLATION_DELAY_MS)
    }

    data class Metrics(val received: Long, val emitted: Long, val conflated: Long)

    fun getMetrics() = _metricsFlow.value

    fun shutdown() {
        processorJob?.cancel()
        conflationJob?.cancel()
        highQueue.close()
        normalQueue.close()
        conflationSignal.close()
    }
    
    companion object {
        private const val SIGNALING_EMIT_DELAY_MS = 100L
        private const val SIGNALING_EMIT_DELAY_VIOLATION_MS = 50L
        private const val SIGNALING_CONFLATION_DELAY_MS = 250L
        private const val SIGNALING_CONFLATION_DELAY_VIOLATION_MS = 100L
        
        private const val BURST_PRESSURE_THRESHOLD = 5
        private const val MAX_CONFLATION_DELAY_MS = 2000L
    }
}
