# Issues Archive (Historical Resolutions)

This document contains the unified record of all resolved issues and technical debt for the GPS-Tracker system.

**Total Unique Resolutions: 641**

## 62. Forensic Performance & JNI Optimization (Aug.18.06)
*   **Issue #202: Forensic Performance: JNI Memory Pressure Audit**.
    - **Resolution**: Eliminated intermediate heap allocations in the forensic log draining pipeline. Implemented `peekToEntities` in `ForensicSpillBuffer.kt` to directly map off-heap buffer data to `LogEntity` (Room) objects. Updated `LogRepository.kt` to utilize this zero-churn path, removing the `LogEntry` allocation loop and significantly reducing GC pressure during sustained 100Hz telemetry bursts on budget hardware. (R202)

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

... (rest of archive)
