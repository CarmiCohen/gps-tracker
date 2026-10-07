package com.gps19.core.engine

import java.util.Locale
import kotlin.math.*

/**
 * LocationProcessor: Handles accuracy filtering and coordinate processing.
 * Oct.7.6:
 * - Issue #SIMP-1007-16: JNI FastPath Expansion. Updated LogAdded event 
 *   emission to utilize unified ForensicSnapshot container, resolving 
 *   compilation regressions from Oct7.5 container migration.
 * Oct.4.6:
 * - Issue #1160: Flyweight & Pooling Expansion. Migrated processed location 
 *   generation to EnginePools.PROCESSED_LOCATION and EnginePools.GEO_POINT 
 *   to eliminate per-tick allocations (R1160).
 */
class LocationProcessor(
    private val timeProvider: TimeProvider,
    private val domainEventBus: DomainEventBus? = null, // Optional for unit tests
    private val isPrimary: Boolean = true
) {
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
        savedLastValidFixRt: Long = 0L,
        savedVibrationFloor: Double = -1.0,
        savedLuxBaseline: Double = -1.0,
        savedAcousticFloor: Double = -1.0
    ) {
        if (savedMaxAccuracy > 0.0) {
            state.accuracy.maxAccuracy = savedMaxAccuracy
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

        state.lastValidFixRt = savedLastValidFixRt
        
        if (trackerState != null && trackerState.lat != 0.0) {
            state.lastLat = trackerState.lat
            state.lastLng = trackerState.lng
            state.lastTs = trackerState.gpsTs
            state.lastRt = timeProvider.elapsedRealtime()
            LocationSentinel.setSpatialAnchor(state, trackerState.lat, trackerState.lng, trackerState.alt, trackerState.gpsTs, state.lastRt)
        }

        state.cachedHomePoints = homePoints
        state.maxDistanceAuthority = maxDistance
        AnchorEvaluator.reset(state)
    }

    private fun addAccuracyToWindow(acc: Double) {
        state.accuracy.windowBuffer[state.accuracy.windowHead] = acc
        state.accuracy.windowHead = (state.accuracy.windowHead + 1) % ACCURACY_WINDOW_MAX_SIZE
        if (state.accuracy.windowSize < ACCURACY_WINDOW_MAX_SIZE) state.accuracy.windowSize++
    }

    private fun fillAccuracyWindow(acc: Double) {
        for (i in 0 until ACCURACY_WINDOW_MAX_SIZE) {
            state.accuracy.windowBuffer[i] = acc
        }
        state.accuracy.windowSize = ACCURACY_WINDOW_MAX_SIZE
        state.accuracy.windowHead = 0
    }

    private fun resetAccuracyWindow() {
        state.accuracy.windowSize = 0
        state.accuracy.windowHead = 0
        state.accuracy.windowBuffer.fill(0.0)
    }

    private fun updateLastAccuracyInWindow(acc: Double) {
        if (state.accuracy.windowSize > 0) {
            val lastIdx = (state.accuracy.windowHead - 1 + ACCURACY_WINDOW_MAX_SIZE) % ACCURACY_WINDOW_MAX_SIZE
            state.accuracy.windowBuffer[lastIdx] = acc
        } else {
            addAccuracyToWindow(acc)
        }
    }

    private fun getMaxAccuracyFromWindow(): Double {
        if (state.accuracy.windowSize == 0) return 0.0
        var m = 0.0
        for (i in 0 until state.accuracy.windowSize) {
            m = max(m, state.accuracy.windowBuffer[i])
        }
        return m
    }

    fun setMaxDistanceAuthority(distance: Double) {
        state.maxDistanceAuthority = distance
    }

    fun setHomePoints(points: List<EngineGeoPoint>) {
        state.cachedHomePoints = points
    }

    fun getLastProcessedAccuracy() = state.accuracy.lastProcessedAccuracy
    fun getMaxTrackerAccuracy() = state.accuracy.maxAccuracy
    fun getLastValidFixRt() = state.lastValidFixRt
    fun setLastValidFixRt(rt: Long) { state.lastValidFixRt = rt }
    fun getMaxDistanceAuthority() = state.maxDistanceAuthority

    fun getLuxBaseline() = state.forensic.luxBaseline
    fun getBaroBaseline() = state.forensic.baroBaseline
    fun getAcousticFloorDb() = state.forensic.acousticFloorDb
    fun getAdaptiveVibrationFloor() = state.forensic.adaptiveVibrationFloor
    fun getPeakVibrationShock() = state.forensic.peakVibrationShock
    fun getPeakVibrationShockRt() = state.forensic.peakVibrationShockRt
    
    fun getChairBaselineTilt() = state.forensic.baselineSitTilt
    fun getLastSitTs() = state.forensic.lastSitTs
    fun getLastSitRt() = state.forensic.lastSitRt

    fun consumeSitDetected(): Boolean = LocationSentinel.consumeSitDetected(state)

    fun checkPhysicalTamper(nowRt: Long, isMuzzled: Boolean, cpuLoad: Double = 0.0): SentinelStatus {
        return LocationSentinel.checkPhysicalTamper(state, nowRt, isMuzzled, cpuLoad)
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

    fun updateSensorData(update: LocationUpdate): Boolean {
        return LatencyMonitor.measureAndAudit<Boolean>(
            timeProvider,
            LATENCY_THRESHOLD_SENSOR_PROCESS_MS,
            "updateSensorData",
            LatencyMonitor.AuditType.PERFORMANCE,
            { message, _ ->
                emitEvent(ProcessorEvent.LogAdded(message, "system", false, true, 0.0, 0.0, 0.0, ForensicSnapshot(vibe = update.atmospheric.vibration), isPrimary))
            }
        ) {
            val oldVibeFloor = state.forensic.adaptiveVibrationFloor
            val oldLuxBaseline = state.forensic.luxBaseline
            val oldAcousticFloor = state.forensic.acousticFloorDb
            
            val baselineChanged = LocationSentinel.updateSensorState(state, update)
            
            val newVibeFloor = state.forensic.adaptiveVibrationFloor
            val newLuxBaseline = state.forensic.luxBaseline
            val newAcousticFloor = state.forensic.acousticFloorDb
            
            if (abs(newVibeFloor - oldVibeFloor) > 0.01) {
                emitEvent(ProcessorEvent.VibrationFloorChanged(newVibeFloor, isPrimary))
            }
            
            if (abs(newLuxBaseline - oldLuxBaseline) > 1.0) {
                emitEvent(ProcessorEvent.LuxBaselineChanged(newLuxBaseline, isPrimary))
            }

            if (abs(newAcousticFloor - oldAcousticFloor) > 1.0) {
                emitEvent(ProcessorEvent.AcousticFloorChanged(newAcousticFloor, isPrimary))
            }

            if (baselineChanged) {
                emitEvent(ProcessorEvent.ChairBaselineChanged(state.forensic.baselineSitTilt, isPrimary))
            }
            baselineChanged
        }
    }

    fun resetChairBaseline() {
        LocationSentinel.resetChairBaseline(state)
        emitEvent(ProcessorEvent.ChairBaselineChanged(state.forensic.baselineSitTilt, isPrimary))
    }

    fun shouldThrottlePolling(providedIsStationary: Boolean? = null, cpuLoad: Double = 0.0): Boolean = LocationSentinel.shouldThrottlePolling(state, providedIsStationary, cpuLoad)

    fun updateWindowedAccuracy(acc: Double) {
        if (acc <= 0.0) return
        val nowRt = timeProvider.elapsedRealtime()
        val bucketDuration = ACCURACY_WINDOW_BUCKET_MS / ACCURACY_WINDOW_MAX_SIZE
        
        if (state.accuracy.windowSize == 0 || (state.accuracy.lastUpdateRt > 0 && nowRt - state.accuracy.lastUpdateRt >= bucketDuration)) {
            addAccuracyToWindow(acc)
            state.accuracy.lastUpdateRt = nowRt
        } else {
            if (state.accuracy.lastUpdateRt == 0L) state.accuracy.lastUpdateRt = nowRt
            val lastIdx = (state.accuracy.windowHead - 1 + ACCURACY_WINDOW_MAX_SIZE) % ACCURACY_WINDOW_MAX_SIZE
            val currentMaxInBucket = state.accuracy.windowBuffer[lastIdx]
            if (acc > currentMaxInBucket * GEOFENCE_ACCURACY_HYSTERESIS_MULT) {
                updateLastAccuracyInWindow(acc)
            }
        }
        
        val rawMax = getMaxAccuracyFromWindow()
        val newMax = (rawMax * 10.0).roundToLong() / 10.0
        
        if (abs(newMax - state.accuracy.maxAccuracy) > 0.05) {
            state.accuracy.maxAccuracy = newMax
            emitEvent(ProcessorEvent.MaxAccuracyChanged(state.accuracy.maxAccuracy, isPrimary))
        }
    }

    fun processGpsPoint(
        update: LocationUpdate,
        isViewerTrail: Boolean,
        lastGpsTs: Long,
        isLocal: Boolean = false
    ): ProcessedLocation {
        val lat = update.kinetic.lat
        val lng = update.kinetic.lng
        val alt = update.kinetic.alt
        val androidSpeedMps = update.kinetic.speed
        val gpsTs = update.kinetic.gpsTs
        val accuracy = update.kinetic.accuracy
        val bearing = update.kinetic.bearing
        val snr = update.snrSnapshot ?: 0.0
        val nowRt = update.nowRt
        val nowWall = update.nowTs
        val cpuLoad = update.integrity.cpuLoad

        return LatencyMonitor.measureAndAudit<ProcessedLocation>(
            timeProvider,
            LATENCY_THRESHOLD_GPS_PROCESS_MS,
            "processGpsPoint",
            LatencyMonitor.AuditType.PERFORMANCE,
            { message, _ ->
                emitEvent(ProcessorEvent.LogAdded(message, "system", false, true, lat, lng, accuracy, ForensicSnapshot(snr = snr, vibe = state.forensic.currentVibrationIndex), isPrimary))
            }
        ) {
            val res = EnginePools.PROCESSED_LOCATION.acquire()
            val effectiveTs = if (gpsTs > 0) gpsTs else nowWall
            val adaptationMuzzled = isAdaptationMuzzled(nowRt)

            if (state.lastTs > 0 && effectiveTs < state.lastTs) {
                val delta = state.lastTs - effectiveTs
                if (delta > CLOCK_REGRESSION_GATE_MS) { 
                    emitEvent(ProcessorEvent.LogAdded("Merge-on-Stale: Coordinate update bypassed due to hardware clock regression (${delta}ms). Merging status-only data.", "system", false, true, 0.0, 0.0, 0.0, ForensicSnapshot(snr = snr, vibe = state.forensic.currentVibrationIndex), isPrimary))
                    if (delta > 86400000L) { state.lastTs = 0L; state.lastRt = 0L; LocationSentinel.reset(state) }
                }
                val status = update.status
                val fallbackCoordPoint = EnginePools.GEO_POINT.acquire().apply { update(if (state.lastLat != 0.0) state.lastLat else lat, if (state.lastLng != 0.0) state.lastLng else lng, alt = alt, ts = if (state.lastTs != 0L) state.lastTs else effectiveTs, rt = if (state.lastRt != 0L) state.lastRt else nowRt, accuracy = accuracy, maxAccuracy = state.accuracy.maxAccuracy) }
                return@measureAndAudit res.apply {
                    this.rawPoint = EnginePools.GEO_POINT.acquire().apply { update(lat, lng, alt, effectiveTs, nowRt, accuracy, state.accuracy.maxAccuracy) }
                    this.optimizedPoint = fallbackCoordPoint
                    this.status = status
                    this.maxAccuracy = state.accuracy.maxAccuracy
                    this.currentAccuracy = accuracy
                    this.filteredSpeed = state.forensic.estimatedSpeedMps
                    this.timestamp = effectiveTs
                    this.rt = nowRt
                    this.isStalled = update.integrity.isStalled
                    this.isClockRegression = true
                    this.receiptRt = nowRt
                    this.isTrajectoryPromoted = false
                    this.jumpTier = update.kinetic.jumpTier
                    this.isAdaptiveJump = update.kinetic.isAdaptiveJump
                    this.distToHome = state.lastNearestHomeDistance
                    this.isSpatiallyValid = true
                    this.tamperDetected = update.integrity.isTamperDetected
                    this.jammerDetected = update.integrity.isJammer
                    this.kineticEnergy = update.kinetic.kineticEnergy
                }
            }

            val TRAJECTORY_PROMOTION_WINDOW_MS = 60000L
            if (accuracy > HIGH_ACCURACY_THRESHOLD_METERS * TRAJECTORY_REJECTION_ACCURACY_MULT && state.lastHighAccRt > 0 && nowRt - state.lastHighAccRt < TRAJECTORY_PROMOTION_WINDOW_MS) {
                if (PhysicsUtils.calculateDistance(lat, lng, state.lastHighAccLat, state.lastHighAccLng) > accuracy) {
                    val fallbackCoordPoint = EnginePools.GEO_POINT.acquire().apply { update(if (state.lastLat != 0.0) state.lastLat else lat, if (state.lastLng != 0.0) state.lastLng else lng, alt = alt, ts = if (state.lastTs != 0L) state.lastTs else effectiveTs, rt = if (state.lastRt != 0L) state.lastRt else nowRt, accuracy = accuracy, maxAccuracy = state.accuracy.maxAccuracy) }
                    return@measureAndAudit res.apply {
                        this.rawPoint = EnginePools.GEO_POINT.acquire().apply { update(lat, lng, alt = alt, ts = effectiveTs, rt = nowRt, accuracy = accuracy, maxAccuracy = state.accuracy.maxAccuracy) }
                        this.optimizedPoint = fallbackCoordPoint
                        this.status = SentinelStatus.VALID
                        this.maxAccuracy = state.accuracy.maxAccuracy
                        this.currentAccuracy = accuracy
                        this.filteredSpeed = state.forensic.estimatedSpeedMps
                        this.timestamp = effectiveTs
                        this.rt = nowRt
                        this.isStalled = update.integrity.isStalled
                        this.receiptRt = nowRt
                        this.jumpTier = update.kinetic.jumpTier
                        this.isAdaptiveJump = update.kinetic.isAdaptiveJump
                        this.distToHome = state.lastNearestHomeDistance
                        this.isSpatiallyValid = false
                        this.tamperDetected = update.integrity.isTamperDetected
                        this.jammerDetected = update.integrity.isJammer
                        this.kineticEnergy = update.kinetic.kineticEnergy
                    }
                }
            }
            
            if (accuracy <= HIGH_ACCURACY_THRESHOLD_METERS) { state.lastHighAccLat = lat; state.lastHighAccLng = lng; state.lastHighAccTs = nowWall; state.lastHighAccRt = nowRt }
            if (isLocal) updateWindowedAccuracy(accuracy) else if (update.kinetic.maxAccuracy > 0.0) state.accuracy.maxAccuracy = update.kinetic.maxAccuracy
            
            if (update.acousticLockoutRt > 0 || update.lightSpikeRt > 0 || update.providedAdaptiveFloor >= 0.0) {
                val oldVibeFloor = state.forensic.adaptiveVibrationFloor
                val oldLuxBaseline = state.forensic.luxBaseline
                val oldAcousticFloor = state.forensic.acousticFloorDb
                
                LocationSentinel.updateSensorState(state, update)
                
                val newVibeFloor = state.forensic.adaptiveVibrationFloor
                val newLuxBaseline = state.forensic.luxBaseline
                val newAcousticFloor = state.forensic.acousticFloorDb

                if (abs(newVibeFloor - oldVibeFloor) > 0.01) {
                    emitEvent(ProcessorEvent.VibrationFloorChanged(newVibeFloor, isPrimary))
                }
                if (abs(newLuxBaseline - oldLuxBaseline) > 1.0) {
                    emitEvent(ProcessorEvent.LuxBaselineChanged(newLuxBaseline, isPrimary))
                }
                if (abs(newAcousticFloor - oldAcousticFloor) > 1.0) {
                    emitEvent(ProcessorEvent.AcousticFloorChanged(newAcousticFloor, isPrimary))
                }
            }

            val sentinelResult = LocationSentinel.processLocation(
                state = state,
                lat = lat, lng = lng, alt = alt, accuracy = accuracy, maxAccuracy = state.accuracy.maxAccuracy, 
                bearing = bearing, snr = snr, satsUsed = update.integrity.satsUsed, timestamp = effectiveTs,
                bypassBehavioral = !isLocal, isSuspicious = update.isMuzzled || adaptationMuzzled,
                isMuzzled = update.isMuzzled, nowTs = nowWall, nowRt = nowRt, cpuLoad = cpuLoad
            )
            
            if (sentinelResult.status == SentinelStatus.TRAJECTORY_PROMOTED) {
                val promotedPoints = sentinelResult.promotedPoints
                if (promotedPoints != null && promotedPoints.isNotEmpty() && state.lastLat != 0.0) {
                    val firstPromoted = promotedPoints.first()
                    PhysicsUtils.interpolateSegmentCallback(
                        state.lastLat, state.lastLng, state.lastTs, firstPromoted.lat, firstPromoted.lng, firstPromoted.ts,
                        startAcc = state.lastAcc, startMaxAcc = state.lastMaxAcc, endAcc = accuracy, endMaxAcc = state.accuracy.maxAccuracy
                    ) { pLat, pLng, pTs, pAcc, pMaxAcc ->
                        emitEvent(ProcessorEvent.TrailPointSaved(pLat, pLng, isViewerTrail, SentinelStatus.VALID, pTs, accuracy = pAcc, maxAccuracy = pMaxAcc, isPrimary = isPrimary))
                    }
                }
                promotedPoints?.forEach { p ->
                    emitEvent(ProcessorEvent.TrailPointSaved(p.lat, p.lng, isViewerTrail, SentinelStatus.VALID, p.ts, accuracy = p.accuracy, maxAccuracy = p.maxAccuracy, isPrimary = isPrimary))
                }
            }

            val isActualJump = (sentinelResult.status == SentinelStatus.JUMP || sentinelResult.status == SentinelStatus.OUTLIER || sentinelResult.status == SentinelStatus.JITTER || (sentinelResult.jumpConfidence?.isJump == true))
            val isMuzzledJump = adaptationMuzzled && (sentinelResult.status == SentinelStatus.JUMP || sentinelResult.status == SentinelStatus.JITTER)
            val finalStatus = if (isMuzzledJump) SentinelStatus.VALID else sentinelResult.status
            val finalSuppressionNote = if (isMuzzledJump) "Settling A15 Polling..." else sentinelResult.reason

            val isActualJammer = (sentinelResult.status == SentinelStatus.JAMMER_SUSPICION || (sentinelResult.jumpConfidence?.isOutlier == true))
            val finalIsJump = (isActualJump && !isMuzzledJump) || update.integrity.isJammer
            val finalIsTrajectoryPromoted = sentinelResult.status == SentinelStatus.TRAJECTORY_PROMOTED
            val finalJumpTier = maxOf(sentinelResult.jumpConfidence?.tier ?: 0, update.kinetic.jumpTier)
            val finalIsAdaptiveJump = (sentinelResult.jumpConfidence?.isAdaptiveJump == true) || update.kinetic.isAdaptiveJump
            val finalIsTamper = sentinelResult.status == SentinelStatus.TAMPER || update.integrity.isTamperDetected
            val finalIsJammer = finalIsJump || finalIsTamper || isActualJammer || update.integrity.isJammer
            // R-ID 544: Explicitly check manual injection flag for stalls even in local mode.
            val finalIsStalled = update.integrity.isStalled || (isLocal && (gpsTs != 0L && gpsTs == lastGpsTs))
            val isSpatiallyValid = !finalIsJump && !finalIsTamper && finalStatus != SentinelStatus.OUTLIER
            
            val fallbackPoint = EnginePools.GEO_POINT.acquire().apply { update(if (state.lastLat != 0.0) state.lastLat else lat, if (state.lastLng != 0.0) state.lastLng else lng, alt = alt, ts = if (state.lastTs != 0L) state.lastTs else effectiveTs, rt = if (state.lastRt != 0L) state.lastRt else nowRt, accuracy = state.lastAcc, maxAccuracy = state.lastMaxAcc) }

            if (!isSpatiallyValid) {
                if (shouldSavePoint(update.isMuzzled || adaptationMuzzled, true, PhysicsUtils.calculateDistance(state.lastSavedLat, state.lastSavedLng, lat, lng), 0L, state.accuracy.maxAccuracy, nowRt)) {
                    emitEvent(ProcessorEvent.TrailPointSaved(lat, lng, isViewerTrail, finalStatus, effectiveTs, accuracy = accuracy, maxAccuracy = state.accuracy.maxAccuracy, isPrimary = isPrimary))
                }
                return@measureAndAudit res.apply {
                    this.rawPoint = EnginePools.GEO_POINT.acquire().apply { update(lat, lng, alt = alt, ts = effectiveTs, rt = nowRt, accuracy = accuracy, maxAccuracy = state.accuracy.maxAccuracy) }
                    this.optimizedPoint = fallbackPoint
                    this.status = finalStatus
                    this.maxAccuracy = state.accuracy.maxAccuracy
                    this.currentAccuracy = accuracy
                    this.filteredSpeed = state.forensic.estimatedSpeedMps
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
                    this.kineticEnergy = if (isLocal) state.kineticEnergy else update.kinetic.kineticEnergy
                }
            }

            val optimizedPoint = sentinelResult.optimizedPoint ?: EnginePools.GEO_POINT.acquire().apply { update(lat, lng, alt = alt, ts = effectiveTs, rt = nowRt, accuracy = accuracy, maxAccuracy = state.accuracy.maxAccuracy) }
            val persistencePoint = if (isLocal) optimizedPoint else EnginePools.GEO_POINT.acquire().apply { update(lat, lng, alt = alt, ts = effectiveTs, rt = nowRt, accuracy = accuracy, maxAccuracy = state.accuracy.maxAccuracy) }
            
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
                        val speedMps = state.forensic.estimatedSpeedMps
                        val predictiveMargin = speedMps * GEOFENCE_PREDICTIVE_LOOKAHEAD_S
                        val threshold = state.maxDistanceAuthority + (state.accuracy.maxAccuracy * GEOFENCE_BUFFER_MULT * GEOFENCE_ACCURACY_EXPANSION_MULT)
                        if (speedMps > GEOFENCE_PREDICTIVE_MIN_SPEED_MPS && minD > (threshold - predictiveMargin)) geofenceViolation = true
                    }
                }
            }

            state.lastLat = lat; state.lastLng = lng; state.lastTs = effectiveTs; state.lastRt = nowRt; state.lastAcc = accuracy; state.lastMaxAcc = state.accuracy.maxAccuracy
            if (!finalIsStalled) state.lastValidFixRt = nowRt else if (isLocal && !isViewerTrail) emitEvent(ProcessorEvent.GpsStallDetected(nowRt, isPrimary))
            
            val isThrottled = LocationSentinel.shouldThrottlePolling(state, cpuLoad = cpuLoad)
            val estimatedSpeed = state.forensic.estimatedSpeedMps
            val stationaryProb = state.forensic.stationaryProb
            
            val anchorResult = AnchorEvaluator.evaluate(
                state = state,
                point = persistencePoint,
                isPhysicallyStationary = LocationSentinel.isStationary(state, cpuLoad),
                stationaryProb = stationaryProb,
                estimatedSpeed = estimatedSpeed,
                maxAccuracy = state.accuracy.maxAccuracy,
                isSuspicious = update.isMuzzled || adaptationMuzzled,
                isAdaptationMuzzled = adaptationMuzzled,
                isAccuracySnap = sentinelResult.jumpConfidence?.reason?.contains("Suppressed Accuracy Snap") == true,
                snr = snr,
                vibeIndex = state.forensic.currentVibrationIndex,
                onLog = { msg, lLat, lLng, lAcc, lVibe ->
                    emitEvent(ProcessorEvent.LogAdded(msg, "system", false, false, lLat, lLng, lAcc, ForensicSnapshot(vibe = lVibe), isPrimary))
                }
            )

            val skipPersistence = anchorResult.shouldSkipPersistence
            val isAnchorLockedNow = anchorResult.isLocked

            val timeSinceLastGpsSaveRt = if (nowRt > 0 && state.lastSavedRt > 0) nowRt - state.lastSavedRt else 0L
            if (shouldSavePoint(update.isMuzzled || adaptationMuzzled, isThrottled, PhysicsUtils.calculateDistance(state.lastSavedLat, state.lastSavedLng, persistencePoint.lat, persistencePoint.lng), timeSinceLastGpsSaveRt, state.accuracy.maxAccuracy, nowRt) && !skipPersistence) {
                emitEvent(ProcessorEvent.TrailPointSaved(persistencePoint.lat, persistencePoint.lng, isViewerTrail, finalStatus, effectiveTs, accuracy = persistencePoint.accuracy, maxAccuracy = persistencePoint.maxAccuracy, isPrimary = isPrimary))
                state.lastSavedLat = persistencePoint.lat; state.lastSavedLng = persistencePoint.lng; state.lastSavedTs = nowWall; state.lastSavedRt = nowRt; state.lastSavedGpsTs = gpsTs
            }
            
            state.accuracy.lastProcessedAccuracy = accuracy
            
            val finalOptimized = if (isAnchorLockedNow && !isViewerTrail) {
                anchorResult.optimizedPoint
            } else {
                optimizedPoint
            }

            return@measureAndAudit res.apply {
                this.rawPoint = EnginePools.GEO_POINT.acquire().apply { update(lat, lng, alt = alt, ts = effectiveTs, rt = nowRt, accuracy = accuracy, maxAccuracy = state.accuracy.maxAccuracy) }
                this.optimizedPoint = finalOptimized
                this.status = finalStatus
                this.maxAccuracy = state.accuracy.maxAccuracy
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
                this.kineticEnergy = if (isLocal) state.kineticEnergy else update.kinetic.kineticEnergy
            }
        }
    }

    private fun emitEvent(event: ProcessorEvent) {
        domainEventBus?.emit(event)
    }

    private fun isStationary(cpuLoad: Double = 0.0): Boolean = LocationSentinel.isStationary(state, cpuLoad)

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
    
    fun getEstimatedBearing(): Double = state.forensic.estimatedBearing
    fun resetFilter() { 
        LocationSentinel.reset(state)
        AnchorEvaluator.reset(state)
    }
    fun invalidateHomePointsCache() { state.cachedHomePoints = null }
    fun resetStats() {
        state.accuracy.lastProcessedAccuracy = 0.0; state.accuracy.maxAccuracy = 0.0
        resetAccuracyWindow()
        state.lastDistanceToTracker = null; state.lastNearestHomeDistance = null
        state.lastLat = 0.0; state.lastLng = 0.0; state.lastTs = 0L; state.lastRt = 0L; state.lastAcc = 0.0; state.lastMaxAcc = 0.0
        state.lastSavedLat = 0.0; state.lastSavedLng = 0.0; state.lastSavedTs = 0L; state.lastSavedRt = 0L; state.lastSavedGpsTs = 0L
        state.lastHighAccLat = 0.0; state.lastHighAccLng = 0.0; state.lastHighAccTs = 0L; state.lastHighAccRt = 0L; state.lastValidFixRt = 0L
        AnchorEvaluator.reset(state)
        invalidateHomePointsCache()
        LocationSentinel.reset(state)
        emitEvent(ProcessorEvent.MaxAccuracyChanged(0.0, isPrimary))
        state.lastExpectedIntervalMs = 0L
        state.lastIntervalChangeRt = 0L
    }
}
