package com.gps19.core.engine

import java.util.*
import kotlin.math.*

/**
 * MainAlarmLogic: Detection logic for system violations.
 * v9.4.00:
 * - Issue #102: Temporal Forensic Integrity. Refactored all timing, duration, 
 *   and debounce logic to use monotonic 'rt' timestamps from AlarmEvaluationState.
 * v9.3.16:
 * - Requirement R999b: Standardized lift detection to use barometer delta 
 *   (Absolute - EMA) instead of raw absolute altitude.
 */
object MainAlarmLogic {

    fun detectViolations(
        state: AlarmEvaluationState,
        isWarmup: Boolean = false
    ): SystemHealthReport {
        val nowRt = state.nowRt
        val reports = mutableListOf<ViolationReport>()
        
        val phase = state.discoveryPhase
        val isTracker = state.isTrackerMode
        val canCheckPeerErrors = phase == DiscoveryPhase.MONITORING
        
        val isDistanceGraceActive = phase == DiscoveryPhase.BOOTSTRAP
        
        // 1. LOCAL ALERTS (R747: Localized to "This device")
        reports.add(
            ViolationReport(
                type = ALERT_ID_LOCAL_INTERNET,
                title = getTrackerTitle(isTracker, ALERT_TITLE_LOCAL_INTERNET),
                subtitle = "This device has no internet access",
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
                subtitle = "Internet is OK but relay unreachable",
                conditionMet = isRelayConditionMet
            )
        )

        // 3. PEER-DEPENDENT ALERTS
        val shouldSuppressPeerErrors = !isInternetHardwareOk || !isRelayConnected
        
        reports.add(
            ViolationReport(
                type = ALERT_ID_TRACKER_OFFLINE,
                title = getTrackerTitle(isTracker, if (isTracker) ALERT_TITLE_VIEWER_OFFLINE else ALERT_TITLE_TRACKER_OFFLINE),
                subtitle = "Device is not connected to relay server",
                conditionMet = canCheckPeerErrors && !state.isTrackerConnected && !shouldSuppressPeerErrors
            )
        )

        reports.add(
            ViolationReport(
                type = ALERT_ID_JUMP_ALERT,
                title = getTrackerTitle(isTracker, ALERT_TITLE_JUMP_ALERT),
                subtitle = "Device data is erratic or jumping",
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
                subtitle = "No data received from device for >${if (isTracker) VIEWER_SIGNAL_LOSS_THRESHOLD_MS/1000 else TRACKER_SIGNAL_LOSS_THRESHOLD_MS/1000}s",
                conditionMet = canCheckPeerErrors && state.isSignalLoss && !shouldSuppressPeerErrors
            )
        )
        
        reports.add(
            ViolationReport(
                type = ALERT_ID_GPS_STALL,
                title = getTrackerTitle(isTracker, ALERT_TITLE_GPS_STALL),
                subtitle = "Device GPS location has not updated",
                conditionMet = canCheckPeerErrors && state.isGpsStalling && !shouldSuppressPeerErrors
            )
        )

        reports.add(
            ViolationReport(
                type = ALERT_ID_TRACKER_GAP,
                title = getTrackerTitle(isTracker, if (isTracker) ALERT_TITLE_VIEWER_GAP else ALERT_TITLE_TRACKER_GAP),
                subtitle = "Device GPS fix is older than ${GPS_GAP_THRESHOLD_MS / 1000}s",
                conditionMet = canCheckPeerErrors && state.isGpsGap && !shouldSuppressPeerErrors
            )
        )

        // 4. STATUS ALERTS
        val isPowerViolation = (state.powerAlarmPending || state.isPowerTamper) && state.trackerCurrentMa <= 0

        reports.add(
            ViolationReport(
                type = ALERT_ID_TRACKER_POWER,
                title = getTrackerTitle(isTracker, ALERT_TITLE_TRACKER_POWER),
                subtitle = "Charger was removed from the device",
                conditionMet = isPowerViolation
            )
        )

        val isShock = SentinelValidator.isShockViolated(state.peakVibrationShock, state.adaptiveVibrationFloor)
        val isTilt = SentinelValidator.isTiltViolated(state.trackerTiltDegrees)
        val isAcousticMet = SentinelValidator.isAcousticViolated(state.trackerAcousticDb, state.acousticFloorDb)
        
        val liftDelta = if (state.trackerBaroAltEma > -999.0) state.trackerBaroAlt - state.trackerBaroAltEma else 0.0
        val isLift = SentinelValidator.isLiftViolated(liftDelta)
        
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
            isLift -> "Lift: ${String.format(Locale.getDefault(), "%.1f", liftDelta)}m"
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
                extremeValue = maxOf(state.peakVibrationShock, state.trackerTiltDegrees)
            )
        )

        reports.add(
            ViolationReport(
                type = ALERT_ID_TRACKER_TILT,
                title = getTrackerTitle(isTracker, ALERT_TITLE_TRACKER_TILT),
                subtitle = String.format(Locale.getDefault(), "%.1f° tilt", state.trackerTiltDegrees),
                conditionMet = isTilt,
                extremeValue = state.trackerTiltDegrees
            )
        )

        val acousticTechnical = if (state.isLocationPending) "LOCATION_PENDING: ${state.locationPendingReason.name}" else null
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
                subtitle = "Lift: ${String.format(Locale.getDefault(), "%.1f", liftDelta)}m",
                conditionMet = isLift,
                extremeValue = abs(liftDelta)
            )
        )

        // 5. GEOFENCE LOGIC
        val tLat = state.trackerLat
        val tLng = state.trackerLng
        val home = state.homePoints
        val maxD = state.maxDistance
        
        // Bayesian Uncertainty Expansion (Issue #460) using monotonic time (Issue #102)
        var acc = state.maxTrackerAccuracy
        if (state.isLocationPending && state.trackerLastValidFixRt > 0) {
            val elapsedSec = (nowRt - state.trackerLastValidFixRt) / 1000.0
            if (elapsedSec > 0) {
                val driftRate = if (state.trackerSpeed > 1.0) {
                    state.trackerSpeed.coerceIn(PENDING_UNCERTAINTY_GROWTH_RATE_MPS, PENDING_UNCERTAINTY_SPEED_CAP_MPS)
                } else {
                    PENDING_UNCERTAINTY_DRIFT_STATIONARY_MPS
                }
                acc += (driftRate * elapsedSec)
            }
        }
        
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

        if (distVal != null && !isGeofenceSuppressed && state.lastGpsPacketRt > 0) {
            val dValue = distVal
            val isJump = state.isTrackerVisualJump
            val jumpTier = state.jumpTier
            val isPromoted = state.isTrajectoryPromoted
            val isPredictedExit = dValue > predictiveThreshold && state.trackerSpeed > GEOFENCE_PREDICTIVE_MIN_SPEED_MPS

            if (dValue > threshold || (isPredictedExit && !isJump)) {
                if (state.firstViolationRt == 0L) {
                    state.firstViolationTs = state.now
                    state.firstViolationRt = nowRt
                    state.firstViolationWasJump = isJump && (jumpTier == 1 || jumpTier == 2)
                }
                
                if (isPromoted || isPredictedExit) {
                    state.wasDistanceViolated = true
                    state.firstViolationWasJump = false 
                }

                if (!isJump || jumpTier == 3) {
                    if (!isPredictedExit) state.distanceViolationCounter++
                }

                val timeSinceFirstRt = nowRt - state.firstViolationRt
                val effectiveHoldMs = if (state.isAdaptiveJump) (JUMP_HOLD_DURATION_MS * ADAPTIVE_JUMP_HOLD_MULTIPLIER).toLong() else JUMP_HOLD_DURATION_MS
                
                val isSustained = if (state.firstViolationWasJump) {
                    timeSinceFirstRt >= effectiveHoldMs
                } else {
                    state.distanceViolationCounter >= DISTANCE_ALARM_SAMPLES_REQUIRED
                }

                if (isSustained || isPromoted || isPredictedExit) {
                    state.wasDistanceViolated = true
                }
                
                val deviation = dValue - threshold
                val durationSec = timeSinceFirstRt / 1000
                val debounceStr = when {
                    isPredictedExit -> "PREDICTIVE EXIT (${String.format(Locale.getDefault(), "%.1f", state.trackerSpeed * 3.6)} km/h)"
                    isPromoted -> "TRAJECTORY PROMOTED"
                    isSustained -> "ALARM ACTIVE"
                    state.firstViolationWasJump -> "Jump Hold: ${durationSec}s/${effectiveHoldMs/1000}s${if (state.isAdaptiveJump) " (Adaptive)" else ""}"
                    else -> "Wait: ${state.distanceViolationCounter}/$DISTANCE_ALARM_SAMPLES_REQUIRED"
                }

                val geoTech = String.format(Locale.getDefault(), "Dev: %.1fm (Dist: %.1fm, Fence: %.1fm) (%s)%s", 
                    maxOf(0.1, deviation), dValue, threshold, debounceStr,
                    if (state.isLocationPending) " [LOCATION_PENDING: ${state.locationPendingReason.name}]" else "")

                reports.add(
                    ViolationReport(
                        type = ALERT_ID_TRACKER_GEOFENCE,
                        title = getTrackerTitle(isTracker, ALERT_TITLE_TRACKER_GEOFENCE),
                        subtitle = "Device is ${ceil(dValue).toInt()}m away from home",
                        conditionMet = !isDistanceGraceActive && (isSustained || isPromoted || isPredictedExit),
                        technicalDetails = geoTech,
                        extremeValue = deviation
                    )
                )
            } else if (dValue <= (threshold - GEOFENCE_HYSTERESIS_METERS) && !isJump) {
                if (state.wasDistanceViolated && state.maxTrackerAccuracy < RETURN_TO_SAFE_RANGE_ACCURACY_LIMIT) {
                    state.wasDistanceViolated = false
                    reports.add(
                        ViolationReport(
                            type = ALERT_ID_TRACKER_GEOFENCE,
                            title = getTrackerTitle(isTracker, ALERT_TITLE_TRACKER_GEOFENCE),
                            subtitle = "Device returned to safe range (${ceil(dValue).toInt()}m)",
                            conditionMet = false
                        )
                    )
                }
                state.distanceViolationCounter = 0
                state.firstViolationTs = 0L
                state.firstViolationRt = 0L
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
            state.firstViolationRt = 0L
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
                    isCriticalBattery -> "Device battery level is at ${state.trackerBattery}% (Critical)"
                    isChargeDeficit -> "Charge Deficit: ${100 - state.trackerBattery}%"
                    else -> "Device battery level is at ${state.trackerBattery}%"
                }),
                conditionMet = batteryConditionMet,
                extremeValue = (100.0 - state.trackerBattery)
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
                subtitle = String.format(Locale.getDefault(), "Device temperature reached %.1f°C", state.trackerTemp),
                conditionMet = state.trackerTemp > MAX_SAFE_TEMPERATURE_CELSIUS || state.isCoolingModeActive,
                extremeValue = state.trackerTemp
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
        val uptimeRt = nowRt - state.serviceStartRt
        val isXiaomiBootGraceActive = uptimeRt < XIAOMI_BOOT_GRACE_MS
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

        reports.add(
            ViolationReport(
                type = ALERT_ID_XIAOMI_SYSTEM_MISSING,
                title = getTrackerTitle(isTracker, ALERT_TITLE_XIAOMI_SYSTEM_MISSING),
                subtitle = xiaomiSubtitle,
                conditionMet = xiaomiViolation
            )
        )

        return SystemHealthReport(reports)
    }

    private fun getTrackerTitle(isTracker: Boolean, title: String): String {
        return if (isTracker) {
            title.removePrefix("Tracker:").trim()
        } else {
            title.removePrefix("Viewer:").trim()
        }
    }

    private fun isDefaultLocation(lat: Double, lng: Double) = abs(lat - DEFAULT_LAT) < 0.0001 && abs(lng - DEFAULT_LNG) < 0.0001
}
