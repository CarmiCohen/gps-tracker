# Project Issues & Hardening Tracking (v9.3.37)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are in the [Resolution Archive](STATUS/RESOLUTION_ARCHIVE.md), and validation tasks are in [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 284 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Stray Proto File**: A stray file was accidentally created at `app/src/proto/app_settings.proto` during optimization. It should be manually deleted to avoid build confusion, as automated deletion is restricted.

---

## 🔴 Open Issues
*No open technical issues.*

---

## 🟢 Recently Resolved Issues (v9.3.37 / July17.02)
*   **Issue #092**: ANR & Main Thread Starvation.
    *   Eliminated `runBlocking` in `SystemStatusProviderImpl`.
    *   Converted all permission and battery status checks to `suspend` functions offloaded to `Dispatchers.IO`.
    *   Resolved UI freezes on Samsung A15/S21FE during diagnostic polling.

---

## 🟢 Recently Resolved Issues (v9.3.36)
*   **Issue #092**: Landing Page Responsiveness & Redundant Service Startup.
    *   Eliminated mandatory 2s delay for manual role selection in `MainAppContent.kt`.
    *   Optimized `LaunchedEffect` to prevent redundant service calls during manual selection.
    *   Formalized requirement **R925** in `SOT_MASTER_REQUIREMENTS.md`.
