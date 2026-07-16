package com.gps19.app

import com.gps19.core.engine.GpsIndexData
import com.gps19.core.engine.TelemetryUtils
import com.gps19.core.engine.PhysicsUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan

/**
 * GpsStatusManager: Centralized reactive Flow for the GPS-Index.
 * v9.5.0:
 * - Issue #503: Hilt Removal. Manual dependency injection.
 */
class GpsStatusManager(
    private val telemetryRepository: TelemetryRepository,
    private val settingsRepository: SettingsRepository
) {
    fun observeGpsIndex(nowFlow: Flow<Long>): Flow<GpsIndexData> {
        data class IndexParams(val gpsTs: Long, val maxAccuracy: Double, val satsUsed: Int)

        return combine(
            settingsRepository.appModeFlow,
            telemetryRepository.isRelayConnected,
            telemetryRepository.localLocation,
            telemetryRepository.trackerLocation,
            nowFlow
        ) { appMode, isRelayConnected, localUpdate, trackerUpdate, now ->
            val isTracker = appMode == "tracker"
            val effectiveUpdate = if (isTracker) localUpdate else if (isRelayConnected) trackerUpdate else null
            
            val params = if (effectiveUpdate != null && 
                effectiveUpdate.gpsTs > 0 && 
                PhysicsUtils.isValidLocation(effectiveUpdate.lat, effectiveUpdate.lng)) {
                IndexParams(effectiveUpdate.gpsTs, effectiveUpdate.maxAccuracy, effectiveUpdate.satsUsed)
            } else null
            
            params to now
        }.scan(GpsIndexData(0.0, 0.0, 0.0, 0.0) to (null as IndexParams?)) { state, (params, now) ->
            val lastParams = state.second
            
            val activeParams = if (params != null && (lastParams == null || params.gpsTs >= lastParams.gpsTs)) {
                params
            } else {
                lastParams
            }
            
            if (activeParams != null) {
                val index = TelemetryUtils.calculateGpsIndex(
                    gpsAgeMs = now - activeParams.gpsTs,
                    maxAccuracy = activeParams.maxAccuracy,
                    satsUsed = activeParams.satsUsed
                )
                index to activeParams
            } else {
                GpsIndexData(0.0, 0.0, 0.0, 0.0) to null
            }
        }.map { it.first }
    }
}
