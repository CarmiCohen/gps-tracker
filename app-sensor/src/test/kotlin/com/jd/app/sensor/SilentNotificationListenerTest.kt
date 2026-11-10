package com.jd.app.sensor

import android.app.NotificationManager
import android.media.AudioManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** ISSUE-NOV9.4-O65: which posted notifications make the Sensor phone vibrate. */
class SilentNotificationListenerTest {

    private fun vibrates(
        isOwnPackage: Boolean = false,
        isOngoing: Boolean = false,
        importance: Int = NotificationManager.IMPORTANCE_DEFAULT,
        channelVibrates: Boolean = true,
        ringerMode: Int = AudioManager.RINGER_MODE_VIBRATE,
        matchesInterruptionFilter: Boolean = true
    ) = notificationWouldVibrate(isOwnPackage, isOngoing, importance, channelVibrates, ringerMode, matchesInterruptionFilter)

    @Test
    fun anotherAppsVibratingNotificationCounts() {
        assertTrue(vibrates())
        assertTrue("Normal ringer", vibrates(ringerMode = AudioManager.RINGER_MODE_NORMAL))
        assertTrue("High importance", vibrates(importance = NotificationManager.IMPORTANCE_HIGH))
    }

    @Test
    fun notificationsThatDoNotVibrateDoNotCount() {
        assertFalse("The Sensor's own", vibrates(isOwnPackage = true))
        assertFalse("Ongoing", vibrates(isOngoing = true))
        assertFalse("Low importance", vibrates(importance = NotificationManager.IMPORTANCE_LOW))
        assertFalse("Channel without vibration", vibrates(channelVibrates = false))
        assertFalse("Silent ringer", vibrates(ringerMode = AudioManager.RINGER_MODE_SILENT))
        assertFalse("Held back by Do Not Disturb", vibrates(matchesInterruptionFilter = false))
    }

    @Test
    fun aVibratingNotificationIsTheOwnVibrationSource() {
        assertTrue(isOwnVibrationSource(AudioManager.MODE_NORMAL, emptyList(), isNotificationVibrating = true))
        assertFalse(isOwnVibrationSource(AudioManager.MODE_NORMAL, emptyList(), isNotificationVibrating = false))
    }
}
