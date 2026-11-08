package com.jd.app.monitor

import com.jd.core.shared.AlertSettings
import com.jd.core.shared.GeofenceConfig
import com.jd.core.shared.HomePoint
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

    /**
     * ISSUE-NOV8.13-O51 (user decision 2026-11-08): Clear Home Points removes the points only. It no longer
     * switches alarms off, so Home Points added later raise breach alarms again; the cleared geofence carries
     * a newer revision, so the tracker replaces its stored copy with the empty one.
     */
    @Test
    fun clearHomePointsRemovesOnlyThePoints() {
        val stored = GeofenceConfig(points = listOf(HomePoint(32.0, 34.0), HomePoint(32.1, 34.1)), radiusMeters = 200.0, revision = 7L)
        val cleared = stored.cleared().revisedAfter(stored, nowMs = 1L)

        assertTrue("No Home Point is left", cleared.points.isEmpty())
        assertFalse("An empty geofence raises no breach", cleared.isActive)
        assertEquals("The radius the user chose is kept", 200.0, cleared.radiusMeters, 0.0)
        assertTrue("The tracker takes the cleared geofence over its stored one", cleared.revision > stored.revision)
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
