package com.gps19.core.engine

import com.gps19.core.engine.PhysicsUtils.safeDouble
import kotlin.math.*

/**
 * LocationSentinel: A multi-layered location validation engine.
 * Sep.24.95:
 * - Issue #1163: Transitioned to a completely stateless model. All operational 
 *   state metrics are read from and written to LocationProcessingState.
 */
object LocationSentinel {

    private val resultFlyweight = SentinelResult().apply { 
        jumpConfidence = JumpConfidence() 
    }

    fun loadForensicState(
        state: LocationProcessingState,
        savedLastSitTs: Long, 
        savedBaseline: Double,
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
        state.lastSitTs = savedLastSitTs
        state.baselineSitTilt = savedBaseline
        state.lastSitVz = savedSitVz
        state.lastSitDz = savedSitDz
        state.lastSitBaro = savedSitBaro
        state.lastSitTilt = savedSitTilt
        state.lastSitShock = savedSitShock
        state.lastSitVzTs = savedSitVzTs
        state.lastSitVzRt = savedSitVzRt
        if (savedVibrationFloor >= 0.0) {
            state.adaptiveVibrationFloor = savedVibrationFloor
        }
        if (savedLuxBaseline >= 0.0) {
            state.luxBaseline = savedLuxBaseline
        }
        if (savedAcousticFloor >= 0.0) {
            state.acousticFloorDb = savedAcousticFloor
        }
    }

    fun setSpatialAnchor(state: LocationProcessingState, lat: Double, lng: Double, alt: Double, timestamp: Long, rt: Long, accuracy: Double = 0.0) {
        state.sentinelLastValidLat = lat
        state.sentinelLastValidLng = lng
        state.sentinelLastValidAlt = alt
        state.sentinelLastValidTs = timestamp
        state.sentinelLastValidRt = rt
        state.sentinelLastValidAccuracy = accuracy
        updateFilters(state, lat, lng, timestamp, 1.0)
    }

    private fun updateFilters(state: LocationProcessingState, lat: Double, lng: Double, ts: Long, qScale: Double) {
        if (state.sentinelLastValidTs > 0) {
            val d = PhysicsUtils.calculateDistance(state.sentinelLastValidLat, state.sentinelLastValidLng, lat, lng)
            val dt = max(0.1, (ts - state.sentinelLastValidTs) / 1000.0)
            val speed = d / dt
            state.estimatedSpeedMps = PhysicsUtils.smoothCoordinate(state.estimatedSpeedMps, speed, SPEED_EMA_ALPHA)
            
            val bearing = PhysicsUtils.calculateBearing(state.sentinelLastValidLat, state.sentinelLastValidLng, lat, lng)
            state.estimatedBearing = PhysicsUtils.smoothBearing(state.estimatedBearing, bearing, BEARING_EMA_ALPHA)
            
            val prob = if (state.estimatedSpeedMps < STATIONARY_SPEED_THRESHOLD_MPS) 1.0 else 0.0
            
            val isLowSnr = state.lastSnr > 0 && state.lastSnr < JUMP_GATE_LOW_SNR_THRESHOLD
            val alpha = if (isStationary(state) && isLowSnr && prob < state.stationaryProb) {
                POSITION_EMA_ALPHA_STATIONARY * 0.2
            } else {
                POSITION_EMA_ALPHA_STATIONARY
            }
            
            state.stationaryProb = PhysicsUtils.smoothCoordinate(state.stationaryProb, prob, alpha)
        }
    }

    fun updateSensorState(state: LocationProcessingState, snapshot: SensorStateSnapshot): Boolean {
        var baselineChanged = false
        
        state.lastCompassHeading = state.currentCompassHeading
        if (snapshot.vibration >= 0.0) state.currentVibrationIndex = safeDouble(snapshot.vibration)
        if (snapshot.acousticLockoutRt > 0) state.lastFastPathAcousticSpikeRt = snapshot.acousticLockoutRt
        if (snapshot.lightSpikeRt > 0) state.lastFastPathLightSpikeRt = snapshot.lightSpikeRt
        state.kineticEnergy = safeDouble(snapshot.kineticEnergy)
        
        if (snapshot.peakShock > state.peakVibrationShock && !snapshot.peakShock.isNaN()) {
            state.peakVibrationShock = snapshot.peakShock
            state.peakVibrationShockRt = snapshot.nowRt
        }

        val currentTilt = safeDouble(snapshot.tiltDegrees)
        val tiltDelta = if (state.baselineSitTilt >= 0.0) abs(currentTilt - state.baselineSitTilt) else 0.0
        val baroDelta = if (state.baroBaseline > -999.0) abs(safeDouble(snapshot.baroAlt) - state.baroBaseline) else 0.0
        
        if (snapshot.nowRt > state.sitDetectionCooldownRt && !snapshot.isMuzzled && !snapshot.isWarming) {
            val isSpatialTriggered = (tiltDelta > TILT_THRESHOLD_DEGREES) || 
                                     (baroDelta > BARO_LIFT_THRESHOLD_METERS) || 
                                     snapshot.plungeMatched
            
            if (isSpatialTriggered) {
                val hasSufficientForce = (snapshot.peakShock > VIBRATION_SHOCK_THRESHOLD_G) || snapshot.plungeMatched || (abs(snapshot.peakVerticalVelocity) > CHAIR_PLUNGE_VELOCITY_THRESHOLD)
                
                if (hasSufficientForce) {
                    state.isSitDetected = true
                    state.lastSitTs = snapshot.nowTs
                    state.lastSitRt = snapshot.nowRt
                    state.sitDetectionCooldownRt = snapshot.nowRt + SIT_DUPLICATE_GUARD_MS
                    
                    state.lastSitVz = safeDouble(snapshot.peakVerticalVelocity)
                    state.lastSitVzTs = if (snapshot.peakVerticalVelocityTs > 0) snapshot.peakVerticalVelocityTs else snapshot.nowTs
                    state.lastSitVzRt = if (snapshot.peakVerticalVelocityRt > 0) snapshot.peakVerticalVelocityRt else snapshot.nowRt
                    state.lastSitDz = safeDouble(snapshot.peakVerticalDisplacement)
                    state.lastSitBaro = safeDouble(baroDelta)
                    state.lastSitTilt = safeDouble(tiltDelta)
                    state.lastSitShock = safeDouble(snapshot.peakShock)
                }
            }
        }

        if (isStationary(state) && !state.isSitDetected) {
            if (state.stationaryStartRt == 0L) state.stationaryStartRt = snapshot.nowRt
            else if (snapshot.nowRt - state.stationaryStartRt > PASSIVE_ZEROING_STATIONARY_MS) {
                if (abs(state.baselineSitTilt - currentTilt) > 0.1 && !currentTilt.isNaN()) {
                    state.baselineSitTilt = currentTilt
                    baselineChanged = true
                }
                state.stationaryStartRt = 0L
            }
        } else {
            state.stationaryStartRt = 0L
        }

        if (snapshot.heading >= 0.0) state.currentCompassHeading = safeDouble(snapshot.heading)
        if (snapshot.baroAlt > -999.0) state.currentBaroAlt = safeDouble(snapshot.baroAlt)
        if (snapshot.lux >= 0.0) state.currentLux = safeDouble(snapshot.lux)
        state.isNear = snapshot.isNear
        state.isPowerTamper = snapshot.powerTamper
        state.currentTiltDegrees = currentTilt
        if (snapshot.acousticDb >= 0.0) state.currentAcousticDb = safeDouble(snapshot.acousticDb)

        state.luxBaseline = SentinelValidator.updateLuxBaseline(state.luxBaseline, snapshot.lux, isStationary(state), snapshot.isWarming)
        state.baroBaseline = SentinelValidator.updateBaroBaseline(state.baroBaseline, snapshot.baroAlt, snapshot.isWarming)

        if (!snapshot.isSirenActive) {
            val updateDb = if (snapshot.acousticMinDb >= 0.0) snapshot.acousticMinDb else if (snapshot.acousticMinDb == -1.0 && snapshot.acousticDb >= 0.0) snapshot.acousticDb else -1.0
            state.acousticFloorDb = SentinelValidator.updateAcousticFloor(state.acousticFloorDb, updateDb, snapshot.isWarming)
            
            val contractionElapsedRt = snapshot.nowRt - state.lastAcousticContractionRt
            if (contractionElapsedRt >= 500 || state.lastAcousticContractionRt == 0L) {
                if (state.acousticFloorDb > ACOUSTIC_FLOOR_MIN_DB && state.lastAcousticContractionRt > 0) {
                    val secondsPassed = contractionElapsedRt / 1000.0
                    if (secondsPassed > 0) {
                        val decayFactor = Math.pow(ACOUSTIC_FLOOR_CONTRACTION_EMA, secondsPassed)
                        state.acousticFloorDb = max(state.acousticFloorDb * decayFactor, ACOUSTIC_FLOOR_MIN_DB)
                    }
                }
                state.lastAcousticContractionRt = snapshot.nowRt
            }
        }
        
        if (snapshot.manualAdaptiveFloor >= 0.0) {
            state.adaptiveVibrationFloor = snapshot.manualAdaptiveFloor
        } else if (snapshot.providedAdaptiveFloor >= 0.0) {
            state.adaptiveVibrationFloor = snapshot.providedAdaptiveFloor
        } else if (snapshot.vibration >= 0.0) { 
            state.adaptiveVibrationFloor = SentinelValidator.updateVibrationFloor(state.adaptiveVibrationFloor, state.currentVibrationIndex, snapshot.isWarming)
        }
        
        return baselineChanged
    }

    fun consumeSitDetected(state: LocationProcessingState): Boolean {
        val result = state.isSitDetected
        state.isSitDetected = false
        return result
    }

    fun resetChairBaseline(state: LocationProcessingState) {
        state.baselineSitTilt = -1.0
    }

    fun getHindsightBuffer(state: LocationProcessingState): List<RejectedPoint> = GtoEngine.getWindow(state).map {
        RejectedPoint(it.lat, it.lng, it.alt, it.accuracy, it.bearing, it.speedMps, it.ts, it.rt)
    }

    fun processLocation(
        state: LocationProcessingState,
        lat: Double, lng: Double, alt: Double, accuracy: Double, 
        maxAccuracy: Double, 
        bearing: Double,
        snr: Double, satsUsed: Int, timestamp: Long, 
        bypassBehavioral: Boolean = false,
        isSuspicious: Boolean = false,
        isMuzzled: Boolean = false, 
        nowTs: Long,
        nowRt: Long,
        acousticFloorDb: Double = -1.0
    ): SentinelResult {
        state.lastSnr = snr
        state.lastSatsUsed = satsUsed

        if (acousticFloorDb >= 0.0) {
            state.acousticFloorDb = max(acousticFloorDb, ACOUSTIC_FLOOR_MIN_DB)
        }
        
        if (state.sentinelLastValidTs == 0L) {
            updateLastValid(state, lat, lng, alt, timestamp, nowRt, 0.0, bearing, accuracy)
            updateFilters(state, lat, lng, timestamp, 1.0)
            resultFlyweight.reset(SentinelStatus.VALID)
            resultFlyweight.optimizedPoint = EngineGeoPoint(lat, lng, alt, timestamp, nowRt, accuracy, maxAccuracy)
            return resultFlyweight
        }

        val timeDeltaMs = timestamp - state.sentinelLastValidTs
        if (timeDeltaMs <= 0 && timestamp != 0L) {
            resultFlyweight.reset(SentinelStatus.VALID)
            return resultFlyweight
        }
        
        val altitudeDelta = if (state.sentinelLastValidAlt != 0.0) alt - state.sentinelLastValidAlt else 0.0
        val isParking = isStationary(state)
        
        val dist = PhysicsUtils.calculateDistance(state.sentinelLastValidLat, state.sentinelLastValidLng, lat, lng)
        val impliesMotion = dist > ACTIVE_MOVE_THRESHOLD
        
        if (impliesMotion) {
            if (state.gpsMotionStartRt == 0L) state.gpsMotionStartRt = nowRt
        } else {
            state.gpsMotionStartRt = 0L
        }
        
        val isTractorSlowOverride = state.gpsMotionStartRt > 0 && (nowRt - state.gpsMotionStartRt > 10000L)
        val hasPhysicalMotion = if (isMuzzled) false else (state.currentVibrationIndex > (state.adaptiveVibrationFloor * 1.5) || isTractorSlowOverride)

        resultFlyweight.reset()
        val conf = resultFlyweight.jumpConfidence!!
        PhysicsUtils.isVisualJump(
            lastLat = state.sentinelLastValidLat, lastLng = state.sentinelLastValidLng,
            newLat = lat, newLng = lng,
            timeDeltaMs = if (state.sentinelLastValidRt > 0) (nowRt - state.sentinelLastValidRt) else timeDeltaMs, 
            accuracy = accuracy,
            lastAccuracy = state.sentinelLastValidAccuracy,
            snr = snr,
            lastSpeedMps = state.sentinelLastValidSpeedMps,
            isParking = isParking,
            altitudeDelta = altitudeDelta,
            hasPhysicalMotion = hasPhysicalMotion,
            result = conf
        )
        
        var score = conf.score
        val augmentedScore = score.coerceIn(0, 100)
        val timeDeltaSec = (if (state.sentinelLastValidRt > 0) (nowRt - state.sentinelLastValidRt) else timeDeltaMs) / 1000.0
        val currentSpeedMps = dist / max(0.1, timeDeltaSec)
        
        conf.score = augmentedScore
        conf.isJump = augmentedScore >= 50 || conf.isJump

        if (conf.isOutlier) {
            resultFlyweight.status = SentinelStatus.JUMP
            resultFlyweight.reason = conf.reason
            return resultFlyweight
        }
        
        var behavioralStatus = if (conf.isJump) SentinelStatus.JUMP else SentinelStatus.VALID
        var behavioralReason = conf.reason

        if (!bypassBehavioral) {
            if (GtoEngine.evaluateTrajectory(state, lat, lng, bearing, currentSpeedMps, timestamp, nowRt)) {
                val promoted = mutableListOf<EngineGeoPoint>()
                GtoEngine.getWindow(state).forEach { p ->
                    updateFilters(state, p.lat, p.lng, p.ts, SUSPICIOUS_Q_SCALE)
                    promoted.add(EngineGeoPoint(p.lat, p.lng, p.alt, p.ts, p.rt, p.accuracy, p.maxAccuracy))
                    updateLastValid(state, p.lat, p.lng, p.alt, p.ts, p.rt, p.speedMps, p.bearing, p.accuracy)
                }
                GtoEngine.clear(state)
                updateFilters(state, lat, lng, timestamp, SUSPICIOUS_Q_SCALE)
                updateLastValid(state, lat, lng, alt, timestamp, nowRt, currentSpeedMps, bearing, accuracy)
                
                resultFlyweight.status = SentinelStatus.TRAJECTORY_PROMOTED
                resultFlyweight.reason = "Trajectory Promoted (GTO)"
                resultFlyweight.optimizedPoint = EngineGeoPoint(lat, lng, alt, timestamp, nowRt, accuracy, maxAccuracy)
                resultFlyweight.promotedPoints = promoted
                return resultFlyweight
            }

            if (behavioralStatus == SentinelStatus.JUMP) {
                GtoEngine.addPoint(state, lat, lng, alt, accuracy, maxAccuracy, bearing, currentSpeedMps, timestamp, nowRt, state.currentVibrationIndex)
                resultFlyweight.status = behavioralStatus
                resultFlyweight.reason = behavioralReason
                return resultFlyweight
            }

            resultFlyweight.status = checkPhysicalTamper(state, nowRt, isMuzzled)
            if (resultFlyweight.status != SentinelStatus.VALID) {
                return resultFlyweight
            }
            
            if (resultFlyweight.status == SentinelStatus.VALID && resultFlyweight.suppressionNote != null) {
                updateFilters(state, lat, lng, timestamp, if (isSuspicious) SUSPICIOUS_Q_SCALE else 1.0)
                updateLastValid(state, lat, lng, alt, timestamp, nowRt, currentSpeedMps, bearing, accuracy)
                GtoEngine.clear(state)
                resultFlyweight.optimizedPoint = EngineGeoPoint(lat, lng, alt, timestamp, nowRt, accuracy, maxAccuracy)
                return resultFlyweight
            }
        }

        updateFilters(state, lat, lng, timestamp, if (isSuspicious) SUSPICIOUS_Q_SCALE else 1.0)
        updateLastValid(state, lat, lng, alt, timestamp, nowRt, currentSpeedMps, bearing, accuracy)
        GtoEngine.clear(state)
        resultFlyweight.status = behavioralStatus
        resultFlyweight.reason = behavioralReason
        resultFlyweight.optimizedPoint = EngineGeoPoint(lat, lng, alt, timestamp, nowRt, accuracy, maxAccuracy)
        return resultFlyweight
    }

    fun checkPhysicalTamper(
        state: LocationProcessingState,
        nowRt: Long = 0L,
        isMuzzled: Boolean = false
    ): SentinelStatus {
        if (isMuzzled) return SentinelStatus.VALID

        if (!state.isNear) {
            resultFlyweight.reason = "Proximity Far"
            return SentinelStatus.TAMPER
        }
        if (state.isPowerTamper) {
            resultFlyweight.reason = "Power disconnected"
            return SentinelStatus.TAMPER
        }
        if (SentinelValidator.isTiltViolated(state.currentTiltDegrees)) {
            resultFlyweight.reason = "Tilt detected"
            return SentinelStatus.TAMPER
        }
        if (SentinelValidator.isShockViolated(state.peakVibrationShock, state.adaptiveVibrationFloor)) {
            resultFlyweight.reason = "Shock detected"
            return SentinelStatus.TAMPER
        }
        
        if (state.baroBaseline > -999.0) {
            val liftDelta = state.currentBaroAlt - state.baroBaseline
            if (SentinelValidator.isLiftViolated(liftDelta)) {
                if (state.currentVibrationIndex > VIBRATION_STATIONARY_THRESHOLD) {
                    resultFlyweight.reason = "Lift detected"
                    return SentinelStatus.TAMPER
                } else {
                    resultFlyweight.reason = "Barometric drift suspicion (No vibration)"
                    return SentinelStatus.TAMPER
                }
            }
        }
        
        if (SentinelValidator.isLightViolated(state.currentLux, state.luxBaseline)) {
            resultFlyweight.reason = "Light jump"
            return SentinelStatus.TAMPER
        }

        val isLightSpikeRecently = (state.lastFastPathLightSpikeRt > 0 && (nowRt - state.lastFastPathLightSpikeRt < LIGHT_LOCKOUT_MS))
        if (isLightSpikeRecently) {
            resultFlyweight.reason = "Light jump (FastPath)"
            return SentinelStatus.TAMPER
        }

        val isAcousticLockedOut = (state.lastFastPathAcousticSpikeRt > 0 && (nowRt - state.lastFastPathAcousticSpikeRt < LIGHT_LOCKOUT_MS))
        
        if (!isAcousticLockedOut && SentinelValidator.isAcousticViolated(state.currentAcousticDb, state.acousticFloorDb)) {
            resultFlyweight.reason = "Acoustic alarm"
            return SentinelStatus.TAMPER
        }

        if (SentinelValidator.isVibrationSuspicious(state.currentVibrationIndex, state.adaptiveVibrationFloor)) {
            resultFlyweight.reason = "Vibration suspicion"
            return SentinelStatus.TAMPER
        }
        
        if (!isAcousticLockedOut && SentinelValidator.isAcousticSuspicious(state.currentAcousticDb, state.acousticFloorDb, state.currentVibrationIndex)) {
            resultFlyweight.reason = "Acoustic suspicion"
            return SentinelStatus.TAMPER
        }

        return SentinelStatus.VALID
    }

    fun isStationary(state: LocationProcessingState): Boolean = SentinelValidator.isStationary(state.currentVibrationIndex, state.adaptiveVibrationFloor)

    fun shouldThrottlePolling(state: LocationProcessingState, providedIsStationary: Boolean? = null): Boolean {
        val stationary = providedIsStationary ?: isStationary(state)
        return stationary &&
               abs(state.currentCompassHeading - state.lastCompassHeading) < THROTTLE_COMPASS_LIMIT &&
               (if (state.baroBaseline > -999.0) abs(state.currentBaroAlt - state.baroBaseline) < THROTTLE_BARO_LIMIT else true) &&
               state.isNear && (state.currentLux - state.luxBaseline < THROTTLE_LUX_LIMIT) && !state.isPowerTamper &&
               state.currentTiltDegrees < THROTTLE_TILT_LIMIT && (state.currentAcousticDb - state.acousticFloorDb < THROTTLE_ACOUSTIC_LIMIT)
    }

    private fun updateLastValid(state: LocationProcessingState, lat: Double, lng: Double, alt: Double, ts: Long, rt: Long, speedMps: Double, bearing: Double, accuracy: Double) {
        state.sentinelLastValidLat = lat
        state.sentinelLastValidLng = lng
        state.sentinelLastValidAlt = alt
        state.sentinelLastValidTs = ts
        state.sentinelLastValidRt = rt
        state.sentinelLastValidSpeedMps = speedMps
        state.sentinelLastValidBearing = bearing
        state.sentinelLastValidAccuracy = accuracy
    }

    fun reset(state: LocationProcessingState) {
        state.sentinelLastValidTs = 0L
        state.sentinelLastValidRt = 0L
        state.currentVibrationIndex = 0.0
        state.currentBaroAlt = 0.0
        state.currentLux = 0.0
        state.isNear = true
        state.isPowerTamper = false
        state.currentTiltDegrees = 0.0
        state.currentAcousticDb = 0.0
        state.luxBaseline = -1.0
        state.baroBaseline = -1000.0
        state.acousticFloorDb = -1.0
        state.adaptiveVibrationFloor = INITIAL_VIBRATION_FLOOR
        state.peakVibrationShock = 0.0
        state.peakVibrationShockRt = 0L
        state.lastAcousticContractionRt = 0L
        state.isSitDetected = false
        state.lastSitTs = 0L
        state.lastSitRt = 0L
        state.baselineSitTilt = -1.0
        state.sitDetectionCooldownRt = 0L
        state.stationaryStartRt = 0L
        state.lastSitVz = 0.0
        state.lastSitVzTs = 0L
        state.lastSitVzRt = 0L
        state.lastSitDz = 0.0
        state.lastSitBaro = 0.0
        state.lastSitTilt = 0.0
        state.lastSitShock = 0.0
        state.gpsMotionStartRt = 0L
        state.lastFastPathAcousticSpikeRt = 0L
        state.lastFastPathLightSpikeRt = 0L
        state.estimatedSpeedMps = 0.0
        state.estimatedBearing = 0.0
        state.stationaryProb = 1.0
        state.sentinelLastValidAccuracy = 0.0
        state.kineticEnergy = 0.0
        GtoEngine.clear(state)
        resultFlyweight.reset()
    }
}
