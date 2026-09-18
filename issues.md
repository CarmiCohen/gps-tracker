# Project Issues & Hardening Tracking (Rigorous Audit)

## 🎯 Current Resumption Focus: Structural Simplicity & Pattern Convergence
Finalizing the audit of signaling performance under physical stress and ensuring no side-effects remain from the Performance Tier unification.

## 🔴 Open Gaps & Unfinished Integration Points (Identified from Rigorous Audit)

1. **Issue #1104: Battery Baseline Recapture within Stalled Pending Cycles**
    *   *Description*: Inside `checkRevivalLifecycle()`, `forensicAuditor.captureRevivalStart(nowRt)` is executed every 2 seconds while `currentStatus.isPending` is true. When an event like `HardwareLock` clears/computes the footprint and resets `revivalStartBattery` to `null`, the next check tick 2 seconds later immediately snaps a new battery baseline while the hardware is still stalled or locked, instead of waiting for a clean pending cycle.
    *   *Impact*: Inconsistent metric telemetry under sustained hardware faults.

---

## 🟢 Resolved Traceability & Metadata Issues

*   **Issue #1103: Leaked Coroutines in GNSS Revival Burst Timer** (Resolved Sep.18.00)
    *   *Remediation*: Introduced `revivalBurstJob` to track the 10-second raw provider timeout coroutine. Ensured explicit cancellation in `stop()` and `setSafeMode()`, enforcing structured concurrency and preventing background leaks during suite teardown.

*   **Issue #1102: Blocked Thread Restart Latency during Polling Interval Changes** (Resolved Sep.17.11)
    *   *Remediation*: Moved physical hardware unregistration (GNSS, sensors, display) into the 800ms deferred teardown grace period. This allows `start()` to cancel the teardown and reuse existing registrations during rapid lifecycle rotations.

*   **Issue #1101: Asynchronous Unregistration Race Condition in setPowerSaveMode** (Resolved Sep.17.10)
    *   *Remediation*: Consolidated sensor unregistration and re-registration on the `hardwareHandler` looper thread to ensure sequential execution.

*(All other resolved issues have been successfully moved to the Resolution Archive file).*

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 356 (Rules: 72, IDs: 356), Resolved: 1101, Open: 1, Testing: 2 (Sub-items: 10), Ideas: 19, QA: 281]**
