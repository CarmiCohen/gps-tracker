# SOT Master Requirements & Hardening Status (Oct18.12)

## 🏗️ Architectural Master Rules (293 Rules)

### 1. Lifecycle & Resource Management
*   **1.1** ... (Historical rules omitted)
*   **1.219 Acoustic Duty Cycle Overlap (R-ID 737)**: To ensure gapless capture of acoustic events during power-save duty cycles, the microphone MUST start recording at least 500ms (OVERLAP_MS) before the OFF cycle ends and MUST continue recording for at least 500ms after the ON cycle transition. This facilitates hardware warm-up and prevents loss of trailing audio data. (Oct18.12 - ISSUE-OCT18.12-F1).
*   **1.218 Steep-Drop Vibration Convergence (R-ID 736)**: The adaptive vibration floor MUST implement accelerated downward convergence (Steep-Drop) when the current vibration energy drops below 40% of the existing floor. This ensures rapid recovery of the "STATIONARY" gate when transitioning from high-vibration (e.g., in-vehicle) to stable environments, preventing stale noise baselines from causing false alerts. (Oct18.10 - ISSUE-OCT18.10-F1).
*   **1.217 SNR Latch for Urban Multipath (R-ID 735)**: The system MUST implement a sustained security hold (SNR Latch) for location jumps that present with high Signal-to-Noise Ratio (SNR ≥ 35). This latch MUST extend the geofence hold duration to 360s and increase stationary anchor skepticism to prevent false alerts caused by strong signal reflections in urban canyons. (Oct18.8 - ISSUE-OCT18.8-F1).
*   **1.216 Synthetic Physical Validation (R-ID 734)**: The system MUST provide synthetic injection hooks for all physical hardware dependencies (Shaking, GNSS signal loss, Power tampering, Network availability) to allow for automated and remote validation of hardening logic. (Oct18.8 - ISSUE-OCT18.6-F1).
*   **1.215 Explicit Component Lifecycle (R-ID 733)**: Core repositories and managers MUST NOT initiate background processing or side-effect heavy operations within their constructors. (Oct18.8 - SIMP-OCT18.2-S1).

...

## 🛡️ Core Hardening Baseline
*   **SOT ID 767**: Acoustic Duty Cycle Overlap - PASSED. Implemented 500ms transition buffering in `HardwareSuite.acousticThread` to mitigate microphone warm-up latency. (Oct18.12 - ISSUE-OCT18.12-F1).
*   **SOT ID 766**: Adaptive Vibration Floor Convergence - PASSED. Implemented Steep-Drop convergence logic in `SentinelValidator` and synchronized JNI fallback. (Oct18.10 - ISSUE-OCT18.10-F1).
*   **SOT ID 765**: Urban Multipath Hardening - PASSED. Implemented 360s SNR Latch and high-SNR anchor skepticism. Updated `PhysicsUtils` and `MainAlarmLogic` to handle signal reflections. (Oct18.8 - ISSUE-OCT18.8-F1).
*   **SOT ID 764**: Synthetic Physical Injection - PASSED. Implemented `SyntheticEventInjector` with hooks for SHAKE, GNSS_LOSS, POWER_TAMPER, and NETWORK_LOSS. (Oct18.8 - ISSUE-OCT18.6-F1).
*   **SOT ID 763**: Explicit Repository Lifecycle - PASSED. Refactored `LogRepository` and `LogManager` to use explicit `start()` triggers. (Oct18.8 - SIMP-OCT18.2-S1).

... [Remaining entries preserved] ...
