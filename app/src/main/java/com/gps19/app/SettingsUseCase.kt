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
 * Extracted from MainViewModel to resolve Issue 115 (Architectural Bloat).
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
            maxDistance = draft.maxDistance.toFloatOrNull() ?: 0f,
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

    suspend fun loadAllSettings(): InitialSettings {
        val dId = repository.getString(MainRepository.TRACKER_ID_KEY, MainRepository.DEFAULT_TRACKER_ID)
        val vId = repository.getString(MainRepository.VIEWER_ID_KEY, MainRepository.DEFAULT_VIEWER_ID)
        val rUrl = repository.getString(MainRepository.RELAY_URL_KEY, MainRepository.DEFAULT_RELAY_URL)
        val maxDist = repository.getFloat(MainRepository.MAX_DISTANCE_STORAGE_KEY, MainRepository.DEFAULT_MAX_DISTANCE.toFloat()).toDouble()
        val hPoints = repository.loadHomePoints()
        val aSettings = repository.loadAlertSettings()
        val mMode = repository.getAppMode()
        val sSiren = repository.getString(MainRepository.SELECTED_SIREN_KEY, "Siren")
        val lAlarmAck = repository.getLastAlarmAckTs()
        val lMaxTemp = repository.getFloat(MainRepository.MAX_TEMP_KEY, 0f)
        var appStartTime = repository.getLong(MainRepository.APP_START_TIME_KEY, 0L)
        
        if (appStartTime == 0L) {
            appStartTime = timeProvider.currentTimeMillis()
            repository.saveLong(MainRepository.APP_START_TIME_KEY, appStartTime)
        }

        val draftAlerts = repository.loadDraftAlertSettings()
        val draftDId = repository.getString(MainRepository.DRAFT_TRACKER_ID, "")
        val draftVId = repository.getString(MainRepository.DRAFT_VIEWER_ID, "")
        val draftRUrl = repository.getString(MainRepository.DRAFT_RELAY_URL, "")
        val draftMaxDist = repository.getFloat(MainRepository.DRAFT_MAX_DISTANCE, 0f)
        
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

        return InitialSettings(
            deviceId = dId, viewerId = vId, relayUrl = rUrl, maxDistance = maxDist,
            homePoints = hPoints, alertSettings = aSettings, appMode = mMode,
            selectedSirenType = sSiren, lastAlarmAckTs = lAlarmAck, maxTemp = lMaxTemp,
            appStartTime = appStartTime, draftSettings = draftSettings,
            trackerStatus = trackerStatus
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
}

data class InitialSettings(
    val deviceId: String, val viewerId: String, val relayUrl: String, val maxDistance: Double,
    val homePoints: List<org.osmdroid.util.GeoPoint>, val alertSettings: AlertSettings,
    val appMode: String?, val selectedSirenType: String, val lastAlarmAckTs: Long,
    val maxTemp: Float, val appStartTime: Long, val draftSettings: DraftSettings?,
    val trackerStatus: TrackerStatus?
)
