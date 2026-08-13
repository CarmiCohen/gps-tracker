package com.gps19.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * MainActivity: Entry point for the GPS Tracker application.
 * Aug.13.04:
 * - Issue #150: Samsung A15 R405 Detection Hardening. Removed redundant R405 
 *   trigger logic as it was moved to MainViewModel monitoring loop (R405).
 * July.31.01:
 * - Issue #661: Foreground Service Start Hardening. Hardened onStartService to 
 *   catch ForegroundServiceStartNotAllowedException even if RESUMED check passes.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val cachedPkgName by lazy { packageName }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        Timber.d("MainActivity onCreate version ${BuildConfig.VERSION_NAME} on ${Build.MODEL}")

        // Handle cold-start intent
        intent?.let { handleIntent(it) }

        setContent {
            MainAppContent(
                activity = this,
                viewModel = viewModel,
                onStartService = { mode ->
                    // Issue #661: Prevent start crash if OS blocks FGS despite foreground state.
                    try {
                        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            val serviceClass = if (mode == "tracker") TrackerService::class.java else ViewerService::class.java
                            val intent = Intent(this, serviceClass)
                            ContextCompat.startForegroundService(this, intent)
                        } else {
                            Timber.w("Issue #661: Deferred service start for $mode (Activity not RESUMED)")
                            viewModel.onEvent(UiEvent.SetRecoveryPending(true))
                        }
                    } catch (e: Throwable) {
                        Timber.e(e, "Issue #661: Foreground service start failed for mode $mode. Marking as pending.")
                        viewModel.onEvent(UiEvent.SetRecoveryPending(true))
                    }
                },
                onCleanupAndExit = {
                    stopService(Intent(this, TrackerService::class.java))
                    stopService(Intent(this, ViewerService::class.java))
                    finishAffinity()
                },
                onRequestBatteryExemption = { launchBatteryExemptionSetting() },
                onRequestOverlayPermission = {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$cachedPkgName".toUri())
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Could not open overlay settings", Toast.LENGTH_SHORT).show()
                        Timber.e(e, "Overlay permission launch failure")
                    }
                },
                onRequestAppInfo = {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            setData("package:$cachedPkgName".toUri())
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Could not open App Info", Toast.LENGTH_SHORT).show()
                    }
                },
                onRequestExactAlarm = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                setData("package:$cachedPkgName".toUri())
                            }
                            startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(this, "Could not open alarm settings", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onRequestHardwarePermission = {
                    // Logic to open manufacturer-specific settings or generic permissions
                },
                onStopTracking = {
                    stopService(Intent(this, TrackerService::class.java))
                    stopService(Intent(this, ViewerService::class.java))
                }
            )
        }
    }

    private fun launchBatteryExemptionSetting() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                setData("package:$cachedPkgName".toUri())
            }
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                startActivity(intent)
            } catch (e2: Exception) {
                Toast.makeText(this, "Could not open battery settings", Toast.LENGTH_SHORT).show()
                Timber.e(e2, "Final battery exemption launch failure")
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == ACTION_NAVIGATE_TO_MAP) {
            Timber.d("Handling ACTION_NAVIGATE_TO_MAP deep link")
            viewModel.onEvent(UiEvent.ToggleMap(true))
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onEvent(UiEvent.RefreshPermissionStatus)
        
        // Issue #626/634: Automated recovery trigger for restricted background starts
        if (viewModel.uiState.value.isRecoveryPending) {
            Timber.i("Issue #634: Resuming deferred service recovery in onResume")
            viewModel.onEvent(UiEvent.TriggerRecovery)
        }

        // R405 logic moved to MainViewModel monitoring loop.
    }
}
