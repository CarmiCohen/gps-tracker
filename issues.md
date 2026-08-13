# Project Issues & Hardening Tracking (Aug.13.02)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 AT RISK | 5 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 590 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #150] R405 Detection Bypass**: Automated Phone Setup prompt (R405) failed to trigger on verified Samsung A15 (SM-A155F) hardware despite missing battery exemptions.
*   **[Issue #152] Excessive GC Pressure**: Logcat indicates high allocation rates in the hot path. *Note: Partially mitigated by R146 optimizations.*
*   **[Issue #153] Startup Davey Stalls**: Significant main-thread stalls (up to 1600ms) detected during application startup and initial composition.
*   **[Issue #155] Phone Setup UI Clutter**: Action buttons in `PhoneSetupOverlay` remain visible after step completion, causing visual noise and user confusion.
*   **[Issue #156] WakeLock Log Saturation**: Frequent `acquireWakeLock(force=true)` calls from `AppSensorManager` saturate logcat, masking other forensic events.

---

## 🔴 Open Issues
*   *(None)*

---

## 🟢 Recently Resolved Issues (Aug.13.02)
*   **[Issue #154] [Severity: Medium] [Category: Build] Type Inference Failures.**
    *   **Resolution**: Hardened the **Latency Monitoring Framework (R154)**. Explicitly typed all `LatencyMonitor.measureAndAudit` calls and refactored `LogRepository` to eliminate generic inference stalls on budget hardware toolchains. Fixed a forensic deduplication bug in `LogRepository` (Issue #705) where `String` signatures were compared against `ForensicSignature` objects.

---

## 🟢 Recently Resolved Issues (Aug.13.00)
*   **[Issue #146] [Severity: High] [Category: Performance] Forensic Drainer Latency.**
    *   **Resolution**: Hardened the **Forensic Drainer (R146)**. Refactored `ForensicSpillBuffer.peek()` and `writeTrace()` to use pre-allocated buffers and single-pass I/O, eliminating per-entry allocations. Optimized `LogRepository.performForensicDrain()` to use single-pass filtering and mapping, significantly reducing latency spikes and GC pressure.

---

## 🟢 Recently Resolved Issues (Aug.11.21)
*   **[Issue #148] [Severity: Low] [Category: UI] Header Layout Inversion.**
    *   **Resolution**: Hardened the **HeaderBar UI (R148)**. Explicitly forced `LayoutDirection.Ltr` within the `HeaderBar` composable using `CompositionLocalProvider`.
*   **[Issue #151] [Severity: High] [Category: Performance] Phone Setup ANR.**
    *   **Resolution**: Hardened the **Forensic Persistence Path (R151)**. Decoupled forensic writes from the UI thread by offloading them to background dispatchers in `LogRepository`.
*   **[Issue #147] [Severity: Low] [Category: Documentation] Version Inconsistency.**
    *   **Resolution**: Synchronized all tracking files to version `Aug.11.21`.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.13.02)
