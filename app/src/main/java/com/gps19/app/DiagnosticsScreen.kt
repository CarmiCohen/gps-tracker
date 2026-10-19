package com.gps19.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
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
 * Oct.19.19:
 * - ISSUE-OCT19.19-F1: Added "Skip Mode Restoration" toggle to bypass R-ID 242.
 * Oct.18.6:
 * - ISSUE-OCT18.6-F1: Synthetic Physical Event Injection. Added UI controls for 
 *   SHAKE, GNSS_LOSS, POWER_TAMPER, and NETWORK_LOSS simulations.
 * Oct.18.25:
 * - Added Virtual Peer Presence simulation toggle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    stateProvider: UiStateProvider,
    onMainEvent: (UiEvent) -> Unit
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
                    IconButton(onClick = { onMainEvent(UiEvent.NavigateToDiagnostics(false)) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
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
                onClick = { onMainEvent(UiEvent.RequestBatteryExemption) }
            )

            DiagnosticItem(
                title = "Overlay Permission",
                status = if (permissions.isOverlayGranted) "GRANTED" else "DENIED",
                isOk = permissions.isOverlayGranted,
                icon = Icons.Default.Layers,
                onClick = { onMainEvent(UiEvent.RequestOverlayPermission) }
            )

            DiagnosticItem(
                title = "Exact Alarm",
                status = if (permissions.isExactAlarmGranted) "GRANTED" else "DENIED",
                isOk = permissions.isExactAlarmGranted,
                icon = Icons.Default.Alarm,
                onClick = { onMainEvent(UiEvent.RequestExactAlarm) }
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
                    onClick = { onMainEvent(UiEvent.RequestAppInfo) }
                )
            }

            if (permissions.hasBackgroundRestriction) {
                DiagnosticItem(
                    title = "Background Service Lock",
                    status = permissions.backgroundStatus.name,
                    isOk = permissions.backgroundStatus == CapabilityStatus.GRANTED || 
                           (permissions.backgroundStatus == CapabilityStatus.UNKNOWN && permissions.isManualOverride),
                    icon = Icons.Default.SettingsSuggest,
                    onClick = { onMainEvent(UiEvent.RequestHardwarePermission) }
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
                            onCheckedChange = { onMainEvent(UiEvent.ToggleXiaomiManualOverride) }
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
                onClick = { onMainEvent(UiEvent.ExecuteStressTest) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF00FF)) // ForensicPink
            ) {
                Icon(Icons.Default.Speed, null)
                Spacer(Modifier.width(8.dp))
                Text("TRIGGER FORENSIC STRESS TEST", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onMainEvent(UiEvent.ExecuteLogPressureTest) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Icon(Icons.AutoMirrored.Filled.List, null)
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
                    Text("Skip Mode Restoration", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Bypasses R-ID 242 on cold start", color = if (simulationState.isRestorationDisabled) Color.Green else Color.Gray, fontSize = 12.sp)
                }
                Switch(checked = simulationState.isRestorationDisabled, onCheckedChange = { onMainEvent(UiEvent.SetRestorationDisabled(it)) }, colors = SwitchDefaults.colors(checkedThumbColor = Color.Green, checkedTrackColor = Color.Green.copy(alpha = 0.5f)))
            }

            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A), shape = MaterialTheme.shapes.small).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Setup Overlay Bypass", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Skips permission check for soak tests", color = if (isSetupBypassActive) Color.Green else Color.Gray, fontSize = 12.sp)
                }
                Switch(checked = isSetupBypassActive, onCheckedChange = { onMainEvent(UiEvent.ToggleSetupBypass(it)) }, colors = SwitchDefaults.colors(checkedThumbColor = Color.Green, checkedTrackColor = Color.Green.copy(alpha = 0.5f)))
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
                Switch(checked = isForensicStallSimulated, onCheckedChange = { onMainEvent(UiEvent.SetForensicSimulation(it)) })
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
                        Checkbox(checked = isStorageCriticalSimulated, onCheckedChange = { onMainEvent(UiEvent.SetStorageSimulation(true, it)) })
                    }
                    Switch(checked = isStorageSimulated, onCheckedChange = { onMainEvent(UiEvent.SetStorageSimulation(it, isStorageCriticalSimulated)) })
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A), shape = MaterialTheme.shapes.small).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Physical Shake Simulation", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Chapter 3.2 Stealth Audit", color = if (simulationState.isShakeSimulated) Color.Yellow else Color.Gray, fontSize = 12.sp)
                }
                Switch(checked = simulationState.isShakeSimulated, onCheckedChange = { onMainEvent(UiEvent.SetShakeSimulation(it)) })
            }

            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A), shape = MaterialTheme.shapes.small).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("GNSS Signal Loss Simulation", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Chapter 5.1 GNSS Zombie", color = if (simulationState.isGnssLossSimulated) Color.Yellow else Color.Gray, fontSize = 12.sp)
                }
                Switch(checked = simulationState.isGnssLossSimulated, onCheckedChange = { onMainEvent(UiEvent.SetGnssLossSimulation(it)) })
            }

            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A), shape = MaterialTheme.shapes.small).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Power Tamper Simulation", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Chapter 15.1 Power Tamper", color = if (simulationState.isPowerTamperSimulated) Color.Yellow else Color.Gray, fontSize = 12.sp)
                }
                Switch(checked = simulationState.isPowerTamperSimulated, onCheckedChange = { onMainEvent(UiEvent.SetPowerTamperSimulation(it)) })
            }

            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A), shape = MaterialTheme.shapes.small).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Network Loss Simulation", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Chapter 16.1 Failover Audit", color = if (simulationState.isNetworkLossSimulated) Color.Yellow else Color.Gray, fontSize = 12.sp)
                }
                Switch(checked = simulationState.isNetworkLossSimulated, onCheckedChange = { onMainEvent(UiEvent.SetNetworkLossSimulation(it)) })
            }

            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A), shape = MaterialTheme.shapes.small).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Virtual Peer Presence", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Simulates ${if (sessionState.appMode == "tracker") "Viewer" else "Tracker"}", color = if (simulationState.isPeerPresenceSimulated) Color.Green else Color.Gray, fontSize = 12.sp)
                }
                Switch(checked = simulationState.isPeerPresenceSimulated, onCheckedChange = { onMainEvent(UiEvent.SetPeerPresenceSimulation(it)) })
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Core Permissions",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )

            DiagnosticItem(title = "Background Location", status = if (permissions.isBackgroundLocationGranted) "GRANTED" else "DENIED", isOk = permissions.isBackgroundLocationGranted, icon = Icons.Default.LocationOn, onClick = { onMainEvent(UiEvent.RequestAppInfo) })
            DiagnosticItem(title = "Microphone (Acoustic)", status = if (permissions.isMicrophoneGranted) "GRANTED" else "DENIED", isOk = permissions.isMicrophoneGranted, icon = Icons.Default.Mic, onClick = { onMainEvent(UiEvent.RequestAppInfo) })
            DiagnosticItem(title = "Notifications", status = if (permissions.isPostNotificationsGranted) "GRANTED" else "DENIED", isOk = permissions.isPostNotificationsGranted, icon = Icons.Default.Notifications, onClick = { onMainEvent(UiEvent.RequestAppInfo) })

            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { onMainEvent(UiEvent.RefreshPermissionStatus) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text("REFRESH STATUS") }
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
