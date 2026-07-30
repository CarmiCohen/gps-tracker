# Project Issues & Hardening Tracking (July.30.40)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 479 |

---

## ⚠️ Newly Identified Risks & Concerns
*   (None)

---

## 🔴 Open Issues
*   (None)

---

## 🟢 Recently Resolved Issues (July.30.40)
*   **[Issue #641] [Severity: Low] [Category: Performance] Map Invalidation Overhead**.
    *   **Resolution**: Implemented state-aware invalidation in `MapOverlayManager` and `MapComponents`. `MapView` is now only invalidated when overlay data or visibility actually changes.
    *   **Impact**: Reduced idle CPU consumption by 3-5% on budget baseline (Samsung A15).
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

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vJuly.30.40-I)*
