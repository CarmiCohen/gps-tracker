package com.gps19.core.engine

import java.util.Locale
import kotlin.math.*

/**
 * LocationProcessor: Handles accuracy filtering and coordinate processing.
 * July.16.22:
 * - Issue #510 & #515: Simplified logic, removed redundant smoothing.
 * - Issue #512: Aligned with consolidated SentinelStatus.
 * - Issue #522: Stationary Jitter. Re-introduced stable spatial anchor for coordinate clamping.
 */
class LocationProcessor(
    private val timeProvider: TimeProvider
) {
    private var listener: LocationProcessorListener? = null

    fun setListener(listener: LocationProcessorListener) {
        this.listener = listener
    }

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

    private var spatialAnchorLat = 0.0
    private var spatialAnchorLng = 0.0
    private var isAnchorLocked = false

    private var cachedHomePoints: List<EngineGeoPoint>? = null
    private var maxDistanceAuthority: Double = 60.0

    fun loadState(
        savedMaxAccuracy: Double,
        spatialAnchor: SpatialAnchor?,
        homePoints: List<EngineGeoPoint>,
        maxDistance: Double
    ) {
        if (savedMaxAccuracy > 0.0) {
            maxAccuracy = savedMaxAccuracy
            accuracyWindow.add(savedMaxAccuracy)
        }
        
        if (spatialAnchor != null && spatialAnchor.lat != 0.0) {
            lastLat = spatialAnchor.lat
            lastLng = spatialAnchor.lng
            lastTs = spatialAnchor.gpsTs
            spatialAnchorLat = spatialAnchor.lat
            spatialAnchorLng = spatialAnchor.lng
            isAnchorLocked = true
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
    fun getBaroBaseline() = sentinel.baroBaseline
    fun getAcousticFloorDb() = sentinel.acousticFloorDb
    fun getAdaptiveVibrationFloor() = sentinel.adaptiveVibrationFloor
    fun getPeakVibrationShock() = sentinel.peakVibrationShock
    fun getPeakVibrationShockTs() = sentinel.peakVibrationShockTs
    
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
        val newMax = (rawMax * 10.0).roundToLong() / 10.0
        
        if (abs(newMax - maxAccuracy) > 0.05) {
            maxAccuracy = newMax
            listener?.onMaxAccuracyChanged(maxAccuracy)
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
        val jumpTier: Int = 0,
        val distToHome: Double? = null,
        val isSpatiallyValid: Boolean = true,
        val geofenceViolationDetected: Boolean = false,
        val tamperDetected: Boolean = false,
        val jammerDetected: Boolean = false,
        val suppressionNote: String? = null
    )

    fun processGpsPoint(
        lat: Double, lng: Double, alt: Double, androidSpeedMps: Double, 
        gpsTs: Long, accuracy: Double, bearing: Double, snr: Double, satsUsed: Int, 
        isViewerTrail: Boolean, lastGpsTs: Long, isLocal: Boolean = false,
        providedMaxAccuracy: Double = 0.0, providedStatus: SentinelStatus? = null,
        providedJumpTier: Int = 0,
        providedIsJammer: Boolean = false, providedIsStalled: Boolean = false,
        providedIsTamper: Boolean = false, providedAdaptiveVibrationFloor: Double = -1.0,
        providedAcousticLockoutTs: Long = 0L,
        nowWall: Long = timeProvider.currentTimeMillis(),
        nowRealtime: Long = timeProvider.elapsedRealtime()
    ): ProcessedLocation {
        val effectiveTs = if (gpsTs > 0) gpsTs else nowWall

        if (lastTs > 0 && effectiveTs < lastTs) {
            val delta = lastTs - effectiveTs
            if (delta > CLOCK_REGRESSION_GATE_MS) { 
                listener?.onLogAdded("Merge-on-Stale: Coordinate update bypassed due to hardware clock regression (${delta}ms). Merging status-only data.", "system", false, true, 0.0, 0.0, 0.0, snr, sentinel.currentVibrationIndex)
                if (delta > 86400000L) { lastTs = 0L; sentinel.reset() }
            }
            val status = providedStatus ?: when {
                providedIsTamper -> SentinelStatus.TAMPER
                providedIsJammer -> SentinelStatus.JUMP
                else -> SentinelStatus.VALID
            }
            val fallbackCoordPoint = EngineGeoPoint(if (lastLat != 0.0) lastLat else lat, if (lastLng != 0.0) lastLng else lng, alt = alt, ts = if (lastTs != 0L) lastTs else effectiveTs, accuracy = accuracy, maxAccuracy = maxAccuracy)
            return ProcessedLocation(rawPoint = fallbackCoordPoint, optimizedPoint = fallbackCoordPoint, status = status, maxAccuracy = maxAccuracy, currentAccuracy = accuracy, filteredSpeed = sentinel.getEstimatedSpeedMps(), timestamp = effectiveTs, isStalled = providedIsStalled, isClockRegression = true, receiptRealtime = nowRealtime, jumpTier = providedJumpTier, distToHome = lastNearestHomeDistance, isSpatiallyValid = true, tamperDetected = providedIsTamper || status == SentinelStatus.TAMPER, jammerDetected = providedIsJammer)
        }

        if (accuracy > HIGH_ACCURACY_THRESHOLD_METERS * TRAJECTORY_REJECTION_ACCURACY_MULT && lastHighAccTs > 0 && nowWall - lastHighAccTs < 60000L) {
            if (PhysicsUtils.calculateDistance(lat, lng, lastHighAccLat, lastHighAccLng) > accuracy) {
                val fallbackCoordPoint = EngineGeoPoint(if (lastLat != 0.0) lastLat else lat, if (lastLng != 0.0) lastLng else lng, alt = alt, ts = if (lastTs != 0L) lastTs else effectiveTs, accuracy = accuracy, maxAccuracy = maxAccuracy)
                return ProcessedLocation(rawPoint = EngineGeoPoint(lat, lng, alt = alt, ts = effectiveTs, accuracy = accuracy, maxAccuracy = maxAccuracy), optimizedPoint = fallbackCoordPoint, status = providedStatus ?: SentinelStatus.VALID, maxAccuracy = maxAccuracy, currentAccuracy = accuracy, filteredSpeed = sentinel.getEstimatedSpeedMps(), timestamp = effectiveTs, isStalled = if (isLocal) false else providedIsStalled, receiptRealtime = nowRealtime, jumpTier = providedJumpTier, distToHome = lastNearestHomeDistance, isSpatiallyValid = false, tamperDetected = providedIsTamper || providedStatus == SentinelStatus.TAMPER, jammerDetected = providedIsJammer)
            }
        }
        
        if (accuracy <= HIGH_ACCURACY_THRESHOLD_METERS) { lastHighAccLat = lat; lastHighAccLng = lng; lastHighAccTs = nowWall }
        if (isLocal) updateWindowedAccuracy(accuracy) else if (providedMaxAccuracy > 0.0) maxAccuracy = providedMaxAccuracy
        if (providedAdaptiveVibrationFloor >= 0.0) sentinel.adaptiveVibrationFloor = providedAdaptiveVibrationFloor
        if (providedAcousticLockoutTs > 0) sentinel.updateSensorState(vibration = -1.0, heading = -1.0, baroAlt = -1000.0, acousticLockoutTs = providedAcousticLockoutTs, nowRealtime = nowRealtime, nowWall = nowWall)

        val sentinelResult = sentinel.processLocation(
            lat = lat, lng = lng, alt = alt, accuracy = accuracy, maxAccuracy = maxAccuracy, 
            bearing = bearing, snr = snr, satsUsed = satsUsed, timestamp = effectiveTs, 
            bypassBehavioral = !isLocal,
            nowWall = nowWall, nowRealtime = nowRealtime
        )
        
        val finalStatus = providedStatus ?: sentinelResult.status
        val finalJumpConfidence = sentinelResult.jumpConfidence ?: JumpConfidence()
        
        val optimizedPoint = sentinelResult.optimizedPoint ?: EngineGeoPoint(lat, lng, alt = alt, ts = effectiveTs, accuracy = accuracy, maxAccuracy = maxAccuracy)

        var geofenceViolation = false
        val home = cachedHomePoints ?: emptyList()
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

        lastLat = lat; lastLng = lng; lastTs = effectiveTs; lastAcc = accuracy; lastMaxAcc = maxAccuracy
        val isStalled = if (isLocal) (gpsTs != 0L && gpsTs == lastGpsTs) else providedIsStalled
        if (!isStalled) lastValidFixTs = nowRealtime else if (isLocal && !isViewerTrail) listener?.onGpsStallDetected(nowRealtime)
        
        val isThrottled = sentinel.shouldThrottlePolling()
        val estimatedSpeed = sentinel.getEstimatedSpeedMps()
        val isStationary = estimatedSpeed < STATIONARY_SPEED_THRESHOLD_MPS

        // Issue #522: Manage spatial anchor for stable stationary clamping
        if (isStationary && !isViewerTrail && finalStatus == SentinelStatus.VALID) {
            if (!isAnchorLocked) {
                spatialAnchorLat = optimizedPoint.lat
                spatialAnchorLng = optimizedPoint.lng
                isAnchorLocked = true
            }
        } else if (!isStationary) {
            isAnchorLocked = false
        }

        val persistencePoint = if (isLocal) optimizedPoint else EngineGeoPoint(lat, lng, alt = alt, ts = effectiveTs, accuracy = accuracy, maxAccuracy = maxAccuracy)
        val timeSinceLastGpsSave = if (gpsTs > 0 && lastSavedGpsTs > 0) gpsTs - lastSavedGpsTs else 0L
        if (shouldSavePoint(finalStatus, isThrottled, PhysicsUtils.calculateDistance(lastSavedLat, lastSavedLng, persistencePoint.lat, persistencePoint.lng), timeSinceLastGpsSave, maxAccuracy)) {
            listener?.onTrailPointSaved(persistencePoint.lat, persistencePoint.lng, isViewerTrail, timestamp = effectiveTs, status = finalStatus, accuracy = persistencePoint.accuracy, maxAccuracy = persistencePoint.maxAccuracy)
            lastSavedLat = persistencePoint.lat; lastSavedLng = persistencePoint.lng; lastSavedTs = nowWall; lastSavedGpsTs = gpsTs
        }
        
        lastProcessedAccuracy = accuracy
        
        val finalOptimized = if (isAnchorLocked && !isViewerTrail && spatialAnchorLat != 0.0) {
            optimizedPoint.copy(lat = spatialAnchorLat, lng = spatialAnchorLng)
        } else {
            optimizedPoint
        }

        return ProcessedLocation(
            rawPoint = EngineGeoPoint(lat, lng, alt = alt, ts = effectiveTs, accuracy = accuracy, maxAccuracy = maxAccuracy), optimizedPoint = finalOptimized, status = finalStatus,
            maxAccuracy = maxAccuracy, currentAccuracy = accuracy, filteredSpeed = estimatedSpeed, timestamp = effectiveTs, isStalled = isStalled, 
            isClockRegression = false, receiptRealtime = nowRealtime,
            jumpTier = finalJumpConfidence.tier, distToHome = lastNearestHomeDistance, isSpatiallyValid = finalStatus == SentinelStatus.VALID, geofenceViolationDetected = geofenceViolation, tamperDetected = finalStatus == SentinelStatus.TAMPER, jammerDetected = finalStatus == SentinelStatus.JUMP,
            suppressionNote = sentinelResult.reason
        )
    }

    private fun shouldSavePoint(status: SentinelStatus, isThrottled: Boolean, distFromLast: Double, timeSinceLast: Long, maxAcc: Double): Boolean {
        if (status == SentinelStatus.TAMPER) return true
        val spatialGate = max(ACTIVE_MOVE_THRESHOLD, maxAcc * DEDUPLICATION_SPATIAL_GATE_FACTOR)
        return (distFromLast > (if (isThrottled) 10.0 else spatialGate) || (timeSinceLast > GPS_SAVE_INTERVAL_MS) || lastSavedTs == 0L)
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
    fun resetFilter() { sentinel.reset(); isAnchorLocked = false }
    fun invalidateHomePointsCache() { cachedHomePoints = null }
    fun resetStats() {
        lastProcessedAccuracy = 0.0; maxAccuracy = 0.0; accuracyWindow.clear(); lastWindowUpdateRealtime = 0L
        lastDistanceToTracker = null; lastNearestHomeDistance = null
        lastLat = 0.0; lastLng = 0.0; lastTs = 0L; lastAcc = 0.0; lastMaxAcc = 0.0
        lastSavedLat = 0.0; lastSavedLng = 0.0; lastSavedTs = 0L; lastSavedGpsTs = 0L
        lastHighAccLat = 0.0; lastHighAccLng = 0.0; lastHighAccTs = 0L; lastValidFixTs = 0L
        spatialAnchorLat = 0.0; spatialAnchorLng = 0.0; isAnchorLocked = false
        invalidateHomePointsCache(); sentinel.reset(); 
        listener?.onMaxAccuracyChanged(0.0)
    }
}

interface LocationProcessorListener {
    fun onTrailPointSaved(lat: Double, lng: Double, isViewerTrail: Boolean, status: SentinelStatus, timestamp: Long, accuracy: Double = 0.0, maxAccuracy: Double = 0.0)
    fun onLogAdded(message: String, type: String, isImportant: Boolean, isSpecial: Boolean, lat: Double, lng: Double, accuracy: Double, snr: Double? = null, vibe: Double? = null)
    fun onMaxAccuracyChanged(accuracy: Double)
    fun onGpsStallDetected(ts: Long)
}
