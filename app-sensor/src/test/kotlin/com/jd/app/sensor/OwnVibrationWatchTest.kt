package com.jd.app.sensor

import android.media.AudioAttributes
import android.media.AudioManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Chapter 166 (ISSUE-NOV9.2-O62): what counts as the Sensor phone's own ringing or notifying. */
class OwnVibrationWatchTest {

    @Test
    fun anIncomingCallRingsEvenWithoutSound() {
        assertTrue(isOwnVibrationSource(AudioManager.MODE_RINGTONE, emptyList()))
    }

    @Test
    fun aRingtoneNotificationOrAlarmPlaying() {
        listOf(
            AudioAttributes.USAGE_NOTIFICATION_RINGTONE,
            AudioAttributes.USAGE_NOTIFICATION,
            AudioAttributes.USAGE_NOTIFICATION_EVENT,
            AudioAttributes.USAGE_ALARM
        ).forEach { usage ->
            assertTrue("usage $usage", isOwnVibrationSource(AudioManager.MODE_NORMAL, listOf(AudioAttributes.USAGE_MEDIA, usage)))
        }
    }

    @Test
    fun aQuietPhoneMediaOrACallInProgressDoNotCount() {
        assertFalse(isOwnVibrationSource(AudioManager.MODE_NORMAL, emptyList()))
        assertFalse("Music", isOwnVibrationSource(AudioManager.MODE_NORMAL, listOf(AudioAttributes.USAGE_MEDIA)))
        assertFalse("A call in progress: a knock must still alert", isOwnVibrationSource(AudioManager.MODE_IN_CALL, listOf(AudioAttributes.USAGE_VOICE_COMMUNICATION)))
        assertFalse(isOwnVibrationSource(AudioManager.MODE_IN_COMMUNICATION, emptyList()))
    }
}
