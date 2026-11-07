package com.jd.core.ui

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Clean & Phone Setup Overlays for Sensor and Monitor applications.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanSetupOverlay(
    isTracker: Boolean,
    onClearHomePoints: () -> Unit,
    onResetStats: () -> Unit,
    onFullInit: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Slate900
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MAINTENANCE & CLEAN SETUP",
                    color = Rose500,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(Modifier.height(16.dp))
            SetupGroupHeader("SYSTEM MAINTENANCE ACTIONS", Rose500)
            Spacer(Modifier.height(12.dp))

            if (!isTracker) {
                Button(
                    onClick = {
                        onClearHomePoints()
                        onClose()
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Rose500)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Clear Home Points & Geofence", fontSize = 13.sp)
                }
                Spacer(Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    onResetStats()
                    onClose()
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Rose500)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Reset Operational Statistics", fontSize = 13.sp)
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    onFullInit()
                    onClose()
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Rose500.copy(alpha = 0.8f))
            ) {
                Icon(Icons.Default.Warning, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Trigger Full System Re-Initialization", fontSize = 13.sp)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneSetupOverlay(
    isTracker: Boolean,
    homePointsCount: Int,
    onOpenAppInfo: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenOverlayPermission: () -> Unit,
    onOpenExactAlarmPermission: () -> Unit,
    onOpenDndSettings: () -> Unit,
    onOpenFullScreenIntentSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onTestAlarm: () -> Unit,
    onRefreshPermissions: () -> Unit,
    onOpenMap: () -> Unit,
    onClose: () -> Unit
) {
    val manufacturer = remember { Build.MANUFACTURER.uppercase() }
    val model = remember { Build.MODEL.uppercase() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Slate900
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Phone Setup & Capabilities", color = BrandGold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Detected Device: $manufacturer $model", color = Slate500, fontSize = 10.sp)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(Modifier.height(16.dp))

            SetupGuideSection(
                title = "1. Location Permission",
                description = "Required for GNSS tracking and geo-fencing. Set location access to 'Allow all the time' and enable Precise Location.",
                buttonText = "Open App Info",
                onClick = onOpenAppInfo,
                icon = Icons.Default.Info
            )

            Spacer(Modifier.height(12.dp))

            SetupGuideSection(
                title = "2. Battery Optimization Exemption",
                description = "Required to prevent Android OS from putting tracking services to sleep in the background. Select 'Unrestricted'.",
                buttonText = "Open Battery Settings",
                onClick = onOpenBatterySettings,
                icon = Icons.Default.Settings
            )

            Spacer(Modifier.height(12.dp))

            SetupGuideSection(
                title = "3. Overlay Permission ('Appear on Top')",
                description = "Required to display high-priority alarm overlays and sirens when violations occur.",
                buttonText = "Authorize Overlay",
                onClick = onOpenOverlayPermission,
                icon = Icons.Default.Build
            )

            Spacer(Modifier.height(12.dp))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SetupGuideSection(
                    title = "4. Exact Alarms Permission",
                    description = "Required to schedule precise safety pulse and watchdog timers without jitter.",
                    buttonText = "Authorize Exact Alarms",
                    onClick = onOpenExactAlarmPermission,
                    icon = Icons.Default.CheckCircle
                )
                Spacer(Modifier.height(12.dp))
            }

            SetupGuideSection(
                title = "5. Notification Alerts",
                description = "Required to show persistent foreground service status and critical alert notifications.",
                buttonText = "Open App Info",
                onClick = onOpenAppInfo,
                icon = Icons.Default.Notifications
            )

            Spacer(Modifier.height(12.dp))

            SetupGuideSection(
                title = "6. Do Not Disturb (DND) Bypass",
                description = "Required so critical alarms can bypass Do Not Disturb mode and sound sirens when emergencies occur.",
                buttonText = "Grant DND Access",
                onClick = onOpenDndSettings,
                icon = Icons.Default.Star
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Spacer(Modifier.height(12.dp))
                SetupGuideSection(
                    title = "7. Full-Screen Intent Permission",
                    description = "Required to automatically display emergency alarm overlays over locked screens.",
                    buttonText = "Authorize Full-Screen Intent",
                    onClick = onOpenFullScreenIntentSettings,
                    icon = Icons.Default.Lock
                )
            }

            if (!isTracker) {
                Spacer(Modifier.height(12.dp))
                SetupGuideSection(
                    title = "8. Home Points & Geofence Definition",
                    description = "Define secure zones on the map. Current defined home points: $homePointsCount",
                    buttonText = "Open Map",
                    onClick = {
                        onClose()
                        onOpenMap()
                    },
                    icon = Icons.Default.LocationOn
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRefreshPermissions,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ViewerCyan)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Refresh Status", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        onClose()
                        onOpenDiagnostics()
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate500)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Diagnostics", fontSize = 12.sp)
                }
            }

            if (!isTracker) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onTestAlarm,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Test Remote Alarm Overlay", fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
fun SetupGroupHeader(title: String, color: Color) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(title, color = color, fontSize = 14.sp)
        HorizontalDivider(color = color.copy(alpha = 0.3f), thickness = 1.dp)
    }
}

@Composable
fun SetupGuideSection(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = BrandGold, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Text(
            text = description,
            color = Slate500,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.padding(start = 28.dp, top = 2.dp)
        )
        Spacer(Modifier.height(6.dp))
        Button(
            onClick = onClick,
            modifier = Modifier.padding(start = 28.dp).heightIn(min = 32.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) {
            Text(buttonText, fontSize = 11.sp)
        }
    }
}
