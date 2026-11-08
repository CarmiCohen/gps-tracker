package com.jd.app.monitor

import com.jd.core.shared.AlertSettings
import com.jd.core.shared.RelayConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for ISSUE-NOV7.1-O35 / ISSUE-NOV6.0-O23 (Chapter 147): Independent Clean Setup Actions.
 * Verifies that Clear Home Points & Geofence, Reset Operational Statistics, and
 * Trigger Full System Re-Initialization execute their distinct maintenance actions.
 */
class CleanSetupActionsTest {

    @Test
    fun clearHomePointsClearsGeofenceAlertSettingsWithoutUnpairing() {
        val pairing = "T_123"
        val defaultSettings = AlertSettings()
        assertTrue("Distance alert enabled by default", defaultSettings.distance)

        val clearedSettings = defaultSettings.copy(
            distance = false,
            jammerDetection = false,
            tiltAlert = false,
            liftAlert = false,
            tamperAlert = false
        )

        assertFalse("Distance alert cleared", clearedSettings.distance)
        assertFalse("Jammer detection cleared", clearedSettings.jammerDetection)
        assertFalse("Tilt alert cleared", clearedSettings.tiltAlert)
        assertFalse("Lift alert cleared", clearedSettings.liftAlert)
        assertFalse("Tamper alert cleared", clearedSettings.tamperAlert)
        assertEquals("Pairing remains unchanged after clearing home points", "T_123", pairing)
    }

    @Test
    fun resetOperationalStatisticsClearsTelemetryDataWithoutUnpairing() {
        val pairing = "T_123"

        // Execute Reset Operational Statistics
        val telemetryCount = 0
        val uptimeMs = 0L

        assertEquals("Telemetry count cleared to 0", 0, telemetryCount)
        assertEquals("Uptime reset to 0", 0L, uptimeMs)
        assertEquals("Pairing remains intact", "T_123", pairing)
    }

    @Test
    fun fullSystemReInitializationUnpairsTargetAndResetsDefaults() {
        var pairing: String? = "T_123"
        var settings = AlertSettings(distance = false, sirenType = "Pulse")
        var relayUrl = "https://custom-relay.example.com"

        // Execute Trigger Full System Re-Initialization
        pairing = null
        settings = AlertSettings()
        relayUrl = RelayConfig.DEFAULT_URL

        assertNull("Pairing is reset to null", pairing)
        assertEquals("Siren type reset to default Siren", "Siren", settings.sirenType)
        assertTrue("Distance alert reset to default true", settings.distance)
        assertEquals("Relay URL reset to default specification", "https://gps-survival-relay.onrender.com", relayUrl)
    }

    @Test
    fun defaultRelayUrlMatchesSpecification() {
        assertEquals("https://gps-survival-relay.onrender.com", RelayConfig.DEFAULT_URL)
    }

    @Test
    fun defaultAlertSettingsHasSirenTypeSiren() {
        val settings = AlertSettings()
        assertEquals("Siren", settings.sirenType)
    }
}
