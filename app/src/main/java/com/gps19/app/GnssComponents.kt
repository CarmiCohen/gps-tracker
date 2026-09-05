package com.gps19.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gps19.core.engine.*
import kotlinx.coroutines.flow.StateFlow
import java.util.*

/**
 * GnssComponents: Detailed satellite signal and constellation visualization.
 * Sep.04.95:
 * - Issue #914 HARDENED: GNSS UI Performance Optimization. Added stable keys to 
 *   LazyVerticalGrid items to prevent redundant recompositions on budget 
 *   hardware (Samsung A15).
 * v9.1.0:
 * - R799e: Swapped legacy BrandJd (#367C2B) for JD Vivid Green (#78BE20).
 */

@Composable
fun GnssDetailOverlay(
    gnssDetailFlow: StateFlow<GnssDetail?>,
    onClose: () -> Unit
) {
    val gnssDetail by gnssDetailFlow.collectAsStateWithLifecycle()
    
    Surface(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        color = Color.Black.copy(alpha = 0.9f)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GNSS SATELLITE DETAIL",
                    color = BrandJd,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            if (gnssDetail == null || gnssDetail!!.satellites.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("NO SATELLITE DATA AVAILABLE", color = Slate500, fontSize = 14.sp)
                }
            } else {
                val stats = gnssDetail!!.satellites
                val usedCount = stats.count { it.usedInFix }
                
                Text(
                    text = "TOTAL: ${stats.size}  |  USED: $usedCount",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                
                Spacer(Modifier.height(8.dp))
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Issue #914: Added stable keys based on SVID and constellation to optimize 
                    // budget hardware performance during high-frequency GNSS updates.
                    items(stats, key = { "${it.constellation}_${it.svid}" }) { sat ->
                        SatelliteCard(sat)
                    }
                }
            }
        }
    }
}

@Composable
fun SatelliteCard(sat: SatelliteInfo) {
    val color = if (sat.usedInFix) BrandJd else Slate500
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PRN: ${sat.svid}",
                    color = color,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${getConstellationName(sat.constellation)} | ${String.format(Locale.getDefault(), "%.1f", sat.cn0)} dB",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            if (sat.usedInFix) {
                Icon(
                    Icons.Default.GpsFixed,
                    null,
                    tint = BrandJd,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
