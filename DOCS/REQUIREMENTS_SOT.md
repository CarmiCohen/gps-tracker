# System Source of Truth (SoT) - v8.9.27

This document serves as the definitive operational specification for the GPS-Tracker system.

## 1. Core Integrity Constants & Thresholds
| Constant | Value | Description |
| :--- | :--- | :--- |
| `EARTH_RADIUS_METERS` | 6371000.0 | Physics constant for spherical calculations. |
| `LAT_DEG_TO_METERS` | 111194.92664455874 | Conversion factor at the equator. (Issue #228) |
| `DEFAULT_LAT` | 32.7940 | Default system latitude (Haifa). |
| `DEFAULT_LNG` | 34.9896 | Default system longitude (Haifa). |
| `ABSOLUTE_DISTANCE_CAP_METERS` | 50000.0m | Safety ceiling for distance calculations. |
| `BATTERY_ALARM_THRESHOLD` | 99% | Trigger for Charge Deficit when plugged. |
| `CRITICAL_BATTERY_THRESHOLD` | 20% | Trigger for Low Battery when unplugged. |
| `BATTERY_STEEP_DISCHARGE_THRESHOLD` | 5% | Trigger for Battery Health warning. |
| `BATTERY_STEEP_DISCHARGE_WINDOW_MS` | 600,000ms | 10-minute window for profiling steep discharge. |
| `MAX_SAFE_TEMPERATURE_CELSIUS`| 46.0°C | Thermal limit. Triggers "Cooling Mode". |
| `MAX_SAFE_TEMPERATURE_RECOVERY`| 44.0°C | Temperature for recovery from "Cooling Mode". |
| `GPS_GAP_THRESHOLD_MS` | 60,000ms | Threshold for Tracker GPS Gap alarm (v8.9.10). |
| `TRACKER_SIGNAL_LOSS_THRESHOLD_MS`| 180,000ms | Threshold for Tracker Signal Lost (Communication). |
| `VIEWER_SIGNAL_LOSS_THRESHOLD_MS`| 30,000ms | Threshold for Viewer Signal Lost (Communication). |
| `JAMMER_DETECTION_THRESHOLD_MS`| 180,000ms | Threshold for Jump Alert (sustained signal instability). |
| `INTERNET_LOSS_THRESHOLD_MS` | 60,000ms | Threshold for Viewer Local Internet loss. |
| `WATCH_TIMEOUT_MS` | 30,000ms | Deadline for peer activity. |
| `SYSTEM_WATCHDOG_INTERVAL_MS`| 90,000ms | Main system watchdog cycle for service health. |
| `SYSTEM_WATCHDOG_THROTTLE_MS`| 60,000ms | Throttling window for system watchdog re-triggers. |
| `GPS_UI_FAIL_THRESHOLD_MS` | 10,000ms | UI Staleness threshold for Position health (v8.9.10). |
| `TELEMETRY_UI_STALE_THRESHOLD_MS`| 10,000ms | Threshold for "Ghost Mode" visual dimming (v8.9.10). |
| `WATCH_DOG_UI_GRACE_MS` | 30,000ms | UI Staleness threshold for Link health. |
| `SENSOR_GRACE_PERIOD_MS` | 600,000ms | UI Visibility cutoff (10m). Values revert to -- after this period. |
| `COMMUNICATION_ALARM_GRACE_PERIOD_MS`| 60,000ms | Grace period for network-related peer alarms. |
| `LOCATION_ALARM_GRACE_PERIOD_MS`| 30,000ms | Grace period for location-related peer alarms. |
| `SIREN_AUTO_STOP_MS` | 45,000ms | Automatic siren cutoff to prevent battery drain. |
| `SIREN_RESUME_COOLDOWN_MS` | 15,000ms | Cooldown period before a siren can re-trigger. |
| `ALARM_OVERLAY_THROTTLE_MS` | 30,000ms | Mandatory lockout period for red-screen Activity re-launches. |
| `HEARTBEAT_INTERVAL_MS` | 3,600,000ms | Hourly service heartbeat log for forensic continuity. |
| `GEOFENCE_BUFFER_MULT` | 6.0 | Confidence multiplier for dynamic geofence gate (6-sigma). |
| `GEOFENCE_HYSTERESIS_METERS`| 5.0m | Hysteresis for geofence return logic. |
| `GEOFENCE_PREDICTIVE_LOOKAHEAD_S`| 2.0s | Speed-aware look-ahead for predictive geofence breach. |
| `GEOFENCE_PREDICTIVE_MIN_SPEED_MPS`| 1.0 m/s | Speed floor for predictive look-ahead markers. |
| `GEOFENCE_ACCURACY_EXPANSION_MULT`| 1.0 | Prioritizes safety over jitter in geofence calculations. |
| `GEOFENCE_ACCURACY_HYSTERESIS_MULT`| 1.10 | Margin for accuracy recovery. |
| `MONOTONIC_JITTER_TOLERANCE_MS`| 2,000ms | Suppression threshold for hardware clock stutters. |
| `CLOCK_REGRESSION_GATE_MS` | 100ms | Minimum jump to trigger clock regression protection. |
| `POWER_DISCONNECT_DEBOUNCE_MS` | 3,000ms | Confirmation window for unplugged event. |
| `STATIONARY_GPS_POLLING_MS` | 20,000ms | Standard background polling interval when stationary. |
| `MOVING_GPS_POLLING_MS` | 2,000ms | High-frequency polling interval when motion is detected. |
| `SUSPICIOUS_GPS_POLLING_MS` | 1,000ms | Max-frequency polling during security violations. |
| `HIGH_FREQUENCY_GPS_POLLING_MS`| 100ms | 10Hz specialized polling for Xiaomi/Samsung devices. |
| `A15_STABLE_GPS_POLLING_MS` | 1,000ms | Stabilization polling interval for Samsung A15. |
| `COOLING_GPS_POLLING_MS` | 30,000ms | Throttled interval for thermal mitigation. |
| `VIEWER_GPS_POLLING_MS` | 10,000ms | Forced polling interval in Viewer mode. |
| `TICK_INTERVAL_MS` | 1,000ms | Standard system pulse (1Hz). |
| `TICK_INTERVAL_SLOW_MS` | 5,000ms | Power-save pulse (0.2Hz) for background idle. |
| `REAL_TIME_GAP_LIMIT_MS` | 10,000ms | Forensic gap threshold for slow-tick variance. |
| `JUMP_HOLD_DURATION_MS` | 180,000ms | Grace period for Tier 1/2 Jumps before geofence alarm. |
| `MAX_TRACTOR_ACCEL` | 2.0 m/s² | Physical limit for acceleration monitoring. |
| `FGS_STICKY_DELAY_MS` | 45,000ms | Hysteresis for Mic Active icon stability. |
| `INITIAL_VIBRATION_FLOOR` | 0.05g | Baseline floor for vibration noise floor. |
| `VIBRATION_STATIONARY_THRESHOLD`| 0.12g | Threshold for transition to Parking/Stationary state. |
| `STATIONARY_FLOOR_MULT` | 1.5x | Multiplier for vibration stationary gate. |
| `PROXIMITY_DEBOUNCE_STATIONARY_A15_MS`| 5,000ms | Virtual proximity debounce window for Samsung A15. |
| `PROXIMITY_DEBOUNCE_STATIONARY_MS` | 3,000ms | Standard proximity debounce window. |
| `PROXIMITY_DEBOUNCE_MOVING_MS` | 1,000ms | Reduced proximity debounce window when moving. |
| `GPS_STALL_THRESHOLD_MS` | 60,000ms | Watchdog threshold for hardware GPS chip freeze (v8.9.10). |
| `GPS_REVIVAL_RETRY_INTERVAL_MS` | 120,000ms | Interval between hardware revival attempts during stall (v8.9.10). |
| `MAX_REVIVAL_ATTEMPTS` | 3 | Limit for soft-revival before critical hardware lock alarm. |
| `SYSTEM_STORAGE_LOW_THRESHOLD_MB` | 50MB | Threshold for non-critical storage warning. |
| `SYSTEM_STORAGE_CRITICAL_THRESHOLD_MB` | 10MB | Threshold for critical storage emergency. |
| `ACOUSTIC_LOCKOUT_MS` | 1,000ms | Lockout period for acoustic triggers. |
| `ACOUSTIC_MIN_THRESHOLD_DB` | 50.0dB | Absolute floor gate for acoustic violations. |
| `SENSOR_WARMING_MS` | 5,000ms | Initial warming phase for sensor baselines. |
| `DRIFT_TOLERANCE_MS` | 5,000ms | Forensic threshold for Clock-Jump detection. |
| `TRAJECTORY_PROMOTION_WINDOW_MS`| 30,000ms | Window for consistent movement to override Jump rejection. |
| `DISTANCE_ALARM_SAMPLES_REQUIRED`| 6 | Required consecutive samples for distance violation. |
| `OUTLIER_DISTANCE_THRESHOLD` | 2000.0m | Tier 1 Outlier distance floor. |
| `OUTLIER_SPEED_CAP_MPS` | 83.33 m/s| Tier 1 Outlier speed_cap (300 km/h). |
| `JUMP_POINT_DISTANCE_THRESHOLD`| 100.0m | Tier 2 Security Jump distance floor. |
| `JUMP_GATE_VISUAL_JITTER_METERS`| 10.0m | Tier 3 Visual Jitter distance floor. |
| `MAX_PHYSICAL_SPEED_MPS` | 33.33 m/s| Tier 2 Security speed limit (120 km/h). |
| `PATH_EFFICIENCY_THRESHOLD` | 0.1 | Radial noise filter for trajectory promotion. |
| `VIBRATION_SHOCK_THRESHOLD_G` | 0.8g | Absolute floor for shock violation gate. |
| `VIBRATION_SUSPICIOUS_THRESHOLD_G` | 0.25g | Absolute floor for vibration suspicion gate. |
| `VIBRATION_SHOCK_MULTIPLIER` | 7.0x | Multiplier for adaptive vibration shock gate. |
| `VIBRATION_SUSPICIOUS_MULTIPLIER`| 2.5x | Multiplier for adaptive vibration suspicion gate. |
| `TILT_THRESHOLD_DEGREES` | 15.0° | Maximum allowed tilt before violation. |
| `THROTTLE_TILT_LIMIT` | 5.0° | Threshold for tilt suspicion/throttling. |
| `BARO_LIFT_THRESHOLD_METERS` | 0.8m | Vertical displacement threshold for lift violation. |
| `BARO_ZEROING_INTERVAL_MS` | 600,000ms | Interval for periodic barometric zeroing. |
| `ACOUSTIC_THRESHOLD_DB_JUMP` | 40.0dB | Relative acoustic jump threshold for violation. |
| `ACOUSTIC_SUSPICIOUS_THRESHOLD_DB_JUMP` | 20.0dB | Relative acoustic jump threshold for suspicion. |
| `CHAIR_SIT_TILT_THRESHOLD` | 7.0° | Relative tilt delta for chair occupancy. |
| `CHAIR_SIT_VIBRATION_THRESHOLD` | 0.35g | Vibration peak for chair occupancy. |
| `CHAIR_SIT_BARO_THRESHOLD` | 0.08m | Barometric displacement for chair occupancy. |
| `CHAIR_PLUNGE_VELOCITY_THRESHOLD` | 0.18 m/s | Downward velocity threshold for plunge-matching. |
| `CHAIR_PLUNGE_DISTANCE_THRESHOLD`| 0.05m | Minimum distance for a valid plunge event. |
| `CHAIR_PLUNGE_WINDOW_MS` | 800ms | Window for matching downward velocity. |
| `CHAIR_SIT_COOLDOWN_MS` | 5,000ms | Cooldown between chair occupancy events. |
| `BOOTSTRAP_PHASE_MS` | 60,000ms | Duration of initial high-sensitivity bootstrap phase. |
| `DISCOVERY_PHASE_MS` | 60,000ms | Duration of peer discovery phase. |
| `PASSIVE_ZEROING_STATIONARY_MS` | 300,000ms | Stationary state required for auto-calibration (5m). |
| `MARKER_POOL_PRUNE_THRESHOLD` | 50 | Maximum visual markers before pruning. |
| `UI_PULSE_TIMEOUT_MS`| 5,000ms | Hysteresis for UI pulse stability. |
| `ACCURACY_WINDOW_BUCKET_MS` | 60,000ms | Window duration for accuracy high-water tracking. |
| `ACCURACY_WINDOW_MAX_SIZE` | 4 | Number of 15s buckets in sliding window. |
| `TEST_ALARM_DURATION_MS` | 3,000ms | Duration for the manual test siren. |
| `MOVING_HOLD_DURATION_MS` | 60,000ms | Anti-flapping hold for MOVING state. |
| `DAILY_ARCHIVE_HOUR` | 3 | Hour (0-23) for daily archive tasks. |
| `DAILY_ARCHIVE_MINUTE` | 30 | Minute for daily archive tasks. |
| `DAILY_CLEANUP_HOUR` | 2 | Hour (0-23) for daily cleanup tasks. |
| `DAILY_CLEANUP_MINUTE` | 5 | Minute for daily cleanup tasks. |
| `DB_PRUNE_THRESHOLD` | 50 writes | Counter threshold for triggering database pruning. |
| `HISTORY_BATCH_WRITE_INTERVAL_MS`| 5,000ms | Interval for flushing history buffer to database. |
| `HISTORY_BUFFER_MAX_SIZE` | 100 points| Max buffer size for history points. |
| `SUSPICIOUS_STATE_COOLDOWN_MS` | 60,000ms | Duration of the elevated suspicious state after a trigger. |
| `MUZZLE_WINDOW_DURATION_MS` | 2,000ms | Suppression window for physical triggers during sync I/O (v8.9.10). |
| `MUZZLE_HYSTERESIS_MS` | 200ms | Standard muzzle hysteresis (v8.9.10). |
| `MUZZLE_HYSTERESIS_A15_MS` | 500ms | Samsung A15 specific muzzle hysteresis (v8.9.10). |
| `LOG_MUZZLE_STARTUP_MS` | 10,000ms | Suppression of non-important system logs during service boot. |
| `XIAOMI_BOOT_GRACE_MS` | 30,000ms | Suppression of "System Not Ready" alarms during MIUI boot (v8.9.10). |
| `LIGHT_THRESHOLD_LUX_JUMP` | 150.0 lux | Threshold for light-based tamper alerts. |
| `SUSPICIOUS_Q_SCALE` | 1000.0 | Measurement noise multiplier during suspicious states. |
| `HIGH_ACCURACY_THRESHOLD_METERS`| 35.0m | Threshold for "High Accuracy" status classification. |
| `ACTIVE_MOVE_THRESHOLD` | 2.0m | Minimum displacement to trigger movement logic. |
| `GPS_SAVE_INTERVAL_MS` | 60000ms | Interval for periodic persistent GPS state storage. |
| `PARKING_ANCHOR_MIN_DIST` | 20.0m | Minimum radius for parking anchor establishment. |
| `THROTTLE_LUX_LIMIT` | 50.0 lux | Threshold for thermal/power polling throttling (Light). |
| `THROTTLE_ACOUSTIC_LIMIT`| 15.0 dB | Threshold for thermal/power polling throttling (Audio). |
| `THROTTLE_COMPASS_LIMIT` | 5.0° | Threshold for thermal/power polling throttling (Compass). |
| `THROTTLE_BARO_LIMIT` | 0.5m | Threshold for thermal/power polling throttling (Baro). |
| `ALERT_TRIGGER_GRACE_PERIOD_MS`| 2000ms | Anti-flood grace period between alert triggers. |
| `DEFAULT_ACCURACY_FALLBACK` | 15.0m | Default accuracy value when hardware returns 0. |
| `GPS_SEQUENCE_TOLERANCE_MS` | 60000ms | Maximum age for GPS sequence continuity. |
| `GPS_MIN_UPDATE_DISTANCE_METERS`| 2.0m | Minimum displacement for repository updates. |
| `PARKING_ACCEL_LIMIT` | 1.0 m/s² | Accel limit for parking confirmation. |
| `ALTITUDE_VELOCITY_CAP` | 10.0 m/s | Maximum vertical velocity allowed. |
| `PROMOTION_ANGLE_TOLERANCE` | 30.0° | Angle tolerance for trajectory promotion. |
| `COMPASS_STABILITY_THRESHOLD` | 20.0° | Stability required for compass-based movement. |
| `WILD_JUMP_THRESHOLD_METERS` | 500,000.0m | Ceiling for extreme GPS outliers. |
| `PARKING_ANCHOR_FACTOR` | 0.8 | Decay factor for parking anchor stability. |
| `RETURN_TO_SAFE_RANGE_ACCURACY_LIMIT` | 20.0m | Accuracy required to resolve distance violations. |
| `GPS_STABILITY_AUDIT_INTERVAL_MS` | 10,000ms | Interval for periodic GPS performance evaluation. |
| `GPS_STABILITY_GAP_THRESHOLD_MS` | 200ms | Maximum fix-to-fix gap allowed before audit log. |
| `GPS_STABILITY_RELIABILITY_THRESHOLD` | 98.0% | Minimum fix reliability required before audit log. |
| `PENDING_UNCERTAINTY_GROWTH_RATE_MPS`| 15.0 m/s | Growth rate of UI uncertainty radius during GPS stalls (v8.9.18). |
| `HINDSIGHT_BUFFER_SIZE` | 5 | Size of the rolling buffer for hindsight trajectory correction. |
| `HINDSIGHT_MAX_AGE_MS` | 30,000ms | Maximum age of points in the hindsight buffer. |
| `ADAPTIVE_JUMP_SNR_THRESHOLD`| 35.0 dB | SNR threshold for classifying jumps as potential spoofing. |
| `ADAPTIVE_JUMP_HOLD_MULTIPLIER`| 2.0x | Multiplier for jump hold duration in low-vibration scenarios. |
| `XIAOMI_SUPPRESSION_THRESHOLD_MS`| 15,000ms | Gap threshold for detecting MIUI background suppression. |
| `XIAOMI_RECOVERY_COOLDOWN_MS` | 60,000ms | Cooldown between Xiaomi heuristic recovery pulses. |
| `GPS_TRANSITION_LOG_MUZZLE_MS`| 30,000ms | Suppression window for GPS polling interval transition logs. |
| `SIT_TRANSMISSION_LATCH_MS` | 10,000ms | Duration for which SIT detected status is latched for transmission. |
| `JUMP_CHECK_MIN_DIST` | 5.0m | Minimum distance for a valid jump engine check. |
| `ACCEL_CHECK_MIN_DIST` | 10.0m | Minimum distance for acceleration-based validity check. |
| `EFFICIENCY_MIN_SEGMENT_DIST` | 10.0m | Minimum segment distance for path efficiency calculation. |
| `EFFICIENCY_MIN_TOTAL_DIST` | 50.0m | Minimum total distance for path efficiency calculation. |
| `SCATTER_MIN_SPEED_MPS`| 0.5 m/s | Minimum speed for scatter angle analysis. |
| `SCATTER_ANGLE_THRESHOLD` | 120.0° | Angle threshold for classifying movement as scatter (noise). |
| `ACOUSTIC_FLOOR_CONTRACTION_EMA`| 0.995 | EMA factor for passive acoustic floor decay. |
| `ACOUSTIC_RECOVERY_DELAY_MS` | 30,000ms | Delay before acoustic monitoring recovers after a violation. |
| `ACOUSTIC_SAMPLE_RATE` | 8000 | Sampling rate for acoustic magnitude detection. |
| `VIBRATION_WINDOW_SIZE` | 5 samples | Sliding window size for vibration EMA filtering. |
| `ROTATION_INIT_STATIONARY_MS`| 3,000ms | Stationary duration required for rotation vector initialization. |
| `SPIKE_DEBOUNCE_MS` | 5,000ms | Debounce period for physical sensor spikes. |
| `RTT_WINDOW_SIZE` | 5 samples | Window size for RTT averaging. |
| `MAX_HISTORY_POINTS_PER_RIBBONS`| 240 | Maximum history points retained per ribbon key in-memory. |
| `GPS_INDEX_AGE_EXCELLENT_SEC` | 5.0s | Age threshold for "Excellent" GPS Index rating. |
| `GPS_INDEX_AGE_SCALING` | 2.0 | Scaling factor for GPS age indexing. |
| `GPS_INDEX_ACCURACY_EXCELLENT_METERS`| 8.0m | Accuracy threshold for "Excellent" GPS Index rating. |
| `GPS_INDEX_SATS_TARGET` | 12 | Satellite count target for optimal GPS Index. |
| `LUX_EMA_SLOW` | 0.99 | EMA factor (Slow) for light. |
| `LUX_EMA_FAST` | 0.01 | EMA factor (Fast) for light. |
| `LUX_EMA_UP_SLOW` | 0.999 | EMA factor for rising light values (Slow). |
| `LUX_EMA_UP_FAST` | 0.001 | EMA factor for rising light values (Fast). |
| `ACOUSTIC_EMA_DOWN_SLOW` | 0.999 | EMA factor for falling acoustic floor (Slow). |
| `ACOUSTIC_EMA_DOWN_FAST` | 0.01 | EMA factor for falling acoustic floor (Fast). |
| `ACOUSTIC_EMA_UP_SLOW` | 0.9999 | EMA factor for rising acoustic floor (Slow). |
| `ACOUSTIC_EMA_UP_FAST` | 0.001 | EMA factor for rising acoustic floor (Fast). |
| `VIBRATION_EMA_DOWN_SLOW` | 0.99 | EMA factor for falling vibration floor (Slow). |
| `VIBRATION_EMA_DOWN_FAST` | 0.01 | EMA factor for falling vibration floor (Fast). |
| `VIBRATION_EMA_UP_SLOW` | 0.999 | EMA factor for rising vibration floor (Slow). |
| `VIBRATION_EMA_UP_FAST` | 0.0001 | EMA factor for rising vibration floor (Fast). |
| `BARO_EMA_SLOW` | 0.999 | EMA factor for barometric baseline stabilization. |

## 2. Forensic Ribbon Scaling
| Constant | Value | Description |
| :--- | :--- | :--- |
| `RIBBON_NOISE_SCALE_DB` | 40.0dB | Maximum range for acoustic ribbon mapping. |
| `RIBBON_LUX_LOG_SCALE` | 5.0 | Logarithmic scale factor for light ribbon mapping. |
| `RIBBON_VIBRATION_SCALE_G` | 2.0g | Maximum range for vibration ribbon mapping. |
| `RIBBON_LIFT_SCALE_METERS` | 5.0m | Maximum range for barometric lift ribbon mapping. |
| `RIBBON_SNR_SCALE_DB` | 45.0dB | Maximum range for SNR ribbon mapping. |
| `RIBBON_CURRENT_SCALE_MA` | 1000mA | Maximum range for battery current ribbon mapping. (v8.9.19) |
| `RIBBON_SIT_TILT_SCALE_DEG`| 15.0° | Maximum range for tilt stability ribbon mapping. (v8.9.21) |
| `RIBBON_SIT_BARO_SCALE_METERS`| 0.5m | Maximum range for baro stability ribbon mapping. (v8.9.21) |

## 3. Network & Connectivity
| Constant | Value | Description |
| :--- | :--- | :--- |
| `MAX_ALLOWED_RTT_MS` | 5000ms | Maximum Round Trip Time for healthy communication. |
| `COMM_RTT_FLOOR_MS` | 150ms | Baseline floor for RTT indexing. |
| `COMM_RTT_SCALING_FACTOR` | 2000.0 | Factor for communication quality normalization. |
| `NETWORK_TIMEOUT_MS` | 10000ms | Timeout for network requests. |
| `PING_INTERVAL_MS` | 10000ms | Interval for relay heartbeat (ping_cmd) and log synchronization. |
| `NET_REJOIN_THRESHOLD_MS` | 15000ms | Delay before attempting relay reconnection. |
| `NET_HEAL_THRESHOLD_MS` | 45000ms | Window for network "Healing" phase. |
| `SOCKET_TIMEOUT_MS` | 60,000ms | Socket.io connection timeout. |
| `HOME_POINT_REFRESH_INTERVAL_MS`| 30,000ms | Interval for refreshing geofence centers. |

---

## 4. Remote Forensic Verification
### 4.1. Version & Role Visibility
*   **Engine Identity**: The system operates on the v8.9.27 baseline logic.
*   **Dynamic Versioning**: `versionCode` in `build.gradle` is generated using `git rev-list --count HEAD`. (Issue #199)
*   **Engine Unification**: `MainAlarmLogic` in `:core:engine` is the exclusive source for violation detection.
*   **Standardized Alert IDs**: Aligned with `EngineConstants.kt`. Includes `VISUAL_JUMP` for trajectory-based jumps.
*   **Xiaomi System Ready**: Hardened gating for Xiaomi devices using `is_xiaomi_manual_override` and autostart verification.
*   **Network Serialization**: Unified to **snake_case** for Relay alignment.
*   **Time Integrity**: All alarm evaluations use monotonic time via `TimeProvider.elapsedRealtime()`.
*   **Module Hardening**: `:core:engine` is a pure `java-library` with zero Android dependencies.
*   **Role Forensic**: Mandatory `role` field present in all sync payloads and JSON exports for multi-role trace stability. Viewers explicitly latch and record peer visual jumps to local forensics.
*   **Role Identity Standards**: IDs must use enforced prefixes: **"T"** for Tracker (e.g., Ttk) and **"C"** for Viewer (e.g., Cohen). (Issue #182)
*   **Schema Cleanup**: Legacy `ver` and `vid` columns formally removed from database schema in v33 (v8.8.35).
*   **Power Parity**: `currentMa` field added to Database v35 (PendingStatusEntity and HistoryEntity) and `TrackerStatusProto` for end-to-end power forensics.
*   **SIT Acknowledgement**: Discrete SIT events are synchronized via a 10s acknowledged loop to prevent forensic loss during blackouts.
*   **Ghost Mode UI**: Visual staleness indicators applied to all sensor fields and markers when telemetry > 10s old.
*   **Log Spatial Anchor**: All forensic logs and critical alerts are tagged with `lat`/`lng` coordinates to enable historical marker reconstruction on the map.
*   **Accuracy Parity**: Forensic logs now include explicit `accuracy` fields, ensuring historical map markers match real-time precision.
*   **Forensic Snapshots**: Log entries now include `snrSnapshot` and `vibeSnapshot` for Jump and Stall forensic enrichment (v8.9.19).
*   **Stability Expansion**: Added `tiltIdx` and `baroIdx` to the analytical ribbons and telemetry pipeline for enhanced "SIT" event analysis (v8.9.21).
*   **Uncertainty Context**: Propagating `locationPendingReason` for Bayesian uncertainty expansion in the UI (v8.9.22).

---

## 5. Location & Jump Validation (Technical Parameters)
### 5.1. Jump Model Weights
| Metric | Weight |
| :--- | :--- |
| `JUMP_WEIGHT_SENSOR_MISMATCH` | 60 |
| `JUMP_WEIGHT_ACCEL_CHECK` | 40 |
| `JUMP_WEIGHT_ALTITUDE_DELTA` | 30 |
| `JUMP_WEIGHT_TRADITIONAL_SPEED` | 50 |
| `JUMP_WEIGHT_ACCURACY_LOW` | 30 |
| `JUMP_WEIGHT_ACCURACY_HIGH` | 20 |

### 5.2. Jump Gates
*   `JUMP_GATE_SENSOR_MISMATCH_MPS`: 10.0 m/s
*   `JUMP_GATE_SPEED_ACCURACY_LOW_MPS`: 22.2 m/s
*   `JUMP_GATE_SPEED_ACCURACY_HIGH_MPS`: 8.3 m/s
*   `JUMP_GATE_ACCURACY_LOW_THRESHOLD`: 40.0m
*   `JUMP_GATE_ACCURACY_HIGH_THRESHOLD`: 150.0m
*   `JUMP_GATE_VISUAL_JITTER_METERS`: 10.0m

---

## 6. IMM Filter Specification
*   `IMM_STATION_PROBABILITY`: 0.8
*   `IMM_KINEMATIC_PROBABILITY`: 0.2
*   `IMM_MIN_MEASUREMENT_NOISE_METERS`: 5.0m
*   `IMM_STATIONARY_Q_POS`: 0.01
*   `IMM_STATIONARY_Q_VEL`: 0.001
*   `IMM_KINEMATIC_Q_POS`: 0.5
*   `IMM_KINEMATIC_Q_VEL`: 0.2
*   `IMM_STALL_RECOVERY_DT_SEC`: 60.0s

---

## 7. Behavioral Model (TrackerState)
| State | Criteria |
| :--- | : :--- |
| **MOVING** | Speed >= 2.0 m/s OR trajectory confirmed. |
| **PARKING** | Device stationary (Vibration < 0.12g) and within parking anchor. |
| **JUMPING** | Encountering Jump Points (Tier 1 or 2). |
| **COOLING** | Device temperature exceeds 46.0°C. |
| **STALLED** | GPS hardware active but frozen (> 60s). |

---

## 8. Forensic Alert Manifest (v8.9.27)
| Alert ID | Alert Title (Standardized) | Trigger Description |
| :--- | :--- | :--- |
| `LOCAL_INTERNET` | This device: Internet Lost | Local connectivity failure. |
| `RELAY_OFFLINE` | This device: Relay Lost | Connectivity to relay server failed. |
| `TRACKER_OFFLINE` | Tracker: Offline | Tracker disconnected from relay. |
| `SIGNAL_LOSS` | Tracker: Signal Lost | No telemetry for >180s (Tracker) / >30s (Viewer). |
| `JUMP_ALERT` | Tracker: Jammer Alert | GPS sustained instability / Jump rejection / Outlier filtering. (Issue #231) |
| `VISUAL_JUMP` | Tracker: Visual Jump | Trajectory-based jump detected by engine. |
| `GEOFENCE_VIOLATION`| Tracker: Geofence | Breach of max distance or predictive exit. |
| `GPS_STALL` | Tracker: GPS Stalled | Hardware chip freeze (no updates >60s). |
| `GPS_GAP` | Tracker: GPS Gap | Fix age exceeded 60s. |
| `POWER_TAMPER` | Tracker: Charger unplugged | Power disconnection. |
| `LOW_BATTERY` | Tracker: Low Battery | Level < 20% (unplugged) or charge deficit. |
| `HIGH_TEMP` | Tracker: High Temp | Thermal > 46.0°C. |
| `TRACKER_TAMPER` | Tracker: Tamper Detected | Unified sensor violation (Shock, Tilt, Light, Prox). |
| `TILT_ALERT` | Tracker: Tilt Alert | Orientation change > 15°. |
| `ACOUSTIC_ALERT` | Tracker: Acoustic Alert | Audio spike > 40dB above baseline. |
| `LIFT_ALERT` | Tracker: Lift | Barometric lift > 0.8m. |
| `CHAIR_OCCUPIED` | Tracker: Chair Occupied | Multi-sensor pattern matching "sitting" event. |
| `SYSTEM_STORAGE_LOW`| Tracker: System Storage Low | < 50MB free. |
| `SYSTEM_STORAGE_CRITICAL`| Tracker: System Storage Critical | < 10MB free. |
| `BATTERY_HEALTH` | Tracker: Critical Battery Health| Steep discharge (>5% in 10m). |
| `XIAOMI_SYSTEM_MISSING`| Tracker: Xiaomi System Not Ready | Background restrictions detected on Xiaomi. |

---

## 9. Compliance & Operational Requirements
| Requirement ID | Requirement Description | Implementation Status |
| :--- | :--- | :--- |
| **R872** | **Alert Suppression**: No local alerts/sirens shall trigger on the Tracker device. New alerts must strictly respect the 2s grace period. | **Verified (BehaviorUseCase)** |
| **R915** | **UI Responsiveness**: The Map settings toggle must reliably respond to touch events over the AndroidView. | **Verified (MapComponents)** |
| **R916** | **Settings Persistence**: Users must be able to modify and persist IDs, distance, alert config, and sound selections at all times. | **Verified (SettingsRepository)** |
| **R917** | **Smooth Update**: The app must operate normally after an APK update without requiring a manual "Force Stop" or removal from recents. | **Verified (DataStore + Sticky FGS)** |
| **R921/R926** | **Session Lifecycle**: Exhaustive state reset and mandatory landing page pause to ensure lifecycle stability across sessions. | **Verified (MainViewModel)** |
| **R922** | **LED Logic**: INT LED reflects local relay state; SRV/TRK/DAT/GPS are gated by peer health (isRemote) for end-to-end verification. | **Verified (SharedUiComponents)** |
| **R923** | **Forensic Refresh**: Dashboard recovers immediately upon telemetry receipt using the maximum of GPS and arrival timestamps. | **Verified (DashboardUseCase)** |
| **R933** | **Alert Grace Period**: A mandatory 2-second grace period is enforced between consecutive alert triggers to prevent event flooding. | **Verified (AppAlarmManager)** |
| **R935** | **Icon Branding**: The app icon shall use the John Deere deer logo without any accompanying text. | **Verified (ic_jd_logo.xml)** |
| **R866** | **Branding Accuracy**: JD Branding Green must match exactly #367C2B. | **Verified (Color.kt)** |
| **R867** | **Role Identity**: Default Tracker ID shall be "Ttk" and Default Viewer ID shall be "Cohen". | **Verified (SettingsRepository)** |
| **R868** | **Telemetry Layout**: The Status Card must display `maxAccuracy` for both Tracker and Viewer roles in a unified format. | **Verified (SharedUiComponents)** |
| **Issue 6** | **Xiaomi Gating**: Implemented `ALERT_ID_XIAOMI_SYSTEM_MISSING` to detect and alert on MIUI background restrictions. | **Verified (MainAlarmLogic)** |
| **Issue 13** | **Timing Integrity**: Migrated to `SystemClock.elapsedRealtime()` for all debouncing and persistence timing to prevent wall-clock leaks. | **Verified (MainRepository)** |
| **Issue 15** | **Forensic I/O**: Implemented safety-flush in service `onDestroy` and monotonic interval checks for history persistence. | **Verified (MainRepository)** |
| **Issue 45** | **FGS Compliance**: Correctly passing `FOREGROUND_SERVICE_TYPE_LOCATION` for Android 10+ and asserting types in callbacks. | **Verified (ViewerService)** |
| **Issue 58** | **Module Hardening**: Converted `:core:engine` to a pure `java-library` to enforce zero Android framework dependencies. | **Verified (build.gradle)** |
| **Issue 70** | **Thermal Throttling**: Implemented "Cooling Mode" (46°C/44°C) that throttles GPS polling to protect hardware. | **Verified (IntegrityMonitor)** |
| **Issue 71/72** | **System Integrity**: Monitoring of low/critical storage and OS-level restrictions (Standby Buckets, Power Save) for forensic visibility. | **Verified (IntegrityMonitor)** |
| **Issue 102** | **TimeProvider**: Standardized all duration and timeout logic across the system to use the `TimeProvider` abstraction. | **Verified (Standardized Architecture)** |
| **Issue 115** | **Modularization**: Decoupled `MainViewModel` into feature UseCases (Navigation, Settings, Telemetry, Behavior, Session, Alert, Map). | **Verified (MainViewModel)** |
| **Issue 124** | **GPS Revival**: System retries hardware revival every 2m during stall and escalates to critical after 3 failures. | **Verified (TrackerService)** |
| **Issue 125** | **Monotonic UI**: UI lockout and pulse logic must use monotonic time to survive system clock jumps. | **Verified (MainViewModel)** |
| **Issue 146** | **Startup Performance**: Optimized launch by moving `OsmConfig` to background thread and staggering ViewModel initialization. | **Verified (GpsApplication)** |
| **Issue 148** | **A15 Stability**: Enforced 1000ms GPS polling (`A15_STABLE_GPS_POLLING_MS`) and 5s proximity debounce for Samsung A15 devices. | **Verified (ServiceBehaviorUseCase)** |
| **Issue 149** | **Forensic Parity**: Symbol parity for jump markers (Magenta Squares). Viewers explicitly latch peer visual jumps to local forensics. | **Verified (ViewerService/Map)** |
| **Issue 163** | **Power Tamper Hardening**: Reconnected power tamper detection via `IntegrityMonitor` using `EXTRA_PLUGGED` and auto-recovery. | **Verified (IntegrityMonitor)** |
| **Issue 168** | **Stability Audit Suite**: Implemented GPS Stability Audit suite in TrackerService.kt to track fix reliability and inter-fix gaps. | **Verified (TrackerService)** |
| **Issue 169** | **Version Synchronization**: Synchronized source headers in Tracker/Viewer services with the v8.9.11 baseline for audit integrity. | **Verified (TrackerService/ViewerService)** |
| **Issue 170/172** | **Xiaomi Alert Guard**: Added explicit `isXiaomiDevice` check to gate `ALERT_ID_XIAOMI_SYSTEM_MISSING` violations. | **Verified (MainAlarmLogic)** |
| **Issue 171** | **GPS Transition Muzzle**: Implemented `GPS_TRANSITION_LOG_MUZZLE_MS` (30s) to prevent forensic log flooding. | **Verified (TrackerService)** |
| **Issue 173** | **SIT Marker Persistence**: Reconnected `ALERT_ID_TRACKER_CHAIR` to `forensicUseCase` for persistent map visualization on the Tracker. | **Verified (TrackerService)** |
| **Issue 174** | **Default Identity**: Updated `DEFAULT_TRACKER_ID` to "Ttk" and `DEFAULT_VIEWER_ID` to "Cohen" for verified role identity. | **Verified (SettingsRepository)** |
| **Issue 175** | **Version Update Smoothness**: Verified `MY_PACKAGE_REPLACED` handling in `BootReceiver` for background service continuity. | **Verified (BootReceiver)** |
| **Issue 176** | **Statistics Persistence**: Verified forensic ribbon and statistics accumulation across app restarts using Room and DataStore. | **Verified (HistoryManager/SettingsRepository)** |
| **Issue 177** | **Dead Code Cleanup**: Formally removed redundant telemetry methods in `SyncManager` following consolidation around `pushCurrentStatus`. | **Verified (SyncManager)** |
| **Issue 178/179**| **Forensic Parity Audit**: Verified 100% field parity for `verticalVelocity` and SIT metrics across the pipeline and `RemoteHandler`. | **Verified (RemoteHandler/HistoryManager)** |
| **Issue 189** | **Viewer Background Location**: Implemented 10s background polling and relative distance calculation in `ViewerService`. | **Verified (ViewerService)** |
| **Issue 190** | **Xiaomi Autostart & Boot Resilience**: Implemented robust handling for indeterminate "Unknown" status and `XIAOMI_BOOT_GRACE_MS` (30s) to suppress transient boot alarms. | **Verified (MainAlarmLogic)** |
| **Issue 191** | **Muzzle Window Hardening**: Increased `MUZZLE_WINDOW_DURATION_MS` to 2000ms and added device-specific hysteresis (500ms for A15). | **Verified (SyncManager/TrackerService)** |
| **Issue 192** | **Power Forensic Parity**: Achieved absolute parity for `currentMa` across models, database (v35), and ribbons. | **Verified (SyncManager/HistoryManager)** |
| **Issue 193** | **Zombie Telemetry UX Sweep**: Implemented visual staleness indicators ("Ghost Mode") for all sensor derived dashboard fields and markers when telemetry > 10s old. | **Verified (DashboardUseCase/MapComponents)** |
| **Issue 194** | **SIT Persistence Packet Loss Risk**: Implemented a 10s acknowledged sync loop for discrete SIT events to prevent forensic loss during blackouts. | **Verified (SyncManager/ViewerService)** |
| **Issue 195** | **Room Migration Forensic Audit**: Implemented Room migration (v36) for full table reconstruction and Android 15 compatibility. | **Verified (Database)** |
| **Issue 196** | **Advanced SIT Detection**: Refined "Plunge" state machine and propagated `sitVzTs` for forensic parity. | **Verified (AppSensorManager)** |
| **Issue 197** | **Database Schema Expansion (v38)**: Added `sitVzTs` to history tables for improved chair event reconstruction. | **Verified (Database)** |
| **Issue 198** | **GPS Availability Hardening**: Shortened GPS stall detection to 60s and revival retry to 120s for high-availability tracking. | **Verified (TrackerService)** |
| **Issue 199** | **Toolchain Modernization**: Upgraded to Java 17 and Android SDK 35. Aligned Gradle DSL syntax. | **Verified (build.gradle)** |
| **Issue 200** | **Room Migration Registry**: Registered `MIGRATION_37_38` in `AppModule.kt` to prevent startup crashes. | **Verified (AppModule)** |
| **Issue 201** | **Schema Robustness**: Hardened `MIGRATION_35_36` with dynamic column detection for resilient upgrades. | **Verified (Database)** |
| **Issue 202** | **Multi-Version Forensic Path**: Verified schema integrity across diverse legacy upgrade paths (v33 to v38). | **Verified (Database)** |
| **Issue 203** | **Documentation Synchronization**: Synchronized all core documentation to the v8.9.11 logic baseline. | **Verified (DOCS)** |
| **Issue 204/205**| **Constant Hardening Audit**: Verified GPS Stall (60s), Revival (120s), and Muzzle Window (2000ms) thresholds across documentation. | **Verified (SoT/EngineConstants)** |
| **Issue 206** | **Staleness Unification Audit**: Verified 10s unification for Ghost Mode and Position Health thresholds. | **Verified (SoT/Dashboard)** |
| **Issue 207** | **SoT Typo Audit**: Fixed incorrect quote characters in constant definitions in REQUIREMENTS_SOT.md. | **Verified (SoT)** |
| **Issue 208** | **Log Spatial Anchor Gap**: Implemented coordinate-aware forensic logging. All critical alerts and system events now include `lat`/`lng` for map reconstruction. | **Verified (LogManager/Services)** |
| **Issue 209** | **Coordinate Propagation Race**: Ensured 1:1 mapping between processing and logging coordinates by expanding the processing listener. | **Verified (LocationProcessor/Services)** |
| **Issue 210** | **Log Manager Redundancy Audit**: Eliminated redundant service-level coordinate logic; standardized on `LogManager` auto-anchoring. | **Verified (TrackerService/ViewerService)** |
| **Issue 211** | **Stability Audit Verbosity**: Gated STABILITY AUDIT logs to only emit when performance falls below 98% reliability or 200ms gap. | **Verified (TrackerService)** |
| **Issue 212** | **Forensic Accuracy Parity**: Logs now preserve and reconstruct historical marker precision using log-specific accuracy values. | **Verified (LogEntry/RemoteHandler)** |
| **Issue 213** | **Forensic Anchor Desync in AppAlarmManager**: Explicitly propagated coordinates and accuracy in alarm logging callbacks. | **Verified (AppAlarmManager/Services)** |
| **Issue 214** | **Redundant Accuracy Fallback Logic**: Unified accuracy fallback in LogManager to prioritize fix accuracy then maxAccuracy. | **Verified (LogManager)** |
| **Issue 215** | **Missing "Recovery" Logs for Stability Audit**: Implemented "STABILITY RESTORED" marker for performance recovery events. | **Verified (TrackerService)** |
| **Issue 216** | **Accuracy Propagation Gaps in ViewerService**: Hardened spatial anchoring for Viewer pulses and session terminations. | **Verified (ViewerService)** |
| **Issue 217** | **LocationProcessor Inconsistency**: Standardized "Merge-on-Stale" logs to use hardened auto-anchoring path. | **Verified (LocationProcessor)** |
| **Issue 218** | **Xiaomi MIUI 14 Heuristic Recovery**: Detects background tick suppression (gap > 15s) and triggers GPS revival and WakeLock renewal. | **Verified (TrackerService)** |
| **Issue 219** | **Adaptive Jump Confidence**: Correlates SNR and Vibration to distinguish between signal reflection and legitimate movement. | **Verified (LocationSentinel)** |
| **Issue 220** | **Hindsight Correction**: Implements retroactive trajectory smoothing using a hindsight rolling buffer. | **Verified (LocationSentinel)** |
| **Issue 221** | **Bayesian Uncertainty Scaling**: UI radius expands at 15m/s during GPS stalls based on `lastValidFixRealtime`. | **Verified (MapComponents)** |
| **Issue 222** | **Hindsight Path Visualization**: Renders "Ghost Paths" (Slate500) for points retroactively promoted. | **Verified (MapComponents)** |
| **Issue 223** | **Forensic Log Enrichment**: SNR and Vibration snapshots attached to jump/stall logs for black-box analysis. | **Verified (LogManager/Services)** |
| **Issue 224** | **SIT Forensic Expansion**: Added tiltIdx and baroIdx to analytical ribbons and telemetry pipeline for enhanced chair event analysis. | **Verified (AnalyticalRibbons/Telemetry)** |
| **Issue 225** | **Analytical Ribbon Persistence**: Verified 240-point in-memory retention for all new forensic indices. | **Verified (HistoryManager)** |
| **Issue 226** | **Contextual Uncertainty**: Implemented `LocationPendingReason` to provide UI context for Bayesian expansion. | **Verified (EngineModels/SyncManager)** |
| **Issue 227** | **Hindsight Transition Smoothing**: Added `promotedPoints` to `SentinelResult` for improved map trail continuity. | **Verified (LocationSentinel)** |
| **Issue 228/229**| **SoT Constant Synchronization**: Standardized GPS_STABILITY_RELIABILITY_THRESHOLD to 98.0% and removed redundant DISTANCE_GRACE_MS. | **Verified (SoT/EngineConstants)** |
| **Issue 230** | **Alert Label Unification**: Standardized `ALERT_ID_TRACKER_CHAIR` subtitle to "Chair occupancy detected". | **Verified (MainAlarmLogic)** |
| **Issue 231** | **Visual Jump Integrity**: Implemented trajectory-aware `VISUAL_JUMP` detection in the core engine. | **Verified (MainAlarmLogic/Sentinel)** |
| **Issue 232** | **Forensic Current Ribbon**: Added `RIBBON_CURRENT_SCALE_MA` (1000mA) for battery current visualization. | **Verified (EngineConstants)** |
| **Issue 233** | **Manual Overrides Recovery**: Ensured `is_xiaomi_manual_override` is correctly propagated during thermal/low-power throttling. | **Verified (IntegrityMonitor)** |
| **Issue 234** | **Vibration Window Stabilization**: Adjusted `VIBRATION_WINDOW_SIZE` to 5 samples for reduced jitter in stationary states. | **Verified (EngineConstants)** |
| **Issue 235** | **Acoustic Floor Decay**: Implemented `ACOUSTIC_FLOOR_CONTRACTION_EMA` (0.995) for passive environmental calibration. | **Verified (LocationSentinel)** |
| **Issue 236** | **Relay Rejoin Strategy**: Verified `NET_REJOIN_THRESHOLD_MS` (15s) logic in `AppNetworkManager` for rapid blackout recovery. | **Verified (Network)** |
| **Issue 237** | **Session Metrics Persistence**: Verified atomic `saveSessionMetricsBulk` calls for reduced DataStore I/O during high-frequency tracking. | **Verified (SettingsRepository)** |
| **Issue 238** | **Siren Auto-Recovery**: Verified `SIREN_RESUME_COOLDOWN_MS` (15s) logic to prevent permanent siren lockout after hardware auto-stop. | **Verified (AppAlarmManager)** |
| **Issue 239** | **Hindsight Buffer Bug**: Fixed bug in `LocationSentinel.kt` where old rejected points were not cleared after trajectory promotion. | **Verified (LocationSentinel)** |
| **Issue 240** | **Contextual Uncertainty Context**: Propagated `locationPendingReason` through `SyncManager` to ensure remote Viewer context. | **Verified (SyncManager)** |
| **Issue 241** | **Forensic Log Enrichment**: Bridged SNR and Vibration snapshots to `AppAlarmManager` for more detailed black-box analysis. | **Verified (AppAlarmManager)** |
| **Issue 242** | **Redundant Index Calculation**: Viewer now utilizes pre-calculated indices transmitted from the tracker's engine. | **Verified (ViewerService)** |
| **Issue 243** | **Visual Jump Sensitivity**: Expanded Tier 3 Jitter gating to ensure no "visual jitters" are missed during low-vibration movement. | **Verified (TrackerService)** |
| **Issue 244** | **Location Pending Persistence**: Ensured `locationPendingReason` survives offline storage and synchronization cycles. | **Verified (SyncManager/Room)** |
| **Issue 245** | **Duplicate SIT Event Rising-Edge**: Centralized SIT rising-edge detection in `RemoteHandler.kt` to eliminate redundant logs. | **Verified (RemoteHandler)** |
