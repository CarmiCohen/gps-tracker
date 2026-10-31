package com.jd.app.sensor

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jd.core.shared.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * GeofenceForensicTest: Audits Geofence transition recording for nanosecond precision and zero loss.
 * Part of ISSUE-REVOL30.1-F3.
 */
@RunWith(AndroidJUnit4::class)
class GeofenceForensicTest {

    private lateinit var forensicBuffer: ForensicBuffer
    private val testGeofence = SentinelLogic.Geofence(
        id = "TEST_ZONE",
        lat = 37.7749,
        lng = -122.4194,
        radiusMeters = 100.0
    )

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testDir = File(context.filesDir, "test_forensic")
        testDir.deleteRecursively()
        forensicBuffer = ForensicBuffer(testDir, capacity = 1000)
        forensicBuffer.initialize()
    }

    @Test
    fun testRapidGeofenceTransitions_ZeroLoss() {
        var currentState = SentinelLogic.GeofenceState.INSIDE
        val transitionsFound = mutableListOf<SentinelLogic.GeofenceTransition>()
        
        // 1. Simulate 50 rapid transitions (Exit/Enter cycles)
        for (i in 0 until 100) {
            val isInside = i % 2 == 0
            val update = LocationUpdate().apply {
                // Alternating between center and 200m away
                lat = if (isInside) 37.7749 else 37.7769 
                lng = -122.4194
                timestamp = System.currentTimeMillis()
                monotonicNanos = System.nanoTime()
                sequenceId = i.toLong()
            }

            val result = SentinelLogic.evaluateGeofenceTransition(update, testGeofence, currentState)
            
            if (result.transition != SentinelLogic.GeofenceTransition.NONE) {
                transitionsFound.add(result.transition)
                // In a real scenario, the service would write a ViolationEvent or a tagged trace.
                // Here we verify the trace itself is stored with the transition status.
                update.status = "GEOFENCE_${result.transition}"
                forensicBuffer.writeTrace(update, isCritical = true)
            }
            
            currentState = result.currentState
        }

        // 2. Verify all 99 transitions (from i=1 to 99) were detected and stored
        val storedTraces = forensicBuffer.readTraces()
        val transitionTraces = storedTraces.filter { it.status.startsWith("GEOFENCE_") }
        
        assertEquals("Should detect exactly 99 transitions", 99, transitionTraces.size)
        
        // 3. Verify Precision: Monotonic Nanos must be increasing
        for (i in 1 until transitionTraces.size) {
            assertTrue(
                "Nanosecond precision check failed at index $i",
                transitionTraces[i].monotonicNanos > transitionTraces[i-1].monotonicNanos
            )
        }
    }
}
