package com.gps19.core.engine

import java.util.Locale
import kotlin.math.*

/**
 * LocationSentinel: A multi-layered location validation engine.
 * July.21.00:
 * - Forensic Hardening: Replaced ImmFilter with low-latency EMA smoothing.
 * - Monotonic Rt Alignment: Standardized timing to 'nowRt'.
 * July.20.07:
 * - Issue #102: Temporal Forensic Integrity. Refactored timing to monotonic 'rt'.
 * - Issue #107: Forensic Sitting Detection restored.
 */
class LocationSentinel {

    private val gtoEngine = GtoEngine()

    private var lastValidLat: Double = 0.0
    private var lastValidLng: Double = 0.0
    private var lastValidAlt: Double = 0.0
    private var lastValidTs: Long = 0L // Wall-clock
    private var lastValidRt: Long = 0L // Monotonic
    private var lastValidSpeedMps: Double = 0.0
    private var lastValidBearing: Double = 0.0

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

    // Sit Detection State
    var isSitDetected: Boolean = false
        private set
        
    var lastSitTs: Long = 0L // Wall-clock
    var lastSitRt: Long = 0L // Monotonic
    var baselineSitTilt: Double = -1.0
    
    private var lastSitVz: Double = 0.0
    private var lastSitVzTs: Long = 0L
    private var lastSitVzRt: Long = 0L
    private var lastSitDz: Double = 0.0
    private var lastSitBaro: Double = 0.0
    private var lastSitTilt: Double = 0.0
    private var lastSitShock: Double = 0.0

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

    private fun safeD(v: Double): Double = if (v.isNaN() || v.isInfinite()) 0.0 else v

    fun loadForensicState(savedLastSitTs: Long, savedBaseline: Double) {
        this.lastSitTs = savedLastSitTs
        this.baselineSitTilt = savedBaseline
    }

    fun setSpatialAnchor(lat: Double, lng: Double, alt: Double, timestamp: Long, rt: Long) {
        lastValidLat = lat; lastValidLng = lng; lastValidAlt = alt; lastValidTs = timestamp; lastValidRt = rt
        updateFilters(lat, lng, timestamp, 1.0)
    }

    private fun updateFilters(lat: Double, lng: Double, ts: Long, qScale: Double) {
        if (lastValidTs > 0) {
            val d = PhysicsUtils.calculateDistance(lastValidLat, lastValidLng, lat, lng)
            val dt = max(0.1, (ts - lastValidTs) / 1000.0)
            val speed = d / dt
            estimatedSpeedMps = PhysicsUtils.smoothCoordinate(estimatedSpeedMps, speed, SPEED_EMA_ALPHA)
            
            val bearing = atan2(lng - lastValidLng, lat - lastValidLat) * 180.0 / PI
            estimatedBearing = PhysicsUtils.smoothBearing(estimatedBearing, (bearing + 360) % 360, BEARING_EMA_ALPHA)
            
            val prob = if (estimatedSpeedMps < STATIONARY_SPEED_THRESHOLD_MPS) 1.0 else 0.0
            stationaryProb = PhysicsUtils.smoothCoordinate(stationaryProb, prob, POSITION_EMA_ALPHA_STATIONARY)
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
        nowRt: Long,
        nowTs: Long
    ): Boolean {
        var baselineChanged = false
        
        this.lastCompassHeading = this.currentCompassHeading
        this.currentVibrationIndex = safeD(vibration)
        this.lastFastPathAcousticSpikeRt = acousticLockoutRt
        
        if (peakShock > this.peakVibrationShock && !peakShock.isNaN()) {
            this.peakVibrationShock = peakShock
            this.peakVibrationShockRt = nowRt
        }

        val tiltDelta = abs(safeD(tiltDegrees) - baselineSitTilt)
        val baroDelta = if (baroBaseline > -999.0) abs(safeD(baroAlt) - baroBaseline) else 0.0
        
        if (nowRt > sitDetectionCooldownRt && !isMuzzled) {
            val isSpatialTriggered = (tiltDelta > TILT_THRESHOLD_DEGREES) || 
                                     (baroDelta > BARO_LIFT_THRESHOLD_METERS) || 
                                     plungeMatched
            
            if (isSpatialTriggered && (peakShock > VIBRATION_SHOCK_THRESHOLD_G || plungeMatched)) {
                isSitDetected = true
                lastSitTs = nowTs
                lastSitRt = nowRt
                sitDetectionCooldownRt = nowRt + 60000L // 1 minute cooldown
                
                lastSitVz = safeD(peakVerticalVelocity)
                lastSitVzTs = peakVerticalVelocityTs
                lastSitVzRt = peakVerticalVelocityRt
                lastSitDz = safeD(peakVerticalDisplacement)
                lastSitBaro = safeD(baroDelta)
                lastSitTilt = safeD(tiltDelta)
                lastSitShock = safeD(peakShock)
            }
        }

        if (isStationary() && !isSitDetected) {
            if (stationaryStartRt == 0L) stationaryStartRt = nowRt
            else if (nowRt - stationaryStartRt > PASSIVE_ZEROING_STATIONARY_MS) {
                if (abs(baselineSitTilt - tiltDegrees) > 0.1 && !tiltDegrees.isNaN()) {
                    baselineSitTilt = tiltDegrees
                    baselineChanged = true
                }
                stationaryStartRt = 0L
            }
        } else {
            stationaryStartRt = 0L
        }

        this.currentCompassHeading = safeD(heading)
        this.currentBaroAlt = safeD(baroAlt)
        this.currentLux = safeD(lux)
        this.isNear = isNear
        this.isPowerTamper = powerTamper
        this.currentTiltDegrees = safeD(tiltDegrees)
        this.currentAcousticDb = safeD(acousticDb)

        if (luxBaseline < 0) {
            if (!lux.isNaN()) luxBaseline = lux
        } else {
            if (!lux.isNaN()) {
                val baseAlpha = if (lux < luxBaseline) {
                    if (isStationary()) LUX_EMA_DOWN_SLOW else LUX_EMA_DOWN_FAST
                } else {
                    if (isStationary()) LUX_EMA_UP_SLOW else LUX_EMA_UP_FAST
                }
                val alpha = SentinelValidator.accelerateAlpha(baseAlpha, isWarming)
                luxBaseline = (luxBaseline * (1.0 - alpha)) + (lux * alpha)
            }
        }
        
        if (baroBaseline < -999.0) {
            if (!baroAlt.isNaN()) baroBaseline = baroAlt
        } else {
            if (!baroAlt.isNaN()) {
                val alpha = SentinelValidator.accelerateAlpha(BARO_EMA_SLOW, isWarming)
                baroBaseline = (baroBaseline * (1.0 - alpha)) + (baroAlt * alpha)
            }
        }

        if (!isSirenActive) {
            val updateDb = if (acousticMinDb >= 0.0) acousticMinDb else if (acousticMinDb == -1.0 && acousticDb >= 0.0) acousticDb else -1.0
            
            if (updateDb >= 0.0 && !updateDb.isNaN()) {
                if (acousticFloorDb < 0) {
                    acousticFloorDb = max(updateDb, ACOUSTIC_FLOOR_MIN_DB)
                } else if (updateDb < acousticFloorDb) {
                    val alpha = SentinelValidator.accelerateAlpha(ACOUSTIC_EMA_DOWN_FAST, isWarming)
                    acousticFloorDb = (acousticFloorDb * (1.0 - alpha)) + (updateDb * alpha) 
                } else {
                    val alpha = SentinelValidator.accelerateAlpha(ACOUSTIC_EMA_UP_FAST, isWarming)
                    acousticFloorDb = (acousticFloorDb * (1.0 - alpha)) + (updateDb * alpha)
                }
                if (acousticFloorDb < ACOUSTIC_FLOOR_MIN_DB) acousticFloorDb = ACOUSTIC_FLOOR_MIN_DB
            }
            
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
            updateLastValid(lat, lng, alt, timestamp, nowRt, 0.0, bearing)
            updateFilters(lat, lng, timestamp, 1.0)
            return SentinelResult(SentinelStatus.VALID, optimizedPoint = EngineGeoPoint(lat, lng, alt, timestamp, nowRt, accuracy, maxAccuracy))
        }

        val timeDeltaMs = timestamp - lastValidTs
        if (timeDeltaMs <= 0 && timestamp != 0L) return SentinelResult(SentinelStatus.VALID) 
        
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

        val jumpConfidence = PhysicsUtils.isVisualJump(
            lastLat = lastValidLat, lastLng = lastValidLng,
            newLat = lat, newLng = lng,
            timeDeltaMs = if (lastValidRt > 0) (nowRt - lastValidRt) else timeDeltaMs, 
            accuracy = accuracy,
            snr = snr,
            lastSpeedMps = lastValidSpeedMps,
            isParking = isParking,
            altitudeDelta = altitudeDelta,
            hasPhysicalMotion = hasPhysicalMotion
        )
        
        var score = jumpConfidence.score
        val augmentedScore = score.coerceIn(0, 100)
        val timeDeltaSec = (if (lastValidRt > 0) (nowRt - lastValidRt) else timeDeltaMs) / 1000.0
        val currentSpeedMps = dist / max(0.1, timeDeltaSec)
        
        val finalJumpConfidence = jumpConfidence.copy(
            score = augmentedScore, 
            isJump = augmentedScore >= 50 || jumpConfidence.isJump,
            reason = jumpConfidence.reason
        )

        if (finalJumpConfidence.isOutlier) return SentinelResult(SentinelStatus.JUMP, finalJumpConfidence.reason, jumpConfidence = finalJumpConfidence)
        
        var behavioralStatus = if (finalJumpConfidence.isJump) SentinelStatus.JUMP else SentinelStatus.VALID
        var behavioralReason = finalJumpConfidence.reason

        if (!bypassBehavioral) {
            if (gtoEngine.evaluateTrajectory(lat, lng, bearing, currentSpeedMps, timestamp, nowRt)) {
                val promoted = mutableListOf<EngineGeoPoint>()
                gtoEngine.getWindow().forEach { p ->
                    updateFilters(p.lat, p.lng, p.ts, SUSPICIOUS_Q_SCALE)
                    promoted.add(EngineGeoPoint(p.lat, p.lng, p.alt, p.ts, p.rt, p.accuracy, p.maxAccuracy))
                    updateLastValid(p.lat, p.lng, p.alt, p.ts, p.rt, p.speedMps, p.bearing)
                }
                gtoEngine.clear()
                updateFilters(lat, lng, timestamp, SUSPICIOUS_Q_SCALE)
                updateLastValid(lat, lng, alt, timestamp, nowRt, currentSpeedMps, bearing)
                return SentinelResult(SentinelStatus.TRAJECTORY_PROMOTED, "Trajectory Promoted (GTO)", EngineGeoPoint(lat, lng, alt, timestamp, nowRt, accuracy, maxAccuracy), finalJumpConfidence, promotedPoints = promoted)
            }

            if (behavioralStatus == SentinelStatus.JUMP) {
                gtoEngine.addPoint(lat, lng, alt, accuracy, maxAccuracy, bearing, currentSpeedMps, timestamp, nowRt, currentVibrationIndex)
                return SentinelResult(behavioralStatus, finalJumpConfidence.reason, jumpConfidence = finalJumpConfidence)
            }

            val sensorSentinel = runSensorSentinel(lat, lng, alt, accuracy, bearing, nowRt, isMuzzled)
            if (sensorSentinel.status != SentinelStatus.VALID) {
                behavioralStatus = sensorSentinel.status
                behavioralReason = sensorSentinel.reason
            }
            
            if (sensorSentinel.status == SentinelStatus.VALID && sensorSentinel.suppressionNote != null) {
                updateFilters(lat, lng, timestamp, if (isSuspicious) SUSPICIOUS_Q_SCALE else 1.0)
                updateLastValid(lat, lng, alt, timestamp, nowRt, currentSpeedMps, bearing)
                gtoEngine.clear()
                return SentinelResult(behavioralStatus, behavioralReason, optimizedPoint = EngineGeoPoint(lat, lng, alt, timestamp, nowRt, accuracy, maxAccuracy), jumpConfidence = finalJumpConfidence, suppressionNote = sensorSentinel.suppressionNote)
            }
        }

        updateFilters(lat, lng, timestamp, if (isSuspicious) SUSPICIOUS_Q_SCALE else 1.0)
        updateLastValid(lat, lng, alt, timestamp, nowRt, currentSpeedMps, bearing)
        gtoEngine.clear()
        return SentinelResult(behavioralStatus, behavioralReason, optimizedPoint = EngineGeoPoint(lat, lng, alt, timestamp, nowRt, accuracy, maxAccuracy), jumpConfidence = finalJumpConfidence)
    }

    private fun runSensorSentinel(
        lat: Double, lng: Double, alt: Double, accuracy: Double, bearing: Double,
        nowRt: Long,
        isMuzzled: Boolean = false
    ): SentinelResult {
        if (!isNear) return SentinelResult(SentinelStatus.TAMPER, "Proximity Far")
        if (isPowerTamper) return SentinelResult(SentinelStatus.TAMPER, "Power disconnected")
        if (SentinelValidator.isTiltViolated(currentTiltDegrees)) return SentinelResult(SentinelStatus.TAMPER, "Tilt detected")
        if (SentinelValidator.isShockViolated(peakVibrationShock, adaptiveVibrationFloor)) return SentinelResult(SentinelStatus.TAMPER, "Shock detected")
        
        if (baroBaseline > -999.0) {
            val liftDelta = currentBaroAlt - baroBaseline
            if (SentinelValidator.isLiftViolated(liftDelta)) {
                if (currentVibrationIndex > VIBRATION_STATIONARY_THRESHOLD) {
                    return SentinelResult(SentinelStatus.TAMPER, "Lift detected")
                } else {
                    return SentinelResult(SentinelStatus.TAMPER, "Barometric drift suspicion (No vibration)")
                }
            }
        }
        
        if (SentinelValidator.isLightViolated(currentLux, luxBaseline)) return SentinelResult(SentinelStatus.TAMPER, "Light jump")

        val isAcousticLockedOut = (lastFastPathAcousticSpikeRt > 0 && (nowRt - lastFastPathAcousticSpikeRt < ACOUSTIC_LOCKOUT_MS))
        
        if (!isAcousticLockedOut && SentinelValidator.isAcousticViolated(currentAcousticDb, acousticFloorDb)) {
            return SentinelResult(SentinelStatus.TAMPER, "Acoustic alarm")
        }

        if (SentinelValidator.isVibrationSuspicious(currentVibrationIndex, adaptiveVibrationFloor)) {
            return SentinelResult(SentinelStatus.TAMPER, "Vibration suspicion")
        }
        
        if (!isAcousticLockedOut && SentinelValidator.isAcousticSuspicious(currentAcousticDb, acousticFloorDb, currentVibrationIndex)) {
            return SentinelResult(SentinelStatus.TAMPER, "Acoustic suspicion")
        }

        return SentinelResult(SentinelStatus.VALID)
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

    private fun updateLastValid(lat: Double, lng: Double, alt: Double, ts: Long, rt: Long, speedMps: Double, bearing: Double) {
        lastValidLat = lat; lastValidLng = lng; lastValidAlt = alt; lastValidTs = ts; lastValidRt = rt
        lastValidSpeedMps = speedMps; lastValidBearing = bearing
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
        gtoEngine.clear()
    }
}
