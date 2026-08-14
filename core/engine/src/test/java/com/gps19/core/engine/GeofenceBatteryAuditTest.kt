package com.gps19.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.*

/**
 * GeofenceBatteryAuditTest: Verification of R406a Dynamic Polling vs. Geofence Integrity.
 * [Issue #169] Geofence Accuracy vs. Battery Audit.
 */
class GeofenceBatteryAuditTest {

    private val mockTimeProvider = object : TimeProvider {
        private var time = 1700000000000L
        private var rt = 100000L
        
        override fun elapsedRealtime(): Long = rt
        override fun currentTimeMillis(): Long = time
        
        fun advance(ms: Long) {
            time += ms
            rt += ms
        }
    }

    private val spikeLogger: (String, Long) -> Unit = { _, _ -> }

    private fun createDefaultState(): AlarmEvaluationState {
        val now = mockTimeProvider.currentTimeMillis()
        val rt = mockTimeProvider.elapsedRealtime()
        val state = AlarmEvaluationState()
        state.update(
            now = now,
            nowRt = rt,
            serviceStartTime = now - 60000,
            serviceStartRt = rt - 60000,
            lastAlarmAckTs = 0L,
            appStartTime = now - 60000,
            isRelayConnected = true,
            isTrackerConnected = true,
            discoveryPhase = DiscoveryPhase.MONITORING,
            trackerLat = 10.0,
            trackerLng = 10.0,
            trackerGpsAccuracy = 5.0,
            maxTrackerAccuracy = 5.0,
            lastGpsPacketTs = now,
            lastGpsPacketRt = rt,
            trackerLastValidFixTs = now,
            trackerLastValidFixRt = rt,
            trackerSpeed = 0.0,
            jumpTier = 0,
            isAdaptiveJump = false,
            trackerBattery = 100,
            trackerTemp = 30.0,
            wasDistanceViolated = false,
            distanceViolationCounter = 0,
            firstViolationTs = 0L,
            firstViolationRt = 0L,
            firstViolationWasJump = false,
            maxDistance = 100.0,
            distToHomeAuthority = null,
            isGpsGap = false,
            trackerBaroAltEma = 0.0,
            isTrackerMode = true,
            capabilities = HardwareCapabilities(isA15Device = true)
        )
        state.health.apply {
            isHardwareOnline = true
            batteryLevel = 100
            status = SentinelStatus.VALID
            isNear = true
            maxTemp = 30.0
            isLocationPending = false
        }
        state.getOrCreateHomePoint(0).update(10.0, 10.0)
        return state
    }

    @Test
    fun `Audit Geofence Breach under Throttled Polling (45s)`() {
        val state = createDefaultState()
        val report = SystemHealthReport()
        state.trackerSpeed = 0.0
        mockTimeProvider.advance(45000)
        state.now = mockTimeProvider.currentTimeMillis()
        state.nowRt = mockTimeProvider.elapsedRealtime()
        state.trackerLat = 10.0011 // ~120m away
        state.trackerSpeed = 5.0
        state.lastGpsPacketRt = state.nowRt
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report, spikeLogger)
        val geofence = report.reports.find { it.type == ALERT_ID_TRACKER_GEOFENCE }
        assertTrue("Geofence should trigger immediately on throttled fix due to predictive exit", geofence?.conditionMet == true)
    }

    @Test
    fun `Audit Battery Health Correlation during Geofence Breach`() {
        val state = createDefaultState()
        val report = SystemHealthReport()
        state.trackerLat = 10.005 
        state.distanceViolationCounter = DISTANCE_ALARM_SAMPLES_REQUIRED
        state.firstViolationRt = state.nowRt - 10000
        state.health.isBatterySteepDischarge = true
        state.health.cpuLoad = 0.8
        state.trackerBattery = 19
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report, spikeLogger)
        val geofence = report.reports.find { it.type == ALERT_ID_TRACKER_GEOFENCE }
        val battery = report.reports.find { it.type == ALERT_ID_BATTERY_STEEP_DISCHARGE }
        assertTrue("Geofence breach must be active", geofence?.conditionMet == true)
        assertTrue("Steep discharge must be flagged", battery?.conditionMet == true)
    }

    @Test
    fun `Audit Bayesian Drift Persistence during Polling Failure`() {
        val state = createDefaultState()
        val report = SystemHealthReport()
        state.trackerLat = 10.005
        state.wasDistanceViolated = true
        state.distanceViolationCounter = DISTANCE_ALARM_SAMPLES_REQUIRED
        state.trackerLastValidFixRt = state.nowRt
        mockTimeProvider.advance(100000)
        state.now = mockTimeProvider.currentTimeMillis()
        state.nowRt = mockTimeProvider.elapsedRealtime()
        state.health.isLocationPending = true
        state.trackerSpeed = 20.0
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report, spikeLogger)
        assertTrue("Violation must NOT clear during GPS gap uncertainty expansion", state.wasDistanceViolated)
    }

    @Test
    fun `Audit LocationProcessor Status Transitions`() {
        val processor = LocationProcessor(mockTimeProvider)
        processor.loadState(5.0, 0L, -1.0, null, listOf(EngineGeoPoint(10.0, 10.0)), 100.0)
        val nowRt = mockTimeProvider.elapsedRealtime()
        val nowWall = mockTimeProvider.currentTimeMillis()
        
        processor.processGpsPoint(10.0, 10.0, 0.0, 0.0, nowWall, 5.0, 0.0, 40.0, 10, false, 0L, true, nowWall = nowWall, nowRt = nowRt)
        
        processor.updateSensorData(vibration = 2.0, heading = 0.0, baroAlt = 0.0, nowRt = nowRt + 1000, nowWall = nowWall + 1000)
        val res2 = processor.processGpsPoint(10.00005, 10.0, 0.0, 5.0, nowWall + 2000, 5.0, 0.0, 40.0, 10, false, nowWall, true, nowWall = nowWall + 2000, nowRt = nowRt + 2000)
        assertEquals(SentinelStatus.TAMPER, res2.status)
        
        processor.updateSensorData(vibration = 0.05, heading = 0.0, baroAlt = 0.0, nowRt = nowRt + 3000, nowWall = nowWall + 3000)
        val res3 = processor.processGpsPoint(10.0, 10.0, 0.0, 0.0, nowWall + 4000, 5.0, 0.0, 40.0, 10, false, nowWall + 2000, true, nowWall = nowWall + 4000, nowRt = nowRt + 4000)
        assertEquals(SentinelStatus.VALID, res3.status)
    }

    @Test
    fun `Audit Silent Failure Correlation with Thermal Throttling`() {
        val state = createDefaultState()
        val report = SystemHealthReport()
        state.trackerLat = 10.005
        state.distanceViolationCounter = DISTANCE_ALARM_SAMPLES_REQUIRED
        state.health.apply {
            gpsStalled = true
            isThermalThrottling = true
            cpuLoad = 0.9
            ioWait = 0.5
            maxIoLatency = 1000L
        }
        MainAlarmLogic.detectViolations(state, mockTimeProvider, report, spikeLogger)
        val silentFailure = report.reports.find { it.type == ALERT_ID_SILENT_FAILURE }
        assertTrue("Silent Failure should trigger due to thermal-correlated stall", silentFailure?.conditionMet == true)
    }
}
