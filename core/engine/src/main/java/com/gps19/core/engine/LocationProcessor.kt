package com.gps19.core.engine

import java.util.Locale
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.math.*

/**
 * ProcessorEvent: Reactive event container for location processing results.
 */
sealed class ProcessorEvent {
    data class TrailPointSaved(val lat: Double, val lng: Double, val isViewerTrail: Boolean, val status: SentinelStatus, val timestamp: Long, val accuracy: Double, val maxAccuracy: Double) : ProcessorEvent()
    data class LogAdded(val message: String, val type: String, val isImportant: Boolean, val isSpecial: Boolean, val lat: Double, val lng: Double, val accuracy: Double, val snr: Double?, val vibe: Double?) : ProcessorEvent()
    data class MaxAccuracyChanged(val accuracy: Double) : ProcessorEvent()
    data class ChairBaselineChanged(val baseline: Double) : ProcessorEvent()
    data class GpsStallDetected(val rt: Long) : ProcessorEvent()
}

/**
 * LocationProcessor: Handles accuracy filtering and coordinate processing.
 * July.29.01:
 * - Issue #623: Structural: Latency Monitor Metric Cleanup. Standardized spike 
 *   reporting strings and migrated to measureAndAudit API.
 * July.28.22:
 * - Issue #617: Global SharedFlow Audit. Hardened _processorEvents with 
 *   BufferOverflow.DROP_OLDEST to ensure non-blocking location processing (R617).
 */
class LocationProcessor(
    private val timeProvider: TimeProvider
) {
    private val _processorEvents = MutableSharedFlow<ProcessorEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val processorEvents: SharedFlow<ProcessorEvent> = _processorEvents.asSharedFlow()

    val sentinel = LocationSentinel()
    
    private val anchorEvaluator = AnchorEvaluator { msg, lat, lng, acc, vibe ->
        _processorEvents.tryEmit(ProcessorEvent.LogAdded(msg, "system", false, false, lat, lng, acc, null, vibe))
    }

    private var lastProcessedAccuracy = 0.0
    private var maxAccuracy = 0.0
    
    private val accuracyWindowBuffer = DoubleArray(ACCURACY_WINDOW_MAX_SIZE)
    private var accuracyWindowSize = 0
    private var accuracyWindowHead = 0
    
    private var lastWindowUpdateRt = 0L // Monotonic
    
    private var lastValidFixRt = 0L // Monotonic
    private var lastLat = 0.0
    private var lastLng = 0.0
    private var lastTs = 0L // Wall-clock
    private var lastRt = 0L // Monotonic
    private var lastAcc = 0.0
    private var lastMaxAcc = 0.0

    private var lastSavedLat = 0.0
    private var lastSavedLng = 0.0
    private var lastSavedTs = 0L // Wall-clock
    private var lastSavedRt = 0L // Monotonic
    private var lastSavedGpsTs = 0L

    private var lastHighAccLat = 0.0
    private var lastHighAccLng = 0.0
    private var lastHighAccTs = 0L // Wall-clock
    private var lastHighAccRt = 0L // Monotonic

    private var cachedHomePoints: List<EngineGeoPoint>? = null
    private var maxDistanceAuthority: Double = 60.0

    fun loadState(
        savedMaxAccuracy: Double,
        savedLastSitTs: Long,
        savedBaseline: Double,
        trackerState: SpatialAnchor?,
        homePoints: List<EngineGeoPoint>,
        maxDistance: Double
    ) {
        if (savedMaxAccuracy > 0.0) {
            maxAccuracy = savedMaxAccuracy
            addAccuracyToWindow(savedMaxAccuracy)
        }
        
        sentinel.loadForensicState(savedLastSitTs, savedBaseline)
        
        if (trackerState != null && trackerState.lat != 0.0) {
            lastLat = trackerState.lat
            lastLng = trackerState.lng
            lastTs = trackerState.gpsTs
            lastRt = timeProvider.elapsedRealtime()
            sentinel.setSpatialAnchor(trackerState.lat, trackerState.lng, trackerState.alt, trackerState.gpsTs, lastRt)
        }

        this.cachedHomePoints = homePoints
        this.maxDistanceAuthority = maxDistance
        anchorEvaluator.reset()
    }

    private fun addAccuracyToWindow(acc: Double) {
        accuracyWindowBuffer[accuracyWindowHead] = acc
        accuracyWindowHead = (accuracyWindowHead + 1) % ACCURACY_WINDOW_MAX_SIZE
        if (accuracyWindowSize < ACCURACY_WINDOW_MAX_SIZE) accuracyWindowSize++
    }

    private fun updateLastAccuracyInWindow(acc: Double) {
        if (accuracyWindowSize > 0) {
            val lastIdx = (accuracyWindowHead - 1 + ACCURACY_WINDOW_MAX_SIZE) % ACCURACY_WINDOW_MAX_SIZE
            accuracyWindowBuffer[lastIdx] = acc
        } else {
            addAccuracyToWindow(acc)
        }
    }

    private fun getMaxAccuracyFromWindow(): Double {
        if (accuracyWindowSize == 0) return 0.0
        var m = 0.0
        for (i in 0 until accuracyWindowSize) {
            m = max(m, accuracyWindowBuffer[i])
        }
        return m
    }

    fun setMaxDistanceAuthority(distance: Double) {
        this.maxDistanceAuthority = distance
    }

    fun setHomePoints(points: List<EngineGeoPoint>) {
        this.cachedHomePoints = points
    }

    fun getLastProcessedAccuracy() = lastProcessedAccuracy
    fun getMaxTrackerAccuracy() = maxAccuracy
    fun getLastValidFixRt() = lastValidFixRt
    fun setLastValidFixRt(rt: Long) { lastValidFixRt = rt }
    fun getMaxDistanceAuthority() = maxDistanceAuthority

    fun getLuxBaseline() = sentinel.luxBaseline
    fun getBaroBaseline() = sentinel.baroBaseline
    fun getAcousticFloorDb() = sentinel.acousticFloorDb
    fun getAdaptiveVibrationFloor() = sentinel.adaptiveVibrationFloor
    fun getPeakVibrationShock() = sentinel.peakVibrationShock
    fun getPeakVibrationShockRt() = sentinel.peakVibrationShockRt
    
    fun getChairBaselineTilt() = sentinel.baselineSitTilt
    fun getLastSitTs() = sentinel.lastSitTs
    fun getLastSitRt() = sentinel.lastSitRt

    fun consumeSitDetected(): Boolean = sentinel.consumeSitDetected()

    fun updateSensorData(
        vibration: Double, heading: Double, baroAlt: Double, 
        lux: Double = 0.0, isNear: Boolean = true, powerTamper: Boolean = false,
        tiltDegrees: Double = 0.0, acousticDb: Double = 0.0, peakShock: Double = 0.0,
        acousticMinDb: Double = -1.0, peakVerticalVelocity: Double = 0.0,
        plungeMatched: Boolean = false, peakVerticalVelocityTs: Long = 0L, 
        peakVerticalVelocityRt: Long = 0L,
        peakVerticalDisplacement: Double = 0.0,
        isSirenActive: Boolean = false, isWarming: Boolean = false,
        manualAdaptiveFloor: Double = -1.0, acousticLockoutRt: Long = 0L,
        isMuzzled: Boolean = false,
        kineticEnergy: Double = 0.0,
        nowRt: Long = timeProvider.elapsedRealtime(),
        nowWall: Long = timeProvider.currentTimeMillis()
    ): Boolean {
        return LatencyMonitor.measureAndAudit(
            timeProvider,
            LATENCY_THRESHOLD_SENSOR_PROCESS_MS,
            "updateSensorData",
            LatencyMonitor.AuditType.PERFORMANCE,
            { message, _ ->
                _processorEvents.tryEmit(ProcessorEvent.LogAdded(message, "system", false, true, 0.0, 0.0, 0.0, null, vibration))
            }
        ) {
            val baselineChanged = sentinel.updateSensorState(
                vibration, heading, baroAlt, lux, isNear, powerTamper, tiltDegrees, 
                acousticDb, peakShock, acousticMinDb, peakVerticalVelocity, peakVerticalVelocityTs, peakVerticalVelocityRt, plungeMatched, peakVerticalDisplacement,
                isSirenActive, isWarming, manualAdaptiveFloor, acousticLockoutRt, isMuzzled, kineticEnergy, nowRt, nowWall
            )
            if (baselineChanged) {
                _processorEvents.tryEmit(ProcessorEvent.ChairBaselineChanged(sentinel.baselineSitTilt))
            }
            baselineChanged
        }
    }

    fun resetChairBaseline() {
        sentinel.resetChairBaseline()
        _processorEvents.tryEmit(ProcessorEvent.ChairBaselineChanged(sentinel.baselineSitTilt))
    }

    fun shouldThrottlePolling(providedIsStationary: Boolean? = null): Boolean = sentinel.shouldThrottlePolling(providedIsStationary)

    fun updateWindowedAccuracy(acc: Double) {
        if (acc <= 0.0) return
        val nowRt = timeProvider.elapsedRealtime()
        val bucketDuration = ACCURACY_WINDOW_BUCKET_MS / ACCURACY_WINDOW_MAX_SIZE
        
        if (accuracyWindowSize == 0 || (lastWindowUpdateRt > 0 && nowRt - lastWindowUpdateRt >= bucketDuration)) {
            addAccuracyToWindow(acc)
            lastWindowUpdateRt = nowRt
        } else {
            if (lastWindowUpdateRt == 0L) lastWindowUpdateRt = nowRt
            val lastIdx = (accuracyWindowHead - 1 + ACCURACY_WINDOW_MAX_SIZE) % ACCURACY_WINDOW_MAX_SIZE
            val currentMaxInBucket = accuracyWindowBuffer[lastIdx]
            if (acc > currentMaxInBucket * GEOFENCE_ACCURACY_HYSTERESIS_MULT) {
                updateLastAccuracyInWindow(acc)
            }
        }
        
        val rawMax = getMaxAccuracyFromWindow()
        val newMax = (rawMax * 10.0).roundToLong() / 10.0
        
        if (abs(newMax - maxAccuracy) > 0.05) {
            maxAccuracy = newMax
            _processorEvents.tryEmit(ProcessorEvent.MaxAccuracyChanged(maxAccuracy))
        }
    }

    private var lastNearestHomeDistance: Double? = null

    data class ProcessedLocation(
        val rawPoint: EngineGeoPoint,
        val optimizedPoint: EngineGeoPoint,
        val status: SentinelStatus,
        val maxAccuracy: Double,
        val currentAccuracy: Double,
        val filteredSpeed: Double,
        val timestamp: Long,
        val rt: Long = 0L, // Monotonic
        val isStalled: Boolean = false,
        val isClockRegression: Boolean = false,
        val receiptRt: Long = 0L, // Monotonic (Issue #102)
        val isTrajectoryPromoted: Boolean = false,
        val isAdaptiveJump: Boolean = false,
        val jumpTier: Int = 0,
        val distToHome: Double? = null,
        val isSpatiallyValid: Boolean = true,
        val geofenceViolationDetected: Boolean = false,
        val tamperDetected: Boolean = false,
        val jammerDetected: Boolean = false,
        val isAnchorLocked: Boolean = false,
        val suppressionNote: String? = null,
        val kineticEnergy: Double = 0.0
    )

    fun processGpsPoint(
        lat: Double, lng: Double, alt: Double, androidSpeedMps: Double, 
        gpsTs: Long, accuracy: Double, bearing: Double, snr: Double, satsUsed: Int, 
        isViewerTrail: Boolean, lastGpsTs: Long, isLocal: Boolean = false,
        providedMaxAccuracy: Double = 0.0, providedStatus: SentinelStatus? = null,
        providedJumpTier: Int = 0,
        providedIsJump: Boolean = false,
        providedIsTrajectoryPromoted: Boolean = false,
        providedIsAdaptiveJump: Boolean = false,
        providedIsJammer: Boolean = false, providedIsStalled: Boolean = false,
        providedIsTamper: Boolean = false, providedAdaptiveVibrationFloor: Double = -1.0,
        providedAcousticLockoutRt: Long = 0L,
        isSuspicious: Boolean = false, 
        isMuzzled: Boolean = false,
        isAdaptationMuzzled: Boolean = false,
        providedKineticEnergy: Double = 0.0,
        nowWall: Long = timeProvider.currentTimeMillis(),
        nowRt: Long = timeProvider.elapsedRealtime()
    ): ProcessedLocation {
        return LatencyMonitor.measureAndAudit(
            timeProvider,
            LATENCY_THRESHOLD_GPS_PROCESS_MS,
            "processGpsPoint",
            LatencyMonitor.AuditType.PERFORMANCE,
            { message, _ ->
                _processorEvents.tryEmit(ProcessorEvent.LogAdded(message, "system", false, true, lat, lng, accuracy, snr, sentinel.currentVibrationIndex))
            }
        ) {
            val effectiveTs = if (gpsTs > 0) gpsTs else nowWall

            if (lastTs > 0 && effectiveTs < lastTs) {
                val delta = lastTs - effectiveTs
                if (delta > CLOCK_REGRESSION_GATE_MS) { 
                    _processorEvents.tryEmit(ProcessorEvent.LogAdded("Merge-on-Stale: Coordinate update bypassed due to hardware clock regression (${delta}ms). Merging status-only data.", "system", false, true, 0.0, 0.0, 0.0, snr, sentinel.currentVibrationIndex))
                    if (delta > 86400000L) { lastTs = 0L; lastRt = 0L; sentinel.reset() }
                }
                val status = providedStatus ?: when {
                    providedIsTamper -> SentinelStatus.TAMPER
                    providedIsJammer -> SentinelStatus.JUMP
                    else -> SentinelStatus.VALID
                }
                val fallbackCoordPoint = EngineGeoPoint(if (lastLat != 0.0) lastLat else lat, if (lastLng != 0.0) lastLng else lng, alt = alt, ts = if (lastTs != 0L) lastTs else effectiveTs, rt = if (lastRt != 0L) lastRt else nowRt, accuracy = accuracy, maxAccuracy = maxAccuracy)
                return@measureAndAudit ProcessedLocation(rawPoint = fallbackCoordPoint, optimizedPoint = fallbackCoordPoint, status = status, maxAccuracy = maxAccuracy, currentAccuracy = accuracy, filteredSpeed = sentinel.getEstimatedSpeedMps(), timestamp = effectiveTs, rt = nowRt, isStalled = providedIsStalled, isClockRegression = true, receiptRt = nowRt, isTrajectoryPromoted = providedIsTrajectoryPromoted, jumpTier = providedJumpTier, isAdaptiveJump = providedIsAdaptiveJump, distToHome = lastNearestHomeDistance, isSpatiallyValid = true, tamperDetected = providedIsTamper, jammerDetected = providedIsJammer, kineticEnergy = providedKineticEnergy)
            }

            val TRAJECTORY_PROMOTION_WINDOW_MS = 60000L
            if (accuracy > HIGH_ACCURACY_THRESHOLD_METERS * TRAJECTORY_REJECTION_ACCURACY_MULT && lastHighAccRt > 0 && nowRt - lastHighAccRt < TRAJECTORY_PROMOTION_WINDOW_MS) {
                if (PhysicsUtils.calculateDistance(lat, lng, lastHighAccLat, lastHighAccLng) > accuracy) {
                    val fallbackCoordPoint = EngineGeoPoint(if (lastLat != 0.0) lastLat else lat, if (lastLng != 0.0) lastLng else lng, alt = alt, ts = if (lastTs != 0L) lastTs else effectiveTs, rt = if (lastRt != 0L) lastRt else nowRt, accuracy = accuracy, maxAccuracy = maxAccuracy)
                    return@measureAndAudit ProcessedLocation(rawPoint = EngineGeoPoint(lat, lng, alt = alt, ts = effectiveTs, rt = nowRt, accuracy = accuracy, maxAccuracy = maxAccuracy), optimizedPoint = fallbackCoordPoint, status = SentinelStatus.VALID, maxAccuracy = maxAccuracy, currentAccuracy = accuracy, filteredSpeed = sentinel.getEstimatedSpeedMps(), timestamp = effectiveTs, rt = nowRt, isStalled = if (isLocal) false else providedIsStalled, receiptRt = nowRt, jumpTier = providedJumpTier, isAdaptiveJump = providedIsAdaptiveJump, distToHome = lastNearestHomeDistance, isSpatiallyValid = false, tamperDetected = providedIsTamper, jammerDetected = providedIsTamper, kineticEnergy = providedKineticEnergy)
                }
            }
            
            if (accuracy <= HIGH_ACCURACY_THRESHOLD_METERS) { lastHighAccLat = lat; lastHighAccLng = lng; lastHighAccTs = nowWall; lastHighAccRt = nowRt }
            if (isLocal) updateWindowedAccuracy(accuracy) else if (providedMaxAccuracy > 0.0) maxAccuracy = providedMaxAccuracy
            if (providedAdaptiveVibrationFloor >= 0.0) sentinel.adaptiveVibrationFloor = providedAdaptiveVibrationFloor
            if (providedAcousticLockoutRt > 0) sentinel.updateSensorState(vibration = -1.0, heading = -1.0, baroAlt = -1000.0, acousticLockoutRt = providedAcousticLockoutRt, isMuzzled = isMuzzled, nowRt = nowRt, nowTs = nowWall)

            val sentinelResult = sentinel.processLocation(
                lat = lat, lng = lng, alt = alt, accuracy = accuracy, maxAccuracy = maxAccuracy, 
                bearing = bearing, snr = snr, satsUsed = satsUsed, timestamp = effectiveTs, 
                bypassBehavioral = !isLocal, isSuspicious = isSuspicious || isAdaptationMuzzled,
                isMuzzled = isMuzzled, nowTs = nowWall, nowRt = nowRt
            )
            
            if (sentinelResult.status == SentinelStatus.TRAJECTORY_PROMOTED) {
                val promotedPoints = sentinelResult.promotedPoints ?: emptyList<EngineGeoPoint>()
                if (promotedPoints.isNotEmpty() && lastLat != 0.0) {
                    val firstPromoted = promotedPoints.first()
                    val interpolated = PhysicsUtils.interpolateSegment(
                        lastLat, lastLng, lastTs, firstPromoted.lat, firstPromoted.lng, firstPromoted.ts,
                        startAcc = lastAcc, startMaxAcc = lastMaxAcc, endAcc = accuracy, endMaxAcc = maxAccuracy
                    )
                    interpolated.forEach { p ->
                        _processorEvents.tryEmit(ProcessorEvent.TrailPointSaved(p.lat, p.lng, isViewerTrail, SentinelStatus.VALID, p.ts, accuracy = p.accuracy, maxAccuracy = p.maxAccuracy))
                    }
                }
                promotedPoints.forEach { p ->
                    _processorEvents.tryEmit(ProcessorEvent.TrailPointSaved(p.lat, p.lng, isViewerTrail, SentinelStatus.VALID, p.ts, accuracy = p.accuracy, maxAccuracy = p.maxAccuracy))
                }
            }

            val isActualJump = (sentinelResult.status == SentinelStatus.JUMP || sentinelResult.status == SentinelStatus.OUTLIER || sentinelResult.status == SentinelStatus.JITTER || (sentinelResult.jumpConfidence?.isJump == true))
            val isMuzzledJump = isAdaptationMuzzled && (sentinelResult.status == SentinelStatus.JUMP || sentinelResult.status == SentinelStatus.JITTER)
            val finalStatus = if (isMuzzledJump) SentinelStatus.VALID else sentinelResult.status
            val finalSuppressionNote = if (isMuzzledJump) "Settling A15 Polling..." else sentinelResult.reason

            val isActualJammer = (sentinelResult.status == SentinelStatus.JAMMER_SUSPICION || (sentinelResult.jumpConfidence?.isOutlier == true))
            val finalIsJump = (isActualJump && !isMuzzledJump) || providedIsJump
            val finalIsTrajectoryPromoted = sentinelResult.status == SentinelStatus.TRAJECTORY_PROMOTED || providedIsTrajectoryPromoted
            val finalJumpTier = maxOf(sentinelResult.jumpConfidence?.tier ?: 0, providedJumpTier)
            val finalIsAdaptiveJump = (sentinelResult.jumpConfidence?.isAdaptiveJump == true) || providedIsAdaptiveJump
            val finalIsTamper = sentinelResult.status == SentinelStatus.TAMPER || providedIsTamper
            val finalIsJammer = isActualJammer || providedIsJammer
            val finalIsStalled = if (isLocal) (gpsTs != 0L && gpsTs == lastGpsTs) else providedIsStalled
            val isSpatiallyValid = !finalIsJump && !finalIsJammer && finalStatus != SentinelStatus.OUTLIER
            
            val fallbackPoint = EngineGeoPoint(if (lastLat != 0.0) lastLat else lat, if (lastLng != 0.0) lastLng else lng, alt = alt, ts = if (lastTs != 0L) lastTs else effectiveTs, rt = if (lastRt != 0L) lastRt else nowRt, accuracy = lastAcc, maxAccuracy = lastMaxAcc)

            if (!isSpatiallyValid) {
                if (shouldSavePoint(isSuspicious || isAdaptationMuzzled, true, PhysicsUtils.calculateDistance(lastSavedLat, lastSavedLng, lat, lng), 0L, maxAccuracy, nowRt)) {
                    _processorEvents.tryEmit(ProcessorEvent.TrailPointSaved(lat, lng, isViewerTrail, finalStatus, effectiveTs, accuracy = accuracy, maxAccuracy = maxAccuracy))
                }
                return@measureAndAudit ProcessedLocation(rawPoint = EngineGeoPoint(lat, lng, alt = alt, ts = effectiveTs, rt = nowRt, accuracy = accuracy, maxAccuracy = maxAccuracy), optimizedPoint = fallbackPoint, status = finalStatus, maxAccuracy = maxAccuracy, currentAccuracy = accuracy, filteredSpeed = sentinel.getEstimatedSpeedMps(), timestamp = effectiveTs, rt = nowRt, isStalled = finalIsStalled, receiptRt = nowRt, isTrajectoryPromoted = finalIsTrajectoryPromoted, jumpTier = finalJumpTier, isAdaptiveJump = finalIsAdaptiveJump, distToHome = lastNearestHomeDistance, isSpatiallyValid = false, tamperDetected = finalIsTamper, jammerDetected = finalIsJammer, suppressionNote = finalSuppressionNote, kineticEnergy = if (isLocal) sentinel.kineticEnergy else providedKineticEnergy)
            }

            val optimizedPoint = sentinelResult.optimizedPoint ?: EngineGeoPoint(lat, lng, alt = alt, ts = effectiveTs, rt = nowRt, accuracy = accuracy, maxAccuracy = maxAccuracy)
            val persistencePoint = if (isLocal) optimizedPoint else EngineGeoPoint(lat, lng, alt = alt, ts = effectiveTs, rt = nowRt, accuracy = accuracy, maxAccuracy = maxAccuracy)
            
            var geofenceViolation = false
            val home = cachedHomePoints ?: emptyList<EngineGeoPoint>()
            if (home.isNotEmpty() && finalStatus == SentinelStatus.VALID) {
                val validHome = home.filter { PhysicsUtils.isValidLocation(it.lat, it.lng) }
                if (validHome.isNotEmpty()) {
                    val d = validHome.minOf { PhysicsUtils.calculateDistance(optimizedPoint.lat, optimizedPoint.lng, it.lat, it.lng) }
                    lastNearestHomeDistance = d
                    if (!isViewerTrail) {
                        val speedMps = sentinel.getEstimatedSpeedMps()
                        val predictiveMargin = speedMps * GEOFENCE_PREDICTIVE_LOOKAHEAD_S
                        val threshold = maxDistanceAuthority + (maxAccuracy * GEOFENCE_BUFFER_MULT * GEOFENCE_ACCURACY_EXPANSION_MULT)
                        if (speedMps > GEOFENCE_PREDICTIVE_MIN_SPEED_MPS && d > (threshold - predictiveMargin)) geofenceViolation = true
                    }
                }
            }

            lastLat = lat; lastLng = lng; lastTs = effectiveTs; lastRt = nowRt; lastAcc = accuracy; lastMaxAcc = maxAccuracy
            if (!finalIsStalled) lastValidFixRt = nowRt else if (isLocal && !isViewerTrail) _processorEvents.tryEmit(ProcessorEvent.GpsStallDetected(nowRt))
            
            val isThrottled = sentinel.shouldThrottlePolling()
            val estimatedSpeed = sentinel.getEstimatedSpeedMps()
            val stationaryProb = sentinel.getStationaryProbability()
            
            val anchorResult = anchorEvaluator.evaluate(
                point = persistencePoint,
                isPhysicallyStationary = sentinel.isStationary(),
                stationaryProb = stationaryProb,
                estimatedSpeed = estimatedSpeed,
                maxAccuracy = maxAccuracy,
                isSuspicious = isSuspicious || isAdaptationMuzzled,
                isAdaptationMuzzled = isAdaptationMuzzled,
                isAccuracySnap = sentinelResult.jumpConfidence?.reason?.contains("Suppressed Accuracy Snap") == true,
                vibeIndex = sentinel.currentVibrationIndex
            )

            val skipPersistence = anchorResult.shouldSkipPersistence
            val isAnchorLockedNow = anchorResult.isLocked

            val timeSinceLastGpsSaveRt = if (nowRt > 0 && lastSavedRt > 0) nowRt - lastSavedRt else 0L
            if (shouldSavePoint(isSuspicious || isAdaptationMuzzled, isThrottled, PhysicsUtils.calculateDistance(lastSavedLat, lastSavedLng, persistencePoint.lat, persistencePoint.lng), timeSinceLastGpsSaveRt, maxAccuracy, nowRt) && !skipPersistence) {
                _processorEvents.tryEmit(ProcessorEvent.TrailPointSaved(persistencePoint.lat, persistencePoint.lng, isViewerTrail, finalStatus, effectiveTs, accuracy = persistencePoint.accuracy, maxAccuracy = persistencePoint.maxAccuracy))
                lastSavedLat = persistencePoint.lat; lastSavedLng = persistencePoint.lng; lastSavedTs = nowWall; lastSavedRt = nowRt; lastSavedGpsTs = gpsTs
            }
            
            lastProcessedAccuracy = accuracy
            
            val finalOptimized = if (isAnchorLockedNow && !isViewerTrail) {
                anchorResult.optimizedPoint
            } else {
                optimizedPoint
            }

            ProcessedLocation(
                rawPoint = EngineGeoPoint(lat, lng, alt = alt, ts = effectiveTs, rt = nowRt, accuracy = accuracy, maxAccuracy = maxAccuracy), optimizedPoint = finalOptimized, status = finalStatus,
                maxAccuracy = maxAccuracy, currentAccuracy = accuracy, filteredSpeed = estimatedSpeed, timestamp = effectiveTs, rt = nowRt, isStalled = finalIsStalled, 
                isClockRegression = false, receiptRt = nowRt, isTrajectoryPromoted = finalIsTrajectoryPromoted,
                jumpTier = finalJumpTier, isAdaptiveJump = finalIsAdaptiveJump, distToHome = lastNearestHomeDistance, isSpatiallyValid = true, geofenceViolationDetected = geofenceViolation, tamperDetected = finalIsTamper, jammerDetected = finalIsJammer, 
                isAnchorLocked = isAnchorLockedNow, suppressionNote = finalSuppressionNote, kineticEnergy = if (isLocal) sentinel.kineticEnergy else providedKineticEnergy
            )
        }
    }

    private fun isStationary(): Boolean = sentinel.isStationary()

    private fun shouldSavePoint(isSuspicious: Boolean, isThrottled: Boolean, distFromLast: Double, timeSinceLastRt: Long, maxAcc: Double, nowRt: Long): Boolean {
        if (isSuspicious) return true
        val spatialGate = max(ACTIVE_MOVE_THRESHOLD, maxAcc * DEDUPLICATION_SPATIAL_GATE_FACTOR)
        return (distFromLast > (if (isThrottled) PARKING_ANCHOR_MIN_DIST else spatialGate) || (timeSinceLastRt > GPS_SAVE_INTERVAL_MS) || lastSavedRt == 0L)
    }

    private var lastDistanceToTracker: Double? = null
    fun getDistanceToTracker() = lastDistanceToTracker
    fun getNearestHomeDistance() = lastNearestHomeDistance
    
    fun updateCalculatedDistances(lat: Double, lng: Double, isViewerTrail: Boolean, trackerState: SpatialAnchor?) {
        val home = cachedHomePoints ?: emptyList<EngineGeoPoint>()
        if (isViewerTrail) { 
            if (trackerState != null && PhysicsUtils.isValidLocation(trackerState.lat, trackerState.lng)) lastDistanceToTracker = PhysicsUtils.calculateDistance(lat, lng, trackerState.lat, trackerState.lng) 
        } else if (home.isNotEmpty()) { 
            val validHome = home.filter { PhysicsUtils.isValidLocation(it.lat, it.lng) }; if (validHome.isNotEmpty()) lastNearestHomeDistance = validHome.minOf { PhysicsUtils.calculateDistance(lat, lng, it.lat, it.lng) } 
        }
    }
    
    fun getEstimatedBearing(): Double = sentinel.getEstimatedBearing()
    fun resetFilter() { 
        sentinel.reset()
        anchorEvaluator.reset()
    }
    fun invalidateHomePointsCache() { cachedHomePoints = null }
    fun resetStats() {
        lastProcessedAccuracy = 0.0; maxAccuracy = 0.0
        accuracyWindowSize = 0; accuracyWindowHead = 0; lastWindowUpdateRt = 0L
        lastDistanceToTracker = null; lastNearestHomeDistance = null
        lastLat = 0.0; lastLng = 0.0; lastTs = 0L; lastRt = 0L; lastAcc = 0.0; lastMaxAcc = 0.0
        lastSavedLat = 0.0; lastSavedLng = 0.0; lastSavedTs = 0L; lastSavedRt = 0L; lastSavedGpsTs = 0L
        lastHighAccLat = 0.0; lastHighAccLng = 0.0; lastHighAccTs = 0L; lastHighAccRt = 0L; lastValidFixRt = 0L
        anchorEvaluator.reset()
        invalidateHomePointsCache(); sentinel.reset(); 
        _processorEvents.tryEmit(ProcessorEvent.MaxAccuracyChanged(0.0))
    }
}
