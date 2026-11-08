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
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter 155 (ISSUE-NOV5.2-O11 Group 2): Geofence Breach Alarm Verification.
 * Verifies that when a GEOFENCE_BREACH violation event is received from the Sensor,
 * the Monitor alarm engine raises AlarmType.GEOFENCE_BREACH and sounds the siren.
 * Chapter 156 (ISSUE-NOV8.13-O51): the relay client delivers the breach as the status of a telemetry row,
 * so that path is verified too.
 */
class GeofenceBreachAlarmTest {

    private class Watch(scope: CoroutineScope) {
        val target = MutableStateFlow<String?>("T")
        val telemetry = MutableSharedFlow<LocationUpdate>(extraBufferCapacity = 16)
        val violations = MutableSharedFlow<ViolationEvent>(extraBufferCapacity = 16)
        val settings = MutableSharedFlow<AlertSettings>(replay = 1)
        val ticks = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
        val acks = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
        val isRelayConnected = MutableStateFlow(true)
        val isLocalInternet = MutableStateFlow(true)
        @Volatile var now = 1_000_000L
        val latest = MutableStateFlow<AlarmBook?>(null)
        val job = scope.launch {
            alarmBooks(target, telemetry, violations, settings, ticks, acks, isRelayConnected, isLocalInternet) { now }.collect { latest.value = it }
        }

        suspend fun ready() = withTimeout(5.seconds) {
            listOf(telemetry, violations, ticks, acks).forEach { flow -> flow.subscriptionCount.first { it > 0 } }
        }

        suspend fun awaitBook(predicate: (AlarmBook) -> Boolean): AlarmBook =
            withTimeout(5.seconds) { latest.filterNotNull().first(predicate) }
    }

    @Test
    fun `geofence breach violation raises geofence breach alarm`() = runBlocking {
        val watch = Watch(this)
        watch.settings.emit(AlertSettings())
        watch.ready()

        val violation = ViolationEvent(
            type = "GEOFENCE_BREACH",
            location = LocationUpdate(deviceId = "T", lat = 38.0, lng = -122.0),
            nonce = "geofence-nonce-1"
        )
        watch.violations.emit(violation)

        val book = watch.awaitBook { it.needsSiren }
        assertEquals(listOf(AlarmType.GEOFENCE_BREACH), book.pending.map { it.type })
        assertTrue(book.needsSiren)
        watch.job.cancel()
    }

    @Test
    fun `geofence breach telemetry status raises geofence breach alarm`() = runBlocking {
        val watch = Watch(this)
        watch.settings.emit(AlertSettings())
        watch.ready()

        watch.telemetry.emit(
            LocationUpdate(
                deviceId = "T", lat = 32.003, lng = 34.0, accuracy = 5f, batteryLevel = 80, isCharging = true,
                tempCelsius = 30f, status = com.jd.core.shared.GEOFENCE_BREACH_STATUS
            )
        )

        val book = watch.awaitBook { it.needsSiren }
        assertEquals(listOf(AlarmType.GEOFENCE_BREACH), book.pending.map { it.type })
        watch.job.cancel()
    }
}
