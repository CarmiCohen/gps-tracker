# GtoEngine - Heavy Trajectory Optimization Vision (v8.9.37)

The **GtoEngine** is the project's specialized spatial intelligence layer, designed to filter mechanical jitter and reject coordinate "Jumps" with high-assurance forensic logic.

## 1. Physical Isolation
With the **v8.9.37 Baseline**, the engine is strictly isolated in the `:core:engine` module (a pure `java-library`). This ensures that zero Android framework classes (like `Location` or `Context`) leak into the tracking logic, enabling 100% deterministic unit testing.

## 2. Sensor Fusion Hierarchy
The engine moves beyond simple GPS filtering by correlating coordinate changes with real-time IMU signatures:
- **Accelerometer (IMU)**: Real-time mechanical vibration signatures to determine movement states (Stationary, Idle, Moving).
- **Barometer**: Atmospheric pressure deltas to validate vertical displacement.
- **GNSS Metadata**: Satellite-level SNR (Signal-to-Noise Ratio) to identify multi-path reflections in urban canyons.

## 3. The "Rubber-Band" Logic (Hindsight Correction)
The GtoEngine utilizes a 30s sliding window (`TRAJECTORY_PROMOTION_WINDOW_MS`) to evaluate suspicious movement:
- **Scenario A**: A jump is detected but doesn't continue. **Decision**: Reject as jitter.
- **Scenario B**: Next points continue from the jump location. **Decision**: The jump was real high-speed movement. The engine retroactively "promotes" the point and smooths the historical trail.

## 4. Key Hardening Features
- **Monotonic Integrity**: Uses monotonic time via `TimeProvider` (Issue #283) for all lockout windows.
- **Authoritative Uncertainty**: UI circles reflect engine-calculated `maxAccuracy` rather than raw hardware reported values.
- **Jamming Detection**: A specialized flag raised when satellite health is erratic/degraded while mechanical movement is detected.
