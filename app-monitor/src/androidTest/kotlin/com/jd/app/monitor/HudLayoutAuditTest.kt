package com.jd.app.monitor

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jd.core.ui.HudState
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
                HudStatusBar(hudState = HudState(appMode = "monitor"))
            }
        }

        val intLeft = composeRule.onNodeWithText("INT").getUnclippedBoundsInRoot().left
        val wdgLeft = composeRule.onNodeWithText("WDG").getUnclippedBoundsInRoot().left
        assertTrue("INT must stay left of WDG under RTL", intLeft < wdgLeft)
    }

    @Test
    fun compactHudIsShorterThanStandardHud() {
        composeRule.setContent {
            RevolutionTheme {
                HudStatusBar(HudState(appMode = "monitor"), Modifier.testTag("standard"))
                HudStatusBar(HudState(appMode = "monitor"), Modifier.testTag("compact"), compact = true)
            }
        }

        val standard = composeRule.onNodeWithTag("standard").getUnclippedBoundsInRoot()
        val compact = composeRule.onNodeWithTag("compact").getUnclippedBoundsInRoot()
        assertTrue(
            "Compact HUD must be under 60% of the standard height",
            (compact.bottom - compact.top).value < (standard.bottom - standard.top).value * 0.6f
        )
    }
}
