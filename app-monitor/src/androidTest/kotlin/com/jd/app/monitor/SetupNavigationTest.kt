package com.jd.app.monitor

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jd.core.shared.AlertSettings
import com.jd.core.ui.HudState
import com.jd.core.ui.RevolutionTheme
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Chapter 178: Back on a Monitor setup screen returns to the map instead of closing the app (ISSUE-NOV10.8-O78),
 * the Phone Setup screen offers a visible Close (ISSUE-NOV10.8-O77), and since Nov19.1 every overlay's shared header
 * (SIMP-NOV10.8-S1) shows its close X on screen and handles Back, on Alerts, Siren, Clean Setup and Events too
 * (ISSUE-NOV10.7-O74).
 */
@RunWith(AndroidJUnit4::class)
class SetupNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val phoneSetupTitle = "Phone Setup & Capabilities"

    private fun showMap() {
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
                    onReset = {},
                    onUpdateRelayUrl = {}
                )
            }
        }
    }

    /** Opens a setup screen with its button in the Setup dialog and checks its [title] is shown. */
    private fun openFromSetup(button: String, title: String) {
        showMap()
        composeRule.onNodeWithContentDescription("Setup").performClick()
        composeRule.onNodeWithText(button).performClick()
        composeRule.onNodeWithText(title).assertExists()
    }

    private fun openPhoneSetup() = openFromSetup("PHONE", phoneSetupTitle)

    private fun assertBackOnMap(title: String = phoneSetupTitle) {
        composeRule.onNodeWithText(title).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Setup").assertExists()
        assertFalse(composeRule.activity.isFinishing)
    }

    /** The header's X is on screen (ISSUE-NOV10.7-O74) and closes; a fresh open, then Back closes too. */
    private fun assertHeaderAndBackClose(button: String, title: String) {
        openFromSetup(button, title)
        composeRule.onAllNodesWithContentDescription("Close")[0].assertIsDisplayed().performClick()
        assertBackOnMap(title)
        composeRule.onNodeWithContentDescription("Setup").performClick()
        composeRule.onNodeWithText(button).performClick()
        composeRule.onNodeWithText(title).assertExists()
        Espresso.pressBack()
        assertBackOnMap(title)
    }

    @Test
    fun backFromPhoneSetupReturnsToTheMap() {
        openPhoneSetup()
        Espresso.pressBack()
        assertBackOnMap()
    }

    @Test
    fun headerCloseIsShownAndCloses() {
        openPhoneSetup()
        composeRule.onAllNodesWithContentDescription("Close")[0].assertIsDisplayed().performClick()
        assertBackOnMap()
    }

    @Test
    fun bottomCloseButtonCloses() {
        openPhoneSetup()
        composeRule.onNodeWithText("Close").performScrollTo().performClick()
        assertBackOnMap()
    }

    @Test
    fun alertsHeaderAndBackClose() = assertHeaderAndBackClose("ALERTS", "ALERTS & SENSORS SETUP")

    @Test
    fun sirenHeaderAndBackClose() = assertHeaderAndBackClose("SIREN", "SIREN & AUDIO SETUP")

    @Test
    fun cleanSetupHeaderAndBackClose() = assertHeaderAndBackClose("CLEAN", "MAINTENANCE & CLEAN SETUP")

    @Test
    fun backFromEventsReturnsToTheMap() {
        showMap()
        composeRule.onNodeWithContentDescription("Events").performClick()
        composeRule.onNodeWithText("EVENTS").assertExists()
        Espresso.pressBack()
        assertBackOnMap("EVENTS")
    }
}
