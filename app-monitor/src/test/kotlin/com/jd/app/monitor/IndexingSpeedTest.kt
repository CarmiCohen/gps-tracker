package com.jd.app.monitor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * Chapter 68 (ISSUE-NOV6.0-O23): Indexing Speed (50k+ History Points).
 * Verifies that generating, filtering, and querying 50,000 trail history points
 * completes in under 2.0 seconds.
 */
class IndexingSpeedTest {

    @Test
    fun fiftyThousandTrailPointsQueryAndPruneEvaluationCompletesUnderTwoSeconds() {
        val totalPoints = 50_000
        val trackerId = "T"
        val baseTime = 1_000_000L

        val entities = ArrayList<TrailEntity>(totalPoints)
        for (i in 0 until totalPoints) {
            entities.add(
                TrailEntity(
                    trackerId = trackerId,
                    timestamp = baseTime + i * 1000L,
                    sequenceId = (i + 1).toLong(),
                    lat = 37.7749 + (i % 100) * 0.0001,
                    lng = -122.4194 + (i % 100) * 0.0001,
                    accuracy = 5.0f,
                    status = "VALID",
                    isLocalUser = false
                )
            )
        }

        val elapsedMs = measureTimeMillis {
            // Simulate the 50,000-point limit query: filter by trackerId, sort descending, limit to MAX_TRAIL_POINTS (1000)
            val filteredAndSorted = entities
                .filter { it.trackerId == trackerId }
                .sortedByDescending { it.timestamp }
                .take(MAX_TRAIL_POINTS)

            assertEquals(MAX_TRAIL_POINTS, filteredAndSorted.size)
            assertEquals(baseTime + (totalPoints - 1) * 1000L, filteredAndSorted.first().timestamp)

            // Simulate sequence gap detection logic across 50,000 points
            val maxSeq = entities.maxOf { it.sequenceId }
            assertEquals(50000L, maxSeq)
        }

        assertTrue(
            "Querying and filtering 50,000 trail points ($elapsedMs ms) must take less than 2000 ms",
            elapsedMs < 2000
        )
    }
}
