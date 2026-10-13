package com.gps19.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gps19.core.engine.*
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * OfflineRepositorySmokeTest: Targeted burst test for SIMP-1012-4 batching logic.
 * Ensures that high-frequency status updates are not lost during buffer flushes.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OfflineRepositorySmokeTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject lateinit var offlineRepository: OfflineRepository
    @Inject lateinit var pendingStatusDao: PendingStatusDao
    @Inject lateinit var telemetry: TelemetryRepository

    @Before
    fun init() {
        hiltRule.inject()
        runBlocking {
            offlineRepository.clear()
            // Reset health to normal
            val health = SystemHealthState()
            telemetry.updateHealth(health)
        }
    }

    @Test
    fun verifyHighFrequencyBurstPersistence() = runBlocking {
        val burstCount = 50
        val startTime = System.currentTimeMillis()

        // 1. Submit a burst of updates
        repeat(burstCount) { i ->
            val entity = PendingStatusEntity(
                timestamp = startTime + i,
                lat = 32.0,
                lng = 34.0,
                speed = 1.0,
                accuracy = 5.0,
                bearing = 0.0,
                battery = 80,
                temp = 30.0,
                isCharging = false,
                satsView = 10,
                satsUsed = 8,
                maxAccuracy = 10.0
            )
            offlineRepository.addPendingStatusUpdate(entity)
        }

        // 2. Wait for background flushes to complete. 
        // BATCH_SIZE_THRESHOLD is 5, so we expect ~10 inserts.
        // We add extra time for any race conditions in the async flush.
        delay(2000)

        // 3. Verify total count in database
        val count = pendingStatusDao.getCount()
        assertEquals("Should have persisted all burst updates", burstCount, count)

        // 4. Verify data integrity (no duplicates or gaps)
        val allEntities = pendingStatusDao.getOldestPending(100)
        val timestamps = allEntities.map { it.timestamp }.sorted()
        
        assertEquals("Retrieved count should match burst count", burstCount, timestamps.size)
        for (i in 0 until burstCount) {
            assertEquals("Timestamp mismatch at index $i", startTime + i, timestamps[i])
        }
    }

    @Test
    fun verifyPeriodicFlush() = runBlocking {
        // Submit fewer updates than BATCH_SIZE_THRESHOLD (5)
        val updateCount = 3
        repeat(updateCount) { i ->
            val entity = PendingStatusEntity(
                timestamp = System.currentTimeMillis() + i,
                lat = 32.0, lng = 34.0, speed = 0.0, accuracy = 0.0, bearing = 0.0,
                battery = 0, temp = 0.0, isCharging = false, satsView = 0, satsUsed = 0, maxAccuracy = 0.0
            )
            offlineRepository.addPendingStatusUpdate(entity)
        }

        // Initially they should NOT be in the DB (buffered)
        var count = pendingStatusDao.getCount()
        assertTrue("Updates should be buffered, not yet in DB. Count: $count", count < updateCount)

        // Wait for BATCH_FLUSH_INTERVAL_MS (10s) + buffer
        // For testing purposes, we might want to trigger a manual flush or wait.
        // Since we can't easily change the private constant, we wait or call getPendingStatusUpdates which triggers flush.
        
        offlineRepository.getPendingStatusUpdates(10)
        
        count = pendingStatusDao.getCount()
        assertEquals("Updates should be flushed after getPendingStatusUpdates call", updateCount, count)
    }
}
