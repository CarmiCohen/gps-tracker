package com.jd.core.shared

/**
 * Test commands sent with `adb shell am broadcast` to the debug builds only (SIMP-NOV8.100-S1).
 * Each app has a receiver in its `src/debug` source set, so release builds contain no receiver.
 * Every command answers with one `REV_DEBUG_CMD` logcat line, read instead of a screenshot.
 *
 * Example: `adb shell am broadcast -n com.jd.app.monitor/.DebugCommandReceiver -a com.jd.debug.PAIR --es id T`
 */
sealed interface DebugCommand {
    data class SetSensorId(val id: String) : DebugCommand
    data class Pair(val id: String) : DebugCommand
    data object Unpair : DebugCommand
    data class AddHomePoint(val point: HomePoint) : DebugCommand
    data object ClearHomePoints : DebugCommand
    data object State : DebugCommand

    /** A command this app does not accept, or one with missing or invalid extras. */
    data class Rejected(val reason: String) : DebugCommand

    companion object {
        const val LOG_TAG = "DEBUG_CMD"
        const val ACTION_PREFIX = "com.jd.debug."
        const val SET_ID = ACTION_PREFIX + "SET_ID"
        const val PAIR = ACTION_PREFIX + "PAIR"
        const val UNPAIR = ACTION_PREFIX + "UNPAIR"
        const val ADD_HOME = ACTION_PREFIX + "ADD_HOME"
        const val CLEAR_HOME = ACTION_PREFIX + "CLEAR_HOME"
        const val STATE = ACTION_PREFIX + "STATE"

        val SENSOR_ACTIONS = setOf(SET_ID, STATE)
        val MONITOR_ACTIONS = setOf(PAIR, UNPAIR, ADD_HOME, CLEAR_HOME, STATE)

        /**
         * Parses a broadcast into a command. [extra] returns a string extra by name (sent with `--es`).
         * Actions outside [accepted] are rejected, so each app only runs its own commands.
         */
        fun parse(action: String?, accepted: Set<String>, extra: (String) -> String?): DebugCommand {
            if (action == null || action !in accepted) return Rejected("unsupported action $action")
            return when (action) {
                SET_ID -> idExtra(extra)?.let(::SetSensorId) ?: Rejected("missing id")
                PAIR -> idExtra(extra)?.let(::Pair) ?: Rejected("missing id")
                UNPAIR -> Unpair
                ADD_HOME -> homePoint(extra("lat"), extra("lng"))
                CLEAR_HOME -> ClearHomePoints
                STATE -> State
                else -> Rejected("unsupported action $action")
            }
        }

        private fun idExtra(extra: (String) -> String?): String? =
            extra("id")?.trim()?.takeIf { ForensicIdentity.isValid(it) }

        private fun homePoint(lat: String?, lng: String?): DebugCommand {
            val la = lat?.toDoubleOrNull()
            val lo = lng?.toDoubleOrNull()
            return if (la != null && lo != null && la in -90.0..90.0 && lo in -180.0..180.0) {
                AddHomePoint(HomePoint(la, lo))
            } else {
                Rejected("invalid lat/lng $lat,$lng")
            }
        }
    }
}
