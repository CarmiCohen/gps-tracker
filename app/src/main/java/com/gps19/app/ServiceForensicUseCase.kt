package com.gps19.app

import com.gps19.core.engine.*
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * ServiceForensicUseCase: Manages state-latched violation recording for background services.
 * Oct.23.120:
 * - ISSUE-OCT23.101-F4: Violation Persistence Integrity Gaps. Updated 
 *   recordViolationMarkers() to accept and propagate isJammingCandidate.
 * Oct.22.13:
 * - ISSUE-OCT22.0-F4: Telemetry Sync Hardening. Integrated ConnectivitySuite 
 *   to propagate violation events to remote Viewers via emitViolation().
 */
@Singleton
class ServiceForensicUseCase @Inject constructor(
    private val repository: MainRepository,
    private val connectivitySuite: Provider<ConnectivitySuite>
) {
    private val latches = mutableMapOf<String, Boolean>()

    fun recordViolationMarkers(
        now: Long,
        lat: Double,
        lng: Double,
        accuracy: Double,
        maxTrackerAccuracy: Double,
        activeViolations: Set<String>,
        unresolvedAlarms: Set<String>,
        isJammingCandidate: Boolean = false
    ) {
        if (lat == 0.0 || lng == 0.0) return

        // 1. Logic-based violations (passed in via activeViolations)
        handleLatch(ALERT_ID_SIGNAL_LOSS, ALERT_ID_SIGNAL_LOSS in activeViolations, lat, lng, accuracy, maxTrackerAccuracy, now, isJammingCandidate)
        handleLatch(ALERT_ID_JUMP_ALERT, ALERT_ID_JUMP_ALERT in activeViolations, lat, lng, accuracy, maxTrackerAccuracy, now, isJammingCandidate)
        handleLatch(ALERT_ID_VISUAL_JUMP, ALERT_ID_VISUAL_JUMP in activeViolations, lat, lng, accuracy, maxTrackerAccuracy, now, isJammingCandidate)
        handleLatch(ALERT_ID_GPS_STALL, ALERT_ID_GPS_STALL in activeViolations, lat, lng, accuracy, maxTrackerAccuracy, now, isJammingCandidate)
        handleLatch(ALERT_ID_TRACKER_GAP, ALERT_ID_TRACKER_GAP in activeViolations, lat, lng, accuracy, maxTrackerAccuracy, now, isJammingCandidate)
        handleLatch(ALERT_ID_TRACKER_CHAIR, ALERT_ID_TRACKER_CHAIR in activeViolations, lat, lng, accuracy, maxTrackerAccuracy, now, isJammingCandidate)

        // 2. Alarm-based violations (passed in via unresolvedAlarms)
        handleLatch(ALERT_ID_TRACKER_TAMPER, ALERT_ID_TRACKER_TAMPER in unresolvedAlarms, lat, lng, accuracy, maxTrackerAccuracy, now, isJammingCandidate)
        handleLatch(ALERT_ID_TRACKER_GEOFENCE, ALERT_ID_TRACKER_GEOFENCE in unresolvedAlarms, lat, lng, accuracy, maxTrackerAccuracy, now, isJammingCandidate)
    }

    private fun handleLatch(id: String, active: Boolean, lat: Double, lng: Double, accuracy: Double, maxTrackerAccuracy: Double, now: Long, isJammingCandidate: Boolean) {
        val wasRecorded = latches[id] ?: false
        if (active && !wasRecorded) {
            latches[id] = true
            repository.addViolation(lat, lng, id, accuracy, maxTrackerAccuracy, timestamp = now, isJammingCandidate = isJammingCandidate)
            
            // vOct22.13: Propagate violation to remote peer (Viewer)
            connectivitySuite.get().emitViolation(id, lat, lng, accuracy, maxTrackerAccuracy, isJammingCandidate = isJammingCandidate)
        } else if (!active) {
            latches[id] = false
        }
    }

    fun resetLatches() {
        latches.clear()
    }
}
