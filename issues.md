# Project Issues & Hardening Tracking (Aug.11.21)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 AT RISK | 3 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 587 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #146] Drain Convergence**: The forensic drainer in `LogRepository` is showing significant latency spikes (up to 198ms) during "High Pressure" buffer states. Confirmed via log monitoring. Hot-path requires optimization.
*   **[Issue #148] Header Layout Inversion**: The `HeaderBar` composable's visual output is reversed compared to the `Row` declaration order in `SharedUiComponents.kt`. This suggests an unintended RTL override or arrangement logic error.
*   **[Issue #150] R405 Detection Bypass**: Automated Phone Setup prompt (R405) failed to trigger on verified Samsung A15 (SM-A155F) hardware despite missing battery exemptions.

---

## 🔴 Open Issues
*   *(None)*

---

## 🟢 Recently Resolved Issues (Aug.11.21)
*   **[Issue #151] [Severity: High] [Category: Performance] Phone Setup ANR.**
    *   **Resolution**: Hardened the **Forensic Persistence Path (R151)**. Decoupled forensic writes from the UI thread by offloading them to background dispatchers in `LogRepository`. Refactored `ForensicSpillBuffer` to use granular locking and `MappedByteBuffer` duplication, allowing I/O operations in `peek()` to occur outside the main synchronization lock. This eliminates main-thread stalls during storage pressure on budget hardware (Samsung A15).
*   **[Issue #147] [Severity: Low] [Category: Documentation] Version Inconsistency.**
    *   **Resolution**: Synchronized `app/build.gradle` and all status tracking files to version `Aug.11.21`.

---

## 🟢 Recently Resolved Issues (Aug.11.20)
*   **[Issue #145] [Severity: High] [Category: Logic] Forensic Spill-Buffer Overflow Protection.**
    *   **Resolution**: Hardened the **Forensic Sampling Authority (R669/R700)**. Implemented proactive pressure-aware throttling in the `TrackerService`. (R669)

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.11.21)
