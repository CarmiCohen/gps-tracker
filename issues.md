# Project Issues & Hardening Tracking (Aug.04.110)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 1 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 532 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #721] [Severity: High] [Category: Performance] Logcat Spam: getPackageName() recursion/spam on Samsung A15.**
    *   **Concern**: Repetitive `getPackageName: com.gps19.app` logs are flooding the buffer, causing significant UI jank (1s+ stalls) and high CPU overhead during Phone Setup and Dashboard rendering.

---

## 🔴 Open Issues
*   **[Issue #721] Performance: getPackageName() logcat spam.**
    *   **Context**: Native library collision resolved; investigation into remaining system-triggered spam (specifically from permission APIs) is active.

---

## 🟢 Recently Resolved Issues (Aug.04.110)
*   **[Issue #723] [Severity: Medium] [Category: Performance] Main-Thread Jitter: Synchronous /proc Reads**.
    *   **Resolution**: Changed `getCpuLoad` and `getIoWait` to suspend functions and wrapped implementations in `withContext(Dispatchers.IO)`. Updated `IntegrityMonitor.performIntegrityHeartbeat` to accommodate non-blocking execution (R723).
*   **[Issue #722] [Severity: High] [Category: Performance] Setup-Phase Polling Overhead**.
    *   **Resolution**: Increased `FORCED_REFRESH_COOLDOWN_MS` to 15s in `SystemStatusProviderImpl`. This throttles expensive system permission checks (`Settings.canDrawOverlays`, `isIgnoringBatteryOptimizations`) while the Setup Overlay is active, significantly reducing main-thread stalls and logcat pressure on budget hardware (R722).

---

## 🟢 Recently Resolved Issues (Aug.04.101)
*   **[Issue #721] [Severity: High] [Category: Performance] Samsung A15 Native Collision**.
    *   **Resolution**: Renamed native library from `mbrainSDK` to `jdMbrain` to resolve naming collision with Samsung's internal system libraries (`libmbrainSDK`). Standardized log tags and updated JNI bridge initialization to use unique namespace (R721).
*   **[Issue #721] [Severity: Low] [Category: Robustness] Log Noise Reduction**.
    *   **Resolution**: Reduced verbosity of forensic convergence logs in `LogRepository` to minimize logcat pressure on budget hardware.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.04.110)
