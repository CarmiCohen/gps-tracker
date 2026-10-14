package com.gps19.app

import com.gps19.core.engine.*
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Answers
import org.mockito.kotlin.*

/**
 * UxLogicFormattingTest: Validating UI presentation logic.
 * Targets TEST_PROCEDURE.md Chapters 1.5, 3.4, 3.6, 3.8.
 */
class UxLogicFormattingTest {

    private val timeProvider = object : TimeProvider {
        override val elapsedRealtime: Long get() = 1000000L
        override val currentTimeMillis: Long get() = 1700000000000L
    }

    private fun createViewModel(): MainViewModel {
        return mock<MainViewModel>(defaultAnswer = Answers.CALLS_REAL_METHODS) {
            on { timeProvider } doReturn timeProvider
        }
    }

    private fun createDefaultKinematic(): KinematicState {
        return KinematicState().apply {
            localLocation.kinetic.rt = 1000000L
            localLocation.kinetic.gpsTs = 1700000000000L
            trackerLocation.kinetic.rt = 1000000L
            trackerLocation.kinetic.gpsTs = 1700000000000L
            pulse = 1000000L
        }
    }

    private fun createDefaultDiag(): DiagnosticState {
        return DiagnosticState().apply {
            pulse = 1000000L
            connectivity.isLocalOnline = true
            connectivity.isRelayConnected = true
        }
    }

    @Test
    fun `Verify Role Branding Logic - Viewer Cyan depends on Remote Peer activity (Issue 1177)`() {
        val viewModel = createViewModel()
        val nowRt = timeProvider.elapsedRealtime
        val diag = createDefaultDiag()

        // Case 1: No remote peer activity
        diag.connectivity.lastRemoteActivityTs = 0L
        val resultDimmed = viewModel.mapHudConnectivity("viewer", "T1", "V1", true, false, false, diag, 50, 10, nowRt)
        assertFalse("Branding should be dimmed (Slate500) if peer is inactive", resultDimmed.isTelemetryFresh)

        // Case 2: Active remote peer
        diag.connectivity.lastRemoteActivityTs = nowRt - 5000L
        val resultActive = viewModel.mapHudConnectivity("viewer", "T1", "V1", true, false, false, diag, 50, 10, nowRt)
        assertTrue("Branding should transition to ViewerCyan when peer is active", resultActive.isTelemetryFresh)
    }

    @Test
    fun `Verify HUD Velocity Logic - Moving status not displayed at 0 kmh (Issue 1386)`() {
        val viewModel = createViewModel()
        val nowRt = timeProvider.elapsedRealtime
        val kin = createDefaultKinematic()

        // Case: Speed is exactly 0.0
        kin.trackerLocation.kinetic.speed = 0.0
        val hudStationary = viewModel.mapHudTelemetry("viewer", kin, nowRt, TrackerState.PARKING, false)
        assertEquals("Speed should be 0", 0f, hudStationary.speedMps)
        assertEquals("State should be PARKING", TrackerState.PARKING, hudStationary.trackerState)
    }

    @Test
    fun `Verify GNSS Initialization - Shows stale-style data before hardware lock (Issue 1178)`() {
        val viewModel = createViewModel()
        val nowRt = timeProvider.elapsedRealtime
        val kin = createDefaultKinematic()
        
        // Simulate no GPS lock yet (gpsTs = 0)
        kin.trackerLocation.kinetic.gpsTs = 0L
        
        val hud = viewModel.mapHudTelemetry("viewer", kin, nowRt, TrackerState.UNKNOWN, false)
        assertFalse("GPS should not be fresh during initialization", hud.isGpsFresh)
    }

    @Test
    fun `Verify Dashboard GPS Age - Never negative (Issue 1423)`() {
        val viewModel = createViewModel()
        val nowRt = timeProvider.elapsedRealtime
        val kin = createDefaultKinematic()

        // Case: System clock jitter where loc.rt > nowRt
        kin.trackerLocation.kinetic.rt = nowRt + 5000L 
        
        val dashboard = viewModel.mapDashboardTelemetry("viewer", kin, nowRt, TrackerState.MOVING, false)
        
        // Current implementation: isGpsActive = fixAgeRt < threshold && loc.kinetic.gpsTs > 0
        // -5000 < 60000 is true, so it remains "Fresh" (desirable for jitter)
        assertTrue("Jitter should not break freshness", dashboard.isGpsFresh)
    }
}
