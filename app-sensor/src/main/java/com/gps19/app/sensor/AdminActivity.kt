package com.gps19.app.sensor

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
import com.gps19.core.ui.RevolutionTheme
import com.gps19.core.ui.StatusGreen
import com.gps19.core.ui.StatusRed
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * AdminActivity: The "Admin Console" for the Sensor.
 * Hardened for API 34: Robust permission handling and FGS lifecycle management.
 */
@AndroidEntryPoint
class AdminActivity : ComponentActivity() {

    @Inject
    lateinit var dataStore: SensorDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RevolutionTheme {
                val trackerId by dataStore.trackerId.collectAsStateWithLifecycle(initialValue = "LOADING...")
                
                LaunchedEffect(Unit) {
                    dataStore.ensureIdentityGenerated()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isLocked by remember { mutableStateOf(true) }
                    var isMuzzled by remember { mutableStateOf(false) }

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
                            SensorAdminConsole(
                                trackerId = trackerId,
                                isMuzzled = isMuzzled,
                                onIgnoreBatteryOptimization = { requestIgnoreBatteryOptimization() }
                            )
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
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
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
            Text(
                "This app needs location and notification access to function as a reliable tracker.",
                modifier = Modifier.padding(vertical = 16.dp)
            )
            Button(onClick = {
                launcher.launch(requiredPermissions.toTypedArray())
            }) {
                Text("Grant Permissions")
            }
        }
    } else {
        content()
    }
}

@Composable
fun SensorAdminConsole(trackerId: String, isMuzzled: Boolean, onIgnoreBatteryOptimization: () -> Unit) {
    val context = LocalContext.current
    
    Column(modifier = Modifier.padding(16.dp)) {
        if (isMuzzled) {
            Card(
                colors = CardDefaults.cardColors(containerColor = StatusRed),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("SYSTEM MUZZLED", fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Safe-start triggered after crash loop. Factory reset required.", fontSize = 12.sp, color = Color.White)
                    Button(onClick = { /* TODO */ }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = StatusRed)) {
                        Text("RESET TO FACTORY")
                    }
                }
            }
        }

        Text(
            text = "SENSOR ADMIN CONSOLE",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(text = "ID: $trackerId", fontSize = 12.sp, color = Color.Gray)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatusIndicator("GPS", !isMuzzled)
            StatusIndicator("RELAY", true)
            StatusIndicator("POWER", true)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = onIgnoreBatteryOptimization, modifier = Modifier.fillMaxWidth()) {
            Text("WHITELIST BATTERY SAVER")
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
                .background(Color.Black.copy(alpha = 0.1f))
                .padding(4.dp)
        ) {
            LazyColumn {
                item { Text("09:41:02 - Sensing Engine Ready", fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace) }
            }
        }
    }
}

@Composable
fun StatusIndicator(label: String, isActive: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).background(if (isActive) StatusGreen else Color.Gray))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
