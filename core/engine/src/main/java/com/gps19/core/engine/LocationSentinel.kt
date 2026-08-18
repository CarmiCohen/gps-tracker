package com.gps19.core.engine

import com.gps19.core.engine.PhysicsUtils.safeDouble
import kotlin.math.*

/**
 * LocationSentinel: A multi-layered location validation engine.
 * Aug.18.05:
 * - Issue #201: Urban Edge Case Multipath Mitigation. Dampened stationaryProb 
 *   decay when physically stationary in low-SNR environments (R201).
 * Aug.14.06:
 * - Issue #172: Viewer-Side State Audit. Finalized forensic parity by adding 
 *   Vz timestamps (sitVzTs, sitVzRt) to loadForensicState (R172).
 */
class LocationSentinel {

    private val gtoEngine = GtoEngine()
    private val resultFlyweight = SentinelResult().apply { 
        jumpConfidence = JumpConfidence() 
    }

    private var lastValidLat: Double = 0.0
    private var lastValidLng: Double = 0.0
    private var lastValidAlt: Double = 0.0
    private var lastValidTs: Long = 0L // Wall-clock
    private var lastValidRt: Long = 0L // Monotonic
    private var lastValidSpeedMps: Double = 0.0
    private var lastValidBearing: Double = 0.0
    private var lastValidAccuracy: Double = 0.0

    private var estimatedSpeedMps: Double = 0.0
    private var estimatedBearing: Double = 0.0
    private var stationaryProb: Double = 1.0

    internal var currentVibrationIndex: Double = 0.0
    var peakVibrationShock: Double = 0.0
        private set
    var peakVibrationShockRt: Long = 0L
        private set

    internal var currentCompassHeading: Double = 0.0
    internal var lastCompassHeading: Double = 0.0
    internal var currentBaroAlt: Double = 0.0
    internal var currentLux: Double = 0.0
    internal var isNear: Boolean = true
    internal var isPowerTamper: Boolean = false
    
    var currentTiltDegrees: Double = 0.0
        private set
    
    var currentAcousticDb: Double = 0.0
        private set
    private var lastFastPathAcousticSpikeRt: Long = 0L

    // Issue #601: Kinetic Energy
    var kineticEnergy: Double = 0.0
        private set

    // Sit Detection State
    var isSitDetected: Boolean = false
        private set
        
    var lastSitTs: Long = 0L // Wall-clock
    var lastSitRt: Long = 0L // Monotonic
    var baselineSitTilt: Double = -1.0
    
    // SIT Forensic Parameters (Forensic Parity R522)
    var lastSitVz: Double = 0.0; internal set
    var lastSitVzTs: Long = 0L; internal set
    var lastSitVzRt: Long = 0L; internal set
    var lastSitDz: Double = 0.0; internal set
    var lastSitBaro: Double = 0.0; internal set
    var lastSitTilt: Double = 0.0; internal set
    var lastSitShock: Double = 0.0; internal set

    private var sitDetectionCooldownRt: Long = 0L 
    private var stationaryStartRt: Long = 0L 

    // Tractor-Slow state
    private var gpsMotionStartRt: Long = 0L 

    // Dynamic Baselines
    var luxBaseline: Double = -1.0
        private set
    var baroBaseline: Double = -1000.0
        private set
    var acousticFloorDb: Double = -1.0
        private set
    var adaptiveVibrationFloor: Double = INITIAL_VIBRATION_FLOOR
        internal set
        
    private var lastAcousticContractionRt: Long = 0L

    private var lastSnr: Double = 0.0
    private var lastSatsUsed: Int = 0

    fun loadForensicState(
        savedLastSitTs: Long, 
        savedBaseline: Double,
        savedSitVz: Double = 0.0,
        savedSitDz: Double = 0.0,
        savedSitBaro: Double = 0.0,
        savedSitTilt: Double = 0.0,
        savedSitShock: Double = 0.0,
        savedSitVzTs: Long = 0L,
        savedSitVzRt: Long = 0L
    ) {
        this.lastSitTs = savedLastSitTs
        this.baselineSitTilt = savedBaseline
        this.lastSitVz = savedSitVz
        this.lastSitDz = savedSitDz
        this.lastSitBaro = savedSitBaro
        this.lastSitTilt = savedSitTilt
        this.lastSitShock = savedSitShock
        this.lastSitVzTs = savedSitVzTs
        this.lastSitVzRt = savedSitVzRt
    }

    fun setSpatialAnchor(lat: Double, lng: Double, alt: Double, timestamp: Long, rt: Long, accuracy: Double = 0.0) {
        lastValidLat = lat; lastValidLng = lng; lastValidAlt = alt; lastValidTs = timestamp; lastValidRt = rt; lastValidAccuracy = accuracy
        updateFilters(lat, lng, timestamp, 1.0)
    }

    private fun updateFilters(lat: Double, lng: Double, ts: Long, qScale: Double) {
        if (lastValidTs > 0) {
            val d = PhysicsUtils.calculateDistance(lastValidLat, lastValidLng, lat, lng)
            val dt = max(0.1, (ts - lastValidTs) / 1000.0)
            val speed = d / dt
            estimatedSpeedMps = PhysicsUtils.smoothCoordinate(estimatedSpeedMps, speed, SPEED_EMA_ALPHA)
            
            val bearing = PhysicsUtils.calculateBearing(lastValidLat, lastValidLng, lat, lng)
            estimatedBearing = PhysicsUtils.smoothBearing(estimatedBearing, bearing, BEARING_EMA_ALPHA)
            
            val prob = if (estimatedSpeedMps < STATIONARY_SPEED_THRESHOLD_MPS) 1.0 else 0.0
            
            // Issue #201: Urban Canyon Dampening.
            // If physically stationary but GPS speed suggests motion in a low-SNR environment,
            // we dampen the probability decay to avoid rapid anchor release.
            val isLowSnr = lastSnr > 0 && lastSnr < JUMP_GATE_LOW_SNR_THRESHOLD
            val alpha = if (isStationary() && isLowSnr && prob < stationaryProb) {
                POSITION_EMA_ALPHA_STATIONARY * 0.2 // 5x slower decay
            } else {
                POSITION_EMA_ALPHA_STATIONARY
            }
            
            stationaryProb = PhysicsUtils.smoothCoordinate(stationaryProb, prob, alpha)
        }
    }

    fun updateSensorState(
        vibration: Double, 
        heading: Double, 
        baroAlt: Double, 
        lux: Double = 0.0, 
        isNear: Boolean = true, 
        powerTamper: Boolean = false,
        tiltDegrees: Double = 0.0,
        acousticDb: Double = 0.0,
        peakShock: Double = 0.0,
        acousticMinDb: Double = -1.0,
        peakVerticalVelocity: Double = 0.0,
        peakVerticalVelocityTs: Long = 0L,
        peakVerticalVelocityRt: Long = 0L,
        plungeMatched: Boolean = false,
        peakVerticalDisplacement: Double = 0.0,
        isSirenActive: Boolean = false,
        isWarming: Boolean = false,
        manualAdaptiveFloor: Double = -1.0,
        acousticLockoutRt: Long = 0L,
        isMuzzled: Boolean = false,
        kineticEnergy: Double = 0.0,
        nowRt: Long,
        nowTs: Long
    ): Boolean {
        var baselineChanged = false
        
        this.lastCompassHeading = this.currentCompassHeading
        this.currentVibrationIndex = safeDouble(vibration)
        this.lastFastPathAcousticSpikeRt = acousticLockoutRt
        this.kineticEnergy = safeDouble(kineticEnergy)
        
        if (peakShock > this.peakVibrationShock && !peakShock.isNaN()) {
            this.peakVibrationShock = peakShock
            this.peakVibrationShockRt = nowRt
        }

        val currentTilt = safeDouble(tiltDegrees)
        val tiltDelta = if (baselineSitTilt >= 0.0) abs(currentTilt - baselineSitTilt) else 0.0
        val baroDelta = if (baroBaseline > -999.0) abs(safeDouble(baroAlt) - baroBaseline) else 0.0
        
        if (nowRt > sitDetectionCooldownRt && !isMuzzled && !isWarming) {
            val isSpatialTriggered = (tiltDelta > TILT_THRESHOLD_DEGREES) || 
                                     (baroDelta > BARO_LIFT_THRESHOLD_METERS) || 
                                     plungeMatched
            
            if (isSpatialTriggered) {
                val hasSufficientForce = (peakShock > VIBRATION_SHOCK_THRESHOLD_G) || plungeMatched || (abs(peakVerticalVelocity) > CHAIR_PLUNGE_VELOCITY_THRESHOLD)
                
                if (hasSufficientForce) {
                    isSitDetected = true
                    lastSitTs = nowTs
                    lastSitRt = nowRt
                    sitDetectionCooldownRt = nowRt + SIT_DUPLICATE_GUARD_MS
                    
                    lastSitVz = safeDouble(peakVerticalVelocity)
                    lastSitVzTs = if (peakVerticalVelocityTs > 0) peakVerticalVelocityTs else nowTs
                    lastSitVzRt = if (peakVerticalVelocityRt > 0) peakVerticalVelocityRt else nowRt
                    lastSitDz = safeDouble(peakVerticalDisplacement)
                    lastSitBaro = safeDouble(baroDelta)
                    lastSitTilt = safeDouble(tiltDelta)
                    lastSitShock = safeDouble(peakShock)
                }
            }
        }

        if (isStationary() && !isSitDetected) {
            if (stationaryStartRt == 0L) stationaryStartRt = nowRt
            else if (nowRt - stationaryStartRt > PASSIVE_ZEROING_STATIONARY_MS) {
                if (abs(baselineSitTilt - currentTilt) > 0.1 && !currentTilt.isNaN()) {
                    baselineSitTilt = currentTilt
                    baselineChanged = true
                }
                stationaryStartRt = 0L
            }
        } else {
            stationaryStartRt = 0L
        }

        this.currentCompassHeading = safeDouble(heading)
        this.currentBaroAlt = safeDouble(baroAlt)
        this.currentLux = safeDouble(lux)
        this.isNear = isNear
        this.isPowerTamper = powerTamper
        this.currentTiltDegrees = currentTilt
        this.currentAcousticDb = safeDouble(acousticDb)

        this.luxBaseline = SentinelValidator.updateLuxBaseline(this.luxBaseline, lux, isStationary(), isWarming)
        this.baroBaseline = SentinelValidator.updateBaroBaseline(this.baroBaseline, baroAlt, isWarming)

        if (!isSirenActive) {
            val updateDb = if (acousticMinDb >= 0.0) acousticMinDb else if (acousticMinDb == -1.0 && acousticDb >= 0.0) acousticDb else -1.0
            this.acousticFloorDb = SentinelValidator.updateAcousticFloor(this.acousticFloorDb, updateDb, isWarming)
            
            val contractionElapsedRt = nowRt - lastAcousticContractionRt
            if (contractionElapsedRt >= 500 || lastAcousticContractionRt == 0L) {
                if (acousticFloorDb > ACOUSTIC_FLOOR_MIN_DB && lastAcousticContractionRt > 0) {
                    val secondsPassed = contractionElapsedRt / 1000.0
                    if (secondsPassed > 0) {
                        val decayFactor = Math.pow(ACOUSTIC_FLOOR_CONTRACTION_EMA, secondsPassed)
                        acousticFloorDb = max(acousticFloorDb * decayFactor, ACOUSTIC_FLOOR_MIN_DB)
                    }
                }
                lastAcousticContractionRt = nowRt
            }
        }
        
        if (manualAdaptiveFloor >= 0.0) {
            this.adaptiveVibrationFloor = manualAdaptiveFloor
        } else { 
            this.adaptiveVibrationFloor = SentinelValidator.updateVibrationFloor(this.adaptiveVibrationFloor, currentVibrationIndex, isWarming)
        }
        
        return baselineChanged
    }

    fun consumeSitDetected(): Boolean {
        val result = isSitDetected
        isSitDetected = false
        return result
    }

    fun resetChairBaseline() {
        baselineSitTilt = -1.0
    }

    fun getEstimatedSpeedMps(): Double = estimatedSpeedMps
    fun getEstimatedBearing(): Double = estimatedBearing
    fun getStationaryProbability(): Double = stationaryProb

    fun getHindsightBuffer(): List<RejectedPoint> = gtoEngine.getWindow().map {
        RejectedPoint(it.lat, it.lng, it.alt, it.accuracy, it.bearing, it.speedMps, it.ts, it.rt)
    }

    fun processLocation(
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
        this.lastSnr = snr
        this.lastSatsUsed = satsUsed

        if (acousticFloorDb >= 0.0) {
            this.acousticFloorDb = max(acousticFloorDb, ACOUSTIC_FLOOR_MIN_DB)
        }
        
        if (lastValidTs == 0L) {
            updateLastValid(lat, lng, alt, timestamp, nowRt, 0.0, bearing, accuracy)
            updateFilters(lat, lng, timestamp, 1.0)
            resultFlyweight.reset(SentinelStatus.VALID)
            resultFlyweight.optimizedPoint = EngineGeoPoint(lat, lng, alt, timestamp, nowRt, accuracy, maxAccuracy)
            return resultFlyweight
        }

        val timeDeltaMs = timestamp - lastValidTs
        if (timeDeltaMs <= 0 && timestamp != 0L) {
            resultFlyweight.reset(SentinelStatus.VALID)
            return resultFlyweight
        }
        
        val altitudeDelta = if (lastValidAlt != 0.0) alt - lastValidAlt else 0.0
        val isParking = isStationary()
        
        val dist = PhysicsUtils.calculateDistance(lastValidLat, lastValidLng, lat, lng)
        val impliesMotion = dist > ACTIVE_MOVE_THRESHOLD
        
        if (impliesMotion) {
            if (gpsMotionStartRt == 0L) gpsMotionStartRt = nowRt
        } else {
            gpsMotionStartRt = 0L
        }
        
        val isTractorSlowOverride = gpsMotionStartRt > 0 && (nowRt - gpsMotionStartRt > 10000L)
        val hasPhysicalMotion = if (isMuzzled) false else (currentVibrationIndex > (adaptiveVibrationFloor * 1.5) || isTractorSlowOverride)

        resultFlyweight.reset()
        val conf = resultFlyweight.jumpConfidence!!
        PhysicsUtils.isVisualJump(
            lastLat = lastValidLat, lastLng = lastValidLng,
            newLat = lat, newLng = lng,
            timeDeltaMs = if (lastValidRt > 0) (nowRt - lastValidRt) else timeDeltaMs, 
            accuracy = accuracy,
            lastAccuracy = lastValidAccuracy,
            snr = snr,
            lastSpeedMps = lastValidSpeedMps,
            isParking = isParking,
            altitudeDelta = altitudeDelta,
            hasPhysicalMotion = hasPhysicalMotion,
            result = conf
        )
        
        var score = conf.score
        val augmentedScore = score.coerceIn(0, 100)
        val timeDeltaSec = (if (lastValidRt > 0) (nowRt - lastValidRt) else timeDeltaMs) / 1000.0
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
            if (gtoEngine.evaluateTrajectory(lat, lng, bearing, currentSpeedMps, timestamp, nowRt)) {
                val promoted = mutableListOf<EngineGeoPoint>()
                gtoEngine.getWindow().forEach { p ->
                    updateFilters(p.lat, p.lng, p.ts, SUSPICIOUS_Q_SCALE)
                    promoted.add(EngineGeoPoint(p.lat, p.lng, p.alt, p.ts, p.rt, p.accuracy, p.maxAccuracy))
                    updateLastValid(p.lat, p.lng, p.alt, p.ts, p.rt, p.speedMps, p.bearing, p.accuracy)
                }
                gtoEngine.clear()
                updateFilters(lat, lng, timestamp, SUSPICIOUS_Q_SCALE)
                updateLastValid(lat, lng, alt, timestamp, nowRt, currentSpeedMps, bearing, accuracy)
                
                resultFlyweight.status = SentinelStatus.TRAJECTORY_PROMOTED
                resultFlyweight.reason = "Trajectory Promoted (GTO)"
                resultFlyweight.optimizedPoint = EngineGeoPoint(lat, lng, alt, timestamp, nowRt, accuracy, maxAccuracy)
                resultFlyweight.promotedPoints = promoted
                return resultFlyweight
            }

            if (behavioralStatus == SentinelStatus.JUMP) {
                gtoEngine.addPoint(lat, lng, alt, accuracy, maxAccuracy, bearing, currentSpeedMps, timestamp, nowRt, currentVibrationIndex)
                resultFlyweight.status = behavioralStatus
                resultFlyweight.reason = behavioralReason
                return resultFlyweight
            }

            resultFlyweight.status = checkPhysicalTamper(nowRt, isMuzzled)
            if (resultFlyweight.status != SentinelStatus.VALID) {
                return resultFlyweight
            }
            
            if (resultFlyweight.status == SentinelStatus.VALID && resultFlyweight.suppressionNote != null) {
                updateFilters(lat, lng, timestamp, if (isSuspicious) SUSPICIOUS_Q_SCALE else 1.0)
                updateLastValid(lat, lng, alt, timestamp, nowRt, currentSpeedMps, bearing, accuracy)
                gtoEngine.clear()
                resultFlyweight.optimizedPoint = EngineGeoPoint(lat, lng, alt, timestamp, nowRt, accuracy, maxAccuracy)
                return resultFlyweight
            }
        }

        updateFilters(lat, lng, timestamp, if (isSuspicious) SUSPICIOUS_Q_SCALE else 1.0)
        updateLastValid(lat, lng, alt, timestamp, nowRt, currentSpeedMps, bearing, accuracy)
        gtoEngine.clear()
        resultFlyweight.status = behavioralStatus
        resultFlyweight.reason = behavioralReason
        resultFlyweight.optimizedPoint = EngineGeoPoint(lat, lng, alt, timestamp, nowRt, accuracy, maxAccuracy)
        return resultFlyweight
    }

    /**
     * checkPhysicalTamper: Publicly accessible sensor status evaluator (R141).
     */
    fun checkPhysicalTamper(
        nowRt: Long = 0L,
        isMuzzled: Boolean = false
    ): SentinelStatus {
        if (isMuzzled) return SentinelStatus.VALID

        if (!isNear) {
            resultFlyweight.reason = "Proximity Far"
            return SentinelStatus.TAMPER
        }
        if (isPowerTamper) {
            resultFlyweight.reason = "Power disconnected"
            return SentinelStatus.TAMPER
        }
        if (SentinelValidator.isTiltViolated(currentTiltDegrees)) {
            resultFlyweight.reason = "Tilt detected"
            return SentinelStatus.TAMPER
        }
        if (SentinelValidator.isShockViolated(peakVibrationShock, adaptiveVibrationFloor)) {
            resultFlyweight.reason = "Shock detected"
            return SentinelStatus.TAMPER
        }
        
        if (baroBaseline > -999.0) {
            val liftDelta = currentBaroAlt - baroBaseline
            if (SentinelValidator.isLiftViolated(liftDelta)) {
                if (currentVibrationIndex > VIBRATION_STATIONARY_THRESHOLD) {
                    resultFlyweight.reason = "Lift detected"
                    return SentinelStatus.TAMPER
                } else {
                    resultFlyweight.reason = "Barometric drift suspicion (No vibration)"
                    return SentinelStatus.TAMPER
                }
            }
        }
        
        if (SentinelValidator.isLightViolated(currentLux, luxBaseline)) {
            resultFlyweight.reason = "Light jump"
            return SentinelStatus.TAMPER
        }

        val isAcousticLockedOut = (lastFastPathAcousticSpikeRt > 0 && (nowRt - lastFastPathAcousticSpikeRt < ACOUSTIC_LOCKOUT_MS))
        
        if (!isAcousticLockedOut && SentinelValidator.isAcousticViolated(currentAcousticDb, acousticFloorDb)) {
            resultFlyweight.reason = "Acoustic alarm"
            return SentinelStatus.TAMPER
        }

        if (SentinelValidator.isVibrationSuspicious(currentVibrationIndex, adaptiveVibrationFloor)) {
            resultFlyweight.reason = "Vibration suspicion"
            return SentinelStatus.TAMPER
        }
        
        if (!isAcousticLockedOut && SentinelValidator.isAcousticSuspicious(currentAcousticDb, acousticFloorDb, currentVibrationIndex)) {
            resultFlyweight.reason = "Acoustic suspicion"
            return SentinelStatus.TAMPER
        }

        return SentinelStatus.VALID
    }

    fun isStationary(): Boolean = SentinelValidator.isStationary(currentVibrationIndex, adaptiveVibrationFloor)

    fun shouldThrottlePolling(providedIsStationary: Boolean? = null): Boolean {
        val stationary = providedIsStationary ?: isStationary()
        return stationary &&
               abs(currentCompassHeading - lastCompassHeading) < THROTTLE_COMPASS_LIMIT &&
               (if (baroBaseline > -999.0) abs(currentBaroAlt - baroBaseline) < THROTTLE_BARO_LIMIT else true) &&
               isNear && (currentLux - luxBaseline < THROTTLE_LUX_LIMIT) && !isPowerTamper &&
               currentTiltDegrees < THROTTLE_TILT_LIMIT && (currentAcousticDb - acousticFloorDb < THROTTLE_ACOUSTIC_LIMIT)
    }

    private fun updateLastValid(lat: Double, lng: Double, alt: Double, ts: Long, rt: Long, speedMps: Double, bearing: Double, accuracy: Double) {
        lastValidLat = lat; lastValidLng = lng; lastValidAlt = alt; lastValidTs = ts; lastValidRt = rt
        lastValidSpeedMps = speedMps; lastValidBearing = bearing; lastValidAccuracy = accuracy
    }

    fun reset() {
        lastValidTs = 0L; lastValidRt = 0L; currentVibrationIndex = 0.0; currentBaroAlt = 0.0
        currentLux = 0.0; isNear = true; isPowerTamper = false; currentTiltDegrees = 0.0
        currentAcousticDb = 0.0; luxBaseline = -1.0; baroBaseline = -1000.0; acousticFloorDb = -1.0
        adaptiveVibrationFloor = INITIAL_VIBRATION_FLOOR; peakVibrationShock = 0.0; peakVibrationShockRt = 0L
        lastAcousticContractionRt = 0L
        isSitDetected = false; lastSitTs = 0L; lastSitRt = 0L; baselineSitTilt = -1.0; sitDetectionCooldownRt = 0L; stationaryStartRt = 0L
        lastSitVz = 0.0; lastSitVzTs = 0L; lastSitVzRt = 0L; lastSitDz = 0.0; lastSitBaro = 0.0; lastSitTilt = 0.0; lastSitShock = 0.0
        gpsMotionStartRt = 0L
        lastFastPathAcousticSpikeRt = 0L
        estimatedSpeedMps = 0.0
        estimatedBearing = 0.0
        stationaryProb = 1.0
        lastValidAccuracy = 0.0
        kineticEnergy = 0.0
        gtoEngine.clear()
        resultFlyweight.reset()
    }
}
