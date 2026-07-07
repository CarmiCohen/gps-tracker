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
import androidx.compose.material.icons.automirrored.filled.HelpCenter
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gps19.core.engine.*

/**
 * SettingsComponents: UI for app configuration and permissions.
 * v9.2.6:
 * - Forensic Stress Test: Added onTriggerForensicTest button to PhoneSetupOverlay
 *   to facilitate manual verification of violation latching logic.
 * v9.1.0:
 * - R799e: Swapped legacy BrandJd (#367C2B) for JD Vivid Green (#78BE20).
 * v9.0.4:
 * - R799d: Changed Viewer color to ViewerCyan.
 */

@Composable
fun SettingsOverlay(
    uiState: MainUiState, onClose: () -> Unit, onReset: (() -> Unit)?=null, onExport: (() -> Unit)?=null, 
    onClear: (() -> Unit)?=null, onImportConfig: () -> Unit, onFullInitialization: () -> Unit,
    onUpdateDeviceId: (String) -> Unit, onUpdateViewerId: (String) -> Unit, onUpdateRelayUrl: (String) -> Unit,
    onUpdateMaxDistance: (String) -> Unit, onUpdateAlertSettings: (AlertSettings) -> Unit, onUpdateSirenType: (String) -> Unit,
    onUpdateAlarmVolume: (Float) -> Unit, onTestSiren: () -> Unit, onShowPhoneSetup: () -> Unit = {}, viewModel: MainViewModel? = null,
    onCalibrateChair: () -> Unit = {}
) { 
    val activeSub = uiState.navigation.activeSubSettings

    Box(modifier = Modifier.fillMaxSize()) {
        Card(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), colors = CardDefaults.cardColors(containerColor = Slate900), shape = androidx.compose.ui.graphics.RectangleShape) { 
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) { 
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.settings_title), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = uiState.draftSettings.deviceId, 
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
                        value = uiState.draftSettings.viewerId, 
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
                OutlinedTextField(value = uiState.draftSettings.maxDistance, onValueChange = onUpdateMaxDistance, label = { Text(stringResource(R.string.settings_label_geofence), fontSize = 12.sp) }, placeholder = { Text(stringResource(R.string.settings_placeholder_radius), color = Slate500) }, leadingIcon = { Icon(Icons.Default.RadioButtonChecked, null, tint = Color.White, modifier = Modifier.size(18.dp)) }, trailingIcon = { Text(stringResource(R.string.settings_unit_meters), color = Slate500, fontSize = 10.sp, modifier = Modifier.padding(end = 8.dp)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.White, unfocusedBorderColor = Color.White.copy(alpha = 0.3f), focusedLabelColor = Color.White, unfocusedLabelColor = Slate500, focusedTextColor = Color.White, unfocusedTextColor = Color.White), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
                
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel?.onEvent(UiEvent.SetSubSettings(SubSettings.CLEAN)) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Rose500)) { Icon(Icons.Default.DeleteSweep, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.btn_clean)) }
                    Button(onClick = onShowPhoneSetup, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = ViewerCyan.copy(alpha = 0.8f))) { Icon(Icons.AutoMirrored.Filled.HelpCenter, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.btn_phone_setup)) }
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel?.onEvent(UiEvent.SetSubSettings(SubSettings.ALERTS)) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = ViewerCyan.copy(alpha = 0.8f))) { Icon(Icons.Default.NotificationsActive, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.btn_alerts)) }
                    Button(onClick = { viewModel?.onEvent(UiEvent.SetSubSettings(SubSettings.SOUND)) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Violet500.copy(alpha = 0.8f))) { Icon(Icons.Default.VolumeUp, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.btn_sound)) }
                }
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onImportConfig, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Slate500.copy(alpha = 0.8f))) { Text(stringResource(R.string.btn_load_config), fontSize = 11.sp) }
                    if (onExport != null) { Button(onClick = onExport, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Slate500.copy(alpha = 0.8f))) { Text(stringResource(R.string.btn_save_logs), fontSize = 11.sp) } }
                }
                Spacer(Modifier.height(24.dp)); OutlinedTextField(value = uiState.draftSettings.relayUrl, onValueChange = onUpdateRelayUrl, label = { Text(stringResource(R.string.settings_label_relay_url)) }, modifier = Modifier.fillMaxWidth()); Spacer(Modifier.height(48.dp))
            } 
        }
        when (activeSub) {
            SubSettings.CLEAN -> CleanSetupOverlay(uiState = uiState, onClear = onClear, onReset = onReset, onFullInitialization = onFullInitialization, onClose = { viewModel?.onEvent(UiEvent.SetSubSettings(null)) })
            SubSettings.ALERTS -> AlertManagementOverlay(uiState = uiState, onUpdateAlertSettings = onUpdateAlertSettings, onCalibrateChair = onCalibrateChair, onClose = { viewModel?.onEvent(UiEvent.SetSubSettings(null)) })
            SubSettings.SOUND -> AlarmSoundOverlay(uiState = uiState, onUpdateAlertSettings = onUpdateAlertSettings, onUpdateSirenType = onUpdateSirenType, onUpdateAlarmVolume = onUpdateAlarmVolume, onTestSiren = onTestSiren, onClose = { viewModel?.onEvent(UiEvent.SetSubSettings(null)) })
            else -> {}
        }
    }
}

@Composable
fun CleanSetupOverlay(uiState: MainUiState, onClear: (() -> Unit)?, onReset: (() -> Unit)?, onFullInitialization: () -> Unit, onClose: () -> Unit) {
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
fun AlertManagementOverlay(uiState: MainUiState, onUpdateAlertSettings: (AlertSettings) -> Unit, onCalibrateChair: () -> Unit = {}, onClose: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), color = Slate900) {
        Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.alert_mgmt_title), color = ViewerCyan, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(16.dp)); Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(stringResource(R.string.alert_group_toggles), color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp); Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) { TextButton(onClick = { onUpdateAlertSettings(uiState.draftSettings.alertSettings.copy(localInternet = true, serverConnection = true, relayConnection = true, jammerDetection = true, signalLoss = true, gpsStalling = true, distance = true, power = true, lowBattery = true, batteryHealth = true, highTemperature = true, longTimeGap = true, tamperAlert = true, tiltAlert = true, acousticAlert = true, liftAlert = true, chairOccupied = true, systemStorageLow = true)) }, contentPadding = PaddingValues(horizontal = 2.dp)) { Text(stringResource(R.string.btn_all_on), fontSize = 7.5.sp, color = BrandJd) }; TextButton(onClick = { onUpdateAlertSettings(uiState.draftSettings.alertSettings.copy(localInternet = false, serverConnection = false, relayConnection = false, jammerDetection = false, signalLoss = false, gpsStalling = false, distance = false, power = false, lowBattery = false, batteryHealth = false, highTemperature = false, longTimeGap = false, tamperAlert = false, tiltAlert = false, acousticAlert = false, liftAlert = false, chairOccupied = false, systemStorageLow = false)) }, contentPadding = PaddingValues(horizontal = 2.dp)) { Text(stringResource(R.string.btn_reset), fontSize = 7.5.sp, color = Rose500) } } }
            
            Spacer(Modifier.height(8.dp)); SettingsGroupHeader(stringResource(R.string.alert_group_master), BrandJd)
            AlarmToggle(stringResource(R.string.alert_label_global_mute), uiState.draftSettings.alertSettings.globalMute) { onUpdateAlertSettings(uiState.draftSettings.alertSettings.copy(globalMute = it)) }
            Text(stringResource(R.string.alert_desc_global_mute), color = Slate500, fontSize = 10.sp, modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))

            Spacer(Modifier.height(8.dp)); SettingsGroupHeader(stringResource(R.string.alert_group_comm), Rose500)
            AlarmToggle(ALERT_TITLE_LOCAL_INTERNET, uiState.draftSettings.alertSettings.localInternet) { onUpdateAlertSettings(uiState.draftSettings.alertSettings.copy(localInternet = it)) }
            AlarmToggle(ALERT_TITLE_RELAY_OFFLINE, uiState.draftSettings.alertSettings.relayConnection) { onUpdateAlertSettings(uiState.draftSettings.alertSettings.copy(relayConnection = it)) }
            AlarmToggle(ALERT_TITLE_TRACKER_OFFLINE, uiState.draftSettings.alertSettings.serverConnection) { onUpdateAlertSettings(uiState.draftSettings.alertSettings.copy(serverConnection = it)) }
            AlarmToggle(ALERT_TITLE_SIGNAL_LOSS, uiState.draftSettings.alertSettings.signalLoss) { onUpdateAlertSettings(uiState.draftSettings.alertSettings.copy(signalLoss = it)) }
            AlarmToggle(ALERT_TITLE_JUMP_ALERT, uiState.draftSettings.alertSettings.jammerDetection) { onUpdateAlertSettings(uiState.draftSettings.alertSettings.copy(jammerDetection = it)) }
            
            SettingsGroupHeader(stringResource(R.string.alert_group_location), ViewerCyan)
            AlarmToggle(ALERT_TITLE_TRACKER_GEOFENCE, uiState.draftSettings.alertSettings.distance) { onUpdateAlertSettings(uiState.draftSettings.alertSettings.copy(distance = it)) }
            AlarmToggle(ALERT_TITLE_GPS_STALL, uiState.draftSettings.alertSettings.gpsStalling) { onUpdateAlertSettings(uiState.draftSettings.alertSettings.copy(gpsStalling = it)) }
            AlarmToggle(ALERT_TITLE_TRACKER_GAP, uiState.draftSettings.alertSettings.longTimeGap) { onUpdateAlertSettings(uiState.draftSettings.alertSettings.copy(longTimeGap = it)) }
            
            SettingsGroupHeader(stringResource(R.string.alert_group_device), BrandJd)
            AlarmToggle(ALERT_TITLE_TRACKER_POWER, uiState.draftSettings.alertSettings.power) { onUpdateAlertSettings(uiState.draftSettings.alertSettings.copy(power = it)) }
            AlarmToggle(ALERT_TITLE_TRACKER_BATTERY, uiState.draftSettings.alertSettings.lowBattery) { onUpdateAlertSettings(uiState.draftSettings.alertSettings.copy(lowBattery = it)) }
            AlarmToggle(ALERT_TITLE_BATTERY_STEEP_DISCHARGE, uiState.draftSettings.alertSettings.batteryHealth) { onUpdateAlertSettings(uiState.draftSettings.alertSettings.copy(batteryHealth = it)) }
            AlarmToggle(ALERT_TITLE_TRACKER_TEMP, uiState.draftSettings.alertSettings.highTemperature) { onUpdateAlertSettings(uiState.draftSettings.alertSettings.copy(highTemperature = it)) }
            
            SettingsGroupHeader(stringResource(R.string.alert_group_integrity), Violet500)
            AlarmToggle(ALERT_TITLE_SYSTEM_STORAGE_LOW, uiState.draftSettings.alertSettings.systemStorageLow) { onUpdateAlertSettings(uiState.draftSettings.alertSettings.copy(systemStorageLow = it)) }

            SettingsGroupHeader(stringResource(R.string.alert_group_sentinel), BrandJd)
            AlarmToggle(ALERT_TITLE_TRACKER_TAMPER, uiState.draftSettings.alertSettings.tamperAlert) { onUpdateAlertSettings(uiState.draftSettings.alertSettings.copy(tamperAlert = it)) }
            AlarmToggle(ALERT_TITLE_TRACKER_TILT, uiState.draftSettings.alertSettings.tiltAlert) { onUpdateAlertSettings(uiState.draftSettings.alertSettings.copy(tiltAlert = it)) }
            AlarmToggle(ALERT_TITLE_TRACKER_ACOUSTIC, uiState.draftSettings.alertSettings.acousticAlert) { onUpdateAlertSettings(uiState.draftSettings.alertSettings.copy(acousticAlert = it)) }

            if (uiState.draftSettings.alertSettings.acousticAlert) {
                Text(stringResource(R.string.alert_desc_mic_hysteresis), color = Slate500, fontSize = 10.sp, modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))
            }

            AlarmToggle(ALERT_TITLE_TRACKER_LIFT, uiState.draftSettings.alertSettings.liftAlert) { onUpdateAlertSettings(uiState.draftSettings.alertSettings.copy(liftAlert = it)) }
            AlarmToggle(ALERT_TITLE_TRACKER_CHAIR, uiState.draftSettings.alertSettings.chairOccupied) { onUpdateAlertSettings(uiState.draftSettings.alertSettings.copy(chairOccupied = it)) }
            
            if (uiState.draftSettings.alertSettings.chairOccupied) {
                Spacer(Modifier.height(16.dp))
                Button(onClick = onCalibrateChair, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(FORENSIC_PINK_COLOR))) {
                    Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.btn_calibrate_chair))
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun AlarmSoundOverlay(uiState: MainUiState, onUpdateAlertSettings: (AlertSettings) -> Unit, onUpdateSirenType: (String) -> Unit, onUpdateAlarmVolume: (Float) -> Unit, onTestSiren: () -> Unit, onClose: () -> Unit) {
    val sirenOptions = listOf("Siren", "Chimes", "Pulse"); val settings = uiState.draftSettings.alertSettings
    Surface(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), color = Slate900) {
        Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.sound_title), color = Violet500, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(24.dp)); Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(stringResource(R.string.sound_label_test_audio), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium); Button(onClick = onTestSiren, colors = ButtonDefaults.buttonColors(containerColor = BrandJd)) { Icon(if (uiState.isSirenPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text(if (uiState.isSirenPlaying) stringResource(R.string.btn_test_audio_stop) else stringResource(R.string.btn_test_audio_test)) } }
            Spacer(Modifier.height(24.dp)); SettingsGroupHeader(stringResource(R.string.sound_group_type), Amber500); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { sirenOptions.forEach { type -> FilterChip(selected = uiState.selectedSirenType == type, onClick = { onUpdateSirenType(type) }, label = { Text(type) }) } }
            Spacer(Modifier.height(24.dp)); SettingsGroupHeader(stringResource(R.string.sound_group_behaviors), ViewerCyan)
            AlarmToggle(stringResource(R.string.sound_label_vibration), settings.vibrationEnabled) { onUpdateAlertSettings(settings.copy(vibrationEnabled = it)) }
            AlarmToggle(stringResource(R.string.sound_label_override_silent), settings.overrideSilence) { onUpdateAlertSettings(settings.copy(overrideSilence = it)) }
            AlarmToggle(stringResource(R.string.sound_label_force_max), settings.useMaxVolume) { onUpdateAlertSettings(settings.copy(overrideSilence = true, useMaxVolume = it)) }
            Spacer(Modifier.height(24.dp)); SettingsGroupHeader(stringResource(R.string.sound_group_volume_mode), ViewerCyan); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { FilterChip(selected = !settings.useCustomVolume, onClick = { onUpdateAlertSettings(settings.copy(useCustomVolume = false)) }, label = { Text(stringResource(R.string.sound_val_system_control)) }); FilterChip(selected = settings.useCustomVolume, onClick = { onUpdateAlertSettings(settings.copy(useCustomVolume = true)) }, label = { Text(stringResource(R.string.sound_val_app_control)) }) }
            if (settings.useCustomVolume) { Spacer(Modifier.height(16.dp)); SettingsGroupHeader(stringResource(R.string.sound_group_app_volume), Rose500); Slider(value = settings.alarmVolume, onValueChange = onUpdateAlarmVolume, valueRange = 0f..1f, modifier = Modifier.padding(horizontal = 8.dp), colors = SliderDefaults.colors(thumbColor = Rose500, activeTrackColor = Rose500)); Text(stringResource(R.string.sound_desc_app_volume, (settings.alarmVolume * 100).toInt(), if (settings.useMaxVolume) "(Max Overridden)" else ""), color = Slate500, fontSize = 11.sp) } else { Spacer(Modifier.height(8.dp)); Text(stringResource(R.string.sound_desc_system_volume), color = Slate500, fontSize = 11.sp) }
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
fun PhoneSetupOverlay(
    onClose: () -> Unit, onWhitelist: () -> Unit, onOverlay: () -> Unit, onAppInfo: () -> Unit, 
    onExactAlarm: () -> Unit, onXiaomi: () -> Unit, onRefresh: () -> Unit, 
    onToggleXiaomiOverride: () -> Unit = {},
    onTestAlarm: () -> Unit,
    onTriggerForensicTest: () -> Unit = {},
    isBatteryWhitelisted: Boolean, isOverlayGranted: Boolean,
    isMicrophoneGranted: Boolean, isExactAlarmGranted: Boolean, isPostNotificationsGranted: Boolean, 
    isBackgroundLocationGranted: Boolean, xiaomiStatus: XiaomiPermissionStatus, 
    isXiaomiManualOverride: Boolean,
    homePointsCount: Int, isTrackerMode: Boolean, onGoToMap: () -> Unit = {}
) {
    val manufacturer = Build.MANUFACTURER.uppercase(); val model = Build.MODEL.uppercase()
    Card(modifier = Modifier.fillMaxSize().padding(16.dp).statusBarsPadding().navigationBarsPadding(), colors = CardDefaults.cardColors(containerColor = Slate950)) {
        Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Text("Phone Setup", color = BrandJd, fontSize = 24.sp, fontWeight = FontWeight.Bold); Text(stringResource(R.string.setup_detected_device, manufacturer, model), color = Slate500, fontSize = 10.sp) } }
            Spacer(Modifier.height(16.dp)); GuideSection(stringResource(R.string.setup_step1_title), getRecentsLockDescription(), {}, stringResource(R.string.setup_info_only), if (isS21FEDevice()) true else null, Icons.Default.Lock)
            Spacer(Modifier.height(16.dp)); GuideSection(stringResource(R.string.setup_step2_title), getBatteryOptimizationDescription(), onWhitelist, stringResource(R.string.btn_open_settings), isBatteryWhitelisted, Icons.Default.BatteryChargingFull, reason = if (!isBatteryWhitelisted) "Battery Optimization: Unrestricted mode NOT active" else null)
            Spacer(Modifier.height(16.dp)); GuideSection(stringResource(R.string.setup_step3_title), stringResource(R.string.setup_step3_desc), onOverlay, stringResource(R.string.btn_authorize), isOverlayGranted, Icons.Default.Layers, reason = if (!isOverlayGranted) "Appear on Top: Permission NOT granted" else null)
            Spacer(Modifier.height(16.dp)); GuideSection(stringResource(R.string.setup_step4_title), stringResource(R.string.setup_step4_desc), onAppInfo, stringResource(R.string.btn_app_info), isMicrophoneGranted, Icons.Default.Mic, reason = if (!isMicrophoneGranted) "Microphone: Permission NOT granted" else null)
            Spacer(Modifier.height(16.dp)); GuideSection(title = stringResource(R.string.setup_step5_title), description = getAutoStartDescription(), onClick = onAppInfo, buttonText = stringResource(R.string.btn_app_info), isCompleted = isBatteryWhitelisted, icon = Icons.Default.PlayCircle, reason = if (!isBatteryWhitelisted) "Manual verification required: Ensure 'Unrestricted' battery mode and 'Background activity' are allowed in system settings." else null)
            Spacer(Modifier.height(16.dp)); GuideSection(stringResource(R.string.setup_step6_title), stringResource(R.string.setup_step6_desc), {}, stringResource(R.string.setup_info_only), null, Icons.Default.Wifi)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { Spacer(Modifier.height(16.dp)); GuideSection(stringResource(R.string.setup_step7_title), stringResource(R.string.setup_step7_desc), onExactAlarm, stringResource(R.string.btn_authorize), isExactAlarmGranted, Icons.Default.Alarm, reason = if (!isExactAlarmGranted) "Exact Alarms: Permission NOT granted" else null) }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Spacer(Modifier.height(16.dp)); GuideSection(title = "Notification Alerts", description = "Required to show status and critical alerts in the notification shade.", onClick = onAppInfo, buttonText = stringResource(R.string.btn_app_info), isCompleted = isPostNotificationsGranted, icon = Icons.Default.Notifications, reason = if (!isPostNotificationsGranted) "Notifications: Permission NOT granted" else null)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Spacer(Modifier.height(16.dp)); GuideSection(title = "Background Location", description = "Allows tracking and geofencing to work while the screen is off or app is in background.", onClick = onAppInfo, buttonText = stringResource(R.string.btn_app_info), isCompleted = isBackgroundLocationGranted, icon = Icons.Default.LocationOn, reason = if (!isBackgroundLocationGranted) "Background Location: Set to 'Allow all the time' in system settings" else null)
            }

            if (isXiaomiDevice()) { 
                Spacer(Modifier.height(16.dp))
                val isCompleted = when(xiaomiStatus) {
                    XiaomiPermissionStatus.GRANTED -> true
                    XiaomiPermissionStatus.DENIED -> false
                    XiaomiPermissionStatus.UNKNOWN -> isXiaomiManualOverride
                }
                GuideSection(
                    title = stringResource(R.string.setup_step8_title), 
                    description = stringResource(R.string.setup_step8_desc), 
                    onClick = onXiaomi, 
                    buttonText = stringResource(R.string.btn_miui_permissions), 
                    isCompleted = isCompleted, 
                    icon = Icons.Default.Security, 
                    reason = if (xiaomiStatus == XiaomiPermissionStatus.DENIED) "Xiaomi: Required for Lock Screen alerts" else if (xiaomiStatus == XiaomiPermissionStatus.UNKNOWN && !isXiaomiManualOverride) "Xiaomi: Automatic verification failed. Please check manually." else null
                )
                if (xiaomiStatus == XiaomiPermissionStatus.UNKNOWN) {
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.padding(start = 28.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isXiaomiManualOverride, onCheckedChange = { onToggleXiaomiOverride() })
                        Text("Manually verified (Status detection failed)", color = Color.White, fontSize = 11.sp)
                    }
                }
            }
            if (!isTrackerMode) { Spacer(Modifier.height(16.dp)); GuideSection(title = stringResource(R.string.setup_step9_title), description = stringResource(R.string.setup_step9_desc), onClick = onGoToMap, buttonText = stringResource(R.string.btn_open_map), isCompleted = homePointsCount > 0, icon = Icons.Default.Map, reason = if (homePointsCount == 0) "Geofence: No Home Points defined" else null) }
            Spacer(Modifier.height(32.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRefresh, modifier = Modifier.weight(1f).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = ViewerCyan)) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.btn_refresh)) }
                Button(onClick = onTestAlarm, modifier = Modifier.weight(1f).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Violet500)) { Icon(Icons.Default.NotificationImportant, null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.btn_test_alarm)) }
            }
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
        Text(description, color = Slate500, fontSize = 12.sp, modifier = Modifier.padding(start = 28.dp)); if (buttonText.isNotEmpty() && buttonText != stringResource(R.string.setup_info_only)) { Spacer(Modifier.height(4.dp)); Button(onClick = onClick, modifier = Modifier.padding(start = 28.dp).height(32.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) { Text(buttonText, fontSize = 11.sp) } }
    }
}
