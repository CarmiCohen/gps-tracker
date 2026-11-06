package com.jd.app.monitor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

/** Verifies Chapter 124: the real Setup dialog reports the relay state that drives the SRV LED. */
@RunWith(AndroidJUnit4::class)
class RelayStatusDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var isRelayConnected by mutableStateOf(false)

    private fun openSetupDialog() {
        composeRule.setContent {
            RevolutionTheme {
                MonitorScreen(
                    localHudState = HudState(appMode = "monitor", isRelayConnected = isRelayConnected),
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
                    onReset = {},
                    onUpdateRelayUrl = {}
                )
            }
        }
        composeRule.onNodeWithContentDescription("Setup").performClick()
    }

    @Test
    fun disconnectedRelayIsReportedAsDisconnected() {
        openSetupDialog()

        composeRule.onNodeWithText("Relay Status: Disconnected").assertExists()
        composeRule.onNodeWithText("Relay Status: Connected").assertDoesNotExist()
    }

    @Test
    fun openDialogFollowsTheRelayState() {
        isRelayConnected = true
        openSetupDialog()
        composeRule.onNodeWithText("Relay Status: Connected").assertExists()

        isRelayConnected = false
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Relay Status: Disconnected").assertExists()
    }
}
