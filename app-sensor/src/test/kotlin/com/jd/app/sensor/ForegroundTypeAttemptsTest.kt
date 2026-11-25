package com.jd.app.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Chapter 163 (ISSUE-NOV9.1-O61): the foreground service types the Sensor tries. On API 34+ the base is
 * `remoteMessaging` (0x200) without `dataSync` (0x1), which Android 15 forbids from `BOOT_COMPLETED` at `targetSdk` 35+.
 * Values: dataSync 0x1, location 0x8, microphone 0x80, remoteMessaging 0x200.
 */
class ForegroundTypeAttemptsTest {

    @Test
    fun api34AndLaterNeverUseDataSync() {
        for (sdk in listOf(34, 35, 36)) {
            assertEquals(listOf(0x288, 0x208, 0x200), SensorService.foregroundTypeAttempts(sdk, withMicrophone = true))
            assertEquals(listOf(0x208, 0x200), SensorService.foregroundTypeAttempts(sdk, withMicrophone = false))
            SensorService.foregroundTypeAttempts(sdk, withMicrophone = true).forEach { assertEquals(0, it and 0x1) }
        }
    }

    @Test
    fun belowApi34DataSyncStaysTheBase() {
        // remoteMessaging does not exist before API 34.
        assertEquals(listOf(0x89, 0x9, 0x1), SensorService.foregroundTypeAttempts(30, withMicrophone = true))
        assertEquals(listOf(0x9, 0x1), SensorService.foregroundTypeAttempts(29, withMicrophone = false))
    }

    @Test
    fun manifestDeclaresEveryTypeTried() {
        val manifest = File("src/main/AndroidManifest.xml").readText(Charsets.UTF_8)
        val service = Regex("""<service\s+android:name="\.SensorService".*?/>""", RegexOption.DOT_MATCHES_ALL).find(manifest)!!.value
        val types = Regex("""android:foregroundServiceType="([^"]+)"""").find(service)!!.groupValues[1].split('|').toSet()
        assertTrue(types.containsAll(listOf("location", "remoteMessaging", "dataSync", "microphone")))
    }
}
