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
import com.jd.core.shared.AlertSettings

/**
 * Revived Alarming & Alert Setup Overlays (R747 Locality Authority & R872 Stealth)
 */

/** Shown on the Monitor instead of the tilt slider: the Sensor detects tilt with its own (ISSUE-NOV5.2-O11 Group 5). */
const val TILT_SENSITIVITY_ON_SENSOR_NOTE = "The tilt angle is set on the Sensor (Tilt Sensitivity Threshold)"

/** Shown on the Monitor instead of the vibration slider: the Sensor detects vibration with its own (ISSUE-NOV5.2-O11 Group 7). */
const val VIBRATION_SENSITIVITY_ON_SENSOR_NOTE = "The vibration limit is set on the Sensor (Vibration Sensitivity Threshold)"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertManagementOverlay(
    isTracker: Boolean,
    draftAlertSettings: AlertSettings,
    onUpdateAlertSettings: (AlertSettings) -> Unit,
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
                    text = "ALERTS & SENSORS SETUP",
                    color = ViewerCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClose) {
                    Text("CLOSE (X)", color = Color.White)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Quick Batch Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("BULK CONTROLS", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onUpdateAlertSettings(
                                draftAlertSettings.copy(
                                    localInternet = true, serverConnection = true, relayConnection = true,
                                    jammerDetection = true, signalLoss = true, gpsStalling = true,
                                    distance = true, power = true, lowBattery = true, batteryHealth = true,
                                    highTemperature = true, longTimeGap = true, tamperAlert = true,
                                    tiltAlert = true, acousticAlert = true, liftAlert = true,
                                    systemStorageLow = true
                                )
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("ALL ON", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            onUpdateAlertSettings(
                                draftAlertSettings.copy(
                                    localInternet = false, serverConnection = false, relayConnection = false,
                                    jammerDetection = false, signalLoss = false, gpsStalling = false,
                                    distance = false, power = false, lowBattery = false, batteryHealth = false,
                                    highTemperature = false, longTimeGap = false, tamperAlert = false,
                                    tiltAlert = false, acousticAlert = false, liftAlert = false,
                                    systemStorageLow = false
                                )
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("RESET ALL", fontSize = 11.sp, color = StatusRed)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Master Global Mute Section
            Text("MASTER CONTROLS", color = BrandGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            AlarmToggle("Global Mute (Suppress All Alarms)", draftAlertSettings.globalMute) {
                onUpdateAlertSettings(draftAlertSettings.copy(globalMute = it))
            }
            Text(
                "When enabled, all local and remote alert triggers are temporarily muted.",
                color = Slate500, fontSize = 10.sp, modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
            )

            Spacer(Modifier.height(12.dp))

            // Section 1: Communication & Link
            Text("COMMUNICATION & LINK ALARMS", color = StatusRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            AlarmToggle("Local Internet Offline", draftAlertSettings.localInternet) {
                onUpdateAlertSettings(draftAlertSettings.copy(localInternet = it))
            }
            AlarmToggle("Relay Server Disconnected", draftAlertSettings.relayConnection) {
                onUpdateAlertSettings(draftAlertSettings.copy(relayConnection = it))
            }
            AlarmToggle("Peer Connection Offline", draftAlertSettings.serverConnection) {
                onUpdateAlertSettings(draftAlertSettings.copy(serverConnection = it))
            }
            AlarmToggle("GNSS Signal Loss", draftAlertSettings.signalLoss) {
                onUpdateAlertSettings(draftAlertSettings.copy(signalLoss = it))
            }
            AlarmToggle("Jammer / GPS Jump Detection", draftAlertSettings.jammerDetection) {
                onUpdateAlertSettings(draftAlertSettings.copy(jammerDetection = it))
            }

            Spacer(Modifier.height(12.dp))

            // Section 2: Location & Geofence
            Text("LOCATION & GEOFENCE ALARMS", color = ViewerCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            AlarmToggle("Geofence Boundary Breach", draftAlertSettings.distance) {
                onUpdateAlertSettings(draftAlertSettings.copy(distance = it))
            }
            AlarmToggle("GPS Telemetry Stall", draftAlertSettings.gpsStalling) {
                onUpdateAlertSettings(draftAlertSettings.copy(gpsStalling = it))
            }
            AlarmToggle("Long Telemetry Time Gap", draftAlertSettings.longTimeGap) {
                onUpdateAlertSettings(draftAlertSettings.copy(longTimeGap = it))
            }

            Spacer(Modifier.height(12.dp))

            // Section 3: Power & Device Health
            Text("POWER & DEVICE HEALTH ALARMS", color = BrandGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            AlarmToggle("Power Disconnected / Unplugged", draftAlertSettings.power) {
                onUpdateAlertSettings(draftAlertSettings.copy(power = it))
            }
            AlarmToggle("Low Battery Level (<20%)", draftAlertSettings.lowBattery) {
                onUpdateAlertSettings(draftAlertSettings.copy(lowBattery = it))
            }
            AlarmToggle("Steep Battery Discharge", draftAlertSettings.batteryHealth) {
                onUpdateAlertSettings(draftAlertSettings.copy(batteryHealth = it))
            }
            AlarmToggle("High Device Temperature (>46°C)", draftAlertSettings.highTemperature) {
                onUpdateAlertSettings(draftAlertSettings.copy(highTemperature = it))
            }

            Spacer(Modifier.height(12.dp))

            // Section 4: System Storage & Integrity
            Text("SYSTEM INTEGRITY ALARMS", color = StatusOrange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            AlarmToggle("System Storage Space Low", draftAlertSettings.systemStorageLow) {
                onUpdateAlertSettings(draftAlertSettings.copy(systemStorageLow = it))
            }

            Spacer(Modifier.height(12.dp))

            // Section 5: Sentinel Hardware Sensors
            Text("SENTINEL HARDWARE MOTION SENSORS", color = StatusGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            AlarmToggle("Tamper / Vibration Sensor", draftAlertSettings.tamperAlert) {
                onUpdateAlertSettings(draftAlertSettings.copy(tamperAlert = it))
            }
            // Like tilt, the Sensor detects vibration with its own slider; the Monitor's switch only gates the alarm.
            if (isTracker) {
                SensitivitySlider(
                    label = "Vibration Sensitivity Threshold",
                    value = draftAlertSettings.vibrationSensitivity,
                    onValueChange = { onUpdateAlertSettings(draftAlertSettings.copy(vibrationSensitivity = it)) }
                )
            } else if (draftAlertSettings.tamperAlert) {
                Text(
                    VIBRATION_SENSITIVITY_ON_SENSOR_NOTE,
                    color = Slate500, fontSize = 10.sp, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                )
            }

            AlarmToggle("Tilt Angle Detection Sensor", draftAlertSettings.tiltAlert) {
                onUpdateAlertSettings(draftAlertSettings.copy(tiltAlert = it))
            }
            // The Sensor detects tilt with its own slider (user decision, 2026-11-09); the Monitor's switch only gates the alarm.
            if (isTracker) {
                SensitivitySlider(
                    label = "Tilt Sensitivity Threshold",
                    value = draftAlertSettings.tiltSensitivity,
                    onValueChange = { onUpdateAlertSettings(draftAlertSettings.copy(tiltSensitivity = it)) }
                )
            } else if (draftAlertSettings.tiltAlert) {
                Text(
                    TILT_SENSITIVITY_ON_SENSOR_NOTE,
                    color = Slate500, fontSize = 10.sp, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                )
            }

            AlarmToggle("Acoustic / Microphone Noise Sensor", draftAlertSettings.acousticAlert) {
                onUpdateAlertSettings(draftAlertSettings.copy(acousticAlert = it))
            }

            AlarmToggle("Lift / Height Change Sensor", draftAlertSettings.liftAlert) {
                onUpdateAlertSettings(draftAlertSettings.copy(liftAlert = it))
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSoundOverlay(
    isTracker: Boolean,
    draftAlertSettings: AlertSettings,
    selectedSirenType: String,
    isSirenPlaying: Boolean,
    onUpdateAlertSettings: (AlertSettings) -> Unit,
    onUpdateSirenType: (String) -> Unit,
    onUpdateAlarmVolume: (Float) -> Unit,
    onTestSiren: () -> Unit,
    onClose: () -> Unit
) {
    val sirenOptions = listOf("Siren", "Chimes", "Pulse")
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
                Text("SIREN & AUDIO SETUP", color = StatusOrange, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = onClose) {
                    Text("CLOSE (X)", color = Color.White)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Test Siren Audio Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Test Audio Output", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                if (isTracker) {
                    Text("Disabled (Stealth Tracker)", color = Slate500, fontSize = 13.sp)
                } else {
                    Button(
                        onClick = onTestSiren,
                        colors = ButtonDefaults.buttonColors(containerColor = if (isSirenPlaying) StatusRed else StatusGreen)
                    ) {
                        Text(if (isSirenPlaying) "STOP SIREN TEST" else "TEST SIREN AUDIO")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Siren Type Choice Chips
            Text("SIREN SOUND WAVE TYPE", color = BrandGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                sirenOptions.forEach { type ->
                    FilterChip(
                        selected = selectedSirenType == type,
                        onClick = { onUpdateSirenType(type) },
                        label = { Text(type) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Audio Behaviors
            Text("AUDIO & SIREN BEHAVIORS", color = ViewerCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            AlarmToggle("Enable Device Haptic Vibration", draftAlertSettings.vibrationEnabled) {
                onUpdateAlertSettings(draftAlertSettings.copy(vibrationEnabled = it))
            }
            AlarmToggle("Override System Do Not Disturb / Silent Mode", draftAlertSettings.overrideSilence) {
                onUpdateAlertSettings(draftAlertSettings.copy(overrideSilence = it))
            }
            AlarmToggle("Force Maximum Hardware Audio Volume", draftAlertSettings.useMaxVolume) {
                onUpdateAlertSettings(draftAlertSettings.copy(overrideSilence = true, useMaxVolume = it))
            }

            Spacer(Modifier.height(20.dp))

            // Volume Mode & Custom Slider
            Text("VOLUME CONTROL MODE", color = ViewerCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                FilterChip(
                    selected = !draftAlertSettings.useCustomVolume,
                    onClick = { onUpdateAlertSettings(draftAlertSettings.copy(useCustomVolume = false)) },
                    label = { Text("System Stream Control") }
                )
                FilterChip(
                    selected = draftAlertSettings.useCustomVolume,
                    onClick = { onUpdateAlertSettings(draftAlertSettings.copy(useCustomVolume = true)) },
                    label = { Text("Custom App Volume") }
                )
            }

            if (draftAlertSettings.useCustomVolume) {
                Spacer(Modifier.height(16.dp))
                Text("CUSTOM SIREN VOLUME", color = StatusRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = draftAlertSettings.alarmVolume,
                    onValueChange = onUpdateAlarmVolume,
                    valueRange = 0f..1f,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(thumbColor = StatusRed, activeTrackColor = StatusRed)
                )
                Text(
                    "Volume: ${(draftAlertSettings.alarmVolume * 100).toInt()}% ${if (draftAlertSettings.useMaxVolume) "(Max Overridden)" else ""}",
                    color = Slate500, fontSize = 11.sp
                )
            } else {
                Spacer(Modifier.height(8.dp))
                Text("Using system alarm stream volume levels.", color = Slate500, fontSize = 11.sp)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun AlarmToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = StatusGreen, checkedTrackColor = ViewerCyan)
        )
    }
}

@Composable
fun SensitivitySlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, bottom = 8.dp)) {
        Text(text = "$label: ${(value * 100).toInt()}%", color = Slate500, fontSize = 11.sp)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0.1f..1.0f,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(thumbColor = ViewerCyan, activeTrackColor = ViewerCyan)
        )
    }
}
