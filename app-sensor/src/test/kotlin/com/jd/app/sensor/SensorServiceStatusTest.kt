package com.jd.app.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Chapter 138 (ISSUE-NOV6.4-O34, updated Nov7.34 for ISSUE-NOV7.2-O36): the Sensor Setup dialog and console show the service's real state and uptime. */
class SensorServiceStatusTest {

    @Test
    fun aNewStatusIsStopped() {
        val status = SensorServiceStatus()
        assertFalse(status.isRunning.value)
        assertEquals(0L, status.startTimeMillis.value)
    }

    @Test
    fun markRunningThenStoppedFollowsTheService() {
        val status = SensorServiceStatus()

        status.markRunning(1000L)
        assertTrue(status.isRunning.value)
        assertEquals(1000L, status.startTimeMillis.value)

        status.markStopped()
        assertFalse(status.isRunning.value)
        assertEquals(0L, status.startTimeMillis.value)
    }

    @Test
    fun resetUptimeResetsStartTimeWhenRunning() {
        val status = SensorServiceStatus()
        status.markRunning(1000L)
        assertEquals(1000L, status.startTimeMillis.value)

        status.resetUptime(5000L)
        assertEquals(5000L, status.startTimeMillis.value)

        status.markStopped()
        status.resetUptime(6000L)
        assertEquals(0L, status.startTimeMillis.value)
    }

    @Test
    fun uptimeFormattingIsCorrect() {
        assertEquals("00:00:00", formatUptime(0L, 1000L))
        assertEquals("00:00:00", formatUptime(1000L, 500L))
        assertEquals("00:01:04", formatUptime(1000L, 65000L))
        assertEquals("01:01:00", formatUptime(1000L, 3661000L))
    }

    @Test
    fun serviceLineShowsRunningOrStoppedWithUptime() {
        assertEquals("Sensor Service: Running", serviceStatusText(true))
        assertEquals("Sensor Service: Running (00:01:04)", serviceStatusText(true, "00:01:04"))
        assertEquals("Sensor Service: Stopped", serviceStatusText(false))
        assertEquals("Sensor Service: Stopped", serviceStatusText(false, "00:01:04"))
    }

    @Test
    fun relayLineShowsConnectedOrDisconnected() {
        assertEquals("Relay Connection: Connected", relayStatusText(true))
        assertEquals("Relay Connection: Disconnected", relayStatusText(false))
    }

    /** Chapter 161 (ISSUE-NOV5.2-O11 Group 4) and chapter 51: the microphone line, with "Audio Denied" without the permission. */
    @Test
    fun microphoneLineFollowsTheServiceAndIsOffWhenStopped() {
        val status = SensorServiceStatus()
        assertEquals(MicrophoneState.OFF, status.microphone.value)

        status.markRunning(1000L)
        status.setMicrophone(MicrophoneState.LISTENING)
        assertEquals("Microphone: Listening", microphoneStatusText(status.microphone.value))

        status.markStopped()
        assertEquals(MicrophoneState.OFF, status.microphone.value)
        assertEquals("Microphone: Audio Denied", microphoneStatusText(MicrophoneState.NO_PERMISSION))
        assertEquals(
            "Microphone: Off after a restart: tap its notification or open the Sensor app",
            microphoneStatusText(MicrophoneState.NEEDS_APP_OPEN)
        )
    }

    /** Chapter 163 (ISSUE-NOV8.103-O60): which microphone states tell the Monitor that noise detection is off. */
    @Test
    fun noiseDetectionIsReportedOffOnlyWhenTheRunningServiceCannotListen() {
        val off = MicrophoneState.entries.filter { it.isNoiseDetectionOff }.toSet()
        assertEquals(
            setOf(MicrophoneState.NO_PERMISSION, MicrophoneState.NEEDS_APP_OPEN, MicrophoneState.SILENCED, MicrophoneState.UNAVAILABLE),
            off
        )
        // OFF is a stopped or starting service, which sends no row or has not opened the microphone yet.
        assertFalse(MicrophoneState.OFF.isNoiseDetectionOff)
        assertFalse(MicrophoneState.LISTENING.isNoiseDetectionOff)
    }

    /** Chapter 163 (ISSUE-NOV8.103-O60): what a background start was refused picks the tap-to-enable notification. */
    @Test
    fun refusedAccessPicksTheRestartNotificationAndClearsWhenStopped() {
        assertEquals("Tracking off - tap to enable", restartNotificationTitle(RefusedAccess(location = true, microphone = true)))
        assertEquals("Tracking off - tap to enable", restartNotificationTitle(RefusedAccess(location = true)))
        assertEquals("Noise detection off - tap to enable", restartNotificationTitle(RefusedAccess(microphone = true)))
        assertEquals(null, restartNotificationTitle(RefusedAccess()))
        assertFalse(RefusedAccess().isAny)

        val status = SensorServiceStatus()
        status.markRunning(1000L)
        status.setRefusedAccess(RefusedAccess(location = true, microphone = true))
        assertTrue(status.refusedAccess.value.isAny)
        status.markStopped()
        assertEquals(RefusedAccess(), status.refusedAccess.value)
    }

    /** Chapter 163 (ISSUE-NOV8.103-O60): the "Allow all the time" line. */
    @Test
    fun backgroundLocationLineTellsWhetherARestartNeedsATap() {
        assertEquals("Location after a restart: Automatic (allowed all the time)", backgroundLocationStatusText(true))
        assertEquals("Location after a restart: Needs a tap on the notification", backgroundLocationStatusText(false))
    }

    /** Chapter 162 (ISSUE-NOV5.2-O11 Group 5): the tilt and lift line. */
    @Test
    fun motionLineShowsArmedStateAndSensorsAndIsOffWhenStopped() {
        val status = SensorServiceStatus()
        assertEquals("Tilt & Lift: Off", motionStatusText(status.motion.value))

        status.setMotion(MotionState(isRunning = true))
        assertEquals("Tilt & Lift: Arms after the phone rests 30 s", motionStatusText(status.motion.value))
        status.setMotion(MotionState(isRunning = true, isArmed = true))
        assertEquals("Tilt & Lift: Armed (accelerometer)", motionStatusText(status.motion.value))
        assertEquals("Tilt & Lift: Armed (accelerometer + barometer)", motionStatusText(MotionState(true, true, true)))

        status.markStopped()
        assertEquals(MotionState(), status.motion.value)
    }

    @Test
    fun microphoneIsAskedForButNotRequired() {
        assertEquals(android.Manifest.permission.RECORD_AUDIO, OPTIONAL_MICROPHONE_PERMISSION)
        assertFalse(getRequiredSensorPermissions(android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE).contains(OPTIONAL_MICROPHONE_PERMISSION))
    }

    @Test
    fun noLineClaimsAnInventedReadyState() {
        listOf(serviceStatusText(true), serviceStatusText(false), relayStatusText(true), relayStatusText(false))
            .forEach { assertFalse(it, it.contains("Ready") || it.contains("Active")) }
    }
}

