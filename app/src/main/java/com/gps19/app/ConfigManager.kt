package com.gps19.app

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ConfigManager: Manages identity and core configuration settings.
 * July.28.24:
 * - Issue #618: Forensic UI State Collection Audit. Migrated to 
 *   Dispatchers.Main.immediate to eliminate configuration sync latency (R618).
 * July.24.04:
 * - Issue #540: Identity Sync Bug.
 */
@Singleton
class ConfigManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MainRepository
) {
    var isTrackerMode: Boolean = true
    var deviceId: String = ""
    var viewerId: String = ""
    var relayUrl: String = DEFAULT_RELAY_URL

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    init {
        observeSettings()
    }

    private fun observeSettings() {
        scope.launch {
            repository.appModeFlow.collectLatest { mode ->
                if (mode != null) isTrackerMode = (mode == "tracker")
            }
        }
        scope.launch {
            repository.trackerIdFlow.collectLatest { id ->
                if (id.isNotEmpty()) deviceId = id
            }
        }
        scope.launch {
            repository.relayUrlFlow.collectLatest { url ->
                if (url.isNotEmpty()) relayUrl = url
            }
        }
        scope.launch {
            repository.viewerIdFlow.collectLatest { id ->
                if (id.isNotEmpty()) viewerId = id
            }
        }
    }

    companion object {
        const val DEFAULT_RELAY_URL = SettingsRepository.DEFAULT_RELAY_URL
    }
}
