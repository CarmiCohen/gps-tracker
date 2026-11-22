package com.jd.app.sensor

import android.os.Build
import com.jd.core.shared.SensorFault
import com.jd.core.shared.SensorSetupItem
import com.jd.core.shared.SensorSetupReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 189 (ISSUE-NOV10.8-O83 part 2): what the Sensor tells the Monitor about its phone: the same missing items as
 * its own Phone Setup line counts, and what the running service lacks although the setup allows it.
 */
class SensorSetupReportTest {

    private val allGranted = SensorSetupStatus(
        preciseLocation = true,
        backgroundLocation = true,
        microphone = true,
        notifications = true,
        batteryUnrestricted = true,
        keptWhenUnused = true,
        notificationAccess = true
    )
    private val samsungItems = sensorSetupItems(Build.VERSION_CODES.UPSIDE_DOWN_CAKE, "samsung")

    private fun report(
        missing: List<SensorSetupItem> = emptyList(),
        microphone: MicrophoneState = MicrophoneState.LISTENING,
        refused: RefusedAccess = RefusedAccess(),
        revision: Long = 5L
    ) = sensorSetupReport(missing, microphone, refused, revision)

    @Test
    fun `a complete setup with a listening microphone reports nothing but the applied revision`() {
        assertEquals(SensorSetupReport(behaviourRevision = 5L), report(allGranted.missingItems(samsungItems)))
    }

    @Test
    fun `the missing items are those the console line counts, in setup order`() {
        val status = allGranted.copy(batteryUnrestricted = false, microphone = false, notificationAccess = false)
        val missing = status.missingItems(samsungItems)
        assertEquals(
            "The optional and the unreadable items are never reported",
            listOf(SensorSetupItem.MICROPHONE, SensorSetupItem.BATTERY_UNRESTRICTED),
            missing
        )
        assertEquals(status.missingCount(samsungItems), missing.size)
        assertEquals(missing, report(missing, MicrophoneState.NO_PERMISSION).missing)
    }

    @Test
    fun `an item this phone does not show is not reported`() {
        val android8 = sensorSetupItems(Build.VERSION_CODES.O, "Google")
        assertTrue(allGranted.copy(backgroundLocation = false, keptWhenUnused = false).missingItems(android8).isEmpty())
    }

    @Test
    fun `each microphone state that needs the Sensor phone is one fault`() {
        assertEquals(listOf(SensorFault.MICROPHONE_NEEDS_TAP), report(microphone = MicrophoneState.NEEDS_APP_OPEN).faults)
        assertEquals(listOf(SensorFault.MICROPHONE_SILENCED), report(microphone = MicrophoneState.SILENCED).faults)
        assertEquals(listOf(SensorFault.MICROPHONE_UNAVAILABLE), report(microphone = MicrophoneState.UNAVAILABLE).faults)
    }

    @Test
    fun `a microphone that is fine, starting, switched off on the Monitor or without permission is no fault`() {
        listOf(MicrophoneState.LISTENING, MicrophoneState.OFF, MicrophoneState.TURNED_OFF, MicrophoneState.NO_PERMISSION).forEach {
            assertTrue("$it", report(microphone = it).faults.isEmpty())
        }
    }

    @Test
    fun `every state with noise detection off reaches the Monitor as a missing item or a fault`() {
        MicrophoneState.entries.filter { it.isNoiseDetectionOff }.forEach { state ->
            val missing = if (state == MicrophoneState.NO_PERMISSION) listOf(SensorSetupItem.MICROPHONE) else emptyList()
            val sent = report(missing, state)
            assertTrue("$state", sent.missing.isNotEmpty() || sent.faults.isNotEmpty())
        }
    }

    @Test
    fun `location refused after a restart comes first, with the microphone fault`() {
        assertEquals(
            listOf(SensorFault.LOCATION_NEEDS_TAP, SensorFault.MICROPHONE_NEEDS_TAP),
            report(microphone = MicrophoneState.NEEDS_APP_OPEN, refused = RefusedAccess(location = true, microphone = true)).faults
        )
        assertEquals(listOf(SensorFault.LOCATION_NEEDS_TAP), report(refused = RefusedAccess(location = true)).faults)
    }

    /** Chapter 237 (ISSUE-NOV22.4-O130): the Monitor shows it only while its light detection is on. */
    @Test
    fun `a phone without a light sensor reports it last`() {
        assertEquals(
            listOf(SensorFault.MICROPHONE_SILENCED, SensorFault.NO_LIGHT_SENSOR),
            sensorSetupReport(emptyList(), MicrophoneState.SILENCED, RefusedAccess(), 5L, hasLightSensor = false).faults
        )
        assertTrue(report().faults.isEmpty())
    }

    @Test
    fun `a new detection revision is a new report, so the Monitor learns it was applied`() {
        assertTrue(report(revision = 5L) != report(revision = 6L))
        assertEquals(6L, report(revision = 6L).behaviourRevision)
    }
}
