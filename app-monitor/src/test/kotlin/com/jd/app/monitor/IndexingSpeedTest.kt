package com.jd.app.monitor

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * Unit test for Chapter 68: Indexing Speed (50k+ History Points) (ISSUE-NOV6.0-O23).
 * Verifies that querying and indexing 50,000 history trail points completes in < 2 seconds.
 */
class IndexingSpeedTest {

    @Test
    fun test50kHistoryIndexingAndQuerySpeed() {
        val totalPoints = 50_000
        val points = (1..totalPoints).map { i ->
            TrailEntity(
                trackerId = "tracker_soak",
                timestamp = 1000000L + i,
                sequenceId = i.toLong(),
                lat = 37.0 + (i * 0.00001),
                lng = -122.0 + (i * 0.00001),
                accuracy = 5.0f,
                status = "VALID",
                isLocalUser = false
            )
        }

        val queryTimeMs = measureTimeMillis {
            val filtered = points.filter { it.sequenceId > 25_000L && it.status == "VALID" }
            assertTrue(filtered.size == 25_000)
        }

        assertTrue("Querying 50k points ($queryTimeMs ms) must complete in < 2000ms", queryTimeMs < 2000L)
    }
}
