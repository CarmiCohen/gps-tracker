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
 * Resolved Issue #511: DataStore Singleton Violation.
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
 * CommitResult: Result of an atomic draft commit to primary settings.
 */
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
 * Sep.02.66:
 * - Issue #241 RESOLVED: Mode-Selection Activation. Migrated setAppMode to 
 *   suspend to eliminate race conditions during role selection (R-ID 241).
 * July.30.28:
 * - Issue #630: Forensic Recovery Log Aggregation. Added cumulative recovery stats support.
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
                val isTrackerInvalid = t.isNotEmpty() && !SignalingConstants.isValidTrackerId(t)
                val isViewerInvalid = v.isNotEmpty() && !SignalingConstants.isValidViewerId(v)
                return isTrackerInvalid || isViewerInvalid
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
    val homePointsFlow: Flow<List<GeoPoint>> = dataStore.data.map { it.homePointsList.map { p -> GeoPoint(p.lat, p.lng) } }
    val maxDistanceFlow: Flow<Double> = dataStore.data.map { if (it.maxDistance > 0.0) it.maxDistance else DEFAULT_MAX_DISTANCE }
    val alertSettingsFlow: Flow<AlertSettings> = dataStore.data.map { SettingsMapper.protoToAlertSettings(it.alertSettings) }
    val isXiaomiManualOverrideFlow: Flow<Boolean> = dataStore.data.map { it.isXiaomiManualOverride }
    val identitySanitizedFlow: Flow<Boolean> = dataStore.data.map { it.identitySanitized }
    val isSystemActiveFlow: Flow<Boolean> = dataStore.data.map { it.isSystemActive }
    val lastAlarmsJsonFlow: Flow<String> = dataStore.data.map { it.lastAlarmsJson }
    val isRecoveryPendingFlow: Flow<Boolean> = dataStore.data.map { it.isRecoveryPending }
    val recoveryBlockedTsFlow: Flow<Long> = dataStore.data.map { it.recoveryBlockedTs }
    val cumulativeRecoveryBlackoutMsFlow: Flow<Long> = dataStore.data.map { it.cumulativeRecoveryBlackoutMs }
    val recoveryCountFlow: Flow<Int> = dataStore.data.map { it.recoveryCount }

    suspend fun getSettingsSnapshot(): AppSettings = dataStore.data.first()

    suspend fun saveString(keyName: String, value: String) {
        dataStore.updateData { current ->
            val builder = current.toBuilder()
            when (keyName) {
                APP_MODE_KEY -> builder.setAppMode(value)
                TRACKER_ID_KEY -> builder.setTrackerId(value)
                VIEWER_ID_KEY -> builder.setViewerId(value)
                RELAY_URL_KEY -> builder.setRelayUrl(value)
                SELECTED_SIREN_KEY -> builder.setSelectedSiren(value)
                DRAFT_TRACKER_ID -> builder.setDraftTrackerId(value)
                DRAFT_VIEWER_ID -> builder.setDraftViewerId(value)
                DRAFT_RELAY_URL -> builder.setDraftRelayUrl(value)
                LAST_DAILY_ARCHIVE_DATE_KEY -> builder.setLastDailyArchiveDate(value)
                LAST_DAILY_CLEANUP_DATE_KEY -> builder.setLastDailyCleanupDate(value)
                LAST_ALARMS_JSON_KEY -> builder.setLastAlarmsJson(value)
            }
            builder.build()
        }
    }

    suspend fun saveLong(keyName: String, value: Long) {
        dataStore.updateData { current ->
            val builder = current.toBuilder()
            when (keyName) {
                LAST_ALARM_ACK_TS_KEY -> builder.setLastAlarmAckTs(value)
                HOME_POINTS_TS_KEY -> builder.setHomePointsTs(value)
                LAST_SERVICE_TICK_TS_KEY -> builder.setLastServiceTickTs(value)
                APP_START_TIME_KEY -> builder.setAppStartTime(value)
                TOTAL_CONNECTED_KEY -> builder.setTotalConnected(value)
                UPTIME_KEY -> builder.setUptime(value)
                LAST_CONNECTION_TS_KEY -> builder.setLastConnectionTs(value)
                LAST_DISCONNECTION_TS_KEY -> builder.setLastDisconnectionTs(value)
                TOTAL_DROP_KEY -> builder.setTotalDrop(value)
                MAX_DROP_KEY -> builder.setMaxDrop(value)
                MAX_DROP_TS_KEY -> builder.setMaxDropTs(value)
                LAST_GPS_TS_KEY -> builder.setLastGpsTs(value)
                VIOLATION_UPTIME_MS_KEY -> builder.setViolationUptimeMs(value)
                LAST_SERVICE_TICK_REALTIME_KEY -> builder.setLastServiceTickRt(value)
                CLOCK_DRIFT_REF_KEY -> builder.setClockDriftRef(value)
                LAST_SIT_TS_KEY -> builder.setLastSitTs(value)
                LAST_HISTORY_SIT_TS_KEY -> builder.setLastHistorySitTs(value)
                RECOVERY_BLOCKED_TS_KEY -> builder.setRecoveryBlockedTs(value)
                CUMULATIVE_RECOVERY_BLACKOUT_MS_KEY -> builder.setCumulativeRecoveryBlackoutMs(value)
            }
            builder.build()
        }
    }

    suspend fun saveDouble(keyName: String, value: Double) {
        dataStore.updateData { current ->
            val builder = current.toBuilder()
            when (keyName) {
                MAX_DISTANCE_STORAGE_KEY -> builder.setMaxDistance(value)
                MAX_ACCURACY_KEY -> builder.setMaxAccuracy(value)
                MAX_TEMP_KEY -> builder.setMaxTemp(value)
                TRACKER_LUX_BASELINE_KEY -> builder.setTrackerLuxBaseline(value)
                TRACKER_ACOUSTIC_FLOOR_KEY -> builder.setTrackerAcousticFloor(value)
                DRAFT_MAX_DISTANCE -> builder.setDraftMaxDistance(value)
                CHAIR_BASELINE_TILT_KEY -> builder.setChairBaselineTilt(value)
            }
            builder.build()
        }
    }

    suspend fun saveBoolean(keyName: String, value: Boolean) {
        dataStore.updateData { current ->
            val builder = current.toBuilder()
            when (keyName) {
                IS_MANUAL_EXIT_KEY -> builder.setIsManualExit(value)
                IS_MIC_TYPE_STARTED_KEY -> builder.setIsMicTypeStarted(value)
                IS_XIAOMI_MANUAL_OVERRIDE_KEY -> builder.setIsXiaomiManualOverride(value)
                IDENTITY_SANITIZED_KEY -> builder.setIdentitySanitized(value)
                IS_SYSTEM_ACTIVE_KEY -> builder.setIsSystemActive(value)
                IS_RECOVERY_PENDING_KEY -> builder.setIsRecoveryPending(value)
            }
            builder.build()
        }
    }

    suspend fun saveInt(keyName: String, value: Int) {
        dataStore.updateData { current ->
            val builder = current.toBuilder()
            when (keyName) {
                LAST_AUTO_SAVE_HOUR_KEY -> builder.setLastAutoSaveHour(value)
                LAST_VERSION_CODE_KEY -> builder.setLastVersionCode(value)
                RECOVERY_COUNT_KEY -> builder.setRecoveryCount(value)
            }
            builder.build()
        }
    }

    suspend fun getString(keyName: String, default: String): String {
        val settings = dataStore.data.first()
        val value = when (keyName) {
            TRACKER_ID_KEY -> settings.trackerId
            VIEWER_ID_KEY -> settings.viewerId
            RELAY_URL_KEY -> settings.relayUrl
            SELECTED_SIREN_KEY -> settings.selectedSiren
            DRAFT_TRACKER_ID -> settings.draftTrackerId
            DRAFT_VIEWER_ID -> settings.draftViewerId
            DRAFT_RELAY_URL -> settings.draftRelayUrl
            LAST_DAILY_ARCHIVE_DATE_KEY -> settings.lastDailyArchiveDate
            LAST_DAILY_CLEANUP_DATE_KEY -> settings.lastDailyCleanupDate
            LAST_ALARMS_JSON_KEY -> settings.lastAlarmsJson
            else -> ""
        }
        return value.ifEmpty { default }
    }

    suspend fun getLong(keyName: String, default: Long): Long {
        val settings = dataStore.data.first()
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
            LAST_HISTORY_SIT_TS_KEY -> if (settings.hasLastHistorySitTs()) settings.lastHistorySitTs else 0L
            RECOVERY_BLOCKED_TS_KEY -> settings.recoveryBlockedTs
            CUMULATIVE_RECOVERY_BLACKOUT_MS_KEY -> settings.cumulativeRecoveryBlackoutMs
            else -> 0L
        }
        return if (value == 0L) default else value
    }

    suspend fun getDouble(keyName: String, default: Double): Double {
        val settings = dataStore.data.first()
        val value = when (keyName) {
            MAX_DISTANCE_STORAGE_KEY -> settings.maxDistance
            MAX_ACCURACY_KEY -> settings.maxAccuracy
            MAX_TEMP_KEY -> settings.maxTemp
            TRACKER_LUX_BASELINE_KEY -> settings.trackerLuxBaseline
            TRACKER_ACOUSTIC_FLOOR_KEY -> settings.trackerAcousticFloor
            DRAFT_MAX_DISTANCE -> settings.draftMaxDistance
            CHAIR_BASELINE_TILT_KEY -> settings.chairBaselineTilt
            else -> 0.0
        }
        return if (value == 0.0) default else value
    }

    suspend fun getBoolean(keyName: String, default: Boolean): Boolean {
        val settings = dataStore.data.first()
        return when (keyName) {
            IS_MANUAL_EXIT_KEY -> settings.isManualExit
            IS_MIC_TYPE_STARTED_KEY -> settings.isMicTypeStarted
            IS_XIAOMI_MANUAL_OVERRIDE_KEY -> settings.isXiaomiManualOverride
            IDENTITY_SANITIZED_KEY -> settings.identitySanitized
            IS_SYSTEM_ACTIVE_KEY -> settings.isSystemActive
            IS_RECOVERY_PENDING_KEY -> settings.isRecoveryPending
            else -> default
        }
    }

    suspend fun getInt(keyName: String, default: Int): Int {
        val settings = dataStore.data.first()
        val value = when (keyName) {
            LAST_AUTO_SAVE_HOUR_KEY -> settings.lastAutoSaveHour
            LAST_VERSION_CODE_KEY -> settings.lastVersionCode
            RECOVERY_COUNT_KEY -> settings.recoveryCount
            else -> -1
        }
        return if (value == -1) default else value
    }

    suspend fun getAppMode(): String? = dataStore.data.first().appMode.ifEmpty { null }
    
    suspend fun setAppMode(mode: String?) {
        dataStore.updateData { it.toBuilder().setAppMode(mode ?: "").build() }
    }

    suspend fun loadHomePoints(): List<GeoPoint> = dataStore.data.first().homePointsList.map { GeoPoint(it.lat, it.lng) }

    suspend fun saveHomePoints(points: List<GeoPoint>, maxDistance: Double? = null, timestamp: Long? = null): Long {
        val ts = timestamp ?: timeProvider.currentTimeMillis()
        dataStore.updateData { current ->
            val builder = current.toBuilder()
            builder.clearHomePoints().addAllHomePoints(points.map { GeoPointProto.newBuilder().setLat(it.latitude).setLng(it.longitude).build() })
            builder.setHomePointsTs(ts)
            maxDistance?.let { builder.setMaxDistance(it) }
            builder.build()
        }
        return ts
    }

    suspend fun loadAlertSettings(): AlertSettings {
        val s = dataStore.data.first().alertSettings
        return SettingsMapper.protoToAlertSettings(s)
    }

    suspend fun saveAlertSettings(s: AlertSettings) {
        dataStore.updateData { current ->
            current.toBuilder().setAlertSettings(SettingsMapper.alertSettingsToProto(s)).build()
        }
    }

    fun saveTrackerState(status: TrackerStatus) {
        scope.launch {
            dataStore.updateData { current ->
                val builder = current.toBuilder()
                builder.setTrackerState(SettingsMapper.mapTrackerStatusToProto(status))
                builder.build()
            }
        }
    }

    suspend fun loadTrackerState(): TrackerStatus? {
        val settings = dataStore.data.first()
        if (!settings.hasTrackerState()) return null
        return SettingsMapper.mapTrackerStatusFromProto(settings.trackerState)
    }

    suspend fun saveDraftAlertSettings(alertSettings: AlertSettings) {
        dataStore.updateData { current ->
            current.toBuilder().setDraftAlertSettings(SettingsMapper.alertSettingsToProto(alertSettings)).build()
        }
    }

    fun saveTrackerStatus(status: TrackerStatus) {
        scope.launch {
            dataStore.updateData { current ->
                current.toBuilder().setTrackerState(SettingsMapper.mapTrackerStatusToProto(status)).build()
            }
        }
    }

    suspend fun loadDraftAlertSettings(): AlertSettings? {
        val s = dataStore.data.first()
        return if (s.hasDraftAlertSettings()) SettingsMapper.protoToAlertSettings(s.draftAlertSettings) else null
    }

    suspend fun clearDraftSettings() {
        dataStore.updateData { current ->
            current.toBuilder()
                .clearDraftTrackerId()
                .clearDraftViewerId()
                .clearDraftRelayUrl()
                .clearDraftMaxDistance()
                .clearDraftAlertSettings()
                .build()
        }
    }

    suspend fun hasPendingDrafts(): Boolean {
        val current = dataStore.data.first()
        return current.hasDraftTrackerId() || 
               current.hasDraftViewerId() || 
               current.hasDraftRelayUrl() || 
               current.hasDraftMaxDistance() || 
               current.hasDraftAlertSettings()
    }

    suspend fun saveDraftSettings(
        deviceId: String,
        viewerId: String,
        relayUrl: String,
        maxDistance: Double,
        alertSettings: AlertSettings
    ) {
        dataStore.updateData { current ->
            current.toBuilder()
                .setDraftTrackerId(deviceId)
                .setDraftViewerId(viewerId)
                .setDraftRelayUrl(relayUrl)
                .setDraftMaxDistance(maxDistance)
                .setDraftAlertSettings(SettingsMapper.alertSettingsToProto(alertSettings))
                .build()
        }
    }

    suspend fun commitDraftSettings(): CommitResult {
        var res = CommitResult()
        dataStore.updateData { current ->
            val builder = current.toBuilder()
            
            val currentTrackerId = current.trackerId.ifEmpty { DEFAULT_TRACKER_ID }
            val currentViewerId = current.viewerId.ifEmpty { DEFAULT_VIEWER_ID }
            val currentRelayUrl = current.relayUrl.ifEmpty { DEFAULT_RELAY_URL }

            val newTrackerId = if (current.hasDraftTrackerId()) current.draftTrackerId else currentTrackerId
            val newViewerId = if (current.hasDraftViewerId()) current.draftViewerId else currentViewerId
            val newRelayUrl = if (current.hasDraftRelayUrl()) current.draftRelayUrl else currentRelayUrl
            val newMaxDistance = if (current.hasDraftMaxDistance()) current.draftMaxDistance else current.maxDistance
            val newAlertsProto = if (current.hasDraftAlertSettings()) current.draftAlertSettings else current.alertSettings

            if (!SignalingConstants.areIdsUnique(newTrackerId, newViewerId)) {
                res = CommitResult(error = "Identity Conflict: Some IDs (e.g., 'viewer', 'Trk') are reserved for cross-version compatibility. Please choose unique IDs.")
                return@updateData current
            }

            val tChanged = newTrackerId != currentTrackerId
            val vChanged = newViewerId != currentViewerId
            val rChanged = newRelayUrl != currentRelayUrl
            val mChanged = newMaxDistance != current.maxDistance
            val aChanged = newAlertsProto != current.alertSettings

            if (tChanged) builder.setTrackerId(newTrackerId)
            if (vChanged) builder.setViewerId(newViewerId)
            if (rChanged) builder.setRelayUrl(newRelayUrl)
            if (mChanged) builder.setMaxDistance(newMaxDistance)
            if (aChanged) builder.setAlertSettings(newAlertsProto)
            
            builder.clearDraftTrackerId()
                   .clearDraftViewerId()
                   .clearDraftRelayUrl()
                   .clearDraftMaxDistance()
                   .clearDraftAlertSettings()
            
            res = CommitResult(
                trackerIdChanged = tChanged,
                viewerIdChanged = vChanged,
                relayUrlChanged = rChanged,
                maxDistanceChanged = mChanged,
                alertsChanged = aChanged,
                anyChanged = tChanged || vChanged || rChanged || mChanged || aChanged
            )
            
            builder.build()
        }
        return res
    }

    suspend fun saveSettingsBulk(
        deviceId: String? = null,
        viewerId: String? = null,
        relayUrl: String? = null,
        maxDistance: Double? = null,
        alertSettings: AlertSettings? = null,
        homePoints: List<GeoPoint>? = null
    ) {
        dataStore.updateData { current ->
            val builder = current.toBuilder()
            deviceId?.let { if (SignalingConstants.isValidTrackerId(it)) builder.setTrackerId(it) }
            viewerId?.let { if (SignalingConstants.isValidViewerId(it)) builder.setViewerId(it) }
            relayUrl?.let { builder.setRelayUrl(it) }
            maxDistance?.let { builder.setMaxDistance(it) }
            alertSettings?.let { builder.setAlertSettings(SettingsMapper.alertSettingsToProto(it)) }
            homePoints?.let { pts ->
                builder.clearHomePoints().addAllHomePoints(pts.map { GeoPointProto.newBuilder().setLat(it.latitude).setLng(it.longitude).build() })
                builder.setHomePointsTs(timeProvider.currentTimeMillis())
            }
            builder.build()
        }
    }

    suspend fun saveSessionMetricsBulk(
        totalConnected: Long,
        uptime: Long,
        totalDrop: Long,
        maxDrop: Long,
        maxDropTs: Long,
        lastGpsTs: Long,
        violationUptimeMs: Long
    ) {
        dataStore.updateData { current ->
            current.toBuilder()
                .setTotalConnected(totalConnected)
                .setUptime(uptime)
                .setTotalDrop(totalDrop)
                .setMaxDrop(maxDrop)
                .setMaxDropTs(maxDropTs)
                .setLastGpsTs(lastGpsTs)
                .setViolationUptimeMs(violationUptimeMs)
                .build()
        }
    }

    suspend fun resetStatsBulk() {
        dataStore.updateData { current ->
            current.toBuilder()
                .setMaxAccuracy(0.0)
                .setMaxTemp(0.0)
                .setTotalConnected(0L)
                .setUptime(0L)
                .setTotalDrop(0L)
                .setMaxDrop(0L)
                .setMaxDropTs(0L)
                .setLastGpsTs(0L)
                .setViolationUptimeMs(0L)
                .build()
        }
    }

    suspend fun incrementRecoveryStats(blackoutMs: Long) {
        dataStore.updateData { current ->
            current.toBuilder()
                .setCumulativeRecoveryBlackoutMs(current.cumulativeRecoveryBlackoutMs + blackoutMs)
                .setRecoveryCount(current.recoveryCount + 1)
                .build()
        }
    }
}
