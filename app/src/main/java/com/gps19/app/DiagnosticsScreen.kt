package com.gps19.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gps19.core.engine.CapabilityStatus
import com.gps19.core.engine.PerformanceTier
import kotlinx.coroutines.flow.StateFlow

/**
 * DiagnosticsScreen: Detailed health check for system permissions and background stability.
 * Oct.6.8:
 * - Issue #AUDIT-1006-8: Signaling Efficiency Audit. Added signaling metrics section 
 *   to expose real-time conflation efficiency and radio emission counts (Rule 1.123).
 * Oct.6.2:
 * - Issue #AUDIT-1006-5: Added TRIGGER LOG PRESSURE TEST button to simulate 
 *   high-frequency telemetry bursts.
 * Oct.5.20:
 * - SIMP-1426-3: Refactored DiagnosticsScreen to consume unified UiStateProvider. 
 *   Reduced parameter overhead and unified state collection. (R1426-3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    stateProvider: UiStateProvider,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onToggleManualOverride: () -> Unit,
    onToggleForensicSimulation: (Boolean) -> Unit,
    onToggleStorageSimulation: (Boolean, Boolean) -> Unit,
    onToggleSetupBypass: (Boolean) -> Unit = {},
    onExecuteStressTest: () -> Unit = {},
    onExecuteLogPressureTest: () -> Unit = {},
    onRequestBatteryExemption: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestAppInfo: () -> Unit,
    onRequestAppInfoForStaggered: () -> Unit = onRequestAppInfo,
    onRequestExactAlarm: () -> Unit,
    onRequestHardwarePermission: () -> Unit
) {
    val sessionState by stateProvider.session.collectAsStateWithLifecycle()
    val diagnosticState by stateProvider.diagnostic.collectAsStateWithLifecycle()
    val simulationState by stateProvider.simulation.collectAsStateWithLifecycle()

    val permissions = sessionState.permissions
    val isSetupBypassActive = sessionState.isSetupBypassActive
    val recoveryCount = diagnosticState.recoveryCount
    val cumulativeRecoveryBlackoutMs = diagnosticState.cumulativeRecoveryBlackoutMs
    val isForensicStallSimulated = simulationState.isForensicStallSimulated
    val isStorageSimulated = simulationState.isStorageSimulated
    val isStorageCriticalSimulated = simulationState.isStorageCriticalSimulated
    val isGnssThrottled = diagnosticState.isGnssThrottled
    val signaling = diagnosticState.signalingMetrics

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("System Diagnostics", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Background Resilience Health",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )

            DiagnosticItem(
                title = "Battery Optimization",
                status = if (permissions.isBatteryWhitelisted) "UNRESTRICTED" else "OPTIMIZED",
                isOk = permissions.isBatteryWhitelisted,
                icon = Icons.Default.BatteryChargingFull,
                onClick = onRequestBatteryExemption
            )

            DiagnosticItem(
                title = "Overlay Permission",
                status = if (permissions.isOverlayGranted) "GRANTED" else "DENIED",
                isOk = permissions.isOverlayGranted,
                icon = Icons.Default.Layers,
                onClick = onRequestOverlayPermission
            )

            DiagnosticItem(
                title = "Exact Alarm",
                status = if (permissions.isOverlayGranted) "GRANTED" else "DENIED", // Legacy field check
                isOk = permissions.isExactAlarmGranted,
                icon = Icons.Default.Alarm,
                onClick = onRequestExactAlarm
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Signaling Efficiency Audit",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )

            val savings = if (signaling.received > 0) {
                ((signaling.conflated.toDouble() / signaling.received.toDouble()) * 100).toInt()
            } else 0

            DiagnosticItem(
                title = "Conflation Efficiency",
                status = "$savings% Savings (${signaling.conflated} frames)",
                isOk = savings > 0 || signaling.received < 10,
                icon = Icons.Default.Compress,
                onClick = {}
            )

            DiagnosticItem(
                title = "Radio Emission Ratio",
                status = "${signaling.emitted} emits / ${signaling.received} calls",
                isOk = signaling.emitted <= signaling.received,
                icon = Icons.Default.WifiTethering,
                onClick = {}
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Forensic Recovery Audit",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )

            val avgBlackout = if (recoveryCount > 0) {
                cumulativeRecoveryBlackoutMs / recoveryCount
            } else 0L

            DiagnosticItem(
                title = "Total Recovery Events",
                status = "$recoveryCount",
                isOk = true,
                icon = Icons.Default.History,
                onClick = {}
            )

            DiagnosticItem(
                title = "Average Blackout Duration",
                status = "${avgBlackout}ms",
                isOk = avgBlackout < 30000L,
                icon = Icons.Default.Timer,
                onClick = {}
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Hardware Capabilities",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )

            if (permissions.performanceTier == PerformanceTier.STAGGERED) {
                DiagnosticItem(
                    title = "GNSS Sampling Status",
                    status = if (isGnssThrottled) "THROTTLED (5s)" else "STANDARD (2s)",
                    isOk = !isGnssThrottled,
                    icon = Icons.Default.Speed,
                    onClick = onRequestAppInfoForStaggered
                )
            }

            if (permissions.hasBackgroundRestriction) {
                DiagnosticItem(
                    title = "Background Service Lock",
                    status = permissions.backgroundStatus.name,
                    isOk = permissions.backgroundStatus == CapabilityStatus.GRANTED || 
                           (permissions.backgroundStatus == CapabilityStatus.UNKNOWN && permissions.isManualOverride),
                    icon = Icons.Default.SettingsSuggest,
                    onClick = onRequestHardwarePermission
                )
                
                if (permissions.backgroundStatus == CapabilityStatus.UNKNOWN) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Manual Override", color = Color.LightGray)
                        Switch(
                            checked = permissions.isManualOverride,
                            onCheckedChange = { onToggleManualOverride() }
                        )
                    }
                }
            } else {
                Text(text = "Standard background policy detected.", color = Color.Gray, fontSize = 12.sp)
            }

            if (permissions.requiresWakeLockRenewal) {
                Text(text = "Hardware Tuning: WAKELOCK_RENEWAL active", color = Color.Cyan, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Validation Hooks (Temporary)",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = onExecuteStressTest,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF00FF)) // ForensicPink
            ) {
                Icon(Icons.Default.Speed, null)
                Spacer(Modifier.width(8.dp))
                Text("TRIGGER FORENSIC STRESS TEST", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onExecuteLogPressureTest,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Icon(Icons.Default.List, null)
                Spacer(Modifier.width(8.dp))
                Text("TRIGGER LOG PRESSURE TEST (100Hz)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A), shape = MaterialTheme.shapes.small).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Setup Overlay Bypass", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Skips permission check for soak tests", color = if (isSetupBypassActive) Color.Green else Color.Gray, fontSize = 12.sp)
                }
                Switch(checked = isSetupBypassActive, onCheckedChange = { onToggleSetupBypass(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color.Green, checkedTrackColor = Color.Green.copy(alpha = 0.5f)))
            }

            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A), shape = MaterialTheme.shapes.small).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Forensic Stall Simulation", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Simulates Urban Multipath / IO Latency", color = if (isForensicStallSimulated) Color.Yellow else Color.Gray, fontSize = 12.sp)
                }
                Switch(checked = isForensicStallSimulated, onCheckedChange = { onToggleForensicSimulation(it) })
            }

            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A), shape = MaterialTheme.shapes.small).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Storage Pressure Simulation", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(if (isStorageSimulated) (if (isStorageCriticalSimulated) "CRITICAL (99% full)" else "LOW (95% full)") else "Simulate Disk Exhaustion", color = if (isStorageSimulated) Color.Yellow else Color.Gray, fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isStorageSimulated) {
                        Text("CRIT", color = if (isStorageCriticalSimulated) Color.Red else Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(end = 4.dp))
                        Checkbox(checked = isStorageCriticalSimulated, onCheckedChange = { onToggleStorageSimulation(true, it) })
                    }
                    Switch(checked = isStorageSimulated, onCheckedChange = { onToggleStorageSimulation(it, isStorageCriticalSimulated) })
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Core Permissions",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )

            DiagnosticItem(title = "Background Location", status = if (permissions.isBackgroundLocationGranted) "GRANTED" else "DENIED", isOk = permissions.isBackgroundLocationGranted, icon = Icons.Default.LocationOn, onClick = onRequestAppInfo)
            DiagnosticItem(title = "Microphone (Acoustic)", status = if (permissions.isMicrophoneGranted) "GRANTED" else "DENIED", isOk = permissions.isMicrophoneGranted, icon = Icons.Default.Mic, onClick = onRequestAppInfo)
            DiagnosticItem(title = "Notifications", status = if (permissions.isPostNotificationsGranted) "GRANTED" else "DENIED", isOk = permissions.isPostNotificationsGranted, icon = Icons.Default.Notifications, onClick = onRequestAppInfo)

            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text("REFRESH STATUS") }
        }
    }
}

@Composable
fun DiagnosticItem(title: String, status: String, isOk: Boolean, icon: ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = if (isOk) Color.Green else Color.Red, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column { Text(text = title, color = Color.White, fontWeight = FontWeight.SemiBold); Text(text = status, color = if (isOk) Color.Green else Color.Red, fontSize = 12.sp) }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}
