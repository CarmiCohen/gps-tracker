# Simplification Ideas - Phase 2

## UI & Navigation
1.  **Forensic Component Consolidation**: Consolidate `LogFilterButton` (in `LogComponents.kt`) and the action buttons in `LogDetailPane` into a single `ForensicActionButton` with variant styles. This reduces duplicate styling logic for buttons used across forensic overlays.
2.  **State Unification**: Evaluate if `KinematicState` and `DiagnosticState` can be partially merged into a `HighFrequencyState` and `ScalarState` to further simplify the `MainViewModel` combine chains.
