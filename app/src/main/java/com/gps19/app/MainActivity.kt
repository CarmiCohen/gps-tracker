package com.gps19.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * MainActivity: Entry point for the GPS Tracker application.
 * Oct.14.1:
 * - SIMP-OCT13.30-S1: Delegated system intent orchestration to SystemIntentHandler.
 * - SIMP-OCT13.21-S2: Finalized Pure Event transition.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var systemIntentHandler: SystemIntentHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        Timber.d("MainActivity onCreate version ${BuildConfig.VERSION_NAME} on ${Build.MODEL}")

        intent?.let { handleIntent(it) }

        setContent {
            MainAppContent(
                activity = this,
                viewModel = viewModel
            )
        }
    }

    fun startTrackingService() {
        try {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                val intent = Intent(this, MonitorService::class.java)
                ContextCompat.startForegroundService(this, intent)
            } else {
                Timber.w("Issue #661: Deferred service start (Activity not RESUMED)")
                viewModel.onEvent(UiEvent.SetRecoveryPending(true))
            }
        } catch (e: Throwable) {
            Timber.e(e, "Issue #661: Foreground service start failed.")
            viewModel.onEvent(UiEvent.SetRecoveryPending(true))
        }
    }

    fun stopTrackingService() {
        Timber.w("Issue #910: stopTrackingService invoked.")
        stopService(Intent(this, MonitorService::class.java))
    }

    fun cleanupAndExit() {
        Timber.w("Issue #910: cleanupAndExit invoked.")
        stopTrackingService()
        finishAffinity()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_NAVIGATE_TO_MAP -> {
                viewModel.onEvent(UiEvent.ToggleMap(true))
            }
            ACTION_FIX_PERMISSIONS -> {
                systemIntentHandler.getOverlayPermissionIntent()?.let { startActivity(it) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onEvent(UiEvent.RefreshPermissionStatus)
        if (viewModel.uiState.value.isRecoveryPending) {
            viewModel.onEvent(UiEvent.TriggerRecovery)
        }
    }

    companion object {
        const val ACTION_NAVIGATE_TO_MAP = "com.gps19.app.ACTION_NAVIGATE_TO_MAP"
        const val ACTION_FIX_PERMISSIONS = "com.gps19.app.ACTION_FIX_PERMISSIONS"
    }
}
