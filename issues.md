# Project Issues & Hardening Tracking (July.30.40)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 481 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #642] [Severity: Low] [Category: UI] Map Settings Icon Contrast**. The purple settings icon may have low contrast when certain map tile sets (dark/satellite) are used in future updates. Review contrast ratios for accessibility.

---

## 🔴 Open Issues
*   (None)

---

## 🟢 Recently Resolved Issues (July.30.40)
*   **[Issue #643] [Severity: High] [Category: Stability] Foreground Service Start Crash (Regression)**.
    *   **Resolution**: Implemented lifecycle-aware guards in `MainActivity.kt`. Service start is now deferred via `isRecoveryPending` if the activity is not in the `RESUMED` state. The catch block was expanded to `Throwable` to handle unexpected OS-level state exceptions.
    *   **Impact**: Eliminated fatal cold-start crashes on Samsung A15.
*   **[Issue #644] [Severity: Low] [Category: Consistency] Version Inconsistency**.
    *   **Resolution**: Aligned `app/build.gradle` `versionName` to `July.30.40`.
*   **[Issue #641] [Severity: Low] [Category: Performance] Map Invalidation Overhead**.
    *   **Resolution**: Implemented state-aware invalidation in `MapOverlayManager` and `MapComponents`.
*   **[Issue #635] [Severity: Med] [Category: UI/UX] Phone Setup: Permission Status Stalling**.
*   **[Issue #636] [Severity: Low] [Category: Technical Debt] Permission Cache Latency**.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vJuly.30.40-J)*
