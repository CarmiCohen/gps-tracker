package com.jd.core.shared

/**
 * The Sensor's one-time Phone Setup items (ISSUE-NOV10.8-O81): only what the Sensor needs. Shared with the Monitor,
 * which names the missing ones (ISSUE-NOV10.8-O83 part 2); [label] is the item's name on both phones.
 */
enum class SensorSetupItem(val label: String, val optional: Boolean = false) {
    PRECISE_LOCATION("Precise Location"),
    BACKGROUND_LOCATION("Location 'Allow all the time'"),
    MICROPHONE("Microphone"),
    NOTIFICATIONS("Notifications"),
    BATTERY_UNRESTRICTED("Battery 'Unrestricted'"),
    KEEP_WHEN_UNUSED("Keep the Sensor When Unused"),
    SAMSUNG_NEVER_SLEEPING("Samsung 'Never sleeping apps'"),
    NOTIFICATION_ACCESS("Notification Access (optional)", optional = true)
}

/**
 * What the running Sensor cannot do now although its Phone Setup allows it (ISSUE-NOV10.8-O83 part 2). [text] is the
 * Monitor's warning; [isMicrophone] faults matter only while noise detection is switched on.
 */
enum class SensorFault(val text: String, val isMicrophone: Boolean = false) {
    LOCATION_NEEDS_TAP("Location is off since the Sensor phone restarted: tap its notification or open the Sensor app"),
    MICROPHONE_NEEDS_TAP(
        "The microphone is off since the Sensor phone restarted: tap its notification or open the Sensor app",
        isMicrophone = true
    ),
    MICROPHONE_SILENCED("The microphone is silenced by the Sensor phone", isMicrophone = true),
    MICROPHONE_UNAVAILABLE("The Sensor phone's microphone is not available", isMicrophone = true)
}

/**
 * What the Sensor [trackerId] tells its Monitors about its phone (ISSUE-NOV10.8-O83 part 2): the required Phone Setup
 * items known to be [missing] (an optional item, or one Android does not let the app read, is never listed), the
 * [faults] of the running service, and [behaviourRevision], the revision of the detection settings it applies
 * (ISSUE-NOV15.2-O97). The Sensor leaves [trackerId] empty; the signaling client fills it. [sentAtMs] is the Sensor's
 * clock when the signaling client sent the report (0 from a Sensor before Nov18.12): the same report reaches the Monitor
 * by the relay connection and by push, and the later sent one wins (ISSUE-NOV10.8-O86).
 */
data class SensorSetupReport(
    val trackerId: String = "",
    val missing: List<SensorSetupItem> = emptyList(),
    val faults: List<SensorFault> = emptyList(),
    val behaviourRevision: Long = 0L,
    val sentAtMs: Long = 0L
)
