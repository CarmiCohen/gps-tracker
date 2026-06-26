package com.gps19.core.engine

import java.util.Locale
import kotlin.math.*

/**
 * LocationSentinel: A multi-layered location validation engine.
 * v8.9.34:
 * - Issue #304: Corrected Tier 3 Jump Floor. Now uses JUMP_GATE_VISUAL_JITTER_METERS (10.0m).
 * - Issue #324: Lux EMA Implementation. Integrated Slow/Fast variants for rising/falling light. (Legacy-#366 / #266)
 * v8.9.31:
 * - Issue #292: Acoustic Floor Decay Logic. Enforced ACOUSTIC_FLOOR_MIN_DB.
 */
class LocationSentinel {

    private val immFilter = ImmFilter()
    private val gtoEngine = GtoEngine()

    private var prevValidLat: Double = 0.0
    private var prevValidLng: Double = 0.0
    
    private var lastValidLat: Double = 0.0
    private var lastValidLng: Double = 0.0
    private var lastValidAlt: Double = 0.0
    private var lastValidTs: Long = 0L
    private var lastValidSpeedMps: Double = 0.0
    private var lastValidBearing: Float = 0.0f

    internal var currentVibrationIndex: Float = 0f
    var peakVibrationShock: Float = 0f
        private set
    var peakVibrationShockTs: Long = 0L
        private set

    private var currentCompassHeading: Float = 0f
    private var lastCompassHeading: Float = 0f
    private var currentBaroAlt: Float = 0f
    private var currentLux: Float = 0f
    private var isNear: Boolean = true
    private var isPowerTamper: Boolean = false
    var currentTiltDegrees: Float = 0f
        private set
    var currentAcousticDb: Double = 0.0
        private set
    private var lastFastPathAcousticSpikeTs: Long = 0L

    // Sit Detection State
    var isSitDetected: Boolean = false
        private set
        
    var lastSitTs: Long = 0L 
    var lastSitRealtime: Long = 0L 
    var baselineSitTilt: Float = -1f
    
    var lastSitVz: Float = 0f
    var lastSitVzTs: Long = 0L
    var lastSitDz: Float = 0f
    var lastSitBaro: Float = 0f
    var lastSitTilt: Float = 0f
    var lastSitShock: Float = 0f

    private var sitDetectionCooldownTs: Long = 0L 
    private var stationaryStartTs: Long = 0L 

    // Tractor-Slow state
    private var gpsMotionStartTs: Long = 0L 

    // Dynamic Baselines
    var luxBaseline: Float = -1f
        private set
    var baroBaseline: Float = -1000f
        private set
    var acousticFloorDb: Double = -1.0
        private set
    var adaptiveVibrationFloor: Float = INITIAL_VIBRATION_FLOOR
        internal set
        
    private var lastAcousticContractionRealtime: Long = 0L

    private var lastSnr: Float = 0f
    private var lastSatsUsed: Int = 0

    private fun safeF(v: Float): Float = if (v.isNaN() || v.isInfinite()) 0f else v
    private fun safeD(v: Double): Double = if (v.isNaN() || v.isInfinite()) 0.0 else v

    fun setSpatialAnchor(lat: Double, lng: Double, alt: Double, timestamp: Long) {
        lastValidLat = lat; lastValidLng = lng; lastValidAlt = alt; lastValidTs = timestamp
        immFilter.update(lat, lng, 10f, timestamp) 
    }

    fun updateSensorState(
        vibration: Float, 
        heading: Float, 
        baroAlt: Float, 
        lux: Float = 0f, 
        isNear: Boolean = true, 
        powerTamper: Boolean = false,
        tiltDegrees: Float = 0f,
        acousticDb: Double = 0.0,
        peakShock: Float = 0f,
        acousticMinDb: Double = -1.0,
        peakVerticalVelocity: Float = 0f,
        peakVerticalVelocityTs: Long = 0L,
        plungeMatched: Boolean = false,
        peakVerticalDisplacement: Float = 0f,
        isSirenActive: Boolean = false,
        isWarming: Boolean = false,
        manualAdaptiveFloor: Float = -1f,
        acousticLockoutTs: Long = 0L,
        isMuzzled: Boolean = false,
        nowRealtime: Long,
        nowWall: Long
    ): Boolean {
        var baselineChanged = false
        
        this.lastCompassHeading = this.currentCompassHeading
        this.currentVibrationIndex = safeF(vibration)
        this.lastFastPathAcousticSpikeTs = acousticLockoutTs
        
        if (peakShock > this.peakVibrationShock && !peakShock.isNaN() && !isMuzzled) {
            this.peakVibrationShock = peakShock
            this.peakVibrationShockTs = nowWall
        }

        if (baselineSitTilt < -500f || baselineSitTilt == -1f) {
            baselineSitTilt = safeF(tiltDegrees)
            baselineChanged = true
        } else {
            val tiltDelta = abs(safeF(tiltDegrees) - baselineSitTilt)
            val baroDelta = if (baroBaseline > -999) abs(safeF(baroAlt) - baroBaseline) else 0f
            
            if (nowRealtime > sitDetectionCooldownTs && !isMuzzled) {
                val isSpatialTriggered = (tiltDelta > CHAIR_SIT_TILT_THRESHOLD) || 
                                         (baroDelta > CHAIR_SIT_BARO_THRESHOLD) || 
                                         plungeMatched
                
                if (isSpatialTriggered && (peakShock > CHAIR_SIT_VIBRATION_THRESHOLD || plungeMatched)) {
                    isSitDetected = true
                    lastSitTs = nowWall
                    lastSitRealtime = nowRealtime
                    sitDetectionCooldownTs = nowRealtime + CHAIR_SIT_COOLDOWN_MS
                    
                    lastSitVz = safeF(peakVerticalVelocity)
                    lastSitVzTs = peakVerticalVelocityTs
                    lastSitDz = safeF(peakVerticalDisplacement)
                    lastSitBaro = safeF(baroDelta)
                    lastSitTilt = safeF(tiltDelta)
                    lastSitShock = safeF(peakShock)
                }
            }

            if (isStationary() && !isSitDetected) {
                if (stationaryStartTs == 0L) stationaryStartTs = nowRealtime
                else if (nowRealtime - stationaryStartTs > PASSIVE_ZEROING_STATIONARY_MS) {
                    if (abs(baselineSitTilt - tiltDegrees) > 0.1f && !tiltDegrees.isNaN()) {
                        baselineSitTilt = tiltDegrees
                        baselineChanged = true
                    }
                    stationaryStartTs = 0L
                }
            } else {
                stationaryStartTs = 0L
            }
        }

        this.currentCompassHeading = safeF(heading)
        this.currentBaroAlt = safeF(baroAlt)
        this.currentLux = safeF(lux)
        this.isNear = isNear
        this.isPowerTamper = powerTamper
        this.currentTiltDegrees = safeF(tiltDegrees)
        this.currentAcousticDb = safeD(acousticDb)

        if (luxBaseline < 0) {
            if (!lux.isNaN()) luxBaseline = lux
        } else {
            if (!lux.isNaN()) {
                // Issue #324: Use rising/falling EMA factors from EngineConstants (Legacy-#366 / #266)
                val baseAlpha = if (lux < luxBaseline) {
                    if (isStationary()) LUX_EMA_DOWN_SLOW else LUX_EMA_DOWN_FAST
                } else {
                    if (isStationary()) LUX_EMA_UP_SLOW else LUX_EMA_UP_FAST
                }
                val alpha = SentinelValidator.accelerateAlpha(baseAlpha, isWarming)
                luxBaseline = (luxBaseline * (1f - alpha)) + (lux * alpha)
            }
        }
        
        if (baroBaseline < -999) {
            if (!baroAlt.isNaN()) baroBaseline = baroAlt
        } else {
            if (!baroAlt.isNaN()) {
                val alpha = SentinelValidator.accelerateAlpha(BARO_EMA_SLOW, isWarming)
                baroBaseline = (baroBaseline * (1f - alpha)) + (baroAlt * alpha)
            }
        }

        if (!isSirenActive) {
            val updateDb = if (acousticMinDb >= 0.0) acousticMinDb else if (acousticMinDb == -1.0 && acousticDb >= 0.0) acousticDb else -1.0
            
            if (updateDb >= 0.0 && !updateDb.isNaN()) {
                if (acousticFloorDb < 0) {
                    acousticFloorDb = max(updateDb, ACOUSTIC_FLOOR_MIN_DB)
                } else if (updateDb < acousticFloorDb) {
                    val alpha = SentinelValidator.accelerateAlpha(ACOUSTIC_EMA_DOWN_FAST, isWarming)
                    acousticFloorDb = (acousticFloorDb * (1f - alpha)) + (updateDb * alpha) 
                } else {
                    val alpha = SentinelValidator.accelerateAlpha(ACOUSTIC_EMA_UP_FAST, isWarming)
                    acousticFloorDb = (acousticFloorDb * (1f - alpha)) + (updateDb * alpha)
                }
                if (acousticFloorDb < ACOUSTIC_FLOOR_MIN_DB) acousticFloorDb = ACOUSTIC_FLOOR_MIN_DB
            }
            
            val contractionElapsed = nowRealtime - lastAcousticContractionRealtime
            if (contractionElapsed >= 500 || lastAcousticContractionRealtime == 0L) {
                if (acousticFloorDb > ACOUSTIC_FLOOR_MIN_DB && lastAcousticContractionRealtime > 0) {
                    val secondsPassed = contractionElapsed / 1000.0
                    if (secondsPassed > 0) {
                        val decayFactor = Math.pow(ACOUSTIC_FLOOR_CONTRACTION_EMA.toDouble(), secondsPassed)
                        acousticFloorDb = max(acousticFloorDb * decayFactor, ACOUSTIC_FLOOR_MIN_DB)
                    }
                }
                lastAcousticContractionRealtime = nowRealtime
            }
        }
        
        if (manualAdaptiveFloor >= 0f) {
            this.adaptiveVibrationFloor = manualAdaptiveFloor
        } else if (!isMuzzled) { 
            this.adaptiveVibrationFloor = SentinelValidator.updateVibrationFloor(this.adaptiveVibrationFloor, vibration, isWarming)
        }
        
        return baselineChanged
    }

    fun consumeSitDetected(): Boolean {
        val result = isSitDetected
        isSitDetected = false
        return result
    }

    fun resetChairBaseline() {
        baselineSitTilt = -1f
    }

    fun getEstimatedSpeedKph(): Double = immFilter.getEstimatedSpeedKph()
    fun getEstimatedBearing(): Float = immFilter.getEstimatedBearing()
    fun getStationaryProbability(): Double = immFilter.getStationaryProbability()

    fun getHindsightBuffer(): List<RejectedPoint> = gtoEngine.getWindow().map {
        RejectedPoint(it.lat, it.lng, it.alt, it.accuracy, it.bearing, it.speedMps, it.ts)
    }

    fun processLocation(
        lat: Double, lng: Double, alt: Double, accuracy: Float, bearing: Float,
        snr: Float, satsUsed: Int, timestamp: Long, 
        bypassBehavioral: Boolean = false,
        isSuspicious: Boolean = false,
        isMuzzled: Boolean = false, 
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
            val optimized = immFilter.update(lat, lng, accuracy, timestamp, 1.0)
            return SentinelResult(SentinelStatus.VALID, optimizedPoint = optimized)
        }

        val timeDeltaMs = timestamp - lastValidTs
        if (timeDeltaMs <= 0) return SentinelResult(SentinelStatus.VALID) 
        
        val altitudeDelta = if (lastValidAlt != 0.0) alt - lastValidAlt else 0.0
        val stationaryProb = getStationaryProbability()
        val isParking = stationaryProb > 0.8
        
        val dist = PhysicsUtils.calculateDistance(lastValidLat, lastValidLng, lat, lng)
        val impliesMotion = dist > ACTIVE_MOVE_THRESHOLD
        
        if (impliesMotion) {
            if (gpsMotionStartTs == 0L) gpsMotionStartTs = nowRealtime
        } else {
            gpsMotionStartTs = 0L
        }
        
        val isTractorSlowOverride = gpsMotionStartTs > 0 && (nowRealtime - gpsMotionStartTs > 10000L)
        val hasPhysicalMotion = if (isMuzzled) false else (currentVibrationIndex > (adaptiveVibrationFloor * 1.5f) || isTractorSlowOverride)

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

        if (finalJumpConfidence.isOutlier) return SentinelResult(SentinelStatus.OUTLIER, finalJumpConfidence.reason, jumpConfidence = finalJumpConfidence)
        
        var behavioralStatus = SentinelStatus.VALID
        var behavioralReason = finalJumpConfidence.reason

        if (finalJumpConfidence.isJump) {
            behavioralStatus = if (finalJumpConfidence.tier == 3) SentinelStatus.JITTER else SentinelStatus.JUMP
        }

        if (!bypassBehavioral) {
            if (gtoEngine.evaluateTrajectory(lat, lng, bearing, currentSpeedMps, timestamp)) {
                val promoted = mutableListOf<EngineGeoPoint>()
                gtoEngine.getWindow().forEach { p ->
                    val opt = immFilter.update(p.lat, p.lng, p.accuracy, p.ts, SUSPICIOUS_Q_SCALE)
                    promoted.add(opt)
                    updateLastValid(p.lat, p.lng, p.alt, p.ts, p.speedMps, p.bearing)
                }
                gtoEngine.clear()
                val optimized = immFilter.update(lat, lng, accuracy, timestamp, SUSPICIOUS_Q_SCALE)
                updateLastValid(lat, lng, alt, timestamp, currentSpeedMps, bearing)
                return SentinelResult(SentinelStatus.TRAJECTORY_PROMOTED, "Trajectory Promoted (GTO)", optimized, finalJumpConfidence, promotedPoints = promoted)
            }

            if (behavioralStatus == SentinelStatus.JUMP || behavioralStatus == SentinelStatus.JITTER) {
                gtoEngine.addPoint(lat, lng, alt, accuracy, bearing, currentSpeedMps, timestamp, currentVibrationIndex)
                return SentinelResult(behavioralStatus, finalJumpConfidence.reason, jumpConfidence = finalJumpConfidence)
            }

            val sensorSentinel = runSensorSentinel(lat, lng, alt, accuracy, bearing, nowRealtime, isMuzzled)
            if (sensorSentinel.status != SentinelStatus.VALID) {
                behavioralStatus = sensorSentinel.status
                behavioralReason = sensorSentinel.reason
            }
        }

        val effectiveQScale = if (isSuspicious || 
                                 behavioralStatus == SentinelStatus.TAMPER_ALERT || 
                                 behavioralStatus == SentinelStatus.ACOUSTIC_WARNING || 
                                 behavioralStatus == SentinelStatus.SENSOR_SUSPICIOUS) SUSPICIOUS_Q_SCALE else 1.0

        val optimizedPoint = immFilter.update(lat, lng, accuracy, timestamp, effectiveQScale)
        updateLastValid(lat, lng, alt, timestamp, currentSpeedMps, bearing)
        gtoEngine.clear()
        return SentinelResult(behavioralStatus, behavioralReason, optimizedPoint = optimizedPoint, jumpConfidence = finalJumpConfidence)
    }

    private fun runSensorSentinel(
        lat: Double, lng: Double, alt: Double, accuracy: Float, bearing: Float,
        nowRealtime: Long,
        isMuzzled: Boolean = false 
    ): SentinelResult {
        if (!isMuzzled) {
            if (!isNear) return SentinelResult(SentinelStatus.TAMPER_ALERT, "Proximity Far")
            if (isPowerTamper) return SentinelResult(SentinelStatus.TAMPER_ALERT, "Power disconnected")
            if (SentinelValidator.isTiltViolated(currentTiltDegrees)) return SentinelResult(SentinelStatus.TAMPER_ALERT, "Tilt detected")
            if (SentinelValidator.isShockViolated(peakVibrationShock, adaptiveVibrationFloor)) return SentinelResult(SentinelStatus.TAMPER_ALERT, "Shock detected")
            
            if (baroBaseline > -999) {
                val liftDelta = currentBaroAlt - baroBaseline
                if (SentinelValidator.isAltitudeViolated(liftDelta)) {
                    if (currentVibrationIndex > VIBRATION_STATIONARY_THRESHOLD) {
                        return SentinelResult(SentinelStatus.TAMPER_ALERT, "Lift detected")
                    } else {
                        return SentinelResult(SentinelStatus.SENSOR_SUSPICIOUS, "Barometric drift suspicion (No vibration)")
                    }
                }
            }
        }
        
        if (!isMuzzled && SentinelValidator.isLightViolated(currentLux, luxBaseline)) return SentinelResult(SentinelStatus.TAMPER_ALERT, "Light jump")

        val isAcousticLockedOut = (lastFastPathAcousticSpikeTs > 0 && (nowRealtime - lastFastPathAcousticSpikeTs < ACOUSTIC_LOCKOUT_MS))
        if (!isMuzzled && !isAcousticLockedOut && SentinelValidator.isAcousticViolated(currentAcousticDb, acousticFloorDb)) {
            return SentinelResult(SentinelStatus.TAMPER_ALERT, "Acoustic alarm")
        }

        if (!isMuzzled && SentinelValidator.isVibrationSuspicious(currentVibrationIndex, adaptiveVibrationFloor)) {
            return SentinelResult(SentinelStatus.SENSOR_SUSPICIOUS, "Vibration suspicion")
        }
        
        if (!isMuzzled && !isAcousticLockedOut && SentinelValidator.isAcousticSuspicious(currentAcousticDb, acousticFloorDb)) {
            return SentinelResult(SentinelStatus.ACOUSTIC_WARNING, "Acoustic suspicion")
        }

        return SentinelResult(SentinelStatus.VALID)
    }

    fun isStationary(): Boolean = SentinelValidator.isStationary(currentVibrationIndex, adaptiveVibrationFloor)

    fun shouldThrottlePolling(providedIsStationary: Boolean? = null): Boolean {
        val stationary = providedIsStationary ?: isStationary()
        return stationary &&
               abs(currentCompassHeading - lastCompassHeading) < THROTTLE_COMPASS_LIMIT &&
               (if (baroBaseline > -999) abs(currentBaroAlt - baroBaseline) < THROTTLE_BARO_LIMIT else true) &&
               isNear && (currentLux - luxBaseline < THROTTLE_LUX_LIMIT) && !isPowerTamper &&
               currentTiltDegrees < THROTTLE_TILT_LIMIT && (currentAcousticDb - acousticFloorDb < THROTTLE_ACOUSTIC_LIMIT)
    }

    private fun updateLastValid(lat: Double, lng: Double, alt: Double, ts: Long, speedMps: Double, bearing: Float) {
        prevValidLat = lastValidLat; prevValidLng = lastValidLng
        lastValidLat = lat; lastValidLng = lng; lastValidAlt = alt; lastValidTs = ts
        lastValidSpeedMps = speedMps; lastValidBearing = bearing
    }

    fun reset() {
        lastValidTs = 0L; currentVibrationIndex = 0f; currentBaroAlt = 0f; prevValidLat = 0.0; prevValidLng = 0.0
        currentLux = 0f; isNear = true; isPowerTamper = false; currentTiltDegrees = 0f
        currentAcousticDb = 0.0; luxBaseline = -1f; baroBaseline = -1000f; acousticFloorDb = -1.0
        adaptiveVibrationFloor = INITIAL_VIBRATION_FLOOR; peakVibrationShock = 0f; peakVibrationShockTs = 0L
        lastAcousticContractionRealtime = 0L
        isSitDetected = false; lastSitTs = 0L; lastSitRealtime = 0L; baselineSitTilt = -1f; sitDetectionCooldownTs = 0L; stationaryStartTs = 0L
        lastSitVz = 0f; lastSitVzTs = 0L; lastSitDz = 0f; lastSitBaro = 0f; lastSitTilt = 0f; lastSitShock = 0f
        gpsMotionStartTs = 0L
        lastFastPathAcousticSpikeTs = 0L
        gtoEngine.clear()
        immFilter.reset()
    }
}
