package com.jd.app.sensor

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Chapter 185 (ISSUE-NOV10.8-O81): the Sensor's Phone Setup lists only what the Sensor needs, for this phone. */
class SensorPhoneSetupTest {

    private val allGranted = SensorSetupStatus(
        preciseLocation = true,
        backgroundLocation = true,
        microphone = true,
        notifications = true,
        batteryUnrestricted = true,
        keptWhenUnused = true,
        notificationAccess = true
    )

    @Test
    fun samsungOnAndroid14ShowsAllEightItemsInOrder() {
        assertEquals(
            listOf(
                SensorSetupItem.PRECISE_LOCATION,
                SensorSetupItem.BACKGROUND_LOCATION,
                SensorSetupItem.MICROPHONE,
                SensorSetupItem.NOTIFICATIONS,
                SensorSetupItem.BATTERY_UNRESTRICTED,
                SensorSetupItem.KEEP_WHEN_UNUSED,
                SensorSetupItem.SAMSUNG_NEVER_SLEEPING,
                SensorSetupItem.NOTIFICATION_ACCESS
            ),
            sensorSetupItems(Build.VERSION_CODES.UPSIDE_DOWN_CAKE, "samsung")
        )
    }

    @Test
    fun otherMakersHaveNoSamsungItem() {
        val items = sensorSetupItems(Build.VERSION_CODES.UPSIDE_DOWN_CAKE, "Google")
        assertFalse(SensorSetupItem.SAMSUNG_NEVER_SLEEPING in items)
        assertEquals(7, items.size)
    }

    @Test
    fun manufacturerMatchIgnoresCase() {
        assertTrue(SensorSetupItem.SAMSUNG_NEVER_SLEEPING in sensorSetupItems(Build.VERSION_CODES.TIRAMISU, "SAMSUNG"))
    }

    @Test
    fun android8HasNeitherAllTheTimeNorUnusedPause() {
        val items = sensorSetupItems(Build.VERSION_CODES.O, "Google")
        assertFalse(SensorSetupItem.BACKGROUND_LOCATION in items)
        assertFalse(SensorSetupItem.KEEP_WHEN_UNUSED in items)
    }

    @Test
    fun android10HasAllTheTimeButNoUnusedPause() {
        val items = sensorSetupItems(Build.VERSION_CODES.Q, "Google")
        assertTrue(SensorSetupItem.BACKGROUND_LOCATION in items)
        assertFalse(SensorSetupItem.KEEP_WHEN_UNUSED in items)
        assertTrue(SensorSetupItem.KEEP_WHEN_UNUSED in sensorSetupItems(Build.VERSION_CODES.R, "Google"))
    }

    @Test
    fun noMonitorItemExists() {
        val names = SensorSetupItem.entries.map { it.name }
        listOf("OVERLAY", "DND", "FULL_SCREEN", "EXACT_ALARM").forEach { monitorItem ->
            assertTrue(names.none { it.contains(monitorItem) })
        }
    }

    @Test
    fun samsungListCannotBeRead() {
        assertNull(allGranted.isGranted(SensorSetupItem.SAMSUNG_NEVER_SLEEPING))
    }

    @Test
    fun missingCountIgnoresOptionalAndUnreadableItems() {
        val items = sensorSetupItems(Build.VERSION_CODES.UPSIDE_DOWN_CAKE, "samsung")
        assertEquals(0, allGranted.missingCount(items))
        assertEquals(0, allGranted.copy(notificationAccess = false).missingCount(items))
        assertEquals(2, allGranted.copy(microphone = false, batteryUnrestricted = false).missingCount(items))
    }

    @Test
    fun missingCountSkipsItemsThisPhoneDoesNotShow() {
        val items = sensorSetupItems(Build.VERSION_CODES.O, "Google")
        assertEquals(0, allGranted.copy(backgroundLocation = false, keptWhenUnused = false).missingCount(items))
    }

    @Test
    fun consoleLineText() {
        assertEquals("PHONE SETUP: COMPLETE", phoneSetupLineText(0))
        assertEquals("PHONE SETUP: 3 MISSING", phoneSetupLineText(3))
    }
}
