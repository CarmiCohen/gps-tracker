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
 * July.28.23:
 * - Issue #618: Forensic UI State Collection Audit. Migrated history 
 *   observation collection to Dispatchers.Main.immediate to reduce 
 *   dispatch latency for UI updates.
 * July.26.03:
 * - Issue #545c: Flow Architecture Standardization.
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

    fun getHistoryFlow(key: String): StateFlow<List<ConnectionPoint>> {
        return _historyFlows[key]?.asStateFlow() ?: MutableStateFlow(emptyList<ConnectionPoint>()).asStateFlow()
    }

    fun startHistoryObservations(scope: CoroutineScope) {
        _historyFlows.forEach { (key, stateFlow) ->
            scope.launch(Dispatchers.Main.immediate) {
                repository.getHistoryFlow(key).collect { dbList ->
                    stateFlow.update { current ->
                        val lastDbTs = dbList.lastOrNull()?.ts ?: 0L
                        val incremental = current.filter { it.ts > lastDbTs }
                        (dbList + incremental).takeLast(240)
                    }
                }
            }
        }

        scope.launch(Dispatchers.Main.immediate) {
            repository.liveHistoryFlow.collect { (key, points) ->
                _historyFlows[key]?.update { (it + points).takeLast(240) }
            }
        }
    }

    fun clearHistory() {
        _historyFlows.values.forEach { it.value = emptyList() }
    }

    /**
     * observeGpsIndex: Standardized pass-through to GpsStatusManager.
     */
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
        }.flowOn(Dispatchers.Default)
    }

    fun observeConnectivityBasics(): Flow<ConnectivityUpdate> {
        return combine(
            repository.isRelayConnected,
            repository.lastRtt,
            repository.lastRemoteActivityTs
        ) { connected, rtt, remoteTs ->
            ConnectivityUpdate(connected, rtt, remoteTs)
        }.flowOn(Dispatchers.Default)
    }

    fun observeIntegrityUpdates(): Flow<IntegrityUpdate> = repository.systemHealth.map { health ->
        IntegrityUpdate(
            health = health,
            isLocalOnline = health.isHardwareOnline,
            batteryLevel = health.batteryLevel,
            batteryTemp = health.batteryTemp,
            isCharging = health.isCharging,
            maxTemp = health.maxTemp,
            activeAlarms = emptyList(),
            activeAlarmTypes = emptySet()
        )
    }.flowOn(Dispatchers.Default)

    data class IntegrityUpdate(
        val health: SystemHealthState,
        val isLocalOnline: Boolean,
        val batteryLevel: Int,
        val batteryTemp: Double,
        val isCharging: Boolean,
        val maxTemp: Double,
        val activeAlarms: List<AlarmInfo>,
        val activeAlarmTypes: Set<String>
    )

    data class SettingsUpdate(
        val trackerId: String,
        val viewerId: String,
        val relayUrl: String,
        val maxDistance: Double,
        val homePoints: List<GeoPoint>,
        val isXiaomiManualOverride: Boolean,
        val lastAlarmAckTs: Long,
        val appMode: String?,
        val isSystemActive: Boolean
    )

    data class ConnectivityUpdate(
        val isRelayConnected: Boolean,
        val lastRtt: Int,
        val lastRemoteActivityTs: Long
    )
}
