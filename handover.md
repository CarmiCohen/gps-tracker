# Handover - GPS Tracker Project (v8.9.33)

## ⚖️ Documentation Governance (Engineering Rule)
To maintain audit integrity during the hardening phase, the following workflow is mandatory:
1. **Active Tasks**: Tracked exclusively in `issues.md`.
2. **Promotion**: Once a task is verified on hardware or via code-audit, it is moved to the "Resolved" section of `issues.md`.
3. **Archiving**: The resolution summary is appended to `COMPLIANCE.md`, and the "Verification Manifest" (Chapter 1) is updated.
4. **Specification**: `SoT.md` remains the static logic reference; it must never contain "Fixed" history—only links to `COMPLIANCE.md`.

## 🛠️ Active Development Phase: Forensic & Stability Hardening
### Next Tasks (Priority Order):
1. **Field Verification (Issue #10 / #190)**: Physical verification of Xiaomi MIUI 14 boot-grace stability.

## 📜 Phase History (v8.9.33 Reorganization)
- **Issue #19 Resolved**: Fixed dead code `isRevivalTriggered` flag. Remedied the logic in `TrackerService.kt` by ensuring the flag is set to `true` during Xiaomi heuristic recovery pulses and standard GPS stall revival attempts. This ensures the session state accurately reflects active revival sequences.
- **Issue #25 Resolved**: Redundant Barometric Baselining. Remedied double-filtering by modifying `AppSensorManager.kt` to expose `absoluteAltitude` (raw) and updating `TrackerService.kt` to pass this raw value to `LocationSentinel`. This ensures the engine handles the primary baselining, restoring sensitivity to kinetic events like chair occupancy.
- **Issue #21 Resolved**: SIT Forensic Duplicate Risk. Fixed in `TelemetryAggregator.kt` by ensuring `isSitDetected` defaults to `false` during `backfillGaps` when explicit sensor samples are missing.
- **Issue #22 Resolved**: Acoustic Floor Decay Logic. Hardened `LocationSentinel.kt` by enforcing a minimum acoustic floor (`ACOUSTIC_FLOOR_MIN_DB = 25.0`).
- **Issue #24 Resolved**: Viewer Offline Detection Logic Gap. Modified `TrackerService.kt` to calculate Viewer connectivity status and pass it to `alarmManager.evaluateAlarms`.
- **Issue #26 Resolved**: `serviceStartRealtime` Initialization Gap. Explicitly initialized `serviceStartRealtime` at the end of engine initialization in `TrackerService.kt` and `ViewerService.kt`.
- **Issue #18 Resolved**: Vertical Displacement & Velocity Propagation. Corrected the bridge between `AppSensorManager` and `LocationSentinel` for peak vertical velocity and displacement.
- **Issue #23 Resolved**: Geofence Evaluation Bug (Viewer Side). Corrected `ViewerService.kt` to use `trackerDistToHome` exclusively.
- **Issue #17 Resolved**: Role-Aware Alert Title Visibility. Refactored `getTrackerTitle()` in `MainAlarmLogic.kt` to be fully role-aware.
- **Issue #16 Resolved**: SoT Naming Alignment (IMM). Aligned `DOCS/REQUIREMENTS_SOT.md` with code precision.
- **Issue #15 Resolved**: GtoEngine Implementation. Implemented `GtoEngine.kt` and integrated into `LocationSentinel.kt`.
- **Issue #11 Resolved**: Hindsight Promotion Coverage. Implemented exhaustive unit test suite `LocationSentinelHindsightTest.kt`.
- **Issue #12 Resolved**: SIT Duplicate Guard. Implemented a persistent 15s sanity check in `HistoryManager.kt`.
- **Issue #9 Resolved**: Hardened foreground service transitions for Android 14+.
- **Issue #13 Resolved**: Centralized EMA constants in `AppSensorManager.kt`.
- **Issue #14 Resolved**: Implemented rising/falling light EMA factors in `LocationSentinel.kt`.
