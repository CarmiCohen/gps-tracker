# Project Issues & Hardening Tracking (Aug.05.119)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 1 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 543 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #735] [Severity: Low] [Category: Performance] UI Thread Jitter during Startup.**
    *   *Concern*: `MainActivity` skipped 130 frames during cold start. Heavy initialization in `onCreate` or Compose composition needs optimization.

---

## 🔴 Open Issues
*   **[Issue #735] UI Thread Jitter during Startup.**

---

## 🟢 Recently Resolved Issues (Aug.05.119)
*   **[Issue #734] [Severity: Medium] [Category: Stability] Resource Leak: Unclosed Closeable**.
    *   **Resolution**: Identified and fixed unclosed `RandomAccessFile` in `ForensicSpillBuffer.kt`. Verified all hardware callbacks and receivers in `SystemStatusProvider` and `GpsManager` use `awaitClose` correctly. (R734).

---

## 🟢 Recently Resolved Issues (Aug.05.118)
*   **[Issue #732] [Severity: Critical] [Category: Compatibility] Android 15 (16KB Page Size) Remediation**.
    *   **Resolution**: Aligned native libraries for 16KB page size. Bumped `androidx.datastore` to `1.2.1` and explicitly added `androidx.graphics:graphics-path:1.1.0`. (R732).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.05.119)
