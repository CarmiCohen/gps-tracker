package com.jd.app.monitor

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jd.core.shared.AlertSettings
import com.jd.core.shared.GeofenceConfig
import com.jd.core.shared.HomePoint
import com.jd.core.ui.HudState
import com.jd.core.ui.HudStatusBar
import com.jd.core.ui.LedState
import com.jd.core.ui.ownLeds
import com.jd.core.ui.RevolutionTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Chapter 225 (Nov18.10): texts that were cut off on the S21FE in Nov18.9 are shown whole: the Monitor's own ID beside
 * a long telemetry line (ISSUE-NOV18.9-O117), the Setup dialog's UNPAIR button (ISSUE-NOV18.9-O118) and a Home Point's
 * coordinates (ISSUE-NOV18.9-O119).
 */
@RunWith(AndroidJUnit4::class)
class TextFitTest {

    @get:Rule
    val composeRule = createComposeRule()

    /**
     * Fails when the node's text is ellipsized, cut by its lines or a line is wider than the text's drawn size.
     * Not `hasVisualOverflow`: a one-line or centered text is laid out at its full allowed width and reports a width
     * overflow while its glyphs fit (seen on the A15: `This phone: VIEWER` 413 px of 576).
     */
    private fun SemanticsNodeInteraction.assertTextFits() {
        val layouts = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val layout = layouts.single()
        val text = layout.layoutInput.text
        assertFalse("Lines cut off: $text", layout.didOverflowHeight)
        for (line in 0 until layout.lineCount) {
            assertFalse("Line $line ellipsized: $text", layout.isLineEllipsized(line))
            val lineWidth = layout.getLineRight(line) - layout.getLineLeft(line)
            assertTrue("Line $line of '$text' is $lineWidth px, wider than ${layout.size.width}", lineWidth <= layout.size.width + 0.5f)
        }
    }

    /** Chapter 232.3: the S21FE (about 333 dp at its display zoom) cut the motion label to `STATION` in Nov19.5. */
    @Test
    fun theMotionLabelStaysWholeInANarrowMenuBar() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.3f)) {
                RevolutionTheme {
                    androidx.compose.foundation.layout.Box(Modifier.width(320.dp)) {
                        MonitorMenuBar("STATIONARY", true, {}, {}, {}, {}, {})
                    }
                }
            }
        }
        composeRule.onNodeWithText(com.jd.core.ui.motionLabelText("STATIONARY")).assertTextFits()
        listOf("Setup", "Events", "Info", "Graphs", "Close").forEach {
            composeRule.onNodeWithContentDescription(it).assertIsDisplayed()
        }
    }

    @Test
    fun theMonitorIdStaysWholeBesideLongTelemetry() {
        val state = HudState(selfId = "VIEWER", batteryLevel = 38, isCharging = true, tempCelsius = 29f,
            accuracyMeters = 96f, telemetryAgeSec = 465L,
            leds = ownLeds(isInternet = true, isRelayConnected = true, gps = LedState.OK, peerLabel = "TRK", peer = LedState.OK))
        composeRule.setContent {
            // A narrow phone; the S21FE (about 384 dp) cut the ID in Nov18.9.
            RevolutionTheme { HudStatusBar(hudState = state, modifier = Modifier.width(360.dp)) }
        }
        composeRule.onNodeWithText(state.idText).assertTextFits()
        composeRule.onNodeWithText(state.telemetryText).assertTextFits()
    }

    @Test
    fun theHomePointCoordinatesWrapInsteadOfBeingCut() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
                RevolutionTheme {
                    HomePointEditor(
                        geofence = GeofenceConfig(points = listOf(HomePoint(32.65295, 34.98765))),
                        onSetRadius = {},
                        onRemovePoint = {},
                        onDone = {},
                        modifier = Modifier.width(220.dp)
                    )
                }
            }
        }
        composeRule.onNodeWithText("1.  32.65295, 34.98765").assertTextFits()
    }

    @Test
    fun unpairIsReachableInATallSetupDialog() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
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
                        onClose = {}
                    )
                }
            }
        }
        composeRule.onNodeWithContentDescription("Setup").performClick()
        composeRule.onNodeWithText("UNPAIR").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("UNPAIR").assertTextFits()
    }
}
