package com.gps19.app

import com.gps19.core.engine.SignalingConstants
import org.junit.Assert.*
import org.junit.Test

class SettingsSanitizationTest {

    @Test
    fun `test identity validation rejects shell injection and spaces`() {
        assertFalse("Should reject spaces", SignalingConstants.isValidTrackerId("T 123"))
        assertFalse("Should reject shell command", SignalingConstants.isValidTrackerId("pm clear com.gps19.app"))
        assertFalse("Should reject special characters", SignalingConstants.isValidTrackerId("Tracker!"))
        assertFalse("Should reject empty string", SignalingConstants.isValidTrackerId(""))
        assertFalse("Should reject overly long IDs", SignalingConstants.isValidTrackerId("T".repeat(33)))
        
        assertTrue("Should allow alphanumeric", SignalingConstants.isValidTrackerId("Tracker123"))
        assertTrue("Should allow underscores", SignalingConstants.isValidTrackerId("Tracker_01"))
        assertTrue("Should allow hyphens", SignalingConstants.isValidTrackerId("Tracker-01"))
    }

    @Test
    fun `test identity uniqueness check`() {
        // Same IDs should be rejected
        assertFalse("Identical IDs should be rejected", SignalingConstants.areIdsUnique("T1", "T1"))
        assertFalse("Identical IDs with different case should be rejected", SignalingConstants.areIdsUnique("T1", "t1"))
        
        // Invalid IDs should be rejected
        assertFalse("Invalid IDs should be rejected by uniqueness check", SignalingConstants.areIdsUnique("T 1", "V 1"))
        
        // Unique valid IDs should pass
        assertTrue("Unique valid IDs should pass", SignalingConstants.areIdsUnique("T1", "V1"))
    }
}
