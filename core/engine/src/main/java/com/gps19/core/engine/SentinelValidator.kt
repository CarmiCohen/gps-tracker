package com.gps19.core.engine

import kotlin.math.abs

/**
 * SentinelValidator: Centralized "Sentinel Hard Gates".
 * v8.9.34:
 * - Issue #166: Shadow Constants Remediation. Replaced magic numbers with EngineConstants.
 */
object SentinelValidator {

    fun isTiltViolated(tiltDegrees: Float): Boolean {
        return tiltDegrees > TILT_THRESHOLD_DEGREES
    }

    fun isAltitudeViolated(relativeAltitude: Float): Boolean {
        return abs(relativeAltitude) > BARO_LIFT_THRESHOLD_METERS
    }
    
    fun isLiftViolated(relativeAltitude: Float): Boolean = isAltitudeViolated(relativeAltitude)

    fun isShockViolated(peakShock: Float, adaptiveFloor: Float = INITIAL_VIBRATION_FLOOR): Boolean {
        val dynamicThreshold = maxOf(VIBRATION_SHOCK_THRESHOLD_G, adaptiveFloor * VIBRATION_SHOCK_MULTIPLIER)
        return peakShock > dynamicThreshold
    }

    fun isVibrationSuspicious(vibration: Float, adaptiveFloor: Float = INITIAL_VIBRATION_FLOOR): Boolean {
        val dynamicThreshold = maxOf(VIBRATION_SUSPICIOUS_THRESHOLD_G, adaptiveFloor * VIBRATION_SUSPICIOUS_MULTIPLIER)
        return vibration > dynamicThreshold
    }

    fun isStationary(vibration: Float, adaptiveFloor: Float): Boolean {
        val dynamicGate = (adaptiveFloor * STATIONARY_FLOOR_MULT).coerceIn(INITIAL_VIBRATION_FLOOR, VIBRATION_STATIONARY_THRESHOLD)
        return vibration < dynamicGate
    }

    fun isAcousticViolated(peakDb: Double, floorDb: Double): Boolean {
        if (floorDb < 0.0) return false
        val jump = peakDb - floorDb
        return jump > ACOUSTIC_THRESHOLD_DB_JUMP && peakDb >= ACOUSTIC_MIN_THRESHOLD_DB
    }

    fun isAcousticSuspicious(peakDb: Double, floorDb: Double): Boolean {
        if (floorDb < 0.0) return false
        val jump = peakDb - floorDb
        return jump > ACOUSTIC_SUSPICIOUS_THRESHOLD_DB_JUMP && peakDb >= ACOUSTIC_MIN_THRESHOLD_DB
    }

    fun isLightViolated(lux: Float, luxBaseline: Float): Boolean {
        if (luxBaseline < 0f) return false
        return (lux - luxBaseline) > LIGHT_THRESHOLD_LUX_JUMP
    }

    /**
     * R729: Unified Vibration Floor Update (EMA).
     */
    fun updateVibrationFloor(currentFloor: Float, vibration: Float, isWarming: Boolean): Float {
        if (vibration.isNaN() || vibration <= 0f) return currentFloor
        
        return if (vibration < currentFloor) {
            val alpha = accelerateAlpha(VIBRATION_EMA_DOWN_FAST, isWarming, 0.5f)
            (currentFloor * (1f - alpha)) + (vibration * alpha)
        } else if (vibration < 1.0f) {
            val alpha = accelerateAlpha(VIBRATION_EMA_UP_FAST, isWarming, 0.1f)
            (currentFloor * (1f - alpha)) + (vibration * alpha)
        } else {
            currentFloor
        }
    }

    /**
     * SOT 6.230: Centralized EMA alpha acceleration for the 5s warming phase.
     */
    fun accelerateAlpha(baseAlpha: Float, isWarming: Boolean, limit: Float = 0.5f): Float {
        val multiplier = if (isWarming) 10.0f else 1.0f
        return (baseAlpha * multiplier).coerceAtMost(limit)
    }
}
