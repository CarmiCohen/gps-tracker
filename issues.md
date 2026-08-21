# Project Issues & Hardening Tracking (Aug.20.10)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟢 CLEAN | 0 |
| **Validation Tasks** | 🟡 PENDING | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 676 |

---

## ⚠️ Newly Identified Risks & Concerns
*   (No active risks identified).

---

## 🔴 Open Issues
*   (No open issues).

---

## 🟢 Recently Resolved Issues (Aug.20.10)
*   **Issue #238**: **Anchor Evaluator Coordinate Leak**: Hardened stationary anchor center by restricting averaging to points within the "dead zone" (below transition start) to prevent anchor drift absorption (R238).
*   **Issue #241**: **ViewModel Combine Mismatch**: Fixed argument count mismatches in `MainViewModel.kt` combine blocks for `dashboardState` and `hudState` (R241).
*   **Issue #240**: **HUD Component API Drift**: Migrated `TrackerScreen.kt` and `ViewerScreen.kt` to consume centralized `HudState` (R240).
*   **Issue #239**: **Undefined `addPersistentLog`**: Restored missing `addPersistentLog` logic in `MainViewModel.kt` and `MainFileHelper.kt` (R239).
*   **Issue #235**: **Missing Dependency Hardening**: Restored `datastore`, `protobuf`, `socket.io-client`, and `play-services-location` to `app/build.gradle` (R235).
*   **Issue #233**: Diagnostics Screen Visibility layering: Modified `MainAppContent.kt` to dismiss `PhoneSetupOverlay` (R233).
*   **Issue #232**: UI Clipping Hardening (PhoneSetup): Optimized button heights and font sizes (R232).
*   **Issue #227**: Versioning Mismatch: Synchronized `BuildConfig.VERSION_NAME` to `Aug.20.08` (R227).

---
*For older resolutions, see [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md). (vAug.20.10)
