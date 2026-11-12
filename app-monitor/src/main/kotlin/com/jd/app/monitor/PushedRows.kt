package com.jd.app.monitor

import com.jd.core.network.SensorSilence
import com.jd.core.shared.LocationUpdate
import com.jd.core.shared.SystemLogger
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/** How long a pushed alarm row or notice may wait for the alarm engine; an older one is dropped rather than raised late. */
const val PUSHED_ROW_FRESH_MS = 30_000L

/**
 * What reached this phone as a push, waiting for [MonitorAlarmEngine] (ISSUE-NOV10.8-O86): the Sensor's alarm rows,
 * and the relay's notices that the Sensor went silent. The wait matters when the push finds the process dead: the
 * push is taken here first, and the engine starts after it. All of it goes to the alarm engine only; the trail and
 * the gap detection keep reading the relay connection.
 */
@Singleton
class PushedRows @Inject constructor() {

    private class Pushed<T>(val item: T, val receivedAtMs: Long)

    private val waitingRows = Channel<Pushed<LocationUpdate>>(CAPACITY, BufferOverflow.DROP_OLDEST)
    private val waitingSilences = Channel<Pushed<SensorSilence>>(CAPACITY, BufferOverflow.DROP_OLDEST)

    /**
     * Takes [row], received at [receivedAtMs], when it comes from the paired tracker [trackerId].
     * Returns false for another tracker's row and for an unpaired Monitor, which must stay asleep.
     */
    fun offer(row: LocationUpdate, trackerId: String?, receivedAtMs: Long): Boolean {
        if (trackerId == null || row.deviceId != trackerId) return false
        waitingRows.trySend(Pushed(row, receivedAtMs))
        return true
    }

    /** Takes the relay's [silence] notice under the same rule as [offer]. */
    fun offerSilence(silence: SensorSilence, trackerId: String?, receivedAtMs: Long): Boolean {
        if (trackerId == null || silence.trackerId != trackerId) return false
        waitingSilences.trySend(Pushed(silence, receivedAtMs))
        return true
    }

    /** The waiting rows and every later one, for a single collector; a row that waited too long at [clock] time is dropped. */
    fun fresh(clock: () -> Long): Flow<LocationUpdate> =
        waitingRows.fresh(clock) { "Pushed row ${it.status} seq ${it.sequenceId}" }

    /** The waiting silence notices and every later one, under the same rules as [fresh]. */
    fun freshSilences(clock: () -> Long): Flow<SensorSilence> =
        waitingSilences.fresh(clock) { "Pushed silence notice of '${it.trackerId}'" }

    private fun <T> Channel<Pushed<T>>.fresh(clock: () -> Long, name: (T) -> String): Flow<T> =
        receiveAsFlow().mapNotNull { pushed ->
            val waitedMs = clock() - pushed.receivedAtMs
            if (waitedMs <= PUSHED_ROW_FRESH_MS) {
                pushed.item
            } else {
                SystemLogger.w("Push", "${name(pushed.item)} dropped: it waited $waitedMs ms for the alarm engine")
                null
            }
        }

    private companion object {
        const val CAPACITY = 16
    }
}

/** The log line of a pushed alarm row; the lag is this phone's clock minus the Sensor's, as in the relay lag lines. */
fun pushArrivalLine(row: LocationUpdate, nowMs: Long, isHighPriority: Boolean): String =
    "Alert row ${row.status} seq ${row.sequenceId} arrived by push ${nowMs - row.timestamp} ms after it was sent" +
        lowPriorityNote(isHighPriority)

/** The log line of a pushed silence notice. */
fun silenceArrivalLine(silence: SensorSilence, isHighPriority: Boolean): String =
    "Relay notice by push: tracker '${silence.trackerId}' sent nothing for ${silence.silentMs / 1000} s " +
        "(last row seq ${silence.lastSequenceId})" + lowPriorityNote(isHighPriority)

private fun lowPriorityNote(isHighPriority: Boolean) = if (isHighPriority) "" else " (delivered at normal priority)"
