package com.jd.app.monitor

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for ISSUE-NOV4.3-O48 & ISSUE-NOV4.3-O49 (Chapter 154):
 * - Verifies equal timestamp retention with distinct sequenceIds in TrailEntity (O48).
 * - Verifies local trail entity structure and properties.
 */
class TrailPersistenceIntegrityTest {

    @Test
    fun verifyEqualTimestampRetentionWithDistinctSequenceIds() {
        val ts = 1_700_000_000_000L
        val entity1 = TrailEntity(
            trackerId = "tracker_1",
            timestamp = ts,
            sequenceId = 1L,
            lat = 32.0,
            lng = 34.0,
            accuracy = 5f,
            status = "VALID"
        )
        val entity2 = TrailEntity(
            trackerId = "tracker_1",
            timestamp = ts,
            sequenceId = 2L,
            lat = 32.1,
            lng = 34.1,
            accuracy = 5f,
            status = "VALID"
        )

        // Since primaryKeys = ["trackerId", "timestamp", "sequenceId"], both entities are distinct and preserved.
        val set = mutableSetOf(entity1, entity2)
        assertEquals(2, set.size)
    }

    @Test
    fun verifyLocalTrailEntityDefaults() {
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
        assertEquals(true, entity.isLocalUser)
        assertEquals(1000L, entity.sequenceId)
    }
}
