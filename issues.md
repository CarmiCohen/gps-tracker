# Project Issues & Hardening Tracking (Aug.25.01)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟡 HARDENING | 48 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 713 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Issue #312: Persistent Lock Verification Failures**: `SnapshotStateList.conditionalUpdate` warnings continue on A15 hardware despite #309. This indicates hidden reactive lists in aggregation or component logic are still triggering overhead.
*   **Samsung A15 Setup Blocker**: Initial deployment on SM-A155F identifies "Unrestricted" battery mode and "Appear on Top" permissions as hard blockers for system readiness.

---

## 🔴 Open Issues
*   **Issue #312**: **Compose Runtime Lock Contention**. (Investigation Pending)

---

## 🟢 Recently Resolved Issues (Aug.25.01)
*   **Issue #311**: **Mode Transition Navigation Regression**. Migrated navigation selection state to `MainUiState` to ensure survival across Activity recreation.
*   **Issue #309**: **Compose Lock Verification Persistent Warnings**. Refactored `MapOverlayManager` pools to standard collections. (Partial resolution, see #312).
*   **Issue #310**: **libmbrainSDK Ghost Load Persistence**. Neutralized legacy signatures to prevent Samsung CFMS triggers.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.25.01)
