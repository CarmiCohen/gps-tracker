package com.gps19.core.engine

import kotlin.math.*

/**
 * TelemetryUtils: Logic for scoring and evaluating signal quality.
 * v8.8.21: Migrated from :app to :core:engine to ensure logic purity.
 */

data class GpsIndexData(val ageIndex: Float, val accIndex: Float, val satsIndex: Float, val totalIndex: Float)

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
    fun calculateGpsIndex(gpsAgeMs: Long, maxAccuracy: Float, satsUsed: Int): GpsIndexData {
        if (gpsAgeMs < -30000L) {
            return GpsIndexData(0f, 0f, 0f, 0f)
        }

        val ageMsValue = maxOf(0L, gpsAgeMs)
        val ageSec = maxOf(0.1, ageMsValue / 1000.0)
        val ageIndex = if (ageSec <= GPS_INDEX_AGE_EXCELLENT_SEC) 1.0 else 1.0 / (ageSec / GPS_INDEX_AGE_SCALING).coerceAtLeast(1.0)
        
        val accIndex = if (maxAccuracy > 0f) {
            if (maxAccuracy <= GPS_INDEX_ACCURACY_EXCELLENT_METERS) 1.0 else (GPS_INDEX_ACCURACY_EXCELLENT_METERS.toDouble() / maxAccuracy.toDouble()).coerceIn(0.01, 1.0)
        } else 0.001 
        
        val satsIndex = if (satsUsed >= GPS_INDEX_SATS_TARGET) 1.0 else {
            val diff = GPS_INDEX_SATS_TARGET.toDouble() - satsUsed.toDouble()
            if (diff <= 1.0) 1.0 else 1.0 / diff
        }
        
        val total = ((ageIndex + accIndex + satsIndex) / 3.0).toFloat()
        
        return GpsIndexData(ageIndex.toFloat(), accIndex.toFloat(), satsIndex.toFloat(), total)
    }
}
