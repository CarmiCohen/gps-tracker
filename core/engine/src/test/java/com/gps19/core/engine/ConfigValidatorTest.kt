package com.gps19.core.engine

import org.junit.Assert.*
import org.junit.Test

/**
 * ConfigValidatorTest: Centralized validation tests for identity and configuration rules.
 * Oct24.16:
 * - SIMP-OCT22.0-S1: Verified centralized identity validation logic.
 */
class ConfigValidatorTest {

    @Test
    fun `test identity validation rejects shell injection and spaces`() {
        assertFalse("Should reject spaces", ConfigValidator.isValidTrackerId("T 123"))
        assertFalse("Should reject shell command", ConfigValidator.isValidTrackerId("pm clear com.gps19.app"))
        assertFalse("Should reject special characters", ConfigValidator.isValidTrackerId("Tracker!"))
        assertFalse("Should reject empty string", ConfigValidator.isValidTrackerId(""))
        assertFalse("Should reject overly long IDs", ConfigValidator.isValidTrackerId("T".repeat(33)))
        
        assertTrue("Should allow alphanumeric", ConfigValidator.isValidTrackerId("Tracker123"))
        assertTrue("Should allow underscores", ConfigValidator.isValidTrackerId("Tracker_01"))
        assertTrue("Should allow hyphens", ConfigValidator.isValidTrackerId("Tracker-01"))
    }

    @Test
    fun `test identity uniqueness check`() {
        // Same IDs should be rejected
        assertFalse("Identical IDs should be rejected", ConfigValidator.areIdsUnique("T1", "T1"))
        assertFalse("Identical IDs with different case should be rejected", ConfigValidator.areIdsUnique("T1", "t1"))
        
        // Invalid IDs should be rejected
        assertFalse("Invalid IDs should be rejected by uniqueness check", ConfigValidator.areIdsUnique("T 1", "V 1"))
        
        // Reserved ID Cross-Contamination (R182)
        assertFalse("Tracker cannot use V", ConfigValidator.areIdsUnique("V", "V1"))
        assertFalse("Tracker cannot use viewer", ConfigValidator.areIdsUnique("viewer", "V1"))
        assertFalse("Viewer cannot use T", ConfigValidator.areIdsUnique("T1", "T"))
        assertFalse("Viewer cannot use Trk", ConfigValidator.areIdsUnique("T1", "Trk"))
        
        // Unique valid IDs should pass
        assertTrue("Unique valid IDs should pass", ConfigValidator.areIdsUnique("T1", "V1"))
        assertTrue("Default IDs should pass", ConfigValidator.areIdsUnique("T", "V"))
    }

    @Test
    fun `test validation result messages`() {
        // Use a reserved ID for the wrong role to trigger failure
        val result = ConfigValidator.validateIdentityUniqueness("viewer", "V1")
        assertTrue(result is ConfigValidator.ValidationResult.Failure)
        assertEquals("Identity Conflict: Some IDs (e.g., 'viewer', 'Trk') are reserved for cross-version compatibility. Please choose unique IDs.", (result as ConfigValidator.ValidationResult.Failure).message)
    }
}
