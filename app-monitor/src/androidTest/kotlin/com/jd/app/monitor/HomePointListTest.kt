package com.jd.app.monitor

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jd.core.shared.AlertSettings
import com.jd.core.shared.GeofenceConfig
import com.jd.core.shared.HomePoint
import com.jd.core.ui.HudState
import com.jd.core.ui.RevolutionTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Chapter 193: the Home Point editor lists every point with a remove button, and a remove button asks
 * before the point is removed, through the same dialog as a marker tap (ISSUE-NOV10.8-O85).
 */
@RunWith(AndroidJUnit4::class)
class HomePointListTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val removed = mutableListOf<Int>()

    private fun openHomeEditor() {
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
                    onClearHomePoints = {},
                    onResetStats = {},
                    onFullInit = {},
                    onClose = {},
                    geofence = GeofenceConfig(points = listOf(HomePoint(32.1, 34.8), HomePoint(31.25, 34.791234))),
                    onRemoveHomePoint = { removed += it }
                )
            }
        }
        composeRule.onNodeWithText("HOME").performClick()
    }

    @Test
    fun editorListsEveryPoint() {
        openHomeEditor()
        composeRule.onNodeWithText("1.  32.10000, 34.80000").assertExists()
        composeRule.onNodeWithText("2.  31.25000, 34.79123").assertExists()
        composeRule.onNodeWithContentDescription("Remove Home Point 1").assertExists()
        composeRule.onNodeWithContentDescription("Remove Home Point 2").assertExists()
    }

    @Test
    fun removeButtonAsksAndCancelKeepsThePoint() {
        openHomeEditor()
        composeRule.onNodeWithContentDescription("Remove Home Point 2").performClick()
        composeRule.onNodeWithText("Remove Home Point 2?").assertExists()
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithText("Remove Home Point 2?").assertDoesNotExist()
        assertEquals(emptyList<Int>(), removed)
    }

    @Test
    fun removeButtonRemovesOnlyAfterConfirmation() {
        openHomeEditor()
        composeRule.onNodeWithContentDescription("Remove Home Point 2").performClick()
        composeRule.onNodeWithText("Remove").performClick()

        assertEquals(listOf(1), removed)
        composeRule.onNodeWithText("Remove Home Point 2?").assertDoesNotExist()
    }
}
