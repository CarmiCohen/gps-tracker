package com.gps19.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HudVisibilityTest: Verifies HUD occlusion prevention logic.
 * Targets Chapter 4.11 of TEST_PROCEDURE.md (Issue #1421).
 */
class HudVisibilityTest {

    @Test
    fun `Verify HUD Occlusion Logic - HUD hides when any overlay is open`() {
        // 1. All overlays closed
        val navClosed = NavigationState(
            isSettingsOpen = false,
            isLogVisible = false,
            isRibbonsVisible = false,
            isGnssDetailVisible = false,
            isPhoneSetupVisible = false
        )
        assertFalse("HUD should be visible when no overlays are open", navClosed.isAnyOverlayOpen)

        // 2. Settings open
        val navSettings = navClosed.copy(isSettingsOpen = true)
        assertTrue("HUD must hide when Settings is open", navSettings.isAnyOverlayOpen)

        // 3. Logs open
        val navLogs = navClosed.copy(isLogVisible = true)
        assertTrue("HUD must hide when Logs is open", navLogs.isAnyOverlayOpen)

        // 4. Phone Setup open
        val navSetup = navClosed.copy(isPhoneSetupVisible = true)
        assertTrue("HUD must hide when Phone Setup is open", navSetup.isAnyOverlayOpen)
    }
}
