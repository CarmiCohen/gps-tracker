package com.gps19.app

import android.content.Context
import android.widget.Toast
import com.gps19.core.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SettingsUseCase: Encapsulates business logic for application configuration.
 * Handles draft lifecycle, atomic commits, and full system resets.
 * v9.3.3:
 * - Issue #039 Identity Rejection Feedback: Added bulkUpdateSettings to support 
 *   atomic updates with validation from external sources.
 * v9.3.0:
 * - Issue #042: Sanitization Visibility. Added identitySanitized to InitialSettings 
 *   and loadAllSettings to inform UI of automatic ID resets.
 */
@Singleton
class SettingsUseCase @Inject constructor(
    private val repository: MainRepository,
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
        val dId = repository.getString(MainRepository.TRACKER_ID_KEY, MainRepository.DEFAULT_TRACKER_ID)
        val vId = repository.getString(MainRepository.VIEWER_ID_KEY, MainRepository.DEFAULT_VIEWER_ID)
        val rUrl = repository.getString(MainRepository.RELAY_URL_KEY, MainRepository.DEFAULT_RELAY_URL)
        
        val maxDist = repository.getDouble(MainRepository.MAX_DISTANCE_STORAGE_KEY, MainRepository.DEFAULT_MAX_DISTANCE)
        val hPoints = repository.loadHomePoints()
        val aSettings = repository.loadAlertSettings()
        val mMode = repository.getAppMode()
        val sSiren = repository.getString(MainRepository.SELECTED_SIREN_KEY, "Siren")
        val lAlarmAck = repository.getLastAlarmAckTs()
        val lMaxTemp = repository.getDouble(MainRepository.MAX_TEMP_KEY, 0.0)
        val sanitized = repository.getBoolean(MainRepository.IDENTITY_SANITIZED_KEY, false)
        
        var appStartTime = repository.getLong(MainRepository.APP_START_TIME_KEY, 0L)
        if (appStartTime == 0L) {
            appStartTime = timeProvider.currentTimeMillis()
            repository.saveLong(MainRepository.APP_START_TIME_KEY, appStartTime)
        }

        val draftAlerts = repository.loadDraftAlertSettings()
        val draftDId = repository.getString(MainRepository.DRAFT_TRACKER_ID, "")
        val draftVId = repository.getString(MainRepository.DRAFT_VIEWER_ID, "")
        val draftRUrl = repository.getString(MainRepository.DRAFT_RELAY_URL, "")
        val draftMaxDist = repository.getDouble(MainRepository.DRAFT_MAX_DISTANCE, 0.0)
        
        var draftSettings: DraftSettings? = null
        if (draftDId.isNotEmpty() || draftVId.isNotEmpty() || draftRUrl.isNotEmpty() || draftMaxDist > 0 || draftAlerts != null) {
            draftSettings = DraftSettings(
                deviceId = if (draftDId.isNotEmpty()) draftDId else dId,
                viewerId = if (draftVId.isNotEmpty()) draftVId else vId,
                relayUrl = if (draftRUrl.isNotEmpty()) draftRUrl else rUrl,
                maxDistance = if (draftMaxDist > 0) draftMaxDist.toInt().toString() else if (maxDist > 0) maxDist.toInt().toString() else "",
                alertSettings = draftAlerts ?: aSettings
            )
        }

        val trackerStatus = repository.loadTrackerState()

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
