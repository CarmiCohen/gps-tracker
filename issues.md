# Project Issues & Hardening Tracking (July.22.07)

This document tracks active issues, technical debt, and pending implementation tasks. Historical resolutions are preserved in [RESOLUTION_ARCHIVE.md](STATUS/RESOLUTION_ARCHIVE.md).

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | Active | 0 |
| **Validation Tasks** | 🔍 Tracked | [QA Validation Status](STATUS/QA_VALIDATION_STATUS.md) |
| **Resolved (Total)** | 🟢 Progress | 330 |

---

## ⚠️ Newly Identified Risks & Concerns
*   **Issue #121: Provider Latency**: Circularity resolution via `Provider<T>` is stable but introduces minor lookup overhead in `LogManager`.
*   **Issue #120b: Budget Hardware Initialization Spikes**: Budget devices (A15) remain sensitive. The 500ms staggered startup is critical.

---

## 🔴 Open Issues
*None.*

---

## 🟢 Recently Resolved Issues (July.22.07)
*   **Issue #031: Stability Audit Hardening**.
    *   **Resolution**: Standardized stability auditing across both `TrackerService` and `ViewerService`. Fixed a logic error in the Viewer role where the audit condition prevented execution. This enables reliable gap detection (R951) during 24-hour soak tests.
*   **Issue #108: Startup Recovery Race Hardening**.
    *   **Resolution**: Implemented a `RECOVERY_GRACE_PERIOD_MS` (60s) in `MaintenanceWorker.kt`. The worker now verifies `appStartTime` before triggering recovery, ensuring that the staggered startup sequence (Requirement R955b) has sufficient time to initialize without redundant service restarts.

## 🟢 Recently Resolved Issues (July.22.06)
*   **Issue #113: R405c Fallback Hardening (Samsung A15)**.
    *   **Resolution**: Implemented 5-minute periodic re-registration loop for Step Detector and enhanced Accelerometer fallback pulse visibility in `AppSensorManager.kt`. This fulfills the "Self-Healing" and "Stay-Alive" requirements for Samsung budget hardware.
*   **Issue #125: Baseline Verification**.
    *   **Resolution**: Executed a full clean build (`clean assembleDebug`) to verify Hilt graph integrity. Confirmed zero circularities or compilation errors following the removal of manual DI components.
*   **Issue #126: Complete Hilt Migration and Decommission**.
    *   **Resolution**: Finalized Hilt transition by decommissioning legacy `AppContainer.kt` and `MainViewModelFactory.kt`. Conducted code-wide audit to confirm zero remaining references to manual DI container.

## 🟢 Recently Resolved Issues (July.22.05)
*   **Issue #124: Hilt Migration Completion & AppContainer Decommissioning**.
    *   **Resolution**: Fully migrated all services, activities, repositories, and managers to Hilt. Removed manual DI logic from `GpsApplication` and `BaseMonitorService`.

## 🟢 Recently Resolved Issues (July.22.04)
*   **Issue #511: DataStore Singleton Violation**.
    *   **Resolution**: Refactored `SettingsRepository` to use a single `DataStore` instance via `Context` extension delegate. This ensures that even during the Hilt transition, multiple repository instances share the same underlying `DataStore` connection, preventing `IllegalStateException`.
