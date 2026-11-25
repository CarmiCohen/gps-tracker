package com.jd.app.monitor

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jd.core.shared.AlertSettings
import com.jd.core.shared.EventEntry
import com.jd.core.shared.EventLevel
import com.jd.core.shared.EventSource
import com.jd.core.ui.HudState
import com.jd.core.ui.RevolutionTheme
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Chapter 178: Back on a Monitor setup screen returns to the map instead of closing the app (ISSUE-NOV10.8-O78), and
 * every overlay's shared header (SIMP-NOV10.8-S1) handles Back, on Alerts, Siren, Clean Setup and Events too
 * (ISSUE-NOV10.7-O74). Since ISSUE-NOV25.22-O151 Back is the only way: no overlay shows a close X or a Close button,
 * and Back in an event's details closes the details before the list.
 */
@RunWith(AndroidJUnit4::class)
class SetupNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val phoneSetupTitle = "Phone Setup & Capabilities"

    private fun showMap(events: List<EventEntry> = emptyList()) {
        composeRule.setContent {
            RevolutionTheme {
                MonitorScreen(
                    localHudState = HudState(),
                    remoteHudState = HudState(),
                    localPosition = null,
                    trackerId = "T",
                    monitorId = "M",
                    relayUrl = "https://relay.example",
                    trail = emptyList(),
                    localTrail = emptyList(),
                    alertSettings = AlertSettings(),
                    isSirenPlaying = false,
                    onUpdateAlertSettings = {},
                    onUpdateAlarmVolume = {},
                    onTestSiren = {},
                    onReset = {},
                    onUpdateRelayUrl = {},
                    events = events
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

    /** The header shows no close X (ISSUE-NOV25.22-O151) and Back closes; a second open closes by Back again. */
    private fun assertNoXAndBackCloses(button: String, title: String) {
        openFromSetup(button, title)
        composeRule.onNodeWithContentDescription("Close").assertDoesNotExist()
        Espresso.pressBack()
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
    fun phoneSetupHeaderShowsNoCloseX() {
        openPhoneSetup()
        composeRule.onNodeWithContentDescription("Close").assertDoesNotExist()
        Espresso.pressBack()
        assertBackOnMap()
    }

    @Test
    fun phoneSetupHasNoBottomCloseButton() {
        openPhoneSetup()
        composeRule.onNodeWithText("Close").assertDoesNotExist()
        Espresso.pressBack()
        assertBackOnMap()
    }

    @Test
    fun alertsHeaderAndBackClose() = assertNoXAndBackCloses("ALERTS", "ALERTS & SENSORS SETUP")

    @Test
    fun sirenHeaderAndBackClose() = assertNoXAndBackCloses("SIREN", "SIREN & AUDIO SETUP")

    @Test
    fun cleanSetupHeaderAndBackClose() = assertNoXAndBackCloses("CLEAN", "MAINTENANCE & CLEAN SETUP")

    @Test
    fun backFromEventsReturnsToTheMap() {
        showMap()
        composeRule.onNodeWithContentDescription("Events").performClick()
        composeRule.onNodeWithText("EVENTS").assertExists()
        Espresso.pressBack()
        assertBackOnMap("EVENTS")
    }

    /** An event's details have no X (ISSUE-NOV25.22-O151): the first Back closes them, the second the list. */
    @Test
    fun backClosesEventDetailsBeforeTheList() {
        val event = EventEntry("e1", System.currentTimeMillis(), EventSource.MONITOR, EventLevel.INFO, "Test event")
        showMap(listOf(event))
        composeRule.onNodeWithContentDescription("Events").performClick()
        composeRule.onNodeWithText("Test event").performClick()
        composeRule.onNodeWithText("Graphs at this time").assertExists()
        composeRule.onNodeWithContentDescription("Close details").assertDoesNotExist()
        Espresso.pressBack()
        composeRule.onNodeWithText("Graphs at this time").assertDoesNotExist()
        composeRule.onNodeWithText("EVENTS").assertExists()
        Espresso.pressBack()
        assertBackOnMap("EVENTS")
    }
}
