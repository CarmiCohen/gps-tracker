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
 * Sep.15.04:
 * - Context Shadowing Automation (#1047): Switched to @ApplicationContext 
 *   as IPC optimization is now handled globally in GpsApplication (R-ID 240).
 * Sep.14.10:
 * - IPC Noise Suppression (#1019): Migrated to @ShadowContext to utilize 
 *   ShadowCache for package name lookups during config hydration (R-ID 324).
 * Aug.01.00:
 * - Issue #664: Forensic Audit: Startup Davey Stalls. Consolidated collectors 
 *   into a single background observation to reduce main-thread pressure.
 */
@Singleton
class ConfigManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MainRepository
) {
    @Volatile var isTrackerMode: Boolean = true
    @Volatile var deviceId: String = ""
    @Volatile var viewerId: String = ""
    @Volatile var maxDistance: Double = DEFAULT_MAX_DISTANCE
    @Volatile var relayUrl: String = DEFAULT_RELAY_URL

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
                repository.relayUrlFlow,
                repository.maxDistanceFlow
            ) { mode, tId, vId, url, dist ->
                if (mode != null) isTrackerMode = (mode == "tracker")
                if (tId.isNotEmpty()) deviceId = tId
                if (vId.isNotEmpty()) viewerId = vId
                if (url.isNotEmpty()) relayUrl = url
                maxDistance = dist
            }.collect { }
        }
    }

    companion object {
        const val DEFAULT_RELAY_URL = SettingsRepository.DEFAULT_RELAY_URL
        const val DEFAULT_MAX_DISTANCE = SettingsRepository.DEFAULT_MAX_DISTANCE
    }
}
