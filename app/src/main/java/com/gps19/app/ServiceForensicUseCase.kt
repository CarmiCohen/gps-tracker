package com.gps19.app

import com.gps19.core.engine.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ServiceForensicUseCase: Manages state-latched violation recording for background services.
 * July.22.00:
 * - Hilt Hardening: Added @Inject constructor and @Singleton.
 * July.21.00:
 * - Issue #102: Temporal Forensic Integrity. Standardized 'maxTrackerAccuracy'.
 * - Restored Sit Detection (ALERT_ID_TRACKER_CHAIR) forensic markers.
 * - Maintained Hilt compatibility as per hardened Golden Master architecture.
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
        maxTrackerAccuracy: Double,
        activeViolations: Set<String>,
        unresolvedAlarms: Set<String>
    ) {
        if (lat == 0.0 || lng == 0.0) return

        // 1. Logic-based violations (passed in via activeViolations)
        handleLatch(ALERT_ID_SIGNAL_LOSS, ALERT_ID_SIGNAL_LOSS in activeViolations, lat, lng, accuracy, maxTrackerAccuracy, now)
        handleLatch(ALERT_ID_JUMP_ALERT, ALERT_ID_JUMP_ALERT in activeViolations, lat, lng, accuracy, maxTrackerAccuracy, now)
        handleLatch(ALERT_ID_VISUAL_JUMP, ALERT_ID_VISUAL_JUMP in activeViolations, lat, lng, accuracy, maxTrackerAccuracy, now)
        handleLatch(ALERT_ID_GPS_STALL, ALERT_ID_GPS_STALL in activeViolations, lat, lng, accuracy, maxTrackerAccuracy, now)
        handleLatch(ALERT_ID_TRACKER_GAP, ALERT_ID_TRACKER_GAP in activeViolations, lat, lng, accuracy, maxTrackerAccuracy, now)
        handleLatch(ALERT_ID_TRACKER_CHAIR, ALERT_ID_TRACKER_CHAIR in activeViolations, lat, lng, accuracy, maxTrackerAccuracy, now)

        // 2. Alarm-based violations (passed in via unresolvedAlarms)
        handleLatch(ALERT_ID_TRACKER_TAMPER, ALERT_ID_TRACKER_TAMPER in unresolvedAlarms, lat, lng, accuracy, maxTrackerAccuracy, now)
        handleLatch(ALERT_ID_TRACKER_GEOFENCE, ALERT_ID_TRACKER_GEOFENCE in unresolvedAlarms, lat, lng, accuracy, maxTrackerAccuracy, now)
    }

    private fun handleLatch(id: String, active: Boolean, lat: Double, lng: Double, accuracy: Double, maxTrackerAccuracy: Double, now: Long) {
        val wasRecorded = latches[id] ?: false
        if (active && !wasRecorded) {
            latches[id] = true
            repository.addViolation(lat, lng, id, accuracy, maxTrackerAccuracy, timestamp = now)
        } else if (!active) {
            latches[id] = false
        }
    }

    fun resetLatches() {
        latches.clear()
    }
}
