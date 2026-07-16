package com.gps19.core.engine

import java.util.Locale
import kotlin.math.*

/**
 * LocationSentinel: A multi-layered location validation engine.
 * July.16.22:
 * - Issue #510: Abandon Chair Sit Detection. Removed all sit-related logic and state.
 * - Issue #508: Optimization Removal. Removed isMuzzled parameter and logic.
 * - Issue #512: Consolidate Sentinel Statuses. Simplified result mapping to VALID, JUMP, TAMPER.
 * - Issue #521: Passive Zeroing. Implemented tiltBaseline capture for relative tilt detection.
 */
class LocationSentinel {

    private var smoothedLat: Double = 0.0
    private var smoothedLng: Double = 0.0
    private var smoothedSpeedMps: Double = 0.0
    private var smoothedBearing: Double = 0.0

    private var prevValidLat: Double = 0.0
    private var prevValidLng: Double = 0.0
    
    private var lastValidLat: Double = 0.0
    private var lastValidLng: Double = 0.0
    private var lastValidAlt: Double = 0.0
    internal var lastValidTs: Long = 0L
    private var lastValidSpeedMps: Double = 0.0
    private var lastValidBearing: Double = 0.0

    internal var currentVibrationIndex: Double = 0.0
    var peakVibrationShock: Double = 0.0
        private set
    var peakVibrationShockTs: Long = 0L
        private set

    internal var currentCompassHeading: Double = 0.0
    internal var lastCompassHeading: Double = 0.0
    internal var currentBaroAlt: Double = 0.0
    internal var currentLux: Double = 0.0
    internal var isNear: Boolean = true
    internal var isPowerTamper: Boolean = false
    
    var currentTiltDegrees: Double = 0.0
        private set
    var tiltBaseline: Double = 0.0
        private set

    var currentAcousticDb: Double = 0.0
        private set
    internal var lastFastPathAcousticSpikeTs: Long = 0L

    private var stationaryStartTs: Long = 0L 

    // Tractor-Slow state
    private var gpsMotionStartTs: Long = 0L 

    // Dynamic Baselines
    var luxBaseline: Double = -1.0
        private set
    var baroBaseline: Double = -1000.0
        private set
    var acousticFloorDb: Double = -1.0
        private set
    var adaptiveVibrationFloor: Double = INITIAL_VIBRATION_FLOOR
        internal set
        
    private var lastAcousticContractionRealtime: Long = 0L

    private var lastSnr: Double = 0.0
    private var lastSatsUsed: Int = 0

    private fun safeD(v: Double): Double = if (v.isNaN() || v.isInfinite()) 0.0 else v

    fun setSpatialAnchor(lat: Double, lng: Double, alt: Double, timestamp: Long) {
        lastValidLat = lat; lastValidLng = lng; lastValidAlt = alt; lastValidTs = timestamp
        smoothedLat = lat; smoothedLng = lng
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
        plungeMatched: Boolean = false,
        peakVerticalDisplacement: Double = 0.0,
        isSirenActive: Boolean = false,
        isWarming: Boolean = false,
        manualAdaptiveFloor: Double = -1.0,
        acousticLockoutTs: Long = 0L,
        nowRealtime: Long,
        nowWall: Long
    ): Boolean {
        var baselineChanged = false
        
        this.lastCompassHeading = this.currentCompassHeading
        this.currentVibrationIndex = safeD(vibration)
        this.lastFastPathAcousticSpikeTs = acousticLockoutTs
        
        if (peakShock > this.peakVibrationShock && !peakShock.isNaN()) {
            this.peakVibrationShock = peakShock
            this.peakVibrationShockTs = nowWall
        }

        // Issue #521: Passive Zeroing for tilt baseline
        if (isStationary()) {
            if (stationaryStartTs == 0L) {
                stationaryStartTs = nowRealtime
            } else if (nowRealtime - stationaryStartTs > PASSIVE_ZEROING_STATIONARY_MS) {
                this.tiltBaseline = safeD(tiltDegrees)
                stationaryStartTs = nowRealtime // Allow periodic updates if it stays parked
            }
        } else {
            stationaryStartTs = 0L
        }

        this.currentCompassHeading = safeD(heading)
        this.currentBaroAlt = safeD(baroAlt)
        this.currentLux = safeD(lux)
        this.isNear = isNear
        this.isPowerTamper = powerTamper
        this.currentTiltDegrees = abs(safeD(tiltDegrees) - tiltBaseline)
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
            
            val contractionElapsed = nowRealtime - lastAcousticContractionRealtime
            if (contractionElapsed >= 500 || lastAcousticContractionRealtime == 0L) {
                if (acousticFloorDb > ACOUSTIC_FLOOR_MIN_DB && lastAcousticContractionRealtime > 0) {
                    val secondsPassed = contractionElapsed / 1000.0
                    if (secondsPassed > 0) {
                        val decayFactor = Math.pow(ACOUSTIC_FLOOR_CONTRACTION_EMA, secondsPassed)
                        acousticFloorDb = max(acousticFloorDb * decayFactor, ACOUSTIC_FLOOR_MIN_DB)
                    }
                }
                lastAcousticContractionRealtime = nowRealtime
            }
        }
        
        if (manualAdaptiveFloor >= 0.0) {
            this.adaptiveVibrationFloor = manualAdaptiveFloor
        } else { 
            this.adaptiveVibrationFloor = SentinelValidator.updateVibrationFloor(this.adaptiveVibrationFloor, currentVibrationIndex, isWarming)
        }
        
        return baselineChanged
    }

    fun getEstimatedSpeedMps(): Double = smoothedSpeedMps
    fun getEstimatedBearing(): Double = smoothedBearing
    fun getStationaryProbability(): Double = if (isStationary()) 1.0 else 0.0

    fun processLocation(
        lat: Double, lng: Double, alt: Double, accuracy: Double, 
        maxAccuracy: Double, 
        bearing: Double,
        snr: Double, satsUsed: Int, timestamp: Long, 
        bypassBehavioral: Boolean = false,
        nowWall: Long,
        nowRealtime: Long,
        acousticFloorDb: Double = -1.0
    ): SentinelResult {
        this.lastSnr = snr
        this.lastSatsUsed = satsUsed

        if (acousticFloorDb >= 0.0) {
            this.acousticFloorDb = max(acousticFloorDb, ACOUSTIC_FLOOR_MIN_DB)
        }
        
        if (lastValidTs == 0L) {
            updateLastValid(lat, lng, alt, timestamp, 0.0, bearing)
            val optimized = updateSmoothedPosition(lat, lng, 1.0, timestamp)
            return SentinelResult(SentinelStatus.VALID, optimizedPoint = EngineGeoPoint(optimized.lat, optimized.lng, ts = timestamp, accuracy = accuracy, maxAccuracy = maxAccuracy))
        }

        val timeDeltaMs = timestamp - lastValidTs
        if (timeDeltaMs <= 0) return SentinelResult(SentinelStatus.VALID) 
        
        val altitudeDelta = if (lastValidAlt != 0.0) alt - lastValidAlt else 0.0
        val isParking = isStationary()
        
        val dist = PhysicsUtils.calculateDistance(lastValidLat, lastValidLng, lat, lng)
        val impliesMotion = dist > ACTIVE_MOVE_THRESHOLD
        
        if (impliesMotion) {
            if (gpsMotionStartTs == 0L) gpsMotionStartTs = nowRealtime
        } else {
            gpsMotionStartTs = 0L
        }
        
        val isTractorSlowOverride = gpsMotionStartTs > 0 && (nowRealtime - gpsMotionStartTs > 10000L)
        val hasPhysicalMotion = (currentVibrationIndex > (adaptiveVibrationFloor * 1.5) || isTractorSlowOverride)

        val jumpConfidence = PhysicsUtils.isVisualJump(
            lastLat = lastValidLat, lastLng = lastValidLng,
            newLat = lat, newLng = lng,
            timeDeltaMs = timeDeltaMs, accuracy = accuracy,
            snr = snr,
            lastSpeedMps = lastValidSpeedMps,
            isParking = isParking,
            altitudeDelta = altitudeDelta,
            hasPhysicalMotion = hasPhysicalMotion
        )
        
        var score = jumpConfidence.score

        if (!bypassBehavioral && prevValidLat != 0.0) {
            if (dist > EFFICIENCY_MIN_SEGMENT_DIST) {
                val totalDisplacement = PhysicsUtils.calculateDistance(prevValidLat, prevValidLng, lat, lng)
                val segment1 = PhysicsUtils.calculateDistance(prevValidLat, prevValidLng, lastValidLat, lastValidLng)
                val efficiency = totalDisplacement / (segment1 + dist)
                
                if (efficiency < PATH_EFFICIENCY_THRESHOLD && (segment1 + dist) > EFFICIENCY_MIN_TOTAL_DIST) {
                    score += 40
                }
                
                val gpsBearingDelta = abs(bearing - lastValidBearing).let { if (it > 180) 360 - it else it }
                if (gpsBearingDelta > SCATTER_ANGLE_THRESHOLD && dist > 10.0) {
                    score += 30
                }
            }
        }

        val augmentedScore = score.coerceIn(0, 100)
        val timeDeltaSec = timeDeltaMs / 1000.0
        val currentSpeedMps = dist / max(0.1, timeDeltaSec)
        
        val isTier2 = dist >= JUMP_POINT_DISTANCE_THRESHOLD && (currentSpeedMps > MAX_PHYSICAL_SPEED_MPS || augmentedScore >= 40)
        val isTier3 = dist >= JUMP_GATE_VISUAL_JITTER_METERS && dist < JUMP_POINT_DISTANCE_THRESHOLD && augmentedScore >= 30
        
        val finalTier = when {
            jumpConfidence.tier == 1 -> 1
            isTier2 -> 2
            isTier3 -> 3
            else -> 0
        }
        
        var finalReason = jumpConfidence.reason
        if (finalTier != 0 && finalReason.isEmpty()) {
            finalReason = if (finalTier == 3) "Visual Jitter (Augmented)" else "Jump (Augmented)"
        }

        val finalJumpConfidence = jumpConfidence.copy(
            score = augmentedScore, 
            isJump = finalTier != 0 || augmentedScore >= 50 || jumpConfidence.isJump,
            tier = finalTier,
            reason = finalReason
        )

        if (finalJumpConfidence.isOutlier) return SentinelResult(SentinelStatus.JUMP, finalJumpConfidence.reason, jumpConfidence = finalJumpConfidence)
        
        var behavioralStatus = SentinelStatus.VALID
        var behavioralReason = finalJumpConfidence.reason

        if (finalJumpConfidence.isJump) {
            behavioralStatus = SentinelStatus.JUMP
        }

        if (!bypassBehavioral) {
            if (behavioralStatus == SentinelStatus.JUMP) {
                return SentinelResult(behavioralStatus, finalJumpConfidence.reason, jumpConfidence = finalJumpConfidence)
            }

            val sensorResult = runSensorSentinel(lat, lng, alt, accuracy, bearing, nowRealtime)
            if (sensorResult.status != SentinelStatus.VALID) {
                behavioralStatus = sensorResult.status
                behavioralReason = sensorResult.reason
            }
            
            if (sensorResult.status == SentinelStatus.VALID && sensorResult.suppressionNote != null) {
                val alpha = if (isParking) POSITION_EMA_ALPHA_STATIONARY else POSITION_EMA_ALPHA_DEFAULT
                val optimizedPoint = updateSmoothedPosition(lat, lng, alpha, timestamp)
                updateSmoothedMetrics(currentSpeedMps, bearing)
                updateLastValid(lat, lng, alt, timestamp, currentSpeedMps, bearing)
                return SentinelResult(behavioralStatus, behavioralReason, optimizedPoint = EngineGeoPoint(optimizedPoint.lat, optimizedPoint.lng, ts = timestamp, accuracy = accuracy, maxAccuracy = maxAccuracy), jumpConfidence = finalJumpConfidence, suppressionNote = sensorResult.suppressionNote)
            }
        }

        val alpha = if (behavioralStatus == SentinelStatus.TAMPER) POSITION_EMA_ALPHA_SUSPICIOUS else if (isParking) POSITION_EMA_ALPHA_STATIONARY else POSITION_EMA_ALPHA_DEFAULT

        val optimizedPoint = updateSmoothedPosition(lat, lng, alpha, timestamp)
        updateSmoothedMetrics(currentSpeedMps, bearing)
        updateLastValid(lat, lng, alt, timestamp, currentSpeedMps, bearing)
        return SentinelResult(behavioralStatus, behavioralReason, optimizedPoint = EngineGeoPoint(optimizedPoint.lat, optimizedPoint.lng, ts = timestamp, accuracy = accuracy, maxAccuracy = maxAccuracy), jumpConfidence = finalJumpConfidence)
    }

    private fun updateSmoothedPosition(lat: Double, lng: Double, alpha: Double, ts: Long): EngineGeoPoint {
        if (smoothedLat == 0.0) {
            smoothedLat = lat
            smoothedLng = lng
        } else {
            val dt = (ts - lastValidTs) / 1000.0
            if (dt > POSITION_STALL_RECOVERY_DT_SEC) {
                smoothedLat = lat
                smoothedLng = lng
            } else {
                smoothedLat = PhysicsUtils.smoothCoordinate(smoothedLat, lat, alpha)
                smoothedLng = PhysicsUtils.smoothCoordinate(smoothedLng, lng, alpha)
            }
        }
        return EngineGeoPoint(smoothedLat, smoothedLng, ts = ts)
    }

    private fun updateSmoothedMetrics(speedMps: Double, bearing: Double) {
        smoothedSpeedMps = if (smoothedSpeedMps == 0.0) speedMps else (smoothedSpeedMps * (1.0 - SPEED_EMA_ALPHA)) + (speedMps * SPEED_EMA_ALPHA)
        smoothedBearing = if (smoothedBearing == 0.0) bearing else PhysicsUtils.smoothBearing(smoothedBearing, bearing, BEARING_EMA_ALPHA)
    }

    private fun runSensorSentinel(
        lat: Double, lng: Double, alt: Double, accuracy: Double, bearing: Double,
        nowRealtime: Long
    ): SentinelResult {
        if (!isNear) return SentinelResult(SentinelStatus.TAMPER, "Proximity Far")
        if (isPowerTamper) return SentinelResult(SentinelStatus.TAMPER, "Power disconnected")
        if (SentinelValidator.isTiltViolated(currentTiltDegrees)) return SentinelResult(SentinelStatus.TAMPER, "Tilt detected")
        if (SentinelValidator.isShockViolated(peakVibrationShock, adaptiveVibrationFloor)) return SentinelResult(SentinelStatus.TAMPER, "Shock detected")
        
        if (baroBaseline > -999.0) {
            val liftDelta = currentBaroAlt - baroBaseline
            if (SentinelValidator.isAltitudeViolated(liftDelta)) {
                if (currentVibrationIndex > VIBRATION_STATIONARY_THRESHOLD) {
                    return SentinelResult(SentinelStatus.TAMPER, "Lift detected")
                } else {
                    return SentinelResult(SentinelStatus.TAMPER, "Barometric drift suspicion (No vibration)")
                }
            }
        }
        
        if (SentinelValidator.isLightViolated(currentLux, luxBaseline)) return SentinelResult(SentinelStatus.TAMPER, "Light jump")

        val isAcousticLockedOut = (lastFastPathAcousticSpikeTs > 0 && (nowRealtime - lastFastPathAcousticSpikeTs < ACOUSTIC_LOCKOUT_MS))
        
        if (!isAcousticLockedOut && SentinelValidator.isAcousticViolated(currentAcousticDb, acousticFloorDb, currentVibrationIndex)) {
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

    private fun updateLastValid(lat: Double, lng: Double, alt: Double, ts: Long, speedMps: Double, bearing: Double) {
        prevValidLat = lastValidLat; prevValidLng = lastValidLng
        lastValidLat = lat; lastValidLng = lng; lastValidAlt = alt; lastValidTs = ts
        lastValidSpeedMps = speedMps; lastValidBearing = bearing
    }

    fun reset() {
        lastValidTs = 0L; currentVibrationIndex = 0.0; currentBaroAlt = 0.0; prevValidLat = 0.0; prevValidLng = 0.0
        currentLux = 0.0; isNear = true; isPowerTamper = false; currentTiltDegrees = 0.0
        tiltBaseline = 0.0
        currentAcousticDb = 0.0; luxBaseline = -1.0; baroBaseline = -1000.0; acousticFloorDb = -1.0
        adaptiveVibrationFloor = INITIAL_VIBRATION_FLOOR; peakVibrationShock = 0.0; peakVibrationShockTs = 0L
        lastAcousticContractionRealtime = 0L
        stationaryStartTs = 0L
        gpsMotionStartTs = 0L
        lastFastPathAcousticSpikeTs = 0L
        smoothedLat = 0.0; smoothedLng = 0.0; smoothedSpeedMps = 0.0; smoothedBearing = 0.0
    }
}
