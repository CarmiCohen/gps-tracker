package com.jd.app.monitor

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/** Verifies Chapter 123: Monitor trail storage stays bounded (ISSUE-NOV5.2-O14). */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TrailStorageBoundTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject lateinit var database: MonitorDatabase
    @Inject lateinit var trailDao: TrailDao
    @Inject lateinit var workerFactory: HiltWorkerFactory

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    private fun rows(trackerId: String, sequences: LongRange, isLocalUser: Boolean = false) =
        sequences.map { TrailEntity(trackerId, it, it, 32.0, 35.0, 5f, "VALID", isLocalUser) }

    private fun longValue(sql: String, vararg args: Any): Long =
        database.query(sql, args).use { cursor ->
            cursor.moveToFirst()
            cursor.getLong(0)
        }

    private fun rowCount(trackerId: String) =
        longValue("SELECT COUNT(*) FROM trails WHERE trackerId = ?", trackerId)

    private fun oldestSequence(trackerId: String) =
        longValue("SELECT MIN(sequenceId) FROM trails WHERE trackerId = ?", trackerId)

    /** Creates the worker through the production Hilt factory, as WorkManager does. */
    private fun runWorker(): ListenableWorker.Result = runBlocking {
        TestListenableWorkerBuilder<TrailPruningWorker>(context)
            .setWorkerFactory(workerFactory)
            .build()
            .doWork()
    }

    @Test
    fun workerKeepsTheNewestRowsOfEachTrackerAtTheRealLimit() = runBlocking {
        val total = MAX_STORED_TRAIL_POINTS + 100L
        trailDao.insertAll(rows("T", 1..total))
        trailDao.insertAll(rows("U", 1..10L))

        assertEquals(ListenableWorker.Result.success(), runWorker())

        assertEquals(MAX_STORED_TRAIL_POINTS.toLong(), rowCount("T"))
        assertEquals(101L, oldestSequence("T"))
        assertEquals("A small tracker is not trimmed by a large one", 10L, rowCount("U"))
    }

    @Test
    fun pruningDoesNotMoveTheLastSequence() = runBlocking {
        trailDao.insertAll(rows("T", 1..500L))

        trailDao.pruneOldestRows("T", 100)

        assertEquals("Gap detection must not see a false gap", 500L, trailDao.getLastSequenceId("T"))
    }

    @Test
    fun rowCountAtTheLimitIsLeftAlone() = runBlocking {
        trailDao.insertAll(rows("T", 1..3L))

        assertEquals(0, trailDao.pruneOldestRows("T", 3))
        assertEquals(3L, rowCount("T"))
    }

    @Test
    fun oneRowOverTheLimitRemovesOnlyTheOldest() = runBlocking {
        trailDao.insertAll(rows("T", 1..4L))

        assertEquals(1, trailDao.pruneOldestRows("T", 3))
        assertEquals(3L, rowCount("T"))
        assertEquals(2L, oldestSequence("T"))
    }

    @Test
    fun emptyTableIsHandled() {
        assertEquals(ListenableWorker.Result.success(), runWorker())
    }

    @Test
    fun oldRemoteRowsSurviveWhenUnderTheLimit() = runBlocking {
        val year2020 = 1_577_836_800_000L
        trailDao.insert(TrailEntity("T", year2020, 7L, 32.0, 35.0, 5f, "VALID"))

        runWorker()

        assertEquals("An idle tracker keeps its position and sequence", 7L, trailDao.getLastSequenceId("T"))
    }

    @Test
    fun localRowsExpireAfter48Hours() = runBlocking {
        val now = System.currentTimeMillis()
        val hour = 3_600_000L
        trailDao.insert(TrailEntity("L", now - 49 * hour, 1L, 32.0, 35.0, 5f, "VALID", isLocalUser = true))
        trailDao.insert(TrailEntity("L", now - 47 * hour, 2L, 32.0, 35.0, 5f, "VALID", isLocalUser = true))

        runWorker()

        assertEquals(1L, rowCount("L"))
        assertEquals(2L, oldestSequence("L"))
    }

    @Test
    fun schedulingTwiceKeepsOnePeriodicJob() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setWorkerFactory(workerFactory).build()
        )

        TrailPruningWorker.schedule(context)
        TrailPruningWorker.schedule(context)

        val jobs = WorkManager.getInstance(context).getWorkInfosForUniqueWork("trail_pruning").get()
        assertEquals(1, jobs.size)
        // A new periodic job starts at once, so it may be ENQUEUED or RUNNING here.
        assertFalse("State: ${jobs.single().state}", jobs.single().state.isFinished)
    }
}
