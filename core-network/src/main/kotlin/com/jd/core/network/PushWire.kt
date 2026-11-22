package com.jd.core.network

import com.jd.core.shared.LocationUpdate
import com.jd.core.shared.SensorSetupReport

/**
 * True when [senderId] is the paired tracker [trackerId]; an unpaired Monitor ([trackerId] null) takes nothing and
 * stays asleep. The one pairing rule for what reaches the Monitor by push or as a setup report (SIMP-NOV18.12-S1).
 */
fun isPairedTracker(senderId: String, trackerId: String?): Boolean = trackerId != null && senderId == trackerId

/** One message the relay pushed to the Monitor (ISSUE-NOV10.8-O86), as [PushWire.decode] reads it. */
sealed interface PushMessage {
    /** The Sensor the message is about. */
    val trackerId: String

    /** The Sensor's alarm row, pushed at high priority beside the relay connection. */
    data class Row(val row: LocationUpdate) : PushMessage {
        override val trackerId: String get() = row.deviceId
    }

    /** The relay's own notice that the Sensor went silent. */
    data class Silence(val silence: SensorSilence) : PushMessage {
        override val trackerId: String get() = silence.trackerId
    }

    /** The Sensor's changed phone setup report (relay v6.047), pushed at normal priority. */
    data class Setup(val report: SensorSetupReport) : PushMessage {
        override val trackerId: String get() = report.trackerId
    }
}

/**
 * The push data keys the relay writes, each holding one JSON text, and their single decoder (SIMP-NOV18.12-S1).
 * A push carries one key; should one ever carry more, the setup report is read first, then the row, then the notice.
 */
object PushWire {
    /** The Sensor's alarm row, as [LocationRowWire] writes it. */
    const val ROW_KEY = "row"

    /** What the relay noticed by itself, as [SensorSilenceWire] reads it. */
    const val NOTICE_KEY = "notice"

    /** The Sensor's changed setup report, as [SensorSetupWire] writes it (relay v6.047). */
    const val SETUP_KEY = "setup"

    /** Null when no key holds a readable message. */
    fun decode(data: Map<String, String>): PushMessage? =
        data[SETUP_KEY]?.let(SensorSetupWire::decodeText)?.let(PushMessage::Setup)
            ?: data[ROW_KEY]?.let(LocationRowWire::decodeText)?.let(PushMessage::Row)
            ?: data[NOTICE_KEY]?.let(SensorSilenceWire::decodeText)?.let(PushMessage::Silence)
}
