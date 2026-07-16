package com.gps19.app

import android.content.Context
import android.widget.Toast
import com.gps19.core.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SettingsUseCase: Encapsulates business logic for application configuration.
 * v9.5.0:
 * - Issue #503: Hilt Removal.
 */
class SettingsUseCase(
    private val repository: MainRepository,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider,
    private val logManager: LogManager
) {
    fun prepareDraft(currentState: MainUiState): DraftSettings {
        val currentDraft = currentState.draftSettings
        val isDraftEmpty = currentDraft.deviceId.isEmpty() && 
                          currentDraft.viewerId.isEmpty() && 
                          currentDraft.relayUrl.isEmpty() && 
                          currentDraft.maxDistance.isEmpty() &&
                          currentDraft.alertSettings == AlertSettings()

        return if (isDraftEmpty) {
            DraftSettings(
                deviceId = currentState.deviceId,
                viewerId = currentState.viewerId,
                relayUrl = currentState.relayUrl,
                maxDistance = if (currentState.maxDistance == 0.0) "" else currentState.maxDistance.toInt().toString(),
                alertSettings = currentState.alertSettings
            )
        } else {
            currentDraft
        }
    }

    suspend fun saveDraftToRepo(draft: DraftSettings) {
        if (draft.deviceId.isEmpty() && draft.viewerId.isEmpty()) return
        
        repository.saveDraftSettings(
            deviceId = draft.deviceId.trim(),
            viewerId = draft.viewerId.trim(),
            relayUrl = draft.relayUrl.trim(),
            maxDistance = draft.maxDistance.toDoubleOrNull() ?: 0.0,
            alertSettings = draft.alertSettings
        )
    }

    suspend fun commitDraft(): CommitResult {
        return repository.commitDraftSettings()
    }

    suspend fun handleImmediateAlertUpdate(settings: AlertSettings) {
        withContext(Dispatchers.IO) {
            repository.saveAlertSettings(settings)
        }
    }

    suspend fun loadAllSettings(): InitialSettings = withContext(Dispatchers.IO) {
        val s = settingsRepository.getSettingsSnapshot()
        
        val dId = s.trackerId.ifEmpty { SettingsRepository.DEFAULT_TRACKER_ID }
        val vId = s.viewerId.ifEmpty { SettingsRepository.DEFAULT_VIEWER_ID }
        val rUrl = s.relayUrl.ifEmpty { SettingsRepository.DEFAULT_RELAY_URL }
        
        val maxDist = if (s.maxDistance > 0.0) s.maxDistance else SettingsRepository.DEFAULT_MAX_DISTANCE
        val hPoints = s.homePointsList.map { org.osmdroid.util.GeoPoint(it.lat, it.lng) }
        val aSettings = SettingsMapper.protoToAlertSettings(s.alertSettings)
        val mMode = s.appMode.ifEmpty { null }
        val sSiren = s.selectedSiren.ifEmpty { "Siren" }
        val lAlarmAck = s.lastAlarmAckTs
        val lMaxTemp = s.maxTemp
        val sanitized = s.identitySanitized
        
        var appStartTime = s.appStartTime
        if (appStartTime == 0L) {
            appStartTime = timeProvider.currentTimeMillis()
            repository.saveLong(MainRepository.APP_START_TIME_KEY, appStartTime)
        }

        var draftSettings: DraftSettings? = null
        if (s.hasDraftTrackerId() || s.hasDraftViewerId() || s.hasDraftRelayUrl() || s.draftMaxDistance > 0 || s.hasDraftAlertSettings()) {
            draftSettings = DraftSettings(
                deviceId = if (s.hasDraftTrackerId()) s.draftTrackerId else dId,
                viewerId = if (s.hasDraftViewerId()) s.draftViewerId else vId,
                relayUrl = if (s.hasDraftRelayUrl()) s.draftRelayUrl else rUrl,
                maxDistance = if (s.draftMaxDistance > 0) s.draftMaxDistance.toInt().toString() else if (maxDist > 0) maxDist.toInt().toString() else "",
                alertSettings = if (s.hasDraftAlertSettings()) SettingsMapper.protoToAlertSettings(s.draftAlertSettings) else aSettings
            )
        }

        val trackerStatus = if (s.hasTrackerState()) SettingsMapper.mapTrackerStatusFromProto(s.trackerState) else null

        InitialSettings(
            deviceId = dId, viewerId = vId, relayUrl = rUrl, maxDistance = maxDist,
            homePoints = hPoints, alertSettings = aSettings, appMode = mMode,
            selectedSirenType = sSiren, lastAlarmAckTs = lAlarmAck, maxTemp = lMaxTemp,
            appStartTime = appStartTime, draftSettings = draftSettings,
            trackerStatus = trackerStatus, identitySanitized = sanitized
        )
    }

    suspend fun fullInitialization(context: Context): Long {
        return withContext(Dispatchers.IO) {
            repository.resetStats()
            repository.clearLogs()
            repository.clearTrails()
            repository.saveHomePoints(emptyList(), MainRepository.DEFAULT_MAX_DISTANCE)
            repository.saveAlertSettings(AlertSettings())
            repository.saveString(MainRepository.TRACKER_ID_KEY, MainRepository.DEFAULT_TRACKER_ID)
            repository.saveString(MainRepository.VIEWER_ID_KEY, MainRepository.DEFAULT_VIEWER_ID)
            repository.saveString(MainRepository.RELAY_URL_KEY, MainRepository.DEFAULT_RELAY_URL)
            repository.saveBoolean(MainRepository.IDENTITY_SANITIZED_KEY, false)
            repository.clearDraftSettings()
            
            val appStartTime = timeProvider.currentTimeMillis()
            repository.saveLong(MainRepository.APP_START_TIME_KEY, appStartTime)
            repository.sendCommand(UiCommand.FullInitializationReset)
            
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Full initialization complete", Toast.LENGTH_SHORT).show()
            }
            appStartTime
        }
    }

    suspend fun updateDeviceId(id: String) {
        logManager.submitToLogSink("USER ACTION: Tracker ID changed to: $id", "user", important = true)
        repository.saveString(MainRepository.TRACKER_ID_KEY, id)
    }

    suspend fun updateViewerId(id: String) {
        logManager.submitToLogSink("USER ACTION: Viewer ID changed to: $id", "user", important = true)
        repository.saveString(MainRepository.VIEWER_ID_KEY, id)
    }

    suspend fun updateRelayUrl(url: String) {
        logManager.submitToLogSink("USER ACTION: Relay URL changed to: $url", "user", important = true)
        repository.saveString(MainRepository.RELAY_URL_KEY, url)
    }

    /**
     * Performs a bulk update of settings.
     * v9.3.3: Enforces validation via MainRepository.
     */
    suspend fun bulkUpdateSettings(
        deviceId: String? = null,
        viewerId: String? = null,
        relayUrl: String? = null,
        maxDistance: Double? = null,
        alertSettings: AlertSettings? = null,
        homePoints: List<org.osmdroid.util.GeoPoint>? = null
    ) {
        withContext(Dispatchers.IO) {
            repository.saveSettingsBulk(deviceId, viewerId, relayUrl, maxDistance, alertSettings, homePoints)
        }
    }
}

data class InitialSettings(
    val deviceId: String, val viewerId: String, val relayUrl: String, val maxDistance: Double,
    val homePoints: List<org.osmdroid.util.GeoPoint>, val alertSettings: AlertSettings,
    val appMode: String?, val selectedSirenType: String, val lastAlarmAckTs: Long,
    val maxTemp: Double, val appStartTime: Long, val draftSettings: DraftSettings?,
    val trackerStatus: TrackerStatus?, val identitySanitized: Boolean = false
)
