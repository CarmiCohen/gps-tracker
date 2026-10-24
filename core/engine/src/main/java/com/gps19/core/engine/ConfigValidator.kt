package com.gps19.core.engine

/**
 * ConfigValidator: Pure logic for validating application configuration and identity rules.
 * Oct24.16:
 * - SIMP-OCT22.0-S1: Centralized identity validation and uniqueness rules.
 */
object ConfigValidator {

    private val ID_REGEX = Regex("^[a-zA-Z0-9_-]{1,32}$")
    
    // Identity Constants (Shared with SignalingConstants if needed, but localized here for validation)
    private const val DEFAULT_TRACKER_ID = "T"
    private const val DEFAULT_VIEWER_ID = "V"
    private const val LEGACY_TRACKER_ID = "Trk"
    private const val LEGACY_VIEWER_ID = "viewer"

    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Failure(val message: String) : ValidationResult()
        
        val isSuccess: Boolean get() = this is Success
    }

    /**
     * R182: Validates Tracker ID format.
     */
    fun validateTrackerId(id: String): ValidationResult {
        val trimmed = id.trim()
        if (trimmed.isEmpty()) return ValidationResult.Failure("Tracker ID cannot be empty.")
        if (ID_REGEX.matches(trimmed) || trimmed == LEGACY_TRACKER_ID) {
            return ValidationResult.Success
        }
        return ValidationResult.Failure("Invalid Tracker ID format. Use 1-32 alphanumeric characters, _ or -.")
    }

    /**
     * R182: Validates Viewer ID format.
     */
    fun validateViewerId(id: String): ValidationResult {
        val trimmed = id.trim()
        if (trimmed.isEmpty()) return ValidationResult.Failure("Viewer ID cannot be empty.")
        if (ID_REGEX.matches(trimmed) || trimmed == LEGACY_VIEWER_ID) {
            return ValidationResult.Success
        }
        return ValidationResult.Failure("Invalid Viewer ID format. Use 1-32 alphanumeric characters, _ or -.")
    }

    /**
     * R182: Ensures IDs are unique and do not cross-contaminate reserved alias sets.
     */
    fun validateIdentityUniqueness(trackerId: String, viewerId: String): ValidationResult {
        val t = trackerId.trim()
        val v = viewerId.trim()

        // 1. Format Validation
        if (!isValidTrackerId(t)) return ValidationResult.Failure("Invalid Tracker ID format.")
        if (!isValidViewerId(v)) return ValidationResult.Failure("Invalid Viewer ID format.")

        // 2. Strict string equality
        if (t.lowercase() == v.lowercase()) {
            return ValidationResult.Failure("Identity Conflict: Tracker and Viewer IDs must be unique.")
        }

        // 3. Reserved Set Cross-Contamination (R182)
        // Tracker cannot use Viewer reserved IDs
        if (t == DEFAULT_VIEWER_ID || t == LEGACY_VIEWER_ID) {
            return ValidationResult.Failure("Identity Conflict: Some IDs (e.g., 'viewer', 'Trk') are reserved for cross-version compatibility. Please choose unique IDs.")
        }
        // Viewer cannot use Tracker reserved IDs
        if (v == DEFAULT_TRACKER_ID || v == LEGACY_TRACKER_ID) {
            return ValidationResult.Failure("Identity Conflict: Some IDs (e.g., 'viewer', 'Trk') are reserved for cross-version compatibility. Please choose unique IDs.")
        }

        return ValidationResult.Success
    }

    // Helper methods for boolean-style checks (delegated to by SignalingConstants)
    fun isValidTrackerId(id: String): Boolean = (id.trim().let { ID_REGEX.matches(it) || it == LEGACY_TRACKER_ID })
    fun isValidViewerId(id: String): Boolean = (id.trim().let { ID_REGEX.matches(it) || it == LEGACY_VIEWER_ID })
    
    fun areIdsUnique(trackerId: String, viewerId: String): Boolean {
        return validateIdentityUniqueness(trackerId, viewerId) is ValidationResult.Success
    }
}
