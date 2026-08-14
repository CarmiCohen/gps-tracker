# Issues Archive (Historical Resolutions)

This document contains the unified record of all resolved issues and technical debt for the GPS-Tracker system.

**Total Unique Resolutions: 612**

## 45. Forensic Replay UI Audit (Aug.14.02)
*   **Issue #170: Forensic Replay UI Audit**.
    - **Resolution**: Restored coordinate-aware scrubbing functionality in the Analytical Ribbons. Implemented `replayCursorTs` synchronization between the telemetry visualization and the map layer. Leveraged binary search for frame-perfect matching of historical coordinates during 100Hz playback simulation. Integrated a high-visibility Replay Cursor marker to verify zero-drift alignment between sensor spikes (e.g., vibration) and spatial positioning. (R170)

## 44. Geofence Accuracy vs. Battery Audit (Aug.14.01)
*   **Issue #169: Geofence Accuracy vs. Battery Audit**.
    - **Resolution**: Resolved a "false-secure" risk where moving devices with screen-off dropped to 45s GPS polling. Updated `ServiceBehaviorUseCase` to maintain a safe 5s/2s polling interval whenever a geofence is active (R406a). Verified integrity via `GeofenceBatteryAuditTest` (Bayesian drift, status transitions) and `ServiceBehaviorAuditTest`. (R169)

## 43. Forensic Trace Persistence Stress Test (Aug.14.00)
*   **Issue #165: Forensic Trace Persistence Stress Test**.
    - **Resolution**: Implemented a 5-minute sustained 100Hz stress test routine in `TrackerService` to audit database throughput and buffer drainage. Verified that R167 pruning cooldowns and R164 path hardening prevent SQLite contention and Main-thread stalls. (R165)

## 42. Database Pruning Optimization (Aug.14.00)
*   **Issue #167: Database Pruning Thrash**.
    - **Resolution**: Increased `DB_PRUNE_THRESHOLD` to 500 and implemented a **1-minute temporal cooldown** (`PRUNE_COOLDOWN_MS`) in `LogRepository` to prevent SQLite lock contention during high-frequency forensic sampling. (R167)

## 41. Settings Overlay ANR Remediation (Aug.14.00)
*   **Issue #166: Settings Overlay ANR**.
    - **Resolution**: Resolved Main-thread stalls by implementing **Staggered Hydration** in `SettingsOverlay` (60ms offsets) and throttling `eventLogsFlow` using `sample(500ms)`, eliminating object churn from the UI path. (R166)

## 40. Build Restoration (Aug.13.14)
*   **Issue #168: Build Restoration**.
    *   **Resolution**: Fixed compilation errors in `LogRepository.kt` flush logic where 'it' was incorrectly used in a nested lambda. (R168)

## 39. Forensic Log Path Hardening (Aug.13.13)
*   **Issue #164: Forensic Log Buffer Audit**.
    *   **Resolution**: Hardened the 100Hz forensic logging path via deterministic composite IDs (`F-timestamp-idx`) and raw snapshots in `LogEntry`. Expanded spill and buffer capacities. (R164)

## 38. 1Hz Telemetry Path Optimization (Aug.13.12)
*   **Issue #163: Telemetry Path Churn**.
    *   **Resolution**: Eliminated object churn in the 1Hz telemetry path by refactoring `DashboardState` to use primitive types and moving formatting to UI layer via `remember` blocks (R163).

## 37. Phone Setup ANR Remediation (Aug.13.11)
*   **Issue #162: Phone Setup ANR Stall**.
    *   **Resolution**: Implemented 150ms hydration gate and 80ms staggered rendering offsets in `PhoneSetupOverlay` (R162).

## 36. Version Synchronization (Aug.13.10)
*   **Issue #160: Version Mismatch**.
    *   **Resolution**: Synchronized build system and UI to `Aug.13.10`. (R160)

## 35. False Positive Remediation (Aug.13.10)
*   **Issue #161: Persistent Denials (False Positive)**.
    *   **Resolution**: Verified SDK-aware branching in `SystemStatusProviderImpl.kt`. (R161)

## 34. SELinux Telemetry Remediation (Aug.13.10)
*   **Issue #159: SELinux LoadAvg Denials**.
    *   **Resolution**: Bypassed restricted `/proc` reads on Android 10+ (R159).

## 33. Forensic Validation & QA Audit (Aug.13.09)
*   **Issue #158: Forensic Validation & QA Audit**.
    *   **Resolution**: End-to-end audit of performance hardening (R152-R157). (R158)

## 32. Violation Path Optimization (Aug.13.09)
*   **Issue #157: Violation Path Allocations**.
    *   **Resolution**: Eliminated object churn in violation detection via mutable points and caching (R157).

## 31. System Log Hardening (Aug.13.08)
*   **Issue #156: WakeLock Log Saturation**.
    *   **Resolution**: Implemented WakeLock log throttling (1/min). (R156)

## 30. Phone Setup UI Refinement (Aug.13.07)
*   **Issue #155: Phone Setup UI Clutter**.
    *   **Resolution**: Hidden completion-dependent buttons in `GuideSection` (R155).

## 29. Telemetry GC Pressure Mitigation (Aug.13.06)
*   **Issue #152: Excessive GC Pressure**.
    *   **Resolution**: Implemented Telemetry Flyweight Pooling. (R152)

## 28. Startup Davey Stall Mitigation (Aug.13.05)
*   **Issue #153: Startup Davey Stalls**.
    *   **Resolution**: Implemented Staggered UI Hydration using `hydrationLevel` (R153).

## 27. Samsung A15 Detection Hardening (Aug.13.04)
*   **Issue #150: Samsung A15 Phone Setup Bypass**.
    *   **Resolution**: Hardened A15 detection via device/product string inspection (R405).

---
*For older resolutions, see Git history or backlog shards. (vAug.14.02)
