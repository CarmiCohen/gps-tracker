# Project Issues & Hardening Tracking (Aug.05.118)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 2 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 542 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #734] [Severity: Medium] [Category: Stability] Resource Leak: Unclosed Closeable.**
    *   *Concern*: Logcat reports `A resource failed to call close.`. Likely a leaked `NetworkCallback` or `BroadcastReceiver` in `SystemStatusProvider`.
*   **[Issue #735] [Severity: Low] [Category: Performance] UI Thread Jitter during Startup.**
    *   *Concern*: `MainActivity` skipped 130 frames during cold start. Heavy initialization in `onCreate` or Compose composition needs optimization.

---

## 🔴 Open Issues
*   **[Issue #734] Resource Leak: Unclosed Closeable.**
*   **[Issue #735] UI Thread Jitter during Startup.**

---

## 🟢 Recently Resolved Issues (Aug.05.118)
*   **[Issue #732] [Severity: Critical] [Category: Compatibility] Android 15 (16KB Page Size) Remediation**.
    *   **Resolution**: Aligned native libraries for 16KB page size. Bumped `androidx.datastore` to `1.2.1` and explicitly added `androidx.graphics:graphics-path:1.1.0` to resolve transitive dependency alignment issues. Verified `useLegacyPackaging=false` and `extractNativeLibs="false"`. (R732).

---

## 🟢 Recently Resolved Issues (Aug.04.117)
*   **[Issue #733] [Severity: High] [Category: JNI] Native Library Initialization Failure (Naming Inconsistency)**.
    *   **Resolution**: Corrected misleading log references in `TrackerService.kt` that still referred to legacy `libmbrainSDK`. Verified JNI loading path for `jdMbrain` to ensure consistency with R721. (R733).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.05.118)
