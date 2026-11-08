package com.jd.app.monitor

import com.jd.core.shared.GEOFENCE_BREACH_STATUS
import com.jd.core.shared.GeofenceConfig
import com.jd.core.shared.HomePoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

/**
 * Chapter 156 (ISSUE-NOV8.13-O51): the Monitor only delivers the geofence; the tracker enforces it.
 */
class GeofenceSyncTest {

    private val first = GeofenceConfig(points = listOf(HomePoint(32.0, 34.0)), revision = 1L)
    private val second = first.withPoint(HomePoint(32.01, 34.0)).copy(revision = 2L)

    /** Lets the single test thread run the collector, which combine reaches through internal channels. */
    private suspend fun settle() = delay(SETTLE)

    @Test
    fun `the geofence is sent when the tracker comes online and on every edit while online`() = runBlocking {
        val geofence = MutableStateFlow(first)
        val online = MutableStateFlow(false)
        val sent = mutableListOf<GeofenceConfig>()
        val job = launch { geofenceDeliveries(geofence, online).toList(sent) }
        settle()
        assertEquals("Nothing is sent while the tracker is offline", emptyList<GeofenceConfig>(), sent)

        online.value = true; settle()
        assertEquals(listOf(first), sent)

        geofence.value = second; settle()
        assertEquals(listOf(first, second), sent)

        online.value = false; settle()
        online.value = true; settle()
        assertEquals("A reconnecting tracker receives the stored geofence again", listOf(first, second, second), sent)
        job.cancel()
    }

    @Test
    fun `an edit made while the tracker is offline is sent once it is back`() = runBlocking {
        val geofence = MutableStateFlow(first)
        val online = MutableStateFlow(false)
        val sent = mutableListOf<GeofenceConfig>()
        val job = launch { geofenceDeliveries(geofence, online).toList(sent) }
        settle()
        geofence.value = second; settle()
        assertEquals(emptyList<GeofenceConfig>(), sent)
        online.value = true; settle()
        assertEquals(listOf(second), sent)
        job.cancel()
    }

    @Test
    fun `out markers are the breach rows with a position`() {
        fun row(seq: Long, status: String, lat: Double = 32.0, lng: Double = 34.0) =
            TrailEntity(trackerId = "T", timestamp = seq, sequenceId = seq, lat = lat, lng = lng, accuracy = 5f, status = status)
        val trail = listOf(row(1, "VALID"), row(2, GEOFENCE_BREACH_STATUS), row(3, "JUMP"), row(4, GEOFENCE_BREACH_STATUS, lat = 0.0, lng = 0.0))
        assertEquals(listOf(2L), geofenceBreaches(trail).map { it.sequenceId })
    }

    private companion object {
        val SETTLE = 50.milliseconds
    }
}
