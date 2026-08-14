# Issues Archive (Historical Resolutions)

This document contains the unified record of all resolved issues and technical debt for the GPS-Tracker system.

**Total Unique Resolutions: 609**

## 41. Database Pruning Optimization (Aug.14.00)
*   **Issue #167: Database Pruning Thrash**.
    - **Resolution**: Increased `DB_PRUNE_THRESHOLD` to 500 and implemented a **1-minute temporal cooldown** (`PRUNE_COOLDOWN_MS`) in `LogRepository` to prevent SQLite lock contention during high-frequency forensic sampling. (R167)

## 40. Settings Overlay ANR Remediation (Aug.14.00)
*   **Issue #166: Settings Overlay ANR**.
    - **Resolution**: Resolved Main-thread stalls by implementing **Staggered Hydration** in `SettingsOverlay` (60ms offsets) and throttling `eventLogsFlow` using `sample(500ms)`, eliminating object churn from the UI path. (R166)

## 39. Build Restoration (Aug.13.14)
*   **Issue #168: Build Restoration**.
    *   **Resolution**: Fixed compilation errors in `LogRepository.kt` flush logic where 'it' was incorrectly used in a nested lambda. (R168)

## 38. Forensic Log Path Hardening (Aug.13.13)
*   **Issue #164: Forensic Log Buffer Audit**.
    *   **Resolution**: Hardened the 100Hz forensic logging path via deterministic composite IDs (`F-timestamp-idx`) and raw snapshots in `LogEntry`. Expanded spill and buffer capacities. (R164)

## 37. 1Hz Telemetry Path Optimization (Aug.13.12)
*   **Issue #163: Telemetry Path Churn**.
    *   **Resolution**: Eliminated object churn in the 1Hz telemetry path by refactoring `DashboardState` to use primitive types and moving formatting to UI layer via `remember` blocks (R163).

---
*For historical resolutions #1 through #36, see Git history or backlog shards. (vAug.14.00)
