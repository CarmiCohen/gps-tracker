# Project Issues & Hardening Tracking (Aug.25.04)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟡 HARDENING | 52 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 714 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Issue #314: Startup UI Stall (Davey)**: Initialization sequence triggers a 1.5s UI stall on A15 hardware. Need to further offload `init` blocks and stagger heavy observations (R314).
*   **Issue #315: Immediate Signal Loss False Positive**: Alarm triggers before GPS provider stabilization. A 30s "settling" grace period is required (R315).
*   **Issue #316: Shadow-Cache LRU Trace (#721)**: Logcat confirms existence of `Issue #721` hardening logic which was missing from documentation. Formalized as R280.
*   **Samsung Setup Blockers (A15 & S21 FE)**: Deployment on SM-A155F identifies "Unrestricted" battery mode and "Appear on Top" permissions as hard blockers.

---

## 🔴 Open Issues
*   **Issue #313**: **Multi-Device Deployment Failure**. (A15 Detection)
*   **Issue #314**: **Startup UI Stall (Davey)**.
*   **Issue #315**: **Immediate Signal Loss False Positive**.
*   **Issue #316**: **Shadow-Cache LRU Documentation Gap (#721)**.

---

## 🟢 Recently Resolved Issues (Aug.25.04)
*   **Issue #312**: **Persistent Lock Verification Failures**. Implemented Snap-Isolation via deep-parity flow throttling in `MainViewModel`.
*   **Issue #311**: **Mode Transition Navigation Regression**. Migrated navigation selection state to `MainUiState`.
*   **Issue #309**: **Compose Lock Verification Persistent Warnings**. Refactored map pools to standard collections.
*   **Issue #310**: **libmbrainSDK Ghost Load Persistence**. Neutralized legacy signatures to prevent Samsung CFMS triggers.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.25.04)
