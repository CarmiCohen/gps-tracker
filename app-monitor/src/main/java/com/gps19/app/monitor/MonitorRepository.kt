package com.gps19.app.monitor

import com.gps19.core.network.SignalingClient
import com.gps19.core.shared.BackfillRequest
import com.gps19.core.shared.LocationUpdate
import com.gps19.core.shared.SystemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MonitorRepository: Orchestrates telemetry ingestion and Forensic Backfill (R-ID 768).
 * Detects sequence gaps in incoming data and requests missing ranges from the Sensor.
 */
@Singleton
class MonitorRepository @Inject constructor(
    private val trailDao: TrailDao,
    private val signalingClient: SignalingClient
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeRequests = mutableSetOf<String>()

    init {
        observeTelemetry()
        observeBackfill()
    }

    private fun observeTelemetry() {
        repositoryScope.launch {
            signalingClient.telemetryFlow.collect { update ->
                processIncomingUpdate(update)
            }
        }
    }

    private fun observeBackfill() {
        repositoryScope.launch {
            signalingClient.backfillResponseFlow.collect { response ->
                SystemLogger.i("Backfill", "Received backfill chunk for req: ${response.requestId}")
                response.updates.forEach { update ->
                    processIncomingUpdate(update, isBackfill = true)
                }
                if (response.isComplete) {
                    activeRequests.remove(response.requestId)
                }
            }
        }
    }

    private suspend fun processIncomingUpdate(update: LocationUpdate, isBackfill: Boolean = false) {
        val lastSeq = trailDao.getLastSequenceId(update.deviceId) ?: 0L
        
        // Gap Detection Logic (R-ID 768)
        if (!isBackfill && update.sequenceId > lastSeq + 1) {
            triggerBackfill(update.deviceId, lastSeq + 1, update.sequenceId - 1)
        }

        trailDao.insert(
            TrailEntity(
                trackerId = update.deviceId,
                timestamp = update.timestamp,
                sequenceId = update.sequenceId,
                lat = update.lat,
                lng = update.lng,
                accuracy = update.accuracy,
                status = update.status, // Fixed: update.status is now a String
                isLocalUser = false
            )
        )
    }

    private suspend fun triggerBackfill(deviceId: String, start: Long, end: Long) {
        val requestId = UUID.randomUUID().toString()
        if (activeRequests.size > 5) return // Prevent flooding

        SystemLogger.w("Backfill", "Gap detected ($start to $end). Requesting backfill...")
        val request = BackfillRequest(deviceId, start, end, requestId)
        if (signalingClient.requestBackfill(request)) {
            activeRequests.add(requestId)
        }
    }
}
