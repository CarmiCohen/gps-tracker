# Handover - GPS Tracker Project (v8.9.29)

## ⚖️ Documentation Governance (Engineering Rule)
To maintain audit integrity during the hardening phase, the following workflow is mandatory:
1. **Active Tasks**: Tracked exclusively in `issues.md`.
2. **Promotion**: Once a task is verified on hardware or via code-audit, it is removed from `issues.md`.
3. **Archiving**: The resolution summary is appended to `COMPLIANCE.md`, and the "Verification Manifest" (Chapter 1) is updated.
4. **Specification**: `SoT.md` remains the static logic reference; it must never contain "Fixed" history—only links to `COMPLIANCE.md`.

## 🛠️ Active Development Phase: Forensic & Stability Hardening
### Next Tasks (Priority Order):
1. **Field Verification (Issue #10 / #190)**: Physical verification of Xiaomi MIUI 14 boot-grace stability.

## 📜 Phase History (v8.9.29 Reorganization)
- **Issue #17 Resolved**: Role-Aware Alert Title Visibility. Refactored `getTrackerTitle()` in `MainAlarmLogic.kt` to be fully role-aware. The function now always strips "This device:" for local clarity and dynamically strips "Tracker:" or "Viewer:" based on the current device role. This ensures clean local UI while preserving forensic parity for remote peer alerts.
- **Issue #16 Resolved**: SoT Naming Alignment (IMM). Aligned `DOCS/REQUIREMENTS_SOT.md` with code precision (changed `IMM_STATION_PROBABILITY` to `IMM_STATIONARY_PROBABILITY`).
- **Issue #15 Resolved**: GtoEngine Implementation. Implemented `GtoEngine.kt` as a formal sliding-window Graph Trajectory Optimization system. Integrated into `LocationSentinel.kt` to replace simpler hindsight logic.
- **Issue #11 Resolved**: Hindsight Promotion Coverage. Implemented exhaustive unit test suite `LocationSentinelHindsightTest.kt` in `:core:engine`.
- **Issue #12 Resolved**: SIT Duplicate Guard. Implemented a persistent 15s sanity check in `HistoryManager.kt`.
- **Issue #9 Resolved**: Hardened `TrackerService.kt` and `ViewerService.kt` against Android 14+ `ForegroundServiceStartNotAllowedException`.
- **Issue #13 Resolved**: Centralized hardcoded EMA constants in `AppSensorManager.kt`.
- **Issue #14 Resolved**: Implemented rising/falling light EMA factors in `LocationSentinel.kt`.
