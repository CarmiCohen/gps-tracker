# Project Issues & Hardening Tracking (Aug.07.05)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Action Required | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 555 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #746] [Severity: Low] [Category: Infrastructure] Missing libmbrainSDK.** Logcat reports `Can't load libmbrainSDK` and `initMbrain failed`. While non-fatal, it adds noise to logs.

---

## 🔴 Open Issues
*   *(None)*

---

## 🟢 Recently Resolved Issues (Aug.07.05)
*   **[Issue #745] [Severity: High] [Category: Functional] Missing Critical Background Permissions.**
    *   **Resolution**: Hardened permission detection responsiveness by reducing `FORCED_REFRESH_COOLDOWN_MS` from 15s to 1s in `SystemStatusProviderImpl`. This ensures the Setup UI "Refresh" button provides immediate feedback. Reorganized `SystemStatusProvider.kt` to resolve tool-specific compilation stalls (R745).

---

## 🟢 Recently Resolved Issues (Aug.07.04)
*   **[Issue #743] [Severity: Low] [Category: Performance] Forensic Spill-Buffer Write Compression.**
    *   **Resolution**: Implemented structural compression for the circular spill-buffer (R743).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.07.05)
