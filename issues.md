# Project Issues & Hardening Tracking (Aug.13.05)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 AT RISK | 3 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 592 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #152] Excessive GC Pressure**: Logcat indicates high allocation rates in the hot path. *Note: Partially mitigated by R146 optimizations.*
*   **[Issue #155] Phone Setup UI Clutter**: Action buttons in `PhoneSetupOverlay` remain visible after step completion, causing visual noise and user confusion.
*   **[Issue #156] WakeLock Log Saturation**: Frequent `acquireWakeLock(force=true)` calls from `AppSensorManager` saturate logcat, masking other forensic events.

---

## 🔴 Open Issues
*   *(None)*

---

## 🟢 Recently Resolved Issues (Aug.13.05)
*   **[Issue #153] [Severity: High] [Category: Performance] Startup Davey Stalls.**
    *   **Resolution**: Implemented **Staggered UI Hydration (R153)**. Introduced a `hydrationLevel` state to `MainUiState` to allow the application to boot in stages. Refactored `MainViewModel` to progressively increment hydration with intentional delays, spreading the initial composition load across multiple frames. Deferring the `NavHost` and complex screen rendering in `MainAppContent` until the theme and basic scaffold are stable has eliminated the 1600ms Davey stalls on budget hardware (Samsung A15).

---

## 🟢 Recently Resolved Issues (Aug.13.04)
*   **[Issue #150] [Severity: High] [Category: Compatibility] Samsung A15 Phone Setup Bypass.**
    *   **Resolution**: Hardened the **Samsung A15 Detection Logic (R405)**. Broadened `isA15Device` in `SystemStatusProvider` to inspect `Build.DEVICE` and `Build.PRODUCT` strings. Relocated the automated setup trigger from `MainActivity` to the `MainViewModel` monitoring loop to eliminate race conditions between initialization and permission state acquisition.

---

## 🟢 Recently Resolved Issues (Aug.13.02)
*   **[Issue #154] [Severity: Medium] [Category: Build] Type Inference Failures.**
    *   **Resolution**: Hardened the **Latency Monitoring Framework (R154)**. Explicitly typed all `LatencyMonitor.measureAndAudit` calls and refactored `LogRepository` to eliminate generic inference stalls on budget hardware toolchains. Fixed a forensic deduplication bug in `LogRepository` (Issue #705) where `String` signatures were compared against `ForensicSignature` objects.

---

## 🟢 Recently Resolved Issues (Aug.13.00)
*   **[Issue #146] [Severity: High] [Category: Performance] Forensic Drainer Latency.**
    *   **Resolution**: Hardened the **Forensic Drainer (R146)**. Refactored `ForensicSpillBuffer.peek()` and `writeTrace()` to use pre-allocated buffers and single-pass I/O, eliminating per-entry allocations. Optimized `LogRepository.performForensicDrain()` to use single-pass filtering and mapping, significantly reducing latency spikes and GC pressure.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.13.05)
