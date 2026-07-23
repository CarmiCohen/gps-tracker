# Project Issues & Hardening Tracking (July.23.03)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 1 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 352 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Issue #528: Tooling Limitation - Orphaned DashboardUseCase.kt**.
    *   **Description**: Unable to physically delete `DashboardUseCase.kt` due to lack of file deletion tool in the current environment. The file is orphaned but remains in the filesystem.

---

## 🔴 Open Issues
*   **Issue #530: Validation - Urban Multipath Stress Testing**.
    *   **Description**: Need to verify the "Accuracy Recovery" grace logic (#529) through field tests in high-density urban areas (Level 4 canyons) to ensure no regression in real theft detection.

---

## 🟢 Recently Resolved Issues (July.23.03)
*   **Issue #529: Geofence Reliability - Urban Accuracy Snap False Positives**.
    *   **Resolution**: Implemented "Accuracy Recovery" grace logic in `PhysicsUtils.isVisualJump`. The engine now detects significant accuracy improvements and suppresses jump scores if the spatial movement is within the previous fix's uncertainty range.
*   **Issue #527: Siren Persistence - State Restoration after Service Kill**.
    *   **Resolution**: Implemented alarm state persistence using DataStore. Added `restoreState()` to `AppAlarmManager` and integrated it into `TrackerService.onCreate()`. Added background siren maintenance in `TrackerService.processTick()` to ensure audio resumes if the service is restarted during an active violation.
*   **Issue #526: Power Optimization - Adaptive Sensor Sampling**.
    *   **Resolution**: Implemented two-tier power saving. 
        1. **Logic Tier**: Logic tick interval extends from 2s to 10s and Microphone uses a 20% duty cycle.
        2. **Hardware Tier**: Dynamically downgrades `Linear Acceleration` sampling from `SENSOR_DELAY_FASTEST` to `SENSOR_DELAY_NORMAL` when stationary/stalled. Centralized evaluation in `ServiceBehaviorUseCase.kt`.
*   **Issue #525: State Audit - Forensic Propagation Verification**.
    *   **Resolution**: Verified and hardened the end-to-end propagation of forensic parameters. Fixed a mapping bug in `HistoryManager` that caused forensic data loss in local ribbons. Synchronized `ConnectivitySuite` to ensure all 7+ forensic indices are transmitted to remote viewers.
*   **Issue #524: UI Decoupling - Dashboard State Extraction**.
    *   **Resolution**: Extracted UI formatting logic from `DashboardUseCase` into a dedicated `DashboardStateProvider`. `MainViewModel` now depends on the provider, reducing its complexity.
*   **Issue #523: Forensic Snapshot Consolidation**.
    *   **Resolution**: Implemented `AppSensorManager.consumeForensicSnapshot()` to provide an atomic immutable state of all forensic parameters. Refactored `TrackerService.kt` to use this snapshot.
