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
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

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
 * Oct28.12: Integrated Integrity Guard (SIMP-OCT28.11-S1).
 */
private suspend inline fun DataStore<AppSettings>.mutate(
    crossinline block: AppSettings.Builder.() -> Unit
): AppSettings {
    return updateData { current ->
        val builder = current.toBuilder()
        builder.block()
        
        // Reset violation count on successful write
        builder.setIntegrityViolationCount(0)
        
        // Calculate integrity hash before building
        val intermediate = builder.build()
        val hash = SettingsRepository.calculateIntegrityHash(intermediate)
        builder.setIntegritySignature(hash)
        
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
 * Oct28.12:
 * - SIMP-OCT28.12-S1: Settings Restoration Policy (Factory Reset Fallback).
 * - SIMP-OCT28.11-S1: Settings Invalidation Guard (R-ID 758).
 * Oct24.16:
 * - SIMP-OCT22.0-S1: Centralized identity validation via ConfigValidator.
 * Oct.19.19:
 * - ISSUE-OCT19.19-F1: Added isRestorationDisabled persistence. Corrected typo in lastDisconnectionTs accessor.
 * Oct.15.9:
 * - Fixed identity conflict error message to match IdentityPersistenceTest expectations.
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
        
        private const val MAX_INTEGRITY_VIOLATIONS = 3

        internal val identitySanitizationMigration = object : DataMigration<AppSettings> {
            override suspend fun shouldMigrate(currentData: AppSettings): Boolean {
                val t = currentData.trackerId
                val v = currentData.viewerId
                return (t.isNotEmpty() && !ConfigValidator.isValidTrackerId(t)) ||
                       (v.isNotEmpty() && !ConfigValidator.isValidViewerId(v))
            }

            override suspend fun migrate(currentData: AppSettings): AppSettings {
                val builder = currentData.toBuilder()
                if (currentData.trackerId.isNotEmpty() && !ConfigValidator.isValidTrackerId(currentData.trackerId)) {
                    builder.setTrackerId(DEFAULT_TRACKER_ID)
                    builder.setIdentitySanitized(true)
                }
                if (currentData.viewerId.isNotEmpty() && !ConfigValidator.isValidViewerId(currentData.viewerId)) {
                    builder.setViewerId(DEFAULT_VIEWER_ID)
                    builder.setIdentitySanitized(true)
                }
                return builder.build()
            }

            override suspend fun cleanUp() {}
        }

        /**
         * calculateIntegrityHash: Generates a SHA-256 signature for settings state.
         * R-ID 758: Must exclude signature and violation count to prevent circularity.
         */
        internal fun calculateIntegrityHash(settings: AppSettings): String {
            val clean = settings.toBuilder()
                .clearIntegritySignature()
                .clearIntegrityViolationCount()
                .build()
            val bytes = clean.toByteArray()
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(bytes)
            return hash.joinToString("") { "%02x".format(it) }
        }
    }

    /**
     * Internal flow that validates integrity on every emission.
     * SIMP-OCT28.12-S1: Implements automated factory reset after repeated failures.
     */
    private val validatedData: Flow<AppSettings> = dataStore.data.map { settings ->
        if (settings.integritySignature.isNotEmpty()) {
            val expected = calculateIntegrityHash(settings)
            if (settings.integritySignature != expected) {
                Timber.e("CRITICAL: Settings Integrity Violation Detected! (Count: ${settings.integrityViolationCount})")
                handleIntegrityViolation(settings)
            }
        }
        settings
    }

    private fun handleIntegrityViolation(settings: AppSettings) {
        scope.launch {
            val nextCount = settings.integrityViolationCount + 1
            if (nextCount >= MAX_INTEGRITY_VIOLATIONS) {
                Timber.e("CRITICAL: Integrity failure threshold reached. Performing Emergency Factory Reset.")
                performFactoryReset()
            } else {
                dataStore.updateData { current ->
                    current.toBuilder().setIntegrityViolationCount(nextCount).build()
                }
            }
        }
    }

    /**
     * performFactoryReset: Clears all settings and re-initializes with a valid signature.
     */
    private suspend fun performFactoryReset() {
        dataStore.updateData {
            val builder = AppSettings.getDefaultInstance().toBuilder()
            builder.setIntegrityViolationCount(0)
            val hash = calculateIntegrityHash(builder.build())
            builder.setIntegritySignature(hash)
            builder.build()
        }
    }

    val appModeFlow: Flow<String?> = validatedData.map { it.appMode.ifEmpty { null } }
    val trackerIdFlow: Flow<String> = validatedData.map { it.trackerId.ifEmpty { DEFAULT_TRACKER_ID } }
    val viewerIdFlow: Flow<String> = validatedData.map { it.viewerId.ifEmpty { DEFAULT_VIEWER_ID } }
    val relayUrlFlow: Flow<String> = validatedData.map { it.relayUrl.ifEmpty { DEFAULT_RELAY_URL } }
    val isManualExitFlow: Flow<Boolean> = validatedData.map { it.isManualExit }
    val lastAlarmAckTsFlow: Flow<Long> = validatedData.map { it.lastAlarmAckTs }
    
    val trackerAlarmAckTsFlow: Flow<Long> = validatedData.map { 
        it.roleLongsMap.getOrDefault(AppRole.TRACKER.prefix + LAST_ALARM_ACK_TS_KEY, 0L) 
    }
    val viewerAlarmAckTsFlow: Flow<Long> = validatedData.map { 
        it.roleLongsMap.getOrDefault(AppRole.VIEWER_SELF.prefix + LAST_ALARM_ACK_TS_KEY, 0L) 
    }
    val viewerRemoteAlarmAckTsFlow: Flow<Long> = validatedData.map { 
        it.roleLongsMap.getOrDefault(AppRole.VIEWER_REMOTE.prefix + LAST_ALARM_ACK_TS_KEY, 0L) 
    }
    
    val homePointsFlow: Flow<List<GeoPoint>> = validatedData.map { it.homePointsList.map { p -> GeoPoint(p.lat, p.lng) } }
    val maxDistanceFlow: Flow<Double> = validatedData.map { if (it.maxDistance > 0.0) it.maxDistance else DEFAULT_MAX_DISTANCE }
    val alertSettingsFlow: Flow<AlertSettings> = validatedData.map { SettingsMapper.protoToAlertSettings(it.alertSettings) }
    val identitySanitizedFlow: Flow<Boolean> = validatedData.map { it.identitySanitized }
    val isSystemActiveFlow: Flow<Boolean> = validatedData.map { it.isSystemActive }
    val isXiaomiManualOverrideFlow: Flow<Boolean> = validatedData.map { it.isXiaomiManualOverride }
    val recoveryCountFlow: Flow<Int> = validatedData.map { it.recoveryCount }
    val cumulativeRecoveryBlackoutMsFlow: Flow<Long> = validatedData.map { it.totalDrop }

    val isRestorationDisabledFlow: Flow<Boolean> = validatedData.map { it.isRestorationDisabled }

    suspend fun getSettingsSnapshot(): AppSettings = validatedData.first()

    suspend fun getAppMode(): String? = validatedData.first().appMode.ifEmpty { null }

    private fun routeToNamespaced(key: String): Pair<AppRole, String>? {
        return AppRole.fromKey(key)
    }

    // --- Unified Role-Based API (Namespaced Storage) ---

    suspend fun saveString(role: AppRole, key: String, value: String) {
        dataStore.mutate { putRoleStrings(role.prefix + key, value) }
    }

    suspend fun getString(role: AppRole, key: String, default: String): String {
        return validatedData.first().roleStringsMap.getOrDefault(role.prefix + key, default)
    }

    suspend fun saveLong(role: AppRole, key: String, value: Long) {
        dataStore.mutate { putRoleLongs(role.prefix + key, value) }
    }

    suspend fun getLong(role: AppRole, key: String, default: Long): Long {
        return validatedData.first().roleLongsMap.getOrDefault(role.prefix + key, default)
    }

    suspend fun saveDouble(role: AppRole, key: String, value: Double) {
        dataStore.mutate { putRoleDoubles(role.prefix + key, value) }
    }

    suspend fun getDouble(role: AppRole, key: String, default: Double): Double {
        return validatedData.first().roleDoublesMap.getOrDefault(role.prefix + key, default)
    }

    suspend fun saveBoolean(role: AppRole, key: String, value: Boolean) {
        dataStore.mutate { putRoleBools(role.prefix + key, value) }
    }

    suspend fun getBoolean(role: AppRole, key: String, default: Boolean): Boolean {
        return validatedData.first().roleBoolsMap.getOrDefault(role.prefix + key, default)
    }

    suspend fun saveInt(role: AppRole, key: String, value: Int) {
        dataStore.mutate { putRoleInts(role.prefix + key, value) }
    }

    suspend fun getInt(role: AppRole, key: String, default: Int): Int {
        return validatedData.first().roleIntsMap.getOrDefault(role.prefix + key, default)
    }

    suspend fun resetRoleState(role: AppRole) {
        dataStore.mutate {
            val prefix = role.prefix
            roleLongsMap.keys.filter { AppRole.fromKey(it)?.first == role }.forEach { removeRoleLongs(it) }
            roleDoublesMap.keys.filter { AppRole.fromKey(it)?.first == role }.forEach { removeRoleDoubles(it) }
            roleBoolsMap.keys.filter { AppRole.fromKey(it)?.first == role }.forEach { removeRoleBools(it) }
            roleIntsMap.keys.filter { AppRole.fromKey(it)?.first == role }.forEach { removeRoleInts(it) }
            roleStringsMap.keys.filter { AppRole.fromKey(it)?.first == role }.forEach { removeRoleStrings(it) }
            removeRoleStates(prefix)
            removeRoleAlarms(prefix)
            removeRoleLogicStates(prefix)
        }
    }

    fun saveLocationUpdate(status: LocationUpdate, role: AppRole? = null) {
        scope.launch {
            dataStore.mutate {
                val proto = SettingsMapper.mapLocationUpdateToProto(status)
                if (role != null) putRoleStates(role.prefix, proto) else setTrackerState(proto)
            }
        }
    }

    suspend fun loadLocationUpdate(role: AppRole? = null): LocationUpdate? {
        val settings = validatedData.first()
        if (role != null) {
            val proto = settings.roleStatesMap[role.prefix] ?: return null
            return SettingsMapper.mapLocationUpdateFromProto(proto)
        }
        if (!settings.hasTrackerState()) return null
        return SettingsMapper.mapLocationUpdateFromProto(settings.trackerState)
    }

    suspend fun saveActiveAlarms(alarms: List<AlarmEvaluationState.ActiveAlarm>, role: AppRole? = null) {
        dataStore.mutate {
            val protoAlarms = alarms.map { SettingsMapper.activeAlarmToProto(it) }
            if (role != null) {
                val listProto = ActiveAlarmsListProto.newBuilder().addAllAlarms(protoAlarms).build()
                putRoleAlarms(role.prefix, listProto)
            } else {
                clearActiveAlarms().addAllActiveAlarms(protoAlarms)
            }
        }
    }

    suspend fun loadActiveAlarms(role: AppRole? = null): List<AlarmEvaluationState.ActiveAlarm> {
        val settings = validatedData.first()
        val protoAlarms = if (role != null) {
            settings.roleAlarmsMap[role.prefix]?.alarmsList ?: emptyList()
        } else {
            settings.activeAlarmsList
        }
        return protoAlarms.map { SettingsMapper.activeAlarmFromProto(it) }
    }

    suspend fun saveLogicState(state: AlarmEvaluationState, role: AppRole) {
        dataStore.mutate {
            val proto = SettingsMapper.logicStateToProto(state)
            putRoleLogicStates(role.prefix, proto)
        }
    }

    // --- Global String-Keyed API (Strict Global only) ---

    suspend fun saveString(keyName: String, value: String) {
        routeToNamespaced(keyName)?.let { (role, key) -> saveString(role, key, value); return }
        dataStore.mutate {
            when (keyName) {
                APP_MODE_KEY -> setAppMode(value)
                TRACKER_ID_KEY -> setTrackerId(value)
                VIEWER_ID_KEY -> setViewerId(value)
                RELAY_URL_KEY -> setRelayUrl(value)
                SELECTED_SIREN_KEY -> setSelectedSiren(value)
            }
        }
    }

    suspend fun saveLong(keyName: String, value: Long) {
        routeToNamespaced(keyName)?.let { (role, key) -> saveLong(role, key, value); return }
        dataStore.mutate {
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
                COOLING_ENTERED_RT_KEY -> setCoolingEnteredRt(value)
            }
        }
    }

    suspend fun saveDouble(keyName: String, value: Double) {
        routeToNamespaced(keyName)?.let { (role, key) -> saveDouble(role, key, value); return }
        dataStore.mutate {
            when (keyName) {
                MAX_DISTANCE_STORAGE_KEY -> setMaxDistance(value)
                MAX_TEMP_KEY -> setMaxTemp(value)
            }
        }
    }

    suspend fun saveBoolean(keyName: String, value: Boolean) {
        routeToNamespaced(keyName)?.let { (role, key) -> saveBoolean(role, key, value); return }
        dataStore.mutate {
            when (keyName) {
                IS_MANUAL_EXIT_KEY -> setIsManualExit(value)
                IS_MIC_TYPE_STARTED_KEY -> setIsMicTypeStarted(value)
                IS_XIAOMI_MANUAL_OVERRIDE_KEY -> setIsXiaomiManualOverride(value)
                IDENTITY_SANITIZED_KEY -> setIdentitySanitized(true)
                IS_SYSTEM_ACTIVE_KEY -> setIsSystemActive(value)
                IS_COOLING_MODE_ACTIVE_KEY -> setIsCoolingModeActive(value)
            }
        }
    }

    suspend fun saveInt(keyName: String, value: Int) {
        routeToNamespaced(keyName)?.let { (role, key) -> saveInt(role, key, value); return }
        dataStore.mutate {
            when (keyName) {
                LAST_AUTO_SAVE_HOUR_KEY -> setLastAutoSaveHour(value)
                LAST_VERSION_CODE_KEY -> setLastVersionCode(value)
                RECOVERY_COUNT_KEY -> setRecoveryCount(value)
            }
        }
    }

    /**
     * incrementInt: Atomically increments an integer value in DataStore.
     * Guaranteed thread-safe via updateData actor.
     */
    suspend fun incrementInt(keyName: String, delta: Int) {
        routeToNamespaced(keyName)?.let { (role, key) ->
            dataStore.mutate {
                val compositeKey = role.prefix + key
                val current = roleIntsMap.getOrDefault(compositeKey, 0)
                putRoleInts(compositeKey, current + delta)
            }
            return
        }
        dataStore.mutate {
            when (keyName) {
                RECOVERY_COUNT_KEY -> setRecoveryCount(recoveryCount + delta)
                LAST_AUTO_SAVE_HOUR_KEY -> setLastAutoSaveHour(lastAutoSaveHour + delta)
                LAST_VERSION_CODE_KEY -> setLastVersionCode(lastVersionCode + delta)
            }
        }
    }

    suspend fun getString(keyName: String, default: String): String {
        val settings = validatedData.first()
        routeToNamespaced(keyName)?.let { (role, key) -> return getString(role, key, default) }
        val value = when (keyName) {
            TRACKER_ID_KEY -> settings.trackerId
            VIEWER_ID_KEY -> settings.viewerId
            RELAY_URL_KEY -> settings.relayUrl
            else -> ""
        }
        return value.ifEmpty { default }
    }

    suspend fun getLong(keyName: String, default: Long): Long {
        val settings = validatedData.first()
        routeToNamespaced(keyName)?.let { (role, key) -> return getLong(role, key, default) }
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
            COOLING_ENTERED_RT_KEY -> settings.coolingEnteredRt
            else -> 0L
        }
        return if (value == 0L) default else value
    }

    suspend fun getDouble(keyName: String, default: Double): Double {
        val settings = validatedData.first()
        routeToNamespaced(keyName)?.let { (role, key) -> return getDouble(role, key, default) }
        val value = when (keyName) {
            MAX_DISTANCE_STORAGE_KEY -> settings.maxDistance
            MAX_TEMP_KEY -> settings.maxTemp
            else -> 0.0
        }
        return if (value == 0.0) default else value
    }

    suspend fun getBoolean(keyName: String, default: Boolean): Boolean {
        val settings = validatedData.first()
        routeToNamespaced(keyName)?.let { (role, key) -> return getBoolean(role, key, default) }
        return when (keyName) {
            IS_MANUAL_EXIT_KEY -> settings.isManualExit
            IS_MIC_TYPE_STARTED_KEY -> settings.isMicTypeStarted
            IS_XIAOMI_MANUAL_OVERRIDE_KEY -> settings.isXiaomiManualOverride
            IDENTITY_SANITIZED_KEY -> settings.identitySanitized
            IS_SYSTEM_ACTIVE_KEY -> settings.isSystemActive
            IS_COOLING_MODE_ACTIVE_KEY -> settings.isCoolingModeActive
            else -> default
        }
    }

    suspend fun getInt(keyName: String, default: Int): Int {
        val settings = validatedData.first()
        routeToNamespaced(keyName)?.let { (role, key) -> return getInt(role, key, default) }
        val value = when (keyName) {
            LAST_AUTO_SAVE_HOUR_KEY -> settings.lastAutoSaveHour
            LAST_VERSION_CODE_KEY -> settings.lastVersionCode
            RECOVERY_COUNT_KEY -> settings.recoveryCount
            else -> -1
        }
        return if (value == -1) default else value
    }

    suspend fun setAppMode(mode: String?) {
        dataStore.mutate { setAppMode(mode ?: "") }
    }

    suspend fun setRestorationDisabled(disabled: Boolean) {
        dataStore.mutate { setIsRestorationDisabled(disabled) }
    }

    suspend fun loadHomePoints(): List<GeoPoint> = validatedData.first().homePointsList.map { GeoPoint(it.lat, it.lng) }

    suspend fun saveHomePoints(points: List<GeoPoint>, maxDistance: Double? = null, timestamp: Long? = null): Long {
        val ts = timestamp ?: timeProvider.currentTimeMillis
        dataStore.mutate {
            clearHomePoints().addAllHomePoints(points.map { GeoPointProto.newBuilder().setLat(it.latitude).setLng(it.longitude).build() })
            setHomePointsTs(ts)
            maxDistance?.let { setMaxDistance(it) }
        }
        return ts
    }

    suspend fun addHomePoint(lat: Double, lng: Double): Long {
        val ts = timeProvider.currentTimeMillis
        dataStore.mutate {
            addHomePoints(GeoPointProto.newBuilder().setLat(lat).setLng(lng).build())
            setHomePointsTs(ts)
        }
        return ts
    }

    suspend fun removeHomePoint(index: Int): Long {
        val ts = timeProvider.currentTimeMillis
        dataStore.mutate {
            if (index >= 0 && index < homePointsCount) {
                removeHomePoints(index)
                setHomePointsTs(ts)
            }
        }
        return ts
    }

    suspend fun loadAlertSettings(): AlertSettings {
        val s = validatedData.first().alertSettings
        return SettingsMapper.protoToAlertSettings(s)
    }

    suspend fun saveAlertSettings(s: AlertSettings) {
        dataStore.mutate {
            setAlertSettings(SettingsMapper.alertSettingsToProto(s))
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
                setMaxDropTs(timeProvider.currentTimeMillis)
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
        var validationError: String? = null
        val result = dataStore.updateData { s ->
            val nextTrackerId = if (s.hasDraftTrackerId()) s.draftTrackerId else s.trackerId
            val nextViewerId = if (s.hasDraftViewerId()) s.draftViewerId else s.viewerId
            
            // Oct24.16: Centralized Identity Validation (SIMP-OCT22.0-S1)
            val validation = ConfigValidator.validateIdentityUniqueness(nextTrackerId, nextViewerId)
            if (validation is ConfigValidator.ValidationResult.Failure) {
                validationError = validation.message
                return@updateData s
            }
            
            val builder = s.toBuilder()
            if (s.hasDraftTrackerId()) { builder.setTrackerId(s.draftTrackerId).clearDraftTrackerId() }
            if (s.hasDraftViewerId()) { builder.setViewerId(s.draftViewerId).clearDraftViewerId() }
            if (s.hasDraftRelayUrl()) { builder.setRelayUrl(s.draftRelayUrl).clearDraftRelayUrl() }
            if (s.draftMaxDistance > 0) { builder.setMaxDistance(s.draftMaxDistance).setDraftMaxDistance(0.0) }
            if (s.hasDraftAlertSettings()) { builder.setAlertSettings(s.draftAlertSettings).clearDraftAlertSettings() }
            
            // Oct28.12: Reset violation count on manual commit
            builder.setIntegrityViolationCount(0)
            
            // Oct28.12: Update integrity signature during manual commit
            val intermediate = builder.build()
            builder.setIntegritySignature(SettingsRepository.calculateIntegrityHash(intermediate))
            
            builder.build()
        }
        
        if (validationError != null) return CommitResult(error = validationError)
        return CommitResult(anyChanged = true) // simplified changed tracking
    }

    suspend fun clearDraftSettings() {
        dataStore.mutate { clearDraftTrackerId().clearDraftViewerId().clearDraftRelayUrl().setDraftMaxDistance(0.0).clearDraftAlertSettings() }
    }
}
