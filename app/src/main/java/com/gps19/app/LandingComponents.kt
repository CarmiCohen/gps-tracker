package com.gps19.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gps19.core.engine.*

/**
 * LandingComponents: Initial role selection screens.
 * Extracted from OverlayComponents for Issue 115 modularization.
 */

@Composable
fun LandingScreen(onMode: (String) -> Unit) {
    val versionDisplay = BuildConfig.VERSION_NAME
    Column(modifier = Modifier.fillMaxSize().background(Slate950).statusBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f)); LandingButton(stringResource(R.string.landing_tracker_title), stringResource(R.string.landing_tracker_subtitle), Icons.Default.Agriculture, Lime500) { onMode("tracker") }
        Spacer(Modifier.height(24.dp)); LandingButton(stringResource(R.string.landing_viewer_title), stringResource(R.string.landing_viewer_subtitle), Icons.Default.Person, ViewerOrange) { onMode("viewer") }
        Spacer(Modifier.weight(1.2f)); Text(versionDisplay, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun LandingButton(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, roleColor: Color, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().height(100.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = Slate900)) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.padding(16.dp)) { Icon(icon, null, tint = roleColor, modifier = Modifier.size(48.dp)) }
            Spacer(Modifier.width(16.dp)); Column { Text(title, color = roleColor, fontSize = 22.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = Slate500, fontSize = 12.sp) }
        }
    }
}
