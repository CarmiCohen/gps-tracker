# Location Sentinel Specification (v8.8.35)

The **Location Sentinel** is a multi-modal sensor fusion engine designed to detect unauthorized physical interaction with the tracker. In v8.8.35, this logic is strictly isolated within the `:core:engine` module and hardened with monotonic timing (Issue 125).

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
The sentinel implements a 500ms `MUZZLE_WINDOW_DURATION_MS` during sync operations. While `isMuzzled` is true:
-   Shock and Vibration triggers are suppressed.
-   Tilt, Light, and Proximity violations are gated.
-   Acoustic spikes are ignored.
This prevents false tamper triggers caused by hardware interference during heavy SQLite/Network I/O.

### B. Lift Detection (Barometer)
The sentinel tracks a slow-moving baseline of atmospheric pressure. If the current pressure indicates a vertical rise of more than 0.8 meters, it assumes the vehicle is being lifted or winched.

### C. Tilt Detection (Rotation Vector)
Using the `Sensor.TYPE_ROTATION_VECTOR`, the engine calculates the angular delta from the point of "System Activation." If the device is rotated more than 15 degrees in any axis, a tilt violation is recorded.

### D. Light Jump (EMA Baseline)
Ambient light is tracked using a 1% Exponential Moving Average (EMA). Sudden spikes (> 150 lux over baseline) trigger a tamper alert.

### E. Acoustic Sentinel
The system samples ambient noise floor. All acoustic triggers are gated by an absolute minimum of **50dB** (`ACOUSTIC_MIN_THRESHOLD_DB`) to prevent false positives in silent environments.
- **Suspicion**: Jump > 20dB above floor.
- **Violation**: Jump > 40dB above floor.
- **Lockout**: 1s lockout after fast-path events.

### F. Adaptive Vibration Normalization
Vibration thresholds are dynamic and normalized against the hardware noise floor (Adaptive Floor) using EMA.
- **Stationary Floor**: Level < 0.12g (`VIBRATION_STATIONARY_THRESHOLD`).

## 4. Forensic Continuity (v8.8.35)
Physical violations are logged with forensic metadata:
1.  **Monotonic Timing (Issue 125)**: All timing deltas and violation durations are calculated using `TimeProvider.elapsedRealtime()`.
2.  **Fuzzy Batching**: Repeated strikes or noise spikes are collapsed into single entries.
3.  **Identity Integrity**: Every forensic entry is tagged with the mandatory `role` field. Identity is preserved at the emission point (LogManager/SyncManager) via `BuildConfig.VERSION_NAME`.

## 5. State Integration
When a physical violation occurs:
1.  The system transitions to `SUSPICIOUS` state.
2.  GPS polling increases to 1Hz.
3.  Kalman/ImmFilter Process Noise (Q) is scaled up.
