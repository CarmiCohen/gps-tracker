package com.gps19.app

import com.gps19.core.engine.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GpsStatusManager: Centralized reactive Flow for the GPS-Index.
 * Aug.20.00:
 * - Issue #219 Performance Audit: Implemented flowOn(Dispatchers.Default) 
 *   and 500ms sampling to prevent UI thread jitter during 100Hz forensic 
 *   bursts. Offloads weighted averaging math from the UI pulse (R219).
 * July.26.03:
 * - Issue #545c: Flow Architecture Standardization. Refactored to manage a 
 *   SharedFlow pipeline using @ApplicationScope, ensuring all collectors 
 *   share a single logic pulse and calculation sequence.
 */
@Singleton
class GpsStatusManager @Inject constructor(
    private val telemetryRepository: TelemetryRepository,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider,
    @ApplicationScope private val externalScope: CoroutineScope
) {
    private data class IndexParams(val gpsTs: Long, val maxAccuracy: Double, val satsUsed: Int)

    /**
     * gpsIndexFlow: Standardized SharedFlow for GPS Index updates.
     * Aug.20.00: Throttled to 2Hz (500ms) to ensure smooth UI updates 
     * during high-frequency sensor telemetry bursts.
     */
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val gpsIndexFlow: SharedFlow<GpsIndexData> = flow {
        while (true) {
            emit(timeProvider.currentTimeMillis())
            delay(TICK_INTERVAL_MS)
        }
    }.flatMapLatest { now ->
        combine(
            settingsRepository.appModeFlow,
            telemetryRepository.isRelayConnected,
            telemetryRepository.localLocation,
            telemetryRepository.trackerLocation
        ) { appMode, isRelayConnected, localUpdate, trackerUpdate ->
            val isTracker = appMode == "tracker"
            val effectiveUpdate = if (isTracker) localUpdate else if (isRelayConnected) trackerUpdate else null
            
            val params = if (effectiveUpdate != null && 
                effectiveUpdate.gpsTs > 0 && 
                PhysicsUtils.isValidLocation(effectiveUpdate.lat, effectiveUpdate.lng)) {
                IndexParams(effectiveUpdate.gpsTs, effectiveUpdate.maxAccuracy, effectiveUpdate.satsUsed)
            } else null
            
            params to now
        }
    }.sample(500L) // Issue #219: Throttle UI churn during high-frequency bursts
     .scan(GpsIndexData(0.0, 0.0, 0.0, 0.0) to (null as IndexParams?)) { state, (params, now) ->
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
     .distinctUntilChanged()
     .flowOn(Dispatchers.Default) // Issue #219: Ensure math logic runs off-UI
     .shareIn(
        scope = externalScope,
        started = SharingStarted.WhileSubscribed(5000),
        replay = 1
     )

    fun observeGpsIndex(): Flow<GpsIndexData> = gpsIndexFlow
}
