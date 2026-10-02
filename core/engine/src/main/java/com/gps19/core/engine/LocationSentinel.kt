package com.gps19.core.engine

import com.gps19.core.engine.PhysicsUtils.safeDouble
import kotlin.math.*

/**
 * LocationSentinel: A multi-layered location validation engine.
 * Oct.2.8:
 * - Issue #1330: Snap-to-Update Monolith. Migrated from SystemEvaluationSnapshot 
 *   to unified LocationUpdate DTO (R-ID 596).
 * Oct.2.5:
 * - Issue #SIMP-1416-1: Native Sensor Pulse Hardening. Remediated build 
 *   failure by correctly passing cpuLoad to shouldThrottlePolling.
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
        state.forensic.lastSitTs = savedLastSitTs
        state.forensic.baselineSitTilt = savedBaseline
        state.forensic.lastSitVz = savedSitVz
        state.forensic.lastSitDz = savedSitDz
        state.forensic.lastSitBaro = savedSitBaro
        state.forensic.lastSitTilt = savedSitTilt
        state.forensic.lastSitShock = savedSitShock
        state.forensic.lastSitVzTs = savedSitVzTs
        state.forensic.lastSitVzRt = savedSitVzRt
        if (savedVibrationFloor >= 0.0) {
            state.forensic.adaptiveVibrationFloor = savedVibrationFloor
        }
        if (savedLuxBaseline >= 0.0) {
            state.forensic.luxBaseline = savedLuxBaseline
        }
        if (savedAcousticFloor >= 0.0) {
            state.forensic.acousticFloorDb = savedAcousticFloor
        }
    }

    fun setSpatialAnchor(state: LocationProcessingState, lat: Double, lng: Double, alt: Double, timestamp: Long, rt: Long, accuracy: Double = 0.0) {
        state.forensic.lastValidLat = lat
        state.forensic.lastValidLng = lng
        state.forensic.lastValidAlt = alt
        state.forensic.lastValidTs = timestamp
        state.forensic.lastValidRt = rt
        state.forensic.lastValidAccuracy = accuracy
        updateFilters(state, lat, lng, timestamp, 1.0)
    }

    private fun updateFilters(state: LocationProcessingState, lat: Double, lng: Double, ts: Long, qScale: Double) {
        if (state.forensic.lastValidTs > 0) {
            val d = PhysicsUtils.calculateDistance(state.forensic.lastValidLat, state.forensic.lastValidLng, lat, lng)
            val dt = max(0.1, (ts - state.forensic.lastValidTs) / 1000.0)
            val speed = d / dt
            state.forensic.estimatedSpeedMps = PhysicsUtils.smoothCoordinate(state.forensic.estimatedSpeedMps, speed, SPEED_EMA_ALPHA)
            
            val bearing = PhysicsUtils.calculateBearing(state.forensic.lastValidLat, state.forensic.lastValidLng, lat, lng)
            state.forensic.estimatedBearing = PhysicsUtils.smoothBearing(state.forensic.estimatedBearing, bearing, BEARING_EMA_ALPHA)
            
            val prob = if (state.forensic.estimatedSpeedMps < STATIONARY_SPEED_THRESHOLD_MPS) 1.0 else 0.0
            
            val isLowSnr = state.forensic.lastSnr > 0 && state.forensic.lastSnr < JUMP_GATE_LOW_SNR_THRESHOLD
            val alpha = if (isStationary(state) && isLowSnr && prob < state.forensic.stationaryProb) {
                POSITION_EMA_ALPHA_STATIONARY * 0.2
            } else {
                POSITION_EMA_ALPHA_STATIONARY
            }
            
            state.forensic.stationaryProb = PhysicsUtils.smoothCoordinate(state.forensic.stationaryProb, prob, alpha)
        }
    }

    fun updateSensorState(state: LocationProcessingState, update: LocationUpdate): Boolean {
        var baselineChanged = false
        
        state.forensic.lastCompassHeading = state.forensic.currentCompassHeading
        if (update.atmospheric.vibration >= 0.0) state.forensic.currentVibrationIndex = safeDouble(update.atmospheric.vibration)
        if (update.acousticLockoutRt > 0) state.forensic.lastFastPathAcousticSpikeRt = update.acousticLockoutRt
        if (update.lightSpikeRt > 0) state.forensic.lastFastPathLightSpikeRt = update.lightSpikeRt
        state.kineticEnergy = safeDouble(update.kinetic.kineticEnergy)
        
        if (update.atmospheric.peakVibrationShock > state.forensic.peakVibrationShock && !update.atmospheric.peakVibrationShock.isNaN()) {
            state.forensic.peakVibrationShock = update.atmospheric.peakVibrationShock
            state.forensic.peakVibrationShockRt = update.nowRt
        }

        val currentTilt = safeDouble(update.atmospheric.tiltDegrees)
        val tiltDelta = if (state.forensic.baselineSitTilt >= 0.0) abs(currentTilt - state.forensic.baselineSitTilt) else 0.0
        val baroDelta = if (state.forensic.baroBaseline > -999.0) abs(safeDouble(update.atmospheric.baroAlt) - state.forensic.baroBaseline) else 0.0
        
        if (update.nowRt > state.forensic.sitDetectionCooldownRt && !update.isMuzzled && !update.isWarming) {
            val isSpatialTriggered = (tiltDelta > TILT_THRESHOLD_DEGREES) || 
                                     (baroDelta > BARO_LIFT_THRESHOLD_METERS) || 
                                     (update.integrity.sitDz > BARO_LIFT_THRESHOLD_METERS)
            
            if (isSpatialTriggered) {
                val hasSufficientForce = (update.atmospheric.peakVibrationShock > VIBRATION_SHOCK_THRESHOLD_G) || (abs(update.kinetic.verticalVelocity) > CHAIR_PLUNGE_VELOCITY_THRESHOLD)
                
                if (hasSufficientForce) {
                    state.forensic.isSitDetected = true
                    state.forensic.lastSitTs = update.nowTs
                    state.forensic.lastSitRt = update.nowRt
                    state.forensic.sitDetectionCooldownRt = update.nowRt + SIT_DUPLICATE_GUARD_MS
                    
                    state.forensic.lastSitVz = safeDouble(update.kinetic.verticalVelocity)
                    state.forensic.lastSitVzTs = if (update.integrity.sitVzTs > 0) update.integrity.sitVzTs else update.nowTs
                    state.forensic.lastSitVzRt = if (update.integrity.sitVzRt > 0) update.integrity.sitVzRt else update.nowRt
                    state.forensic.lastSitDz = safeDouble(update.integrity.sitDz)
                    state.forensic.lastSitBaro = safeDouble(baroDelta)
                    state.forensic.lastSitTilt = safeDouble(tiltDelta)
                    state.forensic.lastSitShock = safeDouble(update.atmospheric.peakVibrationShock)
                }
            }
        }

        if (isStationary(state, update.cpuLoad) && !state.forensic.isSitDetected) {
            if (state.forensic.stationaryStartRt == 0L) state.forensic.stationaryStartRt = update.nowRt
            else if (update.nowRt - state.forensic.stationaryStartRt > PASSIVE_ZEROING_STATIONARY_MS) {
                if (abs(state.forensic.baselineSitTilt - currentTilt) > 0.1 && !currentTilt.isNaN()) {
                    state.forensic.baselineSitTilt = currentTilt
                    baselineChanged = true
                }
                state.forensic.stationaryStartRt = 0L
            }
        } else {
            state.forensic.stationaryStartRt = 0L
        }

        if (update.atmospheric.heading >= 0.0) state.forensic.currentCompassHeading = safeDouble(update.atmospheric.heading)
        if (update.atmospheric.baroAlt > -999.0) state.forensic.currentBaroAlt = safeDouble(update.atmospheric.baroAlt)
        if (update.atmospheric.lux >= 0.0) state.forensic.currentLux = safeDouble(update.atmospheric.lux)
        state.forensic.isNear = update.atmospheric.isNear
        state.forensic.isPowerTamper = update.integrity.isPowerTamper
        state.forensic.currentTiltDegrees = currentTilt
        if (update.atmospheric.acousticDb >= 0.0) state.forensic.currentAcousticDb = safeDouble(update.atmospheric.acousticDb)

        state.forensic.luxBaseline = SentinelValidator.updateLuxBaseline(state.forensic.luxBaseline, update.atmospheric.lux, isStationary(state, update.cpuLoad), update.isWarming)
        state.forensic.baroBaseline = SentinelValidator.updateBaroBaseline(state.forensic.baroBaseline, update.atmospheric.baroAlt, update.isWarming)

        // Using LocationUpdate flags
        if (!update.isSirenActive) {
            val updateDb = if (update.acousticMinDb >= 0.0) update.acousticMinDb else if (update.acousticMinDb == -1.0 && update.atmospheric.acousticDb >= 0.0) update.atmospheric.acousticDb else -1.0
            state.forensic.acousticFloorDb = SentinelValidator.updateAcousticFloor(state.forensic.acousticFloorDb, updateDb, update.isWarming)
            
            val contractionElapsedRt = update.nowRt - state.forensic.lastAcousticContractionRt
            if (contractionElapsedRt >= 500 || state.forensic.lastAcousticContractionRt == 0L) {
                if (state.forensic.acousticFloorDb > ACOUSTIC_FLOOR_MIN_DB && state.forensic.lastAcousticContractionRt > 0) {
                    val secondsPassed = contractionElapsedRt / 1000.0
                    if (secondsPassed > 0) {
                        val decayFactor = Math.pow(ACOUSTIC_FLOOR_CONTRACTION_EMA, secondsPassed)
                        state.forensic.acousticFloorDb = max(state.forensic.acousticFloorDb * decayFactor, ACOUSTIC_FLOOR_MIN_DB)
                    }
                }
                state.forensic.lastAcousticContractionRt = update.nowRt
            }
        }
        
        if (update.providedAdaptiveFloor >= 0.0) {
            state.forensic.adaptiveVibrationFloor = update.providedAdaptiveFloor
        } else if (update.atmospheric.vibration >= 0.0) {
            state.forensic.adaptiveVibrationFloor = SentinelValidator.updateVibrationFloor(state.forensic.adaptiveVibrationFloor, state.forensic.currentVibrationIndex, update.isWarming, update.cpuLoad)
        }
        
        return baselineChanged
    }

    fun consumeSitDetected(state: LocationProcessingState): Boolean {
        val result = state.forensic.isSitDetected
        state.forensic.isSitDetected = false
        return result
    }

    fun resetChairBaseline(state: LocationProcessingState) {
        state.forensic.baselineSitTilt = -1.0
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
        cpuLoad: Double = 0.0,
        acousticFloorDb: Double = -1.0
    ): SentinelResult {
        state.forensic.lastSnr = snr
        state.forensic.lastSatsUsed = satsUsed

        if (acousticFloorDb >= 0.0) {
            state.forensic.acousticFloorDb = max(acousticFloorDb, ACOUSTIC_FLOOR_MIN_DB)
        }
        
        if (state.forensic.lastValidTs == 0L) {
            updateLastValid(state, lat, lng, alt, timestamp, nowRt, 0.0, bearing, accuracy)
            updateFilters(state, lat, lng, timestamp, 1.0)
            resultFlyweight.reset(SentinelStatus.VALID)
            resultFlyweight.optimizedPoint = EngineGeoPoint(lat, lng, alt, timestamp, nowRt, accuracy, maxAccuracy)
            return resultFlyweight
        }

        val timeDeltaMs = timestamp - state.forensic.lastValidTs
        if (timeDeltaMs <= 0 && timestamp != 0L) {
            resultFlyweight.reset(SentinelStatus.VALID)
            return resultFlyweight
        }
        
        val altitudeDelta = if (state.forensic.lastValidAlt != 0.0) alt - state.forensic.lastValidAlt else 0.0
        val isParking = isStationary(state, cpuLoad)
        
        val dist = PhysicsUtils.calculateDistance(state.forensic.lastValidLat, state.forensic.lastValidLng, lat, lng)
        val impliesMotion = dist > ACTIVE_MOVE_THRESHOLD
        
        if (impliesMotion) {
            if (state.forensic.gpsMotionStartRt == 0L) state.forensic.gpsMotionStartRt = nowRt
        } else {
            state.forensic.gpsMotionStartRt = 0L
        }
        
        val isTractorSlowOverride = state.forensic.gpsMotionStartRt > 0 && (nowRt - state.forensic.gpsMotionStartRt > 10000L)
        val hasPhysicalMotion = if (isMuzzled) false else (state.forensic.currentVibrationIndex > (state.forensic.adaptiveVibrationFloor * 1.5) || isTractorSlowOverride)

        resultFlyweight.reset()
        val conf = resultFlyweight.jumpConfidence!!
        PhysicsUtils.isVisualJump(
            lastLat = state.forensic.lastValidLat, lastLng = state.forensic.lastValidLng,
            newLat = lat, newLng = lng,
            timeDeltaMs = if (state.forensic.lastValidRt > 0) (nowRt - state.forensic.lastValidRt) else timeDeltaMs, 
            accuracy = accuracy,
            lastAccuracy = state.forensic.lastValidAccuracy,
            snr = snr,
            lastSpeedMps = state.forensic.lastValidSpeedMps,
            isParking = isParking,
            altitudeDelta = altitudeDelta,
            hasPhysicalMotion = hasPhysicalMotion,
            result = conf
        )
        
        var score = conf.score
        val augmentedScore = score.coerceIn(0, 100)
        val timeDeltaSec = (if (state.forensic.lastValidRt > 0) (nowRt - state.forensic.lastValidRt) else timeDeltaMs) / 1000.0
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
                GtoEngine.addPoint(state, lat, lng, alt, accuracy, maxAccuracy, bearing, currentSpeedMps, timestamp, nowRt, state.forensic.currentVibrationIndex)
                resultFlyweight.status = behavioralStatus
                resultFlyweight.reason = behavioralReason
                return resultFlyweight
            }

            resultFlyweight.status = checkPhysicalTamper(state, nowRt, isMuzzled, cpuLoad)
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
        isMuzzled: Boolean = false,
        cpuLoad: Double = 0.0
    ): SentinelStatus {
        if (isMuzzled) return SentinelStatus.VALID

        if (!state.forensic.isNear) {
            resultFlyweight.reason = "Proximity Far"
            return SentinelStatus.TAMPER
        }
        if (state.forensic.isPowerTamper) {
            resultFlyweight.reason = "Power disconnected"
            return SentinelStatus.TAMPER
        }
        if (SentinelValidator.isTiltViolated(state.forensic.currentTiltDegrees)) {
            resultFlyweight.reason = "Tilt detected"
            return SentinelStatus.TAMPER
        }
        if (SentinelValidator.isShockViolated(state.forensic.peakVibrationShock, state.forensic.adaptiveVibrationFloor, cpuLoad = cpuLoad)) {
            resultFlyweight.reason = "Shock detected"
            return SentinelStatus.TAMPER
        }
        
        if (state.forensic.baroBaseline > -999.0) {
            val liftDelta = state.forensic.currentBaroAlt - state.forensic.baroBaseline
            if (SentinelValidator.isLiftViolated(liftDelta)) {
                if (state.forensic.currentVibrationIndex > VIBRATION_STATIONARY_THRESHOLD) {
                    resultFlyweight.reason = "Lift detected"
                    return SentinelStatus.TAMPER
                } else {
                    resultFlyweight.reason = "Barometric drift suspicion (No vibration)"
                    return SentinelStatus.TAMPER
                }
            }
        }
        
        if (SentinelValidator.isLightViolated(state.forensic.currentLux, state.forensic.luxBaseline)) {
            resultFlyweight.reason = "Light jump"
            return SentinelStatus.TAMPER
        }

        val isLightSpikeRecently = (state.forensic.lastFastPathLightSpikeRt > 0 && (nowRt - state.forensic.lastFastPathLightSpikeRt < LIGHT_LOCKOUT_MS))
        if (isLightSpikeRecently) {
            resultFlyweight.reason = "Light jump (FastPath)"
            return SentinelStatus.TAMPER
        }

        val isAcousticLockedOut = (state.forensic.lastFastPathAcousticSpikeRt > 0 && (nowRt - state.forensic.lastFastPathAcousticSpikeRt < LIGHT_LOCKOUT_MS))
        
        if (!isAcousticLockedOut && SentinelValidator.isAcousticViolated(state.forensic.currentAcousticDb, state.forensic.acousticFloorDb)) {
            resultFlyweight.reason = "Acoustic alarm"
            return SentinelStatus.TAMPER
        }

        if (SentinelValidator.isVibrationSuspicious(state.forensic.currentVibrationIndex, state.forensic.adaptiveVibrationFloor, cpuLoad = cpuLoad)) {
            resultFlyweight.reason = "Vibration suspicion"
            return SentinelStatus.TAMPER
        }
        
        if (!isAcousticLockedOut && SentinelValidator.isAcousticSuspicious(state.forensic.currentAcousticDb, state.forensic.acousticFloorDb, state.forensic.currentVibrationIndex)) {
            resultFlyweight.reason = "Acoustic suspicion"
            return SentinelStatus.TAMPER
        }

        return SentinelStatus.VALID
    }

    fun isStationary(state: LocationProcessingState, cpuLoad: Double = 0.0): Boolean = SentinelValidator.isStationary(state.forensic.currentVibrationIndex, state.forensic.adaptiveVibrationFloor, cpuLoad)

    fun shouldThrottlePolling(state: LocationProcessingState, providedIsStationary: Boolean? = null, cpuLoad: Double = 0.0): Boolean {
        val stationary = providedIsStationary ?: isStationary(state, cpuLoad)
        return stationary &&
               abs(state.forensic.currentCompassHeading - state.forensic.lastCompassHeading) < THROTTLE_COMPASS_LIMIT &&
               (if (state.forensic.baroBaseline > -999.0) abs(state.forensic.currentBaroAlt - state.forensic.baroBaseline) < THROTTLE_BARO_LIMIT else true) &&
               state.forensic.isNear && (state.forensic.currentLux - state.forensic.luxBaseline < THROTTLE_LUX_LIMIT) && !state.forensic.isPowerTamper &&
               state.forensic.currentTiltDegrees < THROTTLE_TILT_LIMIT && (state.forensic.currentAcousticDb - state.forensic.acousticFloorDb < THROTTLE_ACOUSTIC_LIMIT)
    }

    private fun updateLastValid(state: LocationProcessingState, lat: Double, lng: Double, alt: Double, ts: Long, rt: Long, speedMps: Double, bearing: Double, accuracy: Double) {
        state.forensic.lastValidLat = lat
        state.forensic.lastValidLng = lng
        state.forensic.lastValidAlt = alt
        state.forensic.lastValidTs = ts
        state.forensic.lastValidRt = rt
        state.forensic.lastValidSpeedMps = speedMps
        state.forensic.lastValidBearing = bearing
        state.forensic.lastValidAccuracy = accuracy
    }

    fun reset(state: LocationProcessingState) {
        state.forensic.lastValidTs = 0L
        state.forensic.lastValidRt = 0L
        state.forensic.currentVibrationIndex = 0.0
        state.forensic.currentBaroAlt = 0.0
        state.forensic.currentLux = 0.0
        state.forensic.isNear = true
        state.forensic.isPowerTamper = false
        state.forensic.currentTiltDegrees = 0.0
        state.forensic.currentAcousticDb = 0.0
        state.forensic.luxBaseline = -1.0
        state.forensic.baroBaseline = -1000.0
        state.forensic.acousticFloorDb = -1.0
        state.forensic.adaptiveVibrationFloor = INITIAL_VIBRATION_FLOOR
        state.forensic.peakVibrationShock = 0.0
        state.forensic.peakVibrationShockRt = 0L
        state.forensic.lastAcousticContractionRt = 0L
        state.forensic.isSitDetected = false
        state.forensic.lastSitTs = 0L
        state.forensic.lastSitRt = 0L
        state.forensic.baselineSitTilt = -1.0
        state.forensic.sitDetectionCooldownRt = 0L
        state.forensic.stationaryStartRt = 0L
        state.forensic.lastSitVz = 0.0
        state.forensic.lastSitVzTs = 0L
        state.forensic.lastSitVzRt = 0L
        state.forensic.lastSitDz = 0.0
        state.forensic.lastSitBaro = 0.0
        state.forensic.lastSitTilt = 0.0
        state.forensic.lastSitShock = 0.0
        state.forensic.gpsMotionStartRt = 0L
        state.forensic.lastFastPathAcousticSpikeRt = 0L
        state.forensic.lastFastPathLightSpikeRt = 0L
        state.forensic.estimatedSpeedMps = 0.0
        state.forensic.estimatedBearing = 0.0
        state.forensic.stationaryProb = 1.0
        state.forensic.lastValidAccuracy = 0.0
        state.kineticEnergy = 0.0
        GtoEngine.clear(state)
        resultFlyweight.reset()
    }
}
