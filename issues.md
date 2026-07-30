# Project Issues & Hardening Tracking (July.30.43)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 487 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #642] [Severity: Low] [Category: UI] Map Settings Icon Contrast**. The purple settings icon may have low contrast when certain map tile sets (dark/satellite) are used in future updates. Review contrast ratios for accessibility.

---

## 🔴 Open Issues
*   (None)

---

## 🟢 Recently Resolved Issues (July.30.43)
*   **[Issue #649] [Severity: Critical] [Category: Performance] Severe UI Jank & Main Thread Stalls (A15)**.
    *   **Resolution**: Hardened hardware IPC calls with `Mutex`-guarded `suspend` execution. Prevented main-thread blocking by offloading to `Dispatchers.IO`.
    *   **Impact**: Restored UI fluidity on budget Samsung hardware; eliminated `Davey!` events during high-frequency telemetry.
*   **[Issue #650] [Severity: High] [Category: Efficiency] Persistent "Kumiho" Log Spam (getPackageName)**.
    *   **Resolution**: Implemented atomic cache check-and-update logic in `SystemStatusProviderImpl` to prevent concurrent race conditions from bypassing the 5s IPC throttle.
    *   **Impact**: Silenced redundant `getPackageName` logcat bursts.
*   **[Issue #648] [Severity: High] [Category: Performance] Persistent "Kumiho" Log Spam & UI Jank**.
    *   **Resolution**: Increased `INTERNET_CACHE_TTL_MS` and hardware IPC throttle to 5000ms. Strictly enforced throttling for `isLocalOnline()`.
    *   **Impact**: Initial mitigation of Samsung Kumiho auditing noise. (Hardened further in #649/#650).
*   **[Issue #646] [Severity: Low] [Category: Efficiency] Persistent Log Spam: Overlay & Permission Checks**.
*   **[Issue #647] [Severity: Low] [Category: Performance] Excessive Hardware Punch Frequency**.
*   **[Issue #645] [Severity: Low] [Category: Efficiency] Persistent Log Spam: getPackageName()**.
*   **[Issue #643] [Severity: High] [Category: Stability] Foreground Service Start Crash (Regression)**.
*   **[Issue #644] [Severity: Low] [Category: Consistency] Version Inconsistency**.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vJuly.30.43-D)*
