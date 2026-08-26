# Project Issues & Hardening Tracking (Aug.25.06)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟡 HARDENING | 49 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 719 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **A15 Budget Performance (Issue #318)**: Startup hydration triggers 70+ frame skips on SM-A155F. Needs deeper staggering or thread-priority adjustments.
*   **Monitor Inflation Failures (Issue #319)**: `Monitor::Inflate: Install failed` errors detected in logcat during background service start. Potential reliability risk.
*   **Samsung Setup Blockers (A15 & S21 FE)**: Deployment on SM-A155F identifies "Unrestricted" battery mode and "Appear on Top" permissions as hard blockers. Verified functional in vAug.25.05.

---

## 🔴 Open Issues
*   **Issue #318**: **A15 Startup Frame Drops**. Optimize hydration sequence for low-tier CPU scaling.
*   **Issue #319**: **Background Monitor Inflation Failure**. Root-cause the `Monitor::Inflate` install error in `:app`.

---

## 🟢 Recently Resolved Issues (Aug.25.05)
*   **Issue #317**: **Hardware SOT Architectural Decoupling**. Migrated hardware detection signatures from `:app:Utils.kt` to `:core:engine:HardwareSot.kt`. Verified on SM-A155F.
*   **Issue #313**: **Multi-Device Deployment Failure**. Resolved by unifying and hardening hardware detection signatures.
*   **Issue #316**: **Shadow-Cache LRU Documentation Gap (#721)**.
*   **Issue #315**: **Immediate Signal Loss False Positive**.
*   **Issue #314**: **Startup UI Stall (Davey)**.
*   **Issue #312**: **Persistent Lock Verification Failures**.
*   **Issue #311**: **Mode Transition Navigation Regression**.
*   **Issue #309**: **Compose Lock Verification Persistent Warnings**.
*   **Issue #310**: **libmbrainSDK Ghost Load Persistence**.

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.25.06)
