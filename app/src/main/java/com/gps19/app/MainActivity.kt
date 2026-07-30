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
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * MainActivity: Entry point for the GPS Tracker application.
 * July.30.23:
 * - Issue #626: Foreground Service Hardening. Wrapped service start in try-catch to handle 
 *   ForegroundServiceStartNotAllowedException on A15/Android 14+.
 * July.20.07:
 * - Release hardening and monitoring.
 * July.19.01:
 * - Issue #099: ANR Hardening. Offloaded hardware property checks to ViewModel.
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
                    try {
                        val serviceClass = if (mode == "tracker") TrackerService::class.java else ViewerService::class.java
                        val intent = Intent(this, serviceClass)
                        ContextCompat.startForegroundService(this, intent)
                    } catch (e: Exception) {
                        Timber.e(e, "Issue #626: Foreground service start failed for mode $mode")
                        // Exception is documented and handled via UI state monitoring
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
        
        // R405: Samsung A15 detected without battery exemption. Prompting user.
        val state = viewModel.uiState.value
        if (state.permissions.isA15Device && !state.permissions.isBatteryWhitelisted && !state.navigation.isPhoneSetupVisible) {
            Timber.i("R405: Samsung A15 detected without battery exemption. Prompting user.")
            viewModel.onEvent(UiEvent.TogglePhoneSetup(true))
        }
    }
}
