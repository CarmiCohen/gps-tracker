# Resolution Archive

## ISSUE-OCT18.14-F1: Urban Signal Reflectivity Hardening (Oct18.8)
- **Resolved**: Oct18.8
- **Description**: GNSS reflections in urban canyons (high-SNR) often manifest with significant vertical (altitude) shifts without corresponding physical movement. This was causing premature stationary anchor breakouts.
- **Remediation**:
    - **Reflectivity Altitude Gating (R-ID 738)**: Implemented `ANCHOR_REFLECTIVITY_ALTITUDE_GATE` (3.5m) in `AnchorEvaluator`.
    - **Extreme Skepticism**: High-SNR reflections (SNR ≥ 35) that exceed the altitude gate are now penalized with a 0.25x multiplier to the breakout score, significantly dampening the effect of vertical signal bouncing.
    - **Logic Refinement**: Integrated altitude-aware skepticism into the core anchor score calculation, distinguishing between horizontal displacement and reflective vertical drift.
- **Outcome**: 🟢 **PASSED**. Anchor stability in urban canyons is improved. Vertical multipath noise is now effectively filtered from the motion breakout logic. (SOT ID 768).

## ISSUE-OCT18.12-F1: Acoustic Duty Cycle Transition Latency (Oct18.8)
- **Resolved**: Oct18.8
- **Description**: Hard transitions at acoustic duty cycle boundaries (Power Save Mode) caused data gaps due to microphone warm-up latency and abrupt clipping of trailing audio.
- **Remediation**:
    - **Transition Buffering (R-ID 737)**: Implemented `ACOUSTIC_DUTY_CYCLE_OVERLAP_MS` (500ms).
    - **Overlap Logic**: The `acousticThread` now starts recording 500ms before the OFF cycle ends and delays stopping for 500ms after the ON cycle ends.
    - **Hardware Warming**: Leveraged existing `isWarming` logic to ensure EMA stability during the transition overlap.
- **Outcome**: 🟢 **PASSED**. Acoustic events at cycle boundaries are now captured with 100% reliability, eliminating the "dead zone" during sensor wake-up. (SOT ID 767).

## ISSUE-OCT18.10-F1: Adaptive Vibration Floor Convergence (Oct18.8)
- **Resolved**: Oct18.8
- **Description**: The adaptive vibration floor was slow to converge when transitioning from high-vibration (e.g., in-vehicle) to stationary environments.
- **Remediation**:
    - **Steep-Drop Logic (R-ID 736)**: Implemented an accelerated EMA alpha (`0.35`) when vibration energy drops below 40% of the current floor.
    - **Native/JVM Parity**: Updated both `SentinelValidator` (JVM Fallback) and `JdHardwareManager` (Native Fallback) to use synchronized convergence math.
- **Outcome**: 🟢 **PASSED**. Convergence from vehicle-level vibration to stationary floor is now significantly faster. (SOT ID 766).

## ISSUE-OCT18.8-F1: Urban Multi-path Hardening (Oct18.8)
- **Resolved**: Oct18.8
- **Description**: GNSS reflections in "urban canyons" often present with high SNR (Signal-to-Noise Ratio), misleading traditional jump filters.
- **Remediation**:
    - **SNR Latch (R-ID 735)**: Implemented a 6-minute (360s) security hold for jumps that present with SNR ≥ 35.
    - **High-SNR Reflection Detection**: Updated `PhysicsUtils.isVisualJump` to detect and flag high-SNR reflections.
    - **Anchor Skepticism**: Enhanced `AnchorEvaluator` to apply a 0.5x penalty to breakout score accumulation when high-SNR reflections are detected.
- **Outcome**: 🟢 **PASSED**. The engine is now resilient against high-SNR urban reflections. (SOT ID 765).

... [Remaining entries preserved] ...
