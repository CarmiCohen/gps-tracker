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
 * Sep.05.11:
 * - Issue #910 Forensic Instrumentation: Added logging to onStopTracking to 
 *   identify the source of service termination (R910).
 * Sep.05.08:
 * - Issue #910 Forensic Instrumentation: Added detailed logging and stack trace 
 *   capture to onCleanupAndExit.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val cachedPkgName: String get() = GpsApplication.PACKAGE_NAME

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
                    val trace = Thread.currentThread().stackTrace.take(15).joinToString("\n")
                    Timber.w("Issue #910: onCleanupAndExit invoked. Trace:\n$trace")
                    stopService(Intent(this, TrackerService::class.java))
                    stopService(Intent(this, ViewerService::class.java))
                    finishAffinity()
                },
                onRequestBatteryExemption = { launchBatteryExemptionSetting() },
                onRequestOverlayPermission = {
                    val pkg = cachedPkgName.ifBlank { packageName }
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        data = android.net.Uri.fromParts("package", pkg, null)
                    }
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                        } catch (e2: Exception) {
                            Toast.makeText(this, "Could not open overlay settings", Toast.LENGTH_SHORT).show()
                            Timber.e(e2, "Overlay permission launch failure")
                        }
                    }
                },
                onRequestAppInfo = {
                    val pkg = cachedPkgName.ifBlank { packageName }
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.fromParts("package", pkg, null)
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Could not open App Info", Toast.LENGTH_SHORT).show()
                    }
                },
                onRequestExactAlarm = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val pkg = cachedPkgName.ifBlank { packageName }
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = android.net.Uri.fromParts("package", pkg, null)
                            }
                            startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(this, "Could not open alarm settings", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onRequestHardwarePermission = {
                },
                onStopTracking = {
                    val trace = Thread.currentThread().stackTrace.take(15).joinToString("\n")
                    Timber.w("Issue #910: onStopTracking invoked. Trace:\n$trace")
                    stopService(Intent(this, TrackerService::class.java))
                    stopService(Intent(this, ViewerService::class.java))
                }
            )
        }
    }

    private fun launchBatteryExemptionSetting() {
        val pkg = cachedPkgName.ifBlank { packageName }
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = android.net.Uri.fromParts("package", pkg, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Issue #896: Primary battery optimization intent failed for $pkg")
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (e2: Exception) {
                Timber.e(e2, "Issue #896: Fallback optimization intent failed. Navigating to App Info.")
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", pkg, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                } catch (e3: Exception) {
                    Toast.makeText(this, "Could not open battery settings", Toast.LENGTH_SHORT).show()
                }
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
        if (viewModel.uiState.value.isRecoveryPending) {
            Timber.i("Issue #634: Resuming deferred service recovery in onResume")
            viewModel.onEvent(UiEvent.TriggerRecovery)
        }
    }

    companion object {
        const val ACTION_NAVIGATE_TO_MAP = "com.gps19.app.ACTION_NAVIGATE_TO_MAP"
    }
}
