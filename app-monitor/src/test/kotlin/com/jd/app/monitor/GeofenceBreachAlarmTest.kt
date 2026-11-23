package com.jd.app.monitor

import com.jd.core.shared.AlertSettings
import com.jd.core.shared.GEOFENCE_BREACH_STATUS
import com.jd.core.shared.LocationUpdate
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
 * Chapter 156 (ISSUE-NOV8.13-O51): the relay client delivers the breach as the status of a telemetry row, its only
 * path since SIMP-NOV8.13-S2; the Monitor raises AlarmType.GEOFENCE_BREACH and sounds the siren.
 */
class GeofenceBreachAlarmTest {

    private class Watch(scope: CoroutineScope) {
        val target = MutableStateFlow<String?>("T")
        val telemetry = MutableSharedFlow<LocationUpdate>(extraBufferCapacity = 16)
        val settings = MutableSharedFlow<AlertSettings>(replay = 1)
        val ticks = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
        val acks = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
        val isRelayConnected = MutableStateFlow(true)
        val isLocalInternet = MutableStateFlow(true)
        @Volatile var now = 1_000_000L
        val latest = MutableStateFlow<AlarmBook?>(null)
        val job = scope.launch {
            alarmBooks(target, telemetry, settings, ticks, acks, isRelayConnected, isLocalInternet) { now }.collect { latest.value = it }
        }

        suspend fun ready() = withTimeout(5.seconds) {
            listOf(telemetry, ticks, acks).forEach { flow -> flow.subscriptionCount.first { it > 0 } }
        }

        suspend fun awaitBook(predicate: (AlarmBook) -> Boolean): AlarmBook =
            withTimeout(5.seconds) { latest.filterNotNull().first(predicate) }
    }

    private fun row(status: String, timestamp: Long) = LocationUpdate(
        deviceId = "T", lat = 32.003, lng = 34.0, accuracy = 5f, batteryLevel = 80, isCharging = true,
        tempCelsius = 30f, status = status, timestamp = timestamp
    )

    /** SIMP-NOV8.13-S2: the breach row is the alarm's only path, so the alarm outlasts the row back home. */
    @Test
    fun `geofence breach alarm stays after the tracker is back until acknowledged`() = runBlocking {
        val watch = Watch(this)
        watch.settings.emit(AlertSettings())
        watch.ready()

        watch.telemetry.emit(row(GEOFENCE_BREACH_STATUS, timestamp = 1L))
        watch.awaitBook { it.needsSiren }
        watch.telemetry.emit(row("VALID", timestamp = 2L))

        val book = watch.awaitBook { book -> book.alarms.any { it.type == AlarmType.GEOFENCE_BREACH && !it.isActive } }
        assertEquals(listOf(AlarmType.GEOFENCE_BREACH), book.pending.map { it.type })
        assertTrue(book.needsSiren)
        watch.acks.emit(Unit)
        watch.awaitBook { !it.needsSiren }
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
                tempCelsius = 30f, status = GEOFENCE_BREACH_STATUS
            )
        )

        val book = watch.awaitBook { it.needsSiren }
        assertEquals(listOf(AlarmType.GEOFENCE_BREACH), book.pending.map { it.type })
        watch.job.cancel()
    }
}
