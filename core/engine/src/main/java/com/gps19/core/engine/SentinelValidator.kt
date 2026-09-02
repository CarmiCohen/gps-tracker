package com.gps19.core.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * SentinelValidator: Centralized "Sentinel Hard Gates" and baseline logic.
 * Sep.02.01:
 * - Issue #897: Added sensitivity mapping for Tilt and Vibration (R2.3).
 *   Tilt range: 5° to 25° (0.5 -> 15°).
 *   Vibration range: 0.2g to 1.4g (0.5 -> 0.8g).
 * Aug.29.11:
 * - Acoustic Refinement (R762b): Encapsulated adaptive acoustic duty-cycle 
 *   logic into computeAdaptiveAcousticOffCycle.
 * Aug.11.08:
 * - Issue #143: Forensic Integrity Verification.
 */
object SentinelValidator {

    fun isTiltViolated(tiltDegrees: Double, sensitivity: Float = 0.5f): Boolean {
        // Map 0.0..1.0 to 25.0..5.0 degrees (Higher sensitivity = Lower threshold)
        val threshold = 5.0 + (25.0 - 5.0) * (1.0 - sensitivity)
        return tiltDegrees > threshold
    }

    fun isAltitudeViolated(relativeAltitude: Double): Boolean {
        return abs(relativeAltitude) > BARO_LIFT_THRESHOLD_METERS
    }
    
    fun isLiftViolated(relativeAltitude: Double): Boolean = isAltitudeViolated(relativeAltitude)

    fun isShockViolated(peakShock: Double, adaptiveFloor: Double = INITIAL_VIBRATION_FLOOR, sensitivity: Float = 0.5f): Boolean {
        // Map 0.0..1.0 to 1.4g..0.2g (Higher sensitivity = Lower threshold)
        val baseThreshold = 0.2 + (1.4 - 0.2) * (1.0 - sensitivity)
        val dynamicThreshold = maxOf(baseThreshold, adaptiveFloor * VIBRATION_SHOCK_MULTIPLIER)
        return peakShock > dynamicThreshold
    }

    fun isVibrationSuspicious(vibration: Double, adaptiveFloor: Double = INITIAL_VIBRATION_FLOOR, sensitivity: Float = 0.5f): Boolean {
        // Map 0.0..1.0 to 0.45g..0.05g (Higher sensitivity = Lower threshold)
        val baseThreshold = 0.05 + (0.45 - 0.05) * (1.0 - sensitivity)
        val dynamicThreshold = maxOf(baseThreshold, adaptiveFloor * VIBRATION_SUSPICIOUS_MULTIPLIER)
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

    fun isSilentFailure(
        gpsStalled: Boolean,
        isTamperDetected: Boolean,
        cpuLoad: Double,
        ioWait: Double,
        maxIoLatency: Long,
        isThermalThrottling: Boolean
    ): Boolean {
        if (!gpsStalled || isTamperDetected) return false
        
        return cpuLoad >= SILENT_FAILURE_CPU_THRESHOLD || 
               ioWait >= SILENT_FAILURE_IOW_THRESHOLD || 
               maxIoLatency >= SILENT_FAILURE_LATENCY_THRESHOLD_MS ||
               isThermalThrottling
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
     * computeNextHpf: Part of Issue #601/653. High-Pass Filter primitive.
     */
    fun computeNextHpf(lastHpfValue: Double, currentRawVibe: Double, lastRawVibe: Double): Double {
        return VIBRATION_HPF_ALPHA * (lastHpfValue + currentRawVibe - lastRawVibe)
    }

    /**
     * computeNextEnergy: Part of Issue #601/653. Energy EMA primitive.
     */
    fun computeNextEnergy(currentEnergy: Double, hpfValue: Double): Double {
        val instantEnergy = abs(hpfValue)
        val alphaEnergy = VIBRATION_ENERGY_EMA_ALPHA
        return (currentEnergy * (1.0 - alphaEnergy)) + (instantEnergy * alphaEnergy)
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
     * computeAdaptiveAcousticOffCycle: Part of Issue #762 (R762b). 
     * Calculates the acoustic monitor off-cycle duration based on stationary duration.
     */
    fun computeAdaptiveAcousticOffCycle(
        isStationary: Boolean,
        stationaryStartRt: Long,
        nowRt: Long
    ): Long {
        if (!isStationary || stationaryStartRt == 0L) return ACOUSTIC_DUTY_CYCLE_OFF_MS
        val durationMs = nowRt - stationaryStartRt
        // Scale linearly from 8s up to 32s (8s * 4) based on minutes of immobility.
        return min(ACOUSTIC_DUTY_CYCLE_OFF_MS * 4, ACOUSTIC_DUTY_CYCLE_OFF_MS + (durationMs / 60000) * 1000L)
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
