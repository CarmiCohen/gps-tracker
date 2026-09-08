# Project Issues & Hardening Tracking (Sep.08.20)

## 🎯 Current Resumption Focus: Structural Refinement & Build Restoration
Telemetry model flattening is in-flight (Idea #2). Build partially restored via property bridges in `LocationUpdate`. Final migration of `MainRepository`, `TelemetryMapper`, and `ConnectivitySuite` required.

## 🟡 Open Issues & Hardening Tasks (Sorted by Implementation Priority)
*   **Telemetry Data Class Flattening (IN-PROGRESS)**: (Idea #2). Monolithic `LocationUpdate` partitioned into `Kinetic`, `Atmospheric`, and `Integrity` states. **NEXT**: Update `MainRepository`, `TelemetryMapper`, and `ConnectivitySuite` to use nested objects and remove legacy property bridges.
*   **Alarm Logic Partitioning**: (Idea #3). Partition `evaluateAlarms` in `AlarmManager` into specialized evaluators (Physical, Health, Connectivity) to reduce cyclomatic complexity.
*   **Legacy Field Cleanup**: Once flattening is stable and all consumers are migrated, remove the bridge properties from `LocationUpdate`.

## 🟢 Recently Resolved Issues (Sep.08.20)
*   **Issue #283 RESOLVED: Teardown Crash (NOT NULL Constraint)**. 
    *   **Root Cause**: `Iterable<Double>.average()` returned `NaN` for empty satellite lists during teardown. Room/SQLite driver mapped `NaN` to `NULL`, violating the `NOT NULL` constraint on `snrIdx`.
    *   **Remediation**: Implemented `safeAverage()` extension in `TelemetryUtils.kt`. Hardened `TrackerService.kt`, `DashboardStateProvider.kt`, and `HardwareProvider.kt` with `isNaN()` checks and empty-collection guards (R-ID 283).
*   **R-ID 284 PARTIAL: Build Restoration**. Implemented property-level delegates in `LocationUpdate.kt` to allow legacy flat field access while data resides in nested state objects, restoring compilation for `TrackerService` and `ViewerService`.

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 301 (Rules: 53, IDs: 248), Resolved: 950, Open: 2, Testing: 95% (Sub-items: 48), Ideas: 4, QA: 267]**

*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vSep.08.20)*
