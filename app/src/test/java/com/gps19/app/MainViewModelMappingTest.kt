package com.gps19.app

import com.gps19.core.engine.*
import org.junit.Assert.*
import org.junit.Test

/**
 * MainViewModelMappingTest: Verifies centralized mapping logic and tier-specific freshness gates.
 * Oct.20.22: Updated engine version expectation to Oct20.22.
 * Oct.20.21: Updated engine version expectation to Oct20.21.
 * Oct.18.18: Updated engine version expectation to Oct18.18.
 * Oct.18.16: Updated engine version expectation to Oct18.16.
 * Oct.17.11: Updated engine version expectation to Oct17.11.
 * Oct.16.2: Updated engine version expectation to Oct16.2.
 * Oct.15.10: Updated to use decoupled Mapper objects (DashboardMapper, HudMapper, MapStateMapper).
 */
class MainViewModelMappingTest {

    private val timeProvider = object : TimeProvider {
        override val elapsedRealtime: Long get() = 1000000L
        override val currentTimeMillis: Long get() = 1700000000000L
    }

    private fun createKinematicState(
        rt: Long = 1000000L,
        gpsTs: Long = 1700000000000L,
        isUltra: Boolean = false,
        isPowerSave: Boolean = false,
        speed: Double = 1.0 // Default to moving to use alpha 0.3
    ): KinematicState {
        return KinematicState().apply {
            localLocation.kinetic.rt = rt
            localLocation.kinetic.gpsTs = gpsTs
            localLocation.kinetic.lat = 32.0
            localLocation.kinetic.lng = 34.0
            localLocation.kinetic.speed = speed
            localHealth.isUltraLongStationary = isUltra
            localHealth.isPowerSaveMode = isPowerSave
            
            trackerLocation.kinetic.rt = rt
            trackerLocation.kinetic.gpsTs = gpsTs
            trackerLocation.kinetic.lat = 32.0
            trackerLocation.kinetic.lng = 34.0
            trackerLocation.kinetic.speed = speed
            trackerHealth.isUltraLongStationary = isUltra
            trackerHealth.isPowerSaveMode = isPowerSave
            
            pulse = rt
        }
    }

    @Test
    fun `Verify Dashboard Telemetry Freshness - Standard Tier`() {
        val nowRt = timeProvider.elapsedRealtime
        
        // 50s age: Should be fresh (Limit 60s)
        val kinFresh = createKinematicState(rt = nowRt - 50000L)
        val resultFresh = DashboardMapper.mapDashboardTelemetry("tracker", kinFresh, nowRt, TrackerState.MOVING, false)
        assertTrue("Standard telemetry should be fresh at 50s", resultFresh.isGpsFresh)

        // 70s age: Should be stale
        val kinStale = createKinematicState(rt = nowRt - 70000L)
        val resultStale = DashboardMapper.mapDashboardTelemetry("tracker", kinStale, nowRt, TrackerState.MOVING, false)
        assertFalse("Standard telemetry should be stale at 70s", resultStale.isGpsFresh)
    }

    @Test
    fun `Verify Dashboard Telemetry Freshness - Power Save Tier`() {
        val nowRt = timeProvider.elapsedRealtime
        
        // 65s age: Should be fresh in Power Save (Threshold ~70s)
        val kinFresh = createKinematicState(rt = nowRt - 65000L, isPowerSave = true)
        val resultFresh = DashboardMapper.mapDashboardTelemetry("tracker", kinFresh, nowRt, TrackerState.MOVING, false)
        assertTrue("Power Save telemetry should be fresh at 65s", resultFresh.isGpsFresh)

        // 75s age: Should be stale
        val kinStale = createKinematicState(rt = nowRt - 75000L, isPowerSave = true)
        val resultStale = DashboardMapper.mapDashboardTelemetry("tracker", kinStale, nowRt, TrackerState.MOVING, false)
        assertFalse("Power Save telemetry should be stale at 75s", resultStale.isGpsFresh)
    }

    @Test
    fun `Verify Dashboard Telemetry Freshness - Parking Tier`() {
        val nowRt = timeProvider.elapsedRealtime
        
        // 70s age: Should be fresh in Parking (Threshold ~75s)
        val kinFresh = createKinematicState(rt = nowRt - 70000L)
        val resultFresh = DashboardMapper.mapDashboardTelemetry("tracker", kinFresh, nowRt, TrackerState.PARKING, false)
        assertTrue("Parking telemetry should be fresh at 70s", resultFresh.isGpsFresh)

        // 80s age: Should be stale
        val kinStale = createKinematicState(rt = nowRt - 80000L)
        val resultStale = DashboardMapper.mapDashboardTelemetry("tracker", kinStale, nowRt, TrackerState.MOVING, false)
        assertFalse("Parking telemetry should be stale at 80s", resultStale.isGpsFresh)
    }

    @Test
    fun `Verify Dashboard Telemetry Freshness - Ultra Tier`() {
        val nowRt = timeProvider.elapsedRealtime
        
        // 300s age: Should be fresh in Ultra (Threshold ~330s)
        val kinFresh = createKinematicState(rt = nowRt - 300000L, isUltra = true)
        val resultFresh = DashboardMapper.mapDashboardTelemetry("tracker", kinFresh, nowRt, TrackerState.UNKNOWN, true)
        assertTrue("Ultra telemetry should be fresh at 300s", resultFresh.isGpsFresh)

        // 340s age: Should be stale
        val kinStale = createKinematicState(rt = nowRt - 340000L, isUltra = true)
        val resultStale = DashboardMapper.mapDashboardTelemetry("tracker", kinStale, nowRt, TrackerState.UNKNOWN, true)
        assertFalse("Ultra telemetry should be stale at 340s", resultStale.isGpsFresh)
    }

    @Test
    fun `Verify HUD Connectivity Data Health - Tracker vs Viewer`() {
        val nowRt = timeProvider.elapsedRealtime
        val diag = DiagnosticState().apply {
            connectivity.isLocalOnline = true
            connectivity.isRelayConnected = true
            connectivity.lastRemoteActivityTs = nowRt - 70000L // Stale telemetry (>60s)
            pulse = nowRt
        }

        // Tracker Mode: Should be healthy (Connectivity only)
        val trkResult = HudMapper.mapHudConnectivity("tracker", "T1", "V1", true, false, false, diag, 50, 10, nowRt)
        assertTrue("Tracker data health should ignore telemetry freshness", trkResult.isDataHealthy)

        // Viewer Mode: Should be unhealthy (Telemetry is stale)
        val vwrResult = HudMapper.mapHudConnectivity("viewer", "T1", "V1", true, false, false, diag, 50, 10, nowRt)
        assertFalse("Viewer data health should require fresh telemetry", vwrResult.isDataHealthy)
    }

    @Test
    fun `Verify Map View State Smoothing Integration`() {
        val nowRt = timeProvider.elapsedRealtime
        val nowMs = timeProvider.currentTimeMillis
        // Ensure speed > 0.5 to use alpha 0.3
        val kin = createKinematicState(rt = nowRt, speed = 1.0)
        val spatial = SpatialUiState()
        
        // Initial mapping (Null Island check)
        val (result1, smoothed1) = MapStateMapper.mapMapViewState("tracker", 4, spatial, kin, nowRt, nowMs, emptyList(), emptyList(), emptyList(), MapStateMapper.SmoothedCoordinates(0.0, 0.0, 0.0, 0.0))
        assertEquals(32.0, result1.smoothedTrackerLat, 0.0001)
        assertEquals(34.0, result1.smoothedTrackerLng, 0.0001)
        
        // Move location slightly: Should smooth
        kin.localLocation.kinetic.lat = 32.0001
        kin.localLocation.kinetic.lng = 34.0001
        val (result2, _) = MapStateMapper.mapMapViewState("tracker", 4, spatial, kin, nowRt + 2000L, nowMs + 2000L, emptyList(), emptyList(), emptyList(), smoothed1)
        
        // sTrkLat = smooth(32.0, 32.0001, alpha=0.3) = 32.0 * 0.7 + 32.0001 * 0.3 = 32.00003
        assertEquals(32.00003, result2.smoothedTrackerLat, 0.00001)
    }

    @Test
    fun `Verify Viewer Smoothing Fix`() {
        val nowRt = timeProvider.elapsedRealtime
        val nowMs = timeProvider.currentTimeMillis
        // Ensure speed > 0.5 to use alpha 0.3
        val kin = createKinematicState(rt = nowRt, speed = 1.0)
        // Set viewer location (localLocation in viewer mode)
        kin.localLocation.kinetic.lat = 35.0
        kin.localLocation.kinetic.lng = 36.0
        
        val (result1, smoothed1) = MapStateMapper.mapMapViewState("viewer", 4, SpatialUiState(), kin, nowRt, nowMs, emptyList(), emptyList(), emptyList(), MapStateMapper.SmoothedCoordinates(0.0, 0.0, 0.0, 0.0))
        assertEquals(35.0, result1.smoothedViewerLat, 0.0001)
        
        kin.localLocation.kinetic.lat = 35.0001
        val (result2, _) = MapStateMapper.mapMapViewState("viewer", 4, SpatialUiState(), kin, nowRt + 2000L, nowMs + 2000L, emptyList(), emptyList(), emptyList(), smoothed1)
        
        // sVwrLat = smooth(35.0, 35.0001, alpha=0.3) = 35.00003
        assertEquals(35.00003, result2.smoothedViewerLat, 0.00001)
    }

    @Test
    fun `Verify HUD Telemetry Freshness - Across Tiers`() {
        val nowRt = timeProvider.elapsedRealtime
        
        // Parking Tier: 75s threshold
        val kinParking = createKinematicState(rt = nowRt - 70000L)
        val hudParking = HudMapper.mapHudTelemetry("tracker", kinParking, nowRt, TrackerState.PARKING, false)
        assertTrue("HUD Parking should be fresh at 70s", hudParking.isGpsFresh)
        
        // Stale Parking
        val kinStale = createKinematicState(rt = nowRt - 80000L)
        val hudStale = HudMapper.mapHudTelemetry("tracker", kinStale, nowRt, TrackerState.PARKING, false)
        assertFalse("HUD Parking should be stale at 80s", hudStale.isGpsFresh)
    }

    @Test
    fun `Verify Map Freshness - Power Save Tier`() {
        val nowRt = timeProvider.elapsedRealtime
        val nowMs = timeProvider.currentTimeMillis
        val kin = createKinematicState(rt = nowRt - 65000L, isPowerSave = true)
        
        val (mapState, _) = MapStateMapper.mapMapViewState("tracker", 4, SpatialUiState(), kin, nowRt, nowMs, emptyList(), emptyList(), emptyList(), MapStateMapper.SmoothedCoordinates(0.0, 0.0, 0.0, 0.0))
        assertTrue("Map should be fresh at 65s in Power Save", mapState.isTrackerFresh)
        
        val kinStale = createKinematicState(rt = nowRt - 80000L, isPowerSave = true)
        val (mapStateStale, _) = MapStateMapper.mapMapViewState("tracker", 4, SpatialUiState(), kinStale, nowRt, nowMs, emptyList(), emptyList(), emptyList(), MapStateMapper.SmoothedCoordinates(0.0, 0.0, 0.0, 0.0))
        assertFalse("Map should be stale at 80s in Power Save", mapStateStale.isTrackerFresh)
    }

    @Test
    fun `Verify Dashboard Connectivity Watchdog`() {
        val nowRt = timeProvider.elapsedRealtime
        val diag = DiagnosticState().apply {
            pulse = nowRt - 5000L // Fresh
            connectivity.lastRemoteActivityTs = nowRt - 10000L
        }
        
        val result = DashboardMapper.mapDashboardConnectivity("viewer", diag, nowRt)
        assertTrue("Watchdog should be OK", result.watchdogOk)
        assertEquals("Oct20.22", result.engineVersion)
        
        // Stale watchdog
        diag.pulse = nowRt - 70000L
        val resultStale = DashboardMapper.mapDashboardConnectivity("viewer", diag, nowRt)
        assertFalse("Watchdog should be failed", resultStale.watchdogOk)
    }

    @Test
    fun `Verify HUD Health - Siren and Alarm State`() {
        val nowRt = timeProvider.elapsedRealtime
        val diag = DiagnosticState().apply {
            isSirenPlaying = true
            activeAlarms = listOf(AlarmInfo(title = "Tamper", subtitle = "Detected", type = "TAMPER", isResolved = false))
            isRedScreenVisible = true
        }
        
        val health = HudMapper.mapHudHealth(diag, nowRt, false)
        assertTrue("HUD should report siren playing", health.isSirenPlaying)
        assertTrue("HUD should report active alarms", health.hasActiveAlarms)
        assertTrue("HUD should report red screen visible", health.isRedScreenVisible)
    }

    @Test
    fun `Verify Dashboard Health Mapping`() {
        val nowRt = timeProvider.elapsedRealtime
        val kin = createKinematicState()
        val diag = DiagnosticState().apply {
            battery.level = 85
            trackerBattery.temp = 35.0
        }
        
        val health = DashboardMapper.mapDashboardHealth("tracker", kin, diag, 40.0, 45.0, nowRt)
        assertEquals(85, health.batteryLevel)
        assertEquals(35.0, health.trackerTemp, 0.1)
        assertEquals(45.0, health.trackerMaxTemp, 0.1)
    }
}
