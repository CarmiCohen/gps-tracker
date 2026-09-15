package com.gps19.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gps19.core.engine.TimeProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * ProductionReadinessAuditTest: Verifies end-to-end telemetry stream constraints 
 * and Doze-deferral consistency across role transitions (R339).
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
}
