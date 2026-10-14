package com.gps19.app

import com.gps19.core.engine.*
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Answers
import org.mockito.kotlin.*

/**
 * DashboardContinuityTest: Verifies that tracker mode telemetry persists locally without peers.
 * Targets Chapter 3.7 of TEST_PROCEDURE.md (Issue #1414).
 */
class DashboardContinuityTest {

    private val timeProvider = object : TimeProvider {
        override val elapsedRealtime: Long get() = 1000000L
        override val currentTimeMillis: Long get() = 1700000000000L
    }

    private fun createViewModel(): MainViewModel {
        return mock<MainViewModel>(defaultAnswer = Answers.CALLS_REAL_METHODS) {
            on { timeProvider } doReturn timeProvider
        }
    }

    @Test
    fun `Verify Tracker Dashboard Continuity - Displays local state when viewer is disconnected`() {
        val viewModel = createViewModel()
        val nowRt = timeProvider.elapsedRealtime
        
        // 1. Setup KinematicState with local movement but NO remote peer
        val kin = KinematicState().apply {
            localLocation.kinetic.apply {
                lat = 40.0
                lng = -74.0
                speed = 5.5 // 19.8 km/h
                rt = nowRt
                gpsTs = timeProvider.currentTimeMillis
            }
            // trackerLocation remains empty/default (no peer)
        }

        // 2. Setup DiagnosticState showing relay is connected but NO viewer is linked
        val diag = DiagnosticState().apply {
            connectivity.isRelayConnected = true
            connectivity.lastRemoteActivityTs = 0L // No peer activity
            pulse = nowRt
        }

        // 3. Act: Map to DashboardTelemetryState in "tracker" mode
        val result = viewModel.mapDashboardTelemetry("tracker", kin, nowRt, TrackerState.MOVING, false)

        // 4. Verify: Should use localLocation data
        assertTrue("Dashboard must be fresh using local GPS", result.isGpsFresh)
        assertEquals(40.0, result.lat, 0.0001)
        assertEquals(5.5, result.gpsSpeedMps, 0.0001)
        assertEquals(TrackerState.MOVING, result.trackerState)
        
        // 5. Verify connectivity mapping for tracker
        val connResult = viewModel.mapDashboardConnectivity("tracker", diag, nowRt)
        assertTrue("Local watchdog should be OK", connResult.watchdogOk)
        assertTrue("Relay should show connected", connResult.isRelayConnected)
    }
}
