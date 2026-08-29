package com.gps19.app

import com.gps19.core.engine.*
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ServiceBehaviorAuditTest: Audit of R406a Dynamic Polling Intervals.
 * Aug.29.09:
 * - Concern #764 Simplification: Refactored tests to use HardwareCapabilities 
 *   instead of redundant DeviceSpecialFlags.
 * Aug.17.08:
 * - Issue #191 Validation: Added Audit of Cooling Mode throttle to ensure 
 *   thermal safety floor (30s GPS / 500ms Forensic) overrides all other 
 *   behavioral states (R191).
 */
class ServiceBehaviorAuditTest {

    private val mockTimeProvider = object : TimeProvider {
        override fun elapsedRealtime(): Long = 100000L
        override fun currentTimeMillis(): Long = 1700000000000L
    }

    @Test
    fun `Audit Polling Interval - Cooling Mode Override`() {
        val behavior = ServiceBehaviorUseCase(mockTimeProvider)
        val stdCaps = HardwareCapabilities(requiresAdaptationMuzzle = false, requiresExtraTopPadding = false)
        
        // SCENARIO: Moving, Geofence Active, Screen On (Normally 5s or 2s)
        // BUT: Cooling Mode is ACTIVE.
        val interval = behavior.calculateGpsInterval(
            isCoolingMode = true, 
            isSuspiciousMode = true, // Even if suspicious
            isStationary = false, 
            isScreenOn = true,
            isGeofenceActive = true, 
            nowRt = 100000L, 
            capabilities = stdCaps
        )
        
        // VERIFICATION: Thermal Safety Floor (30s) MUST be maintained.
        assertEquals(COOLING_GPS_POLLING_MS, interval)
        assertEquals(30000L, interval)
    }

    @Test
    fun `Audit Polling Interval - Moving with Screen Off and Geofence`() {
        val behavior = ServiceBehaviorUseCase(mockTimeProvider)
        
        // --- SCENARIO 1: A15/S21FE Hardware (requiresAdaptationMuzzle = true) ---
        val a15Caps = HardwareCapabilities(requiresAdaptationMuzzle = true, requiresExtraTopPadding = false)
        
        // 1a. Moving, Screen OFF, Geofence INACTIVE -> 45s (SCREEN_OFF_GPS_POLLING_MS)
        val intervalOffNoGeoA15 = behavior.calculateGpsInterval(
            isCoolingMode = false, isSuspiciousMode = false, isStationary = false, isScreenOn = false,
            isGeofenceActive = false, nowRt = 100000L, capabilities = a15Caps
        )
        assertEquals(45000L, intervalOffNoGeoA15)

        // 1b. Moving, Screen OFF, Geofence ACTIVE -> 2s (HIGH_FREQUENCY_GPS_POLLING_MS)
        val intervalOffWithGeoA15 = behavior.calculateGpsInterval(
            isCoolingMode = false, isSuspiciousMode = false, isStationary = false, isScreenOn = false,
            isGeofenceActive = true, nowRt = 100000L, capabilities = a15Caps
        )
        assertEquals(2000L, intervalOffWithGeoA15)

        // --- SCENARIO 2: Standard Hardware ---
        val stdCaps = HardwareCapabilities(requiresAdaptationMuzzle = false, requiresExtraTopPadding = false)

        // 2a. Moving, Screen OFF, Geofence ACTIVE -> 5s (MOVING_GPS_POLLING_MS)
        val intervalOffWithGeoStd = behavior.calculateGpsInterval(
            isCoolingMode = false, isSuspiciousMode = false, isStationary = false, isScreenOn = false,
            isGeofenceActive = true, nowRt = 100000L, capabilities = stdCaps
        )
        assertEquals(5000L, intervalOffWithGeoStd)
    }

    @Test
    fun `Audit Power Save Mode Transitions`() {
        val behavior = ServiceBehaviorUseCase(mockTimeProvider)
        
        // Stationary + Gps Stalled + No Alarms + UI Hidden -> Power Save
        val shouldPowerSave = behavior.evaluatePowerSaveMode(
            isStationary = true,
            isGpsStalled = true,
            hasUnresolvedAlarms = false,
            isUiVisible = false
        )
        assertEquals(true, shouldPowerSave)

        // If Alarms present -> No Power Save
        val noPowerSaveAlarms = behavior.evaluatePowerSaveMode(
            isStationary = true,
            isGpsStalled = true,
            hasUnresolvedAlarms = true,
            isUiVisible = false
        )
        assertEquals(false, noPowerSaveAlarms)
    }
}
