# Project Issues & Hardening Tracking (July.27.08)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 442 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *(No active forensic aliasing risks identified)*

---

## 🔴 Open Issues
*   *(No active critical engine issues)*

---

## 🟢 Recently Resolved Issues (July.27.08)
*   **[Issue #604] [Severity: Low] [Category: UI] Ribbon Density & Aliasing Audit**.
    - **Resolution**: Refined `TelemetryAggregator.MutableAggregationPoint.merge()` to utilize peak-retention logic (`max()`) for `kineticEnergy` and `sitShock` forensic indices. This ensures that brief, high-intensity events (e.g., tamper or impulse shocks) are preserved and visible in the Analytical Ribbon even at high compression scales (such as the 7-day view). Updated `EngineSensorSnapshot` to include `sitShock` for parity during gap backfills.
    - **Validation**: Verified peak retention in `TelemetryAggregator` logic. `:app:assembleDebug` success.

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
