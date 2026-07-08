# Project Issues & Hardening Tracking (v9.3.0)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are in the [Issues Archive](STATUS/issues_archive.md), and validation tasks are in [Testing Status](STATUS/testing_status.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟡 Active | 4 |
| **Validation Tasks** | 🔍 Tracked | [Testing Status](STATUS/testing_status.md) |
| **Resolved (Total)** | 🟢 Progress | 256 |

---

## ⚠️ Newly Identified Risks & Concerns
| ID | Concern | Description |
| :--- | :--- | :--- |
| **#055** | **Issue History Recovery** | Restored 185 "lost" legacy resolutions from `compliance_archive.md`. |
| **#054** | **Requirement ID Collision** | Discovered that Issue #326 was overloaded in `compliance.md`. Audited and corrected. |
| **#039** | **Identity Rejection Feedback** | `MainRepository` now silently rejects bulk updates with colliding IDs. UI feedback needed. |
| **#042** | **Sanitization Visibility** | `SettingsRepository` automatically resets malformed IDs. No UI notification exists. |

---

## 🔴 Open Issues
| ID | Category | Description |
| :--- | :--- | :--- |
| **#058** | **Refactor** | **TrackerService Initialization**: Refactor `onCreate` to move manual dependency injection (IntegrityMonitor, SyncManager, etc.) into Hilt Modules to improve testability and reduce initialization bugs. |
| **#059** | **Feature** | **Permission Health Check UI**: Implement a "Diagnostics" screen in Compose using `isXiaomiSpecialPermissionGranted` to show health indicators and "Fix" buttons for Xiaomi devices. |
| **#061** | **Cleanup** | **Forensic Logging Consolidation**: Create a `ForensicLogUseCase` to standardize "Special Color" (Pink) logging and ensure consistent inclusion of lat/lng/accuracy/vibe data. |
| **#062** | **Hardening** | **Dynamic Anchor Breakout**: Implement a displacement-weighted monitor for the breakout phase. If GPS displacement consistently increases while IMU `stationaryProb` remains high (sticky anchor), trigger a "soft breakout" to prevent locking the user to a stationary position on noisy hardware like the Samsung A15. |

*(Note: Validation-specific issues #031, #050, #056, #057 and #060 have been moved to [Testing Status](STATUS/testing_status.md))*

---

## 🟢 Recently Resolved Issues (v9.3.0)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#400** | **Uncertainty UX Mapping** | **Resolved (R400)**. Re-anchored Bayesian Uncertainty status messages from the map center to the bottom-center metadata cluster. Implemented an 80dp vertical offset to maintain visual separation from the `osmdroid` scale bar. |

---

## 🟢 Recently Resolved Issues (v9.2.9)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **R994** | **WakeLock & Screen-Off** | **Resolved**. |
