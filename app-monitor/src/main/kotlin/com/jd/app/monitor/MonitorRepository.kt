package com.jd.app.monitor

import com.jd.core.network.SignalingClient
import com.jd.core.shared.BackfillRequest
import com.jd.core.shared.LocationUpdate
import com.jd.core.shared.SystemLogger
import com.jd.core.shared.TRAIL_KEEP_ALL_MS
import com.jd.core.shared.thinTrail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
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
    private val signalingClient: SignalingClient,
    private val dataStore: MonitorDataStore
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val ledger = BackfillLedger()
    private val ledgerMutex = Mutex()

    /** Holds the tracker to scan. Conflated, so a burst of updates costs one scan. */
    private val gapScans = Channel<String>(Channel.CONFLATED)

    @Volatile
    private var lastScanMs = 0L

    // The Sensor's rows for the Monitor's own Graphs (ISSUE-NOV10.8-O84 part 3): counted as they arrive, taken each minute.
    private val liveRows = AtomicInteger()
    private val backfilledRows = AtomicInteger()
    @Volatile private var missingRows = 0L

    // Per Sensor, for the thinning guard (ISSUE-NOV27.1-O168): the newest live row's arrival, the open rows of the last scan.
    private val lastLiveMs = ConcurrentHashMap<String, Long>()
    private val openRowsByDevice = ConcurrentHashMap<String, Long>()

    init {
        observeTelemetry()
        observeBackfill()
        observeGapScans()
    }

    fun getTrail(trackerId: String): Flow<List<TrailEntity>> = trailDao.getTrail(trackerId)

    /**
     * The rows received live and by backfill since the last call, and the rows still missing at the newest gap scan:
     * rows the Sensor answered it no longer holds are not counted (ISSUE-NOV15.6-O100 part 2).
     */
    fun takeRowCounts(): RowCounts = RowCounts(liveRows.getAndSet(0), backfilledRows.getAndSet(0), missingRows)

    /** The rows [clearTrail] would delete for [choice], and the rows of its scope (ISSUE-NOV27.1-O167). */
    suspend fun countTrailClear(choice: TrailClearChoice, nowMs: Long = System.currentTimeMillis()): TrailClearCount {
        val sensor = choice.scope.sensor
        val own = choice.scope.own
        return TrailClearCount(
            deleted = trailDao.countRowsBefore(sensor, own, choice.deleteBeforeMs(nowMs)),
            total = trailDao.countRowsBefore(sensor, own, Long.MAX_VALUE)
        )
    }

    /**
     * Deletes the stored trail rows of [choice]'s scope older than its kept span, every one by default (ISSUE-NOV7.1-O35,
     * ISSUE-NOV27.1-O167), and returns their number. With [trackerId] a clear of the Sensor's rows is kept: its rows
     * older than the cutoff are not asked for or stored again (ISSUE-NOV17.7-O107); without it the next pairing gets
     * the whole history. This phone's own rows are never backfilled, so clearing them alone leaves the mark as it is.
     */
    suspend fun clearTrail(
        trackerId: String?,
        choice: TrailClearChoice = TrailClearChoice.EVERYTHING,
        nowMs: Long = System.currentTimeMillis()
    ): Int {
        val sensor = choice.scope.sensor
        if (sensor) dataStore.saveTrailClear(trackerId?.let { dataStore.trailClear.first().raisedTo(it, choice.clearAtMs(nowMs)) })
        if (sensor && trackerId == null) dataStore.saveTrailThinned(null)
        val deleted = trailDao.deleteRowsBefore(sensor, choice.scope.own, choice.deleteBeforeMs(nowMs))
        if (sensor) missingRows = 0L
        return deleted
    }

    /**
     * Thins every stored trail by age and bounds it by [MAX_STORED_TRAIL_POINTS] (ISSUE-NOV27.1-O168); returns the rows
     * deleted. A Sensor's rows are thinned only before its [TrailThinned] mark, which moves to a day ago only while
     * [mayAdvanceThinning]; the mark is stored before any row goes, so a gap scan never asks for a thinned row. This
     * phone's own rows have no holes to ask for and are thinned up to a day ago.
     */
    suspend fun thinTrails(nowMs: Long = System.currentTimeMillis()): Int {
        var deleted = 0
        for (trackerId in trailDao.getTrackerIds()) {
            deleted += thinRows(trackerId, isLocalUser = true, thinBeforeMs = nowMs - TRAIL_KEEP_ALL_MS, nowMs)
            sensorThinBefore(trackerId, nowMs)?.let { deleted += thinRows(trackerId, isLocalUser = false, it, nowMs) }
            deleted += trailDao.pruneOldestRows(trackerId, MAX_STORED_TRAIL_POINTS)
        }
        return deleted
    }

    private suspend fun sensorThinBefore(trackerId: String, nowMs: Long): Long? {
        val mark = dataStore.trailThinned.first()
        val mayAdvance = ledgerMutex.withLock {
            mayAdvanceThinning(lastLiveMs[trackerId], nowMs, ledger.isBackfilling(trackerId, nowMs), openRowsByDevice[trackerId])
        }
        if (!mayAdvance) return mark.upToMsOf(trackerId)
        val raised = mark.thinnedTo(trackerId, nowMs - TRAIL_KEEP_ALL_MS)
        if (raised != mark) dataStore.saveTrailThinned(raised)
        return raised.upToMs
    }

    private suspend fun thinRows(trackerId: String, isLocalUser: Boolean, thinBeforeMs: Long, nowMs: Long): Int {
        val rows = trailDao.getRowsBefore(trackerId, isLocalUser, thinBeforeMs)
        if (rows.isEmpty()) return 0
        val kept = thinTrail(rows, nowMs, thinBeforeMs) { it.toTrailPoint() }.toHashSet()
        val dropped = rows.filterNot { it in kept }
        return if (dropped.isEmpty()) 0 else trailDao.deleteRows(dropped)
    }

    /** Marks [deviceId] as in contact now, as a live row does; for tests (ISSUE-NOV27.1-O168). */
    internal fun markLiveForTest(deviceId: String, nowMs: Long) {
        lastLiveMs[deviceId] = nowMs
    }

    /**
     * For testing purposes only (ISSUE-REVOL30.0-F7).
     */
    suspend fun insertDirectlyForTest(entity: TrailEntity) {
        trailDao.insert(entity)
    }

    /** This Monitor's own trail follows its new ID (ISSUE-NOV22.7-O135). */
    suspend fun moveLocalTrail(fromId: String, toId: String) = trailDao.moveLocalTrail(fromId, toId)

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
                val clear = dataStore.trailClear.first()
                val entities = response.updates.map { update ->
                    update.toEntity()
                }.filter { clear.keeps(it) }
                trailDao.insertAll(entities)
                backfilledRows.addAndGet(entities.size)

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
        lastLiveMs[update.deviceId] = System.currentTimeMillis()
        val lastSeq = trailDao.getLastSequenceId(update.deviceId) ?: 0L

        val entity = update.toEntity()
        if (!dataStore.trailClear.first().keeps(entity)) return

        // Stored first: a hole can only be read from the table once a newer row exists.
        trailDao.insert(entity)
        liveRows.incrementAndGet()

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

    /**
     * The holes worth requesting. A table at the cap has been pruned by time, so neither its missing start nor a
     * hole ending at its oldest row by time is a gap: after a Sensor counter reset the run above the low rows is the
     * oldest, and each prune widens that hole past the range already answered (ISSUE-NOV15.6-O100 part 6). Holes,
     * and a missing start, older than the thinned-up-to mark were made by thinning (ISSUE-NOV27.1-O168).
     */
    private suspend fun findGaps(deviceId: String): List<SequenceGap> {
        val thinnedUpToMs = dataStore.trailThinned.first().upToMsOf(deviceId) ?: Long.MIN_VALUE
        val gaps = trailDao.getSequenceGaps(deviceId, thinnedUpToMs)
        val first = trailDao.getFirstSequenceId(deviceId) ?: return gaps
        if ((trailDao.getFirstSequenceTime(deviceId) ?: Long.MAX_VALUE) < thinnedUpToMs) return gaps

        if (trailDao.countRows(deviceId) >= MAX_STORED_TRAIL_POINTS) {
            val oldest = trailDao.getOldestSequenceId(deviceId)
            return gaps.filterNot { it.lastMissing + 1 == oldest }
        }
        // The rows before a clear are not missing (ISSUE-NOV17.7-O107).
        if (dataStore.trailClear.first().clearedStartOf(deviceId)) return gaps
        return if (first > 1) gaps + SequenceGap(1, first - 1) else gaps
    }

    private suspend fun requestMissingRanges(deviceId: String) {
        lastScanMs = System.currentTimeMillis()
        val gaps = findGaps(deviceId)

        ledgerMutex.withLock {
            val now = System.currentTimeMillis()
            val toRequest = ledger.gapsToRequest(deviceId, gaps, now)
            missingRows = ledger.openRows(deviceId, gaps)
            openRowsByDevice[deviceId] = missingRows
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
