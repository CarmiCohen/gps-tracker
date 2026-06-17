package com.gps19.app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.gps19.core.engine.TimeProvider
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
 * CommitResult: Result of an atomic draft commit to primary settings.
 */
data class CommitResult(
    val trackerIdChanged: Boolean = false,
    val viewerIdChanged: Boolean = false,
    val relayUrlChanged: Boolean = false,
    val maxDistanceChanged: Boolean = false,
    val alertsChanged: Boolean = false,
    val anyChanged: Boolean = false
)

/**
 * SettingsRepository: Manages persistent application settings using DataStore.
 * v8.8.21: Migrated to TimeProvider for all timing logic.
 * v8.8.32: Removed vid propagation.
 * v8.8.32 (Issue 137): Persist isCoolingModeActive and isStorageCritical.
 * v8.8.34: Removed 'ver' references (Issue 145).
 * v8.8.35: Updated to latest baseline following database schema cleanup (Issue 159).
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timeProvider: TimeProvider
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val dataStore: DataStore<AppSettings> = DataStoreFactory.create(
        serializer = AppSettingsSerializer,
        produceFile = { context.dataStoreFile("app_settings.pb") },
        migrations = listOf(AppSettingsMigration(context))
    )

    companion object {
        const val APP_MODE_KEY = "app_mode"
        const val TRACKER_ID_KEY = "tracker_id"
        const val VIEWER_ID_KEY = "viewer_id"
        const val RELAY_URL_KEY = "relay_url"
        const val DEFAULT_RELAY_URL = "https://gps-survival-relay.onrender.com"
        const val DEFAULT_TRACKER_ID = "T"
        const val DEFAULT_VIEWER_ID = "C"
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
        const val LAST_SIT_TS_KEY = "last_sit_ts"
        const val CHAIR_BASELINE_TILT_KEY = "chair_baseline_tilt"

        const val SELECTED_SIREN_KEY = "selected_siren"
        const val LAST_SERVICE_TICK_TS_KEY = "last_service_tick_ts"
        const val LAST_SERVICE_TICK_REALTIME_KEY = "last_service_tick_realtime"
        const val LAST_AUTO_SAVE_HOUR_KEY = "last_auto_save_hour"
        
        const val LAST_DAILY_ARCHIVE_DATE_KEY = "last_daily_archive_date"
        const val LAST_DAILY_CLEANUP_DATE_KEY = "last_daily_cleanup_date"

        // Draft Keys
        const val DRAFT_TRACKER_ID = "draft_tracker_id"
        const val DRAFT_VIEWER_ID = "draft_viewer_id"
        const val DRAFT_RELAY_URL = "draft_relay_url"
        const val DRAFT_MAX_DISTANCE = "draft_max_distance"

        // Issue 47
        const val IS_XIAOMI_MANUAL_OVERRIDE_KEY = "is_xiaomi_manual_override"
    }

    val appModeFlow: Flow<String?> = dataStore.data.map { it.appMode.ifEmpty { null } }
    val trackerIdFlow: Flow<String> = dataStore.data.map { it.trackerId.ifEmpty { DEFAULT_TRACKER_ID } }
    val viewerIdFlow: Flow<String> = dataStore.data.map { it.viewerId.ifEmpty { DEFAULT_VIEWER_ID } }
    val relayUrlFlow: Flow<String> = dataStore.data.map { it.relayUrl.ifEmpty { DEFAULT_RELAY_URL } }
    val isManualExitFlow: Flow<Boolean> = dataStore.data.map { it.isManualExit }
    val lastAlarmAckTsFlow: Flow<Long> = dataStore.data.map { it.lastAlarmAckTs }
    val lastSitTsFlow: Flow<Long> = dataStore.data.map { it.lastSitTs }
    
    val homePointsFlow: Flow<List<GeoPoint>> = dataStore.data.map { it.homePointsList.map { p -> GeoPoint(p.lat, p.lng) } }
    val maxDistanceFlow: Flow<Double> = dataStore.data.map { if (it.maxDistance > 0) it.maxDistance.toDouble() else DEFAULT_MAX_DISTANCE }
    val alertSettingsFlow: Flow<AlertSettings> = dataStore.data.map { protoToAlertSettings(it.alertSettings) }
    
    // Issue 47
    val isXiaomiManualOverrideFlow: Flow<Boolean> = dataStore.data.map { it.isXiaomiManualOverride }

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
                LAST_SIT_TS_KEY -> builder.setLastSitTs(value)
                VIOLATION_UPTIME_MS_KEY -> builder.setViolationUptimeMs(value)
                LAST_SERVICE_TICK_REALTIME_KEY -> builder.setLastServiceTickRealtime(value)
            }
            builder.build()
        }
    }

    suspend fun saveFloat(keyName: String, value: Float) {
        dataStore.updateData { current ->
            val builder = current.toBuilder()
            when (keyName) {
                MAX_DISTANCE_STORAGE_KEY -> builder.setMaxDistance(value)
                MAX_ACCURACY_KEY -> builder.setMaxAccuracy(value)
                MAX_TEMP_KEY -> builder.setMaxTemp(value)
                TRACKER_LUX_BASELINE_KEY -> builder.setTrackerLuxBaseline(value)
                DRAFT_MAX_DISTANCE -> builder.setDraftMaxDistance(value)
                CHAIR_BASELINE_TILT_KEY -> builder.setChairBaselineTilt(value)
            }
            builder.build()
        }
    }

    suspend fun saveDouble(keyName: String, value: Double) {
        dataStore.updateData { current ->
            val builder = current.toBuilder()
            when (keyName) {
                TRACKER_ACOUSTIC_FLOOR_KEY -> builder.setTrackerAcousticFloor(value)
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
        return when (keyName) {
            TRACKER_ID_KEY -> settings.trackerId.ifEmpty { default }
            VIEWER_ID_KEY -> settings.viewerId.ifEmpty { default }
            RELAY_URL_KEY -> settings.relayUrl.ifEmpty { default }
            SELECTED_SIREN_KEY -> settings.selectedSiren.ifEmpty { default }
            DRAFT_TRACKER_ID -> settings.draftTrackerId
            DRAFT_VIEWER_ID -> settings.draftViewerId
            DRAFT_RELAY_URL -> settings.draftRelayUrl
            LAST_DAILY_ARCHIVE_DATE_KEY -> settings.lastDailyArchiveDate.ifEmpty { default }
            LAST_DAILY_CLEANUP_DATE_KEY -> settings.lastDailyCleanupDate.ifEmpty { default }
            else -> default
        }
    }

    suspend fun getLong(keyName: String, default: Long): Long {
        val settings = dataStore.data.first()
        return when (keyName) {
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
            LAST_SIT_TS_KEY -> if (settings.hasLastSitTs()) settings.lastSitTs else default
            VIOLATION_UPTIME_MS_KEY -> settings.violationUptimeMs
            LAST_SERVICE_TICK_REALTIME_KEY -> settings.lastServiceTickRealtime
            else -> default
        }
    }

    suspend fun getFloat(keyName: String, default: Float): Float {
        val settings = dataStore.data.first()
        return when (keyName) {
            MAX_DISTANCE_STORAGE_KEY -> if (settings.maxDistance > 0) settings.maxDistance else default
            MAX_ACCURACY_KEY -> settings.maxAccuracy
            MAX_TEMP_KEY -> if (settings.maxTemp > 0) settings.maxTemp else default
            TRACKER_LUX_BASELINE_KEY -> settings.trackerLuxBaseline
            DRAFT_MAX_DISTANCE -> if (settings.draftMaxDistance > 0) settings.draftMaxDistance else default
            CHAIR_BASELINE_TILT_KEY -> if (settings.hasChairBaselineTilt()) settings.chairBaselineTilt else default
            else -> default
        }
    }

    suspend fun getDouble(keyName: String, default: Double): Double {
        val settings = dataStore.data.first()
        return when (keyName) {
            TRACKER_ACOUSTIC_FLOOR_KEY -> settings.trackerAcousticFloor
            else -> default
        }
    }

    suspend fun getBoolean(keyName: String, default: Boolean): Boolean {
        val settings = dataStore.data.first()
        return when (keyName) {
            IS_MANUAL_EXIT_KEY -> settings.isManualExit
            IS_MIC_TYPE_STARTED_KEY -> settings.isMicTypeStarted
            IS_XIAOMI_MANUAL_OVERRIDE_KEY -> settings.isXiaomiManualOverride
            else -> default
        }
    }

    suspend fun getInt(keyName: String, default: Int): Int {
        val settings = dataStore.data.first()
        return when (keyName) {
            LAST_AUTO_SAVE_HOUR_KEY -> settings.lastAutoSaveHour
            LAST_VERSION_CODE_KEY -> settings.lastVersionCode
            else -> default
        }
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
            maxDistance?.let { builder.setMaxDistance(it.toFloat()) }
            builder.build()
        }
        return ts
    }

    suspend fun loadAlertSettings(): AlertSettings {
        val s = dataStore.data.first().alertSettings
        return protoToAlertSettings(s)
    }

    private fun protoToAlertSettings(s: AlertSettingsProto): AlertSettings {
        return AlertSettings(
            localInternet = s.localInternet,
            serverConnection = s.serverConnection,
            relayConnection = s.relayConnection,
            jammerDetection = s.jammerDetection,
            signalLoss = s.signalLoss,
            gpsStalling = s.gpsStalling,
            distance = s.distance,
            power = s.power,
            lowBattery = s.lowBattery,
            batteryHealth = s.batteryHealth,
            longTimeGap = s.longTimeGap,
            highTemperature = s.highTemperature,
            overrideSilence = s.overrideSilence,
            useMaxVolume = s.useMaxVolume,
            vibrationEnabled = s.vibrationEnabled,
            alarmVolume = s.alarmVolume,
            useCustomVolume = s.useCustomVolume,
            tiltAlert = s.tiltAlert,
            acousticAlert = s.acousticAlert,
            liftAlert = s.liftAlert,
            tamperAlert = s.tamperAlert,
            chairOccupied = s.chairOccupied,
            globalMute = s.globalMute,
            systemStorageLow = s.systemStorageLow
        )
    }

    suspend fun saveAlertSettings(s: AlertSettings) {
        dataStore.updateData { current ->
            current.toBuilder().setAlertSettings(alertSettingsToProto(s)).build()
        }
    }

    private fun alertSettingsToProto(s: AlertSettings): AlertSettingsProto {
        return AlertSettingsProto.newBuilder()
            .setLocalInternet(s.localInternet)
            .setServerConnection(s.serverConnection)
            .setRelayConnection(s.relayConnection)
            .setJammerDetection(s.jammerDetection)
            .setSignalLoss(s.signalLoss)
            .setGpsStalling(s.gpsStalling)
            .setDistance(s.distance)
            .setPower(s.power)
            .setLowBattery(s.lowBattery)
            .setBatteryHealth(s.batteryHealth)
            .setLongTimeGap(s.longTimeGap)
            .setHighTemperature(s.highTemperature)
            .setOverrideSilence(s.overrideSilence)
            .setUseMaxVolume(s.useMaxVolume)
            .setVibrationEnabled(s.vibrationEnabled)
            .setAlarmVolume(s.alarmVolume)
            .setUseCustomVolume(s.useCustomVolume)
            .setTiltAlert(s.tiltAlert)
            .setAcousticAlert(s.acousticAlert)
            .setLiftAlert(s.liftAlert)
            .setTamperAlert(s.tamperAlert)
            .setChairOccupied(s.chairOccupied)
            .setGlobalMute(s.globalMute)
            .setSystemStorageLow(s.systemStorageLow)
            .build()
    }

    fun saveTrackerState(status: TrackerStatus) {
        scope.launch {
            dataStore.updateData { current ->
                val builder = current.toBuilder()
                val trackerBuilder = TrackerStatusProto.newBuilder()
                    .setLat(status.lat)
                    .setLng(status.lng)
                    .setAlt(status.alt)
                    .setSpeed(status.speed)
                    .setBearing(status.bearing)
                    .setAccuracy(status.accuracy)
                    .setMaxAccuracy(status.maxAccuracy)
                    .setGpsTs(status.gpsTs)
                    .setTs(status.ts)
                    .setBattery(status.battery)
                    .setTemp(status.temp)
                    .setMaxTemp(status.maxTemp)
                    .setIsCharging(status.isCharging)
                    .setSatsView(status.satsView)
                    .setSatsUsed(status.satsUsed)
                    .setLastConnTs(status.lastConnTs)
                    .setLastDiscTs(status.lastDiscTs)
                    .setUptimeMs(status.uptimeMs)
                    .setTotalConnectedMs(status.totalConnectedMs)
                    .setSessionConnectedMs(status.sessionConnectedMs)
                    .setTotalDropMs(status.totalDropMs)
                    .setMaxDropMs(status.maxDropMs)
                    .setMaxDropTs(status.maxDropTs)
                    .setViolationUptimeMs(status.violationUptimeMs)
                    .setViolationPercentage(status.violationPercentage)
                    .setIsSitDetected(status.isSitDetected)
                    .setLastSitTs(status.lastSitTs)
                    .setVerticalVelocity(status.verticalVelocity)
                    .setSitVz(status.sitVz)
                    .setSitDz(status.sitDz)
                    .setSitBaro(status.sitBaro)
                    .setSitTilt(status.sitTilt)
                    .setSitShock(status.sitShock)
                    .setIsPowerTamper(status.isPowerTamper)
                    .setVibration(status.vibration)
                    .setHeading(status.heading)
                    .setBaroAlt(status.baroAlt)
                    .setLux(status.lux)
                    .setIsNear(status.isNear)
                    .setTiltDegrees(status.tiltDegrees)
                    .setAcousticDb(status.acousticDb)
                    .setPeakShock(status.peakVibrationShock)
                    .setPeakShockTs(status.peakVibrationShockTs)
                    .setLuxBaseline(status.luxBaseline)
                    .setAcousticFloor(status.acousticFloorDb)
                    .setAdaptiveVibrationFloor(status.adaptiveVibrationFloor)
                    .setProxIdx(status.proxIdx)
                    .setIsSuspicious(status.isSuspicious)
                    .setIsTamperDetected(status.isTamperDetected)
                    .setIsPowerSaveMode(status.isPowerSaveMode)
                    .setStandbyBucket(status.standbyBucket)
                    .setNetInterface(status.netInterface)
                    .setIsStorageLow(status.isStorageLow)
                    .setIsBatterySteepDischarge(status.isBatterySteepDischarge)
                    .setIsJammer(status.isJammer)
                    .setIsCoolingModeActive(status.isCoolingModeActive)
                    .setIsStorageCritical(status.isStorageCritical)
                builder.setTrackerState(trackerBuilder.build())
                builder.build()
            }
        }
    }

    suspend fun loadTrackerState(): TrackerStatus? {
        val settings = dataStore.data.first()
        if (!settings.hasTrackerState()) return null
        val s = settings.trackerState
        return TrackerStatus(
            lat = s.lat, lng = s.lng, alt = s.alt,
            speed = s.speed, bearing = s.bearing, accuracy = s.accuracy, maxAccuracy = s.maxAccuracy,
            gpsTs = s.gpsTs, ts = s.ts, battery = s.battery, temp = s.temp, maxTemp = s.maxTemp, isCharging = s.isCharging,
            satsView = s.satsView, satsUsed = s.satsUsed,
            lastConnTs = s.lastConnTs, lastDiscTs = s.lastDiscTs,
            uptimeMs = s.uptimeMs,
            totalConnectedMs = s.totalConnectedMs, sessionConnectedMs = s.sessionConnectedMs,
            totalDropMs = s.totalDropMs, maxDropMs = s.maxDropMs,
            maxDropTs = s.maxDropTs, violationUptimeMs = s.violationUptimeMs, violationPercentage = s.violationPercentage,
            isSitDetected = s.isSitDetected, lastSitTs = s.lastSitTs, verticalVelocity = s.verticalVelocity,
            sitVz = s.sitVz, sitDz = s.sitDz, sitBaro = s.sitBaro, sitTilt = s.sitTilt, sitShock = s.sitShock,
            isPowerTamper = s.isPowerTamper,
            vibration = s.vibration,
            heading = s.heading,
            baroAlt = s.baroAlt,
            lux = s.lux,
            isNear = s.isNear,
            tiltDegrees = s.tiltDegrees,
            acousticDb = s.acousticDb,
            peakVibrationShock = s.peakShock,
            peakVibrationShockTs = s.peakShockTs,
            luxBaseline = s.luxBaseline,
            acousticFloorDb = s.acousticFloor,
            adaptiveVibrationFloor = s.adaptiveVibrationFloor,
            proxIdx = s.proxIdx,
            isSuspicious = s.isSuspicious,
            isTamperDetected = s.isTamperDetected,
            isPowerSaveMode = s.isPowerSaveMode,
            standbyBucket = s.standbyBucket,
            netInterface = s.netInterface,
            isStorageLow = s.isStorageLow,
            isStorageCritical = s.isStorageCritical,
            isBatterySteepDischarge = s.isBatterySteepDischarge,
            isJammer = s.isJammer,
            isCoolingModeActive = s.isCoolingModeActive
        )
    }

    suspend fun saveDraftAlertSettings(s: AlertSettings) {
        dataStore.updateData { current ->
            current.toBuilder().setDraftAlertSettings(alertSettingsToProto(s)).build()
        }
    }

    suspend fun loadDraftAlertSettings(): AlertSettings? {
        val s = dataStore.data.first()
        return if (s.hasDraftAlertSettings()) protoToAlertSettings(s.draftAlertSettings) else null
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
        maxDistance: Float,
        alertSettings: AlertSettings
    ) {
        dataStore.updateData { current ->
            current.toBuilder()
                .setDraftTrackerId(deviceId)
                .setDraftViewerId(viewerId)
                .setDraftRelayUrl(relayUrl)
                .setDraftMaxDistance(maxDistance)
                .setDraftAlertSettings(alertSettingsToProto(alertSettings))
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
            
            val newTrackerId = if (current.hasDraftTrackerId()) current.draftTrackerId else current.trackerId
            val newViewerId = if (current.hasDraftViewerId()) current.draftViewerId else current.viewerId
            val newRelayUrl = if (current.hasDraftRelayUrl()) current.draftRelayUrl else current.relayUrl
            val newMaxDistance = if (current.hasDraftMaxDistance()) current.draftMaxDistance else current.maxDistance
            val newAlertsProto = if (current.hasDraftAlertSettings()) current.draftAlertSettings else current.alertSettings

            val tChanged = newTrackerId != current.trackerId
            val vChanged = newViewerId != current.viewerId
            val rChanged = newRelayUrl != current.relayUrl
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
     * R853: Added homePoints support for atomic bulk updates.
     */
    suspend fun saveSettingsBulk(
        deviceId: String? = null,
        viewerId: String? = null,
        relayUrl: String? = null,
        maxDistance: Float? = null,
        alertSettings: AlertSettings? = null,
        homePoints: List<GeoPoint>? = null
    ) {
        dataStore.updateData { current ->
            val builder = current.toBuilder()
            deviceId?.let { builder.setTrackerId(it) }
            viewerId?.let { builder.setViewerId(it) }
            relayUrl?.let { builder.setRelayUrl(it) }
            maxDistance?.let { builder.setMaxDistance(it) }
            alertSettings?.let { builder.setAlertSettings(alertSettingsToProto(it)) }
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
                .setMaxAccuracy(0f)
                .setMaxTemp(0f)
                .setTotalConnected(0L)
                .setUptime(0L)
                .setTotalDrop(0L)
                .setTotalDrop(0L)
                .setMaxDrop(0L)
                .setMaxDropTs(0L)
                .setLastGpsTs(0L)
                .setViolationUptimeMs(0L)
                .build()
        }
    }
}
