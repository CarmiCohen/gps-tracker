package com.jd.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jd.core.shared.InfoLine
import com.jd.core.shared.InfoScreenState
import com.jd.core.shared.InfoSection
import com.jd.core.shared.InfoTone
import com.jd.core.shared.SatelliteReading
import com.jd.core.shared.constellationName
import java.util.Locale

/**
 * The menu's Info screen of both apps (ISSUE-NOV10.8-O84 part 2): the detailed, live state of the phones in titled
 * sections, warnings in amber or red, old values in gray. The Satellites line opens the satellite list;
 * [onSatelliteListShown] tells when it opens and closes, as the Monitor asks for it only then (ISSUE-NOV15.5-O99 part 1).
 */
@Composable
fun DeviceInfoOverlay(title: String, state: InfoScreenState, onClose: () -> Unit, onSatelliteListShown: (Boolean) -> Unit = {}) {
    var showSatellites by rememberSaveable { mutableStateOf(false) }
    val currentOnShown by rememberUpdatedState(onSatelliteListShown)
    DisposableEffect(showSatellites) {
        currentOnShown(showSatellites)
        onDispose { if (showSatellites) currentOnShown(false) }
    }
    if (showSatellites) SatelliteListDialog(state.satellites, state.isSatelliteListPending, onClose = { showSatellites = false })

    // English labels and numbers: a right-to-left phone language must not mirror them.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Surface(modifier = Modifier.fillMaxSize(), color = Slate900, contentColor = Color.White) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
                OverlayHeader(title = title, color = ViewerCyan, onClose = onClose, fontSize = 18.sp)
                state.note?.let { Text(it, color = BrandGold, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp)) }
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    state.sections.forEach { section ->
                        item(key = "title:${section.title}") { SectionTitle(section) }
                        items(section.lines, key = { "${section.title}:${it.label}" }) { line ->
                            InfoLineRow(line, isStale = section.isStale, onOpenSatellites = { showSatellites = true })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(section: InfoSection) {
    Text(
        section.title + if (section.isStale) " (OLD)" else "",
        color = if (section.isStale) Slate500 else ViewerCyan,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
    )
}

@Composable
private fun InfoLineRow(line: InfoLine, isStale: Boolean, onOpenSatellites: () -> Unit) {
    val opens = line.opensSatellites
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .background(Color.White.copy(alpha = if (opens) 0.08f else 0.03f))
            .then(if (opens) Modifier.clickable(onClick = onOpenSatellites) else Modifier)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(line.label, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.weight(0.4f))
        Text(
            line.value + if (opens) "  ›" else "",
            color = if (isStale) Slate500 else toneColor(line.tone),
            fontSize = 12.sp,
            fontWeight = if (line.tone == InfoTone.WARN || line.tone == InfoTone.BAD) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.6f)
        )
    }
}

/** The satellites in view, those used in the fix first, strongest first, as in the pre-split GNSS detail. */
@Composable
private fun SatelliteListDialog(satellites: List<SatelliteReading>, isPending: Boolean, onClose: () -> Unit) {
    val sorted = remember(satellites) { satellites.sortedWith(compareByDescending<SatelliteReading> { it.usedInFix }.thenByDescending { it.cn0DbHz }) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (isPending) "Satellites" else "Satellites: ${satellites.count { it.usedInFix }} used / ${satellites.size} in view") },
        text = {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                if (isPending) {
                    Text("Waiting for the Sensor's satellite list...")
                } else if (sorted.isEmpty()) {
                    Text("No satellite in view")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                        items(sorted, key = { "${it.constellation}_${it.svid}" }) { sat ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                Text("%3d".format(Locale.US, sat.svid), fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.width(36.dp))
                                Text(constellationName(sat.constellation), fontSize = 12.sp, modifier = Modifier.weight(1f))
                                Text("%.1f dB-Hz".format(Locale.US, sat.cn0DbHz), fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.width(88.dp))
                                Text(
                                    if (sat.usedInFix) "used" else "",
                                    color = StatusGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(40.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Close") } }
    )
}

private fun toneColor(tone: InfoTone): Color = when (tone) {
    InfoTone.NORMAL -> Color.White
    InfoTone.GOOD -> StatusGreen
    InfoTone.WARN -> BrandGold
    InfoTone.BAD -> Rose500
}
