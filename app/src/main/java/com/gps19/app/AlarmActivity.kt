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
 * Oct.5.20:
 * - SIMP-1426-3: Updated AlarmOverlay call to use unified UiStateProvider (viewModel).
 *   Corrected property references to use 'session' flow.
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
            val sessionState by viewModel.session.collectAsStateWithLifecycle()

            GpsTrackerTheme(appMode = sessionState.appMode) {
                BackHandler {
                    viewModel.onEvent(UiEvent.DismissAlarms)
                    finish()
                }

                Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                    AlarmOverlay(
                        stateProvider = viewModel,
                        onMute = {
                            val currentAlarms = viewModel.hudHealthState.value.activeAlarms
                            val currentCauses = currentAlarms.filter { !it.isResolved }.joinToString { it.title }.ifBlank { "Muted" }
                            viewModel.onEvent(UiEvent.StopSiren(currentCauses))
                        },
                        onClose = {
                            viewModel.onEvent(UiEvent.DismissAlarms)
                            finish()
                        },
                        onGoToMap = {
                            viewModel.onEvent(UiEvent.DismissAlarms)
                            
                            val currentAlarms = viewModel.hudHealthState.value.activeAlarms
                            val currentCauses = currentAlarms.filter { !it.isResolved }.joinToString { it.title }.ifBlank { "Map Navigation" }
                            viewModel.onEvent(UiEvent.StopSiren(currentCauses))
                            
                            val intent = Intent(this, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                action = ACTION_NAVIGATE_TO_MAP
                            }
                            startActivity(intent)
                            finish()
                        },
                        onHardwarePermissionClick = {
                            viewModel.onEvent(UiEvent.NavigateToDiagnostics(true))
                            val intent = Intent(this, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                action = "com.gps19.app.ACTION_VIEW_DIAGNOSTICS"
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
