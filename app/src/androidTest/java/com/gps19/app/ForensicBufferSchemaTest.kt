package com.gps19.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gps19.core.engine.*
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ForensicBufferSchemaTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var spillBuffer: ForensicSpillBuffer

    @Before
    fun init() { hiltRule.inject() }

    @Test
    fun verifySchemaAlignment() {
        val entry = LogEntry(
            localId = "TEST", timestamp = 12345L, message = "Forensic Handover: TEST", type = "FORENSIC_TRACE",
            isImportant = false
        )
        
        while (spillBuffer.hasPending()) {
            spillBuffer.commitDrain(1000)
        }
        
        spillBuffer.writeTrace(entry)
        val peekResult = spillBuffer.peekToEntities(10)
        val traces = peekResult.entities
        
        Timber.i("SCHEMA_TEST: Written message: ${entry.message}")
        if (traces.isNotEmpty()) {
            Timber.i("SCHEMA_TEST: Read message: ${traces.last().message}")
        }
        
        assertTrue("Traces should not be empty", traces.isNotEmpty())
        assertEquals(entry.message, traces.last().message)
    }

    /**
     * verifyOverflowOverwrite: Ensures the buffer overwrites oldest entries when full (R746).
     * Oct.28.11: Updated to use dynamic buffer capacity.
     */
    @Test
    fun verifyOverflowOverwrite() {
        val cap = spillBuffer.capacity
        spillBuffer.resetBufferForTest()
        
        // Fill the buffer to capacity
        repeat(cap) { i ->
            spillBuffer.writeTraceOptimized(
                timestamp = 1000L + i, lat = 1.0, lng = 2.0, accuracy = 3.0, maxAccuracy = 4.0,
                vibe = 0.5, snr = 30.0, batteryLevel = 50, isCharging = true, batteryTemp = 30.0
            )
        }
        
        assertEquals(cap, spillBuffer.getPendingCount())
        assertTrue(spillBuffer.isFull())
        
        // Write one more to trigger overwrite
        val overflowTs = 1000L + cap + 1
        spillBuffer.writeTraceOptimized(
            timestamp = overflowTs, lat = 1.0, lng = 2.0, accuracy = 3.0, maxAccuracy = 4.0,
            vibe = 0.5, snr = 30.0, batteryLevel = 50, isCharging = true, batteryTemp = 30.0
        )
        
        assertEquals(cap, spillBuffer.getPendingCount())
        
        // Peek to verify the oldest (1000L) was dropped and the newest (overflowTs) is present
        val peekResult = spillBuffer.peekToEntities(cap)
        val entities = peekResult.entities
        
        assertEquals(cap, entities.size)
        assertFalse("Oldest entry should be overwritten", entities.any { it.timestamp == 1000L })
        assertTrue("Newest entry should be present", entities.any { it.timestamp == overflowTs })
        assertEquals("Head of buffer should be the second entry", 1001L, entities.first().timestamp)
    }
}
