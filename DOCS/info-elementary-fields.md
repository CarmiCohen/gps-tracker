# Info Elementary Page: Field Definitions and Functions (v8.9.21)

This document describes the technical fields displayed on the Info (Dashboard) page of the GPS Tracker application.

## 1. Device State & Safety
*   **DEVICE STATE**: The current operational state of the tracker logic.
    *   `MOVING`: Active movement or confirmed trajectory detected (Speed >= 2.0 m/s).
    *   `PARKING`: The device is stationary (Vibration < 0.12g `VIBRATION_STATIONARY_THRESHOLD`) and has settled.
    *   `JUMPING`: Erratic GPS movement detected (Security Hold `JUMP_HOLD_DURATION_MS` 180s active).
    *   `COOLING`: The device has exceeded `MAX_SAFE_TEMPERATURE_CELSIUS` (46.0°C) and is in power-save mode.
    *   `STALLED`: GPS hardware is active but providing frozen coordinates (> 60s `GPS_STALL_THRESHOLD_MS`).
        *   **Escalated Revival (Issue 124/198)**: System attempts soft-revival every 120s (`GPS_REVIVAL_RETRY_INTERVAL_MS`). After 3 failures, it escalates to a CRITICAL hardware lock alert.
    *   `OFFLINE`: Peer device is disconnected from the relay.
*   **[SUSPICIOUS]**: Indicates high-risk telemetry patterns (e.g., high vibration/noise without movement).
*   **[TAMPER]**: Indicates a physical sentinel violation (Tilt, Acoustic, Light, etc.).
*   **[PROMOTED]**: A trajectory confirmed through 30s (`TRAJECTORY_PROMOTION_WINDOW_MS`) of consistent movement, bypassing jitter filters.
*   **[BATT HEALTH]**: Triggered by a **Steep Discharge** event (> 5% drop in 10 minutes).

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
*   **Age Index**: Staleness of the current position fix (10s `GPS_UI_FAIL_THRESHOLD_MS` gray-out/Ghost Mode).
*   **Acc Index**: Sub-score for coordinate precision.
*   **Avg SNR**: Average Signal-to-Noise Ratio (dB) across all used satellites (`snrIdx`). Provides a primary indicator of signal quality and potential jamming/obstruction.
*   **Bayesian Expansion (lastValidFixRealtime)**: The timestamp of the last high-confidence GPS fix.
    *   *Logic*: If `isLocationPending` is active, the UI radius expands at 15m/s (`PENDING_UNCERTAINTY_GROWTH_RATE_MPS`) relative to this timestamp to visualize growing spatial uncertainty during a signal blackout.

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
These fields are primarily visualized in the **Analytical Ribbons** overlay and track mechanical "sitting" events.
*   **SIT (isSitActive)**: A binary forensic latch indicating an active sitting event. Triggered when `tiltDelta > 7.0°`, `baroDelta > 0.08m`, and `peakShock > 0.35g` occur simultaneously.
*   **TLT Ribbon (tiltIdx)**: Forensic visualization of device orientation stability.
    *   *Mapping*: Normalized to a 15° scale (`tiltDegrees / 15.0`). Captures subtle orientation shifts during chair occupancy or device handling.
*   **BAR Ribbon (baroIdx)**: Forensic visualization of barometric stability.
    *   *Mapping*: Normalized to a 0.5m scale (`abs(relativeAltitude) / 0.5`). High-resolution tracking of vertical micro-movements.
*   **SVZ (sitVz)**: SIT Vertical Velocity. Records the peak vertical speed (m/s) during the detection window.
*   **SDZ (sitDz)**: SIT Vertical Displacement. Records the total vertical drop (meters) measured by the barometer during the SIT event.

## 6. Power & Forensic Current (Issue 192)
*   **Battery Drain (currentMa)**: Real-time battery current in mA. 
    *   Negative values indicate discharge (e.g., -450mA), positive values indicate charging.
*   **CUR Ribbon**: Forensic visualization of power consumption in the Analytical Ribbons overlay.
    *   *Mapping*: Normalized to a 1000mA scale (`abs(currentMa) / 1000f`). Captures high-drain events like GPS cold starts or active telemetry bursts.
*   **BAT Ribbon**: Indicates binary **Steep Discharge** status (1.0 if `isBatterySteepDischarge` is active).

## 7. Baselines & Hardening
*   **Peak Shock**: Highest instantaneous g-force detected (> 0.8g `VIBRATION_SHOCK_THRESHOLD_G`).
*   **Vibration Floor**: Adaptive noise-floor EMA (Initial: 0.05g `INITIAL_VIBRATION_FLOOR`).
*   **Lux Baseline**: Environmental light EMA.
*   **Acoustic Floor**: Ambient noise baseline EMA (Min: 50.0dB `ACOUSTIC_MIN_THRESHOLD_DB`).
*   **Jump Tier**: Classification of current GPS noise (1: Outlier, 2: Security, 3: Jitter).
*   **Muzzle Window**: 2000ms suppression window (`MUZZLE_WINDOW_DURATION_MS`) to eliminate false tamper triggers during sync.
*   **Log Spatial Anchor (v8.9.10)**: All forensic logs and alerts are now automatically anchored with `lat`/`lng` coordinates. This enables historical marker reconstruction on the Map even for events that occurred during relay blackouts.
*   **Identity Unification**: Legacy `ver` and `vid` fields have been removed as part of the v8.8.35 forensic simplification.
