package com.jd.app.monitor

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies Chapter 120.3: the map receives at most the newest 1000 trail points. */
@RunWith(AndroidJUnit4::class)
class TrailCapTest {

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

    @Test
    fun trailIsCappedToTheNewestPoints() = runBlocking {
        val total = MAX_TRAIL_POINTS + 200
        database.trailDao().insertAll(
            (1..total).map { TrailEntity("T", it.toLong(), it.toLong(), 32.0, 35.0, 5f, "VALID") }
        )

        val trail = database.trailDao().getTrail("T").first()

        assertEquals(MAX_TRAIL_POINTS, trail.size)
        assertEquals(total.toLong(), trail.first().sequenceId)
    }
}
