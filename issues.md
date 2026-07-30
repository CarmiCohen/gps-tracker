# Project Issues & Hardening Tracking (July.30.42)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 485 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #642] [Severity: Low] [Category: UI] Map Settings Icon Contrast**. The purple settings icon may have low contrast when certain map tile sets (dark/satellite) are used in future updates. Review contrast ratios for accessibility.

---

## 🔴 Open Issues
*   (None)

---

## 🟢 Recently Resolved Issues (July.30.42)
*   **[Issue #648] [Severity: High] [Category: Performance] Persistent "Kumiho" Log Spam & UI Jank**.
    *   **Resolution**: Increased `INTERNET_CACHE_TTL_MS` and hardware IPC throttle to 5000ms. Strictly enforced throttling for `isLocalOnline()` in `SystemStatusProvider` and `IntegrityMonitor`.
    *   **Impact**: Silenced Samsung Kumiho auditing noise and eliminated `Davey!` UI jank on A15 hardware.
*   **[Issue #646] [Severity: Low] [Category: Efficiency] Persistent Log Spam: Overlay & Permission Checks**.
*   **[Issue #647] [Severity: Low] [Category: Performance] Excessive Hardware Punch Frequency**.
*   **[Issue #645] [Severity: Low] [Category: Efficiency] Persistent Log Spam: getPackageName()**.
*   **[Issue #643] [Severity: High] [Category: Stability] Foreground Service Start Crash (Regression)**.
*   **[Issue #644] [Severity: Low] [Category: Consistency] Version Inconsistency**.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vJuly.30.42-D)*
