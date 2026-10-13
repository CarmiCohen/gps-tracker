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
 * Oct.13.5: Hardened for high-frequency concurrent bursts (1,000 updates).
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
            // Reset health to normal to avoid storage pressure gating during test
            val health = SystemHealthState()
            telemetry.updateHealth(health)
        }
    }

    @Test
    fun verifyExtremeHighFrequencyBurstPersistence() = runBlocking {
        val burstCount = 1000
        val startTime = System.currentTimeMillis()

        // 1. Submit a massive burst of updates concurrently (Stress the Actor Mailbox)
        val jobs = List(burstCount) { i ->
            launch(Dispatchers.Default) {
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
        }
        jobs.joinAll()

        // 2. Wait for sequential background flushes to complete (BATCH_SIZE_THRESHOLD is 5)
        var count = 0
        withTimeout(15000) {
            while (count < burstCount) {
                delay(1000)
                count = pendingStatusDao.getCount()
            }
        }

        // 3. Verify total count and data integrity
        assertEquals("Extreme burst persistence failed! Data loss or duplication detected.", burstCount, count)

        val allEntities = pendingStatusDao.getOldestPending(burstCount + 100)
        val timestamps = allEntities.map { it.timestamp }.sorted()
        
        assertEquals("Retrieved list size mismatch", burstCount, timestamps.size)
        for (i in 0 until burstCount) {
            assertEquals("Sequence broken at index $i", startTime + i, timestamps[i])
        }
    }

    @Test
    fun verifyManualFlushDrainage() = runBlocking {
        val updateCount = 3
        repeat(updateCount) { i ->
            val entity = PendingStatusEntity(
                timestamp = System.currentTimeMillis() + i,
                lat = 0.0, lng = 0.0, speed = 0.0, accuracy = 0.0, bearing = 0.0,
                battery = 0, temp = 0.0, isCharging = false, satsView = 0, satsUsed = 0, maxAccuracy = 0.0
            )
            offlineRepository.addPendingStatusUpdate(entity)
        }

        // Stability delay for mailbox move
        delay(1000)

        // Trigger manual flush
        offlineRepository.getPendingStatusUpdates(10)
        
        assertEquals("Buffer not drained on manual flush call", updateCount, pendingStatusDao.getCount())
    }
}
