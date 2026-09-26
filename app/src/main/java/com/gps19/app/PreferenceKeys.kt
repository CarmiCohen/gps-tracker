package com.gps19.app

/**
 * PreferenceKeys: Centralized keys for DataStore and SharedPreferences.
 * Sep.25.07:
 * - Issue #1329: Added LAST_VALID_FIX_RT_KEY for monotonic fix tracking parity.
 * Sep.23.70:
 * - Issue #1230: Added missing keys for logic state and background auditing (R-ID 453).
 */

const val APP_MODE_KEY = "app_mode"
const val TRACKER_ID_KEY = "tracker_id"
const val VIEWER_ID_KEY = "viewer_id"
const val RELAY_URL_KEY = "relay_url"

const val MAX_DISTANCE_STORAGE_KEY = "max_distance"
const val MAX_ACCURACY_KEY = "max_accuracy"
const val MAX_TEMP_KEY = "max_temp"
const val LAST_ALARM_ACK_TS_KEY = "last_alarm_ack_ts"
const val HOME_POINTS_TS_KEY = "home_points_ts"
const val IS_MANUAL_EXIT_KEY = "is_manual_exit"
const val APP_START_TIME_KEY = "app_start_time"
const val LAST_VERSION_CODE_KEY = "last_version_code"

const val TOTAL_CONNECTED_KEY = "total_connected"
const val UPTIME_KEY = "uptime"
const val LAST_CONNECTION_TS_KEY = "last_conn_ts"
const val LAST_DISCONNECTION_TS_KEY = "last_disc_ts"
const val TOTAL_DROP_KEY = "total_drop"
const val MAX_DROP_KEY = "max_drop"
const val MAX_DROP_TS_KEY = "max_drop_ts"
const val LAST_GPS_TS_KEY = "last_gps_ts"
const val VIOLATION_UPTIME_MS_KEY = "violation_uptime_ms"

const val TRACKER_LUX_BASELINE_KEY = "tracker_lux_baseline"
const val TRACKER_ACOUSTIC_FLOOR_KEY = "tracker_acoustic_floor"

const val IS_MIC_TYPE_STARTED_KEY = "is_mic_type_started"

const val SELECTED_SIREN_KEY = "selected_siren"
const val LAST_SERVICE_TICK_TS_KEY = "last_service_tick_ts"
const val LAST_SERVICE_TICK_REALTIME_KEY = "last_service_tick_realtime"
const val LAST_AUTO_SAVE_HOUR_KEY = "last_auto_save_hour"

const val LAST_DAILY_ARCHIVE_DATE_KEY = "last_daily_archive_date"
const val LAST_DAILY_CLEANUP_DATE_KEY = "last_daily_cleanup_date"

const val DRAFT_TRACKER_ID = "draft_tracker_id"
const val DRAFT_VIEWER_ID = "draft_viewer_id"
const val DRAFT_RELAY_URL = "draft_relay_url"
const val DRAFT_MAX_DISTANCE = "draft_max_distance"

const val IS_XIAOMI_MANUAL_OVERRIDE_KEY = "is_xiaomi_manual_override"
const val IS_SYSTEM_ACTIVE_KEY = "is_system_active"

const val IDENTITY_SANITIZED_KEY = "identity_sanitized"
const val CLOCK_DRIFT_REF_KEY = "clock_drift_ref"

const val LAST_SIT_TS_KEY = "last_sit_ts"
const val CHAIR_BASELINE_TILT_KEY = "chair_baseline_tilt"
const val LAST_HISTORY_SIT_TS_KEY = "last_history_sit_ts"
const val ADAPTIVE_VIBRATION_FLOOR_KEY = "adaptive_vibration_floor"

const val LAST_ALARMS_JSON_KEY = "last_alarms_json"
const val LAST_VALID_FIX_RT_KEY = "last_valid_fix_rt"

// Issue #626: Foreground Service Start Hardening
const val IS_RECOVERY_PENDING_KEY = "is_recovery_pending"

// Issue #629: Deferred Recovery Latency Audit
const val RECOVERY_BLOCKED_TS_KEY = "recovery_blocked_ts"

// Issue #630: Forensic Recovery Log Aggregation
const val CUMULATIVE_RECOVERY_BLACKOUT_MS_KEY = "cumulative_recovery_blackout_ms"
const val RECOVERY_COUNT_KEY = "recovery_count"

// Issue #1164: Logic State Persistence Keys
const val FIRST_VIOLATION_TS_KEY = "first_violation_ts"
const val FIRST_VIOLATION_RT_KEY = "first_violation_rt"
const val FIRST_VIOLATION_WAS_JUMP_KEY = "first_violation_was_jump"
const val DISTANCE_VIOLATION_COUNTER_KEY = "distance_violation_counter"
const val WAS_DISTANCE_VIOLATED_KEY = "was_distance_violated"
const val POWER_ALARM_PENDING_KEY = "power_alarm_pending"
const val LAST_SIREN_STOP_RT_KEY = "last_siren_stop_rt"
const val LAST_GLOBAL_TRIGGER_RT_KEY = "last_global_trigger_rt"
const val FORENSIC_RELIABILITY_DEGRADATION_START_RT_KEY = "forensic_reliability_degradation_start_rt"

// Background Audit Keys
const val LAST_INTEGRITY_CHECK_TS_KEY = "last_integrity_check_ts"
