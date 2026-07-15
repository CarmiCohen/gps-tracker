# Location Sentinel Specification (v9.4.0)

The **Location Sentinel** is a multi-modal sensor fusion engine designed to detect unauthorized physical interaction with the tracker. In v9.4.0, the sentinel is fully integrated into the unified 2s heartbeat timing model.

## 1. Monitored Sensors
The Sentinel monitors the following hardware sensors:
1.  **Barometer**: Relative altitude changes.
2.  **Rotation Vector**: 3D orientation and tilt.
3.  **Light Sensor**: Ambient light levels.
4.  **Proximity**: Distance to mounting surface.
5.  **Microphone**: Ambient noise floor (dB).
6.  **Accelerometer**: Continuous vibration and shock indexing.

## 2. Violation Logic (Hard Gates)
Any of the following conditions will trigger a **Suspicious State** and an optional **Immediate Siren**. These thresholds are defined in `EngineConstants.kt`.

| Condition | Threshold | Alert Title | Action |
| :--- | :--- | :--- | :--- |
| **Lift** | Δ Alt > `BARO_LIFT_THRESHOLD_METERS` (0.8m) | `Lift` | Immediate Siren |
| **Tilt** | Δ Orientation > `TILT_THRESHOLD_DEGREES` (15°) | `Tilt Alert` | Immediate Siren |
| **Light Jump** | Δ Lux > `LIGHT_THRESHOLD_LUX_JUMP` (150.0f) | `Tamper Detected` | Immediate Siren |
| **Proximity** | NEAR → FAR (Debounced) | `Tamper Detected` | Immediate Siren |
| **Acoustic (High)** | Δ Noise (Jump) > `ACOUSTIC_THRESHOLD_DB_JUMP` (40.0) | `Acoustic Alert` | Immediate Siren |
| **Acoustic (Low)** | Δ Noise (Jump) > `ACOUSTIC_SUSPICIOUS_THRESHOLD_DB_JUMP` (20.0) | `None` | Suspicious State |
| **Shock** | Peak > `VIBRATION_SHOCK_THRESHOLD_G` (0.8g) | `Tamper Detected` | Immediate Siren |
| **Vibration** | Level > `VIBRATION_SUSPICIOUS_THRESHOLD_G` (0.25g) | `None` | Suspicious State |

*Note: All Acoustic triggers require the absolute signal to be above the safety floor of 50dB (`ACOUSTIC_FLOOR_MIN_DB`). Alert titles are standardized per R747 (Issue #424).*

## 3. Implementation Details

### A. Muzzle Window (Issue #191)
The sentinel implements a `MUZZLE_WINDOW_DURATION_MS` (2000ms) during sync operations. While `isMuzzled` is true, sensor triggers are suppressed.

### B. Siren Authority (Issue #441)
Siren operations are protected by a hardware-safety **30s auto-stop** (`SIREN_AUTO_STOP_MS`) and a **15s resume cooldown**. All silence latches utilize monotonic time to prevent clock-tamper bypass.

### C. Light Jump (EMA Baseline) (Issue #372)
Uses asymmetrical EMA to detect sudden illumination spikes.

### D. Acoustic Sentinel
Gated by `ACOUSTIC_MIN_THRESHOLD_DB` (50.0dB) to prevent false positives in silent environments. Thresholds are evaluated as relative **jumps** over the rolling floor.

## 4. Forensic Continuity (v9.4.0)
Physical violations are logged with extended forensic metadata:
1.  **Dual-Metric Spatial Anchor (Issue #325)**: All tamper, lift, and tilt events are anchored with `lat`, `lng`, `accuracy`, and `maxAccuracy`. **`maxAccuracy` is the exclusive authority** for trajectory deduplication (Issue #450).
2.  **Bayesian Uncertainty expansion (Issue #431)**: When location fixes are pending, spatial uncertainty expands at **15.0m/s** (Moving) or **1.5m/s** (Stationary), capped at **33.3m/s**. This expansion is synchronized between UI visualization and breach logic.
3.  **Monotonic Timing (Issue #311 / Issue #441)**: All timing deltas and hardware locks are calculated using `TimeProvider.elapsedRealtime()`.
4.  **Ghost Mode UX (Issue #338)**: Visual staleness indicators are applied when telemetry is older than 35s (aligned with unified heartbeat + jitter tolerance).

## 5. State Integration (Issue #501)
When a physical violation occurs:
1.  The system transitions to `SUSPICIOUS` state.
2.  GPS polling remains at the unified **2000ms** (`TICK_INTERVAL_MS`) to maintain background stability and avoid OS-level suppression.
3.  Kalman/ImmFilter Process Noise (Q) is scaled up via `SUSPICIOUS_Q_SCALE` (1000.0) to allow for faster adaptation to potential movement.
