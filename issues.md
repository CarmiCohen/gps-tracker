# Project Issues & Hardening Tracking (Rigorous Audit)

## 🎯 Current Resumption Focus: Structural Simplicity & Pattern Convergence
Finalizing the audit of signaling performance under physical stress and ensuring no side-effects remain from the Performance Tier unification.

## 🔴 Open Gaps & Unfinished Integration Points (Identified from Rigorous Audit)

*   **Issue #1106: Energy Footprint Data Loss following Intermediate Audit Consumption**
    *   *Problem*: The `HardwareLock` event consumes the battery baseline in `ForensicAuditor.computeEnergyFootprint()`, setting it to null.
    *   *Effect*: Since `revivalBaselineCaptured` prevents recapture during the same cycle, the subsequent (and more critical) `Success` event lacks energy footprint data for the total recovery period.
    *   *Details*: `HardwareSuite.kt` lines 726, 730; `ForensicAuditor.kt` line 186.

*   **Issue #1107: GNSS Stall Timing Leakage during Suite Inactivity**
    *   *Problem*: `pendingEnterRt` is initialized and updated in the background `init` loop of `HardwareSuite.kt` even when `isStarted` is false.
    *   *Effect*: If the app is launched indoors and left idle, tracking start will immediately trigger `HardwareLock` due to the "leakage" of the stall duration from the inactive period.
    *   *Details*: `HardwareSuite.kt` lines 205, 715.

*   **Issue #1108: Redundant Battery Baseline Capture in Background/Idle State**
    *   *Problem*: The `init` loop triggers `forensicAuditor.captureRevivalStart()` as soon as the app starts (since `lastFixRt` is 0).
    *   *Effect*: Unnecessary overhead and potential for stale baseline data if tracking starts much later than app initialization.
    *   *Details*: `HardwareSuite.kt` lines 714-722.

*   **Issue #1109: Resource Leak in GNSS Revival Burst during Safe Mode Transition**
    *   *Problem*: `setSafeMode(true)` cancels `revivalBurstJob` but does not unregister the `rawRevivalListener`.
    *   *Effect*: The raw GPS provider remains active if safe mode is toggled during a 10-second burst, leading to unintended high battery drain.
    *   *Details*: `HardwareSuite.kt` lines 739-745, 689-705.

---

## 🟢 Resolved Traceability & Metadata Issues

*   **Issue #1105: Battery Baseline Capture Persistence across Lifecycle Transitions** (Resolved Sep.19.01)
    *   *Remediation*: Ensured `revivalBaselineCaptured` is reset to `false` in both `stop()` and `resetBaseline()`. This guarantees that if the suite is stopped/started or reset while in a GNSS stall state, subsequent pending cycles can correctly capture a fresh battery baseline rather than carrying over a stale `true` flag from previous lifecycle phases.

*   **Issue #1104: Battery Baseline Recapture within Stalled Pending Cycles** (Resolved Sep.19.00)
    *   *Remediation*: Implemented `revivalBaselineCaptured` flag in `HardwareSuite.kt` to ensure a single battery baseline capture per GNSS pending cycle. This prevents premature recapture when intermediate audit events (like `HardwareLock`) consume the internal baseline while the system is still in a pending state.

*   **Issue #1103: Leaked Coroutines in GNSS Revival Burst Timer** (Resolved Sep.18.00)
    *   *Remediation*: Introduced `revivalBurstJob` to track the 10-second raw provider timeout coroutine. Ensured explicit cancellation in `stop()` and `setSafeMode()`, enforcing structured concurrency and preventing background leaks during suite teardown.

*   **Issue #1102: Blocked Thread Restart Latency during Polling Interval Changes** (Resolved Sep.17.11)
    *   *Remediation*: Moved physical hardware unregistration (GNSS, sensors, display) into the 800ms deferred teardown grace period. This allows `start()` to cancel the teardown and reuse existing registrations during rapid lifecycle rotations.

*   **Issue #1101: Asynchronous Unregistration Race Condition in setPowerSaveMode** (Resolved Sep.17.10)
    *   *Remediation*: Consolidated sensor unregistration and re-registration on the `hardwareHandler` looper thread to ensure sequential execution.

*(All other resolved issues have been successfully moved to the Resolution Archive file).*

## 📊 Hardening Progress Dashboard
- **Current Audit Baseline: [SOT: 359 (Rules: 72, IDs: 359), Resolved: 1103, Open: 4, Testing: 2 (Sub-items: 10), Ideas: 18, QA: 281]**
