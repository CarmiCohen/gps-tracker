# Project Issues & Hardening Tracking (July.30.31)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 470 |

---

## ⚠️ Newly Identified Risks & Concerns
*   None.

---

## 🔴 Open Issues
*   None.

---

## 🟢 Recently Resolved Issues (July.30.31)
*   **[Issue #632] [Severity: Med] [Category: UI/Forensic] Analytical Ribbons: Recovery Markers**.
    - **Resolution**: Integrated service recovery blackout markers into the high-frequency Analytical Ribbons. Added `isRecoveryEvent` flag to `EngineConnectionPoint` and `HistoryEntity`. Enhanced `TrackerService` to detect heuristic recovery pulses and tag the telemetry stream. Implemented visual markers (white vertical lines) in `ConnectionQualityRibbon` to identify forensic service restoration points.
    - **Impact**: Provides users with visual confirmation of when the system recovered from a service gap, improving the auditability of the black-box tracking mechanism.
    - **Database**: Incremented schema version to 63 with migration for `connection_history`.

---

## 🟢 Recently Resolved Issues (July.30.29)
*   **[Issue #631] [Severity: Med] [Category: UI/Forensic] Forensic UI: Service Blackout Trends**.
    - **Resolution**: Implemented the "Forensic Recovery Audit" section in the Diagnostics UI. Users can now monitor "Total Recovery Events" and the "Average Blackout Duration" reactively. 
    - **Fix (July.30.30)**: Corrected a defect in `MainViewModel` where cumulative recovery stats were not bound to the `DiagnosticState` pipeline, preventing UI updates.
    - **Impact**: Provides users and developers with direct visibility into the effectiveness of the deferred recovery mechanism on Samsung A15/Android 14 devices.
    - **Validation**: Verified build success and UI reactive binding in `DiagnosticsScreen`.

---

## 🟢 Recently Resolved Issues (July.30.28)
*   **[Issue #630] [Severity: Med] [Category: Analytics] Forensic Recovery Log Aggregation**.
    - **Resolution**: Implemented cumulative tracking of service recovery latency. Added `cumulative_recovery_blackout_ms` and `recovery_count` to Protobuf settings. Updated `MainViewModel.TriggerRecovery` to atomically increment these stats and log the running average "Service Blackout Duration" upon foreground restoration.
    - **Impact**: Provides fleet-wide insights into recovery performance on restricted hardware (A15).
    - **Validation**: Verified build success and requirement alignment (**R630**).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).*
