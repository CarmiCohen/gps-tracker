# Project Issues & Hardening Tracking (Aug.26.13)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 STABLE | 47 |
| **Validation Tasks** | 🟢 PASSED | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 737 |

---

## ⚠️ Newly Identified Risks & Concerns
*   *(No new concerns identified in this subversion)*

---

## 🔴 Open Issues
*   *(No high-priority open issues remaining for this subversion)*

---

## 🟢 Recently Resolved Issues (Aug.26.13)
*   **Concern #737 Resolved**: **Identity Sanitization Persistence**. Hardened the identity sanitization lifecycle by persisting the warning dismissal state. This eliminates "re-init" noise where the sanitization overlay would reappear on every cold start even after being dismissed (R976).
*   **Issue #736 Hardening**: **Compilation Error Remediation**. Fixed a non-exhaustive `when` expression in `CommandRouter.kt` (Aug.26.12).
*   **Issue #735 Hardening**: **Setup Overlay Bypass**. Implemented a developer-mode bypass for the `PhoneSetupOverlay` (Aug.26.11).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.26.13)
