# Project Issues & Hardening Tracking (v9.3.0)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are in the [Issues Archive](STATUS/issues_archive.md), and validation tasks are in [Testing Status](STATUS/testing_status.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🟡 Active | 5 |
| **Validation Tasks** | 🔍 Tracked | [Testing Status](STATUS/testing_status.md) |
| **Resolved (Total)** | 🟢 Progress | 260 |

---

## ⚠️ Newly Identified Risks & Concerns
| ID | Concern | Description |
| :--- | :--- | :--- |
| **#039** | **Identity Rejection Feedback** | `MainRepository` now silently rejects bulk updates with colliding IDs. UI feedback needed. (Validation: #063) |

---

## 🔴 Open Issues
| ID | Category | Description |
| :--- | :--- | :--- |
| **#058** | **Refactor** | **TrackerService Initialization**: Refactor `onCreate` to move manual dependency injection into Hilt Modules. (Validation: #066) |
| **#059** | **Feature** | **Permission Health Check UI**: Implement a "Diagnostics" screen in Compose for Xiaomi special permissions. (Validation: #064) |
| **#061** | **Cleanup** | **Forensic Logging Consolidation**: Create a `ForensicLogUseCase` to standardize "Special Color" (Pink) logging. (Validation: #065) |
| **#062** | **Hardening** | **Dynamic Anchor Breakout**: Implement displacement-weighted monitor to prevent "sticky anchors". (Validation: #053) |

---

## 🟢 Recently Resolved Issues (v9.3.0)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **#042** | **Sanitization Visibility** | **Resolved**. Added `identity_sanitized` flag to DataStore and implemented UI notification (AlertDialog) to inform user of automatic ID resets. |
| **#030** | **Proto Schema Duplication** | **Resolved (R973)**. Consolidated all schemas into `app/src/main/proto` and removed legacy path. |
| **#055** | **Issue History Recovery** | **Resolved**. Restored 185 "lost" legacy resolutions from `compliance_archive.md`. |
| **#054** | **Requirement ID Collision** | **Resolved**. Audited and corrected `compliance.md` where Issue #326 was overloaded. |
| **#400** | **Uncertainty UX Mapping** | **Resolved (R400)**. Re-anchored Bayesian Uncertainty status messages to bottom-center with 80dp offset. |

---

## 🟢 Recently Resolved Issues (v9.2.9)

| ID | Issue | Resolution |
| :--- | :--- | :--- |
| **R994** | **WakeLock & Screen-Off** | **Resolved**. Dynamic GPS down-sampling implemented. |
