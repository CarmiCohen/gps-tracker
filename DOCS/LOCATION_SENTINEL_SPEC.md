# Location Sentinel Specification (v8.9.10)

The **Location Sentinel** is a multi-modal sensor fusion engine designed to detect unauthorized physical interaction with the tracker. In v8.9.10, this logic is strictly isolated within the `:core:engine` module and geographically anchored via **Log Spatial Anchors** (Issue 208).

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
| **Lift** | Δ Alt > 0.8m | `Tracker: Lift` | Immediate Siren |
| **Tilt** | Δ Orientation > 15° | `Tracker: Tilt Alert` | Immediate Siren |
| **Light Jump** | Δ Lux > 150 | `Tracker: Tamper Detected` | Immediate Siren |
| **Proximity** | NEAR → FAR (Debounced) | `Tracker: Tamper Detected` | Immediate Siren |
| **Acoustic (High)** | Δ Noise > 40dB | `Tracker: Acoustic Alert` | Immediate Siren |
| **Acoustic (Low)** | Δ Noise > 20dB | `None` | Suspicious State |
| **Shock** | Peak > max(0.8g, Floor × 7.0) | `Tracker: Tamper Detected` | Immediate Siren |
| **Vibration** | Level > max(0.25g, Floor × 2.5) | `None` | Suspicious State |

## 3. Implementation Details

### A. Muzzle Window
The sentinel implements a 2000ms `MUZZLE_WINDOW_DURATION_MS` during sync operations. While `isMuzzled` is true, sensor triggers are suppressed to prevent interference-based false positives. Includes device-specific hysteresis.

### B. Lift Detection (Barometer)
If the current pressure indicates a vertical rise of more than 0.8 meters, the system assumes the vehicle is being lifted.

### C. Tilt Detection (Rotation Vector)
Calculates the angular delta from the point of activation. If the device is rotated more than 15 degrees, a tilt violation is recorded.

### D. Light Jump (EMA Baseline)
Sudden light spikes (> 150 lux over baseline) trigger a tamper alert.

### E. Acoustic Sentinel
Triggers are gated by a **50dB** absolute floor to prevent false positives in silent environments.

### F. Adaptive Vibration Normalization
Vibration thresholds are dynamic and normalized against the hardware noise floor using EMA.

## 4. Forensic Continuity (v8.9.10)
Physical violations are logged with forensic metadata:
1.  **Log Spatial Anchor (Issue 208)**: All tamper, lift, and tilt events are now automatically anchored with `lat`/`lng` coordinates to show exactly where the physical violation occurred.
2.  **Monotonic Timing**: All timing deltas and violation durations are calculated using `TimeProvider.elapsedRealtime()`.
3.  **Ghost Mode UX**: Visual staleness indicators are applied to all sensor-derived fields when telemetry is older than 10s.
4.  **SIT Acknowledgment**: Discrete SIT events are synchronized via a 10s acknowledged loop to ensure persistence.

## 5. State Integration
When a physical violation occurs:
1.  The system transitions to `SUSPICIOUS` state.
2.  GPS polling increases to 1Hz.
3.  Kalman/ImmFilter Process Noise (Q) is scaled up.
