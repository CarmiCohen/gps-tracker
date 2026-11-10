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
 * and the Phone Setup screen offers a visible Close (ISSUE-NOV10.8-O77).
 */
@RunWith(AndroidJUnit4::class)
class SetupNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val phoneSetupTitle = "Phone Setup & Capabilities"

    private fun openPhoneSetup() {
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
        composeRule.onNodeWithContentDescription("Setup").performClick()
        composeRule.onNodeWithText("PHONE").performClick()
        composeRule.onNodeWithText(phoneSetupTitle).assertExists()
    }

    private fun assertBackOnMap() {
        composeRule.onNodeWithText(phoneSetupTitle).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Setup").assertExists()
        assertFalse(composeRule.activity.isFinishing)
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
}
