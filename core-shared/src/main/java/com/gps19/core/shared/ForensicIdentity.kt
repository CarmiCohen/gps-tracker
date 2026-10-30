package com.gps19.core.shared

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
     * Validates that an ID is a valid 64-character hex hash.
     */
    fun isValid(id: String): Boolean {
        return id.length == 64 && id.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
    }
}
