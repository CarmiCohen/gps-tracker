package com.jd.app.sensor

import android.content.Intent
import com.jd.core.shared.SilenceCause
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Chapter 250 (ISSUE-NOV22.10-O137): which system broadcast makes the Sensor say its last word. */
class LastWordsTest {

    private fun word(
        action: String?,
        airplane: Boolean = false,
        sim: String? = null,
        mobileData: Boolean = false,
        battery: Int? = 50,
        plugged: Boolean = false
    ) = lastWordFor(action, airplane, sim, mobileData, battery, plugged)

    @Test
    fun aShutdownIsAShutdownOrAnEmptyBattery() {
        assertEquals(SilenceCause.SHUTDOWN, word(Intent.ACTION_SHUTDOWN))
        assertEquals(SilenceCause.BATTERY_EMPTY, word(Intent.ACTION_SHUTDOWN, battery = 2))
        assertEquals(SilenceCause.SHUTDOWN, word(Intent.ACTION_SHUTDOWN, battery = 2, plugged = true))
    }

    @Test
    fun airplaneModeOnlyWhenSwitchedOn() {
        assertEquals(SilenceCause.AIRPLANE_MODE, word(Intent.ACTION_AIRPLANE_MODE_CHANGED, airplane = true))
        assertNull(word(Intent.ACTION_AIRPLANE_MODE_CHANGED, airplane = false))
    }

    @Test
    fun aRemovedSimOnlyOnMobileData() {
        assertEquals(SilenceCause.SIM_REMOVED, word(ACTION_SIM_STATE_CHANGED, sim = "ABSENT", mobileData = true))
        assertNull("On Wi-Fi the Sensor stays connected", word(ACTION_SIM_STATE_CHANGED, sim = "ABSENT", mobileData = false))
        assertNull(word(ACTION_SIM_STATE_CHANGED, sim = "READY", mobileData = true))
    }

    @Test
    fun theReceiverListensToExactlyTheseBroadcasts() {
        assertEquals(listOf(Intent.ACTION_SHUTDOWN, Intent.ACTION_AIRPLANE_MODE_CHANGED, ACTION_SIM_STATE_CHANGED), LAST_WORD_ACTIONS)
        assertNull(word(Intent.ACTION_BATTERY_CHANGED))
        assertNull(word(null))
    }
}
