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
 * Oct.10.3:
 * - Issue #SIMP-1010-4: HUD Interface Alignment. Refactored to property-based 
 *   TimeProvider API and fixed property invocation errors.
 */
@Singleton
class GpsStatusManager @Inject constructor(
    private val telemetryRepository: TelemetryRepository,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider,
    @ApplicationScope private val externalScope: CoroutineScope
) {
    private data class IndexParams(val rt: Long, val maxAccuracy: Double, val satsUsed: Int)

    /**
     * gpsIndexFlow: Standardized SharedFlow for GPS Index updates.
     * Aug.20.00: Throttled to 2Hz (500ms) to ensure smooth UI updates 
     * during high-frequency sensor telemetry bursts.
     */
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val gpsIndexFlow: SharedFlow<GpsIndexData> = flow {
        while (true) {
            emit(timeProvider.elapsedRealtime)
            delay(TICK_INTERVAL_MS)
        }
    }.flatMapLatest { nowRt ->
        combine(
            settingsRepository.appModeFlow,
            telemetryRepository.isRelayConnected,
            telemetryRepository.localLocation,
            telemetryRepository.trackerLocation
        ) { appMode, isRelayConnected, localUpdate, trackerUpdate ->
            val isTracker = appMode == "tracker"
            val effectiveUpdate = if (isTracker) localUpdate else if (isRelayConnected) trackerUpdate else null
            
            val params = if (effectiveUpdate != null && 
                effectiveUpdate.rt > 0 && 
                PhysicsUtils.isValidLocation(effectiveUpdate.kinetic.lat, effectiveUpdate.kinetic.lng)) {
                IndexParams(effectiveUpdate.rt, effectiveUpdate.kinetic.maxAccuracy, effectiveUpdate.integrity.satsUsed)
            } else null
            
            params to nowRt
        }
    }.sample(500L)
     .scan(GpsIndexData(0.0, 0.0, 0.0, 0.0) to (null as IndexParams?)) { state, (params, nowRt) ->
        val lastParams = state.second
        val activeParams = if (params != null && (lastParams == null || params.rt >= lastParams.rt)) {
            params
        } else {
            lastParams
        }
        
        if (activeParams != null) {
            val index = TelemetryUtils.calculateGpsIndex(
                gpsAgeMs = nowRt - activeParams.rt,
                maxAccuracy = activeParams.maxAccuracy,
                satsUsed = activeParams.satsUsed
            )
            index to activeParams
        } else {
            GpsIndexData(0.0, 0.0, 0.0, 0.0) to null
        }
    }.map { it.first }
     .distinctUntilChanged()
     .flowOn(Dispatchers.Default)
     .shareIn(
        scope = externalScope,
        started = SharingStarted.WhileSubscribed(5000),
        replay = 1
     )

    fun observeGpsIndex(): Flow<GpsIndexData> = gpsIndexFlow
}
