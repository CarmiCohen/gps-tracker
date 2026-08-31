# Project Issues & Hardening Tracking (Aug.31.04)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 Progress | 26 |
| **Validation Tasks** | 🟢 Validated | 210 |
| **Resolved (Total)** | 🟢 Progress | 787 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *(None identified in current audit cycle)*

---

## 🔴 Open Issues
*   *(See Dashboard for count)*

---

## 🟢 Recently Resolved Issues (Aug.31.04)
*   **Issue #779 Validated: Forensic Replay & Metadata Hardening**. Extended the `ForensicSanitizer` policy to the telemetry mapping and historical audit layers.
    *   **Telemetry Hardening**: Updated `TrackerStatus.toMap()` in `Models.kt` to scrub technical network identifiers (`net_interface`) before transmission.
    *   **Audit Sanitization**: Hardened `HistoryManager.kt` to sanitize continuity audit and backfilling logs at the source (R779).
    *   **Consistency Audit**: Confirmed that all `MainFileHelper` export paths and `LogEntry` JSON serializations utilize centralized scrubbing.
*   **Issue #762 Validated: Acoustic Duty-Cycle & [ULTRA] Badge Correlation**. Hardened end-to-end propagation of the `isUltraLongStationary` state (Aug.31.03).
*   **Issue #782 Validated: UI Performance Hardening (History Sampling)**. Hardened the forensic ribbon pipeline in `MainViewModel` using the `sample()` operator (Aug.31.03).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.31.04)*
*Simplification Ideas: 216*
