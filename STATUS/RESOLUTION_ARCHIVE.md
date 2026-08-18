# Issues Archive (Historical Resolutions)

This document contains the unified record of all resolved issues and technical debt for the GPS-Tracker system.

**Total Unique Resolutions: 645**

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

## 58. Migration Recovery & Schema Hardening (Aug.17.10)
*   **Issue #195: Database Migration Crash Loop**.
    - **Resolution**: Resolved a critical startup crash caused by `UNIQUE constraint` violations and schema mismatches in `AppDatabase`. Hardened migrations `68` through `72` to explicitly drop legacy indices before creation. Implemented a recovery migration (v72) to force-fix the `connection_history` table schema (`sitVzRt` column). Registered `MIGRATION_71_72` in `AppModule.kt` to restore system availability. (R195)

## 57. Forensic Persistence & Thermal Recovery (Aug.17.10)
*   **Issue #193: Forensic Signature Persistence Audit**.
    - **Resolution**: Instrumented `ForensicSpillBuffer` initialization to audit and log the restoration of forensic traces from the memory-mapped `forensic_spill.bin` file during initialization. Verified that write/read indices and entry counts are correctly recovered after process death, ensuring zero-data-loss during thermal recovery windows. (R193)
*   **Issue #192: Automated Recovery Latency Audit**.
    - **Resolution**: Instrumented `TrackerService.startForensicSamplingLoop` to detect the transition from Cooling Mode to Active Mode. The system now logs the precise latency until high-frequency sampling resumes, ensuring temporal fidelity is maintained post-throttle. (R192)
*   **Issue #191: Heat Mitigation Validation**.
    - **Resolution**: Implemented `SimulateThermalEvent` trigger in `MainViewModel` and `TrackerService`. Verified the dynamic throttling mechanism that forces the forensic sampling interval to 500ms during thermal events. (R191)
*   **Issue #190: Database Migration Failure (v68-v71)**.
    - **Resolution**: Hardened `AppDatabase` by implementing aggressive deduplication in migrations, removing invalid `UNIQUE` constraints on `localId`, and restoring missing columns in `connection_history`. (R190)
*   **Issue #189: Forensic Stress Test**.
    - **Resolution**: Successfully executed 5-minute CPU/IO saturation routine at 100Hz on API 35. Verified system survival and recovery transition. (R189)

## 56. Sensor Startup Hardening (Aug.16.14)
*   **Issue #186: Gated Sensor Startup**.
    - **Resolution**: Implemented a deferred sensor registration mechanism in `AppSensorManager`. High-frequency sensors (Accelerometer, Linear Accel) are now gated by a 2000ms settling delay (`SENSOR_SETTLING_DELAY_MS`) upon service start. This prevents IPC/Binder saturation during the critical first 2 seconds of Tracker/Viewer entry. (R186)

## 55. Viewer Service Stabilization (Aug.17.01)
*   **Issue #188: Build Regression in ViewerService**.
    - **Resolution**: Restored build stability by fixing invalid string template escaping and correcting the unresolved reference for forensic telemetry (`peakVibrationShock`). Verified the coordinate alignment between local and remote processors in the Viewer role. (R188)

## 54. Map Hydration & IO Hardening (Aug.16.13)
*   **Issue #185: Startup ANR during Map Hydration**.
    - **Resolution**: Eliminated main-thread saturation by offloading trail segment hashing and simplification to background threads. `MapTrailSegment` now carries a pre-computed `checksum` calculated in the `MainViewModel`, allowing `MapOverlayManager.updateTrails` to perform O(1) change detection. (R185)
*   **Issue #184: Stress Test IO Race Condition**.
    - **Resolution**: Hardened the forensic stress test `ioJob` in `TrackerService` to use unique timestamps in filenames and internal try-catch blocks. (R184)
*   **Issue #183: Startup OOM in Tracker Mode**.
    - **Resolution**: Reduced trail and violation retrieval limits from 10,000 to 2,000 in `Database.kt`. (R183)

## 53. Map & Startup Hardening (Aug.16.00)
*   **Issue #182: Startup ANR & GC Thrashing**.
    - **Resolution**: Eliminated the massive allocation churn in the map rendering pipeline. Increased `STARTUP_SETTLING_DELAY_MS` to 10s. (R182)
*   **Issue #181: DeadSystemException on Startup**.
    - **Resolution**: Addressed Binder exhaustion by increasing the startup settling delay to 10,000ms. (R181)

... (rest of archive)
