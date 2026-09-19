# Resolution Archive (Sep.19.01)

## 🟢 Sep.19.01
*   **Battery Baseline Capture Persistence across Lifecycle Transitions (#1105)**: Resolved an issue in `HardwareSuite.kt` where the `revivalBaselineCaptured` flag was not reset to `false` when the suite was stopped or when its baseline was reset. This prevented a new battery baseline from being captured if the hardware suite went through lifecycle transitions (stopped and restarted) or baseline resets while in a GNSS stall state. The fix ensures the flag is properly cleared in `stop()` and `resetBaseline()`, allowing fresh battery baselines to be recorded for subsequent tracking sessions.
*   **Version Advance**: Updated `app/build.gradle` and all status tracking documents to the `Sep.19.01` audit baseline.

## 🟢 Sep.19.00
*   **Battery Baseline Recapture within Stalled Pending Cycles (#1104)**: Resolved a telemetry inconsistency in `HardwareSuite.kt` where the battery baseline for energy footprint audits was being prematurely recaptured during sustained GNSS stalls. Introduced the `revivalBaselineCaptured` flag to ensure that `ForensicAuditor.captureRevivalStart()` is invoked exactly once per pending cycle, even if intermediate audit events (such as `HardwareLock` triggers) consume and reset the internal baseline before the cycle completes. This ensures high-assurance energy metrics for the full duration of hardware recovery attempts.

## 🟢 Sep.18.00
*   **Leaked Coroutines in GNSS Revival Burst Timer (#1103)**: Resolved a coroutine leak in `HardwareSuite.kt` where the 10-second raw GPS unregistration timeout was launched as an un-tracked child of a fire-and-forget registration task. Implemented explicit job tracking via `revivalBurstJob` and ensured immediate cancellation during suite teardown and safe-mode transitions, enforcing structured concurrency across the hardware revival pipeline.

## 🟢 Sep.17.11
*   **Blocked Thread Restart Latency (#1102)**: Resolved a performance bottleneck in `HardwareSuite.kt` where rapid lifecycle rotations (e.g., during polling interval adaptations via `flatMapLatest`) were suffering from artificial latency. Moved physical hardware unregistration (GNSS, sensors, and display listeners) into the 800ms deferred teardown grace period. This allows `start()` to cancel any pending teardown and "rescue" existing registrations, eliminating redundant binder calls to system services and removing the restart stall.

## 🟢 Sep.17.10
*   **Sensor Unregistration Race Condition (#1101)**: Resolved a race condition in `HardwareSuite.kt` where asynchronous unregistration tasks (posted via `ManagedUnregistrationHelper`) could execute after synchronous re-registration during `setPowerSaveMode` calls. Consolidated both operations within the hardware handler looper thread to ensure sequential execution and stable telemetry during power-save transitions.

## 🟢 Sep.17.07
*   **Dead Code Elimination (#1093)**: Completed the purging of deprecated stub contents from `UnifiedPowerPolicy.kt`, `HardwareProvider.kt`, and `UnifiedPowerPolicyProfileTest.kt`. Finalized authority convergence into `HardwareSuite`. Advanced project baseline to Sep.17.07.
*   **Power & Hardware Provider Convergence (#1093)**: Merged legacy authorities into `HardwareSuite.kt` to reduce dependency overhead and consolidate platform state monitoring.
*   **Multi-Service Pool Collision & DB Write Safety (#1094)**: Implemented `Mutex`-based serialization on `updateRibbons` inside `HistoryManager` to isolate concurrent service invocations. Conducted forensic audit verifying that `MainRepository` performs deep copies/mapping to independent `HistoryEntity` snapshots before asynchronous DB batch flushes, guaranteeing full memory isolation and safe flyweight reuse.
*   **Integrity Audit**: Verified that all legacy references have been removed from the production and test paths, and that `HardwareSuite` correctly handles converged power and hardware monitoring logic.
