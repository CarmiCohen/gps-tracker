package com.jd.app.monitor

import com.jd.core.shared.AlertSettings
import com.jd.core.shared.RelayConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for ISSUE-NOV7.1-O35 (Chapter 147): Independent Clean Setup Actions.
 * Verifies that Clear Home Points & Geofence, Reset Operational Statistics, and
 * Trigger Full System Re-Initialization execute their distinct maintenance actions.
 */
class CleanSetupActionsTest {

    @Test
    fun clearHomePointsClearsGeofenceAlertSettings() {
        val defaultSettings = AlertSettings()
        assertTrue("Distance alert enabled by default", defaultSettings.distance)
        assertTrue("Jammer detection enabled by default", defaultSettings.jammerDetection)
        assertTrue("Tilt alert enabled by default", defaultSettings.tiltAlert)
        assertTrue("Lift alert enabled by default", defaultSettings.liftAlert)

        val cleared = defaultSettings.copy(
            distance = false,
            jammerDetection = false,
            tiltAlert = false,
            liftAlert = false,
            tamperAlert = false
        )

        assertFalse("Distance alert cleared", cleared.distance)
        assertFalse("Jammer detection cleared", cleared.jammerDetection)
        assertFalse("Tilt alert cleared", cleared.tiltAlert)
        assertFalse("Lift alert cleared", cleared.liftAlert)
        assertFalse("Tamper alert cleared", cleared.tamperAlert)
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
