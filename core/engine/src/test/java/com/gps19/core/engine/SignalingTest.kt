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
        assertTrue(SignalingConstants.isValidTrackerId("T123"))
        assertTrue(SignalingConstants.isValidTrackerId("C123"))
        assertFalse(SignalingConstants.isValidTrackerId(" "))

        assertTrue(SignalingConstants.isValidViewerId("C123"))
        assertTrue(SignalingConstants.isValidViewerId("T123"))
        assertFalse(SignalingConstants.isValidViewerId(""))
    }

    @Test
    fun `SignalingValidator enforces Identity Locking R982 correctly`() {
        val trackerId = "T1"
        val defaultViewer = SignalingConstants.DEFAULT_VIEWER_ID // "V"
        val customViewer = "MyPhone"
        val hackerViewer = "Hacker"

        // --- TRACKER MODE TESTS ---

        // 1. Initial Pairing: Tracker is default, accepts custom viewer pulse
        assertTrue("Tracker should adopt custom viewer if currently default",
            SignalingValidator.shouldProcessLocationUpdate(
                incomingId = trackerId, ownDeviceId = trackerId,
                isFromViewer = true, viewerId = customViewer, ownViewerId = defaultViewer,
                isTrackerMode = true
            )
        )

        // 2. Locked State: Tracker is paired, accepts matching viewer
        assertTrue("Tracker should accept linked viewer",
            SignalingValidator.shouldProcessLocationUpdate(
                incomingId = trackerId, ownDeviceId = trackerId,
                isFromViewer = true, viewerId = customViewer, ownViewerId = customViewer,
                isTrackerMode = true
            )
        )

        // 3. Locked State: Tracker is paired, REJECTS mismatched viewer (The core of R982)
        assertFalse("Tracker MUST reject mismatched viewer once locked",
            SignalingValidator.shouldProcessLocationUpdate(
                incomingId = trackerId, ownDeviceId = trackerId,
                isFromViewer = true, viewerId = hackerViewer, ownViewerId = customViewer,
                isTrackerMode = true
            )
        )

        // 4. Settings Guard: Tracker accepts settings from linked viewer
        assertTrue("Tracker should accept settings from linked viewer",
            SignalingValidator.shouldProcessSettingsUpdate(
                incomingId = trackerId, ownDeviceId = trackerId,
                incomingViewerId = customViewer, ownViewerId = customViewer,
                fromViewer = true, isTrackerMode = true
            )
        )

        // 5. Settings Guard: Tracker rejects settings from hacker
        assertFalse("Tracker MUST reject settings from unauthorized viewer",
            SignalingValidator.shouldProcessSettingsUpdate(
                incomingId = trackerId, ownDeviceId = trackerId,
                incomingViewerId = hackerViewer, ownViewerId = customViewer,
                fromViewer = true, isTrackerMode = true
            )
        )

        // --- VIEWERS MODE TESTS ---

        // 6. Echo Suppression: Viewer ignores its own packets
        assertFalse("Viewer should suppress its own echoes",
            SignalingValidator.shouldProcessLocationUpdate(
                incomingId = trackerId, ownDeviceId = trackerId,
                isFromViewer = true, viewerId = customViewer, ownViewerId = customViewer,
                isTrackerMode = false
            )
        )

        // 7. Multi-Viewer: Viewer accepts packets from other viewers (Relay mode)
        assertTrue("Viewer should accept packets from other viewers",
            SignalingValidator.shouldProcessLocationUpdate(
                incomingId = trackerId, ownDeviceId = trackerId,
                isFromViewer = true, viewerId = customViewer, ownViewerId = hackerViewer,
                isTrackerMode = false
            )
        )
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

        val validPong = SignalPayloadGenerator.createPongPayload(validPing, "T1", true)
        assertNotNull(validPong)
        assertEquals("T1", validPong!!["id"])

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
