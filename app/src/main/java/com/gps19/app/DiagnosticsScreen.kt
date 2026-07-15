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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gps19.core.engine.CapabilityStatus

/**
 * DiagnosticsScreen: Detailed health check for system permissions and background stability.
 * v9.4.0:
 * - Issue #502: Device Independency. Replaced vendor-specific checks with capability logic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestAppInfo: () -> Unit,
    onRequestExactAlarm: () -> Unit,
    onRequestHardwarePermission: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions = uiState.permissions

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

            // Critical Permissions
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
                status = if (permissions.isExactAlarmGranted) "GRANTED" else "DENIED",
                isOk = permissions.isExactAlarmGranted,
                icon = Icons.Default.Alarm,
                onClick = onRequestExactAlarm
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Hardware Capabilities",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )

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
                            onCheckedChange = { viewModel.onEvent(UiEvent.ToggleXiaomiManualOverride) }
                        )
                    }
                }
            } else {
                Text(
                    text = "Standard background policy detected.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            if (permissions.requiresWakeLockRenewal) {
                Text(
                    text = "Hardware Tuning: WAKELOCK_RENEWAL active",
                    color = Color.Cyan,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Core Permissions",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )

            DiagnosticItem(
                title = "Background Location",
                status = if (permissions.isBackgroundLocationGranted) "GRANTED" else "DENIED",
                isOk = permissions.isBackgroundLocationGranted,
                icon = Icons.Default.LocationOn,
                onClick = onRequestAppInfo
            )

            DiagnosticItem(
                title = "Microphone (Acoustic)",
                status = if (permissions.isMicrophoneGranted) "GRANTED" else "DENIED",
                isOk = permissions.isMicrophoneGranted,
                icon = Icons.Default.Mic,
                onClick = onRequestAppInfo
            )

            DiagnosticItem(
                title = "Notifications",
                status = if (permissions.isPostNotificationsGranted) "GRANTED" else "DENIED",
                isOk = permissions.isPostNotificationsGranted,
                icon = Icons.Default.Notifications,
                onClick = onRequestAppInfo
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { viewModel.onEvent(UiEvent.RefreshPermissionStatus) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text("REFRESH STATUS")
            }
        }
    }
}

@Composable
fun DiagnosticItem(
    title: String,
    status: String,
    isOk: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isOk) Color.Green else Color.Red,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(
                    text = status,
                    color = if (isOk) Color.Green else Color.Red,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}
