# Project Issues & Hardening Tracking (Rigorous Audit)

## 🎯 Current Resumption Focus: Structural Simplicity & Pattern Convergence
Finalizing the audit of signaling performance under physical stress and ensuring no side-effects remain from the Performance Tier unification.

## 🔴 Open Gaps & Unfinished Integration Points (Identified from Rigorous Audit)

1. **Issue #1102: Blocked Thread Restart Latency during Polling Interval Changes**
    *   *Description*: When `pollingIntervalFlow` adapts, `hardwareObservationFlow` switches blocks via `flatMapLatest`, invoking `stop()` then `start()`. If active users temporarily drop to 0, `stop()` schedules a `teardownJob` to quit threads after an 800ms delay. The new flow block calls `start()`, which blocks and joins `teardownJob`. This introduces an artificial 800ms stall/latency before updates resume at the new polling frequency.
    *   *Impact*: Unfinished integration point causing a performance/latency bottleneck during dynamic interval adaptation.

2. **Issue #1103: Leaked Coroutines in GNSS Revival Burst Timer**
    *   *Description*: In `restartLocationUpdates()`, the 10-second timeout loop to unregister the raw GPS provider is launched via an un-tracked `scope.launch` inside the main revival block instead of as a child job of `revivalPulseJob`. Consequently, calling `stop()` and cancelling `revivalPulseJob` fails to cancel the 10-second timeout coroutine, causing background leakage after the suite teardown completes.
    *   *Impact*: Remaining risk / Non-structured concurrency leak during rapid lifecycle switches.

3. **Issue #1104: Battery Baseline Recapture within Stalled Pending Cycles**
    *   *Description*: Inside `checkRevivalLifecycle()`, `forensicAuditor.captureRevivalStart(nowRt)` is executed every 2 seconds while `currentStatus.isPending` is true. When an event like `HardwareLock` clears/computes the footprint and resets `revivalStartBattery` to `null`, the next check tick 2 seconds later immediately snaps a new battery baseline while the hardware is still stalled or locked, instead of waiting for a clean pending cycle.
    *   *Impact*: Inconsistent metric telemetry under sustained hardware faults.

---

## 🟢 Resolved Traceability & Metadata Issues

*   **Issue #1101: Asynchronous Unregistration Race Condition in setPowerSaveMode** (Resolved Sep.17.10)
    *   *Remediation*: Consolidated sensor unregistration and re-registration on the `hardwareHandler` looper thread to ensure sequential execution.

*(All other resolved issues have been successfully moved to the Resolution Archive file).*

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 355 (Rules: 72, IDs: 355), Resolved: 1099, Open: 3, Testing: 2 (Sub-items: 10), Ideas: 18, QA: 281]**
