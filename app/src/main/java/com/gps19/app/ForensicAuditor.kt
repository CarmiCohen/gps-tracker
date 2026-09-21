package com.gps19.app

import com.gps19.core.engine.*
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.round

/**
 * ForensicAuditor: Encapsulates high-assurance hardware audits (Stability, Jitter, Sensor Rates, Energy).
 * Sep.20.18:
 * - Issue #1132 Hardening: Implemented internal synchronization for RoleState to 
 *   ensure atomic check-and-set for jitter peaks and stability counters across 
 *   GNSS/Sensor and Tick threads (R-ID 382).
 * Sep.20.15:
 * - Issue #1122: Resolved False GNSS Jitter Spike on Suite Restart. 
 *   Explicitly zeroed lastGnssStatusRt in resetGnssJitter() and reset() (R-ID 379).
 */
@Singleton
class ForensicAuditor @Inject constructor(
    private val timeProvider: TimeProvider,
    private val systemStatusProvider: SystemStatusProvider
) {
    private class RoleState {
        var maxGnssJitterMs = 0L
        var lastGpsFixRealtime = 0L
        var stabilityAuditFixCount = 0
        var stabilityAuditViolationCount = 0
        var lastStabilityAuditTs = 0L

        var lastExpectedIntervalMs = 0L
        var lastIntervalChangeRt = 0L
        
        var accelEventCount = 0
        var accelAuditStartRt = 0L
        var isSensorRateAudited = false

        fun reset() {
            maxGnssJitterMs = 0L
            lastGpsFixRealtime = 0L
            stabilityAuditFixCount = 0
            stabilityAuditViolationCount = 0
            lastStabilityAuditTs = 0L
            lastExpectedIntervalMs = 0L
            lastIntervalChangeRt = 0L
            accelEventCount = 0
            accelAuditStartRt = 0L
            isSensorRateAudited = false
        }
    }

    private val roleStates = ConcurrentHashMap<String, RoleState>().apply {
        put("T", RoleState())
        put("V", RoleState())
    }

    @Volatile private var lastGnssStatusRt = 0L

    /**
     * recordGnssStatus: Updates jitter metrics relative to the expected interval.
     * Issue #1132: Thread-safe atomic peak tracking for GNSS jitter.
     */
    fun recordGnssStatus(nowRt: Long, expectedIntervalMs: Long) {
        val lastRt = lastGnssStatusRt
        if (lastRt > 0) {
            val interval = nowRt - lastRt
            val jitter = abs(interval - expectedIntervalMs)
            roleStates.values.forEach { state ->
                synchronized(state) {
                    if (jitter > state.maxGnssJitterMs) {
                        state.maxGnssJitterMs = jitter
                    }
                }
            }
        }
        lastGnssStatusRt = nowRt
    }

    /**
     * Updates the expected polling interval and tracks transitions for muzzling.
     */
    fun updateExpectedInterval(nowRt: Long, expectedIntervalMs: Long, roleTag: String) {
        val state = roleStates[roleTag] ?: return
        synchronized(state) {
            if (expectedIntervalMs != state.lastExpectedIntervalMs) {
                if (state.lastExpectedIntervalMs != 0L) {
                    state.lastIntervalChangeRt = nowRt
                }
                state.lastExpectedIntervalMs = expectedIntervalMs
            }
        }
    }

    fun isAdaptationMuzzled(nowRt: Long, roleTag: String): Boolean {
        val state = roleStates[roleTag] ?: return false
        val changeRt = state.lastIntervalChangeRt
        if (changeRt == 0L) return false
        return nowRt - changeRt < ADAPTATION_SETTLING_MS
    }

    /**
     * recordGpsFix: Atomic stability audit for each logic pulse.
     */
    fun recordGpsFix(nowRt: Long, expectedIntervalMs: Long, roleTag: String): String? {
        val state = roleStates[roleTag] ?: return null
        return synchronized(state) {
            updateExpectedIntervalLocked(nowRt, expectedIntervalMs, state)
            val isMuzzled = isAdaptationMuzzledLocked(nowRt, state)
            
            var gapMessage: String? = null
            if (state.lastGpsFixRealtime > 0) {
                val gap = nowRt - state.lastGpsFixRealtime
                state.stabilityAuditFixCount++
                if (gap > expectedIntervalMs + GPS_STABILITY_GAP_THRESHOLD_MS) {
                    if (!isMuzzled) {
                        state.stabilityAuditViolationCount++
                        gapMessage = "${gap}ms detected during logic pulse."
                    } else {
                        Timber.d("ForensicAuditor: Stability gap of ${gap}ms muzzled (Adaptation $roleTag).")
                    }
                }
            }
            state.lastGpsFixRealtime = nowRt
            if (state.lastStabilityAuditTs == 0L) state.lastStabilityAuditTs = nowRt
            gapMessage
        }
    }

    private fun updateExpectedIntervalLocked(nowRt: Long, expectedIntervalMs: Long, state: RoleState) {
        if (expectedIntervalMs != state.lastExpectedIntervalMs) {
            if (state.lastExpectedIntervalMs != 0L) state.lastIntervalChangeRt = nowRt
            state.lastExpectedIntervalMs = expectedIntervalMs
        }
    }

    private fun isAdaptationMuzzledLocked(nowRt: Long, state: RoleState): Boolean {
        if (state.lastIntervalChangeRt == 0L) return false
        return nowRt - state.lastIntervalChangeRt < ADAPTATION_SETTLING_MS
    }

    data class StabilityVerdict(
        val message: String,
        val isJitterViolation: Boolean,
        val isReliabilityViolation: Boolean
    )

    /**
     * evaluateStability: Atomically reads and resets stability counters for a role.
     */
    fun evaluateStability(nowRt: Long, roleTag: String): StabilityVerdict? {
        val state = roleStates[roleTag] ?: return null
        
        val fixCount: Int
        val violationCount: Int
        val jitter: Long
        
        synchronized(state) {
            if (nowRt - state.lastStabilityAuditTs <= GPS_STABILITY_AUDIT_INTERVAL_MS) return null
            
            fixCount = state.stabilityAuditFixCount
            violationCount = state.stabilityAuditViolationCount
            jitter = state.maxGnssJitterMs
            
            if (fixCount == 0 && jitter == 0L) {
                state.lastStabilityAuditTs = nowRt
                return null
            }
            
            // Reset for next window
            state.stabilityAuditFixCount = 0
            state.stabilityAuditViolationCount = 0
            state.maxGnssJitterMs = 0L
            state.lastStabilityAuditTs = nowRt
        }

        val reliability = if (fixCount > 0) 100.0 * (fixCount - violationCount) / fixCount else 100.0
        val jitterViolation = jitter > GNSS_JITTER_THRESHOLD_MS
        val reliabilityViolation = reliability < GPS_STABILITY_RELIABILITY_THRESHOLD
        
        if (reliabilityViolation || jitterViolation) {
            val msg = StringBuilder("STABILITY AUDIT ($roleTag): ")
            if (reliabilityViolation) {
                msg.append("Reliability ${reliability.roundToOneDecimal()}% ($violationCount gaps in $fixCount fixes). ")
            }
            if (jitterViolation) {
                msg.append("GNSS Jitter: ${jitter}ms (Hardware Instability).")
            }
            return StabilityVerdict(
                message = msg.toString().trim(),
                isJitterViolation = jitterViolation,
                isReliabilityViolation = reliabilityViolation
            )
        }
        
        return null
    }

    private fun Double.roundToOneDecimal(): String = (round(this * 10) / 10).toString()

    /**
     * resetGnssJitter: Zeroes the jitter source and peak trackers.
     */
    fun resetGnssJitter() {
        roleStates.values.forEach { state ->
            synchronized(state) { state.maxGnssJitterMs = 0L }
        }
        lastGnssStatusRt = 0L
    }

    /**
     * auditSensorRate: Atomically evaluates sensor frequency per role.
     */
    fun auditSensorRate(nowRt: Long, isWarming: Boolean): List<Pair<String, String>> {
        if (isWarming) return emptyList()
        val results = mutableListOf<Pair<String, String>>()
        
        roleStates.forEach { (role, state) ->
            synchronized(state) {
                if (state.isSensorRateAudited) return@synchronized
                
                if (state.accelAuditStartRt == 0L) {
                    state.accelAuditStartRt = nowRt
                }
                
                state.accelEventCount++
                
                if (nowRt - state.accelAuditStartRt >= 1000L) {
                    val durationSec = (nowRt - state.accelAuditStartRt) / 1000.0
                    val hz = state.accelEventCount.toDouble() / durationSec
                    val isEffective = hz > 200.0
                    state.isSensorRateAudited = true
                    val msg = "Sensor Rate Audit (R-ID 256): ${hz.toInt()} Hz. Efficacy: $isEffective"
                    Timber.i("ForensicAuditor: [$role] $msg")
                    results.add(role to msg)
                }
            }
        }
        return results
    }

    // --- Energy Footprint Snapshot (R-ID 259) ---
    private var revivalStartBattery: BatteryStatus? = null
    @Volatile private var revivalStartRtForFootprint = 0L

    fun captureRevivalStart(nowRt: Long) {
        synchronized(this) {
            if (revivalStartBattery == null) {
                revivalStartBattery = systemStatusProvider.getBatteryStatus()
                revivalStartRtForFootprint = nowRt
            }
        }
    }

    fun computeEnergyFootprint(nowRt: Long, consume: Boolean = true): HardwareSuite.RevivalEvent.Footprint? {
        val start: BatteryStatus
        val startRt: Long
        synchronized(this) {
            start = revivalStartBattery ?: return null
            startRt = revivalStartRtForFootprint
            if (consume) {
                revivalStartBattery = null
                revivalStartRtForFootprint = 0L
            }
        }
        
        val current = systemStatusProvider.getBatteryStatus()
        val deltaMa = current.currentMa - start.currentMa
        val deltaTemp = current.temp - start.temp
        val durationMs = nowRt - startRt
        
        Timber.i("ForensicAuditor: Energy Footprint Verdict (R-ID 259): Delta mA: $deltaMa, Delta Temp: $deltaTemp°C, Duration: ${durationMs}ms")
        return HardwareSuite.RevivalEvent.Footprint(deltaMa, deltaTemp, durationMs)
    }

    fun clearRevivalState() {
        synchronized(this) {
            revivalStartBattery = null
            revivalStartRtForFootprint = 0L
        }
    }

    fun reset(roleTag: String? = null) {
        if (roleTag == null) {
            roleStates.values.forEach { synchronized(it) { it.reset() } }
            resetGnssJitter()
            clearRevivalState()
        } else {
            roleStates[roleTag]?.let { synchronized(it) { it.reset() } }
            // Reset common jitter source if any role is reset to prevent false restart spike
            lastGnssStatusRt = 0L
        }
    }

    val maxGnssJitterMs get() = roleStates.values.maxOfOrNull { state -> synchronized(state) { state.maxGnssJitterMs } } ?: 0L

    fun getLastGpsFixRealtime(roleTag: String): Long = roleStates[roleTag]?.lastGpsFixRealtime ?: 0L
}
