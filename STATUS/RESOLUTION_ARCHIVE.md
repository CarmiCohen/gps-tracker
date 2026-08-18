# Issues Archive (Historical Resolutions)

This document contains the unified record of all resolved issues and technical debt for the GPS-Tracker system.

**Total Unique Resolutions: 638**

## 59. Battery Health & Logic Hardening (Aug.17.11)
*   **Issue #194: Battery Steep Discharge Logic Hardening**.
    - **Resolution**: Refined `checkBatteryDischarge()` in `IntegrityMonitor.kt` to utilize load-aware thresholds. Introduced `BATTERY_STEEP_DISCHARGE_THRESHOLD_NORMAL` (4%) and `BATTERY_STEEP_DISCHARGE_THRESHOLD_HIGH_LOAD` (8%) in `EngineConstants.kt`. The system now dynamically selects the threshold based on CPU load (>70%) and thermal throttling status, preventing false positives during 100Hz forensic sampling and stress tests while maintaining high sensitivity during idle states. (R194)

## 58. Migration Recovery & Schema Hardening (Aug.17.10)
*   **Issue #195: Database Migration Crash Loop**.
    - **Resolution**: Resolved a critical startup crash caused by `UNIQUE constraint` violations and schema mismatches in `AppDatabase`. Hardened migrations `68` through `72` to explicitly drop legacy indices before recreation. Implemented a recovery migration (v72) to force-fix the `connection_history` table schema (`sitVzRt` column). Registered `MIGRATION_71_72` in `AppModule.kt` to restore system availability. (R195)

## 57. Forensic Persistence & Thermal Recovery (Aug.17.10)
*   **Issue #193: Forensic Signature Persistence Audit**.
    - **Resolution**: Instrumented `ForensicSpillBuffer` initialization to audit and log the restoration of forensic traces from the memory-mapped `forensic_spill.bin` file. Verified that write/read indices and entry counts are correctly recovered after process death, ensuring zero-data-loss during thermal recovery windows. (R193)
*   **Issue #192: Automated Recovery Latency Audit**.
    - **Resolution**: Instrumented `TrackerService.startForensicSamplingLoop` to detect the transition from Cooling Mode to Active Mode. The system now logs the precise latency until high-frequency sampling resumes, ensuring temporal fidelity is maintained post-throttle. (R192)
*   **Issue #191: Heat Mitigation Validation**.
    - **Resolution**: Implemented `SimulateThermalEvent` trigger in `MainViewModel` and `TrackerService`. Verified the dynamic throttling mechanism that forces the forensic sampling interval to 500ms during thermal events. (R191)
*   **Issue #190: Database Migration Failure (v68-v71)**.
    - **Resolution**: Hardened `AppDatabase` by implementing aggressive deduplication in migrations, removing invalid `UNIQUE` constraints on `localId`, and restoring missing columns in `connection_history`. (R190)
*   **Issue #189: Forensic Stress Test**.
    - **Resolution**: Successfully executed 5-minute CPU/IO saturation routine at 100Hz on API 35. Verified system survival and recovery transition. (R189)

... (rest of archive)
