package com.gps19.core.engine

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicReference

/**
 * SmartSignalingDispatcher: Unified reactive coordination layer for signaling.
 * Oct.6.4:
 * - Issue #AUDIT-1006-7: Telemetry Conflation Audit. Integrated Log Conflation 
 *   to merge high-frequency normal-priority logs. Enhanced Location conflation 
 *   to use deep-merge to prevent telemetry fidelity loss (R-ID 511).
 * - Log Conflation Logic: Implemented immediate flush on message change to 
 *   ensure forensic sequence integrity while still merging identical bursts.
 */
class SmartSignalingDispatcher(
    private val scope: CoroutineScope,
    private val isViolationProvider: () -> Boolean,
    private val jsonSink: (String, Map<String, Any?>) -> Unit,
    private val binarySink: (String, String, ByteArray) -> Unit,
    private val isConnectedProvider: () -> Boolean
) {
    sealed class Command {
        data class Json(val event: String, val data: Map<String, Any?>, val priority: SignalingPriority) : Command()
        data class Binary(val event: String, val routingId: String, val data: ByteArray, val priority: SignalingPriority) : Command()
    }

    private val queue = Channel<Command>(capacity = Channel.UNLIMITED)
    
    private val pendingLocationMap = AtomicReference<Map<String, Any?>?>(null)
    private var locationConflationJob: Job? = null
    
    private val pendingLogMap = AtomicReference<Map<String, Any?>?>(null)
    private var logConflationJob: Job? = null

    private var processorJob: Job? = null

    init {
        startProcessor()
    }

    private fun startProcessor() {
        processorJob?.cancel()
        processorJob = scope.launch(Dispatchers.Default) {
            for (command in queue) {
                // Connection Guard: Ensure we only attempt emission when the transport is ready.
                while (!isConnectedProvider() && isActive) {
                    delay(1000)
                }
                if (!isActive) break
                
                val priority = when(command) {
                    is Command.Json -> command.priority
                    is Command.Binary -> command.priority
                }

                when (command) {
                    is Command.Json -> jsonSink(command.event, command.data)
                    is Command.Binary -> binarySink(command.event, command.routingId, command.data)
                }

                // Adaptive Throttling: High priority commands (joins, pings, immediate alerts)
                // bypass the inter-frame delay to ensure system responsiveness.
                if (priority != SignalingPriority.HIGH) {
                    val delayMs = if (isViolationProvider()) SIGNALING_EMIT_DELAY_VIOLATION_MS else SIGNALING_EMIT_DELAY_MS
                    delay(delayMs)
                }
            }
        }
    }

    fun dispatch(command: Command) {
        when (command) {
            is Command.Json -> {
                when {
                    command.event == "location_update" && command.priority != SignalingPriority.HIGH -> {
                        dispatchConflatedLocation(command.data)
                    }
                    command.event == "log_update" && command.priority == SignalingPriority.NORMAL -> {
                        dispatchConflatedLog(command.data)
                    }
                    else -> {
                        // All other JSON commands (including HIGH priority) are queued to maintain order.
                        queue.trySend(command)
                    }
                }
            }
            is Command.Binary -> {
                // Binary telemetry currently bypasses conflation but respects the queue/throttling.
                queue.trySend(command)
            }
        }
    }

    private fun dispatchConflatedLocation(incoming: Map<String, Any?>) {
        pendingLocationMap.updateAndGet { current ->
            SignalingMessageConflator.conflate(current, incoming)
        }

        if (locationConflationJob == null || !locationConflationJob!!.isActive) {
            locationConflationJob = scope.launch(Dispatchers.Default) {
                val delayMs = if (isViolationProvider()) SIGNALING_CONFLATION_DELAY_VIOLATION_MS else SIGNALING_CONFLATION_DELAY_MS
                delay(delayMs)
                
                val toSend = pendingLocationMap.getAndSet(null)
                if (toSend != null) {
                    queue.trySend(Command.Json("location_update", toSend, SignalingPriority.NORMAL))
                }
            }
        }
    }

    private fun dispatchConflatedLog(incoming: Map<String, Any?>) {
        // R-ID 511: Log Conflation Audit.
        // If the incoming log differs from the pending one, flush the pending one 
        // immediately to maintain sequence integrity.
        val current = pendingLogMap.get()
        if (current != null) {
            val pendingMsg = current["message"] as? String
            val incomingMsg = incoming["message"] as? String
            if (pendingMsg != incomingMsg) {
                val toSend = pendingLogMap.getAndSet(null)
                if (toSend != null) {
                    queue.trySend(Command.Json("log_update", toSend, SignalingPriority.NORMAL))
                }
            }
        }

        pendingLogMap.updateAndGet { cur ->
            SignalingMessageConflator.conflateLogs(cur, incoming)
        }

        if (logConflationJob == null || !logConflationJob!!.isActive) {
            logConflationJob = scope.launch(Dispatchers.Default) {
                // Log conflation uses a slightly longer delay to catch bursts.
                delay(SIGNALING_CONFLATION_DELAY_MS * 2)
                
                val toSend = pendingLogMap.getAndSet(null)
                if (toSend != null) {
                    queue.trySend(Command.Json("log_update", toSend, SignalingPriority.NORMAL))
                }
            }
        }
    }

    fun shutdown() {
        processorJob?.cancel()
        locationConflationJob?.cancel()
        logConflationJob?.cancel()
        queue.close()
    }
    
    companion object {
        private const val SIGNALING_EMIT_DELAY_MS = 100L
        private const val SIGNALING_EMIT_DELAY_VIOLATION_MS = 50L
        private const val SIGNALING_CONFLATION_DELAY_MS = 250L
        private const val SIGNALING_CONFLATION_DELAY_VIOLATION_MS = 100L
    }
}
