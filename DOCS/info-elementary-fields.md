# Info Elementary Page: Field Definitions and Functions (v8.9.37)

This document describes the technical fields displayed on the Info (Dashboard) page of the GPS Tracker application.

## 1. Device State & Safety
*   **DEVICE STATE**: The current operational state of the tracker logic.
    *   `MOVING`: Active movement or confirmed trajectory detected (Speed >= 2.0 m/s).
    *   `PARKING`: The device is stationary (Vibration < 0.12g `VIBRATION_STATIONARY_THRESHOLD`) and has settled.
    *   `JUMPING`: Erratic GPS movement detected (Security Hold `JUMP_HOLD_DURATION_MS` 180s active).
    *   `COOLING`: The device has exceeded `MAX_SAFE_TEMPERATURE_CELSIUS` (46.0°C) and is in power-save mode. (Issue #352 - Formerly #273-B)
    *   `STALLED`: GPS hardware is active but providing frozen coordinates (> 60s `GPS_STALL_THRESHOLD_MS`). (Issue #198)
        *   **Escalated Revival**: System attempts soft-revival every 120s (`GPS_REVIVAL_RETRY_INTERVAL_MS`). After 3 failures, it escalates to a CRITICAL hardware lock alert. (Issue #341 - Formerly #124)
    *   `OFFLINE`: Peer device is disconnected from the relay.
*   **[SUSPICIOUS]**: Indicates high-risk telemetry patterns (e.g., high vibration/noise without movement).
*   **[TAMPER]**: Indicates a physical sentinel violation (Tilt, Acoustic, Light, etc.).
*   **[PROMOTED]**: A trajectory confirmed through 30s (`TRAJECTORY_PROMOTION_WINDOW_MS`) of consistent movement, bypassing jitter filters. (Issue #367 - Formerly #285)
*   **[BATT HEALTH]**: Triggered by a **Steep Discharge** event (> 5% drop in 10 minutes). (Issue #353)

## 2. Timing & Connectivity (Diagnostic)
*   **Max Drop**: The longest period of disconnection recorded in the session.
*   **Last seen**: Timestamp of the last successful data packet.
*   **Total Drop**: Cumulative time spent disconnected.
*   **Bruto**: Total time since session start (reset on Stats Reset).
*   **Ping**: Latency between Viewer and Tracker via relay (RTT-aware).
*   **Total Mon**: Cumulative monitoring time.
*   **Uptime**: Process lifespan (`uptimeMs`). (Issue #357)

## 3. GPS Analysis
*   **GPS-Index**: 0.0–1.0 score for overall reliability. 
    *   *Interactive*: Tap this label to open the **GNSS Detail Overlay**, showing real-time SNR and status for individual satellites.
*   **Tr Accuracy**: Current accuracy in meters.
*   **Satellites Index**: Satellites used vs. in view.
*   **Tr Max**: Worst accuracy recorded in the session (High-water mark). (Issue #325 - Formerly #214)
*   **Age Index**: Staleness of the current position fix (10s `GPS_UI_FAIL_THRESHOLD_MS` gray-out/Ghost Mode). (Issue #338 - Formerly #193)
*   **Acc Index**: Sub-score for coordinate precision.
*   **Avg SNR**: Average Signal-to-Noise Ratio (dB) across all used satellites. Provides a primary indicator of signal quality and potential jamming/obstruction.
*   **Bayesian Expansion**: The UI displays spatial uncertainty growing at `PENDING_UNCERTAINTY_GROWTH_RATE_MPS` (15m/s) during blackouts if `locationPendingReason` is active. (Issue #328 - Formerly #221)

## 4. Environment & Sensors
*   **Vibration**: Normalized magnitude (g-units).
*   **Compass**: Magnetic heading.
*   **Tilt**: Angular delta relative to calibrated "flat" (> 15° `TILT_THRESHOLD_DEGREES` violation).
*   **Noise Level**: Ambient intensity in dB.
    *   *Note*: When active, the system "Mic Active" icon may linger for 45s (`FGS_STICKY_DELAY_MS`) after exit due to safety hysteresis.
*   **Lift**: Barometric height change in meters (> 0.8m `BARO_LIFT_THRESHOLD_METERS` violation).
*   **Lux**: Ambient light intensity (> 150 lux jump `LIGHT_THRESHOLD_LUX_JUMP` violation).
*   **Proximity**: Detection of device covering/handling. Hardened for Samsung A15 (Issue #363 - Formerly #148).

## 5. SIT (Sitting) Detection
These fields are primarily visualized in the **Analytical Ribbons** overlay and track mechanical "sitting" events. (Issue #336 - Formerly #331)
*   **SIT (isSitActive)**: A binary forensic latch indicating an active sitting event.
*   **TLT Ribbon (tiltIdx)**: Forensic visualization of device orientation stability (normalized to 15°).
*   **BAR Ribbon (baroIdx)**: Forensic visualization of barometric stability (normalized to 0.5m).
*   **SVZ (sitVz)**: SIT Vertical Velocity (peak speed m/s).
*   **SDZ (sitDz)**: SIT Vertical Displacement (total drop in meters).

## 6. Power & Forensic Current
*   **Battery Drain (currentMa)**: Real-time battery current in mA. (Issue #337 - Formerly #192)
    *   Negative values indicate discharge, positive values indicate charging.
*   **CUR Ribbon**: Forensic visualization of power consumption (normalized to 1000mA).
*   **BAT Ribbon**: Indicates binary **Steep Discharge** status. (Issue #353)

## 7. Baselines & Hardening
*   **Peak Shock**: Highest instantaneous g-force detected (> 0.8g `VIBRATION_SHOCK_THRESHOLD_G`).
*   **Vibration Floor**: Adaptive noise-floor EMA (Issue #301).
*   **Lux Baseline**: Environmental light EMA (dual-rate Slow/Fast). (Issue #372 - Formerly #266)
*   **Acoustic Floor**: Ambient noise baseline EMA (Min: 25.0dB `ACOUSTIC_FLOOR_MIN_DB`). (Issue #292)
*   **Jump Tier**: Classification of current GPS noise (1: Outlier, 2: Security, 3: Jitter).
*   **Muzzle Window**: Unified suppression window (Issue #191). Includes 2000ms (Global), 500ms (A15 Hysteresis), and 5000ms (A15 Proximity).
*   **Log Spatial Anchor**: All forensic logs and alerts are automatically anchored with `lat`/`lng` coordinates (Issue #208).
*   **Identity Unification**: Standardized role identity (Ttk/Cohen) and removal of legacy `ver`/`vid` columns. (Issue #182)
