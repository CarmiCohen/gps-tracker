package com.gps19.app

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ConfigManager: Manages identity and core configuration settings.
 * Aug.01.00:
 * - Issue #664: Forensic Audit: Startup Davey Stalls. Consolidated multiple 
 *   Main.immediate collectors into a single background observation to reduce 
 *   main-thread pressure during startup.
 * July.28.24:
 * - Issue #618: Forensic UI State Collection Audit.
 */
@Singleton
class ConfigManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MainRepository
) {
    @Volatile var isTrackerMode: Boolean = true
    @Volatile var deviceId: String = ""
    @Volatile var viewerId: String = ""
    @Volatile var relayUrl: String = DEFAULT_RELAY_URL

    // Issue #664: Using IO scope for initialization and observations to 
    // prevent main-thread contention during Hilt injection.
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        observeSettings()
    }

    private fun observeSettings() {
        scope.launch {
            combine(
                repository.appModeFlow,
                repository.trackerIdFlow,
                repository.viewerIdFlow,
                repository.relayUrlFlow
            ) { mode, tId, vId, url ->
                // Apply values to volatile fields
                if (mode != null) isTrackerMode = (mode == "tracker")
                if (tId.isNotEmpty()) deviceId = tId
                if (vId.isNotEmpty()) viewerId = vId
                if (url.isNotEmpty()) relayUrl = url
            }.collect { }
        }
    }

    companion object {
        const val DEFAULT_RELAY_URL = SettingsRepository.DEFAULT_RELAY_URL
    }
}
