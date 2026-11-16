package com.jd.app.monitor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jd.core.shared.InfoScreenState
import com.jd.core.shared.LinkStats
import com.jd.core.shared.LocationUpdate
import com.jd.core.shared.PhoneState
import com.jd.core.shared.ReceivedSensorInfo
import com.jd.core.shared.SENSOR_INFO_STALE_MS
import com.jd.core.shared.SpatialMath
import com.jd.core.shared.durationText
import com.jd.core.shared.monitorPhoneSection
import com.jd.core.shared.newestRowSection
import com.jd.core.shared.sensorInfoSections
import com.jd.core.ui.DeviceInfoOverlay

/** The Monitor's Info screen (ISSUE-NOV10.8-O84 part 2); open, it asks the Sensor for its Info. */
@Composable
fun MonitorInfoScreen(viewModel: MonitorViewModel, onClose: () -> Unit) {
    val state by viewModel.infoScreen.collectAsStateWithLifecycle()
    DeviceInfoOverlay(title = "INFO", state = state, onClose = onClose)
}

/**
 * What the Monitor's Info screen shows (user decisions 2026-11-15): the Sensor's own Info when one came (gray when it
 * is older than [SENSOR_INFO_STALE_MS]), else the Sensor's newest row, then a short section about this phone.
 */
fun monitorInfoScreen(
    received: ReceivedSensorInfo?,
    newestRow: LocationUpdate?,
    link: LinkStats,
    localFix: LocalFix?,
    phone: PhoneState,
    monitorId: String,
    version: String,
    nowMs: Long
): InfoScreenState {
    val info = received?.info
    val sensorPosition = when {
        info != null && info.fixTimeMs > 0L -> MapPosition(info.lat, info.lng)
        newestRow != null && (newestRow.lat != 0.0 || newestRow.lng != 0.0) -> MapPosition(newestRow.lat, newestRow.lng)
        else -> null
    }
    val distance = if (sensorPosition != null && localFix != null) {
        SpatialMath.calculateDistance(localFix.position.lat, localFix.position.lng, sensorPosition.lat, sensorPosition.lng)
    } else null
    val own = monitorPhoneSection(
        link = link,
        fixAccuracyM = localFix?.reading?.accuracyMeters,
        fixAgeMs = localFix?.reading?.timeMs?.let { nowMs - it },
        distanceToSensorM = distance,
        phone = phone,
        monitorId = monitorId,
        version = version,
        nowMs = nowMs
    )
    return when {
        received != null -> {
            val ageMs = nowMs - received.receivedAtMs
            val isStale = ageMs > SENSOR_INFO_STALE_MS
            InfoScreenState(
                sections = sensorInfoSections(received.info, isStale, SENSOR_TITLE_PREFIX) + own,
                satellites = received.info.satellites,
                note = if (isStale) "No Info from the Sensor for ${durationText(ageMs)}: the gray values are from then." else null
            )
        }
        newestRow != null -> InfoScreenState(
            sections = listOf(newestRowSection(newestRow, nowMs, isStale = nowMs - newestRow.timestamp > SENSOR_INFO_STALE_MS), own),
            note = WAITING_FOR_INFO_NOTE
        )
        else -> InfoScreenState(sections = listOf(own), note = NO_SENSOR_DATA_NOTE)
    }
}

internal const val SENSOR_TITLE_PREFIX = "SENSOR - "
internal const val WAITING_FOR_INFO_NOTE = "Waiting for the Sensor's Info: it answers while it is online."
internal const val NO_SENSOR_DATA_NOTE = "Nothing from the Sensor yet: it answers while it is online."
