# Location Sentinel Specification (v8.9.51)

The **Location Sentinel** is a multi-modal sensor fusion engine designed to detect unauthorized physical interaction with the tracker. In v8.9.51, this logic is strictly isolated within the `:core:engine` module and geographically anchored via **Dual-Metric Spatial Anchors** (Issue #325).

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
| **Lift** | Δ Alt > `BARO_LIFT_THRESHOLD_METERS` (0.8m) | `Lift` | Immediate Siren |
| **Tilt** | Δ Orientation > `TILT_THRESHOLD_DEGREES` (15°) | `Tilt Alert` | Immediate Siren |
| **Light Jump** | Δ Lux > `LIGHT_THRESHOLD_LUX_JUMP` (150.0f) | `Tamper Detected` | Immediate Siren |
| **Proximity** | NEAR → FAR (Debounced) | `Tamper Detected` | Immediate Siren |
| **Acoustic (High)** | Δ Noise > `ACOUSTIC_THRESHOLD_DB_JUMP` (40.0) | `Acoustic Alert` | Immediate Siren |
| **Acoustic (Low)** | Δ Noise > `ACOUSTIC_SUSPICIOUS_THRESHOLD_DB_JUMP` (20.0) | `None` | Suspicious State |
| **Shock** | Peak > `VIBRATION_SHOCK_THRESHOLD_G` (0.8g) | `Tamper Detected` | Immediate Siren |
| **Vibration** | Level > `VIBRATION_SUSPICIOUS_THRESHOLD_G` (0.25g) | `None` | Suspicious State |

*Note: Alert titles are standardized per R747 (Issue #424).*

## 3. Implementation Details

### A. Muzzle Window (Issue #191)
The sentinel implements a `MUZZLE_WINDOW_DURATION_MS` (2000ms) during sync operations. While `isMuzzled` is true, sensor triggers are suppressed.

### B. Lift Detection (Barometer)
Uses `BARO_LIFT_THRESHOLD_METERS` (0.8m) to detect vertical rise.

### C. Tilt Detection (Rotation Vector)
Detects orientation changes exceeding `TILT_THRESHOLD_DEGREES` (15°).

### D. Light Jump (EMA Baseline) (Issue #372)
Uses asymmetrical EMA to detect sudden illumination spikes.

### E. Acoustic Sentinel
Gated by `ACOUSTIC_MIN_THRESHOLD_DB` (50.0dB) to prevent false positives in silent environments.

### F. Adaptive Vibration Normalization
Uses `VIBRATION_STATIONARY_THRESHOLD` (0.12f) for motion discrimination (Issue #318).

## 4. Forensic Continuity (v8.9.51 / DB v50)
Physical violations are logged with extended forensic metadata:
1.  **Dual-Metric Spatial Anchor (Issue #325)**: All tamper, lift, and tilt events are anchored with `lat`, `lng`, `accuracy`, and `maxAccuracy`. This provides a forensic audit trail of the engine's uncertainty at the exact moment of the violation.
2.  **Monotonic Timing (Issue #311)**: All timing deltas are calculated using `TimeProvider.elapsedRealtime()`.
3.  **Ghost Mode UX (Issue #338)**: Visual staleness indicators are applied when telemetry is older than 10s.
4.  **SIT Acknowledgment (Issue #194)**: Discrete SIT events are synchronized via a 10s acknowledged loop.

## 5. State Integration
When a physical violation occurs:
1.  The system transitions to `SUSPICIOUS` state.
2.  GPS polling increases to `SUSPICIOUS_GPS_POLLING_MS` (1000ms).
3.  Kalman/ImmFilter Process Noise (Q) is scaled up via `SUSPICIOUS_Q_SCALE` (1000.0).
