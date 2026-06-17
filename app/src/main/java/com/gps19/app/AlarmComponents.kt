package com.gps19.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gps19.core.engine.*

/**
 * AlarmComponents: Overlay for active alarm states and sirens.
 * Extracted from OverlayComponents for Issue 115 modularization.
 */

@Composable
fun AlarmOverlay(
    alarms: List<AlarmInfo>, 
    isMuted: Boolean, 
    onMute: () -> Unit, 
    onClose: () -> Unit, 
    onGoToMap: () -> Unit = onClose, 
    isLocationPending: Boolean = false,
    xiaomiStatus: XiaomiPermissionStatus = XiaomiPermissionStatus.UNKNOWN,
    onXiaomiPermissionClick: () -> Unit = {}
) {
    val unresolvedAlarms = alarms.filter { !it.isResolved }
    val hasUnresolved = unresolvedAlarms.isNotEmpty()
    val isSirenPlaying = unresolvedAlarms.any { !it.isSirenDisabled } && !isMuted
    
    val alarmCategories = listOf(
        ALERT_TITLE_LOCAL_INTERNET, ALERT_TITLE_RELAY_OFFLINE, ALERT_TITLE_TRACKER_OFFLINE,
        ALERT_TITLE_JUMP_ALERT, ALERT_TITLE_SIGNAL_LOSS, ALERT_TITLE_GPS_STALL,
        ALERT_TITLE_TRACKER_POWER, ALERT_TITLE_TRACKER_GEOFENCE, ALERT_TITLE_TRACKER_BATTERY,
        ALERT_TITLE_BATTERY_STEEP_DISCHARGE,
        ALERT_TITLE_TRACKER_TEMP, ALERT_TITLE_TRACKER_GAP, ALERT_TITLE_TRACKER_TAMPER,
        ALERT_TITLE_TRACKER_TILT, ALERT_TITLE_TRACKER_ACOUSTIC, ALERT_TITLE_TRACKER_LIFT,
        ALERT_TITLE_TRACKER_CHAIR, ALERT_TITLE_SYSTEM_STORAGE_LOW, ALERT_TITLE_SYSTEM_STORAGE_CRITICAL
    )

    val activeDisplayList = alarmCategories.mapNotNull { cat -> alarms.find { it.type == cat || it.title == cat } }
    val activeCount = activeDisplayList.size
    val showSubtitle = activeCount <= 8
    val titleFontSize = when { activeCount <= 3 -> 24.sp; activeCount <= 6 -> 18.sp; activeCount <= 10 -> 15.sp; else -> 12.sp }
    val subtitleFontSize = when { activeCount <= 3 -> 14.sp; activeCount <= 6 -> 11.sp; else -> 9.sp }
    val verticalPadding = when { activeCount <= 4 -> 16.dp; activeCount <= 8 -> 8.dp; else -> 4.dp }
    val bgColor = if (isMuted) Color(0xFF450A0A) else if (hasUnresolved) Rose500.copy(alpha = 0.8f) else Color(0xFF064E3B)

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.size(80.dp).background(if (isMuted) Slate500.copy(alpha = 0.2f) else if (hasUnresolved) Rose500.copy(alpha = 0.4f) else Emerald500.copy(alpha = 0.4f), CircleShape).border(3.dp, if (isMuted) Slate500 else Color.White, CircleShape).padding(16.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { if (!isMuted && isSirenPlaying) onMute() }, contentAlignment = Alignment.Center) {
                Icon(if (isMuted) Icons.Default.NotificationsOff else if (hasUnresolved) Icons.Default.Warning else Icons.Default.CheckCircle, null, tint = if (isMuted) Slate500 else Color.White, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(if (isMuted) stringResource(R.string.alarm_title_muted) else if (hasUnresolved) stringResource(R.string.alarm_title_active) else stringResource(R.string.alarm_title_secure), color = if (isMuted) Slate500 else Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            
            if (hasUnresolved) {
                if (xiaomiStatus == XiaomiPermissionStatus.DENIED) {
                    Spacer(Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onXiaomiPermissionClick() },
                        colors = CardDefaults.cardColors(containerColor = Amber500),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.alarm_xiaomi_permission_required), color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(stringResource(R.string.alarm_xiaomi_permission_desc), color = Color.Black.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                } else if (isLocationPending) {
                    Spacer(Modifier.height(4.dp))
                    Box(modifier = Modifier.background(Amber500, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Text(stringResource(R.string.alarm_location_pending), color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            
            Column(modifier = Modifier.fillMaxWidth().weight(1f).background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(24.dp)).border(2.dp, if (isMuted) Slate500.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.3f), RoundedCornerShape(24.dp)).padding(8.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(verticalPadding)) {
                if (activeDisplayList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { Text(stringResource(R.string.alarm_no_violations), color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp) }
                } else {
                    activeDisplayList.forEach { info ->
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = verticalPadding / 2), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = info.title, color = if (info.isResolved) Emerald500 else if (info.isSirenDisabled) Amber500 else Color.White, fontSize = titleFontSize, fontWeight = FontWeight.Bold, lineHeight = titleFontSize * 1.2f)
                                if (showSubtitle) { Text(text = info.subtitle, color = Color.White.copy(alpha = 0.8f), fontSize = subtitleFontSize, fontWeight = FontWeight.Medium, lineHeight = subtitleFontSize * 1.3f) }
                            }
                            Spacer(Modifier.width(8.dp))
                            Icon(imageVector = if (info.isResolved) Icons.Default.CheckCircle else if (info.isSirenDisabled) Icons.Default.Info else Icons.Default.Error, contentDescription = null, tint = if (isMuted) Slate500 else if (info.isResolved) Emerald500 else if (info.isSirenDisabled) Amber500 else Rose500, modifier = Modifier.size(if (activeCount <= 6) 24.dp else 18.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (!isMuted && isSirenPlaying) {
                Button(onClick = onMute, modifier = Modifier.fillMaxWidth().height(75.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White), shape = RoundedCornerShape(24.dp)) { Text(stringResource(R.string.btn_mute_siren), color = Rose500, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
            } else {
                Button(onClick = onGoToMap, modifier = Modifier.fillMaxWidth().height(85.dp), colors = ButtonDefaults.buttonColors(containerColor = ViewerOrange.copy(alpha = 0.2f)), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, ViewerOrange.copy(alpha = 0.5f))) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Map, null, tint = ViewerOrange, modifier = Modifier.size(28.dp)); Text(stringResource(R.string.btn_go_to_map), color = ViewerOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
