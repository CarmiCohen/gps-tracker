# Resolution Archive

## [Oct13.30] - Build Restoration & Pure Event Finalization
**Issue ID**: #ISSUE-OCT13.21-H1, #ISSUE-OCT13.21-M1, #ISSUE-OCT13.21-L1, #SIMP-OCT13.21-S2
**Status**: RESOLVED
**Description**: Restored engine and app compilation after over-aggressive hygiene pruning. Pruned remaining dead code and completed the "Pure Event" UI transition.
**Root Cause**: 
- **Constant Pruning**: Critical thresholds `JUMP_HOLD_DURATION_MS` and `GPS_INDEX_AGE_SCALING` were removed, breaking engine logic.
- **Signature Mismatches**: Refactors to `BaseMonitorService` left `MonitorService` with incorrect overrides.
- **Prop Drilling**: Activity-level orchestration (launchers, service control) was still being passed via lambdas instead of the `UiEffect` bus.
**Remediation**:
- **Build Recovery**: Restored constants to `EngineConstants.kt` and fixed `verifyProjectIntegrity` version parsing.
- **Dead Code Pruning**: Removed `isAdaptiveJump` (Engine/App), `qScale` (Sentinel), and `nowTs` (Sentinel) across all layers.
- **Pure Event Transition**: Refactored `MainActivity` and `MainAppContent` to use the `UiEffect` bus for all system actions, removing 10+ injected lambda parameters.
- **Signature Harmonization**: Updated `onServiceInitialize` and `loadLogicState` in `MonitorService.kt` to be suspendable and consistent with base class.

## [Oct13.21] - Dead Code & Hygiene Cleanup
**Issue ID**: #ISSUE-OCT13.20-L1
**Status**: RESOLVED
**Description**: Comprehensive cleanup of unused parameters, variables, and redundant logic across UI, Engine, and Infrastructure layers.
**Root Cause**: 
- **Refactor Leftovers**: Parameters in `ViewerScreen` and `TrackerScreen` became redundant after the `UnifiedHUDOverlay` migration.
- **Engine Evolution**: Local variables in `LocationProcessor` and `LocationSentinel` were no longer needed following recent architectural shifts.
- **Redundant Casts**: Legacy type assertions in `MapOverlayManager` remained after icon pooling optimizations.
**Remediation**:
- **Signature Pruning**: Slimmed down UI composable signatures to use centralized event handlers.
- **Engine Cleanup**: Pruned unused state trackers and retired obsolete constants.
- **Logic Correction**: Fixed acoustic lockout constant mismatch in `LocationSentinel`.

## [Oct13.20] - Signaling Hardening & API Modernization
... (Rest of file preserved)
