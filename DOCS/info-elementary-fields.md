# Info Elementary Page: Field Definitions and Functions (v8.8.35)

This document describes the technical fields displayed on the Info (Dashboard) page of the GPS Tracker application.

## 1. Device State & Safety
*   **DEVICE STATE**: The current operational state of the tracker logic.
    *   `MOVING`: Active movement or confirmed trajectory detected (Speed >= 2.0 m/s).
    *   `PARKING`: The device is stationary (Vibration < 0.12g `VIBRATION_STATIONARY_THRESHOLD`) and has settled.
    *   `JUMPING`: Erratic GPS movement detected (Security Hold `JUMP_HOLD_DURATION_MS` 180s active).
    *   `COOLING`: The device has exceeded `MAX_SAFE_TEMPERATURE_CELSIUS` (46.0°C) and is in power-save mode.
    *   `STALLED`: GPS hardware is active but providing frozen coordinates (> 180s `GPS_STALL_THRESHOLD_MS`).
        *   **Escalated Revival (Issue 124)**: System attempts soft-revival every 5m. After 3 failures, it escalates to a CRITICAL hardware lock alert.
    *   `OFFLINE`: Peer device is disconnected from the relay.
*   **[SUSPICIOUS]**: Indicates high-risk telemetry patterns (e.g., high vibration/noise without movement).
*   **[TAMPER]**: Indicates a physical sentinel violation (Tilt, Acoustic, Light, etc.).
*   **[PROMOTED]**: A trajectory confirmed through 30s (`TRAJECTORY_PROMOTION_WINDOW_MS`) of consistent movement, bypassing jitter filters.

## 2. Timing & Connectivity (Diagnostic)
*   **Max Drop**: The longest period of disconnection recorded in the session.
*   **Last seen**: Timestamp of the last successful data packet.
*   **Total Drop**: Cumulative time spent disconnected.
*   **Bruto**: Total time since session start (reset on Stats Reset).
*   **Ping**: Latency between Viewer and Tracker via relay.
*   **Total Mon**: Cumulative monitoring time.
*   **Uptime**: Process lifespan.

## 3. GPS Analysis
*   **GPS-Index**: 0.0–1.0 score for overall reliability. 
    *   *Interactive*: Tap this label to open the **GNSS Detail Overlay**, showing real-time SNR (`snrIdx`) and status for individual satellites.
*   **Tr Accuracy**: Current accuracy in meters.
*   **Satellites Index**: Satellites used vs. in view.
*   **Tr Max**: Worst accuracy recorded in the session.
*   **Age Index**: Staleness of the current position fix (7s `GPS_UI_FAIL_THRESHOLD_MS` gray-out).
*   **Acc Index**: Sub-score for coordinate precision.
*   **Avg SNR**: Average Signal-to-Noise Ratio (dB) across all used satellites (`snrIdx`). Provides a primary indicator of signal quality and potential jamming/obstruction.

## 4. Environment & Sensors
*   **Vibration**: Normalized magnitude (g-units).
*   **Compass**: Magnetic heading.
*   **Tilt**: Angular delta relative to calibrated "flat" (> 15° `TILT_THRESHOLD_DEGREES` violation).
*   **Noise Level**: Ambient intensity in dB.
    *   *Note*: When active, the system "Mic Active" icon may linger for 45s (`FGS_STICKY_DELAY_MS`) after exit due to safety hysteresis.
*   **Lift**: Barometric height change in meters (> 0.8m `BARO_LIFT_THRESHOLD_METERS` violation).
*   **Lux**: Ambient light intensity (> 150 lux jump `LIGHT_THRESHOLD_LUX_JUMP` violation).
*   **Proximity**: Detection of device covering/handling.

## 5. SIT (Sitting) Detection
These fields are primarily visualized in the **Analytical Ribbons** overlay and track mechanical "sitting" events (e.g., when the tracker is placed on a chair or a person sits down with the device).
*   **SIT (isSitActive)**: A binary forensic latch indicating an active sitting event. Triggered when `tiltDelta > 7.0°`, `baroDelta > 0.08m`, and `peakShock > 0.35g` occur simultaneously, or if a specialized "plunge" kinematic pattern is matched.
*   **SVZ (sitVz)**: SIT Vertical Velocity. Records the peak vertical speed (m/s) during the detection window.
*   **SDZ (sitDz)**: SIT Vertical Displacement. Records the total vertical drop (meters) measured by the barometer during the SIT event.

## 6. Baselines & Hardening
*   **Peak Shock**: Highest instantaneous g-force detected (> 0.8g `VIBRATION_SHOCK_THRESHOLD_G`).
*   **Vibration Floor**: Adaptive noise-floor EMA (Initial: 0.05g `INITIAL_VIBRATION_FLOOR`).
*   **Lux Baseline**: Environmental light EMA.
*   **Acoustic Floor**: Ambient noise baseline EMA (Min: 50.0dB `ACOUSTIC_MIN_THRESHOLD_DB`).
*   **Jump Tier**: Classification of current GPS noise (1: Outlier, 2: Security, 3: Jitter).
*   **Muzzle Window**: 500ms suppression window (`MUZZLE_WINDOW_DURATION_MS`) to eliminate false tamper triggers during sync.
*   **Identity Unification**: Legacy `ver` and `vid` fields have been removed as part of the v8.8.35 forensic simplification.
