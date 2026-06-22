# Handover - GPS Tracker Project (v8.9.28)

## ⚖️ Documentation Governance (Engineering Rule)
To maintain audit integrity during the hardening phase, the following workflow is mandatory:
1. **Active Tasks**: Tracked exclusively in `issues.md`.
2. **Promotion**: Once a task is verified on hardware or via code-audit, it is removed from `issues.md`.
3. **Archiving**: The resolution summary is appended to `COMPLIANCE.md`, and the "Verification Manifest" (Chapter 1) is updated.
4. **Specification**: `SoT.md` remains the static logic reference; it must never contain "Fixed" history—only links to `COMPLIANCE.md`.

## 🛠️ Active Development Phase: Forensic & Stability Hardening
### Next Tasks (Priority Order):
1. **Field Verification (Issue #10 / #190)**: Physical verification of Xiaomi MIUI 14 boot-grace stability.

## 📜 Phase History (v8.9.28 Reorganization)
- **Issue #11 Resolved**: Hindsight Promotion Coverage. Implemented exhaustive unit test suite `LocationSentinelHindsightTest.kt` in `:core:engine`. Covered multi-point "rubber-band" transitions, angle/speed consistency gates, and buffer pruning logic to ensure zero-lag trajectory smoothing reliability.
- **Issue #12 Resolved**: SIT Duplicate Guard. Implemented a persistent 15s sanity check in `HistoryManager.kt` using a new `last_history_sit_ts` setting and `SIT_DUPLICATE_GUARD_MS` constant. This prevents redundant forensic SIT markers caused by relay re-transmissions or tracker-side latching.
- **Issue #9 Resolved**: Hardened `TrackerService.kt` and `ViewerService.kt` against Android 14+ `ForegroundServiceStartNotAllowedException`. Implemented `safeStartForeground` wrapper in `BaseMonitorService` and wrapped recovery pulses/foreground updates in `try-catch` blocks.
- **Issue #13 Resolved**: Centralized hardcoded EMA constants in `AppSensorManager.kt` by replacing hardcoded `0.01f` with `LUX_EMA_FAST`.
- **Issue #14 Resolved**: Implemented rising/falling light EMA factors in `LocationSentinel.kt` (LUX_EMA_UP_FAST, LUX_EMA_DOWN_FAST) to match SoT asymmetrical noise-floor tracking.
- **Unified Compliance**: Consolidated `issues_history_archive.md`, `issues_history_v88.md`, and SoT Chapter 9 into `COMPLIANCE.md`.
- **Primary Tracker**: Slimmed `issues.md` to focus on the remaining hardening tasks.
- **SoT Cleanup**: Removed historical noise from `REQUIREMENTS_SOT.md`.
