# Project Issues & Hardening Tracking (Rigorous Audit)

## 🎯 Current Resumption Focus: Structural Simplicity & Pattern Convergence
Finalizing the audit of signaling performance under physical stress and ensuring no side-effects remain from the Performance Tier unification.

## 🔴 Open Gaps & Unfinished Integration Points (Identified from Rigorous Audit)

*   **Issue #1120: Inconsistent Jitter Audit during Adaptive GNSS Throttling**
    *   *Detail*: `ForensicAuditor.recordGnssStatus` uses a hardcoded `GNSS_EXPECTED_INTERVAL_MS` for jitter calculation, but `HardwareSuite` now adapts the GNSS sampling rate (R-ID 348). This causes false jitter alerts during intentional throttling periods.
    *   *Risk*: Spurious stability alerts in logs during cooling or high-load states.
    *   *File*: `ForensicAuditor.kt` (Line 41).

*   **Issue #1121: ViewerService Local Hardware Leak into Remote Alarm Evaluation**
    *   *Detail*: In `ViewerService.evaluateAlarmsInternal`, the alarm manager is passed `snrSnapshot = hardwareSuite.averageSnr`. This uses the Viewer's local SNR to evaluate the remote Tracker's GNSS integrity.
    *   *Risk*: Failure to trigger GNSS-related alarms (Jammer/Stall) for the remote tracker in the Viewer role.
    *   *File*: `ViewerService.kt` (Line 458).

---

## 🟢 Resolved Traceability & Metadata Issues

*   **Issue #1118: Excessive WakeLock Acquisition in Activity-Denied Scenarios** (Resolved Sep.19.13)
    *   *Remediation*: Modified the accelerometer-based stay-alive mechanism in `HardwareSuite.kt` to check for `ACTIVITY_RECOGNITION` permission before poking the system WakeLock. This prevents unintended battery drain on devices where the user has withheld tracking permissions, shifting the system to a passive monitoring state. (R-ID 372)

*   **Issue #1117: Uncontrolled Sensor Registration in setPowerSaveMode Race** (Resolved Sep.19.12)
    *   *Remediation*: Added an explicit `isStarted.get()` check within the `hardwareHandler` runnable in `setPowerSaveMode()`. This ensures that sensors are not re-registered if the suite has been stopped before the asynchronous registration task executes, preventing persistent sensor leaks. (R-ID 371)

*   **Issue #1116: Acoustic Monitor Resource Race on Rapid Restart** (Resolved Sep.19.11)
    *   *Remediation*: Implemented `acousticLock` in `HardwareSuite.kt` to synchronize acoustic monitoring lifecycle transitions. Updated `startAcousticMonitoring()` to definitively join any previous alive `acousticThread` before initiating a new one, preventing simultaneous `AudioRecord` initialization attempts and resource collisions during rapid service restarts. (R-ID 370)

*   **Issue #1115: Stale Forensic and SNR Buffers across Suite Lifecycle** (Resolved Sep.19.10)
    *   *Remediation*: Updated the `stop()` method in `HardwareSuite.kt` to explicitly clear `sensorBuffer`, `snrBuffer`, `logicSnapshotBuffer`, and `forensicSnapshotBuffer`, and reset `lastBufferRecordRt`. This ensures forensic history and GNSS stability metrics are fresh for each service lifecycle restart. (R-ID 369)

*   **Issue #1114: Thread-Safety and Visibility Vulnerabilities in HardwareSuite Snapshotting** (Resolved Sep.19.09)
    *   *Remediation*: Applied `@Volatile` to high-frequency state variables (lux, acoustic, tilt, velocity, etc.) to ensure cross-thread visibility. Unified the synchronization strategy by wrapping both the sensor update paths and the forensic snapshot consumption methods (`consumeLogicSnapshot`, `consumeForensicSnapshot`) in `synchronized(this)`, ensuring atomic read-and-reset operations. (R-ID 368)

*   **Issue #1113: Singleton State Collision in ForensicAuditor Multi-Role Tick** (Resolved Sep.19.08)
    *   *Remediation*: Refactored `ForensicAuditor.kt` to use role-based state tracking via `ConcurrentHashMap`. Implemented `RoleState` to encapsulate stability counters, GNSS jitter peaks, and sensor rate audit flags. Updated `TrackerService.kt` and `ViewerService.kt` to pass role tags ("T" and "V") to auditing methods, ensuring independent audits when both services are active. (R-ID 367)

*   **Issue #1119: Shared Sensor Rate Audit Flag Persistence** (Resolved Sep.19.08)
    *   *Remediation*: Decoupled the `isSensorRateAudited` flag in `ForensicAuditor` by moving it into the role-specific `RoleState` objects. This allows both Tracker and Viewer roles to complete their respective sensor rate efficacy audits independently.

*   **Issue #1112: Incomplete Reset in resetServiceTimers (HardwareSuite State Persistence)** (Resolved Sep.19.07)
    *   *Remediation*: Updated both `TrackerService.kt` and `ViewerService.kt` to call `hardwareSuite.resetBaseline()` within the `resetServiceTimers()` method. This ensures that all internal hardware states, including IMU peak values, adaptive floors, and GNSS revival flags, are properly zeroed when a session is terminated or reset. (R-ID 366)

*   **Issue #1111: Proximity Suppression Lock-in due to Hysteresis Persistence** (Resolved Sep.19.06)
    *   *Remediation*: Implemented temporal decay for the display flickering suppression logic in `HardwareSuite.kt`. By checking if the last display transition occurred within `DISPLAY_FLICKER_TIMEOUT_MS` (3s), the system now allows proximity "Far" transitions once flickering ceases, even without a further display event. Ensured state reset in `stop()` and `resetBaseline()`. (R-ID 365)

*   **Issue #1110: Initialization Race in HardwareSuite.start() causing False GPS Gap** (Resolved Sep.19.05)
    *   *Remediation*: Reordered the state initialization block in `HardwareSuite.start()`. By initializing `sessionStartRt`, `lastBaroZeroingRt`, and `lastFixRt` before `isStarted` is flipped to `true`, the background audit thread can no longer run an audit update loop with non-initialized/zeroed parameters, avoiding false GPS gaps and incorrect battery baselines on session initialization. (R-ID 364)

*   **Issue #1109: Resource Leak in GNSS Revival Burst during Safe Mode Transition** (Resolved Sep.19.04)
    *   *Remediation*: Refactored the GNSS revival pulse logic in `HardwareSuite.kt` to use structured concurrency. By wrapping the burst lifecycle in a `try-finally` block within a single coroutine, Raw and Fused listeners are guaranteed to be unregistered upon completion or cancellation (e.g., during Safe Mode transition). Eliminated the redundant `revivalBurstJob` variable. (R-ID 363)

*   **Issue #1108: Redundant Battery Baseline Capture in Background/Idle State** (Resolved Sep.19.03)
    *   *Remediation*: Fully resolved the redundant battery baseline capture logic by initializing `lastFixRt` to `sessionStartRt` inside both `start()` and `resetBaseline()`. This guarantees a proper grace period before any GNSS gap or stall state can be declared, preventing immediate redundant baseline captures upon app activation or reset. (R-ID 361)

*   **Issue #1107: GNSS Stall Timing Leakage during Suite Inactivity** (Resolved Sep.19.02)
    *   *Remediation*: Implemented explicit reset of revival state variables (`pendingEnterRt`, `revivalAttemptCount`, etc.) in `stop()` and `resetBaseline()`. Guarded the background audit loop in `HardwareSuite.kt` with `isStarted.get()` to prevent `pendingEnterRt` from accumulating stall duration while the suite is inactive, ensuring no immediate hardware locks occur upon activation. (R-ID 360)

*   **Issue #1106: Energy Footprint Data loss following Intermediate Audit Consumption** (Resolved Sep.19.02)
    *   *Remediation*: Modified `ForensicAuditor.computeEnergyFootprint()` to support non-destructive peeking via a `consume` parameter. Updated `HardwareSuite.kt` to use `consume = false` during intermediate `HardwareLock` events, ensuring the final `Success` event retains the full battery baseline for accurate total-cycle energy reporting.

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
- **Current Audit Baseline: [SOT: 372 (Rules: 76, IDs: 372), Resolved: 1118, Open: 2, Testing: 2 (Sub-items: 10), Ideas: 19, QA: 282]**
