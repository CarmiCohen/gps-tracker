# Project Issues & Hardening Tracking (Rigorous Audit)

## 🎯 Current Resumption Focus: Structural Simplicity & Pattern Convergence
Finalizing the audit of signaling performance under physical stress and ensuring no side-effects remain from the Performance Tier unification.

## 🔴 Open Gaps & Unfinished Integration Points (Identified from Rigorous Audit)

### Missing Functionality & Unfinished Integration
*   **Issue #1124: Non-Selective Forensic Audit Reset (Multi-Role Collision)**
    *   *Problem*: `HardwareSuite.resetBaseline()` calls `forensicAuditor.reset()` without arguments, which clears role states for *all* tags ("T" and "V"). If the Tracker role resets its session, it inadvertently wipes the stability and jitter metrics for an active Viewer role.
    *   *File*: `HardwareSuite.kt` (line 746), `ForensicAuditor.kt` (lines 207-214)
    *   *Risk*: Inconsistent audit history when both service roles are active.

*   **Issue #1138: Missing GNSS Throttling Propagation in Telemetry**
    *   *Problem*: `ConnectivitySuite.pushCurrentStatus` does not accept or transmit the `isGnssThrottled` flag. Consequently, the remote Viewer role remains unaware when a Tracker is undergoing adaptive GNSS throttling, potentially misinterpreting the resulting latency as a hardware degradation rather than an intentional thermal policy.
    *   *File*: `ConnectivitySuite.kt` (lines 560-610), `TrackerService.kt` (line 387)
    *   *Risk*: Viewer-side telemetry inconsistency and false performance audits of remote peers.

*   **Issue #1147: GPS Hardware Lock state lost in Offline Storage**
    *   *Problem*: `ConnectivitySuite.flushPendingUpdates` hardcodes `gpsHardwareLock = false` when reconstructing a `TrackerStatus` from a `PendingStatusEntity`. If a critical stall occurs while offline, the Hardware Lock status is lost.
    *   *File*: `ConnectivitySuite.kt` (line 347)
    *   *Risk*: Loss of critical forensic diagnostic data across connection drops.

### Unhandled Edge Cases & Lifecycle Vulnerabilities
*   **Issue #1122: False GNSS Jitter Spike on Suite Restart**
    *   *Problem*: `ForensicAuditor.lastGnssStatusRt` is not reset when `HardwareSuite` is stopped or started. The first GNSS status event after a suite restart calculates jitter against a timestamp from the previous session, causing a massive false jitter peak that propagates to all active roles.
    *   *File*: `ForensicAuditor.kt` (line 74)
    *   *Risk*: False stability alerts and reliability violations on every service restart.

*   **Issue #1125: Sensor Rate Audit Suppression across Service Sessions**
    *   *Problem*: The `isSensorRateAudited` flag in `ForensicAuditor.RoleState` is never reset in `HardwareSuite.stop()`. Once a sensor rate audit completes, it will never run again until the entire app process is killed.
    *   *File*: `ForensicAuditor.kt` (line 52)
    *   *Risk*: Failure to detect hardware sensor degradation or failures occurring after a service restart.

*   **Issue #1127: Hysteresis and Lockout Persistence in Hardware State**
    *   *Problem*: Temporal state variables used for hysteresis and lockout (`lastAnomalyActiveRt`, `lastAcousticLockoutRt`, `lastLightSpikeRt`) are not reset in `stop()` or `resetBaseline()`. Stale state from a previous high-load or noisy session can pollute the behavior of a new session.
    *   *File*: `HardwareSuite.kt` (lines 217, 232)
    *   *Risk*: Unintended GNSS throttling or acoustic suppression at the start of a fresh tracking session.

*   **Issue #1128: Stale Proximity State on Hardware Re-initialization**
    *   *Problem*: `rawProximityNear` in `HardwareSuite.kt` is not reset on stop/reset. If the physical proximity state at suite start matches the state at the end of the previous session, the transition check (`newValue != rawProximityNear`) fails, leading to incorrect proximity status.
    *   *File*: `HardwareSuite.kt` (line 250)
    *   *Risk*: Incorrect "Far" status during critical session initialization.

*   **Issue #1133: Stale Peak Accumulators across Lifecycle**
    *   *Problem*: Intermediate accumulators used for `sensorBuffer` (`secPeakLux`, `secPeakVibe`, `secSumProxIdx`, `secProxCount`, `secPeakTilt`, `secPeakLift`, `secPeakDb`, `secPeakKinetic`) are not reset in `stop()` or `resetBaseline()`. 
    *   *File*: `HardwareSuite.kt` (lines 222-226)
    *   *Risk*: Inaccurate telemetry history and false peaks on session start.

*   **Issue #1135: Stale Plunge Phase and Stationary Timing Persistence**
    *   *Problem*: `plungePhase` and `stationaryStartRt` are not reset in `stop()`. If restarted after a move, it may carry over stale stationary or "mid-plunge" state.
    *   *File*: `HardwareSuite.kt` (lines 272, 186)
    *   *Risk*: Incorrect "Sit Detected" events during the initialization phase.

*   **Issue #1137: Missing Forensic State Reset in TrackerService**
    *   *Problem*: `recoveryTriggerRt`, `lastWasCooling`, and spatial/IMU gates (`lastForensicLat`, `lastForensicLng`, `lastForensicVibe`, `lastForensicTilt`) are not reset in `resetServiceTimers()`. 
    *   *File*: `TrackerService.kt` (lines 53-65)
    *   *Risk*: Forensic log gaps and inaccurate thermal recovery latency reports.

*   **Issue #1142: Stale Integrity Vitality Timestamps**
    *   *Problem*: `lastInternetUpdateRt`, `lastBatteryUpdateRt`, etc., in `IntegrityMonitor` are not cleared in `resetStats()`. 
    *   *File*: `IntegrityMonitor.kt` (lines 61-65, 497)
    *   *Risk*: Spurious "Flow Stall" alerts immediately after service startup.

*   **Issue #1146: GPS Data Loss in TrackerService Tick Conflation**
    *   *Problem*: `TrackerService` conflates GPS updates in `onLocationChanged` but only processes the *last* one during the 2s `processTick`. Intermediate updates are discarded.
    *   *File*: `TrackerService.kt` (line 343)
    *   *Risk*: Degraded tracking precision and loss of forensic detail.

### Unintended Side Effects & Thread Safety
*   **Issue #1123: Synchronous Thread Join in HardwareSuite Lifecycle**
    *   *Problem*: `startAcousticMonitoring` performs a synchronous `acousticThread?.join(1500)` while holding a lock. This blocks service initialization.
    *   *File*: `HardwareSuite.kt` (line 540)
    *   *Risk*: Service start latency and potential ANRs.

*   **Issue #1132: ForensicAuditor Multi-Thread Race Condition**
    *   *Problem*: `RoleState` members are written by GNSS/Sensor threads and read/reset by the service Tick thread without synchronization.
    *   *File*: `ForensicAuditor.kt` (lines 35-53)
    *   *Risk*: Memory inconsistency and inaccurate stability reports.

*   **Issue #1126: Missing Thread Visibility markers in HardwareSuite**
    *   *Problem*: Critical timing variables (e.g., `lastBufferRecordRt`, `stationaryStartRt`, `lastStayAliveRt`, `revivalBaselineCaptured`) lack `@Volatile` markers.
    *   *File*: `HardwareSuite.kt`
    *   *Risk*: Unpredictable behavior in adaptive throttling and sensor buffering.

*   **Issue #1143: Divergent Vibration Floor Calculation (Dual-State Logic)**
    *   *Problem*: `HardwareSuite` and `LocationSentinel` independently calculate `adaptiveVibrationFloor`, leading to potential divergence in stationary detection.
    *   *File*: `HardwareSuite.kt`, `LocationSentinel.kt`
    *   *Risk*: Inconsistent polling intervals or anchor locking.

---

## 🟢 Resolved Traceability & Metadata Issues

*   **Issue #1130: Missing Light Fast-Path Integration in TrackerService** (Resolved Sep.20.10)
    *   *Remediation*: Initialized the light sensor fast-path (`setLightFastPath`) in `TrackerService.setupPhysicalFastPaths()` using the baseline from `LocationProcessor` and the `LIGHT_THRESHOLD_LUX_JUMP` threshold. This ensures the specialized light-spike logic in `HardwareSuite` is alive and active for light-based tampering detection. (R-ID 375)

*   **Issue #1121: ViewerService Local Hardware Leak into Remote Alarm Evaluation** (Resolved Sep.20.02)
    *   *Remediation*: Refactored `ViewerService.evaluateAlarmsInternal` to use remote telemetry (`snrIdx`, `vibeIdx`) from `connectivitySuite.trackerStatus` instead of local `hardwareSuite` snapshots. Corrected propagation of `isJammer`, `isStalled`, and `isGpsGap` flags in the evaluation loop, ensuring the Viewer role accurately reflects the remote Tracker's GNSS integrity. (R-ID 374)

*   **Issue #1120: Inconsistent Jitter Audit during Adaptive GNSS Throttling** (Resolved Sep.20.00)
    *   *Remediation*: Updated `ForensicAuditor.recordGnssStatus` to accept a dynamic `expectedIntervalMs` parameter. Modified `HardwareSuite.gnssStatusCallback` to calculate the active interval (accounting for Performance Tier throttling) and pass it to the auditor, eliminating false jitter alerts during intentional thermal/load cooling cycles. (R-ID 373)

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

*   **Issue #1109: Resource Leak in GNSS Revival Burst during Safe Mode transition** (Resolved Sep.19.04)
    *   *Remediation*: Refactored the GNSS revival pulse logic in `HardwareSuite.kt` to use structured concurrency. By wrapping the burst lifecycle in a `try-finally` block within a single coroutine, Raw and Fused listeners are guaranteed to be unregistered upon completion or cancellation (e.g., during Safe Mode transition). Eliminated the redundant `revivalBurstJob` variable. (R-ID 363)

*   **Issue #1108: Redundant Battery Baseline Capture in Background/Idle State** (Resolved Sep.19.03)
    *   *Remediation*: Fully resolved the redundant battery baseline capture logic by initializing `lastFixRt` to `sessionStartRt` inside both `start()` and `resetBaseline()`. This guarantees a proper grace period before any GNSS gap or stall state can be declared, preventing immediate redundant battery baselines upon app activation or reset. (R-ID 361)

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
- **Current Audit Baseline: [SOT: 375 (Rules: 78, IDs: 375), Resolved: 1122, Open: 17, Testing: 2 (Sub-items: 10), Ideas: 19, QA: 282]**
