package com.gps19.core.engine

import kotlin.math.abs

/**
 * SentinelValidator: Centralized "Sentinel Hard Gates".
 * v9.3.20:
 * - R405: Samsung A15 Hardening. Removed device-specific isA15 branching. 
 *   Simplified acoustic validation to a single standard.
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
     * R405: Unified acoustic validation.
     * Removed isA15 device-specific coherence checks.
     */
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
