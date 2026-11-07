package com.jd.app.monitor

import com.jd.core.shared.LocationUpdate
import com.jd.core.shared.atUiRate
import com.jd.core.ui.HudState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies Chapter 75.1 and 120.4 (ISSUE-NOV6.0-O21): HUD data flows rate-limit high frequency 10Hz streams to 2Hz (500ms). */
class HudRateTest {

    @Test
    fun tenHertzTelemetryStreamIsThrottledToTwoHertzForHudDisplay() = runBlocking {
        val highRateTelemetry = flow {
            repeat(20) { seq ->
                emit(LocationUpdate(deviceId = "T", sequenceId = seq.toLong(), speed = seq * 1.0f))
                delay(100) // 10Hz
            }
        }

        val throttled = highRateTelemetry.atUiRate().toList()

        // 20 items emitted over 2000ms at 100ms interval (10Hz).
        // Sampled every 500ms (2Hz), expected ~4 items returned instead of 20.
        assertTrue("Expected 3 to 4 items but got ${throttled.size}", throttled.size in 3..4)
    }

    @Test
    fun hudStateStreamThrottlesRapidEmissionsToMaxTwoHertz() = runBlocking {
        val highRateHudStates = flow {
            repeat(10) { step ->
                emit(HudState(appMode = "monitor", sequenceId = step.toLong()))
                delay(100) // 10Hz
            }
        }

        val throttledStates = highRateHudStates.atUiRate().toList()

        // 10 items emitted over 1000ms at 100ms interval.
        // Sampled every 500ms (2Hz), expected ~2 items returned instead of 10.
        assertTrue("Expected 1 to 2 items but got ${throttledStates.size}", throttledStates.size in 1..2)
    }
}
