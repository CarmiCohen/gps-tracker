package com.gps19.app

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.HelpCenter
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.gps19.core.engine.*
import kotlinx.coroutines.delay

/**
 * SettingsComponents: UI for app configuration and permissions.
 * Aug.13.14:
 * - Issue #166: Settings Overlay ANR. Implemented staggered hydration 
 *   to reduce initial composition load on the main thread (R166).
 * Aug.13.11:
 * - Issue #162: Phone Setup ANR Remediation. Hardened hydration gate (150ms) 
 *   and increased staggered rendering offsets (80ms) to prevent main-thread 
 *   stalls on budget hardware. Memoized static descriptions (R162).
 */

@Composable
fun SettingsOverlay(
    activeSubSettings: SubSettings?,
    draftDeviceId: String,
    draftViewerId: String,
    draftRelayUrl: String,
    draftMaxDistance: String,
    draftAlertSettings: AlertSettings,
    selectedSirenType: String,
    isSirenPlaying: Boolean,
    onClose: () -> Unit, 
    onReset: (() -> Unit)?=null, 
    onExport: (() -> Unit)?=null, 
    onClear: (() -> Unit)?=null, 
    onImportConfig: () -> Unit, 
    onFullInitialization: () -> Unit,
    onUpdateDeviceId: (String) -> Unit, 
    onUpdateViewerId: (String) -> Unit, 
    onUpdateRelayUrl: (String) -> Unit,
    onUpdateMaxDistance: (String) -> Unit, 
    onUpdateAlertSettings: (AlertSettings) -> Unit, 
    onUpdateSirenType: (String) -> Unit,
    onUpdateAlarmVolume: (Float) -> Unit, 
    onTestSiren: () -> Unit, 
    onShowPhoneSetup: () -> Unit = {}, 
    onEvent: (UiEvent) -> Unit
) { 
    // Issue #166: Staggered hydration to prevent ANR during heavy composition
    var isHydrated by remember { mutableStateOf(false) }
    var visibleCount by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        delay(150) 
        isHydrated = true
        repeat(10) { 
            visibleCount++
            delay(60) 
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Card(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), colors = CardDefaults.cardColors(containerColor = Slate900), shape = androidx.compose.ui.graphics.RectangleShape) { 
            if (isHydrated && visibleCount > 0) {
                Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) { 
                    if (visibleCount >= 1) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.settings_title), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.height(16.dp))
                    }

                    if (visibleCount >= 2) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = draftDeviceId, 
                                onValueChange = onUpdateDeviceId, 
                                label = { Text(stringResource(R.string.settings_label_tracker_id), fontSize = 12.sp) }, 
                                leadingIcon = { Icon(Icons.Default.Agriculture, null, tint = BrandJd, modifier = Modifier.size(18.dp)) }, 
                                modifier = Modifier.weight(1f), 
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandJd, 
                                    unfocusedBorderColor = BrandJd.copy(alpha = 0.5f), 
                                    focusedLabelColor = BrandJd, 
                                    unfocusedLabelColor = BrandJd.copy(alpha = 0.7f), 
                                    focusedTextColor = BrandJd, 
                                    unfocusedTextColor = BrandJd,
                                    cursorColor = BrandJd
                                ), 
                                singleLine = true, 
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            )
                            OutlinedTextField(
                                value = draftViewerId, 
                                onValueChange = onUpdateViewerId, 
                                label = { Text(stringResource(R.string.settings_label_viewer_id), fontSize = 12.sp) }, 
                                leadingIcon = { Icon(Icons.Default.Person, null, tint = ViewerCyan, modifier = Modifier.size(18.dp)) }, 
                                modifier = Modifier.weight(1f), 
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ViewerCyan, 
                                    unfocusedBorderColor = ViewerCyan.copy(alpha = 0.5f), 
                                    focusedLabelColor = ViewerCyan, 
                                    unfocusedLabelColor = ViewerCyan.copy(alpha = 0.7f), 
                                    focusedTextColor = ViewerCyan, 
                                    unfocusedTextColor = ViewerCyan,
                                    cursorColor = ViewerCyan
                                ), 
                                singleLine = true, 
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            ) 
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    if (visibleCount >= 3) {
                        OutlinedTextField(value = draftMaxDistance, onValueChange = onUpdateMaxDistance, label = { Text(stringResource(R.string.settings_label_geofence), fontSize = 12.sp) }, placeholder = { Text(stringResource(R.string.settings_placeholder_radius), color = Slate500) }, leadingIcon = { Icon(Icons.Default.RadioButtonChecked, null, tint = Color.White, modifier = Modifier.size(18.dp)) }, trailingIcon = { Text(stringResource(R.string.settings_unit_meters), color = Slate500, fontSize = 10.sp, modifier = Modifier.padding(end = 8.dp)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.White, unfocusedBorderColor = Color.White.copy(alpha = 0.3f), focusedLabelColor = Color.White, unfocusedLabelColor = Slate500, focusedTextColor = Color.White, unfocusedTextColor = Color.White), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
                        Spacer(Modifier.height(24.dp))
                    }

                    if (visibleCount >= 4) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onEvent(UiEvent.SetSubSettings(SubSettings.CLEAN)) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Rose500)) { Icon(Icons.Default.DeleteSweep, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.btn_clean)) }
                            Button(onClick = onShowPhoneSetup, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = ViewerCyan.copy(alpha = 0.8f))) { Icon(Icons.AutoMirrored.Filled.HelpCenter, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.btn_phone_setup)) }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    if (visibleCount >= 5) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onEvent(UiEvent.SetSubSettings(SubSettings.ALERTS)) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = ViewerCyan.copy(alpha = 0.8f))) { Icon(Icons.Default.NotificationsActive, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.btn_alerts)) }
                            Button(onClick = { onEvent(UiEvent.SetSubSettings(SubSettings.SOUND)) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Violet500.copy(alpha = 0.8f))) { Icon(Icons.Default.VolumeUp, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.btn_sound)) }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    if (visibleCount >= 6) {
                        Button(onClick = { onEvent(UiEvent.NavigateToDiagnostics(true)) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Slate700)) { Icon(Icons.Default.HealthAndSafety, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.btn_diagnostics)) }
                        Spacer(Modifier.height(24.dp))
                    }

                    if (visibleCount >= 7) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onImportConfig, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Slate500.copy(alpha = 0.8f))) { Text(stringResource(R.string.btn_load_config), fontSize = 11.sp) }
                            if (onExport != null) { Button(onClick = onExport, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Slate500.copy(alpha = 0.8f))) { Text(stringResource(R.string.btn_save_logs), fontSize = 11.sp) } }
                        }
                        Spacer(Modifier.height(24.dp))
                    }

                    if (visibleCount >= 8) {
                        OutlinedTextField(value = draftRelayUrl, onValueChange = onUpdateRelayUrl, label = { Text(stringResource(R.string.settings_label_relay_url)) }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(48.dp))
                    }
                } 
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandJd, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
                }
            }
        }
        if (isHydrated && visibleCount >= 9) {
            when (activeSubSettings) {
                SubSettings.CLEAN -> CleanSetupOverlay(onClear = onClear, onReset = onReset, onFullInitialization = onFullInitialization, onClose = { onEvent(UiEvent.SetSubSettings(null)) })
                SubSettings.ALERTS -> AlertManagementOverlay(draftAlertSettings = draftAlertSettings, onUpdateAlertSettings = onUpdateAlertSettings, onClose = { onEvent(UiEvent.SetSubSettings(null)) })
                SubSettings.SOUND -> AlarmSoundOverlay(draftAlertSettings = draftAlertSettings, selectedSirenType = selectedSirenType, isSirenPlaying = isSirenPlaying, onUpdateAlertSettings = onUpdateAlertSettings, onUpdateSirenType = onUpdateSirenType, onUpdateAlarmVolume = onUpdateAlarmVolume, onTestSiren = onTestSiren, onClose = { onEvent(UiEvent.SetSubSettings(null)) })
                else -> {}
            }
        }
    }
}

@Composable
fun CleanSetupOverlay(onClear: (() -> Unit)?, onReset: (() -> Unit)?, onFullInitialization: () -> Unit, onClose: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), color = Slate900) {
        Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.clean_init_title), color = Rose500, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(16.dp)); SettingsGroupHeader(stringResource(R.string.clean_group_maintenance), Rose500)
            if (onClear != null) { Button(onClick = { onClear(); onClose() }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Rose500)) { Icon(Icons.Default.DeleteForever, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.btn_clear_home)) }; Spacer(Modifier.height(16.dp)) }
            if (onReset != null) { Button(onClick = { onReset(); onClose() }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Rose500)) { Icon(Icons.Default.History, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.btn_reset_stats)) }; Spacer(Modifier.height(16.dp)) }
            Button(onClick = { onFullInitialization(); onClose() }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Rose500.copy(alpha = 0.8f))) { Icon(Icons.Default.Warning, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.btn_full_init)) }; Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
fun AlertManagementOverlay(draftAlertSettings: AlertSettings, onUpdateAlertSettings: (AlertSettings) -> Unit, onClose: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), color = Slate900) {
        Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.alert_mgmt_title), color = ViewerCyan, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(16.dp)); Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(stringResource(R.string.alert_group_toggles), color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp); Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) { TextButton(onClick = { onUpdateAlertSettings(draftAlertSettings.copy(localInternet = true, serverConnection = true, relayConnection = true, jammerDetection = true, signalLoss = true, gpsStalling = true, distance = true, power = true, lowBattery = true, batteryHealth = true, highTemperature = true, longTimeGap = true, tamperAlert = true, tiltAlert = true, acousticAlert = true, liftAlert = true, systemStorageLow = true)) }, contentPadding = PaddingValues(horizontal = 2.dp)) { Text(stringResource(R.string.btn_all_on), fontSize = 7.5.sp, color = BrandJd) }; TextButton(onClick = { onUpdateAlertSettings(draftAlertSettings.copy(localInternet = false, serverConnection = false, relayConnection = false, jammerDetection = false, signalLoss = false, gpsStalling = false, distance = false, power = false, lowBattery = false, batteryHealth = false, highTemperature = false, longTimeGap = false, tamperAlert = false, tiltAlert = false, acousticAlert = false, liftAlert = false, systemStorageLow = false)) }, contentPadding = PaddingValues(horizontal = 2.dp)) { Text(stringResource(R.string.btn_reset), fontSize = 7.5.sp, color = Rose500) } } }
            
            Spacer(Modifier.height(8.dp)); SettingsGroupHeader(stringResource(R.string.alert_group_master), BrandJd)
            AlarmToggle(stringResource(R.string.alert_label_global_mute), draftAlertSettings.globalMute) { onUpdateAlertSettings(draftAlertSettings.copy(globalMute = it)) }
            Text(stringResource(R.string.alert_desc_global_mute), color = Slate500, fontSize = 10.sp, modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))

            Spacer(Modifier.height(8.dp)); SettingsGroupHeader(stringResource(R.string.alert_group_comm), Rose500)
            AlarmToggle(ALERT_TITLE_LOCAL_INTERNET, draftAlertSettings.localInternet) { onUpdateAlertSettings(draftAlertSettings.copy(localInternet = it)) }
            AlarmToggle(ALERT_TITLE_RELAY_OFFLINE, draftAlertSettings.relayConnection) { onUpdateAlertSettings(draftAlertSettings.copy(relayConnection = it)) }
            AlarmToggle(ALERT_TITLE_TRACKER_OFFLINE, draftAlertSettings.serverConnection) { onUpdateAlertSettings(draftAlertSettings.copy(serverConnection = it)) }
            AlarmToggle(ALERT_TITLE_SIGNAL_LOSS, draftAlertSettings.signalLoss) { onUpdateAlertSettings(draftAlertSettings.copy(signalLoss = it)) }
            AlarmToggle(ALERT_TITLE_JUMP_ALERT, draftAlertSettings.jammerDetection) { onUpdateAlertSettings(draftAlertSettings.copy(jammerDetection = it)) }
            
            SettingsGroupHeader(stringResource(R.string.alert_group_location), ViewerCyan)
            AlarmToggle(ALERT_TITLE_TRACKER_GEOFENCE, draftAlertSettings.distance) { onUpdateAlertSettings(draftAlertSettings.copy(distance = it)) }
            AlarmToggle(ALERT_TITLE_GPS_STALL, draftAlertSettings.gpsStalling) { onUpdateAlertSettings(draftAlertSettings.copy(gpsStalling = it)) }
            AlarmToggle(ALERT_TITLE_TRACKER_GAP, draftAlertSettings.longTimeGap) { onUpdateAlertSettings(draftAlertSettings.copy(longTimeGap = it)) }
            
            SettingsGroupHeader(stringResource(R.string.alert_group_device), BrandJd)
            AlarmToggle(ALERT_TITLE_TRACKER_POWER, draftAlertSettings.power) { onUpdateAlertSettings(draftAlertSettings.copy(power = it)) }
            AlarmToggle(ALERT_TITLE_TRACKER_BATTERY, draftAlertSettings.lowBattery) { onUpdateAlertSettings(draftAlertSettings.copy(lowBattery = it)) }
            AlarmToggle(ALERT_TITLE_BATTERY_STEEP_DISCHARGE, draftAlertSettings.batteryHealth) { onUpdateAlertSettings(draftAlertSettings.copy(batteryHealth = it)) }
            AlarmToggle(ALERT_TITLE_TRACKER_TEMP, draftAlertSettings.highTemperature) { onUpdateAlertSettings(draftAlertSettings.copy(highTemperature = it)) }
            
            SettingsGroupHeader(stringResource(R.string.alert_group_integrity), Violet500)
            AlarmToggle(ALERT_TITLE_SYSTEM_STORAGE_LOW, draftAlertSettings.systemStorageLow) { onUpdateAlertSettings(draftAlertSettings.copy(systemStorageLow = it)) }

            SettingsGroupHeader(stringResource(R.string.alert_group_sentinel), BrandJd)
            AlarmToggle(ALERT_TITLE_TRACKER_TAMPER, draftAlertSettings.tamperAlert) { onUpdateAlertSettings(draftAlertSettings.copy(tamperAlert = it)) }
            AlarmToggle(ALERT_TITLE_TRACKER_TILT, draftAlertSettings.tiltAlert) { onUpdateAlertSettings(draftAlertSettings.copy(tiltAlert = it)) }
            AlarmToggle(ALERT_TITLE_TRACKER_ACOUSTIC, draftAlertSettings.acousticAlert) { onUpdateAlertSettings(draftAlertSettings.copy(acousticAlert = it)) }

            if (draftAlertSettings.acousticAlert) {
                Text(stringResource(R.string.alert_desc_mic_hysteresis), color = Slate500, fontSize = 10.sp, modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))
            }

            AlarmToggle(ALERT_TITLE_TRACKER_LIFT, draftAlertSettings.liftAlert) { onUpdateAlertSettings(draftAlertSettings.copy(liftAlert = it)) }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun AlarmSoundOverlay(draftAlertSettings: AlertSettings, selectedSirenType: String, isSirenPlaying: Boolean, onUpdateAlertSettings: (AlertSettings) -> Unit, onUpdateSirenType: (String) -> Unit, onUpdateAlarmVolume: (Float) -> Unit, onTestSiren: () -> Unit, onClose: () -> Unit) {
    val sirenOptions = listOf("Siren", "Chimes", "Pulse")
    Surface(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), color = Slate900) {
        Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.sound_title), color = Violet500, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(24.dp)); Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(stringResource(R.string.sound_label_test_audio), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium); Button(onClick = onTestSiren, colors = ButtonDefaults.buttonColors(containerColor = BrandJd)) { Icon(if (isSirenPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text(if (isSirenPlaying) stringResource(R.string.btn_test_audio_stop) else stringResource(R.string.btn_test_audio_test)) } }
            Spacer(Modifier.height(24.dp)); SettingsGroupHeader(stringResource(R.string.sound_group_type), Amber500); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { sirenOptions.forEach { type -> FilterChip(selected = selectedSirenType == type, onClick = { onUpdateSirenType(type) }, label = { Text(type) }) } }
            Spacer(Modifier.height(24.dp)); SettingsGroupHeader(stringResource(R.string.sound_group_behaviors), ViewerCyan)
            AlarmToggle(stringResource(R.string.sound_label_vibration), draftAlertSettings.vibrationEnabled) { onUpdateAlertSettings(draftAlertSettings.copy(vibrationEnabled = it)) }
            AlarmToggle(stringResource(R.string.sound_label_override_silent), draftAlertSettings.overrideSilence) { onUpdateAlertSettings(draftAlertSettings.copy(overrideSilence = it)) }
            AlarmToggle(stringResource(R.string.sound_label_force_max), draftAlertSettings.useMaxVolume) { onUpdateAlertSettings(draftAlertSettings.copy(overrideSilence = true, useMaxVolume = it)) }
            Spacer(Modifier.height(24.dp)); SettingsGroupHeader(stringResource(R.string.sound_group_volume_mode), ViewerCyan); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { FilterChip(selected = !draftAlertSettings.useCustomVolume, onClick = { onUpdateAlertSettings(draftAlertSettings.copy(useCustomVolume = false)) }, label = { Text(stringResource(R.string.sound_val_system_control)) }); FilterChip(selected = draftAlertSettings.useCustomVolume, onClick = { onUpdateAlertSettings(draftAlertSettings.copy(useCustomVolume = true)) }, label = { Text(stringResource(R.string.sound_val_app_control)) }) }
            if (draftAlertSettings.useCustomVolume) { Spacer(Modifier.height(16.dp)); SettingsGroupHeader(stringResource(R.string.sound_group_app_volume), Rose500); Slider(value = draftAlertSettings.alarmVolume, onValueChange = onUpdateAlarmVolume, valueRange = 0f..1f, modifier = Modifier.padding(horizontal = 8.dp), colors = SliderDefaults.colors(thumbColor = Rose500, activeTrackColor = Rose500)); Text(stringResource(R.string.sound_desc_app_volume, (draftAlertSettings.alarmVolume * 100).toInt(), if (draftAlertSettings.useMaxVolume) "(Max Overridden)" else ""), color = Slate500, fontSize = 11.sp) } else { Spacer(Modifier.height(8.dp)); Text(stringResource(R.string.sound_desc_system_volume), color = Slate500, fontSize = 11.sp) }
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
fun PhoneSetupOverlay(
    onClose: () -> Unit, onWhitelist: () -> Unit, onOverlay: () -> Unit, onAppInfo: () -> Unit, 
    onExactAlarm: () -> Unit, onHardwarePermission: () -> Unit, onRefresh: () -> Unit, 
    onToggleManualOverride: () -> Unit = {},
    onTestAlarm: () -> Unit,
    onTriggerForensicTest: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
    permissions: PermissionState,
    homePointsCount: Int, isTrackerMode: Boolean, onGoToMap: () -> Unit = {}
) {
    // Issue #162: Hardened hydration gate.
    var isHydrated by remember { mutableStateOf(false) }
    var visibleCount by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        delay(150) // Issue #162: Increased initial delay to allow container transition to settle
        isHydrated = true
        repeat(15) { // Up to 15 sections/spacers
            visibleCount++
            delay(80) // Issue #162: Increased delay to 80ms to provide more breathing room per frame
        }
    }

    // Issue #162: Memoize static descriptions to avoid redundant lookups during heartbeats
    val manufacturer = remember { Build.MANUFACTURER.uppercase() }
    val model = remember { Build.MODEL.uppercase() }
    val recentsLockDesc = remember { getRecentsLockDescription() }
    val batteryOptDesc = remember { getBatteryOptimizationDescription() }
    val autoStartDesc = remember { getAutoStartDescription() }

    Card(modifier = Modifier.fillMaxSize().padding(16.dp).statusBarsPadding().navigationBarsPadding(), colors = CardDefaults.cardColors(containerColor = Slate950)) {
        if (isHydrated && visibleCount > 0) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Text("Phone Setup", color = BrandJd, fontSize = 24.sp, fontWeight = FontWeight.Bold); Text(stringResource(R.string.setup_detected_device, manufacturer, model), color = Slate500, fontSize = 10.sp) } }
                
                if (visibleCount >= 1) {
                    Spacer(Modifier.height(16.dp)); GuideSection(stringResource(R.string.setup_step1_title), recentsLockDesc, {}, stringResource(R.string.setup_info_only), if (permissions.requiresWakeLockRenewal) true else null, Icons.Default.Lock)
                }
                if (visibleCount >= 2) {
                    Spacer(Modifier.height(16.dp)); GuideSection(stringResource(R.string.setup_step2_title), batteryOptDesc, onWhitelist, stringResource(R.string.btn_open_settings), permissions.isBatteryWhitelisted, Icons.Default.BatteryChargingFull, reason = if (!permissions.isBatteryWhitelisted) "Battery Optimization: Unrestricted mode NOT active" else null)
                }
                if (visibleCount >= 3) {
                    Spacer(Modifier.height(16.dp)); GuideSection(stringResource(R.string.setup_step3_title), stringResource(R.string.setup_step3_desc), onOverlay, stringResource(R.string.btn_authorize), permissions.isOverlayGranted, Icons.Default.Layers, reason = if (!permissions.isOverlayGranted) "Appear on Top: Permission NOT granted" else null)
                }
                if (visibleCount >= 4) {
                    Spacer(Modifier.height(16.dp)); GuideSection(stringResource(R.string.setup_step4_title), stringResource(R.string.setup_step4_desc), onAppInfo, stringResource(R.string.btn_app_info), permissions.isMicrophoneGranted, Icons.Default.Mic, reason = if (!permissions.isMicrophoneGranted) "Microphone: Permission NOT granted" else null)
                }
                if (visibleCount >= 5) {
                    Spacer(Modifier.height(16.dp)); GuideSection(title = stringResource(R.string.setup_step5_title), description = autoStartDesc, onClick = onAppInfo, buttonText = stringResource(R.string.btn_app_info), isCompleted = permissions.isBatteryWhitelisted, icon = Icons.Default.PlayCircle, reason = if (!permissions.isBatteryWhitelisted) "Manual verification required: Ensure 'Unrestricted' battery mode and 'Background activity' are allowed in system settings." else null)
                }
                if (visibleCount >= 6) {
                    Spacer(Modifier.height(16.dp)); GuideSection(stringResource(R.string.setup_step6_title), stringResource(R.string.setup_step6_desc), {}, stringResource(R.string.setup_info_only), null, Icons.Default.Wifi)
                }
                
                if (visibleCount >= 7) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { Spacer(Modifier.height(16.dp)); GuideSection(stringResource(R.string.setup_step7_title), stringResource(R.string.setup_step7_desc), onExactAlarm, stringResource(R.string.btn_authorize), permissions.isExactAlarmGranted, Icons.Default.Alarm, reason = if (!permissions.isExactAlarmGranted) "Exact Alarms: Permission NOT granted" else null) }
                }
                
                if (visibleCount >= 8) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Spacer(Modifier.height(16.dp)); GuideSection(title = "Notification Alerts", description = "Required to show status and critical alerts in the notification shade.", onClick = onAppInfo, buttonText = stringResource(R.string.btn_app_info), isCompleted = permissions.isPostNotificationsGranted, icon = Icons.Default.Notifications, reason = if (!permissions.isPostNotificationsGranted) "Notifications: Permission NOT granted" else null)
                    }
                }
                
                if (visibleCount >= 9) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Spacer(Modifier.height(16.dp)); GuideSection(title = "Background Location", description = "Allows tracking and geofencing to work while the screen is off or app is in background.", onClick = onAppInfo, buttonText = stringResource(R.string.btn_app_info), isCompleted = permissions.isBackgroundLocationGranted, icon = Icons.Default.LocationOn, reason = if (!permissions.isBackgroundLocationGranted) "Background Location: Set to 'Allow all the time' in system settings" else null)
                        Spacer(Modifier.height(16.dp)); GuideSection(title = "Physical Activity", description = "Required for Step Detector and stay-alive pulsing to stabilize background performance.", onClick = onAppInfo, buttonText = stringResource(R.string.btn_app_info), isCompleted = permissions.isActivityRecognitionGranted, icon = Icons.AutoMirrored.Filled.DirectionsRun, reason = if (!permissions.isActivityRecognitionGranted) "Physical Activity: Permission NOT granted" else null)
                    }
                }

                if (visibleCount >= 10) {
                    if (permissions.hasBackgroundRestriction) { 
                        Spacer(Modifier.height(16.dp))
                        val isCompleted = when(permissions.backgroundStatus) {
                            CapabilityStatus.GRANTED -> true
                            CapabilityStatus.DENIED -> false
                            CapabilityStatus.UNKNOWN -> permissions.isManualOverride
                        }
                        GuideSection(
                            title = "Background Service Lock", 
                            description = "Required to ensure the tracking service remains active on this hardware.", 
                            onClick = onHardwarePermission, 
                            buttonText = stringResource(R.string.btn_miui_permissions), 
                            isCompleted = isCompleted, 
                            icon = Icons.Default.Security, 
                            reason = if (permissions.backgroundStatus == CapabilityStatus.DENIED) "Hardware Policy: Required for Lock Screen alerts" else if (permissions.backgroundStatus == CapabilityStatus.UNKNOWN && !permissions.isManualOverride) "Hardware Policy: Automatic verification failed. Please check manually." else null
                        )
                        if (permissions.backgroundStatus == CapabilityStatus.UNKNOWN) {
                            Spacer(Modifier.height(4.dp))
                            Row(modifier = Modifier.padding(start = 28.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = permissions.isManualOverride, onCheckedChange = { onToggleManualOverride() })
                                Text("Manually verified (Status detection failed)", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }

                if (visibleCount >= 11) {
                    if (!isTrackerMode) { Spacer(Modifier.height(16.dp)); GuideSection(title = stringResource(R.string.setup_step9_title), description = stringResource(R.string.setup_step9_desc), onClick = onGoToMap, buttonText = stringResource(R.string.btn_open_map), isCompleted = homePointsCount > 0, icon = Icons.Default.Map, reason = if (homePointsCount == 0) "Geofence: No Home Points defined" else null) }
                }
                
                if (visibleCount >= 12) {
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = onNavigateToDiagnostics,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate700)
                    ) {
                        Icon(Icons.Default.HealthAndSafety, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_view_diagnostics), fontWeight = FontWeight.Bold)
                    }
                }

                if (visibleCount >= 13) {
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onRefresh, modifier = Modifier.weight(1f).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = ViewerCyan)) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.btn_refresh)) }
                        Button(onClick = onTestAlarm, modifier = Modifier.weight(1f).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Violet500)) { Icon(Icons.Default.NotificationImportant, null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.btn_test_alarm)) }
                    }
                }

                if (visibleCount >= 14) {
                    if (isTrackerMode) {
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onTriggerForensicTest, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(FORENSIC_PINK_COLOR).copy(alpha = 0.8f))) {
                            Icon(Icons.Default.BugReport, null)
                            Spacer(Modifier.width(8.dp))
                            Text("TRIGGER FORENSIC STRESS TEST", fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandJd, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
fun SettingsGroupHeader(title: String, color: Color) { Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text(title, color = color, fontSize = 14.sp); HorizontalDivider(color = color.copy(alpha = 0.3f), thickness = 1.dp) } }

@Composable
fun AlarmToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(label, color = Color.White, fontSize = 15.sp); Switch(checked = checked, onCheckedChange = onCheckedChange) } }

@Composable
fun GuideSection(title: String, description: String, onClick: () -> Unit, buttonText: String, isCompleted: Boolean?, icon: androidx.compose.ui.graphics.vector.ImageVector, reason: String? = null) {
    val statusIcon = when(isCompleted) { true -> Icons.Default.CheckCircle; false -> Icons.Default.Warning; null -> Icons.Default.Info }; 
    val statusColor = when(isCompleted) { true -> BrandJd; false -> Amber500; null -> Slate500 }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Icon(icon, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
        if (isCompleted == false && !reason.isNullOrEmpty()) { Text("Reason: $reason", color = Amber500, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 28.dp, top = 2.dp)) }
        Text(description, color = Slate500, fontSize = 12.sp, modifier = Modifier.padding(start = 28.dp)); if (isCompleted != true && buttonText.isNotEmpty() && buttonText != stringResource(R.string.setup_info_only)) { Spacer(Modifier.height(4.dp)); Button(onClick = onClick, modifier = Modifier.padding(start = 28.dp).height(32.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) { Text(buttonText, fontSize = 11.sp) } }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
fun SettingsOverlayPreview() {
    SettingsOverlay(
        activeSubSettings = null,
        draftDeviceId = "TRK-001",
        draftViewerId = "VIEW-001",
        draftRelayUrl = "wss://relay.example.com",
        draftMaxDistance = "100",
        draftAlertSettings = AlertSettings(),
        selectedSirenType = "Siren",
        isSirenPlaying = false,
        onClose = {},
        onImportConfig = {},
        onFullInitialization = {},
        onUpdateDeviceId = {},
        onUpdateViewerId = {},
        onUpdateRelayUrl = {},
        onUpdateMaxDistance = {},
        onUpdateAlertSettings = {},
        onUpdateSirenType = {},
        onUpdateAlarmVolume = {},
        onTestSiren = {},
        onEvent = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF020617)
@Composable
fun PhoneSetupOverlayPreview() {
    PhoneSetupOverlay(
        onClose = {},
        onWhitelist = {},
        onOverlay = {},
        onAppInfo = {},
        onExactAlarm = {},
        onHardwarePermission = {},
        onRefresh = {},
        onTestAlarm = {},
        permissions = PermissionState(
            isFineLocationGranted = true,
            isBatteryWhitelisted = false,
            isOverlayGranted = true
        ),
        homePointsCount = 0,
        isTrackerMode = true
    )
}
