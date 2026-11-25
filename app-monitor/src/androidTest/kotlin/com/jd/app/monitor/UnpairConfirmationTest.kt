package com.jd.app.monitor

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jd.core.shared.AlertSettings
import com.jd.core.ui.HudState
import com.jd.core.ui.RevolutionTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies Chapter 127: no control on the real Monitor screen unpairs without confirmation (ISSUE-NOV5.2-O15),
 * and chapter 183: the Tools and Disconnect buttons are gone (ISSUE-NOV10.8-O76); since ISSUE-NOV25.22-O151 the menu has no
 * red X either (Back on the map sends the app away). Events (chapter 190) and Graphs (chapter 192) came back with
 * ISSUE-NOV10.8-O84 and close by Back.
 */
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
                    onReset = { resetCount++ },
                    onUpdateRelayUrl = {},
                    onClearHomePoints = { maintenanceCalls += "clear" },
                    onResetStats = { maintenanceCalls += "stats" },
                    onFullInit = { fullInitCount++ },
                    graphsOverlay = { onClose ->
                        com.jd.core.ui.GraphsOverlay(
                            title = "GRAPHS",
                            state = com.jd.core.shared.monitorGraphScreen(emptyList(), emptyList(), com.jd.core.shared.ViewPeriod.DAY, System.currentTimeMillis()),
                            period = com.jd.core.shared.ViewPeriod.DAY,
                            onPeriod = {},
                            onClose = onClose
                        )
                    }
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

    /** One setup icon (ISSUE-NOV17.7-O109): unpairing is the Setup dialog's UNPAIR button, the menu has no gear. */
    private fun tapUnpair() {
        composeRule.onNodeWithContentDescription("Setup").performClick()
        composeRule.onNodeWithText("UNPAIR").performClick()
    }

    @Test
    fun gearAsksBeforeUnpairing() {
        showMonitorScreen()
        composeRule.onNodeWithContentDescription("Settings / Re-pair").assertDoesNotExist()
        tapUnpair()
        assertConfirmationShownWithoutUnpairing()
    }

    @Test
    fun menuHasNoRedXAndNothingUnpairs() {
        showMonitorScreen()
        composeRule.onNodeWithContentDescription("Close").assertDoesNotExist()

        composeRule.onNodeWithText("Unpair from tracker T?").assertDoesNotExist()
        assertEquals(0, resetCount)
    }

    @Test
    fun deadMenuButtonsAreGone() {
        showMonitorScreen()
        listOf("Tools", "Disconnect").forEach {
            composeRule.onNodeWithContentDescription(it).assertDoesNotExist()
        }
    }

    /** The menu's Events button opens the Events list, and Back returns to the map (ISSUE-NOV10.8-O84, ISSUE-NOV25.22-O151). */
    @Test
    fun eventsButtonOpensTheEventsList() {
        showMonitorScreen()
        composeRule.onNodeWithContentDescription("Events").performClick()
        composeRule.onNodeWithText("EVENTS").assertExists()
        composeRule.onNodeWithText("24 h").assertExists()
        Espresso.pressBack()
        composeRule.onNodeWithText("EVENTS").assertDoesNotExist()
    }

    /** The menu's Graphs button opens the Graphs, and Back returns to the map (ISSUE-NOV10.8-O84 part 3, ISSUE-NOV25.22-O151). */
    @Test
    fun graphsButtonOpensTheGraphs() {
        showMonitorScreen()
        composeRule.onNodeWithContentDescription("Graphs").performClick()
        composeRule.onNodeWithText("GRAPHS").assertExists()
        composeRule.onNodeWithText("SENSOR - MOTION").assertExists()
        composeRule.onNodeWithText("7 d").assertExists()
        Espresso.pressBack()
        composeRule.onNodeWithText("GRAPHS").assertDoesNotExist()
    }

    /** Phone Setup's Home Points item opens the map's home point editor (ISSUE-NOV10.8-O76). */
    /**
     * Chapter 200 (ISSUE-NOV17.0-O101): the map shows the installed version at its bottom left;
     * chapter 222 (ISSUE-NOV18.6-O116): the name alone, without "Version:".
     */
    @Test
    fun theMapShowsTheVersion() {
        showMonitorScreen()
        val version = com.jd.core.ui.getAppVersionName(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext)
        composeRule.onNodeWithText(version).assertExists()
        composeRule.onNodeWithText("Version: $version").assertDoesNotExist()
    }

    /** ISSUE-NOV17.7-O108: the map has a FENCE button, and tapping it twice leaves the map usable. */
    @Test
    fun theFenceButtonToggles() {
        showMonitorScreen()
        composeRule.onNodeWithText("FENCE").performClick()
        composeRule.onNodeWithText("FENCE").performClick()
        composeRule.onNodeWithText("FENCE").assertExists()
    }

    @Test
    fun phoneSetupOpenMapStartsHomePointEditing() {
        showMonitorScreen()
        composeRule.onNodeWithContentDescription("Setup").performClick()
        composeRule.onNodeWithText("PHONE").performClick()
        listOf("Authorize Exact Alarms", "Refresh Status", "Diagnostics").forEach {
            composeRule.onNodeWithText(it).assertDoesNotExist()
        }
        composeRule.onNodeWithText("Open Map").performScrollTo().performClick()

        composeRule.onNodeWithText("Phone Setup & Capabilities").assertDoesNotExist()
        composeRule.onNodeWithText("DONE").assertExists()
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
        tapUnpair()
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithText("Unpair from tracker T?").assertDoesNotExist()
        assertEquals(0, resetCount)
    }

    @Test
    fun confirmUnpairsOnce() {
        showMonitorScreen()
        tapUnpair()
        composeRule.onNodeWithText("Unpair").performClick()

        composeRule.onNodeWithText("Unpair from tracker T?").assertDoesNotExist()
        assertEquals(1, resetCount)
    }
}
