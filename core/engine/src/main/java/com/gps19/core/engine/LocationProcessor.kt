package com.gps19.core.engine

import kotlin.math.*

/**
 * LocationProcessor: Handles accuracy filtering and coordinate processing.
 * v8.9.38:
 * - Issue #334: Implemented "Rubber-Band" hindsight interpolation.
 * - Issue #327: Refined transition smoothing for promoted trajectories.
 * v8.9.34:
 * - Issue #303: Unified Trajectory Rejection multiplier.
 * - Issue #268: Removed redundant providedAcousticFloorDb from processGpsPoint.
 */
class LocationProcessor(
    private val listener: LocationProcessorListener,
    private val timeProvider: TimeProvider
) {
    val sentinel = LocationSentinel()
    
    private var lastProcessedAccuracy = 0f
    private var maxAccuracy = 0f
    private val accuracyWindow = mutableListOf<Float>()
    private var lastWindowUpdateRealtime = 0L
    
    private var lastValidFixTs = 0L 
    private var lastLat = 0.0
    private var lastLng = 0.0
    private var lastTs = 0L

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

    fun loadState(
        savedMaxAccuracy: Float,
        savedLastSitTs: Long,
        savedChairBaseline: Float,
        spatialAnchor: SpatialAnchor?,
        homePoints: List<EngineGeoPoint>,
        maxDistance: Double
    ) {
        if (savedMaxAccuracy > 0) {
            maxAccuracy = savedMaxAccuracy
            accuracyWindow.add(savedMaxAccuracy)
        }
        
        if (savedLastSitTs > 0) {
            sentinel.lastSitTs = savedLastSitTs
        }
        if (savedChairBaseline > -100f) sentinel.baselineSitTilt = savedChairBaseline
        
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
        vibration: Float, heading: Float, baroAlt: Float, 
        lux: Float = 0f, isNear: Boolean = true, powerTamper: Boolean = false,
        tiltDegrees: Float = 0f, acousticDb: Double = 0.0, peakShock: Float = 0f,
        acousticMinDb: Double = -1.0, peakVerticalVelocity: Float = 0f,
        plungeMatched: Boolean = false, peakVerticalVelocityTs: Long = 0L, 
        peakVerticalDisplacement: Float = 0f,
        isSirenActive: Boolean = false, isWarming: Boolean = false,
        manualAdaptiveFloor: Float = -1f, acousticLockoutTs: Long = 0L,
        isMuzzled: Boolean = false,
        nowRealtime: Long = timeProvider.elapsedRealtime(),
        nowWall: Long = timeProvider.currentTimeMillis()
    ): Boolean { 
        val baselineChanged = sentinel.updateSensorState(
            vibration, heading, baroAlt, lux, isNear, powerTamper, tiltDegrees, 
            acousticDb, peakShock, acousticMinDb, peakVerticalVelocity, peakVerticalVelocityTs, plungeMatched, peakVerticalDisplacement,
            isSirenActive, isWarming, manualAdaptiveFloor, acousticLockoutTs, isMuzzled, nowRealtime, nowWall
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

    fun updateWindowedAccuracy(acc: Float) {
        if (acc <= 0f) return
        val nowRealtime = timeProvider.elapsedRealtime()
        val bucketDuration = ACCURACY_WINDOW_BUCKET_MS / ACCURACY_WINDOW_MAX_SIZE
        
        if (accuracyWindow.isEmpty() || (lastWindowUpdateRealtime > 0 && nowRealtime - lastWindowUpdateRealtime >= bucketDuration)) {
            accuracyWindow.add(acc)
            lastWindowUpdateRealtime = nowRealtime
            while (accuracyWindow.size > ACCURACY_WINDOW_MAX_SIZE) accuracyWindow.removeAt(0)
        } else {
            if (lastWindowUpdateRealtime == 0L) lastWindowUpdateRealtime = nowRealtime
            val currentMax = if (accuracyWindow.isNotEmpty()) accuracyWindow.last() else 0f
            if (acc > currentMax * GEOFENCE_ACCURACY_HYSTERESIS_MULT) accuracyWindow[accuracyWindow.lastIndex] = acc
        }
        val rawMax = accuracyWindow.maxOrNull() ?: acc
        val newMax = rawMax.roundToInt().toFloat()
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
        val maxAccuracy: Float,
        val currentAccuracy: Float,
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
        val jammerDetected: Boolean = false
    )

    fun processGpsPoint(
        lat: Double, lng: Double, alt: Double, androidSpeedKph: Double, 
        gpsTs: Long, accuracy: Float, bearing: Float, snr: Float, satsUsed: Int, 
        isViewerTrail: Boolean, lastGpsTs: Long, isLocal: Boolean = false,
        providedMaxAccuracy: Float = 0f, providedIsJump: Boolean = false,
        providedIsTrajectoryPromoted: Boolean = false, providedJumpTier: Int = 0,
        providedIsAdaptiveJump: Boolean = false,
        providedIsJammer: Boolean = false, providedIsStalled: Boolean = false,
        providedIsTamper: Boolean = false, providedAdaptiveVibrationFloor: Float = -1f,
        providedAcousticLockoutTs: Long = 0L,
        isSuspicious: Boolean = false, 
        isMuzzled: Boolean = false,
        nowWall: Long = timeProvider.currentTimeMillis(),
        nowRealtime: Long = timeProvider.elapsedRealtime()
    ): ProcessedLocation {
        val effectiveTs = if (gpsTs > 0) gpsTs else nowWall

        if (lastTs > 0 && effectiveTs < lastTs) {
            val delta = lastTs - effectiveTs
            if (delta > CLOCK_REGRESSION_GATE_MS) { 
                listener.onLogAdded("Merge-on-Stale: Coordinate update bypassed due to hardware clock regression (${delta}ms). Merging status-only data.", "system", false, true, 0.0, 0.0, 0f, snr, sentinel.currentVibrationIndex)
                if (delta > 86400000L) { lastTs = 0L; sentinel.reset() }
            }
            val status = when {
                providedIsTamper -> SentinelStatus.TAMPER_ALERT
                providedIsJammer -> SentinelStatus.JAMMER_SUSPICION
                providedIsJump -> SentinelStatus.JUMP
                else -> SentinelStatus.VALID
            }
            val fallbackCoordPoint = EngineGeoPoint(if (lastLat != 0.0) lastLat else lat, if (lastLng != 0.0) lastLng else lng, ts = if (lastTs != 0L) lastTs else effectiveTs)
            return ProcessedLocation(rawPoint = fallbackCoordPoint, optimizedPoint = fallbackCoordPoint, status = status, maxAccuracy = maxAccuracy, currentAccuracy = accuracy, filteredSpeed = sentinel.getEstimatedSpeedKph(), timestamp = effectiveTs, isStalled = providedIsStalled, isClockRegression = true, receiptRealtime = nowRealtime, isTrajectoryPromoted = providedIsTrajectoryPromoted, jumpTier = providedJumpTier, isAdaptiveJump = providedIsAdaptiveJump, distToHome = lastNearestHomeDistance, isSpatiallyValid = true, tamperDetected = providedIsTamper, jammerDetected = providedIsJammer)
        }

        if (accuracy > HIGH_ACCURACY_THRESHOLD_METERS * TRAJECTORY_REJECTION_ACCURACY_MULT && lastHighAccTs > 0 && nowWall - lastHighAccTs < TRAJECTORY_PROMOTION_WINDOW_MS) {
            if (PhysicsUtils.calculateDistance(lat, lng, lastHighAccLat, lastHighAccLng) > accuracy) {
                val fallbackCoordPoint = EngineGeoPoint(if (lastLat != 0.0) lastLat else lat, if (lastLng != 0.0) lastLng else lng, ts = if (lastTs != 0L) lastTs else effectiveTs)
                return ProcessedLocation(rawPoint = EngineGeoPoint(lat, lng, ts = effectiveTs), optimizedPoint = fallbackCoordPoint, status = SentinelStatus.VALID, maxAccuracy = maxAccuracy, currentAccuracy = accuracy, filteredSpeed = sentinel.getEstimatedSpeedKph(), timestamp = effectiveTs, isStalled = if (isLocal) false else providedIsStalled, receiptRealtime = nowRealtime, jumpTier = providedJumpTier, isAdaptiveJump = providedIsAdaptiveJump, distToHome = lastNearestHomeDistance, isSpatiallyValid = false, tamperDetected = providedIsTamper, jammerDetected = providedIsJammer)
            }
        }
        
        if (accuracy <= HIGH_ACCURACY_THRESHOLD_METERS) { lastHighAccLat = lat; lastHighAccLng = lng; lastHighAccTs = nowWall }
        if (isLocal) updateWindowedAccuracy(accuracy) else if (providedMaxAccuracy > 0f) maxAccuracy = providedMaxAccuracy
        if (providedAdaptiveVibrationFloor >= 0f) sentinel.adaptiveVibrationFloor = providedAdaptiveVibrationFloor
        if (providedAcousticLockoutTs > 0) sentinel.updateSensorState(vibration = -1f, heading = -1f, baroAlt = -1000f, acousticLockoutTs = providedAcousticLockoutTs, isMuzzled = isMuzzled, nowRealtime = nowRealtime, nowWall = nowWall)

        val sentinelResult = sentinel.processLocation(lat = lat, lng = lng, alt = alt, accuracy = accuracy, bearing = bearing, snr = snr, satsUsed = satsUsed, timestamp = effectiveTs, bypassBehavioral = !isLocal, isSuspicious = isSuspicious, isMuzzled = isMuzzled, nowWall = nowWall, nowRealtime = nowRealtime)
        
        if (sentinelResult.status == SentinelStatus.TRAJECTORY_PROMOTED) {
            val promotedPoints = sentinelResult.promotedPoints ?: emptyList()
            if (promotedPoints.isNotEmpty() && lastLat != 0.0) {
                // Issue #334: Rubber-band interpolation for the gap between last valid and first promoted point.
                val firstPromoted = promotedPoints.first()
                val interpolated = PhysicsUtils.interpolateSegment(lastLat, lastLng, lastTs, firstPromoted.lat, firstPromoted.lng, firstPromoted.ts)
                interpolated.forEach { p ->
                    listener.onTrailPointSaved(p.lat, p.lng, isViewerTrail, false, p.ts, isHindsightCorrected = true)
                }
            }
            promotedPoints.forEach { p ->
                listener.onTrailPointSaved(p.lat, p.lng, isViewerTrail, false, p.ts, isHindsightCorrected = true)
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
        
        val fallbackPoint = EngineGeoPoint(if (lastLat != 0.0) lastLat else lat, if (lastLng != 0.0) lastLng else lng, ts = if (lastTs != 0L) lastTs else effectiveTs)

        if (!isSpatiallyValid) {
            if (shouldSavePoint(isSuspicious, true, 1000.0, 0L)) listener.onTrailPointSaved(lat, lng, isViewerTrail, true, effectiveTs)
            return ProcessedLocation(rawPoint = EngineGeoPoint(lat, lng, ts = effectiveTs), optimizedPoint = fallbackPoint, status = sentinelResult.status, maxAccuracy = maxAccuracy, currentAccuracy = accuracy, filteredSpeed = sentinel.getEstimatedSpeedKph(), timestamp = effectiveTs, isStalled = finalIsStalled, receiptRealtime = nowRealtime, jumpTier = finalJumpTier, isAdaptiveJump = finalIsAdaptiveJump, distToHome = lastNearestHomeDistance, isSpatiallyValid = false, tamperDetected = finalIsTamper, jammerDetected = finalIsJammer)
        }

        val optimized = sentinelResult.optimizedPoint ?: EngineGeoPoint(lat, lng, ts = effectiveTs)
        val persistencePoint = if (isLocal) optimized else EngineGeoPoint(lat, lng, ts = effectiveTs)
        var geofenceViolation = false
        val home = cachedHomePoints ?: emptyList()
        if (home.isNotEmpty()) {
            val validHome = home.filter { PhysicsUtils.isValidLocation(it.lat, it.lng) }
            if (validHome.isNotEmpty()) {
                val d = validHome.minOf { PhysicsUtils.calculateDistance(persistencePoint.lat, persistencePoint.lng, it.lat, it.lng) }
                lastNearestHomeDistance = d
                if (!isViewerTrail) {
                    val speedMps = sentinel.getEstimatedSpeedKph() / 3.6
                    val predictiveMargin = speedMps * GEOFENCE_PREDICTIVE_LOOKAHEAD_S
                    val threshold = maxDistanceAuthority + (maxAccuracy * GEOFENCE_BUFFER_MULT * GEOFENCE_ACCURACY_EXPANSION_MULT)
                    if (speedMps > GEOFENCE_PREDICTIVE_MIN_SPEED_MPS && d > (threshold - predictiveMargin)) geofenceViolation = true
                }
            }
        }

        lastLat = lat; lastLng = lng; lastTs = effectiveTs
        if (!finalIsStalled) lastValidFixTs = nowRealtime else if (isLocal && !isViewerTrail) listener.onGpsStallDetected(nowRealtime)
        
        val isThrottled = sentinel.shouldThrottlePolling()
        val stationaryProb = sentinel.getStationaryProbability()
        var skipPersistence = false
        if (!isSuspicious && isThrottled && stationaryProb > 0.9) {
            if (parkingAnchorPoint == null) parkingAnchorPoint = persistencePoint
            val breakoutThreshold = max(PARKING_ANCHOR_MIN_DIST, accuracy.toDouble() * PARKING_ANCHOR_FACTOR)
            if (PhysicsUtils.calculateDistance(parkingAnchorPoint!!.lat, parkingAnchorPoint!!.lng, persistencePoint.lat, persistencePoint.lng) < breakoutThreshold) skipPersistence = true else parkingAnchorPoint = null
        } else parkingAnchorPoint = null

        val timeSinceLastGpsSave = if (gpsTs > 0 && lastSavedGpsTs > 0) gpsTs - lastSavedGpsTs else 0L
        if (shouldSavePoint(isSuspicious, isThrottled, PhysicsUtils.calculateDistance(lastSavedLat, lastSavedLng, persistencePoint.lat, persistencePoint.lng), timeSinceLastGpsSave) && !skipPersistence) {
            listener.onTrailPointSaved(persistencePoint.lat, persistencePoint.lng, isViewerTrail, false, effectiveTs, isHindsightCorrected = finalIsTrajectoryPromoted)
            lastSavedLat = persistencePoint.lat; lastSavedLng = persistencePoint.lng; lastSavedTs = nowWall; lastSavedGpsTs = gpsTs
        }
        
        lastProcessedAccuracy = accuracy
        var finalOptimized = optimized
        var finalFilteredSpeed = sentinel.getEstimatedSpeedKph()
        if (skipPersistence && parkingAnchorPoint != null) { finalOptimized = parkingAnchorPoint!!; finalFilteredSpeed = 0.0 }

        return ProcessedLocation(
            rawPoint = EngineGeoPoint(lat, lng, ts = effectiveTs), optimizedPoint = finalOptimized, status = sentinelResult.status,
            maxAccuracy = maxAccuracy, currentAccuracy = accuracy, filteredSpeed = finalFilteredSpeed, timestamp = effectiveTs, isStalled = finalIsStalled, 
            isClockRegression = false, receiptRealtime = nowRealtime, isTrajectoryPromoted = finalIsTrajectoryPromoted,
            jumpTier = finalJumpTier, isAdaptiveJump = finalIsAdaptiveJump, distToHome = lastNearestHomeDistance, isSpatiallyValid = true, geofenceViolationDetected = geofenceViolation, tamperDetected = finalIsTamper, jammerDetected = finalIsJammer
        )
    }

    private fun shouldSavePoint(isSuspicious: Boolean, isThrottled: Boolean, distFromLast: Double, timeSinceLast: Long): Boolean {
        if (isSuspicious) return true
        return (distFromLast > (if (isThrottled) PARKING_ANCHOR_MIN_DIST else ACTIVE_MOVE_THRESHOLD) || (timeSinceLast > GPS_SAVE_INTERVAL_MS) || lastSavedTs == 0L)
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
    
    fun getEstimatedBearing(): Float = sentinel.getEstimatedBearing()
    fun resetFilter() { sentinel.reset() }
    fun invalidateHomePointsCache() { cachedHomePoints = null }
    fun resetStats() {
        lastProcessedAccuracy = 0f; maxAccuracy = 0f; accuracyWindow.clear(); lastWindowUpdateRealtime = 0L
        lastDistanceToTracker = null; lastNearestHomeDistance = null
        lastLat = 0.0; lastLng = 0.0; lastTs = 0L; lastSavedLat = 0.0; lastSavedLng = 0.0; lastSavedTs = 0L; lastSavedGpsTs = 0L
        lastHighAccLat = 0.0; lastHighAccLng = 0.0; lastHighAccTs = 0L; lastValidFixTs = 0L; parkingAnchorPoint = null
        invalidateHomePointsCache(); sentinel.reset(); 
        listener.onMaxAccuracyChanged(0f)
    }
}

interface LocationProcessorListener {
    fun onTrailPointSaved(lat: Double, lng: Double, isViewerTrail: Boolean, isJump: Boolean, timestamp: Long, isHindsightCorrected: Boolean = false)
    fun onLogAdded(message: String, type: String, isImportant: Boolean, isSpecial: Boolean, lat: Double, lng: Double, accuracy: Float, snr: Float? = null, vibe: Float? = null)
    fun onMaxAccuracyChanged(accuracy: Float)
    fun onChairBaselineChanged(baseline: Float)
    fun onGpsStallDetected(ts: Long)
}
