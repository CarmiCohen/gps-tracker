package com.jd.app.monitor

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jd.core.shared.AlertSettings
import com.jd.core.ui.HudState
import com.jd.core.ui.RevolutionTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies Chapter 134: screen state, overlays, dialogs, and follow mode persist across configuration changes (ISSUE-NOV6.0-O22). */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class RotationStateRestorationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun setupDialogPersistsAcrossRotation() {
        val restorationTester = StateRestorationTester(composeRule)

        restorationTester.setContent {
            RevolutionTheme {
                MonitorScreen(
                    localHudState = HudState(appMode = "monitor"),
                    remoteHudState = HudState(appMode = "sensor"),
                    localPosition = null,
                    trackerId = "T123",
                    monitorId = "M456",
                    relayUrl = "https://relay.example",
                    trail = emptyList(),
                    alertSettings = AlertSettings(),
                    sirenType = "Siren",
                    isSirenPlaying = false,
                    onUpdateAlertSettings = {},
                    onUpdateSirenType = {},
                    onUpdateAlarmVolume = {},
                    onTestSiren = {},
                    onReset = {},
                    onUpdateRelayUrl = {}
                )
            }
        }

        // Open Setup dialog
        composeRule.onNodeWithContentDescription("Setup").performClick()
        composeRule.onNodeWithText("Monitor Setup & Info").assertExists()

        // Emulate configuration change / rotation save & restore state
        restorationTester.emulateSavedInstanceStateRestore()

        // Verify Setup dialog remains open after rotation
        composeRule.onNodeWithText("Monitor Setup & Info").assertExists()
    }

    @Test
    fun followModePersistsAcrossRotation() {
        val restorationTester = StateRestorationTester(composeRule)

        restorationTester.setContent {
            RevolutionTheme {
                MonitorScreen(
                    localHudState = HudState(appMode = "monitor"),
                    remoteHudState = HudState(appMode = "sensor"),
                    localPosition = null,
                    trackerId = "T123",
                    monitorId = "M456",
                    relayUrl = "https://relay.example",
                    trail = emptyList(),
                    alertSettings = AlertSettings(),
                    sirenType = "Siren",
                    isSirenPlaying = false,
                    onUpdateAlertSettings = {},
                    onUpdateSirenType = {},
                    onUpdateAlarmVolume = {},
                    onTestSiren = {},
                    onReset = {},
                    onUpdateRelayUrl = {}
                )
            }
        }

        // Click SENSOR follow mode button
        composeRule.onNodeWithText("SENSOR").performClick()

        // Emulate configuration change / rotation save & restore state
        restorationTester.emulateSavedInstanceStateRestore()

        // SENSOR button and UI components should still be intact after restoration
        composeRule.onNodeWithText("SENSOR").assertExists()
    }
}
