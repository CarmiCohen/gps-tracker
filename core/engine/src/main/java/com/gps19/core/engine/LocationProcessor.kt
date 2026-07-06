package com.gps19.core.engine

import java.util.Locale
import kotlin.math.*

/**
 * LocationProcessor: Handles accuracy filtering and coordinate processing.
 * v9.1.8:
 * - Issue #047: Standardized to m/s. Renamed androidSpeedKph to androidSpeedMps 
 *   in processGpsPoint and switched to getEstimatedSpeedMps() internally to 
 *   prevent erroneous HUD multipliers.
 * v8.9.94:
 * - Issue #038: Implemented Adaptation Muzzle. Added isAdaptationMuzzled flag 
 *   to processGpsPoint to increase filter skepticism during A15 polling transitions.
 */
class LocationProcessor(
    private val listener: LocationProcessorListener,
    private val timeProvider: TimeProvider
) {
    val sentinel = LocationSentinel()
    
    private var lastProcessedAccuracy = 0.0
    private var maxAccuracy = 0.0
    private val accuracyWindow = mutableListOf<Double>()
    private var lastWindowUpdateRealtime = 0L
    
    private var lastValidFixTs = 0L 
    private var lastLat = 0.0
    private var lastLng = 0.0
    private var lastTs = 0L
    private var lastAcc = 0.0
    private var lastMaxAcc = 0.0

    private var lastSavedLat = 0.0
    private var lastSavedLng = 0.0
    private var lastSavedTs = 0L
    private var lastSavedGpsTs = 0L

    private var lastHighAccLat = 0.0
    private var lastHighAccLng = 0.0
    private var lastHighAccTs = 0L

    private var cachedHomePoints: List<EngineGeoPoint>? = null
    private var maxDistanceAuthority: Double = 60.0

    private var parkingAnchorPoint: EngineGeoPoint? = null
    private var isAnchorLockedState: Boolean = false

    fun loadState(
        savedMaxAccuracy: Double,
        savedLastSitTs: Long,
        savedChairBaseline: Double,
        spatialAnchor: SpatialAnchor?,
        homePoints: List<EngineGeoPoint>,
        maxDistance: Double
    ) {
        if (savedMaxAccuracy > 0.0) {
            maxAccuracy = savedMaxAccuracy
            accuracyWindow.add(savedMaxAccuracy)
        }
        
        if (savedLastSitTs > 0) {
            sentinel.lastSitTs = savedLastSitTs
        }
        if (savedChairBaseline > -100.0) sentinel.baselineSitTilt = savedChairBaseline
        
        if (spatialAnchor != null && spatialAnchor.lat != 0.0) {
            lastLat = spatialAnchor.lat
            lastLng = spatialAnchor.lng
            lastTs = spatialAnchor.gpsTs
            sentinel.setSpatialAnchor(spatialAnchor.lat, spatialAnchor.lng, spatialAnchor.alt, spatialAnchor.gpsTs)
        }

        this.cachedHomePoints = homePoints
        this.maxDistanceAuthority = maxDistance
    }

    fun setMaxDistanceAuthority(distance: Double) {
        this.maxDistanceAuthority = distance
    }

    fun setHomePoints(points: List<EngineGeoPoint>) {
        this.cachedHomePoints = points
    }

    fun getLastProcessedAccuracy() = lastProcessedAccuracy
    fun getMaxTrackerAccuracy() = maxAccuracy
    fun getLastValidFixTs() = lastValidFixTs
    fun setLastValidFixTs(ts: Long) { lastValidFixTs = ts }
    fun getMaxDistanceAuthority() = maxDistanceAuthority

    fun getLuxBaseline() = sentinel.luxBaseline
    fun getAcousticFloorDb() = sentinel.acousticFloorDb
    fun getAdaptiveVibrationFloor() = sentinel.adaptiveVibrationFloor
    fun getPeakVibrationShock() = sentinel.peakVibrationShock
    fun getPeakVibrationShockTs() = sentinel.peakVibrationShockTs
    
    fun getChairBaselineTilt() = sentinel.baselineSitTilt
    fun getLastSitTs() = sentinel.lastSitTs
    fun getLastSitRealtime() = sentinel.lastSitRealtime

    fun consumeSitDetected(): Boolean = sentinel.consumeSitDetected()

    fun updateSensorData(
        vibration: Double, heading: Double, baroAlt: Double, 
        lux: Double = 0.0, isNear: Boolean = true, powerTamper: Boolean = false,
        tiltDegrees: Double = 0.0, acousticDb: Double = 0.0, peakShock: Double = 0.0,
        acousticMinDb: Double = -1.0, peakVerticalVelocity: Double = 0.0,
        plungeMatched: Boolean = false, peakVerticalVelocityTs: Long = 0L, 
        peakVerticalDisplacement: Double = 0.0,
        isSirenActive: Boolean = false, isWarming: Boolean = false,
        manualAdaptiveFloor: Double = -1.0, acousticLockoutTs: Long = 0L,
        isMuzzled: Boolean = false,
        isA15: Boolean = false,
        nowRealtime: Long = timeProvider.elapsedRealtime(),
        nowWall: Long = timeProvider.currentTimeMillis()
    ): Boolean { 
        val baselineChanged = sentinel.updateSensorState(
            vibration, heading, baroAlt, lux, isNear, powerTamper, tiltDegrees, 
            acousticDb, peakShock, acousticMinDb, peakVerticalVelocity, peakVerticalVelocityTs, plungeMatched, peakVerticalDisplacement,
            isSirenActive, isWarming, manualAdaptiveFloor, acousticLockoutTs, isMuzzled, isA15, nowRealtime, nowWall
        )
        if (baselineChanged) {
            listener.onChairBaselineChanged(sentinel.baselineSitTilt)
        }
        return baselineChanged
    }

    fun resetChairBaseline() {
        sentinel.resetChairBaseline()
        listener.onChairBaselineChanged(sentinel.baselineSitTilt)
    }

    fun shouldThrottlePolling(providedIsStationary: Boolean? = null): Boolean = sentinel.shouldThrottlePolling(providedIsStationary)

    fun updateWindowedAccuracy(acc: Double) {
        if (acc <= 0.0) return
        val nowRealtime = timeProvider.elapsedRealtime()
        val bucketDuration = ACCURACY_WINDOW_BUCKET_MS / ACCURACY_WINDOW_MAX_SIZE
        
        if (accuracyWindow.isEmpty() || (lastWindowUpdateRealtime > 0 && nowRealtime - lastWindowUpdateRealtime >= bucketDuration)) {
            accuracyWindow.add(acc)
            lastWindowUpdateRealtime = nowRealtime
            while (accuracyWindow.size > ACCURACY_WINDOW_MAX_SIZE) accuracyWindow.removeAt(0)
        } else {
            if (lastWindowUpdateRealtime == 0L) lastWindowUpdateRealtime = nowRealtime
            val currentMax = if (accuracyWindow.isNotEmpty()) accuracyWindow.last() else 0.0
            if (acc > currentMax * GEOFENCE_ACCURACY_HYSTERESIS_MULT) accuracyWindow[accuracyWindow.lastIndex] = acc
        }
        val rawMax = accuracyWindow.maxOrNull() ?: acc
        val newMax = rawMax.roundToInt().toDouble()
        if (newMax != maxAccuracy) {
            maxAccuracy = newMax
            listener.onMaxAccuracyChanged(maxAccuracy)
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
        val isStalled: Boolean = false,
        val isClockRegression: Boolean = false,
        val receiptRealtime: Long = 0L,
        val isTrajectoryPromoted: Boolean = false,
        val jumpTier: Int = 0,
        val isAdaptiveJump: Boolean = false,
        val distToHome: Double? = null,
        val isSpatiallyValid: Boolean = true,
        val geofenceViolationDetected: Boolean = false,
        val tamperDetected: Boolean = false,
        val jammerDetected: Boolean = false,
        val isAnchorLocked: Boolean = false,
        val suppressionNote: String? = null
    )

    fun processGpsPoint(
        lat: Double, lng: Double, alt: Double, androidSpeedMps: Double, 
        gpsTs: Long, accuracy: Double, bearing: Double, snr: Double, satsUsed: Int, 
        isViewerTrail: Boolean, lastGpsTs: Long, isLocal: Boolean = false,
        providedMaxAccuracy: Double = 0.0, providedIsJump: Boolean = false,
        providedIsTrajectoryPromoted: Boolean = false, providedJumpTier: Int = 0,
        providedIsAdaptiveJump: Boolean = false,
        providedIsJammer: Boolean = false, providedIsStalled: Boolean = false,
        providedIsTamper: Boolean = false, providedAdaptiveVibrationFloor: Double = -1.0,
        providedAcousticLockoutTs: Long = 0L,
        isSuspicious: Boolean = false, 
        isMuzzled: Boolean = false,
        isA15: Boolean = false,
        isAdaptationMuzzled: Boolean = false, // Issue #038
        nowWall: Long = timeProvider.currentTimeMillis(),
        nowRealtime: Long = timeProvider.elapsedRealtime()
    ): ProcessedLocation {
        val effectiveTs = if (gpsTs > 0) gpsTs else nowWall

        if (lastTs > 0 && effectiveTs < lastTs) {
            val delta = lastTs - effectiveTs
            if (delta > CLOCK_REGRESSION_GATE_MS) { 
                listener.onLogAdded("Merge-on-Stale: Coordinate update bypassed due to hardware clock regression (${delta}ms). Merging status-only data.", "system", false, true, 0.0, 0.0, 0.0, snr, sentinel.currentVibrationIndex)
                if (delta > 86400000L) { lastTs = 0L; sentinel.reset() }
            }
            val status = when {
                providedIsTamper -> SentinelStatus.TAMPER_ALERT
                providedIsJammer -> SentinelStatus.JAMMER_SUSPICION
                providedIsJump -> SentinelStatus.JUMP
                else -> SentinelStatus.VALID
            }
            val fallbackCoordPoint = EngineGeoPoint(if (lastLat != 0.0) lastLat else lat, if (lastLng != 0.0) lastLng else lng, ts = if (lastTs != 0L) lastTs else effectiveTs, accuracy = accuracy, maxAccuracy = maxAccuracy)
            return ProcessedLocation(rawPoint = fallbackCoordPoint, optimizedPoint = fallbackCoordPoint, status = status, maxAccuracy = maxAccuracy, currentAccuracy = accuracy, filteredSpeed = sentinel.getEstimatedSpeedMps(), timestamp = effectiveTs, isStalled = providedIsStalled, isClockRegression = true, receiptRealtime = nowRealtime, isTrajectoryPromoted = providedIsTrajectoryPromoted, jumpTier = providedJumpTier, isAdaptiveJump = providedIsAdaptiveJump, distToHome = lastNearestHomeDistance, isSpatiallyValid = true, tamperDetected = providedIsTamper, jammerDetected = providedIsJammer)
        }

        if (accuracy > HIGH_ACCURACY_THRESHOLD_METERS * TRAJECTORY_REJECTION_ACCURACY_MULT && lastHighAccTs > 0 && nowWall - lastHighAccTs < TRAJECTORY_PROMOTION_WINDOW_MS) {
            if (PhysicsUtils.calculateDistance(lat, lng, lastHighAccLat, lastHighAccLng) > accuracy) {
                val fallbackCoordPoint = EngineGeoPoint(if (lastLat != 0.0) lastLat else lat, if (lastLng != 0.0) lastLng else lng, ts = if (lastTs != 0L) lastTs else effectiveTs, accuracy = accuracy, maxAccuracy = maxAccuracy)
                return ProcessedLocation(rawPoint = EngineGeoPoint(lat, lng, ts = effectiveTs, accuracy = accuracy, maxAccuracy = maxAccuracy), optimizedPoint = fallbackCoordPoint, status = SentinelStatus.VALID, maxAccuracy = maxAccuracy, currentAccuracy = accuracy, filteredSpeed = sentinel.getEstimatedSpeedMps(), timestamp = effectiveTs, isStalled = if (isLocal) false else providedIsStalled, receiptRealtime = nowRealtime, jumpTier = providedJumpTier, isAdaptiveJump = providedIsAdaptiveJump, distToHome = lastNearestHomeDistance, isSpatiallyValid = false, tamperDetected = providedIsTamper, jammerDetected = providedIsJammer)
            }
        }
        
        if (accuracy <= HIGH_ACCURACY_THRESHOLD_METERS) { lastHighAccLat = lat; lastHighAccLng = lng; lastHighAccTs = nowWall }
        if (isLocal) updateWindowedAccuracy(accuracy) else if (providedMaxAccuracy > 0.0) maxAccuracy = providedMaxAccuracy
        if (providedAdaptiveVibrationFloor >= 0.0) sentinel.adaptiveVibrationFloor = providedAdaptiveVibrationFloor
        if (providedAcousticLockoutTs > 0) sentinel.updateSensorState(vibration = -1.0, heading = -1.0, baroAlt = -1000.0, acousticLockoutTs = providedAcousticLockoutTs, isMuzzled = isMuzzled, isA15 = isA15, nowRealtime = nowRealtime, nowWall = nowWall)

        val sentinelResult = sentinel.processLocation(
            lat = lat, lng = lng, alt = alt, accuracy = accuracy, maxAccuracy = maxAccuracy, 
            bearing = bearing, snr = snr, satsUsed = satsUsed, timestamp = effectiveTs, 
            bypassBehavioral = !isLocal, isSuspicious = isSuspicious || isAdaptationMuzzled, // Higher skepticism during adaptation
            isMuzzled = isMuzzled, isA15 = isA15, nowWall = nowWall, nowRealtime = nowRealtime
        )
        
        if (sentinelResult.status == SentinelStatus.TRAJECTORY_PROMOTED) {
            val promotedPoints = sentinelResult.promotedPoints ?: emptyList()
            if (promotedPoints.isNotEmpty() && lastLat != 0.0) {
                val firstPromoted = promotedPoints.first()
                val interpolated = PhysicsUtils.interpolateSegment(
                    lastLat, lastLng, lastTs, firstPromoted.lat, firstPromoted.lng, firstPromoted.ts,
                    startAcc = lastAcc, startMaxAcc = lastMaxAcc, endAcc = accuracy, endMaxAcc = maxAccuracy
                )
                interpolated.forEach { p ->
                    listener.onTrailPointSaved(p.lat, p.lng, isViewerTrail, false, p.ts, isHindsightCorrected = true, accuracy = p.accuracy, maxAccuracy = p.maxAccuracy)
                }
            }
            promotedPoints.forEach { p ->
                listener.onTrailPointSaved(p.lat, p.lng, isViewerTrail, false, p.ts, isHindsightCorrected = true, accuracy = p.accuracy, maxAccuracy = p.maxAccuracy)
            }
        }

        val isActualJump = (sentinelResult.status == SentinelStatus.JUMP || sentinelResult.status == SentinelStatus.OUTLIER || sentinelResult.status == SentinelStatus.JITTER || (sentinelResult.jumpConfidence?.isJump == true))
        val isActualJammer = (sentinelResult.status == SentinelStatus.JAMMER_SUSPICION || (sentinelResult.jumpConfidence?.isOutlier == true))
        val finalIsJump = isActualJump || providedIsJump
        val finalIsTrajectoryPromoted = sentinelResult.status == SentinelStatus.TRAJECTORY_PROMOTED || providedIsTrajectoryPromoted
        val finalJumpTier = maxOf(sentinelResult.jumpConfidence?.tier ?: 0, providedJumpTier)
        val finalIsAdaptiveJump = (sentinelResult.jumpConfidence?.isAdaptiveJump == true) || providedIsAdaptiveJump
        val finalIsTamper = sentinelResult.status == SentinelStatus.TAMPER_ALERT || providedIsTamper
        val finalIsJammer = isActualJammer || providedIsJammer
        val finalIsStalled = if (isLocal) (gpsTs != 0L && gpsTs == lastGpsTs) else providedIsStalled
        val isSpatiallyValid = !finalIsJump && !finalIsJammer && sentinelResult.status != SentinelStatus.OUTLIER
        
        val fallbackPoint = EngineGeoPoint(if (lastLat != 0.0) lastLat else lat, if (lastLng != 0.0) lastLng else lng, ts = if (lastTs != 0L) lastTs else effectiveTs, accuracy = lastAcc, maxAccuracy = lastMaxAcc)

        if (!isSpatiallyValid) {
            if (shouldSavePoint(isSuspicious || isAdaptationMuzzled, true, PhysicsUtils.calculateDistance(lastSavedLat, lastSavedLng, lat, lng), 0L, maxAccuracy)) listener.onTrailPointSaved(lat, lng, isViewerTrail, true, effectiveTs, accuracy = accuracy, maxAccuracy = maxAccuracy)
            return ProcessedLocation(rawPoint = EngineGeoPoint(lat, lng, ts = effectiveTs, accuracy = accuracy, maxAccuracy = maxAccuracy), optimizedPoint = fallbackPoint, status = sentinelResult.status, maxAccuracy = maxAccuracy, currentAccuracy = accuracy, filteredSpeed = sentinel.getEstimatedSpeedMps(), timestamp = effectiveTs, isStalled = finalIsStalled, receiptRealtime = nowRealtime, jumpTier = finalJumpTier, isAdaptiveJump = finalIsAdaptiveJump, distToHome = lastNearestHomeDistance, isSpatiallyValid = false, tamperDetected = finalIsTamper, jammerDetected = finalIsJammer, suppressionNote = if (isAdaptationMuzzled && sentinelResult.status == SentinelStatus.VALID) "Filtering settled during A15 frequency adaptation." else sentinelResult.suppressionNote)
        }

        val optimized = sentinelResult.optimizedPoint ?: EngineGeoPoint(lat, lng, ts = effectiveTs, accuracy = accuracy, maxAccuracy = maxAccuracy)
        val persistencePoint = if (isLocal) optimized else EngineGeoPoint(lat, lng, ts = effectiveTs, accuracy = accuracy, maxAccuracy = maxAccuracy)
        var geofenceViolation = false
        val home = cachedHomePoints ?: emptyList()
        if (home.isNotEmpty()) {
            val validHome = home.filter { PhysicsUtils.isValidLocation(it.lat, it.lng) }
            if (validHome.isNotEmpty()) {
                val d = validHome.minOf { PhysicsUtils.calculateDistance(persistencePoint.lat, persistencePoint.lng, it.lat, it.lng) }
                lastNearestHomeDistance = d
                if (!isViewerTrail) {
                    val speedMps = sentinel.getEstimatedSpeedMps()
                    val predictiveMargin = speedMps * GEOFENCE_PREDICTIVE_LOOKAHEAD_S
                    val threshold = maxDistanceAuthority + (maxAccuracy * GEOFENCE_BUFFER_MULT * GEOFENCE_ACCURACY_EXPANSION_MULT)
                    if (speedMps > GEOFENCE_PREDICTIVE_MIN_SPEED_MPS && d > (threshold - predictiveMargin)) geofenceViolation = true
                }
            }
        }

        lastLat = lat; lastLng = lng; lastTs = effectiveTs; lastAcc = accuracy; lastMaxAcc = maxAccuracy
        if (!finalIsStalled) lastValidFixTs = nowRealtime else if (isLocal && !isViewerTrail) listener.onGpsStallDetected(nowRealtime)
        
        val isThrottled = sentinel.shouldThrottlePolling()
        val stationaryProb = sentinel.getStationaryProbability()
        var skipPersistence = false
        var isAnchorLockedNow = false
        
        // Issue #018: Hard-Lock logic - Behavioral Breakout Refinement
        val isPhysicallyStationary = sentinel.isStationary()
        if (!isSuspicious && !isAdaptationMuzzled && stationaryProb > 0.9) {
            if (parkingAnchorPoint == null && isPhysicallyStationary) {
                parkingAnchorPoint = persistencePoint
                listener.onLogAdded("Stationary Anchor engaged at ${String.format(Locale.getDefault(), "%.5f, %.5f", persistencePoint.lat, persistencePoint.lng)} (Prob: ${String.format(Locale.getDefault(), "%.2f", stationaryProb)})", "system", false, false, persistencePoint.lat, persistencePoint.lng, accuracy, snr, sentinel.currentVibrationIndex)
            }
            
            if (parkingAnchorPoint != null) {
                var breakoutThreshold = max(PARKING_ANCHOR_MIN_DIST, maxAccuracy * PARKING_ANCHOR_FACTOR)
                if (!isPhysicallyStationary) {
                    // Issue #018: Reduce breakout threshold when physical motion is detected 
                    // to prevent "sticky" anchor transitions when leaving a building.
                    breakoutThreshold = 5.0 
                }
                
                val distFromAnchor = PhysicsUtils.calculateDistance(parkingAnchorPoint!!.lat, parkingAnchorPoint!!.lng, persistencePoint.lat, persistencePoint.lng)
                
                if (distFromAnchor < breakoutThreshold) {
                    skipPersistence = true 
                    isAnchorLockedNow = true
                } else {
                    // Breakout detected - reset anchor
                    listener.onLogAdded("Stationary Anchor breakout: Distance ${String.format(Locale.getDefault(), "%.1f", distFromAnchor)}m exceeds threshold ${String.format(Locale.getDefault(), "%.1f", breakoutThreshold)}m", "system", false, false, persistencePoint.lat, persistencePoint.lng, accuracy, snr, sentinel.currentVibrationIndex)
                    parkingAnchorPoint = null
                }
            }
        } else {
            if (parkingAnchorPoint != null) {
                listener.onLogAdded("Stationary Anchor released (Prob: ${String.format(Locale.getDefault(), "%.2f", stationaryProb)})", "system", false, false, persistencePoint.lat, persistencePoint.lng, accuracy, snr, sentinel.currentVibrationIndex)
                parkingAnchorPoint = null
            }
        }
        isAnchorLockedState = isAnchorLockedNow

        val timeSinceLastGpsSave = if (gpsTs > 0 && lastSavedGpsTs > 0) gpsTs - lastSavedGpsTs else 0L
        if (shouldSavePoint(isSuspicious || isAdaptationMuzzled, isThrottled, PhysicsUtils.calculateDistance(lastSavedLat, lastSavedLng, persistencePoint.lat, persistencePoint.lng), timeSinceLastGpsSave, maxAccuracy) && !skipPersistence) {
            listener.onTrailPointSaved(persistencePoint.lat, persistencePoint.lng, isViewerTrail, false, effectiveTs, isHindsightCorrected = finalIsTrajectoryPromoted, accuracy = persistencePoint.accuracy, maxAccuracy = persistencePoint.maxAccuracy)
            lastSavedLat = persistencePoint.lat; lastSavedLng = persistencePoint.lng; lastSavedTs = nowWall; lastSavedGpsTs = gpsTs
        }
        
        lastProcessedAccuracy = accuracy
        var finalOptimized = optimized
        var finalFilteredSpeed = sentinel.getEstimatedSpeedMps()
        
        // Strictly clamp output to anchor if locked
        if (skipPersistence && parkingAnchorPoint != null) { 
            finalOptimized = parkingAnchorPoint!! 
            finalFilteredSpeed = 0.0 
        }

        return ProcessedLocation(
            rawPoint = EngineGeoPoint(lat, lng, ts = effectiveTs, accuracy = accuracy, maxAccuracy = maxAccuracy), optimizedPoint = finalOptimized, status = sentinelResult.status,
            maxAccuracy = maxAccuracy, currentAccuracy = accuracy, filteredSpeed = finalFilteredSpeed, timestamp = effectiveTs, isStalled = finalIsStalled, 
            isClockRegression = false, receiptRealtime = nowRealtime, isTrajectoryPromoted = finalIsTrajectoryPromoted,
            jumpTier = finalJumpTier, isAdaptiveJump = finalIsAdaptiveJump, distToHome = lastNearestHomeDistance, isSpatiallyValid = true, geofenceViolationDetected = geofenceViolation, tamperDetected = finalIsTamper, jammerDetected = finalIsJammer, 
            isAnchorLocked = isAnchorLockedNow, suppressionNote = if (isAdaptationMuzzled) "Settling A15 Polling..." else sentinelResult.suppressionNote
        )
    }

    private fun shouldSavePoint(isSuspicious: Boolean, isThrottled: Boolean, distFromLast: Double, timeSinceLast: Long, maxAcc: Double): Boolean {
        if (isSuspicious) return true
        val spatialGate = max(ACTIVE_MOVE_THRESHOLD, maxAcc * DEDUPLICATION_SPATIAL_GATE_FACTOR)
        return (distFromLast > (if (isThrottled) PARKING_ANCHOR_MIN_DIST else spatialGate) || (timeSinceLast > GPS_SAVE_INTERVAL_MS) || lastSavedTs == 0L)
    }

    private var lastDistanceToTracker: Double? = null
    fun getDistanceToTracker() = lastDistanceToTracker
    fun getNearestHomeDistance() = lastNearestHomeDistance
    
    fun updateCalculatedDistances(lat: Double, lng: Double, isViewerTrail: Boolean, trackerState: SpatialAnchor?) {
        val home = cachedHomePoints ?: emptyList()
        if (isViewerTrail) { 
            if (trackerState != null && PhysicsUtils.isValidLocation(trackerState.lat, trackerState.lng)) lastDistanceToTracker = PhysicsUtils.calculateDistance(lat, lng, trackerState.lat, trackerState.lng) 
        } else if (home.isNotEmpty()) { 
            val validHome = home.filter { PhysicsUtils.isValidLocation(it.lat, it.lng) }; if (validHome.isNotEmpty()) lastNearestHomeDistance = validHome.minOf { PhysicsUtils.calculateDistance(lat, lng, it.lat, it.lng) } 
        }
    }
    
    fun getEstimatedBearing(): Double = sentinel.getEstimatedBearing()
    fun resetFilter() { sentinel.reset() }
    fun invalidateHomePointsCache() { cachedHomePoints = null }
    fun resetStats() {
        lastProcessedAccuracy = 0.0; maxAccuracy = 0.0; accuracyWindow.clear(); lastWindowUpdateRealtime = 0L
        lastDistanceToTracker = null; lastNearestHomeDistance = null
        lastLat = 0.0; lastLng = 0.0; lastTs = 0L; lastAcc = 0.0; lastMaxAcc = 0.0
        lastSavedLat = 0.0; lastSavedLng = 0.0; lastSavedTs = 0L; lastSavedGpsTs = 0L
        lastHighAccLat = 0.0; lastHighAccLng = 0.0; lastHighAccTs = 0L; lastValidFixTs = 0L; parkingAnchorPoint = null
        isAnchorLockedState = false
        invalidateHomePointsCache(); sentinel.reset(); 
        listener.onMaxAccuracyChanged(0.0)
    }
}

interface LocationProcessorListener {
    fun onTrailPointSaved(lat: Double, lng: Double, isViewerTrail: Boolean, isJump: Boolean, timestamp: Long, isHindsightCorrected: Boolean = false, accuracy: Double = 0.0, maxAccuracy: Double = 0.0)
    fun onLogAdded(message: String, type: String, isImportant: Boolean, isSpecial: Boolean, lat: Double, lng: Double, accuracy: Double, snr: Double? = null, vibe: Double? = null)
    fun onMaxAccuracyChanged(accuracy: Double)
    fun onChairBaselineChanged(baseline: Double)
    fun onGpsStallDetected(ts: Long)
}
