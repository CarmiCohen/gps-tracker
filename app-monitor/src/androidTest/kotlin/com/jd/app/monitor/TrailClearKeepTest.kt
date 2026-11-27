package com.jd.app.monitor

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ISSUE-NOV27.1-O167: the map CLEAR's queries count and delete only the rows older than the kept span, of the chosen
 * trails, the Sensor's (of every tracker) or this phone's own.
 */
@RunWith(AndroidJUnit4::class)
class TrailClearKeepTest {

    private lateinit var database: MonitorDatabase
    private val dao get() = database.trailDao()
    private val now = 1_764_000_000_000L
    private val hour = 3_600_000L

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            MonitorDatabase::class.java
        ).build()
        // Ten hourly rows each: the Sensor T, a loaded tracker U, and this Monitor V's own.
        dao.insertAll((1..10).map { TrailEntity("T", now - it * hour, 100L - it, 32.0, 35.0, 5f, "VALID") })
        dao.insertAll((1..10).map { TrailEntity("U", now - it * hour, 100L - it, 32.0, 35.0, 5f, "VALID") })
        dao.insertAll((1..10).map { TrailEntity("V", now - it * hour, now - it * hour, 32.0, 35.0, 5f, "VALID", isLocalUser = true) })
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun aKeptSpanDeletesOnlyTheOlderRowsOfBothTrails() = runBlocking {
        val cutoff = TrailClearChoice(TrailKeep.HOURS_6).deleteBeforeMs(now)
        assertEquals(12, dao.countRowsBefore(sensor = true, own = true, beforeMs = cutoff))
        assertEquals(30, dao.countRowsBefore(sensor = true, own = true, beforeMs = Long.MAX_VALUE))

        assertEquals(12, dao.deleteRowsBefore(sensor = true, own = true, beforeMs = cutoff))
        assertEquals("The last 6 h of each trail stay", 6, dao.countRows("T"))
        assertEquals(18, dao.countRowsBefore(sensor = true, own = true, beforeMs = Long.MAX_VALUE))
    }

    @Test
    fun theSensorsTrailAloneLeavesThisPhonesOwn() = runBlocking {
        assertEquals(20, dao.deleteRowsBefore(sensor = true, own = false, beforeMs = Long.MAX_VALUE))
        assertEquals(0, dao.countRows("T"))
        assertEquals(10, dao.countRowsBefore(sensor = false, own = true, beforeMs = Long.MAX_VALUE))
    }

    @Test
    fun thisPhonesTrailAloneLeavesTheSensors() = runBlocking {
        assertEquals(4, dao.deleteRowsBefore(sensor = false, own = true, beforeMs = now - 6 * hour))
        assertEquals(10, dao.countRows("T"))
        assertEquals(6, dao.countRowsBefore(sensor = false, own = true, beforeMs = Long.MAX_VALUE))
    }
}
