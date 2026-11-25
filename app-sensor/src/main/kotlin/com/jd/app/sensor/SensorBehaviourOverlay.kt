package com.jd.app.sensor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jd.core.shared.SensorBehaviour
import com.jd.core.shared.lightLimitLabel
import com.jd.core.shared.noiseLimitLabel
import com.jd.core.shared.tiltLimitLabel
import com.jd.core.shared.vibrationLimitLabel
import com.jd.core.ui.BrandGold
import com.jd.core.ui.OverlayHeader
import com.jd.core.ui.Slate500
import com.jd.core.ui.Slate900
import com.jd.core.ui.StatusGreen

/** The line under the Sensor's read-only detection page (ISSUE-NOV10.8-O83). */
const val SENSOR_BEHAVIOUR_SET_ON_MONITOR_NOTE = "Set on the Monitor (Alerts & Sensors Setup) and received by relay."

/** Shown until the first message from the Monitor: the Sensor still uses its own earlier limits, or the defaults. */
const val SENSOR_BEHAVIOUR_NOT_RECEIVED_NOTE = "Not received from the Monitor yet: the Sensor uses its earlier limits."

/** One line of the read-only page, for example `Tilt: On, alert above 15°` or `Noise: Off`. */
fun sensorBehaviourLines(behaviour: SensorBehaviour): List<String> = listOf(
    "Sit: " + if (behaviour.sitAlert) "On" else "Off", // ISSUE-NOV25.22-O150
    behaviourLine("Vibration", behaviour.vibrationAlert, vibrationLimitLabel(behaviour.vibrationSensitivity)),
    behaviourLine("Tilt", behaviour.tiltAlert, tiltLimitLabel(behaviour.tiltSensitivity)),
    behaviourLine("Noise", behaviour.noiseAlert, noiseLimitLabel(behaviour.noiseSensitivity) + " over the background"),
    behaviourLine("Light", behaviour.lightAlert, lightLimitLabel(behaviour.lightSensitivity) + " the background"),
    "Lift: " + if (behaviour.liftAlert) "On" else "Off"
)

private fun behaviourLine(name: String, isOn: Boolean, limit: String): String =
    "$name: " + if (isOn) "On, alert above $limit" else "Off"

/** The Sensor's detection settings, read-only: the Monitor sets them (ISSUE-NOV10.8-O83, user decision 2026-11-15). */
@Composable
internal fun SensorBehaviourOverlay(behaviour: SensorBehaviour, onClose: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = Slate900) {
        Column(
            modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            OverlayHeader(title = "DETECTION SETTINGS", color = BrandGold, onClose = onClose)
            Spacer(Modifier.height(12.dp))
            sensorBehaviourLines(behaviour).forEach { line ->
                Text(line, color = Color.White, fontSize = 15.sp, modifier = Modifier.padding(vertical = 6.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(SENSOR_BEHAVIOUR_SET_ON_MONITOR_NOTE, color = StatusGreen, fontSize = 12.sp)
            if (behaviour.revision == 0L) {
                Spacer(Modifier.height(4.dp))
                Text(SENSOR_BEHAVIOUR_NOT_RECEIVED_NOTE, color = Slate500, fontSize = 12.sp)
            }
        }
    }
}
