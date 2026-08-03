package com.gps19.core.engine

import java.util.*
import kotlin.math.*

/**
 * MainAlarmLogic: Detection logic for system violations.
 * Aug.01.10:
 * - Issue #668: Performance: Object Churn. Refactored detectViolations to use
 *   a mutable SystemHealthReport flyweight and eliminated hot-path allocations (R-HARDWARE-01).
 * July.29.01:
 * - Issue #623: Performance Audit. Updated to use standardized measureAndAudit helper.
 */
object MainAlarmLogic {

    private val titleCache = mutableMapOf<Pair<Boolean, String>, String>()

    private fun getTrackerTitleCached(isTracker: Boolean, title: String): String {
        val key = isTracker to title
        return titleCache[key] ?: getTrackerTitle(isTracker, title).also { titleCache[key] = it }
    }

    fun detectViolations(
        state: AlarmEvaluationState,
        timeProvider: TimeProvider,
        report: SystemHealthReport,
        onSpike: (message: String, duration: Long) -> Unit,
        isWarmup: Boolean = false
    ): SystemHealthReport {
        return LatencyMonitor.measureAndAudit(
            timeProvider = timeProvider,
            thresholdMs = LATENCY_THRESHOLD_ALARM_LOGIC_MS,
            operation = "detectViolations",
            type = LatencyMonitor.AuditType.PERFORMANCE,
            onSpike = onSpike
        ) {
            val nowRt = state.nowRt
            var reportIdx = 0
            val health = state.health
            
            val phase = state.discoveryPhase
            val isTracker = state.isTrackerMode
            val canCheckPeerErrors = phase == DiscoveryPhase.MONITORING
            
            val isDistanceGraceActive = phase == DiscoveryPhase.BOOTSTRAP
            
            // 1. LOCAL ALERTS
            report.getOrCreate(reportIdx++).update(
                type = ALERT_ID_LOCAL_INTERNET,
                title = getTrackerTitleCached(isTracker, ALERT_TITLE_LOCAL_INTERNET),
                subtitle = "This device has no internet access",
                conditionMet = health.localInternetLoss
            )

            // 2. INFRASTRUCTURE ALERTS
            val isInternetHardwareOk = health.isHardwareOnline
            val isRelayConnected = state.isRelayConnected
            val isRelayConditionMet = !isRelayConnected && isInternetHardwareOk
            
            report.getOrCreate(reportIdx++).update(
                type = ALERT_ID_RELAY_OFFLINE,
                title = getTrackerTitleCached(isTracker, ALERT_TITLE_RELAY_OFFLINE),
                subtitle = "Internet is OK but relay unreachable",
                conditionMet = isRelayConditionMet
            )

            // 3. PEER-DEPENDENT ALERTS
            val shouldSuppressPeerErrors = !isInternetHardwareOk || !isRelayConnected
            
            report.getOrCreate(reportIdx++).update(
                type = ALERT_ID_TRACKER_OFFLINE,
                title = getTrackerTitleCached(isTracker, if (isTracker) ALERT_TITLE_VIEWER_OFFLINE else ALERT_TITLE_TRACKER_OFFLINE),
                subtitle = "Device is not connected to relay server",
                conditionMet = canCheckPeerErrors && !state.isTrackerConnected && !shouldSuppressPeerErrors
            )

            report.getOrCreate(reportIdx++).update(
                type = ALERT_ID_JUMP_ALERT,
                title = getTrackerTitleCached(isTracker, ALERT_TITLE_JUMP_ALERT),
                subtitle = "Device data is erratic or jumping",
                conditionMet = canCheckPeerErrors && health.isJammer && !shouldSuppressPeerErrors
            )

            val visualJumpMet = canCheckPeerErrors && health.status == SentinelStatus.JUMP && !health.isJammer && !shouldSuppressPeerErrors
            report.getOrCreate(reportIdx++).update(
                type = ALERT_ID_VISUAL_JUMP,
                title = getTrackerTitleCached(isTracker, ALERT_TITLE_VISUAL_JUMP),
                subtitle = "Trajectory-based jump detected",
                conditionMet = visualJumpMet,
                technicalDetails = if (visualJumpMet) "Tier: ${state.jumpTier}" else null
            )
            
            report.getOrCreate(reportIdx++).update(
                type = ALERT_ID_SIGNAL_LOSS,
                title = getTrackerTitleCached(isTracker, if (isTracker) ALERT_TITLE_VIEWER_SIGNAL_LOSS else ALERT_TITLE_SIGNAL_LOSS),
                subtitle = "No data received from device", // Constant subtitle to avoid allocation
                conditionMet = canCheckPeerErrors && health.signalLoss && !shouldSuppressPeerErrors
            )
            
            report.getOrCreate(reportIdx++).update(
                type = ALERT_ID_GPS_STALL,
                title = getTrackerTitleCached(isTracker, ALERT_TITLE_GPS_STALL),
                subtitle = "Device GPS location has not updated",
                conditionMet = canCheckPeerErrors && health.gpsStalled && !shouldSuppressPeerErrors
            )

            report.getOrCreate(reportIdx++).update(
                type = ALERT_ID_TRACKER_GAP,
                title = getTrackerTitleCached(isTracker, if (isTracker) ALERT_TITLE_VIEWER_GAP else ALERT_TITLE_TRACKER_GAP),
                subtitle = "Device GPS fix is older than threshold",
                conditionMet = canCheckPeerErrors && state.isGpsGap && !shouldSuppressPeerErrors
            )

            // 4. STATUS ALERTS
            val isPowerViolation = (health.isPowerTamper) && health.currentMa <= 0

            report.getOrCreate(reportIdx++).update(
                type = ALERT_ID_TRACKER_POWER,
                title = getTrackerTitleCached(isTracker, ALERT_TITLE_TRACKER_POWER),
                subtitle = "Charger was removed from the device",
                conditionMet = isPowerViolation
            )

            val isShock = SentinelValidator.isShockViolated(health.peakVibrationShock, health.adaptiveVibrationFloor)
            val isTilt = SentinelValidator.isTiltViolated(health.tiltDegrees)
            val isAcousticMet = SentinelValidator.isAcousticViolated(health.acousticDb, health.acousticFloorDb)
            
            val liftDelta = if (state.trackerBaroAltEma > -999.0) state.health.baroAlt - state.trackerBaroAltEma else 0.0
            val isLift = SentinelValidator.isLiftViolated(liftDelta)
            
            val isLightMet = SentinelValidator.isLightViolated(health.lux, health.luxBaseline)

            val isTamperCondition = health.status == SentinelStatus.TAMPER ||
                                    health.isTamperDetected || 
                                    (!health.isNear) || 
                                    isLightMet || 
                                    isShock || isTilt || isAcousticMet || isLift || health.isPowerTamper

            val tamperSubtitle = if (isTamperCondition) {
                when {
                    health.status == SentinelStatus.TAMPER -> "Hardware sentinel violation"
                    isShock -> "Shock: ${String.format(Locale.getDefault(), "%.1f", health.peakVibrationShock)}G"
                    isLightMet -> "Light: ${health.lux.roundToInt()} lux"
                    !health.isNear -> "Proximity sensor cleared"
                    isTilt -> String.format(Locale.getDefault(), "%.1f° tilt", health.tiltDegrees)
                    isAcousticMet -> String.format(Locale.getDefault(), "%.1f dB peak", health.acousticDb)
                    isLift -> "Lift: ${String.format(Locale.getDefault(), "%.1f", liftDelta)}m"
                    health.isPowerTamper -> "Power source tamper"
                    health.isTamperDetected -> "Hardware tamper flag"
                    else -> "Device handled or uncovered"
                }
            } else "Device handled or uncovered"

            report.getOrCreate(reportIdx++).update(
                type = ALERT_ID_TRACKER_TAMPER,
                title = getTrackerTitleCached(isTracker, ALERT_TITLE_TRACKER_TAMPER),
                subtitle = tamperSubtitle,
                conditionMet = isTamperCondition,
                extremeValue = maxOf(health.peakVibrationShock, health.tiltDegrees)
            )

            report.getOrCreate(reportIdx++).update(
                type = ALERT_ID_TRACKER_TILT,
                title = getTrackerTitleCached(isTracker, ALERT_TITLE_TRACKER_TILT),
                subtitle = if (isTilt) String.format(Locale.getDefault(), "%.1f° tilt", health.tiltDegrees) else "Tilt violation",
                conditionMet = isTilt,
                extremeValue = health.tiltDegrees
            )

            report.getOrCreate(reportIdx++).update(
                type = ALERT_ID_TRACKER_ACOUSTIC,
                title = getTrackerTitleCached(isTracker, ALERT_TITLE_TRACKER_ACOUSTIC),
                subtitle = if (isAcousticMet) String.format(Locale.getDefault(), "%.1f dB peak (Base: %.1f)", health.acousticDb, health.acousticFloorDb) else "Acoustic violation",
                conditionMet = isAcousticMet,
                extremeValue = health.acousticDb,
                technicalDetails = if (isAcousticMet && health.isLocationPending) "LOCATION_PENDING: ${health.locationPendingReason.name}" else null
            )

            report.getOrCreate(reportIdx++).update(
                type = ALERT_ID_TRACKER_LIFT,
                title = getTrackerTitleCached(isTracker, ALERT_TITLE_TRACKER_LIFT),
                subtitle = if (isLift) "Lift: ${String.format(Locale.getDefault(), "%.1f", liftDelta)}m" else "Lift violation",
                conditionMet = isLift,
                extremeValue = abs(liftDelta)
            )

            // 5. GEOFENCE LOGIC
            val tLat = state.trackerLat
            val tLng = state.trackerLng
            val home = state.homePoints
            val maxD = state.maxDistance
            
            var acc = state.maxTrackerAccuracy
            if (health.isLocationPending && state.trackerLastValidFixRt > 0) {
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
                var minD: Double? = null
                for (i in 0 until home.size) {
                    val h = home[i]
                    if (PhysicsUtils.isValidLocation(h.lat, h.lng) && !isDefaultLocation(h.lat, h.lng)) {
                        val d = PhysicsUtils.calculateDistance(tLat, tLng, h.lat, h.lng)
                        if (minD == null || d < minD) {
                            minD = d
                        }
                    }
                }
                distVal = minD
            }
            
            val accuracyBuffer = ((if (acc > 0) acc else DEFAULT_ACCURACY_FALLBACK) * GEOFENCE_BUFFER_MULT * GEOFENCE_ACCURACY_EXPANSION_MULT)
            val predictiveBuffer = state.trackerSpeed * GEOFENCE_PREDICTIVE_LOOKAHEAD_S
            val threshold = maxD + accuracyBuffer
            val predictiveThreshold = threshold - predictiveBuffer

            val isGeofenceSuppressed = health.isJammer || shouldSuppressPeerErrors

            if (distVal != null && !isGeofenceSuppressed && state.lastGpsPacketRt > 0) {
                val dValue = distVal
                val isJump = health.status == SentinelStatus.JUMP
                val jumpTier = state.jumpTier
                val isPredictedExit = dValue > predictiveThreshold && state.trackerSpeed > GEOFENCE_PREDICTIVE_MIN_SPEED_MPS

                if (dValue > threshold || (isPredictedExit && !isJump)) {
                    if (state.firstViolationRt == 0L) {
                        state.firstViolationTs = state.now
                        state.firstViolationRt = nowRt
                        state.firstViolationWasJump = isJump && (jumpTier == 1 || jumpTier == 2)
                    }
                    
                    if (isPredictedExit) {
                        state.wasDistanceViolated = true
                        state.firstViolationWasJump = false 
                    }

                    if (!isJump || jumpTier == 3) {
                        if (!isPredictedExit) state.distanceViolationCounter++
                    }

                    val timeSinceFirstRt = nowRt - state.firstViolationRt
                    val effectiveHoldMs = if (state.isAdaptiveJump) (JUMP_HOLD_DURATION_MS * 2).toLong() else JUMP_HOLD_DURATION_MS
                    
                    val isSustained = if (state.firstViolationWasJump) {
                        timeSinceFirstRt >= effectiveHoldMs
                    } else {
                        state.distanceViolationCounter >= DISTANCE_ALARM_SAMPLES_REQUIRED
                    }

                    if (isSustained || isPredictedExit) {
                        state.wasDistanceViolated = true
                    }
                    
                    val deviation = dValue - threshold
                    
                    val geoTech = if (isSustained || isPredictedExit) {
                        val timeSinceFirstRt_ = nowRt - state.firstViolationRt
                        val durationSec = timeSinceFirstRt_ / 1000
                        val debounceStr = when {
                            isPredictedExit -> "PREDICTIVE EXIT"
                            isSustained -> "ALARM ACTIVE"
                            state.firstViolationWasJump -> "Jump Hold: ${durationSec}s"
                            else -> "Wait: ${state.distanceViolationCounter}/$DISTANCE_ALARM_SAMPLES_REQUIRED"
                        }
                        String.format(Locale.getDefault(), "Dev: %.1fm (Dist: %.1fm) (%s)", maxOf(0.1, deviation), dValue, debounceStr)
                    } else null

                    report.getOrCreate(reportIdx++).update(
                        type = ALERT_ID_TRACKER_GEOFENCE,
                        title = getTrackerTitleCached(isTracker, ALERT_TITLE_TRACKER_GEOFENCE),
                        subtitle = if (isSustained || isPredictedExit) "Device is ${ceil(dValue).toInt()}m away from home" else "Inside safe range",
                        conditionMet = !isDistanceGraceActive && (isSustained || isPredictedExit),
                        technicalDetails = geoTech,
                        extremeValue = deviation
                    )
                } else if (dValue <= (threshold - GEOFENCE_HYSTERESIS_METERS) && !isJump) {
                    if (state.wasDistanceViolated && state.maxTrackerAccuracy < RETURN_TO_SAFE_RANGE_ACCURACY_LIMIT) {
                        state.wasDistanceViolated = false
                        report.getOrCreate(reportIdx++).update(
                            type = ALERT_ID_TRACKER_GEOFENCE,
                            title = getTrackerTitleCached(isTracker, ALERT_TITLE_TRACKER_GEOFENCE),
                            subtitle = "Device returned to safe range",
                            conditionMet = false
                        )
                    }
                    state.distanceViolationCounter = 0
                    state.firstViolationTs = 0L
                    state.firstViolationRt = 0L
                    state.firstViolationWasJump = false
                    
                    var found = false
                    for (i in 0 until reportIdx) {
                        if (report.reports[i].type == ALERT_ID_TRACKER_GEOFENCE) {
                            found = true; break
                        }
                    }
                    if (!found) {
                        report.getOrCreate(reportIdx++).update(
                            type = ALERT_ID_TRACKER_GEOFENCE,
                            title = getTrackerTitleCached(isTracker, ALERT_TITLE_TRACKER_GEOFENCE),
                            subtitle = "Inside safe range",
                            conditionMet = false
                        )
                    }
                } else {
                     report.getOrCreate(reportIdx++).update(
                         type = ALERT_ID_TRACKER_GEOFENCE,
                         title = getTrackerTitleCached(isTracker, ALERT_TITLE_TRACKER_GEOFENCE),
                         subtitle = "Inside safe range",
                         conditionMet = false
                     )
                }
            } else {
                state.distanceViolationCounter = 0
                state.firstViolationTs = 0L
                state.firstViolationRt = 0L
                state.firstViolationWasJump = false
                report.getOrCreate(reportIdx++).update(
                    type = ALERT_ID_TRACKER_GEOFENCE,
                    title = getTrackerTitleCached(isTracker, ALERT_TITLE_TRACKER_GEOFENCE),
                    subtitle = "Geofence suppressed or missing data",
                    conditionMet = false
                )
            }

            // 6. OTHER SENSORS
            val isBatteryBelowThreshold = health.batteryLevel < BATTERY_ALARM_THRESHOLD && health.batteryLevel != -1
            val isCriticalBattery = health.batteryLevel <= CRITICAL_BATTERY_THRESHOLD && health.batteryLevel != -1
            
            val batteryConditionMet = if (isPowerViolation) isCriticalBattery else isBatteryBelowThreshold
            val isChargeDeficit = !isPowerViolation && batteryConditionMet

            val batterySubtitle = if (batteryConditionMet) {
                when {
                    isCriticalBattery -> "Device battery level is CRITICAL"
                    isChargeDeficit -> "Charge Deficit detected"
                    else -> "Low battery"
                }
            } else "Device battery OK"

            report.getOrCreate(reportIdx++).update(
                type = ALERT_ID_TRACKER_BATTERY,
                title = getTrackerTitleCached(isTracker, ALERT_TITLE_TRACKER_BATTERY),
                subtitle = batterySubtitle,
                conditionMet = batteryConditionMet,
                extremeValue = (100.0 - health.batteryLevel)
            )

            report.getOrCreate(reportIdx++).update(
                type = ALERT_ID_BATTERY_STEEP_DISCHARGE,
                title = getTrackerTitleCached(isTracker, ALERT_TITLE_BATTERY_STEEP_DISCHARGE),
                subtitle = "Abnormal discharge rate detected",
                conditionMet = health.isBatterySteepDischarge
            )

            val tempCondition = health.batteryTemp > MAX_SAFE_TEMPERATURE_CELSIUS || health.isCoolingModeActive
            report.getOrCreate(reportIdx++).update(
                type = ALERT_ID_TRACKER_TEMP,
                title = getTrackerTitleCached(isTracker, ALERT_TITLE_TRACKER_TEMP),
                subtitle = if (tempCondition) String.format(Locale.getDefault(), "Temperature reached %.1f°C", health.batteryTemp) else "Temperature OK",
                conditionMet = tempCondition,
                extremeValue = health.batteryTemp
            )
            
            report.getOrCreate(reportIdx++).update(
                type = ALERT_ID_SYSTEM_STORAGE_LOW,
                title = getTrackerTitleCached(isTracker, ALERT_TITLE_SYSTEM_STORAGE_LOW),
                subtitle = "System storage is low",
                conditionMet = health.isStorageLow && !health.isStorageCritical
            )

            report.getOrCreate(reportIdx++).update(
                type = ALERT_ID_SYSTEM_STORAGE_CRITICAL,
                title = getTrackerTitleCached(isTracker, ALERT_TITLE_SYSTEM_STORAGE_CRITICAL),
                subtitle = "CRITICAL STORAGE EMERGENCY",
                conditionMet = health.isStorageCritical
            )

            // 7. HARDWARE CONFIGURATION GATING
            val uptimeRt = nowRt - state.serviceStartRt
            val isBootGraceActive = uptimeRt < HARDWARE_BOOT_GRACE_MS
            val caps = state.capabilities
            
            val isExplicitlyDenied = caps.backgroundStatus == CapabilityStatus.DENIED || 
                                     caps.autostartStatus == CapabilityStatus.DENIED
            val isIndeterminate = caps.backgroundStatus == CapabilityStatus.UNKNOWN || 
                                 caps.autostartStatus == CapabilityStatus.UNKNOWN

            val configViolation = if (caps.hasBackgroundRestriction && !isBootGraceActive) {
                when {
                    isExplicitlyDenied -> true
                    isIndeterminate -> !caps.isManualOverrideActive
                    else -> false
                }
            } else false

            val configSubtitle = when {
                !caps.hasBackgroundRestriction -> "Hardware configuration OK"
                isBootGraceActive -> "Hardware status stabilizing..."
                isExplicitlyDenied -> "Background/Autostart explicitly DENIED"
                isIndeterminate -> "Hardware status UNKNOWN"
                else -> "Hardware configuration OK"
            }

            report.getOrCreate(reportIdx++).update(
                type = ALERT_ID_HARDWARE_CONFIGURATION,
                title = getTrackerTitleCached(isTracker, ALERT_TITLE_HARDWARE_CONFIGURATION),
                subtitle = configSubtitle,
                conditionMet = configViolation
            )

            report.truncate(reportIdx)
            report
        }
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
