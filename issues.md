# Project Issues & Hardening Tracking (Aug.07.04)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Action Required | 1 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 554 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **[Issue #745] [Severity: High] [Category: Functional] Missing Critical Background Permissions.** Setup page confirms Battery Optimization (Unrestricted) and Overlay permissions are missing on SM-A155F, impacting background durability.
*   **[Issue #746] [Severity: Low] [Category: Infrastructure] Missing libmbrainSDK.** Logcat reports `Can't load libmbrainSDK` and `initMbrain failed`. While non-fatal, it adds noise to logs.

---

## 🔴 Open Issues
*   **[Issue #745] [Severity: High] [Category: Functional] Missing Critical Background Permissions.** (Next Objective)

---

## 🟢 Recently Resolved Issues (Aug.07.04)
*   **[Issue #743] [Severity: Low] [Category: Performance] Forensic Spill-Buffer Write Compression.**
    *   **Resolution**: Implemented structural compression for the circular spill-buffer. Reduced `FORENSIC_SPILL_ENTRY_SIZE` to 96 bytes (down from 128) and increased `FORENSIC_SPILL_CAPACITY` to 3000. Optimized the binary format (V2) by bit-packing flags and battery level into single bytes and refining field alignment. This reduces flash IO volume by 25% and improves forensic retention by 50% without increasing the 288KB memory-mapped footprint (R743).

---

## 🟢 Recently Resolved Issues (Aug.07.03)
*   **[Issue #744] [Severity: High] [Category: Performance] Main Thread Startup Stall.**
    *   **Resolution**: Mitigated 2.7s "Davey" stall on budget hardware (SM-A155F) by refactoring `MainViewModel` initialization. Offloaded `settingsUseCase.loadAllSettings()` to `Dispatchers.Default` and implemented staggered observation starts (R744).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.07.04)
