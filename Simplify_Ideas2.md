# Simplification Ideas (Sep.05.16)

## 💡 Architectural Streamlining
1.  **LED Logic Centralization**: Currently, LED status logic (INT, SRV, GPS, VWR/TRK, DAT) is calculated during HUD aggregation in `DashboardStateProviderImpl`. Consider moving these definitions into a `LedStateEvaluator` utility in the `:core:engine` to ensure consistent "Actual Color" behavior between the dashboard, HUD, and potential future notification LEDs.
2.  **Hydration State Consolidation**: The current 11-level hydration process, while robust, introduces complex race conditions. Consider merging levels 3-10 into a single "Engine Warmup" phase to reduce transient state emissions.
3.  **Navigation Guard Decorator**: Instead of manual `isSystemActive` checks in `MainAppContent`, implement a custom `NavHost` wrapper that automatically enforces system-active invariants for the `Landing` route.
4.  **Forensic Log Pruning**: The stack trace capture in `MainActivity` should be moved to a `ForensicManager` utility to keep the Activity lifecycle code clean once Issue #910 is fully verified in production.
5.  **Role-Agnostic Signaling Pipeline**: With Issue #918, binary telemetry processing is now enabled for both Tracker and Viewer modes. This allows for a further simplification where the signaling reception logic is unified entirely, removing "if (isTrackerMode)" guards from the telemetry handling path.
