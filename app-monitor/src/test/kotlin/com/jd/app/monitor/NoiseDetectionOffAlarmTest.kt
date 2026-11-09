package com.jd.app.monitor

import com.jd.core.shared.AlertSettings
import com.jd.core.shared.LocationUpdate
import com.jd.core.shared.ViolationEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 163 (ISSUE-NOV8.103-O60): a Sensor that cannot detect noise (after an unattended restart, denied, silenced, or
 * failed microphone) raises the Monitor's `Tracker noise detection off` warning, switched by
 * `Acoustic / Microphone Noise Sensor` (`AlertSettings.acousticAlert`), and the warning clears when the Sensor listens again.
 */
class NoiseDetectionOffAlarmTest {

    private class Watch(scope: CoroutineScope) {
        val target = MutableStateFlow<String?>("T")
        val telemetry = MutableSharedFlow<LocationUpdate>(extraBufferCapacity = 16)
        val violations = MutableSharedFlow<ViolationEvent>(extraBufferCapacity = 16)
        val settings = MutableSharedFlow<AlertSettings>(replay = 1)
        val ticks = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
        val acks = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
        val latest = MutableStateFlow<AlarmBook?>(null)
        val job = scope.launch {
            alarmBooks(target, telemetry, violations, settings, ticks, acks, MutableStateFlow(true), MutableStateFlow(true)) { 1_000_000L }
                .collect { latest.value = it }
        }

        suspend fun ready() = withTimeout(5.seconds) {
            listOf(telemetry, violations, ticks, acks).forEach { flow -> flow.subscriptionCount.first { it > 0 } }
        }

        suspend fun awaitBook(predicate: (AlarmBook) -> Boolean): AlarmBook =
            withTimeout(5.seconds) { latest.filterNotNull().first(predicate) }
    }

    private fun row(noiseOff: Boolean, timestamp: Long = 1L) = LocationUpdate(
        deviceId = "T", lat = 32.0, lng = 34.0, accuracy = 5f, batteryLevel = 80, isCharging = true,
        tempCelsius = 30f, timestamp = timestamp, isNoiseDetectionOff = noiseOff
    )

    @Test
    fun `a row with noise detection off raises the warning`() = runBlocking {
        val watch = Watch(this)
        watch.settings.emit(AlertSettings())
        watch.ready()

        watch.telemetry.emit(row(noiseOff = true))

        val book = watch.awaitBook { it.needsSiren }
        assertEquals(listOf(AlarmType.NOISE_DETECTION_OFF), book.pending.map { it.type })
        assertEquals("Tracker noise detection off", AlarmType.NOISE_DETECTION_OFF.title)
        watch.job.cancel()
    }

    @Test
    fun `the warning clears once the Sensor listens again and goes after acknowledgement`() = runBlocking {
        val watch = Watch(this)
        watch.settings.emit(AlertSettings())
        watch.ready()

        watch.telemetry.emit(row(noiseOff = true, timestamp = 1L))
        watch.awaitBook { it.needsSiren }
        watch.telemetry.emit(row(noiseOff = false, timestamp = 2L))
        val cleared = watch.awaitBook { book -> book.alarms.any { it.type == AlarmType.NOISE_DETECTION_OFF && !it.isActive } }
        assertTrue(cleared.needsSiren)

        watch.acks.emit(Unit)
        assertTrue(watch.awaitBook { !it.needsSiren }.alarms.isEmpty())
        watch.job.cancel()
    }

    @Test
    fun `the noise switch also silences the warning`() = runBlocking {
        val watch = Watch(this)
        watch.settings.emit(AlertSettings(acousticAlert = false))
        watch.ready()

        watch.telemetry.emit(row(noiseOff = true))
        watch.ticks.emit(Unit)

        assertFalse(watch.awaitBook { it.alarms.isEmpty() }.needsSiren)
        assertFalse(AlertSettings(acousticAlert = false).allows(AlarmType.NOISE_DETECTION_OFF))
        assertFalse(AlertSettings(globalMute = true).allows(AlarmType.NOISE_DETECTION_OFF))
        watch.job.cancel()
    }

    @Test
    fun `a row from an older Sensor, without the flag, raises nothing`() {
        val reading = LocationUpdate(deviceId = "T", lat = 32.0, lng = 34.0, accuracy = 5f, batteryLevel = 80, isCharging = true, tempCelsius = 30f)
            .toTrackerReading(1_000L)
        assertFalse(reading.isNoiseDetectionOff)
        assertEquals(emptySet<AlarmType>(), alarmConditions(reading, watchingSinceMs = 0L, nowMs = 1_000L, active = emptySet()))
        assertTrue(row(noiseOff = true).toTrackerReading(1_000L).isNoiseDetectionOff)
    }
}
