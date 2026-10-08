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
 * Oct.8.9:
 * - Issue #SIMP-1012-3: Forensic Stability Audit. Implemented evaluateSignalHealth 
 *   using zero-allocation SNR retrieval. Promoted RoleState and roleStates to 
 *   internal/PublishedApi to support inline evaluation (R-ID 684).
 */
@Singleton
class ForensicAuditor @Inject constructor(
    private val timeProvider: TimeProvider,
    private val systemStatusProvider: SystemStatusProvider
) {
    @PublishedApi
    internal class RoleState {
        var maxGnssJitterMs = 0L
        var lastGpsFixRealtime = 0L
        var stabilityAuditFixCount = 0
        var stabilityAuditViolationCount = 0
        var lastStabilityAuditTs = 0L

        var lastExpectedIntervalMs = 0L
        var lastIntervalChangeRt = 0L
        
        var isSensorRateAudited = false
        
        // Signal Health Tracking
        var isJammingSuspected = false
        var lastSignalHealthAuditTs = 0L

        fun reset() {
            maxGnssJitterMs = 0L
            lastGpsFixRealtime = 0L
            stabilityAuditFixCount = 0
            stabilityAuditViolationCount = 0
            lastStabilityAuditTs = 0L
            lastExpectedIntervalMs = 0L
            lastIntervalChangeRt = 0L
            isSensorRateAudited = false
            isJammingSuspected = false
            lastSignalHealthAuditTs = 0L
        }
    }

    @PublishedApi
    internal val roleStates = ConcurrentHashMap<AppRole, RoleState>().apply {
        put(AppRole.TRACKER, RoleState())
        put(AppRole.VIEWER_SELF, RoleState())
        put(AppRole.VIEWER_REMOTE, RoleState())
    }

    @Volatile private var lastGnssStatusRt = 0L

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

    fun updateExpectedInterval(nowRt: Long, expectedIntervalMs: Long, role: AppRole) {
        val state = roleStates[role] ?: return
        synchronized(state) {
            if (expectedIntervalMs != state.lastExpectedIntervalMs) {
                if (state.lastExpectedIntervalMs != 0L) {
                    state.lastIntervalChangeRt = nowRt
                }
                state.lastExpectedIntervalMs = expectedIntervalMs
            }
        }
    }

    fun isAdaptationMuzzled(nowRt: Long, role: AppRole): Boolean {
        val state = roleStates[role] ?: return false
        val changeRt = state.lastIntervalChangeRt
        if (changeRt == 0L) return false
        return nowRt - changeRt < ADAPTATION_SETTLING_MS
    }

    fun recordGpsFix(nowRt: Long, expectedIntervalMs: Long, role: AppRole): String? {
        val state = roleStates[role] ?: return null
        return synchronized(state) {
            updateExpectedIntervalLocked(nowRt, expectedIntervalMs, state)
            val muzzled = isAdaptationMuzzledLocked(nowRt, state)
            
            var gapMessage: String? = null
            if (state.lastGpsFixRealtime > 0) {
                val gap = nowRt - state.lastGpsFixRealtime
                state.stabilityAuditFixCount++
                if (gap > expectedIntervalMs + GPS_STABILITY_GAP_THRESHOLD_MS) {
                    if (!muzzled) {
                        state.stabilityAuditViolationCount++
                        gapMessage = "${gap}ms detected during logic pulse."
                    } else {
                        Timber.d("ForensicAuditor: Stability gap of ${gap}ms muzzled (Adaptation ${role.name}).")
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
        val isReliabilityViolation: Boolean,
        val isJammingDetected: Boolean = false
    )

    /**
     * evaluateSignalHealth: Uses optimized SNR forensic retrieval to distinguish 
     * between active jamming and signal blockage (R-ID 684).
     * This function is inline to ensure the snrProvider lambda is zero-allocation.
     */
    inline fun evaluateSignalHealth(
        nowRt: Long, 
        role: AppRole, 
        crossinline snrProvider: (fromRt: Long, toRt: Long, action: (ForensicSample) -> Unit) -> Unit
    ): Boolean {
        val state = roleStates[role] ?: return false
        
        synchronized(state) {
            if (nowRt - state.lastSignalHealthAuditTs < GPS_STABILITY_AUDIT_INTERVAL_MS) return state.isJammingSuspected
            state.lastSignalHealthAuditTs = nowRt
            
            var lowSnrCount = 0
            var highSnrCount = 0
            var totalSamples = 0
            
            snrProvider(nowRt - JAMMING_FORENSIC_LOOKBACK_MS, nowRt) { sample ->
                totalSamples++
                if (sample.snr > 0.0) {
                    if (sample.snr < JAMMING_SNR_CRITICAL_THRESHOLD) {
                        lowSnrCount++
                    } else {
                        highSnrCount++
                    }
                }
            }
            
            val isJamming = totalSamples >= JAMMING_STABILITY_REQUIRED_SAMPLES && 
                           lowSnrCount > highSnrCount && 
                           lowSnrCount > (totalSamples / 2)
            
            if (isJamming != state.isJammingSuspected) {
                state.isJammingSuspected = isJamming
                Timber.w("ForensicAuditor: [${role.name}] Jamming suspicion changed to $isJamming (Samples: $totalSamples, LowSNR: $lowSnrCount, HighSNR: $highSnrCount)")
            }
            
            return isJamming
        }
    }

    fun evaluateStability(nowRt: Long, role: AppRole): StabilityVerdict? {
        val state = roleStates[role] ?: return null
        
        val fixCount: Int
        val violationCount: Int
        val jitter: Long
        val isJamming: Boolean
        
        synchronized(state) {
            if (nowRt - state.lastStabilityAuditTs <= GPS_STABILITY_AUDIT_INTERVAL_MS) return null
            
            fixCount = state.stabilityAuditFixCount
            violationCount = state.stabilityAuditViolationCount
            jitter = state.maxGnssJitterMs
            isJamming = state.isJammingSuspected
            
            if (fixCount == 0 && jitter == 0L && !isJamming) {
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
        
        val jammingTag = if (isJamming) " [JAMMING DETECTED]" else ""
        val msg = "STABILITY AUDIT (${role.name}): Reliability ${reliability.roundToOneDecimal()}% ($violationCount gaps in $fixCount fixes), Max GNSS Jitter: ${jitter}ms$jammingTag"
        Timber.i(msg)

        if (reliabilityViolation || jitterViolation || isJamming) {
            return StabilityVerdict(
                message = msg,
                isJitterViolation = jitterViolation,
                isReliabilityViolation = reliabilityViolation,
                isJammingDetected = isJamming
            )
        }
        
        return null
    }

    private fun Double.roundToOneDecimal(): String = (round(this * 10) / 10).toString()

    fun resetGnssJitter() {
        roleStates.values.forEach { state ->
            synchronized(state) { state.maxGnssJitterMs = 0L }
        }
        lastGnssStatusRt = 0L
    }

    fun auditSensorRate(isWarming: Boolean): List<Pair<AppRole, String>> {
        if (isWarming) return emptyList()
        val hz = JdHardwareManager.getSensorAuditHz()
        if (hz <= 0.0) return emptyList()

        val results = mutableListOf<Pair<AppRole, String>>()
        roleStates.forEach { (role, state) ->
            synchronized(state) {
                if (!state.isSensorRateAudited) {
                    state.isSensorRateAudited = true
                    val msg = "Sensor Rate Audit (R-ID 256): ${hz.toInt()} Hz. Native Efficacy: ${hz > 200.0}"
                    Timber.i("ForensicAuditor: [${role.name}] $msg")
                    results.add(role to msg)
                }
            }
        }
        return results
    }

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

    fun computeEnergyFootprint(nowRt: Long, consume: Boolean = true): RevivalEvent.Footprint? {
        val start: BatteryStatus
        val startRt: Long
        synchronized(this) {
            start = revivalStartBattery ?: return null
            startRt = revivalStartRtForFootprint
            if (consume) { revivalStartBattery = null; revivalStartRtForFootprint = 0L }
        }
        val current = systemStatusProvider.getBatteryStatus()
        val deltaMa = current.currentMa - start.currentMa
        val deltaTemp = current.temp - start.temp
        val durationMs = nowRt - startRt
        Timber.i("ForensicAuditor: Energy Footprint Verdict (R-ID 259): Delta mA: $deltaMa, Delta Temp: $deltaTemp°C, Duration: ${durationMs}ms")
        return RevivalEvent.Footprint(deltaMa, deltaTemp, durationMs)
    }

    fun clearRevivalState() {
        synchronized(this) { revivalStartBattery = null; revivalStartRtForFootprint = 0L }
    }

    fun reset(role: AppRole? = null) {
        if (role == null) {
            roleStates.values.forEach { synchronized(it) { it.reset() } }
            resetGnssJitter()
            clearRevivalState()
            JdHardwareManager.resetSensorAudit()
        } else {
            roleStates[role]?.let { synchronized(it) { it.reset() } }
            lastGnssStatusRt = 0L
        }
    }

    val maxGnssJitterMs get() = roleStates.values.maxOfOrNull { state -> synchronized(state) { state.maxGnssJitterMs } } ?: 0L
    fun getLastGpsFixRealtime(role: AppRole): Long = roleStates[role]?.lastGpsFixRealtime ?: 0L
    fun isJammingSuspected(role: AppRole): Boolean = roleStates[role]?.isJammingSuspected ?: false
}
