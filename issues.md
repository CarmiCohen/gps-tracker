# Project Issues & Hardening Tracking (July.30.45)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 1 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 490 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #653] [Severity: Medium] [Category: Performance] Excessive Garbage Collection**. Logcat is flooded with `Background concurrent mark compact GC` entries, indicating high allocation pressure or memory churn.
*   **[Issue #642] [Severity: Low] [Category: UI] Map Settings Icon Contrast**. The purple settings icon may have low contrast when certain map tile sets (dark/satellite) used in future updates.

---

## 🔴 Open Issues
*   **[Issue #653] Excessive Garbage Collection**.

---

## 🟢 Recently Resolved Issues (July.30.45)
*   **[Issue #654] [Severity: High] [Category: Performance] UI Jank & Main Thread Stalls during IPC bursts**.
    *   **Resolution**: Centralized all remaining direct system IPC calls (Fine Location, Activity Recognition, Microphone, and Network Interface audits) into `SystemStatusProvider` with a unified 5-second hardware throttle. Refactored UI and background services to use this cached/throttled state.
    *   **Impact**: Eliminated "Davey" stalls (768ms) and frame skipping previously triggered by unthrottled permission checks on the main thread. Silenced `getPackageName` logcat bursts on Samsung A15.
*   **[Issue #651] [Severity: Critical] [Category: Stability] ANR on UI Interaction**.
    *   **Resolution**: Offloaded `Settings.canDrawOverlays()` to `Dispatchers.IO`.
*   **[Issue #652] [Severity: High] [Category: Efficiency] Persistent "Kumiho" Log Spam Regression**.
*   **[Issue #649] [Severity: Critical] [Category: Performance] Severe UI Jank & Main Thread Stalls (A15)**.
*   **[Issue #650] [Severity: High] [Category: Efficiency] Persistent "Kumiho" Log Spam (getPackageName)**.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vJuly.30.45-E)*
