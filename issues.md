# Project Issues & Hardening Tracking (July.23.11)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 372 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Samsung A15 Sensor Latency**: Budget hardware may exhibit registration delays under high thermal load. Issue #098 mitigation ensures we don't crash or spam logs, but registration timing remains hardware-dependent.
*   **Permission Revocation Flow**: Revoking `ACTIVITY_RECOGNITION` at runtime results in sensor silence until the next recovery cycle.

---

## 🔴 Open Issues
*   (None currently identified)

---

## 🟢 Recently Resolved Issues (July.23.11)
*   **Tracker Stealth Violation (Audio Alarm)**.
    *   **Resolution**: Hardened `AppAlarmManager.kt` to suppress `shouldPlaySiren` in tracker mode. Trackers now remain silent even during violations, adhering to stealth requirements.
*   **FGS Startup Crash Loop (v23.09)**.
    *   **Resolution**: Moved `startServiceForeground()` to Main-thread `onCreate` in `BaseMonitorService.kt`. This prevents `ForegroundServiceDidNotStartInTimeException` during automatic restoration from the landing page.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).*
