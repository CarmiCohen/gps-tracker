# Project Issues & Hardening Tracking (Aug.14.00)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 1 | 1 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 609 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #165] [Severity: Medium] [Category: Maintenance] Forensic Trace Persistence Stress Test.**
    *   **Risk**: Potential for SQLite write contention or Main-thread stalls under sustained 100Hz load.
    *   **Status**: PENDING VALIDATION.

---

## 🔴 Open Issues
*   **[Issue #165] Forensic Trace Persistence Stress Test**: Verify database throughput and spill-buffer drainage stability during a 5-minute sustained 100Hz load.

---

## 🟢 Recently Resolved Issues (Aug.14.00)
*   **[Issue #167] [Severity: Medium] [Category: Performance] Database Pruning Thrash.**
    *   **Resolution**: Increased `DB_PRUNE_THRESHOLD` to 500 and implemented a **1-minute temporal cooldown** in `LogRepository` to prevent I/O thrashing during 100Hz forensic streams. (R167)
*   **[Issue #166] [Severity: High] [Category: UI/UX] Settings Overlay ANR.**
    *   **Resolution**: Resolved Main-thread stalls by implementing **Staggered Hydration** in `SettingsOverlay` and throttling `eventLogsFlow` using `sample(500ms)`, eliminating object churn from the UI path. (R166)

---

## 🟢 Recently Resolved Issues (Aug.13.14)
*   **[Issue #168] [Severity: High] [Category: Maintenance] Build Restoration.**
    *   **Resolution**: Fixed compilation errors in `LogRepository.kt` flush logic. (R168)

---

## 🟢 Recently Resolved Issues (Aug.13.13)
*   **[Issue #164] [Severity: Medium] [Category: Maintenance] Forensic Log Buffer Audit.**
    *   **Resolution**: Hardened the 100Hz logging path via deterministic IDs and raw snapshots. Restored 45+ truncated constants and fixed unresolved references in `LogRepository.kt`. (R164)

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.14.00)
