# Project Issues & Hardening Tracking (Aug.26.12)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 STABLE | 47 |
| **Validation Tasks** | 🟢 PASSED | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 736 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Concern #737**: **Identity Sanitization Re-init**. Logcat shows `IDS count updated to 1` on cold start, which might indicate that the identity training/sanitization state is not persisting as expected across sessions.

---

## 🔴 Open Issues
*   *(No high-priority open issues remaining for this subversion)*

---

## 🟢 Recently Resolved Issues (Aug.26.12)
*   **Issue #736 Hardening**: **Compilation Error Remediation**. Fixed a non-exhaustive `when` expression in `CommandRouter.kt` caused by a redundant `ClearTrails` declaration in `Models.kt`. Removed the misplaced inheritance from `UiCommand` in the `UiEvent` class (R736).
*   **Issue #735 Hardening**: **Setup Overlay Bypass**. Implemented a developer-mode bypass for the `PhoneSetupOverlay` to unblock automated soak tests (R735).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.26.12)
