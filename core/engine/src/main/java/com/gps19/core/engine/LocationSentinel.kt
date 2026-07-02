package com.gps19.core.engine

import java.util.Locale
import kotlin.math.*

/**
 * LocationSentinel: A multi-layered location validation engine.
 * v8.9.75:
 * - Issue #014: Type Safety Optimization. Standardized internal state to Double 
 *   to eliminate redundant toDouble()/toFloat() conversions.
 * v8.9.68:
 * - Issue #011: Implemented suppressionNote generation for forensic transparency 
 *   when A15-specific muzzles (coherence, threshold) are triggered.
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
    private var lastValidBearing: Double = 0.0

    internal var currentVibrationIndex: Double = 0.0
    var peakVibrationShock: Double = 0.0
        private set
    var peakVibrationShockTs: Long = 0L
        private set

    private var currentCompassHeading: Double = 0.0
    private var lastCompassHeading: Double = 0.0
    private var currentBaroAlt: Double = 0.0
    private var currentLux: Double = 0.0
    private var isNear: Boolean = true
    private var isPowerTamper: Boolean = false
    var currentTiltDegrees: Double = 0.0
        private set
    var currentAcousticDb: Double = 0.0
        private set
    private var lastFastPathAcousticSpikeTs: Long = 0L

    // Sit Detection State
    var isSitDetected: Boolean = false
        private set
        
    var lastSitTs: Long = 0L 
    var lastSitRealtime: Long = 0L 
    var baselineSitTilt: Double = -1.0
    
    var lastSitVz: Double = 0.0
    var lastSitVzTs: Long = 0L
    var lastSitDz: Double = 0.0
    var lastSitBaro: Double = 0.0
    var lastSitTilt: Double = 0.0
    var lastSitShock: Double = 0.0

    private var sitDetectionCooldownTs: Long = 0L 
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
        immFilter.update(lat, lng, 10.0, timestamp) 
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
        isMuzzled: Boolean = false,
        isA15: Boolean = false,
        nowRealtime: Long,
        nowWall: Long
    ): Boolean {
        var baselineChanged = false
        
        this.lastCompassHeading = this.currentCompassHeading
        this.currentVibrationIndex = safeD(vibration)
        this.lastFastPathAcousticSpikeTs = acousticLockoutTs
        
        if (peakShock > this.peakVibrationShock && !peakShock.isNaN() && !isMuzzled) {
            this.peakVibrationShock = peakShock
            this.peakVibrationShockTs = nowWall
        }

        if (baselineSitTilt < -500.0 || baselineSitTilt == -1.0) {
            baselineSitTilt = safeD(tiltDegrees)
            baselineChanged = true
        } else {
            val tiltDelta = abs(safeD(tiltDegrees) - baselineSitTilt)
            val baroDelta = if (baroBaseline > -999.0) abs(safeD(baroAlt) - baroBaseline) else 0.0
            
            if (nowRealtime > sitDetectionCooldownTs && !isMuzzled) {
                val isSpatialTriggered = (tiltDelta > CHAIR_SIT_TILT_THRESHOLD) || 
                                         (baroDelta > CHAIR_SIT_BARO_THRESHOLD) || 
                                         plungeMatched
                
                if (isSpatialTriggered && (peakShock > CHAIR_SIT_VIBRATION_THRESHOLD || plungeMatched)) {
                    isSitDetected = true
                    lastSitTs = nowWall
                    lastSitRealtime = nowRealtime
                    sitDetectionCooldownTs = nowRealtime + CHAIR_SIT_COOLDOWN_MS
                    
                    lastSitVz = safeD(peakVerticalVelocity)
                    lastSitVzTs = peakVerticalVelocityTs
                    lastSitDz = safeD(peakVerticalDisplacement)
                    lastSitBaro = safeD(baroDelta)
                    lastSitTilt = safeD(tiltDelta)
                    lastSitShock = safeD(peakShock)
                }
            }

            if (isStationary() && !isSitDetected) {
                if (stationaryStartTs == 0L) stationaryStartTs = nowRealtime
                else if (nowRealtime - stationaryStartTs > PASSIVE_ZEROING_STATIONARY_MS) {
                    if (abs(baselineSitTilt - tiltDegrees) > 0.1 && !tiltDegrees.isNaN()) {
                        baselineSitTilt = tiltDegrees
                        baselineChanged = true
                    }
                    stationaryStartTs = 0L
                }
            } else {
                stationaryStartTs = 0L
            }
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
                    if (isStationary()) LUX_EMA_UP_SLOW else if (isA15) LUX_EMA_UP_FAST_A15 else LUX_EMA_UP_FAST
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
                        val decayFactor = Math.pow(ACOUSTIC_FLOOR_CONTRACTION_EMA.toDouble(), secondsPassed)
                        acousticFloorDb = max(acousticFloorDb * decayFactor, ACOUSTIC_FLOOR_MIN_DB)
                    }
                }
                lastAcousticContractionRealtime = nowRealtime
            }
        }
        
        if (manualAdaptiveFloor >= 0.0) {
            this.adaptiveVibrationFloor = manualAdaptiveFloor
        } else if (!isMuzzled) { 
            this.adaptiveVibrationFloor = SentinelValidator.updateVibrationFloor(this.adaptiveVibrationFloor.toFloat(), vibration.toFloat(), isWarming).toDouble()
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

    fun getEstimatedSpeedKph(): Double = immFilter.getEstimatedSpeedKph()
    fun getEstimatedBearing(): Float = immFilter.getEstimatedBearing()
    fun getStationaryProbability(): Double = immFilter.getStationaryProbability()

    fun getHindsightBuffer(): List<RejectedPoint> = gtoEngine.getWindow().map {
        RejectedPoint(it.lat, it.lng, it.alt, it.accuracy, it.bearing, it.speedMps, it.ts)
    }

    fun processLocation(
        lat: Double, lng: Double, alt: Double, accuracy: Double, 
        maxAccuracy: Double, 
        bearing: Double,
        snr: Double, satsUsed: Int, timestamp: Long, 
        bypassBehavioral: Boolean = false,
        isSuspicious: Boolean = false,
        isMuzzled: Boolean = false, 
        isA15: Boolean = false,
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
            return SentinelResult(SentinelStatus.VALID, optimizedPoint = optimized.copy(accuracy = accuracy, maxAccuracy = maxAccuracy))
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
        val hasPhysicalMotion = if (isMuzzled) false else (currentVibrationIndex > (adaptiveVibrationFloor * 1.5) || isTractorSlowOverride)

        val jumpConfidence = PhysicsUtils.isVisualJump(
            lastLat = lastValidLat, lastLng = lastValidLng,
            newLat = lat, newLng = lng,
            timeDeltaMs = timeDeltaMs, accuracy = accuracy.toFloat(),
            snr = snr.toFloat(),
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
                    promoted.add(opt.copy(accuracy = p.accuracy, maxAccuracy = p.maxAccuracy))
                    updateLastValid(p.lat, p.lng, p.alt, p.ts, p.speedMps, p.bearing)
                }
                gtoEngine.clear()
                val optimized = immFilter.update(lat, lng, accuracy, timestamp, SUSPICIOUS_Q_SCALE)
                updateLastValid(lat, lng, alt, timestamp, currentSpeedMps, bearing)
                return SentinelResult(SentinelStatus.TRAJECTORY_PROMOTED, "Trajectory Promoted (GTO)", optimized.copy(accuracy = accuracy, maxAccuracy = maxAccuracy), finalJumpConfidence, promotedPoints = promoted)
            }

            if (behavioralStatus == SentinelStatus.JUMP || behavioralStatus == SentinelStatus.JITTER) {
                gtoEngine.addPoint(lat, lng, alt, accuracy, maxAccuracy, bearing, currentSpeedMps, timestamp, currentVibrationIndex)
                return SentinelResult(behavioralStatus, finalJumpConfidence.reason, jumpConfidence = finalJumpConfidence)
            }

            val sensorSentinel = runSensorSentinel(lat, lng, alt, accuracy, bearing, nowRealtime, isMuzzled, isA15)
            if (sensorSentinel.status != SentinelStatus.VALID) {
                behavioralStatus = sensorSentinel.status
                behavioralReason = sensorSentinel.reason
            }
            
            if (sensorSentinel.status == SentinelStatus.VALID && sensorSentinel.suppressionNote != null) {
                val effectiveQScale = if (isSuspicious) SUSPICIOUS_Q_SCALE else 1.0
                val optimizedPoint = immFilter.update(lat, lng, accuracy, timestamp, effectiveQScale)
                updateLastValid(lat, lng, alt, timestamp, currentSpeedMps, bearing)
                gtoEngine.clear()
                return SentinelResult(behavioralStatus, behavioralReason, optimizedPoint = optimizedPoint.copy(accuracy = accuracy, maxAccuracy = maxAccuracy), jumpConfidence = finalJumpConfidence, suppressionNote = sensorSentinel.suppressionNote)
            }
        }

        val effectiveQScale = if (isSuspicious || 
                                 behavioralStatus == SentinelStatus.TAMPER_ALERT || 
                                 behavioralStatus == SentinelStatus.ACOUSTIC_WARNING || 
                                 behavioralStatus == SentinelStatus.SENSOR_SUSPICIOUS) SUSPICIOUS_Q_SCALE else 1.0

        val optimizedPoint = immFilter.update(lat, lng, accuracy, timestamp, effectiveQScale)
        updateLastValid(lat, lng, alt, timestamp, currentSpeedMps, bearing)
        gtoEngine.clear()
        return SentinelResult(behavioralStatus, behavioralReason, optimizedPoint = optimizedPoint.copy(accuracy = accuracy, maxAccuracy = maxAccuracy), jumpConfidence = finalJumpConfidence)
    }

    private fun runSensorSentinel(
        lat: Double, lng: Double, alt: Double, accuracy: Double, bearing: Double,
        nowRealtime: Long,
        isMuzzled: Boolean = false,
        isA15: Boolean = false
    ): SentinelResult {
        if (!isMuzzled) {
            if (!isNear) return SentinelResult(SentinelStatus.TAMPER_ALERT, "Proximity Far")
            if (isPowerTamper) return SentinelResult(SentinelStatus.TAMPER_ALERT, "Power disconnected")
            if (SentinelValidator.isTiltViolated(currentTiltDegrees.toFloat())) return SentinelResult(SentinelStatus.TAMPER_ALERT, "Tilt detected")
            if (SentinelValidator.isShockViolated(peakVibrationShock.toFloat(), adaptiveVibrationFloor.toFloat())) return SentinelResult(SentinelStatus.TAMPER_ALERT, "Shock detected")
            
            if (baroBaseline > -999.0) {
                val liftDelta = currentBaroAlt - baroBaseline
                if (SentinelValidator.isAltitudeViolated(liftDelta.toFloat())) {
                    if (currentVibrationIndex > VIBRATION_STATIONARY_THRESHOLD) {
                        return SentinelResult(SentinelStatus.TAMPER_ALERT, "Lift detected")
                    } else {
                        return SentinelResult(SentinelStatus.SENSOR_SUSPICIOUS, "Barometric drift suspicion (No vibration)")
                    }
                }
            }
        }
        
        if (!isMuzzled && SentinelValidator.isLightViolated(currentLux.toFloat(), luxBaseline.toFloat())) return SentinelResult(SentinelStatus.TAMPER_ALERT, "Light jump")

        val isAcousticLockedOut = (lastFastPathAcousticSpikeTs > 0 && (nowRealtime - lastFastPathAcousticSpikeTs < ACOUSTIC_LOCKOUT_MS))
        
        if (isA15 && !isMuzzled && !isAcousticLockedOut) {
            val jump = currentAcousticDb - acousticFloorDb
            val rawThreshold = ACOUSTIC_THRESHOLD_DB_JUMP
            val hardenedThreshold = ACOUSTIC_THRESHOLD_DB_JUMP_A15
            
            if (jump > rawThreshold && currentAcousticDb >= ACOUSTIC_MIN_THRESHOLD_DB) {
                if (jump <= hardenedThreshold) {
                    return SentinelResult(SentinelStatus.VALID, suppressionNote = "Acoustic spike (${String.format(Locale.getDefault(), "%.1f", currentAcousticDb)}dB) muzzled by A15 hardware profile.")
                }
                if (currentVibrationIndex < 0.01) {
                    return SentinelResult(SentinelStatus.VALID, suppressionNote = "Acoustic alert (${String.format(Locale.getDefault(), "%.1f", currentAcousticDb)}dB) suppressed on A15 due to vibration incoherence.")
                }
            }
        }

        if (!isMuzzled && !isAcousticLockedOut && SentinelValidator.isAcousticViolated(currentAcousticDb, acousticFloorDb, isA15, currentVibrationIndex.toFloat())) {
            return SentinelResult(SentinelStatus.TAMPER_ALERT, "Acoustic alarm")
        }

        if (!isMuzzled && SentinelValidator.isVibrationSuspicious(currentVibrationIndex.toFloat(), adaptiveVibrationFloor.toFloat())) {
            return SentinelResult(SentinelStatus.SENSOR_SUSPICIOUS, "Vibration suspicion")
        }
        
        if (!isMuzzled && !isAcousticLockedOut && SentinelValidator.isAcousticSuspicious(currentAcousticDb, acousticFloorDb, isA15, currentVibrationIndex.toFloat())) {
            return SentinelResult(SentinelStatus.ACOUSTIC_WARNING, "Acoustic suspicion")
        }

        return SentinelResult(SentinelStatus.VALID)
    }

    fun isStationary(): Boolean = SentinelValidator.isStationary(currentVibrationIndex.toFloat(), adaptiveVibrationFloor.toFloat())

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
        currentAcousticDb = 0.0; luxBaseline = -1.0; baroBaseline = -1000.0; acousticFloorDb = -1.0
        adaptiveVibrationFloor = INITIAL_VIBRATION_FLOOR; peakVibrationShock = 0.0; peakVibrationShockTs = 0L
        lastAcousticContractionRealtime = 0L
        isSitDetected = false; lastSitTs = 0L; lastSitRealtime = 0L; baselineSitTilt = -1.0; sitDetectionCooldownTs = 0L; stationaryStartTs = 0L
        lastSitVz = 0.0; lastSitVzTs = 0L; lastSitDz = 0.0; lastSitBaro = 0.0; lastSitTilt = 0.0; lastSitShock = 0.0
        gpsMotionStartTs = 0L
        lastFastPathAcousticSpikeTs = 0L
        gtoEngine.clear()
        immFilter.reset()
    }
}
