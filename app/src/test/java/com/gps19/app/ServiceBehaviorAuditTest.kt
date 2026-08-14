package com.gps19.app

import com.gps19.core.engine.*
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ServiceBehaviorAuditTest: Audit of R406a Dynamic Polling Intervals.
 * [Issue #169] Geofence Accuracy vs. Battery Audit.
 */
class ServiceBehaviorAuditTest {

    private val mockTimeProvider = object : TimeProvider {
        override fun elapsedRealtime(): Long = 100000L
        override fun currentTimeMillis(): Long = 1700000000000L
    }

    @Test
    fun `Audit Polling Interval - Moving with Screen Off and Geofence`() {
        val behavior = ServiceBehaviorUseCase(mockTimeProvider)
        
        // --- SCENARIO 1: A15/S21FE Hardware ---
        val a15Flags = ServiceBehaviorUseCase.DeviceSpecialFlags(isS21FE = true, isXiaomi = false)
        
        // 1a. Moving, Screen OFF, Geofence INACTIVE -> 45s (SCREEN_OFF_GPS_POLLING_MS)
        val intervalOffNoGeoA15 = behavior.calculateGpsInterval(
            isCoolingMode = false, isSuspiciousMode = false, isStationary = false, isScreenOn = false,
            isGeofenceActive = false, nowRt = 100000L, deviceSpecialFlags = a15Flags
        )
        assertEquals(45000L, intervalOffNoGeoA15)

        // 1b. Moving, Screen OFF, Geofence ACTIVE -> 2s (HIGH_FREQUENCY_GPS_POLLING_MS)
        // FIX VERIFICATION: R406a safety margin maintained. 45s drop bypassed.
        val intervalOffWithGeoA15 = behavior.calculateGpsInterval(
            isCoolingMode = false, isSuspiciousMode = false, isStationary = false, isScreenOn = false,
            isGeofenceActive = true, nowRt = 100000L, deviceSpecialFlags = a15Flags
        )
        assertEquals(2000L, intervalOffWithGeoA15)

        // --- SCENARIO 2: Standard Hardware ---
        val stdFlags = ServiceBehaviorUseCase.DeviceSpecialFlags(isS21FE = false, isXiaomi = false)

        // 2a. Moving, Screen OFF, Geofence ACTIVE -> 5s (MOVING_GPS_POLLING_MS)
        // FIX VERIFICATION: R406a safety margin maintained for standard devices.
        val intervalOffWithGeoStd = behavior.calculateGpsInterval(
            isCoolingMode = false, isSuspiciousMode = false, isStationary = false, isScreenOn = false,
            isGeofenceActive = true, nowRt = 100000L, deviceSpecialFlags = stdFlags
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
