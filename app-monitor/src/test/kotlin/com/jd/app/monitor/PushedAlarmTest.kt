package com.jd.app.monitor

import com.jd.core.shared.ACOUSTIC_ALERT_STATUS
import com.jd.core.shared.AlertSettings
import com.jd.core.shared.GEOFENCE_BREACH_STATUS
import com.jd.core.shared.LocationUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Chapter 180 (ISSUE-NOV10.8-O86): an alarm row that arrives by push raises its alarm at once, and the same row
 * arriving later by the relay connection, behind older rows, does not raise it again.
 */
class PushedAlarmTest {

    /** The alarm book fed as [MonitorAlarmEngine] feeds it: the relay connection merged with the pushed rows. */
    private class Watch(scope: CoroutineScope, val pushed: PushedRows = PushedRows()) {
        val relay = MutableSharedFlow<LocationUpdate>(extraBufferCapacity = 16)
        val settings = MutableSharedFlow<AlertSettings>(replay = 1)
        val acks = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
        @Volatile var now = 1_000_000L
        val books = CopyOnWriteArrayList<AlarmBook>()
        val latest = MutableStateFlow<AlarmBook?>(null)
        val job = scope.launch {
            alarmBooks(
                target = MutableStateFlow<String?>("T"),
                telemetry = merge(relay, pushed.fresh { now }),
                settings = settings,
                ticks = emptyFlow(),
                acknowledgements = acks,
                clock = { now }
            ).collect {
                books += it
                latest.value = it
            }
        }

        suspend fun ready() = withTimeout(5.seconds) {
            listOf(relay, acks).forEach { flow -> flow.subscriptionCount.first { it > 0 } }
        }

        fun push(row: LocationUpdate) = pushed.offer(row, "T", now)

        suspend fun awaitBook(predicate: (AlarmBook) -> Boolean): AlarmBook =
            withTimeout(5.seconds) { latest.filterNotNull().first(predicate) }

        /** How many times the book showed an unacknowledged noise alarm. */
        fun noiseAlarmBooks() = books.count { book -> book.pending.any { it.type == AlarmType.ACOUSTIC_ALERT } }
    }

    private fun row(status: String, sentAtMs: Long) = LocationUpdate(
        deviceId = "T", lat = 32.0, lng = 34.0, accuracy = 5f, batteryLevel = 80, isCharging = true,
        tempCelsius = 30f, status = status, timestamp = sentAtMs
    )

    private val earlier = row("VALID", sentAtMs = 1_000L)
    private val alarm = row(ACOUSTIC_ALERT_STATUS, sentAtMs = 2_000L)
    private val later = row("VALID", sentAtMs = 3_000L)
    private val breach = row(GEOFENCE_BREACH_STATUS, sentAtMs = 4_000L)

    @Test
    fun `a pushed alarm row raises its alarm before the relay delivers it`() = runBlocking {
        val watch = Watch(this)
        watch.settings.emit(AlertSettings())
        watch.ready()

        watch.push(alarm)

        val book = watch.awaitBook { it.needsSiren }
        assertEquals(listOf(AlarmType.ACOUSTIC_ALERT), book.pending.map { it.type })
        watch.job.cancel()
    }

    @Test
    fun `the late relay rows do not raise an acknowledged pushed alarm again`() = runBlocking {
        val watch = Watch(this)
        watch.settings.emit(AlertSettings())
        watch.ready()

        watch.push(alarm)
        watch.awaitBook { it.needsSiren }
        watch.acks.emit(Unit)
        watch.awaitBook { book -> book.alarms.any { it.isAcknowledged && it.isActive } }

        // The delayed connection now delivers what it held back: an older row, the alarm row itself, then newer rows.
        watch.now += 2_000L
        watch.relay.emit(earlier)
        watch.relay.emit(alarm.copy())
        watch.relay.emit(later)
        watch.relay.emit(breach)

        val book = watch.awaitBook { it.needsSiren }
        assertEquals("The newer row cleared the acknowledged noise alarm", listOf(AlarmType.GEOFENCE_BREACH), book.alarms.map { it.type })
        assertEquals("The noise alarm was raised once, by the push", 1, watch.noiseAlarmBooks())
        watch.job.cancel()
    }

    @Test
    fun `a push that found the session dead is raised once the engine starts`() = runBlocking {
        val pushed = PushedRows()
        pushed.offer(alarm, "T", receivedAtMs = 1_000_000L)

        val watch = Watch(this, pushed)
        watch.now = 1_003_000L // The service and the engine took three seconds to start.
        watch.settings.emit(AlertSettings())

        val book = watch.awaitBook { it.needsSiren }
        assertEquals(listOf(AlarmType.ACOUSTIC_ALERT), book.pending.map { it.type })
        watch.job.cancel()
    }

    @Test
    fun `a push that waited too long for the engine is not raised`() = runBlocking {
        val pushed = PushedRows()
        pushed.offer(alarm, "T", receivedAtMs = 1_000_000L)

        val watch = Watch(this, pushed)
        watch.now = 1_000_000L + PUSHED_ROW_FRESH_MS + 1
        watch.settings.emit(AlertSettings())
        watch.ready()
        watch.relay.emit(breach)

        val book = watch.awaitBook { it.needsSiren }
        assertEquals(listOf(AlarmType.GEOFENCE_BREACH), book.pending.map { it.type })
        assertEquals(0, watch.noiseAlarmBooks())
        watch.job.cancel()
    }
}
