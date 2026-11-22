package com.jd.app.monitor

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Chapter 240.2 (ISSUE-NOV22.7-O135): a new Monitor ID takes the Monitor's own trail with it; the Sensor's rows stay, and
 * a row the new ID already holds at the same time is replaced, not a failed move.
 */
@RunWith(AndroidJUnit4::class)
class MonitorIdTrailTest {

    private lateinit var database: MonitorDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            MonitorDatabase::class.java
        ).build()
    }

    @After
    fun tearDown() = database.close()

    private fun own(id: String, ts: Long, lat: Double = 32.0) = TrailEntity(id, ts, ts, lat, 35.0, 5f, "VALID", isLocalUser = true)

    @Test
    fun theOwnTrailMovesToTheNewIdAndTheSensorRowsStay() = runBlocking {
        val dao = database.trailDao()
        dao.insertAll(listOf(own("V", 1L), own("V", 2L), own("M1", 2L, lat = 31.0)))
        dao.insertAll((1L..3L).map { TrailEntity("T", it, it, 32.5, 35.5, 5f, "VALID") })

        dao.moveLocalTrail(from = "V", to = "M1")

        assertTrue("Nothing is left under the old ID", dao.getTrail("V").first().isEmpty())
        val moved = dao.getTrail("M1").first()
        assertEquals(listOf(2L, 1L), moved.map { it.timestamp })
        assertEquals("The moved row replaced the one at the same time", 32.0, moved.first().lat, 0.0)
        assertEquals("The Sensor's rows stay", 3, dao.getTrail("T").first().size)
    }

    @Test
    fun aSensorRowUnderTheOldIdIsNotMoved() = runBlocking {
        val dao = database.trailDao()
        dao.insert(TrailEntity("V", 5L, 5L, 32.5, 35.5, 5f, "VALID"))

        dao.moveLocalTrail(from = "V", to = "M1")

        assertEquals(1, dao.getTrail("V").first().size)
        assertTrue(dao.getTrail("M1").first().isEmpty())
    }
}
