package com.gps19.app

import com.gps19.core.engine.*

/**
 * Constants: Centralized configuration values and system thresholds.
 * v8.8.35:
 * - Issue 156: Synchronized version strings to v8.8.35 baseline.
 * - Forensic Simplification: Reflects removal of 'ver' and 'vid' tags from data stream.
 */

// Global App Defaults
const val DEFAULT_RELAY_URL = "https://gps-survival-relay.onrender.com"

// Modernized Actions & Extras
const val ACTION_NAVIGATE_TO_MAP = "com.gps19.app.ACTION_NAVIGATE_TO_MAP"
const val ACTION_ALARM_WAKEUP = "com.gps19.app.ACTION_ALARM_WAKEUP"
const val ACTION_RELAY_STATUS = "com.gps19.app.ACTION_RELAY_STATUS"
const val ACTION_SUSPICIOUS_STATE_CHANGE = "com.gps19.app.ACTION_SUSPICIOUS_STATE_CHANGE"
const val EXTRA_MODE = "com.gps19.app.EXTRA_MODE"

// Forensic Logs
const val LOG_COROUTINE_FAILURE = "CRITICAL: Coroutine failure in Service: %s"
const val LOG_CHAIR_CALIBRATED = "Passive Zeroing: Chair baseline calibrated to %s°"
const val LOG_SIT_DETECTED = "Sit Detected (Engine Pulse)"
const val LOG_SENSOR_HARDWARE_FAILURE = "CRITICAL: SENSOR_HARDWARE_FAILURE - %s"
const val LOG_SYSTEM_INITIALIZED = "System Initialized"
const val LOG_ACOUSTIC_HYSTERESIS = "Acoustic Hysteresis: Mic icon will linger for 45s to ensure session stability."
const val LOG_SENSORS_STARTED_TRACKER = "Sensors started (Tracker Mode)"
const val LOG_SENSORS_STOPPED_VIEWER = "Sensors stopped (Viewer Mode)"
const val LOG_SUSPICIOUS_ACTIVATED = "Suspicious mode activated: %s"
const val LOG_SUSPICIOUS_DEACTIVATED = "Suspicious mode deactivated"
const val LOG_TRAJECTORY_CONFIRMED = "Trajectory confirmed"
const val LOG_SAFETY_FLUSH = "Safety-flush: Pending settings committed"
const val LOG_UI_PULSE_STABILITY = "UI Pulse Stability: Timeout detected while in foreground (Jitter > %dms). Throttling down."
const val LOG_SYSTEM_HEARTBEAT = "SYSTEM HEARTBEAT (%s) | Process: %s | Cumulative: %s"
const val LOG_VIEWER_CONNECTED = "Viewer connected: %s"
const val LOG_SESSION_TERMINATED = "Session Terminated"
const val LOG_SYSTEM_SHUTDOWN = "System Shutdown"
const val LOG_SENSORS_STOPPED = "Sensors stopped"
const val LOG_SYNC_FLUSH = "Sync: Flushing %d offline status updates..."
const val LOG_SYNC_FAILED = "Sync: Flush failed: %s"
const val LOG_MUZZLE_STARTUP_MS = 10000L

// Network
const val NETWORK_TIMEOUT_MS = 10000
const val NET_REJOIN_THRESHOLD_MS = 15000L
const val NET_HEAL_THRESHOLD_MS = 45000L
const val PING_INTERVAL_MS = 10000L
const val MAX_ALLOWED_RTT_MS = 5000
const val DEFAULT_SIGNAL_STRENGTH = 10
const val SOCKET_TIMEOUT_MS = 60000
const val SOCKET_RECONNECT_DELAY_MS = 2000L
const val SOCKET_RECONNECT_DELAY_MAX_MS = 60000L
const val SOCKET_RANDOMIZATION_FACTOR = 0.5
const val CONFLATION_DELAY_MS = 100L
const val RTT_WINDOW_SIZE = 5
const val COMM_RTT_FLOOR_MS = 150
const val COMM_RTT_SCALING_FACTOR = 2000.0

// Trajectory & Persistence
const val HOME_POINT_REFRESH_INTERVAL_MS = 30000L

// Logging Intervals
const val LOG_INTERVAL_FAST_MS = 15000L
const val LOG_INTERVAL_SLOW_MS = 60000L
const val LOG_INTERVAL_COUNT_THRESHOLD = 30

// History Manager Tasks
const val MAX_HISTORY_POINTS_PER_RIBBONS = 240
const val DAILY_CLEANUP_HOUR = 2
val DAILY_CLEANUP_DATE_KEY = "last_daily_cleanup_date"
const val DAILY_ARCHIVE_HOUR = 3

// Map & Performance
const val ALARM_ACK_RESET_MS = 2000L
const val SILENCE_TIMEOUT_MS = 300000L
const val WAKELOCK_TIMEOUT_MS = 600000L
const val PERMISSION_REFRESH_INTERVAL_SLOW_MS = 5000L
const val PERMISSION_REFRESH_INTERVAL_FAST_MS = 500L
const val SILENCE_AUTO_RECOVERY_MS = 300000L
const val FLOW_SHARING_TIMEOUT_MS = 5000L
