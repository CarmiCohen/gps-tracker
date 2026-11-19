package com.jd.app.monitor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jd.core.shared.AlertSettings
import com.jd.core.shared.SensorSetupItem
import com.jd.core.shared.SensorSetupReport

/**
 * What the Monitor user must fix on the Sensor phone, one line each, from the Sensor's setup [report]
 * (ISSUE-NOV10.8-O83 part 2); empty while no report has arrived. A microphone problem is no warning while noise
 * detection is switched off here: the Sensor keeps its microphone closed then.
 */
fun sensorSetupWarnings(report: SensorSetupReport?, settings: AlertSettings): List<String> {
    report ?: return emptyList()
    val isNoiseOn = settings.acousticAlert
    return report.missing.filter { isNoiseOn || it != SensorSetupItem.MICROPHONE }.map { "${it.label}: missing" } +
        report.faults.filter { isNoiseOn || !it.isMicrophone }.map { it.text }
}

/** The warning line under the Monitor's status lines. */
fun sensorSetupLineText(warningCount: Int): String =
    if (warningCount == 1) "SENSOR PHONE: 1 ITEM NEEDS ATTENTION" else "SENSOR PHONE: $warningCount ITEMS NEED ATTENTION"

/** The Sensor phone line of the Monitor Setup dialog. */
fun sensorSetupStatusText(report: SensorSetupReport?, warningCount: Int): String = "Sensor phone setup: " + when {
    report == null -> "not reported yet"
    warningCount == 0 -> "complete"
    else -> "$warningCount to fix"
}

/**
 * The detection settings line of the Monitor Setup dialog (ISSUE-NOV15.2-O97): whether the Sensor applies the
 * revision this Monitor holds ([AlertSettings.sensorRevision]).
 */
fun sensorBehaviourStatusText(report: SensorSetupReport?, sensorRevision: Long): String = "Sensor detection settings: " + when {
    report == null -> "not reported yet"
    report.behaviourRevision == sensorRevision -> "applied"
    report.behaviourRevision > sensorRevision -> "newer ones from another Monitor are in use"
    else -> "not applied yet"
}

/** Shown under the status lines while the Sensor phone lacks something; a tap lists what. */
@Composable
fun SensorSetupWarningLine(warningCount: Int, onClick: () -> Unit) {
    Surface(color = SETUP_WARNING_COLOR, contentColor = Color.Black, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = sensorSetupLineText(warningCount),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Text("DETAILS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** The list behind [SensorSetupWarningLine]. Android lets these be granted only on the Sensor phone itself. */
@Composable
fun SensorSetupWarningDialog(warnings: List<String>, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Sensor Phone Needs Attention") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                warnings.forEach { Text("- $it") }
                Text("Fix these on the Sensor phone: open the Sensor app, then PHONE SETUP.", fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = { Button(onClick = onClose) { Text("Close") } }
    )
}

private val SETUP_WARNING_COLOR = com.jd.core.ui.BrandGold
