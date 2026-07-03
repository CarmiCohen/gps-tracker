package com.gps19.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalingTest {

    @Test
    fun `SignalingConstants validates roles correctly`() {
        // R182: Validation relaxed to non-empty
        assertTrue(SignalingConstants.isValidTrackerId("T123"))
        assertTrue(SignalingConstants.isValidTrackerId("C123"))
        assertFalse(SignalingConstants.isValidTrackerId(" "))

        assertTrue(SignalingConstants.isValidViewerId("C123"))
        assertTrue(SignalingConstants.isValidViewerId("T123"))
        assertFalse(SignalingConstants.isValidViewerId(""))
    }

    @Test
    fun `SignalPayloadGenerator creates valid ping payload`() {
        val payload = SignalPayloadGenerator.createPingPayload(
            deviceId = "T1",
            viewerId = "C1",
            isTracker = true,
            timestamp = 1000L,
            version = "1.0"
        )

        assertEquals("T1", payload["id"])
        assertEquals("C1", payload["viewer_id"])
        assertEquals("tracker", payload["from"])
        assertEquals(1000L, payload["ts"])
        assertEquals("1.0", payload["ver"])
    }

    @Test
    fun `SignalPayloadGenerator handles pong creation and validation`() {
        val validPing = mapOf("id" to "T1", "ts" to 1000L)
        val invalidPing = mapOf("id" to "T2", "ts" to 1000L)

        // Tracker T1 receives a ping for T1
        val validPong = SignalPayloadGenerator.createPongPayload(validPing, "T1", true)
        assertNotNull(validPong)
        assertEquals("T1", validPong!!["id"])

        // Tracker T1 receives a ping for T2 (should be ignored)
        val ignoredPong = SignalPayloadGenerator.createPongPayload(invalidPing, "T1", true)
        assertNull(ignoredPong)
    }

    @Test
    fun `SignalingConstants returns correct labels`() {
        assertEquals("tracker", SignalingConstants.getOwnTypeLabel(true))
        assertEquals("viewer", SignalingConstants.getOwnTypeLabel(false))
        assertEquals("viewer_pulse", SignalingConstants.getPulseType(true))
        assertEquals("tracker_pulse", SignalingConstants.getPulseType(false))
    }
}
