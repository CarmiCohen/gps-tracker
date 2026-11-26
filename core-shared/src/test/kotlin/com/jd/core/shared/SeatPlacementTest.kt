package com.jd.core.shared

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * ISSUE-NOV26.0-O163 part 1: the Monitor keeps named seat placements, one active, and sends the active one to the
 * Sensor inside [SensorBehaviour]; the list never becomes empty.
 */
class SeatPlacementTest {

    private val start = AlertSettings()

    @Test
    fun settingsBeforeNov26_4ReadAsOneActiveChair() {
        assertEquals(listOf(DEFAULT_SEAT_PLACEMENT), start.seatPlacements)
        assertEquals(DEFAULT_SEAT_PLACEMENT, start.activePlacement())
        assertEquals(DEFAULT_SEAT_PLACEMENT, Json.decodeFromString<AlertSettings>("""{"sitAlert":true}""").activePlacement())
        assertEquals("Chair", DEFAULT_SEAT_PLACEMENT.label)
    }

    @Test
    fun anAddedPlacementBecomesActiveAndIsSentToTheSensor() {
        val car = start.withPlacementAdded("  Driver ", SeatType.CAR_SEAT, nowMs = 1_000L)
        assertEquals(2, car.seatPlacements.size)
        assertEquals(SeatPlacement(1_000L, "Driver", SeatType.CAR_SEAT), car.activePlacement())
        assertEquals(car.activePlacement(), car.sensorBehaviour().placement)
        assertEquals("Driver (car seat)", car.activePlacement().label)
        assertEquals("Survives the Monitor's store", car, Json.decodeFromString<AlertSettings>(Json.encodeToString(car)))
    }

    @Test
    fun aChangedActivePlacementIsANewSensorRevision() {
        val sofa = start.withPlacementAdded("Sofa", SeatType.SOFA, nowMs = 2L)
        assertEquals(5_000L, sofa.revisedAfter(start, nowMs = 5_000L).sensorRevision)
        val back = sofa.withActivePlacement(DEFAULT_SEAT_PLACEMENT.id)
        assertEquals(6_000L, back.revisedAfter(sofa, nowMs = 6_000L).sensorRevision)
        // An inactive placement renamed changes nothing on the Sensor.
        val renamed = back.withPlacementEdited(2L, "Couch", SeatType.SOFA)
        assertEquals(back.sensorRevision, renamed.revisedAfter(back, nowMs = 7_000L).sensorRevision)
    }

    @Test
    fun namesAreTrimmedNonEmptyShortAndUnique() {
        val list = start.seatPlacements
        assertEquals("Enter a name", seatPlacementNameError("   ", list))
        assertEquals("At most 24 characters", seatPlacementNameError("x".repeat(25), list))
        assertEquals("This name is already used", seatPlacementNameError(" chair", list))
        assertNull("Its own name is no clash", seatPlacementNameError("Chair", list, exceptId = DEFAULT_SEAT_PLACEMENT.id))
        assertSame("A refused name changes nothing", start, start.withPlacementAdded("CHAIR", SeatType.SOFA, nowMs = 2L))
        assertSame(start, start.withActivePlacement(99L))
    }

    @Test
    fun theLastPlacementStaysAndADeletedActiveOnePassesToTheFirst() {
        assertSame(start, start.withPlacementDeleted(DEFAULT_SEAT_PLACEMENT.id))
        val two = start.withPlacementAdded("Driver", SeatType.CAR_SEAT, nowMs = 500L)
        val chairOnly = two.withPlacementDeleted(500L)
        assertEquals(listOf(DEFAULT_SEAT_PLACEMENT), chairOnly.seatPlacements)
        assertEquals(DEFAULT_SEAT_PLACEMENT, chairOnly.activePlacement())
        // A deleted placement's id is never given again: ids are add times, and a clock set back still counts up.
        val again = chairOnly.withPlacementAdded("Car", SeatType.CAR_SEAT, nowMs = 600L)
        assertEquals(listOf(1L, 600L), again.seatPlacements.map { it.id })
        val clockBack = again.withPlacementAdded("Sofa", SeatType.SOFA, nowMs = 100L)
        assertEquals(601L, clockBack.activePlacementId)
    }
}
