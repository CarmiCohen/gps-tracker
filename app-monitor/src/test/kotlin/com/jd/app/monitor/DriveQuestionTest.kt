package com.jd.app.monitor

import com.jd.core.shared.LocationUpdate
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** ISSUE-NOV26.0-O163 part 1: the Monitor finds the paired Sensor's drives in its live rows. */
class DriveQuestionTest {

    private val metersPerDegree = 111_195.0

    private fun row(id: String, timeS: Int, northM: Double, speedKmh: Float, noFix: Boolean = false) = LocationUpdate(
        deviceId = id,
        lat = if (noFix) 0.0 else 32.0 + northM / metersPerDegree,
        lng = if (noFix) 0.0 else 34.8,
        speed = speedKmh / 3.6f,
        timestamp = timeS * 1000L
    )

    /** 2 min at 54 km/h, then 10 min parked, one row per 10 s while parked. */
    private fun driveAndPark(id: String, fromS: Int = 0): List<LocationUpdate> =
        (0 until 120).map { row(id, fromS + it, it * 15.0, 54f) } +
            (120 until 720 step 10).map { row(id, fromS + it, 1785.0, 0f) }

    @Test
    fun aDriveOfThePairedSensorEndsOnce() = runBlocking {
        val ends = driveEnds(driveAndPark("T").asFlow()) { "T" }.toList()
        assertEquals(listOf(DriveEnd("T", 118_000L)), ends)
    }

    @Test
    fun anotherTrackersRowsAndRowsWithoutAFixAreSkipped() = runBlocking {
        val rows = driveAndPark("X") + driveAndPark("T").map { if (it.timestamp >= 120_000L) it else it.copy(lat = 0.0, lng = 0.0) }
        assertTrue(driveEnds(rows.asFlow()) { "T" }.toList().isEmpty())
    }

    @Test
    fun aDriveWhilePairedToAnotherSensorOrUnpairedIsNotAsked() = runBlocking {
        // Paired to X during T's drive, unpaired, then to T while it is parked: the new target starts over.
        for (before in listOf<String?>("X", null)) {
            var index = 0
            val ends = driveEnds(driveAndPark("T").asFlow()) { if (index++ < 120) before else "T" }.toList()
            assertTrue("paired to $before", ends.isEmpty())
        }
    }
}
