package com.gps19.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * AlarmActivity: Full-screen alarm overlay that bypasses the lock screen.
 * July.28.24:
 * - Issue #620: State Partitioning Audit. Migrated to partitioned KinematicState 
 *   and DiagnosticState for architectural consistency.
 */
@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        viewModel.onEvent(UiEvent.SetRedScreenVisible(true))

        viewModel.repository.uiCommands
            .onEach { command ->
                if (command is UiCommand.StopSiren) {
                    finish()
                }
            }
            .launchIn(lifecycleScope)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val kinematicState by viewModel.kinematicState.collectAsStateWithLifecycle()
            val diagnosticState by viewModel.diagnosticState.collectAsStateWithLifecycle()

            GpsTrackerTheme(appMode = uiState.appMode) {
                BackHandler {
                    viewModel.onEvent(UiEvent.DismissAlarms)
                    finish()
                }

                Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                    AlarmOverlay(
                        alarms = diagnosticState.activeAlarms,
                        isMuted = diagnosticState.isAlarmSilenced,
                        isLocationPending = kinematicState.trackerHealth.isLocationPending,
                        backgroundStatus = uiState.permissions.backgroundStatus,
                        hasBackgroundRestriction = uiState.permissions.hasBackgroundRestriction,
                        onHardwarePermissionClick = { viewModel.onEvent(UiEvent.ToggleXiaomiManualOverride) },
                        onMute = {
                            val currentCauses = diagnosticState.activeAlarms.filter { !it.isResolved }.joinToString { it.title }.ifBlank { "Muted" }
                            viewModel.onEvent(UiEvent.StopSiren(currentCauses))
                        },
                        onClose = {
                            viewModel.onEvent(UiEvent.DismissAlarms)
                            finish()
                        },
                        onGoToMap = {
                            viewModel.onEvent(UiEvent.DismissAlarms)
                            
                            val currentCauses = diagnosticState.activeAlarms.filter { !it.isResolved }.joinToString { it.title }.ifBlank { "Map Navigation" }
                            viewModel.onEvent(UiEvent.StopSiren(currentCauses))
                            
                            val intent = Intent(this, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                action = ACTION_NAVIGATE_TO_MAP
                            }
                            startActivity(intent)
                            finish()
                        }
                    )
                }
            }
        }
    }
}
