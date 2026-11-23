package com.jd.app.sensor

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * The alarms waiting for a sensing tick (SIMP-NOV8.103-S1). A telemetry row carries one status, so each tick sends one
 * alarm, the oldest waiting. The sensor threads add noise, light, tilt, lift and vibration alerts; the sensing loop adds
 * the geofence breach and the movement verdict it found on its fix, then takes the alarm it sends. Up to Nov23.9 the
 * loop wrote its findings straight onto the row, so a movement verdict overwrote a geofence breach found on the same
 * tick and the Monitor never alarmed for the breach; now the second waits for the next tick, which the loop runs at once.
 */
class PendingAlarms {
    data class Alarm(val status: String, val logLine: String)

    private val queue = ConcurrentLinkedQueue<Alarm>()

    /** Queues an alarm found on a sensor thread. */
    fun add(alarm: Alarm) {
        queue.add(alarm)
    }

    /** Queues the alarms this tick found (nulls skipped, in order), then returns the alarm this tick sends, or null. */
    fun takeForTick(vararg found: Alarm?): Alarm? {
        found.forEach { if (it != null) queue.add(it) }
        return queue.poll()
    }

    fun isNotEmpty(): Boolean = queue.isNotEmpty()
}
