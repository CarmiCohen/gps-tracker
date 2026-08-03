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
 * Aug.01.10:
 * - Issue #668: Performance: Object Churn. Implemented double-buffering for 
 *   IntegrityUpdate to eliminate allocation churn in health observation (R-HARDWARE-01).
 * July.30.29:
 * - Issue #631: Forensic UI: Service Blackout Trends.
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

    /**
     * observeIntegrityUpdates: Swaps flyweight buffers to eliminate allocation churn.
     */
    fun observeIntegrityUpdates(): Flow<IntegrityUpdate> = repository.systemHealth.map { health ->
        val nextIdx = (integrityBufferIdx + 1) % 2
        val next = integrityBuffers[nextIdx]
        
        next.update(
            health = health,
            isLocalOnline = health.isHardwareOnline,
            batteryLevel = health.batteryLevel,
            batteryTemp = health.batteryTemp,
            isCharging = health.isCharging,
            maxTemp = health.maxTemp,
            activeAlarms = emptyList(), // Filled by ViewModel if needed
            activeAlarmTypes = emptySet()
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
        val lastRemoteActivityTs: Long,
        val cumulativeRecoveryBlackoutMs: Long,
        val recoveryCount: Int
    )
}
