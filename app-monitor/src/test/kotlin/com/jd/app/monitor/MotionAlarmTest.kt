package com.jd.app.monitor

import com.jd.core.shared.AlertSettings
import com.jd.core.shared.LIFT_ALERT_STATUS
import com.jd.core.shared.LocationUpdate
import com.jd.core.shared.TILT_ALERT_STATUS
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
 * Chapter 162 (ISSUE-NOV5.2-O11 Group 5): a tilt or lift the Sensor reports raises the Monitor's `Tracker tilted` or
 * `Tracker lifted` alarm, switched by `Tilt Angle Detection Sensor` (`tiltAlert`) and `Lift / Height Change Sensor` (`liftAlert`).
 */
class MotionAlarmTest {

    private class Watch(scope: CoroutineScope) {
        val telemetry = MutableSharedFlow<LocationUpdate>(extraBufferCapacity = 16)
        val violations = MutableSharedFlow<ViolationEvent>(extraBufferCapacity = 16)
        val settings = MutableSharedFlow<AlertSettings>(replay = 1)
        val ticks = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
        val acks = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
        val latest = MutableStateFlow<AlarmBook?>(null)
        val job = scope.launch {
            alarmBooks(MutableStateFlow("T"), telemetry, violations, settings, ticks, acks, MutableStateFlow(true), MutableStateFlow(true)) { 1_000_000L }
                .collect { latest.value = it }
        }

        suspend fun ready() = withTimeout(5.seconds) {
            listOf(telemetry, violations, ticks, acks).forEach { flow -> flow.subscriptionCount.first { it > 0 } }
        }

        suspend fun awaitBook(predicate: (AlarmBook) -> Boolean): AlarmBook =
            withTimeout(5.seconds) { latest.filterNotNull().first(predicate) }
    }

    /** An unplugged row: its power alarm shows it was evaluated. */
    private fun unpluggedRow(status: String) = LocationUpdate(
        deviceId = "T", lat = 32.0, lng = 34.0, accuracy = 5f, batteryLevel = 80, isCharging = false,
        tempCelsius = 30f, status = status
    )

    /**
     * The pending alarms after one row with [status]. The row is also unplugged, so the power alarm proves it was
     * evaluated even when the alarm under test is switched off.
     */
    private fun pendingFor(status: String, settings: AlertSettings = AlertSettings()): List<AlarmType> = runBlocking {
        val watch = Watch(this)
        watch.settings.emit(settings)
        watch.ready()
        watch.telemetry.emit(unpluggedRow(status))
        val book = watch.awaitBook { book -> book.pending.any { it.type == AlarmType.POWER_DISCONNECTED } }
        watch.job.cancel()
        book.pending.map { it.type } - AlarmType.POWER_DISCONNECTED
    }

    @Test
    fun `a tilt status raises the tilt alarm, not a GPS jump`() {
        assertEquals(listOf(AlarmType.TILT_ALERT), pendingFor(TILT_ALERT_STATUS))
    }

    @Test
    fun `a lift status raises the lift alarm, not a GPS jump`() {
        assertEquals(listOf(AlarmType.LIFT_ALERT), pendingFor(LIFT_ALERT_STATUS))
    }

    @Test
    fun `switched off tilt and lift alarms stay silent`() {
        assertTrue(pendingFor(TILT_ALERT_STATUS, AlertSettings(tiltAlert = false)).isEmpty())
        assertTrue(pendingFor(LIFT_ALERT_STATUS, AlertSettings(liftAlert = false)).isEmpty())
    }

    @Test
    fun `a lift violation event raises the lift alarm`() = runBlocking {
        val watch = Watch(this)
        watch.settings.emit(AlertSettings())
        watch.ready()
        watch.violations.emit(ViolationEvent(type = LIFT_ALERT_STATUS, location = LocationUpdate(deviceId = "T"), nonce = "l1"))
        assertEquals(listOf(AlarmType.LIFT_ALERT), watch.awaitBook { it.needsSiren }.pending.map { it.type })
        watch.job.cancel()
    }

    @Test
    fun `each switch allows only its own alarm`() {
        val noTilt = AlertSettings(tiltAlert = false)
        assertFalse(noTilt.allows(AlarmType.TILT_ALERT))
        assertTrue(noTilt.allows(AlarmType.LIFT_ALERT))
        val noLift = AlertSettings(liftAlert = false)
        assertFalse(noLift.allows(AlarmType.LIFT_ALERT))
        assertTrue(noLift.allows(AlarmType.TILT_ALERT))
        assertFalse(AlertSettings(globalMute = true).allows(AlarmType.TILT_ALERT))
    }
}
