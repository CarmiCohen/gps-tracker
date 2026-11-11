package com.jd.app.monitor

import com.jd.core.shared.LocationUpdate
import com.jd.core.shared.SystemLogger
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/** How long a pushed alarm row may wait for the alarm engine; an older one is dropped rather than raised late. */
const val PUSHED_ROW_FRESH_MS = 30_000L

/**
 * The alarm rows that reached this phone as a push, waiting for [MonitorAlarmEngine] (ISSUE-NOV10.8-O86). The wait
 * matters when the push finds the process dead: the row is taken here first, and the engine starts after it.
 * The rows go to the alarm engine only; the trail and the gap detection keep reading the relay connection.
 */
@Singleton
class PushedRows @Inject constructor() {

    private class Pushed(val row: LocationUpdate, val receivedAtMs: Long)

    private val waiting = Channel<Pushed>(CAPACITY, BufferOverflow.DROP_OLDEST)

    /**
     * Takes [row], received at [receivedAtMs], when it comes from the paired tracker [trackerId].
     * Returns false for another tracker's row and for an unpaired Monitor, which must stay asleep.
     */
    fun offer(row: LocationUpdate, trackerId: String?, receivedAtMs: Long): Boolean {
        if (trackerId == null || row.deviceId != trackerId) return false
        waiting.trySend(Pushed(row, receivedAtMs))
        return true
    }

    /** The waiting rows and every later one, for a single collector; a row that waited too long at [clock] time is dropped. */
    fun fresh(clock: () -> Long): Flow<LocationUpdate> = waiting.receiveAsFlow().mapNotNull { pushed ->
        val waitedMs = clock() - pushed.receivedAtMs
        if (waitedMs <= PUSHED_ROW_FRESH_MS) {
            pushed.row
        } else {
            SystemLogger.w("Push", "Pushed row ${pushed.row.status} seq ${pushed.row.sequenceId} dropped: it waited $waitedMs ms for the alarm engine")
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
        if (isHighPriority) "" else " (delivered at normal priority)"
