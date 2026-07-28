package com.gps19.core.engine

import kotlin.math.abs
import kotlin.math.max

/**
 * SentinelValidator: Centralized "Sentinel Hard Gates" and baseline logic.
 * July.27.06:
 * - Issue #601: Kinetic Energy Anomaly Detection. Added updateKineticEnergy 
 *   to distinguish sustained motion from impulse events using HPF.
 * July.26.04:
 * - Architecture Simplification (Issue #588): Centralized EMA baseline update logic 
 *   to reduce code churn in LocationSentinel. Fixed accelerAlpha typo.
 */
object SentinelValidator {

    fun isTiltViolated(tiltDegrees: Double): Boolean {
        return tiltDegrees > TILT_THRESHOLD_DEGREES
    }

    fun isAltitudeViolated(relativeAltitude: Double): Boolean {
        return abs(relativeAltitude) > BARO_LIFT_THRESHOLD_METERS
    }
    
    fun isLiftViolated(relativeAltitude: Double): Boolean = isAltitudeViolated(relativeAltitude)

    fun isShockViolated(peakShock: Double, adaptiveFloor: Double = INITIAL_VIBRATION_FLOOR): Boolean {
        val dynamicThreshold = maxOf(VIBRATION_SHOCK_THRESHOLD_G, adaptiveFloor * VIBRATION_SHOCK_MULTIPLIER)
        return peakShock > dynamicThreshold
    }

    fun isVibrationSuspicious(vibration: Double, adaptiveFloor: Double = INITIAL_VIBRATION_FLOOR): Boolean {
        val dynamicThreshold = maxOf(VIBRATION_SUSPICIOUS_THRESHOLD_G, adaptiveFloor * VIBRATION_SUSPICIOUS_MULTIPLIER)
        return vibration > dynamicThreshold
    }

    fun isStationary(vibration: Double, adaptiveFloor: Double): Boolean {
        val dynamicGate = (adaptiveFloor * STATIONARY_FLOOR_MULT).coerceIn(INITIAL_VIBRATION_FLOOR, VIBRATION_STATIONARY_THRESHOLD)
        return vibration < dynamicGate
    }

    fun isAcousticViolated(peakDb: Double, floorDb: Double, vibration: Double = 0.0): Boolean {
        if (floorDb < 0.0) return false
        val jump = peakDb - floorDb
        val threshold = ACOUSTIC_THRESHOLD_DB_JUMP
        
        return jump > threshold && peakDb >= ACOUSTIC_MIN_THRESHOLD_DB
    }

    fun isAcousticSuspicious(peakDb: Double, floorDb: Double, vibration: Double = 0.0): Boolean {
        if (floorDb < 0.0) return false
        val jump = peakDb - floorDb
        val threshold = ACOUSTIC_SUSPICIOUS_THRESHOLD_DB_JUMP

        return jump > threshold && peakDb >= ACOUSTIC_MIN_THRESHOLD_DB
    }

    fun isLightViolated(lux: Double, luxBaseline: Double): Boolean {
        if (luxBaseline < 0.0) return false
        return (lux - luxBaseline) > LIGHT_THRESHOLD_LUX_JUMP
    }

    /**
     * R730: Unified Vibration Floor Update (EMA).
     */
    fun updateVibrationFloor(currentFloor: Double, vibration: Double, isWarming: Boolean): Double {
        if (vibration.isNaN() || vibration <= 0.0) return currentFloor
        
        return if (vibration < currentFloor) {
            val alpha = accelerateAlpha(VIBRATION_EMA_DOWN_FAST, isWarming, 0.5)
            applyEma(currentFloor, vibration, alpha)
        } else if (vibration < 1.0) {
            val alpha = accelerateAlpha(VIBRATION_EMA_UP_FAST, isWarming, 0.1)
            applyEma(currentFloor, vibration, alpha)
        } else {
            currentFloor
        }
    }

    /**
     * Issue #601: High-Pass Filter for Kinetic Energy.
     * Separates the "impulse" (tamper) from the "sustained" (motion) signal.
     */
    fun updateKineticEnergy(
        currentEnergy: Double, 
        currentRawVibe: Double, 
        lastRawVibe: Double, 
        lastHpfValue: Double
    ): Pair<Double, Double> {
        // High-Pass Filter: y[n] = alpha * (y[n-1] + x[n] - x[n-1])
        val alphaHpf = VIBRATION_HPF_ALPHA
        val hpfValue = alphaHpf * (lastHpfValue + currentRawVibe - lastRawVibe)
        
        // Rectify and Smooth (EMA) to get "Energy"
        val instantEnergy = abs(hpfValue)
        val alphaEnergy = VIBRATION_ENERGY_EMA_ALPHA
        val nextEnergy = (currentEnergy * (1.0 - alphaEnergy)) + (instantEnergy * alphaEnergy)
        
        return Pair(nextEnergy, hpfValue)
    }

    /**
     * Centralized Lux Baseline Update logic.
     */
    fun updateLuxBaseline(currentBaseline: Double, lux: Double, isStationary: Boolean, isWarming: Boolean): Double {
        if (lux.isNaN()) return currentBaseline
        if (currentBaseline < 0) return lux
        
        val baseAlpha = if (lux < currentBaseline) {
            if (isStationary) LUX_EMA_DOWN_SLOW else LUX_EMA_DOWN_FAST
        } else {
            if (isStationary) LUX_EMA_UP_SLOW else LUX_EMA_UP_FAST
        }
        val alpha = accelerateAlpha(baseAlpha, isWarming)
        return applyEma(currentBaseline, lux, alpha)
    }

    /**
     * Centralized Barometric Baseline Update logic.
     */
    fun updateBaroBaseline(currentBaseline: Double, baroAlt: Double, isWarming: Boolean): Double {
        if (baroAlt.isNaN()) return currentBaseline
        if (currentBaseline < -999.0) return baroAlt
        
        val alpha = accelerateAlpha(BARO_EMA_SLOW, isWarming)
        return applyEma(currentBaseline, baroAlt, alpha)
    }

    /**
     * Centralized Acoustic Floor Update logic.
     */
    fun updateAcousticFloor(currentFloor: Double, updateDb: Double, isWarming: Boolean): Double {
        if (updateDb.isNaN() || updateDb < 0.0) return currentFloor
        if (currentFloor < 0) return max(updateDb, ACOUSTIC_FLOOR_MIN_DB)
        
        val alpha = if (updateDb < currentFloor) {
            accelerateAlpha(ACOUSTIC_EMA_DOWN_FAST, isWarming)
        } else {
            accelerateAlpha(ACOUSTIC_EMA_UP_FAST, isWarming)
        }
        
        val nextFloor = applyEma(currentFloor, updateDb, alpha)
        return max(nextFloor, ACOUSTIC_FLOOR_MIN_DB)
    }

    /**
     * SOT 6.230: Centralized EMA alpha acceleration for the 5s warming phase.
     */
    fun accelerateAlpha(baseAlpha: Double, isWarming: Boolean, limit: Double = 0.5): Double {
        val multiplier = if (isWarming) 10.0 else 1.0
        return (baseAlpha * multiplier).coerceAtMost(limit)
    }

    private fun applyEma(last: Double, current: Double, alpha: Double): Double {
        return (last * (1.0 - alpha)) + (current * alpha)
    }
}
