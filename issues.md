# Hardening Phase: Primary Tracking Document (v8.9.33)

This document tracks all open issues, technical debt, and pending validation tasks for the final hardening phase. Once an item is verified on hardware or through code-audit, it is moved to the **[COMPLIANCE.md](COMPLIANCE.md)** archive.

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Critical | 1 |
| **Validation Tasks** | 🟡 Pending Hardware | 1 |
| **Resolved (this phase)** | 🟢 Archived | 24 |

---

## 1. Physical Hardware Validation: Xiaomi & Samsung (Issue #10 / #190)
*   **Description**: Implementation of Xiaomi Boot Grace and Samsung 10Hz polling is complete in code, but requires physical hardware verification to ensure zero-spike behavior during transition.
*   **Location**: `MainAlarmLogic.kt` / Physical Hardware.
*   **Status**: **Pending Validation**.

---

## Resolved (this phase)
*   **Issue #19**: Dead Code: Revival Flag. Fixed in `TrackerService.kt` by ensuring `isRevivalTriggered` is set to `true` during GPS revival sequences (Xiaomi heuristic recovery and GPS stall recovery).
*   **Issue #25**: Redundant Barometric Baselining. Fixed by exposing `absoluteAltitude` (raw) in `AppSensorManager.kt` and updating `TrackerService.kt` to pass this raw value to the engine. This allows `LocationSentinel.kt` to handle primary baselining, restoring sensitivity to kinetic events.
*   **Issue #21**: SIT Forensic Duplicate Risk. Fixed in `TelemetryAggregator.kt` by ensuring `isSitDetected` defaults to `false` during `backfillGaps` when explicit sensor samples are missing.
*   **Issue #22**: Acoustic Floor Decay Logic. Fixed by enforcing `ACOUSTIC_FLOOR_MIN_DB = 25.0` in `LocationSentinel.kt` to prevent excessive sensitivity in quiet environments.
*   **Issue #24**: Viewer Offline Detection Logic Gap. Fixed by calculating Viewer connectivity status in `TrackerService.kt` and passing it to `AppAlarmManager.evaluateAlarms`.
*   **Issue #26**: `serviceStartRealtime` Initialization Gap. Fixed by explicitly initializing `serviceStartRealtime` at the end of engine initialization in `TrackerService` and `ViewerService`.
*   **Issue #18**: Vertical Displacement & Velocity Propagation Failure. Fixed by correctly consuming and propagating `peakVerticalVelocity` and `peakVerticalDisplacement` from `AppSensorManager` to `LocationSentinel` via `TrackerService` and `LocationProcessor`.
*   **Issue #23**: Geofence Evaluation Bug (Viewer Side). Fixed by using `trackerDistToHome` exclusively in `ViewerService.kt`.
*   **Issue #17**: Role-Aware Alert Title Visibility (Forensic Parity). Refactored `getTrackerTitle()` in `MainAlarmLogic.kt` to be fully role-aware.
*   **Issue #16**: SoT Naming Alignment (IMM). Aligned `DOCS/REQUIREMENTS_SOT.md` with code precision.
*   **Issue #15**: GtoEngine Documentation/Code Inconsistency. Implemented `GtoEngine.kt` and integrated it into `LocationSentinel.kt`.
*   **Issue #11**: Hindsight Promotion Coverage. Implemented exhaustive unit test suite `LocationSentinelHindsightTest.kt`.
*   **Issue #12**: SIT Duplicate Guard. Implemented persistent 15s sanity check in `HistoryManager.kt`.
*   **Issue #9**: Foreground Resilience Hardening. `TrackerService.kt` and `ViewerService.kt` recovery pulses hardened.
*   **Issue #13**: Hardcoded EMA in AppSensorManager. Replaced with `LUX_EMA_FAST`.
*   **Issue #14**: Light EMA Logic Inconsistency. Implemented rising/falling EMA factors in `LocationSentinel.kt`.
