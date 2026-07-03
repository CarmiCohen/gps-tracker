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
 * v8.8.6: Deep-Link Cold-Start Handling (Issue #022 - Formerly 44).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Timber.d("All requested permissions granted.")
        } else {
            Timber.w("Some permissions were denied.")
        }
        viewModel.onEvent(UiEvent.RefreshPermissionStatus)
    }

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
                    val serviceClass = if (mode == "tracker") TrackerService::class.java else ViewerService::class.java
                    val intent = Intent(this, serviceClass)
                    ContextCompat.startForegroundService(this, intent)
                },
                onCleanupAndExit = {
                    stopService(Intent(this, TrackerService::class.java))
                    stopService(Intent(this, ViewerService::class.java))
                    finishAffinity()
                },
                onRequestBatteryExemption = {
                    try {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            setData("package:$packageName".toUri())
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
                },
                onRequestOverlayPermission = {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri())
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Could not open overlay settings", Toast.LENGTH_SHORT).show()
                        Timber.e(e, "Overlay permission launch failure")
                    }
                },
                onRequestAppInfo = {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            setData("package:$packageName".toUri())
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
                                setData("package:$packageName".toUri())
                            }
                            startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(this, "Could not open alarm settings", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onRequestXiaomiPermission = {
                    if (isXiaomiDevice()) {
                        try {
                            val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                                setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
                                putExtra("extra_pkgname", packageName)
                            }
                            startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    setData("package:$packageName".toUri())
                                }
                                startActivity(intent)
                            } catch (e2: Exception) {
                                Toast.makeText(this, "Xiaomi settings launch failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                checkAndRequestPermissions = { mode ->
                    val permissions = mutableListOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    
                    if (mode == "tracker") {
                        permissions.add(Manifest.permission.RECORD_AUDIO)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    requestPermissionLauncher.launch(permissions.toTypedArray())
                },
                onStopTracking = {
                    stopService(Intent(this, TrackerService::class.java))
                    stopService(Intent(this, ViewerService::class.java))
                }
            )
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
    }
}
