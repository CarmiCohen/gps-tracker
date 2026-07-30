# Project Issues & Hardening Tracking (July.30.36)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 1 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 478 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #641] [Severity: Low] [Category: Performance] Map Invalidation Overhead**. Continuous `view.invalidate()` in `MapComponents.kt` consumes CPU even when no overlays change. Throttling the invalidation based on actual overlay state changes is recommended for R-HARDWARE-01.

---

## 🔴 Open Issues
*   **[Issue #641] Map Invalidation Overhead**.

---

## 🟢 Recently Resolved Issues (July.30.36)
*   **[Issue #635] [Severity: Med] [Category: UI/UX] Phone Setup: Permission Status Stalling**.
    *   **Resolution**: Implemented "Robust Refresh" in `MainViewModel` (double-check with 1200ms delay) and bypassed permission cache during setup polling.
    *   **Impact**: Resolved unreliable detection of "Exact Alarms" and "Battery Mode" on budget hardware (Samsung A15).
*   **[Issue #636] [Severity: Low] [Category: Technical Debt] Permission Cache Latency**.
    *   **Resolution**: Reduced `PERMISSION_TTL_MS` in `SystemStatusProvider` from 15s to 2s.
    *   **Impact**: UI refresh now feels instantaneous when returning from system settings.

*   **[Issue #640] [Severity: High] [Category: Stability] Tracker Mode ANR (Regression)** (July.30.35).
    - **Resolution**: Implemented aggressive throttling and decoupled updates in `MapOverlayManager.kt`.
    - **Impact**: Eliminated system-level unresponsiveness on Samsung A15 post-relay connection.

*   **[Issue #637] [Severity: Low] [Category: Efficiency] Log Spam: getPackageName()**.
*   **[Issue #639] [Severity: High] [Category: Performance] Tracker Mode ANR on Startup**.
*   **[Issue #338] [Severity: High] [Category: UI/Logic] Incorrect Permission Defaults**.
*   **[Issue #634] [Severity: High] [Category: Stability] ForegroundServiceStartNotAllowedException Crash**.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vJuly.30.36-I)*
