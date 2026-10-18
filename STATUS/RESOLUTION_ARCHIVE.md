# Resolution Archive

## ISSUE-OCT18.10-F1: Adaptive Vibration Floor Convergence (Oct18.10)
- **Resolved**: Oct18.10
- **Description**: The adaptive vibration floor was slow to converge when transitioning from high-vibration (e.g., in-vehicle) to stationary environments, leading to delayed stationary locks and false "Suspicious" alerts.
- **Remediation**:
    - **Steep-Drop Logic (R-ID 736)**: Implemented an accelerated EMA alpha (`0.35`) when vibration energy drops below 40% of the current floor.
    - **Native/JVM Parity**: Updated both `SentinelValidator` (JVM Fallback) and `JdHardwareManager` (Native Fallback) to use synchronized convergence math.
    - **Boundary Hardening**: Ensured all convergence paths strictly adhere to `VIBRATION_FLOOR_MIN` (0.02G) and `VIBRATION_FLOOR_MAX` (2.0G).
- **Outcome**: 🟢 **PASSED**. Convergence from vehicle-level vibration (0.5G) to stationary floor (0.05G) is now significantly faster, improving geofence stability and stealth performance. (SOT ID 766).

## ISSUE-OCT18.8-F1: Urban Multi-path Hardening (Oct18.8)
- **Resolved**: Oct18.8
- **Description**: GNSS reflections in "urban canyons" often present with high SNR (Signal-to-Noise Ratio), misleading traditional jump filters. Strong but inaccurate reflections could trigger false geofence alerts or cause the stationary anchor to break out prematurely.
- **Remediation**:
    - **SNR Latch (R-ID 735)**: Implemented a 6-minute (360s) security hold for jumps that present with SNR ≥ 35. This ensures the system remains in a "hold" state long enough for the reflection burst to clear.
    - **High-SNR Reflection Detection**: Updated `PhysicsUtils.isVisualJump` to detect and flag high-SNR reflections (Potential Multipath).
    - **Anchor Skepticism**: Enhanced `AnchorEvaluator` to apply a 0.25x penalty (extra skepticism) to breakout score accumulation when high-SNR reflections are detected while physically stationary.
    - **State Propagation**: Expanded `SentinelResult`, `ProcessedLocation`, and `AlarmEvaluationState` to propagate the `isHighSnrReflection` flag through the engine layers.
- **Outcome**: 🟢 **PASSED**. The engine is now resilient against high-SNR urban reflections. False breakouts and geofence alerts in dense urban environments are suppressed via the adaptive latch. (SOT ID 765).

... [Remaining entries preserved] ...
