package com.gps19.core.engine

import java.util.*
import kotlin.math.*

/**
 * MainAlarmLogic: Detection logic for system violations.
 * v8.9.34:
 * - Issue #417: Refactored getTrackerTitle to be fully role-aware for forensic parity. (Formerly #287 / #17)
 * - Issue #402: Synchronized version to v8.9.26 baseline. (Formerly #272 / #2)
 * v8.9.20:
 * - Issue #500: Updated ALERT_ID_TRACKER_CHAIR subtitle to "Chair occupancy detected" 
 *   for consistency with "Chair Occupied" forensic status. (Formerly #230)
 * v8.9.19:
 * - Issue #501: Implemented VISUAL_JUMP detection logic. (Formerly #231)
 * v8.9.18:
 * - Issue #489: Implemented Adaptive Jump Confidence. Increased JUMP_HOLD_DURATION_MS 
 *   when isAdaptiveJump is flagged (spoofing/reflection suspicion). (Formerly #219)
 */
object MainAlarmLogic {

    fun detectViolations(
        state: AlarmEvaluationState,
        isWarmup: Boolean = false
    ): SystemHealthReport {
        val now = state.now
        val reports = mutableListOf<ViolationReport>()
        
        val phase = state.discoveryPhase
        val isTracker = state.isTrackerMode
        val canCheckPeerErrors = phase == DiscoveryPhase.MONITORING
        
        val isDistanceGraceActive = phase == DiscoveryPhase.BOOTSTRAP
        
        // 1. LOCAL ALERTS
        reports.add(
            ViolationReport(
                type = ALERT_ID_LOCAL_INTERNET,
                title = getTrackerTitle(isTracker, ALERT_TITLE_LOCAL_INTERNET),
                subtitle = if (isTracker) "Tracker lost internet connection" else "Viewer lost internet connection",
                conditionMet = state.isLocalInternetLoss
            )
        )

        // 2. INFRASTRUCTURE ALERTS
        val isInternetHardwareOk = state.isHardwareOnline
        val isRelayConnected = state.isRelayConnected
        val isRelayConditionMet = !isRelayConnected && isInternetHardwareOk
        
        reports.add(
            ViolationReport(
                type = ALERT_ID_RELAY_OFFLINE,
                title = getTrackerTitle(isTracker, ALERT_TITLE_RELAY_OFFLINE),
                subtitle = "Relay server is unreachable",
                conditionMet = isRelayConditionMet
            )
        )

        // 3. PEER-DEPENDENT ALERTS
        val shouldSuppressPeerErrors = !isInternetHardwareOk || !isRelayConnected
        val peerLabel = if (isTracker) "Viewer" else "Tracker"

        reports.add(
            ViolationReport(
                type = ALERT_ID_TRACKER_OFFLINE,
                title = getTrackerTitle(isTracker, if (isTracker) ALERT_TITLE_VIEWER_OFFLINE else ALERT_TITLE_TRACKER_OFFLINE),
                subtitle = "$peerLabel is not connected to relay server",
                conditionMet = canCheckPeerErrors && !state.isTrackerConnected && !shouldSuppressPeerErrors
            )
        )

        reports.add(
            ViolationReport(
                type = ALERT_ID_JUMP_ALERT,
                title = getTrackerTitle(isTracker, ALERT_TITLE_JUMP_ALERT),
                subtitle = "GPS data is erratic or jumping",
                conditionMet = canCheckPeerErrors && state.isJammerSuspicion && !shouldSuppressPeerErrors
            )
        )

        reports.add(
            ViolationReport(
                type = ALERT_ID_VISUAL_JUMP,
                title = getTrackerTitle(isTracker, ALERT_TITLE_VISUAL_JUMP),
                subtitle = "Trajectory-based jump detected",
                conditionMet = canCheckPeerErrors && state.isTrackerVisualJump && !shouldSuppressPeerErrors,
                technicalDetails = "Tier: ${state.jumpTier}${if (state.isAdaptiveJump) " (Adaptive)" else ""}"
            )
        )
        
        reports.add(
            ViolationReport(
                type = ALERT_ID_SIGNAL_LOSS,
                title = getTrackerTitle(isTracker, if (isTracker) ALERT_TITLE_VIEWER_SIGNAL_LOSS else ALERT_TITLE_SIGNAL_LOSS),
                subtitle = "No data received from $peerLabel for >${if (isTracker) VIEWER_SIGNAL_LOSS_THRESHOLD_MS/1000 else TRACKER_SIGNAL_LOSS_THRESHOLD_MS/1000}s",
                conditionMet = canCheckPeerErrors && state.isSignalLoss && !shouldSuppressPeerErrors
            )
        )
        
        reports.add(
            ViolationReport(
                type = ALERT_ID_GPS_STALL,
                title = getTrackerTitle(isTracker, ALERT_TITLE_GPS_STALL),
                subtitle = "GPS location has not updated",
                conditionMet = canCheckPeerErrors && state.isGpsStalling && !shouldSuppressPeerErrors
            )
        )

        reports.add(
            ViolationReport(
                type = ALERT_ID_TRACKER_GAP,
                title = getTrackerTitle(isTracker, if (isTracker) ALERT_TITLE_VIEWER_GAP else ALERT_TITLE_TRACKER_GAP),
                subtitle = "GPS fix is older than ${GPS_GAP_THRESHOLD_MS / 1000}s",
                conditionMet = canCheckPeerErrors && state.isGpsGap && !shouldSuppressPeerErrors
            )
        )

        // 4. STATUS ALERTS
        val isPowerViolation = (state.powerAlarmPending || state.isPowerTamper) && state.trackerCurrentMa <= 0

        reports.add(
            ViolationReport(
                type = ALERT_ID_TRACKER_POWER,
                title = getTrackerTitle(isTracker, ALERT_TITLE_TRACKER_POWER),
                subtitle = "Charger unplugged",
                conditionMet = isPowerViolation
            )
        )

        val isShock = SentinelValidator.isShockViolated(state.peakVibrationShock, state.adaptiveVibrationFloor)
        val isTilt = SentinelValidator.isTiltViolated(state.trackerTiltDegrees)
        val isAcousticMet = SentinelValidator.isAcousticViolated(state.trackerAcousticDb, state.acousticFloorDb)
        val isLift = SentinelValidator.isLiftViolated(state.trackerBaroAlt)
        val isLightMet = SentinelValidator.isLightViolated(state.trackerLux, state.luxBaseline)

        val isTamperCondition = state.isTamperDetected || 
                                (!state.isNear) || 
                                isLightMet || 
                                isShock || isTilt || isAcousticMet || isLift || state.isPowerTamper

        val tamperReason = when {
            isShock -> "Shock: ${String.format(Locale.getDefault(), "%.1f", state.peakVibrationShock)}G"
            isLightMet -> "Light: ${state.trackerLux.roundToInt()} lux"
            !state.isNear -> "Proximity sensor cleared"
            isTilt -> String.format(Locale.getDefault(), "%.1f° tilt", state.trackerTiltDegrees)
            isAcousticMet -> String.format(Locale.getDefault(), "%.1f dB peak", state.trackerAcousticDb)
            isLift -> "Lift: ${String.format(Locale.getDefault(), "%.1f", state.trackerBaroAlt)}m"
            state.isPowerTamper -> "Power source tamper"
            state.isTamperDetected -> "Hardware tamper flag"
            else -> "Device handled or uncovered"
        }

        reports.add(
            ViolationReport(
                type = ALERT_ID_TRACKER_TAMPER,
                title = getTrackerTitle(isTracker, ALERT_TITLE_TRACKER_TAMPER),
                subtitle = tamperReason,
                conditionMet = isTamperCondition,
                extremeValue = maxOf(state.peakVibrationShock.toDouble(), state.trackerTiltDegrees.toDouble())
            )
        )

        reports.add(
            ViolationReport(
                type = ALERT_ID_TRACKER_TILT,
                title = getTrackerTitle(isTracker, ALERT_TITLE_TRACKER_TILT),
                subtitle = String.format(Locale.getDefault(), "%.1f° tilt", state.trackerTiltDegrees),
                conditionMet = isTilt,
                extremeValue = state.trackerTiltDegrees.toDouble()
            )
        )

        val acousticTechnical = if (state.isLocationPending) "LOCATION_PENDING: True" else null
        reports.add(
            ViolationReport(
                type = ALERT_ID_TRACKER_ACOUSTIC,
                title = getTrackerTitle(isTracker, ALERT_TITLE_TRACKER_ACOUSTIC),
                subtitle = String.format(Locale.getDefault(), "%.1f dB peak (Base: %.1f)", state.trackerAcousticDb, state.acousticFloorDb),
                conditionMet = isAcousticMet,
                extremeValue = state.trackerAcousticDb,
                technicalDetails = acousticTechnical
            )
        )

        reports.add(
            ViolationReport(
                type = ALERT_ID_TRACKER_LIFT,
                title = getTrackerTitle(isTracker, ALERT_TITLE_TRACKER_LIFT),
                subtitle = "Lift: ${String.format(Locale.getDefault(), "%.1f", state.trackerBaroAlt)}m",
                conditionMet = isLift,
                extremeValue = abs(state.trackerBaroAlt).toDouble()
            )
        )

        // 5. GEOFENCE LOGIC
        val tLat = state.trackerLat
        val tLng = state.trackerLng
        val home = state.homePoints
        val maxD = state.maxDistance
        val acc = state.maxTrackerAccuracy
        
        var distVal: Double? = state.distToHomeAuthority?.takeIf { it >= 0.0 }
        
        if (distVal == null && PhysicsUtils.isValidLocation(tLat, tLng) && !isDefaultLocation(tLat, tLng) && home.isNotEmpty()) {
            val validHome = home.filter { 
                PhysicsUtils.isValidLocation(it.lat, it.lng) && !isDefaultLocation(it.lat, it.lng) 
            }
            if (validHome.isNotEmpty()) {
                var minD: Double? = null
                for (h in validHome) {
                    val d = PhysicsUtils.calculateDistance(tLat, tLng, h.lat, h.lng)
                    if (minD == null || d < minD) {
                        minD = d
                    }
                }
                distVal = minD
            }
        }
        
        val accuracyBuffer = ((if (acc > 0) acc else DEFAULT_ACCURACY_FALLBACK) * GEOFENCE_BUFFER_MULT * GEOFENCE_ACCURACY_EXPANSION_MULT)
        val predictiveBuffer = state.trackerSpeed * GEOFENCE_PREDICTIVE_LOOKAHEAD_S
        val threshold = maxD + accuracyBuffer
        val predictiveThreshold = threshold - predictiveBuffer

        val isGeofenceSuppressed = state.isJammerSuspicion || shouldSuppressPeerErrors

        if (distVal != null && !isGeofenceSuppressed && state.lastGpsPacketTs > 0) {
            val dValue = distVal
            val isJump = state.isTrackerVisualJump
            val jumpTier = state.jumpTier
            val isPromoted = state.isTrajectoryPromoted
            val isPredictedExit = dValue > predictiveThreshold && state.trackerSpeed > GEOFENCE_PREDICTIVE_MIN_SPEED_MPS

            if (dValue > threshold || (isPredictedExit && !isJump)) {
                if (state.firstViolationTs == 0L) {
                    state.firstViolationTs = now
                    state.firstViolationWasJump = isJump && (jumpTier == 1 || jumpTier == 2)
                }
                
                if (isPromoted || isPredictedExit) {
                    state.wasDistanceViolated = true
                    state.firstViolationWasJump = false 
                }

                if (!isJump || jumpTier == 3) {
                    if (!isPredictedExit) state.distanceViolationCounter++
                }

                val timeSinceFirst = now - state.firstViolationTs
                
                // Issue #489: Adaptive Jump Confidence - Double hold duration for high-SNR spoofing suspicion (Formerly #219)
                val effectiveHoldMs = if (state.isAdaptiveJump) (JUMP_HOLD_DURATION_MS * ADAPTIVE_JUMP_HOLD_MULTIPLIER).toLong() else JUMP_HOLD_DURATION_MS
                
                val isSustained = if (state.firstViolationWasJump) {
                    timeSinceFirst >= effectiveHoldMs
                } else {
                    state.distanceViolationCounter >= DISTANCE_ALARM_SAMPLES_REQUIRED
                }

                if (isSustained || isPromoted || isPredictedExit) {
                    state.wasDistanceViolated = true
                }
                
                val deviation = dValue - threshold
                val durationSec = timeSinceFirst / 1000
                val debounceStr = when {
                    isPredictedExit -> "PREDICTIVE EXIT (${String.format(Locale.getDefault(), "%.1f", state.trackerSpeed * 3.6)} km/h)"
                    isPromoted -> "TRAJECTORY PROMOTED"
                    isSustained -> "ALARM ACTIVE"
                    state.firstViolationWasJump -> "Jump Hold: ${durationSec}s/${effectiveHoldMs/1000}s${if (state.isAdaptiveJump) " (Adaptive)" else ""}"
                    else -> "Wait: ${state.distanceViolationCounter}/$DISTANCE_ALARM_SAMPLES_REQUIRED"
                }

                val geoTech = String.format(Locale.getDefault(), "Dev: %.1fm (Dist: %.1fm, Fence: %.1fm) (%s)%s", 
                    maxOf(0.1, deviation), dValue, threshold, debounceStr,
                    if (state.isLocationPending) " [LOCATION_PENDING: True]" else "")

                reports.add(
                    ViolationReport(
                        type = ALERT_ID_TRACKER_GEOFENCE,
                        title = getTrackerTitle(isTracker, ALERT_TITLE_TRACKER_GEOFENCE),
                        subtitle = "Tracker is ${ceil(dValue).toInt()}m away from home",
                        conditionMet = !isDistanceGraceActive && (isSustained || isPromoted || isPredictedExit),
                        technicalDetails = geoTech,
                        extremeValue = deviation
                    )
                )
            } else if (dValue <= (threshold - GEOFENCE_HYSTERESIS_METERS) && !isJump) {
                if (state.wasDistanceViolated && state.trackerGpsAccuracy < RETURN_TO_SAFE_RANGE_ACCURACY_LIMIT) {
                    state.wasDistanceViolated = false
                    reports.add(
                        ViolationReport(
                            type = ALERT_ID_TRACKER_GEOFENCE,
                            title = getTrackerTitle(isTracker, ALERT_TITLE_TRACKER_GEOFENCE),
                            subtitle = "Tracker returned to safe range (${ceil(dValue).toInt()}m)",
                            conditionMet = false
                        )
                    )
                }
                state.distanceViolationCounter = 0
                state.firstViolationTs = 0L
                state.firstViolationWasJump = false
                if (reports.none { it.type == ALERT_ID_TRACKER_GEOFENCE }) {
                    reports.add(ViolationReport(type = ALERT_ID_TRACKER_GEOFENCE, title = getTrackerTitle(isTracker, ALERT_TITLE_TRACKER_GEOFENCE), subtitle = "Inside safe range", conditionMet = false))
                }
            } else {
                 reports.add(ViolationReport(type = ALERT_ID_TRACKER_GEOFENCE, title = getTrackerTitle(isTracker, ALERT_TITLE_TRACKER_GEOFENCE), subtitle = "Inside safe range", conditionMet = false))
            }
        } else {
            state.distanceViolationCounter = 0
            state.firstViolationTs = 0L
            state.firstViolationWasJump = false
            reports.add(ViolationReport(type = ALERT_ID_TRACKER_GEOFENCE, title = getTrackerTitle(isTracker, ALERT_TITLE_TRACKER_GEOFENCE), subtitle = "Geofence suppressed or missing data", conditionMet = false))
        }

        // 6. OTHER SENSORS
        val isBatteryBelowThreshold = state.trackerBattery < BATTERY_ALARM_THRESHOLD && state.trackerBattery != -1
        val isCriticalBattery = state.trackerBattery <= CRITICAL_BATTERY_THRESHOLD && state.trackerBattery != -1
        
        val batteryConditionMet = if (isPowerViolation) isCriticalBattery else isBatteryBelowThreshold
        val isChargeDeficit = !isPowerViolation && batteryConditionMet

        reports.add(
            ViolationReport(
                type = ALERT_ID_TRACKER_BATTERY,
                title = getTrackerTitle(isTracker, ALERT_TITLE_TRACKER_BATTERY),
                subtitle = (when {
                    isCriticalBattery -> "Deficit: ${100 - state.trackerBattery}% (Critical battery level)"
                    isChargeDeficit -> "Deficit: ${100 - state.trackerBattery}% (Insufficient charge rate)"
                    else -> "Deficit: ${100 - state.trackerBattery}%"
                }),
                conditionMet = batteryConditionMet,
                extremeValue = (100 - state.trackerBattery).toDouble()
            )
        )

        reports.add(
            ViolationReport(
                type = ALERT_ID_BATTERY_STEEP_DISCHARGE,
                title = getTrackerTitle(isTracker, ALERT_TITLE_BATTERY_STEEP_DISCHARGE),
                subtitle = "Abnormal discharge rate detected",
                conditionMet = state.isBatterySteepDischarge
            )
        )

        reports.add(
            ViolationReport(
                type = ALERT_ID_TRACKER_TEMP,
                title = getTrackerTitle(isTracker, ALERT_TITLE_TRACKER_TEMP),
                subtitle = String.format(Locale.getDefault(), "Temp: %.1f°C", state.trackerTemp),
                conditionMet = state.trackerTemp > MAX_SAFE_TEMPERATURE_CELSIUS || state.isCoolingModeActive,
                extremeValue = state.trackerTemp.toDouble()
            )
        )
        
        reports.add(
            ViolationReport(
                type = ALERT_ID_TRACKER_CHAIR,
                title = getTrackerTitle(isTracker, ALERT_TITLE_TRACKER_CHAIR),
                subtitle = "Chair occupancy detected",
                conditionMet = state.isSitActive,
                technicalDetails = "Vz: ${String.format(Locale.getDefault(), "%.2f", state.verticalVelocity)} m/s"
            )
        )

        reports.add(
            ViolationReport(
                type = ALERT_ID_SYSTEM_STORAGE_LOW,
                title = getTrackerTitle(isTracker, ALERT_TITLE_SYSTEM_STORAGE_LOW),
                subtitle = "System storage is low (< 50MB)",
                conditionMet = state.isStorageLow && !state.isStorageCritical
            )
        )

        reports.add(
            ViolationReport(
                type = ALERT_ID_SYSTEM_STORAGE_CRITICAL,
                title = getTrackerTitle(isTracker, ALERT_TITLE_SYSTEM_STORAGE_CRITICAL),
                subtitle = "CRITICAL STORAGE EMERGENCY (< 10MB)",
                conditionMet = state.isStorageCritical
            )
        )

        // 7. DEVICE SPECIFIC GATING
        
        val uptimeMs = now - state.serviceStartTime
        val isXiaomiBootGraceActive = uptimeMs < XIAOMI_BOOT_GRACE_MS
        
        val isAutostartExplicitlyDenied = state.xiaomiAutostartStatus == EngineXiaomiStatus.DENIED
        val isSpecialExplicitlyDenied = state.xiaomiStatus == EngineXiaomiStatus.DENIED
        
        val isAutostartIndeterminate = state.xiaomiAutostartStatus == EngineXiaomiStatus.UNKNOWN
        val isSpecialIndeterminate = state.xiaomiStatus == EngineXiaomiStatus.UNKNOWN

        val xiaomiViolation = if (state.isXiaomiDevice && !isXiaomiBootGraceActive) {
            when {
                isAutostartExplicitlyDenied || isSpecialExplicitlyDenied -> true
                isAutostartIndeterminate || isSpecialIndeterminate -> !state.isXiaomiManualOverride
                else -> false // Both GRANTED
            }
        } else false
        
        val xiaomiSubtitle = when {
            !state.isXiaomiDevice -> "Not a Xiaomi device"
            isXiaomiBootGraceActive -> "MIUI status stabilizing..."
            isAutostartExplicitlyDenied -> "MIUI Autostart explicitly DENIED - Enable in Phone Setup"
            isSpecialExplicitlyDenied -> "MIUI background permissions explicitly DENIED"
            isAutostartIndeterminate || isSpecialIndeterminate -> "MIUI status UNKNOWN - Toggle manual override in Phone Setup"
            else -> "MIUI status OK"
        }

        val xiaomiTechnical = "MIUI State: autostart=${state.xiaomiAutostartStatus}, special=${state.xiaomiStatus}, override=${state.isXiaomiManualOverride}, grace=$isXiaomiBootGraceActive (Uptime: ${uptimeMs}ms, Threshold: ${XIAOMI_BOOT_GRACE_MS}ms)"

        reports.add(
            ViolationReport(
                type = ALERT_ID_XIAOMI_SYSTEM_MISSING,
                title = getTrackerTitle(isTracker, ALERT_TITLE_XIAOMI_SYSTEM_MISSING),
                subtitle = xiaomiSubtitle,
                conditionMet = xiaomiViolation,
                technicalDetails = xiaomiTechnical
            )
        )

        return SystemHealthReport(reports)
    }

    /**
     * getTrackerTitle: Role-aware title normalization for forensic parity.
     * 1. Always strips "This device:" for local clarity on both roles.
     * 2. Strips the current role's prefix ("Tracker:" or "Viewer:") to keep local alerts clean.
     * 3. Preserves the peer's prefix to distinguish remote alerts.
     */
    private fun getTrackerTitle(isTracker: Boolean, title: String): String {
        val noLocal = title.removePrefix("This device:").trim()
        return if (isTracker) {
            noLocal.removePrefix("Tracker:").trim()
        } else {
            noLocal.removePrefix("Viewer:").trim()
        }
    }

    private fun isDefaultLocation(lat: Double, lng: Double) = abs(lat - DEFAULT_LAT) < 0.0001 && abs(lng - DEFAULT_LNG) < 0.0001
}
