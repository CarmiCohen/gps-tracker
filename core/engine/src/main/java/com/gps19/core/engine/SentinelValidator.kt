package com.gps19.core.engine

import kotlin.math.abs

/**
 * SentinelValidator: Centralized "Sentinel Hard Gates".
 * v8.9.75:
 * - Issue #014: Type Safety Optimization. Standardized parameters to Double 
 *   to eliminate redundant toDouble()/toFloat() conversions.
 * v8.9.67:
 * - Issue #010: Implemented A15 Acoustic/Vibration Coherence Check.
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

    /**
     * Issue #010: A15 Coherence Check.
     * Confirming that spikes observed during isolation are system-generated.
     * We require a concurrent vibration floor (> 0.01g) to trust acoustic data on A15.
     */
    fun isAcousticViolated(peakDb: Double, floorDb: Double, isA15: Boolean = false, vibration: Double = 0.0): Boolean {
        if (floorDb < 0.0) return false
        val jump = peakDb - floorDb
        val threshold = if (isA15) ACOUSTIC_THRESHOLD_DB_JUMP_A15 else ACOUSTIC_THRESHOLD_DB_JUMP
        
        if (isA15 && vibration < 0.01) return false // Muzzle system-generated noise
        
        return jump > threshold && peakDb >= ACOUSTIC_MIN_THRESHOLD_DB
    }

    fun isAcousticSuspicious(peakDb: Double, floorDb: Double, isA15: Boolean = false, vibration: Double = 0.0): Boolean {
        if (floorDb < 0.0) return false
        val jump = peakDb - floorDb
        val threshold = if (isA15) (ACOUSTIC_THRESHOLD_DB_JUMP_A15 / 2.0) else ACOUSTIC_SUSPICIOUS_THRESHOLD_DB_JUMP
        
        if (isA15 && vibration < 0.01) return false // Muzzle system-generated noise

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
            (currentFloor * (1.0 - alpha)) + (vibration * alpha)
        } else if (vibration < 1.0) {
            val alpha = accelerateAlpha(VIBRATION_EMA_UP_FAST, isWarming, 0.1)
            (currentFloor * (1.0 - alpha)) + (vibration * alpha)
        } else {
            currentFloor
        }
    }

    /**
     * SOT 6.230: Centralized EMA alpha acceleration for the 5s warming phase.
     */
    fun accelerateAlpha(baseAlpha: Double, isWarming: Boolean, limit: Double = 0.5): Double {
        val multiplier = if (isWarming) 10.0 else 1.0
        return (baseAlpha * multiplier).coerceAtMost(limit)
    }
}
