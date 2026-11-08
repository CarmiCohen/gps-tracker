package com.jd.app.monitor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for ISSUE-NOV6.2-O29 / ISSUE-NOV6.0-O23 (Chapter 148): Local Trail Storage and Pruning.
 * Verifies local user trail points (isLocalUser = true), dual-trail separation,
 * and 48-hour TTL pruning thresholds.
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

        assertFalse("Entity defaults to remote user (isLocalUser = false)", entity.isLocalUser)
    }

    @Test
    fun dualTrailClassificationSeparatesLocalAndViewerTrails() {
        val entities = listOf(
            TrailEntity("tracker_1", 1000L, 1L, 37.77, -122.41, 5f, "VALID", isLocalUser = false),
            TrailEntity("tracker_1", 2000L, 2L, 37.78, -122.42, 5f, "VALID", isLocalUser = false),
            TrailEntity("viewer_self", 1500L, 1500L, 37.775, -122.415, 3f, "VALID", isLocalUser = true)
        )

        val remoteTrail = entities.filter { !it.isLocalUser }
        val localTrail = entities.filter { it.isLocalUser }

        assertEquals(2, remoteTrail.size)
        assertEquals(1, localTrail.size)
        assertEquals("viewer_self", localTrail.first().trackerId)
    }

    @Test
    fun localTrailTtlThresholdCalculatesExactFortyEightHoursOffset() {
        val now = 1_700_000_000_000L
        val fortyEightHoursMs = 48 * 60 * 60 * 1000L // 172,800,000 ms
        val threshold = now - fortyEightHoursMs

        val oldLocalEntity = TrailEntity("viewer_self", now - 200_000_000L, 1L, 37.77, -122.41, 5f, "VALID", isLocalUser = true)
        val freshLocalEntity = TrailEntity("viewer_self", now - 50_000_000L, 2L, 37.78, -122.42, 5f, "VALID", isLocalUser = true)
        val oldRemoteEntity = TrailEntity("tracker_1", now - 200_000_000L, 3L, 37.79, -122.43, 5f, "VALID", isLocalUser = false)

        val entities = listOf(oldLocalEntity, freshLocalEntity, oldRemoteEntity)

        // Simulate TrailDao.pruneLocalTrail(threshold) logic: DELETE FROM trails WHERE isLocalUser = 1 AND timestamp < threshold
        val remainingEntities = entities.filterNot { it.isLocalUser && it.timestamp < threshold }

        assertEquals(2, remainingEntities.size)
        assertFalse("Old local entity pruned", remainingEntities.contains(oldLocalEntity))
        assertTrue("Fresh local entity retained", remainingEntities.contains(freshLocalEntity))
        assertTrue("Old remote entity retained (isLocalUser = false)", remainingEntities.contains(oldRemoteEntity))
    }
}
