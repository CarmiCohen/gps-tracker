package com.jd.app.monitor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jd.core.ui.RevolutionTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies Chapter 129.4 and 129.6 on the device: the real siren plays and stops, a forced maximum volume is restored,
 * the alarm channel cannot be lowered, and the alarm screen acknowledges (ISSUE-NOV5.2-O11).
 * The siren plays at gain 0, so the test phone stays silent.
 */
@RunWith(AndroidJUnit4::class)
class MonitorAlarmDeviceTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val siren = SirenPlayer(context)
    private val silent = SirenSound("Siren", gain = 0f, useAlarmStream = true, forceMaxVolume = false, vibrate = false)

    // The phone's own alarm volume, put back after each test.
    private val phoneAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)

    @After
    fun tearDown() {
        siren.stop()
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, phoneAlarmVolume, 0)
    }

    @Test
    fun theSirenPlaysInALoopUntilStopped() {
        siren.play(silent)

        assertTrue(siren.isPlaying.value)
        assertEquals(AudioTrack.PLAYSTATE_PLAYING, siren.trackPlayState)
        Thread.sleep(2_500) // longer than one loop of the wave
        assertEquals(AudioTrack.PLAYSTATE_PLAYING, siren.trackPlayState)

        siren.stop()
        assertFalse(siren.isPlaying.value)
        assertNull(siren.trackPlayState)
    }

    @Test
    fun playingTheSameSoundAgainKeepsOneSiren() {
        siren.play(silent)
        siren.play(silent)
        assertEquals(silent, siren.currentSound)

        val pulse = silent.copy(waveType = "Pulse")
        siren.play(pulse)
        assertEquals(pulse, siren.currentSound)
        assertEquals(AudioTrack.PLAYSTATE_PLAYING, siren.trackPlayState)
    }

    @Test
    fun aForcedMaximumVolumeIsRestoredWhenTheSirenStops() {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val before = if (phoneAlarmVolume in 1 until max) phoneAlarmVolume else max - 1
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, before, 0)

        siren.play(silent.copy(forceMaxVolume = true))
        assertEquals(max, audioManager.getStreamVolume(AudioManager.STREAM_ALARM))

        siren.stop()
        assertEquals(before, audioManager.getStreamVolume(AudioManager.STREAM_ALARM))
    }

    @Test
    fun theAlarmChannelIsHighAndTheLoweredLegacyChannelIsRemoved() {
        // The state found on installed phones: the shared channel lowered to low importance.
        notifications.createNotificationChannel(
            NotificationChannel(MonitorApp.LEGACY_CHANNEL_ID, "Critical Theft Alerts", NotificationManager.IMPORTANCE_LOW)
        )

        createMonitorNotificationChannels(context)

        assertNull(notifications.getNotificationChannel(MonitorApp.LEGACY_CHANNEL_ID))
        val alarm = notifications.getNotificationChannel(MonitorApp.ALARM_CHANNEL_ID)
        assertNotNull(alarm)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, alarm.importance)
        assertNull(alarm.sound)
        assertEquals(NotificationManager.IMPORTANCE_LOW, notifications.getNotificationChannel(MonitorApp.SESSION_CHANNEL_ID).importance)
    }

    @Test
    fun theAlarmScreenNamesEachAlarmAndAcknowledges() {
        var acknowledged = 0
        val pending = listOf(
            Alarm(AlarmType.POWER_DISCONNECTED, raisedAtMs = 0L, isActive = true, isAcknowledged = false),
            Alarm(AlarmType.TRACKER_OFFLINE, raisedAtMs = 0L, isActive = false, isAcknowledged = false)
        )
        composeRule.setContent {
            RevolutionTheme { AlarmAlertScreen(pending, trackerId = "T", onAcknowledge = { acknowledged++ }) }
        }

        composeRule.onNodeWithText("ALARM").assertExists()
        composeRule.onNodeWithText(pending[0].description("T")).assertExists()
        assertTrue(pending[0].description("T").startsWith("Tracker T power disconnected since "))
        assertTrue(pending[1].description("T").endsWith("(cleared)"))
        composeRule.onNodeWithText("ACKNOWLEDGE ALARM").performClick()
        assertEquals(1, acknowledged)
    }
}
