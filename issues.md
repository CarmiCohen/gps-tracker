# Project Issues & Hardening Tracking (Rigorous Audit)

## 🎯 Current Resumption Focus: Structural Simplicity & Pattern Convergence
Finalizing the audit of signaling performance under physical stress and ensuring no side-effects remain from the Performance Tier unification.

## 🔴 Open Gaps & Unfinished Integration Points (Identified from Rigorous Audit)

*   **Issue #1111: Proximity Suppression Lock-in due to Hysteresis Persistence**
    *   *Detail*: `HardwareSuite` uses `isDisplayFlickering` to suppress proximity "Far" transitions (Line 532). However, `isDisplayFlickering` is only reset to `false` within the `onDisplayChanged` callback when a stable transition is detected (Line 243). If the display stops flickering but remains in a stable state without a further transition event being emitted, the flag persists, potentially blocking all future proximity updates.
    *   *Risk*: Proximity detection stuck in "Near" state indefinitely while stationary.
    *   *File*: `HardwareSuite.kt` (Lines 232-243, 532).

*   **Issue #1112: Incomplete Reset in resetServiceTimers (HardwareSuite State Persistence)**
    *   *Detail*: Both `TrackerService` and `ViewerService` reset `ForensicAuditor` during session termination but fail to call `hardwareSuite.resetBaseline()`. This leaves internal `HardwareSuite` states—such as IMU peaks, adaptive vibration floors, and revival flags—in a stale state across session resets.
    *   *Risk*: Corrupted telemetry and inconsistent baseline comparisons after a session reset.
    *   *Files*: `TrackerService.kt` (Line 427), `ViewerService.kt` (Line 389).

*   **Issue #1113: Singleton State Collision in ForensicAuditor Multi-Role Tick**
    *   *Detail*: `ForensicAuditor` is a Singleton tracking a single `lastStabilityAuditTs` and audit counters. When both `TrackerService` and `ViewerService` are active (or rapidly alternating), they both invoke `evaluateStability()`. The first service to tick after the audit interval consumes the results and resets the shared counters, leaving the other service with no audit data.
    *   *Risk*: Erratic and roles-crossed stability logs in history; reliability metrics mangled due to shared state.
    *   *File*: `ForensicAuditor.kt` (Lines 94-130).

*   **Issue #1114: Thread-Safety and Visibility Vulnerabilities in HardwareSuite Snapshotting**
    *   *Detail*: Multiple non-volatile fields (e.g., `currentLux`, `currentAcousticDb`, `currentTiltDegrees`, `lastAnomalyActiveRt`) are updated on background threads and read in `consumeLogicSnapshot()` or `onSatelliteStatusChanged` on different threads without synchronization or volatile qualifiers. Furthermore, the "read-and-reset" pattern for peaks (e.g., `logicPeakVibration`) uses a different lock (`synchronized(logicSnapshotBuffer)`) than the update path (`synchronized(this)`).
    *   *Risk*: Memory visibility issues (Double/Long tearing) and inconsistent forensic snapshots under high system load.
    *   *File*: `HardwareSuite.kt` (Lines 605-625, 218).

*   **Issue #1115: Stale Forensic and SNR Buffers across Suite Lifecycle**
    *   *Detail*: `HardwareSuite` circular buffers (`sensorBuffer`, `snrBuffer`, `logicSnapshotBuffer`, `forensicSnapshotBuffer`) are only cleared in `resetBaseline()`. They are not cleared in `stop()`. If the service is stopped and restarted without a full process termination, the buffers contain data from the previous session.
    *   *Risk*: Corrupted history graphs and delayed GNSS throttling recovery after service restarts.
    *   *File*: `HardwareSuite.kt` (Lines 320-330).

*   **Issue #1116: Acoustic Monitor Resource Race on Rapid Restart**
    *   *Detail*: `stopAcousticMonitoring()` interrupts the monitor thread but joins with a 1000ms timeout. If it times out, `AudioRecord` release is not guaranteed before `startAcousticMonitoring()` is called again, which may fail to initialize a new `AudioRecord` while the old one is still closing.
    *   *Risk*: Acoustic monitoring failure or "Hardware Failure" log spam during rapid suite restarts.
    *   *File*: `HardwareSuite.kt` (Lines 660-705).

*   **Issue #1117: Uncontrolled Sensor Registration in setPowerSaveMode Race**
    *   *Detail*: `setPowerSaveMode` posts a block to the handler thread that calls `registerSensors()` without checking if the suite is still started. If `stop()` runs and unregisters listeners before this block executes, sensors are re-registered on a dead session.
    *   *Risk*: Permanent sensor listener leak and high battery drain after service stop.
    *   *File*: `HardwareSuite.kt` (Line 830).

*   **Issue #1118: Excessive WakeLock Acquisition in Activity-Denied Scenarios**
    *   *Detail*: If Step Detector registration fails (e.g., Activity Recognition permission denied), the accelerometer handler acquires a WakeLock every 10 seconds to "stay alive" (Line 508). This persists indefinitely, draining battery.
    *   *Risk*: Unintended battery drain on devices where specific permissions are withheld.
    *   *File*: `HardwareSuite.kt` (Line 508).

*   **Issue #1119: Shared Sensor Rate Audit Flag Persistence**
    *   *Detail*: `isSensorRateAudited` is a singleton flag in `ForensicAuditor`. Once one role completes the audit, it is suppressed for the other role indefinitely until a full reset, even if the other role has different sensor constraints.
    *   *Risk*: Incomplete forensic auditing if roles are toggled or running concurrently.
    *   *File*: `ForensicAuditor.kt` (Line 158).

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

*   **Issue #1110: Initialization Race in HardwareSuite.start() causing False GPS Gap** (Resolved Sep.19.05)
    *   *Remediation*: Reordered the state initialization block in `HardwareSuite.start()`. By initializing `sessionStartRt`, `lastBaroZeroingRt`, and `lastFixRt` before `isStarted` is flipped to `true`, the background audit thread can no longer run an audit update loop with non-initialized/zeroed parameters, avoiding false GPS gaps and incorrect battery baselines on session initialization. (R-ID 364)

*   **Issue #1109: Resource Leak in GNSS Revival Burst during Safe Mode Transition** (Resolved Sep.19.04)
    *   *Remediation*: Refactored the GNSS revival pulse logic in `HardwareSuite.kt` to use structured concurrency. By wrapping the burst lifecycle in a `try-finally` block within a single coroutine, Raw and Fused listeners are guaranteed to be unregistered upon completion or cancellation (e.g., during Safe Mode transition). Eliminated the redundant `revivalBurstJob` variable. (R-ID 363)

*   **Issue #1108: Redundant Battery Baseline Capture in Background/Idle State** (Resolved Sep.19.03)
    *   *Remediation*: Fully resolved the redundant battery baseline capture logic by initializing `lastFixRt` to `sessionStartRt` inside both `start()` and `resetBaseline()`. This guarantees a proper grace period before any GNSS gap or stall state can be declared, preventing immediate redundant baseline captures upon app activation or reset. (R-ID 361)

*   **Issue #1107: GNSS Stall Timing Leakage during Suite Inactivity** (Resolved Sep.19.02)
    *   *Remediation*: Implemented explicit reset of revival state variables (`pendingEnterRt`, `revivalAttemptCount`, etc.) in `stop()` and `resetBaseline()`. Guarded the background audit loop in `HardwareSuite.kt` with `isStarted.get()` to prevent `pendingEnterRt` from accumulating stall duration while the suite is inactive, ensuring no immediate hardware locks occur upon activation. (R-ID 360)

*   **Issue #1106: Energy Footprint Data Loss following Intermediate Audit Consumption** (Resolved Sep.19.02)
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
- **Current Audit Baseline: [SOT: 364 (Rules: 73, IDs: 364), Resolved: 1110, Open: 11, Testing: 2 (Sub-items: 10), Ideas: 18, QA: 282]**
