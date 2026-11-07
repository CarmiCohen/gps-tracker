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
                    alertSettings = AlertSettings(),
                    sirenType = "Siren",
                    isSirenPlaying = false,
                    onUpdateAlertSettings = {},
                    onUpdateSirenType = {},
                    onUpdateAlarmVolume = {},
                    onTestSiren = {},
                    onReset = { resetCount++ },
                    onUpdateRelayUrl = {}
                )
            }
        }
    }

    private fun assertConfirmationShownWithoutUnpairing() {
        composeRule.onNodeWithText("Unpair from tracker T?").assertExists()
        assertEquals(0, resetCount)
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

    @Test
    fun cleanActionsAskBeforeUnpairing() {
        listOf(
            "Clear Home Points & Geofence",
            "Reset Operational Statistics",
            "Trigger Full System Re-Initialization"
        ).forEachIndexed { index, label ->
            if (index == 0) showMonitorScreen()
            composeRule.onNodeWithContentDescription("Setup").performClick()
            composeRule.onNodeWithText("CLEAN").performClick()
            composeRule.onNodeWithText(label).performScrollTo().performClick()
            assertConfirmationShownWithoutUnpairing()
            composeRule.onNodeWithText("Cancel").performClick()
        }
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
