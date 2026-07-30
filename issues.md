# Project Issues & Hardening Tracking (July.30.41)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 484 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #642] [Severity: Low] [Category: UI] Map Settings Icon Contrast**. The purple settings icon may have low contrast when certain map tile sets (dark/satellite) are used in future updates. Review contrast ratios for accessibility.

---

## 🔴 Open Issues
*   (None)

---

## 🟢 Recently Resolved Issues (July.30.41)
*   **[Issue #646] [Severity: Low] [Category: Efficiency] Persistent Log Spam: Overlay & Permission Checks**.
    *   **Resolution**: Extended `HARDWARE_IPC_THROTTLE_MS` (5000ms) to all permission and overlay checks in `SystemStatusProviderImpl.kt`.
    *   **Impact**: Silenced remaining Samsung Kumiho auditing noise during Setup/Diagnostics screen polling.
*   **[Issue #647] [Severity: Low] [Category: Performance] Excessive Hardware Punch Frequency**.
    *   **Resolution**: Increased `A15_POKE_INTERVAL_MS` to 60s in `TrackerService.kt`.
    *   **Impact**: Reduced JNI overhead and Logcat noise on Samsung A15 hardware.
*   **[Issue #645] [Severity: Low] [Category: Efficiency] Persistent Log Spam: getPackageName()**.
    *   **Resolution**: Implemented a 5000ms hardware IPC throttle in `SystemStatusProviderImpl.kt` specifically for `isIgnoringBatteryOptimizations()`.
*   **[Issue #643] [Severity: High] [Category: Stability] Foreground Service Start Crash (Regression)**.
    *   **Resolution**: Implemented lifecycle-aware guards in `MainActivity.kt`.
*   **[Issue #644] [Severity: Low] [Category: Consistency] Version Inconsistency**.
*   **[Issue #641] [Severity: Low] [Category: Performance] Map Invalidation Overhead**.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vJuly.30.41-D)*
