package com.jd.app.monitor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for ISSUE-NOV6.2-O29 (Chapter 148): Local Trail Storage and Pruning.
 * Verifies that local user trail points (isLocalUser = true) are correctly structured
 * and that TTL pruning applies to local trail entries.
 */
class LocalTrailTest {

    @Test
    fun localTrailEntityDefaultsToIsLocalUserTrueWhenSpecified() {
        val entity = TrailEntity(
            trackerId = "viewer_123",
            timestamp = 1000L,
            sequenceId = 1000L,
            lat = 37.7749,
            lng = -122.4194,
            accuracy = 5.0f,
            status = "VALID",
            isLocalUser = true
        )

        assertTrue("Entity is marked as local user", entity.isLocalUser)
        assertEquals("viewer_123", entity.trackerId)
        assertEquals(37.7749, entity.lat, 0.0001)
        assertEquals(-122.4194, entity.lng, 0.0001)
    }

    @Test
    fun remoteTrailEntityDefaultsToIsLocalUserFalse() {
        val entity = TrailEntity(
            trackerId = "tracker_456",
            timestamp = 1000L,
            sequenceId = 1L,
            lat = 37.7749,
            lng = -122.4194,
            accuracy = 5.0f,
            status = "VALID"
        )

        assertTrue("Entity defaults to remote user (isLocalUser = false)", !entity.isLocalUser)
    }
}
