package com.gps19.app

import com.gps19.core.engine.*
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ServiceBehaviorAuditTest: Audit of R406a Dynamic Polling Intervals.
 * Sep.04.01:
 * - Issue #898 RESOLVED: A15 Background Hardening. Added audit for 10s 
 *   forced baseline on A15 devices when moving with screen off (R898).
 * Aug.29.09:
 * - Concern #764 Simplification: Refactored tests to use HardwareCapabilities 
 *   instead of redundant DeviceSpecialFlags.
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
        
        // --- SCENARIO 1: A15 Hardware (R898 Hardening) ---
        val a15Caps = HardwareCapabilities(isA15Device = true)
        
        // 1a. Moving, Screen OFF, Geofence INACTIVE -> MUST be 10s (SUSPICIOUS_GPS_POLLING_MS)
        // R898: Prevents 45s drop to stay within 90s UI staleness window.
        val intervalOffNoGeoA15 = behavior.calculateGpsInterval(
            isCoolingMode = false, isSuspiciousMode = false, isStationary = false, isScreenOn = false,
            isGeofenceActive = false, nowRt = 100000L, capabilities = a15Caps
        )
        assertEquals(SUSPICIOUS_GPS_POLLING_MS, intervalOffNoGeoA15)
        assertEquals(10000L, intervalOffNoGeoA15)

        // 1b. Moving, Screen OFF, Geofence ACTIVE -> 2s (HIGH_FREQUENCY_GPS_POLLING_MS)
        // (A15 typically has requiresAdaptationMuzzle=true which triggers HF)
        val a15CapsHF = HardwareCapabilities(isA15Device = true, requiresAdaptationMuzzle = true)
        val intervalOffWithGeoA15 = behavior.calculateGpsInterval(
            isCoolingMode = false, isSuspiciousMode = false, isStationary = false, isScreenOn = false,
            isGeofenceActive = true, nowRt = 100000L, capabilities = a15CapsHF
        )
        assertEquals(2000L, intervalOffWithGeoA15)

        // --- SCENARIO 2: Standard Hardware ---
        val stdCaps = HardwareCapabilities(isA15Device = false)

        // 2a. Moving, Screen OFF, Geofence INACTIVE -> 45s (SCREEN_OFF_GPS_POLLING_MS)
        val intervalOffNoGeoStd = behavior.calculateGpsInterval(
            isCoolingMode = false, isSuspiciousMode = false, isStationary = false, isScreenOn = false,
            isGeofenceActive = false, nowRt = 100000L, capabilities = stdCaps
        )
        assertEquals(SCREEN_OFF_GPS_POLLING_MS, intervalOffNoGeoStd)
        assertEquals(45000L, intervalOffNoGeoStd)
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
    }
}
