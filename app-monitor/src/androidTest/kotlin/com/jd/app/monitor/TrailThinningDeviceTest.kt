package com.jd.app.monitor

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jd.core.network.SignalingClient
import com.jd.core.shared.BackfillRequest
import com.jd.core.shared.BackfillResponse
import com.jd.core.shared.LocationUpdate
import com.jd.core.shared.TRAIL_KEEP_ALL_MS
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections
import javax.inject.Inject

/** ISSUE-NOV27.1-O168: older rows are thinned, and the gap scan does not ask for the rows thinning removed. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TrailThinningDeviceTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject lateinit var repository: MonitorRepository
    @Inject lateinit var signalingClient: SignalingClient
    @Inject lateinit var trailDao: TrailDao
    @Inject lateinit var dataStore: MonitorDataStore

    private val requests = Collections.synchronizedList(mutableListOf<BackfillRequest>())
    private val client get() = signalingClient as FakeSignalingClient
    private val now = System.currentTimeMillis()
    private val day = 86_400_000L

    @Before
    fun setUp() = runBlocking {
        hiltRule.inject()
        repository
        client.onRequestBackfill = { requests.add(it) }
        dataStore.saveTrailClear(null)
        dataStore.saveTrailThinned(null)
    }

    @After
    fun tearDown() = runBlocking { dataStore.saveTrailThinned(null) }

    /** Still rows every 10 s, the first with sequence [firstSeq], starting at [startMs]. */
    private fun still(firstSeq: Long, count: Int, startMs: Long) =
        (0 until count).map { TrailEntity("T", startMs + it * 10_000L, firstSeq + it, 32.0, 35.0, 5f, "VALID") }

    private fun update(sequence: Long, timestamp: Long = now) =
        LocationUpdate(deviceId = "T", sequenceId = sequence, timestamp = timestamp, lat = 32.0, lng = 35.0)

    /** A live row, then the gap scan it starts. */
    private suspend fun liveRow(sequence: Long) {
        client.emitTelemetry(update(sequence))
        delay(3_000)
    }

    @Test
    fun thinnedRowsAreNotRequestedButANewHoleIs() = runBlocking {
        trailDao.insertAll(still(1, 360, now - 2 * day))
        trailDao.insertAll(still(361, 10, now - 200_000L))
        liveRow(371)
        assertEquals("A whole trail, nothing to ask for", 0, requests.size)

        val deleted = repository.thinTrails(now)

        assertEquals("One row a minute of the old hour stays, and its last row", 360 - 61, deleted)
        assertEquals(now - TRAIL_KEEP_ALL_MS, dataStore.trailThinned.first()?.upToMs)
        liveRow(374)
        assertEquals("Only the new hole", listOf(372L to 373L), requests.map { it.startSequenceId to it.endSequenceId })
    }

    @Test
    fun aHoleFromAnOutageIsStillRequestedAndStopsTheMark() = runBlocking {
        trailDao.insertAll(still(1, 100, now - 2 * day))
        liveRow(200)
        withTimeout(5_000) { while (requests.isEmpty()) delay(50) }

        assertEquals(0, repository.thinTrails(now))

        assertNull("The mark waits for the backfill", dataStore.trailThinned.first())
        assertEquals(listOf(101L to 199L), requests.map { it.startSequenceId to it.endSequenceId })
    }

    @Test
    fun withoutContactTheMarkStays() = runBlocking {
        trailDao.insertAll(still(1, 360, now - 2 * day))

        assertEquals(0, repository.thinTrails(now))

        assertNull(dataStore.trailThinned.first())
        assertEquals(360, trailDao.countRows("T"))
    }

    @Test
    fun lateBackfillRowsBeforeTheMarkAreKeptThenThinned() = runBlocking {
        dataStore.saveTrailThinned(TrailThinned("T", now - day))
        val late = (1L..360L).map { update(it, now - 2 * day + it * 10_000L) }
        client.emitBackfillResponse(BackfillResponse("late", late, true))
        withTimeout(5_000) { while (trailDao.countRows("T") < 360) delay(50) }

        repository.thinTrails(now)

        assertEquals("Thinned up to the old mark, without contact", 61, trailDao.countRows("T"))
    }

    @Test
    fun theGapQueryLeavesOutHolesOlderThanTheMark() = runBlocking {
        val mark = now - day
        trailDao.insertAll(listOf(1L, 5L).map { TrailEntity("T", mark - 1_000L + it, it, 32.0, 35.0, 5f, "VALID") })
        trailDao.insertAll(listOf(9L, 12L).map { TrailEntity("T", mark + it, it, 32.0, 35.0, 5f, "VALID") })

        assertEquals(
            "The hole 2..4 is older than the mark, 6..8 reaches past it",
            listOf(SequenceGap(10, 11), SequenceGap(6, 8)),
            trailDao.getSequenceGaps("T", mark)
        )
        assertEquals(3, trailDao.getSequenceGaps("T").size)
    }

    @Test
    fun thisPhonesOwnTrailIsThinnedToo() = runBlocking {
        val own = (0 until 360).map {
            val ts = now - 2 * day + it * 10_000L
            TrailEntity("V", ts, ts, 32.0, 35.0, 5f, "VALID", isLocalUser = true)
        } + TrailEntity("V", now - 40 * day, now - 40 * day, 32.0, 35.0, 5f, "VALID", isLocalUser = true) +
            TrailEntity("V", now - 60_000L, now - 60_000L, 32.0, 35.0, 5f, "VALID", isLocalUser = true)
        trailDao.insertAll(own)

        repository.thinTrails(now)

        assertEquals("60 of the old hour, its last row and the recent row", 62, trailDao.countRowsBefore(sensor = false, own = true, beforeMs = Long.MAX_VALUE))
        assertTrue(trailDao.countRowsBefore(sensor = false, own = true, beforeMs = now - 31 * day) == 0)
    }
}
