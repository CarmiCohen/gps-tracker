package com.gps19.app

import com.gps19.core.engine.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.osmdroid.util.GeoPoint
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * StateSubscriptionUseCase: Centralizes observation of repository flows and system states.
 * v9.3.37:
 * - Issue #092: Added appMode to observeRepositorySettings for reactive UI synchronization.
 * v8.9.79: Issue #014 - Type Migration: Standardized temperature fields to Double.
 * v8.9.5:
 * - Issue #337: Propagated currentMa in observeIntegrityUpdates for power forensic parity.
 */
@Singleton
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
            repository.appModeFlow
        ) { args: Array<Any?> ->
            SettingsUpdate(
                trackerId = args[0] as String,
                viewerId = args[1] as String,
                relayUrl = args[2] as String,
                maxDistance = args[3] as Double,
                homePoints = args[4] as List<GeoPoint>,
                isXiaomiManualOverride = args[5] as Boolean,
                lastAlarmAckTs = args[6] as Long,
                appMode = args[7] as String?
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

    fun observeIntegrityUpdates(): Flow<IntegrityUpdate> = repository.integrityState.map { info ->
        val alarms = if (info.activeAlarmsJson != null) {
            try {
                val array = JSONArray(info.activeAlarmsJson)
                val newList = mutableListOf<AlarmInfo>()
                val activeTypes = mutableSetOf<String>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val type = obj.optString("type", "")
                    val isResolved = obj.getBoolean("isResolved")
                    if (!isResolved) activeTypes.add(type)
                    newList.add(AlarmInfo(
                        title = obj.getString("title"),
                        subtitle = obj.getString("subtitle"),
                        type = type,
                        isResolved = isResolved,
                        isSirenDisabled = obj.optBoolean("isSirenDisabled", false)
                    ))
                }
                newList to activeTypes
            } catch (e: Exception) {
                Timber.e(e, "Error parsing alarms")
                emptyList<AlarmInfo>() to emptySet<String>()
            }
        } else {
            emptyList<AlarmInfo>() to emptySet<String>()
        }

        IntegrityUpdate(
            integrityUi = IntegrityStateUi(
                signalLoss = info.signalLoss,
                gpsStalled = info.gpsStalled,
                jammerSuspicion = info.jammerSuspicion,
                localInternetLoss = info.localInternetLoss,
                isHardwareOnline = info.isHardwareOnline,
                isSuspicious = info.isSuspicious,
                isTamperDetected = info.isTamperDetected,
                isPowerTamper = info.isPowerTamper,
                isSitDetected = info.isSitDetected,
                lastSitTs = info.lastSitTs,
                micPending = info.micPending,
                isLocationPending = info.isLocationPending,
                isPowerSaveMode = info.isPowerSaveMode,
                standbyBucket = info.standbyBucket,
                netInterface = info.netInterface,
                isStorageLow = info.isStorageLow,
                isStorageCritical = info.isStorageCritical,
                isCoolingModeActive = info.isCoolingModeActive,
                currentMa = info.currentMa,
                isAnchorLocked = info.isAnchorLocked
            ),
            isLocalOnline = info.isHardwareOnline,
            batteryLevel = info.batteryLevel,
            batteryTemp = info.batteryTemp,
            isCharging = info.isCharging,
            maxTemp = info.maxTemp,
            activeAlarms = alarms.first,
            activeAlarmTypes = alarms.second
        )
    }

    data class IntegrityUpdate(
        val integrityUi: IntegrityStateUi,
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
        val appMode: String?
    )

    data class ConnectivityUpdate(
        val isRelayConnected: Boolean,
        val lastRtt: Int,
        val lastRemoteActivityTs: Long
    )
}
