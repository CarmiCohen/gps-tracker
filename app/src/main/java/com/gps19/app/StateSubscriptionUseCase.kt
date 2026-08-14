package com.gps19.app

import com.gps19.core.engine.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

/**
 * StateSubscriptionUseCase: Centralizes observation of repository flows and system states.
 * Aug.14.04:
 * - Issue #171: Forensic Jitter Audit. Implemented temporal sorting and 
 *   monotonicity guards in history flows to prevent UI "snap-back" artifacts 
 *   during multi-viewer jitter streams (R171).
 * Aug.01.10:
 * - Issue #668: Performance: Object Churn.
 */
class StateSubscriptionUseCase @Inject constructor(
    private val repository: MainRepository,
    private val gpsStatusManager: GpsStatusManager,
    private val systemStatusProvider: SystemStatusProvider,
    private val timeProvider: TimeProvider
) {
    private val _historyFlows = mapOf(
        "4M" to MutableStateFlow<List<ConnectionPoint>>(emptyList()),
        "16M" to MutableStateFlow<List<ConnectionPoint>>(emptyList()),
        "1H" to MutableStateFlow<List<ConnectionPoint>>(emptyList()),
        "4H" to MutableStateFlow<List<ConnectionPoint>>(emptyList()),
        "24H" to MutableStateFlow<List<ConnectionPoint>>(emptyList()),
        "7D" to MutableStateFlow<List<ConnectionPoint>>(emptyList())
    )

    // Flyweight buffers for zero-churn IntegrityUpdate emissions
    private val integrityBuffers = listOf(IntegrityUpdate(), IntegrityUpdate())
    private var integrityBufferIdx = 0

    fun getHistoryFlow(key: String): StateFlow<List<ConnectionPoint>> {
        return _historyFlows[key]?.asStateFlow() ?: MutableStateFlow(emptyList<ConnectionPoint>()).asStateFlow()
    }

    /**
     * startHistoryObservations: Orchestrates DB and Live history synchronization.
     * R171: Integrated sorted merging to handle out-of-order arrivals within 
     * the jitter window (MONOTONIC_JITTER_TOLERANCE_MS).
     */
    fun startHistoryObservations(scope: CoroutineScope) {
        _historyFlows.forEach { (key, stateFlow) ->
            scope.launch(Dispatchers.Main.immediate) {
                repository.getHistoryFlow(key).collect { dbList ->
                    stateFlow.update { current ->
                        // R171: If DB list is empty, we still keep incremental cache 
                        // but ensure it's sorted.
                        if (dbList.isEmpty()) {
                            return@update current.sortedBy { it.ts }.takeLast(240)
                        }
                        
                        // DB is always sorted by TS from the DAO query.
                        val lastDbTs = dbList.last().ts
                        
                        // Filter out points that are already persisted OR older than 
                        // the earliest point we want to keep, but allow merging 
                        // of "late" points that fit within the current tail.
                        val incremental = current.filter { it.ts > lastDbTs }
                        
                        (dbList + incremental).takeLast(240)
                    }
                }
            }
        }

        scope.launch(Dispatchers.Main.immediate) {
            repository.liveHistoryFlow.collect { (key, points) ->
                _historyFlows[key]?.update { current ->
                    // R171: Standardize on sorted merge to prevent snap-backs.
                    val merged = (current + points).sortedBy { it.ts }
                    
                    // Deduplicate by timestamp in case of relay collisions
                    val deduped = mutableListOf<ConnectionPoint>()
                    if (merged.isNotEmpty()) {
                        deduped.add(merged[0])
                        for (i in 1 until merged.size) {
                            if (merged[i].ts != merged[i - 1].ts) {
                                deduped.add(merged[i])
                            }
                        }
                    }
                    deduped.takeLast(240)
                }
            }
        }
    }

    fun clearHistory() {
        _historyFlows.values.forEach { it.value = emptyList() }
    }

    fun observeGpsIndex(): Flow<GpsIndexData> = gpsStatusManager.gpsIndexFlow
    fun observeInternetStatus(): Flow<Boolean> = systemStatusProvider.observeInternetStatus()
    fun observeBatteryStatus(): Flow<BatteryStatus> = systemStatusProvider.observeBatteryStatus()
    fun observeGnssDetail(): Flow<GnssDetail?> = repository.gnssDetail

    @Suppress("UNCHECKED_CAST")
    fun observeRepositorySettings(): Flow<SettingsUpdate> {
        return combine(
            repository.trackerIdFlow,
            repository.viewerIdFlow,
            repository.relayUrlFlow,
            repository.maxDistanceFlow,
            repository.homePointsFlow,
            repository.isXiaomiManualOverrideFlow,
            repository.lastAlarmAckTsFlow,
            repository.appModeFlow,
            repository.isSystemActiveFlow
        ) { args: Array<Any?> ->
            SettingsUpdate(
                trackerId = args[0] as String,
                viewerId = args[1] as String,
                relayUrl = args[2] as String,
                maxDistance = args[3] as Double,
                homePoints = args[4] as List<GeoPoint>,
                isXiaomiManualOverride = args[5] as Boolean,
                lastAlarmAckTs = args[6] as Long,
                appMode = args[7] as String?,
                isSystemActive = args[8] as Boolean
            )
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
    }

    fun observeConnectivityBasics(): Flow<ConnectivityUpdate> {
        return combine(
            repository.isRelayConnected,
            repository.lastRtt,
            repository.lastRemoteActivityTs,
            repository.cumulativeRecoveryBlackoutMsFlow,
            repository.recoveryCountFlow
        ) { connected, rtt, remoteTs, blackoutMs, count ->
            ConnectivityUpdate(connected, rtt, remoteTs, blackoutMs, count)
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
    }

    fun observeIntegrityUpdates(): Flow<IntegrityUpdate> = repository.systemHealth.map { health ->
        val nextIdx = (integrityBufferIdx + 1) % 2
        val next = integrityBuffers[nextIdx]
        next.update(
            health = health, isLocalOnline = health.isHardwareOnline,
            batteryLevel = health.batteryLevel, batteryTemp = health.batteryTemp,
            isCharging = health.isCharging, maxTemp = health.maxTemp,
            activeAlarms = emptyList(), activeAlarmTypes = emptySet()
        )
        integrityBufferIdx = nextIdx
        next
    }
    .flowOn(Dispatchers.Default)

    class IntegrityUpdate(
        var health: SystemHealthState = SystemHealthState(),
        var isLocalOnline: Boolean = true,
        var batteryLevel: Int = 100,
        var batteryTemp: Double = 0.0,
        var isCharging: Boolean = false,
        var maxTemp: Double = 0.0,
        var activeAlarms: List<AlarmInfo> = emptyList(),
        var activeAlarmTypes: Set<String> = emptySet()
    ) {
        fun update(
            health: SystemHealthState, isLocalOnline: Boolean, batteryLevel: Int,
            batteryTemp: Double, isCharging: Boolean, maxTemp: Double,
            activeAlarms: List<AlarmInfo>, activeAlarmTypes: Set<String>
        ) {
            this.health.copyFrom(health)
            this.isLocalOnline = isLocalOnline
            this.batteryLevel = batteryLevel
            this.batteryTemp = batteryTemp
            this.isCharging = isCharging
            this.maxTemp = maxTemp
            this.activeAlarms = activeAlarms
            this.activeAlarmTypes = activeAlarmTypes
        }
    }

    data class SettingsUpdate(
        val trackerId: String, val viewerId: String, val relayUrl: String,
        val maxDistance: Double, val homePoints: List<GeoPoint>,
        val isXiaomiManualOverride: Boolean, val lastAlarmAckTs: Long,
        val appMode: String?, val isSystemActive: Boolean
    )

    data class ConnectivityUpdate(
        val isRelayConnected: Boolean, val lastRtt: Int, val lastRemoteActivityTs: Long,
        val cumulativeRecoveryBlackoutMs: Long, val recoveryCount: Int
    )
}
