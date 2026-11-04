package com.jd.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Revived System Tools & Diagnostics Overlay (vNov4.32)
 * Accessible directly via the Tools option under the menu settings gear step icon.
 */
@Composable
fun ToolsOverlay(
    onTriggerBurst: () -> Unit,
    onPruneDatabase: () -> Unit,
    onTriggerTestAlarm: () -> Unit,
    onResetWatchdog: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Slate900
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SYSTEM TOOLS & DIAGNOSTICS",
                    color = ViewerCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClose) {
                    Text("CLOSE (X)", color = Color.White)
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Advanced Hardware & Telemetry Control Panel",
                color = Slate500,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(24.dp))

            // Tool Action 1: High-Frequency Telemetry Burst
            ToolActionButton(
                title = "Trigger High-Frequency Burst (100Hz)",
                description = "Forces 100Hz IMU & GNSS sensor sampling for stress evaluation.",
                buttonText = "RUN 100Hz BURST",
                buttonColor = BrandGold,
                onClick = onTriggerBurst
            )

            Spacer(Modifier.height(16.dp))

            // Tool Action 2: Prune Database
            ToolActionButton(
                title = "Prune Database & Reclaim Cache",
                description = "Executes maintenance pruning worker to optimize local storage.",
                buttonText = "PRUNE DATABASE",
                buttonColor = ViewerCyan,
                onClick = onPruneDatabase
            )

            Spacer(Modifier.height(16.dp))

            // Tool Action 3: Trigger Test Alarm
            ToolActionButton(
                title = "Trigger Test Alarm / Violation",
                description = "Simulates a geofence breach or tamper violation event.",
                buttonText = "SIMULATE VIOLATION",
                buttonColor = StatusRed,
                onClick = onTriggerTestAlarm
            )

            Spacer(Modifier.height(16.dp))

            // Tool Action 4: Reset Watchdog
            ToolActionButton(
                title = "Reset Service Watchdog",
                description = "Forces watchdog re-alignment and pulse heartbeat sync.",
                buttonText = "RESET WATCHDOG",
                buttonColor = StatusGreen,
                onClick = onResetWatchdog
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun ToolActionButton(
    title: String,
    description: String,
    buttonText: String,
    buttonColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(description, color = Slate500, fontSize = 11.sp)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
            ) {
                Text(buttonText, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
