# Project Issues & Hardening Tracking (Aug.07.03)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Action Required | 1 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 553 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #745] [Severity: High] [Category: Functional] Missing Critical Background Permissions.** Setup page confirms Battery Optimization (Unrestricted) and Overlay permissions are missing on SM-A155F, impacting background durability.
*   **[Issue #746] [Severity: Low] [Category: Infrastructure] Missing libmbrainSDK.** Logcat reports `Can't load libmbrainSDK` and `initMbrain failed`. While non-fatal, it adds noise to logs.

---

## 🔴 Open Issues
*   **[Issue #743] [Severity: Low] [Category: Performance] Forensic Spill-Buffer Write Compression.** (Next Objective)

---

## 🟢 Recently Resolved Issues (Aug.07.03)
*   **[Issue #744] [Severity: High] [Category: Performance] Main Thread Startup Stall.**
    *   **Resolution**: Mitigated 2.7s "Davey" stall on budget hardware (SM-A155F) by refactoring `MainViewModel` initialization. Offloaded `settingsUseCase.loadAllSettings()` to `Dispatchers.Default` and implemented staggered observation starts. Added a 300ms settling delay to ensure first-frame rendering completion before declaring UI initialized. This ensures main-thread responsiveness remains under 100ms during cold start (R744).

---

## 🟢 Recently Resolved Issues (Aug.07.01)
*   **[Issue #742] [Severity: Medium] [Category: Infrastructure] Proximity Index Sensitivity Refinement.** (R742)

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.07.03)
