package com.jd.core.shared

import org.junit.Assert.*
import org.junit.Test

/**
 * ForensicIdentityTest: Ported from OLD codebase.
 * Validates R-ID 765: Salted Identity Generation and Format Validation.
 * Ensures strict hex-encoding and length requirements in pure Kotlin.
 */
class ForensicIdentityTest {

    @Test
    fun verifyIdentityGenerationFormat() {
        val rawId = "device-123"
        val salt = ForensicIdentity.generateSalt()
        val identity = ForensicIdentity.create(rawId, salt)
        
        // Assert: Format must be exactly 64-character SHA-256 hex string
        assertEquals("Identity must be 64 characters", 64, identity.length)
        assertTrue("Identity must be hex encoded", identity.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' })
    }

    @Test
    fun verifyIdentitySaltUniqueness() {
        val rawId = "stable-hardware-id"
        
        val salt1 = ForensicIdentity.generateSalt()
        val salt2 = ForensicIdentity.generateSalt()
        assertNotEquals("Salts must be unique (UUID)", salt1, salt2)

        val id1 = ForensicIdentity.create(rawId, salt1)
        val id2 = ForensicIdentity.create(rawId, salt2)
        assertNotEquals("Identities with different salts must differ", id1, id2)
    }

    @Test
    fun verifyIdentityValidationLogic() {
        val validId = "a".repeat(64)
        val shortId = "a".repeat(63)
        val longId = "a".repeat(65)
        val nonHexId = "g".repeat(64)

        assertTrue("Valid ID must be accepted", ForensicIdentity.isValid(validId))
        assertFalse("Short ID must be rejected", ForensicIdentity.isValid(shortId))
        assertFalse("Long ID must be rejected", ForensicIdentity.isValid(longId))
        assertFalse("Non-hex ID must be rejected", ForensicIdentity.isValid(nonHexId))
    }
}
