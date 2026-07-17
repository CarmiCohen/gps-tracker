package com.gps19.app

import com.gps19.core.engine.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.osmdroid.util.GeoPoint
import timber.log.Timber

/**
 * StateSubscriptionUseCase: Centralizes observation of repository flows and system states.
 * July.17.02:
 * - Added isSystemActive to observeRepositorySettings.
 * July.16.18:
 * - Issue #516: De-duplicate "Status" Logic. Use SystemHealthState.
 */
class StateSubscriptionUseCase(
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
            scope.launch {
                repository.getHistoryFlow(key).collect { dbList ->
                    stateFlow.update { current ->
                        val lastDbTs = dbList.lastOrNull()?.ts ?: 0L
                        val incremental = current.filter { it.ts > lastDbTs }
                        (dbList + incremental).takeLast(240)
                    }
                }
            }
        }

        scope.launch {
            repository.liveHistoryFlow.collect { (key, points) ->
                _historyFlows[key]?.update { (it + points).takeLast(240) }
            }
        }
    }

    fun clearHistory() {
        _historyFlows.values.forEach { it.value = emptyList() }
    }

    fun observeGpsIndex(): Flow<GpsIndexData> {
        val nowFlow = flow {
            while (true) {
                emit(timeProvider.currentTimeMillis())
                delay(TICK_INTERVAL_MS)
            }
        }
        return gpsStatusManager.observeGpsIndex(nowFlow)
    }

    fun observeInternetStatus(): Flow<Boolean> = systemStatusProvider.observeInternetStatus()

    fun observeBatteryStatus(): Flow<BatteryStatus> = systemStatusProvider.observeBatteryStatus()

    fun observeGnssDetail(): Flow<GnssDetail?> = repository.gnssDetail

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
    }

    fun observeConnectivityBasics(): Flow<ConnectivityUpdate> {
        return combine(
            repository.isRelayConnected,
            repository.lastRtt,
            repository.lastRemoteActivityTs
        ) { connected, rtt, remoteTs ->
            ConnectivityUpdate(connected, rtt, remoteTs)
        }
    }

    fun observeIntegrityUpdates(): Flow<IntegrityUpdate> = repository.systemHealth.map { health ->
        // Note: Alarms are now managed by AppAlarmManager and synchronized via LogManager/Repository
        // We can still derive active alarm types from health if needed, but for now we keep the UI compatible.
        val alarms = emptyList<AlarmInfo>() // Alarms to be refactored in a future issue if needed

        IntegrityUpdate(
            health = health,
            isLocalOnline = health.isHardwareOnline,
            batteryLevel = health.batteryLevel,
            batteryTemp = health.batteryTemp,
            isCharging = health.isCharging,
            maxTemp = health.maxTemp,
            activeAlarms = alarms,
            activeAlarmTypes = emptySet()
        )
    }

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
