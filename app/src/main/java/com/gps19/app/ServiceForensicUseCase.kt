package com.gps19.app

import com.gps19.core.engine.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ServiceForensicUseCase: Manages state-latched violation recording for background services.
 * v8.9.79: Issue #014 - Type Migration: Standardized accuracy to Double.
 * v8.9.42:
 * - Issue #325: Authoritative Spatial Anchoring (Dual-Metric). Refactored recordViolationMarkers 
 *   to propagate both raw accuracy and authoritative maxAccuracy for forensic parity.
 */
@Singleton
class ServiceForensicUseCase @Inject constructor(
    private val repository: MainRepository
) {
    private val latches = mutableMapOf<String, Boolean>()

    fun recordViolationMarkers(
        now: Long,
        lat: Double,
        lng: Double,
        accuracy: Double,
        maxAccuracy: Double,
        activeViolations: Set<String>,
        unresolvedAlarms: Set<String>
    ) {
        if (lat == 0.0 || lng == 0.0) return

        // 1. Logic-based violations (passed in via activeViolations)
        handleLatch(ALERT_ID_SIGNAL_LOSS, ALERT_ID_SIGNAL_LOSS in activeViolations, lat, lng, accuracy, maxAccuracy, now)
        handleLatch(ALERT_ID_JUMP_ALERT, ALERT_ID_JUMP_ALERT in activeViolations, lat, lng, accuracy, maxAccuracy, now)
        handleLatch(ALERT_ID_VISUAL_JUMP, ALERT_ID_VISUAL_JUMP in activeViolations, lat, lng, accuracy, maxAccuracy, now)
        handleLatch(ALERT_ID_GPS_STALL, ALERT_ID_GPS_STALL in activeViolations, lat, lng, accuracy, maxAccuracy, now)
        handleLatch(ALERT_ID_TRACKER_GAP, ALERT_ID_TRACKER_GAP in activeViolations, lat, lng, accuracy, maxAccuracy, now)
        handleLatch(ALERT_ID_TRACKER_CHAIR, ALERT_ID_TRACKER_CHAIR in activeViolations, lat, lng, accuracy, maxAccuracy, now)

        // 2. Alarm-based violations (passed in via unresolvedAlarms)
        handleLatch(ALERT_ID_TRACKER_TAMPER, ALERT_ID_TRACKER_TAMPER in unresolvedAlarms, lat, lng, accuracy, maxAccuracy, now)
        handleLatch(ALERT_ID_TRACKER_GEOFENCE, ALERT_ID_TRACKER_GEOFENCE in unresolvedAlarms, lat, lng, accuracy, maxAccuracy, now)
    }

    private fun handleLatch(id: String, active: Boolean, lat: Double, lng: Double, accuracy: Double, maxAccuracy: Double, now: Long) {
        val wasRecorded = latches[id] ?: false
        if (active && !wasRecorded) {
            latches[id] = true
            repository.addViolation(lat, lng, id, accuracy, maxAccuracy, timestamp = now)
        } else if (!active) {
            latches[id] = false
        }
    }

    fun resetLatches() {
        latches.clear()
    }
}
