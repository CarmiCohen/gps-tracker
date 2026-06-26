# Location Sentinel Specification (v8.9.37)

The **Location Sentinel** is a multi-modal sensor fusion engine designed to detect unauthorized physical interaction with the tracker. In v8.9.37, this logic is strictly isolated within the `:core:engine` module and geographically anchored via **Log Spatial Anchors** (Issue #208).

## 1. Monitored Sensors
The Sentinel monitors the following hardware sensors:
1.  **Barometer**: Relative altitude changes.
2.  **Rotation Vector**: 3D orientation and tilt.
3.  **Light Sensor**: Ambient light levels.
4.  **Proximity**: Distance to mounting surface.
5.  **Microphone**: Ambient noise floor (dB).
6.  **Accelerometer**: Continuous vibration and shock indexing.

## 2. Violation Logic (Hard Gates)
Any of the following conditions will trigger a **Suspicious State** (forcing 1s GPS polling) and an optional **Immediate Siren**. These thresholds are defined in `EngineConstants.kt`.

| Condition | Threshold | Alert Title | Action |
| :--- | :--- | :--- | :--- |
| **Lift** | Δ Alt > `BARO_LIFT_THRESHOLD_METERS` (0.8m) | `Tracker: Lift` | Immediate Siren |
| **Tilt** | Δ Orientation > `TILT_THRESHOLD_DEGREES` (15°) | `Tracker: Tilt Alert` | Immediate Siren |
| **Light Jump** | Δ Lux > `LIGHT_THRESHOLD_LUX_JUMP` (150.0f) | `Tracker: Tamper Detected` | Immediate Siren |
| **Proximity** | NEAR → FAR (Debounced) | `Tracker: Tamper Detected` | Immediate Siren |
| **Acoustic (High)** | Δ Noise > `ACOUSTIC_THRESHOLD_DB_JUMP` (40.0) | `Tracker: Acoustic Alert` | Immediate Siren |
| **Acoustic (Low)** | Δ Noise > `ACOUSTIC_SUSPICIOUS_THRESHOLD_DB_JUMP` (20.0) | `None` | Suspicious State |
| **Shock** | Peak > `VIBRATION_SHOCK_THRESHOLD_G` (0.8g) | `Tracker: Tamper Detected` | Immediate Siren |
| **Vibration** | Level > `VIBRATION_SUSPICIOUS_THRESHOLD_G` (0.25g) | `None` | Suspicious State |

## 3. Implementation Details

### A. Muzzle Window (Issue #191)
The sentinel implements a `MUZZLE_WINDOW_DURATION_MS` (2000ms) during sync operations. While `isMuzzled` is true, sensor triggers are suppressed to prevent interference-based false positives. Includes device-specific hysteresis (e.g., `MUZZLE_HYSTERESIS_A15_MS` 500ms).

### B. Lift Detection (Barometer)
If the current pressure indicates a vertical rise of more than `BARO_LIFT_THRESHOLD_METERS` (0.8m), the system assumes the vehicle is being lifted.

### C. Tilt Detection (Rotation Vector)
Calculates the angular delta from the point of activation. If the device is rotated more than `TILT_THRESHOLD_DEGREES` (15°), a tilt violation is recorded.

### D. Light Jump (EMA Baseline) (Issue #284)
Sudden light spikes over the asymmetrical rising/falling EMA baseline trigger a tamper alert.

### E. Acoustic Sentinel
Triggers are gated by `ACOUSTIC_MIN_THRESHOLD_DB` (50.0dB) absolute floor to prevent false positives in silent environments.

### F. Adaptive Vibration Normalization
Vibration thresholds are dynamic and normalized against the hardware noise floor using EMA. Uses `VIBRATION_STATIONARY_THRESHOLD` (0.12f) for motion discrimination (Issue #318).

## 4. Forensic Continuity (v8.9.37)
Physical violations are logged with forensic metadata:
1.  **Log Spatial Anchor (Issue #208)**: All tamper, lift, and tilt events are now automatically anchored with `lat`/`lng` coordinates to show exactly where the physical violation occurred.
2.  **Monotonic Timing (Issue #283)**: All timing deltas and violation durations are calculated using `TimeProvider.elapsedRealtime()`.
3.  **Ghost Mode UX (Issue #193)**: Visual staleness indicators are applied to all sensor-derived fields when telemetry is older than 10s.
4.  **SIT Acknowledgment (Issue #194)**: Discrete SIT events are synchronized via a 10s acknowledged loop to ensure persistence.

## 5. State Integration
When a physical violation occurs:
1.  The system transitions to `SUSPICIOUS` state.
2.  GPS polling increases to `SUSPICIOUS_GPS_POLLING_MS` (1000ms).
3.  Kalman/ImmFilter Process Noise (Q) is scaled up via `SUSPICIOUS_Q_SCALE` (1000.0).
