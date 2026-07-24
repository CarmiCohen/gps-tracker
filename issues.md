# Project Issues & Hardening Tracking (July.24.04)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 3 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 381 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Inefficient Telemetry Path**: High memory churn due to Binary -> JSON -> DataClass conversion for every incoming packet. This triggers GC every ~100ms during active tracking.
*   **Maintenance Worker Redundancy**: The recovery logic in `MaintenanceWorker` may conflict with `BootReceiver`'s expedited work on some devices.

---

## 🔴 Open Issues
*   **Issue #538: High-Frequency Memory Allocations**.
    *   *Symptom*: Continuous Background GC logs.
    *   *Status*: Optimized aggregator; needs serialization optimization (see #541).
*   **Issue #539: Background Start Reliability (API 34+)**.
    *   *Resolution*: Migrated to Expedited Work and enforced `setForeground` in both `BootReceiver` and `MaintenanceWorker`.
*   **Issue #541: Inefficient Telemetry Serialization**.
    *   *Symptom*: High object churn in `CommunicationManager` and `ConnectivitySuite`.
    *   *Impact*: Performance degradation and GC pressure.

---

## 🟢 Recently Resolved Issues (July.24.04)
*   **Tracker Stealth Violation (No Loud Alarms)**.
    *   **Resolution**: Enforced stealth in `AudioSynthesizer` and `CommandRouter`. Even direct test commands are suppressed in Tracker Mode.
*   **Issue #540: Signaling Rejoin Loop / IPC Congestion**.
    *   **Resolution**: Implemented `lastForceJoinTs` cooldown in `ConnectivitySuite` and increased traffic staleness tolerance.
*   **Issue #537: Main Thread Initialization Bottleneck**. 
    *   **Resolution**: Refactored `MainViewModel` to prioritize UI initialization and fully decouple repository pruning and heavy observations.
*   **Telemetry Aggregator Churn (Issue #538a)**.
    *   **Resolution**: Minimized `copy()` calls in `TelemetryAggregator.processPoint`.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).*