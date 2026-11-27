package com.jd.app.monitor

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jd.core.ui.RevolutionTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ISSUE-NOV27.1-O167: the map CLEAR dialog keeps the last 24 h of both trails by default, shows the rows a choice
 * deletes, counted again on every change, and clears the chosen span and trail only on Clear.
 */
@RunWith(AndroidJUnit4::class)
class TrailClearDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val counted = mutableListOf<TrailClearChoice>()
    private val cleared = mutableListOf<TrailClearChoice>()

    private fun showDialog(count: (TrailClearChoice) -> TrailClearCount) {
        composeRule.setContent {
            RevolutionTheme {
                TrailClearDialog(
                    countRows = { choice -> counted += choice; count(choice) },
                    onClear = { cleared += it },
                    onDismiss = {}
                )
            }
        }
    }

    @Test
    fun theDefaultKeepsTheLastDayAndShowsWhatGoes() {
        showDialog { TrailClearCount(12_340, 15_020) }

        composeRule.onNodeWithText("24 h").assertIsSelected()
        composeRule.onNodeWithText("Both trails").assertIsSelected()
        composeRule.onNodeWithTag(TRAIL_CLEAR_COUNT_TAG).assertTextEquals("Deletes 12,340 of 15,020 rows")
        composeRule.onNodeWithText("Clear").assertIsEnabled().performClick()

        assertEquals(listOf(TrailClearChoice()), cleared)
    }

    @Test
    fun aNewChoiceIsCountedAgainAndCleared() {
        showDialog { if (it.keep == TrailKeep.HOURS_6) TrailClearCount(500, 900) else TrailClearCount(100, 900) }

        composeRule.onNodeWithText("6 h").performClick()
        composeRule.onNodeWithText("The Sensor's trail only").performClick()
        composeRule.onNodeWithTag(TRAIL_CLEAR_COUNT_TAG).assertTextEquals("Deletes 500 of 900 rows")
        composeRule.onNodeWithText("Clear").performClick()

        val chosen = TrailClearChoice(TrailKeep.HOURS_6, TrailScope.SENSOR)
        assertEquals(chosen, counted.last())
        assertEquals(listOf(chosen), cleared)
    }

    @Test
    fun nothingToDeleteLeavesClearOff() {
        showDialog { TrailClearCount(0, 40) }

        composeRule.onNodeWithTag(TRAIL_CLEAR_COUNT_TAG).assertTextEquals("Deletes 0 of 40 rows")
        composeRule.onNodeWithText("Clear").assertIsNotEnabled()
    }
}
