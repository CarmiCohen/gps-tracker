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
 * Oct.15.3:
 * - SIMP-OCT15.2-S1: Ribbon Selector Consolidation. Pre-calculated normalized 
 *   arrays in HistoryBuffer to offload UI thread calculations. Fixed missing commNorm.
 * Oct.15.2:
 * - SIMP-OCT15.1-S1: Primitive History Buffers. Refactored history observation 
 *   to use HistoryBuffer (primitive arrays), significantly reducing O(N) object 
 *   allocations during ribbon updates. Fixed syntax error in SettingsUpdate.
 * Oct.15.1:
 * - SIMP-OCT15.0-S1: Trail Buffer Optimization. Updated findClosestTrailPoint to 
 *   support TrailBuffer (primitive arrays), reducing object overhead during replay.
 */
class StateSubscriptionUseCase @Inject constructor(
    private val repository: MainRepository,
    private val gpsStatusManager: GpsStatusManager,
    private val systemStatusProvider: SystemStatusProvider,
    private val timeProvider: TimeProvider,
    private val alarmManager: AppAlarmManager
) {
    private val _historyFlows = mapOf(
        "4M" to MutableStateFlow<HistoryBuffer>(HistoryBuffer.EMPTY),
        "16M" to MutableStateFlow<HistoryBuffer>(HistoryBuffer.EMPTY),
        "1H" to MutableStateFlow<HistoryBuffer>(HistoryBuffer.EMPTY),
        "4H" to MutableStateFlow<HistoryBuffer>(HistoryBuffer.EMPTY),
        "24H" to MutableStateFlow<HistoryBuffer>(HistoryBuffer.EMPTY),
        "7D" to MutableStateFlow<HistoryBuffer>(HistoryBuffer.EMPTY)
    )

    private val integrityBuffers = listOf(IntegrityUpdate(), IntegrityUpdate())
    private var integrityBufferIdx = 0

    fun getHistoryFlow(key: String): StateFlow<HistoryBuffer> {
        return _historyFlows[key]?.asStateFlow() ?: MutableStateFlow(HistoryBuffer.EMPTY).asStateFlow()
    }

    fun startHistoryObservations(scope: CoroutineScope) {
        _historyFlows.forEach { (key, stateFlow) ->
            scope.launch(Dispatchers.Default) {
                repository.getHistoryFlow(key).collect { dbBuffer ->
                    val merged = withContext(Dispatchers.Default) {
                        val current = stateFlow.value
                        if (dbBuffer.size == 0) {
                            current // Keep current if DB is empty
                        } else {
                            val lastDbTs = dbBuffer.ts[dbBuffer.size - 1]
                            val currentPoints = mutableListOf<ConnectionPoint>()
                            for (i in 0 until current.size) {
                                if (current.ts[i] > lastDbTs) {
                                    currentPoints.add(current.getPoint(i))
                                }
                            }
                            
                            val dbPoints = mutableListOf<ConnectionPoint>()
                            for (i in 0 until dbBuffer.size) {
                                dbPoints.add(dbBuffer.getPoint(i))
                            }
                            
                            mapPointsToBuffer((dbPoints + currentPoints).takeLast(240))
                        }
                    }
                    stateFlow.emit(merged)
                }
            }
        }

        scope.launch(Dispatchers.Default) {
            repository.liveHistoryFlow.collect { (key, pointsBuffer) ->
                _historyFlows[key]?.let { stateFlow ->
                    val merged = withContext(Dispatchers.Default) {
                        val current = stateFlow.value
                        val combined = mutableListOf<ConnectionPoint>()
                        for (i in 0 until current.size) combined.add(current.getPoint(i))
                        for (i in 0 until pointsBuffer.size) combined.add(pointsBuffer.getPoint(i))
                        
                        combined.sortBy { it.ts }
                        val deduped = mutableListOf<ConnectionPoint>()
                        if (combined.isNotEmpty()) {
                            deduped.add(combined[0])
                            for (i in 1 until combined.size) {
                                if (combined[i].ts != combined[i - 1].ts) {
                                    deduped.add(combined[i])
                                }
                            }
                        }
                        mapPointsToBuffer(deduped.takeLast(240))
                    }
                    stateFlow.emit(merged)
                }
            }
        }
    }

    private fun mapPointsToBuffer(points: List<ConnectionPoint>): HistoryBuffer {
        val size = points.size
        val ts = LongArray(size); val rt = LongArray(size); val rtt = IntArray(size)
        val localSig = IntArray(size); val remoteSig = IntArray(size)
        val isConnected = BooleanArray(size); val isGap = BooleanArray(size)
        val isRecoveryEvent = BooleanArray(size); val hasGps = BooleanArray(size)
        val gpsIndex = DoubleArray(size); val snrIdx = DoubleArray(size)
        val noiseIdx = DoubleArray(size); val luxIdx = DoubleArray(size)
        val vibeIdx = DoubleArray(size); val proxIdx = DoubleArray(size)
        val liftIdx = DoubleArray(size); val tiltIdx = DoubleArray(size)
        val baroIdx = DoubleArray(size); val isBatterySteepDischarge = BooleanArray(size)
        val isCoolingModeActive = BooleanArray(size); val currentMa = IntArray(size)
        val isSitActive = BooleanArray(size); val sitVz = DoubleArray(size)
        val sitVzTs = LongArray(size); val sitDz = DoubleArray(size)
        val cpuLoad = DoubleArray(size); val ioWait = DoubleArray(size)
        val maxIoLatency = LongArray(size); val kineticEnergy = DoubleArray(size)

        val snrNorm = FloatArray(size)
        val noiseNorm = FloatArray(size)
        val kineticNorm = FloatArray(size)
        val luxNorm = FloatArray(size)
        val vibeNorm = FloatArray(size)
        val proxNorm = FloatArray(size)
        val liftNorm = FloatArray(size)
        val batNorm = FloatArray(size)
        val thmNorm = FloatArray(size)
        val curNorm = FloatArray(size)
        val sitNorm = FloatArray(size)
        val tltNorm = FloatArray(size)
        val baroNorm = FloatArray(size)
        val svzNorm = FloatArray(size)
        val sdzNorm = FloatArray(size)
        val cpuNorm = FloatArray(size)
        val iowNorm = FloatArray(size)
        val latNorm = FloatArray(size)
        val commNorm = FloatArray(size)

        points.forEachIndexed { i, p ->
            ts[i] = p.ts; rt[i] = p.rt; rtt[i] = p.rtt; localSig[i] = p.localSig; remoteSig[i] = p.remoteSig
            isConnected[i] = p.isConnected; isGap[i] = p.isGap; isRecoveryEvent[i] = p.isRecoveryEvent
            hasGps[i] = p.hasGps; gpsIndex[i] = p.gpsIndex; snrIdx[i] = p.snrIdx; noiseIdx[i] = p.noiseIdx
            luxIdx[i] = p.luxIdx; vibeIdx[i] = p.vibeIdx; proxIdx[i] = p.proxIdx; liftIdx[i] = p.liftIdx
            tiltIdx[i] = p.tiltIdx; baroIdx[i] = p.baroIdx; isBatterySteepDischarge[i] = p.isBatterySteepDischarge
            isCoolingModeActive[i] = p.isCoolingModeActive; currentMa[i] = p.currentMa; isSitActive[i] = p.isSitActive
            sitVz[i] = p.sitVz; sitVzTs[i] = p.sitVzTs; sitDz[i] = p.sitDz
            cpuLoad[i] = p.cpuLoad; ioWait[i] = p.ioWait; maxIoLatency[i] = p.maxIoLatency; kineticEnergy[i] = p.kineticEnergy

            snrNorm[i] = p.snrIdx.toFloat()
            noiseNorm[i] = p.noiseIdx.toFloat()
            kineticNorm[i] = (p.kineticEnergy.toFloat() / RIBBON_KINETIC_ENERGY_SCALE.toFloat()).coerceIn(0f, 1f)
            luxNorm[i] = p.luxIdx.toFloat()
            vibeNorm[i] = p.vibeIdx.toFloat()
            proxNorm[i] = p.proxIdx.toFloat()
            liftNorm[i] = p.liftIdx.toFloat()
            batNorm[i] = if (p.isBatterySteepDischarge) 1f else 0f
            thmNorm[i] = if (p.isCoolingModeActive) 1f else 0f
            curNorm[i] = (kotlin.math.abs(p.currentMa).toFloat() / RIBBON_CURRENT_SCALE_MA.toFloat()).coerceIn(0f, 1f)
            sitNorm[i] = if (p.isSitActive) 1f else 0f
            tltNorm[i] = p.tiltIdx.toFloat()
            baroNorm[i] = p.baroIdx.toFloat()
            svzNorm[i] = (kotlin.math.abs(p.sitVz).toFloat() / 2.0f).coerceIn(0f, 1f)
            sdzNorm[i] = (kotlin.math.abs(p.sitDz).toFloat() / 0.5f).coerceIn(0f, 1f)
            cpuNorm[i] = (p.cpuLoad.toFloat() / RIBBON_CPU_LOAD_SCALE.toFloat()).coerceIn(0f, 1f)
            iowNorm[i] = (p.ioWait.toFloat() / RIBBON_IO_WAIT_SCALE.toFloat()).coerceIn(0f, 1f)
            latNorm[i] = (p.maxIoLatency.toFloat() / RIBBON_LATENCY_SCALE_MS.toFloat()).coerceIn(0f, 1f)
            val commIdx = TelemetryUtils.calculateCommIndex(p.rtt, p.localSig, p.remoteSig).toFloat()
            commNorm[i] = if (p.isConnected) (commIdx / 10f).coerceIn(0.1f, 1f) else 1f
        }

        return HistoryBuffer(
            ts, rt, rtt, localSig, remoteSig, isConnected, isGap, isRecoveryEvent, hasGps,
            gpsIndex, snrIdx, noiseIdx, luxIdx, vibeIdx, proxIdx, liftIdx, tiltIdx, baroIdx,
            isBatterySteepDischarge, isCoolingModeActive, currentMa, isSitActive,
            sitVz, sitVzTs, sitDz, cpuLoad, ioWait, maxIoLatency, kineticEnergy,
            snrNorm, noiseNorm, kineticNorm, luxNorm, vibeNorm, proxNorm, liftNorm,
            batNorm, thmNorm, curNorm, sitNorm, tltNorm, baroNorm, svzNorm, sdzNorm,
            cpuNorm, iowNorm, latNorm, commNorm, size
        )
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
        _historyFlows.values.forEach { it.value = HistoryBuffer.EMPTY }
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
        val isXiaomiManualOverride: Boolean = false, val lastAlarmAckTs: Long,
        val appMode: String?, val isSystemActive: Boolean,
        val identitySanitized: Boolean,
        val alertSettings: AlertSettings
    )

    data class ConnectivityUpdate(
        val isRelayConnected: Boolean, val lastRtt: Int, val lastRemoteActivityTs: Long,
        val cumulativeRecoveryBlackoutMs: Long, val recoveryCount: Int
    )
}
