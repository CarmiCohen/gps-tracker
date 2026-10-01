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
 * Oct.1.7:
 * - Issue #1402-B: System-Wide Alarm Overlay. Integrated ACTION_FIX_PERMISSIONS 
 *   handler to navigate directly to overlay or battery settings from the overlay UI.
 * Sep.24.92:
 * - Issue #1261: Service Unification. Migrated to unified MonitorService for 
 *   all background operations (R-ID 471).
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
                            // Issue #1261: Unified MonitorService manages role transitions internally
                            val intent = Intent(this, MonitorService::class.java)
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
                    stopService(Intent(this, MonitorService::class.java))
                    finishAffinity()
                },
                onRequestBatteryExemption = { launchBatteryExemptionSetting() },
                onRequestOverlayPermission = { launchOverlayPermissionSetting() },
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
                    // Forward to specialized vendor settings if needed, otherwise App Info
                    launchOverlayPermissionSetting()
                },
                onStopTracking = {
                    val trace = Thread.currentThread().stackTrace.take(15).joinToString("\n")
                    Timber.w("Issue #910: onStopTracking invoked. Trace:\n$trace")
                    stopService(Intent(this, MonitorService::class.java))
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
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                } catch (e3: Exception) {
                    Toast.makeText(this, "Could not open battery settings", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun launchOverlayPermissionSetting() {
        val pkg = cachedPkgName.ifBlank { packageName }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    data = android.net.Uri.fromParts("package", pkg, null)
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
                Timber.d("Handling ACTION_NAVIGATE_TO_MAP deep link")
                viewModel.onEvent(UiEvent.ToggleMap(true))
            }
            ACTION_FIX_PERMISSIONS -> {
                Timber.d("Handling ACTION_FIX_PERMISSIONS deep link")
                launchOverlayPermissionSetting()
            }
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
        const val ACTION_FIX_PERMISSIONS = "com.gps19.app.ACTION_FIX_PERMISSIONS"
    }
}
