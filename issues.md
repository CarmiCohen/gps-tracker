# Project Issues & Hardening Tracking (v9.3.6)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are in the [Resolution Archive](STATUS/RESOLUTION_ARCHIVE.md), and validation tasks are in [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 3 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 260 |

---

## ⚠️ Newly Identified Risks & Concerns
| ID | Concern | Description |
| :--- | :--- | :--- |
| | | |

---

## 🔴 Open Issues
| ID | Category | Description |
| :--- | :--- | :--- |
| **#059** | **Feature** | **Permission Health Check UI**: Implement a "Diagnostics" screen in Compose for Xiaomi special permissions. (Validation: #064) |
| **#061** | **Cleanup** | **Forensic Logging Consolidation**: Create a `ForensicLogUseCase` to standardize "Special Color" (Pink) logging. (Validation: #065) |
| **#062** | **Hardening** | **Dynamic Anchor Breakout**: Implement displacement-weighted monitor to prevent "sticky anchors". (Validation: #053) |

---

## 🟢 Recently Resolved Issues (v9.3.6)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#058** | **TrackerService Initialization** | **Resolved**. Finalized Hilt migration by moving all common dependencies to `BaseMonitorService`. Eliminated `EntryPointAccessors` in `TrackerService`, `ViewerService`, `WatchdogReceiver`, and `GpsApplication`. Standardized component initialization and cleanup. |
| **#047** | **Speed Zeroing Authority** | **Resolved**. Verified immediate speed drop to 0.0 on GPS loss in Viewer HUD. (Requirement R987) |
| **#046** | **State Sync Audit** | **Resolved**. Verified simultaneous Tracker/Viewer HUD state transitions under load. (Requirement R986) |

---

## 🟢 Recently Resolved Issues (v9.3.4)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#039** | **Identity Rejection Feedback** | **Resolved**. Updated `MainRepository.saveSettingsBulk` to throw `IllegalArgumentException` on identity collision. Implemented `BulkUpdateSettings` handling in `MainViewModel` with UI feedback (Toast) and persistent logging. |

---

## 🟢 Recently Resolved Issues (v9.3.2)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#042** | **Sanitization Visibility** | **Resolved**. Implemented `identitySanitizedFlow` in `SettingsRepository` and migration flag to notify UI of auto-sanitization events. |

---

## 🟢 Recently Resolved Issues (v9.3.0)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#030** | **Proto Schema Duplication** | **Resolved (R973)**. Consolidated all schemas into `app/src/main/proto` and removed legacy path. |
| **#055** | **Issue History Recovery** | **Resolved**. Restored 185 "lost" legacy resolutions from the legacy archive. |
| **#054** | **Requirement ID Collision** | **Resolved**. Audited and corrected `VERIFICATION_MANIFEST.md` where Issue #326 was overloaded. |
| **#400** | **Uncertainty UX Mapping** | **Resolved (R400)**. Re-anchored Bayesian Uncertainty status messages to bottom-center with 80dp offset. |
