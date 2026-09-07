# Simplification Ideas - Phase 2

## UI & Navigation
1.  **Forensic Component Consolidation**: Consolidate `LogFilterButton` (in `LogComponents.kt`) and the action buttons in `LogDetailPane` into a single `ForensicActionButton` with variant styles. This reduces duplicate styling logic for buttons used across forensic overlays.
2.  **State Unification**: Evaluate if `KinematicState` and `DiagnosticState` can be partially merged into a `HighFrequencyState` and `ScalarState` to further simplify the `MainViewModel` combine chains.

## Logic & Architecture
3.  **Forensic Auditor Encapsulation**: Extract the `Stability Audit` loop and `Revival Event` observation logic into a standalone `ForensicAuditor` component. Currently, this logic is duplicated between `TrackerService` and `ViewerService`. Encapsulation would reduce boilerplate and ensure uniform audit standards across roles.
4.  **GPS Staleness Thresholding**: Simplify the `UiStateAggregator` GPS status logic by using a unified "Pipeline Health" signal instead of checking individual callback timestamps, which seems to be causing the post-hydration RED lock (Issue #935).
