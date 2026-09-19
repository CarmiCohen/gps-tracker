# Project Resolution Archive (Sep.15.101)

## 🟢 Sep.19.04
*   **Resource Leak in GNSS Revival Burst during Safe Mode Transition (#1109)**: Refactored GNSS revival pulse logic in `HardwareSuite.kt` to use a `try-finally` block within a single coroutine. This guarantees that Raw and Fused location listeners are always unregistered upon burst completion or cancellation (e.g., during Safe Mode transition or suite shutdown). The refactoring enabled the removal of the redundant `revivalBurstJob` state variable, simplifying the class's resource management (R-ID 363).
*   **Audit Baseline Advance**: advanced project metrics to [SOT Rules: 72, SOT IDs: 363, Resolved: 1109, QA: 281].

## 🟢 Sep.19.03
*   **Redundant Battery Baseline Capture in Background/Idle State (#1108)**: Fully resolved the redundant battery baseline capture logic by initializing `lastFixRt` to `sessionStartRt` inside both `start()` and `resetBaseline()`. This guarantees a proper grace period before any GNSS gap or stall state can be declared, preventing immediate redundant baseline captures upon app activation or reset (R-ID 361).
*   **Audit Baseline Advance**: advanced project metrics to [SOT Rules: 72, SOT IDs: 362, Resolved: 1108, QA: 281].

## 🟢 Sep.19.02
*   **GNSS Stall Timing Leakage during Suite Inactivity (#1107)**: Resolved a critical logic flaw in `HardwareSuite.kt` where the background audit loop was accumulating GNSS stall duration (`pendingEnterRt`) even while the suite was inactive. Guarded the background coroutine with `isStarted.get()` and implemented an explicit reset of revival state variables in `stop()` and `resetBaseline()`. This prevents immediate `HardwareLock` triggers upon app activation after prolonged indoor idle periods (R-ID 360).
*   **Redundant Battery Baseline Capture in Background/Idle State (#1108)**: Optimized power consumption by preventing `ForensicAuditor.captureRevivalStart()` from being triggered during app initialization or idle states. The capture is now strictly gated by the suite's active lifecycle (R-ID 361).
*   **Resource Leak in GNSS Revival Burst during Safe Mode (#1109)**: Modified `setSafeMode(active)` in `HardwareSuite.kt` to explicitly unregister `rawRevivalListener` and `revivalCallback` when Safe Mode is enabled. This ensures that the raw GPS provider and high-accuracy fused updates are immediately terminated if Safe Mode is toggled during a 10-second burst, preventing unintended battery drain (R-ID 362).
*   **Energy Footprint Data Loss following Intermediate Audit Consumption (#1106)**: Modified `ForensicAuditor.computeEnergyFootprint()` to support non-destructive peeking via a `consume` parameter. Updated `HardwareSuite.kt` to use `consume = false` during intermediate `HardwareLock` events, ensuring the final `Success` event retains the full battery baseline for accurate total-cycle energy reporting.

## 🟢 Sep.19.01
*   **Battery Baseline Capture Persistence across Lifecycle Transitions (#1105)**: Resolved an issue in `HardwareSuite.kt` where the `revivalBaselineCaptured` flag was not reset to `false` when the suite was stopped or when its baseline was reset. The fix ensures the flag is properly cleared in `stop()` and `resetBaseline()`, allowing fresh battery baselines to be recorded for subsequent tracking sessions (R-ID 359).

*(All other resolved issues have been successfully moved to the Resolution Archive file).*
