# Project Issues & Hardening Tracking (Aug.17.07)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Clean | 0 |
| **Validation Tasks** | 🔍 Pending | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 632 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *No new risks identified.*

---

## 🔴 Open Issues
*   *No open critical issues. System stable and stress-tested.*

---

## 🟢 Recently Resolved Issues (Aug.17.07)
*   **[Issue #189] [Severity: High] [Category: Forensic] Forensic Stress Test.**
    *   **Resolution**: Successfully executed 5-minute CPU/IO saturation routine at 100Hz on API 35. Verified system survival, lack of ANRs, and successful recovery transition (R189).
*   **[Issue #190] [Severity: Critical] [Category: Stability] Database Migration Failure (v68-v71).**
    *   **Resolution**: Hardened `AppDatabase` by implementing aggressive deduplication in migrations, removing invalid `UNIQUE` constraints on `localId` in both entity and indices, and restoring the missing `sitVzRt` column in `connection_history` (R190).
*   **[Issue #187] [Severity: Low] [Category: UI] Dashboard Layout Jitter.**
    *   **Resolution**: Stabilized `InfoRow` in `OverlayComponents.kt` with fixed height (18.dp) and disabled font padding (R187).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.17.07)
