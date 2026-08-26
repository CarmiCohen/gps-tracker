# Project Issues & Hardening Tracking (Aug.25.05)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟡 HARDENING | 47 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 719 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Samsung Setup Blockers (A15 & S21 FE)**: Deployment on SM-A155F identifies "Unrestricted" battery mode and "Appear on Top" permissions as hard blockers.

---

## 🔴 Open Issues
*   *(No high-priority blockers currently open)*

---

## 🟢 Recently Resolved Issues (Aug.25.05)
*   **Issue #317**: **Hardware SOT Architectural Decoupling**. Migrated hardware detection signatures from `:app:Utils.kt` to `:core:engine:HardwareSot.kt`. This enables the core engine and background services to perform standalone environment identification without depending on application-layer utilities (R313/R212).
*   **Issue #313**: **Multi-Device Deployment Failure**. Resolved by unifying and hardening hardware detection signatures (R313). Centralized Samsung and budget-device (A15) detection ensures consistency across all layers.
*   **Issue #316**: **Shadow-Cache LRU Documentation Gap (#721)**. Formalized R280 logic and verified LRU eviction strategy via unit tests in `ShadowCacheTest.kt`. (vAug.25.03)
*   **Issue #315**: **Immediate Signal Loss False Positive**. Implemented GPS_WARMUP_GRACE_MS (30s) in `MainAlarmLogic` to suppress false alerts during provider stabilization (R315). (vAug.25.01)
*   **Issue #314**: **Startup UI Stall (Davey)**. Implemented Staggered Hydration (R314) with A15-specific observation delays in `MainViewModel`. (vAug.25.00)
*   **Issue #312**: **Persistent Lock Verification Failures**. Implemented Snap-Isolation via deep-parity flow throttling in `MainViewModel`. (vAug.25.04-audit)
*   **Issue #311**: **Mode Transition Navigation Regression**. Migrated navigation selection state to `MainUiState`. (vAug.25.01)
*   **Issue #309**: **Compose Lock Verification Persistent Warnings**. Refactored map pools to standard collections. (vAug.25.00)
*   **Issue #310**: **libmbrainSDK Ghost Load Persistence**. Neutralized legacy signatures to prevent Samsung CFMS triggers. (vAug.25.00)

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.25.05)
