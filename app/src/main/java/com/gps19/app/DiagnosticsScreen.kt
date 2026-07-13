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

/**
 * DiagnosticsScreen: Detailed health check for system permissions and background stability.
 * v9.3.20:
 * - R405: Samsung Hardening. Unified engine status display and removed A15-specific jitter notes.
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
    onRequestXiaomiPermission: () -> Unit
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
                text = "Device-Specific Adaptations",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )

            if (isXiaomiDevice()) {
                DiagnosticItem(
                    title = "Xiaomi Special Status",
                    status = permissions.xiaomiStatus.name,
                    isOk = permissions.xiaomiStatus == XiaomiPermissionStatus.GRANTED || 
                           (permissions.xiaomiStatus == XiaomiPermissionStatus.UNKNOWN && permissions.isXiaomiManualOverride),
                    icon = Icons.Default.SettingsSuggest,
                    onClick = onRequestXiaomiPermission
                )
                
                if (permissions.xiaomiStatus == XiaomiPermissionStatus.UNKNOWN) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Manual Override", color = Color.LightGray)
                        Switch(
                            checked = permissions.isXiaomiManualOverride,
                            onCheckedChange = { viewModel.onEvent(UiEvent.ToggleXiaomiManualOverride) }
                        )
                    }
                }
            }

            if (isSamsungDevice()) {
                val samsungNote = if (isS21FEDevice()) " (S21 FE Optimized)" else " (Standard Hardening)"
                Text(
                    text = "Samsung Engine Tuning: ACTIVE$samsungNote",
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
