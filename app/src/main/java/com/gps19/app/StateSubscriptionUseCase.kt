package com.gps19.app

import com.gps19.core.engine.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

/**
 * StateSubscriptionUseCase: Centralizes observation of repository flows and system states.
 * Oct.15.1:
 * - SIMP-OCT15.0-S1: Trail Buffer Optimization. Updated findClosestTrailPoint to 
 *   support TrailBuffer (primitive arrays), reducing object overhead during replay.
 * Sep.30.42:
 * - Issue #1390: Integrated reactive camera flow support.
 */
class StateSubscriptionUseCase @Inject constructor(
    private val repository: MainRepository,
    private val gpsStatusManager: GpsStatusManager,
    private val systemStatusProvider: SystemStatusProvider,
    private val timeProvider: TimeProvider,
    private val alarmManager: AppAlarmManager
) {
    private val _historyFlows = mapOf(
        "4M" to MutableStateFlow<List<ConnectionPoint>>(emptyList()),
        "16M" to MutableStateFlow<List<ConnectionPoint>>(emptyList()),
        "1H" to MutableStateFlow<List<ConnectionPoint>>(emptyList()),
        "4H" to MutableStateFlow<List<ConnectionPoint>>(emptyList()),
        "24H" to MutableStateFlow<List<ConnectionPoint>>(emptyList()),
        "7D" to MutableStateFlow<List<ConnectionPoint>>(emptyList())
    )

    private val integrityBuffers = listOf(IntegrityUpdate(), IntegrityUpdate())
    private var integrityBufferIdx = 0

    fun getHistoryFlow(key: String): StateFlow<List<ConnectionPoint>> {
        return _historyFlows[key]?.asStateFlow() ?: MutableStateFlow(emptyList<ConnectionPoint>()).asStateFlow()
    }

    fun startHistoryObservations(scope: CoroutineScope) {
        _historyFlows.forEach { (key, stateFlow) ->
            scope.launch(Dispatchers.Default) {
                repository.getHistoryFlow(key).collect { dbList ->
                    val merged = withContext(Dispatchers.Default) {
                        val current = stateFlow.value
                        if (dbList.isEmpty()) {
                            current.sortedBy { it.ts }.takeLast(240)
                        } else {
                            val lastDbTs = dbList.last().ts
                            val incremental = current.filter { it.ts > lastDbTs }
                            (dbList + incremental).takeLast(240)
                        }
                    }
                    stateFlow.emit(merged)
                }
            }
        }

        scope.launch(Dispatchers.Default) {
            repository.liveHistoryFlow.collect { (key, points) ->
                _historyFlows[key]?.let { stateFlow ->
                    val merged = withContext(Dispatchers.Default) {
                        val current = stateFlow.value
                        val combined = (current + points).sortedBy { it.ts }
                        val deduped = mutableListOf<ConnectionPoint>()
                        if (combined.isNotEmpty()) {
                            deduped.add(combined[0])
                            for (i in 1 until combined.size) {
                                if (combined[i].ts != combined[i - 1].ts) {
                                    deduped.add(combined[i])
                                }
                            }
                        }
                        deduped.takeLast(240)
                    }
                    stateFlow.emit(merged)
                }
            }
        }
    }

    /**
     * findClosestTrailPoint: Binary search on TrailBuffer timestamps (SIMP-OCT15.0-S1).
     */
    fun findClosestTrailPoint(trail: TrailBuffer, targetTs: Long): TrailPoint? {
        if (trail.size == 0) return null
        
        var low = 0
        var high = trail.size - 1
        var bestIdx = -1

        while (low <= high) {
            val mid = (low + high) / 2
            val midTs = trail.timestamps[mid]
            if (midTs == targetTs) {
                bestIdx = mid
                break
            } else if (midTs < targetTs) {
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        if (bestIdx == -1) {
            bestIdx = when {
                low >= trail.size -> trail.size - 1
                low <= 0 -> 0
                else -> {
                    val d1 = kotlin.math.abs(trail.timestamps[low] - targetTs)
                    val d2 = kotlin.math.abs(trail.timestamps[low - 1] - targetTs)
                    if (d1 < d2) low else low - 1
                }
            }
        }
        
        return trail.getPoint(bestIdx)
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
            repository.isSystemActiveFlow,
            repository.identitySanitizedFlow,
            repository.alertSettingsFlow
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
                isSystemActive = args[8] as Boolean,
                identitySanitized = args[9] as Boolean,
                alertSettings = args[10] as AlertSettings
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
     * Issue #1389: Reactive Integrity Updates.
     */
    fun observeIntegrityUpdates(): Flow<IntegrityUpdate> {
        return combine(
            repository.systemHealth,
            alarmManager.activeAlarmsFlow
        ) { health, alarms ->
            val nextIdx = (integrityBufferIdx + 1) % 2
            val next = integrityBuffers[nextIdx]
            
            next.update(
                health = health, isLocalOnline = health.isHardwareOnline,
                batteryLevel = health.batteryLevel, batteryTemp = health.batteryTemp,
                isCharging = health.isCharging, maxTemp = health.maxTemp,
                activeAlarms = alarms, activeAlarmTypes = alarms.map { it.type }.toSet()
            )
            integrityBufferIdx = nextIdx
            next
        }.flowOn(Dispatchers.Default)
    }

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
        val appMode: String?, val isSystemActive: Boolean,
        val identitySanitized: Boolean,
        val alertSettings: AlertSettings
    )

    data class ConnectivityUpdate(
        val isRelayConnected: Boolean, val lastRtt: Int, val lastRemoteActivityTs: Long,
        val cumulativeRecoveryBlackoutMs: Long, val recoveryCount: Int
    )
}
