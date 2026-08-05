# Project Issues & Hardening Tracking (Aug.04.117)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 3 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 541 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #732] [Severity: Critical] [Category: Compatibility] Android 15 (16KB Page Size) Incompatibility.** 
    *   *Concern*: Deployment on Android 15 (SM-A155F) triggers a compatibility warning: "This app is not compatible with 16KB page sizes". Impacted libraries: `libjdMbrain.so`, `libdatastore_shared_counter.so`, `libandroidx.graphics.path.so`.
*   **[Issue #734] [Severity: Medium] [Category: Stability] Resource Leak: Unclosed Closeable.**
    *   *Concern*: Logcat reports `A resource failed to call close.`. Likely a leaked `NetworkCallback` or `BroadcastReceiver` in `SystemStatusProvider`.
*   **[Issue #735] [Severity: Low] [Category: Performance] UI Thread Jitter during Startup.**
    *   *Concern*: `MainActivity` skipped 130 frames during cold start. Heavy initialization in `onCreate` or Compose composition needs optimization.

---

## 🔴 Open Issues
*   **[Issue #732] Android 15 (16KB Page Size) Incompatibility.**
*   **[Issue #734] Resource Leak: Unclosed Closeable.**
*   **[Issue #735] UI Thread Jitter during Startup.**

---

## 🟢 Recently Resolved Issues (Aug.04.117)
*   **[Issue #733] [Severity: High] [Category: JNI] Native Library Initialization Failure (Naming Inconsistency)**.
    *   **Resolution**: Corrected misleading log references in `TrackerService.kt` that still referred to legacy `libmbrainSDK`. Verified JNI loading path for `jdMbrain` to ensure consistency with R721. (R733).

---

## 🟢 Recently Resolved Issues (Aug.04.116)
*   **[Issue #731] [Severity: High] [Category: Persistence] Forensic Bloat: Important/Special Logs Exempt from Pruning**.
    *   **Resolution**: Implemented a secondary safety tier in `LogRepository.proactivePruning`. Introduced `LogDao.pruneSpecialLogsChunk` to allow chunked deletion of `isSpecial` (Forensic Trace) logs once the database exceeds `LOG_LIMIT_STRICT` (5000). (R731).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.04.117)
