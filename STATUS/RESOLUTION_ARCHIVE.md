# Issues Archive (Historical Resolutions)

This document contains the unified record of all resolved issues and technical debt for the GPS-Tracker system.

**Total Unique Resolutions: 646**

## 66. Diagnostic Stress Isolation (Aug.18.08)
*   **Issue #204: Diagnostic Stress Isolation (Sensor Sampling Rates)**.
    - **Resolution**: Implemented temporary diagnostic down-sampling to isolate the impact of high-frequency sensor processing and telemetry logging. Forensic sampling intervals were reduced from 100Hz/10Hz to 4Hz (250ms) and 2Hz (500ms) in `EngineConstants.kt`. Hardware IMU listeners in `AppSensorManager.kt` were switched from `SENSOR_DELAY_FASTEST` to `SENSOR_DELAY_NORMAL`. This diagnostic state allows isolation of context-switching and I/O pressure as root causes for thermal and battery instability. (R204)

## 65. Forensic Alignment & Temporal Hardening (Aug.18.07)
*   **Issue #203: Forensic Multi-Session Alignment Audit (Temporal Hardening)**.
    - **Resolution**: Hardened the forensic telemetry pipeline against temporal jitter and duplication across service restarts. Refactored `ForensicSpillBuffer.kt` to store absolute `Long` timestamps and `Double` coordinates (v3) in the memory-mapped buffer, eliminating session base-time dependencies and overflow risks. Implemented signature-based deduplication (timestamp + `spillIdx`) in `LogRepository.kt` before database insertion to ensure idempotency and strict temporal monotonicity during recovery from "dirty" shutdowns or crashes. (R203)

## 64. Forensic Performance & JNI Optimization (Aug.18.06)
*   **Issue #202: Forensic Performance: JNI Memory Pressure Audit**.
    - **Resolution**: Eliminated intermediate heap allocations in the forensic log draining pipeline. Implemented `peekToEntities` in `ForensicSpillBuffer.kt` to directly map off-heap buffer data to `LogEntity` (Room) objects. Updated `LogRepository.kt` to utilize this zero-churn path, removing the `LogEntry` allocation loop and significantly reducing GC pressure during sustained 100Hz telemetry bursts on budget hardware. (R202)

## 63. Urban Multipath Mitigation (Aug.18.05)
*   **Issue #201: Urban Edge Case: Multipath Mitigation Audit (Core Hardening)**.
    - **Resolution**: Hardened stationary state management against GPS signal bouncing in urban canyons (multipath). Modified `AnchorEvaluator.kt` to prevent binary anchor release when GPS-derived confidence drops, provided the IMU confirms the device is physically stationary and SNR is low (indicating signal bounce). Refined `LocationSentinel.kt` to dampen `stationaryProb` decay during low-SNR physically stationary events, preventing jittery state transitions. (R201)

## 62. Forensic UI Performance & Recomposition (Aug.18.03)
*   **Issue #198: Forensic UI Performance & Recomposition Audit**.
    - **Resolution**: Hardened the UI telemetry pipeline by implementing `.sample(100L)` on high-frequency `LocationUpdate` collectors in `MainViewModel.kt`. Capped UI processing at 10Hz to prevent Main thread saturation during 100Hz forensic bursts while maintaining fluid visual motion. (R198)

## 61. Forensic Storage & Pruning Hardening (Aug.18.02)
*   **Issue #197: Forensic Storage-Aware Adaptive Pruning Refinement**.
    - **Resolution**: Hardened storage management for 100Hz sampling by implementing forensic-specific retention limits in `EngineConstants.kt`. Optimized `LogDao` with chunk-based pruning for `FORENSIC_TRACE` entries to minimize transaction lock duration during heavy I/O. Refined `proactivePruning` in `LogRepository.kt` to adaptively throttle pruning intensity based on `SystemHealthState` (Storage Critical/Low, Battery Charging/Low). (R197)

## 60. Forensic Buffer & Pressure Hardening (Aug.18.00)
*   **Issue #196: Forensic Log Buffer Pressure Audit**.
    - **Resolution**: Hardened the forensic logging pipeline to prevent `FORENSIC_OVERFLOW` during 100Hz sampling. Increased `LOG_BUFFER_CAPACITY` to 5000 and `LOG_BATCH_SIZE` to 100 in `EngineConstants.kt`. Refined `LogRepository.kt` to initiate forensic draining at 25% fill level (lowered from 50%) and modified the drainer loop to prioritize buffer relief during high-pressure events even under heavy CPU load. (R196)

## 59. Battery Health & Logic Hardening (Aug.17.11)
*   **Issue #194: Battery Steep Discharge Logic Hardening**.
    - **Resolution**: Refined `checkBatteryDischarge()` in `IntegrityMonitor.kt` to utilize load-aware thresholds. Introduced `BATTERY_STEEP_DISCHARGE_THRESHOLD_NORMAL` (4%) and `BATTERY_STEEP_DISCHARGE_THRESHOLD_HIGH_LOAD` (8%) in `EngineConstants.kt`. The system now dynamically selects the threshold based on CPU load (>70%) and thermal throttling status, preventing false positives during 100Hz forensic sampling and stress tests while maintaining high sensitivity during idle states. (R194)

... (rest of archive)
