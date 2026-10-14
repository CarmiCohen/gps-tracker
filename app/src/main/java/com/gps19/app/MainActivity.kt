package com.gps19.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * MainActivity: Entry point for the GPS Tracker application.
 * Oct.13.30:
 * - SIMP-OCT13.21-S2: Finalized Pure Event transition. Moved system-level 
 *   launchers to Activity-scope public methods triggered by AppEffectAggregator.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

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

    fun launchAppInfo() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open App Info", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchExactAlarmSetting() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Could not open alarm settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun launchBatteryExemptionSetting() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (e2: Exception) {
                launchAppInfo()
            }
        }
    }

    fun launchOverlayPermissionSetting() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            } catch (e: Exception) {
                try {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                } catch (e2: Exception) {
                    Toast.makeText(this, "Could not open overlay settings", Toast.LENGTH_SHORT).show()
                }
            }
        }
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
                launchOverlayPermissionSetting()
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
