package com.jd.app.monitor

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jd.core.network.SignalingClient
import com.jd.core.shared.BackfillRequest
import com.jd.core.shared.BackfillResponse
import com.jd.core.shared.LocationUpdate
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections
import javax.inject.Inject

/** Verifies Chapter 125: stored holes are found and refilled (ISSUE-NOV6.3-O32). */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BackfillGapRecoveryTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject lateinit var repository: MonitorRepository
    @Inject lateinit var signalingClient: SignalingClient
    @Inject lateinit var trailDao: TrailDao

    private val requests = Collections.synchronizedList(mutableListOf<BackfillRequest>())
    private val client get() = signalingClient as FakeSignalingClient

    @Before
    fun setUp() {
        hiltRule.inject()
        repository // Hilt creates it lazily, and only then does it collect telemetry
        client.onRequestBackfill = { requests.add(it) }
    }

    private fun row(sequence: Long) = TrailEntity("T", sequence, sequence, 32.0, 35.0, 5f, "VALID")

    private fun rows(sequences: Iterable<Long>) = sequences.map(::row)

    private fun update(sequence: Long) =
        LocationUpdate(deviceId = "T", sequenceId = sequence, timestamp = sequence, lat = 32.0, lng = 35.0)

    private suspend fun awaitRequests(count: Int) = withTimeout(15_000) {
        while (requests.size < count) delay(50)
    }

    /** Answers as the Sensor does: every requested row, then done. */
    private fun answer(request: BackfillRequest) {
        val updates = (request.startSequenceId..request.endSequenceId).map(::update)
        client.emitBackfillResponse(BackfillResponse(request.requestId, updates, true))
    }

    @Test
    fun gapsAreListedNewestFirst() = runBlocking {
        trailDao.insertAll(rows(listOf(1L, 2L, 5L, 6L, 10L)))

        assertEquals(
            listOf(SequenceGap(7, 9), SequenceGap(3, 4)),
            trailDao.getSequenceGaps("T")
        )
    }

    @Test
    fun aContinuousTrailHasNoGaps() = runBlocking {
        trailDao.insertAll(rows(1L..500L))

        assertEquals(emptyList<SequenceGap>(), trailDao.getSequenceGaps("T"))
    }

    @Test
    fun otherTrackersAndLocalRowsAreIgnored() = runBlocking {
        trailDao.insertAll(rows(listOf(1L, 3L)))
        trailDao.insert(TrailEntity("U", 2L, 2L, 32.0, 35.0, 5f, "VALID"))
        trailDao.insert(TrailEntity("T", 99L, 102L, 32.0, 35.0, 5f, "VALID", isLocalUser = true))

        assertEquals(listOf(SequenceGap(2, 2)), trailDao.getSequenceGaps("T"))
    }

    @Test
    fun scanningTheFullTableStaysFast() = runBlocking {
        val sequences = (1L..MAX_STORED_TRAIL_POINTS + 100L).filterNot { it % 1000L == 0L }
        trailDao.insertAll(rows(sequences))

        val started = System.currentTimeMillis()
        val gaps = trailDao.getSequenceGaps("T")
        val elapsed = System.currentTimeMillis() - started

        assertEquals(50, gaps.size)
        assertTrue("Took ${elapsed}ms", elapsed < 1_000)
    }

    /** The field test: 13 holes, three more than the request limit. */
    @Test
    fun everyHoleIsRefilledWhenThereAreMoreThanTheLimit() = runBlocking {
        val holes = (1L..13L).map { it * 10L }
        trailDao.insertAll(rows((1L..140L) - holes.toSet()))

        client.emitTelemetry(update(142L))
        awaitRequests(BackfillLedger.MAX_ACTIVE_REQUESTS)
        delay(500)
        assertEquals("The limit still applies", BackfillLedger.MAX_ACTIVE_REQUESTS, requests.size)

        var answered = 0
        withTimeout(20_000) {
            while (answered < holes.size + 1) {
                while (answered < requests.size) answer(requests[answered++])
                delay(50)
            }
        }
        withTimeout(5_000) {
            while (trailDao.getSequenceGaps("T").isNotEmpty()) delay(50)
        }

        assertEquals(142L, trailDao.countRows("T").toLong())
    }

    @Test
    fun aHoleTheSensorCannotFillIsAskedOnce() = runBlocking {
        trailDao.insertAll(rows(listOf(1L, 2L, 3L)))

        client.emitTelemetry(update(10L))
        awaitRequests(1)
        client.emitBackfillResponse(BackfillResponse(requests.single().requestId, emptyList(), true))
        delay(3_000)

        // A new jump forces another scan, which still sees the unfilled hole.
        client.emitTelemetry(update(13L))
        awaitRequests(2)
        delay(500)

        assertEquals(
            listOf(4L to 9L, 11L to 12L),
            requests.map { it.startSequenceId to it.endSequenceId }
        )
    }

    @Test
    fun aFailedRequestIsNotForgotten() = runBlocking {
        trailDao.insertAll(rows(listOf(1L, 2L, 3L)))
        var relayReachable = false
        client.onRequestBackfill = { request -> relayReachable && requests.add(request) }

        client.emitTelemetry(update(10L))
        delay(3_000)
        assertEquals("Nothing could be sent", 0, requests.size)

        relayReachable = true
        client.emitTelemetry(update(12L))
        awaitRequests(2)

        assertEquals(
            listOf(11L to 11L, 4L to 9L),
            requests.map { it.startSequenceId to it.endSequenceId }
        )
    }

    @Test
    fun aMissingStartIsRequestedOnAFreshPairing() = runBlocking {
        client.emitTelemetry(update(100L))
        awaitRequests(1)

        assertEquals(1L, requests.single().startSequenceId)
        assertEquals(99L, requests.single().endSequenceId)
    }

    @Test
    fun aPrunedStartIsNotRequested() = runBlocking {
        val first = 1_000L
        val last = first + MAX_STORED_TRAIL_POINTS - 1
        trailDao.insertAll(rows(first..last))

        client.emitTelemetry(update(last + 2))
        awaitRequests(1)
        delay(500)

        assertEquals(
            "Only the real hole, not the pruned rows",
            listOf((last + 1) to (last + 1)),
            requests.map { it.startSequenceId to it.endSequenceId }
        )
    }
}
