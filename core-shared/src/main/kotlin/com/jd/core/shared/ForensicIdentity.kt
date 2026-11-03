package com.jd.core.shared

import java.util.UUID

/**
 * ForensicIdentity: Standardized identity generation for Tracker and Viewer (Point 25).
 * Centralizes the 64-char salted SHA-256 hashing logic.
 */
object ForensicIdentity {
    
    /**
     * Standardized prefix for forensic identifiers to distinguish between roles.
     */
    const val PREFIX_SENSOR = "SNS"
    const val PREFIX_MONITOR = "MON"

    /**
     * Generates a stable identity from a raw hardware ID and a persistent salt.
     */
    fun create(rawId: String, salt: String): String {
        return CryptoUtils.hashIdentity(rawId, salt)
    }
    
    fun generateSalt(): String = UUID.randomUUID().toString()

    /**
     * Validates that an ID is not blank (allows any sequence of characters including a single character).
     */
    fun isValid(id: String): Boolean {
        return id.isNotBlank()
    }
}
