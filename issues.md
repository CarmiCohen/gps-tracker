# Hardening Phase: Primary Tracking Document (v8.9.34)

This document tracks all open issues, technical debt, and pending validation tasks for the final hardening phase. Once an item is verified on hardware or through code-audit, it is moved to the **[COMPLIANCE.md](COMPLIANCE.md)** archive.

## 📊 Hardening Progress Dashboard
| Category | Status | Count |
| :--- | :--- | :--- |
| **Open Technical Issues** | 🔴 Critical | 0 |
| **Validation Tasks** | 🟡 Pending Hardware | 1 |
| **Resolved (this phase)** | 🟢 Archived | 26 |

---

## 🟢 Open Issues (Hardening)

### 1. Physical Hardware Validation: Xiaomi & Samsung (Issue #262)
*   **Description**: Implementation of Xiaomi Boot Grace (#262) and Samsung 10Hz polling is complete in code, but requires physical hardware verification to ensure zero-spike behavior during transition. (Linked to Legacy #190).
*   **Location**: `MainAlarmLogic.kt` / Physical Hardware.
*   **Status**: **Pending Validation**.

---

## 🟢 Resolved (this phase)
*   **Issue #263**: EMA Constant Inversion Audit. Corrected inverted weights in `EngineConstants.kt`.
*   **Issue #264**: GtoEngine Magic Number Consolidation. Moved thresholds to `EngineConstants.kt`.
*   **Issue #265**: TrackerService Redundant Evaluation Audit. Optimized `TrackerService.kt` to prevent redundant alarm triggers.
*   **Issue #266**: Lux EMA Implementation Omission. Integrated Slow/Fast variants in `LocationSentinel.kt`.
*   **Issue #267**: Dead Code Cleanup: `isRevivalTriggered`. Removed unused flag in `TrackerService.kt`.
*   **Issue #268**: Acoustic Floor Logic Redundancy. Removed redundant parameter passing in `LocationProcessor.kt`.
*   **Issue #271**: Uptime Consistency. Consolidated redundant session timing fields into `uptimeMs` in `AppSettingsMigration.kt`. (Formerly #1)
*   **Issue #272**: Battery Profile. Implemented discharge profiling in `app_settings.proto`. (Formerly #2)
*   **Issue #273**: Network Integrity. Hardened signaling integrity flags in `app_settings.proto`. (Formerly #3)
*   **Issue #276**: Documentation Gating. Implemented `ALERT_ID_XIAOMI_SYSTEM_MISSING` and updated audit docs. (Formerly #6)
*   **Issue #289**: Dead State Cleanup: Revival Flag. Fixed in `TrackerService.kt` by ensuring `isRevivalTriggered` is set to `true` during GPS revival sequences. (Formerly #19)
*   **Issue #295**: Redundant Barometric Baselining. Fixed by exposing `absoluteAltitude` (raw) in `AppSensorManager.kt` and updating engine consumption. (Formerly #25)
*   **Issue #291**: SIT Forensic Duplicate Risk. Fixed in `TelemetryAggregator.kt` by ensuring `isSitDetected` defaults to `false` during backfill. (Formerly #21)
*   **Issue #292**: Acoustic Floor Decay Logic. Fixed by enforcing `ACOUSTIC_FLOOR_MIN_DB = 25.0` in `LocationSentinel.kt`. (Formerly #22)
*   **Issue #294**: Viewer Offline Detection Logic Gap. Fixed by calculating Viewer connectivity status in `TrackerService.kt`. (Formerly #24)
*   **Issue #296**: `serviceStartRealtime` Initialization Gap. Fixed by explicitly initializing `serviceStartRealtime` in `TrackerService` and `ViewerService`. (Formerly #26)
*   **Issue #288**: Vertical Displacement & Velocity Propagation Failure. Fixed by correctly bridging `AppSensorManager` and `LocationSentinel`. (Formerly #18)
*   **Issue #293**: Geofence Evaluation Bug (Viewer Side). Fixed by using `trackerDistToHome` exclusively in `ViewerService.kt`. (Formerly #23)
*   **Issue #287**: Role-Aware Alert Title Visibility. Refactored `getTrackerTitle()` in `MainAlarmLogic.kt` for forensic parity. (Formerly #17)
*   **Issue #281**: SoT Naming Alignment (IMM). Aligned `DOCS/REQUIREMENTS_SOT.md` with code precision. (Formerly #11)
*   **Issue #285**: GtoEngine Implementation. Implemented `GtoEngine.kt` as per trajectory optimization spec. (Formerly #15)
*   **Issue #297**: Hindsight Promotion Coverage. Implemented exhaustive unit test suite `LocationSentinelHindsightTest.kt`. (Formerly #27)
*   **Issue #282**: SIT Duplicate Guard. Implemented persistent 15s sanity check in `HistoryManager.kt`. (Formerly #12)
*   **Issue #279**: Foreground Resilience Hardening. `TrackerService.kt` and `ViewerService.kt` recovery pulses hardened. (Formerly #9)
*   **Issue #286**: Hardcoded EMA in AppSensorManager. Replaced with `LUX_EMA_FAST`. (Formerly #16)
*   **Issue #284**: Light EMA Logic Inconsistency. Implemented rising/falling EMA factors in `LocationSentinel.kt`. (Formerly #14)
