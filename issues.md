# Project Issues & Hardening Tracking (Aug.25.02)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟡 HARDENING | 50 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 713 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Issue #312: Persistent Lock Verification Failures**: `SnapshotStateList.conditionalUpdate` warnings continue on Samsung hardware (confirmed on SM-G990E and SM-A155F). This indicates reactive list overhead in the UI state aggregation logic.
*   **Issue #313: Device Detection Failure**: The deployment tool fails to recognize the SM-A155F (A15) despite it being connected, preventing multi-device connection testing in the current environment.
*   **Samsung Setup Blockers (A15 & S21 FE)**: Deployment on SM-A155F and SM-G990E identifies "Unrestricted" battery mode and "Appear on Top" permissions as hard blockers for system readiness.

---

## 🔴 Open Issues
*   **Issue #312**: **Compose Runtime Lock Contention**. (Investigation Pending)
*   **Issue #313**: **Multi-Device Deployment Failure**. (A15 Detection)

---

## 🟢 Recently Resolved Issues (Aug.25.01)
*   **Issue #311**: **Mode Transition Navigation Regression**. Migrated navigation selection state to `MainUiState` to ensure survival across Activity recreation.
*   **Issue #309**: **Compose Lock Verification Persistent Warnings**. Refactored `MapOverlayManager` pools to standard collections. (Partial resolution, see #312).
*   **Issue #310**: **libmbrainSDK Ghost Load Persistence**. Neutralized legacy signatures to prevent Samsung CFMS triggers.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.25.02)
