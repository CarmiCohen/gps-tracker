package com.gps19.app.sensor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gps19.core.ui.RevolutionTheme
import com.gps19.core.ui.StatusGreen
import com.gps19.core.ui.StatusRed

/**
 * AdminActivity: The "Admin Console" for the Sensor.
 * Corrected: Implements Stealth Mode and Muzzled Banner (Points 4 & 8).
 */
class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RevolutionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isLocked by remember { mutableStateOf(true) }
                    var isMuzzled by remember { mutableStateOf(false) } // Should be backed by DataStore

                    if (isLocked) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { isLocked = false }, // Triple-tap logic in real impl
                            contentAlignment = Alignment.Center
                        ) {
                            Text("System Idle", color = Color.Gray.copy(alpha = 0.2f))
                        }
                    } else {
                        SensorAdminConsole(
                            isMuzzled = isMuzzled,
                            onIgnoreBatteryOptimization = { requestIgnoreBatteryOptimization() }
                        )
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
fun SensorAdminConsole(isMuzzled: Boolean, onIgnoreBatteryOptimization: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        if (isMuzzled) {
            // Point 8: High-priority Muzzled Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = StatusRed),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("SYSTEM MUZZLED", fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Safe-start triggered after crash loop. Factory reset required.", fontSize = 12.sp, color = Color.White)
                    Button(onClick = { /* TODO: Factory Reset */ }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = StatusRed)) {
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
                item { Text("09:41:02 - Sensing Engine Started", fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace) }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { /* TODO */ }, modifier = Modifier.weight(1f)) { Text("ZERO CHAIR") }
            Button(onClick = { /* TODO */ }, modifier = Modifier.weight(1f)) { Text("RESET VIBE") }
        }
    }
}

@Composable
fun StatusIndicator(label: String, isActive: Boolean) {
    Row {
        Box(modifier = Modifier.size(12.dp).background(if (isActive) StatusGreen else Color.Gray))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
