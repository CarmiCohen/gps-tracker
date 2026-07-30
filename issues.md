# Project Issues & Hardening Tracking (July.30.31)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 2 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 471 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #635] [Severity: Med] [Category: UI/UX] Phone Setup: Permission Status Stalling**. "Exact Alarms" and "Battery Mode" detection is unreliable or doesn't update reactively on Samsung A15 even after manual refresh.
*   **[Issue #636] [Severity: Low] [Category: Technical Debt] Permission Cache Latency**. 15s TTL in `SystemStatusProvider` causes "Refresh" button in Setup to appear unresponsive if clicked immediately after returning from system settings.

---

## 🔴 Open Issues
*   **[Issue #635] Phone Setup: Permission Status Stalling**.
*   **[Issue #636] Permission Cache Latency**.

---

## 🟢 Recently Resolved Issues (July.30.31)
*   **[Issue #634] [Severity: High] [Category: Stability] ForegroundServiceStartNotAllowedException Crash**.
    - **Resolution**: Implemented Foreground Service Start Hardening in `MainActivity`. Wrapped `startForegroundService` in a try-catch block for `ForegroundServiceStartNotAllowedException`. Introduced `SetRecoveryPending` to `UiEvent` to allow the ViewModel to persist the restricted state. The app now defers service restoration until `onResume` when a valid foreground window exists.
    - **Impact**: Eliminates startup crashes on Samsung A15 devices during automatic session restoration when background start restrictions are enforced by the OS.
    - **Validation**: Verified build success and event flow alignment.

*   **[Issue #632] [Severity: Med] [Category: UI/Forensic] Analytical Ribbons: Recovery Markers**.
    - **Resolution**: Integrated service recovery blackout markers into the high-frequency Analytical Ribbons. Added `isRecoveryEvent` flag to `EngineConnectionPoint` and `HistoryEntity`. Enhanced `TrackerService` to detect heuristic recovery pulses and tag the telemetry stream. Implemented visual markers (white vertical lines) in `ConnectionQualityRibbon` to identify forensic service restoration points.
    - **Impact**: Provides users with visual confirmation of when the system recovered from a service gap, improving the auditability of the black-box tracking mechanism.
    - **Database**: Incremented schema version to 63 with migration for `connection_history`.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vJuly.30.31-G)*
