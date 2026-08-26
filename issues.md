# Project Issues & Hardening Tracking (Aug.26.15)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟡 MONITORING | 49 |
| **Validation Tasks** | 🟢 PASSED | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 739 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Concern #739**: **Hydration Performance Stall (A15)**. Detected a 1482ms main-thread stall ("Davey!") during Level 4 hydration on Samsung A15 hardware.
*   **Concern #738**: **EventQueue Resource Leak**. Logcat warning: `A resource failed to call BaseEventQueue.dispose`, indicating a lifecycle management failure in the core engine.

---

## 🔴 Open Issues
*   *(No high-priority open issues remaining for this subversion)*

---

## 🟢 Recently Resolved Issues (Aug.26.15)
*   **Concern #740 Resolved**: **System Issue Counter Mismatch**. Synchronized `PhoneSetupOverlay` items with `MainUiState.systemIssuesCount`. Added Step 0 (Precise Location) and corrected Step 5 completion flag (R740).
*   **Concern #737 Resolved**: **Identity Sanitization Persistence**. Verified fix on `Aug.26.14`. The dismissal state now correctly persists through cold starts (R976).
*   **Issue #736 Hardening**: **Compilation Error Remediation**. Fixed a non-exhaustive `when` expression in `CommandRouter.kt` (Aug.26.12).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.26.15)
