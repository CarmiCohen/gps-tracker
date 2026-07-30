# Project Issues & Hardening Tracking (July.30.44)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 1 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 489 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #653] [Severity: Medium] [Category: Performance] Excessive Garbage Collection**. Logcat is flooded with `Background concurrent mark compact GC` entries, indicating high allocation pressure or memory churn.
*   **[Issue #642] [Severity: Low] [Category: UI] Map Settings Icon Contrast**. The purple settings icon may have low contrast when certain map tile sets (dark/satellite) are used in future updates. Review contrast ratios for accessibility.

---

## 🔴 Open Issues
*   **[Issue #653] Excessive Garbage Collection**.

---

## 🟢 Recently Resolved Issues (July.30.44)
*   **[Issue #651] [Severity: Critical] [Category: Stability] ANR on UI Interaction**.
    *   **Resolution**: Offloaded `Settings.canDrawOverlays()` to `Dispatchers.IO`. This call is an IPC that can block the main thread, especially on Samsung devices under system load.
    *   **Impact**: Eliminated ANR dialog when tapping the "System Issues" button.
*   **[Issue #652] [Severity: High] [Category: Efficiency] Persistent "Kumiho" Log Spam Regression**.
    *   **Resolution**: Unified and strictly enforced a 5000ms hardware IPC throttle across all permission checks within `SystemStatusProviderImpl`. Consolidated the refresh logic to ensure concurrent UI requests and background monitoring share the same throttled result.
    *   **Impact**: Silenced `getPackageName` logcat bursts on Samsung A15.
*   **[Issue #649] [Severity: Critical] [Category: Performance] Severe UI Jank & Main Thread Stalls (A15)**.
*   **[Issue #650] [Severity: High] [Category: Efficiency] Persistent "Kumiho" Log Spam (getPackageName)**.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vJuly.30.44-E)*
