# Project Issues & Hardening Tracking (Aug.25.01)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟡 HARDENING | 50 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 716 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Issue #316: Shadow-Cache LRU Trace (#721)**: Logcat confirms existence of `Issue #721` hardening logic which was missing from documentation. Formalized as R280.
*   **Samsung Setup Blockers (A15 & S21 FE)**: Deployment on SM-A155F identifies "Unrestricted" battery mode and "Appear on Top" permissions as hard blockers.

---

## 🔴 Open Issues
*   **Issue #313**: **Multi-Device Deployment Failure**. (A15 Detection)
*   **Issue #316**: **Shadow-Cache LRU Documentation Gap (#721)**.

---

## 🟢 Recently Resolved Issues (Aug.25.01)
*   **Issue #315**: **Immediate Signal Loss False Positive**. Implemented GPS_WARMUP_GRACE_MS (30s) in `MainAlarmLogic` to suppress false alerts during provider stabilization (R315).
*   **Issue #314**: **Startup UI Stall (Davey)**. Implemented Staggered Hydration (R314) with A15-specific observation delays in `MainViewModel`.
*   **Issue #312**: **Persistent Lock Verification Failures**. Implemented Snap-Isolation via deep-parity flow throttling in `MainViewModel`.
*   **Issue #311**: **Mode Transition Navigation Regression**. Migrated navigation selection state to `MainUiState`.
*   **Issue #309**: **Compose Lock Verification Persistent Warnings**. Refactored map pools to standard collections.
*   **Issue #310**: **libmbrainSDK Ghost Load Persistence**. Neutralized legacy signatures to prevent Samsung CFMS triggers.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.25.01)
