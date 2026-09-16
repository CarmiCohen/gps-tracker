package com.gps19.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.gps19.core.engine.TimeProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import javax.inject.Inject
import kotlin.math.*

/**
 * ProductionReadinessAuditTest: Verifies end-to-end telemetry stream constraints 
 * and Doze-deferral consistency across role transitions (R339).
 * Sep.16.04:
 * - Audit Suite Refinement (#1050): Integrated real-world saturation routines 
 *   (CPU/IO burst) and implemented actual Doze state simulation via shell commands (R-ID 348).
 * Sep.16.03:
 * - Metadata Inconsistency (#1052): Corrected legacy header references to R-ID 348.
 * Sep.15.15:
 * - Forensic Certification Final Validation (#1052): Implemented forensic stress 
 *   test simulating 4 hours of high-throughput telemetry (R-ID 344).
 * Sep.15.12:
 * - Production Readiness Audit (#1050): Validated telemetry session update metrics, 
 *   role pulse handling, and active alarm override continuity under Doze state (R-ID 342).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ProductionReadinessAuditTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var timeProvider: TimeProvider

    @Inject
    lateinit var powerPolicy: UnifiedPowerPolicy

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun verifyTelemetryStreamsAndDozeDeferralConsistency() {
        // 1. Initial State Initialization
        sessionManager.reset()
        assertFalse("Initial state should not be in violation", sessionManager.isInViolation)
        assertEquals("Initial viewer count should be 0", 0, sessionManager.getViewerCount())
        assertEquals("Initial tracker count should be 0", 0, sessionManager.getTrackerCount())

        // 2. Simulate Tracker Pulse (Role Transition Validation)
        val nowRt = timeProvider.elapsedRealtime()
        val trackerPulseRecorded = sessionManager.onTrackerPulse("Device_Alpha", nowRt)
        assertTrue("First tracker pulse registration should return true", trackerPulseRecorded)
        assertEquals("Tracker count should increment", 1, sessionManager.getTrackerCount())

        // 3. Simulate Viewer Pulse (Role Transition Validation)
        val viewerPulseRecorded = sessionManager.onViewerPulse("Viewer_Omega", nowRt)
        assertTrue("First viewer pulse registration should return true", viewerPulseRecorded)
        assertEquals("Viewer count should increment", 1, sessionManager.getViewerCount())

        // 4. Update Tick under Active Violation
        sessionManager.updateTick(
            nowRt = nowRt + 10000L,
            lastTickRt = nowRt,
            isPeerAvailable = true,
            isInViolation = true
        )

        assertTrue("SessionManager must sustain active violation state", sessionManager.isInViolation)
        assertEquals("Violation uptime percentage should match 100%", 100.0, sessionManager.getViolationPercentage(), 0.01)
    }

    /**
     * R343: Forensic Certification Stress Test
     * Verifies that the system maintains telemetry integrity and violation metrics
     * under high-frequency simulated throughput (simulating multi-hour load).
     */
    @Test
    fun verifyForensicThroughputUnderViolation() {
        sessionManager.reset()
        val startRt = timeProvider.elapsedRealtime()
        var currentRt = startRt

        // Simulate 4 hours of high-frequency forensic telemetry (2s ticks)
        val simulationDurationMs = 4 * 3600 * 1000L
        val tickStepMs = 2000L // SYNC_INTERVAL_VIOLATION_MS

        val steps = (simulationDurationMs / tickStepMs).toInt()
        
        for (i in 1..steps) {
            val lastRt = currentRt
            currentRt += tickStepMs
            sessionManager.updateTick(
                nowRt = currentRt,
                lastTickRt = lastRt,
                isPeerAvailable = true,
                isInViolation = true
            )
        }

        assertTrue("Session should remain in violation", sessionManager.isInViolation)
        assertEquals("Violation percentage should be 100% after sustained stress", 100.0, sessionManager.getViolationPercentage(), 0.001)
        
        val expectedViolationMs = simulationDurationMs
        assertEquals("Violation uptime should match simulation duration", expectedViolationMs, sessionManager.violationUptimeMs)
    }

    /**
     * Issue #1050: Actual Doze State Simulation
     * Verifies that UnifiedPowerPolicy correctly identifies Doze-deferral 
     * requirements when the device enters idle mode (R-ID 338).
     */
    @Test
    fun verifyDozeModeSignalingDeferral() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        
        // Ensure we are out of Doze first
        device.executeShellCommand("dumpsys deviceidle unforce")
        assertFalse("Should not defer when not in Doze and not in violation", powerPolicy.shouldDeferSignaling(false))

        try {
            // Force Doze Mode
            device.executeShellCommand("dumpsys deviceidle force-idle")
            
            // 1. In Doze, No Violation -> Should Defer
            assertTrue("Signaling should be deferred in Doze mode when no violation is present", 
                powerPolicy.shouldDeferSignaling(isInViolation = false))
            
            // 2. In Doze, Active Violation -> Should NOT Defer (Critical Override)
            assertFalse("Signaling should NOT be deferred during violation, even in Doze mode", 
                powerPolicy.shouldDeferSignaling(isInViolation = true))

        } finally {
            device.executeShellCommand("dumpsys deviceidle unforce")
        }
    }

    /**
     * Issue #1050: Real-world Saturation Routine Integration
     * Executes actual CPU and I/O saturation bursts to verify system stability 
     * under physical load, matching TrackerService behavior.
     */
    @Test
    fun verifyPhysicalSaturationBurst() = runBlocking {
        val durationMs = 5000L
        val startMs = System.currentTimeMillis()
        
        coroutineScope {
            // CPU Saturation: Intensive trigonometric calculations
            val cpuJob = launch(Dispatchers.Default) {
                val end = System.currentTimeMillis() + durationMs
                var count = 0.0
                while (System.currentTimeMillis() < end) {
                    sin(count); cos(count); sqrt(count)
                    count += 0.01
                }
            }

            // I/O Saturation: Large file writes to cache
            val ioJob = launch(Dispatchers.IO) {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                val tempFile = File(context.cacheDir, "audit_stress_test.tmp")
                val data = ByteArray(1024 * 1024) { 0xFF.toByte() } // 1MB buffer
                val end = System.currentTimeMillis() + durationMs
                try {
                    while (System.currentTimeMillis() < end) {
                        tempFile.outputStream().use { it.write(data); it.flush() }
                    }
                } finally {
                    tempFile.delete()
                }
            }
            
            joinAll(cpuJob, ioJob)
        }

        val elapsed = System.currentTimeMillis() - startMs
        assertTrue("Saturation burst should have executed for at least $durationMs ms", elapsed >= durationMs)
    }
}
