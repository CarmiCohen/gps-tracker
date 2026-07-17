package com.gps19.app

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.gps19.core.engine.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

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
 * July.17.02:
 * - Added IS_SYSTEM_ACTIVE_KEY to prevent unintended engine starts on boot.
 * v9.5.0:
 * - Issue #503: Hilt Removal.
 */
class SettingsRepository(
    private val context: Context,
    private val timeProvider: TimeProvider
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val typeMigration = object : DataMigration<AppSettings> {
        override suspend fun shouldMigrate(currentData: AppSettings): Boolean {
            return currentData.hasLegacyMaxDistance() || 
                   currentData.hasLegacyMaxAccuracy() || 
                   currentData.hasLegacyMaxTemp()
        }

        override suspend fun migrate(currentData: AppSettings): AppSettings {
            val builder = currentData.toBuilder()
            if (currentData.hasLegacyMaxDistance()) {
                builder.setMaxDistance(currentData.legacyMaxDistance.toDouble())
                builder.clearLegacyMaxDistance()
            }
            if (currentData.hasLegacyMaxAccuracy()) {
                builder.setMaxAccuracy(currentData.legacyMaxAccuracy.toDouble())
                builder.clearLegacyMaxAccuracy()
            }
            if (currentData.hasLegacyMaxTemp()) {
                builder.setMaxTemp(currentData.legacyMaxTemp.toDouble())
                builder.clearLegacyMaxTemp()
            }
            return builder.build()
        }

        override suspend fun cleanUp() {}
    }

    private val identitySanitizationMigration = object : DataMigration<AppSettings> {
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

    private val dataStore: DataStore<AppSettings> = DataStoreFactory.create(
        serializer = AppSettingsSerializer,
        produceFile = { context.dataStoreFile("app_settings.pb") },
        migrations = listOf(
            AppSettingsMigration(context), 
            typeMigration,
            identitySanitizationMigration
        )
    )

    companion object {
        const val APP_MODE_KEY = "app_mode"
        const val TRACKER_ID_KEY = "tracker_id"
        const val VIEWER_ID_KEY = "viewer_id"
        const val RELAY_URL_KEY = "relay_url"
        const val DEFAULT_RELAY_URL = "https://gps-survival-relay.onrender.com"
        
        const val DEFAULT_TRACKER_ID = SignalingConstants.DEFAULT_TRACKER_ID
        const val DEFAULT_VIEWER_ID = SignalingConstants.DEFAULT_VIEWER_ID
        
        const val MAX_DISTANCE_STORAGE_KEY = "max_distance"
        const val DEFAULT_MAX_DISTANCE = 60.0
        const val MAX_ACCURACY_KEY = "max_accuracy"
        const val MAX_TEMP_KEY = "max_temp"
        const val LAST_ALARM_ACK_TS_KEY = "last_alarm_ack_ts"
        const val HOME_POINTS_TS_KEY = "home_points_ts"
        const val IS_MANUAL_EXIT_KEY = "is_manual_exit"
        const val APP_START_TIME_KEY = "app_start_time"
        const val LAST_VERSION_CODE_KEY = "last_version_code"
        
        const val TOTAL_CONNECTED_KEY = "total_connected"
        const val UPTIME_KEY = "uptime"
        const val LAST_CONNECTION_TS_KEY = "last_conn_ts"
        const val LAST_DISCONNECTION_TS_KEY = "last_disc_ts"
        const val TOTAL_DROP_KEY = "total_drop"
        const val MAX_DROP_KEY = "max_drop"
        const val MAX_DROP_TS_KEY = "max_drop_ts"
        const val LAST_GPS_TS_KEY = "last_gps_ts"
        const val VIOLATION_UPTIME_MS_KEY = "violation_uptime_ms"

        const val TRACKER_LUX_BASELINE_KEY = "tracker_lux_baseline"
        const val TRACKER_ACOUSTIC_FLOOR_KEY = "tracker_acoustic_floor"
        
        const val IS_MIC_TYPE_STARTED_KEY = "is_mic_type_started"

        const val SELECTED_SIREN_KEY = "selected_siren"
        const val LAST_SERVICE_TICK_TS_KEY = "last_service_tick_ts"
        const val LAST_SERVICE_TICK_REALTIME_KEY = "last_service_tick_realtime"
        const val LAST_AUTO_SAVE_HOUR_KEY = "last_auto_save_hour"
        
        const val LAST_DAILY_ARCHIVE_DATE_KEY = "last_daily_archive_date"
        const val LAST_DAILY_CLEANUP_DATE_KEY = "last_daily_cleanup_date"

        const val DRAFT_TRACKER_ID = "draft_tracker_id"
        const val DRAFT_VIEWER_ID = "draft_viewer_id"
        const val DRAFT_RELAY_URL = "draft_relay_url"
        const val DRAFT_MAX_DISTANCE = "draft_max_distance"

        const val IS_XIAOMI_MANUAL_OVERRIDE_KEY = "is_xiaomi_manual_override"
        const val IS_SYSTEM_ACTIVE_KEY = "is_system_active"
        
        const val IDENTITY_SANITIZED_KEY = "identity_sanitized"
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
                LAST_SERVICE_TICK_REALTIME_KEY -> builder.setLastServiceTickRealtime(value)
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
            LAST_SERVICE_TICK_REALTIME_KEY -> settings.lastServiceTickRealtime
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
            else -> default
        }
    }

    suspend fun getInt(keyName: String, default: Int): Int {
        val settings = dataStore.data.first()
        val value = when (keyName) {
            LAST_AUTO_SAVE_HOUR_KEY -> settings.lastAutoSaveHour
            LAST_VERSION_CODE_KEY -> settings.lastVersionCode
            else -> -1
        }
        return if (value == -1) default else value
    }

    suspend fun getAppMode(): String? = dataStore.data.first().appMode.ifEmpty { null }
    fun setAppMode(mode: String?) {
        scope.launch { dataStore.updateData { it.toBuilder().setAppMode(mode ?: "").build() } }
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

    suspend fun saveDraftAlertSettings(s: AlertSettings) {
        dataStore.updateData { current ->
            current.toBuilder().setDraftAlertSettings(SettingsMapper.alertSettingsToProto(s)).build()
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

    /**
     * Atomically saves multiple draft settings to reduce I/O cycles.
     */
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

    /**
     * Atomically commits draft settings to primary keys and clears drafts.
     * Returns CommitResult indicating which primary values were updated.
     */
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

            // v9.3.25: Refined Alias-Aware Uniqueness Check
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

    /**
     * Atomically saves multiple primary settings.
     */
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

    /**
     * Atomically saves multiple session metrics to reduce I/O cycles.
     */
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

    /**
     * Atomically resets connectivity stats to reduce I/O cycles.
     */
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
}
