package com.jd.app.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Chapter 186 (ISSUE-NOV12.6-O92): the Sensor starts itself after a reboot and after an app update. */
class BootReceiverTest {

    private val manifest by lazy { File("src/main/AndroidManifest.xml").readText(Charsets.UTF_8) }

    @Test
    fun startsAfterRebootAndUpdate() {
        assertTrue(BootReceiver.startsSensorOn("android.intent.action.BOOT_COMPLETED"))
        assertTrue(BootReceiver.startsSensorOn("android.intent.action.MY_PACKAGE_REPLACED"))
    }

    @Test
    fun ignoresOtherBroadcasts() {
        assertFalse(BootReceiver.startsSensorOn(null))
        // Another app's update must not start the Sensor.
        assertFalse(BootReceiver.startsSensorOn("android.intent.action.PACKAGE_REPLACED"))
        assertFalse(BootReceiver.startsSensorOn("android.intent.action.LOCKED_BOOT_COMPLETED"))
    }

    @Test
    fun manifestDeclaresExactlyTheStartActions() {
        val receiver = Regex("""<receiver\s+android:name="\.BootReceiver".*?</receiver>""", RegexOption.DOT_MATCHES_ALL)
            .find(manifest)?.value ?: error("BootReceiver missing from the manifest")
        val actions = Regex("""<action android:name="([^"]+)"""").findAll(receiver).map { it.groupValues[1] }.toSet()
        assertEquals(BootReceiver.START_ACTIONS, actions)
    }

    @Test
    fun receiverIsNotExported() {
        // Both broadcasts are protected and sent by the system; no other app may trigger a start.
        val receiver = Regex("""<receiver\s+android:name="\.BootReceiver".*?>""", RegexOption.DOT_MATCHES_ALL).find(manifest)!!.value
        assertTrue(receiver.contains("""android:exported="false""""))
    }
}
