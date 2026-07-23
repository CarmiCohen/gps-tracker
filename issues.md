# Project Issues & Hardening Tracking (July.23.04)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 1 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 356 |

---

## ⚠️ Newly Identified Risks & Concerns
*   (None currently identified)

---

## 🔴 Open Issues
*   **Issue #530: Validation - Urban Multipath Stress Testing**.
    *   **Description**: Need to verify the "Accuracy Recovery" grace logic (#529) through field tests in high-density urban areas (Level 4 canyons) to ensure no regression in real theft detection.

---

## 🟢 Recently Resolved Issues (July.23.04)
*   **Issue #533: Hardening - Stationary Anchor Refinement**.
    *   **Resolution**: Implemented coordinate-averaging convergence in `LocationProcessor.kt` using an 8-point sliding window buffer (`anchorAveragingBuffer`). Refined breakout scoring logic to integrate displacement trends and velocity weights. This suppresses micro-drifts and GPS "spaghetti" trails in urban canyons while maintaining immediate responsiveness to physical IMU triggers.
*   **Issue #532: Hardening - Type Safety Audit (R999)**.
    *   **Resolution**: Conducted a full audit of the telemetry pipeline. Upgraded hardware processing in `AppSensorManager` to use `Double` precision for all kinematic integrations. Verified that `GpsManager`, `LocationProcessor`, `PhysicsUtils`, and Room/Protobuf persistence layers strictly adhere to the `Double` standard to prevent precision drift.
*   **Issue #531: Hardening - Acoustic Duty Cycle Logic Refinement**.
    *   **Resolution**: Refined `ForegroundServiceType` management to use the *intent* to monitor (`isAcousticMonitoringEnabled`) rather than the instantaneous recording state. This prevents OS notification flickering during the 8s "OFF" phases of the power-saving duty cycle.
*   **Issue #528: Tooling Limitation - Orphaned DashboardUseCase.kt**.
    *   **Resolution**: Logic migrated to `DashboardStateProvider`. Since physical deletion is restricted by environment tools, the file was decommissioned as a tombstone with a `@Deprecated` annotation. All references in `MainViewModel` and `DashboardStateProvider` were scrubbed.
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
