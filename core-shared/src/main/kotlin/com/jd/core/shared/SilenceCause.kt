package com.jd.core.shared

/**
 * Why a Sensor stopped sending rows, as the relay's silence notice tells the Monitor (ISSUE-NOV22.10-O137).
 * [wire] is the notice's `cause` text, which the relay copies from the Sensor's last word or writes itself.
 * [isForced] marks a deliberate disconnection of the Sensor, the Monitor's highest-priority alarm.
 */
enum class SilenceCause(val wire: String, val isForced: Boolean) {
    /** The relay heard nothing for its limit and had no last word: a lost connection, the "no connection" alarm. */
    SILENT("silent", isForced = false),

    /** The relay saw the Sensor's socket closed without a last word and no return: the app was killed. */
    CLOSED("closed", isForced = true),

    /** The owner pressed STOP: no alarm at all. */
    OWNER_STOP("owner_stop", isForced = false),

    /** The phone shut down with an empty battery: only the battery-low alarm. */
    BATTERY_EMPTY("battery_empty", isForced = false),

    /** The phone shut down, not from an empty battery. */
    SHUTDOWN("shutdown", isForced = true),

    /** Airplane mode was switched on. */
    AIRPLANE_MODE("airplane_mode", isForced = true),

    /** The SIM was removed while the Sensor used mobile data. */
    SIM_REMOVED("sim_removed", isForced = true),

    /** The Sensor service ended without the owner's STOP. */
    SERVICE_STOPPED("service_stopped", isForced = true);

    companion object {
        /** A missing or unknown cause, from a relay before v6.049, is a plain silence. */
        fun fromWire(text: String?): SilenceCause = entries.firstOrNull { it.wire == text } ?: SILENT

        /** The phone shut down at or below this battery level, unplugged, counts as an empty battery. */
        const val BATTERY_EMPTY_AT_PERCENT = 3

        /** The last word of a phone shutting down with [batteryLevel] (null: unknown) and [isPluggedIn]. */
        fun ofShutdown(batteryLevel: Int?, isPluggedIn: Boolean): SilenceCause =
            if (!isPluggedIn && batteryLevel != null && batteryLevel <= BATTERY_EMPTY_AT_PERCENT) BATTERY_EMPTY else SHUTDOWN
    }
}
