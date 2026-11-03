package com.jd.app.sensor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jd.core.ui.RevolutionTheme
import com.jd.core.ui.StatusGreen
import com.jd.core.ui.StatusRed
import com.jd.core.shared.SystemLogger
import com.jd.core.shared.ForensicIdentity
import com.jd.core.network.ForensicIdentityManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AdminActivity: The "Admin Console" for the Sensor.
 * Hardened for Revol37.3: Restored mandatory Settings Gear (R797) and Factory Defaults (R-ID 615).
 */
@AndroidEntryPoint
class AdminActivity : ComponentActivity() {

    @Inject
    lateinit var identityManager: ForensicIdentityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RevolutionTheme {
                val trackerId by identityManager.selfId.collectAsStateWithLifecycle(initialValue = "LOADING...")
                val logs by SystemLogger.logs.collectAsStateWithLifecycle()
                val scope = rememberCoroutineScope()
                
                LaunchedEffect(Unit) {
                    identityManager.ensureIdentityGenerated(ForensicIdentity.PREFIX_SENSOR)
                    while(true) {
                        SystemLogger.updateUiSnapshot()
                        delay(1000)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isLocked by remember { mutableStateOf(true) }
                    var showSettings by remember { mutableStateOf(false) }

                    if (isLocked) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { isLocked = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("System Idle", color = Color.Gray.copy(alpha = 0.2f))
                        }
                    } else {
                        PermissionGuard {
                            if (showSettings) {
                                SettingsOverlay(
                                    onClose = { showSettings = false },
                                    onLoadDefaults = { 
                                        scope.launch {
                                            identityManager.resetIdentity(ForensicIdentity.PREFIX_SENSOR)
                                        }
                                    },
                                    onSetCustomId = { customId ->
                                        scope.launch {
                                            identityManager.setSelfId(customId)
                                        }
                                    }
                                )
                            } else {
                                SensorAdminConsole(
                                    trackerId = trackerId,
                                    logs = logs,
                                    onSettingsClick = { showSettings = true },
                                    onIgnoreBatteryOptimization = { requestIgnoreBatteryOptimization() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestIgnoreBatteryOptimization() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }
}

@Composable
fun PermissionGuard(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val requiredPermissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_NETWORK_STATE
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        }
    }

    var allGranted by remember {
        mutableStateOf(requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        allGranted = permissions.values.all { it }
    }

    if (!allGranted) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Permissions Required", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = { launcher.launch(requiredPermissions.toTypedArray()) }) {
                Text("Grant Permissions")
            }
        }
    } else {
        content()
    }
}

fun getAppVersionName(context: android.content.Context): String {
    return try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        pInfo.versionName ?: "Nov3.21_PLATINUM"
    } catch (e: Exception) {
        "Nov3.21_PLATINUM"
    }
}

@Composable
fun SettingsOverlay(onClose: () -> Unit, onLoadDefaults: () -> Unit, onSetCustomId: (String) -> Unit) {
    var customId by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("CONFIGURATION", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onClose) { Text("CLOSE (X)") }
        }
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Manual Device ID Setup", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = customId,
            onValueChange = { 
                customId = it
                isError = it.isNotEmpty() && !ForensicIdentity.isValid(it)
            },
            label = { Text("Enter Custom ID (Any non-blank chars)") },
            isError = isError,
            supportingText = {
                if (isError) {
                    Text("Device ID cannot be blank")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                if (!isError && customId.isNotBlank()) {
                    onSetCustomId(customId)
                    onClose()
                }
            },
            enabled = customId.isNotBlank() && !isError,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("APPLY CUSTOM ID")
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { 
                onLoadDefaults()
                onClose()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
        ) {
            Text("LOAD FACTORY DEFAULTS")
        }
    }
}

@Composable
fun SensorAdminConsole(
    trackerId: String, 
    logs: List<SystemLogger.LogEntry>, 
    onSettingsClick: () -> Unit,
    onIgnoreBatteryOptimization: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    Column(modifier = Modifier.padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "SENSOR ADMIN",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.Gray)
            }
        }
        
        Text(text = "ID: ${trackerId.take(12)}...", fontSize = 12.sp, color = Color.Gray)
        Text(text = "Version: ${getAppVersionName(context)}", fontSize = 12.sp, color = Color.Gray)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = onIgnoreBatteryOptimization, modifier = Modifier.fillMaxWidth()) {
            Text("WHITELIST BATTERY")
        }

        Button(
            onClick = {
                val intent = Intent(context, SensorService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("START SENSOR SERVICE")
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Text(text = "LIVE EVENT LOG", style = MaterialTheme.typography.labelLarge)
        
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.05f))
        ) {
            LazyColumn(state = listState) {
                items(logs) { entry ->
                    Text(
                        text = entry.toString(),
                        fontSize = 10.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }
    }
}
