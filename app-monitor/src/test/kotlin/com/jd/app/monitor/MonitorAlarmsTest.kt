package com.jd.app.monitor

import com.jd.core.shared.AlertSettings
import com.jd.core.shared.LocationUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
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

/** Verifies Chapter 129.1 to 129.3: when the Monitor alarms are raised, kept, acknowledged, and dropped (ISSUE-NOV5.2-O11). */
class MonitorAlarmsTest {

    private val start = 1_000_000L
    private val settings = AlertSettings()

    private fun reading(at: Long = start, battery: Int = 80, charging: Boolean = true, temp: Float = 30f, sensorTime: Long = at) =
        TrackerReading(receivedAtMs = at, sensorTimeMs = sensorTime, batteryLevel = battery, isCharging = charging, tempCelsius = temp)

    private fun conditions(reading: TrackerReading?, now: Long = start, active: Set<AlarmType> = emptySet()) =
        alarmConditions(reading, watchingSinceMs = start, nowMs = now, active = active)

    // 129.1 Conditions

    @Test
    fun aHealthyTrackerHoldsNoCondition() {
        assertEquals(emptySet<AlarmType>(), conditions(reading()))
    }

    @Test
    fun offlineCountsFromTheLastUpdateAndFromTheStartWhenNothingWasHeard() {
        assertEquals(emptySet<AlarmType>(), conditions(null, now = start + TRACKER_PRESENT_MS - 1))
        assertEquals(setOf(AlarmType.TRACKER_OFFLINE), conditions(null, now = start + TRACKER_PRESENT_MS))
        assertEquals(setOf(AlarmType.TRACKER_OFFLINE), conditions(reading(at = start), now = start + TRACKER_PRESENT_MS))
        assertEquals(emptySet<AlarmType>(), conditions(reading(at = start + 10_000), now = start + TRACKER_PRESENT_MS))
    }

    @Test
    fun powerBatteryAndTemperatureFollowTheirLimits() {
        assertEquals(setOf(AlarmType.POWER_DISCONNECTED), conditions(reading(charging = false)))
        assertEquals(setOf(AlarmType.LOW_BATTERY), conditions(reading(battery = LOW_BATTERY_BELOW - 1)))
        assertEquals(emptySet<AlarmType>(), conditions(reading(battery = LOW_BATTERY_BELOW)))
        assertEquals(setOf(AlarmType.HIGH_TEMPERATURE), conditions(reading(temp = 46.1f)))
        assertEquals(emptySet<AlarmType>(), conditions(reading(temp = HIGH_TEMPERATURE_ABOVE)))
    }

    @Test
    fun aRaisedConditionNeedsAMarginToClear() {
        val low = setOf(AlarmType.LOW_BATTERY)
        assertEquals(low, conditions(reading(battery = 21), active = low))
        assertEquals(emptySet<AlarmType>(), conditions(reading(battery = 22), active = low))

        val hot = setOf(AlarmType.HIGH_TEMPERATURE)
        assertEquals(hot, conditions(reading(temp = 45f), active = hot))
        assertEquals(emptySet<AlarmType>(), conditions(reading(temp = 44f), active = hot))
    }

    @Test
    fun aLateUpdateDoesNotReplaceANewerOne() {
        val newer = reading(sensorTime = 2_000, charging = true)
        val late = reading(sensorTime = 1_000, charging = false)

        assertEquals(newer, newerReading(newer, late))
        assertEquals(newer, newerReading(late, newer))
        assertEquals(late, newerReading(null, late))
    }

    // 129.2 The alarm book

    private val power = setOf(AlarmType.POWER_DISCONNECTED)

    @Test
    fun anAlarmIsRaisedOnceAndSoundsUntilAcknowledged() {
        val raised = AlarmBook().update(power, settings, start)
        val again = raised.update(power, settings, start + 1_000)

        assertEquals(1, again.alarms.size)
        assertEquals(start, again.alarms.single().raisedAtMs)
        assertTrue(again.needsSiren)

        val acknowledged = again.acknowledge()
        assertFalse(acknowledged.needsSiren)
        assertFalse(acknowledged.update(power, settings, start + 2_000).needsSiren)
    }

    @Test
    fun anUnacknowledgedAlarmStaysAfterItsConditionClears() {
        val cleared = AlarmBook().update(power, settings, start).update(emptySet(), settings, start + 1_000)

        assertTrue(cleared.needsSiren)
        assertFalse(cleared.alarms.single().isActive)
        assertEquals(emptyList<Alarm>(), cleared.acknowledge().alarms)
    }

    @Test
    fun anAcknowledgedAlarmIsRaisedAgainOnlyAfterItsConditionCleared() {
        val acknowledged = AlarmBook().update(power, settings, start).acknowledge()
        val cleared = acknowledged.update(emptySet(), settings, start + 1_000)
        val back = cleared.update(power, settings, start + 2_000)

        assertEquals(emptyList<Alarm>(), cleared.alarms)
        assertTrue(back.needsSiren)
        assertEquals(start + 2_000, back.alarms.single().raisedAtMs)
    }

    @Test
    fun aSwitchedOffAlarmIsDroppedAndNotRaised() {
        val off = settings.copy(power = false)
        val raised = AlarmBook().update(power, settings, start)

        assertEquals(emptyList<Alarm>(), raised.update(power, off, start + 1_000).alarms)
        assertEquals(emptyList<Alarm>(), AlarmBook().update(power, off, start).alarms)
    }

    @Test
    fun globalMuteSilencesEveryAlarm() {
        val muted = settings.copy(globalMute = true)
        val all = AlarmType.entries.toSet()

        assertEquals(emptyList<Alarm>(), AlarmBook().update(all, settings, start).update(all, muted, start + 1_000).alarms)
        AlarmType.entries.forEach { assertFalse(muted.allows(it)) }
    }

    @Test
    fun eachAlarmFollowsItsOwnSwitch() {
        assertFalse(settings.copy(serverConnection = false).allows(AlarmType.TRACKER_OFFLINE))
        assertFalse(settings.copy(power = false).allows(AlarmType.POWER_DISCONNECTED))
        assertFalse(settings.copy(lowBattery = false).allows(AlarmType.LOW_BATTERY))
        assertFalse(settings.copy(highTemperature = false).allows(AlarmType.HIGH_TEMPERATURE))
        AlarmType.entries.forEach { assertTrue(settings.allows(it)) }
    }

    // 129.3 The book flow

    private fun unplugged(device: String, time: Long) = LocationUpdate().apply {
        deviceId = device
        isCharging = false
        batteryLevel = 80
        tempCelsius = 30f
        timestamp = time
    }

    /** Collects the book flow into [latest]. Shared flows drop emissions without subscribers, so [ready] waits for them. */
    private class Watch(scope: CoroutineScope) {
        val target = MutableStateFlow<String?>("T")
        val telemetry = MutableSharedFlow<LocationUpdate>(extraBufferCapacity = 16)
        val settings = MutableSharedFlow<AlertSettings>(replay = 1)
        val ticks = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
        val acks = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
        @Volatile var now = 1_000_000L
        val latest = MutableStateFlow<AlarmBook?>(null)
        val job = scope.launch { alarmBooks(target, telemetry, settings, ticks, acks) { now }.collect { latest.value = it } }

        suspend fun ready() = withTimeout(5.seconds) {
            listOf(telemetry, ticks, acks).forEach { flow -> flow.subscriptionCount.first { it > 0 } }
        }

        suspend fun awaitBook(predicate: (AlarmBook) -> Boolean): AlarmBook =
            withTimeout(5.seconds) { latest.filterNotNull().first(predicate) }
    }

    @Test
    fun nothingIsEvaluatedBeforeTheSettingsAreKnown() = runBlocking {
        val watch = Watch(this)
        watch.ready()
        watch.now += TRACKER_PRESENT_MS
        watch.ticks.emit(Unit)
        delay(200)

        assertEquals(emptyList<Alarm>(), watch.latest.value?.alarms.orEmpty())

        watch.settings.emit(AlertSettings())
        assertEquals(listOf(AlarmType.TRACKER_OFFLINE), watch.awaitBook { it.needsSiren }.pending.map { it.type })
        watch.job.cancel()
    }

    @Test
    fun onlyThePairedTrackerRaisesAlarms() = runBlocking {
        val watch = Watch(this)
        watch.settings.emit(AlertSettings())
        watch.ready()
        watch.telemetry.emit(unplugged("OTHER", time = 1))
        delay(200)
        assertEquals(emptyList<Alarm>(), watch.latest.value?.alarms.orEmpty())

        watch.telemetry.emit(unplugged("T", time = 2))
        assertEquals(listOf(AlarmType.POWER_DISCONNECTED), watch.awaitBook { it.needsSiren }.pending.map { it.type })
        watch.job.cancel()
    }

    @Test
    fun anAcknowledgementSilencesTheBook() = runBlocking {
        val watch = Watch(this)
        watch.settings.emit(AlertSettings())
        watch.ready()
        watch.telemetry.emit(unplugged("T", time = 2))
        watch.awaitBook { it.needsSiren }

        watch.acks.emit(Unit)
        val silenced = watch.awaitBook { it.alarms.isNotEmpty() && !it.needsSiren }

        assertTrue(silenced.alarms.single().isAcknowledged)
        watch.job.cancel()
    }

    @Test
    fun aNewPairingStartsAnEmptyBook() = runBlocking {
        val watch = Watch(this)
        watch.settings.emit(AlertSettings())
        watch.ready()
        watch.telemetry.emit(unplugged("T", time = 2))
        watch.awaitBook { it.needsSiren }

        watch.target.value = "U"

        assertEquals(emptyList<Alarm>(), watch.awaitBook { it.alarms.isEmpty() }.alarms)
        watch.job.cancel()
    }
}
