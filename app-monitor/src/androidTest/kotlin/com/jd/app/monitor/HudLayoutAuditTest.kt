package com.jd.app.monitor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jd.core.ui.HudState
import com.jd.core.ui.LedState
import com.jd.core.ui.ownLeds
import com.jd.core.ui.HudStatusBar
import com.jd.core.ui.RevolutionTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies Chapter 118.1 and 118.3: the HUD stays left-to-right and compact mode is shorter. */
@RunWith(AndroidJUnit4::class)
class HudLayoutAuditTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun hudStaysLeftToRightUnderRtlLayout() {
        composeRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                HudStatusBar(hudState = monitorLine)
            }
        }

        // TRK is the last LED since Nov19.5 (WDG and DAT are gone).
        val intLeft = composeRule.onNodeWithText("INT").getUnclippedBoundsInRoot().left
        val trkLeft = composeRule.onNodeWithText("TRK").getUnclippedBoundsInRoot().left
        assertTrue("INT must stay left of TRK under RTL", intLeft < trkLeft)
    }

    @Test
    fun speedStaysVisibleAtTheLargestSystemFont() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                RevolutionTheme {
                    Box(Modifier.width(360.dp)) {
                        HudStatusBar(monitorLine.copy(speedKmh = 0f), compact = true)
                    }
                }
            }
        }

        val speed = composeRule.onNodeWithText("0.0 km/h").getUnclippedBoundsInRoot()
        assertTrue("Speed must keep its width", speed.right > speed.left)
        assertTrue("Speed must stay on screen", speed.right.value <= 360f)
    }

    @Test
    fun menuBarKeepsEveryActionVisibleWithLongIds() {
        // Since Nov19.3 the bar's text is the Sensor's motion state; MOVING (between guillemets) is the longest.
        composeRule.setContent {
            RevolutionTheme {
                Box(Modifier.width(320.dp)) {
                    MonitorMenuBar("MOVING", true, {}, {}, {}, {}, {}, {})
                }
            }
        }

        listOf("Setup", "Map", "Events", "Info", "Graphs", "Close").forEach {
            val bounds = composeRule.onNodeWithContentDescription(it).getUnclippedBoundsInRoot()
            assertTrue("$it must stay visible", bounds.right > bounds.left && bounds.right.value <= 320f)
        }
    }

    @Test
    fun compactHudIsShorterThanStandardHud() {
        composeRule.setContent {
            RevolutionTheme {
                HudStatusBar(monitorLine, Modifier.testTag("standard"))
                HudStatusBar(monitorLine, Modifier.testTag("compact"), compact = true)
            }
        }

        val standard = composeRule.onNodeWithTag("standard").getUnclippedBoundsInRoot()
        val compact = composeRule.onNodeWithTag("compact").getUnclippedBoundsInRoot()
        assertTrue(
            "Compact HUD must be under 60% of the standard height",
            (compact.bottom - compact.top).value < (standard.bottom - standard.top).value * 0.6f
        )
    }

    private val monitorLine = HudState(
        appMode = "monitor",
        leds = ownLeds(isInternet = true, isRelayConnected = true, gps = LedState.OK, peerLabel = "TRK", peer = LedState.NOT_OK)
    )
}
