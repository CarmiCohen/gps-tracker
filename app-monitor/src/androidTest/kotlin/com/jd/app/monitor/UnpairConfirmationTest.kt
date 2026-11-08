package com.jd.app.monitor

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jd.core.shared.AlertSettings
import com.jd.core.ui.HudState
import com.jd.core.ui.RevolutionTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies Chapter 127: no control on the real Monitor screen unpairs without confirmation (ISSUE-NOV5.2-O15). */
@RunWith(AndroidJUnit4::class)
class UnpairConfirmationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var resetCount = 0
    private var fullInitCount = 0
    private val maintenanceCalls = mutableListOf<String>()

    private fun showMonitorScreen() {
        composeRule.setContent {
            RevolutionTheme {
                MonitorScreen(
                    localHudState = HudState(appMode = "monitor"),
                    remoteHudState = HudState(appMode = "sensor"),
                    localPosition = null,
                    trackerId = "T",
                    monitorId = "M",
                    relayUrl = "https://relay.example",
                    trail = emptyList(),
                    localTrail = emptyList(),
                    alertSettings = AlertSettings(),
                    sirenType = "Siren",
                    isSirenPlaying = false,
                    onUpdateAlertSettings = {},
                    onUpdateSirenType = {},
                    onUpdateAlarmVolume = {},
                    onTestSiren = {},
                    onReset = { resetCount++ },
                    onUpdateRelayUrl = {},
                    onClearHomePoints = { maintenanceCalls += "clear" },
                    onResetStats = { maintenanceCalls += "stats" },
                    onFullInit = { fullInitCount++ }
                )
            }
        }
    }

    private fun assertConfirmationShownWithoutUnpairing() {
        composeRule.onNodeWithText("Unpair from tracker T?").assertExists()
        assertEquals(0, resetCount)
        assertEquals(0, fullInitCount)
    }

    private fun tapCleanAction(label: String) {
        composeRule.onNodeWithContentDescription("Setup").performClick()
        composeRule.onNodeWithText("CLEAN").performClick()
        composeRule.onNodeWithText(label).performScrollTo().performClick()
    }

    @Test
    fun gearAsksBeforeUnpairing() {
        showMonitorScreen()
        composeRule.onNodeWithContentDescription("Settings / Re-pair").performClick()
        assertConfirmationShownWithoutUnpairing()
    }

    @Test
    fun disconnectAsksBeforeUnpairing() {
        showMonitorScreen()
        composeRule.onNodeWithContentDescription("Disconnect").performClick()
        assertConfirmationShownWithoutUnpairing()
    }

    /** Full re-initialization unpairs and deletes the trail (ISSUE-NOV7.1-O35), so it asks first (ISSUE-NOV8.2-O46). */
    @Test
    fun fullReinitializationAsksBeforeUnpairing() {
        showMonitorScreen()
        tapCleanAction("Trigger Full System Re-Initialization")
        assertConfirmationShownWithoutUnpairing()

        composeRule.onNodeWithText("Cancel").performClick()
        assertEquals(0, fullInitCount)

        tapCleanAction("Trigger Full System Re-Initialization")
        composeRule.onNodeWithText("Re-initialize").performClick()
        composeRule.onNodeWithText("Unpair from tracker T?").assertDoesNotExist()
        assertEquals(1, fullInitCount)
        assertEquals(0, resetCount)
    }

    /** The two maintenance actions that keep the pairing run at once and never show the unpair dialog (ISSUE-NOV7.1-O35). */
    @Test
    fun maintenanceActionsThatKeepThePairingDoNotAsk() {
        showMonitorScreen()
        tapCleanAction("Clear Home Points & Geofence")
        tapCleanAction("Reset Operational Statistics")

        composeRule.onNodeWithText("Unpair from tracker T?").assertDoesNotExist()
        assertEquals(listOf("clear", "stats"), maintenanceCalls)
        assertEquals(0, resetCount)
        assertEquals(0, fullInitCount)
    }

    @Test
    fun cancelKeepsThePairing() {
        showMonitorScreen()
        composeRule.onNodeWithContentDescription("Settings / Re-pair").performClick()
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithText("Unpair from tracker T?").assertDoesNotExist()
        assertEquals(0, resetCount)
    }

    @Test
    fun confirmUnpairsOnce() {
        showMonitorScreen()
        composeRule.onNodeWithContentDescription("Disconnect").performClick()
        composeRule.onNodeWithText("Unpair").performClick()

        composeRule.onNodeWithText("Unpair from tracker T?").assertDoesNotExist()
        assertEquals(1, resetCount)
    }
}
