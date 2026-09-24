package com.gps19.app

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.gps19.core.engine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extension property to ensure a single instance of DataStore per process.
 */
private val Context.settingsDataStore: DataStore<AppSettings> by dataStore(
    fileName = "app_settings.pb",
    serializer = AppSettingsSerializer,
    produceMigrations = { context ->
        listOf(
            AppSettingsMigration(context),
            SettingsRepository.identitySanitizationMigration
        )
    }
)

/**
 * Generic extension function to mutate DataStore<AppSettings> atomically and race-free.
 */
private suspend inline fun DataStore<AppSettings>.mutate(
    crossinline block: AppSettings.Builder.() -> Unit
): AppSettings {
    return updateData { current ->
        val builder = current.toBuilder()
        builder.block()
        builder.build()
    }
}

data class CommitResult(
    val trackerIdChanged: Boolean = false,
    val viewerIdChanged: Boolean = false,
    val relayUrlChanged: Boolean = false,
    val maxDistanceChanged: Boolean = false,
    val alertsChanged: Boolean = false,
    val anyChanged: Boolean = false,
    val error: String? = null
)

/**
 * SettingsRepository: Manages persistent application settings using DataStore.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timeProvider: TimeProvider
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val dataStore = context.settingsDataStore

    companion object {
        const val DEFAULT_RELAY_URL = "https://gps-survival-relay.onrender.com"
        const val DEFAULT_TRACKER_ID = SignalingConstants.DEFAULT_TRACKER_ID
        const val DEFAULT_VIEWER_ID = SignalingConstants.DEFAULT_VIEWER_ID
        const val DEFAULT_MAX_DISTANCE = 60.0

        internal val identitySanitizationMigration = object : DataMigration<AppSettings> {
            override suspend fun shouldMigrate(currentData: AppSettings): Boolean {
                val t = currentData.trackerId
                val v = currentData.viewerId
                return (t.isNotEmpty() && !SignalingConstants.isValidTrackerId(t)) ||
                       (v.isNotEmpty() && !SignalingConstants.isValidViewerId(v))
            }

            override suspend fun migrate(currentData: AppSettings): AppSettings {
                val builder = currentData.toBuilder()
                if (currentData.trackerId.isNotEmpty() && !SignalingConstants.isValidTrackerId(currentData.trackerId)) {
                    builder.setTrackerId(DEFAULT_TRACKER_ID)
                    builder.setIdentitySanitized(true)
                }
                if (currentData.viewerId.isNotEmpty() && !SignalingConstants.isValidViewerId(currentData.viewerId)) {
                    builder.setViewerId(DEFAULT_VIEWER_ID)
                    builder.setIdentitySanitized(true)
                }
                return builder.build()
            }

            override suspend fun cleanUp() {}
        }
    }

    val appModeFlow: Flow<String?> = dataStore.data.map { it.appMode.ifEmpty { null } }
    val trackerIdFlow: Flow<String> = dataStore.data.map { it.trackerId.ifEmpty { DEFAULT_TRACKER_ID } }
    val viewerIdFlow: Flow<String> = dataStore.data.map { it.viewerId.ifEmpty { DEFAULT_VIEWER_ID } }
    val relayUrlFlow: Flow<String> = dataStore.data.map { it.relayUrl.ifEmpty { DEFAULT_RELAY_URL } }
    val isManualExitFlow: Flow<Boolean> = dataStore.data.map { it.isManualExit }
    val lastAlarmAckTsFlow: Flow<Long> = dataStore.data.map { it.lastAlarmAckTs }
    val trackerAlarmAckTsFlow: Flow<Long> = dataStore.data.map { it.roleLongsMap.getOrDefault("T_$LAST_ALARM_ACK_TS_KEY", 0L) }
    val viewerAlarmAckTsFlow: Flow<Long> = dataStore.data.map { it.roleLongsMap.getOrDefault("V_$LAST_ALARM_ACK_TS_KEY", 0L) }
    val homePointsFlow: Flow<List<GeoPoint>> = dataStore.data.map { it.homePointsList.map { p -> GeoPoint(p.lat, p.lng) } }
    val maxDistanceFlow: Flow<Double> = dataStore.data.map { if (it.maxDistance > 0.0) it.maxDistance else DEFAULT_MAX_DISTANCE }
    val alertSettingsFlow: Flow<AlertSettings> = dataStore.data.map { SettingsMapper.protoToAlertSettings(it.alertSettings) }
    val identitySanitizedFlow: Flow<Boolean> = dataStore.data.map { it.identitySanitized }
    val isSystemActiveFlow: Flow<Boolean> = dataStore.data.map { it.isSystemActive }
    val lastAlarmsJsonFlow: Flow<String> = dataStore.data.map { it.lastAlarmsJson }
    val isXiaomiManualOverrideFlow: Flow<Boolean> = dataStore.data.map { it.isXiaomiManualOverride }
    val recoveryCountFlow: Flow<Int> = dataStore.data.map { it.recoveryCount }
    val cumulativeRecoveryBlackoutMsFlow: Flow<Long> = dataStore.data.map { it.totalDrop }

    suspend fun getSettingsSnapshot(): AppSettings = dataStore.data.first()

    suspend fun getAppMode(): String? = dataStore.data.first().appMode.ifEmpty { null }

    suspend fun saveString(keyName: String, value: String) {
        dataStore.mutate {
            if (keyName.startsWith("T_") || keyName.startsWith("V_") || keyName.startsWith("VR_")) {
                this.putRoleStrings(keyName, value)
            } else {
                when (keyName) {
                    APP_MODE_KEY -> setAppMode(value)
                    TRACKER_ID_KEY -> setTrackerId(value)
                    VIEWER_ID_KEY -> setViewerId(value)
                    RELAY_URL_KEY -> setRelayUrl(value)
                    SELECTED_SIREN_KEY -> setSelectedSiren(value)
                    LAST_ALARMS_JSON_KEY -> setLastAlarmsJson(value)
                }
            }
        }
    }

    suspend fun saveLong(keyName: String, value: Long) {
        dataStore.mutate {
            if (keyName.startsWith("T_") || keyName.startsWith("V_") || keyName.startsWith("VR_")) {
                this.putRoleLongs(keyName, value)
            } else {
                when (keyName) {
                    LAST_ALARM_ACK_TS_KEY -> setLastAlarmAckTs(value)
                    HOME_POINTS_TS_KEY -> setHomePointsTs(value)
                    LAST_SERVICE_TICK_TS_KEY -> setLastServiceTickTs(value)
                    APP_START_TIME_KEY -> setAppStartTime(value)
                    TOTAL_CONNECTED_KEY -> setTotalConnected(value)
                    UPTIME_KEY -> setUptime(value)
                    LAST_CONNECTION_TS_KEY -> setLastConnectionTs(value)
                    LAST_DISCONNECTION_TS_KEY -> setLastDisconnectionTs(value)
                    TOTAL_DROP_KEY -> setTotalDrop(value)
                    MAX_DROP_KEY -> setMaxDrop(value)
                    MAX_DROP_TS_KEY -> setMaxDropTs(value)
                    LAST_GPS_TS_KEY -> setLastGpsTs(value)
                    VIOLATION_UPTIME_MS_KEY -> setViolationUptimeMs(value)
                    LAST_SERVICE_TICK_REALTIME_KEY -> setLastServiceTickRt(value)
                    CLOCK_DRIFT_REF_KEY -> setClockDriftRef(value)
                    LAST_SIT_TS_KEY -> setLastSitTs(value)
                    FIRST_VIOLATION_TS_KEY -> setFirstViolationTs(value)
                    FIRST_VIOLATION_RT_KEY -> setFirstViolationRt(value)
                    LAST_SIREN_STOP_RT_KEY -> setLastSirenStopRt(value)
                    LAST_GLOBAL_TRIGGER_RT_KEY -> setLastGlobalTriggerRt(value)
                    FORENSIC_RELIABILITY_DEGRADATION_START_RT_KEY -> setForensicReliabilityDegradationStartRt(value)
                }
            }
        }
    }

    suspend fun saveDouble(keyName: String, value: Double) {
        dataStore.mutate {
            if (keyName.startsWith("T_") || keyName.startsWith("V_") || keyName.startsWith("VR_")) {
                this.putRoleDoubles(keyName, value)
            } else {
                when (keyName) {
                    MAX_DISTANCE_STORAGE_KEY -> setMaxDistance(value)
                    MAX_ACCURACY_KEY -> setMaxAccuracy(value)
                    MAX_TEMP_KEY -> setMaxTemp(value)
                    TRACKER_LUX_BASELINE_KEY -> setTrackerLuxBaseline(value)
                    TRACKER_ACOUSTIC_FLOOR_KEY -> setTrackerAcousticFloor(value)
                    CHAIR_BASELINE_TILT_KEY -> setChairBaselineTilt(value)
                }
            }
        }
    }

    suspend fun saveBoolean(keyName: String, value: Boolean) {
        dataStore.mutate {
            if (keyName.startsWith("T_") || keyName.startsWith("V_") || keyName.startsWith("VR_")) {
                this.putRoleBools(keyName, value)
            } else {
                when (keyName) {
                    IS_MANUAL_EXIT_KEY -> setIsManualExit(value)
                    IS_MIC_TYPE_STARTED_KEY -> setIsMicTypeStarted(value)
                    IS_XIAOMI_MANUAL_OVERRIDE_KEY -> setIsXiaomiManualOverride(value)
                    IDENTITY_SANITIZED_KEY -> setIdentitySanitized(value)
                    IS_SYSTEM_ACTIVE_KEY -> setIsSystemActive(value)
                    FIRST_VIOLATION_WAS_JUMP_KEY -> setFirstViolationWasJump(value)
                    WAS_DISTANCE_VIOLATED_KEY -> setWasDistanceViolated(value)
                    POWER_ALARM_PENDING_KEY -> setPowerAlarmPending(value)
                }
            }
        }
    }

    suspend fun saveInt(keyName: String, value: Int) {
        dataStore.mutate {
            if (keyName.startsWith("T_") || keyName.startsWith("V_") || keyName.startsWith("VR_")) {
                this.putRoleInts(keyName, value)
            } else {
                when (keyName) {
                    LAST_AUTO_SAVE_HOUR_KEY -> setLastAutoSaveHour(value)
                    LAST_VERSION_CODE_KEY -> setLastVersionCode(value)
                    RECOVERY_COUNT_KEY -> setRecoveryCount(value)
                    DISTANCE_VIOLATION_COUNTER_KEY -> setDistanceViolationCounter(value)
                }
            }
        }
    }

    suspend fun getString(keyName: String, default: String): String {
        val settings = dataStore.data.first()
        if (keyName.startsWith("T_") || keyName.startsWith("V_") || keyName.startsWith("VR_")) {
            return settings.roleStringsMap.getOrDefault(keyName, default)
        }
        val value = when (keyName) {
            TRACKER_ID_KEY -> settings.trackerId
            VIEWER_ID_KEY -> settings.viewerId
            RELAY_URL_KEY -> settings.relayUrl
            LAST_ALARMS_JSON_KEY -> settings.lastAlarmsJson
            else -> ""
        }
        return value.ifEmpty { default }
    }

    suspend fun getLong(keyName: String, default: Long): Long {
        val settings = dataStore.data.first()
        if (keyName.startsWith("T_") || keyName.startsWith("V_") || keyName.startsWith("VR_")) {
            return settings.roleLongsMap.getOrDefault(keyName, default)
        }
        val value = when (keyName) {
            LAST_ALARM_ACK_TS_KEY -> settings.lastAlarmAckTs
            HOME_POINTS_TS_KEY -> settings.homePointsTs
            LAST_SERVICE_TICK_TS_KEY -> settings.lastServiceTickTs
            APP_START_TIME_KEY -> settings.appStartTime
            TOTAL_CONNECTED_KEY -> settings.totalConnected
            UPTIME_KEY -> settings.uptime
            LAST_CONNECTION_TS_KEY -> settings.lastConnectionTs
            LAST_DISCONNECTION_TS_KEY -> settings.lastDisconnectionTs
            TOTAL_DROP_KEY -> settings.totalDrop
            MAX_DROP_KEY -> settings.maxDrop
            MAX_DROP_TS_KEY -> settings.maxDropTs
            LAST_GPS_TS_KEY -> settings.lastGpsTs
            VIOLATION_UPTIME_MS_KEY -> settings.violationUptimeMs
            LAST_SERVICE_TICK_REALTIME_KEY -> settings.lastServiceTickRt
            CLOCK_DRIFT_REF_KEY -> if (settings.hasClockDriftRef()) settings.clockDriftRef else 0L
            LAST_SIT_TS_KEY -> if (settings.hasLastSitTs()) settings.lastSitTs else 0L
            FIRST_VIOLATION_TS_KEY -> settings.firstViolationTs
            FIRST_VIOLATION_RT_KEY -> settings.firstViolationRt
            LAST_SIREN_STOP_RT_KEY -> settings.lastSirenStopRt
            LAST_GLOBAL_TRIGGER_RT_KEY -> settings.lastGlobalTriggerRt
            FORENSIC_RELIABILITY_DEGRADATION_START_RT_KEY -> settings.forensicReliabilityDegradationStartRt
            else -> 0L
        }
        return if (value == 0L) default else value
    }

    suspend fun getDouble(keyName: String, default: Double): Double {
        val settings = dataStore.data.first()
        if (keyName.startsWith("T_") || keyName.startsWith("V_") || keyName.startsWith("VR_")) {
            return settings.roleDoublesMap.getOrDefault(keyName, default)
        }
        val value = when (keyName) {
            MAX_DISTANCE_STORAGE_KEY -> settings.maxDistance
            MAX_ACCURACY_KEY -> settings.maxAccuracy
            MAX_TEMP_KEY -> settings.maxTemp
            TRACKER_LUX_BASELINE_KEY -> settings.trackerLuxBaseline
            TRACKER_ACOUSTIC_FLOOR_KEY -> settings.trackerAcousticFloor
            CHAIR_BASELINE_TILT_KEY -> settings.chairBaselineTilt
            else -> 0.0
        }
        return if (value == 0.0) default else value
    }

    suspend fun getBoolean(keyName: String, default: Boolean): Boolean {
        val settings = dataStore.data.first()
        if (keyName.startsWith("T_") || keyName.startsWith("V_") || keyName.startsWith("VR_")) {
            return settings.roleBoolsMap.getOrDefault(keyName, default)
        }
        return when (keyName) {
            IS_MANUAL_EXIT_KEY -> settings.isManualExit
            IS_MIC_TYPE_STARTED_KEY -> settings.isMicTypeStarted
            IS_XIAOMI_MANUAL_OVERRIDE_KEY -> settings.isXiaomiManualOverride
            IDENTITY_SANITIZED_KEY -> settings.identitySanitized
            IS_SYSTEM_ACTIVE_KEY -> settings.isSystemActive
            FIRST_VIOLATION_WAS_JUMP_KEY -> settings.firstViolationWasJump
            WAS_DISTANCE_VIOLATED_KEY -> settings.wasDistanceViolated
            POWER_ALARM_PENDING_KEY -> settings.powerAlarmPending
            else -> default
        }
    }

    suspend fun getInt(keyName: String, default: Int): Int {
        val settings = dataStore.data.first()
        if (keyName.startsWith("T_") || keyName.startsWith("V_") || keyName.startsWith("VR_")) {
            return settings.roleIntsMap.getOrDefault(keyName, default)
        }
        val value = when (keyName) {
            LAST_AUTO_SAVE_HOUR_KEY -> settings.lastAutoSaveHour
            LAST_VERSION_CODE_KEY -> settings.lastVersionCode
            RECOVERY_COUNT_KEY -> settings.recoveryCount
            DISTANCE_VIOLATION_COUNTER_KEY -> settings.distanceViolationCounter
            else -> -1
        }
        return if (value == -1) default else value
    }

    suspend fun resetRoleState(prefix: String) {
        dataStore.mutate {
            roleLongsMap.keys.filter { it.startsWith(prefix) }.forEach { removeRoleLongs(it) }
            roleDoublesMap.keys.filter { it.startsWith(prefix) }.forEach { removeRoleDoubles(it) }
            roleBoolsMap.keys.filter { it.startsWith(prefix) }.forEach { removeRoleBools(it) }
            roleIntsMap.keys.filter { it.startsWith(prefix) }.forEach { removeRoleInts(it) }
            roleStringsMap.keys.filter { it.startsWith(prefix) }.forEach { removeRoleStrings(it) }
            removeRoleStates(prefix)
        }
    }

    suspend fun setAppMode(mode: String?) {
        dataStore.mutate { setAppMode(mode ?: "") }
    }

    suspend fun loadHomePoints(): List<GeoPoint> = dataStore.data.first().homePointsList.map { GeoPoint(it.lat, it.lng) }

    suspend fun saveHomePoints(points: List<GeoPoint>, maxDistance: Double? = null, timestamp: Long? = null): Long {
        val ts = timestamp ?: timeProvider.currentTimeMillis()
        dataStore.mutate {
            clearHomePoints().addAllHomePoints(points.map { GeoPointProto.newBuilder().setLat(it.latitude).setLng(it.longitude).build() })
            setHomePointsTs(ts)
            maxDistance?.let { setMaxDistance(it) }
        }
        return ts
    }

    suspend fun addHomePoint(lat: Double, lng: Double): Long {
        val ts = timeProvider.currentTimeMillis()
        dataStore.mutate {
            addHomePoints(GeoPointProto.newBuilder().setLat(lat).setLng(lng).build())
            setHomePointsTs(ts)
        }
        return ts
    }

    suspend fun removeHomePoint(index: Int): Long {
        val ts = timeProvider.currentTimeMillis()
        dataStore.mutate {
            if (index >= 0 && index < homePointsCount) {
                removeHomePoints(index)
                setHomePointsTs(ts)
            }
        }
        return ts
    }

    suspend fun loadAlertSettings(): AlertSettings {
        val s = dataStore.data.first().alertSettings
        return SettingsMapper.protoToAlertSettings(s)
    }

    suspend fun saveAlertSettings(s: AlertSettings) {
        dataStore.mutate {
            setAlertSettings(SettingsMapper.alertSettingsToProto(s))
        }
    }

    fun saveTrackerState(status: TrackerStatus, rolePrefix: String? = null) {
        scope.launch {
            dataStore.mutate {
                val proto = SettingsMapper.mapTrackerStatusToProto(status)
                if (rolePrefix != null) putRoleStates(rolePrefix, proto) else setTrackerState(proto)
            }
        }
    }

    suspend fun loadTrackerState(rolePrefix: String? = null): TrackerStatus? {
        val settings = dataStore.data.first()
        if (rolePrefix != null) {
            val proto = settings.roleStatesMap[rolePrefix] ?: return null
            return SettingsMapper.mapTrackerStatusFromProto(proto)
        }
        if (!settings.hasTrackerState()) return null
        return SettingsMapper.mapTrackerStatusFromProto(settings.trackerState)
    }

    suspend fun saveLogicState(
        firstViolationTs: Long, firstViolationRt: Long, firstViolationWasJump: Boolean,
        distanceViolationCounter: Int, wasDistanceViolated: Boolean, powerAlarmPending: Boolean,
        lastSirenStopRt: Long, lastGlobalTriggerRt: Long, forensicReliabilityDegradationStartRt: Long,
        rolePrefix: String? = null
    ) {
        dataStore.mutate {
            if (rolePrefix != null) {
                putRoleLongs(rolePrefix + FIRST_VIOLATION_TS_KEY, firstViolationTs)
                putRoleLongs(rolePrefix + FIRST_VIOLATION_RT_KEY, firstViolationRt)
                putRoleBools(rolePrefix + FIRST_VIOLATION_WAS_JUMP_KEY, firstViolationWasJump)
                putRoleInts(rolePrefix + DISTANCE_VIOLATION_COUNTER_KEY, distanceViolationCounter)
                putRoleBools(rolePrefix + WAS_DISTANCE_VIOLATED_KEY, wasDistanceViolated)
                putRoleBools(rolePrefix + POWER_ALARM_PENDING_KEY, powerAlarmPending)
                putRoleLongs(rolePrefix + LAST_SIREN_STOP_RT_KEY, lastSirenStopRt)
                putRoleLongs(rolePrefix + LAST_GLOBAL_TRIGGER_RT_KEY, lastGlobalTriggerRt)
                putRoleLongs(rolePrefix + FORENSIC_RELIABILITY_DEGRADATION_START_RT_KEY, forensicReliabilityDegradationStartRt)
            } else {
                setFirstViolationTs(firstViolationTs).setFirstViolationRt(firstViolationRt).setFirstViolationWasJump(firstViolationWasJump)
                    .setDistanceViolationCounter(distanceViolationCounter).setWasDistanceViolated(wasDistanceViolated).setPowerAlarmPending(powerAlarmPending)
                    .setLastSirenStopRt(lastSirenStopRt).setLastGlobalTriggerRt(lastGlobalTriggerRt).setForensicReliabilityDegradationStartRt(forensicReliabilityDegradationStartRt)
            }
        }
    }

    suspend fun saveSettingsBulk(
        deviceId: String? = null, viewerId: String? = null, relayUrl: String? = null,
        maxDistance: Double? = null, alertSettings: AlertSettings? = null, homePoints: List<GeoPoint>? = null
    ) {
        dataStore.mutate {
            deviceId?.let { setTrackerId(it) }
            viewerId?.let { setViewerId(it) }
            relayUrl?.let { setRelayUrl(it) }
            maxDistance?.let { setMaxDistance(it) }
            alertSettings?.let { setAlertSettings(SettingsMapper.alertSettingsToProto(it)) }
            homePoints?.let { clearHomePoints().addAllHomePoints(it.map { p -> GeoPointProto.newBuilder().setLat(p.latitude).setLng(p.longitude).build() }) }
        }
    }

    suspend fun saveSessionMetricsBulk(
        totalConnected: Long, uptime: Long, totalDrop: Long,
        maxDrop: Long, maxDropTs: Long, lastGpsTs: Long, violationUptimeMs: Long
    ) {
        dataStore.mutate {
            setTotalConnected(totalConnected).setUptime(uptime).setTotalDrop(totalDrop)
                .setMaxDrop(maxDrop).setMaxDropTs(maxDropTs).setLastGpsTs(lastGpsTs).setViolationUptimeMs(violationUptimeMs)
        }
    }

    suspend fun resetStatsBulk() {
        dataStore.mutate {
            setTotalConnected(0).setUptime(0).setTotalDrop(0).setMaxDrop(0).setMaxDropTs(0).setLastGpsTs(0).setViolationUptimeMs(0).setRecoveryCount(0)
        }
    }

    suspend fun incrementRecoveryStats(blackoutMs: Long) {
        dataStore.mutate {
            setRecoveryCount(recoveryCount + 1)
            setTotalDrop(totalDrop + blackoutMs)
            if (blackoutMs > maxDrop) {
                setMaxDrop(blackoutMs)
                setMaxDropTs(timeProvider.currentTimeMillis())
            }
        }
    }
    
    suspend fun saveDraftSettings(deviceId: String, viewerId: String, relayUrl: String, maxDistance: Double, alertSettings: AlertSettings) {
        dataStore.mutate {
            setDraftTrackerId(deviceId).setDraftViewerId(viewerId).setDraftRelayUrl(relayUrl)
                .setDraftMaxDistance(maxDistance).setDraftAlertSettings(SettingsMapper.alertSettingsToProto(alertSettings))
        }
    }

    suspend fun commitDraftSettings(): CommitResult {
        val s = dataStore.data.first()
        val builder = s.toBuilder()
        var changed = false
        if (s.hasDraftTrackerId()) { builder.setTrackerId(s.draftTrackerId).clearDraftTrackerId(); changed = true }
        if (s.hasDraftViewerId()) { builder.setViewerId(s.draftViewerId).clearDraftViewerId(); changed = true }
        if (s.hasDraftRelayUrl()) { builder.setRelayUrl(s.draftRelayUrl).clearDraftRelayUrl(); changed = true }
        if (s.draftMaxDistance > 0) { builder.setMaxDistance(s.draftMaxDistance).setDraftMaxDistance(0.0); changed = true }
        if (s.hasDraftAlertSettings()) { builder.setAlertSettings(s.draftAlertSettings).clearDraftAlertSettings(); changed = true }
        if (changed) dataStore.updateData { builder.build() }
        return CommitResult(anyChanged = true)
    }

    suspend fun clearDraftSettings() {
        dataStore.mutate { clearDraftTrackerId().clearDraftViewerId().clearDraftRelayUrl().setDraftMaxDistance(0.0).clearDraftAlertSettings() }
    }
}
