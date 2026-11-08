package com.jd.app.monitor

import com.jd.core.network.SignalingClient
import com.jd.core.shared.BackfillRequest
import com.jd.core.shared.LocationUpdate
import com.jd.core.shared.SystemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MonitorRepository: Orchestrates telemetry ingestion and Forensic Backfill (R-ID 768).
 * Missing ranges are read from the stored trail, so a range that could not be requested
 * or was never answered is found again by a later scan (ISSUE-NOV6.3-O32).
 */
@Singleton
class MonitorRepository @Inject constructor(
    private val trailDao: TrailDao,
    private val signalingClient: SignalingClient
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val ledger = BackfillLedger()
    private val ledgerMutex = Mutex()

    /** Holds the tracker to scan. Conflated, so a burst of updates costs one scan. */
    private val gapScans = Channel<String>(Channel.CONFLATED)

    @Volatile
    private var lastScanMs = 0L

    init {
        observeTelemetry()
        observeBackfill()
        observeGapScans()
    }

    fun getTrail(trackerId: String): Flow<List<TrailEntity>> = trailDao.getTrail(trackerId)

    /** Clears all stored trail entries (ISSUE-NOV7.1-O35). */
    suspend fun clearTrail() {
        trailDao.deleteAll()
    }

    /**
     * For testing purposes only (ISSUE-REVOL30.0-F7).
     */
    suspend fun insertDirectlyForTest(entity: TrailEntity) {
        trailDao.insert(entity)
    }

    /** Inserts local user movement fix into trail database (ISSUE-NOV6.2-O29). */
    suspend fun insertLocalTrail(selfId: String, fix: LocalFix) {
        val ts = if (fix.reading.timeMs > 0) fix.reading.timeMs else System.currentTimeMillis()
        val entity = TrailEntity(
            trackerId = selfId,
            timestamp = ts,
            sequenceId = ts,
            lat = fix.position.lat,
            lng = fix.position.lng,
            accuracy = fix.reading.accuracyMeters,
            status = "VALID",
            isLocalUser = true
        )
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

                ledgerMutex.withLock {
                    ledger.onChunkReceived(response.requestId, System.currentTimeMillis())
                    if (response.isComplete) {
                        ledger.onRequestComplete(response.requestId)?.let { gapScans.trySend(it.deviceId) }
                    }
                }
            }
        }
    }

    /** Scans run only while telemetry arrives, so a silent Sensor gets no repeated requests. */
    private fun observeGapScans() {
        repositoryScope.launch {
            for (deviceId in gapScans) {
                delay(GAP_SETTLE_MS)
                requestMissingRanges(deviceId)
            }
        }
    }

    private suspend fun processIncomingUpdate(update: LocationUpdate) {
        val lastSeq = trailDao.getLastSequenceId(update.deviceId) ?: 0L

        // Stored first: a hole can only be read from the table once a newer row exists.
        trailDao.insert(update.toEntity())

        val isSequenceJump = update.sequenceId > lastSeq + 1
        if (isSequenceJump || System.currentTimeMillis() - lastScanMs >= GAP_RESCAN_MS) {
            gapScans.trySend(update.deviceId)
        }
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

    private suspend fun findGaps(deviceId: String): List<SequenceGap> {
        val gaps = trailDao.getSequenceGaps(deviceId)
        val first = trailDao.getFirstSequenceId(deviceId) ?: return gaps

        // A table at the cap has been pruned, so its missing start is not a gap.
        val hasMissingStart = first > 1 && trailDao.countRows(deviceId) < MAX_STORED_TRAIL_POINTS
        return if (hasMissingStart) gaps + SequenceGap(1, first - 1) else gaps
    }

    private suspend fun requestMissingRanges(deviceId: String) {
        lastScanMs = System.currentTimeMillis()
        val gaps = findGaps(deviceId)

        ledgerMutex.withLock {
            val now = System.currentTimeMillis()
            val toRequest = ledger.gapsToRequest(deviceId, gaps, now)
            if (toRequest.isNotEmpty()) {
                SystemLogger.w("Backfill", "${gaps.size} gaps stored. Requesting ${toRequest.size}.")
            }

            for (gap in toRequest) {
                val request = BackfillRequest(deviceId, gap.firstMissing, gap.lastMissing, UUID.randomUUID().toString())
                if (!signalingClient.requestBackfill(request)) break
                SystemLogger.i("Backfill", "Requested ${gap.firstMissing} to ${gap.lastMissing}")
                ledger.onRequestSent(request, now)
            }
        }
    }

    private companion object {
        /** Lets the rest of a reconnect burst land, so its rows are not requested as well. */
        const val GAP_SETTLE_MS = 2_000L
        const val GAP_RESCAN_MS = 30_000L
    }
}
