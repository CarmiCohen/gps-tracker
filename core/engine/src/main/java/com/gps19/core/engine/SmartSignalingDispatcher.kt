package com.gps19.core.engine

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * SmartSignalingDispatcher: Unified reactive coordination layer for signaling.
 * Oct.6.6:
 * - Issue #AUDIT-1006-8: Conflation Metrics Audit. Added AtomicLong counters for 
 *   received, emitted, and conflated frames to monitor radio efficiency (R-ID 511).
 * Oct.6.5:
 * - Issue #AUDIT-1006-7: Binary Telemetry Conflation. Added Object-based 
 *   LocationUpdate command to allow conflation of Protobuf telemetry before 
 *   serialization, matching JSON efficiency (Rule 1.122).
 * - Issue #AUDIT-1006-5: Zero-Latency High Priority Dispatch. Refactored the 
 *   processor to ensure HIGH priority messages bypass the inter-frame delay 
 *   of preceding NORMAL messages (Rule 1.119).
 */
class SmartSignalingDispatcher(
    private val scope: CoroutineScope,
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

    private val highQueue = Channel<Command>(capacity = Channel.UNLIMITED)
    private val normalQueue = Channel<Command>(capacity = Channel.UNLIMITED)
    
    private val pendingLocationMap = AtomicReference<Map<String, Any?>?>(null)
    private var locationConflationJob: Job? = null
    
    private val pendingLocationObject = AtomicReference<LocationUpdate?>(null)
    private var locationObjectConflationJob: Job? = null
    
    private val pendingLogMap = AtomicReference<Map<String, Any?>?>(null)
    private var logConflationJob: Job? = null

    private val framesReceived = AtomicLong(0)
    private val framesEmitted = AtomicLong(0)
    private val framesConflated = AtomicLong(0)

    private var processorJob: Job? = null
    
    @Volatile
    private var lastNormalEmitTs = 0L

    init {
        startProcessor()
    }

    private fun startProcessor() {
        processorJob?.cancel()
        processorJob = scope.launch(dispatcher) {
            while (isActive) {
                while (!isConnectedProvider() && isActive) {
                    delay(1000)
                }
                if (!isActive) break

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
                            highQueue.trySend(command) 
                            continue
                        }
                    }
                    lastNormalEmitTs = timeProvider.currentTimeMillis()
                }

                emit(command)
            }
        }
    }

    private fun emit(command: Command) {
        framesEmitted.incrementAndGet()
        when (command) {
            is Command.Json -> jsonSink(command.event, command.data)
            is Command.Binary -> binarySink(command.event, command.routingId, command.data)
            is Command.Object -> objectSink(command.event, command.update)
        }
    }

    fun dispatch(command: Command) {
        framesReceived.incrementAndGet()
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

    private fun dispatchConflatedLocationMap(incoming: Map<String, Any?>) {
        var wasConflated = false
        pendingLocationMap.updateAndGet { current ->
            if (current != null) wasConflated = true
            SignalingMessageConflator.conflate(current, incoming)
        }
        if (wasConflated) framesConflated.incrementAndGet()

        if (locationConflationJob == null || !locationConflationJob!!.isActive) {
            locationConflationJob = scope.launch(dispatcher) {
                val delayMs = if (isViolationProvider()) SIGNALING_CONFLATION_DELAY_VIOLATION_MS else SIGNALING_CONFLATION_DELAY_MS
                delay(delayMs)
                
                val toSend = pendingLocationMap.getAndSet(null)
                if (toSend != null) {
                    enqueue(Command.Json("location_update", toSend, SignalingPriority.NORMAL))
                }
            }
        }
    }

    private fun dispatchConflatedLocationObject(incoming: LocationUpdate) {
        var wasConflated = false
        pendingLocationObject.updateAndGet { current ->
            if (current != null) wasConflated = true
            SignalingMessageConflator.conflateLocationUpdate(current, incoming)
        }
        if (wasConflated) framesConflated.incrementAndGet()

        if (locationObjectConflationJob == null || !locationObjectConflationJob!!.isActive) {
            locationObjectConflationJob = scope.launch(dispatcher) {
                val delayMs = if (isViolationProvider()) SIGNALING_CONFLATION_DELAY_VIOLATION_MS else SIGNALING_CONFLATION_DELAY_MS
                delay(delayMs)
                
                val toSend = pendingLocationObject.getAndSet(null)
                if (toSend != null) {
                    enqueue(Command.Object("location_update_bin", toSend, SignalingPriority.NORMAL))
                }
            }
        }
    }

    private fun dispatchConflatedLog(incoming: Map<String, Any?>) {
        val current = pendingLogMap.get()
        if (current != null) {
            val pendingMsg = current["message"] as? String
            val incomingMsg = incoming["message"] as? String
            if (pendingMsg != incomingMsg) {
                val toSend = pendingLogMap.getAndSet(null)
                if (toSend != null) {
                    enqueue(Command.Json("log_update", toSend, SignalingPriority.NORMAL))
                }
            } else {
                framesConflated.incrementAndGet()
            }
        }

        pendingLogMap.updateAndGet { cur ->
            SignalingMessageConflator.conflateLogs(cur, incoming)
        }

        if (logConflationJob == null || !logConflationJob!!.isActive) {
            logConflationJob = scope.launch(dispatcher) {
                delay(SIGNALING_CONFLATION_DELAY_MS * 2)
                
                val toSend = pendingLogMap.getAndSet(null)
                if (toSend != null) {
                    enqueue(Command.Json("log_update", toSend, SignalingPriority.NORMAL))
                }
            }
        }
    }

    data class Metrics(val received: Long, val emitted: Long, val conflated: Long)

    fun getMetrics() = Metrics(
        received = framesReceived.get(),
        emitted = framesEmitted.get(),
        conflated = framesConflated.get()
    )

    fun shutdown() {
        processorJob?.cancel()
        locationConflationJob?.cancel()
        locationObjectConflationJob?.cancel()
        logConflationJob?.cancel()
        highQueue.close()
        normalQueue.close()
    }
    
    companion object {
        private const val SIGNALING_EMIT_DELAY_MS = 100L
        private const val SIGNALING_EMIT_DELAY_VIOLATION_MS = 50L
        private const val SIGNALING_CONFLATION_DELAY_MS = 250L
        private const val SIGNALING_CONFLATION_DELAY_VIOLATION_MS = 100L
    }
}
