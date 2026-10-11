package com.gps19.core.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * SentinelValidator: Centralized "Sentinel Hard Gates" and baseline logic.
 * Oct.11.1:
 * - Issue #SIMP-1011-5: Acoustic Profiling. Updated DefaultNativeFastPathProvider 
 *   to match the new TimeProvider-aware interface.
 * Oct.10.1 (Restoration Path):
 * - Issue #SIMP-1015-1: Non-Nullable Native Authority. Standardized on @NotNull 
 *   native providers. All high-frequency gates now route through nativeProvider 
 *   without null-checks, using DefaultNativeFastPathProvider as the baseline.
 * - Issue #SIMP-1013-3: JNI Stationary Authority. Offloaded load-aware movement 
 *   authority to JNI. Eliminated JVM gate calculation in the authoritative path.
 */
object SentinelValidator {

    private var nativeProvider: NativeFastPathProvider = DefaultNativeFastPathProvider

    fun setNativeProvider(provider: NativeFastPathProvider) {
        this.nativeProvider = provider
    }

    fun isTiltViolated(tiltDegrees: Double, sensitivity: Float = 0.5f): Boolean {
        val threshold = 5.0 + (25.0 - 5.0) * (1.0 - sensitivity)
        return tiltDegrees > threshold
    }

    fun isAltitudeViolated(relativeAltitude: Double): Boolean {
        return abs(relativeAltitude) > BARO_LIFT_THRESHOLD_METERS
    }
    
    fun isLiftViolated(relativeAltitude: Double): Boolean = isAltitudeViolated(relativeAltitude)

    fun isShockViolated(
        peakShock: Double, 
        adaptiveFloor: Double = INITIAL_VIBRATION_FLOOR, 
        sensitivity: Float = 0.5f,
        cpuLoad: Double = 0.0
    ): Boolean {
        return nativeProvider.isShockViolated(peakShock, adaptiveFloor, sensitivity, cpuLoad)
    }

    fun isVibrationSuspicious(
        vibration: Double, 
        adaptiveFloor: Double = INITIAL_VIBRATION_FLOOR, 
        sensitivity: Float = 0.5f,
        cpuLoad: Double = 0.0
    ): Boolean {
        return nativeProvider.isVibrationSuspicious(vibration, adaptiveFloor, sensitivity, cpuLoad)
    }

    /**
     * isStationary: Authoritative movement gate.
     */
    fun isStationary(vibration: Double, adaptiveFloor: Double, cpuLoad: Double = 0.0): Boolean {
        return nativeProvider.isStationary(vibration, adaptiveFloor, cpuLoad)
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

    fun updateVibrationFloor(currentFloor: Double, vibration: Double, isWarming: Boolean, cpuLoad: Double = 0.0): Double {
        return nativeProvider.updateVibrationFloor(currentFloor, vibration, isWarming, cpuLoad)
    }

    fun computeNextHpf(lastHpfValue: Double, currentRawVibe: Double, lastRawVibe: Double): Double {
        return nativeProvider.computeNextHpf(lastHpfValue, currentRawVibe, lastRawVibe)
    }

    fun computeNextEnergy(currentEnergy: Double, hpfValue: Double): Double {
        return nativeProvider.computeNextEnergy(currentEnergy, hpfValue)
    }

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

    fun updateBaroBaseline(currentBaseline: Double, baroAlt: Double, isWarming: Boolean): Double {
        if (baroAlt.isNaN()) return currentBaseline
        if (currentBaseline < -999.0) return baroAlt
        
        val alpha = accelerateAlpha(BARO_EMA_SLOW, isWarming)
        return applyEma(currentBaseline, baroAlt, alpha)
    }

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
     */
    fun computeAdaptiveAcousticOffCycle(
        isStationary: Boolean,
        stationaryDurationMs: Long
    ): Long {
        if (!isStationary || stationaryDurationMs == 0L) return ACOUSTIC_DUTY_CYCLE_OFF_MS
        return min(ACOUSTIC_DUTY_CYCLE_OFF_MS * 4, ACOUSTIC_DUTY_CYCLE_OFF_MS + (stationaryDurationMs / 60000) * 1000L)
    }

    /**
     * evaluateLocationPendingReason: Consolidates environment-based GNSS status evaluation.
     */
    fun evaluateLocationPendingReason(
        satellitesInView: Int,
        satellitesUsed: Int,
        deltaSinceFixMs: Long,
        gapThresholdMs: Long,
        isJammingCandidate: Boolean = false,
        isAcousticViolated: Boolean = false
    ): LocationPendingReason {
        // Behavioral priorities first
        if (isJammingCandidate) return LocationPendingReason.JAMMER_SUSPICION
        if (isAcousticViolated) return LocationPendingReason.ACOUSTIC_VIOLATION
        
        // Environment-based GNSS health
        if (deltaSinceFixMs <= gapThresholdMs) return LocationPendingReason.NONE
        
        return when {
            satellitesInView == 0 -> LocationPendingReason.SIGNAL_LOSS
            satellitesInView >= 4 && satellitesUsed < 4 -> LocationPendingReason.GPS_STALL
            else -> LocationPendingReason.GPS_GAP
        }
    }

    fun getReasonPriority(reason: LocationPendingReason): Int {
        return when (reason) {
            LocationPendingReason.NONE -> 0
            LocationPendingReason.GPS_GAP -> 1
            LocationPendingReason.SIGNAL_LOSS -> 2
            LocationPendingReason.GPS_STALL -> 3
            LocationPendingReason.ACOUSTIC_VIOLATION -> 4
            LocationPendingReason.JAMMER_SUSPICION -> 5
        }
    }

    fun getHigherPriorityReason(r1: LocationPendingReason, r2: LocationPendingReason): LocationPendingReason {
        if (r1 == r2) return r1
        return if (getReasonPriority(r2) >= getReasonPriority(r1)) r2 else r1
    }

    fun accelerateAlpha(baseAlpha: Double, isWarming: Boolean, limit: Double = 0.5): Double {
        val multiplier = if (isWarming) 10.0 else 1.0
        return (baseAlpha * multiplier).coerceAtMost(limit)
    }

    private fun applyEma(last: Double, current: Double, alpha: Double): Double {
        return (last * (1.0 - alpha)) + (current * alpha)
    }

    /**
     * DefaultNativeFastPathProvider: JVM implementation of native-eligible math.
     * Serves as a fallback if JdHardwareManager is not available or hasn't 
     * yet injected the native-linked provider.
     */
    private object DefaultNativeFastPathProvider : NativeFastPathProvider {
        override fun isStationary(vibration: Double, adaptiveFloor: Double, cpuLoad: Double): Boolean {
            val loadFactor = if (cpuLoad > SENSOR_LOAD_GATE_CPU_THRESHOLD) 2.0 else 1.0
            val dynamicGate = (adaptiveFloor * STATIONARY_FLOOR_MULT * loadFactor).coerceIn(
                INITIAL_VIBRATION_FLOOR, 
                VIBRATION_STATIONARY_THRESHOLD * loadFactor
            )
            return vibration < dynamicGate
        }

        override fun updateVibrationFloor(currentFloor: Double, vibration: Double, isWarming: Boolean, cpuLoad: Double): Double {
            if (vibration.isNaN() || vibration <= 0.0 || cpuLoad > SENSOR_LOAD_GATE_CPU_THRESHOLD) return currentFloor
            
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

        override fun computeNextHpf(lastHpfValue: Double, currentRawVibe: Double, lastRawVibe: Double): Double {
            return VIBRATION_HPF_ALPHA * (lastHpfValue + currentRawVibe - lastRawVibe)
        }

        override fun computeNextEnergy(currentEnergy: Double, hpfValue: Double): Double {
            val instantEnergy = abs(hpfValue)
            val alphaEnergy = VIBRATION_ENERGY_EMA_ALPHA
            return (currentEnergy * (1.0 - alphaEnergy)) + (instantEnergy * alphaEnergy)
        }

        override fun calculateVibrationDelta(x: Double, y: Double, z: Double, lx: Double, ly: Double, lz: Double): Double {
            return Math.sqrt((x - lx) * (x - lx) + (y - ly) * (y - ly) + (z - lz) * (z - lz)) / GRAVITY_EARTH
        }

        override fun isShockViolated(peakShock: Double, adaptiveFloor: Double, sensitivity: Float, cpuLoad: Double): Boolean {
            val loadFactor = if (cpuLoad > SENSOR_LOAD_GATE_CPU_THRESHOLD) 1.5 else 1.0
            val baseThreshold = (0.2 + (1.4 - 0.2) * (1.0 - sensitivity)) * loadFactor
            val dynamicThreshold = maxOf(baseThreshold, adaptiveFloor * VIBRATION_SHOCK_MULTIPLIER * loadFactor)
            return peakShock > dynamicThreshold
        }

        override fun isVibrationSuspicious(vibration: Double, adaptiveFloor: Double, sensitivity: Float, cpuLoad: Double): Boolean {
            val loadFactor = if (cpuLoad > SENSOR_LOAD_GATE_CPU_THRESHOLD) 1.5 else 1.0
            val baseThreshold = (0.05 + (0.45 - 0.05) * (1.0 - sensitivity)) * loadFactor
            val dynamicThreshold = maxOf(baseThreshold, adaptiveFloor * VIBRATION_SUSPICIOUS_MULTIPLIER * loadFactor)
            return vibration > dynamicThreshold
        }

        override fun processVibrationBatch(timeProvider: TimeProvider, batch: VibrationBatch): Boolean = false
        override fun processGnssBatch(timeProvider: TimeProvider, batch: GnssHealthBatch): Boolean = false
        override fun processAcousticBatch(timeProvider: TimeProvider, batch: AcousticBatch, buffer: ShortArray): Boolean = false
        override fun processProximityBatch(timeProvider: TimeProvider, batch: ProximityBatch): Boolean = false
        override fun processSystemPressure(timeProvider: TimeProvider, batch: SystemPressureBatch): Boolean = false
    }
}
