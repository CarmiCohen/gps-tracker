package com.gps19.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * Oct.14.17:
 * - A15 Hardening: Removed redundant background and status bar padding to 
 *   prevent layout conflicts with root safeDrawingPadding.
 */

@Composable
fun LandingScreen(isPeerActive: Boolean, onMode: (String) -> Unit) {
    val versionDisplay = BuildConfig.VERSION_NAME
    val viewerColor = if (isPeerActive) ViewerCyan else Slate500
    
    // Oct.14.17: Use standard Column without explicit background to allow 
    // root Surface color to show through, preventing "Black on Black" bugs.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        
        LandingButton(
            title = stringResource(R.string.landing_tracker_title),
            subtitle = stringResource(R.string.landing_tracker_subtitle),
            icon = Icons.Default.Agriculture,
            roleColor = BrandJd
        ) { onMode("tracker") }
        
        Spacer(Modifier.height(24.dp))
        
        LandingButton(
            title = stringResource(R.string.landing_viewer_title),
            subtitle = stringResource(R.string.landing_viewer_subtitle),
            icon = Icons.Default.Person,
            roleColor = viewerColor
        ) { onMode("viewer") }
        
        Spacer(Modifier.weight(1.2f))
        
        Text(
            text = versionDisplay,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun LandingButton(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, roleColor: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                Icon(icon, null, tint = roleColor, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, color = roleColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Slate400, fontSize = 12.sp)
            }
        }
    }
}
