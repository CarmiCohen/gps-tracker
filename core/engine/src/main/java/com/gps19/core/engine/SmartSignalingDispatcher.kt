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
 * Oct.10.3:
 * - Issue #SIMP-1010-4: HUD Interface Alignment. Refactored to use property-based 
 *   TimeProvider API.
 */
class SmartSignalingDispatcher(
    private var scope: CoroutineScope,
    private val isViolationProvider: () -> Boolean,
    private val wireSink: SignalingWireSink,
    private val encoder: SignalingEncoder,
    private val isConnectedProvider: () -> Boolean,
    private val timeProvider: TimeProvider = object : TimeProvider {
        override val currentTimeMillis: Long get() = System.currentTimeMillis()
        override val elapsedRealtime: Long get() = System.currentTimeMillis()
    },
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : SignalingPipeline {

    sealed class Command {
        abstract val priority: SignalingPriority
        data class Json(val event: String, val data: Map<String, Any?>, override val priority: SignalingPriority) : Command()
        data class Binary(val event: String, val routingId: String, val data: ByteArray, override val priority: SignalingPriority) : Command()
        data class Object(val event: String, val update: LocationUpdate, val fromViewer: Boolean, override val priority: SignalingPriority) : Command()
    }

    private class ConflationBucket<T> {
        val pending = AtomicReference<T?>(null)
        val scheduledTs = AtomicLong(0)
        val firstEntryTs = AtomicLong(0)
        val burstCount = AtomicInteger(0)

        fun reset() {
            pending.set(null)
            scheduledTs.set(0)
            firstEntryTs.set(0)
            burstCount.set(0)
        }
    }

    private var highQueue = Channel<Command>(capacity = Channel.UNLIMITED)
    private var normalQueue = Channel<Command>(capacity = Channel.UNLIMITED)
    
    private val locMapBucket = ConflationBucket<Map<String, Any?>>()
    private val locObjBucket = ConflationBucket<LocationUpdate>()
    private val logBucket = ConflationBucket<Map<String, Any?>>()

    private var conflationSignal = Channel<Unit>(capacity = Channel.CONFLATED)
    private var conflationJob: Job? = null

    private val framesReceived = AtomicLong(0)
    private val framesEmitted = AtomicLong(0)
    private val framesConflated = AtomicLong(0)

    private val deltaState = SignalingDeltaState()

    private val _metricsFlow = MutableStateFlow(SignalingPipeline.Metrics(0, 0, 0))
    override val metricsFlow: StateFlow<SignalingPipeline.Metrics> = _metricsFlow.asStateFlow()

    private var processorJob: Job? = null
    
    @Volatile
    private var lastNormalEmitTs = 0L

    init {
        startProcessor()
        startConflationLoop()
    }

    override fun reinitialize(scope: CoroutineScope) {
        if (processorJob?.isActive == true && !highQueue.isClosedForSend) return
        
        shutdown()
        this.scope = scope
        highQueue = Channel(capacity = Channel.UNLIMITED)
        normalQueue = Channel(capacity = Channel.UNLIMITED)
        conflationSignal = Channel(capacity = Channel.CONFLATED)
        
        locMapBucket.reset()
        locObjBucket.reset()
        logBucket.reset()
        
        startProcessor()
        startConflationLoop()
    }

    override fun reset() {
        deltaState.reset()
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
                        val now = timeProvider.currentTimeMillis
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
                        lastNormalEmitTs = timeProvider.currentTimeMillis
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
                        val now = timeProvider.currentTimeMillis
                        var nextCheck = Long.MAX_VALUE
                        
                        // Check Location Map
                        nextCheck = minOf(nextCheck, checkBucket(locMapBucket, now) { 
                            Command.Json("location_update", it, SignalingPriority.NORMAL) 
                        })

                        // Check Location Object
                        nextCheck = minOf(nextCheck, checkBucket(locObjBucket, now) { 
                            Command.Object("location_update_bin", it, false, SignalingPriority.NORMAL) 
                        })

                        // Check Log Map
                        nextCheck = minOf(nextCheck, checkBucket(logBucket, now) { 
                            Command.Json("log_update", it, SignalingPriority.NORMAL) 
                        })

                        if (nextCheck == Long.MAX_VALUE) break
                        
                        val waitTime = nextCheck.coerceAtLeast(10L)
                        withTimeoutOrNull(waitTime) {
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

    private fun <T> checkBucket(bucket: ConflationBucket<T>, now: Long, commandFactory: (T) -> Command): Long {
        val ts = bucket.scheduledTs.get()
        if (ts > 0) {
            if (now >= ts) {
                bucket.scheduledTs.set(0)
                bucket.firstEntryTs.set(0)
                bucket.burstCount.set(0)
                bucket.pending.getAndSet(null)?.let {
                    enqueue(commandFactory(it))
                }
            } else {
                return ts - now
            }
        }
        return Long.MAX_VALUE
    }

    private fun emit(command: Command) {
        val emitted = framesEmitted.incrementAndGet()
        updateMetrics(emitted = emitted)
        when (command) {
            is Command.Json -> wireSink.emitJson(command.event, command.data)
            is Command.Binary -> wireSink.emitBinary(command.event, command.routingId, command.data)
            is Command.Object -> {
                val payload = encoder.encodeObject(command.update, deltaState, command.fromViewer)
                wireSink.emitBinary(command.event, SignalingConstants.getTransmissionId(command.update.deviceId), payload)
            }
        }
    }

    override fun dispatchJson(event: String, data: Map<String, Any?>, priority: SignalingPriority) {
        dispatch(Command.Json(event, data, priority))
    }

    override fun dispatchBinary(event: String, routingId: String, data: ByteArray, priority: SignalingPriority) {
        dispatch(Command.Binary(event, routingId, data, priority))
    }

    override fun dispatchObject(event: String, update: LocationUpdate, priority: SignalingPriority, fromViewer: Boolean) {
        dispatch(Command.Object(event, update, fromViewer, priority))
    }

    private fun dispatch(command: Command) {
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
            SignalingPipeline.Metrics(
                received = received ?: current.received,
                emitted = emitted ?: current.emitted,
                conflated = conflated ?: current.conflated
            )
        }
    }

    private fun dispatchConflatedLocationMap(incoming: Map<String, Any?>) {
        var wasConflated = false
        locMapBucket.pending.updateAndGet { current ->
            if (current != null) wasConflated = true
            conflateMaps(current, incoming)
        }
        if (wasConflated) {
            val conf = framesConflated.incrementAndGet()
            updateMetrics(conflated = conf)
            locMapBucket.burstCount.incrementAndGet()
        }

        updateBucketSchedule(locMapBucket)
    }

    private fun dispatchConflatedLocationObject(incoming: LocationUpdate) {
        var wasConflated = false
        locObjBucket.pending.updateAndGet { current ->
            if (current != null) wasConflated = true
            conflateLocationUpdates(current, incoming)
        }
        if (wasConflated) {
            val conf = framesConflated.incrementAndGet()
            updateMetrics(conflated = conf)
            locObjBucket.burstCount.incrementAndGet()
        }

        updateBucketSchedule(locObjBucket)
    }

    private fun dispatchConflatedLog(incoming: Map<String, Any?>) {
        val current = logBucket.pending.get()
        if (current != null) {
            val pendingMsg = current["message"] as? String
            val incomingMsg = incoming["message"] as? String
            if (pendingMsg != incomingMsg) {
                // Sequence break: Flush immediately
                logBucket.pending.getAndSet(null)?.let {
                    enqueue(Command.Json("log_update", it, SignalingPriority.NORMAL))
                }
                logBucket.scheduledTs.set(0)
                logBucket.firstEntryTs.set(0)
                logBucket.burstCount.set(0)
            } else {
                val conf = framesConflated.incrementAndGet()
                updateMetrics(conflated = conf)
                logBucket.burstCount.incrementAndGet()
            }
        }

        logBucket.pending.updateAndGet { cur ->
            conflateLogMaps(cur, incoming)
        }

        updateBucketSchedule(logBucket, delayMultiplier = 2)
    }

    private fun <T> updateBucketSchedule(bucket: ConflationBucket<T>, delayMultiplier: Int = 1) {
        val now = timeProvider.currentTimeMillis
        if (bucket.scheduledTs.get() == 0L) {
            bucket.firstEntryTs.set(now)
            val delayMs = calculateDelay(bucket.burstCount.get()) * delayMultiplier
            bucket.scheduledTs.set(now + delayMs)
            conflationSignal.trySend(Unit)
        } else {
            val currentScheduled = bucket.scheduledTs.get()
            val firstArrival = bucket.firstEntryTs.get()
            
            // Hard Starvation Cap: Never delay more than MAX_CONFLATION_DELAY_MS from the first entry.
            if (now - firstArrival >= MAX_CONFLATION_DELAY_MS) return

            val newDelay = calculateDelay(bucket.burstCount.get()) * delayMultiplier
            val targetScheduled = now + newDelay
            
            // Clamp to starvation cap
            val finalTarget = minOf(targetScheduled, firstArrival + MAX_CONFLATION_DELAY_MS)
            
            if (currentScheduled < finalTarget && finalTarget - now >= newDelay / 2) {
                 bucket.scheduledTs.compareAndSet(currentScheduled, finalTarget)
            }
        }
    }

    private fun calculateDelay(burstCount: Int): Long {
        val baseDelay = if (isViolationProvider()) SIGNALING_CONFLATION_DELAY_VIOLATION_MS else SIGNALING_CONFLATION_DELAY_MS
        if (burstCount < BURST_PRESSURE_THRESHOLD) return baseDelay
        
        val pressureBonus = (burstCount - BURST_PRESSURE_THRESHOLD) * 100L
        return minOf(baseDelay + pressureBonus, MAX_CONFLATION_DELAY_MS)
    }

    // --- Conflation Strategies (Issue #SIGN-1006-13 Consolidation) ---

    private fun conflateMaps(pending: Map<String, Any?>?, incoming: Map<String, Any?>): Map<String, Any?> {
        if (pending == null) return incoming
        val merged = pending.toMutableMap()
        merged.putAll(incoming)
        return merged
    }

    private fun conflateLocationUpdates(pending: LocationUpdate?, incoming: LocationUpdate): LocationUpdate {
        if (pending == null) return incoming
        
        // R-ID 511: Preserve fidelity across the burst.
        // Oct.7.4: Refactored to use unified ForensicSnapshot container.
        val pF = pending.integrity.forensic
        val iF = incoming.integrity.forensic
        
        if (iF.thermal == null && pF.thermal != null) iF.thermal = pF.thermal
        if (iF.heap == null && pF.heap != null) iF.heap = pF.heap
        if (iF.snr == null && pF.snr != null) iF.snr = pF.snr
        if (iF.vibe == null && pF.vibe != null) iF.vibe = pF.vibe

        if (incoming.integrity.uptimeMs == 0L && pending.integrity.uptimeMs > 0) {
            incoming.integrity.uptimeMs = pending.integrity.uptimeMs
        }
        return incoming
    }

    private fun conflateLogMaps(pending: Map<String, Any?>?, incoming: Map<String, Any?>): Map<String, Any?> {
        if (pending == null) return incoming
        
        val pendingMsg = pending["message"] as? String
        val incomingMsg = incoming["message"] as? String
        
        return if (pendingMsg != null && pendingMsg == incomingMsg) {
            val merged = pending.toMutableMap()
            val currentCount = (merged["burst_count"] as? Int) ?: 1
            merged["burst_count"] = currentCount + 1
            merged["timestamp"] = incoming["timestamp"]
            merged
        } else {
            incoming
        }
    }

    override fun shutdown() {
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
