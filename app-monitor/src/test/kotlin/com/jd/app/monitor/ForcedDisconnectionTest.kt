package com.jd.app.monitor

import com.jd.core.network.SensorSilence
import com.jd.core.shared.ACOUSTIC_ALERT_STATUS
import com.jd.core.shared.AlertSettings
import com.jd.core.shared.LocationUpdate
import com.jd.core.shared.SilenceCause
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/** Verifies Chapter 250 (ISSUE-NOV22.10-O137): the forced disconnection and the narrowed lost connection. */
class ForcedDisconnectionTest {

    private val start = 1_000_000L
    private val forced = setOf(AlarmType.TRACKER_FORCED_OFFLINE)
    private val lost = setOf(AlarmType.TRACKER_OFFLINE)
    private val none = emptySet<AlarmType>()

    private fun reading(at: Long = start, battery: Int? = 80) = TrackerReading(
        receivedAtMs = at, sensorTimeMs = at, batteryLevel = battery, isCharging = true, tempCelsius = 30f,
        lat = 32.0, lng = 34.0, accuracy = 5f
    )

    private fun conditions(
        reading: TrackerReading? = reading(),
        now: Long = start + 1_000L,
        cause: SilenceCause? = null,
        lastSensorAlarmAtMs: Long? = null,
        isRelayConnected: Boolean = true,
        isLocalInternet: Boolean = true
    ) = alarmConditions(
        reading, watchingSinceMs = start, nowMs = now, active = emptySet(),
        isRelayConnected = isRelayConnected, relayLostSinceMs = start, isLocalInternet = isLocalInternet,
        silenceCause = cause, lastSensorAlarmAtMs = lastSensorAlarmAtMs
    )

    @Test
    fun eachForcedCauseRaisesTheForcedDisconnectionAtOnce() {
        SilenceCause.entries.filter { it.isForced }.forEach { cause ->
            assertEquals(cause.name, forced, conditions(cause = cause))
        }
        assertEquals(
            listOf(SilenceCause.CLOSED, SilenceCause.SHUTDOWN, SilenceCause.AIRPLANE_MODE, SilenceCause.SIM_REMOVED, SilenceCause.SERVICE_STOPPED),
            SilenceCause.entries.filter { it.isForced }
        )
    }

    @Test
    fun aPlainSilenceIsTheLostConnectionAndTheMonitorsOwnTimeoutToo() {
        assertEquals(lost, conditions(cause = SilenceCause.SILENT))
        assertEquals(lost, conditions(now = start + TRACKER_PRESENT_MS))
        assertEquals(none, conditions(now = start + TRACKER_PRESENT_MS - 1))
    }

    @Test
    fun theOwnersStopRaisesNothingAndAnEmptyBatteryOnlyTheBatteryAlarm() {
        assertEquals(none, conditions(cause = SilenceCause.OWNER_STOP))
        assertEquals(none, conditions(cause = SilenceCause.OWNER_STOP, now = start + 10 * TRACKER_PRESENT_MS))
        assertEquals(setOf(AlarmType.LOW_BATTERY), conditions(cause = SilenceCause.BATTERY_EMPTY))
        assertEquals(setOf(AlarmType.LOW_BATTERY), conditions(reading(battery = null), cause = SilenceCause.BATTERY_EMPTY))
    }

    @Test
    fun aSilenceSoonAfterASensorAlarmIsForced() {
        val silent = start + TRACKER_PRESENT_MS
        assertEquals(forced, conditions(now = silent, lastSensorAlarmAtMs = start))
        assertEquals(forced, conditions(now = silent, lastSensorAlarmAtMs = start - FORCED_AFTER_ALARM_MS))
        assertEquals(lost, conditions(now = silent, lastSensorAlarmAtMs = start - FORCED_AFTER_ALARM_MS - 1))
        assertEquals(forced, conditions(cause = SilenceCause.SILENT, lastSensorAlarmAtMs = start - 60_000L))
        assertEquals("A heard tracker is no silence", none, conditions(lastSensorAlarmAtMs = start))
    }

    @Test
    fun withTheMonitorsOwnPathDownOnlyThatPathsAlarm() {
        val silent = start + TRACKER_PRESENT_MS
        assertEquals(setOf(AlarmType.LOCAL_INTERNET_OFFLINE), conditions(now = silent, isLocalInternet = false))
        assertEquals(
            setOf(AlarmType.LOCAL_INTERNET_OFFLINE),
            conditions(cause = SilenceCause.SHUTDOWN, isLocalInternet = false)
        )
        assertEquals(setOf(AlarmType.RELAY_DISCONNECTED), conditions(now = silent, isRelayConnected = false))
        assertEquals("Within the relay's reconnect grace: nothing", none, conditions(now = start + 1_000L, cause = SilenceCause.CLOSED, isRelayConnected = false))
    }

    @Test
    fun theForcedAlarmIsTheHighestPriorityAndFollowsTheConnectionSwitch() {
        assertEquals(AlarmType.TRACKER_FORCED_OFFLINE, AlarmType.entries.first())
        assertFalse(AlarmType.TRACKER_FORCED_OFFLINE.isEvent)
        assertFalse(AlertSettings(serverConnection = false).allows(AlarmType.TRACKER_FORCED_OFFLINE))
        assertTrue(AlertSettings().allows(AlarmType.TRACKER_FORCED_OFFLINE))
    }

    // The book flow: the notice's cause, and the rows that end it.

    private class Watch(scope: kotlinx.coroutines.CoroutineScope) {
        var now = 2_000_000L
        val relay = MutableSharedFlow<LocationUpdate>()
        val silences = MutableSharedFlow<SensorSilence>()
        val settings = MutableStateFlow(AlertSettings())
        val latest = MutableStateFlow<AlarmBook?>(null)
        val job = scope.launch {
            alarmBooks(
                target = MutableStateFlow<String?>("T"),
                telemetry = relay,
                silences = silences,
                settings = settings,
                ticks = emptyFlow(),
                acknowledgements = emptyFlow(),
                clock = { now }
            ).collect { latest.value = it }
        }

        suspend fun ready() = withTimeout(5.seconds) {
            listOf(relay, silences).forEach { flow -> flow.subscriptionCount.first { it > 0 } }
        }

        suspend fun awaitBook(predicate: (AlarmBook) -> Boolean): AlarmBook =
            withTimeout(5.seconds) { latest.filterNotNull().first(predicate) }
    }

    private fun row(sentAtMs: Long, status: String = "VALID") = LocationUpdate(
        deviceId = "T", lat = 32.0, lng = 34.0, accuracy = 5f, batteryLevel = 80, isCharging = true,
        tempCelsius = 30f, status = status, timestamp = sentAtMs
    )

    private fun AlarmBook.has(type: AlarmType) = alarms.any { it.type == type && it.isActive }

    @Test
    fun aForcedNoticeRaisesTheForcedAlarmAndANewerRowEndsIt() = runBlocking {
        val watch = Watch(this)
        watch.ready()
        watch.relay.emit(row(sentAtMs = 1_000L))
        watch.silences.emit(SensorSilence("T", 1_500L, 1L, 500L, SilenceCause.AIRPLANE_MODE))
        val book = watch.awaitBook { it.has(AlarmType.TRACKER_FORCED_OFFLINE) }
        assertFalse(book.has(AlarmType.TRACKER_OFFLINE))
        assertEquals(false, book.isTrackerOnline)

        watch.relay.emit(row(sentAtMs = 1_400L)) // Sent before the last word: it does not end it.
        assertTrue(watch.awaitBook { true }.has(AlarmType.TRACKER_FORCED_OFFLINE))
        watch.relay.emit(row(sentAtMs = 2_000L))
        val back = watch.awaitBook { !it.has(AlarmType.TRACKER_FORCED_OFFLINE) }
        assertEquals(true, back.isTrackerOnline)
        watch.job.cancel()
    }

    @Test
    fun theOwnersStopKeepsTheBookQuietButTheTrackerUnheard() = runBlocking {
        val watch = Watch(this)
        watch.ready()
        watch.relay.emit(row(sentAtMs = 1_000L))
        watch.awaitBook { it.isTrackerOnline == true }
        watch.silences.emit(SensorSilence("T", 1_100L, 1L, 100L, SilenceCause.OWNER_STOP))
        val book = watch.awaitBook { it.isTrackerOnline == false }
        assertEquals(emptyList<Alarm>(), book.alarms)
        watch.job.cancel()
    }

    @Test
    fun aPlainSilenceAfterASensorAlarmRowIsForced() = runBlocking {
        val watch = Watch(this)
        watch.ready()
        watch.relay.emit(row(sentAtMs = 1_000L, status = ACOUSTIC_ALERT_STATUS))
        watch.now += 30_000L
        watch.relay.emit(row(sentAtMs = 31_000L))
        watch.silences.emit(SensorSilence("T", 31_000L, 2L, 35_000L, SilenceCause.SILENT))
        val book = watch.awaitBook { it.has(AlarmType.TRACKER_FORCED_OFFLINE) }
        assertFalse(book.has(AlarmType.TRACKER_OFFLINE))
        watch.job.cancel()
    }
}
