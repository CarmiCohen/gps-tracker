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
    data class VibrationFloorChanged(val floor: Double) : ProcessorEvent()
    data class LuxBaselineChanged(val baseline: Double) : ProcessorEvent()
    data class AcousticFloorChanged(val floor: Double) : ProcessorEvent()
    data class GpsStallDetected(val rt: Long) : ProcessorEvent()
}

/**
 * LocationProcessor: Handles accuracy filtering and coordinate processing.
 * Sep.24.97:
 * - Issue #1291: Refactored processGpsPoint to consume SystemEvaluationSnapshot 
 *   to align with the unified domain event model.
 * Sep.24.96:
 * - Issue #1312 REMEDIATION: Migrated updateSensorData and processGpsPoint to 
 *   consume unified SystemEvaluationSnapshot, ensuring data consistency across 
 *   all processing layers.
 */
class LocationProcessor(
    private val timeProvider: TimeProvider
) {
    private val _processorEvents = MutableSharedFlow<ProcessorEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val processorEvents: SharedFlow<ProcessorEvent> = _processorEvents.asSharedFlow()

    private val processedLocationFlyweight = ProcessedLocation()
    val state = LocationProcessingState()

    fun loadState(
        savedMaxAccuracy: Double,
        savedLastSitTs: Long,
        savedBaseline: Double,
        trackerState: SpatialAnchor?,
        homePoints: List<EngineGeoPoint>,
        maxDistance: Double,
        savedSitVz: Double = 0.0,
        savedSitDz: Double = 0.0,
        savedSitBaro: Double = 0.0,
        savedSitTilt: Double = 0.0,
        savedSitShock: Double = 0.0,
        savedSitVzTs: Long = 0L,
        savedSitVzRt: Long = 0L,
        savedVibrationFloor: Double = -1.0,
        savedLuxBaseline: Double = -1.0,
        savedAcousticFloor: Double = -1.0
    ) {
        if (savedMaxAccuracy > 0.0) {
            state.maxAccuracy = savedMaxAccuracy
            fillAccuracyWindow(savedMaxAccuracy)
        } else {
            resetAccuracyWindow()
        }
        
        LocationSentinel.loadForensicState(
            state, savedLastSitTs, savedBaseline,
            savedSitVz, savedSitDz, savedSitBaro, savedSitTilt, savedSitShock,
            savedSitVzTs, savedSitVzRt, savedVibrationFloor,
            savedLuxBaseline, savedAcousticFloor
        )
        
        if (trackerState != null && trackerState.lat != 0.0) {
            state.lastLat = trackerState.lat
            state.lastLng = trackerState.lng
            state.lastTs = trackerState.gpsTs
            state.lastRt = timeProvider.elapsedRealtime()
            LocationSentinel.setSpatialAnchor(state, trackerState.lat, trackerState.lat, trackerState.alt, trackerState.gpsTs, state.lastRt)
        }

        state.cachedHomePoints = homePoints
        state.maxDistanceAuthority = maxDistance
        AnchorEvaluator.reset(state)
    }

    private fun addAccuracyToWindow(acc: Double) {
        state.accuracyWindowBuffer[state.accuracyWindowHead] = acc
        state.accuracyWindowHead = (state.accuracyWindowHead + 1) % ACCURACY_WINDOW_MAX_SIZE
        if (state.accuracyWindowSize < ACCURACY_WINDOW_MAX_SIZE) state.accuracyWindowSize++
    }

    private fun fillAccuracyWindow(acc: Double) {
        for (i in 0 until ACCURACY_WINDOW_MAX_SIZE) {
            state.accuracyWindowBuffer[i] = acc
        }
        state.accuracyWindowSize = ACCURACY_WINDOW_MAX_SIZE
        state.accuracyWindowHead = 0
    }

    private fun resetAccuracyWindow() {
        state.accuracyWindowSize = 0
        state.accuracyWindowHead = 0
        state.accuracyWindowBuffer.fill(0.0)
    }

    private fun updateLastAccuracyInWindow(acc: Double) {
        if (state.accuracyWindowSize > 0) {
            val lastIdx = (state.accuracyWindowHead - 1 + ACCURACY_WINDOW_MAX_SIZE) % ACCURACY_WINDOW_MAX_SIZE
            state.accuracyWindowBuffer[lastIdx] = acc
        } else {
            addAccuracyToWindow(acc)
        }
    }

    private fun getMaxAccuracyFromWindow(): Double {
        if (state.accuracyWindowSize == 0) return 0.0
        var m = 0.0
        for (i in 0 until state.accuracyWindowSize) {
            m = max(m, state.accuracyWindowBuffer[i])
        }
        return m
    }

    fun setMaxDistanceAuthority(distance: Double) {
        state.maxDistanceAuthority = distance
    }

    fun setHomePoints(points: List<EngineGeoPoint>) {
        state.cachedHomePoints = points
    }

    fun getLastProcessedAccuracy() = state.lastProcessedAccuracy
    fun getMaxTrackerAccuracy() = state.maxAccuracy
    fun getLastValidFixRt() = state.lastValidFixRt
    fun setLastValidFixRt(rt: Long) { state.lastValidFixRt = rt }
    fun getMaxDistanceAuthority() = state.maxDistanceAuthority

    fun getLuxBaseline() = state.luxBaseline
    fun getBaroBaseline() = state.baroBaseline
    fun getAcousticFloorDb() = state.acousticFloorDb
    fun getAdaptiveVibrationFloor() = state.adaptiveVibrationFloor
    fun getPeakVibrationShock() = state.peakVibrationShock
    fun getPeakVibrationShockRt() = state.peakVibrationShockRt
    
    fun getChairBaselineTilt() = state.baselineSitTilt
    fun getLastSitTs() = state.lastSitTs
    fun getLastSitRt() = state.lastSitRt

    fun consumeSitDetected(): Boolean = LocationSentinel.consumeSitDetected(state)

    fun checkPhysicalTamper(nowRt: Long, isMuzzled: Boolean): SentinelStatus {
        return LocationSentinel.checkPhysicalTamper(state, nowRt, isMuzzled)
    }

    fun updateExpectedInterval(nowRt: Long, expectedIntervalMs: Long) {
        if (expectedIntervalMs != state.lastExpectedIntervalMs) {
            if (state.lastExpectedIntervalMs != 0L) {
                state.lastIntervalChangeRt = nowRt
            }
            state.lastExpectedIntervalMs = expectedIntervalMs
        }
    }

    private fun isAdaptationMuzzled(nowRt: Long): Boolean {
        if (state.lastIntervalChangeRt == 0L) return false
        return nowRt - state.lastIntervalChangeRt < ADAPTATION_SETTLING_MS
    }

    fun updateSensorData(snapshot: SystemEvaluationSnapshot): Boolean {
        return LatencyMonitor.measureAndAudit<Boolean>(
            timeProvider,
            LATENCY_THRESHOLD_SENSOR_PROCESS_MS,
            "updateSensorData",
            LatencyMonitor.AuditType.PERFORMANCE,
            { message, _ ->
                _processorEvents.tryEmit(ProcessorEvent.LogAdded(message, "system", false, true, 0.0, 0.0, 0.0, null, snapshot.vibration))
            }
        ) {
            val oldVibeFloor = state.adaptiveVibrationFloor
            val oldLuxBaseline = state.luxBaseline
            val oldAcousticFloor = state.acousticFloorDb
            
            val baselineChanged = LocationSentinel.updateSensorState(state, snapshot)
            
            val newVibeFloor = state.adaptiveVibrationFloor
            val newLuxBaseline = state.luxBaseline
            val newAcousticFloor = state.acousticFloorDb
            
            if (abs(newVibeFloor - oldVibeFloor) > 0.01) {
                _processorEvents.tryEmit(ProcessorEvent.VibrationFloorChanged(newVibeFloor))
            }
            
            if (abs(newLuxBaseline - oldLuxBaseline) > 1.0) {
                _processorEvents.tryEmit(ProcessorEvent.LuxBaselineChanged(newLuxBaseline))
            }

            if (abs(newAcousticFloor - oldAcousticFloor) > 1.0) {
                _processorEvents.tryEmit(ProcessorEvent.AcousticFloorChanged(newAcousticFloor))
            }

            if (baselineChanged) {
                _processorEvents.tryEmit(ProcessorEvent.ChairBaselineChanged(state.baselineSitTilt))
            }
            baselineChanged
        }
    }

    fun resetChairBaseline() {
        LocationSentinel.resetChairBaseline(state)
        _processorEvents.tryEmit(ProcessorEvent.ChairBaselineChanged(state.baselineSitTilt))
    }

    fun shouldThrottlePolling(providedIsStationary: Boolean? = null): Boolean = LocationSentinel.shouldThrottlePolling(state, providedIsStationary)

    fun updateWindowedAccuracy(acc: Double) {
        if (acc <= 0.0) return
        val nowRt = timeProvider.elapsedRealtime()
        val bucketDuration = ACCURACY_WINDOW_BUCKET_MS / ACCURACY_WINDOW_MAX_SIZE
        
        if (state.accuracyWindowSize == 0 || (state.lastWindowUpdateRt > 0 && nowRt - state.lastWindowUpdateRt >= bucketDuration)) {
            addAccuracyToWindow(acc)
            state.lastWindowUpdateRt = nowRt
        } else {
            if (state.lastWindowUpdateRt == 0L) state.lastWindowUpdateRt = nowRt
            val lastIdx = (state.accuracyWindowHead - 1 + ACCURACY_WINDOW_MAX_SIZE) % ACCURACY_WINDOW_MAX_SIZE
            val currentMaxInBucket = state.accuracyWindowBuffer[lastIdx]
            if (acc > currentMaxInBucket * GEOFENCE_ACCURACY_HYSTERESIS_MULT) {
                updateLastAccuracyInWindow(acc)
            }
        }
        
        val rawMax = getMaxAccuracyFromWindow()
        val newMax = (rawMax * 10.0).roundToLong() / 10.0
        
        if (abs(newMax - state.maxAccuracy) > 0.05) {
            state.maxAccuracy = newMax
            _processorEvents.tryEmit(ProcessorEvent.MaxAccuracyChanged(state.maxAccuracy))
        }
    }

    fun processGpsPoint(
        snapshot: SystemEvaluationSnapshot,
        isViewerTrail: Boolean,
        lastGpsTs: Long,
        isLocal: Boolean = false
    ): ProcessedLocation {
        val lat = snapshot.lat
        val lng = snapshot.lng
        val alt = snapshot.alt
        val androidSpeedMps = snapshot.speed
        val gpsTs = snapshot.gpsTs
        val accuracy = snapshot.accuracy
        val bearing = snapshot.bearing
        val snr = snapshot.snrSnapshot ?: 0.0
        val nowRt = snapshot.nowRt
        val nowWall = snapshot.nowTs

        return LatencyMonitor.measureAndAudit<ProcessedLocation>(
            timeProvider,
            LATENCY_THRESHOLD_GPS_PROCESS_MS,
            "processGpsPoint",
            LatencyMonitor.AuditType.PERFORMANCE,
            { message, _ ->
                _processorEvents.tryEmit(ProcessorEvent.LogAdded(message, "system", false, true, lat, lng, accuracy, snr, state.currentVibrationIndex))
            }
        ) {
            processedLocationFlyweight.reset()
            val effectiveTs = if (gpsTs > 0) gpsTs else nowWall
            val adaptationMuzzled = isAdaptationMuzzled(nowRt)

            if (state.lastTs > 0 && effectiveTs < state.lastTs) {
                val delta = state.lastTs - effectiveTs
                if (delta > CLOCK_REGRESSION_GATE_MS) { 
                    _processorEvents.tryEmit(ProcessorEvent.LogAdded("Merge-on-Stale: Coordinate update bypassed due to hardware clock regression (${delta}ms). Merging status-only data.", "system", false, true, 0.0, 0.0, 0.0, snr, state.currentVibrationIndex))
                    if (delta > 86400000L) { state.lastTs = 0L; state.lastRt = 0L; LocationSentinel.reset(state) }
                }
                val status = snapshot.status
                val fallbackCoordPoint = EngineGeoPoint(if (state.lastLat != 0.0) state.lastLat else lat, if (state.lastLng != 0.0) state.lastLng else lng, alt = alt, ts = if (state.lastTs != 0L) state.lastTs else effectiveTs, rt = if (state.lastRt != 0L) state.lastRt else nowRt, accuracy = accuracy, maxAccuracy = state.maxAccuracy)
                return@measureAndAudit processedLocationFlyweight.apply {
                    this.rawPoint = fallbackCoordPoint
                    this.optimizedPoint = fallbackCoordPoint
                    this.status = status
                    this.maxAccuracy = state.maxAccuracy
                    this.currentAccuracy = accuracy
                    this.filteredSpeed = state.estimatedSpeedMps
                    this.timestamp = effectiveTs
                    this.rt = nowRt
                    this.isStalled = snapshot.isStalled
                    this.isClockRegression = true
                    this.receiptRt = nowRt
                    this.isTrajectoryPromoted = false
                    this.jumpTier = snapshot.jumpTier
                    this.isAdaptiveJump = snapshot.isAdaptiveJump
                    this.distToHome = state.lastNearestHomeDistance
                    this.isSpatiallyValid = true
                    this.tamperDetected = snapshot.tamperDetected
                    this.jammerDetected = snapshot.jammerDetected
                    this.kineticEnergy = snapshot.kineticEnergy
                }
            }

            val TRAJECTORY_PROMOTION_WINDOW_MS = 60000L
            if (accuracy > HIGH_ACCURACY_THRESHOLD_METERS * TRAJECTORY_REJECTION_ACCURACY_MULT && state.lastHighAccRt > 0 && nowRt - state.lastHighAccRt < TRAJECTORY_PROMOTION_WINDOW_MS) {
                if (PhysicsUtils.calculateDistance(lat, lng, state.lastHighAccLat, state.lastHighAccLng) > accuracy) {
                    val fallbackCoordPoint = EngineGeoPoint(if (state.lastLat != 0.0) state.lastLat else lat, if (state.lastLng != 0.0) state.lastLng else lng, alt = alt, ts = if (state.lastTs != 0L) state.lastTs else effectiveTs, rt = if (state.lastRt != 0L) state.lastRt else nowRt, accuracy = accuracy, maxAccuracy = state.maxAccuracy)
                    return@measureAndAudit processedLocationFlyweight.apply {
                        this.rawPoint = EngineGeoPoint(lat, lng, alt = alt, ts = effectiveTs, rt = nowRt, accuracy = accuracy, maxAccuracy = state.maxAccuracy)
                        this.optimizedPoint = fallbackCoordPoint
                        this.status = SentinelStatus.VALID
                        this.maxAccuracy = state.maxAccuracy
                        this.currentAccuracy = accuracy
                        this.filteredSpeed = state.estimatedSpeedMps
                        this.timestamp = effectiveTs
                        this.rt = nowRt
                        this.isStalled = if (isLocal) false else snapshot.isStalled
                        this.receiptRt = nowRt
                        this.jumpTier = snapshot.jumpTier
                        this.isAdaptiveJump = snapshot.isAdaptiveJump
                        this.distToHome = state.lastNearestHomeDistance
                        this.isSpatiallyValid = false
                        this.tamperDetected = snapshot.tamperDetected
                        this.jammerDetected = snapshot.jammerDetected
                        this.kineticEnergy = snapshot.kineticEnergy
                    }
                }
            }
            
            if (accuracy <= HIGH_ACCURACY_THRESHOLD_METERS) { state.lastHighAccLat = lat; state.lastHighAccLng = lng; state.lastHighAccTs = nowWall; state.lastHighAccRt = nowRt }
            if (isLocal) updateWindowedAccuracy(accuracy) else if (snapshot.maxAccuracy > 0.0) state.maxAccuracy = snapshot.maxAccuracy
            
            if (snapshot.acousticLockoutRt > 0 || snapshot.lightSpikeRt > 0 || snapshot.providedAdaptiveFloor >= 0.0) {
                val oldVibeFloor = state.adaptiveVibrationFloor
                val oldLuxBaseline = state.luxBaseline
                val oldAcousticFloor = state.acousticFloorDb
                
                LocationSentinel.updateSensorState(state, snapshot)
                
                val newVibeFloor = state.adaptiveVibrationFloor
                val newLuxBaseline = state.luxBaseline
                val newAcousticFloor = state.acousticFloorDb

                if (abs(newVibeFloor - oldVibeFloor) > 0.01) {
                    _processorEvents.tryEmit(ProcessorEvent.VibrationFloorChanged(newVibeFloor))
                }
                if (abs(newLuxBaseline - oldLuxBaseline) > 1.0) {
                    _processorEvents.tryEmit(ProcessorEvent.LuxBaselineChanged(newLuxBaseline))
                }
                if (abs(newAcousticFloor - oldAcousticFloor) > 1.0) {
                    _processorEvents.tryEmit(ProcessorEvent.AcousticFloorChanged(newAcousticFloor))
                }
            }

            val sentinelResult = LocationSentinel.processLocation(
                state = state,
                lat = lat, lng = lng, alt = alt, accuracy = accuracy, maxAccuracy = state.maxAccuracy, 
                bearing = bearing, snr = snr, satsUsed = 0, timestamp = effectiveTs, 
                bypassBehavioral = !isLocal, isSuspicious = snapshot.isMuzzled || adaptationMuzzled,
                isMuzzled = snapshot.isMuzzled, nowTs = nowWall, nowRt = nowRt
            )
            
            if (sentinelResult.status == SentinelStatus.TRAJECTORY_PROMOTED) {
                val promotedPoints = sentinelResult.promotedPoints
                if (promotedPoints != null && promotedPoints.isNotEmpty() && state.lastLat != 0.0) {
                    val firstPromoted = promotedPoints.first()
                    PhysicsUtils.interpolateSegmentCallback(
                        state.lastLat, state.lastLng, state.lastTs, firstPromoted.lat, firstPromoted.lng, firstPromoted.ts,
                        startAcc = state.lastAcc, startMaxAcc = state.lastMaxAcc, endAcc = accuracy, endMaxAcc = state.maxAccuracy
                    ) { pLat, pLng, pTs, pAcc, pMaxAcc ->
                        _processorEvents.tryEmit(ProcessorEvent.TrailPointSaved(pLat, pLng, isViewerTrail, SentinelStatus.VALID, pTs, accuracy = pAcc, maxAccuracy = pMaxAcc))
                    }
                }
                promotedPoints?.forEach { p ->
                    _processorEvents.tryEmit(ProcessorEvent.TrailPointSaved(p.lat, p.lng, isViewerTrail, SentinelStatus.VALID, p.ts, accuracy = p.accuracy, maxAccuracy = p.maxAccuracy))
                }
            }

            val isActualJump = (sentinelResult.status == SentinelStatus.JUMP || sentinelResult.status == SentinelStatus.OUTLIER || sentinelResult.status == SentinelStatus.JITTER || (sentinelResult.jumpConfidence?.isJump == true))
            val isMuzzledJump = adaptationMuzzled && (sentinelResult.status == SentinelStatus.JUMP || sentinelResult.status == SentinelStatus.JITTER)
            val finalStatus = if (isMuzzledJump) SentinelStatus.VALID else sentinelResult.status
            val finalSuppressionNote = if (isMuzzledJump) "Settling A15 Polling..." else sentinelResult.reason

            val isActualJammer = (sentinelResult.status == SentinelStatus.JAMMER_SUSPICION || (sentinelResult.jumpConfidence?.isOutlier == true))
            val finalIsJump = (isActualJump && !isMuzzledJump) || snapshot.isJammer
            val finalIsTrajectoryPromoted = sentinelResult.status == SentinelStatus.TRAJECTORY_PROMOTED
            val finalJumpTier = maxOf(sentinelResult.jumpConfidence?.tier ?: 0, snapshot.jumpTier)
            val finalIsAdaptiveJump = (sentinelResult.jumpConfidence?.isAdaptiveJump == true) || snapshot.isAdaptiveJump
            val finalIsTamper = sentinelResult.status == SentinelStatus.TAMPER || snapshot.tamperDetected
            val finalIsJammer = finalIsJump || finalIsTamper || isActualJammer || snapshot.jammerDetected
            val finalIsStalled = if (isLocal) (gpsTs != 0L && gpsTs == lastGpsTs) else snapshot.isStalled
            val isSpatiallyValid = !finalIsJump && !finalIsJammer && finalStatus != SentinelStatus.OUTLIER
            
            val fallbackPoint = EngineGeoPoint(if (state.lastLat != 0.0) state.lastLat else lat, if (state.lastLng != 0.0) state.lastLng else lng, alt = alt, ts = if (state.lastTs != 0L) state.lastTs else effectiveTs, rt = if (state.lastRt != 0L) state.lastRt else nowRt, accuracy = state.lastAcc, maxAccuracy = state.lastMaxAcc)

            if (!isSpatiallyValid) {
                if (shouldSavePoint(snapshot.isMuzzled || adaptationMuzzled, true, PhysicsUtils.calculateDistance(state.lastSavedLat, state.lastSavedLng, lat, lng), 0L, state.maxAccuracy, nowRt)) {
                    _processorEvents.tryEmit(ProcessorEvent.TrailPointSaved(lat, lng, isViewerTrail, finalStatus, effectiveTs, accuracy = accuracy, maxAccuracy = state.maxAccuracy))
                }
                return@measureAndAudit processedLocationFlyweight.apply {
                    this.rawPoint = EngineGeoPoint(lat, lng, alt = alt, ts = effectiveTs, rt = nowRt, accuracy = accuracy, maxAccuracy = state.maxAccuracy)
                    this.optimizedPoint = fallbackPoint
                    this.status = finalStatus
                    this.maxAccuracy = state.maxAccuracy
                    this.currentAccuracy = accuracy
                    this.filteredSpeed = state.estimatedSpeedMps
                    this.timestamp = effectiveTs
                    this.rt = nowRt
                    this.isStalled = finalIsStalled
                    this.receiptRt = nowRt
                    this.isTrajectoryPromoted = finalIsTrajectoryPromoted
                    this.jumpTier = finalJumpTier
                    this.isAdaptiveJump = finalIsAdaptiveJump
                    this.distToHome = state.lastNearestHomeDistance
                    this.isSpatiallyValid = false
                    this.tamperDetected = finalIsTamper
                    this.jammerDetected = finalIsJammer
                    this.suppressionNote = finalSuppressionNote
                    this.kineticEnergy = if (isLocal) state.kineticEnergy else snapshot.kineticEnergy
                }
            }

            val optimizedPoint = sentinelResult.optimizedPoint ?: EngineGeoPoint(lat, lng, alt = alt, ts = effectiveTs, rt = nowRt, accuracy = accuracy, maxAccuracy = state.maxAccuracy)
            val persistencePoint = if (isLocal) optimizedPoint else EngineGeoPoint(lat, lng, alt = alt, ts = effectiveTs, rt = nowRt, accuracy = accuracy, maxAccuracy = state.maxAccuracy)
            
            var geofenceViolation = false
            val home = state.cachedHomePoints
            if (home != null && home.isNotEmpty() && finalStatus == SentinelStatus.VALID) {
                var minD = Double.MAX_VALUE
                var hasValidHome = false
                for (i in home.indices) {
                    val p = home[i]
                    if (PhysicsUtils.isValidLocation(p.lat, p.lng)) {
                        val d = PhysicsUtils.calculateDistance(optimizedPoint.lat, optimizedPoint.lng, p.lat, p.lng)
                        if (d < minD) minD = d
                        hasValidHome = true
                    }
                }
                
                if (hasValidHome) {
                    state.lastNearestHomeDistance = minD
                    if (!isViewerTrail) {
                        val speedMps = state.estimatedSpeedMps
                        val predictiveMargin = speedMps * GEOFENCE_PREDICTIVE_LOOKAHEAD_S
                        val threshold = state.maxDistanceAuthority + (state.maxAccuracy * GEOFENCE_BUFFER_MULT * GEOFENCE_ACCURACY_EXPANSION_MULT)
                        if (speedMps > GEOFENCE_PREDICTIVE_MIN_SPEED_MPS && minD > (threshold - predictiveMargin)) geofenceViolation = true
                    }
                }
            }

            state.lastLat = lat; state.lastLng = lng; state.lastTs = effectiveTs; state.lastRt = nowRt; state.lastAcc = accuracy; state.lastMaxAcc = state.maxAccuracy
            if (!finalIsStalled) state.lastValidFixRt = nowRt else if (isLocal && !isViewerTrail) _processorEvents.tryEmit(ProcessorEvent.GpsStallDetected(nowRt))
            
            val isThrottled = LocationSentinel.shouldThrottlePolling(state)
            val estimatedSpeed = state.estimatedSpeedMps
            val stationaryProb = state.stationaryProb
            
            val anchorResult = AnchorEvaluator.evaluate(
                state = state,
                point = persistencePoint,
                isPhysicallyStationary = LocationSentinel.isStationary(state),
                stationaryProb = stationaryProb,
                estimatedSpeed = estimatedSpeed,
                maxAccuracy = state.maxAccuracy,
                isSuspicious = snapshot.isMuzzled || adaptationMuzzled,
                isAdaptationMuzzled = adaptationMuzzled,
                isAccuracySnap = sentinelResult.jumpConfidence?.reason?.contains("Suppressed Accuracy Snap") == true,
                snr = snr,
                vibeIndex = state.currentVibrationIndex,
                onLog = { msg, lLat, lLng, lAcc, lVibe ->
                    _processorEvents.tryEmit(ProcessorEvent.LogAdded(msg, "system", false, false, lLat, lLng, lAcc, null, lVibe))
                }
            )

            val skipPersistence = anchorResult.shouldSkipPersistence
            val isAnchorLockedNow = anchorResult.isLocked

            val timeSinceLastGpsSaveRt = if (nowRt > 0 && state.lastSavedRt > 0) nowRt - state.lastSavedRt else 0L
            if (shouldSavePoint(snapshot.isMuzzled || adaptationMuzzled, isThrottled, PhysicsUtils.calculateDistance(state.lastSavedLat, state.lastSavedLng, persistencePoint.lat, persistencePoint.lng), timeSinceLastGpsSaveRt, state.maxAccuracy, nowRt) && !skipPersistence) {
                _processorEvents.tryEmit(ProcessorEvent.TrailPointSaved(persistencePoint.lat, persistencePoint.lng, isViewerTrail, finalStatus, effectiveTs, accuracy = persistencePoint.accuracy, maxAccuracy = persistencePoint.maxAccuracy))
                state.lastSavedLat = persistencePoint.lat; state.lastSavedLng = persistencePoint.lng; state.lastSavedTs = nowWall; state.lastSavedRt = nowRt; state.lastSavedGpsTs = gpsTs
            }
            
            state.lastProcessedAccuracy = accuracy
            
            val finalOptimized = if (isAnchorLockedNow && !isViewerTrail) {
                anchorResult.optimizedPoint
            } else {
                optimizedPoint
            }

            processedLocationFlyweight.apply {
                this.rawPoint = EngineGeoPoint(lat, lng, alt = alt, ts = effectiveTs, rt = nowRt, accuracy = accuracy, maxAccuracy = state.maxAccuracy)
                this.optimizedPoint = finalOptimized
                this.status = finalStatus
                this.maxAccuracy = state.maxAccuracy
                this.currentAccuracy = accuracy
                this.filteredSpeed = estimatedSpeed
                this.timestamp = effectiveTs
                this.rt = nowRt
                this.isStalled = finalIsStalled
                this.isClockRegression = false
                this.receiptRt = nowRt
                this.isTrajectoryPromoted = finalIsTrajectoryPromoted
                this.jumpTier = finalJumpTier
                this.isAdaptiveJump = finalIsAdaptiveJump
                this.distToHome = state.lastNearestHomeDistance
                this.isSpatiallyValid = true
                this.geofenceViolationDetected = geofenceViolation
                this.tamperDetected = finalIsTamper
                this.jammerDetected = finalIsJammer
                this.isAnchorLocked = isAnchorLockedNow
                this.suppressionNote = finalSuppressionNote
                this.kineticEnergy = if (isLocal) state.kineticEnergy else snapshot.kineticEnergy
            }
        }
    }

    private fun isStationary(): Boolean = LocationSentinel.isStationary(state)

    private fun shouldSavePoint(isSuspicious: Boolean, isThrottled: Boolean, distFromLast: Double, timeSinceLastRt: Long, maxAcc: Double, nowRt: Long): Boolean {
        if (isSuspicious) return true
        val spatialGate = max(ACTIVE_MOVE_THRESHOLD, maxAcc * DEDUPLICATION_SPATIAL_GATE_FACTOR)
        return (distFromLast > (if (isThrottled) PARKING_ANCHOR_MIN_DIST else spatialGate) || (timeSinceLastRt > GPS_SAVE_INTERVAL_MS) || state.lastSavedRt == 0L)
    }

    fun getDistanceToTracker() = state.lastDistanceToTracker
    fun getNearestHomeDistance() = state.lastNearestHomeDistance
    
    fun updateCalculatedDistances(lat: Double, lng: Double, isViewerTrail: Boolean, trackerState: SpatialAnchor?) {
        val home = state.cachedHomePoints
        if (isViewerTrail) { 
            if (trackerState != null && PhysicsUtils.isValidLocation(trackerState.lat, trackerState.lng)) state.lastDistanceToTracker = PhysicsUtils.calculateDistance(lat, lng, trackerState.lat, trackerState.lng) 
        } else if (home != null && home.isNotEmpty()) { 
            var minD = Double.MAX_VALUE
            var hasValidHome = false
            for (i in home.indices) {
                val p = home[i]
                if (PhysicsUtils.isValidLocation(p.lat, p.lng)) {
                    val d = PhysicsUtils.calculateDistance(lat, lng, p.lat, p.lng)
                    if (d < minD) minD = d
                    hasValidHome = true
                }
            }
            if (hasValidHome) state.lastNearestHomeDistance = minD
        }
    }
    
    fun getEstimatedBearing(): Double = state.estimatedBearing
    fun resetFilter() { 
        LocationSentinel.reset(state)
        AnchorEvaluator.reset(state)
    }
    fun invalidateHomePointsCache() { state.cachedHomePoints = null }
    fun resetStats() {
        state.lastProcessedAccuracy = 0.0; state.maxAccuracy = 0.0
        resetAccuracyWindow()
        state.lastDistanceToTracker = null; state.lastNearestHomeDistance = null
        state.lastLat = 0.0; state.lastLng = 0.0; state.lastTs = 0L; state.lastRt = 0L; state.lastAcc = 0.0; state.lastMaxAcc = 0.0
        state.lastSavedLat = 0.0; state.lastSavedLng = 0.0; state.lastSavedTs = 0L; state.lastSavedRt = 0L; state.lastSavedGpsTs = 0L
        state.lastHighAccLat = 0.0; state.lastHighAccLng = 0.0; state.lastHighAccTs = 0L; state.lastHighAccRt = 0L; state.lastValidFixRt = 0L
        AnchorEvaluator.reset(state)
        invalidateHomePointsCache()
        LocationSentinel.reset(state)
        _processorEvents.tryEmit(ProcessorEvent.MaxAccuracyChanged(0.0))
        state.lastExpectedIntervalMs = 0L
        state.lastIntervalChangeRt = 0L
    }
}
