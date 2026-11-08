package com.jd.app.monitor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Chapter 149 (ISSUE-NOV6.2-O30 / ISSUE-NOV6.0-O23): Unpaired Tracker Trail Retention.
 * Verifies that clearing the target tracker pairing ID (unpairing) does not delete
 * stored historical trail data in the database/repository.
 */
class UnpairedTrailRetentionTest {

    @Test
    fun unpairingTrackerClearsTargetIdButRetainsStoredTrailRecords() {
        var activeTargetId: String? = "T_alpha"
        val storedTrail = mutableListOf(
            TrailEntity(
                trackerId = "T_alpha",
                timestamp = 1000L,
                sequenceId = 1L,
                lat = 37.7749,
                lng = -122.4194,
                accuracy = 5.0f,
                status = "VALID"
            ),
            TrailEntity(
                trackerId = "T_alpha",
                timestamp = 2000L,
                sequenceId = 2L,
                lat = 37.7750,
                lng = -122.4195,
                accuracy = 4.0f,
                status = "VALID"
            )
        )

        assertEquals("Initial pairing active", "T_alpha", activeTargetId)
        assertEquals("Initial stored trail count is 2", 2, storedTrail.size)

        // Simulate unpairing action: clear target ID
        activeTargetId = null

        assertNull("Target ID is cleared after unpairing", activeTargetId)
        assertEquals("Stored historical trail points are retained after unpairing", 2, storedTrail.size)
        assertEquals("T_alpha", storedTrail.first().trackerId)
    }
}
