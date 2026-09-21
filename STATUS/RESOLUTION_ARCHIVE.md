# Project Resolution Archive (Sep.21.121)

## 🟢 Sep.21.121
*   **Unified Vibration Authority (#1143)**: Consolidated the `adaptiveVibrationFloor` calculation in `HardwareSuite.kt`. The high-frequency floor is now snapshotted and propagated to `LocationSentinel` via `TrackerService.processTick()`, ensuring that both the hardware layer and the validation engine operate on a single source of truth for stationary detection (R-ID 388).
*   **Non-Blocking Acoustic Teardown (#1123)**: Removed the synchronous `acousticThread.join(1000)` from `HardwareSuite.stopAcousticMonitoring()`. Resource exclusivity is now maintained via the join-before-start pattern in `startAcousticMonitoring()`, which waits for any lingering thread to exit before initializing a new one. This eliminates service lifecycle stalls and potential ANRs during service termination (R-ID 387).

## 🟢 Sep.21.120
*   **GPS Telemetry Conflation Hardening (#1146)**: Replaced single-point location variable in `TrackerService.kt` with a thread-safe `ConcurrentLinkedQueue` buffer. The logic tick now drains and processes all intermediate fixes accumulated between 2-second pulses, preventing the loss of high-resolution trail points and maintaining forensic jitter audit precision (R-ID 386).

## 🟢 Sep.20.22
*   **Missing Forensic State Reset in TrackerService (#1137)**: Explicitly zeroed all forensic sampling state variables (`recoveryTriggerRt`, `lastWasCooling`, and spatial/IMU gates like `lastForensicLat`) in `TrackerService.resetServiceTimers()`. This ensures that a session restart provides a clean slate for thermal recovery audits and sampling triggers (R-ID 384).

## 🟢 Sep.20.20
*   **Stale Integrity Vitality Timestamps (#1142)**: Reset all vitality update timestamps (`lastInternetUpdateRt`, `lastBatteryUpdateRt`, etc.) in `IntegrityMonitor.resetStats()`. This prevents spurious "Flow Stall" alerts when the service restarts (R-ID 383).

## 🟢 Sep.20.18
*   **Completed Fast-Path Light Spike Integration (#1149)**: Captured `lastFastPathLightSpikeTs` in `TrackerService` hardware callbacks and propagated it to `LocationProcessor.processGpsPoint()` to trigger immediate locking/tamper responses. Transient light spikes occurring between tick intervals are now acted upon by the validation engine (R-ID 380).
*   **Harmonized Light Baseline Synchronization (#1150)**: Implemented periodic re-synchronization of the fast-path baseline in `TrackerService.processTick()`. This prevents divergence between the `LocationSentinel` lux baseline and the high-frequency fast-path check in `HardwareSuite` (R-ID 381).
*   **ForensicAuditor Thread Safety Hardening (#1132)**: Implemented internal synchronization for `RoleState` within `ForensicAuditor.kt`. This ensures atomic check-and-set operations for jitter peaks and stability counters, preventing race conditions between GNSS/Sensor provider threads and the service Tick thread (R-ID 382).

## 🟢 Sep.20.15
*   **Selective Forensic Audit Reset (Multi-Role Collision) (#1124)**: Updated `HardwareSuite.resetBaseline(roleTag)` and `ForensicAuditor.reset(roleTag)` to support targeted role resets. This ensures that session restarts in the Tracker role do not inadvertently wipe stability or jitter data for an active Viewer role (R-ID 376).
*   **Hardware Lifecycle Hardening (#1127/1128/1133/1135)**: Implemented `clearLifecycleLeftovers()` in `HardwareSuite.kt`. Explicitly zeroed all transient peak accumulators (`secPeakLux`, etc.), proximity markers (`rawProximityNear`), temporal lockouts (`lastAcousticLockoutRt`), and plunge phases on suite stop and baseline reset (R-ID 377).
*   **Telemetry Propagation Hardening (#1138/1147)**: Expanded `PendingStatusEntity` and `HistoryEntity` in `Database.kt` (Migration v76) to include `gpsHardwareLock` and `isGnssThrottled` flags. Updated `TelemetryMapper` and `ConnectivitySuite` to ensure these diagnostic markers are persisted during offline drops and correctly reflected on remote peer dashboards (R-ID 378).
*   **False GNSS Jitter Spike on Suite Restart (#1122)**: Explicitly zeroed `ForensicAuditor.lastGnssStatusRt` in `reset()` and `resetGnssJitter()`. This ensures that the first GNSS event after a suite restart does not calculate jitter against a timestamp from a previous session (R-ID 379).
*   **Fast-Path Light Baseline Stale Persistence (#1148)**: Explicitly reset `fastPathLightBaseline` and `lastLightSpikeRt` in `HardwareSuite.resetBaseline()`, ensuring a fresh detection threshold on session resumption.
*   **Sensor Rate Audit Suppression (#1125)**: Ensured `isSensorRateAudited` is cleared in `ForensicAuditor.reset()`, allowing the efficacy audit to re-run on every service restart.

## 🟢 Sep.20.10
*   **Missing Light Fast-Path Integration in TrackerService (#1130)**: Initialized the light sensor fast-path (`setLightFastPath`) in `TrackerService.setupPhysicalFastPaths()` using the baseline from `LocationProcessor` and the `LIGHT_THRESHOLD_LUX_JUMP` threshold. This ensures the specialized light-spike logic in `HardwareSuite` is alive and active for light-based tampering detection (R-ID 375).

## 🟢 Sep.20.02
*   **ViewerService Local Hardware Leak into Remote Alarm Evaluation (#1121)**: Resolved a hardware leak in `ViewerService.evaluateAlarmsInternal` where the Viewer's local SNR and vibration snapshots were used to evaluate remote Tracker alarms. Telemetry evaluation now strictly uses `snrIdx` and `vibeIdx` from the remote `TrackerStatus`, ensuring accurate Jammer and Stall detection for tracked devices (R-ID 374).

## 🟢 Sep.20.00
*   **Inconsistent Jitter Audit during Adaptive GNSS Throttling (#1120)**: Resolved inconsistent jitter calculation in `ForensicAuditor.kt` by replacing the hardcoded 1000ms expected interval with a dynamic parameter. Updated `HardwareSuite.gnssStatusCallback` to calculate the active sampling interval (2s/5s) during GNSS status updates, ensuring that intentional performance throttling no longer triggers false hardware instability alerts (R-ID 373).

*(All other resolved issues have been successfully moved to the Resolution Archive file).*
