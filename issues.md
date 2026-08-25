# Project Issues & Hardening Tracking (Aug.25.04)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟡 HARDENING | 49 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 714 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Issue #313: Device Detection Failure**: The deployment tool fails to recognize the SM-A155F (A15) despite it being connected, preventing multi-device connection testing in the current environment.
*   **Samsung Setup Blockers (A15 & S21 FE)**: Deployment on SM-A155F and SM-G990E identifies "Unrestricted" battery mode and "Appear on Top" permissions as hard blockers for system readiness.

---

## 🔴 Open Issues
*   **Issue #313**: **Multi-Device Deployment Failure**. (A15 Detection)

---

## 🟢 Recently Resolved Issues (Aug.25.04)
*   **Issue #312**: **Persistent Lock Verification Failures**. Implemented Snap-Isolation via deep-parity flow throttling in `MainViewModel`.
*   **Issue #311**: **Mode Transition Navigation Regression**. Migrated navigation selection state to `MainUiState`.
*   **Issue #309**: **Compose Lock Verification Persistent Warnings**. Refactored map pools to standard collections.
*   **Issue #310**: **libmbrainSDK Ghost Load Persistence**. Neutralized legacy signatures to prevent Samsung CFMS triggers.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.25.04)
