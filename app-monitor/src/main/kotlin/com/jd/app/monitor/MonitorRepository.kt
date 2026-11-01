package com.jd.app.monitor

import com.jd.core.network.SignalingClient
import com.jd.core.shared.BackfillRequest
import com.jd.core.shared.BackfillResponse
import com.jd.core.shared.LocationUpdate
import com.jd.core.shared.SystemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MonitorRepository: Orchestrates telemetry ingestion and Forensic Backfill (R-ID 768).
 * Detects sequence gaps in incoming data and requests missing ranges from the Sensor.
 * Hardened for Nov1.1: Range-based deduplication to prevent redundant backfill requests (ISSUE-REVOL32.0-F1).
 */
@Singleton
class MonitorRepository @Inject constructor(
    private val trailDao: TrailDao,
    private val signalingClient: SignalingClient
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeRequests = mutableMapOf<String, BackfillRequest>()
    private val rangeMutex = Mutex()

    init {
        observeTelemetry()
        observeBackfill()
    }

    fun getTrail(trackerId: String): Flow<List<TrailEntity>> = trailDao.getTrail(trackerId)

    /**
     * For testing purposes only (ISSUE-REVOL30.0-F7).
     */
    suspend fun insertDirectlyForTest(entity: TrailEntity) {
        trailDao.insert(entity)
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
                
                // Optimization: Batch ingestion for backfill (SIMP-REVOL30.6-S1)
                val entities = response.updates.map { update ->
                    update.toEntity()
                }
                trailDao.insertAll(entities)

                if (response.isComplete) {
                    rangeMutex.withLock {
                        activeRequests.remove(response.requestId)
                    }
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

        trailDao.insert(update.toEntity())
    }

    private fun LocationUpdate.toEntity() = TrailEntity(
        trackerId = deviceId,
        timestamp = timestamp,
        sequenceId = sequenceId,
        lat = lat,
        lng = lng,
        accuracy = accuracy,
        status = status,
        isLocalUser = false
    )

    private suspend fun triggerBackfill(deviceId: String, start: Long, end: Long) {
        rangeMutex.withLock {
            // Hardening: Check if this range (or an overlapping one) is already being fetched
            val isRedundant = activeRequests.values.any { 
                it.deviceId == deviceId && it.startSequenceId <= start && it.endSequenceId >= end 
            }
            
            if (isRedundant) {
                SystemLogger.i("Backfill", "Request for $deviceId ($start..$end) skipped: overlap with active request.")
                return
            }

            if (activeRequests.size > 10) return // Increased limit for multi-peer scenarios

            val requestId = UUID.randomUUID().toString()
            SystemLogger.w("Backfill", "Gap detected ($start to $end). Requesting backfill...")
            val request = BackfillRequest(deviceId, start, end, requestId)
            if (signalingClient.requestBackfill(request)) {
                activeRequests[requestId] = request
            }
        }
    }
}
