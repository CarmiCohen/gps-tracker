package com.jd.app.monitor

import com.jd.core.shared.ACOUSTIC_ALERT_STATUS
import com.jd.core.shared.AlertSettings
import com.jd.core.shared.GEOFENCE_BREACH_STATUS
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
 * Chapter 161 (ISSUE-NOV5.2-O11 Group 4): a noise the Sensor reports raises the Monitor's `Tracker noise detected`
 * alarm, switched by `Acoustic / Microphone Noise Sensor` (`AlertSettings.acousticAlert`).
 */
class AcousticAlarmTest {

    private class Watch(scope: CoroutineScope) {
        val target = MutableStateFlow<String?>("T")
        val telemetry = MutableSharedFlow<LocationUpdate>(extraBufferCapacity = 16)
        val violations = MutableSharedFlow<ViolationEvent>(extraBufferCapacity = 16)
        val settings = MutableSharedFlow<AlertSettings>(replay = 1)
        val ticks = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
        val acks = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
        @Volatile var now = 1_000_000L
        val latest = MutableStateFlow<AlarmBook?>(null)
        val job = scope.launch {
            alarmBooks(target, telemetry, violations, settings, ticks, acks, MutableStateFlow(true), MutableStateFlow(true)) { now }
                .collect { latest.value = it }
        }

        suspend fun ready() = withTimeout(5.seconds) {
            listOf(telemetry, violations, ticks, acks).forEach { flow -> flow.subscriptionCount.first { it > 0 } }
        }

        suspend fun awaitBook(predicate: (AlarmBook) -> Boolean): AlarmBook =
            withTimeout(5.seconds) { latest.filterNotNull().first(predicate) }
    }

    private fun row(status: String) = LocationUpdate(
        deviceId = "T", lat = 32.0, lng = 34.0, accuracy = 5f, batteryLevel = 80, isCharging = true,
        tempCelsius = 30f, status = status
    )

    @Test
    fun `acoustic violation raises the noise alarm, not a GPS jump`() = runBlocking {
        val watch = Watch(this)
        watch.settings.emit(AlertSettings())
        watch.ready()

        watch.violations.emit(ViolationEvent(type = ACOUSTIC_ALERT_STATUS, location = LocationUpdate(deviceId = "T"), nonce = "n1"))

        val book = watch.awaitBook { it.needsSiren }
        assertEquals(listOf(AlarmType.ACOUSTIC_ALERT), book.pending.map { it.type })
        watch.job.cancel()
    }

    @Test
    fun `acoustic telemetry status raises the noise alarm`() = runBlocking {
        val watch = Watch(this)
        watch.settings.emit(AlertSettings())
        watch.ready()

        watch.telemetry.emit(row(ACOUSTIC_ALERT_STATUS))

        val book = watch.awaitBook { it.needsSiren }
        assertEquals(listOf(AlarmType.ACOUSTIC_ALERT), book.pending.map { it.type })
        watch.job.cancel()
    }

    @Test
    fun `the noise alarm stays until acknowledged after the next normal row`() = runBlocking {
        val watch = Watch(this)
        watch.settings.emit(AlertSettings())
        watch.ready()

        watch.telemetry.emit(row(ACOUSTIC_ALERT_STATUS))
        watch.awaitBook { it.needsSiren }
        watch.telemetry.emit(row("VALID"))
        val cleared = watch.awaitBook { book -> book.alarms.any { it.type == AlarmType.ACOUSTIC_ALERT && !it.isActive } }
        assertTrue(cleared.needsSiren)

        watch.acks.emit(Unit)
        assertFalse(watch.awaitBook { !it.needsSiren }.needsSiren)
        watch.job.cancel()
    }

    @Test
    fun `switched off noise alarm stays silent`() = runBlocking {
        val watch = Watch(this)
        watch.settings.emit(AlertSettings(acousticAlert = false))
        watch.ready()

        watch.telemetry.emit(row(ACOUSTIC_ALERT_STATUS))
        watch.violations.emit(ViolationEvent(type = ACOUSTIC_ALERT_STATUS, location = LocationUpdate(deviceId = "T"), nonce = "n2"))
        watch.ticks.emit(Unit)

        val book = watch.awaitBook { it.alarms.isEmpty() }
        assertFalse(book.needsSiren)
        watch.job.cancel()
    }

    @Test
    fun `noise and geofence breach together raise both alarms`() {
        val conditions = alarmConditions(
            reading = TrackerReading(1_000L, 1_000L, 80, true, 30f, 32.0, 34.0, 5f, ACOUSTIC_ALERT_STATUS),
            watchingSinceMs = 0L,
            nowMs = 1_000L,
            active = emptySet(),
            hasViolation = true,
            violationType = GEOFENCE_BREACH_STATUS
        )
        assertEquals(setOf(AlarmType.GEOFENCE_BREACH, AlarmType.ACOUSTIC_ALERT), conditions)
    }

    @Test
    fun `the noise switch allows only the noise alarm`() {
        val off = AlertSettings(acousticAlert = false)
        assertFalse(off.allows(AlarmType.ACOUSTIC_ALERT))
        assertTrue(off.allows(AlarmType.GEOFENCE_BREACH))
        assertFalse(AlertSettings(globalMute = true).allows(AlarmType.ACOUSTIC_ALERT))
    }
}
