package com.gps19.core.engine

import kotlin.math.*

/**
 * TelemetryUtils: Logic for scoring and evaluating signal quality.
 * v9.4.06:
 * - Issue #077 Hardening: Cleaned up satsIndex math to leverage implicit promotion.
 * v9.4.05:
 * - Issue #077 Hardening: Simplified GPS index calculation math.
 */

data class GpsIndexData(val ageIndex: Double, val accIndex: Double, val satsIndex: Double, val totalIndex: Double)

object TelemetryUtils {

    /**
     * Calculates the communication quality index (0-10) based on RTT and signal levels.
     */
    fun calculateCommIndex(rtt: Int, remoteSig: Int, localSig: Int): Int {
        if (rtt >= MAX_ALLOWED_RTT_MS || rtt <= 0) return 0
        val rttFactor = min(1.0, max(0.0, 1.0 - (max(0, rtt - COMM_RTT_FLOOR_MS) / COMM_RTT_SCALING_FACTOR)))
        val signalFactor = (localSig / 10.0) * (remoteSig / 10.0)
        return (signalFactor * rttFactor * 10).roundToInt().coerceIn(0, 10)
    }

    /**
     * Calculates the GPS-Index based on age, accuracy, and satellite count.
     */
    fun calculateGpsIndex(gpsAgeMs: Long, maxAccuracy: Double, satsUsed: Int): GpsIndexData {
        if (gpsAgeMs < -30000L) {
            return GpsIndexData(0.0, 0.0, 0.0, 0.0)
        }

        val ageMsValue = maxOf(0L, gpsAgeMs)
        val ageSec = maxOf(0.1, ageMsValue / 1000.0)
        val ageIndex = if (ageSec <= GPS_INDEX_AGE_EXCELLENT_SEC) 1.0 else 1.0 / (ageSec / GPS_INDEX_AGE_SCALING).coerceAtLeast(1.0)
        
        val accIndex = if (maxAccuracy > 0.0) {
            if (maxAccuracy <= GPS_INDEX_ACCURACY_EXCELLENT_METERS) 1.0 else (GPS_INDEX_ACCURACY_EXCELLENT_METERS / maxAccuracy).coerceIn(0.01, 1.0)
        } else 0.001 
        
        val satsIndex = if (satsUsed >= GPS_INDEX_SATS_TARGET) 1.0 else {
            val diff = GPS_INDEX_SATS_TARGET - satsUsed
            if (diff <= 1) 1.0 else 1.0 / diff
        }
        
        val total = (ageIndex + accIndex + satsIndex) / 3.0
        
        return GpsIndexData(ageIndex, accIndex, satsIndex, total)
    }
}
