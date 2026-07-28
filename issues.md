# Project Issues & Hardening Tracking (July.27.07)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 441 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #604] [Severity: Low] [Category: UI] Ribbon Density in 7D Scale**.
    - **Risk**: At the 7-day scale, 240 points may result in significant data aliasing if high-frequency kinetic bursts occur between samples.
    - **Concern**: Forensic interpretation might miss brief tamper events at high compression.

---

## 🔴 Open Issues
*   *(No active critical engine issues)*

---

## 🟢 Recently Resolved Issues (July.27.07)
*   **[Issue #605] [Severity: Med] [Category: Persistence] Forensic Log Latency Audit**.
    - **Resolution**: Conducted formal I/O audit of `LogRepository`. Optimized `LogEntity` performance by adding a composite index for metadata lookups `(type, role, deviceId, timestamp)` and a dedicated index for `synced` status. Decoupled `deepPruneLogs` from the telemetry insertion hot-path (R585) by refactoring `addLog` to trigger pruning asynchronously with an `AtomicBoolean` concurrency guard. Incremented database version to 61.
    - **Validation**: Verified write-ahead logging (WAL) remains active in `AppModule`. Pruning no longer blocks high-frequency telemetry writes under signaling stress.

*   **[Issue #603] [Severity: Normal] [Category: UI] Analytical Ribbon Optimization**.
    - **Resolution**: Integrated `kineticEnergy` (KNT) ribbon with calibrated 2.0G scale. Refactored drawing loops in `SharedUiComponents.kt` to O(N) performance by consolidating tick, gap, and sensor rendering into a single-pass traversal. Eliminated redundant `toPx()` and `remember` overhead within draw blocks.
    - **Validation**: Verified stable UI performance during 240-point history backfills.

*   **[Issue #602] [Severity: Low] [Category: Sensors] SIT Timestamp Parity Logic**.
    - **Resolution**: Restored full forensic parity (R118) for SIT detection events. Propagated `sitVzTs` and `sitVzRt` through `TrackerService`, `HistoryManager`, and Protobuf telemetry. Added drift visualization in Analytical Ribbons to highlight sensor-to-telemetry latency in Strict Mode.
    - **Validation**: Verified timestamp continuity in binary payloads. `:app:assembleDebug` success.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).*
